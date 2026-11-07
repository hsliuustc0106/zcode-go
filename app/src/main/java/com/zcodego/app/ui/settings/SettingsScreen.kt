package com.zcodego.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zcodego.app.BuildConfig
import com.zcodego.app.R
import com.zcodego.app.data.AppController
import com.zcodego.app.data.HostMode
import com.zcodego.app.data.RemoteSiteWipe
import com.zcodego.app.data.ThemeMode
import com.zcodego.app.ui.components.HostOptionCard
import com.zcodego.app.ui.components.ZCodeOutlinedButton
import com.zcodego.app.ui.web.WebDataCleaner
import com.zcodego.app.ui.web.WebOriginCleaner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    controller: AppController,
    onBack: () -> Unit,
    onHostModeChanged: (HostMode) -> Unit,
    onReplayOnboarding: () -> Unit,
    onOpenDocs: () -> Unit,
    onSessionsCleared: () -> Unit = {},
) {
    val settings by controller.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val license = remember {
        context.resources.openRawResource(R.raw.mit_license).bufferedReader().use { it.readText() }
    }
    var confirmSessions by remember { mutableStateOf(false) }
    var confirmWeb by remember { mutableStateOf(false) }
    var confirmRemoteSite by remember { mutableStateOf(false) }
    var confirmReplay by remember { mutableStateOf(false) }
    val clearedSessions = stringResource(R.string.settings_cleared_sessions)
    val clearedWeb = stringResource(R.string.settings_cleared_web)
    val githubUnavailable = stringResource(R.string.about_github_unavailable)
    val current = settings ?: return

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_section_host))
            HostOptionCard(
                title = stringResource(R.string.host_local_title),
                subtitle = stringResource(R.string.host_local_subtitle),
                icon = Icons.Outlined.Smartphone,
                selected = current.hostMode == HostMode.LOCAL,
                onClick = {
                    if (current.hostMode != HostMode.LOCAL) {
                        scope.launch {
                            controller.setHostMode(HostMode.LOCAL)
                            onHostModeChanged(HostMode.LOCAL)
                        }
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            HostOptionCard(
                title = stringResource(R.string.host_remote_title),
                subtitle = stringResource(R.string.host_remote_subtitle),
                icon = Icons.Outlined.Laptop,
                selected = current.hostMode == HostMode.REMOTE,
                onClick = {
                    if (current.hostMode != HostMode.REMOTE) {
                        scope.launch {
                            controller.setHostMode(HostMode.REMOTE)
                            onHostModeChanged(HostMode.REMOTE)
                        }
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { confirmReplay = true }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_replay_onboarding))
                    Text(
                        stringResource(R.string.settings_replay_onboarding_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionTitle(stringResource(R.string.settings_section_appearance))
            Text(
                text = stringResource(R.string.settings_theme_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                }
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = current.themeMode == mode,
                            role = Role.RadioButton,
                            onClick = { scope.launch { controller.setThemeMode(mode) } },
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = current.themeMode == mode, onClick = null)
                    Text(text = label, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_reopen),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_reopen_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = current.reopenLastSession,
                    onCheckedChange = { enabled ->
                        scope.launch { controller.setReopenLastSession(enabled) }
                    },
                )
            }

            Spacer(Modifier.height(28.dp))
            SectionTitle(stringResource(R.string.settings_section_data))
            TextButton(onClick = { confirmSessions = true }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_clear_sessions), color = MaterialTheme.colorScheme.error)
                    Text(
                        stringResource(R.string.settings_clear_sessions_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = { confirmWeb = true }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_clear_web), color = MaterialTheme.colorScheme.error)
                    Text(
                        stringResource(R.string.settings_clear_web_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = { confirmRemoteSite = true }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(RemoteSiteWipeCopy.ACTION, color = MaterialTheme.colorScheme.error)
                    Text(
                        RemoteSiteWipeCopy.DETAIL,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionTitle(stringResource(R.string.settings_section_about))
            Text(
                text = stringResource(
                    R.string.about_version,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.about_github_releases_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            ZCodeOutlinedButton(
                text = stringResource(R.string.about_open_github_releases),
                onClick = {
                    if (!GithubReleaseLink.open(context)) {
                        scope.launch { snackbar.showSnackbar(githubUnavailable) }
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            Text(text = stringResource(R.string.about_disclaimer), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(text = stringResource(R.string.about_execution), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(text = stringResource(R.string.about_security), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(text = stringResource(R.string.about_attention_limit), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(text = stringResource(R.string.about_license_summary), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onOpenDocs) {
                Text(stringResource(R.string.about_open_docs))
            }
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.about_license_heading),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = license,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmSessions) {
        AlertDialog(
            onDismissRequest = { confirmSessions = false },
            title = { Text(stringResource(R.string.settings_confirm_clear_sessions_title)) },
            text = { Text(stringResource(R.string.settings_confirm_clear_sessions_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSessions = false
                        scope.launch {
                            val plan = controller.clearSessions()
                            onSessionsCleared()
                            WebOriginCleaner.apply(plan)
                            snackbar.showSnackbar(clearedSessions)
                        }
                    },
                ) { Text(stringResource(R.string.settings_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSessions = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
    if (confirmWeb) {
        AlertDialog(
            onDismissRequest = { confirmWeb = false },
            title = { Text(stringResource(R.string.settings_confirm_clear_web_title)) },
            text = { Text(stringResource(R.string.settings_confirm_clear_web_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmWeb = false
                        WebDataCleaner.clear {
                            scope.launch { snackbar.showSnackbar(clearedWeb) }
                        }
                    },
                ) { Text(stringResource(R.string.settings_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmWeb = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
    if (confirmRemoteSite) {
        AlertDialog(
            onDismissRequest = { confirmRemoteSite = false },
            title = { Text(RemoteSiteWipeCopy.CONFIRM_TITLE) },
            text = { Text(RemoteSiteWipeCopy.CONFIRM_BODY) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemoteSite = false
                        val plan = RemoteSiteWipe.plan(controller.sessions.value.map { it.url })
                        WebOriginCleaner.apply(plan)
                        WebDataCleaner.clear {
                            scope.launch { snackbar.showSnackbar(RemoteSiteWipeCopy.DONE) }
                        }
                    },
                ) { Text(stringResource(R.string.settings_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoteSite = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
    if (confirmReplay) {
        AlertDialog(
            onDismissRequest = { confirmReplay = false },
            title = { Text(stringResource(R.string.settings_replay_onboarding_title)) },
            text = { Text(stringResource(R.string.settings_replay_onboarding_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReplay = false
                        scope.launch {
                            controller.clearHostMode()
                            onReplayOnboarding()
                        }
                    },
                ) { Text(stringResource(R.string.settings_replay_onboarding_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReplay = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}
