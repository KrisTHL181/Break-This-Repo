import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { api, ApiError, isNotImplemented } from "../api";
import { I18N } from "../i18n";
import { Package2Icon, RefreshIcon } from "./icons";
import { seedTasks, toast, useAppState } from "../store";
import type { Instance, TuiPluginItem, TuiPluginState } from "../types";
import { errMsg, isTerminalTaskState, isWaitingApproval, normState } from "../utils";

/** 任务状态标签（与 InstancePluginsPanel 同款：这两处显示的是同一批 plugin 任务） */
function taskStateLabel(state: string): string {
  if (isWaitingApproval(state)) return "等待审批";
  const s = normState(state);
  if (s === "FAILED" || s === "ERROR") return "失败";
  if (s === "CANCELLED" || s === "CANCELED") return "已取消";
  if (isTerminalTaskState(s)) return "已完成";
  return "进行中";
}

function taskStateClass(state: string): string {
  if (isWaitingApproval(state)) return "warn";
  const s = normState(state);
  if (s === "FAILED" || s === "ERROR" || s === "CANCELLED" || s === "CANCELED") return "failed";
  if (isTerminalTaskState(s)) return "running";
  return "plain";
}

/**
 * 实例「安装」页的 TUI 终端插件区段。
 *
 * dsh 自身没有终端界面（DshSurface 只有 WEB/HEADLESS/ACP/SDK），终端完全由社区插件提供，
 * 而每个插件必须装进**它自己的 profile**：TUI 插件与 dsh-web-app 都是 app 插件，各按自己的
 * 语法解析同一份 argv，同 profile 装在一起会让实例起不来（社区实测 `unknown option '--no-open'`）。
 * 所以面板这里只做三件事：用 npm 安装、锁定版本、把包挂到该插件自己的 profile ——
 * **不修改 dsh 任何源码**，也不往实例的 web profile 里塞。清单上方先讲清免责声明与这一条约束，
 * 再把每条插件实际安装的包名/版本/来源仓库/许可证/profile 摆出来，让人看得见自己装了什么、也能随时卸掉。
 */
