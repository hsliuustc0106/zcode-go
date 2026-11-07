package com.zcodego.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.zcodego.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6BFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF0B2F86),
    secondary = Color(0xFF525F78),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6EAF3),
    onSecondaryContainer = Color(0xFF1C2433),
    background = Color(0xFFF6F3FA),
    onBackground = Color(0xFF1A1D26),
    surface = Color.White,
    onSurface = Color(0xFF1A1D26),
    surfaceVariant = Color(0xFFE7EAF2),
    onSurfaceVariant = Color(0xFF5C6574),
    outline = Color(0xFFD5DAE5),
    outlineVariant = Color(0xFFE4E8F0),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3D78FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF17356E),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFFB7C3DA),
    onSecondary = Color(0xFF243044),
    secondaryContainer = Color(0xFF2A3344),
    onSecondaryContainer = Color(0xFFDCE3F0),
    background = Color(0xFF14141C),
    onBackground = Color(0xFFE7EAF1),
    surface = Color(0xFF1A2030),
    onSurface = Color(0xFFE7EAF1),
    surfaceVariant = Color(0xFF2A3346),
    onSurfaceVariant = Color(0xFFC2C9D6),
    outline = Color(0xFF3C465C),
    outlineVariant = Color(0xFF2C3548),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

internal val LocalZCodeExtra = staticCompositionLocalOf { LightExtra }

@Composable
fun ZCodeGoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val extra = if (dark) DarkExtra else LightExtra
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalZCodeExtra provides extra) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = Typography,
            content = content,
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
