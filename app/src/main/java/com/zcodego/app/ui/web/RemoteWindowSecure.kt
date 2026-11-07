package com.zcodego.app.ui.web

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * FLAG_SECURE for the saved remote session only. Other screens never set it.
 * The decision does not take or log a remote URL.
 */
internal object RemoteWindowSecure {
    const val FLAG = WindowManager.LayoutParams.FLAG_SECURE

    fun shouldSecure(remoteSession: Boolean, settingOn: Boolean): Boolean =
        remoteSession && settingOn

    override fun toString(): String = "RemoteWindowSecure"
}

@Composable
internal fun RemoteSecureFlag() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        window?.addFlags(RemoteWindowSecure.FLAG)
        onDispose {
            window?.clearFlags(RemoteWindowSecure.FLAG)
        }
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
