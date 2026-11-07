package com.zcodego.app.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class RelativeTimeTest {
    private val zone = ZoneOffset.UTC

    @Test
    fun buckets() {
        val now = 1_700_000_000_000L
        assertEquals("刚刚", RelativeTime.format(now, now, zone))
        assertEquals("刚刚", RelativeTime.format(now + 5_000L, now, zone))
        assertEquals("3 分钟前", RelativeTime.format(now - 3 * 60_000L, now, zone))
        assertEquals("2 小时前", RelativeTime.format(now - 2 * 60 * 60_000L, now, zone))
        assertEquals("4 天前", RelativeTime.format(now - 4 * 24 * 60 * 60_000L, now, zone))
    }
}
