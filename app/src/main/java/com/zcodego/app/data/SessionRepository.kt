package com.zcodego.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.zcodego.app.security.AcceptedRemoteLink
import com.zcodego.app.security.ParseResult
import com.zcodego.app.security.RemoteUrlPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
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
                    sessionsState.value = SessionMerge.sortAndCap(decode(opened.getString(STORAGE_KEY, null)))
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
            store.edit().putString(STORAGE_KEY, encode(sessions)).commit()
        } catch (_: Exception) {
            false
        }
    }

    private fun encode(sessions: List<RemoteSession>): String {
        val array = JSONArray()
        sessions.forEach { session ->
            array.put(
                JSONObject()
                    .put("id", session.id)
                    .put("url", session.url)
                    .put("sid", session.sid)
                    .put("deviceName", session.deviceName)
                    .put("appVersion", session.appVersion ?: JSONObject.NULL)
                    .put("protocolLabel", session.protocolLabel)
                    .put("createdAtEpochMs", session.createdAtEpochMs)
                    .put("lastOpenedAtEpochMs", session.lastOpenedAtEpochMs),
            )
        }
        return JSONObject().put("sessions", array).toString()
    }

    private fun decode(raw: String?): List<RemoteSession> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONObject(raw).optJSONArray("sessions") ?: return emptyList()
            buildList {
                for (index in 0 until array.length()) {
                    val session = array.optJSONObject(index)?.toSession() ?: continue
                    if (RemoteUrlPolicy.parseRemoteLink(session.url) is ParseResult.Ok) {
                        add(session)
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun JSONObject.toSession(): RemoteSession? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val url = optString("url").takeIf { it.isNotBlank() } ?: return null
        val sid = optString("sid").takeIf { it.isNotBlank() } ?: return null
        val versionValue = opt("appVersion")
        val appVersion = if (versionValue == null || versionValue == JSONObject.NULL) {
            null
        } else {
            versionValue.toString().takeIf { it.isNotBlank() }
        }
        return RemoteSession(
            id = id,
            url = url,
            sid = sid,
            deviceName = optString("deviceName"),
            appVersion = appVersion,
            protocolLabel = optString("protocolLabel").ifBlank { "remote" },
            createdAtEpochMs = optLong("createdAtEpochMs"),
            lastOpenedAtEpochMs = optLong("lastOpenedAtEpochMs"),
        )
    }

    private companion object {
        const val PREFS_NAME = "remote_sessions"
        const val STORAGE_KEY = "sessions_json"
    }
}
