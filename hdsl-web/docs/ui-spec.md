# HDSL 桌面版 UI 像素级复刻规范（网页版施工图纸）

> 目标：HDSL-web 的 React 面板与桌面版 HDSL（HMCL 衍生，JavaFX + JFoenix + MonetFX/Material You）像素级一致。
> 视觉基准：`docs/ui-reference/` 下的真实截图（浅色 = Xvfb 实跑捕获，深色 = README 实拍）。
> 目标代码库（只读参考）：`/home/coder/code/dsh/HDSL`。

## 1. 窗口外框（Decorator）

| 项目 | 值 | 来源 |
|---|---|---|
| 窗口样式 | 透明 Stage，内容圆角 8px，外阴影 DropShadow rgba(0,0,0,0.4) blur 10 | `ui/decorator/Decorator.java:101,110,119-125`，`MainWindowPane.java:61,90-91` |
| 内容最小尺寸 | 800×490（标题栏 40 + 内容 450） | `Decorator.java:89-92` |
| 标题栏 | 高 40px；左侧：返回箭头（可返回页）/主页或关闭图标 + `icon-title.png`(24×24) + 标题（14px 粗体，左距 8）；右侧：帮助(?) / 最小化 / 关闭 三个 40×40 圆形按钮（SVG 图标） | `MainWindowPane.java:116-123,171-199,247-299`，`root.css:673-679,773-788` |
| 标题栏背景 | **primary-container（浅 #5C6BC0）**，文字/图标 **on-primary-container（#F8F6FF）** | `MainWindowPane.java:202-218`，`Themes.java:1073-1092,1116-1127` |
| 网页版处理 | **整页铺满视口**（网页化适配，不模拟桌面窗口）：width/height 100vw/100vh、无圆角无阴影；返回箭头保留（导航功能）；**桌面版的 帮助/最小化/关闭 三键属于窗口系统，网页不存在，全部移除** | — |

## 2. 左侧主导航（宽 200px，`MainPage`）

结构：`AdvancedListBox`（内容 padding `12 0 0 0`），主页面侧边栏挂半透明板，中心透出壁纸。

顺序（`MainPage.java:150-164`）：

| 分区标题（12px，大写，padding `8 16`，下 1px 分隔线） | 条目 | 图标 |
|---|---|---|
| 账户 | 账户卡片（32×32 头像，标题=账户名/无账户提示，副标题=供应商/提示语） | — |
| 游戏 | 当前实例项（32px 实例图标，标题=实例名，副标题=版本）；无实例时显示"尚无实例/安装一个新实例" | — |
| | 实例列表 | FORMAT_LIST_BULLETED |
| | 下载 | DOWNLOAD |
| 通用 | 设置 | SETTINGS |

- 条目容器 padding `10 16`；左侧图标 20px（或 32px 图形）；文字区 padding-left 10；标题 13px（on-surface），副标题 10px（on-surface-variant）。
- 选中态：背景 secondary-container-50（#D0D5FD80），标题 on-secondary-container 加粗，图标同色。
- 涟漪：圆形，fill on-secondary-container。

## 3. 主页面

- 背景图：`assets/img/wallpapers/2021-08-26.jpg`（默认；另有 2015-06-22/2016-02-25），cover 铺满、底部居中。
- 内容区**不盖遮罩**（壁纸直透）；仅侧边栏盖 surface-transparent-50。
- 启动面板（右下，margin 20）：整 pane 230×57；主按钮 200×55 左圆角 4，背景 primary-container，两行文字（16px 动作 + 12px 实例/版本，on-primary-container）；右侧 27×55 菜单钮（圆角 4，ARROW_DROP_UP 图标），中间 3px 竖直渐变分隔线。
- 动作：启动/停止当前实例（连带进度展示）；箭头弹实例选择菜单（jfx-popup 样式：surface 底、圆角 4）。

## 4. 子页面骨架（所有内页）

- 标题栏显示"← 页面标题"，? _ × 右侧不变。
- 左侧 200px 子侧边栏（半透明）+ 右侧内容区（**盖 surface-transparent-50 遮罩**），内容根 margin/padding 10。
- 列表页内容 = 卡片（card：surface-container-low-80、圆角 4、阴影 depth-1）包裹 工具栏行 + 列表；空态为居中 20px 灰字。

