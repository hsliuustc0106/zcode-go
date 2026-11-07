package com.zcodego.app.ui.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.zcodego.app.ui.remote.RenameSessionDialog
import com.zcodego.app.ui.remote.SessionRow
import com.zcodego.app.ui.web.OfficialDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalHomeScreen(
    controller: AppController,
    onSettings: () -> Unit,
    onOpenOfficial: (OfficialDestination) -> Unit,
    onOpenSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onSwitchToRemote: () -> Unit,
) {
    val sessions by controller.sessions.collectAsStateWithLifecycle()
    val storageFailed by controller.storageFailed.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<RemoteSession?>(null) }
    var pendingRename by remember { mutableStateOf<RemoteSession?>(null) }
    val now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.local_title), fontWeight = FontWeight.SemiBold) },
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
            ZCodeButton(
                text = stringResource(R.string.local_switch_remote),
                onClick = onSwitchToRemote,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                InfoBanner(
                    title = stringResource(R.string.local_banner_title),
                    body = stringResource(R.string.local_banner_body),
                )
            }
            if (storageFailed) {
                item {
                    Text(
                        text = stringResource(R.string.err_storage),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                ZCodeOutlinedButton(
                    text = stringResource(R.string.local_open_zcode),
                    onClick = { onOpenOfficial(OfficialDestination.ZCODE) },
                )
            }
            item {
                ZCodeOutlinedButton(
                    text = stringResource(R.string.local_open_bigmodel),
                    onClick = { onOpenOfficial(OfficialDestination.BIGMODEL) },
                )
            }
            item {
                Text(
                    text = stringResource(R.string.local_recent),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (sessions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.local_recent_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        nowEpochMs = now,
                        onOpen = { onOpenSession(session.id) },
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
                ) { Text(stringResource(R.string.action_delete)) }
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
