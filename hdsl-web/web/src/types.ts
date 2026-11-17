// REST 契约类型（与后端冻结契约一一对应）

export interface Health {
  status: string;
  version: string;
  /** 面板自身运行的 JDK，只给大版本号（`/api/health` 免认证，不该送完整指纹）。 */
  java?: string;
  uptimeSec: number;
}

export interface VersionInfo {
  version: string;
  time: string | number;
  channel: string;
  installable: boolean;
}

export interface Instance {
  id: string;
  name: string;
  version: string;
  homeMode: string;
  port: number | null;
  portMode: string;
  state: string;
  url?: string;
  uptimeSec?: number;
  icon?: string;
  /** 实例绑定的账户名；null/缺省 = 不使用 */
  account?: string | null;
  /** 详情接口附带：最近一次安装的进度/结果（失败原因在这里） */
  installProgress?: InstallProgress;
  /** state = FAILED 时的原因（启动被拒 / 进程崩溃的尾巴） */
  error?: string | null;
  /** 该实例的 Node.js 选择：null = 跟随默认；auto / 大版本 / 精确版本 */
  nodeRuntime?: string | null;
  /** 服务端对该选择的解析结果（GET/PATCH 实例时给出） */
  nodeResolved?: NodeResolution | null;
}

/** 一次 Node.js 选择的解析结果。 */
export interface NodeResolution {
  version: string | null;
  source: "system" | "managed" | "channel" | "exact" | "none";
  compatible: boolean;
  reason: string;
}

/** 安装进度（GET /api/instances/{id} 的 installProgress 字段）。 */
export interface InstallProgress {
  /** running | pending | done | failed | cancelled | none */
  state: string;
  message: string;
  fraction: number;
}

export interface LogLine {
  level: string;
  time: string;
  text: string;
}

export interface Task {
  id: string;
  kind: string;
  state: string;
  message?: string;
  fraction?: number;
  error?: string;
  /** 任务归属的实例；null/缺省 = 全局任务（导出、插件目录刷新等） */
  instance?: string | null;
  /** state 为 waiting_approval 时携带的审批信息（build-scripts） */
  approval?: TaskApproval;
  /** 任务完成时携带的结果（安装整合包 → instanceId；导出 → filename） */
  result?: TaskResult;
}

/** 任务终态结果体（字段按任务类型可选） */
export interface TaskResult {
  instanceId?: string;
  filename?: string;
  /** 世界导入成功后，导入前那份世界的备份名 */
  safety?: string;
}

/** Task 附带本地更新时间，用于"最近活跃任务"启发式匹配 */
export interface TaskRec extends Task {
  seenAt: number;
}

export interface Toast {
  id: number;
  kind: "success" | "error" | "info";
  text: string;
}

export interface CrashInfo {
  exitCode?: number;
  crashTail?: string;
}

// ---------- Phase 3：插件 ----------

export interface PluginCatalogItem {
  name: string;
  owner: string;
  url: string;
  category: string;
  description: string;
  descriptionZh?: string;
  npm: string;
  version: string;
  stars: number;
  downloads: number;
  tarball: string;
  added: string;
}

export interface InstalledPlugin {
  name: string;
  version: string;
  bundled?: boolean;
  pending?: boolean;
}

/** 面板精选的第一方插件（GET /api/dsh/tui-plugins 的 plugins[] 一条，见 DshTuiPlugins）。
 *  `kind` 区分终端与能力：终端是 app 插件、各有各的 profile；能力插件与实例的 web 应用
 *  同 profile，只给这个实例加东西，不接管屏幕。旧面板没有这个字段，按终端处理。 */
export interface TuiPluginItem {
  id: string;
  packageName: string;
  /** terminal | capability；缺字段视为终端（旧面板） */
  kind?: "terminal" | "capability";
  /** 面板锁定的安装规格 `包名@版本` —— 版本锁定就发生在这里 */
  spec: string;
  name: string;
  version: string;
  license: string;
  repository: string | null;
  description: string;
  /** 包自带的可执行入口名；纯 bundle 为 null（由 dsh 启动它的 profile） */
  bin: string | null;
  /** 必须装进、也从这里启动的 profile —— 绝不是实例的 web（两个 app 插件同 profile 会让实例起不来） */
  profile: string;
  /** 这套插件要求的 dsh 主线（缺省 = 任何已发布线都能起）；工具面靠 0.2.0 起的 agent-preset 层 */
  requiresDsh?: string | null;
  /** 面板自带：安装包在 jar 里，走本地 tarball，不需要 npm registry */
  bundled?: boolean;
}

/** 某个实例上候选 TUI 插件的安装状态（GET /api/instances/{id}/tui-plugins 的一条） */
export interface TuiPluginState {
  /** terminal | capability；缺字段视为终端 */
  kind?: "terminal" | "capability";
  packageName: string;
  /** 该插件的 profile；状态是从这个 profile 的清单里读出来的，不是实例的 web */
  profile: string;
  installed: boolean;
  /** 该 profile 记录的版本（安装时是面板锁定的那个版本） */
  installedVersion?: string;
  /**
   * 清单声明了、文件也真的在磁盘上。`installed` 只读 profile 的 package.json，而 pnpm
   * 先写清单再拉包，所以「声明了但没落地」是可能的状态：这种 profile 起不来，页面必须
   * 能说出来，并且给的是「重新安装」而不是只给「卸载」。旧面板没有这个字段 → undefined
   */
  filesPresent?: boolean;
  /**
   * 装着的不是这一版面板自带的那份：第一方插件比 tarball 摘要（同版本号也可能内容不同），
   * 社区插件比清单锁定的版本。true 时页面给「更新」按钮——重装一次即可。
   */
  outdated?: boolean;
  /** 实例的 dsh 主线能不能跑这套插件；false = 装上也只会得到没有工具面的会话 */
  fitsHost?: boolean;
  /** 这套插件要求的 dsh 主线（缺省 = 任何已发布线） */
  requiresDsh?: string;
  /** 点「终端」时实际会启动的那一个（可以装多个，只有一个生效） */
  active?: boolean;
}

export interface TaskApproval {
  kind: string;
  keys: string[];
}

