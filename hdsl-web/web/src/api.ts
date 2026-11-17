import type {
  Account,
  AgentGrantRow,
  AgentPasswordStatus,
  AgentToken,
  BrandAdminCredentials,
  BrandSettings,
  BrandSourceProbe,
  BrandSourcesBody,
  CreateAccountBody,
  CreateInstanceBody,
  DeveloperGuardScope,
  DeveloperStatus,
  DoctorReport,
  EmergencyStopPreview,
  ExportChoices,
  ExportRecord,
  GitHubTokenStatus,
  Health,
  Instance,
  InstalledPlugin,
  LogLine,
  MarketPack,
  MarketPackDetail,
  ModelInfo,
  ModelProvider,
  PatchAccountBody,
  PatchInstanceBody,
  PluginCatalogItem,
  RegistrySettings,
  RegistryTestResult,
  SessionInfo,
  SkillInfo,
  Task,
  TlsSettings,
  TlsUploadResult,
  TuiPluginItem,
  TuiPluginState,
  VersionInfo,
  Wallpaper,
  WallpaperList,
  WorkspaceInfo,
  ZcodeBuildStatus,
  ZcodeCatalog,
  ZcodeDistInfo,
  ZcodeInstance,
  ZcodeRelease,
  BrandId,
  BrandInstallStatus,
  BrandInstance,
  BrandVersions,
  BrandCapabilities,
  InstancePlugins,
  TerminalKernel,
  SystemStats,
  SystemStorage,
  PanelInfo,
  PlatformCompatibility,
  ChatAssistant,
  ChatSessionSummary,
  ChatSession,
  ChatAttachment,
  ChatAttachmentText,
  ChatSettings,
  ChatBackup,
  ChatAccountProfile,
  ChatModelInfo,
  ChatProbe,
  ChatProfile,
  ChatSearchStatus,
  ChatSearchResult,
  ChatChatTest,
  ChatVisionTest,
  ChatTurnOutcome,
} from "./types";

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

/** 契约中已冻结但后端尚未实现的端点会返回 404/405，UI 据此优雅降级 */
export function isNotImplemented(e: unknown): boolean {
  return e instanceof ApiError && (e.status === 404 || e.status === 405);
}

function jsonInit(method: string, body: unknown): RequestInit {
  return { method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) };
}

/** 任何 API 返回 401（会话过期）时的全局处理，由 store 注册 */
let unauthorizedHandler: (() => void) | null = null;
export function setUnauthorizedHandler(fn: () => void): void {
  unauthorizedHandler = fn;
}

async function request<T>(path: string, init?: RequestInit, opts?: { skipUnauthorizedRedirect?: boolean; timeoutMs?: number }): Promise<T> {
  let res: Response;
  try {
    // 相对路径：由后端注入的 <base href> 解析——根挂载解析到 /api/…，
    // 子路径挂载解析到 <base>/api/…。两种挂载共用一个构建产物。
    res = await fetch(path.replace(/^\//, ""), {
      credentials: "same-origin",
      cache: "no-store",
      signal: AbortSignal.timeout(opts?.timeoutMs ?? 30000),
      ...init,
    });
  } catch (e) {
    if (e instanceof DOMException && e.name === "TimeoutError") {
      throw new ApiError(0, "请求超时，请检查服务器状态");
    }
    throw new ApiError(0, "无法连接服务器");
  }

  let body: unknown = null;
  if (res.status !== 204) {
    try {
      body = await res.json();
    } catch {
      body = null;
    }
  }

  if (!res.ok) {
    if (res.status === 401 && unauthorizedHandler && !opts?.skipUnauthorizedRedirect) {
      // 会话失效：强制回到登录页（登录接口自身的 401 由调用方处理，这里不重复触发）
      unauthorizedHandler();
    }
    const msg =
      body && typeof body === "object" && typeof (body as { error?: unknown }).error === "string"
        ? ((body as { error: string }).error)
        : `请求失败（HTTP ${res.status}）`;
    throw new ApiError(res.status, msg);
  }
  return body as T;
}

