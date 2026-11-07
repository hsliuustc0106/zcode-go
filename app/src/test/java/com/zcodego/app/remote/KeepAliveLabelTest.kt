package com.zcodego.app.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class KeepAliveLabelTest {
    private val url = "https://zcode.z.ai/remote/v4?sid=secret"

    @Test
    fun customNameWinsThenLinkNameThenFallback() {
        assertEquals("家里的电脑", KeepAliveLabel.choose("家里的电脑", "QR-PC", "未命名设备"))
        assertEquals("QR-PC", KeepAliveLabel.choose("   ", "QR-PC", "未命名设备"))
        assertEquals("未命名设备", KeepAliveLabel.choose(null, "", "未命名设备"))
    }

    @Test
    fun urlsNeverBecomeTheNotificationLabel() {
        assertEquals("QR-PC", KeepAliveLabel.choose(url, "QR-PC", "未命名设备"))
        assertEquals("未命名设备", KeepAliveLabel.choose(url, "http://zcode.z.ai/remote", "未命名设备"))
        assertNull(KeepAliveLabel.safe(url))
        val label = KeepAliveLabel.choose("家里的电脑", url, "未命名设备")
        assertFalse(label.contains(url))
        assertFalse(label.contains("http"))
        assertFalse(label.contains("sid"))
    }
}
