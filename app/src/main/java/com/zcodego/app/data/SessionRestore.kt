package com.zcodego.app.data

import com.zcodego.app.security.ParseResult
import com.zcodego.app.security.RemoteUrlPolicy

/**
 * Decides whether a cold start should reopen a saved remote page.
 * A deep link always wins: callers pass null when an incoming URL is waiting.
 */
object SessionRestore {
    fun reopenId(
        hostMode: HostMode?,
        reopenEnabled: Boolean,
        lastSessionId: String?,
        sessions: List<RemoteSession>,
    ): String? {
        if (hostMode != HostMode.REMOTE || !reopenEnabled) return null
        val id = lastSessionId?.takeIf { it.isNotBlank() } ?: return null
        val session = sessions.firstOrNull { it.id == id } ?: return null
        return if (RemoteUrlPolicy.parseRemoteLink(session.url) is ParseResult.Ok) id else null
    }

    fun isStale(lastSessionId: String?, sessions: List<RemoteSession>): Boolean {
        if (lastSessionId.isNullOrBlank()) return false
        val session = sessions.firstOrNull { it.id == lastSessionId } ?: return true
        return RemoteUrlPolicy.parseRemoteLink(session.url) !is ParseResult.Ok
    }
}
