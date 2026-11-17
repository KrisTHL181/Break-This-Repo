# 会话交接：HDSL-web（2026-10-27）

> 本稿接住 **v0.5.92**：面板自带的第一方终端（进程内 / ACP）从"能开"变成"能日常用"，同时面板开始
> 按**安装包摘要**判断"实例里装的是不是这一版"。前两稿仍然有效：
> [`2026-10-25`](session-handoff-2026-10-25.md)（实例生命周期 ↔ 终端、反代、整合包、部署）、
> [`2026-10-26`](session-handoff-2026-10-26.md)（第一方终端本体、三轮审查、两个 profile 的账户交接）。
> 本稿只写它之后的增量、判掉过的结论，和下次不用再摸一遍的事实。
>
> **隐私约定（继续遵守）**：真实域名、内网地址、宿主机路径、口令一律不写进仓库。

## 0. 当前状态

- `main` = **v0.6.0**（tag `v0.6.0` → `a04044c4`；远端 `web-shell` 已与 `main` 同指 `fac7023`）。这条线：
  v0.5.92（第一方终端补完）→ v0.5.93（光标对齐）→ v0.5.94（开发者守卫 + 紧急停机，见 §5.1）→
  v0.5.95（需要开发者密码时弹框 + 安全文档，见 §5.3）→ v0.6.0（Octop 品牌 + 品牌列表窄屏 +
  `/i/*` Host 头 P0）。`v0.6.0` 之后 `main` 又多三个 docs/test-only 提交（「从零重建」提示词与它的
  「只给空仓库用」警示、取消类测试竞态修复），不影响运行时，所以 jar 没有重发。
- 两个第一方插件版本 **0.2.1**（随 jar 分发、从未上 npm；改版本要同时改 `package.json` 与
  `DshTuiPlugins.ENTRIES`）。内容变了而版本没变也没关系：面板比的是摘要，会亮「有新版本」。
  所以 v0.5.93 的插件 tarball 虽然摘要变了，版本号仍是 0.2.1——重新构建镜像后实例页会提示更新。
- **Octop 品牌（v0.6.0 起随面板发布；人工验收仍不完整）**：`cfd7c0b`/`99a8852` 接入 portable 包与本地源码
  编译兜底，`1d63069`/`49642e5` 按审计修了半安装自愈、固定路由接入口、`octop init` 真超时、重装保护、
  停用实例可停、uv 平台 triple 与成本披露。**仍需人工过一遍**：真实浏览器里的 PWA/Service Worker、
  WebSocket 长连接、安装进度与取消、源码构建的失败恢复、手机上的品牌实例列表（v0.6.0 只改了样式）
  都还没有人肉证据。
- 测试基线：后端 **859 用例（2 skipped）**、插件 **进程内 59 / ACP 105**，前端 `typecheck` + `build`
  + `compat` 绿（`fac7023` 上实测）。
- **别在明文 HTTP 上放开开发者模式**：试过一版"警告不拦"，当天撤回。没有 TLS 时"提权"就是明文
  传密码，给一把假锁比不给更糟；现在与 v0.5.94 一致——HTTP 下开发者页只给提示，终端 403
  `insecure_transport`，提权/守卫 409/403 `tls_required`，`/i/*` 的提权页说明"开发者模式不可用"。
- **版本号的一段弯路（结论已更新）**：v0.5.92 之前曾按 v0.6.0 发过一次 Release 并撤回（删 Release、
  删 tag、`main` 强推回 `cf49605`），那一次的 `v0.6.0` 确实没有交付，随后以 **v0.5.92** 发布。
  **但 2026-10-28 的 `v0.6.0` 是正式发布**（tag `v0.6.0` → `a04044c4`，Octop 品牌随此版），
  CHANGELOG 里的 v0.6.0 小节合法、别再删。tag 名是 `v0.6.0`，不存在 `v0.6.00`。
