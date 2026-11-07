package com.zcodego.app.ui.web

import com.zcodego.app.security.ParseResult
import com.zcodego.app.security.RemoteUrlPolicy

/**
 * Copies one saved remote HTTPS link to the clipboard. It does not share or send it.
 * The confirmation text does not include the URL.
 */
internal object SessionLinkCopy {
    const val ACTION = "复制当前链接"
    const val COPIED = "已复制当前链接"
    const val REJECTED = "这条链接不能复制"

    fun textToCopy(url: String?): String? {
        val raw = url?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val parsed = RemoteUrlPolicy.parseRemoteLink(raw)
        return if (parsed is ParseResult.Ok) parsed.link.url else null
    }

    fun message(copied: Boolean): String = if (copied) COPIED else REJECTED

    fun messageHidesUrl(message: String): Boolean =
        !message.contains("http://", ignoreCase = true) && !message.contains("https://", ignoreCase = true)
}
