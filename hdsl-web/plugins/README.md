# 第一方面板自带插件

这个目录放的是 **HDSL-web 自己发布**的插件，与面板从 npm 上列出的社区终端界面区分开。

```
plugins/
  README.md            ← 本契约
  shared/              ← 两个终端共用的终端工具箱
    term/  ui/  i18n.js
  dsh-inproc-tui/      ← 进程内终端：叠在 dsh-base 上的 mode bundle（0.2.x）
    package.json       声明 dsh.bundle.patch
    cordis.patch.yml
    src/…
  dsh-acp-tui/         ← ACP 终端：面板直接启动的程序（0.1.2 起）
    package.json       bin/dsh-acp-tui.js
    src/…
```

这里只有**终端界面**。给 agent 用的面板桥（`dsh-panel-agent`）已移除：面板侧的授权接口
（agent 密码、票、吊销）还在，agent 用面板签发的 bearer token 直接调 `/api`，不需要插件。


`shared/` 不是独立包：每个 tarball 里带一份拷贝（`package/shared`），打包时的 staging
步骤把指向仓库布局的 import 说明符（`../../shared/…`）改写成安装后的形态（`../shared/…`）。
所以插件自己的源码保持可读，tarball 又保持自包含。

## 这里的「第一方」指什么

1. **从本仓库构建。** 源码与发布它的代码放在一起，同一许可证（GPL-3.0-or-later），
   与面板一起评审。
2. **打进 jar。** 构建把每个插件打成 `build/first-party-plugins/<包名>-<版本>.tgz`，并以
   `first-party-plugins/` 放进 jar，附一份描述内容的 `index.json`。面板的终端插件安装路由
   把这个 tarball 写出来，用 `dsh plugin --profile <profile> add file:<tarball>` 安装，
   所以**装第一方插件永远不需要 npm registry**。
3. **离线可构建。** 除了它驱动的 harness 之外没有依赖，除了 `node --check` 之外没有构建步骤。
   插件目录里的 `npm run check` 就是全部构建；CI 在 `gradle check` 里跑它。
4. **按契约容忍版本。** 第一方插件要跨 harness 版本可用，所以只通过稳定接口与 harness 说话
   （ACP 终端：ACP stdio 协议；进程内终端：钉死并实测过的 0.2.x 区间），能力缺失时降级，
   而不是崩掉。

## 新增一个插件

1. 建 `plugins/<名字>/`，当普通 npm 包写。`type: "module"`，程序型终端带 `bin`（见下），
   **没有 `dependencies`**。
2. 写一个离线可跑的 `scripts/check.mjs`（语法检查、清单检查），并把 `npm run check` 指向它。
3. 若它是终端界面，在 `DshTuiPlugins.ENTRIES` 里声明：`bundled = true`、它自己的 profile、
   以及它要求的 harness 线。
4. Gradle 会自动接管：`plugins/` 下每个有 `package.json` 的目录都会被打包、进索引、放进 jar
   资源。打包是 Gradle 的 `Tar` 任务，不是 `npm pack`——**构建过程不需要 npm**，没有 Node 的
   机器同样能产出带插件的 jar（只是跳过插件自己的 `check`/`test` 任务）。

## peer 区间：写整条线，不要逐个枚举 rc

harness 只发预发布版（`0.2.2-rc.3`），而 dsh 判断兼容性时用的是
`semver.satisfies(..., { includePrerelease: true })`：`^0.2.0-rc.1` 或
`>=0.2.0-rc.2 <0.3.0-0` 这样的区间**会**匹配 `0.2.1-rc.5` / `0.2.2-rc.3`。
第一方插件的 peer 因此写成整条 0.2 线（`^0.2.0-rc.1`），面板的安装闸门
（`DshTuiPlugins.requiresDsh`）也写成 `>=0.2.0-rc.2 <0.3.0-0`——两边同一个口径。

逐个枚举 rc 的写法只在一种情况下有意义：明确拒绝某个**已知会坏**的构建。它
带来的问题是每一条新的 rc 都会掉到区间外，`dsh@next` 一升级，面板就把自己的
终端判成"不匹配"。上一版就是这样：枚举到 `0.2.2-rc.1` 为止，`0.2.2-rc.3`
全被挡住。0.3 是新一条线，接缝要重新验证，所以上限仍然卡在 `<0.3.0-0`（注意不是 `<0.3.0`：`includePrerelease`
下它仍然会放 `0.3.0-rc.1` 进来）。

## 终端界面的特殊之处

终端界面是 **app 插件**，所以绝不能装进实例的 `web` profile：`dsh-web-app` 与终端 app 各自
解析同一份 argv，装在一起就起不来。因此清单每项都写清它自己的文档要求装进哪个 profile
（`tui`、`dsh-tui`、`pi-tui`、`dsh-inproc-tui`、`dsh-acp-tui`），面板在那个 profile 上启动
终端，而不是实例自己的。

