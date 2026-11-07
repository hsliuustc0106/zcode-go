# 待办

Origin 没有 Issues。这份清单是 zcode-go 的看板。开发流程见 [AGENTS.md](AGENTS.md)。

状态只用：`agent-ready`、`agent-working`、`done`、`wont`。优先级：P0 / P1 / P2。标签：`remote`、`local`、`security`、`build`、`ux`。`wont` 表示当前明确不做。Origin 不会读取这些状态，也没有合并队列。

非官方客户端，与智谱 / Z.ai 无隶属关系。手机只做控制界面，不在本机运行 ZCode 引擎，也不实现 SSH 或私有中继协议。

## 流程

一条待办 → 一条分支 → 一个 PR → `./gradlew testDebugUnitTest assembleDebug` 通过。更高风险的合并还要另一次会话里的 Reviewer 在同一个 40 位 SHA 上写 PASS。实现者只写 `Self-check:`，不写 `Verdict: PASS`。细节在 [AGENTS.md](AGENTS.md)，报告形状在 [.agents/skills/review-pr/SKILL.md](.agents/skills/review-pr/SKILL.md)。

0.2.1 及更早的改动是在这条流程之前直接进入 `main` 的。


## 1. 深链接与剪贴板接入

- 状态：done
- 优先级：P0
- 标签：remote
- 说明：接受 `https://zcode.z.ai/remote/…` 的 VIEW 链接，以及分享进来的纯文本。远程首页可以在用户点击后读取剪贴板。非法地址按原有白名单拒绝，不写入会话。
- 验收：VIEW / SEND 只取出文本；合法链接保存并打开 WebView；非法链接有中文错误，且不保存。

## 2. 最近会话与再次打开

- 状态：done
- 优先级：P0
- 标签：remote
- 说明：只有远程模式、开关打开、且上次 id 仍对应一条能通过校验的链接时才自动打开。失效 id 会清掉。深链接优先于自动打开。列表标出「上次打开」。同一 `sid` 的新链接仍覆盖旧凭证。
- 验收：单元测试覆盖本机模式、关闭开关、缺失 id、坏链接；深链接存在时不自动打开旧会话。

## 3. 前台保活与断线重连

- 状态：done
- 优先级：P0
- 标签：remote、ux
- 说明：远程页面在前台时显示一条低优先级常驻通知，离开页面即停止。这不是本地引擎。主框架加载失败、渲染进程消失或证书错误时给出「重新连接」。网络断开和恢复有横幅，恢复后由用户决定是否重载。
- 验收：通知文案不含链接；服务不导出；离开页面调用 stop。

## 4. 页面需要留意时的通知

- 状态：done
- 优先级：P1
- 标签：remote
- 说明：应用在后台时，如果官方页面的文档标题变了，发一条通知。标题里若包含 http(s) 链接则丢弃。这不是 Agent 状态回调，关于页写明限制。
- 验收：前台不通知；相同标题不重复；带 URL 的标题被丢弃。单元测试覆盖这些规则。

## 5. 多台电脑切换

- 状态：done
- 优先级：P1
- 标签：remote、ux
- 说明：远程页面顶栏可以打开已保存会话列表并切换。切换会更新「最近打开」，仍走同一套链接校验。
- 验收：当前会话标为「当前」；点其他会话替换当前页面，而不是再叠一层。

## 6. 远程页键盘

- 状态：done
- 优先级：P1
- 标签：ux
- 说明：WebView 可聚焦，内容区避开输入法。跟进条是应用自己的底栏，不替换页面里的输入区，见第 9 条。
- 验收：键盘弹出时页面和跟进条一起上移。

## 7. 仪器冒烟测试

- 状态：done
- 优先级：P2
- 标签：build
- 说明：`HostPickerSmokeTest` 检查首次打开能看到宿主选择。编译用 `assembleDebugAndroidTest`。真正点击需要设备或模拟器，本环境没有跑 `connectedDebugAndroidTest`。
- 验收：测试源存在，且 `assembleDebugAndroidTest` 能编过。

