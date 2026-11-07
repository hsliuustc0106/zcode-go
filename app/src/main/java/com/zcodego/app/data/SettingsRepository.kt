package com.zcodego.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "zcode_go_settings")

class SettingsRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings: StateFlow<AppSettings?> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map<Preferences, AppSettings?> { prefs -> prefs.toAppSettings() }
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun current(): AppSettings = settings.filterNotNull().first()

    suspend fun setHostMode(mode: HostMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.HOST_MODE] = mode.storageValue
        }
    }

    /** Removes the host-mode choice only. Theme, reopen, and last session stay. */
    suspend fun clearHostMode() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(Keys.HOST_MODE)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.storageValue
        }
    }

    suspend fun setReopenLastSession(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.REOPEN_LAST] = enabled
        }
    }

    suspend fun setSecureRemoteScreen(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SECURE_REMOTE_SCREEN] = enabled
        }
    }

    suspend fun setLastSessionId(id: String?) {
        context.settingsDataStore.edit { prefs ->
            if (id.isNullOrBlank()) {
                prefs.remove(Keys.LAST_SESSION_ID)
            } else {
                prefs[Keys.LAST_SESSION_ID] = id
            }
        }
    }

    private fun Preferences.toAppSettings(): AppSettings =
        AppSettings(
            hostMode = HostMode.fromStorage(this[Keys.HOST_MODE]),
            reopenLastSession = this[Keys.REOPEN_LAST] ?: true,
            themeMode = ThemeMode.fromStorage(this[Keys.THEME_MODE]),
            lastSessionId = this[Keys.LAST_SESSION_ID],
            secureRemoteScreen = this[Keys.SECURE_REMOTE_SCREEN] ?: false,
        )

    private object Keys {
        val HOST_MODE = stringPreferencesKey("host_mode")
        val REOPEN_LAST = booleanPreferencesKey("reopen_last")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_SESSION_ID = stringPreferencesKey("last_session_id")
        val SECURE_REMOTE_SCREEN = booleanPreferencesKey("secure_remote_screen")
    }
}
