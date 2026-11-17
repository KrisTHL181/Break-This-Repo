# 面板与 agent：第二把钥匙

> 状态：面板侧、设置页（开发者 → agent 访问）与授权列表均已落地。agent 侧的桥插件已移除。
> 换票有两条路：agent 用 **agent 密码**自己换（`/api/developer/agent-auth`，免会话），或
> 操作员在设置页用提权会话**直接签一张并一键复制**（`/api/developer/agent-token`）。之后
> 都用 `Authorization: Bearer` 调 `/api`。两条路签出的是同一种票；操作员签票之所以敢
> 不绑定实例，是因为自杀闸认的是**请求时的调用者身份**，不是票上写的那行字。

## 1. 要解决的问题

agent 能做的事从来不是问题——它本来就能用 shell 起一个 Minecraft 服务端。问题是它**那么
做之后，面板以为的世界和真实的世界不一样了**：端口被占、面板显示 STOPPED、两份日志写同
一个 world、会话记录里多出一个面板没启动过的实例。

所以这套东西的目标不是"给 agent 能力"，是**让正确做法比旁路更省事**：

1. 模型知道自己在面板里（`systemPrompt.context`）；
2. 需要访问时走面板的 API（`/api`），票由自己用 agent 密码换；
3. 出了问题能查到是谁干的（审计里的 `agent:<label>`）。

## 2. 两把锁，两扇门

```
开发者密码  ──/api/developer/elevate──▶  会话提权（30 分钟，可延一次到 3 小时）——人用

agent 密码  ──/api/developer/agent-auth──▶  agent 自己换票，不需要会话（curl 可用）

提权会话    ──/api/developer/agent-token──▶  操作员直接签票（设置页一键复制）
```

**两个密码存在两个文件里**（`auth/developer.json` / `auth/agent.json`），由两个不同的入口
校验。这个分离是刻意的：

- 网页的提权表单永远**不会**接受 agent 密码——把较小的那串密码输进浏览器里，得到的是
  "密码错误"，而不是提权；
- agent 那扇门也**不会**接受开发者密码。

第三条路（`agent-token`）不是第三把钥匙：它就是提权会话本身在签票，范围由操作员当场勾选，
不受 agent 密码上限的约束——因为授权人就是操作员本人。签出的票与密码换的票同生同死：进
同一个注册表、同一张列表、同样随提权结束失效、同样掐得掉。agent 拿着这种票**签不出下一
张**（`/api/developer/*` 对票关闭）。它曾经被整套撤掉，因为那时挡自杀的只有票上的绑定，
操作员签出的未绑定票等于把枪递给 agent；现在闸门认请求时的调用者身份（见 §3），这条路
才敢回来。

### 借来的权力：agent 密码必须先有人提权

agent 密码**不是**一条独立的路。用它换票之前，必须已经有人在本网页面上用开发者密码提权过；
没人处在这个窗口里时，接口直接拒绝：

```
409 agent_password_not_elevated
```

而且票的寿命**被那个窗口截断**：操作员提权 30 分钟，agent 就算要 3 小时，拿到的也是到
提权结束为止。这样"提权了才能用"不是一句口头约束，是算出来的。

**用票的时候也查一遍。** 窗口可以提前结束——「停止提权」、登出、改或清开发者密码。任何
一种发生，手里那张票立刻失效，不等它自己的到期时间。最后一个人结束提权后，
`/api/developer/agent-auth` 也换不出新票。

面板上的「停止提权」也会**掐掉这次提权发出去的全部票**——把借来的权力还回去，不能只还掉
自己那张。

因此 agent 密码泄漏的后果是有限的：它单独存在时什么都打不开。

### 为什么 agent 密码只在 3、4 档有效

| 守卫档 | 覆盖 | agent 密码 |
|---|---|---|
| `off` | 只守开发者路由 | **拒绝**（409 `agent_password_not_applicable`） |
| `proxy` | 只守 `/i/*` 反代 | **拒绝** |
| `instance` | 外加实例的变更类接口 | 可用 |
| `all` | 全部 `/api/*` | 可用 |