## 8. 持续集成

- 状态：done
- 优先级：P2
- 标签：build
- 说明：`.github/workflows/android.yml` 跑单元测试、debug APK 和仪器测试编译。Origin 不执行 GitHub Actions。没有镜像时，以本地 `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest` 为准。
- 验收：工作流文件存在，README 写明 Origin 不会自动跑它。

## 9. 浮动输入条

- 状态：done
- 优先级：P2
- 标签：ux、remote
- 说明：远程页底部有可收起的「跟进」条，在 WebView 外面。点「填入焦点」时，只往当前已经聚焦的 input、textarea 或 contenteditable 插入文字，并派发 input 事件。没有焦点就不动页面。不自动点击发送，不改页面结构，不注册 JavascriptInterface。官方编辑器若不吃 DOM 输入，用「复制」贴回去。
- 验收：脚本不含 innerHTML / document.write；引号、换行和 `</script>` 被转义；WebView 回传的 `"inserted"` 才算成功。单元测试覆盖这些。

## 10. 协议级 Agent 状态

- 状态：wont
- 优先级：P2
- 标签：remote
- 说明：当前不做。不做中继协议客户端，不实现 WebSocket 鉴权或 HMAC。标题通知只表示网页标题变了，不是「Agent 在等你」。
- 验收：代码里没有自建远控握手。若以后官方提供公开的状态接口，再另开一条。

## 11. 本机引擎、SSH、插件市场

- 状态：wont
- 优先级：P2
- 标签：local
- 说明：当前不做。手机端继续只做控制界面。不提供本机 Linux、SSH 表单或插件市场。执行仍在桌面端已经连上的机器上。
- 验收：本机路径说明限制，并引导去远程连接。没有新增运行环境。

## 12. 代理开发流程

- 状态：done
- 优先级：P2
- 标签：build
- 分支：`cursor/agents-process-8257`
- 说明：把「一条待办、一条分支、一个 PR、本地门禁、独立复核」写进 `AGENTS.md` 和 `.agents/skills/review-pr/SKILL.md`。不是缺陷修复，所以没有「撤掉就失败」的测试。Origin 不执行这份约定。压缩提交已在 `main` 的 `f9d5e775afa7df0ade93dcd7df6dedc8edd67f1f`。PR #1 在压缩之后关闭；Origin 的合并接口没能把该 PR 标成 merged。
- 验收：两份文档在仓库里；看板状态只用 `agent-ready` / `agent-working` / `done` / `wont`；实现者自检不是 PASS。

## 13. 安装包与试用路径

- 状态：done
- 优先级：P0
- 标签：build、ux
- 分支：`cursor/install-trial-8257`
- 说明：让人能立刻在真机试用。写出可复现的安装步骤；`assembleDebug` 必须绿；README 写清如何构建、APK 路径、`adb install`、首次打开选「远程」连官方远控。关于页显示 `versionName` 和 `versionCode`。不是缺陷修复，没有「撤掉就失败」的测试。压缩提交已在 `main` 的 `591ca625fc35a8c9d1a279fd6586c5c2ec559f26`。Origin 的合并接口没能把 PR #2 标成 merged。
- 验收：README 有安装步骤；`./gradlew testDebugUnitTest assembleDebug` 绿；若能产出 APK，把路径写进 PR；不要伪造本机引擎能力。

## 14. 会话自定义名称

- 状态：done
- 优先级：P1
- 标签：ux、remote
- 分支：`cursor/session-display-name-8257`
- 说明：多台电脑时，二维码带来的 deviceName 往往不好认。允许用户给已保存会话起/改一个本地显示名（不改 URL / sid）。列表、顶栏切换、删除确认都显示这个名字；清空自定义名则回到链接里的原名（或「未命名设备」）。不在日志/toString 里打印完整 URL。压缩提交已在 `main` 的 `8b255dae720e5edf069ce24098df221d3bfc4ccf`。Reviewer PASS 的 tip 是 `9e33417a8293043200180ce14bb36216eec7d813`。PR #3 在压缩之后关闭；Origin 的合并接口没有把该 PR 标成 merged。
- 验收：SessionRepository 有 rename/updateDisplayName；单元测试覆盖改名持久化、空名回退、删除会话；UI 可从会话行进入改名；`./gradlew testDebugUnitTest assembleDebug` 绿。

