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

        val restored = files.load(setOf("home", "office", "bad"))
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
        assertEquals("", files.load(setOf("home", "office")).read("home"))
        assertEquals("办公室的跟进", files.load(setOf("home", "office")).read("office"))

        files.save(both.clear())
        assertEquals("", files.load(setOf("home", "office")).read("home"))
        assertEquals("", files.load(setOf("office")).read("office"))
        assertEquals("FollowUpDrafts(sessions=0)", files.load(emptySet()).toString())
        assertTrue(directory.resolve("follow_up_drafts_v1").isFile)
    }

    @Test
    fun loadDropsDraftsWhoseSessionIsGone() {
        val directory = Files.createTempDirectory("drafts-orphan").toFile()
        val files = FollowUpDraftFiles(directory)
        files.save(
            FollowUpDrafts.empty()
                .write("home", "家里的跟进")
                .write("gone", "已经删掉的跟进")
                .write("office", "办公室的跟进"),
        )

        assertEquals("已经删掉的跟进", files.load(null).read("gone"))

        val restored = files.load(setOf("home", "office"))
        assertEquals("家里的跟进", restored.read("home"))
        assertEquals("办公室的跟进", restored.read("office"))
        assertEquals("", restored.read("gone"))
        assertFalse(restored.toString().contains("跟进"))
        assertFalse(files.toString().contains("跟进"))

        val stored = directory.resolve("follow_up_drafts_v1").readText()
        assertFalse(stored.contains("已经删掉的跟进"))
        assertFalse(stored.contains("http"))
        assertEquals("", files.load(setOf("home", "office", "gone")).read("gone"))
        assertEquals("家里的跟进", files.load(null).read("home"))
    }
}
