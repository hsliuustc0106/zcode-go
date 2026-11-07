# zcode-go

当前版本 **0.2.20**。变更记录见 [CHANGELOG.md](CHANGELOG.md)。开发流程见 [AGENTS.md](AGENTS.md)。

非官方 Android 客户端，用来在手机上打开智谱 ZCode 的远程控制页面。交互参考「先选运行位置，再继续」这一类首次引导，但界面、文案和图标都是为本项目单独写的。

手机端只是控制界面。代码和命令仍在桌面端已经连上的那台机器上执行。本应用不会在手机上假装有一套 Linux 或 Agent 运行环境。

**非官方客户端，与智谱 / Z.ai 无隶属关系。** ZCode、智谱、Z.ai、BigModel 及其标识归各自权利人所有。

## 它能做什么

1. 首次打开会问「ZCode 在哪里运行?」。之后可以在设置里改。
2. **远程主机**：扫描电脑上的二维码，或粘贴连接地址，然后在应用内打开官方远程页面。最近的会话保存在本机，可以再次打开；默认会在下次启动时回到上次的会话。
3. **本机**：说明完整引擎还不能在手机上运行，并提供 ZCode 与 BigModel 官方页面入口，以及已保存的远程会话。
4. 设置里可以改运行位置、主题、是否自动打开上次会话，也可以清除会话、网页登录状态，或只清远控站点数据而保留已保存的会话列表。

远程链接的公开形态一般是：

- `https://zcode.z.ai/remote/v3?sid=…`
- `https://zcode.z.ai/remote/v4?sid=…`

v3 链接里常见 `hash` 和 `t`，v4 链接常常没有这两项。应用只检查这是不是 `zcode.z.ai` 上的 `/remote/…` 链接，然后把官方页面交给 WebView。它不自己实现中继协议。

## 和桌面端一起用

1. 在电脑上打开 ZCode，并保持窗口运行、网络可用。
2. 点左下角侧栏的手机图标，打开「移动端远程控制」。
3. 在 zcode-go 里选择「远程主机」，扫描弹窗里的二维码，或复制连接地址后粘贴。
4. 手机上看到的是这个桌面窗口里的工作区、任务和会话。你发出的指令，由桌面端那条已经建好的通路执行（本机、SSH、WSL 或 Docker，取决于桌面端当初怎么连）。
5. 用完后在电脑上点「停止」。关掉弹窗并不会停止远控。
6. 如果地址可能已经外泄，在桌面端刷新二维码。旧地址会立刻失效，手机需要重新扫描。

同一时间官方页面通常只支持一个手机连接。不要把连接地址发到聊天软件或公开场合，它相当于一把临时钥匙。

官方说明：<https://zcode.z.ai/cn/docs/remote-control>

## 安全

- 只接受 HTTPS。明文 `http` 会被拒绝，网络安全配置也禁止明文。
- 配对地址必须是 `zcode.z.ai` 上的 `/remote/…`，并且带有 `sid`。
- WebView 里随后的页面跳转只允许 `z.ai`、`bigmodel.cn`、`zhipuai.cn`（含子域名）。其他主框架和子框架地址会被取消。
- 证书错误直接取消，不能选择继续。不信任用户自行安装的 CA。
- 不注册 JavascriptInterface，网页不能调用应用里的代码。
- 链接存在应用私有目录，并用 Android Keystore 加密。`allowBackup` 为关闭，云备份和设备迁移规则也会排除这些数据。
- 界面和日志不会打印完整链接。列表里只显示设备名、协议标记和大致时间。
- 本应用没有自己的服务器，不会把链接上传到别处。登录态如果出现，也只留在这个应用的 WebView 里。
- 清除「网页登录状态」会删掉本应用里的 Cookie 和网页存储，不影响电脑上的 ZCode。
- 「清除远控站点数据」会清掉本应用 WebView 里 z.ai、bigmodel.cn、zhipuai.cn 的站点数据。不会删除已保存的会话列表。远程页面如果还开着，关掉时仍可能把 Cookie 写回。
- 删除一条会话时，只有当没有别的已保存链接还指向同一网站，才会尽力清掉该网站在本应用 WebView 里的 Cookie 和本地存储。做不到按一条链接或 sid 分开。同一网站还留着别的会话时不清，避免把另一台电脑的登录一起清掉。z.ai、bigmodel.cn 上的登录不在这条链接的源站里，不会被这次删除清掉。页面还开着时，关掉页面仍可能把 Cookie 写回。github.com 不在网页允许列表里。

Keystore 初始化失败时，应用不会改用明文保存链接。

## 安装并试用

当前应用版本 **0.2.20**（versionCode 22）。手机需要 Android 8.0（API 26）或更高。

