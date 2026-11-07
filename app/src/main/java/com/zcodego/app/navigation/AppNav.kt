package com.zcodego.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zcodego.app.R
import com.zcodego.app.data.AppController
import com.zcodego.app.data.HostMode
import com.zcodego.app.security.ParseResult
import com.zcodego.app.security.RemoteUrlPolicy
import com.zcodego.app.ui.local.LocalHomeScreen
import com.zcodego.app.ui.onboarding.OnboardingScreen
import com.zcodego.app.ui.remote.PairingScreen
import com.zcodego.app.ui.remote.RemoteHomeScreen
import com.zcodego.app.ui.remote.ScannerScreen
import com.zcodego.app.ui.remote.messageRes
import com.zcodego.app.ui.remote.readClipboard
import com.zcodego.app.ui.settings.SettingsScreen
import com.zcodego.app.ui.web.OfficialDestination
import com.zcodego.app.ui.web.WebScreen
import kotlinx.coroutines.launch

private object Routes {
    const val ONBOARDING = "onboarding"
    const val LOCAL = "local"
    const val REMOTE = "remote"
    const val SETTINGS = "settings"
    const val SCANNER = "scanner"
    const val PAIRING = "pairing?autoScan={autoScan}"
    const val SESSION = "session/{sessionId}"
    const val OFFICIAL = "official/{page}"
    const val SCANNED_URL = "scanned_url"

    fun pairing(autoScan: Boolean) = "pairing?autoScan=$autoScan"
    fun session(id: String) = "session/$id"
    fun official(page: String) = "official/$page"
}