- **分支血缘曾分叉（已收敛）**：v0.6.0 之后那三个提交推送时 git 端点（https 443）长时间不通，改用
  Git API 重放，远端 `web-shell` 因此拿到一套新 sha（`5c31a8c` 等），内容与 `main` 的 `fac7023`
  相同但历史不同（compare 显示 `diverged`）。2026-10-28 已把远端 `web-shell` 强制对齐到 `fac7023`，
  现在两边 `identical`。**以后 push 超时优先重试、换协议（`http.version=HTTP/1.1`）或换网络，
  别用 API 重放提交**——会造出平行历史，下次合并撞「patch 已应用」。

## 1. 这一版改了什么

### 1.1 审批：默认变严格，四个选项

- 三档在 `plugins/shared/approval/policy.js` 的纯函数 `decide()` 里：`ask`（**默认**，全确认）·
  `critical`（关键确认）· `full`（全自动）。harness 自己的 `ApprovalPolicy` 只有"问"和"一律拒绝"，
  所以"不问就执行"只能是终端自己的决定；自动放行**仍然写审计、也不放宽沙箱**。
- 敏感文件（`.env*`、`.ssh/`、`id_rsa`、`.npmrc`、`.netrc`、`*.pem`、`printenv`/裸 `env`）**任何档位都问**；
  `web_fetch` 不再算只读（URL 由模型拼）。路径先 resolve 再判断，`..` 不能绕过工作区。
- 白名单键是"工具 + 完整命令/路径"，`git status` 不会顺带允许 `git push`。永久规则在
  **`$DSH_HOME/.hdsl-tui/permissions.json`**（不是 `~`），`/allow` `/deny` `/rules [clear]`，deny 优先。
- 审批对话框四个选项，数字键 1-4：1 运行 · 2 当前会话默认运行 · 3 拒绝 · 4 拒绝并说明原因（理由作为
  用户消息交给模型；ACP 不能并发 prompt，排队到回合结束再发）。

### 1.2 命令、补全、输入

- 进程内把 `/permission` `/plan` `/goal` `/feedback` `/export` `/compact` 交给
  `ctx.commands.execute`，结果原样显示；未知命令先问 harness。ACP 没有命令运行时会明确说"这条线跑不了"。
- **Tab 以前根本没到应用**：`plugins/shared/term/keys.js` 的解码表没有 `0x09`，`\t` 落进"未知控制字符"
  被丢掉。补全代码一直是好的（`plugins/shared/ui/complete.js`：命令 / 参数 / `@` 工作区路径，重复 Tab 循环）。
- `/` 命令菜单是**输入框下方的内联列表**（编号 + 描述、↑↓/数字/enter/esc、`↑ N more` 窗口），不是居中
  overlay；提示行也在输入框下面。
- `ctrl+j` 换行、`ctrl+s` 插话（进程内 `agent.steer()`，ACP 留在队列）、`shift+tab` 与 `/plan` 切 Plan
  mode；`/plan` 进入时发**裸 `/plan`**——带 `on` 会被 harness 当成消息 steer 给模型。
- `/yolo`、`/permission` 无参是列表选择（当前项标「当前」），不是"点一下换一档"。
- `/provider`（仅进程内）：写 `llm-pi-ai` 的 settings 段（与网页「模型」页同一份，用 `mutate` 按路径写，
  整段替换会清掉已 redact 的 key），密钥走 `credentials.set`，`--key` 不回显不落盘，`--yes` 才写。

### 1.3 渲染与中文输入

- `Screen#paint` 在整帧和光标都没变时**一个字节都不写**，兜底全量重绘降到每 2 秒——浏览器终端每次写入
  都会重排 xterm 的输入法辅助框，这是"中文被吞"的一半原因。
- 面板 xterm 字体栈补 CJK 等宽（Sarasa Mono SC / Noto Sans Mono CJK / Source Han Mono / 雅黑等宽 / 文泉驿）。
- **IME 守卫**（`web/src/components/TerminalWindow.tsx`）：composition 期间缓冲 PTY 输出、结束后再刷；
  在 keyup/compositionend 之后延迟 50ms 清隐藏 textarea（`screenReaderMode` 除外，那模式故意保留内容）。
  对应两条上游缺陷：composition 期间写屏会重定位候选框；textarea 残留让 229 键把旧输入整段重发
  （xtermjs/xterm.js#6078）。