// ---------- Phase 3：账户 ----------

/** 模型目录里的供应商（GET /api/models/providers 的 providers[] 一条） */
export interface ModelProvider {
  id: string;
  name: string;
  /** dsh 认识的供应商才有固定端点；null = 由账户/区域决定，需用户自填 */
  endpoint: string | null;
  /** 密钥环境变量名，仅用于界面提示 */
  envVar: string;
  /** null = dsh 的三种协议都路由不了这家，卡片禁用 */
  protocol: string | null;
  protocolSource: "dsh" | "directory" | null;
  modelCount: number;
  /** false = 只在 models.dev 目录里，dsh 自家目录没有它 */
  known: boolean;
  preferred: boolean;
  /** 用户以前自己加过的厂商 */
  custom: boolean;
  doc: string | null;
  catalogId: string | null;
  npm: string | null;
}

/** 模型目录里的单个模型（GET /api/models/providers/{id} 的 models[] 一条） */
export interface ModelInfo {
  id: string;
  name: string;
  context: number | null;
  output: number | null;
  reasoning: boolean;
  reasoningEfforts: string[];
  toolCall: boolean;
  attachment: boolean;
  costInput: number | null;
  costOutput: number | null;
  status: string | null;
}

export interface Account {
  name: string;
  vendor: string;
  kind: string | null;
  label: string | null;
  model: string | null;
  endpoint: string | null;
  maskedKey: string;
  skinSet?: boolean;
}

export interface CreateAccountBody {
  vendor?: string;
  endpoint?: string;
  apiKey: string;
  label?: string;
  model?: string;
  kind?: string;
  /** 只在供应商是「目录里新发现的」时才生效（dsh 自家目录里的厂商以 dsh 的协议为准） */
  protocol?: string;
}

export interface PatchAccountBody {
  label?: string;
  model?: string;
  apiKey?: string;
}

// ---------- Phase 3：TLS ----------

export interface TlsSettings {
  https: boolean;
  /** "none" | "self-signed" | "uploaded" | "pkcs12" */
  source: string;
  subject?: string;
  issuer?: string;
  sans?: string[];
  expires?: string | number;
  fingerprintSha256?: string;
}

export interface TlsUploadResult {
  ok: boolean;
  restartRequired?: boolean;
  https?: boolean;
  error?: string;
}

// ---------- Phase 3：下载源（npm registry） ----------

/** 服务端下发的下载源预设；界面不硬编码这张表，直接渲染 */
export interface RegistryPreset {
  id: string;
  label: string;
  url: string;
  note?: string;
}

/** GET /api/settings/registry（POST 成功时也回同样的形状，用它回读省一次请求） */
export interface RegistrySettings {
  /** environment | npmjs | npmmirror | ustc | tencent | huawei | custom */
  preset: string;
  /** 手填的地址（规范化后）；只在 preset = custom 时生效，服务端切到预设后仍保留上次填的值 */
  registry: string;
  /** 最终会用的那个源：面板设置 → 环境变量 → 官方默认 */
  effective: string;
  /** effective 的来源："setting" | "environment" | "default" */
  source: string;
  presets: RegistryPreset[];
}

/** POST /api/settings/registry/test：由面板所在的那台机器实测一次源能不能用、多快 */
export interface RegistryTestResult {
  /** 被测的地址（服务端规范化后的） */
  registry: string;
  ok: boolean;
  /** HTTP 状态码；连不上时为 null */
  status: number | null;
  millis: number;
  /** ok = false 时的原因 */
  error?: string;
}

// ---------- Phase 4：整合包 / 导出 ----------

export interface MarketPack {
  id: string;
  name: string;
  author: string;
  description: string;
  descriptionZh?: string;
  version?: string;
  downloads?: number;
  avatarUrl?: string;
  updatedAt?: string | number;
}

/** 市场详情：契约只冻结 {pack:{...}}，版本列表/readme 字段按存在与否兜底渲染 */
export interface MarketPackDetail extends MarketPack {
  versions?: unknown[];
  readme?: string;
  readmeUrl?: string;
}

export interface ExportRecord {
  filename: string;
  sizeBytes: number;
  createdAt: string | number;
}

/** 可随包的 home 内容：安全过滤之后的一条候选（凭据、密钥、压缩包已被服务端排除） */
export interface ExportHomeEntry {
  /** home 相对路径，一律 `/` 分隔 */
  path: string;
  sizeBytes: number;
}

/** GET /api/instances/{id}/export：导出表单的可选项。从未启动过的实例 `home` 为空数组。 */
export interface ExportChoices {
  home: ExportHomeEntry[];
  /** 本机已装的 dsh 版本，新的在前；写进 manifest 时实例自身版本排在首位 */
  dshVersions: string[];
}

// ---------- Phase 4：会话 / 工作区 / 技能 ----------

export interface SessionInfo {
  id: string;
  title?: string;
  messageCount?: number;
  updatedAt?: string | number;
  sizeBytes?: number;
}

export interface WorkspaceInfo {
  id: string;
  name?: string;
  path?: string;
  updatedAt?: string | number;
}

export interface SkillInfo {
  id: string;
  name: string;
  description?: string;
  enabled?: boolean;
}

// ---------- Phase 4：体检 ----------

export interface DoctorCheck {
  id: string;
  name: string;
  status: "ok" | "warn" | "fail" | string;
  detail?: string;
}

export interface DoctorReport {
  checks: DoctorCheck[];
  generatedAt: string | number;
}

/// GET /api/developer/status：开发者模式的当前状态。
/** agent 密码：与开发者密码分开保存的「较小的那把钥匙」。 */
export interface AgentPasswordStatus {
  /** 是否已设置 */
  set: boolean;
  /** 当前守卫档位下它是否有意义（off / proxy 下没有） */
  applicable: boolean;
  /** 设置时勾选的、这串密码最多能发出去的范围 */
  scopes: string[];
  /** 票是否只认本机回环（默认是） */
  loopbackOnly: boolean;
  /** 非回环使用票是否必须有登录会话（loopbackOnly 关闭时才生效） */
  remoteNeedsSession: boolean;
}

