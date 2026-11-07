package com.zcodego.app.ui.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLinkCopyTest {
    private val remote = "https://zcode.z.ai/remote/v4?sid=abc&name=Office"

    @Test
    fun copiesTheHttpsRemoteUrlAndHidesItFromTheSnackbar() {
        assertEquals(remote, SessionLinkCopy.textToCopy("  $remote  "))
        val message = SessionLinkCopy.message(copied = true)
        assertEquals("已复制当前链接", message)
        assertTrue(SessionLinkCopy.messageHidesUrl(message))
        assertFalse(message.contains(remote))
        assertFalse(message.contains("sid"))
    }

    @Test
    fun rejectsUnsafeOrNonRemoteUrls() {
        assertNull(SessionLinkCopy.textToCopy("http://zcode.z.ai/remote/v4?sid=abc"))
        assertNull(SessionLinkCopy.textToCopy("https://github.com/hsliuustc0106/zcode-go"))
        assertNull(SessionLinkCopy.textToCopy("https://zcode.z.ai/cn/docs/remote-control"))
        assertNull(SessionLinkCopy.textToCopy(null))
        val message = SessionLinkCopy.message(copied = false)
        assertEquals("这条链接不能复制", message)
        assertTrue(SessionLinkCopy.messageHidesUrl(message))
    }
}
