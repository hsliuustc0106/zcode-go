package com.zcodego.app.ui.web

import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteWindowSecureTest {
    @Test
    fun onlyTheRemoteSessionUsesTheSecureFlag() {
        assertFalse(RemoteWindowSecure.shouldSecure(remoteSession = true, settingOn = false))
        assertFalse(RemoteWindowSecure.shouldSecure(remoteSession = false, settingOn = true))
        assertFalse(RemoteWindowSecure.shouldSecure(remoteSession = false, settingOn = false))
        assertTrue(RemoteWindowSecure.shouldSecure(remoteSession = true, settingOn = true))
        assertEquals(WindowManager.LayoutParams.FLAG_SECURE, RemoteWindowSecure.FLAG)
        assertEquals("RemoteWindowSecure", RemoteWindowSecure.toString())
        assertFalse(RemoteWindowSecure.toString().contains("http"))
        assertFalse(RemoteWindowSecure.toString().contains("sid"))
    }
}