/** 一张借给 agent 的票；token 本身永远不出现在列表里。 */
export interface AgentGrantRow {
  id: string;
  label: string;
  scopes: string[];
  instanceId?: string;
  expiresAt: string;
}

/** `POST /api/developer/agent-token` 的应答：token 只在这里出现一次。 */
export interface AgentToken {
  token: string;
  id: string;
  label: string;
  scopes: string[];
  instanceId?: string;
  /** 面板自己报的回环地址（真实端口、协议、挂载前缀），给提示词用 */
  panel?: string;
  expiresAt: string;
}

export interface DeveloperStatus {
  /** 是否已设置开发者密码 */
  devPasswordSet: boolean;
  /** 当前会话是否已通过开发者密码提权（30 分钟滑动续期） */
  elevated: boolean;
  /** 提权到期时间（ISO 字符串）；未提权时不出现 */
  expiresAt?: string;
  /** 实例终端（Web Shell）开关；false 时打开终端会被后端拒绝 */
  webShellEnabled: boolean;
  /** 面板此刻是否真的在讲 TLS；false 时开发者模式与 Web Shell 都不可用 */
  https: boolean;
  /** 开发者守卫的作用域；旧后端没有这个字段，按 off 处理 */
  guardScope?: DeveloperGuardScope;
  /** 紧急停机状态；旧后端没有这个字段，按未停机处理 */
  estop?: EmergencyStopState;
}

/// 一个品牌的“最后手段”策略；默认全关。
export interface BrandPolicyEntry {
  brand: BrandId;
  /// 这个品牌支持的开关（目前只有 octop 有 lockedRoute / sourceBuild）；
  /// 页面按它渲染，未列出的开关不显示，也不允许打开。
  capabilities?: string[];
  singleInstance: boolean;
  survivor: string | null;
  lockedRoute: boolean;
  sourceBuild: boolean;
  /** 品牌默认 Node.js 选择：null = 跟随全局默认；auto / 大版本 / 精确版本 */
  nodeRuntime: string | null;
  instances: {
    id: string;
    name: string;
    version: string;
    state: string;
    disabled: boolean;
  }[];
}

/// 一个可选的下载源（PyPI 索引 / CPython 镜像 / 某品牌的 release 基址）。
export interface BrandSourcePreset {
  id: string;
  label: string;
  url: string;
  note: string;
}

/// 某一类源当前的设置：选中的 preset、手填地址、实际生效地址与来源。
export interface BrandSourceState {
  preset: string;
  /// 手填地址；即使当前选了别的 preset 也会保留，切回“自定义”时还能看到
  url: string;
  effective: string;
  /// `setting` / `environment` / `default`
  source: string;
  presets: BrandSourcePreset[];
}

/// GET /api/settings/brands
export interface BrandSettings {
  sources: {
    pypi: BrandSourceState;
    python: BrandSourceState;
    /// 版本源码归档（codeload 形态：`<base>/<slug>/zip/refs/tags/<tag>`）
    archive: BrandSourceState;
    /// 按品牌 id 索引；目前只有走 GitHub Releases 的品牌（octop）
    release: Record<string, BrandSourceState>;
  };
  policy: BrandPolicyStatus;
}

/// POST /api/settings/brands/test 的结果
export interface BrandSourceProbe {
  kind: string;
  url: string;
  ok: boolean;
  status: number | null;
  millis: number;
  error?: string;
}

/// 一次提交里的一行：选了哪个 preset，以及选择“自定义”时要写入的地址。
export interface BrandSourceChoice {
  preset: string;
  url?: string;
}

/// POST /api/settings/brands/sources 的请求体：全有或全无，任何一项非法就整体不改。
export interface BrandSourcesBody {
  pypi: BrandSourceChoice;
  python: BrandSourceChoice;
  archive: BrandSourceChoice;
  /// 按品牌 id 索引；只提交页面上出现的品牌
  release?: Record<string, BrandSourceChoice>;
}

/// GET /api/settings/brands 的 `policy` 段（读不要求提权）
export interface BrandPolicyStatus {
  brands: BrandPolicyEntry[];
}

/// 开发者守卫能管多远：off / 只反代 / 反代 + 实例操作 / 全部 API。
export type DeveloperGuardScope = "off" | "proxy" | "instance" | "all";

/// 紧急停机（E-Stop）的当前状态。
export interface EmergencyStopState {
  engaged: boolean;
  engagedAt?: string;
  releasesAt?: string;
  /** 冷却剩余秒数；未停机时为 0 */
  remainingSeconds: number;
  reason?: string;
}

/// GET /api/developer/estop/preview：按下之前先看会杀掉什么。
export interface EmergencyStopPreview {
  engagable: boolean;
  estop: EmergencyStopState;
  instances: { id: string; brand: string; state: string; pids: number }[];
  terminals: { brand: string; instance: string; running: boolean }[];
  processCount: number;
  terminalCount: number;
}

// ---------- Phase 4：ACP 控制台 ----------

/** acp-update 事件的 update 体：kind 之外字段按 ACP 会话更新类型各异，宽松读取 */
export interface AcpSessionUpdate {
  kind?: string;
  [key: string]: unknown;
}

// ---------- WebSocket 事件（服务端 → 客户端） ----------

