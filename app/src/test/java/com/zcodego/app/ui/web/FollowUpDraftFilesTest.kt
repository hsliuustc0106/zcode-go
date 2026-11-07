package com.zcodego.app.ui.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class FollowUpDraftFilesTest {
    private val url = "https://zcode.z.ai/remote/v4?sid=secret"

    @Test
    fun coldStartRestoresDraftsAndSkipsRemoteUrls() {
        val directory = Files.createTempDirectory("drafts").toFile()
        val files = FollowUpDraftFiles(directory)
        val saved = FollowUpDrafts.empty()
            .write("home", "家里的跟进")
            .write("office", "办公室的跟进")
            .write("bad", url)
        files.save(saved)

        val stored = directory.resolve("follow_up_drafts_v1").readText()
        assertFalse(stored.contains(url))
        assertFalse(stored.contains("http"))
        assertFalse(files.toString().contains("跟进"))

        val restored = files.load()
        assertEquals("家里的跟进", restored.read("home"))
        assertEquals("办公室的跟进", restored.read("office"))
        assertEquals("", restored.read("bad"))
        assertFalse(restored.toString().contains("跟进"))
        assertFalse(restored.toString().contains(url))
    }

    @Test
    fun removeAndClearStayGoneAfterReload() {
        val directory = Files.createTempDirectory("drafts-clear").toFile()
        val files = FollowUpDraftFiles(directory)
        val both = FollowUpDrafts.empty().write("home", "家里的跟进").write("office", "办公室的跟进")
        files.save(both.remove("home"))
        assertEquals("", files.load().read("home"))
        assertEquals("办公室的跟进", files.load().read("office"))

        files.save(both.clear())
        assertEquals("", files.load().read("home"))
        assertEquals("", files.load().read("office"))
        assertEquals("FollowUpDrafts(sessions=0)", files.load().toString())
        assertTrue(directory.resolve("follow_up_drafts_v1").isFile)
    }
}