## 15. GitHub 安装包下载说明

- 状态：done
- 优先级：P0
- 标签：build、ux
- 分支：`cursor/github-install-8257`
- 说明：公开仓 https://github.com/hsliuustc0106/zcode-go 的 Releases 当前最新是 v0.2.2（`zcode-go-0.2.2-debug.apk`）。README「安装并试用」与关于页优先指向该页的最新 `zcode-go-*-debug.apk`，再写本地 gradle 构建。Origin PRIVATE 仍是开发源；GitHub 是安装包分发。不伪造本机引擎。压缩提交已在 `main` 的 `0daf94ce31eb7d348a455ace5c68c101afe4d70f`。Reviewer PASS 的 tip 是 `a167b6560ef30e0a54c7933ebed5fdb5d1850d53`。PR #4 在压缩之后关闭；Origin 的合并接口没有把该 PR 标成 merged。

## 16. 远控加载失败的扫码引导

- 状态：done
- 优先级：P1
- 标签：remote、ux
- 分支：`cursor/load-failure-scan-8257`
- 说明：FatalWebError.Load / 断线后除「重新连接」外，给出「重新扫码或粘贴」和「删除此会话」入口，文案说明桌面端刷新二维码后旧地址会失效。不实现协议级状态。压缩提交已在 `main` 的 `f11ecf93a0600e6989b0978a5e6e1d2aba61b663`。Reviewer PASS 的 tip 是 `47fdcc264897d1e0469591f399a86cb08dee7c0e`。PR #5 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 17. 断网恢复后自动尝试一次重连

- 状态：done
- 优先级：P1
- 标签：remote、ux
- 分支：`cursor/reconnect-once-8257`
- 说明：远程会话的网页正在显示加载失败或断线时，网络恢复后自动尝试一次重新连接，并保留手动「重新连接」。不要无限循环。若抽出纯策略，用单元测试锁住「恢复后只打一发」。不实现协议级 Agent 状态，不在手机上跑引擎。压缩提交已在 `main` 的 `1f84fd660d453af4ce12676e9eca79ad5ad526c1`。Reviewer PASS 的 tip 是 `73c65d7d4ec47b71a918183207c98663c8004173`。PR #6 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 18. 无效远程链接粘贴的明确错误

- 状态：done
- 优先级：P1
- 标签：remote、ux
- 分支：`cursor/reject-link-copy-8257`
- 说明：粘贴或分享进来的内容如果不是 `https://zcode.z.ai/remote/…`，用清楚的中文说明原因，并且不保存。若现有拒绝文案已经部分覆盖，只补强还不明确的情况。压缩提交已在 `main` 的 `021b48803d5eea10228fe9dda9140840e85d987d`。Reviewer PASS 的 tip 是 `6e3e3b6ce82e9c6aacd77720e1a102688fb6b83f`。PR #7 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 19. 删除会话时清理该会话 WebView 站点数据

- 状态：done
- 优先级：P2
- 标签：remote、security
- 分支：`cursor/session-site-data-8257`
- 说明：删除已保存会话时，尽力清掉该远控源站在 WebView 里的站点数据或 Cookie，避免稍后粘贴新二维码时被旧登录态干扰。写明做不到按单个会话精确隔离时的限制。不把 github.com 放进网页允许列表。压缩提交已在 `main` 的 `b8e08cf222b54c1194c8108867b14ffb003c3855`。Reviewer PASS 的 tip 是 `6296565df52df2a142174823e76851dc4bf743c8`。PR #8 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 20. 深色主题跟随系统

