import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { api } from "../api";
import { disposeConsole } from "../acp";
import { installVersion as runInstall, launchInstance, openDsh, stopInstance } from "../actions";
import { ConfirmDialog, Dialog } from "../components/Dialog";
import { ConsolePanel } from "../components/ConsolePanel";
import { ExportInstanceModal } from "../components/ExportInstanceModal";
import { StateBadge, stateLabel } from "../components/InstanceIcon";
import { InstancePluginsPanel } from "../components/InstancePluginsPanel";
import { InstanceSessionsPanel, InstanceSkillsPanel } from "../components/InstanceExtrasPanel";
import { TuiPluginSection } from "../components/TuiPluginSection";
import { TerminalWindow, type TerminalTargetInfo } from "../components/TerminalWindow";
import { LogView } from "../components/LogView";
import { ProgressBar } from "../components/ProgressBar";
import {
  CancelIcon,
  DeleteIcon,
  DeployedCodeFillIcon,
  DeployedCodeIcon,
  EditIcon,
  ErrorIcon,
  ExtensionFillIcon,
  ExtensionIcon,
  InfoFillIcon,
  InfoIcon,
  MenuIcon,
  MoreVertIcon,
  Package2FillIcon,
  Package2Icon,
  PublicIcon,
  RefreshIcon,
  RocketLaunchIcon,
  SettingsFillIcon,
  SettingsIcon,
  TerminalIcon,
  TextureIcon,
} from "../components/icons";
import { PopupItem, PopupMenu, PopupSep } from "../components/PopupMenu";
import { SubSideBar } from "../components/SideBar";
import { HOME_MODES } from "../constants";
import { hasPendingLaunch } from "../launch";
import { useTickingUptime } from "../hooks";
import { I18N } from "../i18n";
import {
  findTaskForInstance,
  getState,
  loadAccounts,
  loadVersions,
  refreshInstances,
  seedLogs,
  seedTasks,
  toast,
  upsertInstance,
  useAppState,
} from "../store";
import { ws } from "../ws";
import type { PatchInstanceBody, VersionInfo } from "../types";
import {
  channelLabel,
  errMsg,
  formatUptime,
  normState,
  parsePortFromUrl,
} from "../utils";

type DetailTab =
  | "manage"
  | "auto-install"
  | "plugins"
  | "skills"
  | "sessions"
  | "logs"
  | "console"
  | "details";

const TAB_ALIASES: Record<string, DetailTab> = {
  manage: "manage",
  "auto-install": "auto-install",
  plugins: "plugins",
  skills: "skills",
  sessions: "sessions",
  logs: "logs",
  console: "console",
  details: "details",
  // 旧版 tab 值兼容
  overview: "manage",
  settings: "manage",
};

function VersionSelect({
  value,
  onChange,
  versions,
}: {
  value: string;
  onChange: (v: string) => void;
  versions: VersionInfo[] | null;
}) {
  const opts = versions && versions.length > 0 ? versions : null;
  return (
    <select className="input" style={{ flex: 1, fontWeight: 400 }} value={value} onChange={(e) => onChange(e.target.value)}>
      {opts ? (
        opts.map((v) => (
          <option key={v.version} value={v.version}>
            {v.version}（{channelLabel(v.channel)}）{v.installable ? "" : "· 不可安装"}
          </option>
        ))
      ) : (
        <option value={value}>{value}</option>
      )}
    </select>
  );
}

