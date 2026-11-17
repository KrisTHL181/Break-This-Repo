# HDSL-web 管理面板（HMCL 像素级复刻版）

HDSL 面板的单页应用（React SPA），视觉与桌面版 HDSL（HMCL 衍生，JavaFX + MonetFX/Material You）
像素级一致：浏览器内以 1280×800（响应式小屏占满）的"窗口"呈现，含 40px 标题栏、Monet 主题、
壁纸、200px 侧栏、jfx 风格弹窗/菜单/snackbar。构建产物打进 jar 后由后端 Jetty 同源提供，
前端只访问同源 `/api/*`、`/ws`、`/i/*`。

## 技术栈

- Vite 6 + React 18 + react-router-dom 6 + TypeScript（严格模式）
- `@material/material-color-utilities`：种子 **#5C6BC0**、FIDELITY 风格、Contrast 0、**Spec 2025**
  生成亮/暗两套 `--monet-*` CSS 变量（`src/theme.ts` 启动时写入，覆盖 `src/styles/monet.css`
  中的默认值；浅色默认值逐字照抄桌面版 `assets/css/blue.css`）
- 纯手写 CSS（`src/styles/`：monet 变量 / base 原语+动画 / components 组件 / layout 布局）
- 状态管理：自研轻量外部 store（`useSyncExternalStore`），WebSocket 事件直接驱动 UI

## 命令

```bash
npm install                 # 网络慢可用：npm install --registry=https://registry.npmmirror.com
npm run dev                 # 开发服务器（默认 5173，代理 /api /ws /i → 127.0.0.1:3080）
npm run typecheck           # tsc --noEmit 严格检查
npm run build               # vite build → dist/（base 为相对路径，可挂载在任意路径下）
npm run preview             # 预览构建产物
```

`predev` / `prebuild` 会自动执行 `scripts/sync-assets.mjs`：把桌面版
`../src/main/resources/assets/img/` 下的壁纸（`wallpapers/*.jpg`）、`icon.png`、`icon-title.png`、
实例图标与 `dsh-version-list-*.svg` 拷到 `public/assets-img/`（已 gitignore，随时可重建）。

开发时先启动后端（Jetty 监听 3080），再 `npm run dev`。后端未启动时页面会显示连接错误/登录失败，属预期行为。

## 目录结构

```
web/
  index.html                # 标题/ favicon（./assets-img/icon.png）
  scripts/sync-assets.mjs   # 桌面版图片资源同步（predev/prebuild 钩子）
  public/assets-img/        # 同步产物（壁纸/图标，勿手改）
  src/
    main.tsx                # 入口：initTheme + 系统主题监听 + 涟漪 + BrowserRouter
    theme.ts                # Monet 调色板生成（material-color-utilities）+ 亮/暗/跟随系统 + localStorage
    i18n.ts                 # 界面文案（照抄 I18N_zh_Hans.properties，注释标出出处 key）
    version.ts              # 服务端版本号订阅（标题栏 "… vX.Y.Z"）
    selection.ts            # "当前选中实例"（实例列表 radio，localStorage）
    App.tsx                 # 路由表 + 守卫 + Decorator 窗口外壳 + 壁纸背景
    api.ts / ws.ts / launch.ts / acp.ts / store.tsx / actions.ts / hooks.ts / types.ts / utils.ts / constants.ts
                            # 行为契约层（REST 调用、WS 订阅归约、启动自动开标签页、ACP 状态机）——与旧版一致
    styles/                 # monet.css（主题变量）base.css（原语/动画/涟漪/滚动条）
                            # components.css（卡片/按钮/弹窗/菜单/snackbar/列表行/日志/气泡）layout.css（窗口/标题栏/侧栏/启动面板）
    components/             # Decorator（窗口+标题栏+关于弹窗）、SideBar（主导航/子侧栏）、LaunchPane（启动面板+
                            # 实例选择菜单）、Dialog/ConfirmDialog、PopupMenu、Snackbar、Ripple、icons（path 逐字提取自
                            # 桌面版 SVG.java）、ProgressBar、LogView、ConsolePanel（ACP 气泡）、InstancePluginsPanel、
                            # InstanceExtrasPanel（会话/技能）、ExportInstanceModal、ApprovalDialog、FileDropInput、
                            # InstanceIcon、VersionListIcon
    pages/
      LoginPage.tsx         # /login：壁纸 + 居中 HMCL 登录卡（surface-container-high、20px 标题）
      MainPage.tsx          # /：主导航侧栏 + 壁纸 + 启动面板 + 启动/安装进度浮层 + 运行中浮层
      InstancesPage.tsx     # /instances：文件夹项+底部操作框；刷新/搜索工具栏；radio 实例行（火箭/更多菜单）
      InstallInstanceWizard.tsx  # /instances/new：选择版本 → 快速安装（底部 取消/上一步/下一步）
      InstanceDetailPage.tsx     # /instances/:id：操作框（启动/浏览/管理菜单）+ 8 个 tab
                            # （实例管理/自动安装/插件/技能包/会话/日志/控制台/详情；旧 tab 值兼容）
      DownloadPage.tsx      # /download?tab=game|modpack|plugin|skill：NEW GAME + 游戏内容 侧栏；
                            # 过滤卡片（名称/类型/实心刷新）+ 列表行（图标+徽章+时间+地球/箭头）+ 导出记录
      InstallModpackWizard.tsx   # /install/modpack：本地文件 / URL / 市场 三卡片向导（.dspack 上传）
      SettingsPage.tsx      # /settings?tab=…：通用(服务器信息/账户会话) / Node.js / 启动器(通用/外观/下载源/HTTPS 证书)
                            # / 帮助(环境体检/关于)
      AccountsPage.tsx      # /accounts：账户卡网格（首字母头像）+ 添加(供应商目录→表单)/编辑/删除/验证
```

