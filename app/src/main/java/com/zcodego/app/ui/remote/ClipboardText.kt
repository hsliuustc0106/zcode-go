package com.zcodego.app.ui.remote

import android.content.ClipboardManager
import android.content.Context

internal fun readClipboard(context: Context): String {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return ""
    val clip = clipboard.primaryClip ?: return ""
    if (clip.itemCount == 0) return ""
    return clip.getItemAt(0).coerceToText(context)?.toString().orEmpty()
}
