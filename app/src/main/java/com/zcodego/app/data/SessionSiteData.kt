package com.zcodego.app.data

import java.net.URI

/**
 * WebView cookies and local storage are per site, not per saved link.
 * Clearing is skipped while another saved session still uses the same origin.
 */
object SessionSiteData {
    data class Plan(val origins: List<String>) {
        override fun toString(): String = "Plan(origins=${origins.size})"
    }

    fun planForRemoved(removedUrls: List<String>, remainingUrls: List<String>): Plan {
        val remaining = remainingUrls.mapNotNull(::originOf).toSet()
        val origins = removedUrls.mapNotNull(::originOf).filter { it !in remaining }.distinct()
        return Plan(origins)
    }

    fun originOf(url: String): String? {
        val uri = try {
            URI(url.trim())
        } catch (_: Exception) {
            return null
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (!uri.rawUserInfo.isNullOrEmpty() || !uri.userInfo.isNullOrEmpty()) return null
        val host = uri.host?.trim()?.trimEnd('.')?.takeIf { it.isNotEmpty() } ?: return null
        val port = uri.port
        return if (port == -1 || port == 443) {
            "https://$host"
        } else {
            "https://$host:$port"
        }
    }

    fun cookieNames(header: String?): List<String> {
        if (header.isNullOrBlank()) return emptyList()
        return header.split(';').mapNotNull { part ->
            val name = part.substringBefore('=').trim()
            if (name.isEmpty() || name.any { it.isISOControl() || it == ' ' }) null else name
        }.distinct()
    }
}
