package com.zcodego.app.ui.remote

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import com.zcodego.app.security.AcceptedRemoteLink
import com.zcodego.app.security.LinkDecision
import com.zcodego.app.security.ParseResult
import com.zcodego.app.security.RejectReason
import com.zcodego.app.security.RemoteUrlPolicy

/**
 * A savable HTTPS remote link gets one short system haptic before the session
 * opens. Scans, paste, share, and the inbox share this pulse. Invalid payloads
 * decline with no haptic. The debug string does not include the URL.
 */
internal object ScanOutcome {
    fun feedbackConstant(sdkInt: Int = Build.VERSION.SDK_INT): Int =
        if (sdkInt >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        }

    fun pulse(view: View) {
        runCatching {
            view.performHapticFeedback(
                feedbackConstant(),
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
            )
        }
    }

    fun decide(raw: String): Decision {
        val parsed = RemoteUrlPolicy.parseRemoteLink(raw)
        return if (LinkDecision.canSave(parsed)) {
            val link = (parsed as ParseResult.Ok).link
            Accept(link.url, link)
        } else {
            Decline((parsed as ParseResult.Reject).reason)
        }
    }

    internal sealed class Decision {
        override fun toString(): String = "ScanOutcome"
    }

    internal class Accept(val url: String, val link: AcceptedRemoteLink) : Decision()

    internal class Decline(val reason: RejectReason) : Decision()
}