@Composable
fun AppNav(
    controller: AppController,
    initialHostMode: HostMode?,
    reopenSessionId: String?,
) {
    val nav = rememberNavController()
    val start = remember {
        when (initialHostMode) {
            null -> Routes.ONBOARDING
            HostMode.LOCAL -> Routes.LOCAL
            HostMode.REMOTE -> Routes.REMOTE
        }
    }
    val reopen = remember { reopenSessionId }
    var consumedReopen by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var openingLink by remember { mutableStateOf(false) }

    fun openRawLink(raw: String) {
        when (val parsed = RemoteUrlPolicy.parseRemoteLink(raw)) {
            is ParseResult.Reject -> {
                scope.launch { snackbar.showSnackbar(context.getString(parsed.reason.messageRes())) }
            }
            is ParseResult.Ok -> {
                if (openingLink) return
                openingLink = true
                scope.launch {
                    try {
                        controller.setHostMode(HostMode.REMOTE)
                        val session = controller.saveLink(parsed.link)
                        if (session == null) {
                            snackbar.showSnackbar(context.getString(R.string.err_save))
                        } else {
                            nav.resetTo(Routes.REMOTE)
                            nav.navigate(Routes.session(session.id))
                        }
                    } finally {
                        openingLink = false
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        controller.forgetStaleLastSession()
    }

    LaunchedEffect(reopen) {
        if (!consumedReopen && !reopen.isNullOrBlank()) {
            consumedReopen = true
            nav.navigate(Routes.session(reopen))
        }
    }

    LaunchedEffect(Unit) {
        controller.linkInbox.accepted.collect { url ->
            if (url.isNullOrBlank()) return@collect
            val raw = controller.linkInbox.consumeAccepted() ?: return@collect
            openRawLink(raw)
        }
    }

    LaunchedEffect(Unit) {
        controller.linkInbox.rejected.collect { reason ->
            if (reason == null) return@collect
            controller.linkInbox.consumeRejected()
            snackbar.showSnackbar(context.getString(reason.messageRes()))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(navController = nav, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onConfirm = { mode ->
                    scope.launch {
                        controller.setHostMode(mode)
                        if (mode == HostMode.LOCAL) {
                            nav.resetTo(Routes.LOCAL)
                        } else {
                            nav.navigate(Routes.pairing(autoScan = false))
                        }
                    }
                },
            )
        }
        composable(Routes.LOCAL) {
            LocalHomeScreen(
                controller = controller,
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenOfficial = { dest -> nav.navigate(Routes.official(dest.route)) },
                onOpenSession = { id ->
                    scope.launch {
                        controller.markOpened(id)
                        nav.navigate(Routes.session(id))
                    }
                },
                onDeleteSession = { id -> scope.launch { controller.deleteSession(id) } },
                onSwitchToRemote = {
                    scope.launch {
                        controller.setHostMode(HostMode.REMOTE)
                        nav.resetTo(Routes.REMOTE)
                        nav.navigate(Routes.pairing(autoScan = false))
                    }
                },
            )
        }
        composable(Routes.REMOTE) {
            RemoteHomeScreen(
                controller = controller,
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onScan = { nav.navigate(Routes.pairing(autoScan = true)) },
                onPaste = { nav.navigate(Routes.pairing(autoScan = false)) },
                onOpenSession = { id ->
                    scope.launch {
                        controller.markOpened(id)
                        nav.navigate(Routes.session(id))
                    }
                },
                onDeleteSession = { id -> scope.launch { controller.deleteSession(id) } },
                onClipboard = { openRawLink(readClipboard(context)) },
            )
        }
        composable(
            route = Routes.PAIRING,
            arguments = listOf(
                navArgument("autoScan") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            PairingScreen(
                controller = controller,
                pendingScan = entry.savedStateHandle.get<String>(Routes.SCANNED_URL),
                autoScan = entry.arguments?.getBoolean("autoScan") ?: false,
                onPendingScanConsumed = { entry.savedStateHandle.remove<String>(Routes.SCANNED_URL) },
                onScan = { nav.navigate(Routes.SCANNER) },
                onBack = { nav.popBackStack() },
                onConnected = { id ->
                    nav.resetTo(Routes.REMOTE)
                    nav.navigate(Routes.session(id))
                },
                onSkip = { nav.resetTo(Routes.REMOTE) },
            )
        }
        composable(Routes.SCANNER) {
            ScannerScreen(
                onBack = { nav.popBackStack() },
                onAccepted = { url ->
                    nav.previousBackStackEntry?.savedStateHandle?.set(Routes.SCANNED_URL, url)
                    nav.popBackStack()
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                controller = controller,
                onBack = { nav.popBackStack() },
                onHostModeChanged = { mode ->
                    nav.resetTo(if (mode == HostMode.LOCAL) Routes.LOCAL else Routes.REMOTE)
                },
                onOpenDocs = { nav.navigate(Routes.official(OfficialDestination.DOCS.route)) },
            )
        }
        composable(
            route = Routes.SESSION,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("sessionId").orEmpty()
            val sessions by controller.sessions.collectAsStateWithLifecycle()
            val session = sessions.firstOrNull { it.id == id }
            val parsedOk = session != null && RemoteUrlPolicy.parseRemoteLink(session.url) is ParseResult.Ok
            if (session == null || !parsedOk) {
                MissingDestination(onBack = { nav.popBackStack() })
            } else {
                WebScreen(
                    title = session.deviceName.ifBlank { stringResource(R.string.unnamed_device) },
                    url = session.url,
                    onBack = { nav.popBackStack() },
                    keepAlive = true,
                    sessions = sessions,
                    currentSessionId = id,
                    onSwitchSession = { nextId ->
                        scope.launch {
                            controller.markOpened(nextId)
                            nav.popBackStack()
                            nav.navigate(Routes.session(nextId))
                        }
                    },
                )
            }
        }
        composable(
            route = Routes.OFFICIAL,
            arguments = listOf(navArgument("page") { type = NavType.StringType }),
        ) { entry ->
            val destination = OfficialDestination.fromRoute(entry.arguments?.getString("page"))
            if (destination == null || !RemoteUrlPolicy.isAllowedNavigation(destination.url)) {
                MissingDestination(onBack = { nav.popBackStack() })
            } else {
                WebScreen(
                    title = stringResource(destination.titleRes),
                    url = destination.url,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

private fun NavHostController.resetTo(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun MissingDestination(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(R.string.err_session_missing))
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.cd_back))
            }
        }
    }
}
