package com.zcodego.app.ui.remote

import android.view.HapticFeedbackConstants
import com.zcodego.app.security.RejectReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanOutcomeTest {
    private val remote = "https://zcode.z.ai/remote/v4?sid=abc"

    @Test
    fun validRemoteLinkIsAcceptedWithoutLoggingTheUrl() {
        val outcome = ScanOutcome.decide(remote)
        assertTrue(outcome is ScanOutcome.Accept)
        assertEquals(remote, (outcome as ScanOutcome.Accept).url)
        assertEquals("ScanOutcome", outcome.toString())
        assertFalse(outcome.toString().contains("http"))
        assertFalse(outcome.toString().contains("sid"))
    }

    @Test
    fun confirmOnApi30AndContextClickBeforeThat() {
        assertEquals(HapticFeedbackConstants.CONFIRM, ScanOutcome.feedbackConstant(30))
        assertEquals(HapticFeedbackConstants.CONFIRM, ScanOutcome.feedbackConstant(36))
        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, ScanOutcome.feedbackConstant(26))
        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, ScanOutcome.feedbackConstant(29))
    }

    @Test
    fun invalidPayloadsDeclineAndDoNotAccept() {
        val blank = ScanOutcome.decide("")
        val plain = ScanOutcome.decide("not a link")
        val http = ScanOutcome.decide("http://zcode.z.ai/remote/v4?sid=abc")
        assertTrue(blank is ScanOutcome.Decline)
        assertEquals(RejectReason.EMPTY, (blank as ScanOutcome.Decline).reason)
        assertTrue(plain is ScanOutcome.Decline)
        assertTrue(http is ScanOutcome.Decline)
        assertEquals(RejectReason.NOT_HTTPS, (http as ScanOutcome.Decline).reason)
        assertFalse(http.toString().contains("http"))
        assertFalse(plain.toString().contains("not a link"))
    }
}