前两档里，一个会话本身就能访问 `/api/*`，再发一张票不会多出任何能力，只会多一个可能泄漏
的凭据。所以那两档直接拒绝，并说明原因。

## 3. 范围（scope）在服务端定，不在客户端里

`AgentScopes` 把面板的接口分成十二类：

| scope | 覆盖 |
|---|---|
| `read` | `/api/*` 的 GET（凭据类路由除外，见下）；**不含聊天** |
| `mc` | `/api/mc/*` 的启停与控制台命令 |
| `mc-console` | **只**多给一条：MC 服控制台的 PTY 票据（`POST .../console/ticket` → `/ws/terminal/{ticket}`） |
| `system` | `/api/system/*`：宿主机 CPU/内存/磁盘/网络读数与数据目录占用 |
| `instances` | 建/删/启停**别的**实例 |
| `config` | 账号、模型、TLS、网络、Node 运行时、壁纸 |
| `plugins` | 装/卸插件、整合包、技能 |
| `transcode` | 转码队列、预设、启停与日志 |
| `transcode-raw` | 媒体根浏览、原始参数、破坏性文件操作 |
| `chat-read` | 助手、会话、消息、来源与附件下载 |
| `chat-write` | 建会话、发/编辑/重生成/取消、附件；受 `agentChatMaxInputTokens` 上限 |
| `chat-search` | 搜索状态/测试，以及发送时打开搜索；单独控制搜索成本 |

`mc-console` 单独一类，是因为它不是"再发一条命令"：附着上去的是一条**双向的活终端**——历史、
输出流、实时回显全都有。只勾 `mc` 时，agent 仍能启停、发 `console/input`、读日志，但拿不到
票据；勾上之后 `console/ticket` 才放行，ticket 里记的是这张票的 id 而不是会话 cookie，
WebSocket 握手时**必须同时带同一张票的 `Authorization: Bearer`**——URL 里的 ticket 单独
attach 不了任何东西。附着期间每 30 秒回查一次票是否还活着，票被掐、到期或最后一次提权结束
都会把这条控制台关掉。

四条额外规则，都在 `AuthFilter` 里：

- **未分类的路由一律拒绝**（默认关闭），新接口不会因为没人记得加规则而对 agent 敞开；
- **自杀类操作一律拒绝**（409 `self_operation_refused`，拒绝正文会写明原因与出路）：三个
  家族——dsh（`/api/instances/{id}/…`）、品牌（`/api/brands/{brand}/instances/{id}/…`）、
  ZCode（`/api/zcode/instances/{id}/…`）——的 stop / delete / terminal / launch 都算。这类
  操作如果放行，工具调用永远拿不到返回——进程正是被调用方自己杀掉的。**拒绝比排队诚实**：
  排队等于承诺一个已经不存在的执行者；
- **只在 API 边界内**：`/i/*`（反代）与 `/ws/*`（事件流）是浏览器通道，任何 scope 都不放行。
  `read` 不等于"所有 GET"——它只覆盖 `/api/*`。agent 要实例状态就问 API，面板因此记得住
  是谁问的。
- **凭据不出面板**：`/api/brands/*/admin-password`、`/api/exports/*`、`*/open`，以及整个
  `/developer/*`、`/auth/*`，都在网关拒绝，与 scope 无关。字段也要摘：ZCode 的 `token`、
  dsh 实例的 `url`（query 里是访问 token）对 agent 不输出。

判断用的是 **servlet 实际路由的那份解码路径**，不是原始 URI：`%61dmin-password` 在原始
URI 里不叫这个名字，经容器解码后就是它。读原始 URI 的检查等于检查了另一个字符串。

### 自杀判定靠身份，不靠自报