- 状态：done
- 优先级：P1
- 标签：ux
- 分支：`cursor/system-theme-8257`
- 说明：应用外观默认跟随系统浅色或深色。若 Material 3 动态配色已经好接，跟随系统时可以使用。设置里可以改成始终浅色或始终深色，但不作为默认。手机仍只是控制界面。压缩提交已在 `main` 的 `c58fcffb5851fd0683af25e47b6aabfbb9211f5c`。Reviewer PASS 的 tip 是 `47cc008fe412ba26a4a8322587ce8f45d86eecd5`。PR #9 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 21. 会话列表空状态扫码引导

- 状态：done
- 优先级：P1
- 标签：remote、ux
- 分支：`cursor/empty-session-cta-8257`
- 说明：远程模式下还没有已保存会话时，空状态给出「扫码或粘贴」入口和一句短说明。压缩提交已在 `main` 的 `ac66af4a9a8690b511a4b686848e26619117c6a8`。Reviewer PASS 的 tip 是 `171e52841e0f4edbf712fa1b3ac298f45fb6d44b`。PR #10 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 22. 关于页打开最新 Release

- 状态：done
- 优先级：P2
- 标签：ux
- 分支：`cursor/about-release-link-8257`
- 说明：关于页保留一个清楚的动作，用系统浏览器打开 https://github.com/hsliuustc0106/zcode-go/releases 。不要放进 WebView 允许列表。若入口已经存在，只核对文案是否一眼能懂。压缩提交已在 `main` 的 `928ede6d96ccf0b7aeabd6d8e975997bfac5be52`。Reviewer PASS 的 tip 是 `65c44c253d0c70e8b12db471e748a216e6bfc8f0`。PR #11 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 23. 远程页返回键先关跟进栏再退出

- 状态：done
- 优先级：P1
- 标签：remote、ux
- 分支：`cursor/followup-back-8257`
- 说明：远程页按系统返回键时，如果「跟进」栏是展开的，先把它收起；否则仍按现在的方式返回。不自动发送跟进文字。手机仍只是控制界面。压缩提交已在 `main` 的 `6824bcc4da05e3fa933820e273d45326a958d251`。Reviewer PASS 的 tip 是 `b62de7bc6a5d66ce09ffb459816d2305d6d2d49a`。PR #12 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 24. 会话切换后标题栏显示自定义名

- 状态：done
- 优先级：P1
- 标签：remote、ux
- 分支：`cursor/switcher-display-name-8257`
- 说明：多会话切换后，顶栏和「当前」标记使用用户设置的显示名；没有自定义名时回到链接里的原名。手机仍只是控制界面。压缩提交已在 `main` 的 `636ae8df650cf0bacce6863ef7345355a30b5d59`。Reviewer PASS 的 tip 是 `6cfd5d57de207e59e0a7f72ea440cee9cc073fb4`。PR #13 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 25. 设置里一键清除全部远控站点数据

- 状态：done
- 优先级：P2
- 标签：remote、security
- 分支：`cursor/clear-site-data-8257`
- 说明：设置里增加一个动作，确认后清掉远控源站在 WebView 里的站点数据。文案写明这不会删除已保存的会话列表。手机仍只是控制界面。压缩提交已在 `main` 的 `54c8e8197efc9945c95add552a5eaea2eb3b6027`。Reviewer PASS 的 tip 是 `1a747e47f0509b13f9f1d30ecf3845b198d1072d`。PR #14 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 26. 后台标题通知点击打开对应会话

- 状态：done
- 优先级：P1
- 标签：remote、ux
- 分支：`cursor/attention-session-8257`
- 说明：页面留意通知目前只打开应用，没有带上会话 id。发这条通知时把会话 id 放进 PendingIntent 的 extras；回到应用后，如果该会话还在，就打开它的远程页，否则保持现在的行为。通知正文不要放远程链接。手机仍只是控制界面。压缩提交已在 `main` 的 `f69965210430856accc5d207d341bcfbbd951462`。Reviewer PASS 的 tip 是 `0785eb58418541bbc27fce8fe1f01d5bbf88fda2`。PR #15 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 27. 远程页顶栏复制当前链接

