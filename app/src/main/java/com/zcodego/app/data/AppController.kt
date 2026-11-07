package com.zcodego.app.data

import com.zcodego.app.security.AcceptedRemoteLink
import kotlinx.coroutines.flow.StateFlow

class AppController(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    val linkInbox: LinkInbox,
) {
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
    val sessions: StateFlow<List<RemoteSession>> = sessionRepository.sessions
    val sessionsReady: StateFlow<Boolean> = sessionRepository.ready
    val storageFailed: StateFlow<Boolean> = sessionRepository.initError

    fun find(id: String): RemoteSession? = sessionRepository.snapshot().firstOrNull { it.id == id }

    suspend fun setHostMode(mode: HostMode) = settingsRepository.setHostMode(mode)

    suspend fun setThemeMode(mode: ThemeMode) = settingsRepository.setThemeMode(mode)

    suspend fun setReopenLastSession(enabled: Boolean) = settingsRepository.setReopenLastSession(enabled)

    suspend fun saveLink(link: AcceptedRemoteLink): RemoteSession? {
        val session = sessionRepository.upsert(link) ?: return null
        settingsRepository.setLastSessionId(session.id)
        return session
    }

    suspend fun markOpened(id: String) {
        if (sessionRepository.touch(id)) {
            settingsRepository.setLastSessionId(id)
        }
    }

    suspend fun deleteSession(id: String) {
        sessionRepository.delete(id)
        if (settingsRepository.current().lastSessionId == id) {
            settingsRepository.setLastSessionId(null)
        }
    }

    suspend fun clearSessions() {
        sessionRepository.clear()
        settingsRepository.setLastSessionId(null)
    }

    suspend fun updateDisplayName(id: String, rawName: String?): Boolean =
        sessionRepository.updateDisplayName(id, rawName)

    suspend fun forgetStaleLastSession() {
        val id = settingsRepository.current().lastSessionId
        if (!SessionRestore.isStale(id, sessionRepository.snapshot())) return
        if (settingsRepository.current().lastSessionId == id) {
            settingsRepository.setLastSessionId(null)
        }
    }
}