自杀规则的核心问题是「调用者住在哪个实例里」。答案不再问调用者，而是**从内核推导**
（`PeerIdentity`），Linux 与 Windows 各用各的原生接口：请求从回环端口进来，Linux 按
`/proc/net/tcp` 找 socket 的 inode、再按 `/proc/*/fd` 找持有进程（容器里面板是 root、实例
是 uid 1000，而默认的 root 没有 `CAP_SYS_PTRACE`、读不了别人的 fd 表，所以这一步由一个
`setpriv` 到 agent uid 的子进程完成，面板本身不被授予该能力；见 `SocketOwnerLookup`）；
Windows 直接问
`GetExtendedTcpTable`（`netstat -ano` 底下那张表），每行自带 owning pid。然后沿父进程链
上行——每个实例的根进程都是面板亲手起的、互为兄弟，所以链上**第一个**命中的实例根就是
调用者的家。命中后还有一道**成员校验**：父链是快照语义，死掉的祖先 pid 被复用可能指错根，
所以只有内核确认调用者确实在该根的进程树下（`descendants` 成员关系）才作数。走到面板
自身或链断（`setsid` 脱树、容器外脚本、本地代理）就是「身份不明」。

判定优先级（`AuthFilter#fatalRefusal`）：

| 调用者身份 | 目标是自己 | 目标是别的实例 |
|---|---|---|
| 内核推导出 X | **409**（内核事实，压倒一切声明） | 放行 |
| 推导不出，票绑定了 X | **409**（自报绑定兜底） | 放行 |
| 推导不出，未绑定 | **409**（hard-fatal 一律拒绝） | **409**（身份不明的调用者不许停/删/接管任何实例） |

`launch` 单列：它只在目标实例终端开着时致命（launch 会先关终端）。身份明确且目标不是
自己 → 放行；身份不明且目标终端开着 → 409；终端没开 → 放行（停着的实例里没有可杀的人）。

由此得到两个性质：**藏身份只会丢权限**（`setsid` 之后致命操作全被拒，不会被误认成别人），
**偷来的票没有致死力**（从别的进程发，推导结果是身份不明，致命操作照样拒）。

会话（人）不经过这条闸：人用开发者密码提权后停掉跑着 agent 的实例是运维，面板照常执行、
照常审计——这扇门从来只分辨 agent，不分辨人。

路由自己的提权检查（`requireElevation`）对 agent 一律让路：人的门是开发者密码，agent 的门
是 scope，而 scope 已经在过滤器里验过。要求 agent 输一个它永远没有的密码，等于把已经授权
的范围变成死路。`/api/mc/*/console/input` 之前就是这么坏的。

反过来，**agent 票不是开发者令牌**：两者都以 `Bearer` 出现，但 `DeveloperElevation#requireToken`
只认人那张。否则一张 `read` 票在守卫过滤器里就成了完整提权。

客户端里的白名单是装饰品。agent 与面板同一台机器、同一个 OS 用户，能改任何客户端代码；
真正的边界在 root 进程里。

## 4. 票的生命周期

- 票由面板内存持有，**不落盘**；面板重启即失效。
- **绑定是换票时推导的，不是自报的**：agent-auth 那条连接本身会被反查一遍，调用者住在哪个
  实例里，`instanceId` 就绑哪个（wire 形式 `family/id`，如 `kimi/DClDN1J3w0C_`）。推导不出
  时才退回调用者自报的值——自报只会多拒绝，不会少拒绝。**在实例里换票，票就自动绑对自己**。
- **必须先有人提权，而且用的时候还得有人提权。** 发票时到期时间被提权窗口截断；每次请求
  还会再查一次「此刻有没有人提权」。出借人走了（登出、「停止提权」、改或清开发者密码），
  票当场失效，不用等它自己的到期时间。
- **不滑动**：人那张会随使用滑动，agent 这张钉死在发票时给出的时间上。操作员看到"剩 2:47"
  就是真的 2:47。
- 上限 3 小时（与 `extend` 的封顶一致），请求更长的会被**截断而不是拒绝**，设置页显示实际
  拿到的时间。
- 面板列出的是**此刻能用的**票（`GET /api/developer/agent-grants`），可以**单张掐掉**
  （`DELETE /api/developer/agent-grants/{id}`），立即生效。没人提权时列表为空——票已经
  不能用，不该还显示成在用。
- 改或清 agent 密码，会清掉由它发出的**全部**票，但不会影响操作员自己的提权。

### 来源限制：默认只认本机回环

