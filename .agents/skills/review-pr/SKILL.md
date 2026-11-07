---
name: review-pr
description: 按 Magpie 式复核 zcode-go 的一个 tip。只写 PASS、FAIL 或 INC，不授权合并。
---

# zcode-go review-pr

给 `hongxing-wang/zcode-go` 的本地 tip 或 Origin pull request 写复核报告。本技能不授权推送、批准或合并。

过程以 [AGENTS.md](../../../AGENTS.md) 为准。本文件只规定报告形状。普通产品改动不要改这个技能。只有 PASS / FAIL / INC、`Ran:` / `Not run:` 或「谁可以写 PASS」变了，才在同一次改动里改它。

Origin 没有 `review-gate`，也不校验签名。不要发明 `Signed-by`、`Nonce` 或 `Signature`。多出来的行不会被机器执行。

## 确认 tip

1. 仓库是 `hongxing-wang/zcode-go`（或当时约定的 fork）。
2. 记下完整 40 位 SHA：`git rev-parse HEAD` 或 PR head。短 SHA 不算。
3. 先读该 tip 上的 `AGENTS.md` 和 `ISSUES.md`，再看 diff。作者的叙述不是证据。
4. tip 移动后整份报告作废，从头再跑。

## 结论

| 结论 | 含义 |
|---|---|
| PASS | 这次范围内没有要改的问题；你声称的检查是在这个 SHA 上跑的，而且是绿的。 |
| FAIL | 有缺陷，或声称的修复没有撤掉就失败的测试，或报告不诚实。 |
| INC | 证据不够、该跑的没跑、tip 变了、或范围不清。INC 不是 PASS。 |

实现者不能写 PASS。实现者的自检第一行是 `Self-check:`，`Written-by: Implementer`。Reviewer 必须是另一次会话。实现者说「修好了」而复核仍看到问题，结论是 FAIL 或继续改，不能折中成 PASS。

行为、允许列表、WebView、会话存储：没有 Reviewer 的 PASS 就不要当成已复核。只改流程文档时，本地门禁绿即可，不必写 PASS。见 AGENTS.md 的风险表。

## 报告第一行

Reviewer 报告的第一行必须是：

```text
Verdict: PASS|FAIL|INC at <40 位 SHA>
Written-by: Reviewer
Ran:
  - <命令> → pass|fail
Not run:
  - <检查> — 原因
```

第一行之前不能有别的字。`Ran:` 至少一条真实命令。别人机器上、别的 SHA 上的结果不能算 Ran。

建议的本地命令：

```bash
./gradlew testDebugUnitTest assembleDebug
```

没有模拟器时，`connectedDebugAndroidTest` 写入 `Not run:`。仪器测试源的编译可以另记为 `assembleDebugAndroidTest`，但不能代替在设备上跑。

## 报告正文

1. 上面的结论块。
2. 发现（严重程度、路径、怎么触发、后果），或写明没有要改的问题。
3. 明确这不是合并许可。

不要替作者勾选清单，也不要改 PR 的草稿状态，除非人明确要求。