/** 实例详情页组（InstancePage.java 结构）。 */
export function InstanceDetailPage() {
  const { id = "" } = useParams();
  const nav = useNavigate();
  const s = useAppState();
  const instance = s.instances.find((i) => i.id === id);

  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get("tab") ?? "manage";
  const tab: DetailTab = TAB_ALIASES[tabParam] ?? "manage";
  const setTab = (t: DetailTab) => setSearchParams({ tab: t }, { replace: true });

  const [form, setForm] = useState({ name: "", homeMode: "ISOLATED", portMode: "auto", port: "", account: "", nodeRuntime: "" });
  const [nodeOptions, setNodeOptions] = useState<import("../api").NodeStatus | null>(null);
  const [formDirty, setFormDirty] = useState(false);
  const [saveBusy, setSaveBusy] = useState(false);
  const [confirmStop, setConfirmStop] = useState(false);
  const [stopBusy, setStopBusy] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [installVersion, setInstallVersion] = useState("");
  const [installBusy, setInstallBusy] = useState(false);
  const [reinstallVersion, setReinstallVersion] = useState("");
  const [renameOpen, setRenameOpen] = useState(false);
  const [renameValue, setRenameValue] = useState("");
  const [renameBusy, setRenameBusy] = useState(false);
  const [manageAnchor, setManageAnchor] = useState<HTMLElement | null>(null);
  const [exportOpen, setExportOpen] = useState(false);
  const [terminalTarget, setTerminalTarget] = useState<TerminalTargetInfo | null>(null);
  /// Web Shell 总开关 + 面板是否在讲 TLS；任一不满足就不显示终端入口（docs/web-shell.md §4.3）
  const [webShellEnabled, setWebShellEnabled] = useState(true);

  // 挂载：订阅该实例日志 topic + 全局任务，打底数据，并周期性兜底刷新
  useEffect(() => {
    ws.addTopics([`instance:${id}`, "tasks"]);
    void seedTasks();
    void seedLogs(id);
    void loadVersions();
    void loadAccounts();
    void refreshInstances(!getState().instancesLoaded);
    const timer = window.setInterval(() => void refreshInstances(false), 15000);
    return () => {
      window.clearInterval(timer);
      ws.removeTopics([`instance:${id}`]);
      disposeConsole(id);
    };
  }, [id]);

  // 终端入口的显示条件与列表页一致：Web Shell 总开关 + 面板此刻真的在讲 TLS
  useEffect(() => {
    let alive = true;
    api
      .developerStatus()
      .then((status) => {
        if (alive) setWebShellEnabled(status.webShellEnabled && status.https);
      })
      .catch(() => {
        // 读不到就维持显示：后端仍会拒绝打开，按钮不是那道门
      });
    return () => {
      alive = false;
    };
  }, []);

  // 深链直达（store 里还没有该实例）时单独拉取
  useEffect(() => {
    if (instance) return;
    let cancelled = false;
    api
      .getInstance(id)
      .then((inst) => {
        if (!cancelled) upsertInstance(inst);
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [id, instance]);

  // 实例数据 → 设置表单（用户编辑期间不覆盖）
  useEffect(() => {
    if (!instance) return;
    if (formDirty) return;
    const next = {
      name: instance.name,
      homeMode: instance.homeMode || "ISOLATED",
      portMode: instance.portMode === "fixed" ? "fixed" : "auto",
      port: instance.port != null ? String(instance.port) : "",
      account: instance.account ?? "",
      nodeRuntime: instance.nodeRuntime ?? "",
    };
    setForm((prev) =>
      prev.name === next.name &&
      prev.homeMode === next.homeMode &&
      prev.portMode === next.portMode &&
      prev.port === next.port &&
      prev.account === next.account &&
      prev.nodeRuntime === next.nodeRuntime
        ? prev
        : next,
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [instance?.id, instance?.name, instance?.homeMode, instance?.portMode, instance?.port, instance?.account, instance?.nodeRuntime, formDirty]);

  // The Node selector needs the installed majors and exact versions; the page
  // already asks `/api/node` for the settings tab, so reuse the same contract.
  useEffect(() => {
    void api.nodeStatus().then(setNodeOptions).catch(() => setNodeOptions(null));
  }, []);

  const nodeConsumer = nodeOptions?.consumers.find((c) => c.kind === "instance" && c.id === id);

  const revertNode = async () => {
    try {
      await api.nodeRevert({ kind: "instance", id });
      toast("success", "已切回上次成功启动的 Node.js");
      await refreshInstances();
      setNodeOptions(await api.nodeStatus());
    } catch (e) {
      toast("error", `回退失败：${errMsg(e)}`);
    }
  };

  // 版本选择器的初值跟随实例：路由 /instances/:id 不会重挂组件，换实例或实例
  // 版本变化时必须重置，否则会把上一个实例的版本当成这个实例的「重装」目标
  // 提交给后端 —— 版本漂移就是这么产生的。
  useEffect(() => {
    if (!instance) return;
    setInstallVersion(instance.version);
    setReinstallVersion(instance.version);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [instance?.id, instance?.version]);

  const task = useMemo(() => findTaskForInstance(id), [s.tasks, id]);
  const st = normState(instance?.state);
  const uptime = useTickingUptime(id, instance?.uptimeSec);

  if (!instance) {
    return (
      <div className="page-content">
        <div className="empty-state">
          <span className="spinner" />
          <span>正在加载实例…</span>
          <button className="btn btn-outline ripple-host" onClick={() => nav("/instances")}>
            返回列表
          </button>
        </div>
      </div>
    );
  }

  // ---------- 操作 ----------

  const doLaunch = () => void launchInstance(instance);

  const doStop = async () => {
    setStopBusy(true);
    await stopInstance(instance);
    setStopBusy(false);
    setConfirmStop(false);
  };

  const doInstall = async (version: string) => {
    setInstallBusy(true);
    await runInstall(instance, version);
    setInstallBusy(false);
  };

  const cancelInstall = async () => {
    if (!task) {
      toast("error", "未找到对应的安装任务");
      return;
    }
    try {
      await api.cancelTask(task.id);
      toast("info", "已发送取消指令");
    } catch (e) {
      toast("error", `取消失败：${errMsg(e)}`);
    }
  };

  const saveSettings = async () => {
    const body: PatchInstanceBody = {
      name: form.name.trim() || instance.name,
      homeMode: form.homeMode,
      account: form.account || null,
      nodeRuntime: form.nodeRuntime || null,
    };
    if (form.portMode === "fixed") {
      const n = Number(form.port);
      if (!Number.isInteger(n) || n < 1024 || n > 65535) {
        toast("error", I18N["dsh.instance.port.invalid"]);
        return;
      }
      body.portMode = "fixed";
      body.port = n;
    } else {
      body.portMode = "auto";
    }
    setSaveBusy(true);
    try {
      await api.patchInstance(id, body);
      toast("success", "设置已保存");
      setFormDirty(false);
      await refreshInstances(false);
    } catch (e) {
      toast("error", `保存失败：${errMsg(e)}`);
    } finally {
      setSaveBusy(false);
    }
  };

  const doDelete = async () => {
    setDeleteBusy(true);
    try {
      await api.deleteInstance(id);
      toast("success", I18N["dsh.instance.removed"].replace("%s", id));
      nav("/instances", { replace: true });
    } catch (e) {
      toast("error", `删除失败：${errMsg(e)}`);
      setDeleteBusy(false);
    }
  };

  const doRename = async () => {
    const name = renameValue.trim();
    if (!name || name === instance.name) {
      setRenameOpen(false);
      return;
    }
    setRenameBusy(true);
    try {
      await api.patchInstance(id, { name });
      toast("success", "实例已重命名");
      setRenameOpen(false);
      await refreshInstances(false);
    } catch (e) {
      toast("error", `重命名失败：${errMsg(e)}`);
    } finally {
      setRenameBusy(false);
    }
  };

  const running = st === "RUNNING";
  const port = instance.port ?? parsePortFromUrl(instance.url);

  /// 终端入口：打开终端会先停掉同实例的 web 再跑 TUI，所以与实例状态无关，装了就能开。
  /// 不可用时**变灰禁用而不是隐藏**——隐藏会让人以为没有这个功能（docs/web-shell.md §4.3）。
  const terminalButton = (
    <button
      className="btn btn-outline ripple-host"
      title={webShellEnabled ? I18N["terminal.open"] : I18N["terminal.unavailable"]}
      disabled={!webShellEnabled}
      onClick={() => setTerminalTarget({ kind: "dsh", id, name: instance.name })}
    >
      <TerminalIcon size={15} />
      {I18N["terminal.open"]}
    </button>
  );

  // ---------- 概览（实例管理 tab 顶部状态卡） ----------

  const overview = (() => {
    switch (st) {
      case "NOT_INSTALLED": {
        // 上一次安装为什么失败：任务终态带 error，详情接口也留了
        // installProgress —— 不显示的话，用户只能看到"没装上"。
        const failure =
          task?.error
          ?? (instance.installProgress?.state === "failed" ? instance.installProgress.message : null);
        return (
          <div className="card" style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <div className="hint info" style={{ margin: 0 }}>
              该实例尚未安装 dsh 运行时：可以只安装，也可以「安装并启动」，装好会自动拉起
            </div>
            {failure && <div className="hint error">{failure}</div>}
            <div style={{ display: "flex", gap: 8 }}>
              <VersionSelect
                value={installVersion || instance.version}
                onChange={setInstallVersion}
                versions={s.versions}
              />
              <button
                className="btn btn-raised ripple-host"
                disabled={installBusy}
                onClick={() => void doInstall(installVersion || instance.version)}
              >
                {installBusy ? "正在提交…" : I18N["dsh.install.start"]}
              </button>
              <button
                className="btn btn-outline ripple-host"
                disabled={installBusy}
                onClick={() => void launchInstance(instance)}
              >
                安装并启动
              </button>
            </div>
          </div>
        );
      }
      case "INSTALLING":
        return (
          <div className="card" style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <div style={{ fontSize: 14, fontWeight: 600 }}>
              {I18N["dsh.versions.installing"].replace("%s", instance.version)}
            </div>
            <ProgressBar fraction={task?.fraction} />
            <div style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
              {task?.message ?? "正在下载并安装 dsh 运行时，请稍候…"}
            </div>
            <div style={{ display: "flex", justifyContent: "flex-end" }}>
              <button className="btn btn-text dim ripple-host" onClick={() => void cancelInstall()}>
                {I18N["button.cancel"]}
              </button>
            </div>
          </div>
        );
      case "STARTING":
        return (
          <div className="card" style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <div style={{ fontSize: 14, fontWeight: 600 }}>{I18N["dsh.launch.launching"]}</div>
            <ProgressBar fraction={null} />
            <div style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
              正在拉起 dsh 进程，等待就绪信号…
            </div>
            {hasPendingLaunch(id) && (
              <div className="hint info" style={{ margin: 0 }}>
                就绪后将自动打开 dsh 标签页（如未弹出请检查浏览器弹窗拦截，就绪后可手动打开）
              </div>
            )}
            <div style={{ display: "flex", justifyContent: "flex-end" }}>
              <button className="btn btn-text dim ripple-host" onClick={() => void stopInstance(instance)}>
                {I18N["button.cancel"]}
              </button>
            </div>
          </div>
        );
      case "STOPPING":
        return (
          <div className="card" style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <div style={{ fontSize: 14, fontWeight: 600 }}>{I18N["dsh.stopping"]}</div>
            <ProgressBar fraction={null} />
            <div style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>正在终止 dsh 进程…</div>
          </div>
        );
      case "RUNNING": {
        return (
          <div className="card" style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
              <span className="tag running">{stateLabel(st)}</span>
              <span style={{ fontSize: 13 }}>
                {uptime != null ? formatUptime(uptime) : "—"}
                {port != null ? ` · ${I18N["dsh.instance.port"]} ${port}` : ""}
                {instance.url ? ` · ${instance.url}` : ""}
              </span>
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              <button className="btn btn-raised ripple-host" onClick={() => openDsh(id)}>
                <PublicIcon size={15} />
                {I18N["dsh.launch.running"]}
              </button>
              <button className="btn btn-outline ripple-host" onClick={() => setConfirmStop(true)}>
                <CancelIcon size={15} />
                {I18N["dsh.stop"]}
              </button>
              {terminalButton}
            </div>
          </div>
        );
      }
      case "FAILED": {
        const crash = s.crash[id];
        const tailLines = (crash?.crashTail ?? "")
          .split(/\r?\n/)
          .map((l) => l.trimEnd())
          .filter((l) => l.trim().length > 0)
          .slice(0, 20);
        return (
          <div className="card" style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8, color: "var(--monet-error)", fontSize: 14, fontWeight: 600 }}>
              <ErrorIcon size={18} />
              实例启动失败
            </div>
            {crash && crash.exitCode != null && (
              <div style={{ fontSize: 12 }}>
                退出码 <code>{crash.exitCode}</code>
              </div>
            )}
            {tailLines.length > 0 && (
              <pre
                className="mono"
                style={{
                  background: "var(--fixed-log-background)",
                  color: "var(--fixed-log-text-fill)",
                  padding: 8,
                  borderRadius: 4,
                  fontSize: 11,
                  maxHeight: 160,
                  overflow: "auto",
                }}
              >
                {tailLines.join("\n")}
              </pre>
            )}
            <div style={{ display: "flex", gap: 8 }}>
              <button className="btn btn-raised ripple-host" onClick={doLaunch}>
                <RocketLaunchIcon size={15} />
                重新启动
              </button>
              <button className="btn btn-outline ripple-host" onClick={() => void seedLogs(id)}>
                <RefreshIcon size={15} />
                刷新日志
              </button>
              {terminalButton}
            </div>
          </div>
        );
      }
      case "STOPPED":
      default:
        return (
          <div className="card" style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <div style={{ fontSize: 13, color: "var(--monet-on-surface-variant)" }}>
              实例已停止。点击启动将拉起 dsh 进程，就绪后自动打开 dsh 页面
            </div>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              <button className="btn btn-raised ripple-host" onClick={doLaunch}>
                <RocketLaunchIcon size={15} />
                {I18N["dsh.launch"]}
              </button>
              {terminalButton}
              <span style={{ flex: 1 }} />
              <VersionSelect
                value={reinstallVersion || instance.version}
                onChange={setReinstallVersion}
                versions={s.versions}
              />
              <button
                className="btn btn-outline ripple-host"
                disabled={installBusy}
                onClick={() => void doInstall(reinstallVersion || instance.version)}
              >
                {I18N["dsh.install.start"]}
              </button>
            </div>
          </div>
        );
    }
  })();

  const showLogs =
    st === "INSTALLING" || st === "STARTING" || st === "STOPPING" || st === "RUNNING" || st === "FAILED" || st === "STOPPED";

  return (
    <div className="page-with-sidebar">
      <nav className="sidebar-sub sidebar-stack" style={{ paddingTop: 0 }}>
        {/* 顶部操作框：启动/停止 + 管理 */}
        <div className="card instance-actions-box" style={{ display: "flex", padding: 4, gap: 2 }}>
          <button
            className="tool-btn ripple-host"
            style={{ flex: 1, justifyContent: "center" }}
            disabled={st === "INSTALLING" || st === "STOPPING" || st === "STARTING"}
            onClick={() => (running ? setConfirmStop(true) : doLaunch())}
          >
            {running || st === "STARTING" ? (
              <CancelIcon size={18} />
            ) : (
              <RocketLaunchIcon size={18} />
            )}
            {running || st === "STARTING" ? I18N["dsh.stop"] : I18N["dsh.launch"]}
          </button>
          <button
            className="tool-btn ripple-host"
            style={{ flex: 1, justifyContent: "center" }}
            title={I18N["settings.game.management"]}
            onClick={(e) => setManageAnchor(e.currentTarget)}
          >
            <MenuIcon size={18} />
            {I18N["settings.game.management"]}
          </button>
        </div>

        <SubSideBar
          activeKey={tab}
          onSelect={(k) => setTab(k as DetailTab)}
          sections={[
            {
              items: [
                {
                  key: "manage",
                  title: I18N["dsh.instance.manage"],
                  icon: <SettingsIcon size={20} />,
                  iconActive: <SettingsFillIcon size={20} />,
                },
                {
                  key: "auto-install",
                  title: I18N["settings.tabs.installers"],
                  icon: <DeployedCodeIcon size={20} />,
                  iconActive: <DeployedCodeFillIcon size={20} />,
                },
                {
                  key: "plugins",
                  title: I18N["dsh.instance.plugins"],
                  icon: <ExtensionIcon size={20} />,
                  iconActive: <ExtensionFillIcon size={20} />,
                },
                {
                  key: "skills",
                  title: I18N["dsh.instance.skills"],
                  icon: <TextureIcon size={20} />,
                },
                {
                  key: "sessions",
                  title: I18N["dsh.instance.sessions"],
                  icon: <Package2Icon size={20} />,
                  iconActive: <Package2FillIcon size={20} />,
                },
                {
                  key: "logs",
                  title: "日志",
                  icon: <InfoIcon size={20} />,
                },
                {
                  key: "console",
                  title: "控制台",
                  icon: <MoreVertIcon size={20} />,
                },
                {
                  key: "details",
                  title: I18N["dsh.instance.details"],
                  icon: <InfoIcon size={20} />,
                  iconActive: <InfoFillIcon size={20} />,
                },
              ],
            },
          ]}
        />
      </nav>

      <div
        className="page-content"
        key={tab}
        style={{ background: "transparent", margin: 0, padding: 10, gap: 8, animation: "slide-up-fade-in 400ms var(--ease)" }}
      >
        {tab === "manage" && (
          <div className="page-content scroll" style={{ margin: 0, gap: 8 }}>
            {overview}

            {/* 设置项列表 */}
            <div className="card" style={{ display: "flex", flexDirection: "column", gap: 2 }}>
              <div className="comp-row">
                <span className="comp-label">{I18N["dsh.instance.name"]}</span>
                <span className="comp-value">
                  <input
                    className="input"
                    value={form.name}
                    onChange={(e) => {
                      setForm((f) => ({ ...f, name: e.target.value }));
                      setFormDirty(true);
                    }}
                  />
                </span>
              </div>
              <div className="comp-row">
                <span className="comp-label">{I18N["dsh.install.home"]}</span>
                <span className="comp-value">
                  <select
                    className="input"
                    style={{ fontWeight: 400 }}
                    value={form.homeMode}
                    onChange={(e) => {
                      setForm((f) => ({ ...f, homeMode: e.target.value }));
                      setFormDirty(true);
                    }}
                  >
                    {HOME_MODES.map((m) => (
                      <option key={m.value} value={m.value}>
                        {m.title}
                      </option>
                    ))}
                  </select>
                </span>
              </div>
              <div className="comp-row">
                <span className="comp-label">Node.js</span>
                <span className="comp-value">
                  <select
                    className="input"
                    style={{ fontWeight: 400 }}
                    value={form.nodeRuntime}
                    onChange={(e) => {
                      setForm((f) => ({ ...f, nodeRuntime: e.target.value }));
                      setFormDirty(true);
                    }}
                  >
                    <option value="">跟随默认</option>
                    <option value="auto">自动（PATH 优先，回落到托管）</option>
                    {(nodeOptions?.channels ?? []).filter((c) => c.installed).map((c) => (
                      <option key={c.name} value={c.name}>
                        Node {c.name}{c.lts ? "（LTS）" : ""}{c.resolved ? ` · ${c.resolved}` : ""}
                      </option>
                    ))}
                    {(nodeOptions?.runtimes ?? []).map((r) => (
                      <option key={r.version} value={r.version}>精确 {r.version}</option>
                    ))}
                    {form.nodeRuntime !== "" && form.nodeRuntime !== "auto"
                      && !(nodeOptions?.channels ?? []).some((c) => c.name === form.nodeRuntime && c.installed)
                      && !(nodeOptions?.runtimes ?? []).some((r) => r.version === form.nodeRuntime) && (
                        <option value={form.nodeRuntime}>{form.nodeRuntime}</option>
                      )}
                  </select>
                </span>
              </div>
              <div className="comp-row">
                <span className="comp-label">{I18N["dsh.instance.port.mode"]}</span>
                <span className="comp-value">
                  <select
                    className="input"
                    style={{ width: 120, fontWeight: 400 }}
                    value={form.portMode}
                    onChange={(e) => {
                      setForm((f) => ({ ...f, portMode: e.target.value }));
                      setFormDirty(true);
                    }}
                  >
                    <option value="auto">{I18N["dsh.instance.port.mode.auto"]}</option>
                    <option value="fixed">{I18N["dsh.instance.port.mode.fixed"]}</option>
                  </select>
                  <input
                    className="input"
                    style={{ width: 110 }}
                    type="number"
                    min={1024}
                    max={65535}
                    placeholder="1024–65535"
                    disabled={form.portMode !== "fixed"}
                    value={form.port}
                    onChange={(e) => {
                      setForm((f) => ({ ...f, port: e.target.value }));
                      setFormDirty(true);
                    }}
                  />
                </span>
              </div>
              <div className="comp-row">
                <span className="comp-label">账户</span>
                <span className="comp-value">
                  {s.accounts !== null ? (
                    <select
                      className="input"
                      style={{ fontWeight: 400 }}
                      value={form.account}
                      onChange={(e) => {
                        setForm((f) => ({ ...f, account: e.target.value }));
                        setFormDirty(true);
                      }}
                    >
                      <option value="">{I18N["dsh.account.none.short"]}</option>
                      {s.accounts.map((a) => (
                        <option key={a.name} value={a.name}>
                          {a.label || a.name}
                          {a.vendor ? `（${a.vendor}）` : ""}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
                      {s.accountsError === "后端尚未支持账户管理"
                        ? "后端尚未支持账户管理"
                        : s.accountsLoading
                          ? "正在加载账户列表…"
                          : "账户列表不可用"}
                      ，可前往「账户」页管理
                    </span>
                  )}
                  {form.account && s.accounts && !s.accounts.some((a) => a.name === form.account) && (
                    <div style={{ fontSize: 12, color: "var(--monet-error)", marginTop: 4, lineHeight: 1.6 }}>
                      这个实例指定的账户「{form.account}」已经不在了：启动不会注入 key 与供应商路由，
                      harness 起来后没有模型可用。重新选一个账户，或去「账户」页把它加回来
                      （账户里的「模型」栏也填上，读不到供应商列表时才有兜底）。
                    </div>
                  )}
                </span>
              </div>
              <div style={{ display: "flex", alignItems: "center", paddingTop: 8 }}>
                <button
                  className="btn btn-text danger ripple-host"
                  disabled={running}
                  title={running ? "运行中的实例无法删除，请先停止" : undefined}
                  onClick={() => setConfirmDelete(true)}
                >
                  <DeleteIcon size={14} />
                  删除实例
                </button>
                <span style={{ flex: 1 }} />
                <button
                  className="btn btn-raised ripple-host"
                  disabled={!formDirty || saveBusy}
                  onClick={() => void saveSettings()}
                >
                  {saveBusy ? "保存中…" : I18N["button.save"]}
                </button>
              </div>
            </div>
          </div>
        )}

        {tab === "auto-install" && (
          <div className="page-content scroll" style={{ margin: 0, gap: 8 }}>
            <div className="card" style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              <div style={{ fontSize: 14, fontWeight: 600 }}>{I18N["settings.tabs.installers"]}</div>
              <div style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
                选择要安装（或更换）的 dsh 运行时版本，安装过程可在「日志」页查看。
              </div>
              {running && (
                <div className="hint warning" style={{ margin: 0 }}>
                  实例正在运行：换版本要先停止，否则后端会拒绝（别在进程跑着的时候换文件）
                </div>
              )}
              <div style={{ display: "flex", gap: 8 }}>
                <VersionSelect
                  value={reinstallVersion || instance.version}
                  onChange={setReinstallVersion}
                  versions={s.versions}
                />
                <button
                  className="btn btn-raised ripple-host"
                  disabled={installBusy || st === "RUNNING" || st === "STARTING" || st === "STOPPING"}
                  onClick={() => void doInstall(reinstallVersion || instance.version)}
                >
                  {installBusy ? "正在提交…" : I18N["dsh.install.start"]}
                </button>
              </div>
            </div>

            {/* TUI 终端插件：dsh 没有内置终端界面，终端由社区插件提供（见 DshTuiPlugins）。
                安装/卸载走的就是本页「插件」那一套接口，这里只是把清单与免责声明摆出来。 */}
            <TuiPluginSection instance={instance} />
          </div>
        )}

        {tab === "plugins" && <InstancePluginsPanel instance={instance} />}
        {tab === "skills" && <InstanceSkillsPanel instance={instance} />}
        {tab === "sessions" && <InstanceSessionsPanel instance={instance} />}

        {tab === "logs" &&
          (showLogs ? (
            <LogView instanceId={id} maxHeight={99999} emptyHint="暂无日志" />
          ) : (
            <div className="card">
              <p className="hint info" style={{ margin: 2 }}>
                实例尚未安装，安装后即可查看运行日志
              </p>
            </div>
          ))}

        {tab === "console" && <ConsolePanel instance={instance} />}

        {tab === "details" && (
          <div className="card" style={{ display: "flex", flexDirection: "column", gap: 2 }}>
            <div className="comp-row">
              <span className="comp-label">ID</span>
              <span className="comp-value mono" style={{ fontSize: 12 }}>{instance.id}</span>
            </div>
            <div className="comp-row">
              <span className="comp-label">{I18N["dsh.instance.name"]}</span>
              <span className="comp-value">{instance.name}</span>
            </div>
            <div className="comp-row">
              <span className="comp-label">版本</span>
              <span className="comp-value">
                {instance.version}
                <StateBadge state={instance.state} />
              </span>
            </div>
            <div className="comp-row">
              <span className="comp-label">{I18N["dsh.install.home"]}</span>
              <span className="comp-value">
                {HOME_MODES.find((m) => m.value === instance.homeMode)?.title ?? instance.homeMode}
              </span>
            </div>
            <div className="comp-row">
              <span className="comp-label">Node.js</span>
              <span className="comp-value" style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                {instance.nodeRuntime ?? "跟随默认"}
                {instance.nodeResolved?.version ? ` → ${instance.nodeResolved.version}` : ""}
                {instance.nodeResolved && !instance.nodeResolved.compatible && (
                  <span className="tag plain">不满足</span>
                )}
                {nodeConsumer?.requirement && (
                  <span className="tag plain" title="该版本声明的 engines.node">
                    要求 {nodeConsumer.requirement}
                  </span>
                )}
                {nodeConsumer?.lastGood && nodeConsumer.lastGood !== nodeConsumer.resolved && (
                  <button className="btn btn-text ripple-host" style={{ height: 24 }} onClick={() => void revertNode()}>
                    回退到 {nodeConsumer.lastGood}
                  </button>
                )}
              </span>
            </div>
            <div className="comp-row">
              <span className="comp-label">{I18N["dsh.instance.port"]}</span>
              <span className="comp-value">
                {port != null ? port : st === "RUNNING" ? I18N["dsh.instance.port.auto.none"] : "—"}
              </span>
            </div>
            <div className="comp-row">
              <span className="comp-label">账户</span>
              <span className="comp-value">{instance.account ?? I18N["dsh.account.none.short"]}</span>
            </div>
            {instance.url && (
              <div className="comp-row">
                <span className="comp-label">URL</span>
                <span className="comp-value mono" style={{ fontSize: 12 }}>{instance.url}</span>
              </div>
            )}
          </div>
        )}
      </div>

      {/* 管理菜单 */}
      <PopupMenu open={manageAnchor !== null} anchor={manageAnchor} onClose={() => setManageAnchor(null)}>
        <PopupItem
          icon={running ? <CancelIcon size={16} /> : <RocketLaunchIcon size={16} />}
          onClick={() => {
            setManageAnchor(null);
            if (running) setConfirmStop(true);
            else doLaunch();
          }}
        >
          {running ? I18N["dsh.stop"] : I18N["dsh.launch"]}
        </PopupItem>
        <PopupItem
          icon={<InfoIcon size={16} />}
          onClick={() => {
            setManageAnchor(null);
            setTab("logs");
          }}
        >
          日志
        </PopupItem>
        <PopupItem
          icon={<Package2Icon size={16} />}
          onClick={() => {
            setManageAnchor(null);
            setExportOpen(true);
          }}
        >
          {I18N["modpack.export"]}
        </PopupItem>
        <PopupItem
          icon={<EditIcon size={16} />}
          onClick={() => {
            setManageAnchor(null);
            setRenameValue(instance.name);
            setRenameOpen(true);
          }}
        >
          {I18N["instance.manage.rename"]}
        </PopupItem>
        <PopupSep />
        <PopupItem
          danger
          icon={<DeleteIcon size={16} />}
          onClick={() => {
            setManageAnchor(null);
            setConfirmDelete(true);
          }}
        >
          {I18N["dsh.instance.remove"]}
        </PopupItem>
      </PopupMenu>

      {/* 重命名 */}
      <Dialog open={renameOpen} title={I18N["instance.manage.rename"]} onClose={() => !renameBusy && setRenameOpen(false)}>
        <div className="form-row">
          <span className="form-label">{I18N["dsh.instance.name"]}</span>
          <input
            className="input"
            autoFocus
            value={renameValue}
            onChange={(e) => setRenameValue(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !renameBusy) void doRename();
            }}
          />
        </div>
        <div className="dialog-actions">
          <button className="btn btn-text dim" onClick={() => setRenameOpen(false)} disabled={renameBusy}>
            {I18N["button.cancel"]}
          </button>
          <button className="btn btn-text" onClick={() => void doRename()} disabled={renameBusy}>
            {I18N["button.ok"]}
          </button>
        </div>
      </Dialog>

      <ConfirmDialog
        open={confirmStop}
        title={`${I18N["dsh.stop"]}「${instance.name}」`}
        message="停止后 dsh 进程将被终结，已打开的 dsh 页面将无法再访问。"
        confirmLabel={I18N["dsh.stop"]}
        danger
        busy={stopBusy}
        onConfirm={() => void doStop()}
        onCancel={() => setConfirmStop(false)}
      />

      <ConfirmDialog
        open={confirmDelete}
        title={I18N["dsh.instance.remove"]}
        message={I18N["dsh.instance.remove.confirm"].replace("%s", id)}
        confirmLabel={I18N["button.delete"]}
        danger
        busy={deleteBusy}
        onConfirm={() => void doDelete()}
        onCancel={() => setConfirmDelete(false)}
      />

      <ExportInstanceModal instance={instance} open={exportOpen} onClose={() => setExportOpen(false)} />
      <TerminalWindow target={terminalTarget} onClose={() => setTerminalTarget(null)} />
    </div>
  );
}
