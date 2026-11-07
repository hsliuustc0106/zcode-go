package com.zcodego.app.remote

/**
 * Foreground keep-alive label. Custom name wins, then the name from the link,
 * then the caller's fallback. A value that contains an http(s) URL is skipped.
 */
object KeepAliveLabel {
    fun choose(custom: String?, linkName: String?, fallback: String): String {
        return safe(custom) ?: safe(linkName) ?: safe(fallback) ?: fallback.trim()
    }

    fun safe(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace(Regex("\\p{Cntrl}"), " ").trim().replace(Regex("\\s+"), " ")
        if (cleaned.isBlank()) return null
        if (cleaned.contains("http://", ignoreCase = true) || cleaned.contains("https://", ignoreCase = true)) {
            return null
        }
        return cleaned.take(80)
    }
}
