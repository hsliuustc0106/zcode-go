package com.zcodego.app.data

import com.zcodego.app.security.RemoteUrlPolicy

/**
 * Origins whose WebView site data a settings wipe may clear.
 * Saved session URLs only contribute origins. The session list itself is not deleted.
 * The in-app WebView only navigates to these official hosts, so clearing its
 * cookie jar and WebStorage clears remote site data and nothing else.
 */
object RemoteSiteWipe {
    private val rootOrigins = listOf(
        "https://z.ai",
        "https://zcode.z.ai",
        "https://passport.z.ai",
        "https://bigmodel.cn",
        "https://open.bigmodel.cn",
        "https://www.bigmodel.cn",
        "https://zhipuai.cn",
        "https://open.zhipuai.cn",
    )

    fun plan(sessionUrls: List<String>): SessionSiteData.Plan {
        val extra = sessionUrls.mapNotNull { url ->
            val origin = SessionSiteData.originOf(url) ?: return@mapNotNull null
            val host = origin.removePrefix("https://").substringBefore(':')
            if (RemoteUrlPolicy.isOfficialHost(host)) origin else null
        }
        return SessionSiteData.Plan((rootOrigins + extra).distinct())
    }
}
