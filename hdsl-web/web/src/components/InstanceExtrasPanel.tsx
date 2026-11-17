import { useCallback, useEffect, useRef, useState } from "react";
import { api, isNotImplemented } from "../api";
import { ConfirmDialog } from "./Dialog";
import { ProgressBar } from "./ProgressBar";
import { I18N } from "../i18n";
import {
  ArchiveIcon,
  DeleteIcon,
  DownloadIcon,
  Package2FillIcon,
  RefreshIcon,
  SelectAllIcon,
  TextureIcon,
} from "./icons";
import { seedTasks, toast, useAppState } from "../store";
import type { Instance, SessionInfo, SkillInfo, WorkspaceInfo } from "../types";
import { errMsg, formatBytes, formatDateTime, isSuccessTaskState, isTerminalTaskState, normState } from "../utils";

// ---------------------------------------------------------------------------
// 会话 tab：会话列表（多选导出） + 工作区列表（只读）
// ---------------------------------------------------------------------------

export function InstanceSessionsPanel({ instance }: { instance: Instance }) {
  const id = instance.id;
  const tasks = useAppState().tasks;
  const installed = normState(instance.state) !== "NOT_INSTALLED";

  const [sessions, setSessions] = useState<SessionInfo[] | null>(null);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [sessionsError, setSessionsError] = useState<string | null>(null);
  const [workspaces, setWorkspaces] = useState<WorkspaceInfo[] | null>(null);
  const [workspacesError, setWorkspacesError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string[]>([]);
  const [exportBusy, setExportBusy] = useState(false);
  const [taskId, setTaskId] = useState<string | null>(null);
  const handledRef = useRef<string | null>(null);

  const loadSessions = useCallback(async () => {
    setSessionsLoading(true);
    setSessionsError(null);
    try {
      const r = await api.instanceSessions(id);
      setSessions(r.sessions ?? []);
    } catch (e) {
      setSessionsError(isNotImplemented(e) ? "后端尚未支持会话管理" : errMsg(e));
    } finally {
      setSessionsLoading(false);
    }
  }, [id]);

  const loadWorkspaces = useCallback(async () => {
    setWorkspacesError(null);
    try {
      const r = await api.instanceWorkspaces(id);
      setWorkspaces(r.workspaces ?? []);
    } catch (e) {
      setWorkspacesError(isNotImplemented(e) ? "后端尚未支持工作区列表" : errMsg(e));
    }
  }, [id]);

  useEffect(() => {
    if (!installed) return;
    void loadSessions();
    void loadWorkspaces();
    void seedTasks();
  }, [id, installed, loadSessions, loadWorkspaces]);

  const task = taskId ? tasks[taskId] : undefined;

  useEffect(() => {
    if (!task || !taskId || handledRef.current === taskId) return;
    if (!isTerminalTaskState(task.state)) return;
    handledRef.current = taskId;
    if (isSuccessTaskState(task.state)) {
      toast("success", "会话导出完成，可在「下载 → 整合包」的导出记录中下载");
      setSelected([]);
    } else {
      toast("error", `会话导出失败：${task.error ?? "任务未完成"}`);
    }
    setTaskId(null);
  }, [task, taskId]);

  const toggle = (sid: string) => {
    setSelected((cur) => (cur.includes(sid) ? cur.filter((x) => x !== sid) : [...cur, sid]));
  };

  const allSelected = (sessions ?? []).length > 0 && selected.length === (sessions ?? []).length;

  const doExport = async () => {
    if (selected.length === 0) return;
    setExportBusy(true);
    try {
      const r = await api.exportSessions(id, selected);
      setTaskId(r.taskId);
      void seedTasks();
      toast("info", `已提交会话导出任务（${r.taskId}）`);
    } catch (e) {
      if (isNotImplemented(e)) toast("info", "后端尚未支持会话导出");
      else toast("error", `导出失败：${errMsg(e)}`);
    } finally {
      setExportBusy(false);
    }
  };

  if (!installed) {
    return (
      <div className="card" style={{ margin: 10 }}>
        <p className="hint info" style={{ margin: 2 }}>
          该实例尚未安装 dsh 运行时，安装完成后才能查看会话
        </p>
      </div>
    );
  }

  return (
    <div className="page-content" style={{ margin: 0, background: "transparent", gap: 8 }}>
      {task && (
        <div className="card" style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          <div style={{ fontSize: 14, fontWeight: 600 }}>会话导出</div>
          <ProgressBar fraction={task.fraction} />
          <div style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
            {task.message ?? "正在导出所选会话…"}
          </div>
        </div>
      )}

      <div className="card toolbar-row">
        <button className="tool-btn ripple-host" disabled={sessionsLoading} onClick={() => void loadSessions()}>
          <RefreshIcon size={20} />
          {I18N["button.refresh"]}
        </button>
        <button
          className="tool-btn ripple-host"
          disabled={selected.length === 0 || exportBusy}
          onClick={() => void doExport()}
        >
          <DownloadIcon size={20} />
          {I18N["dsh.session.pack.export.selected"]}
          {selected.length > 0 ? `（${selected.length}）` : ""}
        </button>
        <span className="spacer" />
        <button
          className="tool-btn ripple-host"
          disabled={!sessions || sessions.length === 0}
          onClick={() => setSelected(allSelected ? [] : (sessions ?? []).map((x) => x.id))}
        >
          <SelectAllIcon size={20} />
          {allSelected ? "取消全选" : I18N["button.select_all"]}
        </button>
      </div>

      <div className="card" style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", padding: 0 }}>
        <div className="list-body scroll-hover">
          {sessionsLoading && !sessions ? (
            <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 10, padding: 32 }}>
              <span className="spinner" />
              <span>正在获取会话列表…</span>
            </div>
          ) : sessionsError && !sessions ? (
            <div className="empty-state">
              <span>{sessionsError}</span>
              <button className="btn btn-outline ripple-host" onClick={() => void loadSessions()}>
                重试
              </button>
            </div>
          ) : (sessions ?? []).length === 0 ? (
            <div className="empty-state">
              <span style={{ fontSize: 20 }}>{I18N["dsh.session.empty"]}</span>
            </div>
          ) : (
            (sessions ?? []).map((sess, idx) => (
              <div key={sess.id}>
                <div
                  className="tlli"
                  style={{ cursor: "default", padding: "8px 12px" }}
                  onClick={() => toggle(sess.id)}
                >
                  <label className="input-check" onClick={(e) => e.stopPropagation()}>
                    <input
                      type="checkbox"
                      checked={selected.includes(sess.id)}
                      onChange={() => toggle(sess.id)}
                    />
                  </label>
                  <span className="tlli-graphic">
                    <Package2FillIcon size={32} />
                  </span>
                  <span className="tlli-text">
                    <span className="tlli-title">{sess.title || sess.id}</span>
                    <span className="tlli-subtitle">
                      {sess.messageCount != null && <span>{sess.messageCount} 条消息 · </span>}
                      {sess.updatedAt != null && <span>{formatDateTime(sess.updatedAt)}</span>}
                      {sess.sizeBytes != null && <span> · {formatBytes(sess.sizeBytes)}</span>}
                    </span>
                  </span>
                </div>
                {idx < (sessions ?? []).length - 1 && <div className="divider" style={{ marginLeft: 12 }} />}
              </div>
            ))
          )}
          {workspacesError && <div className="hint error" style={{ margin: 8 }}>{workspacesError}</div>}
          {workspaces && workspaces.length > 0 && (
            <>
              <div style={{ padding: "8px 12px 4px", fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
                工作区
              </div>
              {workspaces.map((w) => (
                <div className="tlli" key={w.id} style={{ cursor: "default", padding: "8px 12px" }}>
                  <span className="tlli-graphic">
                    <ArchiveIcon size={32} />
                  </span>
                  <span className="tlli-text">
                    <span className="tlli-title">{w.name || w.id}</span>
                    <span className="tlli-subtitle">
                      {w.path && <span className="mono">{w.path} · </span>}
                      {w.updatedAt != null && <span>{formatDateTime(w.updatedAt)}</span>}
                    </span>
                  </span>
                </div>
              ))}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// 技能包 tab：技能列表 + 安装（source）+ 删除
// ---------------------------------------------------------------------------

export function InstanceSkillsPanel({ instance }: { instance: Instance }) {
  const id = instance.id;
  const tasks = useAppState().tasks;
  const installed = normState(instance.state) !== "NOT_INSTALLED";

  const [skills, setSkills] = useState<SkillInfo[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [source, setSource] = useState("");
  const [installBusy, setInstallBusy] = useState(false);
  const [taskId, setTaskId] = useState<string | null>(null);
  const [removeTarget, setRemoveTarget] = useState<SkillInfo | null>(null);
  const [removeBusy, setRemoveBusy] = useState(false);
  const handledRef = useRef<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const r = await api.instanceSkills(id);
      setSkills(r.skills ?? []);
    } catch (e) {
      setError(isNotImplemented(e) ? "后端尚未支持技能管理" : errMsg(e));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    if (!installed) return;
    void load();
    void seedTasks();
  }, [id, installed, load]);

  const task = taskId ? tasks[taskId] : undefined;

  useEffect(() => {
    if (!task || !taskId || handledRef.current === taskId) return;
    if (!isTerminalTaskState(task.state)) return;
    handledRef.current = taskId;
    if (isSuccessTaskState(task.state)) {
      toast("success", "技能安装完成");
      setSource("");
    } else {
      toast("error", `技能安装失败：${task.error ?? "任务未完成"}`);
    }
    setTaskId(null);
    void load();
  }, [task, taskId, load]);

  const doInstall = async () => {
    const src = source.trim();
    if (!src) return;
    setInstallBusy(true);
    try {
      const r = await api.installSkill(id, src);
      setTaskId(r.taskId);
      void seedTasks();
      toast("info", `已提交技能安装任务（${r.taskId}）`);
    } catch (e) {
      if (isNotImplemented(e)) toast("info", "后端尚未支持技能安装");
      else toast("error", `安装失败：${errMsg(e)}`);
    } finally {
      setInstallBusy(false);
    }
  };

  const doRemove = async () => {
    if (!removeTarget) return;
    setRemoveBusy(true);
    try {
      await api.deleteSkill(id, removeTarget.id);
      toast("success", `技能「${removeTarget.name}」已删除`);
      setRemoveTarget(null);
      void load();
    } catch (e) {
      if (isNotImplemented(e)) toast("info", "后端尚未支持技能删除");
      else toast("error", `删除失败：${errMsg(e)}`);
    } finally {
      setRemoveBusy(false);
    }
  };

  if (!installed) {
    return (
      <div className="card" style={{ margin: 10 }}>
        <p className="hint info" style={{ margin: 2 }}>
          该实例尚未安装 dsh 运行时，安装完成后才能管理技能
        </p>
      </div>
    );
  }

  return (
    <div className="page-content" style={{ margin: 0, background: "transparent", gap: 8 }}>
      {task && (
        <div className="card" style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          <div style={{ fontSize: 14, fontWeight: 600 }}>技能安装</div>
          <ProgressBar fraction={task.fraction} />
          <div style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
            {task.message ?? "正在安装技能…"}
          </div>
        </div>
      )}

      <div className="card toolbar-row">
        <input
          className="input"
          style={{ flex: 1, minWidth: 200 }}
          placeholder="github:owner/repo 或 https://…"
          value={source}
          onChange={(e) => setSource(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && source.trim() && !installBusy) void doInstall();
          }}
        />
        <button
          className="btn btn-raised ripple-host"
          disabled={!source.trim() || installBusy}
          onClick={() => void doInstall()}
        >
          <TextureIcon size={16} />
          {installBusy ? "提交中…" : "安装技能"}
        </button>
        <span className="spacer" />
        <button className="tool-btn ripple-host" disabled={loading} onClick={() => void load()}>
          <RefreshIcon size={20} />
          {I18N["button.refresh"]}
        </button>
      </div>

      <div className="card" style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", padding: 0 }}>
        <div className="list-body scroll-hover">
          {loading && !skills ? (
            <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 10, padding: 32 }}>
              <span className="spinner" />
              <span>正在获取技能列表…</span>
            </div>
          ) : error && !skills ? (
            <div className="empty-state">
              <span>{error}</span>
              <button className="btn btn-outline ripple-host" onClick={() => void load()}>
                重试
              </button>
            </div>
          ) : (skills ?? []).length === 0 ? (
            <div className="empty-state">
              <span style={{ fontSize: 20 }}>尚未安装任何技能，可通过上方输入来源安装</span>
            </div>
          ) : (
            (skills ?? []).map((sk, idx) => (
              <div key={sk.id}>
                <div className="tlli" style={{ cursor: "default", padding: "8px 12px" }}>
                  <span className="tlli-graphic">
                    <TextureIcon size={32} />
                  </span>
                  <span className="tlli-text">
                    <span className="tlli-title">
                      {sk.name}
                      <span className={`tag ${sk.enabled === false ? "plain" : "running"}`}>
                        {sk.enabled === false ? "停用" : "启用"}
                      </span>
                    </span>
                    {sk.description && <span className="tlli-subtitle">{sk.description}</span>}
                  </span>
                  <span className="row-actions">
                    <button
                      className="icon-btn on-variant ripple-host"
                      title={I18N["button.delete"]}
                      onClick={() => setRemoveTarget(sk)}
                    >
                      <DeleteIcon size={18} />
                    </button>
                  </span>
                </div>
                {idx < (skills ?? []).length - 1 && <div className="divider" style={{ marginLeft: 12 }} />}
              </div>
            ))
          )}
        </div>
      </div>

      <ConfirmDialog
        open={removeTarget !== null}
        title={`删除技能「${removeTarget?.name ?? ""}」`}
        message="删除后该技能提供的功能将不可用。"
        confirmLabel={I18N["button.delete"]}
        danger
        busy={removeBusy}
        onConfirm={() => void doRemove()}
        onCancel={() => setRemoveTarget(null)}
      />
    </div>
  );
}
