import { useSyncExternalStore } from "react";
import { api, ApiError, isNotImplemented, setUnauthorizedHandler } from "./api";
import { ws } from "./ws";
import { consumeLaunchPopup } from "./launch";
import { MAX_LOG_LINES, LOG_TAIL } from "./constants";
import type { AppState, ExternalInstance, Instance, LogLine, Task, TaskRec, Toast, WsEvent } from "./types";
import { errMsg, isInstanceRunning, isTerminalTaskState, isWaitingApproval, normState } from "./utils";

// ---------------------------------------------------------------------------
// 外部 store：WS 事件在 React 组件树之外到达，直接 setState + 通知订阅者。
// 组件通过 useAppState()（useSyncExternalStore）订阅。
// ---------------------------------------------------------------------------

const initialState: AppState = {
  username: null,
  authChecked: false,
  authError: null,
  sidebarOpen: false,
  setupRequired: false,
  instances: [],
  instancesLoaded: false,
  instancesLoading: false,
  versions: null,
  versionsLoading: false,
  versionsError: null,
  tasks: {},
  logs: {},
  crash: {},
  lastUptime: {},
  accounts: null,
  accountsLoading: false,
  accountsError: null,
  modelProviders: null,
  modelProvidersLoading: false,
  modelProvidersError: null,
  modelProvidersAt: null,
  modelProvidersSource: null,
  zcodeDist: null,
  zcodeLoading: false,
  external: [],
  toasts: [],
};

let state: AppState = initialState;
const listeners = new Set<() => void>();

function emit(): void {
  for (const l of [...listeners]) l();
}

export function setState(updater: (s: AppState) => AppState): void {
  state = updater(state);
  emit();
}

function getState(): AppState {
  return state;
}

export { getState };

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function useAppState(): AppState {
  return useSyncExternalStore(subscribe, getState);
}

// ---------------------------------------------------------------------------
// Toast
// ---------------------------------------------------------------------------

let toastSeq = 1;

export function toast(kind: Toast["kind"], text: string): void {
  const id = toastSeq++;
  setState((s) => ({ ...s, toasts: [...s.toasts, { id, kind, text }] }));
  window.setTimeout(() => {
    setState((s) => ({ ...s, toasts: s.toasts.filter((t) => t.id !== id) }));
  }, 4500);
}

export function dismissToast(id: number): void {
  setState((s) => ({ ...s, toasts: s.toasts.filter((t) => t.id !== id) }));
}

// ---------------------------------------------------------------------------
// 移动版式：主导航抽屉的开合（桌面版式下侧栏常驻，这个状态不起作用）
// ---------------------------------------------------------------------------

export function toggleSidebar(): void {
  setState((s) => ({ ...s, sidebarOpen: !s.sidebarOpen }));
}

export function closeSidebar(): void {
  setState((s) => (s.sidebarOpen ? { ...s, sidebarOpen: false } : s));
}

// ---------------------------------------------------------------------------
// 认证
// ---------------------------------------------------------------------------

export async function bootstrap(): Promise<void> {
  setState((s) => ({ ...s, authChecked: false, authError: null }));
  try {
    const who = await api.whoami();
    setState((s) => ({ ...s, username: who.username, authChecked: true, setupRequired: false }));
    startWsAndSync();
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      // No session: is there even a user? The status endpoint decides
      // between the first-run setup page and the login page.
      try {
        const st = await api.authStatus();
        setState((s) => ({ ...s, username: null, authChecked: true, setupRequired: st.setupRequired }));
      } catch (se) {
        setState((s) => ({
          ...s,
          authChecked: true,
          authError: se instanceof Error ? se.message : "无法连接服务器",
        }));
      }
    } else {
      setState((s) => ({
        ...s,
        authChecked: true,
        authError: e instanceof Error ? e.message : "无法连接服务器",
      }));
    }
  }
}

function startWsAndSync(): void {
  ws.connect();
  ws.addTopics(["instances", "tasks"]);
  void refreshInstances(false);
  void loadVersions();
}

export async function login(username: string, password: string): Promise<void> {
  const r = await api.login(username, password);
  setState((s) => ({ ...s, username: r.username, authChecked: true, authError: null, setupRequired: false }));
  startWsAndSync();
}