开发源是 Cursor Origin 上的私有仓库 `hongxing-wang/zcode-go`。GitHub 公开仓 [hsliuustc0106/zcode-go](https://github.com/hsliuustc0106/zcode-go) 用来分发安装包，也是源码镜像。日常开发仍在 Origin，不在 GitHub。

### 从 GitHub 下载（优先）

打开 [Releases](https://github.com/hsliuustc0106/zcode-go/releases)，下载文件名是 `zcode-go-*-debug.apk` 的**最新**调试包，然后：

```bash
adb install -r zcode-go-*-debug.apk
```

当前最新已发布的安装包是 [v0.2.20](https://github.com/hsliuustc0106/zcode-go/releases/tag/v0.2.20)，文件名 `zcode-go-0.2.20-debug.apk`。以后仍以下载页上最新的 `zcode-go-*-debug.apk` 为准。这个包不会在手机上运行 ZCode 引擎。

应用里：设置 → 关于 → **查看最新 Release**。它用系统浏览器打开上面的 Releases 页。`github.com` 不在应用内网页的允许列表里。

### 在本机构建（备选）

电脑需要 JDK 17 或更高，以及 Android SDK `compileSdk 37`、Build-Tools `36` 或 `37`。

1. 安装命令行工具后，准备 SDK（路径按本机修改）：

   ```bash
   export ANDROID_HOME="$HOME/Android/Sdk"
   export PATH="$ANDROID_HOME/platform-tools:$PATH"
   sdkmanager "platforms;android-37.0" "build-tools;37.0.0" "platform-tools"
   ```

   `local.properties` 里的 `sdk.dir` 不要提交。

2. 在仓库根目录构建并跑单元测试：

   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```

3. 调试 APK 路径：

   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

4. 手机打开 USB 调试后安装：

   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

无论从哪边装上，首次打开都会问「ZCode 在哪里运行?」。选 **远程主机**，点 **继续**，再点 **去连接**。在电脑 ZCode 左下角打开「移动端远程控制」，扫二维码或粘贴 `https://zcode.z.ai/remote/…`。本机选项不会在手机上运行引擎。设置 → 关于里可以看到版本名和 versionCode、非官方声明，以及「查看最新 Release」。

没有装过命令行工具时，从 Android 开发者站点下载 command-line tools，再执行上面的 `sdkmanager`。

单元测试不需要设备或模拟器。仪器测试 `connectedDebugAndroidTest` 需要真机或模拟器；`assembleDebugAndroidTest` 只编译测试包。

持续集成见 [ISSUES.md](ISSUES.md)。Origin 目前不会执行 GitHub Actions。仓库里的 `.github/workflows/android.yml` 留给镜像到 GitHub 时使用。在本机或镜像上的检查命令是：

```bash
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest
```

技术栈：Kotlin、Jetpack Compose、Material 3、CameraX、ZXing、AndroidX WebView、DataStore、EncryptedSharedPreferences。`minSdk` 26。单模块，按包分成 onboarding、remote、local、settings、web、security 和 data。

可以从系统分享或 `https://zcode.z.ai/remote/…` 链接直接打开应用。远程页顶栏点显示名可以改名，保存后标题更新，不离开当前会话。顶栏也可以复制当前这条远程链接，只写入剪贴板，不会自动分享。远程页面打开时会有一条前台通知，用来降低进程被系统收回的概率；离开页面后通知停止。这条通知优先用自定义显示名，没有再用链接里的原名，最后用「未命名设备」，正文不含远程链接。点它会打开当时的会话；会话已经不在时，仍按原来的方式进入应用。后台的「页面有变化」只看网页标题，不是 Agent 状态接口。标题仍是固定的「远程页面有变化」，正文是净化后的页面标题，并带上那台电脑的显示名。点那条标题通知会打开当时的会话；会话已经删掉时，仍回到当前页面。通知标题和正文都不含远程链接。

远程页底部的「跟进」条在页面外面。它只在你点「填入焦点」时，把文字插入当前已经聚焦的输入框，不会自动发送，也不会改页面结构。草稿按会话分开，切换电脑不会带出或清掉另一台的草稿。删除一条会话或清空全部会话时，对应的草稿一并丢掉。官方编辑器若不接收这种输入，用「复制」。协议级 Agent 状态、本机引擎、SSH 和插件市场当前不做，见 [ISSUES.md](ISSUES.md) 第 10、11 条。

## 常见问题

- **证书错误**：不要忽略。对一下系统时间，或换一个网络。本应用故意不信任用户安装的证书。
- **链接打不开**：桌面端 ZCode 需要保持运行并联网。地址过期时，在电脑上刷新二维码再扫一次。同一个 `sid` 的新链接会更新手机上保存的那一条。
- **扫出来提示不是远程地址**：确认扫的是「移动端远程控制」弹窗里的码，而不是别的二维码。
- **登录跳到了被拦截的域名**：页面只会留在 `z.ai`、`bigmodel.cn`、`zhipuai.cn`。如果官方登录改用了别的域名，需要改允许列表，而不是在应用里临时放行。
- **跟进条没有填进去**：先在官方页面里点一下输入框，再点「填入焦点」。条不会自动发送。若页面不用普通输入框，点「复制」后贴到页面里。

## 许可

本仓库以 [MIT 许可证](LICENSE) 授权。应用「关于」页也有同样的声明和许可证文本。

AndroidX、CameraX、ZXing、Tink 等依赖使用它们各自的许可证（多为 Apache-2.0）。本项目没有复制其他客户端的代码、资源或名称。
