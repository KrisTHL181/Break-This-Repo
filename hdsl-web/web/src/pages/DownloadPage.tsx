import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api, isNotImplemented } from "../api";
import { Dialog } from "../components/Dialog";
import { ProgressBar } from "../components/ProgressBar";
import {
  ArrowForwardIcon,
  DownloadIcon,
  ErrorIcon,
  ExtensionFillIcon,
  ExtensionIcon,
  Package2FillIcon,
  Package2Icon,
  PublicIcon,
  RefreshIcon,
  StadiaControllerFillIcon,
  StadiaControllerIcon,
  TextureIcon,
} from "../components/icons";
import { PopupItem, PopupMenu } from "../components/PopupMenu";
import { SubSideBar } from "../components/SideBar";
import { VersionListIcon } from "../components/VersionListIcon";
import { I18N } from "../i18n";
import { loadVersions, refreshInstances, seedTasks, toast, useAppState } from "../store";
import type { ExportRecord, MarketPack, PluginCatalogItem, VersionInfo } from "../types";
import {
  channelLabel,
  errMsg,
  formatBytes,
  formatCount,
  formatDateTime,
  isSuccessTaskState,
  isTerminalTaskState,
  normState,
} from "../utils";
import { DSH_RELEASES } from "../constants";

type DownloadTab = "game" | "modpack" | "plugin" | "skill";

/** 下载页（DownloadPage.java 结构）：NEW INSTANCE（新实例/整合包）+ ADDONS（插件/技能包）。 */
export function DownloadPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get("tab");
  const tab: DownloadTab =
    tabParam === "modpack" || tabParam === "plugin" || tabParam === "skill" ? tabParam : "game";
  const setTab = (t: DownloadTab) => setSearchParams({ tab: t }, { replace: true });

  return (
    <div className="page-with-sidebar">
      <SubSideBar
        activeKey={tab}
        onSelect={(k) => setTab(k as DownloadTab)}
        sections={[
          {
            title: I18N["download.new_game"],
            items: [
              {
                key: "game",
                title: I18N["dsh.download.instance"],
                icon: <StadiaControllerIcon size={20} />,
                iconActive: <StadiaControllerFillIcon size={20} />,
              },
              {
                key: "modpack",
                title: I18N["dsh.download.packs"],
                icon: <Package2Icon size={20} />,
                iconActive: <Package2FillIcon size={20} />,
              },
            ],
          },
          {
            title: I18N["download.content"],
            items: [
              {
                key: "plugin",
                title: I18N["dsh.download.plugins"],
                icon: <ExtensionIcon size={20} />,
                iconActive: <ExtensionFillIcon size={20} />,
              },
              {
                key: "skill",
                title: I18N["dsh.instance.skills"],
                icon: <TextureIcon size={20} />,
              },
            ],
          },
        ]}
      />
      <div
        className="page-content"
        key={tab}
        style={{ animation: "slide-up-fade-in 400ms var(--ease)" }}
      >
        {tab === "game" && <GameTab />}
        {tab === "modpack" && <ModpackTab />}
        {tab === "plugin" && <PluginTab />}
        {tab === "skill" && <SkillTab />}
      </div>
    </div>
  );
}

/* ---------------------------------------------------------------------------
 * 新实例：/api/versions，行点击进安装向导
 * ------------------------------------------------------------------------- */