export function TuiPluginSection({ instance }: { instance: Instance }) {
  const s = useAppState();
  const id = instance.id;
  const installed = normState(instance.state) !== "NOT_INSTALLED";

  const [catalog, setCatalog] = useState<TuiPluginItem[] | null>(null);
  const [states, setStates] = useState<TuiPluginState[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState<string | null>(null);
  /** 本区段提交的任务 id，到达终态后刷新已装状态 */
  const [tracked, setTracked] = useState<string[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // 清单是全局常量，状态按实例的**每个插件自己的 profile** 读——实例的 web profile 里
      // 本来就看不到它们。
      const [list, mine] = await Promise.all([api.tuiPlugins(), api.instanceTuiPlugins(id)]);
      setCatalog(list.plugins ?? []);
      setStates(mine.plugins ?? []);
    } catch (e) {
      setError(isNotImplemented(e) ? I18N["dsh.tui.load_failed"] : errMsg(e));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    if (!installed) return;
    void load();
    void seedTasks();
  }, [id, installed, load]);

  const stateOf = useMemo(() => {
    const byPackage = new Map<string, TuiPluginState>();
    for (const state of states) byPackage.set(state.packageName, state);
    return byPackage;
  }, [states]);

  const tasks = useMemo(() => {
    const mine = Object.values(s.tasks).filter(
      (t) =>
        tracked.includes(t.id) ||
        (t.instance === id && t.kind.toLowerCase().includes("plugin")),
    );
    return mine.sort((a, b) => b.seenAt - a.seenAt).slice(0, 5);
  }, [s.tasks, tracked, id]);

  // 安装/卸载是任务，装的到底是什么要等任务结束才算数——终态到达时重读一次
  const prevStates = useRef<Record<string, string>>({});
  useEffect(() => {
    let finished = false;
    for (const t of tasks) {
      const prev = prevStates.current[t.id];
      if (prev && !isTerminalTaskState(prev) && isTerminalTaskState(t.state)) finished = true;
      prevStates.current[t.id] = t.state;
    }
    if (finished && installed) void load();
  }, [tasks, installed, load]);

  const submit = async (plugin: TuiPluginItem, install: boolean) => {
    setBusy(plugin.packageName);
    try {
      const r = install
        ? await api.installTuiPlugin(id, plugin.packageName)
        : await api.removeTuiPlugin(id, plugin.packageName);
      setTracked((t) => [r.taskId, ...t.filter((x) => x !== r.taskId)].slice(0, 9));
      void seedTasks();
      toast("info", I18N["dsh.tui.submitted"].replace("%s", r.taskId));
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) toast("error", I18N["dsh.tui.needs_install"]);
      else if (isNotImplemented(e)) toast("error", I18N["dsh.tui.load_failed"]);
      else toast("error", `${install ? I18N["dsh.tui.install"] : I18N["dsh.tui.remove"]}失败：${errMsg(e)}`);
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="card" style={{ display: "flex", flexDirection: "column", gap: 8 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
        <Package2Icon size={18} />
        <span style={{ fontSize: 14, fontWeight: 600 }}>{I18N["dsh.tui.title"]}</span>
        <span style={{ flex: 1 }} />
        <button
          className="icon-btn on-variant ripple-host"
          title={I18N["button.refresh"]}
          disabled={!installed || loading}
          onClick={() => void load()}
        >
          <RefreshIcon size={16} />
        </button>
      </div>

      {/* 免责声明 + 硬约束：装的是别人的代码，而且只能装进插件自己的 profile */}
      <div className="hint warning" style={{ margin: 0, flexDirection: "column", gap: 4 }}>
        <span style={{ fontWeight: 600 }}>{I18N["dsh.tui.notice.title"]}</span>
        <span>{I18N["dsh.tui.notice.scope"]}</span>
        <span>{I18N["dsh.tui.notice.core"]}</span>
        <span>{I18N["dsh.tui.notice.third_party"]}</span>
        <span>{I18N["dsh.tui.notice.facts"]}</span>
        <span style={{ fontWeight: 600 }}>{I18N["dsh.tui.notice.web"]}</span>
      </div>

      <div style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
        {I18N["dsh.tui.intro"]} {I18N["dsh.tui.profile.not_listed"]}
      </div>

      <div style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
        {I18N["dsh.tui.rules"]}
      </div>

      {!installed ? (
        <p className="hint info" style={{ margin: 0 }}>
          {I18N["dsh.tui.needs_install"]}
        </p>
      ) : loading && catalog === null ? (
        <div style={{ display: "flex", alignItems: "center", gap: 10, padding: 16 }}>
          <span className="spinner" />
          <span style={{ fontSize: 12 }}>{I18N["dsh.tui.loading"]}</span>
        </div>
      ) : error && catalog === null ? (
        <div className="empty-state">
          <span>{error}</span>
          <button className="btn btn-outline ripple-host" onClick={() => void load()}>
            {I18N["button.refresh"]}
          </button>
        </div>
      ) : (
        // 能力插件排在终端后面，并自带一条标题——它们不是终端，装进的是实例自己的
        // profile，点「终端」也不会启动它们。
        [...(catalog ?? [])]
          .sort((a, b) => (a.kind === "capability" ? 1 : 0) - (b.kind === "capability" ? 1 : 0))
          .map((plugin, index, ordered) => {
          const firstCapability =
            plugin.kind === "capability" && ordered.findIndex((item) => item.kind === "capability") === index;
          const state = stateOf.get(plugin.packageName);
          // 清单里声明了 ≠ 文件在磁盘上：pnpm 先写 package.json 再拉包，中途失败会留下一个
          // 「有声明、没文件」的 profile，它起不来。旧面板没有 filesPresent 字段，缺字段
          // 当作在位——宁可让后端拒绝，也不要在界面上凭空报错。
          const declared = state?.installed === true;
          const isPresent = declared && state?.filesPresent !== false;
          const incomplete = declared && !isPresent;
          // 装着的不是这一版自带的那份（同版本号的旧内容也算）：给「更新」而不是只给卸载。
          const outdated = isPresent && state?.outdated === true;
          // 缺字段（旧面板）当作能装：宁可让后端拒绝，也不要在界面上凭空禁用
          const fitsHost = state?.fitsHost !== false;
          const requiresDsh = state?.requiresDsh ?? plugin.requiresDsh ?? null;
          // Several terminals can be installed on purpose (the ACP one is the fallback for a
          // version that moves); exactly one is what the terminal button starts.
          const active = state?.active === true;
          const shownVersion = declared ? state?.installedVersion ?? plugin.version : plugin.version;
          return (
            <div key={plugin.id}>
              {firstCapability && (
                <div className="section-caption" style={{ marginTop: 12 }}>
                  面板能力（不是终端）
                </div>
              )}
              <div className="tlli" style={{ cursor: "default", alignItems: "flex-start" }}>
                <span className="tlli-graphic">
                  <Package2Icon size={32} />
                </span>
                <span className="tlli-text">
                  <span className="tlli-title">
                    {plugin.name}
                    <span
                      className={`tag ${incomplete ? "warn" : isPresent ? "running" : "plain"}`}
                      title={incomplete ? I18N["dsh.tui.incomplete.hint"] : undefined}
                    >
                      {incomplete
                        ? I18N["dsh.tui.incomplete"]
                        : isPresent
                          ? I18N["dsh.tui.installed"]
                          : I18N["dsh.tui.not_installed"]}
                    </span>
                    <span className="tag" title={plugin.profile}>
                      {I18N["dsh.tui.profile"]}: {plugin.profile}
                    </span>
                    {plugin.bundled && (
                      <span className="tag running" title={I18N["dsh.tui.bundled.hint"]}>
                        {I18N["dsh.tui.bundled"]}
                      </span>
                    )}
                    {outdated && (
                      <span className="tag warn" title={I18N["dsh.tui.outdated.hint"]}>
                        {I18N["dsh.tui.outdated"]}
                      </span>
                    )}
                    {isPresent && active && (
                      <span className="tag running" title={I18N["dsh.tui.active.hint"]}>
                        {I18N["dsh.tui.active"]}
                      </span>
                    )}
                    {isPresent && !active && (
                      <span className="tag plain" title={I18N["dsh.tui.standby.hint"]}>
                        {I18N["dsh.tui.standby"]}
                      </span>
                    )}
                  </span>
                  <span className="tlli-subtitle" style={{ lineHeight: 1.6 }}>
                    {plugin.description}
                  </span>
                  <span className="tlli-subtitle" style={{ lineHeight: 1.8 }}>
                    {I18N["dsh.tui.package"]} <span className="mono">{plugin.packageName}</span>
                    {" · "}
                    {I18N["dsh.tui.version"]} {shownVersion}
                  {isPresent && shownVersion !== plugin.version ? `（清单锁定 ${plugin.version}）` : ""}
                    {" · "}
                    {I18N["dsh.tui.license"]} {plugin.license}
                    {" · "}
                    {plugin.repository ? (
                      <a href={plugin.repository} target="_blank" rel="noreferrer">
                        {I18N["dsh.tui.repository"]}
                      </a>
                    ) : (
                      I18N["dsh.tui.repository.none"]
                    )}
                  </span>
                  <span className="tlli-subtitle" style={{ lineHeight: 1.6 }}>
                    {plugin.bin
                      ? I18N["dsh.tui.bin"].replace("%s", plugin.bin)
                      : I18N["dsh.tui.pure_bundle"].replace("%s", plugin.profile)}
                  </span>
                  <span className="tlli-subtitle" style={{ lineHeight: 1.6 }}>
                    {I18N["dsh.tui.profile.row"].replace("%s", plugin.profile)}
                  </span>
                  {requiresDsh && (
                    <span
                      className="tlli-subtitle"
                      style={{ lineHeight: 1.6, color: fitsHost ? undefined : "var(--monet-warning, #b26a00)" }}
                    >
                      {I18N["dsh.tui.requires_dsh"].replace("%s", requiresDsh)}
                      {!fitsHost && ` — ${I18N["dsh.tui.host_mismatch"]}`}
                    </span>
                  )}
                </span>
                <span className="row-actions">
                  {outdated && (
                    <button
                      className="btn btn-raised ripple-host"
                      disabled={busy === plugin.packageName || !fitsHost}
                      title={I18N["dsh.tui.outdated.hint"]}
                      onClick={() => void submit(plugin, true)}
                    >
                      {I18N["dsh.tui.update"]}
                    </button>
                  )}
                  {isPresent && (
                    <button
                      className="btn btn-text danger ripple-host"
                      disabled={busy === plugin.packageName}
                      onClick={() => void submit(plugin, false)}
                    >
                      {I18N["dsh.tui.remove"]}
                    </button>
                  )}
                  {incomplete && (
                    <>
                      <button
                        className="btn btn-raised ripple-host"
                        disabled={busy === plugin.packageName || !fitsHost}
                        title={I18N["dsh.tui.incomplete.hint"]}
                        onClick={() => void submit(plugin, true)}
                      >
                        {I18N["dsh.tui.reinstall"]}
                      </button>
                      <button
                        className="btn btn-text danger ripple-host"
                        disabled={busy === plugin.packageName}
                        onClick={() => void submit(plugin, false)}
                      >
                        {I18N["dsh.tui.remove"]}
                      </button>
                    </>
                  )}
                  {!declared && (
                    <button
                      className="btn btn-raised ripple-host"
                      disabled={busy === plugin.packageName || !fitsHost}
                      title={fitsHost ? undefined : I18N["dsh.tui.host_mismatch"]}
                      onClick={() => void submit(plugin, true)}
                    >
                      {I18N["dsh.tui.install"]}
                    </button>
                  )}
                </span>
              </div>
              {index < (catalog ?? []).length - 1 && <div className="divider" style={{ marginLeft: 12 }} />}
            </div>
          );
        })
      )}

      {tasks.length > 0 && (
        <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
          {tasks.map((t) => (
            <div key={t.id} style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 12 }}>
              <span className={`tag ${taskStateClass(t.state)}`}>{taskStateLabel(t.state)}</span>
              <span className="mono" style={{ opacity: 0.7 }}>
                {t.kind}
              </span>
              {t.message && <span style={{ opacity: 0.8 }}>{t.message}</span>}
              {t.error && <span style={{ color: "var(--monet-error)" }}>{t.error}</span>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}