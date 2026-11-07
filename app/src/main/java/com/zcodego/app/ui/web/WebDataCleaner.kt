package com.zcodego.app.ui.web

import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebStorage

internal object WebDataCleaner {
    fun clear(onDone: () -> Unit) {
        val complete = {
            runCatching { WebStorage.getInstance().deleteAllData() }
            Handler(Looper.getMainLooper()).post(onDone)
        }
        runCatching {
            val cookies = CookieManager.getInstance()
            cookies.removeAllCookies {
                cookies.flush()
                complete()
            }
        }.onFailure {
            complete()
        }
    }
}