/** 首次启动引导：创建第一个用户，服务端直接下发会话，进入主界面。 */
export async function setup(username: string, password: string): Promise<void> {
  const r = await api.setup(username, password);
  setState((s) => ({ ...s, username: r.username, authChecked: true, authError: null, setupRequired: false }));
  startWsAndSync();
}

export async function logout(): Promise<void> {
  try {
    await api.logout();
  } catch {
    // 即使登出接口失败也继续本地清理
  }
  ws.close();
  setState(() => ({ ...initialState, authChecked: true, username: null }));
}

// 任意 API 401（会话过期）→ 回到登录页。能拿到 401 说明服务端已有用户
// （全新未初始化的服务端走 bootstrap 的 status 分支），因此这里固定 false。
setUnauthorizedHandler(() => {
  ws.close();
  setState((s) => ({
    ...s,
    username: null,
    sidebarOpen: false,
    setupRequired: false,
    instances: [],
    instancesLoaded: false,
    tasks: {},
    logs: {},
    crash: {},
    lastUptime: {},
    accounts: null,
    accountsError: null,
    modelProviders: null,
    modelProvidersError: null,
    zcodeDist: null,
    external: [],
  }));
});

// ---------------------------------------------------------------------------
// 数据加载
// ---------------------------------------------------------------------------

export async function refreshInstances(showLoading = false): Promise<void> {
  if (showLoading) setState((s) => ({ ...s, instancesLoading: true }));
  try {
    const r = await api.instances();
    const now = Date.now();
    setState((s) => {
      const lastUptime = { ...s.lastUptime };
      for (const i of r.instances) {
        if (isInstanceRunning(i.state) && typeof i.uptimeSec === "number") {
          lastUptime[i.id] = { sec: i.uptimeSec, at: now };
        }
      }
      return { ...s, instances: r.instances, instancesLoaded: true, instancesLoading: false, lastUptime };
    });
  } catch (e) {
    setState((s) => ({ ...s, instancesLoading: false }));
    toast("error", `获取实例列表失败：${errMsg(e)}`);
  }
}

let refreshTimer: ReturnType<typeof setTimeout> | undefined;

/// 实验性 ZCode 品类的发行包检测。实例状态不在这一层——统一归
/// [refreshExternal] 的 `external`（唯一跨品牌实例状态源），这里只留发行包
/// 元数据。与 dsh 实例分开拉取，一个失败不影响另一个。
export async function refreshZcode(showLoading = false): Promise<void> {
  if (showLoading) setState((s) => ({ ...s, zcodeLoading: true }));
  try {
    const dist = await api.zcodeDist();
    setState((s) => ({ ...s, zcodeDist: dist, zcodeLoading: false }));
  } catch (e) {
    setState((s) => ({ ...s, zcodeLoading: false }));
    toast("error", `获取 ZCode 状态失败：${errMsg(e)}`);
  }
}