export type WsEvent =
  | { type: "instance-state"; instance: string; state: string; url?: string; exitCode?: number; crashTail?: string }
  | { type: "log"; instance: string; level: string; line: string }
  | { type: "task"; taskId: string; instance?: string | null; state: string; message?: string; fraction?: number; error?: string; approval?: TaskApproval; result?: TaskResult }
  | { type: "instance-created" | "instance-deleted" | "instance-updated"; instance: Instance }
  | { type: "accounts-changed" }
  | { type: "acp-ready"; instance: string; session: string; protocolVersion?: number }
  | { type: "acp-error"; instance: string; error: string }
  | { type: "acp-update"; instance: string; session: string; update: AcpSessionUpdate }
  | { type: "acp-finished"; instance: string; session: string; stopReason?: string }
  // 面板聊天（docs/chat.md §11）：只推事件，命令走 REST。
  | { type: "chat-turn-started"; sessionId: string; turnId: string; messageId: string; nodeId: string; attemptNo: number }
  | { type: "chat-delta"; sessionId: string; turnId: string; seq: number; text: string }
  | { type: "chat-reasoning"; sessionId: string; turnId: string; seq: number; text: string }
  | { type: "chat-block"; sessionId: string; turnId: string; phase: "start" | "end"; blockType: string; seq: number }
  | { type: "chat-usage"; sessionId: string; turnId: string; input: number; output: number }
  | { type: "chat-tool"; sessionId: string; turnId: string; tool?: "search" | "scrape" | "read_link" | "deepseek_web_search" | string; query: string; status: "running" | "done" | "failed" | string; provider?: string; hits?: number; sources?: ChatSearchResult }
  | { type: "chat-sources"; sessionId: string; turnId: string; sources: ChatSearchHit[] }
  | { type: "chat-title"; sessionId: string; turnId: string; title: string }
  | { type: "chat-turn-finished"; sessionId: string; turnId: string; messageId: string; status: string; finishReason: string; input?: number; output?: number }
  | { type: "chat-error"; sessionId: string; turnId: string; code: string; message: string; retryable: boolean }
  | { type: "chat-sessions-changed" | "chat-assistants-changed" | "chat-settings-changed" | "chat-backups-changed"; sessionId?: string };

// ---------- 第三方品牌实例（kimi / opencode / claude-web，REST 挂 /api/brands/<brand>/*） ----------

export type BrandId =
  | "kimi"
  | "opencode"
  | "claude-web"
  | "octop"
  | "hermes"
  | "pi-web"
  | "qwen"
  | "mimo"
  | "minimax";

/// 品牌实例的形态：网页进程 / 纯 TUI / 两者可选（PI WEB）。
export type BrandKind = "web" | "tui" | "hybrid";

/// 终端内核（只有 Claude Web 有不止一个：社区引擎 claude-web-engine 或官方
/// Claude Code 二进制）。`available` 为假时 `detail` 说明缺什么。
export interface TerminalKernel {
  id: string;
  label: string;
  available: boolean;
  detail?: string | null;
}

/// GitHub/npm 版本目录的缓存与配额状态（`GET /api/brands/<brand>/versions` 的 `catalog`）。
export interface BrandCatalogStatus {
  /** 这份列表最后一次来自哪里：内存 / 磁盘 / 刚拉的网络 */
  source?: "memory" | "disk" | "network" | "none";
  /** 数据本身的抓取时间（304 不更新它） */
  fetchedAt?: number;
  /** 最后一次向远端确认的时间（304 会更新它） */
  checkedAt?: number;
  /** 这个品牌的自动刷新周期（GitHub 1 小时，npm 30 分钟） */
  ttlMillis?: number;
  /** GitHub 配额；-1 = 未知（npm 源或还没拿到头） */
  limit?: number;
  remaining?: number;
  resetAt?: number;
  /** 手动刷新冷却结束时间（本地防连点） */
  refreshCooldownUntil?: number;
  rateLimitedUntil?: number;
  backoffUntil?: number;
  failures?: number;
  tokenConfigured?: boolean;
  tokenLogin?: string | null;
  /** 列表为空时的原因：never_fetched / rate_limited / empty */
  reason?: string;
}

/// GitHub 访问令牌的状态（不回传令牌本身）。
export interface GitHubTokenStatus {
  configured: boolean;
  login: string | null;
}

/// GET /api/brands/<brand>/versions：全部干净 semver（最新在前）+ 已装列表 + 新建实例默认版本
export interface BrandVersions {
  versions: string[];
  latest: string;
  releases: {
    version: string;
    current: boolean;
    /** 仅 pi-web：这个 release 配对的 Pi SDK 版本 */
    pairedSdk?: string;
    /** 仅 pi-web：这个 release 能不能起网页（false = 只装了终端的降级 release） */
    webAvailable?: boolean;
    /** 仅 pi-web：安装时自检的浏览器终端能力（node-pty） */
    ptyAvailable?: boolean;
    /** 这个 release 声明的 engines.node（目前只有 Hermes 0.22.6+ 是 >=26） */
    nodeRequirement?: string;
    /** 该声明与当前品牌 Node 是否兼容 */
    nodeCompatible?: boolean;
  }[];
  /** 仅 pi-web：当前推荐版本配对的 Pi SDK */
  pairedSdk?: string;
  /** 各版本声明的 engines.node（版本 → 范围），用于在选项与卡片上标「需 Node 26」 */
  nodeRequirements?: Record<string, string>;
  /** 各版本的要求是否被当前品牌 Node 满足 */
  nodeCompatibility?: Record<string, boolean>;
  /** 最新版本的要求是否满足；无要求时缺省 */
  latestNodeCompatible?: boolean;
  /** 这台机器上该品牌当前解析到的 Node */
  node?: { version?: string | null; source?: string };
  /** 版本目录的缓存/配额状态 */
  catalog?: BrandCatalogStatus;
  /** 新建实例的推荐默认版本；一个版本都没装时为 null */
  default: string | null;
  /// 这个平台上能不能装（目前只有 Hermes 在 Windows 上为 false）。
  /// 为 false 时 `versions` 是空的、`reason` 说清为什么——面板不隐藏这一栏，
  /// 而是把安装入口置灰并把原因摆出来
  supported?: boolean;
  reason?: string;
}

/// 页签行用的平台能力，不触发任何网络请求。
export interface BrandCapabilities {
  platform: string;
  brands: {
    id: BrandId;
    /** web / tui / hybrid：决定实例卡提供启动网页还是只提供终端 */
    kind?: BrandKind;
    web?: boolean;
    terminal?: boolean;
    installable: boolean;
    reason?: string;
  }[];
}

/// 品牌实例自己的管理员凭据（目前只有 Octop 有）。
export interface BrandAdminCredentials {
  username: string;
  /// Octop 只存哈希：面板没记录过的密码（网页向导里设的）读不出来，此时为 null
  password: string | null;
  /// 密码是否可读；false 时页面只提供重置
  readable?: boolean;
  /// `admin`：正常的管理员口令；`wizard`：实例还没初始化，这是 Octop 初始化向导的口令
  mode?: "admin" | "wizard";
  reset?: boolean;
}

