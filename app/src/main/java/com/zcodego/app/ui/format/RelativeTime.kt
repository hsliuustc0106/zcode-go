package com.zcodego.app.ui.format

import java.time.Instant
import java.time.ZoneId

internal object RelativeTime {
    fun format(epochMs: Long, nowEpochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val delta = (nowEpochMs - epochMs).coerceAtLeast(0L)
        val minutes = delta / 60_000L
        return when {
            minutes < 1L -> "刚刚"
            minutes < 60L -> "$minutes 分钟前"
            minutes < 60L * 24L -> "${minutes / 60L} 小时前"
            minutes < 60L * 24L * 7L -> "${minutes / (60L * 24L)} 天前"
            else -> {
                val date = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
                "%d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
            }
        }
    }
}
