package com.zcodego.app.ui.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zcodego.app.R
import com.zcodego.app.data.AppController
import com.zcodego.app.data.RemoteSession
import com.zcodego.app.ui.components.InfoBanner
import com.zcodego.app.ui.components.ZCodeButton
import com.zcodego.app.ui.components.ZCodeOutlinedButton
import com.zcodego.app.ui.web.SessionLinkCopy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteHomeScreen(
    controller: AppController,
    onSettings: () -> Unit,
    onScan: () -> Unit,
    onPaste: () -> Unit,
    onOpenSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onClipboard: () -> Unit,
) {
    val sessions by controller.sessions.collectAsStateWithLifecycle()
    val settings by controller.settings.collectAsStateWithLifecycle()
    val storageFailed by controller.storageFailed.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<RemoteSession?>(null) }
    var pendingRename by remember { mutableStateOf<RemoteSession?>(null) }
    val now = rememberNow()
    val lastId = settings?.lastSessionId
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.remote_title), fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ZCodeButton(text = stringResource(R.string.remote_add_scan), onClick = onScan)
                ZCodeOutlinedButton(text = stringResource(R.string.remote_clipboard), onClick = onClipboard)
                ZCodeOutlinedButton(text = stringResource(R.string.remote_add_paste), onClick = onPaste)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (storageFailed) {
                item {
                    InfoBanner(
                        title = stringResource(R.string.remote_title),
                        body = stringResource(R.string.err_storage),
                    )
                }
            }
            if (RemoteEmptyState.showScanOrPaste(sessions.size)) {
                item {
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        Text(
                            text = stringResource(R.string.remote_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.remote_empty_body),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        ZCodeButton(
                            text = stringResource(R.string.remote_empty_cta),
                            onClick = onPaste,
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.remote_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(sessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        nowEpochMs = now,
                        badge = if (session.id == lastId) stringResource(R.string.session_last_badge) else null,
                        onOpen = { onOpenSession(session.id) },
                        onCopy = {
                            val text = SessionLinkCopy.textToCopy(session.url)
                            val copied = text != null && writeClipboard(context, text)
                            scope.launch { snackbar.showSnackbar(SessionLinkCopy.message(copied)) }
                        },
                        onRename = { pendingRename = session },
                        onDelete = { pendingDelete = session },
                    )
                }
            }
        }
    }

    val deleting = pendingDelete
    if (deleting != null) {
        val name = deleting.visibleName(stringResource(R.string.unnamed_device))
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.settings_delete_session_title)) },
            text = { Text(stringResource(R.string.settings_delete_session_body, name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDeleteSession(deleting.id)
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }

    val renaming = pendingRename
    if (renaming != null) {
        RenameSessionDialog(
            session = renaming,
            onDismiss = { pendingRename = null },
            onSave = { name ->
                pendingRename = null
                onRenameSession(renaming.id, name)
            },
        )
    }
}

@Composable
private fun rememberNow(): Long = androidx.compose.runtime.remember { System.currentTimeMillis() }