/// 跨品牌实例（各品牌实例 + zcode 实验实例）合并进 external，
/// 主页启动面板的实例选择菜单与主按钮动作用它。单个品牌失败不影响其它品牌；
/// 静默失败（不弹 toast）——主页面 5 秒轮询一次，报错会刷爆通知。
export async function refreshExternal(): Promise<void> {
  const [kimi, opencode, claudeWeb, octop, hermes, piWeb, qwen, mimo, minimax, zcode] = await Promise.all([
    api.brandInstances("kimi").then((r) => r.instances).catch(() => null),
    api.brandInstances("opencode").then((r) => r.instances).catch(() => null),
    api.brandInstances("claude-web").then((r) => r.instances).catch(() => null),
    api.brandInstances("octop").then((r) => r.instances).catch(() => null),
    api.brandInstances("hermes").then((r) => r.instances).catch(() => null),
    api.brandInstances("pi-web").then((r) => r.instances).catch(() => null),
    api.brandInstances("qwen").then((r) => r.instances).catch(() => null),
    api.brandInstances("mimo").then((r) => r.instances).catch(() => null),
    api.brandInstances("minimax").then((r) => r.instances).catch(() => null),
    api.zcodeInstances()
      .then((r) => ({ instances: r.instances, workspacesRoot: r.workspacesRoot }))
      .catch(() => null),
  ]);
  const external: ExternalInstance[] = [];
  for (const i of kimi ?? []) {
    external.push({ brand: "kimi", id: i.id, name: i.name, state: i.state, url: i.url, port: i.lastPort, version: i.version, error: i.error, disabled: i.disabled });
  }
  for (const i of opencode ?? []) {
    external.push({ brand: "opencode", id: i.id, name: i.name, state: i.state, url: i.url, port: i.lastPort, version: i.version, error: i.error, disabled: i.disabled });
  }
  for (const i of claudeWeb ?? []) {
    external.push({ brand: "claude-web", id: i.id, name: i.name, state: i.state, url: i.url, port: i.lastPort, version: i.version, error: i.error, disabled: i.disabled });
  }
  for (const i of octop ?? []) {
    external.push({ brand: "octop", id: i.id, name: i.name, state: i.state, url: i.url, port: i.lastPort, version: i.version, error: i.error, disabled: i.disabled });
  }
  for (const i of hermes ?? []) {
    // terminalOpen 必须带上：Hermes 的网页与终端互斥，行内的启动按钮靠它决定
    // 要不要先弹窗（docs/brands.md「终端与网页互斥」）。
    external.push({ brand: "hermes", id: i.id, name: i.name, state: i.state, url: i.url, port: i.lastPort, version: i.version, error: i.error, disabled: i.disabled, terminalOpen: i.terminalOpen });
  }
  for (const i of piWeb ?? []) {
    external.push({
      brand: "pi-web", id: i.id, name: i.name, state: i.state, url: i.url, port: i.lastPort,
      version: i.version, error: i.error, disabled: i.disabled,
      webEnabled: i.webEnabled, versionLocked: i.versionLocked,
      releaseWebAvailable: i.releaseWebAvailable, releasePtyAvailable: i.releasePtyAvailable,
      pairedSdk: i.pairedSdk,
    });
  }
  for (const [brand, list] of [
    ["qwen", qwen],
    ["mimo", mimo],
    ["minimax", minimax],
  ] as const) {
    for (const i of list ?? []) {
      external.push({
        brand, id: i.id, name: i.name, state: i.state, version: i.version,
        error: i.error, disabled: i.disabled, terminalOpen: i.terminalOpen,
        kind: i.kind ?? "tui",
      });
    }
  }
  for (const i of zcode?.instances ?? []) {
    external.push({
      brand: "zcode", id: i.id, name: i.name, state: i.state, port: i.lastPort,
      error: i.error ?? undefined, hasApiKey: i.hasApiKey, baseUrl: i.baseUrl ?? undefined,
      workspacePath: i.workspacePath, version: i.version ?? undefined,
      minVersion: i.minVersion, minVersionAt: i.minVersionAt,
      credentialFormat: i.credentialFormat,
      requiresCredentialSecret: i.requiresCredentialSecret,
      keyFingerprint: i.keyFingerprint,
    });
  }
  // 拿不到时保留上一次的根：一次失败不该把界面上的路径提示变成占位符
  setState((s) => ({ ...s, external, zcodeWorkspacesRoot: zcode?.workspacesRoot ?? s.zcodeWorkspacesRoot }));
}

/** WS 事件驱动的实例列表刷新：合并短时间内的多次事件，避免请求风暴。 */
export function scheduleInstanceRefresh(delay = 400): void {
  if (refreshTimer) clearTimeout(refreshTimer);
  refreshTimer = setTimeout(() => {
    refreshTimer = undefined;
    void refreshInstances(false);
  }, delay);
}

export async function loadVersions(force = false): Promise<void> {
  const cur = getState();
  if (cur.versions && !force) return;
  if (cur.versionsLoading) return;
  setState((s) => ({ ...s, versionsLoading: true, versionsError: null }));
  try {
    const r = await api.versions();
    setState((s) => ({ ...s, versions: r.versions, versionsLoading: false }));
  } catch (e) {
    setState((s) => ({ ...s, versionsLoading: false, versionsError: errMsg(e) }));
  }
}

