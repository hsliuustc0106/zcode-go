package com.zcodego.app.ui.web

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.zcodego.app.BuildConfig
import com.zcodego.app.R
import com.zcodego.app.ZCodeGoApp
import com.zcodego.app.data.RemoteSession
import com.zcodego.app.remote.FollowUpInsert
import com.zcodego.app.remote.InsertOutcome
import com.zcodego.app.remote.PageAttentionNotifier
import com.zcodego.app.remote.RemoteKeepAliveService
import com.zcodego.app.security.RemoteUrlPolicy
import com.zcodego.app.ui.components.ZCodeButton
import com.zcodego.app.ui.components.ZCodeOutlinedButton
import kotlinx.coroutines.launch

private const val VIEWPORT_JS = """
(function(){
  try {
    var head = document.head || document.getElementsByTagName('head')[0];
    if (!head) return;
    var meta = document.querySelector('meta[name="viewport"]');
    if (!meta) {
      meta = document.createElement('meta');
      meta.setAttribute('name', 'viewport');
      meta.setAttribute('content', 'width=device-width, initial-scale=1, viewport-fit=cover');
      head.appendChild(meta);
    }
  } catch (e) {}
})();
"""

internal sealed interface FatalWebError {
    data object Ssl : FatalWebError
    data object Load : FatalWebError
    data object Threat : FatalWebError
    data object Unavailable : FatalWebError
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    title: String,
    url: String,
    onBack: () -> Unit,
    keepAlive: Boolean = false,
    sessions: List<RemoteSession> = emptyList(),
    currentSessionId: String? = null,
    onSwitchSession: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as ZCodeGoApp
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var fatal by remember { mutableStateOf<FatalWebError?>(null) }
    var loading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var showSwitcher by remember { mutableStateOf(false) }
    var sawOffline by remember { mutableStateOf(false) }
    val online = rememberNetworkAvailable()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val blockedTemplate = stringResource(R.string.err_blocked)
    val blockedUnknown = stringResource(R.string.err_blocked_unknown)
    val webView = remember(url) {
        if (!RemoteUrlPolicy.isAllowedNavigation(url)) {
            null
        } else {
            runCatching {
                createPolicyWebView(
                    context = context,
                    onProgress = { progress = it },
                    onLoading = { loading = it },
                    onFatal = { fatal = it },
                    onTitle = { pageTitle ->
                        if (keepAlive) {
                            PageAttentionNotifier.onTitle(
                                context = context,
                                rawTitle = pageTitle,
                                appInForeground = app.processInForeground.value,
                            )
                        }
                    },
                    onBlockedHost = { host ->
                        val label = safeHostLabel(host)
                        val message = if (label == null) {
                            blockedUnknown
                        } else {
                            blockedTemplate.format(label)
                        }
                        scope.launch { snackbar.showSnackbar(message) }
                    },
                )
            }.getOrNull()
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView?.apply {
                stopLoading()
                (parent as? android.view.ViewGroup)?.removeView(this)
                destroy()
            }
        }
    }

    if (webView == null) {
        FatalPane(
            title = title,
            message = stringResource(
                if (RemoteUrlPolicy.isAllowedNavigation(url)) {
                    R.string.err_web_unavailable
                } else {
                    R.string.err_blocked_unknown
                },
            ),
            retryLabel = stringResource(R.string.action_retry),
            onBack = onBack,
            onRetry = null,
        )
        return
    }

