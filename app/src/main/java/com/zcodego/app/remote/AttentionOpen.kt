package com.zcodego.app.remote

/**
 * Tap target for the background title notification.
 * The extra is a saved session id. The notification text is never a remote URL.
 */
object AttentionOpen {
    const val EXTRA_SESSION_ID = "attention_session_id"

    fun sessionId(raw: String?): String? {
        val id = raw?.trim().orEmpty()
        if (id.isEmpty() || id.length > 80) return null
        if (id.any { it.isISOControl() || it.isWhitespace() || it == '/' || it == '?' || it == '#' || it == '\\' }) {
            return null
        }
        if (id.contains("http", ignoreCase = true)) return null
        return id
    }

    /** Session to open, or null when the id is missing or no longer saved. */
    fun destination(requested: String?, knownIds: Collection<String>): String? {
        val id = sessionId(requested) ?: return null
        return id.takeIf { knownIds.any { known -> known == id } }
    }

    fun notificationText(notableTitle: String?): String? = PageAttention.sanitize(notableTitle)

    fun tap(sessionId: String?, notableTitle: String): Tap? {
        val text = notificationText(notableTitle) ?: return null
        return Tap(sessionId = sessionId(sessionId), text = text)
    }

    data class Tap(val sessionId: String?, val text: String)
}