/** 拉取任务列表合并进 store（详情页挂载、安装提交后调用）。 */
export async function seedTasks(): Promise<void> {
  try {
    const tasks = await api.tasks();
    if (!tasks.length) return;
    setState((s) => {
      const merged = { ...s.tasks };
      for (const t of tasks) {
        merged[t.id] = { ...t, seenAt: merged[t.id]?.seenAt ?? Date.now() };
      }
      return { ...s, tasks: merged };
    });
  } catch {
    // 静默失败：WS 任务事件仍会陆续到达
  }
}

// ---------------------------------------------------------------------------
// 账户 / 模型目录（Phase 3）
// ---------------------------------------------------------------------------

/**
 * 加载账户列表。404/405（后端尚未实现）不弹 toast：
 * accounts 保持 null 且 accountsError 置为提示文案，由页面自行降级展示。
 */
export async function loadAccounts(force = false): Promise<void> {
  const cur = getState();
  if (cur.accounts && !force) return;
  if (cur.accountsLoading) return;
  setState((s) => ({ ...s, accountsLoading: true, accountsError: null }));
  try {
    const r = await api.accounts();
    setState((s) => ({ ...s, accounts: r.accounts, accountsLoading: false }));
  } catch (e) {
    const msg = isNotImplemented(e) ? "后端尚未支持账户管理" : errMsg(e);
    setState((s) => ({ ...s, accountsLoading: false, accountsError: msg }));
    if (!isNotImplemented(e)) toast("error", `获取账户列表失败：${errMsg(e)}`);
  }
}

export async function loadModelProviders(force = false): Promise<void> {
  const cur = getState();
  if (cur.modelProviders && !force) return;
  if (cur.modelProvidersLoading) return;
  setState((s) => ({ ...s, modelProvidersLoading: true, modelProvidersError: null }));
  try {
    const r = await api.modelProviders(force);
    setState((s) => ({
      ...s,
      modelProviders: r.providers,
      modelProvidersLoading: false,
      modelProvidersAt: r.fetchedAt ?? Date.now(),
      modelProvidersSource: r.source,
    }));
  } catch (e) {
    const msg = isNotImplemented(e) ? "后端尚未支持模型目录" : errMsg(e);
    setState((s) => ({ ...s, modelProvidersLoading: false, modelProvidersError: msg }));
  }
}

/** 拉取历史日志作为实时流的基底（仅当本地还没有该实例的日志时）。 */
export async function seedLogs(instanceId: string): Promise<void> {
  try {
    const r = await api.logs(instanceId, LOG_TAIL);
    setState((s) => {
      if ((s.logs[instanceId]?.length ?? 0) > 0) return s;
      const lines: LogLine[] = r.lines.map((l) => ({
        level: l.level,
        time: l.time ?? "",
        text: l.text,
      }));
      return { ...s, logs: { ...s.logs, [instanceId]: lines.slice(-MAX_LOG_LINES) } };
    });
  } catch {
    // 静默失败：无历史日志也能正常展示实时流
  }
}

export function clearLogs(instanceId: string): void {
  setState((s) => ({ ...s, logs: { ...s.logs, [instanceId]: [] } }));
}

// ---------------------------------------------------------------------------
// 乐观更新
// ---------------------------------------------------------------------------

export function patchInstanceLocal(id: string, patch: Partial<Instance>): void {
  setState((s) => ({
    ...s,
    instances: s.instances.map((i) => (i.id === id ? { ...i, ...patch } : i)),
  }));
}

export function upsertInstance(inst: Instance): void {
  setState((s) => {
    const idx = s.instances.findIndex((i) => i.id === inst.id);
    if (idx < 0) return { ...s, instances: [...s.instances, inst] };
    const next = s.instances.slice();
    next[idx] = { ...s.instances[idx], ...inst };
    return { ...s, instances: next };
  });
}

// ---------------------------------------------------------------------------
// 任务匹配启发式
// ---------------------------------------------------------------------------

/**
 * 找到属于该实例的"当前任务"。
 *
 * 归属按任务自带的 instance 字段精确匹配（WS task 事件与 REST /api/tasks
 * 都带它）。旧实现按 `kind.includes(id)` 猜，而 kind 只是 "install"、
 * "pack-install" 这类名字，永远匹配不上，于是退化成"最近活跃的任务"——
 * 并发安装时进度和取消会串到别的实例上。
 */