export const api = {
  health: () => request<Health>("/api/health"),
  /** 机器读数：最新一帧，history>0 时顺带取历史（首次打开用 240）。 */
  systemStats: (history = 0) =>
    request<SystemStats>(`/api/system/stats${history > 0 ? `?history=${history}` : ""}`),
  /** 面板自己的坐标：监听地址、真实端口、数据目录、版本。 */
  panelInfo: () => request<PanelInfo>("/api/system/info"),

  /** 平台能力与已知差异（FreeBSD 等未纳入发布验收的平台会在设置页展示）。 */
  platformCompatibility: () => request<PlatformCompatibility>("/api/system/platform"),
  /** 数据目录占用；refresh=true 强制重新遍历（后端默认缓存 60 秒）。 */
  systemStorage: (refresh = false) =>
    request<SystemStorage>(`/api/system/storage${refresh ? "?refresh=1" : ""}`),

  login: (username: string, password: string) =>
    request<{ ok: boolean; username: string }>("/api/auth/login", jsonInit("POST", { username, password })),

  logout: () => request<unknown>("/api/auth/logout", { method: "POST" }),

  whoami: () => request<{ username: string }>("/api/auth/whoami"),

  /** 首次启动引导：服务端是否还没有任何用户 */
  authStatus: () => request<{ setupRequired: boolean }>("/api/auth/status"),

  /** 创建第一个用户并自动登录（服务端在无用户时才接受；201 = 已下发会话） */
  setup: (username: string, password: string) =>
    request<{ ok: boolean; username: string }>("/api/auth/setup", jsonInit("POST", { username, password })),

  /** 修改当前用户密码；成功后服务端会吊销本用户的其它会话 */
  changePassword: (currentPassword: string, newPassword: string) =>
    request<{ ok: boolean }>("/api/auth/password", jsonInit("POST", { currentPassword, newPassword })),

  /** 修改当前用户名；本设备的会话保留并切换到新名，其它会话被吊销 */
  changeUsername: (newUsername: string) =>
    request<{ ok: boolean; username: string }>("/api/auth/username", jsonInit("POST", { newUsername })),

  versions: () => request<{ versions: VersionInfo[] }>("/api/versions"),

  instances: () => request<{ instances: Instance[] }>("/api/instances"),

  getInstance: (id: string) => request<Instance>(`/api/instances/${id}`),

  createInstance: (body: CreateInstanceBody) =>
    request<{ instance: Instance; installTaskId?: string }>("/api/instances", jsonInit("POST", body)),

  patchInstance: (id: string, body: PatchInstanceBody) =>
    request<{ instance?: Instance }>(`/api/instances/${id}`, jsonInit("PATCH", body)),

  deleteInstance: (id: string) => request<unknown>(`/api/instances/${id}`, { method: "DELETE" }),

  install: (id: string, version: string) =>
    request<{ taskId: string }>(`/api/instances/${id}/install`, jsonInit("POST", { version })),

  launch: (id: string, publicHost?: string) =>
    request<{ state: string }>(
      `/api/instances/${id}/launch`,
      jsonInit("POST", publicHost ? { publicHost } : {}),
    ),

  stop: (id: string) => request<unknown>(`/api/instances/${id}/stop`, { method: "POST" }),

  // --- 终端（docs/web-shell.md） -------------------------------------------
  openDshTerminal: (id: string, resume?: string) =>
    request<TerminalOpen>(
      `/api/instances/${encodeURIComponent(id)}/terminal${resume ? `?resume=${encodeURIComponent(resume)}` : ""}`,
      jsonInit("POST", {}),
    ),
  closeDshTerminal: (id: string) =>
    request<{ ok: boolean; killed: boolean }>(`/api/instances/${encodeURIComponent(id)}/terminal`, {
      method: "DELETE",
    }),
  revealDsh: (id: string, target: "home" | "workspace") =>
    request<{ opened: string }>(`/api/instances/${encodeURIComponent(id)}/reveal`, jsonInit("POST", { target })),
  openBrandTerminal: (brand: BrandId, id: string, resume?: string, kernel?: string) => {
    const query = new URLSearchParams();
    if (resume) query.set("resume", resume);
    if (kernel) query.set("kernel", kernel);
    const suffix = query.size > 0 ? `?${query.toString()}` : "";
    return request<TerminalOpen>(
      `/api/brands/${brand}/instances/${encodeURIComponent(id)}/terminal${suffix}`,
      jsonInit("POST", {}),
    );
  },
  /// Claude Web 的两个内核（社区引擎 / 官方）；别的品类有且只有一个 TUI，会回 404。
  brandTerminalKernels: (brand: BrandId, id: string) =>
    request<{ kernels: TerminalKernel[]; default: string | null }>(
      `/api/brands/${brand}/instances/${encodeURIComponent(id)}/terminals`,
    ),
  closeBrandTerminal: (brand: BrandId, id: string) =>
    request<{ ok: boolean; killed: boolean }>(
      `/api/brands/${brand}/instances/${encodeURIComponent(id)}/terminal`,
      { method: "DELETE" },
    ),
  revealBrand: (brand: BrandId, id: string, target: "home" | "workspace" | "log") =>
    request<{ opened: string }>(
      `/api/brands/${brand}/instances/${encodeURIComponent(id)}/reveal`,
      jsonInit("POST", { target }),
    ),
  /** agent 密码的状态：是否已设置、当前守卫档位下是否可用、它的上限范围 */
  agentPassword: () => request<AgentPasswordStatus>("/api/developer/agent-password"),
  /** 设置 agent 密码并声明它最多能发什么；需要先提权，会清掉旧密码发出的全部票 */
  setAgentPassword: (password: string, scopes: string[]) =>
    request<AgentPasswordStatus>("/api/developer/agent-password", jsonInit("POST", { password, scopes })),
  /** 只改 agent 密码能授予的范围，不动密码本身；会掐掉旧范围发出的票 */
  setAgentScopes: (scopes: string[]) =>
    request<AgentPasswordStatus>("/api/developer/agent-password/scopes", jsonInit("POST", { scopes })),
  /** 清除 agent 密码，并掐掉它发出的全部票；需要先提权 */
  clearAgentPassword: () =>
    request<AgentPasswordStatus>("/api/developer/agent-password/clear", { method: "POST" }),
  /** 票是否只允许从本机回环使用；默认是，关掉才轮到 agent.remote_needs_session */
  setAgentLoopback: (loopbackOnly: boolean) =>
    request<AgentPasswordStatus>("/api/developer/agent-loopback", jsonInit("POST", { loopbackOnly })),
  /** 当前活着的 agent 票（不含 token 本身） */
  agentGrants: () => request<{ grants: AgentGrantRow[] }>("/api/developer/agent-grants"),
  /** 用当前提权会话直接签一张票交给 agent；token 只在这个应答里出现一次 */
  mintAgentToken: (body: { label?: string; scopes: string[]; instanceId?: string; ttlSeconds?: number }) =>
    request<AgentToken>("/api/developer/agent-token", jsonInit("POST", body)),
  /** 掐掉一张票，立即生效 */
  revokeAgentGrant: (id: string) =>
    request<{ ok: boolean }>(`/api/developer/agent-grants/${encodeURIComponent(id)}`, { method: "DELETE" }),
  developerStatus: () => request<DeveloperStatus>("/api/developer/status"),
  elevateDeveloper: (password: string) =>
    request<{ elevated: boolean; expiresAt: string; token?: string }>(
      "/api/developer/elevate",
      jsonInit("POST", { password }),
    ),
  /** 已提权的前提下把提权延长到 3 小时后；未提权时后端拒绝 */
  extendDeveloper: () =>
    request<{ elevated: boolean; expiresAt: string }>("/api/developer/extend", { method: "POST" }),
  /** 立即停止当前会话的提权（不需要当前处于提权状态，幂等） */
  revokeDeveloper: () => request<{ elevated: boolean }>("/api/developer/revoke", { method: "POST" }),
  /** 开发者模式下翻转 Web Shell 开关；需要先提权 */
  setWebShell: (enabled: boolean) =>
    request<{ ok: boolean; enabled: boolean }>("/api/developer/web-shell", jsonInit("POST", { enabled })),
  /** 设置开发者守卫的作用域；需要先提权，且必须已启用 HTTPS */
  setDeveloperGuard: (scope: DeveloperGuardScope) =>
    request<{ ok: boolean; scope: string }>("/api/developer/guard", jsonInit("POST", { scope })),
  /** 品牌实例页（设置 → 品牌实例）：下载源 + 每个品牌的策略与实例。
   *  读不需要开发者密码；写操作（策略、下载源、测速）需要提权。 */
  brandSettings: () => request<BrandSettings>("/api/settings/brands"),
  setBrandPolicy: (
    brand: BrandId,
    body: {
      singleInstance: boolean;
      survivor: string | null;
      lockedRoute: boolean;
      sourceBuild: boolean;
      nodeRuntime?: string | null;
    },
  ) => request<{ ok: boolean }>(`/api/settings/brands/policy/${brand}`, jsonInit("PUT", body)),
  setBrandSources: (body: BrandSourcesBody) =>
    request<BrandSettings>("/api/settings/brands/sources", jsonInit("POST", body)),
  /** 从面板这台机器测一个候选源；`url` 省略时测当前生效的那个 */
  probeBrandSource: (kind: "pypi" | "python" | "archive" | "release", brand?: BrandId, url?: string) =>
    request<BrandSourceProbe>("/api/settings/brands/test", jsonInit("POST", { kind, brand, url })),
  /** 紧急停机：按下之前先看清单（会杀掉哪些实例与终端） */
  estopPreview: () => request<EmergencyStopPreview>("/api/developer/estop/preview"),
  /** 紧急停机：强杀所有实例进程树并关掉所有终端，1 小时内不能解除 */
  estopEngage: (reason: string) =>
    request<{
      engaged: boolean;
      /** 已处于停机状态时后端不再杀一遍，也不会重算冷却；此时没有 terminals/processes。 */
      alreadyEngaged?: boolean;
      terminals?: number;
      processes?: number;
      releasesAt?: string;
    }>("/api/developer/estop/engage", jsonInit("POST", { confirm: "ENGAGE-EMERGENCY-STOP", reason })),
  /** 紧急停机：冷却到点后才能解除（否则后端 409 带 remainingSeconds） */
  estopRelease: () => request<{ engaged: boolean }>("/api/developer/estop/release", { method: "POST" }),
  setDeveloperPassword: (
    adminPassword: string,
    password: string,
    confirm: string,
    currentDevPassword?: string,
  ) =>
    request<{ ok: boolean; devPasswordSet: boolean }>(
      "/api/developer/password",
      jsonInit("POST", { adminPassword, password, confirm, currentDevPassword }),
    ),
  clearDeveloperPassword: (adminPassword: string) =>
    request<{ ok: boolean; devPasswordSet: boolean }>(
      "/api/developer/password/clear",
      jsonInit("POST", { adminPassword }),
    ),


  logs: (id: string, tail = 200) =>
    request<{ lines: LogLine[] }>(`/api/instances/${id}/logs?tail=${tail}`),

  open: (id: string) => request<{ url: string }>(`/api/instances/${id}/open`),

  tasks: async (): Promise<Task[]> => {
    const raw: unknown = await request("/api/tasks");
    if (Array.isArray(raw)) return raw as Task[];
    if (raw && typeof raw === "object" && Array.isArray((raw as { tasks?: unknown }).tasks)) {
      return (raw as { tasks: Task[] }).tasks;
    }
    return [];
  },

  // 取消任务：POST /api/tasks/{id}/cancel（TasksApiServlet）。它先杀这次任务起的进程树，
  // 再中断工作线程——下载与解包循环只认这个中断标志。
  cancelTask: (taskId: string) => request<unknown>(`/api/tasks/${taskId}/cancel`, { method: "POST" }),

  task: (taskId: string) => request<Task>(`/api/tasks/${encodeURIComponent(taskId)}`),

  // ---------- Phase 3：插件 ----------

  pluginCatalog: (refresh = false) =>
    request<{ plugins: PluginCatalogItem[] }>(`/api/plugins/catalog${refresh ? "?refresh=1" : ""}`),

  instancePlugins: (id: string) =>
    request<{ plugins: InstalledPlugin[]; bundles: InstalledPlugin[] }>(`/api/instances/${id}/plugins`),

  installPlugins: (id: string, specs: string[]) =>
    request<{ taskId: string }>(`/api/instances/${id}/plugins/install`, jsonInit("POST", { specs })),

  removePlugins: (id: string, specs: string[]) =>
    request<{ taskId: string }>(`/api/instances/${id}/plugins/remove`, jsonInit("POST", { specs })),

  // multipart 上传：不要手动设 Content-Type，浏览器会自动带 boundary
  uploadLocalPlugin: (id: string, file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return request<{ taskId: string }>(`/api/instances/${id}/plugins/local`, { method: "POST", body: fd });
  },

  // 面板精选的 TUI 终端插件清单（全局常量），以及每个实例上它们的安装状态。
  // 安装/卸载走 `/api/instances/{id}/tui-plugins/*`：TUI 插件是 app 插件，必须装进它
  // 自己的 profile —— 装进实例的 web profile 会和 dsh-web-app 撞在同一份 argv 上，实例就起不来了。
  // profile 由服务端按清单决定，请求里只说包名。
  tuiPlugins: () => request<{ plugins: TuiPluginItem[] }>("/api/dsh/tui-plugins"),

  instanceTuiPlugins: (id: string) =>
    request<{ plugins: TuiPluginState[] }>(`/api/instances/${id}/tui-plugins`),

  installTuiPlugin: (id: string, packageName: string) =>
    request<{ taskId: string }>(
      `/api/instances/${id}/tui-plugins/install`,
      jsonInit("POST", { package: packageName }),
    ),

  removeTuiPlugin: (id: string, packageName: string) =>
    request<{ taskId: string }>(
      `/api/instances/${id}/tui-plugins/remove`,
      jsonInit("POST", { package: packageName }),
    ),

  approveTask: (taskId: string, allow: boolean) =>
    request<unknown>(`/api/tasks/${taskId}/approve`, jsonInit("POST", { allow })),

  // ---------- Phase 3：账户 ----------


  accounts: () => request<{ accounts: Account[] }>("/api/accounts"),

  createAccount: (body: CreateAccountBody) =>
    request<{ account: Account; verified?: boolean; verifyError?: string }>(
      "/api/accounts",
      jsonInit("POST", body),
    ),

  verifyAccount: (name: string) =>
    request<{ ok: boolean; models?: string[]; error?: string; note?: string }>(
      `/api/accounts/${encodeURIComponent(name)}/verify`,
    ),

  patchAccount: (name: string, body: PatchAccountBody) =>
    request<unknown>(`/api/accounts/${encodeURIComponent(name)}`, jsonInit("PATCH", body)),

  deleteAccount: (name: string) =>
    request<unknown>(`/api/accounts/${encodeURIComponent(name)}`, { method: "DELETE" }),

  // ---------- Phase 3：模型目录（models.dev） ----------

  /** 供应商名单；refresh = 走后端 ?refresh=1 强制重抓（默认落 12 小时 TTL 缓存） */
  modelProviders: (refresh = false) =>
    request<{ fetchedAt: number | null; source: string; providers: ModelProvider[] }>(
      `/api/models/providers${refresh ? "?refresh=1" : ""}`,
    ),

  /** 单家供应商的模型列表；dsh 认识但目录里没有 → 200 + 空数组 */
  modelProvider: (id: string) =>
    request<{ vendor: ModelProvider; models: ModelInfo[] }>(
      `/api/models/providers/${encodeURIComponent(id)}`,
    ),

  // ---------- Phase 3：TLS ----------

  getTls: () => request<TlsSettings>("/api/settings/tls"),

  uploadTls: (fd: FormData) =>
    request<TlsUploadResult>("/api/settings/tls", { method: "POST", body: fd }),

  /** 生成（或复用）自签名证书并在同一端口切到 HTTPS；浏览器会提示证书不受信任 */
  enableSelfSignedTls: () =>
    request<{ ok: boolean; https?: boolean; restartRequired?: boolean; error?: string; fingerprintSha256?: string }>(
      "/api/settings/tls/self-signed",
      { method: "POST" },
    ),

  // ---------- 下载源（npm registry） ----------

  getRegistry: () => request<RegistrySettings>("/api/settings/registry"),

  /** 选预设传 { preset }；手填传 { preset: "custom", registry }。成功时回读同 GET 的形状 */
  setRegistry: (body: { preset: string; registry?: string }) =>
    request<RegistrySettings>("/api/settings/registry", jsonInit("POST", body)),

  /** 测速：不传地址就测当前生效的那个源 */
  testRegistry: (registry?: string) =>
    request<RegistryTestResult>("/api/settings/registry/test", jsonInit("POST", registry ? { registry } : {})),

  // ---------- Phase 4：整合包市场 / 安装 / 上传 ----------

  packsMarket: (refresh = false) =>
    request<{ packs: MarketPack[] }>(`/api/packs/market${refresh ? "?refresh=1" : ""}`),

  packMarketDetail: (packId: string) =>
    request<{ pack: MarketPackDetail }>(`/api/packs/market/${encodeURIComponent(packId)}`),

  installPack: (body: { packId: string; version?: string; name?: string }) =>
    request<{ taskId: string }>("/api/packs/install", jsonInit("POST", body)),

  // multipart：file(.dspack) + 可选 name；不要手动设 Content-Type
  uploadPack: (file: File, name?: string) => {
    const fd = new FormData();
    fd.append("file", file);
    if (name) fd.append("name", name);
    return request<{ taskId: string }>("/api/packs/upload", { method: "POST", body: fd });
  },

  // ---------- 壁纸 ----------

  /** 可选壁纸清单：内置的在前，用户上传的按时间排在后面 */
  wallpapers: () => request<WallpaperList>("/api/wallpapers"),

  // multipart：只收静态位图，判据在服务端（魔数 + 动图特征 + 真读一遍图片头）
  uploadWallpaper: (file: File, name?: string) => {
    const fd = new FormData();
    fd.append("file", file);
    if (name) fd.append("name", name);
    return request<Wallpaper>("/api/wallpapers", { method: "POST", body: fd });
  },

  deleteWallpaper: (id: string) =>
    request<void>(`/api/wallpapers/${encodeURIComponent(id)}`, { method: "DELETE" }),

  // ---------- Phase 4：导出 ----------

  /** 导出表单的可选项：能随包的 home 内容 + 本机已装的 dsh 版本（从不猜，由后端列） */
  exportChoices: (id: string) => request<ExportChoices>(`/api/instances/${id}/export`),

  exportInstance: (
    id: string,
    body: {
      name?: string;
      includeSessions?: boolean;
      /** 白名单：随包分发的 home 相对路径，缺省/空即不分发任何 home 内容 */
      home?: string[];
      /** 写进 manifest 的兼容集，实例自身版本由后端固定排在首位 */
      dshVersions?: string[];
    },
  ) => request<{ taskId: string }>(`/api/instances/${id}/export`, jsonInit("POST", body)),

  exports: () => request<{ exports: ExportRecord[] }>("/api/exports"),

  /** 导出文件下载地址：浏览器直接打开（同源带 cookie） */
  exportDownloadUrl: (filename: string) => `/api/exports/${encodeURIComponent(filename)}`,

  // ---------- Phase 4：会话 / 工作区 ----------

  instanceSessions: (id: string) =>
    request<{ sessions: SessionInfo[] }>(`/api/instances/${id}/sessions`),

  exportSessions: (id: string, sessionIds: string[]) =>
    request<{ taskId: string }>(`/api/instances/${id}/sessions/export`, jsonInit("POST", { sessionIds })),

  instanceWorkspaces: (id: string) =>
    request<{ workspaces: WorkspaceInfo[] }>(`/api/instances/${id}/workspaces`),

  // ---------- Phase 4：技能 ----------

  instanceSkills: (id: string) => request<{ skills: SkillInfo[] }>(`/api/instances/${id}/skills`),

  installSkill: (id: string, source: string) =>
    request<{ taskId: string }>(`/api/instances/${id}/skills/install`, jsonInit("POST", { source })),

  deleteSkill: (id: string, skillId: string) =>
    request<unknown>(`/api/instances/${id}/skills/${encodeURIComponent(skillId)}`, { method: "DELETE" }),

  // ---------- Phase 4：体检 ----------

  doctor: () => request<DoctorReport>("/api/doctor"),

  // ---------- 实验性：ZCode 品类（见 docs/zcode-experimental.md） ----------

  /** ZCode 发行包检测：HDSL_ZCODE_PACKAGE 或 <dataDir>/zcode/current/ */
  zcodeDist: () => request<ZcodeDistInfo>("/api/zcode/dist"),

  zcodeInstances: () =>
    request<{ instances: ZcodeInstance[]; workspacesRoot?: string }>("/api/zcode/instances"),

  createZcodeInstance: (body: { name: string; baseUrl?: string; apiKey?: string; workspacePath?: string; version?: string; seed?: string }) =>
    request<{ instance: ZcodeInstance }>("/api/zcode/instances", jsonInit("POST", body)),

  patchZcodeInstance: (id: string, body: { name?: string; baseUrl?: string; apiKey?: string; version?: string }) =>
    request<{ instance: ZcodeInstance }>(
      `/api/zcode/instances/${encodeURIComponent(id)}`,
      jsonInit("PATCH", body),
    ),

  /** 上游版本目录：正式 release / 仅 tag / 未发布的 main，带本地构建与凭据状态。 */
  zcodeCatalog: (refresh = false) =>
    request<ZcodeCatalog>(`/api/zcode/catalog${refresh ? "?refresh=1" : ""}`),

  /** 更换实例的 credential-secret；旧凭据会被丢弃，需要重新添加 Key/登录。 */
  rotateZcodeKey: (id: string, body: { seed?: string; clearProviderKey?: boolean }) =>
    request<{ instance: ZcodeInstance }>(
      `/api/zcode/instances/${encodeURIComponent(id)}/rotate-key`,
      jsonInit("POST", body),
    ),

  deleteZcodeInstance: (id: string) =>
    request<unknown>(`/api/zcode/instances/${encodeURIComponent(id)}`, { method: "DELETE" }),

  /** 启动并等 ZCode 的就绪行；error 状态会带日志尾部作为原因 */
  launchZcode: (id: string) =>
    request<{ state: string; error?: string }>(
      `/api/zcode/instances/${encodeURIComponent(id)}/launch`,
      jsonInit("POST", {}),
    ),

  stopZcode: (id: string) =>
    request<{ state: string; error?: string }>(
      `/api/zcode/instances/${encodeURIComponent(id)}/stop`,
      jsonInit("POST", {}),
    ),

  /** 运行中实例的打开地址（HTTP 明文 + 令牌，用请求 Host 拼出来） */
  zcodeOpen: (id: string) => request<{ url: string }>(`/api/zcode/instances/${encodeURIComponent(id)}/open`),

  /// ZCode 的原地终端：`node <发行包>/bin/zcode.mjs`（不带 --web，即官方 TUI）。
  /// 协议与品牌实例相同，见 docs/terminal-protocol.md。
  openZcodeTerminal: (id: string) =>
    request<TerminalOpen>(`/api/zcode/instances/${encodeURIComponent(id)}/terminal`, jsonInit("POST", {})),
  closeZcodeTerminal: (id: string) =>
    request<{ ok: boolean; killed: boolean }>(`/api/zcode/instances/${encodeURIComponent(id)}/terminal`, {
      method: "DELETE",
    }),

  zcodeLogs: (id: string, tail = 200) =>
    request<{ lines: string[] }>(`/api/zcode/instances/${encodeURIComponent(id)}/logs?tail=${tail}`),

  /** 插件通道（预留）：ZCode 品类现在不支持插件，形状与品牌实例一致 */
  zcodePlugins: (id: string) =>
    request<InstancePlugins>(`/api/zcode/instances/${encodeURIComponent(id)}/plugins`),

  /** 已安装的发行包（构造器装出来的那些） */
  zcodeReleases: () => request<{ releases: ZcodeRelease[] }>("/api/zcode/releases"),

  /** 面板内构建：当前状态 */
  zcodeBuild: () => request<ZcodeBuildStatus>("/api/zcode/build"),

  /** 面板内构建：开始（下载源码 → 打补丁 → pnpm 构建 → 安装） */
  startZcodeBuild: (version: string) =>
    request<ZcodeBuildStatus>("/api/zcode/build", jsonInit("POST", { version })),

  zcodeBuildLog: (tail = 400) => request<{ lines: string[] }>(`/api/zcode/build/log?tail=${tail}`),

  // ---------- 第三方品牌实例（kimi / opencode，REST 挂 /api/brands/<brand>/*） ----------

  brandVersions: (brand: BrandId, refresh = false) =>
    request<BrandVersions>(`/api/brands/${brand}/versions${refresh ? "?refresh=1" : ""}`),

  /** 面板托管的 GitHub 访问令牌（只回传是否配置与登录名，不回传令牌）。 */
  gitHubToken: () => request<GitHubTokenStatus>("/api/settings/brands/github-token"),
  saveGitHubToken: (token: string) =>
    request<GitHubTokenStatus>("/api/settings/brands/github-token", jsonInit("POST", { token })),
  clearGitHubToken: () =>
    request<unknown>("/api/settings/brands/github-token", { method: "DELETE" }),

  /** 纯本地：每个品牌在这台机器上能不能装，用于页签上的提醒 */
  brandCapabilities: () => request<BrandCapabilities>("/api/brands/capabilities"),

  brandReleases: (brand: BrandId) =>
    request<{ releases: { version: string; path: string; current: boolean; pairedSdk?: string; webAvailable?: boolean; ptyAvailable?: boolean }[] }>(
      `/api/brands/${brand}/releases`,
    ),

  /** 开始安装指定版本；品牌内同时只允许一个安装（409） */
  brandInstall: (brand: BrandId, version: string, mode?: "full" | "terminal-only") =>
    request<{ taskId: string }>(`/api/brands/${brand}/install`, jsonInit("POST", { version, mode })),

  brandInstallStatus: (brand: BrandId) =>
    request<BrandInstallStatus>(`/api/brands/${brand}/install`),

  brandInstallLog: (brand: BrandId, tail = 300) =>
    request<{ lines: string[] }>(`/api/brands/${brand}/install/log?tail=${tail}`),

  brandInstances: (brand: BrandId) =>
    request<{ instances: BrandInstance[] }>(`/api/brands/${brand}/instances`),

  /** 只有 hermes 有：OpenAI 兼容端点 + 它的 key（面板会话门之内） */
  brandEndpoint: (brand: BrandId, id: string) =>
    request<{ baseUrl: string; apiPort: number; key: string; running: boolean }>(
      `/api/brands/${brand}/instances/${encodeURIComponent(id)}/endpoint`,
    ),

  /** version 缺省用已装最新；一个版本没装 → 400 */
  createBrandInstance: (brand: BrandId, body: { name: string; version?: string; webEnabled?: boolean }) =>
    request<{ instance: BrandInstance }>(`/api/brands/${brand}/instances`, jsonInit("POST", body)),

  /** 运行中改 version → 409；未安装的 version → 400 */
  /** baseUrl 目前只有 claude-web 接受（ANTHROPIC_BASE_URL）；传空串表示清除 */
  patchBrandInstance: (brand: BrandId, id: string, body: { name?: string; version?: string; baseUrl?: string; nodeRuntime?: string | null; webEnabled?: boolean; versionLocked?: boolean }) =>
    request<{ instance: BrandInstance }>(
      `/api/brands/${brand}/instances/${encodeURIComponent(id)}`,
      jsonInit("PATCH", body),
    ),

  /** 运行中删除 → 409 */
  deleteBrandInstance: (brand: BrandId, id: string) =>
    request<unknown>(`/api/brands/${brand}/instances/${encodeURIComponent(id)}`, { method: "DELETE" }),

  /** 实例自己的管理员凭据（目前只有 octop）：读，或带 password 重置（空串=随机生成） */
  brandAdminPassword: (
    brand: BrandId,
    id: string,
    body: { password?: string; adminPassword?: string },
  ) =>
    request<BrandAdminCredentials>(
      `/api/brands/${brand}/instances/${encodeURIComponent(id)}/admin-password`,
      jsonInit("POST", body),
    ),

  /** 同步阻塞至就绪（最多 60s）；error 状态带原因 */
  launchBrand: (brand: BrandId, id: string) =>
    request<{ state: string; error?: string }>(
      `/api/brands/${brand}/instances/${encodeURIComponent(id)}/launch`,
      jsonInit("POST", {}),
    ),

  stopBrand: (brand: BrandId, id: string) =>
    request<{ state: string }>(
      `/api/brands/${brand}/instances/${encodeURIComponent(id)}/stop`,
      jsonInit("POST", {}),
    ),

  /** 未运行 → 409 */
  openBrand: (brand: BrandId, id: string) =>
    request<{ url: string }>(`/api/brands/${brand}/instances/${encodeURIComponent(id)}/open`),

  brandLogs: (brand: BrandId, id: string, tail = 300) =>
    request<{ lines: string[] }>(
      `/api/brands/${brand}/instances/${encodeURIComponent(id)}/logs?tail=${tail}`,
    ),

  /** 插件通道（预留）：品牌实例现在不支持插件，supported=false + 空列表 */
  brandPlugins: (brand: BrandId, id: string) =>
    request<InstancePlugins>(`/api/brands/${brand}/instances/${encodeURIComponent(id)}/plugins`),

  // ---- Node.js 工具链（裸部署一键供给，设置 → Node.js） ----
  nodeStatus: () =>
    request<NodeStatus>("/api/node"),

  /** 出站 TLS 的授权状态与一次性确认（见 NetworkTrust）。 */
  networkTrust: {
    state: () => request<NetworkTrust>("/api/network/trust"),
    accept: (minutes: number, acknowledge: string) =>
      request<NetworkTrust>("/api/network/trust", jsonInit("POST", { minutes, acknowledge })),
    revoke: () => request<NetworkTrust>("/api/network/trust", { method: "DELETE" }),
  },
  nodeInstall: (body: { version?: string; source?: string }) =>
    request<{ taskId: string }>("/api/node/install", jsonInit("POST", body)),
  nodeUninstall: (version: string) =>
    request<{ ok: boolean }>("/api/node/uninstall", jsonInit("POST", { version })),
  /** 预检一个选择：下拉框在应用前拿它显示「会解析到哪个版本、是否满足」。 */
  nodeResolve: (selector: string, brand?: string) =>
    request<import("./types").NodeResolution>(
      `/api/node/resolve?selector=${encodeURIComponent(selector)}${brand ? `&brand=${encodeURIComponent(brand)}` : ""}`,
    ),
  /** 全局默认选择。 */
  nodeDefault: (selection: string) =>
    request<{ ok: boolean; default: string }>("/api/node/default", jsonInit("POST", { selection })),
  /** 禁用/启用一个精确版本（只影响自动解析，不杀进程）。 */
  nodeDisable: (version: string, disabled: boolean) =>
    request<{ ok: boolean; version: string; disabled: boolean }>(
      "/api/node/disable",
      jsonInit("POST", { version, disabled }),
    ),
  /** 把一个大版本更新到上游最新（已安装则只移动指针）。 */
  nodeChannelUpdate: (name: string, source?: "mirror" | "official") =>
    request<{ ok?: boolean; taskId?: string; channel: string; version: string }>(
      "/api/node/channels/update",
      jsonInit("POST", { name, source }),
    ),
  /** 回到某个消费者上次成功启动的精确版本。 */
  nodeRevert: (body: { kind: "instance" | "brand-instance"; brand?: string; id: string }) =>
    request<{ ok: boolean; nodeRuntime: string }>("/api/node/revert", jsonInit("POST", body)),
  /** 删除未使用且超出保留数的运行时。 */
  nodeGc: (keepPerMajor = 2) =>
    request<{ ok: boolean; removed: string[]; keepPerMajor: number }>(
      "/api/node/gc",
      jsonInit("POST", { keepPerMajor }),
    ),
  /** 纯文本版（legacy 表面）增强开关（设置 → 通用）。PUT 要开发者提权。 */
  plain: {
    settings: () => request<{ enhanced: boolean; ssrAvailable: boolean }>("/api/settings/plain"),
    setEnhanced: (enhanced: boolean) =>
      request<{ enhanced: boolean; ssrAvailable: boolean }>(
        "/api/settings/plain",
        jsonInit("PUT", { enhanced }),
      ),
  },
  /** 日志保留窗口（关于 → 日志）。PUT 要开发者提权，且只能往上调。 */
  panelLogs: {
    retention: () => request<LogRetention>("/api/settings/logs"),
    setRetention: (body: { auditDays?: number; panelDays?: number }) =>
      request<LogRetention>("/api/settings/logs", jsonInit("PUT", body)),
    /** 界面能看的日志：服务端写死最近几天，不随保留窗口变。 */
    recent: () => request<{ viewDays: number; files: LogFileEntry[] }>("/api/settings/logs/recent"),
    tail: (name: string, tail = 200) =>
      request<{ name: string; lines: string[] }>(
        `/api/settings/logs/recent/${encodeURIComponent(name)}?tail=${tail}`,
      ),
  },
  mc: {
    feature: () => request<McFeature>("/api/mc/feature"),
    eula: () => request<McEula>("/api/mc/eula"),
    terms: () => request<McTerms>("/api/mc/terms"),
    enable: (body: {
      eulaAcknowledged: boolean;
      termsAcknowledged: boolean;
      eulaHash: string;
      termsVersion: number;
      termsTextHash: string;
      sawDeclineOption: boolean;
      uiVersion: string;
    }) => request<McFeature>("/api/mc/feature/enable", jsonInit("POST", body)),
    stopFeature: () => request<McFeature>("/api/mc/feature/stop", jsonInit("POST", {})),
    confirmClose: () => request<McFeature>("/api/mc/feature/confirm-close", jsonInit("POST", {})),
    cancelClose: () => request<McFeature>("/api/mc/feature/cancel-close", jsonInit("POST", {})),
    decline: () => request<{ ok: boolean }>("/api/mc/feature/decline", jsonInit("POST", {})),
    /** 第三方核心目录：第二道关关闭时只返回 vanilla。 */
    cores: (refresh = false) =>
      request<{ cores: McCoreInfo[] }>(`/api/mc/cores${refresh ? "?refresh=1" : ""}`),
    thirdParty: () => request<{ state: "DISABLED" | "ENABLED"; enabled: boolean; termsVersion: number;
      termsTextHash?: string; acceptedAt?: string; acceptedBy?: string; ackMethod?: string }>(
      "/api/mc/third-party/feature"),
    thirdPartyTerms: () => request<McTerms>("/api/mc/third-party/terms"),
    enableThirdParty: (body: { termsVersion: number; termsTextHash: string; uiVersion: string }) =>
      request<{ state: string; enabled: boolean }>("/api/mc/third-party/enable", jsonInit("POST", body)),
    disableThirdParty: () =>
      request<{ state: string; enabled: boolean }>("/api/mc/third-party/disable", jsonInit("POST", {})),
    declineThirdParty: () =>
      request<{ ok: boolean }>("/api/mc/third-party/decline", jsonInit("POST", {})),
    /** 下载源顺序、自定义基址与配额余量。 */
    sources: () => request<McSourcesSnapshot>("/api/mc/third-party/sources"),
    updateSources: (body: {
      order?: Record<string, string[]>;
      custom?: Record<string, string>;
    }) => request<McSourcesSnapshot>("/api/mc/third-party/sources", jsonInit("POST", body)),
    /** 手动导入核心 jar：所有下载源都不可达时的兜底。 */
    uploadCore: (id: string, version: string, file: File) =>
      request<McInstance>(
        `/api/mc/instances/${encodeURIComponent(id)}/core?version=${encodeURIComponent(version)}`,
        { method: "PUT", body: file },
        { timeoutMs: 10 * 60 * 1000 },
      ),
    /** `withCatalogue=false` 只读已安装列表（设置卡片用）；向导才需要三个发行版的目录。 */
    javaRuntimes: (version?: string, withCatalogue = false) => {
      const params = new URLSearchParams();
      if (version) params.set("version", version);
      params.set("catalog", withCatalogue ? "1" : "0");
      return request<McJavaRuntimesSnapshot>(`/api/mc/java/runtimes?${params.toString()}`);
    },
    installJava: (feature: number, vendor = "temurin") =>
      request<{ taskId: string }>("/api/mc/java/install", jsonInit("POST", { feature, vendor })),
    removeJava: (feature: number) =>
      request<{ ok: boolean; removed: boolean }>(
        `/api/mc/java/${feature}/remove`, jsonInit("POST", {}),
      ),
    versions: () => request<{ versions: McVersion[] }>("/api/mc/versions"),
    /** 实例的插件/模组目录（kind: mods | plugins）。 */
    files: (id: string, kind: "mods" | "plugins") =>
      request<{ folder: { kind: string; loader?: string }; files: McModFile[] }>(
        `/api/mc/instances/${encodeURIComponent(id)}/files?kind=${kind}`,
      ),
    toggleFile: (id: string, kind: "mods" | "plugins", name: string, enabled: boolean) =>
      request<{ ok: boolean; name: string; enabled: boolean }>(
        `/api/mc/instances/${encodeURIComponent(id)}/files/${kind}/${encodeURIComponent(name)}/toggle`,
        jsonInit("POST", { enabled }),
      ),
    deleteFile: (id: string, kind: "mods" | "plugins", name: string) =>
      request<{ ok: boolean }>(
        `/api/mc/instances/${encodeURIComponent(id)}/files/${kind}/${encodeURIComponent(name)}`,
        { method: "DELETE" },
      ),
    uploadFile: (id: string, kind: "mods" | "plugins", file: File) =>
      request<{ ok: boolean; name: string }>(
        `/api/mc/instances/${encodeURIComponent(id)}/files/${kind}?name=${encodeURIComponent(file.name)}`,
        { method: "PUT", body: file },
        { timeoutMs: 5 * 60 * 1000 },
      ),
    modrinthSearch: (core: string, version: string, query: string, offset = 0, limit = 20) =>
      request<{ hits: McModHit[]; totalHits: number; offset: number; relaxed: boolean }>(
        `/api/mc/modrinth/search?core=${encodeURIComponent(core)}&version=${encodeURIComponent(version)}`
        + `&query=${encodeURIComponent(query)}&offset=${offset}&limit=${limit}`,
      ),
    modrinthInstall: (instanceId: string, projectId: string) =>
      request<{ ok: boolean; name: string }>(
        "/api/mc/modrinth/install", jsonInit("POST", { instanceId, projectId }),
        { timeoutMs: 5 * 60 * 1000 },
      ),
    storage: () => request<McStorage>("/api/mc/storage"),
    suggestPorts: () => request<{ port: number; rconPort: number }>("/api/mc/ports"),
    /** 实例列表。`scope` 把原版和第三方彻底分开：界面上它们是两个入口。 */
    instances: (scope?: "vanilla" | "thirdparty") =>
      request<{ instances: McInstance[] }>(
        `/api/mc/instances${scope ? `?scope=${scope}` : ""}`,
      ),
    instance: (id: string) =>
      request<McInstance>(`/api/mc/instances/${encodeURIComponent(id)}`),
    instanceJava: (id: string) =>
      request<McInstanceJava>(`/api/mc/instances/${encodeURIComponent(id)}/java`),
    create: (body: {
      name: string;
      version: string;
      core?: string;
      memoryMb?: number;
      levelName?: string;
      seed?: string;
      port?: number;
      rconPort?: number;
      storage?: "external" | "data";
    }) => request<McInstance>("/api/mc/instances", jsonInit("POST", body)),
    rename: (id: string, name: string) =>
      request<McInstance>(`/api/mc/instances/${encodeURIComponent(id)}`, jsonInit("PATCH", { name })),
    update: (
      id: string,
      body: { name?: string; port?: number; rconPort?: number; memoryMb?: number; javaPath?: string | null },
    ) => request<McInstance>(`/api/mc/instances/${encodeURIComponent(id)}`, jsonInit("PATCH", body)),
    remove: (id: string) =>
      request<{ ok: boolean; backup: string }>(`/api/mc/instances/${encodeURIComponent(id)}`, { method: "DELETE" }),
    start: (id: string) =>
      request<McInstance>(`/api/mc/instances/${encodeURIComponent(id)}/start`, jsonInit("POST", {})),
    stop: (id: string) =>
      request<McInstance>(`/api/mc/instances/${encodeURIComponent(id)}/stop`, jsonInit("POST", {})),
    kill: (id: string) =>
      request<McInstance>(`/api/mc/instances/${encodeURIComponent(id)}/kill`, jsonInit("POST", {})),
    install: (id: string, version: string) =>
      request<{ taskId: string }>(
        `/api/mc/instances/${encodeURIComponent(id)}/install`,
        jsonInit("POST", { version }),
      ),
    backups: (id: string) =>
      request<{ backups: McBackup[] }>(`/api/mc/instances/${encodeURIComponent(id)}/backups`),
    createBackup: (id: string) =>
      request<McBackup>(`/api/mc/instances/${encodeURIComponent(id)}/backups`, jsonInit("POST", {}), {
        timeoutMs: 5 * 60 * 1000,
      }),
    deleteBackup: (id: string, name: string) =>
      request<{ ok: boolean }>(
        `/api/mc/instances/${encodeURIComponent(id)}/backups/${encodeURIComponent(name)}`,
        { method: "DELETE" },
      ),
    restoreBackup: (id: string, name: string) =>
      request<{ ok: boolean; safety: string }>(
        `/api/mc/instances/${encodeURIComponent(id)}/backups/${encodeURIComponent(name)}/restore`,
        jsonInit("POST", {}),
        { timeoutMs: 5 * 60 * 1000 },
      ),
    properties: (id: string) =>
      request<{ properties: Record<string, string> }>(
        `/api/mc/instances/${encodeURIComponent(id)}/properties`,
      ),
    saveProperties: (id: string, values: Record<string, string>) =>
      request<{ properties: Record<string, string> }>(
        `/api/mc/instances/${encodeURIComponent(id)}/properties`,
        jsonInit("PUT", values),
      ),
    logs: (id: string) =>
      request<{ lines: string[] }>(`/api/mc/instances/${encodeURIComponent(id)}/logs?tail=200`),
    consoleTicket: (id: string) =>
      request<TerminalOpen>(`/api/mc/instances/${encodeURIComponent(id)}/console/ticket`, jsonInit("POST", {})),
    consoleInput: (id: string, line: string) =>
      request<{ ok: boolean }>(`/api/mc/instances/${encodeURIComponent(id)}/console/input`, jsonInit("POST", { line })),
    players: (id: string) =>
      request<{ online: number; max: number; names: string[] }>(
        `/api/mc/instances/${encodeURIComponent(id)}/players`,
      ),
    playerAction: (id: string, name: string, action: "kick" | "op" | "deop" | "ban") =>
      request<{ ok: boolean; answer: string }>(
        `/api/mc/instances/${encodeURIComponent(id)}/players/${encodeURIComponent(name)}/${action}`,
        jsonInit("POST", {}),
      ),
    /** 提权探针：浏览器自己的下载带不了 elevated() 的重试，先在这里过一遍密码。 */
    authorizeWorld: (id: string) =>
      request<void>(`/api/mc/instances/${encodeURIComponent(id)}/world/authorize`, jsonInit("POST", {})),
    /** 下载 URL：交给浏览器流式下载。5 GB 的 zip 不能先 res.blob() 进内存。 */
    exportWorldUrl: (id: string) =>
      new URL(`api/mc/instances/${encodeURIComponent(id)}/world/export`, document.baseURI).toString(),
    /**
     * 上传世界 zip。用 XHR 而不是 fetch：只有 XHR 有上传进度事件。
     * 服务端把正文落盘后返回任务 id，解压与备份在后台任务里跑。
     */
    importWorld: (
      id: string,
      file: File,
      onProgress?: (sent: number, total: number) => void,
    ): Promise<{ taskId: string }> =>
      new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open(
          "POST",
          new URL(`api/mc/instances/${encodeURIComponent(id)}/world/import`, document.baseURI).toString(),
        );
        xhr.withCredentials = true;
        xhr.setRequestHeader("Content-Type", "application/zip");
        xhr.upload.onprogress = (event) => {
          if (event.lengthComputable) onProgress?.(event.loaded, event.total);
        };
        xhr.upload.onload = () => onProgress?.(file.size, file.size);
        xhr.onload = () => {
          if (xhr.status >= 200 && xhr.status < 300) {
            try {
              resolve(JSON.parse(xhr.responseText) as { taskId: string });
            } catch {
              reject(new ApiError(xhr.status, "服务器返回了无法解析的响应"));
            }
          } else {
            reject(new ApiError(xhr.status, xhr.responseText));
          }
        };
        xhr.onerror = () => reject(new ApiError(0, "无法连接服务器"));
        xhr.onabort = () => reject(new ApiError(0, "上传已取消"));
        xhr.send(file);
      }),
    importPlayerLists: async (
      id: string,
      file: File,
    ): Promise<{ ok: boolean; files: Record<string, number> }> => {
      const res = await fetch(`api/mc/instances/${encodeURIComponent(id)}/player-lists`, {
        method: "POST",
        credentials: "same-origin",
        cache: "no-store",
        body: file,
        headers: { "Content-Type": "application/zip" },
      });
      if (!res.ok) throw new ApiError(res.status, await res.text());
      return (await res.json()) as { ok: boolean; files: Record<string, number> };
    },
  },
  transcode: {
    feature: () => request<TranscodeFeature>("/api/transcode/feature"),
    terms: () => request<TranscodeTerms>("/api/transcode/terms"),
    enable: (body: {
      termsAcknowledged: boolean;
      termsVersion: number;
      termsTextHash: string;
      sawDeclineOption: boolean;
      uiVersion: string;
    }) => request<TranscodeFeature>("/api/transcode/feature/enable", jsonInit("POST", body)),
    decline: () => request<TranscodeFeature>("/api/transcode/feature/decline", jsonInit("POST", {})),
    disable: () => request<TranscodeFeature>("/api/transcode/feature/disable", jsonInit("POST", {})),
    capabilities: () => request<TranscodeCapabilities>("/api/transcode/capabilities"),
    probeEncoder: (encoder: string) =>
      request<{ ok: boolean; encoder: string; exitCode: number; output: string }>(
        "/api/transcode/capabilities/probe", jsonInit("POST", { encoder })),
    mediaList: (path = "", page = 0, pageSize = 200) =>
      request<TranscodeMediaListing>(
        `/api/transcode/media/list?path=${encodeURIComponent(path)}&page=${page}&pageSize=${pageSize}`,
      ),
    mediaProbe: (path: string) =>
      request<Record<string, unknown>>(`/api/transcode/media/probe?path=${encodeURIComponent(path)}`),
    mediaScan: (path = "", recursive = true, limit = 2000) =>
      request<TranscodeMediaScan>(
        `/api/transcode/media/scan?path=${encodeURIComponent(path)}&recursive=${recursive ? 1 : 0}&limit=${limit}`,
      ),
    plan: (body: { preset?: TranscodePreset; presetId?: string; inputs: string[]; outputDir?: string }) =>
      request<TranscodePlan>("/api/transcode/plan", jsonInit("POST", body)),
    presets: () => request<{ presets: TranscodePresetSummary[] }>("/api/transcode/presets"),
    preset: (id: string) =>
      request<TranscodePresetEntry>(`/api/transcode/presets/${encodeURIComponent(id)}`),
    schema: () => request<TranscodeSchema>("/api/transcode/presets/schema"),
    savePreset: (id: string, preset: Record<string, unknown>) =>
      request<TranscodePresetEntry>("/api/transcode/presets", jsonInit("POST", { id, preset })),
    deletePreset: (id: string) =>
      request<{ ok: boolean }>(`/api/transcode/presets/${encodeURIComponent(id)}`, { method: "DELETE" }),
    importPreset: (id: string, data: Record<string, unknown>, name?: string) =>
      request<TranscodePresetEntry>("/api/transcode/presets/import",
        jsonInit("POST", { id, name, data })),
    exportPreset: (id: string, format: "json" | "3fui") =>
      request<Record<string, unknown>>(
        `/api/transcode/presets/${encodeURIComponent(id)}/export?format=${format}`,
      ),
    preview: (body: { presetId?: string; preset?: Record<string, unknown>; input?: string; outputDir?: string }) =>
      request<TranscodePreview>("/api/transcode/preview", jsonInit("POST", body)),
    queue: () => request<TranscodeQueueSnapshot>("/api/transcode/queue"),
    startQueue: () => request<TranscodeQueueSnapshot>("/api/transcode/queue/start", jsonInit("POST", {})),
    pauseQueue: () => request<TranscodeQueueSnapshot>("/api/transcode/queue/pause", jsonInit("POST", {})),
    stopQueue: () => request<TranscodeQueueSnapshot>("/api/transcode/queue/stop", jsonInit("POST", {})),
    clearFinished: () => request<TranscodeQueueSnapshot>("/api/transcode/queue/clear", jsonInit("POST", {})),
    submitJob: (body: { input: string; presetId?: string; preset?: Record<string, unknown>; outputDir?: string }) =>
      request<TranscodeJob>("/api/transcode/queue/jobs", jsonInit("POST", body)),
    removeJob: (id: string) =>
      request<TranscodeQueueSnapshot>(
        `/api/transcode/queue/jobs/${encodeURIComponent(id)}`,
        { method: "DELETE" },
      ),
    resetJobPreset: (id: string, presetId: string) =>
      request<TranscodeQueueSnapshot>(
        `/api/transcode/queue/jobs/${encodeURIComponent(id)}/reset-preset`,
        jsonInit("POST", { presetId }),
      ),
    startJob: (id: string) =>
      request<TranscodeQueueSnapshot>(
        `/api/transcode/queue/jobs/${encodeURIComponent(id)}/start`,
        jsonInit("POST", {}),
      ),
    queueBatch: (action: "start" | "stop" | "cancel" | "retry" | "pause" | "resume" | "remove", ids: string[]) =>
      request<TranscodeBatchResult>("/api/transcode/queue/action", jsonInit("POST", { action, ids })),
    reorderQueue: (ids: string[]) =>
      request<TranscodeQueueSnapshot>("/api/transcode/queue/reorder", jsonInit("POST", { ids })),
    syncQueuePreset: (presetId: string, ids: string[] = []) =>
      request<TranscodePresetSyncResult>(
        "/api/transcode/queue/sync-preset",
        jsonInit("POST", { presetId, ids }),
      ),
    job: (id: string) =>
      request<TranscodeJob>(`/api/transcode/queue/jobs/${encodeURIComponent(id)}`),
    jobLog: (id: string, tail = 500) =>
      request<{ id: string; log: string }>(
        `/api/transcode/queue/jobs/${encodeURIComponent(id)}/log?tail=${tail}`,
      ),
    jobAction: (id: string, action: "cancel" | "retry" | "pause" | "resume") =>
      request<TranscodeQueueSnapshot>(
        `/api/transcode/queue/jobs/${encodeURIComponent(id)}/${action}`,
        jsonInit("POST", {}),
      ),
    jobDecision: (id: string, action: "stop" | "fallback" | "fallback-queue" | "auto") =>
      request<TranscodeQueueSnapshot>(
        `/api/transcode/queue/jobs/${encodeURIComponent(id)}/decision`,
        jsonInit("POST", { action }),
      ),
    toolsMerge: (inputs: string[], output: string) =>
      request<TranscodeJob>("/api/transcode/tools/merge", jsonInit("POST", { inputs, output })),
    toolsMux: (inputs: string[], maps: string[], output: string) =>
      request<TranscodeJob>("/api/transcode/tools/mux", jsonInit("POST", { inputs, maps, output })),
    toolsExtract: (input: string, type: "video" | "audio" | "subtitle", index: number, output: string) =>
      request<TranscodeJob>("/api/transcode/tools/extract",
        jsonInit("POST", { input, type, index, output })),
    qualityStart: (body: { reference: string; encoded: string; metrics: string[]; start?: string; duration?: string }) =>
      request<TranscodeQualityJob>("/api/transcode/tools/quality", jsonInit("POST", body)),
    qualityJob: (id: string) =>
      request<TranscodeQualityJob>(`/api/transcode/tools/quality/${encodeURIComponent(id)}`),
    qualityCancel: (id: string) =>
      request<TranscodeQualityJob>(`/api/transcode/tools/quality/${encodeURIComponent(id)}/cancel`,
        jsonInit("POST", {})),
    updateSettings: (body: { failurePolicy: string; failureTimeoutMinutes?: number }) =>
      request<TranscodeFeature>("/api/transcode/settings", jsonInit("PUT", body)),
    runtimeInstall: (body: { kind: "static" | "package" | "compile"; url?: string; sha256?: string; version?: string }) =>
      request<TranscodeInstallJob>("/api/transcode/runtime/install", jsonInit("POST", body)),
    runtimeJob: (id: string) =>
      request<TranscodeInstallJob>(`/api/transcode/runtime/install/${encodeURIComponent(id)}`),
    runtimeCancel: (id: string) =>
      request<TranscodeInstallJob>(`/api/transcode/runtime/install/${encodeURIComponent(id)}/cancel`,
        jsonInit("POST", {})),
  },
  // ---------- 面板聊天（docs/chat.md）：命令走 REST，流走 /ws ----------
  chat: {
    assistants: () => request<{ assistants: ChatAssistant[] }>("/api/chat/assistants"),
    createAssistant: (body: Record<string, unknown>) =>
      request<ChatAssistant>("/api/chat/assistants", jsonInit("POST", body)),
    updateAssistant: (id: string, body: Record<string, unknown>) =>
      request<ChatAssistant>(`/api/chat/assistants/${encodeURIComponent(id)}`, jsonInit("PATCH", body)),
    deleteAssistant: (id: string) =>
      request<{ ok: boolean }>(`/api/chat/assistants/${encodeURIComponent(id)}`, { method: "DELETE" }),
    duplicateAssistant: (id: string) =>
      request<ChatAssistant>(`/api/chat/assistants/${encodeURIComponent(id)}/duplicate`, jsonInit("POST", {})),

    sessions: (assistantId?: string) =>
      request<{ sessions: ChatSessionSummary[] }>(
        `/api/chat/sessions${assistantId ? `?assistant=${encodeURIComponent(assistantId)}` : ""}`,
      ),
    session: (id: string) =>
      request<ChatSession>(`/api/chat/sessions/${encodeURIComponent(id)}`),
    /** 导入会话：DeepSeek 官方导出 zip 或 HSDL 单会话 JSON。校验在服务端。 */
    importSessions: (file: File, assistantId: string, title?: string) => {
      const form = new FormData();
      form.append("file", file);
      const query = new URLSearchParams({ assistantId });
      if (title) query.set("title", title);
      return request<{ format: string; imported: { id: string; title: string; messages: number }[]; skipped: string[] }>(
        `api/chat/import?${query.toString()}`,
        { method: "POST", body: form },
      );
    },
    /** 导出会话：返回原始 JSON，由页面触发下载。 */
    exportSession: async (id: string): Promise<Blob> => {
      const res = await fetch(`api/chat/sessions/${encodeURIComponent(id)}/export`.replace(/^\//, ""), {
        credentials: "same-origin",
        cache: "no-store",
      });
      if (!res.ok) throw new ApiError(res.status, "导出失败");
      return res.blob();
    },
    /** 整段会话分叉（复制），可指定目标助手；原会话不动。 */
    forkSession: (id: string, body: { assistantId?: string; title?: string } = {}) =>
      request<ChatSession>(`/api/chat/sessions/${encodeURIComponent(id)}/fork`, jsonInit("POST", body)),
    createSession: (body: { assistantId?: string; title?: string; model?: string; account?: string; reasoningLevel?: string }) =>
      request<ChatSession>("/api/chat/sessions", jsonInit("POST", body)),
    updateSession: (id: string, body: { title?: string; model?: string; account?: string; reasoningLevel?: string }) =>
      request<ChatSession>(`/api/chat/sessions/${encodeURIComponent(id)}`, jsonInit("PATCH", body)),
    deleteSession: (id: string) =>
      request<{ ok: boolean }>(`/api/chat/sessions/${encodeURIComponent(id)}`, { method: "DELETE" }),

    send: (id: string, body: { text: string; attachments?: string[]; model?: string; account?: string; requestId?: string; search?: boolean; searchProvider?: string }) =>
      request<ChatTurnOutcome>(`/api/chat/sessions/${encodeURIComponent(id)}/messages`, jsonInit("POST", body)),
    regenerate: (id: string, body: { nodeId: string; mode: "in_place" | "truncate_after"; requestId?: string; search?: boolean; searchProvider?: string }) =>
      request<ChatTurnOutcome>(`/api/chat/sessions/${encodeURIComponent(id)}/regenerate`, jsonInit("POST", body)),
    edit: (id: string, body: { nodeId: string; text: string; requestId?: string }) =>
      request<ChatTurnOutcome>(`/api/chat/sessions/${encodeURIComponent(id)}/edit`, jsonInit("POST", body)),
    cancel: (turnId: string) =>
      request<{ ok: boolean }>(`/api/chat/turns/${encodeURIComponent(turnId)}/cancel`, jsonInit("POST", {})),
    deleteNode: (sessionId: string, nodeId: string) =>
      request<ChatSession>(
        `/api/chat/sessions/${encodeURIComponent(sessionId)}/nodes/${encodeURIComponent(nodeId)}`,
        { method: "DELETE" },
      ),
    switchVariant: (sessionId: string, nodeId: string, messageId: string) =>
      request<ChatSession>(
        `/api/chat/sessions/${encodeURIComponent(sessionId)}/nodes/${encodeURIComponent(nodeId)}/variant`,
        jsonInit("POST", { messageId }),
      ),

    uploadAttachment: (sessionId: string, file: File) => {
      const fd = new FormData();
      fd.append("file", file);
      return request<ChatAttachment>(
        `/api/chat/attachments?sessionId=${encodeURIComponent(sessionId)}`,
        { method: "POST", body: fd },
        { timeoutMs: 120000 },
      );
    },
    attachmentUrl: (id: string) => `/api/chat/attachments/${encodeURIComponent(id)}`,
    attachmentDownloadUrl: (id: string) =>
      `/api/chat/attachments/${encodeURIComponent(id)}?download=1`,
    attachmentText: (id: string, offset = 0, limit = 65536) =>
      request<ChatAttachmentText>(
        `/api/chat/attachments/${encodeURIComponent(id)}/text?offset=${offset}&limit=${limit}`,
        undefined,
        { timeoutMs: 60000 },
      ),

    searchStatus: () => request<ChatSearchStatus>("/api/chat/search/status"),
    searchTest: (query: string) =>
      request<ChatSearchResult>("/api/chat/search/test", jsonInit("POST", { query }), { timeoutMs: 60000 }),
    profile: () => request<ChatProfile>("/api/chat/profile"),
    updateProfile: (body: { name?: string; avatar?: string | null }) =>
      request<ChatProfile>("/api/chat/profile", jsonInit("PUT", body)),
    uploadAvatar: (file: File) => {
      const fd = new FormData();
      fd.append("file", file);
      return request<{ id: string; url: string }>(
        "/api/chat/avatars",
        { method: "POST", body: fd },
        { timeoutMs: 60000 },
      );
    },
    avatarUrl: (id: string) => `api/chat/avatars/${encodeURIComponent(id)}`,
    settings: () => request<ChatSettings>("/api/chat/settings"),
    updateSettings: (body: Record<string, unknown>) =>
      request<ChatSettings>("/api/chat/settings", jsonInit("PATCH", body)),
    backups: () => request<{ backups: ChatBackup[] }>("/api/chat/backups"),
    backupNow: () => request<ChatBackup>("/api/chat/backup", jsonInit("POST", {})),

    accounts: () => request<{ accounts: ChatAccountProfile[] }>("/api/chat/accounts"),
    models: (account: string) =>
      request<{ account: string; protocol: string; models: ChatModelInfo[] }>(
        `/api/chat/models?account=${encodeURIComponent(account)}`,
      ),
    saveAccountProfile: (account: string, body: Record<string, unknown>) =>
      request<ChatAccountProfile>(
        `/api/chat/accounts/${encodeURIComponent(account)}`,
        jsonInit("PATCH", body),
      ),
    saveCapability: (account: string, body: Record<string, unknown>) =>
      request<ChatModelInfo>(
        `/api/chat/accounts/${encodeURIComponent(account)}/capabilities`,
        jsonInit("POST", body),
      ),
    probe: (account: string) =>
      request<ChatProbe>(`/api/chat/accounts/${encodeURIComponent(account)}/probe`, jsonInit("POST", {})),
    chatTest: (account: string, model?: string | null) =>
      request<ChatChatTest>(
        `/api/chat/accounts/${encodeURIComponent(account)}/chat-test`,
        jsonInit("POST", model ? { model } : {}),
        { timeoutMs: 130000 },
      ),
    visionTest: (account: string, model: string) =>
      request<ChatVisionTest>(
        `/api/chat/accounts/${encodeURIComponent(account)}/vision-test`,
        jsonInit("POST", { model }),
        { timeoutMs: 130000 },
      ),
    refreshModels: (account: string) =>
      request<{ models: string[] }>(
        `/api/chat/accounts/${encodeURIComponent(account)}/models/refresh`,
        jsonInit("POST", {}),
      ),
  },
};