这里的两个终端在**运行时是二选一**：可以都装（ACP 那个是实例 dsh 版本变化时的退路），但只有
第一个匹配的会被启动。清单给两者同一个 `runtimeGroup`（`dsh-terminal`），列表报告哪一行是
`active`，页面把另一个画成「备用」——装它是刻意的选择，不是要拦下来的错误。

支持两种形态，面板按各自的清单启动：

- **程序（`bin`）** —— `dsh-acp-tui`。面板跑
  `node <profile>/node_modules/<package>/<bin>`，让终端本身（而不是包管理器的 shim、也不是
  shell）成为 PTY 的根进程。这个程序随后自己 spawn harness（`dsh --profile acp`），于是面板的
  web-shell 看到的是一个往终端写 ANSI 的进程，与任何终端应用一样。这个形态也是它能跑在 0.1.x
  上的原因：它只需要 ACP 的 profile 模板。

  这里有两个 profile，且不能互换：包自己的 profile（`dsh-acp-tui`）是它的**文件**所在，而
  harness 引导的是 **`acp`** 模板。面板按 `acp` 建启动计划（launcher 把账户的供应商路由与
  default model 写进「这次启动引导的 profile」），并把同一个名字放进
  `HSDL_TUI_ACP_PROFILE`，所以路由就在真正运行的那个 profile 里。过程中不改任何变量的名字：
  第三方账户的密钥始终待在 `HSDL_LAUNCH_API_KEY_<路由名>`（路由自己读这个名字）；
  `DEEPSEEK_API_KEY` 只由 launcher 为 DeepSeek 账户设置，因为它背后的网页搜索入口是
  DeepSeek 的服务。

- **mode bundle（`dsh.bundle`）** —— `dsh-inproc-tui`。面板装上它，再引导它的 profile
  （`dsh --profile dsh-inproc-tui`），与社区 bundle 完全一样。插件跑在 harness 进程里，这是
  它拿到 token 级流式与审批/追问通道的原因——代价是它钉在写它时的那套 0.2.x 接缝上。哪些线
  能跑，看清单里的 `requiresDsh`。

## 终端契约：审批、命令、补全、供应商

### 审批三档（`/yolo`）

harness 自己的审批策略只有"问"和"全拒"（`'never'` 是**拒绝**，不是放行），所以"不问就执行"是终端
自己的决定，落在 `plugins/shared/approval/policy.js` 一个纯函数里：

| 档位 | 行为 |
|---|---|
| `ask`（**默认**） | 只读工具直接跑；写文件、跑命令、未知工具、敏感文件与计划都发问 |
| `critical` | 日常编辑与命令直接跑；危险命令、敏感文件、计划审批发问 |
| `full` | 一律放行（计划/提问仍会问；沙箱仍然生效） |

- 自动放行**仍然写审计**（harness 记 `approval/asked` + `approval/decided`），也**不放宽沙箱**。
- 分类器"未知即问"：读不出命令的 shell 调用、读不出路径的写操作、未知工具、计划/提问类调用一律发问。
- **敏感文件**（`.env*`、`.ssh/`、`id_rsa`、`.npmrc`、`.netrc`、`*.pem`、`*.key` 等）**在任何档位都会问**，
  `full` 也不例外：把密钥打进回放不是"随手 ls"，不可逆。`printenv` 与**裸** `env` 也算敏感
  （`env FOO=bar cmd` 是"带着一个变量跑命令"，不在此列）。
- **网络读取不算"只读"**：`web_fetch` 的 URL 是模型自己拼的，可以把工作区数据带在查询串里，所以它按
  未知工具处理（`ask`/`critical` 会问，`full` 放行）。
- 路径先 `resolve` 再判断包含关系，`..`、相对路径和 `/workspace/../etc/passwd` 都不能绕过工作区边界。
- 档位是**会话级**的，`/new`、`/resume` 之后回到 `ask`，不会把上一个会话的 `full` 带过去。
- **永久规则**：`/allow <工具> <完整命令或路径>`、`/deny <工具> <完整命令或路径>`、`/rules [clear]`。
  规则按 `allowKey()` 的精确字符串匹配，存在**实例 home** 的 `.hdsl-tui/permissions.json`（随 `/data` 保留、不跨实例共享；`HDSL_TUI_RULES` 可覆盖）；
  deny 永远优先，allow/deny 都不经过启发式分类器。

### 审批对话框的四个选项（数字键 1-4）

| 键 | 选项 | 结果 |
|---|---|---|
| 1 | 运行 | 允许这一次 |
| 2 | 当前会话默认运行 | 本会话内同一"工具 + 完整命令/路径"不再问（危险/敏感/计划除外） |
| 3 | 拒绝 | 拒绝 |
| 4 | 拒绝并说明原因 | 拒绝，并把理由作为一条用户消息交给模型 |

第 4 项的理由投递方式取决于线路：进程内 `followup`（下一步入上下文）；ACP 的 `session/prompt`
不允许并发，所以排在当前回合结束后再发。

