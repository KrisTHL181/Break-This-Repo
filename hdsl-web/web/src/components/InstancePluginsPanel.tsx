import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, ApiError, isNotImplemented } from "../api";
import { ConfirmDialog, Dialog } from "./Dialog";
import { FileDropInput } from "./FileDropInput";
import { I18N } from "../i18n";
import { ApprovalDialog } from "./ApprovalDialog";
import { ProgressBar } from "./ProgressBar";
import {
  AddIcon,
  CloseIcon,
  DeleteIcon,
  DownloadIcon,
  ExtensionIcon,
  FolderOpenIcon,
  InfoIcon,
  Package2Icon,
  RefreshIcon,
  SearchIcon,
  SelectAllIcon,
} from "./icons";
import { seedTasks, toast, useAppState } from "../store";
import type { Instance, InstalledPlugin, TaskRec } from "../types";
import { errMsg, isTerminalTaskState, isWaitingApproval, normState } from "../utils";

const TGZ_RE = /\.(tgz|tar\.gz)$/i;
const MAX_PLUGIN_SIZE = 50 * 1024 * 1024;

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
 * 实例插件面板（InstancePage 插件 tab，样式照 6e7b21d4 截图）：
 * 工具栏（刷新/从文件安装/从来源安装/打开目录/搜索）+ checkbox 行
 * （32px 图标 + 两行文字 + 版本徽章 + 右侧信息/删除 30px 圆钮）。
 */
