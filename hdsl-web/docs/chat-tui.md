# 终端客户端的聊天界面（`hdsl-tui`）

状态：P0 + P1 已实现。目标是把面板聊天搬进 Go 客户端，交互参考 Kimi Code / Qwen Code
这类终端 agent 的聊天界面：单列消息流、贴底输入框、斜杠命令、Markdown、思考/搜索折叠。

不新增服务端接口：REST 用 `/api/chat/*`，实时事件用 `/ws` 的 `chat` / `chat:<id>` topic。

## 1. 入口与账号

- 主菜单第一项「聊天」。没有会话时先选助手新建；有会话时打开最近更新的一条。
- 登录复用客户端已有的面板会话 cookie（`~/.config/hdsl-tui/config.json`）。
- 进入聊天时会问一次开发者密码：**读取聊天设置**（默认模型、搜索、每日限额）需要提权，
  和网页版「聊天设置」同一个门槛。可以跳过；聊天本身只需要登录，服务端也不要求提权。
- **开发者守卫 `all` 档**：面板把 `/api/*` 全部关起来，聊天列表和发送也会 403。客户端
  从 `/api/developer/status` 读到 `guardScope=all` 后，进入聊天前**强制**要求开发者密码；
  拒绝就明确说明“all 档未提权无法聊天”，不再等一次 403。`off/proxy/instance` 不影响聊天。
- 会话恢复时遇到 403 不再当成“登录失效”：`/api/developer/status` 在守卫白名单里，
  能读到就说明 cookie 仍然有效，只是缺提权。
- `Ctrl-E` 随时重新提权；`/settings` 显示已读到的设置，`/status` 显示会话状态。

## 2. 界面

```
 聊天 · 标题                                      deepseek-flash · 搜索 · 生成中
──────────────────────────────────────────────────────────────────────────────
 你 · 11-16 00:20
 这个报错怎么修
 助手 · 11-16 00:20
 思考 3.2s
 搜索：go tui · 6 条
 **原因**是 …
──────────────────────────────────────────────────────────────────────────────
 ❯ 输入的内容█
── 输入 ──────────────────────────────────────────────────────────────────────
Enter 发送 · Ctrl-J 换行 · Ctrl-O 展开 · Ctrl-B 会话 · Ctrl-C 停止 · Esc 返回
```

- 助手正文用 Markdown 渲染（标题、粗体、行内代码、代码块、列表、引用、链接、分隔线）。
  渲染器只改前景色/属性，不动背景，所以长行、宽字符和对话框底色都不会错位。
- 思考与搜索默认折成摘要；思考在折叠状态也显示**最后两行**，生成中的标题带
  `生成中 Ns`，长思考不会看起来像卡住；`Ctrl-O` 展开完整推理与来源列表。
- DeepSeek 预设的 `blocks`（thinking / server_tool_use / web_search_tool_result）与
  OpenAI 协议的 `parts` 都走同一条时间线：搜索显示 query 与命中数，来源按工具调用配对。
- 聊天页沿用客户端的蓝底（`37;44`），用户行 `❯` 青色、助手行 `●` 绿色；不另做主题。
- 图片不在终端里绘制，附件也不做上传/下载入口；消息里的附件只显示文件名。
- 流式：`chat-delta` / `chat-reasoning` / `chat-block` 追加，约 10fps 重绘；
  `chat-turn-finished` 后重新拉一次完整会话，保证时间线和最终正文一致。

## 3. 键位

对齐 Kimi Code 的习惯：Enter 提交、Ctrl-J 换行、Ctrl-O 展开、↑/↓ 在输入首/末行
走历史、Ctrl-C 停止、Ctrl-D 退出、`/` 命令补全。

| 键 | 作用 |
|---|---|
| `Enter` | 发送（斜杠命令则执行） |
| `Ctrl-J` | 输入换行（终端里 Shift-Enter 不可靠，所以用 Ctrl-J） |
| `↑` / `↓` | 光标在首行/末行时走上一条/下一条历史，否则移动光标 |
| `Esc` | 生成中：停止本轮；有补全：关闭补全；都没有：返回主菜单 |
| `Ctrl-C` | 生成中：停止；输入非空：清空；空输入双击退出聊天 |
| `Ctrl-D` | 空输入且空闲时退出聊天 |
| `Ctrl-O` | 展开/折叠思考与搜索来源 |
| `Ctrl-B` | 会话列表（Enter 打开，Esc 返回；Kimi 里这是后台任务，这里没有 shell 模式） |
| `Ctrl-E` | 开发者提权（Kimi 里 Ctrl-E 是展开审批内容，这里没有审批面板） |
| `PgUp` / `PgDn` | 滚动消息（贴底时自动跟随） |
| `Ctrl-P` / `Ctrl-N` | 输入历史上一条/下一条（额外的老键位，保留） |
| `Tab` | 斜杠命令补全 |

## 4. 命令

| 命令 | 说明 |
|---|---|
| `/help`（`/h`、`/?`） | 命令与快捷键 |
| `/new`（`/clear`） | 选助手新建会话 |
| `/sessions`（`/resume`） | 会话列表 |
| `/assistant` | 选助手并新建会话 |
| `/model [id]` | 不带参数打开模型选择弹窗（从账户模型列表里挑，标注当前与能力）；带 id 直接切换 |
| `/title [text]`（`/rename`） | 查看/修改会话标题 |
| `/search` | 开关本轮起生效的联网搜索 |
| `/stop` | 停止正在生成的回答 |
| `/regen` | 重新生成最后一条回答（`in_place`） |
| `/edit` | 编辑最后一条提问并从它重新生成 |
| `/copy` | 复制最后一条回答（OSC 52） |
| `/settings`（`/config`） | 已读到的聊天设置（需提权） |
| `/status` | 会话状态 |
| `/quit` | 离开聊天（也可以 Ctrl-D 或空输入双击 Ctrl-C） |

不认识的 `/xxx` 按普通消息发送，这是 Kimi Code 的约定。

## 5. 代码位置

- `internal/panel/chat.go`：聊天 REST 类型与方法；
- `internal/panel/events.go`：通用 `/ws` 网关（订阅、心跳、事件通道）；
- `internal/md/render.go`：Markdown → ANSI；
- `internal/ui/textarea.go`：多行输入（光标、换行、历史）；
- `internal/ui/key.go`：新增 Ctrl-J / Ctrl-P / Ctrl-N / Ctrl-O / Ctrl-B / Ctrl-E 解码；
- `internal/app/chat.go`：聊天页状态机、命令、渲染；
- `internal/app/app.go`：主菜单入口。

## 6. 测试

- `internal/md/render_test.go`：样式、换行、代码块、控制字符过滤、列表/引用。
- `internal/ui/textarea_test.go`：输入、Ctrl-J、光标、换行、历史、上下移动。
- `internal/panel/chat_test.go`：发送往返、时间线解码、提权失败保留状态码。
- `internal/panel/events_test.go`：WebSocket 握手 cookie、订阅帧、事件投递。

## 7. 未做（P2）

- 附件上传/下载、图片预览；
- 正文引用角标的点击（终端只显示链接文本）；
- steer（服务端当前只暴露 followup/cancel，没有 REST steer）；
- 长会话分页：`GET /api/chat/sessions/{id}` 仍一次返回全部消息（上限 64 MiB，超过会明确
  报错），客户端只渲染最近的内容。