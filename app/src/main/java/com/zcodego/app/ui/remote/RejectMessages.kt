package com.zcodego.app.ui.remote

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zcodego.app.R
import com.zcodego.app.security.RejectReason

@Composable
fun rejectMessage(reason: RejectReason): String = stringResource(reason.messageRes())

fun RejectReason.messageRes(): Int = when (this) {
    RejectReason.EMPTY -> R.string.err_empty
    RejectReason.NOT_HTTPS -> R.string.err_not_https
    RejectReason.UNSUPPORTED_HOST -> R.string.err_host
    RejectReason.NOT_REMOTE_LINK -> R.string.err_not_remote
    RejectReason.MISSING_SESSION -> R.string.err_missing_sid
    RejectReason.CREDENTIALS_IN_URL -> R.string.err_userinfo
    RejectReason.BAD_PORT -> R.string.err_port
    RejectReason.UNPARSEABLE -> R.string.err_unparseable
}
