/** 状态归一化：后端枚举大小写未知，统一转大写比较 */
export function normState(s: string | null | undefined): string {
  return (s ?? "").toUpperCase();
}

export function isInstanceRunning(state: string | null | undefined): boolean {
  return normState(state) === "RUNNING";
}

/// MC 实例的五种状态，翻成面板上的中文（接口里仍是 `STOPPED` 这类枚举）。
const MC_STATE_LABEL: Record<string, string> = {
  STOPPED: "已停止",
  STARTING: "启动中",
  RUNNING: "运行中",
  STOPPING: "停止中",
  FAILED: "启动失败",
};

export function mcStateLabel(state: string | null | undefined): string {
  return MC_STATE_LABEL[normState(state)] ?? (state ?? "");
}

/// 服务端核心显示名：列表与详情页的徽章用，未知值原样返回（不吞错）。
const MC_CORE_LABEL: Record<string, string> = {
  vanilla: "原版",
  paper: "Paper",
  folia: "Folia",
  purpur: "Purpur",
  fabric: "Fabric",
  forge: "Forge",
  neoforge: "NeoForge",
};

export function mcCoreLabel(core: string | null | undefined): string {
  return MC_CORE_LABEL[(core ?? "").toLowerCase()] ?? (core ?? "");
}

/// 第三方核心说明：创建向导与实例页共用一段话，避免两处口径漂移。
export function mcCoreNote(core: string | null | undefined): string {
  switch ((core ?? "").toLowerCase()) {
    case "paper":
    case "folia":
    case "purpur":
    case "fabric":
      return "服务端由上游发布，面板下载并校验上游哈希；装好后可在「插件/模组」页签装插件或模组。";
    case "forge":
    case "neoforge":
      return "需要运行上游安装器（数分钟，libraries 会写进实例目录，约 100–300 MB）；装好后可在「插件/模组」页签装模组。";
    default:
      return "服务端由 Mojang 官方下载并校验；原版不加载插件或模组。";
  }
}

const TERMINAL_TASK_STATES = new Set([
  "DONE",
  "SUCCESS",
  "SUCCEEDED",
  "COMPLETED",
  "FAILED",
  "ERROR",
  "CANCELLED",
  "CANCELED",
]);

export function isTerminalTaskState(state: string): boolean {
  return TERMINAL_TASK_STATES.has((state ?? "").toUpperCase());
}

const SUCCESS_TASK_STATES = new Set(["DONE", "SUCCESS", "SUCCEEDED", "COMPLETED"]);

/** 任务终态中的"成功"类（用于读取 result / 触发完成动作） */
export function isSuccessTaskState(state: string): boolean {
  return SUCCESS_TASK_STATES.has((state ?? "").toUpperCase());
}

/** 任务等待审批（build-scripts），属非终态 */
export function isWaitingApproval(state: string | null | undefined): boolean {
  return normState(state) === "WAITING_APPROVAL";
}

export function clamp01(n: number): number {
  return Math.min(1, Math.max(0, n));
}

function pad2(n: number): string {
  return String(n).padStart(2, "0");
}

/** "YYYY-MM-DD HH:mm:ss"；无法解析时原样返回 */
export function formatDateTime(t: string | number | null | undefined): string {
  if (t == null) return "—";
  const d = typeof t === "number" ? new Date(t) : new Date(t);
  if (Number.isNaN(d.getTime())) return String(t);
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;
}

/** "HH:mm:ss"；用于日志行时间 */
export function formatClock(t: string | null | undefined): string {
  if (!t) return "";
  const d = typeof t === "number" ? new Date(t) : new Date(t);
  if (Number.isNaN(d.getTime())) return t;
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;
}

/** 秒数 → "1天2小时3分" / "2小时3分" / "3分4秒" / "5秒" */
export function formatUptime(sec: number): string {
  const s = Math.max(0, Math.floor(sec));
  const d = Math.floor(s / 86400);
  const h = Math.floor((s % 86400) / 3600);
  const m = Math.floor((s % 3600) / 60);
  const r = s % 60;
  if (d > 0) return `${d}天${h}小时${m}分`;
  if (h > 0) return `${h}小时${m}分`;
  if (m > 0) return `${m}分${r}秒`;
  return `${r}秒`;
}