function GameTab() {
  const nav = useNavigate();
  const s = useAppState();
  const [query, setQuery] = useState("");
  const [channel, setChannel] = useState("");

  useEffect(() => {
    void loadVersions();
  }, []);

  const channels = useMemo(() => {
    const set = new Set<string>();
    for (const v of s.versions ?? []) set.add(v.channel);
    return Array.from(set);
  }, [s.versions]);

  const filtered = useMemo(() => {
    let list = s.versions ?? [];
    if (channel) list = list.filter((v) => v.channel === channel);
    const q = query.trim().toLowerCase();
    if (q) list = list.filter((v) => v.version.toLowerCase().includes(q));
    return list;
  }, [s.versions, channel, query]);

  return (
    <>
      <div className="card filter-bar">
        <span className="filter-label">名称</span>
        <input
          className="input"
          placeholder={I18N["search.hint.chinese"]}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <span className="filter-label">类型</span>
        <select className="input" value={channel} onChange={(e) => setChannel(e.target.value)}>
          <option value="">全部</option>
          {channels.map((c) => (
            <option key={c} value={c}>
              {channelLabel(c)}
            </option>
          ))}
        </select>
        <button
          className="btn btn-raised ripple-host"
          disabled={s.versionsLoading}
          onClick={() => void loadVersions(true)}
        >
          <RefreshIcon size={15} />
          {I18N["button.refresh"]}
        </button>
      </div>

      <div className="card" style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", padding: 0 }}>
        <div className="list-body scroll-hover">
          {s.versionsLoading && !s.versions ? (
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 10, padding: 48 }}>
              <span className="spinner" />
              <span>{I18N["dsh.versions.loading"]}</span>
            </div>
          ) : s.versionsError && !s.versions ? (
            <div className="empty-state">
              <ErrorIcon size={30} />
              <span>{I18N["dsh.versions.load_failed"]}</span>
              <span className="empty-sub">{s.versionsError}</span>
              <button className="btn btn-outline ripple-host" onClick={() => void loadVersions(true)}>
                {I18N["button.refresh"]}
              </button>
            </div>
          ) : filtered.length === 0 ? (
            <div className="empty-state">
              <span style={{ fontSize: 20 }}>{I18N["dsh.versions.available.empty"]}</span>
            </div>
          ) : (
            filtered.map((v: VersionInfo, idx: number) => (
              <div key={v.version}>
                <VersionRow
                  v={v}
                  onClick={() => nav(`/instances/new?version=${encodeURIComponent(v.version)}`)}
                />
                {idx < filtered.length - 1 && <div className="divider" style={{ marginLeft: 12 }} />}
              </div>
            ))
          )}
        </div>
      </div>
    </>
  );
}

function VersionRow({ v, onClick }: { v: VersionInfo; onClick: () => void }) {
  return (
    <div className="tlli ripple-host" style={{ cursor: "pointer", padding: "8px 12px" }} onClick={onClick}>
      <span className="tlli-graphic">
        <VersionListIcon size={32} />
      </span>
      <span className="tlli-text">
        <span className="tlli-title">
          {v.version}
          <span className="tag">{channelLabel(v.channel)}</span>
        </span>
        <span className="tlli-subtitle">{formatDateTime(v.time)}</span>
      </span>
      <span className="row-actions">
        {/* 上游发布页。桌面版的版本行上就是这颗地球（`DownloadPage.java:423`，文案「打开
            release 页面」），网页版此前缺一个出路——想看看这个版本改了什么只能自己去搜。 */}
        <button
          className="icon-btn on-variant ripple-host"
          title="打开 release 页面"
          onClick={(e) => {
            e.stopPropagation();
            window.open(`${DSH_RELEASES}${v.version}`, "_blank", "noopener");
          }}
        >
          <PublicIcon size={18} />
        </button>
        <button
          className="icon-btn on-variant ripple-host"
          title={I18N["button.install"]}
          disabled={!v.installable}
          onClick={(e) => {
            e.stopPropagation();
            onClick();
          }}
        >
          <ArrowForwardIcon size={18} />
        </button>
      </span>
    </div>
  );
}

/* ---------------------------------------------------------------------------
 * 整合包：/api/packs/market + 导出记录
 * ------------------------------------------------------------------------- */

