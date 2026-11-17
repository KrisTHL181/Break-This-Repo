# 会话交接：HDSL-web（2026-10-26）

> **已被 [`session-handoff-2026-10-27.md`](session-handoff-2026-10-27.md) 接续**（v0.5.92：三档审批、
> 命令委托、Tab 补全、供应商、会话列表）。本稿仍是第一方终端本体与三轮审查的权威文本。
>
> 本稿接住 **v0.5.91**：面板第一次自带终端（`plugins/` 下两个第一方插件），以及围绕它们的
> **三轮外部审查**修复。上一稿 [`session-handoff-2026-10-25.md`](session-handoff-2026-10-25.md)
> 仍有效——实例生命周期 ↔ 终端、反代、整合包契约、部署与环境备注都在那里，本稿只写它之后的增量
> 与"下一次别踩"的东西。
>
> **隐私约定（继续遵守）**：真实域名、内网地址、宿主机路径、口令一律不写进仓库；示例统一用
> `dsh.example.com`、`192.0.2.x`、`/srv/...`、`<口令>`。仓库账号 `nas200-timmy` 属公开信息。

## 0. 当前状态

- `main` = **v0.5.91**（`web-shell` 以 `--no-ff` 合并进来）。`web-shell` 与 `main` 同步，
  没有遗留的功能分支：**`dsh-tui` 已完成任务并从 GitHub 删除**（内容全在 `web-shell`/`main` 里，
  见 §5）。
- 后端测试基线（本机 Linux）：**814 用例 / 0 失败 / 2 跳过**；前端 `tsc --noEmit` + `vite build`
  + `compat`（ES11）通过；两个第一方插件离线 `check` 通过，插件测试 **ACP 46 条 / 进程内 20 条**。
- 两个终端插件**仍未发布到 npm**，版本停在 `0.1.0`（它们随 jar 走 `file:` tarball 安装；
  清单里的 `version` 与 `package.json` 必须一致，`FirstPartyPluginsTest` 钉着这条）。

## 1. 这一版改了什么

### 1.1 面板自带两个第一方终端

仓库根下新增 `plugins/`（契约见 [`plugins/README.md`](../plugins/README.md)，已改为中文）：

- **`@nas200-timmy/dsh-inproc-tui`**（profile `dsh-inproc-tui`）：mode bundle，叠在 `dsh-base`
  上跑在 harness 进程里——token 级流式、思考增量、dsh 自己的工具卡、审批/追问、模型与推理强度
  切换、`/resume` 与 `/compact`。钉在 0.2.x（`requiresDsh >=0.2.0-rc.2`）。
- **`@nas200-timmy/dsh-acp-tui`**（profile `dsh-acp-tui`，bin `dsh-acp-tui`）：独立程序，
  不 import 任何 harness 包，走 ACP v1。兼容 0.1.2 起的所有线，是版本变化时的退路。

构建把两者打成 npm 布局 tarball 塞进 jar（`first-party-plugins/index.json` + `.tgz`），安装时
`FirstPartyPlugins` 从 jar 写出、以 `file:` 规格交给 `dsh plugin add`——**离线可装，构建机不需要
npm**（打包是 Gradle `Tar`，唯一调用 `node` 的是插件的 `check`/`test`）。

### 1.2 三轮审查修了什么（都是"文档承诺 vs 实现"）

第一轮（提交 `a5a8ad1`）：粘贴、`/new` 每次必抛、启动失败留常驻进程、`acp.js` 四处脆弱
（stdin error / 握手超时 / 行长上限 / 非 JSON 只报一次）、两个审批互相覆盖、进程内那侧的
optional peer 顶层 import、waterfall 归属、退出有界等待、非 TTY 拒开。

第二轮（`15d8ae6`）：命令面板吞掉 `/` 后的字母（现在面板声明 `live`，编辑键继续进正文，
共用 `plugins/shared/ui/edit.js`）、进程内粘贴、`Ctrl+J` 换行、工具卡钉死 `expanded:false`、
`/model` 只改显示（现在同时写 `installModelSelection` 的 `state.current`）、`/new`/`/resume`
不隔离旧回合、`/resume` 失败仍报成功、半安装仍显示"已安装"。

第三轮（`c43db5d`，见 §1.3）与"启动回放 / 空 bundle 判据"。

### 1.3 ACP 账户交接：两个 profile 别混（这版最险的一处）

`DshLauncher.plan` 把第三方账户的供应商路由与 default model 写进「**这次启动引导的** profile」。
开终端时如果按插件**存放**的 profile（`dsh-acp-tui`）建计划，而子进程引导的是 harness 自己的
`acp` 模板，路由就在运行中的 harness 里根本不存在——账户不可用，或落回别的路由。

现在的形状：

- `DshTuiPlugins.harnessProfile(entry)` 声明「bin 插件的程序自己引导哪个 profile」
  （ACP → `acp`；纯 bundle 返回 `null`，表示就是它自己的）；
- `DshTui.runProfile(plugin)` 用它建计划，`entryPoint` 仍去**文件所在** profile 找；
- `DshTui.terminalEnvironment(...)` 把同一个名字放进 `HSDL_TUI_ACP_PROFILE`，程序不可能与
  计划不一致（`plugins/dsh-acp-tui/src/dsh.js` 读这个变量）。