/// GET /api/transcode/feature：转码功能开关、媒体根与运行参数。
export interface TranscodeFeature {
  state: "DISABLED" | "ENABLED";
  enabled: boolean;
  termsVersion: number;
  termsTextHash: string;
  mediaRoot: string;
  mediaRootExists: boolean;
  mediaRootWritable: boolean;
  maxConcurrent: number;
  maxQueue: number;
  failurePolicy: "ask" | "timeout-auto" | "full-auto";
  failureTimeoutMinutes: number;
  autoResume: boolean;
  consent?: TranscodeConsentInfo;
}

export interface TranscodeConsentInfo {
  termsVersion: number;
  termsTextHash: string;
  acceptedAt: string;
  acceptedBy: string;
  acceptedFrom: string;
  ackMethod: string;
  uiVersion: string;
  clientUa?: string | null;
  buildCommit: string;
  sawDeclineOption: boolean;
}

/// GET /api/transcode/terms：启用前展示的须知。
export interface TranscodeTerms {
  version: number;
  text: string;
  hash: string;
}

export interface TranscodeCapabilities {
  available: boolean;
  version: string;
  ffmpeg?: string | null;
  ffprobe?: string | null;
  error?: string | null;
  probedAt: number;
  encoders: string[];
  hwaccels: string[];
  filters: string[];
  mediaRoot: string;
  mediaRootExists: boolean;
  mediaRootWritable: boolean;
  maxConcurrent: number;
  maxQueue: number;
}

