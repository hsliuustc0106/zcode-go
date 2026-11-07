package com.zcodego.app.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteIngestAndAttentionTest {
    @Test
    fun readsViewAndShareTextOnly() {
        assertEquals(
            "https://zcode.z.ai/remote/v4?sid=abc",
            RemoteIngest.rawText(RemoteIngest.ACTION_VIEW, " https://zcode.z.ai/remote/v4?sid=abc ", null),
        )
        assertEquals(
            "see https://zcode.z.ai/remote/v3?sid=abc",
            RemoteIngest.rawText(RemoteIngest.ACTION_SEND, null, "see https://zcode.z.ai/remote/v3?sid=abc"),
        )
        assertNull(RemoteIngest.rawText("android.intent.action.MAIN", "https://zcode.z.ai/remote/v4?sid=abc", null))
        assertNull(RemoteIngest.rawText(RemoteIngest.ACTION_SEND, null, "   "))
    }

    @Test
    fun titleHintIgnoresForegroundAndUrls() {
        assertNull(PageAttention.notableTitle(null, "Agent 完成了", appInForeground = true))
        assertEquals("Agent 完成了", PageAttention.notableTitle(null, "Agent 完成了", appInForeground = false))
        assertNull(PageAttention.notableTitle("Agent 完成了", "Agent 完成了", appInForeground = false))
        assertNull(PageAttention.notableTitle(null, "https://zcode.z.ai/remote/v4?sid=secret", appInForeground = false))
        assertNull(PageAttention.sanitize("   "))
    }
}