/// GET /api/brands/<brand>/install：安装任务状态
export interface BrandInstallStatus {
  /// `queued`：前面的安装占满了槽位，这个还没开始（旧后端没有这个状态，
  /// 那时排队中的任务报的是 `running` 加一条空 message，看着像卡死）
  state: "queued" | "running" | "done" | "failed" | "none";
  message: string;
  fraction: number;
  /// 正在进行的安装任务 id，用来「停止」；旧后端没有这个字段
  taskId?: string;
  /// 当前子步骤的名字（下载 / npm ci / uv sync …）与它自己的进度；旧后端没有这两个字段
  stage?: string;
  stageFraction?: number;
  /// 机器可读的阶段：`download` / `write` / `build` / `install` / `done`；旧后端没有这个字段
  stageKey?: string;
  error?: string;
}

/// GET /api/brands/<brand>/instances 的一项
export interface BrandInstance {
  id: string;
  brand: BrandId;
  name: string;
  version: string;
  /** web / tui / hybrid；TUI 实例没有网页进程与端口 */
  kind?: BrandKind;
  /** 仅 claude-web：ANTHROPIC_BASE_URL 覆盖；空/缺省表示用工具自己的默认端点 */
  baseUrl?: string | null;
  lastPort: number;
  createdAt: string | number;
  state: "created" | "starting" | "running" | "stopped" | "error";
  error?: string;
  /** 被单实例策略禁用：行变灰，启动/打开/删除应被拒绝 */
  disabled?: boolean;
  /** state = running 时的反代地址（/i/<id>/） */
  url?: string;
  /** 交互终端是否正开着（Hermes：网页与终端互斥，UI 先问再切换） */
  terminalOpen?: boolean;
  /** 仅 pi-web：这个实例起不起网页（false = 仅终端形态） */
  webEnabled?: boolean;
  /** 仅 pi-web：实例被锁在当前 release，品牌升级不跟随 */
  versionLocked?: boolean;
  /** 仅 pi-web：当前 release 能不能起网页 */
  releaseWebAvailable?: boolean;
  /** 仅 pi-web：当前 release 的浏览器终端（node-pty）是否可用 */
  releasePtyAvailable?: boolean;
  /** 仅 pi-web：当前 release 配对的 Pi SDK */
  pairedSdk?: string;
}

/// 品牌/zcode 实例的**插件通道**（`GET …/instances/<id>/plugins`）。
/// 现在没有任何品牌支持插件，后端一律回答 `supported: false` + 空列表；
/// 这个形状就是给未来留的位置：后端把 supported 置真并填 `plugins` 后，
/// 品牌实例面板的插件弹窗会直接列出可安装/启停的插件，前端不用改。
export interface InstancePlugins {
  supported: boolean;
  plugins: { name: string; version?: string; enabled?: boolean }[];
}

/// 启动面板"跨品牌实例切换器"里的非 dsh 条目（kimi/opencode/claude-web/octop/hermes 品牌实例 + zcode 实验实例）。
/// 这是**唯一**的跨品牌实例状态源：BrandsView（品牌层）/ ZcodeSection / LaunchPane /
/// MainPage 都从这里读，写入只发生在 refreshExternal。
export interface ExternalInstance {
  brand:
    | "kimi"
    | "opencode"
    | "claude-web"
    | "octop"
    | "hermes"
    | "pi-web"
    | "qwen"
    | "mimo"
    | "minimax"
    | "zcode";
  id: string;
  name: string;
  state: string;
  /** web / tui / hybrid；缺省按 web 处理，兼容旧后端 */
  kind?: BrandKind;
  url?: string;
  /** 最近一次启动播报的端口；0/缺省 = 未知（运行中浮层展示用） */
  port?: number;
  /** 品牌实例的版本号；zcode 现在也 pin 一个上游 ref */
  version?: string;
  /** 仅 zcode：降级下限与已使用的凭据格式 */
  minVersion?: string | null;
  minVersionAt?: number;
  credentialFormat?: number;
  requiresCredentialSecret?: boolean;
  keyFingerprint?: string | null;
  /** 运行出错时的错误信息（state = error） */
  error?: string;
  /** 被单实例策略禁用 */
  disabled?: boolean;
  /** 交互终端是否正开着（Hermes：网页与终端互斥，UI 先问再切换） */
  terminalOpen?: boolean;
  /** 仅 zcode：实例是否已配置 API Key */
  hasApiKey?: boolean;
  /** 仅 zcode：已保存的 API Base URL（编辑表单回填用） */
  baseUrl?: string;
  /** 仅 zcode：这个实例工作的目录（创建时可选，之后只读） */
  workspacePath?: string;
  /** 仅 pi-web：仅终端形态 */
  webEnabled?: boolean;
  /** 仅 pi-web：锁在当前 release */
  versionLocked?: boolean;
  /** 仅 pi-web：当前 release 不能起网页（降级） */
  releaseWebAvailable?: boolean;
  /** 仅 pi-web：当前 release 的浏览器终端不可用 */
  releasePtyAvailable?: boolean;
  /** 仅 pi-web：当前 release 配对的 Pi SDK */
  pairedSdk?: string;
}

// ---------- 全局应用状态 ----------

