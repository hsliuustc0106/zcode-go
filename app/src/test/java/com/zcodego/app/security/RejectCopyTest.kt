package com.zcodego.app.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RejectCopyTest {
    @Test
    fun nonRemotePasteIsExplainedAndNotSaved() {
        val samples = listOf(
            "你好",
            "http://zcode.z.ai/remote/v4?sid=abc",
            "https://github.com/hsliuustc0106/zcode-go",
            "https://zcode.z.ai/",
            "https://zcode.z.ai/cn/docs/remote-control",
            "https://zcode.z.ai/remote/v4",
        )
        samples.forEach { raw ->
            val parsed = RemoteUrlPolicy.parseRemoteLink(raw)
            assertTrue(raw, parsed is ParseResult.Reject)
            assertFalse(raw, LinkDecision.canSave(parsed))
            val message = RejectCopy.explain((parsed as ParseResult.Reject).reason)
            assertTrue(message, message.contains("没有保存"))
            assertTrue(message, message.contains("zcode.z.ai/remote"))
            if (!raw.startsWith("https://zcode.z.ai")) {
                assertFalse(message, message.contains(raw))
            }
        }
    }

    @Test
    fun everyReasonSaysItWasNotSaved() {
        RejectReason.entries.forEach { reason ->
            val message = RejectCopy.explain(reason)
            assertTrue(reason.name, message.contains("没有保存"))
            assertTrue(reason.name, message.contains("https://zcode.z.ai/remote/"))
        }
    }

    @Test
    fun officialRemoteLinkCanBeSaved() {
        val parsed = RemoteUrlPolicy.parseRemoteLink("https://zcode.z.ai/remote/v4?sid=abc")
        assertTrue(LinkDecision.canSave(parsed))
    }
}