const CHANNEL_LABELS: Record<string, string> = {
  stable: "稳定版",
  release: "正式版",
  latest: "最新版",
  beta: "测试版",
  preview: "预览版",
  prerelease: "预发布",
  snapshot: "快照",
};

export function channelLabel(ch: string): string {
  return CHANNEL_LABELS[(ch ?? "").toLowerCase()] ?? ch;
}

/** 从实例 url（如 http://127.0.0.1:3100/?token=…）解析实际端口 */
export function parsePortFromUrl(url: string | null | undefined): number | null {
  if (!url) return null;
  const m = /(?:^|\/\/)[^/:]+:(\d+)/.exec(url);
  return m ? Number(m[1]) : null;
}

/** 把后端错误码翻译成用户能照着做的一句话；未知错误原样返回。 */
export function errMsg(e: unknown): string {
  const raw = e instanceof Error ? e.message : "操作失败";
  // 这几种不是"操作失败"，是"先去做另一件事"。不翻译的话，toast 里只剩一串英文码。
  if (raw.includes("tls_required")) {
    return "面板不在 HTTPS 下，开发者模式不可用：请在「设置 → HTTPS 证书」里启用后重试";
  }
  if (raw.includes("insecure_transport")) {
    return "终端只在 HTTPS 下可用：请在「设置 → HTTPS 证书」里启用后重试";
  }
  if (raw.includes("emergency_stop")) {
    return "面板处于紧急停机：实例网页、终端与实例启动都被冻结，冷却结束或重启面板后才能恢复";
  }
  if (raw.includes("estop_cooldown")) {
    return "紧急停机的冷却期还没结束：到点前谁都不能解除（重启面板进程或容器会清空）";
  }
  if (raw.includes("web_shell_disabled")) {
    return "Web Shell 已在面板里关闭：请在「设置 → 开发者模式」里打开后再连";
  }
  if (raw.includes("developer_password_not_set")) {
    return "还没有设置开发者密码：请在「设置 → 开发者模式」里设置后重试";
  }
  return raw;
}

/** "YYYY-MM-DD"；无法解析时原样返回 */
export function formatDate(t: string | number | null | undefined): string {
  if (t == null) return "—";
  const d = new Date(t);
  if (Number.isNaN(d.getTime())) return String(t);
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

/** 大数字缩写：1234 → "1.2k"，12 → "12" */
export function formatCount(n: number | null | undefined): string {
  if (n == null || !Number.isFinite(n)) return "—";
  if (n >= 1000) {
    const v = n / 1000;
    return `${v >= 100 ? Math.round(v) : v.toFixed(1).replace(/\.0$/, "")}k`;
  }
  return String(n);
}

/** 文件大小：1024 → "1.0 KB" */
export function formatBytes(n: number): string {
  if (!Number.isFinite(n) || n < 0) return "—";
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(2)} MB`;
}

/** 下载源地址里一律拒绝的符号：引号/反引号/反斜杠会破坏配置文件与命令拼接，
 *  # $ 是模板注入面，; | & 是 shell 元字符，@ 会引入 userinfo。 */
const REGISTRY_BAD_CHARS = /["'`\\#$;|&@]/;

/**
 * 下载源地址校验：合法返回 null，否则返回中文说明。
 * 服务端的 NpmRegistry 才是权威（同一套规则再校验一次），这里只是提前拦下明显非法的输入。
 */
export function registryError(value: string): string | null {
  const v = value.trim().replace(/\/+$/, "");
  if (!v) return "请填写下载源地址";
  if (v.length > 200) return "地址过长，最多 200 个字符";
  if (/[\u0000-\u001f\u007f]/.test(v)) return "地址里不能有控制字符或换行";
  if (/\s/.test(v)) return "地址里不能有空格";
  if (REGISTRY_BAD_CHARS.test(v)) return "地址里不能有引号、反斜杠或 # $ ; | & @ 这些符号";

  let u: URL;
  try {
    u = new URL(v);
  } catch {
    return "请填写合法的 http(s) 地址";
  }
  if (u.protocol !== "http:" && u.protocol !== "https:") return "只支持 http:// 或 https:// 地址";
  if (!u.hostname) return "请填写合法的 http(s) 地址";
  if (u.username || u.password) return "地址里不能带用户名或密码";
  if (u.search || u.hash) return "地址里不能带查询参数或 # 片段";
  return null;
}