### 实例列表页（`InstancesPage`）
子侧边栏**两层原地切换**：分区「dsh 实例」（instances 文件夹项，选中态 = 当前层）+
分区「第三方品牌」→「品牌实例」（地球图标）。点哪层，右侧内容区就换成哪层——只换查询串
（`/instances` ↔ `/instances?view=brands&brand=<品牌>`），页面不跳转、不重挂载；分区标题
自带的 1px 底线即两层之间的分割线，移动端 `side-category` 隐藏、两层自动变 chip。
底部操作框（限高 144）：安装新实例(+) / 安装整合包(立方) / 全局设置(齿轮)。
内容（dsh 层）：工具栏（刷新/搜索，搜索态整行变输入框）、实例行（radio 选择 + 32px 图标 +
两行文字 + 右侧火箭/更多圆形 30px 按钮）、行底 1px 分隔线。
旧链接 `/instances?brand=x` 与 `/brands/<品牌>` 都落到品牌层。

### 品牌实例层（`BrandsView`，实例列表页内原地切换，非独立页面）
内容区自上而下：
1. 卡片：品牌页签行——4 个品牌（ZCode / Kimi Code / OpenCode / Claude Web，带各自的品牌标志
   与实例数标签；标志来源与商标声明见 `NOTICE`），点页签只换下面这块内容（同样只改查询串）。
2. `.hint warning`（`CollapsibleNote`）：第三方警示与实验性说明。
3. 卡片「版本」（默认折叠成一行摘要，实测 69~77px）：已安装的版本构建 tag + 「版本构建按品牌共享」
   小字 + 「安装新版本」按钮 + 展开箭头；展开后是版本搜索/下拉/安装/刷新与安装进度、日志。
   zcode 分支换成发行包检测 + 「构建发行包」。
4. 卡片「实例」（`flex:1; min-height:0`，主卡，实测 469~725px）：头部新建行（名称 + 版本下拉 +
   创建；该品牌无实例时名称预填品牌名）+ 实例列表（`list-body` 真滚动）。实例行 = radio（"当前实例"，
   与 dsh 列表同一套）+ 名称 + 状态徽标 + `id · 版本 · 端口/打开地址` + 行内版本下拉（全量目录，
   已装/未装打标）+ 启动停止/打开/更多（信息 / 重命名 / 插件 / 日志 / 删除）。行底 1px 分隔线。

版本切换三条路径：已装且已停止 → 直接 `PATCH`；已装但运行中 → 确认「停止并切换」→ 停止 → `PATCH`；
未装 → 确认「先安装 X 再切到它」→ 品牌级安装（行内进度）→ `PATCH`。插件弹窗只按
`GET …/instances/<id>/plugins` 的 `{supported, plugins[]}` 渲染（现在一律 `supported:false`）。

### 下载页（`DownloadPage`）
子侧边栏分区：NEW GAME（新实例[手柄] / 整合包[立方]）、ADDONS（插件[拼图] / 技能包[纹理]）。
内容：过滤卡片（"名称"标签 + 输入框 + "类型"下拉(max 150) + 右侧实心 Refresh 按钮）；列表行（32px 图标 + 标题=版本号/名称 + channel 徽章 + 副标题=时间 + 右侧 地球/箭头 30px 圆钮）；加载中显示 spinner；失败显示重试。

### 设置页（`SettingsPage`）
子侧边栏：全局实例设置(手柄) / Node.js(咖啡)；分区"启动器"：通用(调谐) / 外观(调色) / 下载源(下载)；分区"帮助"：关于(信息)。右侧 ComponentList 卡片组 + 小标题（16px 帮助图标 + 文字，padding `8 0 0 0`）。
网页版映射：通用（服务器信息/语言）、外观（主题亮暗）、下载源（服务端的 npm/pnpm 下载源，见下）、HTTPS 证书（网页新增，放"启动器"分区末位，样式一致）、环境体检（帮助分区）、关于（版本/版权/第三方声明）。