export function InstancePluginsPanel({ instance }: { instance: Instance }) {
  const nav = useNavigate();
  const s = useAppState();
  const id = instance.id;

  const [plugins, setPlugins] = useState<InstalledPlugin[] | null>(null);
  const [bundles, setBundles] = useState<InstalledPlugin[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<string[]>([]);
  const [searchMode, setSearchMode] = useState(false);

  const [removeTargets, setRemoveTargets] = useState<InstalledPlugin[]>([]);
  const [removeBusy, setRemoveBusy] = useState(false);

  const [uploadOpen, setUploadOpen] = useState(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [uploadBusy, setUploadBusy] = useState(false);

  const [sourceOpen, setSourceOpen] = useState(false);
  const [source, setSource] = useState("");
  const [sourceBusy, setSourceBusy] = useState(false);

  const [infoTarget, setInfoTarget] = useState<InstalledPlugin | null>(null);

  /** 本面板发起的任务 id，用于进度展示与审批定位 */
  const [tracked, setTracked] = useState<string[]>([]);

  const installed = normState(instance.state) !== "NOT_INSTALLED";

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const r = await api.instancePlugins(id);
      setPlugins(r.plugins ?? []);
      setBundles(r.bundles ?? []);
    } catch (e) {
      setError(isNotImplemented(e) ? "后端尚未支持插件管理" : errMsg(e));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    if (installed) void load();
    void seedTasks();
  }, [id, installed, load]);

  const track = (taskId: string) => {
    setTracked((t) => [taskId, ...t.filter((x) => x !== taskId)].slice(0, 9));
    void seedTasks();
  };

  // 任务视图：本面板发起的任务 + 本实例的插件任务。
  // 归属按任务自带的 instance 字段精确匹配——kind 只是 "plugin-install"
  // 这类名字，kind.includes(id) 永远匹配不上（store.findTaskForInstance
  // 的注释记载过同一处推翻，这个面板当时没跟上）。
  const tasks: TaskRec[] = useMemo(() => {
    const all = Object.values(s.tasks);
    const mine = all.filter(
      (t) =>
        tracked.includes(t.id) ||
        (t.instance === id && t.kind.toLowerCase().includes("plugin")),
    );
    return mine.sort((a, b) => b.seenAt - a.seenAt).slice(0, 5);
  }, [s.tasks, tracked, id]);

  const approvalTask = useMemo(
    () => tasks.find((t) => isWaitingApproval(t.state)) ?? null,
    [tasks],
  );

  // 任务到达终态后刷新插件列表（pending 标记随之更新）
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

  const doRemove = async () => {
    if (removeTargets.length === 0) return;
    setRemoveBusy(true);
    try {
      const r = await api.removePlugins(
        id,
        removeTargets.map((p) => p.name),
      );
      track(r.taskId);
      toast("info", `已提交卸载任务（${r.taskId}）`);
      setRemoveTargets([]);
      setSelected([]);
    } catch (e) {
      if (isNotImplemented(e)) toast("info", "后端尚未支持插件移除");
      else toast("error", `移除失败：${errMsg(e)}`);
    } finally {
      setRemoveBusy(false);
    }
  };

  const doUpload = async () => {
    if (!uploadFile) return;
    if (!TGZ_RE.test(uploadFile.name)) {
      setUploadError("仅支持 .tgz / .tar.gz 格式的插件包");
      return;
    }
    if (uploadFile.size > MAX_PLUGIN_SIZE) {
      setUploadError("文件过大（上限 50MB）");
      return;
    }
    setUploadError(null);
    setUploadBusy(true);
    try {
      const r = await api.uploadLocalPlugin(id, uploadFile);
      track(r.taskId);
      toast("info", `已提交本地插件安装任务（${r.taskId}）`);
      setUploadOpen(false);
      setUploadFile(null);
    } catch (e) {
      if (isNotImplemented(e)) toast("info", "后端尚未支持本地插件上传");
      else if (e instanceof ApiError && e.status === 409)
        toast("error", "该实例尚未安装 dsh 运行时，请先完成安装");
      else toast("error", `上传失败：${errMsg(e)}`);
    } finally {
      setUploadBusy(false);
    }
  };

  const doInstallSource = async () => {
    const spec = source.trim();
    if (!spec) return;
    setSourceBusy(true);
    try {
      const r = await api.installPlugins(id, [spec]);
      track(r.taskId);
      toast("info", `已提交安装任务（${r.taskId}）`);
      setSourceOpen(false);
      setSource("");
    } catch (e) {
      if (isNotImplemented(e)) toast("info", "后端尚未支持插件安装");
      else toast("error", `安装失败：${errMsg(e)}`);
    } finally {
      setSourceBusy(false);
    }
  };

  const filtered = useMemo(() => {
    let list = plugins ?? [];
    const q = query.trim().toLowerCase();
    if (q) list = list.filter((p) => p.name.toLowerCase().includes(q));
    return list;
  }, [plugins, query]);

  const allSelected = filtered.length > 0 && selected.length === filtered.length;

  if (!installed) {
    return (
      <div className="card" style={{ margin: 10 }}>
        <p className="hint info" style={{ margin: 2 }}>
          该实例尚未安装 dsh 运行时，安装完成后才能管理插件
        </p>
      </div>
    );
  }

  const selectionMode = selected.length > 0;

  return (
    <div className="page-content" style={{ margin: 0, background: "transparent", gap: 8 }}>
      {/* 工具栏：jfx-tool-bar-button 高 37 圆角 5；选中行后变选择工具栏 */}
      <div className="card toolbar-row">
        {selectionMode ? (
          <>
            <button
              className="tool-btn ripple-host"
              onClick={() => {
                const targets = filtered.filter((p) => selected.includes(p.name) && !p.bundled);
                if (targets.length === 0) {
                  toast("info", "内置插件不可卸载");
                  return;
                }
                setRemoveTargets(targets);
              }}
            >
              <DeleteIcon size={20} />
              {I18N["button.delete"]}
            </button>
            <button
              className="tool-btn ripple-host"
              onClick={() => setSelected(allSelected ? [] : filtered.map((p) => p.name))}
            >
              <SelectAllIcon size={20} />
              {allSelected ? "取消全选" : I18N["button.select_all"]}
            </button>
            <button className="tool-btn ripple-host" onClick={() => setSelected([])}>
              <CloseIcon size={20} />
              取消
            </button>
          </>
        ) : (
          <>
            <button
              className="tool-btn ripple-host"
              disabled={loading}
              title={I18N["button.refresh"]}
              onClick={() => void load()}
            >
              <RefreshIcon size={20} />
              {I18N["button.refresh"]}
            </button>
            <button
              className="tool-btn ripple-host"
              title={I18N["dsh.instance.plugins.add"]}
              onClick={() => {
                setUploadOpen(true);
                setUploadFile(null);
                setUploadError(null);
              }}
            >
              <AddIcon size={20} />
              {I18N["dsh.instance.plugins.add"]}
            </button>
            <button
              className="tool-btn ripple-host"
              title={I18N["dsh.instance.plugins.add.url"]}
              onClick={() => {
                setSourceOpen(true);
                setSource("");
              }}
            >
              <DownloadIcon size={20} />
              {I18N["dsh.instance.plugins.add.url"]}
            </button>
            <button
              className="tool-btn ripple-host"
              title={I18N["dsh.instance.plugins.to_market"]}
              onClick={() => nav(`/download?tab=plugin&instance=${id}`)}
            >
              <FolderOpenIcon size={20} />
              {I18N["dsh.instance.plugins.to_market"]}
            </button>
          </>
        )}
        <span className="spacer" />
        {searchMode ? (
          <input
            className="input"
            autoFocus
            style={{ width: 220 }}
            placeholder={I18N["search"]}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onBlur={() => {
              if (!query) setSearchMode(false);
            }}
            onKeyDown={(e) => {
              if (e.key === "Escape") {
                setQuery("");
                setSearchMode(false);
              }
            }}
          />
        ) : (
          <button className="tool-btn ripple-host" title={I18N["search"]} onClick={() => setSearchMode(true)}>
            <SearchIcon size={20} />
            {I18N["search"]}
          </button>
        )}
      </div>

      {/* 任务进度 */}
      {tasks.length > 0 && (
        <div className="card" style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          {tasks.map((t) => {
            const terminal = isTerminalTaskState(t.state);
            return (
              <div key={t.id} style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span className={`tag ${taskStateClass(t.state)}`}>{taskStateLabel(t.state)}</span>
                  <span className="mono" style={{ fontSize: 11, opacity: 0.7 }}>
                    {t.kind}
                  </span>
                </div>
                {!terminal && <ProgressBar fraction={t.fraction} />}
                {t.message && <div style={{ fontSize: 12 }}>{t.message}</div>}
                {t.error && <div className="hint error">{t.error}</div>}
                {isWaitingApproval(t.state) && (
                  <div className="hint warning">等待你审批构建脚本后才能继续安装</div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* 插件列表 */}
      <div className="card" style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", padding: 0 }}>
        <div className="list-body scroll-hover">
          {loading && !plugins ? (
            <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 10, padding: 32 }}>
              <span className="spinner" />
              <span>正在获取插件列表…</span>
            </div>
          ) : error && !plugins ? (
            <div className="empty-state">
              <span>{error}</span>
              <button className="btn btn-outline ripple-host" onClick={() => void load()}>
                {I18N["button.refresh"]}
              </button>
            </div>
          ) : filtered.length === 0 ? (
            <div className="empty-state">
              <span style={{ fontSize: 20 }}>
                {query ? I18N["search.no_results_found"] : I18N["dsh.instance.plugins.empty"]}
              </span>
            </div>
          ) : (
            filtered.map((p, idx) => {
              const checked = selected.includes(p.name);
              return (
                <div key={p.name}>
                  <div
                    className="tlli"
                    style={{ cursor: "default", padding: "8px 12px" }}
                    onClick={() =>
                      setSelected((cur) =>
                        checked ? cur.filter((x) => x !== p.name) : [...cur, p.name],
                      )
                    }
                  >
                    <label className="input-check" onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() =>
                          setSelected((cur) =>
                            checked ? cur.filter((x) => x !== p.name) : [...cur, p.name],
                          )
                        }
                      />
                    </label>
                    <span className="tlli-graphic">
                      <Package2Icon size={32} />
                    </span>
                    <span className="tlli-text">
                      <span className="tlli-title">
                        {p.name}
                        {p.pending && <span className="tag warn">待生效</span>}
                        {p.bundled && <span className="tag plain">内置</span>}
                      </span>
                      <span className="tlli-subtitle">
                        {I18N["dsh.instance.plugins.source"].replace("%s", p.name)}
                      </span>
                    </span>
                    <span className="tag" style={{ flex: "none" }}>
                      {p.version}
                    </span>
                    <span className="row-actions" onClick={(e) => e.stopPropagation()}>
                      <button
                        className="icon-btn on-variant ripple-host"
                        title={I18N["dsh.instance.details"]}
                        onClick={() => setInfoTarget(p)}
                      >
                        <InfoIcon size={18} />
                      </button>
                      {!p.bundled && (
                        <button
                          className="icon-btn on-variant ripple-host"
                          title={I18N["button.delete"]}
                          onClick={() => setRemoveTargets([p])}
                        >
                          <DeleteIcon size={18} />
                        </button>
                      )}
                    </span>
                  </div>
                  {idx < filtered.length - 1 && <div className="divider" style={{ marginLeft: 12 }} />}
                </div>
              );
            })
          )}
          {bundles.length > 0 && (
            <div style={{ padding: "8px 12px 4px", fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
              运行时捆绑
            </div>
          )}
          {bundles.map((p) => (
            <div className="tlli" key={`bundle-${p.name}`} style={{ cursor: "default", padding: "8px 12px" }}>
              <span className="tlli-graphic">
                <ExtensionIcon size={32} />
              </span>
              <span className="tlli-text">
                <span className="tlli-title">
                  {p.name}
                  <span className="tag plain">内置</span>
                </span>
                <span className="tlli-subtitle">{p.name}（npm 包）</span>
              </span>
              <span className="tag" style={{ flex: "none" }}>
                {p.version}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* 卸载确认 */}
      <ConfirmDialog
        open={removeTargets.length > 0}
        title={
          removeTargets.length === 1
            ? I18N["dsh.instance.plugins.remove"]
            : `卸载 ${removeTargets.length} 个插件`
        }
        message={
          removeTargets.length === 1
            ? I18N["dsh.instance.plugins.remove.confirm"].replace("%s", removeTargets[0]?.name ?? "")
            : `确定从该配置中卸载选中的 ${removeTargets.length} 个插件吗？`
        }
        confirmLabel={I18N["button.delete"]}
        danger
        busy={removeBusy}
        onConfirm={() => void doRemove()}
        onCancel={() => setRemoveTargets([])}
      />

      {/* 本地上传 */}
      <Dialog
        open={uploadOpen}
        title={I18N["dsh.instance.plugins.add"]}
        onClose={() => !uploadBusy && setUploadOpen(false)}
      >
        <div className="form-row">
          <span className="form-label">DSH 插件包 (*.tgz)</span>
          <FileDropInput
            file={uploadFile}
            onFile={(f) => {
              setUploadFile(f);
              setUploadError(f && !TGZ_RE.test(f.name) ? "仅支持 .tgz / .tar.gz 格式的插件包" : null);
            }}
            accept=".tgz,.tar.gz,application/gzip"
            hint="选择或拖入 .tgz / .tar.gz 插件包"
            disabled={uploadBusy}
          />
        </div>
        {uploadError && <div className="form-error">{uploadError}</div>}
        <div className="dialog-actions">
          <button className="btn btn-text dim" onClick={() => setUploadOpen(false)} disabled={uploadBusy}>
            {I18N["button.cancel"]}
          </button>
          <button className="btn btn-text" disabled={!uploadFile || uploadBusy} onClick={() => void doUpload()}>
            {uploadBusy ? "上传中…" : I18N["button.install"]}
          </button>
        </div>
      </Dialog>

      {/* 从来源安装 */}
      <Dialog
        open={sourceOpen}
        title={I18N["dsh.instance.plugins.add.url"]}
        onClose={() => !sourceBusy && setSourceOpen(false)}
      >
        <div className="form-row">
          <span className="form-label">npm 包名（如 @scope/name 或 name@version）</span>
          <input
            className="input"
            autoFocus
            placeholder="@linxin666/dsh-remote-web-ui"
            value={source}
            onChange={(e) => setSource(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && source.trim() && !sourceBusy) void doInstallSource();
            }}
          />
        </div>
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", marginBottom: 8 }}>
          将以 npm 包形式安装到该实例，安装进度见上方任务列表。
        </p>
        <div className="dialog-actions">
          <button className="btn btn-text dim" onClick={() => setSourceOpen(false)} disabled={sourceBusy}>
            {I18N["button.cancel"]}
          </button>
          <button
            className="btn btn-text"
            disabled={!source.trim() || sourceBusy}
            onClick={() => void doInstallSource()}
          >
            {sourceBusy ? "提交中…" : I18N["button.install"]}
          </button>
        </div>
      </Dialog>

      {/* 插件信息 */}
      <Dialog
        open={infoTarget !== null}
        title={infoTarget?.name ?? ""}
        onClose={() => setInfoTarget(null)}
      >
        {infoTarget && (
          <div style={{ display: "flex", flexDirection: "column", gap: 4, minWidth: 320 }}>
            <div className="comp-row">
              <span className="comp-label">版本</span>
              <span className="comp-value">
                <span className="tag">{infoTarget.version}</span>
                {infoTarget.bundled && <span className="tag plain">内置</span>}
                {infoTarget.pending && <span className="tag warn">待生效</span>}
              </span>
            </div>
            <div className="comp-row">
              <span className="comp-label">来源</span>
              <span className="comp-value mono" style={{ fontSize: 12 }}>
                {I18N["dsh.instance.plugins.source"].replace("%s", infoTarget.name)}
              </span>
            </div>
            <div className="dialog-actions">
              <button className="btn btn-text" onClick={() => setInfoTarget(null)}>
                {I18N["button.ok"]}
              </button>
            </div>
          </div>
        )}
      </Dialog>

      <ApprovalDialog task={approvalTask} />
    </div>
  );
}
