package com.zcodego.app.data

import com.zcodego.app.security.AcceptedRemoteLink

internal object SessionMerge {
    const val MAX_SESSIONS = 30

    fun upsert(
        existing: List<RemoteSession>,
        link: AcceptedRemoteLink,
        nowEpochMs: Long,
        newId: () -> String,
    ): List<RemoteSession> {
        val previous = existing.firstOrNull { it.sid == link.sid }
        val session = if (previous == null) {
            RemoteSession(
                id = newId(),
                url = link.url,
                sid = link.sid,
                deviceName = link.deviceName,
                appVersion = link.appVersion,
                protocolLabel = link.protocolLabel,
                createdAtEpochMs = nowEpochMs,
                lastOpenedAtEpochMs = nowEpochMs,
            )
        } else {
            previous.copy(
                url = link.url,
                deviceName = link.deviceName,
                appVersion = link.appVersion,
                protocolLabel = link.protocolLabel,
                lastOpenedAtEpochMs = nowEpochMs,
            )
        }
        val without = existing.filterNot { it.sid == link.sid }
        return sortAndCap(without + session)
    }

    fun touch(existing: List<RemoteSession>, id: String, nowEpochMs: Long): List<RemoteSession> {
        if (existing.none { it.id == id }) return existing
        return sortAndCap(
            existing.map { session ->
                if (session.id == id) session.copy(lastOpenedAtEpochMs = nowEpochMs) else session
            },
        )
    }

    fun delete(existing: List<RemoteSession>, id: String): List<RemoteSession> =
        existing.filterNot { it.id == id }

    fun sortAndCap(list: List<RemoteSession>, max: Int = MAX_SESSIONS): List<RemoteSession> =
        list.sortedByDescending { it.lastOpenedAtEpochMs }.take(max)
}