export function findTaskForInstance(instanceId: string): TaskRec | null {
  const all = Object.values(getState().tasks);
  if (!all.length) return null;
  const mine = all.filter((t) => t.instance === instanceId);
  const pool = mine.length > 0 ? mine : all.filter((t) => t.instance == null);
  if (!pool.length) return null;
  const active = pool.filter((t) => !isTerminalTaskState(t.state));
  const candidates = (active.length > 0 ? active : pool).slice().sort((a, b) => b.seenAt - a.seenAt);
  return candidates[0] ?? null;
}

// ---------------------------------------------------------------------------
// WS 事件 → 状态
// ---------------------------------------------------------------------------

async function handleReady(instanceId: string, url?: string): Promise<void> {
  const popup = consumeLaunchPopup(instanceId);
  if (popup === undefined) return; // 不是本标签页发起的启动
  let target = url;
  if (!target) {
    try {
      target = (await api.open(instanceId)).url;
    } catch {
      // 拿不到 url：运行中视图的 [打开 dsh] 按钮兜底
    }
  }
  toast("success", "dsh 已就绪");
  if (popup && !popup.closed && target) {
    popup.location.href = target;
  }
}

export function applyWsEvent(evt: WsEvent): void {
  switch (evt.type) {
    case "instance-state": {
      const id = evt.instance;
      setState((s) => {
        let next: AppState = {
          ...s,
          instances: s.instances.map((i) =>
            i.id === id ? { ...i, state: evt.state, url: evt.url ?? i.url } : i,
          ),
        };
        if (normState(evt.state) === "FAILED") {
          next = {
            ...next,
            crash: { ...next.crash, [id]: { exitCode: evt.exitCode, crashTail: evt.crashTail } },
          };
        }
        return next;
      });
      if (isInstanceRunning(evt.state)) void handleReady(id, evt.url);
      scheduleInstanceRefresh(500);
      break;
    }
    case "log": {
      // 丢弃已取消订阅实例的日志（后端可能不支持 unsubscribe）
      if (!ws.hasTopic(`instance:${evt.instance}`)) break;
      setState((s) => {
        const cur = s.logs[evt.instance] ?? [];
        const line: LogLine = {
          level: evt.level,
          time: new Date().toISOString(),
          text: evt.line,
        };
        return { ...s, logs: { ...s.logs, [evt.instance]: [...cur, line].slice(-MAX_LOG_LINES) } };
      });
      break;
    }
    case "task": {
      setState((s) => {
        const prev = s.tasks[evt.taskId];
        const waiting = isWaitingApproval(evt.state);
        const rec: TaskRec = {
          id: evt.taskId,
          kind: prev?.kind ?? "unknown",
          state: evt.state,
          message: evt.message ?? prev?.message,
          fraction: typeof evt.fraction === "number" ? evt.fraction : prev?.fraction,
          // 失败原因：终态事件带着 error，必须落库，否则安装失败只剩「卡在安装中」
          error: evt.error ?? prev?.error,
          // 任务归属的实例：安装进度按它归属（WS 与 REST 都带）
          instance: evt.instance ?? prev?.instance,
          // 审批信息：等待审批期间保留，进入其它状态后清除
          approval: evt.approval ?? (waiting ? prev?.approval : undefined),
          // 完成结果（整合包 instanceId / 导出 filename）：WS 未带时保留 REST 打底值
          result: evt.result ?? prev?.result,
          seenAt: Date.now(),
        };
        return { ...s, tasks: { ...s.tasks, [evt.taskId]: rec } };
      });
      // WS task 事件可能不带 approval 详情，拉一次任务列表补齐（含 build-script keys）
      if (isWaitingApproval(evt.state)) void seedTasks();
      if (isTerminalTaskState(evt.state)) {
        scheduleInstanceRefresh(300);
        // WS 终态事件可能不带 result，拉任务列表补齐（安装整合包/导出等场景）
        void seedTasks();
      }
      break;
    }
    case "instance-created":
    case "instance-deleted":
    case "instance-updated":
      scheduleInstanceRefresh(250);
      break;
    case "accounts-changed":
      void loadAccounts(true);
      break;
    default:
      break;
  }
}

// 模块加载即注册（唯一处理器）
ws.on((evt) => applyWsEvent(evt));

export type { Task };
