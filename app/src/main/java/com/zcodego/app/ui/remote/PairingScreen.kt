package com.zcodego.app.ui.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zcodego.app.R
import com.zcodego.app.data.AppController
import com.zcodego.app.security.RejectReason
import com.zcodego.app.ui.components.InfoBanner
import com.zcodego.app.ui.components.ZCodeBackdrop
import com.zcodego.app.ui.components.ZCodeButton
import com.zcodego.app.ui.components.ZCodeOutlinedButton
import kotlinx.coroutines.launch

@Composable
fun PairingScreen(
    controller: AppController,
    pendingScan: String?,
    autoScan: Boolean,
    onPendingScanConsumed: () -> Unit,
    incomingReasonName: String? = null,
    incomingText: String = "",
    onIncomingConsumed: () -> Unit = {},
    onScan: () -> Unit,
    onBack: () -> Unit,
    onConnected: (String) -> Unit,
    onSkip: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    var showPaste by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<RejectReason?>(null) }
    var saveFailed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val storageFailed by controller.storageFailed.collectAsStateWithLifecycle()
    var launchedScan by rememberSaveable { mutableStateOf(false) }

    fun connect(raw: String, alreadyPulsed: Boolean = false) {
        if (saving) return
        val trimmed = raw.trim()
        when (val outcome = ScanOutcome.decide(trimmed)) {
            is ScanOutcome.Decline -> {
                error = outcome.reason
                showPaste = true
                if (trimmed.isNotEmpty()) draft = trimmed
            }
            is ScanOutcome.Accept -> {
                if (!alreadyPulsed) ScanOutcome.pulse(view)
                saving = true
                error = null
                saveFailed = false
                scope.launch {
                    val session = controller.saveLink(outcome.link)
                    saving = false
                    if (session == null) {
                        saveFailed = true
                        showPaste = true
                        draft = trimmed
                    } else {
                        onConnected(session.id)
                    }
                }
            }
        }
    }

    LaunchedEffect(incomingReasonName, incomingText) {
        val name = incomingReasonName ?: return@LaunchedEffect
        val reason = runCatching { RejectReason.valueOf(name) }.getOrNull() ?: return@LaunchedEffect
        error = reason
        showPaste = true
        if (incomingText.isNotBlank()) draft = incomingText
        onIncomingConsumed()
    }

    LaunchedEffect(autoScan) {
        if (autoScan && !launchedScan) {
            launchedScan = true
            onScan()
        }
    }

    LaunchedEffect(pendingScan) {
        val raw = pendingScan?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        onPendingScanConsumed()
        connect(raw, alreadyPulsed = true)
    }

    ZCodeBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxHeight(),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Laptop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.pairing_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { heading() },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.pairing_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    if (storageFailed) {
                        InfoBanner(
                            title = stringResource(R.string.remote_title),
                            body = stringResource(R.string.err_storage),
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    ZCodeButton(
                        text = stringResource(R.string.pairing_scan),
                        onClick = onScan,
                        enabled = !saving,
                    )
                    Spacer(Modifier.height(12.dp))
                    ZCodeOutlinedButton(
                        text = stringResource(R.string.pairing_paste),
                        onClick = { showPaste = true },
                        enabled = !saving,
                    )
                    if (showPaste) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = draft,
                            onValueChange = {
                                draft = it
                                error = null
                                saveFailed = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.pairing_field_label)) },
                            placeholder = { Text(stringResource(R.string.pairing_placeholder)) },
                            minLines = 3,
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done,
                                autoCorrectEnabled = false,
                            ),
                            keyboardActions = KeyboardActions(onDone = { connect(draft) }),
                            isError = error != null || saveFailed,
                        )
                        TextButton(
                            onClick = {
                                val text = readClipboard(context)
                                if (text.isBlank()) {
                                    error = RejectReason.EMPTY
                                    showPaste = true
                                } else {
                                    draft = text
                                    connect(text)
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.pairing_clipboard))
                        }
                        val message = when {
                            error != null -> rejectMessage(error!!)
                            saveFailed -> stringResource(R.string.err_save)
                            else -> null
                        }
                        if (message != null) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        ZCodeButton(
                            text = stringResource(R.string.pairing_connect),
                            onClick = { connect(draft) },
                            enabled = draft.isNotBlank() && !saving,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.pairing_security),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    Text(stringResource(R.string.pairing_skip))
                }
            }
        }
    }
}
