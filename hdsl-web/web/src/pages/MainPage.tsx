import { useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api";
import { isTuiBrand } from "../brandKind";
import { launchBrandInstance, launchInstance, openDsh, stopBrandInstance, stopInstance } from "../actions";
import { LaunchPane } from "../components/LaunchPane";
import { BRAND_LABEL, useLaunchEntry, type LaunchEntry } from "../launchEntry";
import { BrandIcon } from "../components/BrandIcon";
import { LogView } from "../components/LogView";
import { MainSideBar, InstanceGraphic } from "../components/SideBar";
import { ProgressBar } from "../components/ProgressBar";
import { StateBadge } from "../components/InstanceIcon";
import { ContentCopyIcon } from "../components/icons";
import { useMobileLayout, useTickingUptime } from "../hooks";
import { I18N } from "../i18n";
import { findTaskForInstance, getState, refreshExternal, refreshInstances, toast, useAppState } from "../store";
import type { Instance } from "../types";
import { formatUptime, normState, parsePortFromUrl } from "../utils";
import { useWallpaperUrl } from "../wallpaper";
import { withBase } from "../base";

/// 背景壁纸：内置的或用户上传的，见 `wallpaper.ts`。选中的那张拉不到时回落默认。

/** 启动/安装中的进度浮层（HMCL 任务对话框风格：进度条 + 消息 + 实时日志）。 */
function LaunchProgress({ inst }: { inst: Instance }) {
  const s = useAppState();
  const st = normState(inst.state);
  const task = useMemo(() => {
    if (st !== "INSTALLING") return null;
    return findTaskForInstance(inst.id);
  }, [s.tasks, st, inst.id]);

  const title =
    st === "INSTALLING"
      ? I18N["dsh.versions.installing"].replace("%s", inst.version)
      : st === "STARTING"
        ? I18N["dsh.launch.launching"]
        : I18N["dsh.stopping"];

  const cancel = async () => {
    if (st === "INSTALLING" && task) {
      try {
        await api.cancelTask(task.id);
      } catch (e) {
        // 取消失败不阻塞
      }
    } else {
      try {
        await api.stop(inst.id);
      } catch (e) {
        // 取消失败不阻塞
      }
    }
  };

  return (
    <div className="launch-progress">
      <div className="card">
        <div className="progress-title">{title}</div>
        {st === "INSTALLING" && <ProgressBar fraction={task?.fraction} />}
        <div className="progress-msg">{task?.message ?? "…"}</div>
        <div style={{ maxHeight: 150, overflow: "hidden" }}>
          <LogView instanceId={inst.id} maxHeight={150} emptyHint="等待日志输出…" />
        </div>
        <div className="progress-actions">
          <button className="btn btn-text dim ripple-host" onClick={() => void cancel()}>
            {I18N["button.cancel"]}
          </button>
        </div>
      </div>
    </div>
  );
}

/** 运行中浮层：状态 + [打开 dsh]。 */
function RunningPane({ inst }: { inst: Instance }) {
  const port = inst.port ?? parsePortFromUrl(inst.url);
  return (
    <div className="launch-progress">
      <div className="card">
        <div className="progress-title">
          {inst.name}
          <span className="tag running" style={{ marginLeft: 8 }}>
            {I18N["dsh.instance.running"]}
          </span>
        </div>
        <div className="progress-msg">
          {inst.id} · {port != null ? `${I18N["dsh.instance.port"]} ${port}` : ""}
          {inst.url ? ` · ${withBase(inst.url)}` : ""}
        </div>
        <div className="progress-actions">
          <button className="btn btn-text ripple-host" onClick={() => openDsh(inst.id)}>
            {I18N["dsh.launch.running"]}
          </button>
        </div>
      </div>
    </div>
  );
}

/** 品牌/zcode 实例的启动中·运行中浮层：与 dsh 的 RunningPane 同款信息——
 *  品牌 Tag + 名称 + 状态、id · 端口 · 打开地址，外加 [打开] 与复制地址。
 *  品牌实例没有 token 概念，地址就是 /i/<id>/（面板会话门就是门）。 */
function RunningPaneBrand({ entry }: { entry: LaunchEntry }) {
  const ext = entry.external;
  const running = entry.state === "RUNNING";
  const port = ext?.port;
  // ext.url 已由后端带挂载点前缀（REST 边界统一加 base），兜底路径才 withBase
  const openUrl = ext?.url ?? withBase(`/i/${entry.id}/`);

  const doOpen = () => {
    if (entry.kind === "zcode") {
      void api.zcodeOpen(entry.id).then((r) => window.open(r.url, "_blank", "noopener"));  // r.url 已带挂载点前缀
      return;
    }
    window.open(openUrl, "_blank", "noopener");
  };
  const copyUrl = () => {
    void navigator.clipboard.writeText(openUrl).then(
      () => toast("success", "已复制"),
      () => toast("error", "复制失败"),
    );
  };

  return (
    <div className="launch-progress">
      <div className="card">
        <div className="progress-title">
          <span className="tag plain" style={{ marginRight: 6 }}>
            {entry.kind === "dsh" ? null : <BrandIcon brand={entry.kind} size={14} />}
            {BRAND_LABEL[entry.kind]}
          </span>
          {entry.name}
          <span className={`tag ${running ? "running" : "plain"}`} style={{ marginLeft: 8 }}>
            {running ? I18N["dsh.instance.running"] : I18N["dsh.launch.launching"]}
          </span>
        </div>
        {running && (
          <div className="progress-msg">
            {entry.id}
            {port ? ` · ${I18N["dsh.instance.port"]} ${port}` : ""}
            {` · ${openUrl}`}
          </div>
        )}
        <div className="progress-actions">
          {running && (
            <button
              className="icon-btn on-variant ripple-host"
              style={{ width: 26, height: 26, marginRight: 4 }}
              title="复制地址"
              onClick={copyUrl}
            >
              <ContentCopyIcon size={14} />
            </button>
          )}
          <button className="btn btn-text ripple-host" disabled={!running} onClick={doOpen}>
            {I18N["dsh.launch.running"]}
          </button>
        </div>
      </div>
    </div>
  );
}

/** 主页面：200px 侧边栏 + 壁纸内容区 + 右下启动面板（MainPage.java 结构）。
 *
 *  移动版式（≤760px 宽或 ≤520px 高，见 styles/mobile.css）下侧栏收成抽屉，
 *  壁纸上就空了 —— 所以这里补一张「当前实例」卡片，
 *  让远程启动/停止 dsh 在手机上一屏能完成（底部操作栏仍然贴底）。 */
/** 品牌实例状态文案（小写状态 → 中文；与 BrandsView/ZcodeSection 同款） */
const EXT_STATE_TEXT: Record<string, string> = {
  created: I18N["dsh.brand.state.created"],
  starting: I18N["dsh.brand.state.starting"],
  running: I18N["dsh.brand.state.running"],
  stopped: I18N["dsh.brand.state.stopped"],
  error: I18N["dsh.brand.state.error"],
};

export function MainPage() {
  const s = useAppState();
  const mobile = useMobileLayout();
  const wallpaperUrl = useWallpaperUrl();
  // 启动目标（底栏主按钮操作的那个实例，跨品牌）：主卡片跟它走，全屏只剩一个"当前实例"
  const entry = useLaunchEntry();

  useEffect(() => {
    void refreshInstances(!getState().instancesLoaded);
  }, []);

  // 跨品牌实例（kimi/opencode/zcode）没有 WS 推送，5 秒轮询一次；
  // launch/stop 动作内会立即再刷一次
  useEffect(() => {
    void refreshExternal();
    const timer = setInterval(() => void refreshExternal(), 5000);
    return () => clearInterval(timer);
  }, []);

  // dsh 目标的进度/运行浮层；无选中目标时退回旧行为：第一个运行中的 dsh，否则第一个 dsh
  const overlayInst = useMemo(() => {
    if (entry) return entry.kind === "dsh" ? entry.dsh : null;
    const running = s.instances.find((i) => normState(i.state) === "RUNNING");
    return running ?? s.instances[0] ?? null;
  }, [entry, s.instances]);

  // 品牌/zcode 目标的浮层：启动中与运行中都显示（运行中带端口与打开地址）；
  // 无选中目标且没有 dsh 实例时，退回第一个运行中的品牌实例
  const brandOverlay = useMemo<LaunchEntry | null>(() => {
    if (entry) {
      if (entry.kind === "dsh") return null;
      return entry.state === "RUNNING" || entry.state === "STARTING" ? entry : null;
    }
    if (overlayInst) return null;
    const ext = s.external.find((e) => normState(e.state) === "RUNNING");
    if (!ext) return null;
    return { id: ext.id, name: ext.name, state: normState(ext.state), kind: ext.brand, dsh: null, external: ext };
  }, [entry, overlayInst, s.external]);

  // 手机主卡片：目标 = 品牌实例 → 品牌卡；目标 = dsh → 现有卡；无目标 → 空态
  const card = entry ? (
    entry.kind === "dsh" && entry.dsh ? (
      <MobileHome inst={entry.dsh} />
    ) : (
      <MobileHomeBrand entry={entry} />
    )
  ) : overlayInst ? (
    <MobileHome inst={overlayInst} />
  ) : (
    <MobileHomeEmpty />
  );

  const st = overlayInst ? normState(overlayInst.state) : "";
  const showProgress = overlayInst && (st === "INSTALLING" || st === "STARTING" || st === "STOPPING");
  const showRunning = overlayInst && st === "RUNNING";

  return (
    <div className="main-page">
      <MainSideBar />
      <div className="wallpaper-bg" style={{ backgroundImage: `url(${wallpaperUrl})` }} />
      {mobile && card}
      <LaunchPane />
      {showProgress && overlayInst && <LaunchProgress inst={overlayInst} />}
      {showRunning && overlayInst && <RunningPane inst={overlayInst} />}
      {brandOverlay && <RunningPaneBrand entry={brandOverlay} />}
    </div>
  );
}

/** 手机端品牌实例卡：品牌 Tag + 实例名 + 状态徽标 + 版本；操作 = 启动/停止、打开、管理。
 *  打开地址规则与品牌面板一致：品牌实例没有 token 概念，就是 /i/<id>/（会话门就是门）。 */
function MobileHomeBrand({ entry }: { entry: LaunchEntry }) {
  const nav = useNavigate();
  const ext = entry.external;
  const state = entry.state;
  const running = state === "RUNNING";
  const busy = state === "STARTING" || state === "STOPPING";
  // ext.url 已由后端带挂载点前缀（REST 边界统一加 base），兜底路径才 withBase
  const openUrl = ext?.url ?? withBase(`/i/${entry.id}/`);
  const manageTarget = `/instances?view=${isTuiBrand(entry.kind) ? "tui" : "brands"}&brand=${entry.kind}`;

  const doOpen = () => {
    if (entry.kind === "zcode") {
      void api.zcodeOpen(entry.id).then((r) => window.open(r.url, "_blank", "noopener"));  // r.url 已带挂载点前缀
      return;
    }
    window.open(openUrl, "_blank", "noopener");
  };
  const doAction = () => {
    if (running) void stopBrandInstance(entry.kind as "kimi" | "opencode" | "claude-web" | "octop" | "hermes" | "qwen" | "mimo" | "minimax" | "zcode", entry.id);
    else void launchBrandInstance(entry.kind as "kimi" | "opencode" | "claude-web" | "octop" | "hermes" | "qwen" | "mimo" | "minimax" | "zcode", { id: entry.id, name: entry.name });
  };
  const copyUrl = () => {
    void navigator.clipboard.writeText(openUrl).then(
      () => toast("success", "已复制"),
      () => toast("error", "复制失败"),
    );
  };

  return (
    <div className="mobile-home">
      <div className="card">
        <div className="comp-row">
          <span className="comp-label">{I18N["instance"]}</span>
          <span className="comp-value">
            <span className="tag plain" style={{ marginRight: 2 }}>
              {entry.kind === "dsh" ? null : <BrandIcon brand={entry.kind} size={14} />}
              {BRAND_LABEL[entry.kind]}
            </span>
            {entry.name}
            <span className={`tag ${running ? "running" : "plain"}`}>
              {EXT_STATE_TEXT[state.toLowerCase()] ?? state}
            </span>
          </span>
        </div>
        {ext?.version && (
          <div className="comp-row">
            <span className="comp-label">版本</span>
            <span className="comp-value">{ext.version}</span>
          </div>
        )}
        {running && (
          <div className="comp-row">
            <span className="comp-label">地址</span>
            <span className="comp-value url-ellipsis" title={openUrl}>{openUrl}</span>
            <button className="icon-btn on-variant ripple-host" style={{ width: 26, height: 26 }} title="复制地址" onClick={copyUrl}>
              <ContentCopyIcon size={14} />
            </button>
          </div>
        )}
      </div>
      <div className="card" style={{ display: "flex", gap: 6, padding: 6 }}>
        <button
          className="tool-btn ripple-host"
          style={{ flex: 1, justifyContent: "center" }}
          disabled={busy}
          onClick={doAction}
        >
          {running ? I18N["dsh.stop"] : busy ? I18N["dsh.launch.launching"] : I18N["dsh.launch"]}
        </button>
        <button
          className="tool-btn ripple-host"
          style={{ flex: 1, justifyContent: "center" }}
          disabled={!running}
          onClick={doOpen}
        >
          {I18N["dsh.launch.running"]}
        </button>
        <button
          className="tool-btn ripple-host"
          style={{ flex: 1, justifyContent: "center" }}
          onClick={() => nav(manageTarget)}
        >
          {I18N["settings.game.management"]}
        </button>
      </div>
    </div>
  );
}

/** 手机端主页内容：当前实例的状态与三个主要动作（启动/停止、打开 dsh、管理）。 */
function MobileHome({ inst }: { inst: Instance }) {
  const nav = useNavigate();
  const st = normState(inst.state);
  const running = st === "RUNNING";
  const busy = st === "INSTALLING" || st === "STARTING" || st === "STOPPING";
  const uptime = useTickingUptime(inst.id, inst.uptimeSec);
  const port = inst.port ?? parsePortFromUrl(inst.url);

  return (
    <div className="mobile-home">
      <div className="card">
        <div className="comp-row">
          <span className="comp-label">{I18N["instance"]}</span>
          <span className="comp-value">
            <InstanceGraphic inst={inst} size={24} />
            {inst.name}
            <StateBadge state={inst.state} />
          </span>
        </div>
        <div className="comp-row">
          <span className="comp-label">版本</span>
          <span className="comp-value">{inst.version}</span>
        </div>
        {port != null && (
          <div className="comp-row">
            <span className="comp-label">{I18N["dsh.instance.port"]}</span>
            <span className="comp-value">{port}</span>
          </div>
        )}
        {running && uptime != null && (
          <div className="comp-row">
            <span className="comp-label">运行时长</span>
            <span className="comp-value">{formatUptime(uptime)}</span>
          </div>
        )}
      </div>
      <div className="card" style={{ display: "flex", gap: 6, padding: 6 }}>
        <button
          className="tool-btn ripple-host"
          style={{ flex: 1, justifyContent: "center" }}
          disabled={busy}
          onClick={() => (running ? stopInstance(inst) : launchInstance(inst))}
        >
          {running ? I18N["dsh.stop"] : I18N["dsh.launch"]}
        </button>
        <button
          className="tool-btn ripple-host"
          style={{ flex: 1, justifyContent: "center" }}
          disabled={!running}
          onClick={() => openDsh(inst.id)}
        >
          {I18N["dsh.launch.running"]}
        </button>
        <button
          className="tool-btn ripple-host"
          style={{ flex: 1, justifyContent: "center" }}
          onClick={() => nav(`/instances/${inst.id}`)}
        >
          {I18N["settings.game.management"]}
        </button>
      </div>
    </div>
  );
}

/** 手机端主页：一个实例都没有时的入口（桌面版式下这些内容在侧栏里）。 */
function MobileHomeEmpty() {
  const nav = useNavigate();
  return (
    <div className="mobile-home">
      <div className="card">
        <div className="empty-state">
          <span style={{ fontSize: 16 }}>{I18N["dsh.instance.none"]}</span>
          <span className="empty-sub">{I18N["dsh.launch.no_instance.hint"]}</span>
          <button className="btn btn-raised ripple-host" onClick={() => nav("/instances/new")}>
            {I18N["dsh.instance.install"]}
          </button>
        </div>
      </div>
    </div>
  );
}