    LaunchedEffect(keepAlive) {
        if (!keepAlive || Build.VERSION.SDK_INT < 33) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LaunchedEffect(online) {
        if (!online) sawOffline = true
    }

    DisposableEffect(keepAlive, title) {
        if (keepAlive) RemoteKeepAliveService.start(context, title)
        onDispose {
            if (keepAlive) RemoteKeepAliveService.stop(context)
        }
    }

    BackHandler {
        if (fatal != null) {
            onBack()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            onBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (webView.canGoBack() && fatal == null) webView.goBack() else onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    if (keepAlive && sessions.isNotEmpty()) {
                        IconButton(onClick = { showSwitcher = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Devices,
                                contentDescription = stringResource(R.string.cd_switch_session),
                            )
                        }
                    }
                    IconButton(onClick = {
                        fatal = null
                        sawOffline = false
                        loading = true
                        webView.loadUrl(url)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.cd_reload),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )
            if (loading && fatal == null) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            }
            if (fatal == null && !online) {
                StatusBanner(
                    text = stringResource(R.string.net_offline),
                    action = stringResource(R.string.action_reconnect),
                    onAction = {
                        fatal = null
                        loading = true
                        webView.loadUrl(url)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            } else if (fatal == null && sawOffline && online) {
                StatusBanner(
                    text = stringResource(R.string.net_back),
                    action = stringResource(R.string.action_reconnect),
                    onAction = {
                        sawOffline = false
                        loading = true
                        webView.loadUrl(url)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            val currentFatal = fatal
            if (currentFatal != null) {
                FatalPane(
                    title = title,
                    message = stringResource(currentFatal.messageRes()),
                    retryLabel = stringResource(if (keepAlive) R.string.action_reconnect else R.string.action_retry),
                    onBack = onBack,
                    onRetry = {
                        fatal = null
                        loading = true
                        webView.loadUrl(url)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
            if (keepAlive && fatal == null) {
                FollowUpBar(
                    onInsert = { text, done ->
                        val started = runCatching {
                            webView.evaluateJavascript(FollowUpInsert.script(text)) { raw ->
                                done(FollowUpInsert.interpret(raw))
                            }
                        }
                        if (started.isFailure) done(InsertOutcome.NO_TARGET)
                    },
                    onMessage = { message -> scope.launch { snackbar.showSnackbar(message) } },
                )
            }
        }
    }

    if (showSwitcher) {
        ModalBottomSheet(onDismissRequest = { showSwitcher = false }) {
            Text(
                text = stringResource(R.string.switch_session_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            sessions.forEach { session ->
                val name = session.visibleName(stringResource(R.string.unnamed_device))
                ListItem(
                    headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        val mark = if (session.id == currentSessionId) {
                            stringResource(R.string.switch_current)
                        } else {
                            session.protocolLabel
                        }
                        Text(mark)
                    },
                    modifier = Modifier.clickable {
                        showSwitcher = false
                        if (session.id != currentSessionId) onSwitchSession(session.id)
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    LaunchedEffect(url) {
        if (webView.url == null) {
            webView.loadUrl(url)
        }
    }
}

private fun FatalWebError.messageRes(): Int = when (this) {
    FatalWebError.Ssl -> R.string.err_ssl
    FatalWebError.Load -> R.string.err_load
    FatalWebError.Threat -> R.string.err_threat
    FatalWebError.Unavailable -> R.string.err_web_unavailable
}

@Composable
private fun FatalPane(
    title: String,
    message: String,
    retryLabel: String,
    onBack: () -> Unit,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (onRetry != null) {
            ZCodeButton(text = retryLabel, onClick = onRetry)
            Spacer(Modifier.height(12.dp))
        }
        ZCodeOutlinedButton(text = stringResource(R.string.cd_back), onClick = onBack)
    }
}

@Composable
private fun StatusBanner(
    text: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.inverseSurface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(text = text, color = MaterialTheme.colorScheme.inverseOnSurface)
            TextButton(onClick = onAction) {
                Text(action)
            }
        }
    }
}

internal fun safeHostLabel(host: String?): String? {
    if (host.isNullOrBlank()) return null
    val trimmed = host.trim().trimEnd('.').take(80)
    if (!trimmed.matches(Regex("""[A-Za-z0-9.\-]+"""))) return null
    return trimmed
}

@SuppressLint("SetJavaScriptEnabled")
private fun createPolicyWebView(
    context: android.content.Context,
    onProgress: (Int) -> Unit,
    onLoading: (Boolean) -> Unit,
    onFatal: (FatalWebError) -> Unit,
    onTitle: (String) -> Unit,
    onBlockedHost: (String?) -> Unit,
): WebView {
    return WebView(context).apply {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        isFocusable = true
        isFocusableInTouchMode = true
        WebView.setWebContentsDebuggingEnabled(false)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        settings.allowUniversalAccessFromFileURLs = false
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)
        settings.setGeolocationEnabled(false)
        settings.mediaPlaybackRequiresUserGesture = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.safeBrowsingEnabled = true
        val baseAgent = settings.userAgentString.orEmpty()
        settings.userAgentString = "$baseAgent zcode-go/${BuildConfig.VERSION_NAME}"
        // No JavascriptInterface is registered. The page cannot call into the app.
        val cookies = android.webkit.CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(this, true)
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgress(newProgress)
                if (newProgress >= 100) onLoading(false)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) onTitle(title)
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: android.webkit.GeolocationPermissions.Callback?,
            ) {
                callback?.invoke(origin, false, false)
            }

            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                request?.deny()
            }
        }
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val raw = request.url?.toString() ?: return true
                if (!RemoteUrlPolicy.isAllowedNavigation(raw)) {
                    if (request.isForMainFrame) onBlockedHost(request.url?.host)
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                onLoading(true)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (view != null && url != null && url != "about:blank") {
                    view.evaluateJavascript(VIEWPORT_JS, null)
                }
                onLoading(false)
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.cancel()
                onFatal(FatalWebError.Ssl)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError,
            ) {
                if (!request.isForMainFrame) return
                onFatal(FatalWebError.Load)
            }

            override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                onFatal(FatalWebError.Load)
                return true
            }

            override fun onSafeBrowsingHit(
                view: WebView,
                request: WebResourceRequest,
                threatType: Int,
                callback: android.webkit.SafeBrowsingResponse,
            ) {
                if (Build.VERSION.SDK_INT >= 27) {
                    callback.backToSafety(true)
                }
                onFatal(FatalWebError.Threat)
            }
        }
        setDownloadListener { _, _, _, _, _ ->
            onBlockedHost(null)
        }
    }
}