### 命令

- **交给 harness**：`/permission`、`/goal`、`/feedback`、`/export`。
- **终端自己的**：`/help` `/model` `/effort` `/yolo` `/plan` `/resume` `/sessions` `/new`
  `/compact` `/clear` `/status` `/provider` `/allow` `/deny` `/rules` `/quit`。
- **`/sessions` 的列表**：只列有首条人类提示的会话，空会话/误建会话不出现；行标签是首条提示词，
  短 id 只放在后面的元信息里。读不出日志的会话会保留并标「标题未读出」，不会静默消失；判据认
  source 缺失、`agent/inbox/spliced` 和字符串 content，排序用 JSONL 的 mtime。ACP 线路的
  `session/list` 目前只回 sessionId/cwd，服务端给出 `title` 时会用，空会话过滤只在进程内保证。
- 未知命令先问 harness，仍未命中才报错。

### Plan mode 与 Ctrl-S

- `shift+tab` 或 `/plan [on|off]` 切换 Plan mode；状态栏和启动页有 `▣计划` 标记。进程内把
  `/plan on|off` 交给 harness；ACP 协议没有这个通道，会明确说"这条线跑不了"。
- `ctrl+s` 把输入框内容插进**正在跑的回合**（进程内走 `agent.steer()`）；ACP 不能并发 prompt，
  会说明并留在队列里。`ctrl+o` 仍是展开/折叠工具输出。
- **提示、补全候选与 `/` 命令菜单都画在输入框下面**（Kimi Code 的位置），状态栏在最底：
  候选行、`esc 打断`、`/ 命令 · @ 文件 · ctrl+j · ctrl+o` 都在那里。`/` 菜单不是 overlay——
  它是底部块里的内联列表（编号 + 描述，↑↓/数字选择、enter 运行、esc 关闭），不遮住记录区，
  也不隐藏输入框光标。
- **`/yolo` 与 `/permission` 无参时打开列表**（↑↓ / 数字 / 回车，当前项标「当前」），
  不再"执行一次换一档"；带参数仍然直接设置。
- **空闲时终端一个字节都不写**（`Screen#paint` 在整帧与光标都没变时直接返回）。浏览器/终端
  每次写入都会重排输入法的辅助框，空闲重绘会把中文输入法正在组词的内容打断。
- 输入框以 `!` 开头时，命令在实例工作区直接执行，输出是一张 shell 工具卡（进程内；不写进
  agent 上下文，因为那需要伪造一条工具结果）。退出终端会结束仍在跑的 shell 子进程。

### Tab 补全

`plugins/shared/ui/complete.js`：命令名（本地 + harness）、参数（模型/强度/档位/计划/会话/预设/供应商）、
`@` 工作区路径。`@` 空片段会做有界递归搜索（跳过 `.git`、`node_modules` 等），带 `/` 时列下一层；
不会离开工作区。重复 Tab 循环，候选行显示在输入框下方，任何其它按键结束循环。

### 启动页

第一方终端的启动页用 **dsh-TUI README 里像素鲸鱼的静态帧**（MIT；原调色板 `#142660` / `#4e6fff`
/ `#bee1ff` / `#ffffff`），左侧鲸鱼、右侧模型/档位/计划/cwd/会话与按键提示；窄终端改为上下堆叠。

### 模型供应商（仅进程内）

`/provider list|add|remove|test`。写的是 **`llm-pi-ai` 的 settings 段**（与网页「模型」页同一份），
用 `mutate` 按路径写而不是整段替换（整段替换会清掉已 redact 的 secret）。密钥走 `credentials.set`
或只写 `--key-env` 名字；`--key` 不回显、不写日志、不进 settings。`add`/`remove` 需要 `--yes`。

### 能力矩阵

| 能力 | 进程内（0.2.x） | ACP（0.1.x+） |
|---|---|---|
| 三档审批 / `/yolo`（默认 `ask`） | 是 | 是（客户端应答过滤） |
| 敏感文件保护 / 永久 allow-deny | 是 | 是（同一份规则文件） |
| 四选项审批（含拒绝理由） | 是 | 是（理由排队到回合结束） |
| `/permission` `/goal` `/feedback` `/export` | 是（harness 命令） | 否（会明说） |
| Plan mode / `shift+tab` | 是（harness `/plan`） | 否（会明说） |
| `ctrl+s` 中途插入 | 是（`agent.steer()`） | 否（留在队列并说明） |
| `!` Shell mode | 是（工作区、输出为工具卡） | 否 |
| Tab 补全 命令/参数 | 是 | 是 |
| `@` 文件补全 | 是（有界递归，工作区内） | 否 |
| `/provider` | 是 | 否 |
| `/status` | 是 | 是 |

## 检查

```sh
# 插件自己的离线检查
node plugins/dsh-acp-tui/scripts/check.mjs

# 从仓库根：把 tarball 打进 build/first-party-plugins/
./gradlew packageFirstPartyPlugins
```