function ModpackTab() {
  const nav = useNavigate();
  const s = useAppState();
  const [packs, setPacks] = useState<MarketPack[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");

  const [installTarget, setInstallTarget] = useState<MarketPack | null>(null);
  const [installName, setInstallName] = useState("");
  const [installBusy, setInstallBusy] = useState(false);
  const [installTaskId, setInstallTaskId] = useState<string | null>(null);
  const [installError, setInstallError] = useState<string | null>(null);
  const [installCancelling, setInstallCancelling] = useState(false);
  // 用户按过取消：任务终态是失败时，说「安装已取消」而不是后端那句英文（cancelled），
  // 也不再把它当成一次故障来报。
  const installCancelAskedRef = useRef(false);
  const installHandledRef = useRef<string | null>(null);

  const [exports, setExports] = useState<ExportRecord[] | null>(null);
  const [exportsError, setExportsError] = useState<string | null>(null);
  const [menuAnchor, setMenuAnchor] = useState<{ el: HTMLElement; pack: MarketPack } | null>(null);

  const loadMarket = useCallback(async (refresh = false) => {
    if (refresh) setRefreshing(true);
    else setLoading(true);
    setError(null);
    try {
      const r = await api.packsMarket(refresh);
      setPacks(r.packs);
    } catch (e) {
      setError(isNotImplemented(e) ? "后端尚未支持整合包市场" : errMsg(e));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  const loadExports = useCallback(async () => {
    setExportsError(null);
    try {
      const r = await api.exports();
      setExports(r.exports ?? []);
    } catch (e) {
      setExportsError(isNotImplemented(e) ? "后端尚未支持导出记录" : errMsg(e));
    }
  }, []);

  useEffect(() => {
    void loadMarket();
    void loadExports();
    void refreshInstances(false);
  }, [loadMarket, loadExports]);

  const installTask = installTaskId ? s.tasks[installTaskId] : undefined;

  // 市场安装任务终态：成功 → toast + 跳新实例详情
  useEffect(() => {
    if (!installTask || !installTaskId || installHandledRef.current === installTaskId) return;
    if (!isTerminalTaskState(installTask.state)) return;
    installHandledRef.current = installTaskId;
    void (async () => {
      if (isSuccessTaskState(installTask.state)) {
        let iid = installTask.result?.instanceId;
        if (!iid) {
          try {
            const list = await api.tasks();
            iid = list.find((t) => t.id === installTaskId)?.result?.instanceId;
          } catch {
            // 拿不到则只提示
          }
        }
        toast("success", `整合包「${installTarget?.name ?? ""}」安装完成`);
        setInstallTarget(null);
        setInstallTaskId(null);
        await refreshInstances(false);
        if (iid) nav(`/instances/${iid}`);
      } else {
        // 用户按过取消：这一条失败是「我停的」，不是故障——报「安装已取消」，而不是后端那句
        // 英文（cancelled），让人以为出了什么问题。
        setInstallError(installCancelAskedRef.current
          ? "安装已取消"
          : (installTask.error ?? "安装任务未完成"));
        installCancelAskedRef.current = false;
        setInstallTaskId(null);
      }
    })();
  }, [installTask, installTaskId, installTarget, nav]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return packs ?? [];
    return (packs ?? []).filter((p) =>
      [p.name, p.author, p.description, p.descriptionZh ?? ""].join("\n").toLowerCase().includes(q),
    );
  }, [packs, query]);

  const sortedExports = useMemo(
    () => (exports ?? []).slice().sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
    [exports],
  );

  // 取消安装。这里只发请求与给即时反馈——任务真正的终态（成功跳转、失败、还是「已取消」）
  // 由上面那个 effect 统一收尾。后端收到取消会去停掉正在跑的 pnpm / node，所以此刻说的是
  // 「已请求取消」而不是「已取消」。
  const cancelInstall = async () => {
    if (!installTaskId) return;
    installCancelAskedRef.current = true;
    setInstallCancelling(true);
    try {
      await api.cancelTask(installTaskId);
      toast("info", "已请求取消，正在停止安装…");
    } catch (e) {
      installCancelAskedRef.current = false;
      setInstallError(errMsg(e));
    } finally {
      setInstallCancelling(false);
    }
  };

  const doInstall = async () => {
    if (!installTarget) return;
    setInstallBusy(true);
    setInstallError(null);
    try {
      const r = await api.installPack({
        packId: installTarget.id,
        version: installTarget.version,
        name: installName.trim() || undefined,
      });
      setInstallTaskId(r.taskId);
      void seedTasks();
    } catch (e) {
      setInstallError(isNotImplemented(e) ? "后端尚未支持整合包安装" : errMsg(e));
    } finally {
      setInstallBusy(false);
    }
  };

  return (
    <>
      <div className="card filter-bar">
        <span className="filter-label">名称</span>
        <input
          className="input"
          placeholder={I18N["search.hint.chinese"]}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button
          className="btn btn-raised ripple-host"
          disabled={loading || refreshing}
          onClick={() => void loadMarket(true)}
        >
          <RefreshIcon size={15} />
          {I18N["button.refresh"]}
        </button>
        <button className="btn btn-outline ripple-host" onClick={() => nav("/install/modpack")}>
          {I18N["install.modpack"]}
        </button>
      </div>

      <div className="card" style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", padding: 0 }}>
        <div className="list-body scroll-hover">
          {loading && !packs ? (
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 10, padding: 48 }}>
              <span className="spinner" />
              <span>正在拉取整合包市场，首次加载可能较慢…</span>
            </div>
          ) : error && !packs ? (
            <div className="empty-state">
              <span style={{ fontSize: 20 }}>{error}</span>
              <button className="btn btn-outline ripple-host" onClick={() => void loadMarket()}>
                重试
              </button>
            </div>
          ) : filtered.length === 0 ? (
            <div className="empty-state">
              <span style={{ fontSize: 20 }}>{query ? I18N["search.no_results_found"] : "市场暂无整合包"}</span>
              <span className="empty-sub">可通过上方「安装整合包」上传 .dspack 文件安装</span>
            </div>
          ) : (
            filtered.map((p: MarketPack, idx: number) => (
              <div key={p.id}>
                <div className="tlli ripple-host" style={{ cursor: "default", padding: "8px 12px" }}>
                  <span className="tlli-graphic">
                    <PackAvatar pack={p} />
                  </span>
                  <span className="tlli-text">
                    <span className="tlli-title">
                      {p.name}
                      {p.version && <span className="tag">v{p.version}</span>}
                    </span>
                    <span className="tlli-subtitle">
                      {p.author}
                      {p.downloads != null ? ` · ${formatCount(p.downloads)} 次下载` : ""}
                      {p.updatedAt != null ? ` · ${formatDateTime(p.updatedAt)}` : ""}
                    </span>
                  </span>
                  <span className="row-actions">
                    {(p.avatarUrl || p.description) && (
                      <button
                        className="icon-btn on-variant ripple-host"
                        title={I18N["dsh.market.package_page"]}
                        onClick={(e) => setMenuAnchor({ el: e.currentTarget, pack: p })}
                      >
                        <PublicIcon size={18} />
                      </button>
                    )}
                    <button
                      className="icon-btn on-variant ripple-host"
                      title={I18N["button.install"]}
                      onClick={() => {
                        setInstallTarget(p);
                        setInstallName(p.name);
                        setInstallError(null);
                        setInstallTaskId(null);
                        installHandledRef.current = null;
                      }}
                    >
                      <ArrowForwardIcon size={18} />
                    </button>
                  </span>
                </div>
                {idx < filtered.length - 1 && <div className="divider" style={{ marginLeft: 12 }} />}
              </div>
            ))
          )}
        </div>
      </div>

      {/* 导出记录 */}
      <div className="card" style={{ maxHeight: 200, display: "flex", flexDirection: "column", gap: 4 }}>
        <div style={{ display: "flex", alignItems: "center" }}>
          <span style={{ fontSize: 14, fontWeight: 600 }}>导出记录</span>
          <span style={{ flex: 1 }} />
          <button className="tool-btn ripple-host" style={{ height: 28 }} onClick={() => void loadExports()}>
            <RefreshIcon size={15} />
            {I18N["button.refresh"]}
          </button>
        </div>
        <div className="list-body scroll-hover">
          {exportsError && !exports ? (
            <div style={{ padding: 8, fontSize: 12 }}>{exportsError}</div>
          ) : sortedExports.length === 0 ? (
            <div style={{ padding: 8, fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
              {exports ? "暂无导出文件，可在实例「管理」菜单中导出整合包" : "正在加载…"}
            </div>
          ) : (
            sortedExports.map((x) => (
              <div key={x.filename} style={{ display: "flex", alignItems: "center", gap: 10, padding: "6px 4px", fontSize: 12 }}>
                <span className="mono" style={{ flex: 1, minWidth: 0, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={x.filename}>
                  {x.filename}
                </span>
                <span style={{ color: "var(--monet-on-surface-variant)" }}>{formatBytes(x.sizeBytes)}</span>
                <span style={{ color: "var(--monet-on-surface-variant)" }}>{formatDateTime(x.createdAt)}</span>
                <a className="btn btn-outline ripple-host" style={{ height: 26, padding: "0 10px", fontSize: 12 }} href={api.exportDownloadUrl(x.filename)} download>
                  <DownloadIcon size={13} />
                  下载
                </a>
              </div>
            ))
          )}
        </div>
      </div>

      {/* 安装弹窗 */}
      <Dialog
        open={installTarget !== null}
        title={`${I18N["button.install"]}「${installTarget?.name ?? ""}」`}
        onClose={() => {
          if (!installTaskId) setInstallTarget(null);
        }}
      >
        {installTaskId ? (
          <div style={{ display: "flex", flexDirection: "column", gap: 10, minWidth: 340 }}>
            <ProgressBar fraction={installTask?.fraction} />
            <div style={{ fontSize: 13 }}>
              {installTask?.message ?? "正在下载并安装整合包，完成后将自动跳转到新实例…"}
            </div>
            {/* 进行中也要有条出路。此前这里没有任何按钮，而 onClose 在安装期间拒绝关闭——
                点下安装之后就只能看着，装错了版本也没有退路。取消由后端停掉正在跑的
                pnpm / node（POST /api/tasks/{id}/cancel），终态由上面的 effect 收尾。 */}
            <div className="dialog-actions">
              <button
                className="btn btn-text dim"
                disabled={installCancelling}
                onClick={() => void cancelInstall()}
              >
                {installCancelling ? "正在取消…" : I18N["button.cancel"]}
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className="form-row">
              <span className="form-label">{I18N["dsh.instance.name"]}</span>
              <input
                className="input"
                value={installName}
                onChange={(e) => setInstallName(e.target.value)}
                placeholder={installTarget?.name}
              />
            </div>
            <p style={{ fontSize: 13, lineHeight: 1.7 }}>
              将以该整合包创建新实例{installTarget?.version ? `（v${installTarget.version}）` : ""}
              ，安装完成后自动跳转到新实例详情页。
            </p>
            {installError && <div className="hint error">{installError}</div>}
            <div className="dialog-actions">
              <button className="btn btn-text dim" onClick={() => setInstallTarget(null)} disabled={installBusy}>
                {I18N["button.cancel"]}
              </button>
              <button className="btn btn-text" disabled={installBusy} onClick={() => void doInstall()}>
                {installBusy ? "提交中…" : I18N["button.install"]}
              </button>
            </div>
          </>
        )}
      </Dialog>

      <PopupMenu open={menuAnchor !== null} anchor={menuAnchor?.el ?? null} onClose={() => setMenuAnchor(null)}>
        <PopupItem
          onClick={() => {
            const p = menuAnchor?.pack;
            setMenuAnchor(null);
            if (p) toast("info", p.descriptionZh || p.description || p.name);
          }}
        >
          {I18N["dsh.market.package_page"]}
        </PopupItem>
      </PopupMenu>
    </>
  );
}

function PackAvatar({ pack, size = 32 }: { pack: MarketPack; size?: number }) {
  const [failed, setFailed] = useState(false);
  const url = pack.avatarUrl;
  const show = !!url && /^(https?:|\/|data:)/.test(url) && !failed;
  if (!show) {
    return (
      <span
        style={{
          width: size,
          height: size,
          borderRadius: 6,
          background: "var(--monet-primary-container)",
          color: "var(--monet-on-primary-container)",
          display: "inline-flex",
          alignItems: "center",
          justifyContent: "center",
          fontWeight: "bold",
        }}
      >
        {pack.name.slice(0, 1).toUpperCase()}
      </span>
    );
  }
  return <img src={url} alt="" style={{ width: size, height: size, borderRadius: 6, objectFit: "cover" }} onError={() => setFailed(true)} />;
}

/* ---------------------------------------------------------------------------
 * 插件：/api/plugins/catalog，安装到实例
 * ------------------------------------------------------------------------- */

function PluginTab() {
  const [searchParams] = useSearchParams();
  const presetInstance = searchParams.get("instance") ?? "";
  const s = useAppState();

  const [catalog, setCatalog] = useState<PluginCatalogItem[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("");
  const [installTarget, setInstallTarget] = useState<PluginCatalogItem | null>(null);
  const [installInstance, setInstallInstance] = useState("");
  const [installBusy, setInstallBusy] = useState(false);

  const load = useCallback(async (refresh = false) => {
    if (refresh) setRefreshing(true);
    else setLoading(true);
    setError(null);
    try {
      const r = await api.pluginCatalog(refresh);
      setCatalog(r.plugins);
    } catch (e) {
      setError(isNotImplemented(e) ? "后端尚未支持插件市场" : errMsg(e));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void load();
    void refreshInstances(false);
  }, [load]);

  const installable = useMemo(
    () => s.instances.filter((i) => normState(i.state) !== "NOT_INSTALLED"),
    [s.instances],
  );

  const categories = useMemo(() => {
    const set = new Set<string>();
    for (const p of catalog ?? []) if (p.category) set.add(p.category);
    return Array.from(set).sort();
  }, [catalog]);

  const filtered = useMemo(() => {
    let list = catalog ?? [];
    if (category) list = list.filter((p) => p.category === category);
    const q = query.trim().toLowerCase();
    if (q) {
      list = list.filter((p) =>
        [p.name, p.description, p.descriptionZh ?? "", p.npm].join("\n").toLowerCase().includes(q),
      );
    }
    return list;
  }, [catalog, category, query]);

  const openInstall = (p: PluginCatalogItem) => {
    const valid = (x: string) => installable.some((i) => i.id === x);
    setInstallInstance(
      valid(presetInstance)
        ? presetInstance
        : valid(installInstance)
          ? installInstance
          : (installable[0]?.id ?? ""),
    );
    setInstallTarget(p);
  };

  const doInstall = async () => {
    if (!installTarget || !installInstance) return;
    setInstallBusy(true);
    try {
      const spec = installTarget.npm || installTarget.name;
      const r = await api.installPlugins(installInstance, [spec]);
      void seedTasks();
      const inst = installable.find((i) => i.id === installInstance);
      toast(
        "success",
        `已提交安装任务${inst ? `，可在「${inst.name}」详情页的插件页查看进度` : ""}（任务 ${r.taskId}）`,
      );
      setInstallTarget(null);
    } catch (e) {
      if (isNotImplemented(e)) toast("info", "后端尚未支持插件安装");
      else toast("error", `安装失败：${errMsg(e)}`);
    } finally {
      setInstallBusy(false);
    }
  };

  return (
    <>
      <div className="card filter-bar">
        <span className="filter-label">名称</span>
        <input
          className="input"
          placeholder={I18N["search.hint.chinese"]}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <span className="filter-label">类型</span>
        <select className="input" value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">全部</option>
          {categories.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
        <button className="btn btn-raised ripple-host" disabled={loading || refreshing} onClick={() => void load(true)}>
          <RefreshIcon size={15} />
          {I18N["button.refresh"]}
        </button>
      </div>

      <div className="card" style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", padding: 0 }}>
        <div className="list-body scroll-hover">
          {loading && !catalog ? (
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 10, padding: 48 }}>
              <span className="spinner" />
              <span>正在拉取插件目录，首次加载可能较慢…</span>
            </div>
          ) : error && !catalog ? (
            <div className="empty-state">
              <span style={{ fontSize: 20 }}>{error}</span>
              <button className="btn btn-outline ripple-host" onClick={() => void load()}>
                重试
              </button>
            </div>
          ) : filtered.length === 0 ? (
            <div className="empty-state">
              <span style={{ fontSize: 20 }}>{query || category ? I18N["search.no_results_found"] : "目录暂无插件"}</span>
            </div>
          ) : (
            filtered.map((p: PluginCatalogItem, idx: number) => (
              <div key={`${p.npm || p.name}@${p.version}-${idx}`}>
                <div className="tlli" style={{ cursor: "default", padding: "8px 12px" }}>
                  <span className="tlli-graphic">
                    <ExtensionFillIcon size={32} />
                  </span>
                  <span className="tlli-text">
                    <span className="tlli-title">
                      {p.name}
                      <span className="tag">{p.version}</span>
                      {p.category && <span className="tag plain">{p.category}</span>}
                    </span>
                    <span className="tlli-subtitle">
                      {p.owner} · {p.descriptionZh || p.description || "暂无描述"}
                    </span>
                  </span>
                  <span className="row-actions">
                    <button
                      className="icon-btn on-variant ripple-host"
                      title={p.url ? I18N["dsh.market.package_page"] : p.npm}
                      disabled={!p.url}
                      onClick={() => p.url && window.open(p.url, "_blank", "noopener")}
                    >
                      <PublicIcon size={18} />
                    </button>
                    <button
                      className="icon-btn on-variant ripple-host"
                      title={I18N["button.install"]}
                      onClick={() => {
                        if (installable.length === 0) {
                          toast("info", "没有可安装插件的实例（实例需已安装 dsh）");
                          return;
                        }
                        openInstall(p);
                      }}
                    >
                      <ArrowForwardIcon size={18} />
                    </button>
                  </span>
                </div>
                {idx < filtered.length - 1 && <div className="divider" style={{ marginLeft: 12 }} />}
              </div>
            ))
          )}
        </div>
      </div>

      <Dialog
        open={installTarget !== null}
        title={`${I18N["button.install"]}「${installTarget?.name ?? ""}」`}
        onClose={() => setInstallTarget(null)}
      >
        {installable.length === 0 ? (
          <p>没有可安装插件的实例。请先在实例详情页安装 dsh 运行时。</p>
        ) : (
          <>
            <div className="form-row">
              <span className="form-label">目标实例</span>
              <select className="input" value={installInstance} onChange={(e) => setInstallInstance(e.target.value)}>
                {installable.map((i) => (
                  <option key={i.id} value={i.id}>
                    {i.name}（{i.version}）
                  </option>
                ))}
              </select>
            </div>
            <p style={{ fontSize: 13, lineHeight: 1.7 }}>
              将安装 <code>{installTarget?.npm || installTarget?.name}</code>
              ，安装进度可在实例详情页的插件页中查看。
            </p>
          </>
        )}
        <div className="dialog-actions">
          <button className="btn btn-text dim" onClick={() => setInstallTarget(null)} disabled={installBusy}>
            {I18N["button.cancel"]}
          </button>
          <button
            className="btn btn-text"
            disabled={installBusy || !installInstance || installable.length === 0}
            onClick={() => void doInstall()}
          >
            {installBusy ? "提交中…" : I18N["button.install"]}
          </button>
        </div>
      </Dialog>
    </>
  );
}

/* ---------------------------------------------------------------------------
 * 技能包：技能安装走实例内，此处列出实例入口
 * ------------------------------------------------------------------------- */

function SkillTab() {
  const nav = useNavigate();
  const s = useAppState();

  useEffect(() => {
    void refreshInstances(!s.instancesLoaded);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <>
      <div className="card">
        <div className="hint info" style={{ margin: 2 }}>
          技能包安装到实例内：选择一个实例进入其「技能包」页，输入来源（github:owner/repo 或 URL）即可安装。
        </div>
      </div>
      <div className="card" style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", padding: 0 }}>
        <div className="list-body scroll-hover">
          {s.instances.length === 0 ? (
            <div className="empty-state">
              <span style={{ fontSize: 20 }}>{I18N["dsh.instance.empty"]}</span>
              <span className="empty-sub">先在「新实例」页安装一个实例</span>
            </div>
          ) : (
            s.instances.map((i, idx) => (
              <div key={i.id}>
                <div
                  className="tlli ripple-host"
                  style={{ cursor: "pointer", padding: "8px 12px" }}
                  onClick={() => nav(`/instances/${i.id}?tab=skills`)}
                >
                  <span className="tlli-graphic">
                    <TextureIcon size={32} />
                  </span>
                  <span className="tlli-text">
                    <span className="tlli-title">{i.name}</span>
                    <span className="tlli-subtitle">
                      {i.id} · {i.version}
                    </span>
                  </span>
                  <span className="row-actions">
                    <button className="icon-btn on-variant ripple-host" title={I18N["button.install"]}>
                      <ArrowForwardIcon size={18} />
                    </button>
                  </span>
                </div>
                {idx < s.instances.length - 1 && <div className="divider" style={{ marginLeft: 12 }} />}
              </div>
            ))
          )}
        </div>
      </div>
    </>
  );
}
