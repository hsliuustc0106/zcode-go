package com.zcodego.app.remote

/**
 * Pulls a candidate string out of VIEW / SEND intents. Parsing and the allowlist
 * stay in [com.zcodego.app.security.RemoteUrlPolicy].
 */
object RemoteIngest {
    const val ACTION_VIEW = "android.intent.action.VIEW"
    const val ACTION_SEND = "android.intent.action.SEND"

    fun rawText(action: String?, dataString: String?, shareText: String?): String? {
        val raw = when (action) {
            ACTION_VIEW -> dataString
            ACTION_SEND -> shareText
            else -> null
        }
        return raw?.trim()?.ifBlank { null }
    }
}
