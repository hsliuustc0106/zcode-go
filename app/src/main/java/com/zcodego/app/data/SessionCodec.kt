package com.zcodego.app.data

import com.zcodego.app.security.ParseResult
import com.zcodego.app.security.RemoteUrlPolicy

/**
 * JSON codec for saved sessions. Implemented here so unit tests do not depend on
 * the Android `org.json` stub, which returns null from `put`. The URL stays in
 * the blob and out of [RemoteSession.toString].
 */
internal object SessionCodec {
    fun encode(sessions: List<RemoteSession>): String {
        val items = sessions.joinToString(separator = ",") { session ->
            buildString {
                append('{')
                append(field("id", session.id))
                append(',')
                append(field("url", session.url))
                append(',')
                append(field("sid", session.sid))
                append(',')
                append(field("deviceName", session.deviceName))
                append(',')
                append(field("customDisplayName", session.customDisplayName))
                append(',')
                append(field("appVersion", session.appVersion))
                append(',')
                append(field("protocolLabel", session.protocolLabel))
                append(',')
                append(numberField("createdAtEpochMs", session.createdAtEpochMs))
                append(',')
                append(numberField("lastOpenedAtEpochMs", session.lastOpenedAtEpochMs))
                append('}')
            }
        }
        return """{"sessions":[$items]}"""
    }

    fun decode(raw: String?): List<RemoteSession> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val root = JsonValue.parse(raw) as? JsonValue.Obj ?: return emptyList()
            val sessions = root.fields["sessions"] as? JsonValue.Arr ?: return emptyList()
            sessions.items.mapNotNull { item ->
                val obj = item as? JsonValue.Obj ?: return@mapNotNull null
                val session = obj.toSession() ?: return@mapNotNull null
                if (RemoteUrlPolicy.parseRemoteLink(session.url) is ParseResult.Ok) session else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun field(name: String, value: String?): String =
        if (value == null) {
            "${quote(name)}:null"
        } else {
            "${quote(name)}:${quote(value)}"
        }

    private fun numberField(name: String, value: Long): String = "${quote(name)}:$value"

    private fun quote(value: String): String {
        val out = StringBuilder(value.length + 2)
        out.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> if (ch.code < 0x20) {
                    out.append("\\u")
                    out.append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    out.append(ch)
                }
            }
        }
        out.append('"')
        return out.toString()
    }

    private fun JsonValue.Obj.toSession(): RemoteSession? {
        val id = fields["id"].asString()?.takeIf { it.isNotBlank() } ?: return null
        val url = fields["url"].asString()?.takeIf { it.isNotBlank() } ?: return null
        val sid = fields["sid"].asString()?.takeIf { it.isNotBlank() } ?: return null
        return RemoteSession(
            id = id,
            url = url,
            sid = sid,
            deviceName = fields["deviceName"].asString().orEmpty(),
            appVersion = fields["appVersion"].asString()?.takeIf { it.isNotBlank() },
            protocolLabel = fields["protocolLabel"].asString()?.ifBlank { "remote" } ?: "remote",
            createdAtEpochMs = fields["createdAtEpochMs"].asLong(),
            lastOpenedAtEpochMs = fields["lastOpenedAtEpochMs"].asLong(),
            customDisplayName = fields["customDisplayName"].asString()?.trim()?.ifBlank { null },
        )
    }

    private fun JsonValue?.asString(): String? = (this as? JsonValue.Str)?.value

    private fun JsonValue?.asLong(): Long = when (this) {
        is JsonValue.Num -> value
        else -> 0L
    }
}

private sealed interface JsonValue {
    data class Obj(val fields: Map<String, JsonValue>) : JsonValue
    data class Arr(val items: List<JsonValue>) : JsonValue
    data class Str(val value: String) : JsonValue
    data class Num(val value: Long) : JsonValue
    data object Null : JsonValue

    companion object {
        fun parse(raw: String): JsonValue = Reader(raw).parseValue()
    }

    private class Reader(private val raw: String) {
        private var index = 0

        fun parseValue(): JsonValue {
            skip()
            if (index >= raw.length) error("empty")
            return when (val ch = raw[index]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> Str(parseString())
                'n' -> {
                    expect("null")
                    Null
                }
                else -> if (ch == '-' || ch.isDigit()) parseNumber() else error("bad json at $index")
            }
        }

        private fun parseObject(): Obj {
            index++
            val fields = linkedMapOf<String, JsonValue>()
            skip()
            if (peek('}')) {
                index++
                return Obj(fields)
            }
            while (index < raw.length) {
                skip()
                val key = parseString()
                skip()
                if (index >= raw.length || raw[index] != ':') error("expected colon")
                index++
                fields[key] = parseValue()
                skip()
                when {
                    peek(',') -> index++
                    peek('}') -> {
                        index++
                        return Obj(fields)
                    }
                    else -> error("expected comma or end")
                }
            }
            error("unclosed object")
        }

        private fun parseArray(): Arr {
            index++
            val items = mutableListOf<JsonValue>()
            skip()
            if (peek(']')) {
                index++
                return Arr(items)
            }
            while (index < raw.length) {
                items += parseValue()
                skip()
                when {
                    peek(',') -> index++
                    peek(']') -> {
                        index++
                        return Arr(items)
                    }
                    else -> error("expected comma or end")
                }
            }
            error("unclosed array")
        }

        private fun parseString(): String {
            if (index >= raw.length || raw[index] != '"') error("expected string")
            index++
            val out = StringBuilder()
            while (index < raw.length) {
                when (val ch = raw[index++]) {
                    '"' -> return out.toString()
                    '\\' -> {
                        if (index >= raw.length) error("bad escape")
                        when (val esc = raw[index++]) {
                            '"', '\\', '/' -> out.append(esc)
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000c')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                if (index + 4 > raw.length) error("bad unicode")
                                val hex = raw.substring(index, index + 4)
                                index += 4
                                out.append(hex.toInt(16).toChar())
                            }
                            else -> error("bad escape")
                        }
                    }
                    else -> out.append(ch)
                }
            }
            error("unclosed string")
        }

        private fun parseNumber(): Num {
            val start = index
            if (peek('-')) index++
            while (index < raw.length && raw[index].isDigit()) index++
            return Num(raw.substring(start, index).toLong())
        }

        private fun expect(token: String) {
            if (!raw.startsWith(token, index)) error("expected $token")
            index += token.length
        }

        private fun skip() {
            while (index < raw.length && raw[index].isWhitespace()) index++
        }

        private fun peek(ch: Char): Boolean = index < raw.length && raw[index] == ch
    }
}
