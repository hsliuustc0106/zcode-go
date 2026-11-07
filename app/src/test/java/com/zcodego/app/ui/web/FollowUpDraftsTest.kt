package com.zcodego.app.ui.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FollowUpDraftsTest {
    @Test
    fun switchingKeepsEachSessionsDraft() {
        var drafts = FollowUpDrafts.empty()
        drafts = drafts.write("home", "家里的跟进")
        drafts = drafts.write("office", "办公室的跟进")
        assertEquals("家里的跟进", drafts.read("home"))
        assertEquals("办公室的跟进", drafts.read("office"))

        drafts = drafts.write("home", "家里改过")
        assertEquals("家里改过", drafts.read("home"))
        assertEquals("办公室的跟进", drafts.read("office"))

        drafts = drafts.write("home", "")
        assertEquals("", drafts.read("home"))
        assertEquals("办公室的跟进", drafts.read("office"))
        assertFalse(drafts.toString().contains("跟进"))
        assertFalse(drafts.toString().contains("https://"))
    }

    @Test
    fun blankIdDoesNotChangeOtherDrafts() {
        val drafts = FollowUpDrafts.empty().write("home", "保留").write("  ", "泄漏")
        assertEquals("保留", drafts.read("home"))
        assertEquals("", drafts.read("  "))
    }

    @Test
    fun encodeRoundTripDropsEmptyText() {
        val drafts = FollowUpDrafts.empty().write("a", "甲").write("b", "")
        val restored = FollowUpDrafts.decode(drafts.encode())
        assertEquals("甲", restored.read("a"))
        assertEquals("", restored.read("b"))
        assertEquals("FollowUpDrafts(sessions=1)", restored.toString())
    }
}