**「下载源」卡片**（原先是只写 localStorage 的摆设，现在真调后端）：原生 `<select className="input">` 选预设——选项**由服务端下发**（`GET /api/settings/registry` 的 `presets[]`，界面不硬编码）：「跟随部署环境（默认）」/ npm 官方 / npmmirror（淘宝）/ 中科大 / 腾讯云 / 华为云，末尾固定一个「自定义…」哨兵；每个预设的 `note`（服务端记的实测速度）显示在下拉右侧。选中「自定义…」时才条件渲染手填 `<input>`（沿用账户页「选到某值才显示」的写法；`placeholder` 用 `https://registry.example.com` 这类示例地址，不出现真实内网域名）。输入**即时行内校验**：不通过时用 `.form-error` 红字给出原因，并把保存按钮置灰——只允许 `http`/`https`、必须能解析出主机名、禁止用户口令 / 查询串 / 片段，以及空白、控制字符、`"` `'` `` ` `` `\` `#` `$` `;` `|` `&` `@`，长度 ≤ 200，尾斜杠自动去掉。前端那套规则与服务端是同一份口径，服务端仍是权威（非法值 400 且设置不变）。卡片另有一行「当前生效」＝来源 tag（面板设置 / 部署环境变量 / 默认）+ 生效的地址（`.comp-value.mono`：12px 等宽 + `word-break: break-all`）；底部一行小字写明优先级与「保存后对新安装的实例与构建生效；选『跟随部署环境』即清空面板设置」。保存按钮在 `busy`、**未改动**、或有校验错误时禁用；保存成功直接用 POST 的返回体回读（不再多打一次 GET），失败走 toast（沿用 `TlsTab` 的真调后端模板）。窄屏不用额外媒体查询：`.comp-value` 本身就是 `flex-wrap: wrap` + `min-width: 0`，下拉 `width: 260` 且带 `maxWidth: 100%`、手填框 `minWidth: 0`，360px 屏也不横向溢出（旧版内联的 `minWidth: 220` 会）。

### 账户（`AccountListPage`）
账户卡片 `dsh-account-card`：surface-container-low-80、圆角 4、depth-1 阴影；头像（皮肤/首字母单色块 16px 圆角）；名称/供应商/模型行；操作按钮。底部"添加账户"卡片。

「添加账户」弹窗分两步。第一步选供应商：顶部一行 = 搜索框（按 名称 / id / 端点 过滤）+ 刷新按钮（`?refresh=1` 强制重抓，期间禁用并显示「刷新中…」）+「共 N 家供应商 · x 分钟前更新」；下面是自适应卡片网格 `repeat(auto-fill, minmax(180px, 1fr))`（外层限高 50vh、内部滚动），卡片显示 名称 / 端点（没有固定端点时写「需自行填写端点」）/「N 个模型」/ 协议 tag，dsh 自家目录里没有的供应商标「目录」tag，`protocol` 为 null 的卡片禁用并写明「dsh 不支持该协议（models.dev 用 xxx）」；搜索无结果时给一行提示而不是白屏，末尾固定一张「自定义」卡片。第二步填表单：选中供应商后端点自动带出，协议与密钥环境变量只作提示（账户类型由后端推断）。

「默认模型」是 `ModelSelect`——受控的「输入框 + 过滤下拉」：列表项两行（`name` / `id · 128K 上下文 · $0.27/$1.1`，缺的字段整段不显示），首项固定「留空（由 harness 决定）」（选中即清空），输入按 id 与 name 大小写不敏感过滤，点选回填模型 **id**，Esc 或点外部关闭，**手输一直保留**。新建与编辑两处都用它。