export interface TranscodeMediaEntry {
  name: string;
  path: string;
  directory: boolean;
  size: number;
  modified: string;
  kind: "directory" | "video" | "audio" | "subtitle" | "image" | "other";
}

export interface TranscodeMediaListing {
  path: string;
  offset: number;
  total: number;
  entries: TranscodeMediaEntry[];
}

export interface TranscodeMediaScan {
  path: string;
  total: number;
  truncated: boolean;
  entries: TranscodeMediaEntry[];
}

export interface TranscodePlanItem {
  input: string;
  inputAbsolute?: string;
  output?: string;
  outputAbsolute?: string;
  exists?: boolean;
  error?: string;
}

export interface TranscodePlan {
  mediaRoot: string;
  outputDir: string;
  items: TranscodePlanItem[];
}

export interface TranscodePresetSummary {
  id: string;
  builtin: boolean;
  name: string;
  description: string;
}

export type TranscodePreset = Record<string, unknown>;

export interface TranscodePresetEntry {
  id: string;
  builtin: boolean;
  preset: TranscodePreset;
}

export interface TranscodeSchemaField {
  id: string;
  label: string;
  type: "text" | "textarea" | "select" | "switch";
  default: string | boolean;
  options?: string[];
  /// Encoder-specific option lists for the speed control
  /// (`software` / `nvenc` / `qsv` / `vaapi` / `vaapiHevc`).
  optionGroups?: Record<string, string[]>;
  /// One-line explanation under the control (the web port of 3FUI's field description).
  hint?: string;
  /// Per-option explanations for a select, keyed by option value.
  optionHints?: Record<string, string>;
  /// Fields the overview and the beginner flow read first.
  important?: boolean;
  /// Encoder values the current ffmpeg does not list; the form grays them out.
  unavailable?: string[];
}

