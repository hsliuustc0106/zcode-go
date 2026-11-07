package com.zcodego.app.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AttentionNoticeTest {
    private val fixed = "远程页面有变化"
    private val url = "https://zcode.z.ai/remote/v4?sid=secret"

    @Test
    fun keepsTheFixedTitleAndNamesTheComputer() {
        val copy = AttentionNotice.copy(fixed, "构建完成了", "家里的电脑", "QR-PC", "未命名设备")
        assertEquals(fixed, copy?.title)
        assertEquals("家里的电脑 · 构建完成了", copy?.body)
        assertFalse(copy!!.title.contains(url))
        assertFalse(copy.body.contains("http"))
        assertFalse(copy.toString().contains(url))
        assertFalse(copy.toString().contains("构建"))
    }

    @Test
    fun blankCustomNameUsesTheLinkNameThenTheFallback() {
        assertEquals(
            "QR-PC · 构建完成了",
            AttentionNotice.copy(fixed, "构建完成了", "  ", "QR-PC", "未命名设备")?.body,
        )
        assertEquals(
            "未命名设备 · 构建完成了",
            AttentionNotice.copy(fixed, "构建完成了", null, "", "未命名设备")?.body,
        )
    }

    @Test
    fun dropsUrlsFromTheTitleAndTheBody() {
        assertNull(AttentionNotice.copy(fixed, url, "家里的电脑", "QR-PC", "未命名设备"))
        val copy = AttentionNotice.copy(fixed, "构建完成了", url, "QR-PC", "未命名设备")
        assertEquals(fixed, copy?.title)
        assertEquals("QR-PC · 构建完成了", copy?.body)
        assertFalse(copy!!.body.contains(url))
    }
}