#### 模型目录（models.dev）
- 来源 `https://models.dev/api.json`（约 5MB、200+ 家供应商），服务端与 dsh 自家目录合并后由 `GET /api/models/providers` 下发供应商名单，单家的模型走 `GET /api/models/providers/{id}`（dsh 认识但目录里没有 → 200 + 空模型数组，不是 404）。
- 缓存：**TTL 12 小时**，落盘 `/data/hdsl/catalog/`；`?refresh=1` 绕过后端缓存强制重抓（同步请求，界面按「刷新中…」反馈）；目录整个拉不到且无缓存时返回 502。
- **协议以 dsh 自家目录为准**，models.dev 只用来补模型名、端点与文档链接（`protocolSource` 标明来源）。dsh 自家目录里**没有记录协议**的供应商（`assets/dsh-providers.txt` 里 `api` 为空，例如 `github-copilot`、`cloudflare-*`、`opencode`）不以 dsh 那条记录出现，而是交给 models.dev 那一份推导：推得出的（`@ai-sdk/openai-compatible` 之类）照常可选，推不出的（`@ai-sdk/azure`、`@ai-sdk/amazon-bedrock`、`@ai-sdk/google-vertex` 这类签名体制）仍然列出，但卡片禁用并写明原因，不假装可用。
- 降级：目录不可用时先退回磁盘缓存（`source:"cache"`）；彻底不可用则供应商列表为空并提示一行，用户仍可走「自定义 Endpoint」；模型下拉在拉取失败或这家没有模型时退化为普通输入框 + 一行 12px 灰字说明，不挡路、不弹错。
- 取舍：面板里配的代理只传给 dsh 子进程，目录抓取不走它（与 `DshAccount` / `DshPluginCatalog` 等现有出站请求一致）；模型 id 用 models.dev（AI SDK 口径）的名字，与厂商 `/models` 不一定一致，所以「留空」是默认项、手输一直保留。

## 5. 实例详情页组（`InstancePage`）

标题"实例管理 - <实例id>"。子侧边栏：实例管理(齿轮) / 自动安装(部署码) / 插件(拼图) / 技能包(纹理) / 会话(包裹) /（网页新增：日志 / 控制台）/ 详情(信息)；顶部独立操作框：启动(火箭)/停止(取消) + 浏览(文件夹) + 管理(菜单：启动/停止、日志、导出整合包、重命名、复制、删除)。

- 插件页：工具栏（jfx-tool-bar-button 高 37 圆角 5：刷新/添加文件/从来源安装/下载/打开目录 + 搜索；选中行后变选择工具栏：启用/停用/删除/全选/取消）；列表行（checkbox + 32px 图标 + 两行文字 + 版本徽章 + 右侧信息/删除 30px 圆钮）。
- 其余子页复用同构列表/卡片。
- 自动安装 = 快速安装预设（AppBoot 选择）。

## 6. 向导（`DecoratorWizardDisplayer`）

以页面推入导航栈，标题"向导名 - 步骤名"；每步内容 = 垂直卡片选项（TwoLineListItem + 右箭头，max-width 400、间距 8）或表单；底部右侧 取消 / 上一步 / 下一步（文字按钮）。

## 7. 主题（Monet / Material You）

- 机制：种子 **#5C6BC0**、ColorStyle FIDELITY、Contrast DEFAULT、Spec 2025；亮暗双套 `-monet-*` CSS 变量。
- 网页实现：npm `@material/material-color-utilities`，启动时用种子生成 light/dark 两套变量写入 CSS 自定义属性；默认跟随 `prefers-color-scheme`，外观页可手动切换并持久化（localStorage）。
- 浅色默认全量色板（`assets/css/blue.css`，照抄）：

| 变量 | 值 |
|---|---|
| primary / on-primary | #4352A5 / #FFFFFF |
| **primary-container / on-primary-container** | **#5C6BC0 / #F8F6FF**（标题栏、启动钮） |
| secondary-container / on-secondary-container | #D0D5FD / #565B7D（选中态、徽章） |
| surface / on-surface | #FBF8FF / #1B1B21 |
| surface-variant / on-surface-variant | #E2E1EF / #454651（副文字/图标/分隔） |
| surface-container-low(est) | #F5F2FA / #FFFFFF（卡片/输入框） |
| surface-container / high / highest | #EFEDF5 / #E9E7EF / #E3E1E9（列表行/对话框/菜单） |
| outline / outline-variant | #767683 / #C6C5D3 |
| error / error-container | #BA1A1A / #FFDAD6 |
| tertiary 系 | #775200 / #976900 / #FFDEAC（警告） |
| inverse-surface / inverse-on-surface | #303036 / #F2EFF7（tooltip/snackbar） |
| 透明派生 | primary-50 #4352A580、surface-50 #FBF8FF80、surface-container-low-80 #F5F2FACC、secondary-container-50 #D0D5FD80 |

- 暗色：用 material-color-utilities 以同种子生成（HSLCToner 对应 Fidelity 风格），不要手拍。

## 8. 组件样式速查（`root.css`）

