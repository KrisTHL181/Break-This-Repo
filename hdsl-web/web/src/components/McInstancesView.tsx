import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, type McInstance, type McVersion } from "../api";
import { refreshMcFeature, useMcFeature } from "../mcFeature";
import {
  AddIcon,
  ArrowForwardIcon,
  CancelIcon,
  CloseIcon,
  DeployedCodeIcon,
  InfoIcon,
  RefreshIcon,
  RocketLaunchIcon,
  SearchIcon,
  SettingsFillIcon,
} from "./icons";
import { McCreateWizard } from "./McCreateWizard";
import { McThirdPartyWizard } from "./McThirdPartyWizard";
import { McThirdPartySettings } from "./McThirdPartySettings";
import { ProgressBar } from "./ProgressBar";
import { findTaskForInstance, seedTasks, toast, useAppState } from "../store";
import { errMsg, isTerminalTaskState, mcCoreLabel, mcStateLabel, normState } from "../utils";

/// MC 服务端实例视图，嵌在「实例列表」页里原地切换（`?view=mc`）。
///
/// 和品牌实例同一种东西：面板托管的另一套进程。所以它不该是一个独立页面——从实例列表
/// 点进来是换内容区，不是跳走再点「返回」。`/mc` 现在只是重定向到这里。
///
/// 版式与 dsh 实例列表一致：工具栏一张卡（刷新 / 计数 / 新建），列表一张卡、卡内
/// `.list-body` 真的滚动。手机上沿用同一套响应式规则，不需要另写一份。
export function McInstancesView({ scope = "vanilla" }: { scope?: "vanilla" | "thirdparty" }) {
  const nav = useNavigate();
  const feature = useMcFeature();
  const taskState = useAppState();
  const thirdParty = scope === "thirdparty";
  const [instances, setInstances] = useState<McInstance[]>([]);
  const [versions, setVersions] = useState<McVersion[]>([]);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [wizardOpen, setWizardOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [searchMode, setSearchMode] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      // 版本清单要联网：拉不到只让「创建/装版本」退化，不该把整个列表变成错误页。
      const list = await api.mc.instances(scope);
      setInstances(list.instances);
      if (!thirdParty) {
        api.mc.versions()
          .then((catalog) => setVersions(catalog.versions))
          .catch(() => setVersions([]));
      }
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void refreshMcFeature();
  }, []);

  useEffect(() => {
    if (feature?.enabled) {
      void load();
      // 创建向导可能在别的标签页/弹窗里启动了安装；列表挂载时先认领一次任务。
      void seedTasks();
    }
  }, [feature?.enabled, scope]);

  /// 每个实例正在跑的安装任务：进度条与「安装中」状态都从这里来。
  const activeInstalls = useMemo(() => {
    const map = new Map<string, ReturnType<typeof findTaskForInstance>>();
    for (const task of Object.values(taskState.tasks)) {
      if (!task || task.kind !== "mc-install" || isTerminalTaskState(task.state) || !task.instance) continue;
      const current = map.get(task.instance);
      if (!current || current.seenAt < task.seenAt) map.set(task.instance, task);
    }
    return map;
  }, [taskState.tasks]);

  const activeInstallKey = useMemo(
    () => [...activeInstalls.keys()].sort().join(","),
    [activeInstalls],
  );

  // WS 任务事件是主路径；轮询兜底，防 WebSocket 断线时进度停在原地。
  useEffect(() => {
    if (!activeInstallKey) return;
    const timer = window.setInterval(() => { void seedTasks(); }, 3000);
    return () => window.clearInterval(timer);
  }, [activeInstallKey]);

  // 安装结束后刷新列表：版本号、错误状态都可能变了。
  const previousActive = useRef("");
  useEffect(() => {
    if (previousActive.current && !activeInstallKey) void load();
    previousActive.current = activeInstallKey;
  }, [activeInstallKey]);

  // 起停是服务端推进的异步状态，有过渡态时轻量轮询。
  const transitioning = instances.some(
    (instance) => instance.state === "STARTING" || instance.state === "STOPPING",
  );
  useEffect(() => {
    if (!transitioning) return;
    const timer = window.setInterval(() => {
      void api.mc.instances()
        .then((list) => setInstances(list.instances))
        .catch(() => undefined);
    }, 2000);
    return () => window.clearInterval(timer);
  }, [transitioning]);

  const toggle = async (instance: McInstance) => {
    const running = instance.state === "RUNNING" || instance.state === "STARTING";
    setBusy(true);
    try {
      if (running) {
        await api.mc.stop(instance.id);
        toast("info", `正在停止「${instance.name}」，等世界存盘`);
      } else {
        await api.mc.start(instance.id);
        toast("info", `正在启动「${instance.name}」`);
      }
      await load();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const running = instances.filter((instance) => normState(instance.state) === "RUNNING").length;
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return instances;
    return instances.filter(
      (instance) =>
        instance.name.toLowerCase().includes(q) ||
        instance.id.toLowerCase().includes(q) ||
        instance.version.toLowerCase().includes(q),
    );
  }, [instances, query]);

  if (!feature?.enabled) {
    return (
      <div className="card" style={{ padding: 16, display: "flex", flexDirection: "column", gap: 12 }}>
        <h2 style={{ margin: 0 }}>{thirdParty ? "MC 第三方服务器实例" : "MC 原版服务器实例"}</h2>
        <p className="dim" style={{ margin: 0, lineHeight: 1.7 }}>
          还没开启。开关在「设置 → 品牌实例」里的「MC 服务端」卡片：需要开发者密码，
          读完 Mojang 的 EULA 与服务条款后开启。开启之后，入口才会出现在这一栏下面。
        </p>
        <div>
          <button className="btn btn-raised" onClick={() => nav("/settings?tab=brands")}>
            去品牌实例管理
          </button>
        </div>
      </div>
    );
  }

  // 第三方核心的第二道关：未开启时这一栏不该"存在"，即便用户手里有旧链接。
  if (thirdParty && feature.thirdParty !== true) {
    return (
      <div className="card" style={{ padding: 16, display: "flex", flexDirection: "column", gap: 12 }}>
        <h2 style={{ margin: 0 }}>MC 第三方服务器实例</h2>
        <p className="dim" style={{ margin: 0, lineHeight: 1.7 }}>
          第三方核心还没开启。它是在「MC 原版服务器实例」之上的第二道关：先开启原版功能，
          再在「设置 → 品牌实例」里同意第三方核心条款。开启之前，这里没有可用的服务端。
        </p>
        <div>
          <button className="btn btn-raised" onClick={() => nav("/settings?tab=brands")}>
            去开启第三方核心
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8, flex: 1, minHeight: 0 }}>
      {searchMode ? (
        <div className="card toolbar-row" style={{ flexWrap: "nowrap" }}>
          <SearchIcon size={18} style={{ color: "var(--monet-on-surface-variant)" }} />
          <input
            className="input"
            style={{ flex: 1 }}
            autoFocus
            placeholder="按名字、编号或版本找"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Escape") {
                setQuery("");
                setSearchMode(false);
              }
            }}
          />
          <button
            className="icon-btn on-variant ripple-host"
            onClick={() => {
              setQuery("");
              setSearchMode(false);
            }}
          >
            <CloseIcon size={18} />
          </button>
        </div>
      ) : (
        <div className="card toolbar-row">
          <button
            className="tool-btn ripple-host"
            disabled={loading}
            title="刷新"
            onClick={() => void load()}
          >
            <RefreshIcon size={20} />
            刷新
          </button>
          <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
            {instances.length} 个{thirdParty ? "第三方" : "原版"}服务器
            {running > 0 ? ` · ${running} 个运行中` : ""}
          </span>
          <span className="spacer" />
          {instances.length > 0 && (
            <button
              className="tool-btn ripple-host"
              title="搜索"
              onClick={() => setSearchMode(true)}
            >
              <SearchIcon size={20} />
              搜索
            </button>
          )}
          <button
            className="tool-btn ripple-host"
            title="下载源与配额"
            onClick={() => setSettingsOpen(true)}
          >
            <SettingsFillIcon size={20} />
            全局设置
          </button>
          <button
            className="btn btn-raised"
            disabled={busy || feature.locked || (!thirdParty && versions.length === 0)}
            onClick={() => setWizardOpen(true)}
            title={!thirdParty && versions.length === 0 ? "版本清单拉不到（需要联网）" : undefined}
          >
            <AddIcon size={16} /> {thirdParty ? "新建第三方服务器" : "新建原版服务器"}
          </button>
        </div>
      )}

      {feature.locked && (
        <div className="card" style={{ padding: "10px 12px", fontSize: 13, lineHeight: 1.6 }}>
          功能正在冷却：按下「停止 MC 功能」后一小时内不能启服、不能新建。倒计时与撤销在
          「设置 → 品牌实例」。
        </div>
      )}

      <div
        className="card"
        style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", padding: 0 }}
      >
        <div className="list-body scroll-hover">
          {filtered.length === 0 ? (
            <div className="empty-state">
              <span>{query ? "没有匹配的服务器" : thirdParty ? "还没有第三方服务器" : "还没有原版服务器"}</span>
              <span className="empty-sub">
                {query
                  ? "换个关键词，或清掉搜索框。"
                  : thirdParty
                    ? "选一个核心与版本建一个：Paper / Purpur / Folia / Fabric / Forge / NeoForge，走完向导就能启动。"
                    : "选一个 Mojang 版本建一个：参数 → 版本 → 存储位置 → 下载服务端，走完就能启动。"}
              </span>
            </div>
          ) : (
            filtered.map((instance) => {
              const state = normState(instance.state);
              const up = state === "RUNNING" || state === "STARTING";
              const busyState = state === "STARTING" || state === "STOPPING";
              const installTask = activeInstalls.get(instance.id) ?? null;
              return (
                <div
                  key={instance.id}
                  className="tlli ripple-host"
                  style={{ cursor: "pointer", padding: "8px 12px" }}
                  onClick={() => nav(`/mc/${encodeURIComponent(instance.id)}`)}
                >
                  <span className="tlli-graphic">
                    <DeployedCodeIcon size={32} />
                  </span>
                  <span className="tlli-text">
                    <span className="tlli-title">
                      {instance.name}
                      <span className="tag plain">{mcCoreLabel(instance.core)}</span>
                      <span className="tag plain">
                        {instance.thirdParty ? "第三方" : "官方"}
                      </span>
                      <span
                        className={`tag ${
                          state === "RUNNING" ? "running" : state === "FAILED" ? "failed" : "plain"
                        }`}
                      >
                        {mcStateLabel(instance.state)}
                      </span>
                    </span>
                    <span className="tlli-subtitle">
                      {instance.version} · 端口 {instance.port} · RCON {instance.rconPort} ·{" "}
                      {instance.memoryMb} MB
                      {instance.storage === "data" ? " · data 目录" : ""}
                    </span>
                    {installTask && (
                      <span
                        className="tlli-subtitle"
                        style={{ display: "flex", alignItems: "center", gap: 8 }}
                        onClick={(e) => e.stopPropagation()}
                      >
                        <span style={{ flex: 1, minWidth: 60 }}>
                          <ProgressBar fraction={installTask.fraction} />
                        </span>
                        <span
                          style={{
                            whiteSpace: "nowrap",
                            maxWidth: "60%",
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                          }}
                        >
                          {installTask.message || "正在下载服务端…"}
                          {typeof installTask.fraction === "number" && installTask.fraction >= 0
                            ? ` ${Math.round(installTask.fraction * 100)}%`
                            : ""}
                        </span>
                        <button
                          className="btn btn-text"
                          style={{ height: 24, flex: "none" }}
                          onClick={() => {
                            void api.cancelTask(installTask.id)
                              .then(() => {
                                toast("info", "已请求取消安装");
                                void seedTasks();
                              })
                              .catch((e) => toast("error", errMsg(e)));
                          }}
                        >
                          取消
                        </button>
                      </span>
                    )}
                    {/* 失败要说原因：只说「失败」等于没说，原因藏在详情页的事件流里。 */}
                    {state === "FAILED" && instance.error && (
                      <span className="tlli-subtitle" style={{ color: "var(--monet-error)" }}>
                        {instance.error}
                      </span>
                    )}
                  </span>
                  <span className="row-actions" onClick={(e) => e.stopPropagation()}>
                    <button
                      className="icon-btn on-variant ripple-host"
                      title={
                        installTask
                          ? "服务端还在下载/安装，装完才能启动"
                          : feature.locked
                            ? "锁定期间不能启服"
                            : up
                              ? "停止（先存盘再关）"
                              : "启动"
                      }
                      disabled={busy || feature.locked || busyState || installTask !== null}
                      onClick={() => void toggle(instance)}
                    >
                      {up ? <CancelIcon size={18} /> : <RocketLaunchIcon size={18} />}
                    </button>
                    <button
                      className="icon-btn on-variant ripple-host"
                      title="打开控制台与设置"
                      onClick={() => nav(`/mc/${encodeURIComponent(instance.id)}`)}
                    >
                      <ArrowForwardIcon size={18} />
                    </button>
                  </span>
                </div>
              );
            })
          )}
        </div>
      </div>

      <p
        style={{
          margin: 0,
          padding: "0 4px",
          fontSize: 12,
          color: "var(--monet-on-surface-variant)",
          lineHeight: 1.7,
        }}
      >
        <InfoIcon size={14} style={{ verticalAlign: "-2px", marginRight: 4 }} />
        {thirdParty ? (
          <>
            第三方核心由各上游项目发布，面板只负责下载、校验与运行；
            <b> 下载源可在「全局设置」里调整</b>，源都不可达时可以手动导入核心 jar。
          </>
        ) : (
          <>
            客户端连的是游戏端口，不是面板；默认只建议局域网或加防火墙。
            <b> 服务端程序不随本项目分发</b>，首次安装时从 Mojang 官方下载。
          </>
        )}
      </p>

      {thirdParty ? (
        <McThirdPartyWizard
          open={wizardOpen}
          onClose={() => setWizardOpen(false)}
          onCreated={() => void load()}
          onOpenSettings={() => setSettingsOpen(true)}
        />
      ) : (
        <McCreateWizard
          open={wizardOpen}
          versions={versions}
          thirdPartyCores={false}
          onClose={() => setWizardOpen(false)}
          onCreated={() => void load()}
        />
      )}
      {thirdParty && (
        <McThirdPartySettings open={settingsOpen} onClose={() => setSettingsOpen(false)} />
      )}
    </div>
  );
}
