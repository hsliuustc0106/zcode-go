package com.zcodego.app.ui.web

import com.zcodego.app.data.RemoteSession

/** Top bar and the current-row marker share one label. A blank custom name falls back. */
internal object SessionSwitchLabel {
    fun title(session: RemoteSession, unnamed: String): String = session.visibleName(unnamed)

    fun currentMarker(title: String, currentWord: String): String {
        val label = title.trim()
        return if (label.isEmpty()) currentWord else "$currentWord · $label"
    }
}