同一处还删掉了一段**改密钥名**的逻辑：ACP 终端曾在 `DEEPSEEK_API_KEY` 为空时，按对象键顺序取
第一把非空的 `HSDL_LAUNCH_API_KEY_*` 复制过去——第三方密钥因此被交给 DeepSeek 的模型与搜索入口。
现在什么都不复制：launcher 按路由名给密钥（`HSDL_LAUNCH_API_KEY_<route>`），路由自己读；
`DEEPSEEK_API_KEY` 只由 launcher 给 DeepSeek 账户设置（网页搜索入口是 DeepSeek 的服务）。

`dsh plugin --profile acp add` 会初始化模板；`dsh` 的 `loadProfile` 在缺 `package.json` 时补模板、
且**不覆盖**已存在的 `cordis.patch.yml`，所以首启前写路由是安全的（读的是安装的 dsh 源码，见
`@deepseek-ai/dsh-app-boot/lib/index.js` 的 `loadProfile` / `initProfile`）。

### 1.4 ACP 切换会话的竞态

`session/cancel` 只是通知，被取消的 `session/prompt` 会在之后正常返回。旧的 `detachTurn()` 只发
通知就返回，于是旧回合返回时会 `finishStreaming()`、把 `status` 改成 `ready`、`drainQueue()`
——对着**新会话**做。现在：`runPrompt` 把请求存进 `pendingTurn`，`detachTurn()` 取消后**有界等待**
（`turnSettleMs`，默认 2s，工具卡在审批上也要能走），并推进 `turnSerial`；晚到的答复由
`ownsTurn(serial, sessionId)` 判为空操作。

### 1.5 面板侧：`filesPresent` 与「安装未完成」

`GET …/tui-plugins` 每行多给 `filesPresent`：`installed` 只读 profile 清单（`pnpm` 先写清单再
拉包），磁盘判据是另一半。`installed && !filesPresent` 的行在页面上标「安装未完成」（变灰并说明
原因，**不隐藏**），按钮同时给「重新安装」与「卸载」——先卸再装不是用户该知道的步骤。

`DshTui.filesPresent` 的判据：目录在 **且目录里有 `package.json`**（空壳目录不算），有 bin 的包
还要求它声明的入口文件存在。纯 bundle 只靠清单与磁盘这一对，没有别的东西可查。

### 1.6 测试：两次"假覆盖"的教训

- **进程内的"24 条"其实是 12×2**：`test/index.js` 导入 `unit.test.js`，而 `node --test` 两者都
  会发现。删掉 `index.js`，现在 20 条唯一测试。
- **源码正则断言不是覆盖**：进程内终端此前只能靠读源码断言（peers 在模块顶层 import，脱离 harness
  就加载不了）。现在两个 peer 改成 `open()` 里动态 import，`test/app.test.js` 直接驱动
  `TerminalApp`/`ChatSession`——粘贴、面板跟随、`ctrl+enter`、工具卡、`/model` 写
  `modelState.current`、恢复回放、`/new` 先收尾。
- **断言要钉住"只有这个修复才会给的理由"**：ACP 那两条竞态测试用未决的 `session/prompt` 复现，
  换回修复前的 `app.js` 会红（2 failed / 44 passed）。做修复时先跑一遍这个反向验证。

## 2. 关键文件

```
plugins/README.md                           第一方插件契约（中文）
plugins/shared/ui/edit.js                   两个终端共用的编辑键 + live overlay 判据
plugins/shared/{term,ui,i18n.js}            终端工具箱（屏幕、按键解码、composer、对话框、渲染）
plugins/dsh-inproc-tui/src/app.js           进程内终端：面板/切换/回放/键位
plugins/dsh-inproc-tui/src/agent.js         ChatSession：显式模型状态、history()、动态 peer import
plugins/dsh-acp-tui/src/ui/app.js           ACP 终端：runPrompt/detachTurn/ownsTurn、childEnvironment
plugins/dsh-acp-tui/src/dsh.js             locateHarness（读 HSDL_TUI_ACP_PROFILE）
src/main/java/…/dsh/DshTuiPlugins.java     清单 + harnessProfile（谁引导哪个 profile）
src/main/java/…/dsh/DshLauncher.java       plan()：账户路由/default model 写进哪个 profile
src/main/java/…/web/terminal/DshTui.java   终端计划：runProfile / terminalEnvironment / filesPresent
src/main/java/…/web/http/InstancesApiServlet.java  /tui-plugins：installed 与 filesPresent 分开说
web/src/components/TuiPluginSection.tsx    「安装未完成 / 重新安装」那一行
```

## 3. 验证到哪一步

- `./gradlew --no-daemon build`：**814 / 0 / 2**；`web`：`typecheck` + `build` + `compat` 绿。
- 插件：`node plugins/dsh-inproc-tui/scripts/check.mjs`、ACP 同理；ACP 46 条、进程内 20 条。
- `PluginApiTest` 覆盖：清单字段、安装进自己的 profile、半安装（`installed` 真 / `filesPresent` 假）、
  重新安装把文件带回、目录还在但清单没了。
