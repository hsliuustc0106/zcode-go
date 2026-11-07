# zcode-go 代理开发流程

本文件把 ThinkFlowLab/FlyBot 的「一条待办 → 一条分支 → 一个 PR → 测试通过 → 独立复核」改写成这个仓库能做到的约定。不复制 FlyBot 的代码、门禁脚本或合并队列。复核报告的格式见 [.agents/skills/review-pr/SKILL.md](.agents/skills/review-pr/SKILL.md)。

非官方客户端，与智谱 / Z.ai 无隶属关系。

## Origin 能做和不能做

能做：

- 在 `ISSUES.md` 里编号记账。
- 从最新的 `main` 拉一条分支，开一个 Origin pull request。
- 本地跑 `./gradlew testDebugUnitTest assembleDebug`。有模拟器时再跑 `connectedDebugAndroidTest`。
- 人在 PR 评论里贴一份复核报告。新提交会使上一份 PASS 失效。

不能做，也不要假装能做：

- 没有 GitHub Issues，没有标签机器人，没有 `review-gate` / `merge-gate`。
- 没有合并队列。Origin 不会因为评论里写了 PASS 就自动合并。
- 没有签名校验。`Verdict:` 是文字，不是密码学证明。谁都能写一行 PASS，所以实现者禁止写 PASS。
- `.github/workflows/android.yml` 只在 GitHub 镜像上会跑。Origin 不执行它。

## 准入

只开这两类 PR：

1. 可复现的缺陷，并带一条在撤掉修复后会失败的测试。
2. 流程本身需要的改动（例如本文件、复核技能、看板状态）。

其余不单开 PR：纯外观打磨、新配置项、设想。写在 `ISSUES.md` 里，状态用 `wont` 或留着等有人明确要做。协议级 Agent 状态、本机引擎、SSH、插件市场已经是 `wont`（第 10、11 条）。

## 看板

`ISSUES.md` 是唯一看板。状态只用：

| 状态 | 含义 |
|---|---|
| `agent-ready` | 行为写清楚了，没有未决问题，可以开工。 |
| `agent-working` | 已有人认领，分支名写在该条下面。 |
| `done` | 对应改动已在 `main`。 |
| `wont` | 当前明确不做，并写了原因。 |

一条待办同时只属于一条分支、一个 PR。已有 PR 就往那条分支上推，不另开第二条。

认领时把该条改成 `agent-working`，并写分支名。停工且没有 PR：改回 `agent-ready`，写明原因。

## 管道

1. **一条待办。** 一次只做 `ISSUES.md` 里的一个编号。
2. **从刚拉到的 `main` 开分支。** `git fetch origin && git switch -c <branch> origin/main`。动手前 `git diff origin/main...HEAD --stat` 必须为空。基座旧了就删掉重开，不要手补幽灵文件。
3. **先写会失败的测试，再改代码**（只针对声称的缺陷）。流程文档和纯说明改动没有这种测试，要在 PR 里写明「不是缺陷修复」。
4. **本地门禁。** 在仓库根目录：

   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```

   有模拟器时加上 `connectedDebugAndroidTest`。没有就写进 `Not run:`。不要绕过失败的测试。
5. **PR 指向 `main`。** 标题用祈使句。正文写待办编号、在哪个 40 位 SHA 上跑了什么、没跑什么。实现者可以附自检，但第一行必须是 `Self-check:`，不能是 `Verdict: PASS`。
6. **复核。** 更高风险的合并需要另一次会话里的 Reviewer，按复核技能对**同一个 40 位 SHA** 写报告。实现者的自检不是 PASS。新提交让上一份 PASS 作废。
7. **合并。** 由人在 Origin 上合并。没有自动队列。文档类改动在本地门禁通过后可以由人直接合并。行为、允许列表、WebView、会话存储改动，在合并前要有 Reviewer 的 PASS。
8. **停。** 安全敏感、范围不清、或本地门禁无法变绿时停下来，不要改保护规则，不要强推 `main`。

云环境如果只能先落到 `main` 上的草稿，提交仍按「一条待办一次提交」来写，并在提交说明里附自检。事后应补 PR，或在 `ISSUES.md` 写明为什么没有 PR。0.2.1 及更早的提交发生在本流程之前，当时直接进了 `main`。

## 风险（人工判断，没有脚本）

| 范围 | 合并前 |
|---|---|
| 只改 `AGENTS.md`、`ISSUES.md`、`CHANGELOG.md`、README、复核技能 | 本地门禁绿即可。不需要 PASS。 |
| 链接校验、WebView、会话存储、通知、深链接 | 本地门禁绿，并且 Reviewer 在该 SHA 上 PASS。 |

没有 `risk-gate`。上表是给人看的，Origin 不会执行。

## 复核报告

Reviewer 的报告第一行只能是：

```text
Verdict: PASS|FAIL|INC at <40 位 SHA>
```

接着：

```text
Written-by: Reviewer
Ran:
  - <命令> → pass|fail
Not run:
  - <没跑的检查> — 原因
```

| 结论 | 含义 |
|---|---|
| PASS | 该 SHA 上 Reviewer 自己跑过的检查是绿的，没有要改的问题。 |
| FAIL | 有可复现的缺陷，或声称的修复没有「撤掉就失败」的测试。 |
| INC | 证据不够、SHA 变了、或该跑的没跑。INC 不是 PASS。 |

实现者禁止写 `Verdict: PASS`。自检用 `Self-check:`，`Written-by: Implementer`。一份 PASS 只覆盖报告里的那一个完整 SHA。之后任何新提交都使它失效。

这份约定不授予合并权。