- 状态栏会话号用 `shortId()`（harness 的 id 是 `session-<hex>`，直接截前 6 位会显示成 `#sessio`）。

### 1.4 `/sessions` 的数据模型（下次别再摸一遍）

harness 的 `sessionPersistence` 只有这些事实，其余都得自己推：

- `list()` 返回 `{header}`（新后端 `listSnapshots()` 是 `{header, revision, sizeBytes}`）。header 字段：
  `id / cwd / createdAt / parentSession / origin / delegationDepth / seedLength / agentPreset`——
  **没有 `updatedAt`**。所以"按最后活动排序"不能用 header，只能用日志文件 mtime。
- 日志布局 `<root>/<workspace>/<sessionId>/session[.vN].jsonl[.zstd]`；一个目录会同时留着多代，
  要取**最新一代**（按 mtime 取最大，不要按 `readdir` 顺序取最后一个——`v10` 的字母序在 `v2` 前面）。
- 根的顺序：`DSH_TUI_SESSION_ROOT` → `$DSH_HOME/sessions` → `~/.dsh/sessions` → `~/.dsh-tui/sessions`。
- 读日志用 `open(id, 'read')` + `read(offset, length)`，**offset/length 是事件条数**；短读不算 EOF，
  只有空切片才算读到尾。
- 首条人类提示：`user/message` 且 `source.kind === 'user'`；但 `source` 缺失/null 的老日志也算人类，
  提示也可能先以 `agent/inbox/spliced`（`inserted[]`）落盘，`content` 可能是数组也可能是字符串。
- **判据纪律**：读不出、或扫到 2048 条上限还没找到提示 → **保留一行**并标「标题未读出」；只有确实读到
  日志结尾且没有人类提示才过滤。删掉一个真实会话比多显示一行哈希严重得多（上游 `digest.ts` 同款纪律）。
- 代价：一次 `/sessions` 最多 200 个会话、8 个并发句柄。首条提示永不改变，所以按 persistence 服务缓存在
  进程内（`WeakMap`），空会话与读不出的每次重查。
- dsh 面板侧还有个 `sessionListMetadata` 投影 `{ blank, lastPromptAt }`（harness 自己的网页侧栏用的），
  插件里拿不到，别指望它。

### 1.5 面板侧：装着的不是这一版

- `GET …/tui-plugins` 每行多一个 `outdated`：第一方插件比**安装包摘要**（安装成功时记进
  `<实例 home>/.hdsl-web/first-party-installs.json`），社区插件比清单锁定的版本。
- 没有记录的安装 = `outdated`，页面标「有新版本」并给「更新」（同一条 `file:` tarball 安装路由）；
  卸载清记录，重装写记录，按实例 home 隔离。
- `filesPresent` 分开返回：整个包目录缺失、bin 型入口缺失、纯 bundle 型包目录为空，都算"安装未完成"，
  页面给「重新安装」+「卸载」而不是让用户对着闪屏猜。

## 2. 关键文件

```
plugins/shared/approval/policy.js     三档判定 + 危险规则 + 敏感文件 + 白名单 key（纯函数）
plugins/shared/ui/complete.js         token 切分、候选、循环
plugins/shared/ui/dialogs.js         选项编号、审批详情翻页、理由输入行
plugins/shared/ui/render.js          状态栏、提示行、内联命令菜单、候选行
plugins/shared/term/keys.js          按键解码（Tab=0x09 这条就是它的锅）
plugins/dsh-inproc-tui/src/agent.js  ChatSession：会话索引（mtime / 首条提示 / 缓存）
plugins/dsh-inproc-tui/src/app.js    审批、命令委托、补全、会话切换、/provider
plugins/dsh-acp-tui/src/ui/app.js    ACP 侧的降级实现（四选项、理由排队、切会话关旧会话）
web/src/components/TerminalWindow.tsx  xterm 实例、IME 守卫、CJK 字体栈
web/src/components/TuiPluginSection.tsx 插件安装状态 /「有新版本」/「更新」
src/main/java/…/dsh/FirstPartyPlugins.java  摘要记录、isOutdated、tarball 清单
```

