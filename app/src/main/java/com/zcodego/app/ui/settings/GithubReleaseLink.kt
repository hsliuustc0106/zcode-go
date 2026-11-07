package com.zcodego.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Public install page. Opened in the system browser, never in the in-app WebView. */
object GithubReleaseLink {
    const val RELEASES_URL = "https://github.com/hsliuustc0106/zcode-go/releases"

    fun browserIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

    fun open(context: Context): Boolean {
        return try {
            context.startActivity(browserIntent())
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