## 主题

- 三种模式：跟随系统（默认）/ 浅色 / 深色；设置 → 外观切换，`localStorage["hdsl.theme.mode"]` 持久化；
  `prefers-color-scheme` 变化实时响应。
- 浅色默认值与桌面版 `blue.css` 逐项一致（#5C6BC0 种子、primary-container #5C6BC0、
  on-primary-container #F8F6FF、secondary-container #D0D5FD、surface #FBF8FF……含 -50/-80 透明派生）；
  暗色由 material-color-utilities 以同种子生成，非手拍。

## 与旧版的功能映射（入口无丢失）

| 旧版页面 | 新版位置 |
|---|---|
| `/` 实例网格（HomePage） | `/` 主页面（启动面板）+ `/instances` 实例列表行 |
| `/instances/new` 两步向导 | `/instances/new`（同路径，HMCL 向导样式） |
| `/instances/:id` 七个 tab | `/instances/:id` 八个 tab（概览/设置并入"实例管理"；新增"详情"；"设置"旧链接自动落到 manage） |
| `/packs` 市场/上传/导出记录 | `/download?tab=modpack` + `/install/modpack`（旧 `/packs` 重定向） |
| `/plugins` 插件市场 | `/download?tab=plugin`（旧 `/plugins` 重定向，保留 `?instance=` 预选） |
| `/accounts` | `/accounts`（账户卡 + 添加/编辑/删除/验证，供应商目录流程不变） |
| `/settings`（服务器信息/TLS/退出登录） | `/settings?tab=general` + `tab=tls`（并新增外观/下载源/Node.js/关于） |
| `/doctor` 体检 | `/settings?tab=doctor`（旧 `/doctor` 重定向） |
| 实例"设置"tab 的导出整合包 | 实例"管理"菜单 → 导出整合包（ExportInstanceModal） |

启动流程保持不变：点击启动瞬间 `window.open("about:blank")` 保存引用 → 收到 RUNNING+url 的 WS
事件后自动导航到 `/i/<id>/?token=…`；弹窗被拦截时运行中视图提供 [打开 dsh] 兜底。

## 与桌面版的差异（仅有的三处 + 必要降级）

1. 首屏登录：壁纸 + 居中 HMCL 风格登录卡片（"登录 HDSL-web"）。
2. 窗口按钮：最小化/关闭保留视觉但无操作；帮助(?) = 关于弹窗。
3. 新增条目：设置里"HTTPS 证书/下载源/环境体检"、实例侧栏"日志/控制台"，均用既有 HMCL 组件样式。

其余必要降级（后端契约无对应端点，入口保留并提示）：实例"浏览"菜单（无法打开服务器目录）、
"从 URL 安装整合包"（提示改用本地 .dspack）、Node.js 运行时页（服务端托管）、
下载源 npm registry（保存于 localStorage）。

## ACP 控制台状态机（`src/acp.ts`，与旧版一致）

后端经 `/ws` 提供 ACP 通道。每实例一份状态，tab 切换不丢（路由动画容器 key 只取 pathname），
离开详情页时 `disposeConsole` 尽力 `acp-stop` 并释放。阶段 `idle → starting → ready/error`；
流式 chunk 按轮次归入气泡（`.bubble`：inverse-surface-80 底、圆角 2），工具调用卡片化可展开。

## 与后端的契约假设

与旧版相同（见 git 历史中的 README 或 docs/），此处不再赘述；api.ts / ws.ts / store.tsx 未改契约。