export interface TranscodeSchemaSection {
  title: string;
  /// One-line explanation of the whole section.
  hint?: string;
  fields: TranscodeSchemaField[];
}

export interface TranscodeSchema {
  version: number;
  sections: TranscodeSchemaSection[];
}

export interface TranscodePreview {
  input: string;
  output: string;
  args: string[];
  command: string;
}

export type TranscodeJobState =
  | "queued"
  | "running"
  | "paused"
  | "waiting_decision"
  | "stopping"
  | "done"
  | "failed"
  | "cancelled";

export interface TranscodeJob {
  id: string;
  input: string;
  output: string;
  presetId: string;
  presetName: string;
  preset: TranscodePreset;
  /** Non-empty for queue-backed tools (merge / mux / extract); those jobs have no preset of their own. */
  tool?: string;
  state: TranscodeJobState;
  fraction: number;
  message: string;
  speed: number;
  fps: number;
  bitrate: string;
  outTimeMs: number;
  durationMs: number;
  sizeBytes: number;
  frame: number;
  error?: string | null;
  encoder?: string | null;
  fallbackFrom?: string | null;
  fallbackTo?: string | null;
  fallbackAttempted?: boolean;
  decisionDeadline?: number;
  logFile?: string | null;
  createdAt: number;
  startedAt: number;
  endedAt: number;
  command: string[];
  /** Every encoder hop this job went through, newest last. */
  switchRecords?: TranscodeSwitchRecord[];
  /** One entry per ffmpeg invocation (a two-pass preset has two). */
  steps?: TranscodeJobStep[];
  /** Computed server-side. Never derive button availability from `state` in the UI. */
  caps: TranscodeJobCaps;
}