- `DshTuiTest` 覆盖：ACP 计划引导 `acp`、bundle 用自己的 profile、环境里告诉程序同一个名字、
  空目录与缺入口都不算已安装。

## 4. 已知缺口与下一步

- **真机端到端未验**：需要一台 0.2.x 实例 + 真实账户，才能确认第三方账户经 ACP 终端能对话；
  PTY 的 fd 复用竞态、品牌 agent 的渲染问题也只能真机看。这一版的结论都停在代码与单测层面。
- **ACP 侧没有历史回放**：`session/resume` 只返回 `configOptions`，协议没有历史通道。回放只做在
  进程内终端（启动 `--resume` 与交互式 `/resume` 都会回放）。
- **插件版本仍是 `0.1.0`**：它们从未发布到 npm，随 jar 分发。要单独发布时，记得同时改
  `package.json` 与 `DshTuiPlugins.ENTRIES` 的 `version`（测试钉着两者一致）。
- 上一稿 §4 里没动的缺口仍在（社区 TUI 插件要联网安装、反代的 br/zstd、`internal/rtty` 零测试等）。

## 5. 发布动作：v0.5.91（本次执行）

顺序：改动提交在 `web-shell` → `--no-ff` 合并进 `main` → 推 `main` → `shadowJar` 出包 →
打 tag → `gh release create`。`dsh-tui` 的内容已在合并前落进 `web-shell`，合并后**删掉该分支**
（本地与远端）——它的任务结束，不再作为公开分支出现。

```bash
flock /tmp/hdsl-gradle.lock ./gradlew --no-daemon shadowJar -PreleaseVersion=0.5.91
sha256sum build/libs/hdsl-web-0.5.91.jar > SHA256SUMS.txt
git tag -a v0.5.91 -m "面板自带两个第一方终端（进程内 / ACP）"
git -c http.version=HTTP/1.1 push origin main web-shell v0.5.91
gh release create v0.5.91 build/libs/hdsl-web-0.5.91.jar SHA256SUMS.txt --notes-file <notes>
```

**Release 说明写什么**：v0.5.91 起，Release 正文是**面向用户的功能介绍**（这一版有什么、怎么用、
边界在哪），不再是 CHANGELOG 的摘录——第一次直接 `sed` 提了工程记录，被用户退回重写。CHANGELOG 仍是
工程记录，两者的读者不同。正文写在 `/tmp` 的文件里再 `--notes-file`，发布后要改走
`gh api -X PATCH repos/<owner>/<repo>/releases/<id> --input <json>`（`gh release edit` 走同一接口）。

客户端 5 个平台资产与 `SHA256SUMS-client.txt` 由 tag 触发的 `client.yml` 上传（`--clobber`，可重复跑）；
docker `0.5.91` 会拿到 `latest`（`sort -V`）。

**容器镜像这条路容易被漏掉**：`Dockerfile` 的构建阶段是**白名单 COPY**，新增任何会被 Gradle 读到的
顶层目录都要显式 COPY 进去。v0.5.91 第一次发布就漏了 `COPY plugins ./plugins`——镜像里那份 jar
**不带第一方终端**（索引为空，面板照样列出条目，点安装才失败），而 CI 全绿。现在 `Dockerfile`
里有 `COPY plugins` 和一条构建期断言（jar 里必须至少有一个 `first-party-plugins/*.tgz`），断言
放在 `shadowJar` 之后、镜像组装之前，漏了当场失败。

## 6. 环境备注（下次接手别踩）

- **推送一律 `-c http.version=HTTP/1.1`**：`github.com` 的连接能建立、传满 32 KB 就停；同一时刻
  `gh api`（api.github.com）0.3 秒就回。加 `-c http.lowSpeedLimit=1000 -c http.lowSpeedTime=45`
  卡住就快速失败。推送后用 `gh api repos/nas200-timmy/HDSL-WEB/git/refs/heads/<分支>` 核远端 sha，
  **不要用 `ls-remote` 判成败**（ref 已存在会假阳性）。
- **`github.com:443` 会整段不可用**（`curl` 25s 超时、`git fetch` 报 `GnuTLS recv error (-110)`），
  而 `api.github.com` 一直正常。上一轮用 Git Data API 兜底提交过一次，代价是**commit 对象的 sha
  与本地不同**（GitHub 会去掉信息末尾的换行），只能靠 `git fetch` 后 `git reset --soft` 对齐。
  现在网络恢复后已按这个办法对齐，历史里第二轮的提交是 `15d8ae6`（tree 与本地当时那份完全相同）。
  **下次遇到同样情况：先等几分钟重试 `git push`，别急着走 API**——走了就要做一次对齐。
- Gradle 有锁：所有构建/测试都用 `flock /tmp/hdsl-gradle.lock ./gradlew …`；后端测试共享
  `build/test-home`，别并发跑。
- 插件测试用裸 `node --test`（Gradle 也是）：**不要在 `test/` 下放一个导入其它测试文件的入口**，
  否则每个测试都会跑两遍，条数看着翻倍。