- `.card`：bg surface-container-low-80、圆角 4、padding 8、阴影 `0 10px rgba(0,0,0,0.26)` depth-1 微调（gaussian 10 / spread 0.12 / 偏移 -1,2）。
- 实心按钮 `.jfx-button-raised`：bg primary、文字 on-primary、14px；描边按钮 `.jfx-button-border`：透明底、outline 0.2px、圆角 5、文字 primary。
- 工具栏按钮 `.jfx-tool-bar-button`：高 37、圆角 5、透明底、图标 20px+文字 14px。
- 圆形图标按钮 `.toggle-icon4`：30×30。
- 输入框：bg surface-container-highest、粗体、focus 边 primary、padding 8。
- 复选框/单选：选中 primary。开关 `.jfx-toggle-button`。
- 进度条：track secondary-container、bar primary。
- 徽章 tag：bg secondary-container、文字 on-secondary-container、圆角 2、padding 2、12px。
- 提示条 `.hint`：圆角 5 padding 6（info=primary 底/error=error-container/warning=tertiary）。
- 弹窗：遮罩 rgba(0,0,0,0.1)；布局 bg surface-container-high、圆角 4、padding `24 24 16 24`；标题 20px 粗；正文 pref 400；按钮区右对齐（确认=primary 文字、取消=on-surface-variant 文字）。缩放淡入。
- Snackbar：底部居中，inverse-surface 底、inverse-on-surface 文字。
- 弹出菜单：bg surface、圆角 4、项 padding `8 16` 间距 10、12px。
- 滚动条：轨道 #F1F1F1、thumb #BCBCBC（浮动式，hover 才显示）。

## 9. 图标

- 全部内联 SVG path（`HDSL/src/main/java/org/jackhuang/hmcl/ui/SVG.java`，Material Symbols outlined/24px）。网页版把用到的枚举提取成 React 组件，按 currentColor 上色。
- 需要的映射：SETTINGS、DOWNLOAD、FORMAT_LIST_BULLETED、EXTENSION(+FILL)、PACKAGE2(+FILL)、STADIA_CONTROLLER(+FILL)、TEXTURE、LOCAL_CAFE(+FILL)、STYLE(+FILL)、INFO(+FILL)、TUNE、ARROW_BACK/FORWARD、CLOSE、SEARCH、REFRESH、ROCKET_LAUNCH、CANCEL、DELETE/DELETE_FOREVER、FOLDER_OPEN、MENU、HELP、WARNING、ERROR、CHECK、SELECT_ALL、PUBLIC、ADD、HOME、VISIBILITY、MINIMIZE_CENTER 等（以页面实际需要为准，从 SVG.java 抄 path 数据）。

## 10. 字号

窗口标题 14 粗；分区标题 12；侧栏条目 13/10；列表行标题 15 / 副 12 / 徽章 12；启动钮 16+12；工具栏 14；弹窗标题 20 / 正文 14；空态 20；日志等宽 11。字体：系统 sans（中文回退栈）。

## 11. 动画（Motion.java 的 CSS 映射）

- EASE=(0.25,0.1,0.25,1)；EASE_IN_OUT_CUBIC_EMPHASIZED ≈ cubic-bezier(0.2,0,0,1) 近似即可。
- 页面导航（Controllers.navigate）：200ms——旧页 100ms 淡出+左移 30px，新页淡入归位。
- FORWARD 前进：旧页左移 20%+淡出，新页右侧 20% 淡入，200ms。
- Tab 切换：SLIDE_UP_FADE_IN 400ms。
- 点击涟漪：JFXRippler 圆形扩散（CSS 实现）。
- 弹窗：缩放 0.9→1 + 淡入 200ms。

## 12. 文案

以 `HDSL/src/main/resources/assets/lang/I18N_zh_Hans.properties` 为准（实例列表/下载/设置/账户/启动游戏/安装新实例…直接取原文）。

## 13. 网页版与桌面版的差异（仅有的四处）