/** What the server allows right now; the UI only renders what is true here. */
export interface TranscodeJobCaps {
  start: boolean;
  pause: boolean;
  resume: boolean;
  stop: boolean;
  remove: boolean;
  reset: boolean;
  reorder: boolean;
}

export interface TranscodeSwitchRecord {
  at: number;
  from: string;
  to: string;
  /** `operator`, `timeout`, `policy` or `queue`. */
  trigger: string;
  reason: string;
  droppedArgs?: string[];
}

export interface TranscodeJobStep {
  name: string;
  state: "pending" | "running" | "done" | "failed" | "cancelled";
  args: string[];
}

export interface TranscodeBatchResult {
  ok: boolean;
  applied: number;
  results: Array<{ id: string; ok: boolean; reason?: string }>;
  queue: TranscodeQueueSnapshot;
}

export interface TranscodePresetSyncResult {
  updated: number;
  failed: number;
  skippedTool: number;
  skippedActive: number;
  skippedTerminal: number;
  queue: TranscodeQueueSnapshot;
}

export interface TranscodeQueueSnapshot {
  runState: "idle" | "running" | "paused";
  maxConcurrent: number;
  maxQueue: number;
  jobs: TranscodeJob[];
}

export interface TranscodeQualityFrame {
  n: number;
  value: number;
}