export interface AppState {
  username: string | null;
  authChecked: boolean;
  authError: string | null;
  /** 移动版式下主导航抽屉是否展开；桌面版式下无意义 */
  sidebarOpen: boolean;
  /** 服务端是否存在任何用户；false = 已有用户（或尚未询问），true = 需先走初始化引导页 */
  setupRequired: boolean;
  instances: Instance[];
  instancesLoaded: boolean;
  instancesLoading: boolean;
  versions: VersionInfo[] | null;
  versionsLoading: boolean;
  versionsError: string | null;
  tasks: Record<string, TaskRec>;
  logs: Record<string, LogLine[]>;
  crash: Record<string, CrashInfo>;
  /** 运行中实例最近一次拿到的 uptime 采样，用于本地平滑计时 */
  lastUptime: Record<string, { sec: number; at: number }>;
  /** null = 尚未加载或后端不支持（见 accountsError） */
  accounts: Account[] | null;
  accountsLoading: boolean;
  accountsError: string | null;
  /** 合并后的供应商名单（dsh 自家目录 + models.dev）；null = 尚未加载或不可用（见 modelProvidersError） */
  modelProviders: ModelProvider[] | null;
  modelProvidersLoading: boolean;
  modelProvidersError: string | null;
  /** 目录数据的抓取时间（毫秒），用于"x 分钟前更新" */
  modelProvidersAt: number | null;
  /** 这份名单从哪来：`remote` / `cache` / `builtin`（模型目录不可达时的内置供应商） */
  modelProvidersSource: string | null;
  /** 实验性 ZCode 品类：发行包检测结果（null = 尚未加载） */
  zcodeDist: ZcodeDistInfo | null;
  zcodeLoading: boolean;
  /** 实验性 ZCode 品类：新建实例默认工作区的父目录（`<数据目录>/zcode/workspaces`）；undefined = 未取到 */
  zcodeWorkspacesRoot?: string;
  /** 跨品牌实例（kimi/opencode/zcode）——唯一状态源，见 ExternalInstance 的注释 */
  external: ExternalInstance[];
  toasts: Toast[];
}

// ---------- REST 请求体 ----------

export interface CreateInstanceBody {
  name: string;
  version: string;
  homeMode: string;
  portMode: string;
  port?: number;
}

export interface PatchInstanceBody {
  name?: string;
  homeMode?: string;
  portMode?: string;
  port?: number;
  autoPort?: boolean;
  /** 绑定账户名；null 表示解绑（不使用） */
  account?: string | null;
  /** Node.js 选择：null/省略 = 跟随默认；auto / 大版本 / 精确版本 */
  nodeRuntime?: string | null;
}

// ---------- ZCode（实验性） ----------

/// ZCode 实例（GET /api/zcode/instances 的一项）。实验性功能，
/// 定位与限制见 docs/zcode-experimental.md。
export interface ZcodeInstance {
  id: string;
  name: string;
  workspacePath: string;
  baseUrl: string | null;
  /** 是否已保存 API Key；明文存在实例清单里，接口不返回原文 */
  hasApiKey: boolean;
  /** 访问令牌，打开实例 URL 时作为查询串带上 */
  token: string;
  /** 最近一次启动时 ZCode 自选的端口；0 = 未知/未启动 */
  lastPort: number;
  /** 实例 pin 的上游 ref；null = 历史兼容，跟随 current */
  version: string | null;
  /** 降级下限；null = 还没成功启动过 */
  minVersion: string | null;
  minVersionAt: number;
  /** 已经使用过的凭据格式：1 legacy / 2 system credential storage / 0 unknown */
  credentialFormat: number;
  /** 当前 pin 的版本是否要求 credential-secret */
  requiresCredentialSecret: boolean;
  /** credential-secret 的 fingerprint，可在 UI 里显示；不返回密钥本身 */
  keyFingerprint: string | null;
  createdAt: number;
  /** created | starting | running | stopped | error */
  state: string;
  /** state = error 时的原因（进程日志尾部） */
  error?: string;
}

/// ZCode 发行包检测结果（GET /api/zcode/dist）。
export interface ZcodeDistInfo {
  present: boolean;
  path: string;
  version: string | null;
}

/// 已安装的 ZCode 发行包（GET /api/zcode/releases 的一项）。
export interface ZcodeRelease {
  version: string;
  path: string;
  builtAt: number;
  /** 是否 `current` 指向它（启动用的就是它） */
  current: boolean;
}

/// 上游版本目录的一项（GET /api/zcode/catalog）。
export interface ZcodeCatalogEntry {
  ref: string;
  version: string;
  /** release | tag | main */
  channel: string;
  commit: string | null;
  publishedAt: number | null;
  /** 1 = legacy credentials, 2 = system credential storage */
  credentialFormat: number;
  requiresCredentialSecret: boolean;
  installed: boolean;
  /** installed | not-installed | building | failed */
  buildState: string;
  prerelease: boolean;
  notes: string;
}

export interface ZcodeCatalog {
  entries: ZcodeCatalogEntry[];
  fetchedAt: number;
}

/// 面板内构建的状态（GET/POST /api/zcode/build）。
export interface ZcodeBuildStatus {
  /** idle | running | done | error */
  state: string;
  version: string;
  message: string;
  fraction: number;
  error?: string;
}

/// 一张可选壁纸（GET /api/wallpapers 的一项）。
/// `builtin` 为真的那些随面板打包，不能删；上传的那些由后端命名与校验。
export interface Wallpaper {
  id: string;
  name: string;
  /** 相对路径：由 <base href> 解析，两种挂载下都对 */
  url: string;
  builtin: boolean;
  width?: number;
  height?: number;
  bytes?: number;
}

export interface WallpaperList {
  wallpapers: Wallpaper[];
}

/* ---------------- 系统资源（通用 → 系统资源） ---------------- */

/** 这台机器能画什么；后端按平台给能力位，前端只按它渲染。 */
export interface SystemCapabilities {
  perCore: boolean;
  diskIo: boolean;
  network: boolean;
  gpu: boolean;
}

export interface SystemCpu {
  /** 0..1；-1 = 还在采第一个差值 */
  load: number;
  count: number;
  /** cgroup CPU 配额（核），-1 = 无限制 */
  quotaCores: number;
  /** 每核 0..1；不支持时为空数组 */
  cores: number[];
  /** 1/5/15 分钟负载；Windows 上为空 */
  loadAverage: number[];
}

export interface SystemMemory {
  totalBytes: number;
  usedBytes: number;
  availableBytes: number;
  /** 完全空闲的物理内存 */
  freeBytes: number;
  /** 页缓存 + buffers，可回收，Linux 看起来"满"就是因为它 */
  cachedBytes: number;
  swapTotalBytes: number;
  swapUsedBytes: number;
  /** cgroup = 数字是容器限额；host = 宿主/整机 */
  source: "cgroup" | "host";
}

export interface SystemDiskRate {
  name: string;
  readBytesPerSec: number;
  writeBytesPerSec: number;
}

