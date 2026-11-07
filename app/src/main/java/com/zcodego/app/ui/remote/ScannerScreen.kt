package com.zcodego.app.ui.remote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zcodego.app.R
import com.zcodego.app.camera.QrAnalyzer
import com.zcodego.app.security.RejectReason
import com.zcodego.app.ui.components.ZCodeBackdrop
import com.zcodego.app.ui.components.ZCodeButton
import com.zcodego.app.ui.components.ZCodeOutlinedButton
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onAccepted: (String) -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    if (!granted) {
        PermissionGate(
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onBack = onBack,
        )
        return
    }

    var error by remember { mutableStateOf<RejectReason?>(null) }
    var accepted by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var cameraMissing by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val view = LocalView.current
    val onQr = rememberUpdatedState { text: String ->
        if (accepted) return@rememberUpdatedState
        when (val outcome = ScanOutcome.decide(text)) {
            is ScanOutcome.Accept -> {
                accepted = true
                runCatching {
                    view.performHapticFeedback(
                        ScanOutcome.feedbackConstant(),
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
                    )
                }
                onAccepted(outcome.url)
            }
            is ScanOutcome.Decline -> error = outcome.reason
        }
    }

    if (cameraMissing) {
        PermissionGate(
            message = stringResource(R.string.scanner_no_camera),
            onOpenSettings = null,
            onBack = onBack,
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(
            onCamera = { bound ->
                camera = bound
                hasFlash = bound.cameraInfo.hasFlashUnit()
            },
            onUnavailable = { cameraMissing = true },
            onQr = { onQr.value(it) },
        )
        ScanMask()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.cd_close),
                    tint = Color.White,
                )
            }
            if (hasFlash) {
                IconButton(onClick = { torch = !torch }) {
                    Icon(
                        imageVector = if (torch) Icons.Outlined.FlashOff else Icons.Outlined.FlashOn,
                        contentDescription = stringResource(
                            if (torch) R.string.cd_torch_off else R.string.cd_torch_on,
                        ),
                        tint = Color.White,
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val reason = error
            if (reason != null) {
                Text(
                    text = rejectMessage(reason),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
            Text(
                text = stringResource(R.string.scanner_hint),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.scanner_hint_sub),
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    androidx.compose.runtime.LaunchedEffect(torch, camera) {
        camera?.cameraControl?.enableTorch(torch && hasFlash)
    }
}

@Composable
private fun CameraPreview(
    onCamera: (Camera) -> Unit,
    onUnavailable: () -> Unit,
    onQr: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val latestOnQr = rememberUpdatedState(onQr)
    val latestOnCamera = rememberUpdatedState(onCamera)
    val latestUnavailable = rememberUpdatedState(onUnavailable)

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val provider = future.get()
                val selector = when {
                    runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false) ->
                        CameraSelector.DEFAULT_BACK_CAMERA
                    runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false) ->
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }
                if (selector == null) {
                    latestUnavailable.value()
                    return@Runnable
                }
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor, QrAnalyzer { text -> latestOnQr.value(text) })
                provider.unbindAll()
                val bound = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                latestOnCamera.value(bound)
            } catch (_: Exception) {
                latestUnavailable.value()
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            if (future.isDone) {
                runCatching { future.get().unbindAll() }
            }
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun ScanMask() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        drawRect(Color.Black.copy(alpha = 0.55f))
        val side = 260.dp.toPx()
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f - 24.dp.toPx()
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = CornerRadius(24.dp.toPx()),
            blendMode = BlendMode.Clear,
        )
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val side = 260.dp.toPx()
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f - 24.dp.toPx()
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = CornerRadius(24.dp.toPx()),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun PermissionGate(
    onBack: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    message: String = stringResource(R.string.scanner_need_permission),
) {
    ZCodeBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.widthIn(max = 420.dp),
            )
            Spacer(Modifier.height(20.dp))
            if (onOpenSettings != null) {
                ZCodeButton(
                    text = stringResource(R.string.scanner_open_settings),
                    onClick = onOpenSettings,
                    modifier = Modifier.widthIn(max = 420.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
            ZCodeOutlinedButton(
                text = stringResource(R.string.scanner_use_paste),
                onClick = onBack,
                modifier = Modifier.widthIn(max = 420.dp),
            )
        }
    }
}
