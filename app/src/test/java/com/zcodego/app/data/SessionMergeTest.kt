package com.zcodego.app.data

import com.zcodego.app.security.AcceptedRemoteLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionMergeTest {
    @Test
    fun newSidCreatesSessionAndSameSidUpdatesUrl() {
        val first = link(sid = "s1", url = "https://zcode.z.ai/remote/v3?sid=s1", name = "Home")
        var sessions = SessionMerge.upsert(emptyList(), first, nowEpochMs = 10L) { "id-1" }
        assertEquals(1, sessions.size)
        assertEquals("id-1", sessions[0].id)
        assertEquals(10L, sessions[0].createdAtEpochMs)

        val refreshed = link(sid = "s1", url = "https://zcode.z.ai/remote/v4?sid=s1", name = "Office")
        sessions = SessionMerge.upsert(sessions, refreshed, nowEpochMs = 20L) { "id-2" }
        assertEquals(1, sessions.size)
        assertEquals("id-1", sessions[0].id)
        assertEquals(10L, sessions[0].createdAtEpochMs)
        assertEquals(20L, sessions[0].lastOpenedAtEpochMs)
        assertEquals("https://zcode.z.ai/remote/v4?sid=s1", sessions[0].url)
        assertEquals("Office", sessions[0].deviceName)
        assertFalse(sessions[0].toString().contains("https://"))
        assertFalse(sessions[0].toString().contains("s1"))
    }

    @Test
    fun differentSidsStaySeparateAndNewestIsFirst() {
        val older = SessionMerge.upsert(
            emptyList(),
            link("a", "https://zcode.z.ai/remote/v4?sid=a", "A"),
            10L,
        ) { "1" }
        val both = SessionMerge.upsert(
            older,
            link("b", "https://zcode.z.ai/remote/v4?sid=b", "B"),
            30L,
        ) { "2" }
        assertEquals(listOf("2", "1"), both.map { it.id })
    }

    @Test
    fun touchUpdatesOrderAndDeleteRemovesOnlyThatId() {
        val sessions = listOf(
            session("1", "a", 10L),
            session("2", "b", 20L),
        )
        val touched = SessionMerge.touch(sessions, "1", 40L)
        assertEquals("1", touched.first().id)
        assertEquals(40L, touched.first().lastOpenedAtEpochMs)
        val deleted = SessionMerge.delete(touched, "2")
        assertEquals(listOf("1"), deleted.map { it.id })
        assertEquals(sessions, SessionMerge.touch(sessions, "missing", 50L))
    }

    @Test
    fun capDropsTheLeastRecentlyOpened() {
        val sessions = (1..32).map { index ->
            session(id = index.toString(), sid = "s$index", opened = index.toLong())
        }
        val capped = SessionMerge.sortAndCap(sessions)
        assertEquals(30, capped.size)
        assertTrue(capped.none { it.id == "1" || it.id == "2" })
        assertEquals("32", capped.first().id)
    }

    private fun link(sid: String, url: String, name: String) = AcceptedRemoteLink(
        url = url,
        sid = sid,
        deviceName = name,
        appVersion = "1.0.0",
        protocolLabel = "v4",
    )

    private fun session(id: String, sid: String, opened: Long) = RemoteSession(
        id = id,
        url = "https://zcode.z.ai/remote/v4?sid=$sid",
        sid = sid,
        deviceName = "PC",
        appVersion = null,
        protocolLabel = "v4",
        createdAtEpochMs = opened,
        lastOpenedAtEpochMs = opened,
    )
}