export interface SystemMount {
  path: string;
  totalBytes: number;
  usedBytes: number;
}

export interface SystemNetworkRate {
  name: string;
  rxBytesPerSec: number;
  txBytesPerSec: number;
}

export interface SystemGpu {
  index: number;
  name: string;
  vendor?: string;
  backend?: string;
  utilization: number;
  encoderUtilization?: number;
  decoderUtilization?: number;
  memoryUsedBytes: number;
  memoryTotalBytes: number;
  temperatureC: number;
  powerW?: number;
}

export interface SystemPanelSelf {
  cpuLoad: number;
  rssBytes: number;
  threads: number;
  uptimeSec: number;
}

/** 面板运行在哪：页面文案按它切换（"容器里…"只在容器里说）。 */
export interface SystemHost {
  os: "linux" | "windows" | "macos" | "other";
  environment: "container" | "wsl" | "native";
}

/** 一帧快照；`at` 是 ISO 时间串，历史按它排序/去重。 */
export interface SystemSnapshot {
  at: string;
  /** 这一帧所属的采样档位：1000 = 有人用，30000 = 低频守护 */
  intervalMs: number;
  host: SystemHost;
  capabilities: SystemCapabilities;
  cpu: SystemCpu;
  memory: SystemMemory;
  disks: SystemDiskRate[];
  mounts: SystemMount[];
  networks: SystemNetworkRate[];
  gpus: SystemGpu[];
  panel: SystemPanelSelf;
}

/** GET /api/system/stats：最新一帧 + 历史（首次打开时带 history=240）。 */
export interface SystemStats {
  snapshot: SystemSnapshot | null;
  history: SystemSnapshot[];
}

/** GET /api/system/info：面板自己的坐标。 */
export interface PanelInfo {
  version: string;
  java: string;
  bindHost: string;
  port: number;
  basePath: string;
  dataDir: string;
  publicBaseUrl: string;
  https: boolean;
  security: string;
  startedAt: number;
}

/** GET /api/system/platform：当前平台能做什么、哪些是已知差异。 */
export type PlatformLevel = "supported" | "limited" | "unsupported";

export interface PlatformItem {
  id: string;
  level: PlatformLevel;
  title: string;
  detail: string;
}

export interface PlatformCompatibility {
  os: string;
  version: string;
  arch: string;
  level: PlatformLevel;
  summary: string;
  items: PlatformItem[];
}

/** GET /api/system/storage：数据目录按顶层子目录分桶的大小。 */
export interface StorageBucket {
  name: string;
  label: string;
  bytes: number;
}

export interface StorageMount {
  path: string;
  totalBytes: number;
  usedBytes: number;
}

export interface SystemStorage {
  dataDir: string;
  totalBytes: number;
  buckets: StorageBucket[];
  mount: StorageMount;
  /** 上一次真正遍历的时间（ISO）；缓存命中时不变 */
  computedAt: string;
}

// ---------- 面板聊天（docs/chat.md） ----------

export interface ChatAssistant {
  id: string;
  name: string;
  avatar: string;
  description: string;
  systemPrompt: string;
  openingMessage: string;
  account: string | null;
  model: string | null;
  temperature: number | null;
  maxTokens: number | null;
  reasoningLevel: string | null;
  quickPhrases: string[];
  builtin: boolean;
  /** `deepseek-official` 表示内置预设；复制出来的角色也保留这个值。 */
  preset?: string | null;
  /** 预设固定的协议；普通助手为 null，跟随账户配置。 */
  protocol?: string | null;
  createdAt: number;
  updatedAt: number;
}

export interface ChatSessionSummary {
  id: string;
  assistantId: string;
  title: string;
  model: string | null;
  account: string | null;
  createdAt: number;
  updatedAt: number;
  messageCount: number;
  lastMessage: string | null;
}

export interface ChatAttachment {
  id: string;
  name: string;
  mime: string;
  kind: "image" | "document" | "text" | "audio" | "video" | string;
  size: number;
  extractStatus: "pending" | "done" | "failed" | "skipped" | string;
  extractError?: string | null;
  url: string;
  preview?: string;
}

/** GET /api/chat/attachments/{id}/text 的一页（按字符分页）。 */
export interface ChatAttachmentText {
  text: string;
  offset: number;
  nextOffset: number;
  hasMore: boolean;
  charset: string;
  /** true = document 的抽取文本，不是原文件内容 */
  extracted: boolean;
}

/** DeepSeek 官方协议的原生内容块（按流顺序；思考、服务端工具、正文都在里面）。 */
export interface ChatBlock {
  type: string;
  thinking?: string;
  text?: string;
  signature?: string;
  id?: string;
  name?: string;
  kind?: string;
  input?: {
    action?: string;
    query?: string;
    queries?: string[];
    url?: string;
    [key: string]: unknown;
  };
  [key: string]: unknown;
}

/** 一轮里 thinking / tool_calls / tool 的时间线；普通协议用它分段渲染。 */
export interface ChatPart {
  type: "thinking" | "text" | "tool_calls" | "tool" | string;
  text?: string;
  id?: string;
  content?: string;
  startedAt?: number;
  endedAt?: number;
  durationMs?: number;
  hits?: number;
  sources?: { title?: string; url?: string; site?: string }[];
  calls?: { id: string; name: string; arguments: string }[];
}

export interface ChatMessage {
  id: string;
  nodeId: string;
  variantIndex: number;
  content: string;
  reasoning: string;
  /** DeepSeek 预设才有：保留思考/搜索的原始顺序，前端据此分段渲染。 */
  blocks?: ChatBlock[];
  /** 普通协议的时间线：thinking / tool_calls / tool，按顺序。 */
  parts?: ChatPart[];
  status: "streaming" | "done" | "stopped" | "error" | "interrupted" | string;
  model: string | null;
  createdAt: number;
  editedAt: number | null;
  usage?: { input?: number; output?: number };
  /** 这一轮联网搜索的来源（有搜索时才有） */
  sources?: ChatSearchHit[];
  /** 模型自己调用 web_search 的每一轮（按时间顺序） */
  tools?: ChatToolRound[];
  searchQuery?: string;
  attachments: ChatAttachment[];
}

