package com.zcodego.app.data

import com.zcodego.app.security.AcceptedRemoteLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionDisplayNameTest {
    @Test
    fun renamePersistsWithoutChangingUrlAndBlankNameFallsBack() {
        val original = session(id = "1", sid = "abc", deviceName = "QR-PC")
        val renamed = SessionMerge.updateDisplayName(listOf(original), "1", "  家里的电脑  ")
        assertEquals("家里的电脑", renamed!![0].customDisplayName)
        assertEquals("https://zcode.z.ai/remote/v4?sid=abc", renamed[0].url)
        assertEquals("abc", renamed[0].sid)
        assertEquals("家里的电脑", renamed[0].visibleName("未命名设备"))

        val stored = SessionCodec.decode(SessionCodec.encode(renamed))
        assertEquals(1, stored.size)
        assertEquals("家里的电脑", stored[0].customDisplayName)
        assertEquals("https://zcode.z.ai/remote/v4?sid=abc", stored[0].url)
        assertFalse(stored[0].toString().contains("https://"))
        assertFalse(stored[0].toString().contains("家里的电脑"))

        val cleared = SessionMerge.updateDisplayName(stored, "1", "   ")
        assertNull(cleared!![0].customDisplayName)
        assertEquals("QR-PC", cleared[0].visibleName("未命名设备"))
        val unnamed = cleared[0].copy(deviceName = "")
        assertEquals("未命名设备", unnamed.visibleName("未命名设备"))
    }

    @Test
    fun deleteRemovesTheRenamedSessionOnly() {
        val named = session(id = "1", sid = "a", deviceName = "A").copy(customDisplayName = "客厅")
        val other = session(id = "2", sid = "b", deviceName = "B")
        val left = SessionMerge.delete(listOf(named, other), "1")
        assertEquals(listOf("2"), left.map { it.id })
        assertNull(SessionMerge.updateDisplayName(left, "1", "不再存在"))
    }

    @Test
    fun refreshingTheSameLinkKeepsTheCustomName() {
        val saved = SessionMerge.upsert(
            emptyList(),
            link("abc", "https://zcode.z.ai/remote/v4?sid=abc", "QR"),
            1L,
        ) { "1" }
        val renamed = SessionMerge.updateDisplayName(saved, "1", "办公室")!!
        val refreshed = SessionMerge.upsert(
            renamed,
            link("abc", "https://zcode.z.ai/remote/v4?sid=abc&app_version=9", "NEW-QR"),
            2L,
        ) { "should-not-use" }
        assertEquals("1", refreshed[0].id)
        assertEquals("办公室", refreshed[0].customDisplayName)
        assertEquals("NEW-QR", refreshed[0].deviceName)
        assertEquals("abc", refreshed[0].sid)
        assertTrue(refreshed[0].url.contains("app_version=9"))
    }

    private fun session(id: String, sid: String, deviceName: String) = RemoteSession(
        id = id,
        url = "https://zcode.z.ai/remote/v4?sid=$sid",
        sid = sid,
        deviceName = deviceName,
        appVersion = null,
        protocolLabel = "v4",
        createdAtEpochMs = 1L,
        lastOpenedAtEpochMs = 1L,
    )

    private fun link(sid: String, url: String, name: String) = AcceptedRemoteLink(
        url = url,
        sid = sid,
        deviceName = name,
        appVersion = null,
        protocolLabel = "v4",
    )
}
