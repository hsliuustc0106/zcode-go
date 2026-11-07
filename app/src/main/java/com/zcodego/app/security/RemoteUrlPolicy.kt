package com.zcodego.app.security

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Accepts public ZCode remote-control links and decides which navigations the
 * in-app page may follow.
 *
 * Pairing links are https://zcode.z.ai/remote/… and carry a sid. v3 links also
 * carry hash and t; v4 links often do not. This class does not speak the relay
 * protocol. The official page loaded in the WebView does that itself.
 *
 * The stored string is the caller's URL, not a rebuilt one, so encoded
 * credentials inside the query are left intact.
 */
object RemoteUrlPolicy {
    const val REMOTE_HOST = "zcode.z.ai"
    private const val MAX_URL_LENGTH = 4096
    private const val MAX_LABEL_LENGTH = 80
    private const val MAX_SID_LENGTH = 256

    private val officialSuffixes = listOf("z.ai", "bigmodel.cn", "zhipuai.cn")
    private val httpsUrl = Regex("""https://[^\s<>"'“”]+""", RegexOption.IGNORE_CASE)
    private val httpUrl = Regex("""http://""", RegexOption.IGNORE_CASE)

    fun parseRemoteLink(raw: String): ParseResult {
        val trimmed = raw.trim().trim('\uFEFF')
        if (trimmed.isEmpty()) return ParseResult.Reject(RejectReason.EMPTY)
        if (trimmed.length > MAX_URL_LENGTH) return ParseResult.Reject(RejectReason.UNPARSEABLE)

        val candidate = extractCandidate(trimmed)
            ?: return if (httpUrl.containsMatchIn(trimmed)) {
                ParseResult.Reject(RejectReason.NOT_HTTPS)
            } else if (trimmed.isBlank()) {
                ParseResult.Reject(RejectReason.EMPTY)
            } else {
                ParseResult.Reject(RejectReason.UNPARSEABLE)
            }

        if (candidate.length > MAX_URL_LENGTH || candidate.any { it.isISOControl() }) {
            return ParseResult.Reject(RejectReason.UNPARSEABLE)
        }
        if (!candidate.startsWith("https://", ignoreCase = true)) {
            return if (candidate.startsWith("http://", ignoreCase = true)) {
                ParseResult.Reject(RejectReason.NOT_HTTPS)
            } else {
                ParseResult.Reject(RejectReason.UNPARSEABLE)
            }
        }

        val uri = try {
            URI(candidate)
        } catch (_: URISyntaxException) {
            return ParseResult.Reject(RejectReason.UNPARSEABLE)
        }

        if (!uri.scheme.equals("https", ignoreCase = true)) {
            return ParseResult.Reject(RejectReason.NOT_HTTPS)
        }
        if (!uri.rawUserInfo.isNullOrEmpty() || !uri.userInfo.isNullOrEmpty()) {
            return ParseResult.Reject(RejectReason.CREDENTIALS_IN_URL)
        }
        if (uri.port != -1 && uri.port != 443) {
            return ParseResult.Reject(RejectReason.BAD_PORT)
        }
        val host = normalizeHost(uri.host) ?: return ParseResult.Reject(RejectReason.UNSUPPORTED_HOST)
        if (host != REMOTE_HOST || isIpAddress(host)) {
            return ParseResult.Reject(RejectReason.UNSUPPORTED_HOST)
        }

        val path = (uri.rawPath ?: uri.path ?: "").lowercase(Locale.ROOT)
        if (path != "/remote" && !path.startsWith("/remote/")) {
            return ParseResult.Reject(RejectReason.NOT_REMOTE_LINK)
        }

        val sid = queryParam(uri.rawQuery, "sid", decode = ::decodeToken)
        if (sid.isNullOrEmpty() || sid.length > MAX_SID_LENGTH || sid.any { it.isISOControl() || it.isWhitespace() }) {
            return ParseResult.Reject(RejectReason.MISSING_SESSION)
        }

        val deviceName = sanitizeLabel(queryParam(uri.rawQuery, "name", decode = ::decodeDisplay))
        val appVersion = sanitizeVersion(queryParam(uri.rawQuery, "app_version", decode = ::decodeDisplay))

        return ParseResult.Ok(
            AcceptedRemoteLink(
                url = candidate,
                sid = sid,
                deviceName = deviceName,
                appVersion = appVersion,
                protocolLabel = protocolLabel(path, uri.rawQuery),
            ),
        )
    }

