package com.zcodego.app.ui.remote

/** Remote home shows the scan-or-paste prompt only when nothing is saved. */
internal object RemoteEmptyState {
    fun showScanOrPaste(sessionCount: Int): Boolean = sessionCount == 0
}
