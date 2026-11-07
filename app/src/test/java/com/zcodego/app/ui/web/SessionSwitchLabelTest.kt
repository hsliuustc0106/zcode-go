package com.zcodego.app.ui.web

import com.zcodego.app.data.RemoteSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SessionSwitchLabelTest {
    @Test
    fun topBarAndCurrentMarkerUseTheCustomName() {
        val session = sample(custom = "家里的电脑", linkName = "QR-PC")
        val title = SessionSwitchLabel.title(session, "未命名设备")
        assertEquals("家里的电脑", title)
        assertEquals("当前 · 家里的电脑", SessionSwitchLabel.currentMarker(title, "当前"))
        assertFalse(session.toString().contains("https://"))
    }

    @Test
    fun blankCustomNameFallsBackToTheLinkName() {
        val session = sample(custom = "   ", linkName = "Office")
        val title = SessionSwitchLabel.title(session, "未命名设备")
        assertEquals("Office", title)
        assertEquals("当前 · Office", SessionSwitchLabel.currentMarker(title, "当前"))
    }

    @Test
    fun missingLinkNameUsesTheFallback() {
        val title = SessionSwitchLabel.title(sample(custom = null, linkName = ""), "未命名设备")
        assertEquals("未命名设备", title)
    }

    private fun sample(custom: String?, linkName: String) = RemoteSession(
        id = "1",
        url = "https://zcode.z.ai/remote/v4?sid=abc",
        sid = "abc",
        deviceName = linkName,
        appVersion = null,
        protocolLabel = "v4",
        createdAtEpochMs = 1L,
        lastOpenedAtEpochMs = 1L,
        customDisplayName = custom,
    )
}