    fun isAllowedNavigation(raw: String): Boolean {
        if (raw == "about:blank") return true
        if (raw.length > MAX_URL_LENGTH || raw.any { it.isISOControl() }) return false
        val uri = try {
            URI(raw)
        } catch (_: URISyntaxException) {
            return false
        } catch (_: IllegalArgumentException) {
            return false
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (!uri.rawUserInfo.isNullOrEmpty() || !uri.userInfo.isNullOrEmpty()) return false
        if (uri.port != -1 && uri.port != 443) return false
        val host = normalizeHost(uri.host) ?: return false
        if (isIpAddress(host)) return false
        return isOfficialHost(host)
    }

    fun isOfficialHost(host: String): Boolean {
        val normalized = normalizeHost(host) ?: return false
        return officialSuffixes.any { suffix ->
            normalized == suffix || normalized.endsWith(".$suffix")
        }
    }

    private fun extractCandidate(trimmed: String): String? {
        val match = httpsUrl.find(trimmed)?.value ?: return null
        return match.trimEnd('.', ',', ';', ')', ']', '>', '”', '"', '\'')
    }

    private fun protocolLabel(path: String, rawQuery: String?): String {
        return when {
            path.contains("/v3") -> "v3"
            path.contains("/v4") -> "v4"
            queryContains(rawQuery, "hash") && queryContains(rawQuery, "t") -> "v3"
            else -> "remote"
        }
    }

    private fun queryContains(rawQuery: String?, name: String): Boolean =
        queryParam(rawQuery, name, decode = ::decodeToken) != null

    private fun queryParam(rawQuery: String?, name: String, decode: (String) -> String): String? {
        if (rawQuery.isNullOrEmpty()) return null
        for (part in rawQuery.split('&')) {
            if (part.isEmpty()) continue
            val eq = part.indexOf('=')
            val rawKey = if (eq >= 0) part.substring(0, eq) else part
            val rawValue = if (eq >= 0) part.substring(eq + 1) else ""
            if (decode(rawKey) == name) {
                return decode(rawValue).trim().ifEmpty { null }
            }
        }
        return null
    }

    private fun decodeDisplay(value: String): String =
        try {
            URLDecoder.decode(value, StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            value
        }

    /** Keeps literal '+' so base64-like session ids are not turned into spaces. */
    private fun decodeToken(value: String): String =
        try {
            URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            value
        }

    private fun sanitizeLabel(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val cleaned = raw.replace(Regex("[\\p{Cntrl}]"), "").trim()
        return cleaned.take(MAX_LABEL_LENGTH)
    }

    private fun sanitizeVersion(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim().take(32)
        if (!cleaned.matches(Regex("""[0-9A-Za-z._+\-]+"""))) return null
        return cleaned
    }

    private fun normalizeHost(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val host = raw.trim().trimEnd('.').lowercase(Locale.ROOT)
        if (host.isEmpty() || host.any { it.isISOControl() || it.isWhitespace() }) return null
        return host
    }

    private fun isIpAddress(host: String): Boolean {
        if (host.contains(':')) return true
        return host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))
    }
}

data class AcceptedRemoteLink(
    val url: String,
    val sid: String,
    val deviceName: String,
    val appVersion: String?,
    val protocolLabel: String,
) {
    override fun toString(): String =
        "AcceptedRemoteLink(sidLength=${sid.length}, protocol=$protocolLabel)"
}

sealed interface ParseResult {
    data class Ok(val link: AcceptedRemoteLink) : ParseResult
    data class Reject(val reason: RejectReason) : ParseResult
}

enum class RejectReason {
    EMPTY,
    NOT_HTTPS,
    UNSUPPORTED_HOST,
    NOT_REMOTE_LINK,
    MISSING_SESSION,
    CREDENTIALS_IN_URL,
    BAD_PORT,
    UNPARSEABLE,
}