## 3. 审阅里被现场否证 / 订正过的结论

- **"品牌 agent 零输出是因为 PTY 没有 `TIOCSCTTY`"——错。** 现场日志里 kimi 的 TUI 在没有控制终端的
  PTY 下连续跑了 12 分 45 秒、重连回放 262144 字节、再跑 21 分钟。真因是**插件包目录为空**
  （`node_modules/.pnpm` 条目 0、`dependencies` 空），面板却当作安装成功 → 现在由 `filesPresent` 拦。
- **"Tab 补全坏了"——不是补全的锅**，是 `0x09` 从没进解码表。
- **"会话列表乱、只有哈希"**——header 没有 `updatedAt`（排序退化到 createdAt），标签只用了 `shortId`。
- 上游代价数据：旧 picker 每次打开都把最近 20 个日志的全部帧解压一遍，31MB 历史要 **3.9 秒**；
  现在的做法是 header + 只读变更部分 + 后台补标题。我们没有做到那一步，靠"首条提示缓存 + 200 条上限"。

## 4. 还没验证的

- **真机中文输入法**：IME 守卫只在代码层成立，需要在浏览器里开着一个流式回合敲拼音验证。
- `/provider` 写完 settings 是否**热生效**（`applies: 'live'`）；不生效就补一句"需要重开终端"。
- `/sessions` 的真实观感：需要重建镜像 → 实例页点「更新」拿到 0.2.1 → 看顺序与标题。
- ACP 的理由排队在真实慢回合下的顺序；`@` 补全在真实工作区的性能。

## 5. 发布动作：v0.5.92 → v0.5.94（本日共三次）

```bash
git checkout main && git merge --no-ff web-shell
flock /tmp/hdsl-gradle.lock ./gradlew --no-daemon shadowJar -PreleaseVersion=<版本>
sha256sum build/libs/hdsl-web-<版本>.jar > SHA256SUMS.txt
git tag -a v<版本> -m "…"
git -c http.version=HTTP/1.1 push origin main web-shell v<版本>
gh release create v<版本> …   # 正文写面向用户的介绍，不抄 CHANGELOG
```

**`Dockerfile` 的白名单 `COPY plugins ./plugins` 与 `first-party-plugins/*.tgz` 断言别动**——
v0.5.91 第一次发布就是在这里漏掉的：镜像里的 jar 没有插件，而 CI 全绿。

### 5.1 v0.5.94：开发者守卫与紧急停机（新增，看这节）

- **守卫**：`developer_guard.scope = off|proxy|instance|all`（`HDSL_DEVELOPER_GUARD`），默认 `off`。
  实现在 `web/developer/DeveloperGuardFilter`（挂在 `AuthFilter` 之后的 `/api/*` 与 `/i/*`）与
  `HdslServer.createRelay`（WebSocket 升级不过 servlet 过滤器，必须单独判一次——这个坑踩过一次）。
  **只挡新请求**：已加载的页面、已附着的终端、正在跑的 agent 都不动。
- **提权 token**：`POST /api/developer/elevate` 多返回一个 `token`（`DeveloperElevation.mintToken()`，
  内存里的 `bearer:` 前缀 key），守卫与 WS 都认 `Authorization: Bearer`。这条是给宿主机脚本/巡检
  agent 的正门，不是后门：同 TTL、同滑动、改密码即清空。
- **E-Stop**：`web/emergency/EmergencyStop`，**只在内存**（重启即解除，这是设计选择）。顺序是
  "先翻转状态 → `TerminalManager.terminateAll()` → `DshProcessManager.killAllForcibly()`"
  （后者走 `DshProcess.killNow()`：先收集 descendants 再 `destroyForcibly`，**不跑 `post-exit`**）。
  冷却 1 小时，未到点 `409 estop_cooldown`。
- **冻结范围**：`/i/*`、实例的写操作、终端接口 → `503 emergency_stop`；登录、健康检查、
  `/api/developer/*`、只读列表/日志、`/ws` 照常。
