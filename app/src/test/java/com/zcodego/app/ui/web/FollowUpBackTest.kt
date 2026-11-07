package com.zcodego.app.ui.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowUpBackTest {
    @Test
    fun backCollapsesOnlyAnOpenVisibleBar() {
        assertTrue(FollowUpBack.consume(barVisible = true, expanded = true))
        assertFalse(FollowUpBack.consume(barVisible = true, expanded = false))
        assertFalse(FollowUpBack.consume(barVisible = false, expanded = true))
        assertFalse(FollowUpBack.consume(barVisible = false, expanded = false))
    }
}
