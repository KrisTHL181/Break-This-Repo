# 会话交接：面板聊天 v1（2026-11-12）

> 本稿接住 **面板聊天（docs/chat.md）从设计到 v1 落地** 这一段。前稿
> [`2026-11-10`](session-handoff-2026-11-10.md) 的转码结论仍然有效；本稿只写聊天相关
> 的新增事实、判过的结论和下次不用再摸的地方。
>
> **分支状态**：已合入本地 `main`（合并点 `92034bd39`），**未 push、未发布**；
> `web-shell` 只作为后续开发分支保留。设计稿 [`docs/chat.md`](chat.md) 已按实现回写；
> 本稿记录代码、验证与未完成项。
>
> **隐私约定（继续遵守）**：真实域名、内网地址、宿主机路径、口令、真实 API Key
> 一律不写进仓库。

## 0. 当前状态

- 聊天 v1 的核心链路已端到端跑通：SQLite 会话/助手 → REST 命令 → turn 线程 →
  OpenAI/Anthropic SSE → `/ws` 事件 → 前端流式渲染 → 附件/备份/设置。
- 后端 `./gradlew test` 全绿；前端 `npm run typecheck` / `build` / `compat` 绿；
  `shadowJar` 成功。
- 用隔离数据目录 + 假 SSE provider 做过一次真实 turn 的集成测试；用 Playwright
  在 1440×900 与 390×844 下检查了聊天页、模型弹层、独立设置页、能力弹窗。
- **线上面板仍是旧构建**：0.6.85 的静态资源还是旧的 `index-CKtzZtgw.js`。要看到
  本轮界面（跨服务模型选择、独立设置页、代码块修复、消息左右对齐），必须重新
  构建/重启容器；仓库里没有执行部署。

## 1. 提交

```
71636e843 feat(chat): 跨服务模型选择、模型服务入口与原生文件开关
1db4814d7 feat(chat): 独立设置页与模型服务管理；消息对齐与常驻操作
a806c8b8c feat(chat): 聊天页视觉与提权/空态体验
d300c8e65 fix(chat): 流式竞态、附件清理与图片压缩
9d3e5ddae fix(chat): 默认账户兜底与 probe 缓存补全；补端到端 turn 测试
15c2a7c78 feat(chat): 聊天页、Markdown 与助手/设置界面
9da754812 feat(chat): 会话 API、turn 状态机与 agent 权限边界
244eea39b feat(chat): 附件落盘、MIME 嗅探与文本抽取
82815c238 feat(chat): 存储、端点解析与两种协议适配
```

## 2. 已实现

### 2.1 后端

- `ChatStore`：SQLite（WAL），`chat_assistant` / `chat_session` / `chat_node` /
  `chat_message` / `chat_attachment` / `chat_account_profile` /
  `chat_model_capability` / `chat_setting`；启动把残留 `streaming` 归一为
  `interrupted`。
- `ChatTurnManager`：每会话一个 active turn；两种重生成、编辑截断、停止、1 秒快照、
  最终文本落库；`chat:<sessionId>` 推 started/delta/reasoning/usage/finished/error。
- 协议：`openai-completions` + `anthropic-messages`；共享 SSE 解析；图片原生，
  Anthropic PDF `document`，OpenAI 可选 `file` part，Office/文本抽取。
- 附件：sha256 去重、MIME/魔数、宏文档与 SVG/HTML/可执行文件拒绝、图片校验/去 EXIF、
  PDFBox/POI 抽取、OOXML zip bomb 检查、会话删除清孤儿文件。
- 设置/备份：`/chat/settings` 数据、保留、每日备份、立即备份、允许私网/strict。
- 权限：`/api/chat/*` 对 agent ticket 全关（放在 `GET → READ` 之前）；设置/备份/
  probe/能力覆盖要求开发者提权。

### 2.2 前端

- 路由：`/chat` 聊天页、`/chat/settings` 独立设置页。
- 聊天页：助手切换器、会话搜索/日期分组、欢迎建议卡、跨服务模型弹层、消息左右
  对齐、常驻操作栏、variant 分支、附件缩略图、日期分隔、流式光标/三点动画。
- 设置页：模型服务（服务列表/添加供应商/Key/Base/probe/拉模型/模型列表/默认星标/
  能力覆盖）、默认模型、数据与备份、网络。
- 模型弹层：按服务分组、搜索、能力徽章；选中时同时切换会话 `account` 与 `model`；
  底部「添加 / 管理模型服务」。

## 3. 验证

- `./gradlew test`：全绿（SQLite/SSE/路径/权限 + 假 SSE provider 的真实 turn）。
- `npm run typecheck`、`npm run build`、`npm run compat`：绿。
- `./gradlew shadowJar`：成功；当前 jar 约 68 MB。
- 本地面板冒烟：`/api/health`、`/chat` SPA shell、登录、`/api/chat/assistants`、
  文本/图片附件上传与抽取。
- Playwright：聊天页、模型弹层（DeepSeek/Moonshot 分组并切换）、设置页模型服务/
  能力弹窗/移动端；无 console 错误。

## 4. 线上状态与部署

- 0.6.85 面板可登录；开发者密码已由操作员在会话中提供并验证，检查完已主动
  `POST /api/developer/revoke`，没有留下提权。
- 线上账户：DeepSeek（2 个模型）、Moonshot 中国站（6 个）、阿里云百炼（182 个）；
  默认账户 `deepseek|deepseek`，默认模型为空，备份开启但尚无备份。
- 线上仍是旧前端资源；新界面要重新构建/重启容器后生效。本轮没有修改线上数据。

## 5. 已知未做 / 下次注意

- 模型服务的 ON/OFF 开关、模型家族分组、拖拽排序未做；当前是状态点 + 默认星标。
- 原生 OpenAI PDF `file` part 只在代码里支持，未对真实网关联调；默认关闭，用户需在
  「模型能力」里显式打开，避免不支持时 400。
- 备份恢复仍是手动流程（停面板 → 替换 `<data>/chat` → 启动迁移），没有 UI。
- 每日 token 上限是进程内按自然日计数，重启归零；不是持久化账本。
- 音频/视频只保存不送模型；`openai-responses`、fork、搜索、KaTeX/Mermaid 未做。
- 从聊天设置删除模型服务会影响主界面账户与使用它的 dsh 实例，确认框已提示；如果
  只想“聊天里停用”，应另做启用字段，而不是删账户。
- 设置/备份/probe 需要 HTTPS + 开发者提权；纯 HTTP 部署会得到 `tls_required`。