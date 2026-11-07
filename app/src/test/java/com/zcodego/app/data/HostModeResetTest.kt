package com.zcodego.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostModeResetTest {
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
    fun clearingHostModeShowsThePickerAndKeepsTheRest() {
        val before = AppSettings(
            hostMode = HostMode.REMOTE,
            reopenLastSession = false,
            themeMode = ThemeMode.DARK,
            lastSessionId = "id-1",
        )
        val sessions = listOf(session)
        val after = HostModeReset.withoutChoice(before)

        assertTrue(HostModeReset.showsPicker(after))
        assertNull(after.hostMode)
        assertFalse(after.reopenLastSession)
        assertEquals(ThemeMode.DARK, after.themeMode)
        assertEquals("id-1", after.lastSessionId)
        assertEquals(1, sessions.size)
        assertEquals("id-1", sessions.single().id)
        assertNull(
            SessionRestore.reopenId(
                after.hostMode,
                after.reopenLastSession,
                after.lastSessionId,
                sessions,
            ),
        )
        assertFalse(after.toString().contains("https://"))
    }
}
