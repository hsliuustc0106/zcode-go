package com.zcodego.app.ui.remote

import android.os.Build
import android.view.HapticFeedbackConstants
import com.zcodego.app.security.LinkDecision
import com.zcodego.app.security.ParseResult
import com.zcodego.app.security.RejectReason
import com.zcodego.app.security.RemoteUrlPolicy

/**
 * A successful scan of a savable HTTPS remote link gets one short system haptic
 * before the session opens. Decode failures never reach this type. Invalid
 * payloads decline with no haptic. The debug string does not include the URL.
 */
internal object ScanOutcome {
    fun feedbackConstant(sdkInt: Int = Build.VERSION.SDK_INT): Int =
        if (sdkInt >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        }

    fun decide(raw: String): Decision {
        val parsed = RemoteUrlPolicy.parseRemoteLink(raw)
        return if (LinkDecision.canSave(parsed)) {
            Accept((parsed as ParseResult.Ok).link.url)
        } else {
            Decline((parsed as ParseResult.Reject).reason)
        }
    }

    internal sealed class Decision {
        override fun toString(): String = "ScanOutcome"
    }

    internal class Accept(val url: String) : Decision()

    internal class Decline(val reason: RejectReason) : Decision()
}