- **Windows**：实例进程的 kill 两平台一致；**桌面终端窗口不在面板进程表里，杀不掉**，UI 与文档都写明。
- **验证纪律**：E-Stop 只能在独立起的临时面板（临时 `HDSL_DATA`、`HDSL_PORT=0`）上试。
  生产面板里往往正跑着干活的 agent（包括改面板的那个），按下去会一起杀掉，1 小时内起不回来。

### 5.3 v0.5.95：需要开发者密码时弹框 + 安全文档

- 守卫挡住 `启动 / 停止 / 装版本 / 打开实例` 时，面板弹「需要开发者密码」并**重试原操作一次**
  （`web/src/elevation.ts` + `components/ElevationDialog.tsx` + `actions.ts` 的 `withElevation`）。
  只对 `developer_elevation_required` / `developer_password_not_set` 弹；HTTPS、Web Shell 关、
  紧急停机这些不弹（弹了也没用）。
- `/i/<id>/` 的拦截页自带密码表单（`DeveloperGuardFilter.gatePage`，静态方法便于测试）；
  `tls_required` 的页面故意不给表单——填了也只会被拒，且是明文。
- `errMsg` 把六个后端码翻成人话，toast 不再只剩英文。
- 同一版里把"明文 HTTP 放开开发者模式"那一版撤回了（`8d451f2`）：**HTTP 下开发者模式/守卫/E-Stop
  一律不开**，`security.require_https_for_developer` 这个配置项不存在。别再放开。
- `docs/design-notes.md` §7 是这一版重写的安全说明（保护什么 / 不负责的边界 / 三层门 + 刹车 /
  明文政策 / 其余不变量），以后谈安全先看它。

### 5.4 同 uid 的 agent：明确不做假防护（2026-10-28）

审计提的"agent 有权限就能直接改哈希密码，0600 挡不住同一个 uid"是**结构性事实**，不是待修 bug：

- `users.json` / `developer.json` / 证书私钥的 0600 只挡别的用户；同 uid 下 agent 可以重写它们，
  也能从 `/proc/<pid>/environ` 读面板的环境变量。校验和、签名、内存基线告警**都挡不住**
  （密钥/记录也在它读得到的地方），做了只会让下一个人误以为这里有边界。
- 结论写进了 `docs/design-notes.md` §7.2 与 `docs/deployment.md` §11（三条真边界：外层再认证 /
  拆容器 / 不同 uid 跑实例）。**别再为这件事加进程内检查**；要防就上 OS/容器边界。
- 顺带记住：`HDSL_ADMIN_PASSWORD` 以环境变量形式留在 `/proc/<pid>/environ` 里，同 uid 可读——
  部署时首启建好账号后把它从环境里去掉，或改用一次性种子文件（这个改进还没做，见 §4 的"还没验证"）。

### 5.5 从零重建的提示词

`docs/build-from-scratch-prompt.md` 是一份**只给"从零新建项目"用**的任务提示词（**不是**接手本仓库的
操作手册——在现有仓库里照它重建会覆盖真实实现）：硬约束、功能清单、
对外契约、构建与验证命令，以及**这一路踩过的坑**（Host 头顺序、Dockerfile 漏 COPY plugins、
xterm 输入法、会话 mtime、半安装、init 超时、同 uid 不是边界、CI 的 latest 闸门……）。
修了新坑就往 §5 里加一条（现象 / 根因 / 正确做法），别让下一个人重新发现。

## 6. 环境备注

沿用 [`2026-10-25`](session-handoff-2026-10-25.md) §6 与 [`2026-10-26`](session-handoff-2026-10-26.md) §6：
推送强制 `-c http.version=HTTP/1.1`（github.com 传满 32 KB 就停）、推送后用 `gh api` 核 sha、
`github.com:443` 整段不可用时先重试再考虑走 API（API 生成的 commit sha 与本地不同）、
Gradle 用 `flock /tmp/hdsl-gradle.lock`、插件的 `test/` 下不要放导入其它测试文件的入口
（`node --test` 会让每个测试跑两遍）、本地化断言要写成两套表都接受（`/中文|english/`），
否则在 `LANG=en` 的机器上必红。
