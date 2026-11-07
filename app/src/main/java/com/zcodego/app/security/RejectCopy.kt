package com.zcodego.app.security

/**
 * User-facing Chinese for a rejected paste or share. The text never includes the
 * entered URL. Every rejection says the link was not saved.
 */
internal object RejectCopy {
    const val REQUIRED = "https://zcode.z.ai/remote/…"

    fun explain(reason: RejectReason): String = when (reason) {
        RejectReason.EMPTY ->
            "请粘贴电脑上「移动端远程控制」给出的地址，形如 $REQUIRED。这次没有保存。"
        RejectReason.NOT_HTTPS ->
            "只接受 HTTPS。明文或其它协议已拒绝，没有保存。请改用 $REQUIRED。"
        RejectReason.UNSUPPORTED_HOST ->
            "这不是 zcode.z.ai 上的远程控制链接，没有保存。请粘贴 $REQUIRED。"
        RejectReason.NOT_REMOTE_LINK ->
            "这个链接不在 /remote/… 路径上，没有保存。请使用电脑上给出的 $REQUIRED。"
        RejectReason.MISSING_SESSION ->
            "地址里缺少会话参数，无法连接，没有保存。请换一条完整的 $REQUIRED。"
        RejectReason.CREDENTIALS_IN_URL ->
            "链接里不能带账号或密码，没有保存。请改用电脑上给出的 $REQUIRED。"
        RejectReason.BAD_PORT ->
            "只接受标准 HTTPS 端口，没有保存。请使用 $REQUIRED。"
        RejectReason.UNPARSEABLE ->
            "无法识别成远程控制链接，没有保存。请粘贴完整的 $REQUIRED。"
    }
}

/** A link is stored only after it parses as an official remote URL. */
internal object LinkDecision {
    fun canSave(result: ParseResult): Boolean = result is ParseResult.Ok
}
