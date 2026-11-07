package com.zcodego.app.ui.web

/**
 * Follow-up text kept per saved session. Switching computers reads one id
 * and leaves every other id alone. The debug string does not include the text.
 */
internal class FollowUpDrafts private constructor(
    private val bySession: Map<String, String>,
) {
    fun read(sessionId: String): String = bySession[sessionId].orEmpty()

    fun write(sessionId: String, text: String): FollowUpDrafts {
        if (sessionId.isBlank()) return this
        val next = if (text.isEmpty()) bySession - sessionId else bySession + (sessionId to text)
        return FollowUpDrafts(next)
    }

    fun remove(sessionId: String): FollowUpDrafts = write(sessionId, "")

    fun clear(): FollowUpDrafts = empty()

    fun encode(): ArrayList<String> {
        val flat = ArrayList<String>(bySession.size * 2)
        bySession.forEach { (id, text) ->
            flat += id
            flat += text
        }
        return flat
    }

    override fun toString(): String = "FollowUpDrafts(sessions=${bySession.size})"

    companion object {
        fun empty(): FollowUpDrafts = FollowUpDrafts(emptyMap())

        fun decode(flat: List<String>): FollowUpDrafts {
            val map = LinkedHashMap<String, String>()
            var index = 0
            while (index + 1 < flat.size) {
                val id = flat[index]
                val text = flat[index + 1]
                if (id.isNotBlank() && text.isNotEmpty()) map[id] = text
                index += 2
            }
            return FollowUpDrafts(map)
        }
    }
}