export interface ChatNode {
  id: string;
  ordinal: number;
  role: "user" | "assistant";
  activeVariantId: string | null;
  createdAt: number;
  variants: ChatMessage[];
}

export interface ChatSession {
  id: string;
  assistantId: string;
  title: string;
  model: string | null;
  account: string | null;
  /** 会话级思考挡位；null = 跟随助手默认 */
  reasoningLevel?: string | null;
  createdAt: number;
  updatedAt: number;
  streaming: boolean;
  nodes: ChatNode[];
}

/** 模型怎么控思考：toggle=开关，effort=英文档位，budget=数字预算，forced=只能开，none=没声明 */
export interface ChatReasoningMode {
  kind: "none" | "toggle" | "effort" | "budget" | "forced";
  canDisable: boolean;
  efforts: string[];
  minBudget: number;
  maxBudget: number;
  source: string;
}

export interface ChatSearchHit {
  /** 短哈希，模型引用时写 [citation,domain](id) */
  id?: string;
  title: string;
  url: string;
  snippet?: string;
  site?: string;
  publishedAt?: string;
}

export interface ChatSearchProvider {
  id: "bocha" | "deepseek" | "zhipu" | "tavily" | "brave" | "exa" | string;
  name: string;
  configured: boolean;
}

export interface ChatSearchStatus {
  provider: "bocha" | "deepseek" | string;
  configured: boolean;
  maxResults: number;
  keyMasked?: string | null;
  deepseekPresetEnabled?: boolean;
  deepseekReady?: boolean;
  deepseekAccount?: string | null;
  /** 每个服务商是否已配置 key；聊天输入区的搜索菜单用它置灰。 */
  providers?: ChatSearchProvider[];
}

export interface ChatSearchResult {
  query: string;
  provider: string;
  hits: ChatSearchHit[];
}

/** 模型自己发起的一次 web_search（每轮对话最多 3 次）。 */
export interface ChatToolRound {
  query: string;
  provider: string;
  hits: ChatSearchHit[];
}

export interface ChatModelCapabilities {
  vision: boolean;
  files: boolean;
  reasoning: boolean;
  tools: boolean;
  maxContext: number;
  maxOutput: number;
  source: string;
  reasoningMode?: ChatReasoningMode;
}

/** 面板用户的聊天资料：名字 + 头像（`img:<id>` 或 null）。 */
export interface ChatProfile {
  name: string;
  avatar: string | null;
}

export interface ChatModelInfo {
  id: string;
  capabilities: ChatModelCapabilities;
}

export interface ChatAccountProfile {
  name: string;
  vendor: string;
  label: string;
  endpoint: string | null;
  model?: string | null;
  maskedKey?: string | null;
  protocol: string;
  base?: string;
  modelsPath?: string;
  chatPath?: string;
  authStyle?: string;
  configured?: boolean;
  resolved?: { modelsUrl: string; chatUrl: string };
  models?: string[];
  modelList?: ChatModelInfo[];
  modelsFetchedAt?: number;
  lastProbe?: ChatProbe & { at?: number };
  extraHeaders?: Record<string, string>;
}

export interface ChatProbe {
  reachable: boolean;
  tlsOk: boolean;
  authOk: boolean;
  modelsOk: boolean;
  models: string[];
  latencyMs: number;
  resolved: { modelsUrl: string; chatUrl: string };
  warnings: string[];
  error: string | null;
}

export interface ChatBackup {
  name: string;
  path: string;
  createdAt: number;
  bytes: number;
  sessions: number;
  messages: number;
  attachments: number;
  complete: boolean;
  error?: string | null;
}

export interface ChatSettings {
  retention: "forever" | "30d" | "90d" | "180d" | "365d" | "custom";
  retentionDays: number;
  backupEnabled: boolean;
  backupTime: string;
  backupKeep: number;
  backupDir: string;
  privateEndpoints: "allow" | "strict";
  privateAllowlist: string[];
  longTextTokens: number;
  dailyTokenLimit: number;
  streamFlushMs: number;
  defaultAccount: string | null;
  defaultModel: string | null;
  retentionOptions: string[];
  backups: ChatBackup[];
  /** models.dev 目录里有多少个模型；0 = 还没拉取，能力判断回落到内置表 */
  catalogModels?: number;
  catalogUpdatedAt?: number;
  searchProvider?: "bocha" | "deepseek" | "zhipu" | "tavily" | "brave" | "exa";
  searchMaxResults?: number;
  searchKeyMasked?: string | null;
  /** 把搜索/抓取的完整链路存进消息，追问时模型能回看 */
  persistToolTrail?: boolean;
  /** 是否在角色列表里启用内置的 DeepSeek 官方预设助手 */
  deepseekPresetEnabled?: boolean;
  /** agent 票 chat-write 的单次输入 token 上限；0 = 不限制 */
  agentChatMaxInputTokens?: number;
}

/** POST /api/chat/accounts/{name}/chat-test 的结果（会消耗少量 token）。 */
export interface ChatChatTest {
  model: string;
  ok: boolean;
  latencyMs: number;
  answer: string;
  /** 本次流里的思考字数：思考模型可能把很小的 max_tokens 全用在 reasoning 上 */
  reasoningChars?: number;
  /** 正文和思考都为空：HTTP 通了，但端点没给出内容 */
  empty?: boolean;
  status?: number;
  error?: string;
}

/** POST /api/chat/accounts/{name}/vision-test 的结果。 */
export interface ChatVisionTest {
  model: string;
  reasoningChars?: number;
  /** supported = 读出图上的数字；unsupported = 明确拒收/说不看图；uncertain = 有回答但对不上 */
  verdict: "supported" | "unsupported" | "uncertain" | "error";
  supported: boolean;
  latencyMs: number;
  answer: string;
  status?: number;
  error?: string;
  expected: string;
  /** 判定确定时已写入能力库（source=probe） */
  saved: boolean;
}

export interface ChatTurnOutcome {
  turnId: string;
  assistantNodeId?: string;
  userMessage?: ChatMessage;
  assistantMessage?: ChatMessage;
}
