package com.zcodego.app.ui.web

import android.webkit.CookieManager
import android.webkit.WebStorage
import com.zcodego.app.data.SessionSiteData

/**
 * Best-effort wipe of one site in this app's WebView. The platform stores data
 * per origin, and a page that is still open can write cookies back when it closes.
 */
internal object WebOriginCleaner {
    fun apply(plan: SessionSiteData.Plan) {
        plan.origins.forEach { origin ->
            runCatching { WebStorage.getInstance().deleteOrigin(origin) }
            runCatching {
                val cookies = CookieManager.getInstance()
                SessionSiteData.cookieNames(cookies.getCookie(origin)).forEach { name ->
                    cookies.setCookie(origin, "$name=; Max-Age=0; Path=/")
                }
                cookies.flush()
            }
        }
    }
}
