package com.zcodego.app.ui.settings

/** Settings copy for the remote site-data wipe. The saved session list stays. */
internal object RemoteSiteWipeCopy {
    const val ACTION = "清除远控站点数据"
    const val DETAIL = "清掉本应用里远控网站的 Cookie 和本地存储。不会删除已保存的会话列表。"
    const val CONFIRM_TITLE = "清除远控站点数据？"
    const val CONFIRM_BODY =
        "会清掉本应用 WebView 里 z.ai、bigmodel.cn 和 zhipuai.cn 的站点数据。不会删除已保存的会话列表。电脑上的 ZCode 不受影响。"
    const val DONE = "已清除远控站点数据。已保存的会话还在。"

    fun statesSessionsStay(text: String): Boolean = text.contains("不会删除已保存的会话列表")
}