export interface TranscodeQualityMetric {
  average: number;
  frames: TranscodeQualityFrame[];
}

export interface TranscodeQualityJob {
  id: string;
  reference: string;
  encoded: string;
  metrics: string[];
  state: "running" | "done" | "failed" | "cancelled";
  progress: number;
  message: string;
  error?: string | null;
  createdAt: number;
  endedAt: number;
  results: Record<string, TranscodeQualityMetric>;
}

export interface TranscodeInstallJob {
  id: string;
  kind: "static" | "package" | "compile";
  state: "running" | "done" | "failed" | "cancelled";
  progress: number;
  message: string;
  log: string;
  error?: string | null;
  createdAt: number;
  endedAt: number;
}

/// GET /api/mc/feature：功能开关与内存锁。
export interface McFeature {
  state: "DISABLED" | "ENABLED";
  enabled: boolean;
  locked: boolean;
  lockRemainingSeconds: number;
  keepFeature: boolean | null;
  /** 同意记录（有则回看用；功能关闭后仍保留）。 */
  termsVersion?: number;
  termsAcceptedAt?: string;
  acceptedBy?: string;
  termsTextHash?: string;
  eulaHash?: string;
  eulaRetrievedAt?: string;
  ackMethod?: string;
  /** 这个平台的控制台形态：Linux/macOS 是 PTY，Windows 是日志 + 命令。 */
  console: "pty" | "pipe";
  /** 第二道关：第三方核心是否已开启（只在 enabled=true 时有意义）。 */
  thirdParty?: boolean;
  /** 服务端将用哪个 Java 启动 MC（默认就是面板自己的运行时）。 */
  java: McJavaInfo;
  /** 新实例会落到哪个根目录，以及这个设置来自哪里。 */
  storage: McFeatureStorage;
  /** 每个实例的堆上限范围；来源是 `server.yaml` 的 `mc.max_memory_mb`。 */
  limits?: McFeatureLimits;
}