1. 首屏登录：壁纸 + 居中 HMCL 风格登录卡片（圆角 4、surface-container-high、20px 标题"登录 HDSL-web"）。
2. 窗口控制：帮助/最小化/关闭三键为桌面窗口系统功能，网页版不呈现；「关于」在设置页。
3. 新增条目：设置里"HTTPS 证书"、实例子导航里"日志/控制台"、帮助里"环境体检"——全部用既有 HMCL 组件样式。
4. 设置里"下载源"承载的是**服务端**的 npm/pnpm 下载源（预设下拉 + 自定义 URL + 行内校验 + 显示当前生效源与来源）：桌面版是本地启动器，没有这一层"服务端设置"。

## 14. 移动端适配（手机浏览器远程启停 dsh）

桌面版没有这一节——它只跑在桌面窗口里。网页版要能被手机浏览器打开使用，所以多了一层版式：

**判定条件**（两处必须逐字一致：`web/src/styles/mobile.css` 顶部与 `web/src/hooks.ts` 的 `MOBILE_QUERY`）

| 条件 | 查询 | 用途 |
| --- | --- | --- |
| 移动版式 | `(max-width: 760px), (max-height: 520px)` | 抽屉、标签条、贴底操作栏（后者覆盖横屏手机，如 844×390） |
| 触摸优化 | `(hover: none) and (pointer: coarse)` | 44px 可点区域、16px 输入框（iOS 聚焦不缩放）；与宽度无关，平板也生效 |

**三条版式规则**（全部在 `styles/mobile.css` 内，桌面端零变化）

1. **主导航侧栏 → 抽屉**：`position: fixed` + `translateX(-100%)`，标题栏出现汉堡按钮（`Decorator.tsx`）唤出，`.open` 类滑入；点条目、点遮罩、或路由变化都收起（开合状态在 store 的 `sidebarOpen`）。抽屉用不透明 `surface` 底色，不透明的可读性优于透出壁纸。主导航本身只挂在主页（桌面版就是这个结构），所以汉堡按钮也只在主页出现，别的页面点了没抽屉可开。
2. **子侧栏 → 顶部横向标签条**：`.page-with-sidebar` 转列、`.sidebar-sub` 转横排可滑动；分区外层 `div` 也必须转横排（DOM 里它是每段条目的包裹层），分区之间用一条浅分隔线区分；紧凑 chip（`padding: 8px 12px`、圆角 18px）。例外：实例页在 `InstanceDetailPage.tsx` 手写了一个 `nav.sidebar-sub.sidebar-stack` 外壳，把「启动/浏览/管理」操作框和标签条装在一列里——那一层保持竖排（操作框一整行 + 标签条一整行），只让它内部的 `SubSideBar` 变标签条。
3. **启动面板 → 贴底整条操作栏**：`.launch-pane` 改 `position: fixed` 铺满底部，主钮 56px 高、与箭头钮同高；进度/运行浮层变成它上方的底部卡片；内容区留出对应底部内边距。手机主页（壁纸上没有侧栏了）补一张「当前实例」卡片，状态 + 启动/停止 + 打开 dsh + 管理三个动作一屏可达（`MainPage.tsx` 的 `MobileHome`）。

另有：`100dvh` 跟随实际可视高度（地址栏不再顶掉一屏）、`env(safe-area-inset-*)` 避让刘海与手势条（需 `index.html` 的 `viewport-fit=cover`）、弹窗 `min-width` 在窄屏解除、登录卡片宽度交给 CSS（原来内联 380px 在 360px 屏上必然溢出）。

**加到主屏幕**：`web/public/manifest.json` + `apple-mobile-web-app-*` 元信息，`display: standalone`。不注册 service worker（面板是单页应用，没有离线需求）。图标沿用仓库里 32×32 的 `icon.png`；要更精致的桌面图标需要另配 192/512 尺寸的图。

**顺带修掉的既有缺陷（深链接白屏）**：`vite.config.ts` 原来用相对 base `"./"`，于是直接打开或刷新形如 `/instances/xxx` 的地址时，`./assets/…` 会被解析成 `/instances/assets/…`，落到 SPA 兜底返回 `text/html`，模块脚本被浏览器拒绝执行 → 整页空白（手机上加书签、刷新、从主屏幕图标进某个实例都会踩到）。现在 base 改成 `"/"`：路由、`/api`、`/ws`、`/i/*` 本来就是绝对根路径，面板也只挂在根下（`HdslServer` 的 contextPath 就是 `/`）。`manifest.json` 与图标的引用也一并改成绝对路径。

