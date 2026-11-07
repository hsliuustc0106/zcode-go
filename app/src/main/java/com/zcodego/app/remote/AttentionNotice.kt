package com.zcodego.app.remote

/**
 * Background title notice. The fixed title and the sanitized page title stay.
 * The computer name is added so the user can tell which session fired it.
 * Neither line includes an http(s) URL.
 */
object AttentionNotice {
    data class Copy(val title: String, val body: String) {
        override fun toString(): String = "Copy(titleChars=${title.length}, bodyChars=${body.length})"
    }

    fun copy(
        fixedTitle: String,
        pageTitle: String,
        custom: String?,
        linkName: String?,
        fallback: String,
    ): Copy? {
        val title = PageAttention.sanitize(fixedTitle) ?: return null
        val page = PageAttention.sanitize(pageTitle) ?: return null
        val computer = KeepAliveLabel.choose(custom, linkName, fallback)
        if (hasUrl(title) || hasUrl(page) || hasUrl(computer)) return null
        val body = "$computer · $page"
        if (hasUrl(body)) return null
        return Copy(title, body)
    }

    private fun hasUrl(value: String): Boolean =
        value.contains("http://", ignoreCase = true) || value.contains("https://", ignoreCase = true)
}
