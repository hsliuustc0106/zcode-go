package com.zcodego.app.ui.web

import androidx.annotation.StringRes
import com.zcodego.app.R

/**
 * Fixed official entry points. These are not user-supplied URLs.
 */
enum class OfficialDestination(
    val route: String,
    val url: String,
    @param:StringRes val titleRes: Int,
) {
    ZCODE("zcode", "https://zcode.z.ai/", R.string.dest_zcode),
    BIGMODEL("bigmodel", "https://open.bigmodel.cn/", R.string.dest_bigmodel),
    DOCS("docs", "https://zcode.z.ai/cn/docs/remote-control", R.string.dest_docs),
    ;

    companion object {
        fun fromRoute(value: String?): OfficialDestination? =
            entries.firstOrNull { it.route == value }
    }
}
