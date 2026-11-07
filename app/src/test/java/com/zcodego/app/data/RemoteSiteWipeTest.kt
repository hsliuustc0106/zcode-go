package com.zcodego.app.data

import com.zcodego.app.ui.settings.RemoteSiteWipeCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteSiteWipeTest {
    @Test
    fun wipeNamesOfficialOriginsAndKeepsTheSessionList() {
        val saved = "https://zcode.z.ai/remote/v4?sid=secret"
        val plan = RemoteSiteWipe.plan(listOf(saved))
        assertTrue(plan.origins.contains("https://z.ai"))
        assertTrue(plan.origins.contains("https://zcode.z.ai"))
        assertTrue(plan.origins.contains("https://passport.z.ai"))
        assertTrue(plan.origins.contains("https://bigmodel.cn"))
        assertTrue(plan.origins.contains("https://open.bigmodel.cn"))
        assertTrue(plan.origins.contains("https://zhipuai.cn"))
        assertFalse(plan.origins.any { it.contains("secret") || it.contains("sid=") })
        assertEquals("Plan(origins=${plan.origins.size})", plan.toString())
        assertTrue(RemoteSiteWipeCopy.statesSessionsStay(RemoteSiteWipeCopy.DETAIL))
        assertTrue(RemoteSiteWipeCopy.statesSessionsStay(RemoteSiteWipeCopy.CONFIRM_BODY))
        assertFalse(RemoteSiteWipeCopy.CONFIRM_BODY.contains("https://"))
    }

    @Test
    fun nonOfficialAndUnsafeUrlsAreNotAdded() {
        val plan = RemoteSiteWipe.plan(
            listOf(
                "https://evil.example/remote/v4?sid=nope",
                "http://zcode.z.ai/remote/v4?sid=plain",
                "https://user:pass@zcode.z.ai/remote/v4?sid=creds",
            ),
        )
        assertFalse(plan.origins.any { it.contains("evil.example") })
        assertFalse(plan.toString().contains("nope"))
        assertFalse(plan.toString().contains("plain"))
        assertTrue(plan.origins.contains("https://zcode.z.ai"))
    }

    @Test
    fun emptySessionListStillClearsTheRemoteRoots() {
        val plan = RemoteSiteWipe.plan(emptyList())
        assertTrue(plan.origins.contains("https://z.ai"))
        assertTrue(plan.origins.contains("https://open.bigmodel.cn"))
        assertTrue(RemoteSiteWipeCopy.DONE.contains("已保存的会话还在"))
    }
}
