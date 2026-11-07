package com.zcodego.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRestoreTest {
    private val session = RemoteSession(
        id = "id-1",
        url = "https://zcode.z.ai/remote/v4?sid=abc",
        sid = "abc",
        deviceName = "Office",
        appVersion = null,
        protocolLabel = "v4",
        createdAtEpochMs = 1L,
        lastOpenedAtEpochMs = 2L,
    )

    @Test
    fun reopensOnlyAValidRemoteSession() {
        assertEquals(
            "id-1",
            SessionRestore.reopenId(HostMode.REMOTE, true, "id-1", listOf(session)),
        )
        assertNull(SessionRestore.reopenId(HostMode.LOCAL, true, "id-1", listOf(session)))
        assertNull(SessionRestore.reopenId(HostMode.REMOTE, false, "id-1", listOf(session)))
        assertNull(SessionRestore.reopenId(HostMode.REMOTE, true, "missing", listOf(session)))
        assertNull(SessionRestore.reopenId(null, true, "id-1", listOf(session)))
    }

    @Test
    fun rejectsAStoredUrlThatIsNoLongerARemoteLink() {
        val broken = session.copy(url = "https://example.com/remote/v4?sid=abc")
        assertNull(SessionRestore.reopenId(HostMode.REMOTE, true, "id-1", listOf(broken)))
        assertTrue(SessionRestore.isStale("id-1", listOf(broken)))
        assertTrue(SessionRestore.isStale("gone", listOf(session)))
        assertFalse(SessionRestore.isStale("id-1", listOf(session)))
        assertFalse(SessionRestore.isStale(null, listOf(session)))
    }
}
