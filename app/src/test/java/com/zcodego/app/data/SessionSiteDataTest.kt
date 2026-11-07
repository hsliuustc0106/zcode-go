package com.zcodego.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSiteDataTest {
    @Test
    fun lastSessionOnASiteIsClearedAndASiblingIsKept() {
        val office = "https://zcode.z.ai/remote/v4?sid=office"
        val home = "https://zcode.z.ai/remote/v4?sid=home"
        val shared = SessionSiteData.planForRemoved(listOf(office), listOf(home))
        assertTrue(shared.origins.isEmpty())
        assertFalse(shared.toString().contains("sid"))

        val last = SessionSiteData.planForRemoved(listOf(office), emptyList())
        assertEquals(listOf("https://zcode.z.ai"), last.origins)
    }

    @Test
    fun otherHostsAndUnsafeUrlsStayOut() {
        val plan = SessionSiteData.planForRemoved(
            removedUrls = listOf(
                "https://zcode.z.ai/remote/v4?sid=a",
                "http://zcode.z.ai/remote/v4?sid=a",
                "https://user:pass@zcode.z.ai/remote/v4?sid=a",
            ),
            remainingUrls = listOf("https://open.bigmodel.cn/"),
        )
        assertEquals(listOf("https://zcode.z.ai"), plan.origins)
        assertEquals("https://zcode.z.ai:8443", SessionSiteData.originOf("https://zcode.z.ai:8443/remote/v4?sid=a"))
    }

    @Test
    fun cookieNamesKeepNamesOnly() {
        assertEquals(listOf("sid", "theme"), SessionSiteData.cookieNames("sid=secret; theme=dark"))
        assertTrue(SessionSiteData.cookieNames(null).isEmpty())
        assertTrue(SessionSiteData.cookieNames(" =x; bad name=1").isEmpty())
    }
}