export interface McFeatureLimits {
  minMemoryMb: number;
  maxMemoryMb: number;
  maxInstances: number;
}

export interface McJavaInfo {
  path: string;
  version: string | null;
  major: number;
  // managed = 面板自己装的 JRE（<data>/minecraft/java/<feature>）；
  // installed = 本机另一份 JDK：所选 MC 版本要求更高时，面板挑满足要求的最低那份。
  source: "panel" | "managed" | "installed" | "configured" | "none";
  ok: boolean;
  /** 面板托管的 JRE 列表（Adoptium，sha256 校验后才落位）。 */
  managed?: { feature: number; path: string }[];
  error?: string;
}

export interface McFeatureStorage {
  root: string;
  source: string;
  writable: boolean;
  legacy: string | null;
}

export interface McEula {
  text: string;
  hash: string;
  fetchedAt: string;
  source: string;
}

export interface McTerms {
  version: number;
  text: string;
  hash: string;
}

/// GET /api/settings/logs：磁盘上留多久 + 界面能看多久 + 时钟可不可信。
export interface LogRetention {
  auditDays: number;
  panelDays: number;
  floorDays: number;
  ceilingDays: number;
  viewDays: number;
  /// 系统时钟可不可信。不可信时清理一个文件都不删（要重启面板才恢复）。
  clock?: {
    trusted: boolean;
    reason?: string | null;
    skewMillis: number;
    checkedAt?: string | null;
  };
  /// 最近一轮清理的结果；`skippedReason` 非空表示那轮什么都没删。
  lastSweep?: {
    deleted: number;
    at?: string | null;
    skippedReason?: string | null;
  };
}

/// GET /api/settings/logs/recent 里的一份日志。
export interface LogFileEntry {
  name: string;
  kind: "audit" | "panel";
  bytes: number;
  modified: string;
  compressed: boolean;
}

export interface McVersion {
  id: string;
  type: string;
}

/** GET /api/mc/cores：一个服务端核心及其可用游戏版本。 */
export interface McCoreInfo {
  id: string;
  label: string;
  thirdParty: boolean;
  versions: string[];
}

/** 一个下载源还剩多少（`-1` = 未知，`0` + `exhausted` = 这一窗口用完了）。 */
export interface McSourceQuota {
  limit: number;
  remaining: number;
  resetAt: number;
  used: number;
  exhausted: boolean;
}

export interface McSourceInfo {
  id: string;
  label: string;
  note: string;
  /** 启用时界面必须显示提供方署名（MSL 的条款要求）。 */
  attribution: boolean;
  cores: string[];
  quota: McSourceQuota;
}

/** GET /api/mc/third-party/sources */
export interface McSourcesSnapshot {
  sources: McSourceInfo[];
  order: Record<string, string[]>;
  custom: Record<string, string>;
  userAgent: string;
}

/** 插件/模组目录里的一个文件。 */
export interface McModFile {
  name: string;
  enabled: boolean;
  bytes: number;
  /** 元数据自称纯客户端：服务端加载会报错或没效果。 */
  clientOnly: boolean;
  /** 从 Modrinth 安装时记下的项目信息（`.panel.json`），手工放的文件没有。 */
  projectId?: string;
  title?: string;
  versionNumber?: string;
}

/** Modrinth 搜索命中。 */
export interface McModHit {
  projectId: string;
  slug: string;
  title: string;
  author: string;
  downloads: number;
  description: string;
}

/** GET /api/mc/java/runtimes：面板托管的 JRE 与 Adoptium 可下载项。 */
export interface McJavaRuntimesSnapshot {
  installed: { feature: number; path: string; vendor?: string; vendorLabel?: string;
    version?: string | null; bytes?: number }[];
  available: { vendor: string; vendorLabel: string; feature: number; version: string; size: number }[];
  /** 每个发行版的目录是否读到了（Foojay 挂了不影响 Temurin）。 */
  vendors?: { id: string; label: string; ok: boolean; error?: string }[];
  recommended: number;
  source: "api" | "unavailable" | "skipped";
  error?: string;
  root: string;
  /** 面板自身 JVM；只读展示，面板不会安装或改动它。 */
  panel: { version: string; vendor: string; home: string };
  /** PATH 上的 java；只读展示。 */
  system: { path: string; version: string | null; major: number; ok: boolean; error?: string };
  /** 把实例钉在某个托管版本上的记录，删除时用来拒绝。 */
  consumers?: { feature: number; id: string; path: string; running: boolean }[];
}

export interface McInstance {
  id: string;
  name: string;
  version: string;
  /** 服务端核心：vanilla / paper / folia / purpur / fabric / forge / neoforge */
  core: string;
  /** 是否是第三方核心（非 Mojang 原版）；界面据此标注「官方 / 第三方」 */
  thirdParty?: boolean;
  port: number;
  rconPort: number;
  memoryMb: number;
  /** 高级设置里选的 java 可执行文件；空/缺省 = 按版本要求自动挑。 */
  javaPath?: string | null;
  levelName: string;
  seed?: string;
  createdAt: number;
  state: "STOPPED" | "STARTING" | "RUNNING" | "STOPPING" | "FAILED";
  error?: string;
  consoleAvailable: boolean;
  /** 服务器目录（存档、server.jar、日志都在这儿） */
  dir: string;
  /** 在专用目录（容器里是 /home/hdsl/mc）还是旧的 data 目录 */
  storage: "external" | "data";
}

/** GET /api/mc/storage：两个可选存储位置与默认值 */
export interface McStorage {
  external: { path: string; writable: boolean };
  data: { path: string; writable: boolean } | null;
  default: "external" | "data";
}

/// GET /api/mc/instances/{id}/java：这台实例会用的 Java 与可选的运行时。
export interface McJavaProbe {
  path: string;
  version: string | null;
  major: number;
  source: "panel" | "managed" | "installed" | "configured" | "none" | string;
  ok: boolean;
  error?: string;
}

export interface McInstanceJava {
  selected: string | null;
  effective: McJavaProbe;
  options: McJavaProbe[];
}

export interface McBackup {
  name: string;
  createdAt: string;
  bytes: number;
}

/// GET /api/node 的快照：系统 PATH 探测 + 面板托管运行时列表
export interface NodeStatus {
  system: {
    node: string;
    nodeVersion: string;
    npm?: string;
    npmVersion?: string;
    pnpm?: string;
    pnpmVersion?: string;
  } | null;
  managed: { version: string; suitable: boolean; node: string; pnpm: boolean }[];
  requirement: string;
  /** 全局默认选择：auto / 大版本 / 精确版本 */
  default: string;
  /** 每个已安装大版本一张卡 */
  channels: NodeChannel[];
  /** 每个已安装的精确版本 */
  runtimes: NodeRuntimeInfo[];
  /** 谁在用哪个运行时 */
  consumers: NodeConsumer[];
}

export interface NodeChannel {
  name: string;
  major: number;
  lts: boolean;
  /** 通道指针（上次「更新」记录到的版本），或 null */
  tracked: string | null;
  resolved: string | null;
  installed: boolean;
  disabled: boolean;
  healthy: boolean;
  /** 是否在面板支持的 Node 范围内（18/20 低于要求，装得上但 dsh 用不了） */
  supported: boolean;
  consumers: string[];
}

export interface NodeRuntimeInfo {
  version: string;
  path: string;
  healthy: boolean;
  disabled: boolean;
  pnpm: boolean;
  sizeMb: number;
  lastUsedAt: string | null;
  usedBy: string[];
  sha256: string | null;
  verified: boolean;
  supported: boolean;
}

export interface NodeConsumer {
  kind: "instance" | "brand-instance" | "zcode-build" | "task";
  brand: string | null;
  id: string;
  selector: string;
  resolved: string | null;
  source: string;
  compatible: boolean;
  running: boolean;
  /** 消费者自己声明的 engines.node（品牌包），或 null */
  requirement: string | null;
  /** 上次成功启动的精确版本，或 null */
  lastGood: string | null;
}

/** 出站 TLS 的策略：strict 是默认（只认 Java 的 cacerts）；
 *  accept_unverified 是操作者确认过的窗口（到 acceptedUntil 为止），期间无法校验的
 *  证书会被接受并在日志里记下。acknowledge 是必须逐字回传的那句话。 */
export interface NetworkTrust {
  mode: "strict" | "accept_unverified";
  acceptedUntil: string | null;
  acknowledge: string;
  maxMinutes: number;
  lastTrouble?: { host: string; reason: string; at: string } | null;
  explanation?: string;
}

/** 打开终端的结果：POSIX 返回 ticket + ws，Windows 返回 desktop。 */
export interface TerminalOpen {
  mode: "pty" | "desktop" | "pipe";
  instance: string;
  brand?: string;
  ticket?: string;
  ws?: string;
}