- 状态：done
- 优先级：P1
- 标签：remote、ux
- 分支：`cursor/copy-session-link-8257`
- 说明：远程页顶栏增加一个动作，把当前会话的 HTTPS 远程链接复制到剪贴板，并用一条短 Snackbar 说明。不要自动分享。手机仍只是控制界面。压缩提交已在 `main` 的 `b2ed23c3665942b49696f7d3f81ad796f06549b3`。Reviewer PASS 的 tip 是 `6dcdde5bc91017a6ac5a7ff4fcbf5eb249a25c52`。PR #16 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 28. 切换会话时跟进草稿按会话隔离

- 状态：done
- 优先级：P2
- 标签：remote、ux
- 分支：`cursor/followup-draft-8257`
- 说明：跟进草稿按会话 id 分开保存，切换电脑时不要带出或清掉另一台的草稿。手机仍只是控制界面。压缩提交已在 `main` 的 `cfed7e0e057143fbe4d3eb8eb852d93ffe59d53b`。Reviewer PASS 的 tip 是 `1a14eb2c237d77ee7b0f9081a474cf1ee96993ad`。PR #17 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 29. 删除或清空会话时同步清除跟进草稿

- 状态：done
- 优先级：P1
- 标签：remote、ux
- 分支：`cursor/drop-followup-draft-8257`
- 说明：删除一条会话或清空全部会话时，一并丢掉该会话（或全部）的跟进草稿，避免会话已经不在后草稿还留着。手机仍只是控制界面。压缩提交已在 `main` 的 `ba762ea0a8a3998417a49e2b3210a8e5dfb5156a`。Reviewer PASS 的 tip 是 `6ebcc3312ca0e201cc3f6b0d7f3b17dd284e904b`。PR #18 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 30. 远程页顶栏点显示名可改名

- 状态：done
- 优先级：P1
- 标签：remote、ux
- 分支：`cursor/rename-from-title-8257`
- 说明：远程页顶栏点显示名，打开和列表里一样的改名对话框。保存后更新标题，不离开当前会话。手机仍只是控制界面。压缩提交已在 `main` 的 `036e4eb153d5bb8ed70a7e783a083e86191ca5c2`。Reviewer PASS 的 tip 是 `7be2f99d251f798ba410957b0a898e85b34550ce`。PR #19 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 31. 保活通知使用自定义显示名

- 状态：done
- 优先级：P2
- 标签：remote、ux
- 分支：`cursor/keepalive-display-name-8257`
- 说明：远程页前台保活通知优先用自定义显示名，没有再用链接里的原名，最后用现有的兜底文案。通知里不要放远程链接。手机仍只是控制界面。压缩提交已在 `main` 的 `d87b6e3182d095a9f21f9aa4d3172eec540ad15b`。Reviewer PASS 的 tip 是 `b6350b8967ef7d665a06d51b747ec7b847b94532`。PR #20 在压缩之后关闭；Origin 的合并接口拒绝了这个 token。

## 下一步建议

真机或模拟器上按这个顺序点一遍即可。没有设备时，单元测试和 `assembleDebug` 不能代替这些步骤。

1. 首次打开看到「ZCode 在哪里运行?」，选远程主机并在设置里改回本机。
2. 扫码或粘贴一条 `https://zcode.z.ai/remote/…`，确认页面打开；再从系统分享或浏览器打开同一类链接。
3. 杀掉应用后重新进入，确认只在远程模式且开关打开时回到上次仍有效的会话。
4. 远程页在前台时有保活通知，返回后通知消失。切到后台时，标题变化最多出现一条不含链接的通知。
5. 开飞行模式看断网横幅。恢复网络后远程会话会自动重连一次；没连上时仍可手动点重新连接，不会自己反复重试。
6. 展开「跟进」，先点页面输入框再填入，确认没有自动发送；没点到输入框时文字还在，复制可用。
7. 保存两台电脑后，从顶栏切换，当前一条标为「当前」。

