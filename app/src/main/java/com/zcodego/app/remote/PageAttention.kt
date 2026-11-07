package com.zcodego.app.remote

/**
 * Best-effort attention signal from the official page's document title.
 * The remote page does not call back into this app, so a title change while
 * zcode-go is backgrounded is only a hint, not proof that an agent is waiting.
 */
object PageAttention {
    fun notableTitle(previous: String?, next: String?, appInForeground: Boolean): String? {
        if (appInForeground) return null
        val sanitized = sanitize(next) ?: return null
        if (sanitized == previous) return null
        return sanitized
    }

    fun sanitize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace(Regex("\\p{Cntrl}"), " ").trim().replace(Regex("\\s+"), " ").take(80)
        if (cleaned.isBlank()) return null
        if (cleaned.contains("http://", ignoreCase = true) || cleaned.contains("https://", ignoreCase = true)) {
            return null
        }
        return cleaned
    }
}
