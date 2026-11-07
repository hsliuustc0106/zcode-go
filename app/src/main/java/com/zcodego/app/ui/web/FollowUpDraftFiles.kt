package com.zcodego.app.ui.web

import com.zcodego.app.remote.AttentionOpen
import java.io.File

/**
 * Private-file snapshot of follow-up drafts. Keys are session ids.
 * Remote URLs are not written. The codec string is not logged.
 */
internal object FollowUpDraftCodec {
    fun encode(drafts: FollowUpDrafts): String = buildString {
        val pairs = storable(drafts)
        append(pairs.size)
        append('\n')
        for ((id, text) in pairs) {
            appendField(id)
            appendField(text)
        }
    }

    fun decode(raw: String?): FollowUpDrafts {
        if (raw.isNullOrBlank()) return FollowUpDrafts.empty()
        val reader = Cursor(raw)
        val count = reader.readInt() ?: return FollowUpDrafts.empty()
        if (count < 0 || count > 100) return FollowUpDrafts.empty()
        var drafts = FollowUpDrafts.empty()
        repeat(count) {
            val idLen = reader.readInt() ?: return drafts
            val id = reader.readField(idLen) ?: return drafts
            val textLen = reader.readInt() ?: return drafts
            val text = reader.readField(textLen) ?: return drafts
            val sessionId = AttentionOpen.sessionId(id)
            if (sessionId != null && text.isNotEmpty() && !hasUrl(text)) {
                drafts = drafts.write(sessionId, text)
            }
        }
        return drafts
    }

    private fun storable(drafts: FollowUpDrafts): List<Pair<String, String>> {
        val flat = drafts.encode()
        val pairs = ArrayList<Pair<String, String>>()
        var index = 0
        while (index + 1 < flat.size) {
            val id = AttentionOpen.sessionId(flat[index])
            val text = flat[index + 1]
            if (id != null && text.isNotEmpty() && !hasUrl(text)) pairs += id to text
            index += 2
        }
        return pairs
    }

    private fun StringBuilder.appendField(value: String) {
        append(value.length)
        append('\n')
        append(value)
        append('\n')
    }

    private fun hasUrl(value: String): Boolean =
        value.contains("http://", ignoreCase = true) || value.contains("https://", ignoreCase = true)

    private class Cursor(private val raw: String) {
        private var index = 0

        fun readInt(): Int? {
            val end = raw.indexOf('\n', index)
            if (end < 0) return null
            val value = raw.substring(index, end).toIntOrNull() ?: return null
            index = end + 1
            return value
        }

        fun readField(length: Int): String? {
            if (length < 0 || index + length > raw.length) return null
            val value = raw.substring(index, index + length)
            index += length
            if (index >= raw.length || raw[index] != '\n') return null
            index += 1
            return value
        }
    }
}

internal class FollowUpDraftFiles(private val directory: File) {
    private val file = File(directory, FILE_NAME)

    fun load(): FollowUpDrafts {
        return runCatching {
            if (!file.isFile) FollowUpDrafts.empty()
            else FollowUpDraftCodec.decode(file.readText(Charsets.UTF_8))
        }.getOrDefault(FollowUpDrafts.empty())
    }

    fun save(drafts: FollowUpDrafts) {
        runCatching {
            if (!directory.exists() && !directory.mkdirs()) return
            val encoded = FollowUpDraftCodec.encode(drafts)
            val temporary = File(directory, "$FILE_NAME.tmp")
            temporary.writeText(encoded, Charsets.UTF_8)
            if (!temporary.renameTo(file)) {
                file.writeText(encoded, Charsets.UTF_8)
                temporary.delete()
            }
        }
    }

    override fun toString(): String = "FollowUpDraftFiles"

    private companion object {
        const val FILE_NAME = "follow_up_drafts_v1"
    }
}