`agent.loopbackOnly`（**默认开**）打开时，票只从**回环**地址被接受：非回环一律
`403 agent_loopback_only`，带不带会话都一样。理由很直接——agent 和面板在同一台机器上，
票是给本地工具用的，从局域网或公网地址递过来的票已经走了不该走的路。设置页的开关在
「开发者模式 → agent 访问 → 票的来源」，`server.yaml` 里是 `agent.loopback_only`，
环境变量 `HDSL_AGENT_LOOPBACK_ONLY`；改一次立即生效，不用重启。

关掉这个开关之后，才轮到 `agent.remoteNeedsSession`（默认关，仅 yaml / 环境变量）：

| 票怎么来的 | 非回环 + 无会话 | 非回环 + 有会话 | 本机回环 |
|---|---|---|---|
| 浏览器签发（agent 页面复制） | 放行 | 放行 | 放行 |
| agent 密码换的 | **403** | 放行 | 放行 |

浏览器签发的票本身就证明有人打开过面板并复制了 token；agent 密码是长期秘密，泄漏后
不应该在远程直接能用。`remoteNeedsSession` 也关着时，远程票就和以前一样随便用——这是
两个开关都关掉之后的姿态，默认不会走到这里。

`agent-auth`（用密码换票）在回环限制开着时也直接拒绝远程调用，免得发出去一张用不了的票。

**反代会绕过这条规则。** 反代转发时 `getRemoteAddr()` 是反代地址（通常是 `127.0.0.1`），
于是所有请求都按回环放行。要让它在反代后也生效，只能在反代上限制来源，或者让面板只
监听回环、由反代转发固定路径。

## 5. 审计

每张票带着 label 进场，`AuthFilter` 把请求伪装成一个 `agent:<label>` 的会话，所以**所有
既有的 `actorOf(request)` 调用点都不用改**，`audit.jsonl` 里直接就能分出人干的还是模型干的。

agent 请求还会带三样**机器判定**的字段（`AuditLog` 的请求上下文，每个 handler 不用改）：
`grant`（票 id）、`scopes`（实际持有的类别）、`caller`（推导/绑定的 `family/id`）。gate 的
**拒绝也留痕**（`action: agent.denied`）：scope 拒绝记 `route_closed` / `scope_denied:*`，
自杀拒绝记 `self_refused` / `self_refused_unidentified` / `self_refused_launch`——护栏起
作用时看得见，护栏缺席时事后也查得出。真的执行出去的自指操作（本不该发生，防御纵深）
outcome 记成 `self_stop` / `self_delete` / `self_launch`，不与 `success` 混排。

```jsonl
{"channel":"web","action":"developer.agent","object":"AbC123","outcome":"mint","actor":"nas200"}
{"channel":"web","action":"agent.auth","object":"joh9by87","outcome":"success","actor":"agent:probe","caller":"kimi/DClDN1J3w0C_","scopes":["read","instances"],"grant":"joh9by87"}
{"channel":"web","action":"agent.denied","object":"/api/brands/kimi/instances/DClDN1J3w0C_/stop","outcome":"self_refused","actor":"agent:probe","identity":"kimi/DClDN1J3w0C_","route":"/api/brands/kimi/instances/DClDN1J3w0C_/stop"}
{"channel":"web","action":"instance.stop","object":"vanilla-1.14.4","outcome":"success","actor":"agent:dsh-0.2.1-alpha.1","caller":"kimi/DClDN1J3w0C_","grant":"joh9by87"}
```

## 6. 已知的边界

- **给开发者密码就没办法了。** 操作员若把开发者密码（连同登录）交给 agent，上面所有限制
  都可以被绕过——它可以直接提权，再自己发票。会话走的门不装自杀闸：人停掉跑着 agent 的
  实例是运维。这套设计只保证"不需要给开发者密码也能让 agent 干活"。
- **票据泄漏的后果被 scope 兜住，致死力被身份兜住。** agent 与面板同一用户，理论上能读到
  自己进程里的票；但即使拿到票，能碰的也只有操作员授权的那几类，且随时可以在面板上掐掉。
  而且致命操作看的是**当下这条连接**的身份：偷来的票从别的进程发，推导不出原主，stop /
  delete / terminal / launch 照样被拒——票能带走的是读与常规变更，不是致死力。
