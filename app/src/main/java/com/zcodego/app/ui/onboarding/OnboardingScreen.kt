package com.zcodego.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.zcodego.app.R
import com.zcodego.app.data.HostMode
import com.zcodego.app.data.ThemeMode
import com.zcodego.app.ui.components.HostOptionCard
import com.zcodego.app.ui.components.ProgressDots
import com.zcodego.app.ui.components.ZCodeBackdrop
import com.zcodego.app.ui.components.ZCodeButton
import com.zcodego.app.ui.theme.ZCodeGoTheme

@Composable
fun OnboardingScreen(
    onConfirm: (HostMode) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var selectedName by rememberSaveable { mutableStateOf(HostMode.REMOTE.name) }
    val selected = HostMode.valueOf(selectedName)

    BackHandler(enabled = step > 0) { step = 0 }

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
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (step == 1) {
                        TextButton(
                            onClick = { step = 0 },
                            modifier = Modifier.align(Alignment.Start),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_back_step))
                        }
                    } else {
                        Spacer(Modifier.height(12.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    if (step == 0) {
                        Header(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Devices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp),
                                )
                            },
                            title = stringResource(R.string.host_picker_title),
                            subtitle = stringResource(R.string.host_picker_subtitle),
                        )
                        Spacer(Modifier.height(28.dp))
                        HostOptionCard(
                            title = stringResource(R.string.host_local_title),
                            subtitle = stringResource(R.string.host_local_subtitle),
                            icon = Icons.Outlined.Smartphone,
                            selected = selected == HostMode.LOCAL,
                            onClick = { selectedName = HostMode.LOCAL.name },
                        )
                        Spacer(Modifier.height(12.dp))
                        HostOptionCard(
                            title = stringResource(R.string.host_remote_title),
                            subtitle = stringResource(R.string.host_remote_subtitle),
                            icon = Icons.Outlined.Laptop,
                            selected = selected == HostMode.REMOTE,
                            onClick = { selectedName = HostMode.REMOTE.name },
                        )
                    } else if (selected == HostMode.REMOTE) {
                        Header(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Laptop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp),
                                )
                            },
                            title = stringResource(R.string.step_remote_title),
                            subtitle = stringResource(R.string.step_remote_subtitle),
                        )
                        Spacer(Modifier.height(8.dp))
                        Bullet(stringResource(R.string.step_remote_b1))
                        Bullet(stringResource(R.string.step_remote_b2))
                        Bullet(stringResource(R.string.step_remote_b3))
                    } else {
                        Header(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Smartphone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp),
                                )
                            },
                            title = stringResource(R.string.step_local_title),
                            subtitle = stringResource(R.string.step_local_subtitle),
                        )
                        Spacer(Modifier.height(8.dp))
                        Bullet(stringResource(R.string.step_local_b1))
                        Bullet(stringResource(R.string.step_local_b2))
                        Bullet(stringResource(R.string.step_local_b3))
                    }
                    Spacer(Modifier.height(24.dp))
                }
                val buttonLabel = when {
                    step == 0 -> stringResource(R.string.action_continue)
                    selected == HostMode.REMOTE -> stringResource(R.string.action_go_connect)
                    else -> stringResource(R.string.action_enter_home)
                }
                ZCodeButton(
                    text = buttonLabel,
                    onClick = {
                        if (step == 0) {
                            step = 1
                        } else {
                            onConfirm(selected)
                        }
                    },
                )
                Spacer(Modifier.height(14.dp))
                ProgressDots(count = 2, selected = step)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Header(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
    Spacer(Modifier.height(20.dp))
    Text(
        text = title,
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
        text = subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Bullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Host picker")
@Composable
private fun HostPickerPreview() {
    ZCodeGoTheme(themeMode = ThemeMode.LIGHT) {
        OnboardingScreen(onConfirm = {})
    }
}
