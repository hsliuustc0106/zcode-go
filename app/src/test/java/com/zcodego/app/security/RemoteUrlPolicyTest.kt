package com.zcodego.app.security

import com.zcodego.app.ui.web.OfficialDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteUrlPolicyTest {
    @Test
    fun acceptsPublicV3LinkAndKeepsEncoding() {
        val raw = "https://zcode.z.ai/remote/v3?sid=abc&hash=abc%2Bdef%3D&t=1700000000000&name=HOME-PC&app_version=3.1.8"
        val result = RemoteUrlPolicy.parseRemoteLink(raw)
        assertTrue(result is ParseResult.Ok)
        val link = (result as ParseResult.Ok).link
        assertEquals(raw, link.url)
        assertEquals("abc", link.sid)
        assertEquals("HOME-PC", link.deviceName)
        assertEquals("3.1.8", link.appVersion)
        assertEquals("v3", link.protocolLabel)
        assertFalse(link.toString().contains("abc%2Bdef"))
        assertFalse(link.toString().contains("hash"))
    }

    @Test
    fun acceptsV4LinkWithoutHash() {
        val raw = "https://zcode.z.ai/remote/v4?sid=abc&mid=m1&name=Office&app_version=3.16.2"
        val link = (RemoteUrlPolicy.parseRemoteLink(raw) as ParseResult.Ok).link
        assertEquals("abc", link.sid)
        assertEquals("Office", link.deviceName)
        assertEquals("3.16.2", link.appVersion)
        assertEquals("v4", link.protocolLabel)
        assertEquals(raw, link.url)
    }

    @Test
    fun decodesDisplayNameAndKeepsPlusInSid() {
        val raw = "https://zcode.z.ai/remote/v4?sid=abc%2Bdef&name=My%20PC"
        val link = (RemoteUrlPolicy.parseRemoteLink(raw) as ParseResult.Ok).link
        assertEquals("abc+def", link.sid)
        assertEquals("My PC", link.deviceName)
    }

    @Test
    fun extractsUrlFromSurroundingText() {
        val raw = "打开 https://zcode.z.ai/remote/v4?sid=abc&name=Lab 即可"
        val link = (RemoteUrlPolicy.parseRemoteLink(raw) as ParseResult.Ok).link
        assertEquals("https://zcode.z.ai/remote/v4?sid=abc&name=Lab", link.url)
        assertEquals("Lab", link.deviceName)
    }

    @Test
    fun acceptsExplicitHttpsPortAndMixedCaseHost() {
        val raw = "HTTPS://ZCode.z.AI:443/remote/v4?sid=abc"
        val link = (RemoteUrlPolicy.parseRemoteLink("  $raw  ") as ParseResult.Ok).link
        assertEquals(raw, link.url)
        assertEquals("abc", link.sid)
    }

    @Test
    fun unnamedDeviceStaysBlank() {
        val link = (RemoteUrlPolicy.parseRemoteLink("https://zcode.z.ai/remote/v4?sid=abc") as ParseResult.Ok).link
        assertEquals("", link.deviceName)
        assertEquals("v4", link.protocolLabel)
    }

    @Test
    fun firstSidWins() {
        val link = (
            RemoteUrlPolicy.parseRemoteLink("https://zcode.z.ai/remote/v4?sid=abc&sid=other") as ParseResult.Ok
            ).link
        assertEquals("abc", link.sid)
    }

    @Test
    fun rejectsUnsafeRemoteLinks() {
        val cases = listOf(
            "" to RejectReason.EMPTY,
            "   " to RejectReason.EMPTY,
            "http://zcode.z.ai/remote/v4?sid=abc" to RejectReason.NOT_HTTPS,
            "请用 http://zcode.z.ai/remote/v4?sid=abc" to RejectReason.NOT_HTTPS,
            "https://zcode.z.ai.evil.com/remote/v4?sid=abc" to RejectReason.UNSUPPORTED_HOST,
            "https://evil.com/remote/v4?sid=abc" to RejectReason.UNSUPPORTED_HOST,
            "https://notz.ai/remote/v4?sid=abc" to RejectReason.UNSUPPORTED_HOST,
            "https://user:pass@zcode.z.ai/remote/v4?sid=abc" to RejectReason.CREDENTIALS_IN_URL,
            "https://token@zcode.z.ai/remote/v4?sid=abc" to RejectReason.CREDENTIALS_IN_URL,
            "https://zcode.z.ai:8443/remote/v4?sid=abc" to RejectReason.BAD_PORT,
            "https://zcode.z.ai/cn/docs/remote-control" to RejectReason.NOT_REMOTE_LINK,
            "https://zcode.z.ai/remote-not?sid=abc" to RejectReason.NOT_REMOTE_LINK,
            "https://zcode.z.ai/" to RejectReason.NOT_REMOTE_LINK,
            "https://zcode.z.ai/remote/v4" to RejectReason.MISSING_SESSION,
            "https://zcode.z.ai/remote/v4?sid=" to RejectReason.MISSING_SESSION,
            "javascript:alert(1)" to RejectReason.UNPARSEABLE,
            "https://127.0.0.1/remote/v4?sid=abc" to RejectReason.UNSUPPORTED_HOST,
        )
        cases.forEach { (raw, reason) ->
            val result = RemoteUrlPolicy.parseRemoteLink(raw)
            assertTrue(raw, result is ParseResult.Reject)
            assertEquals(raw, reason, (result as ParseResult.Reject).reason)
        }
    }

    @Test
    fun navigationAllowlistStaysOnOfficialDomains() {
        assertTrue(RemoteUrlPolicy.isAllowedNavigation("https://zcode.z.ai/"))
        assertTrue(RemoteUrlPolicy.isAllowedNavigation("https://zcode.z.ai/remote/v4?sid=abc"))
        assertTrue(RemoteUrlPolicy.isAllowedNavigation("https://open.bigmodel.cn/"))
        assertTrue(RemoteUrlPolicy.isAllowedNavigation("https://www.bigmodel.cn/login"))
        assertTrue(RemoteUrlPolicy.isAllowedNavigation("https://z.ai/"))
        assertTrue(RemoteUrlPolicy.isAllowedNavigation("https://passport.z.ai/oauth"))
        assertTrue(RemoteUrlPolicy.isAllowedNavigation("https://a.b.zhipuai.cn/"))
        assertTrue(RemoteUrlPolicy.isAllowedNavigation("https://z.ai./"))
        assertTrue(RemoteUrlPolicy.isAllowedNavigation("about:blank"))
        assertTrue(RemoteUrlPolicy.isAllowedNavigation("https://zcode.z.ai:443/cn"))

        assertFalse(RemoteUrlPolicy.isAllowedNavigation("http://z.ai/"))
        assertFalse(RemoteUrlPolicy.isAllowedNavigation("https://notz.ai/"))
        assertFalse(RemoteUrlPolicy.isAllowedNavigation("https://evilz.ai/"))
        assertFalse(RemoteUrlPolicy.isAllowedNavigation("https://z.ai.evil.com/"))
        assertFalse(RemoteUrlPolicy.isAllowedNavigation("https://bigmodel.cn.evil.com/"))
        assertFalse(RemoteUrlPolicy.isAllowedNavigation("https://user:pass@z.ai/"))
        assertFalse(RemoteUrlPolicy.isAllowedNavigation("https://169.254.1.1/"))
        assertFalse(RemoteUrlPolicy.isAllowedNavigation("https://zcode.z.ai:444/cn"))
        assertFalse(RemoteUrlPolicy.isAllowedNavigation("javascript:https://z.ai/"))
    }

    @Test
    fun officialEntryPointsStayOnTheAllowlist() {
        OfficialDestination.entries.forEach { destination ->
            assertTrue(destination.url, destination.url.startsWith("https://"))
            assertTrue(destination.url, RemoteUrlPolicy.isAllowedNavigation(destination.url))
            assertTrue(
                destination.url,
                RemoteUrlPolicy.parseRemoteLink(destination.url) is ParseResult.Reject,
            )
        }
    }
}
