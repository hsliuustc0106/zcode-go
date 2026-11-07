package com.zcodego.app.ui.remote

import com.zcodego.app.security.RejectCopy
import com.zcodego.app.security.RejectReason

fun rejectMessage(reason: RejectReason): String = RejectCopy.explain(reason)