- **藏在树外就没有致命权限。** `setsid` 脱树、经本地系统代理、容器外脚本，推导都是
  「身份不明」：hard-fatal 一律拒绝。这挡的是"藏起来骗闸门"——骗成功的结果只是丢权限，
  不是得到权限。代价是容器外的合法脚本（CI、巡检）想停实例得让操作员做，或换票时自报
  `instanceId`。
- **身份推导是 Linux 与 Windows 的。** Linux 走 `/proc`，Windows 走
  `GetExtendedTcpTable` + JDK 进程快照，两端都带成员校验挡 PID 复用。其余平台（macOS 等）
  推导恒为「身份不明」，退化为自报绑定 + 未绑定拒绝致命操作的保守行为。
- **无人值守没有面板权限。** 这是设计选择，不是缺陷：票到期就得有人重新授权。
- **日志不是脱敏的。** `read` 能读日志，而日志里可能印着 URL、端口、模型回复。面板摘掉了
  结构化凭据（`token`、`url` 字段），但不做日志正文的脱敏——那是运维数据，agent 读得到是
  `read` 的本意。

## 7. 接口一览

| 方法 | 路径 | 需要 | 作用 |
|---|---|---|---|
| `POST` | `/api/developer/agent-auth` | 仅 agent 密码（**免会话**），且必须有人已提权 | agent 自己换票；**绑定由这条连接推导**（住在实例里换票即自动绑对），推导不出才用 body 里的 `instanceId` |
| `POST` | `/api/developer/agent-token` | 已提权的会话 | 操作员直接签票（设置页「生成票并一键复制」）；`instanceId` 可选，Linux 上留空也安全 |
| `GET` | `/api/developer/agent-grants` | 已提权 | 列出当前所有票（密码换的与签的混在一起） |
| `DELETE` | `/api/developer/agent-grants/{id}` | 已提权 | 掐掉一张 |
| `GET/POST` | `/api/developer/agent-password[/clear]` | 已提权 | 设置/查看/清除 agent 密码与它的 cap |

`curl` 一把梭（在实例里执行时，面板会从这条连接推导出 `caller: family/你的实例id` 并绑进票里）：

```sh
TOKEN=$(curl -sk https://127.0.0.1:3080/api/developer/agent-auth \
  -H 'Content-Type: application/json' \
  -d '{"password":"<agent password>","label":"my-script","scopes":["read","mc"]}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

curl -sk https://127.0.0.1:3080/api/mc/instances \
  -H "Authorization: Bearer $TOKEN"
```

### 7.1 让 agent 接管 MC 控制台

票里要有 `mc-console`（不勾就只有 `mc` 的老能力：启停、`console/input`、日志）。换票时
一起要：

```sh
TOKEN=$(curl -sk https://127.0.0.1:3080/api/developer/agent-auth \
  -H 'Content-Type: application/json' \
  -d '{"password":"<agent password>","label":"mc-bot","scopes":["read","mc","mc-console"]}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

TICKET=$(curl -sk -X POST https://127.0.0.1:3080/api/mc/instances/<id>/console/ticket \
  -H "Authorization: Bearer $TOKEN" | sed -n 's/.*"ticket":"\([^"]*\)".*/\1/p')
```

然后用**同一个 Bearer** 连 `wss://127.0.0.1:3080/ws/terminal/$TICKET`（自定义请求头：
`Authorization: Bearer $TOKEN`）。ticket 30 秒内有效、只能用一次；token 不在 URL 里，所以
日志、历史记录、`Referer` 拿到的 ticket 单独没用。

- 这是**独占**的浏览器通道吗？不是——它和网页控制台共用同一个 PTY，两边都能看、都能输入。
- 票没了，控制台就断：掐票、到期、最后一次提权结束，30 秒内这条 WebSocket 会被关掉
  （显式掐票是立即关）。

## 8. 还没做的

- 无人值守：票到期仍要有人重新授权（见 §6）。
- 日志正文的脱敏：`read` 能读到日志，面板不替他做内容过滤（见 §6）。
