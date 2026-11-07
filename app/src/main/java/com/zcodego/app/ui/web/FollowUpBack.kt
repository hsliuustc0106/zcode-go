package com.zcodego.app.ui.web

/** System Back collapses an open follow-up bar before leaving the remote page. */
internal object FollowUpBack {
    fun consume(barVisible: Boolean, expanded: Boolean): Boolean = barVisible && expanded
}
