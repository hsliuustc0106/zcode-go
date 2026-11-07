package com.zcodego.app.ui.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteEmptyStateTest {
    @Test
    fun promptOnlyWhenNothingIsSaved() {
        assertTrue(RemoteEmptyState.showScanOrPaste(0))
        assertFalse(RemoteEmptyState.showScanOrPaste(1))
        assertFalse(RemoteEmptyState.showScanOrPaste(3))
    }
}
