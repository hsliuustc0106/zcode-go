package com.zcodego.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.zcodego.app.security.AcceptedRemoteLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Session links are credentials. They live in EncryptedSharedPreferences, which
 * wraps a Keystore key, inside app-private storage. Backup is disabled separately.
 * This repository never writes the URL to logcat.
 */
class SessionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val sessionsState = MutableStateFlow<List<RemoteSession>>(emptyList())
    val sessions: StateFlow<List<RemoteSession>> = sessionsState.asStateFlow()
    private val readyState = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = readyState.asStateFlow()
    private val initErrorState = MutableStateFlow(false)
    val initError: StateFlow<Boolean> = initErrorState.asStateFlow()
    private var prefs: SharedPreferences? = null

    init {
        scope.launch {
            mutex.withLock {
                try {
                    val opened = openPrefs()
                    prefs = opened
                    sessionsState.value = SessionMerge.sortAndCap(SessionCodec.decode(opened.getString(STORAGE_KEY, null)))
                } catch (_: Throwable) {
                    initErrorState.value = true
                    prefs = null
                } finally {
                    readyState.value = true
                }
            }
        }
    }

    fun snapshot(): List<RemoteSession> = sessionsState.value

    suspend fun upsert(link: AcceptedRemoteLink, nowEpochMs: Long = System.currentTimeMillis()): RemoteSession? {
        return mutex.withLock {
            val store = prefs ?: return@withLock null
            val updated = SessionMerge.upsert(sessionsState.value, link, nowEpochMs) {
                UUID.randomUUID().toString()
            }
            if (!write(store, updated)) return@withLock null
            sessionsState.value = updated
            updated.firstOrNull { it.sid == link.sid }
        }
    }

    suspend fun touch(id: String, nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        return mutex.withLock {
            val store = prefs ?: return@withLock false
            if (sessionsState.value.none { it.id == id }) return@withLock false
            val updated = SessionMerge.touch(sessionsState.value, id, nowEpochMs)
            if (!write(store, updated)) return@withLock false
            sessionsState.value = updated
            true
        }
    }

    suspend fun delete(id: String): Boolean {
        return mutex.withLock {
            val store = prefs ?: return@withLock false
            val updated = SessionMerge.delete(sessionsState.value, id)
            if (!write(store, updated)) return@withLock false
            sessionsState.value = updated
            true
        }
    }

    /** Local label only. A blank name clears the custom label. URL and sid stay put. */
    suspend fun updateDisplayName(id: String, rawName: String?): Boolean {
        return mutex.withLock {
            val store = prefs ?: return@withLock false
            val updated = SessionMerge.updateDisplayName(sessionsState.value, id, rawName) ?: return@withLock false
            if (!write(store, updated)) return@withLock false
            sessionsState.value = updated
            true
        }
    }

    suspend fun rename(id: String, rawName: String?): Boolean = updateDisplayName(id, rawName)

    suspend fun clear(): Boolean {
        return mutex.withLock {
            val store = prefs ?: return@withLock false
            if (!write(store, emptyList())) return@withLock false
            sessionsState.value = emptyList()
            true
        }
    }

    private fun openPrefs(): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            appContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun write(store: SharedPreferences, sessions: List<RemoteSession>): Boolean {
        return try {
            store.edit().putString(STORAGE_KEY, SessionCodec.encode(sessions)).commit()
        } catch (_: Exception) {
            false
        }
    }

    private companion object {
        const val PREFS_NAME = "remote_sessions"
        const val STORAGE_KEY = "sessions_json"
    }
}
