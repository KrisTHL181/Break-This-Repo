import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, isNotImplemented } from "../api";
import { Dialog } from "./Dialog";
import { ProgressBar } from "./ProgressBar";
import { seedTasks, toast, useAppState } from "../store";
import type { ExportChoices, Instance } from "../types";
import { errMsg, formatBytes, isSuccessTaskState, isTerminalTaskState } from "../utils";
import { I18N } from "../i18n";

type ExportPhase = "form" | "running" | "done" | "failed";

/** 写进 manifest 的兼容集：实例自身版本固定在首位（DshPackForge 也是这么排的），其余去重。 */
function measuredVersions(own: string, offered: readonly string[] | null): string[] {
  const set = new Set<string>([own]);
  for (const version of offered ?? []) {
    if (version && version !== own) set.add(version);
  }
  return [...set];
}

/** 「导出整合包」弹窗：名称 + 是否包含会话 + 随包的 home 内容 + 实测兼容集 → 任务进度 → 完成提示去 下载→整合包。 */
export function ExportInstanceModal({
  instance,
  open,
  onClose,
}: {
  instance: Instance;
  open: boolean;
  onClose: () => void;
}) {
  const nav = useNavigate();
  const tasks = useAppState().tasks;
  const [name, setName] = useState(instance.name);
  const [includeSessions, setIncludeSessions] = useState(false);
  const [choices, setChoices] = useState<ExportChoices | null>(null);
  const [choicesError, setChoicesError] = useState<string | null>(null);
  const [homeSelected, setHomeSelected] = useState<string[]>([]);
  const [versionsSelected, setVersionsSelected] = useState<string[]>([instance.version]);
  const [busy, setBusy] = useState(false);
  const [taskId, setTaskId] = useState<string | null>(null);
  const [phase, setPhase] = useState<ExportPhase>("form");
  const [filename, setFilename] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const handledRef = useRef<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setName(instance.name);
    setIncludeSessions(false);
    setChoices(null);
    setChoicesError(null);
    setHomeSelected([]);
    setVersionsSelected([instance.version]);
    setBusy(false);
    setTaskId(null);
    setPhase("form");
    setFilename(null);
    setError(null);
    handledRef.current = null;
    // 可选项是这台机器的事实（home 里能随包的路径、已装的 harness 版本），不由表单猜：
    // 打开时问一次后端。home 默认全不勾——白名单是显式选择；版本按后端给的实测集预勾。
    let cancelled = false;
    void (async () => {
      try {
        const fetched = await api.exportChoices(instance.id);
        if (cancelled) return;
        setChoices(fetched);
        setVersionsSelected(measuredVersions(instance.version, fetched.dshVersions));
      } catch (e) {
        if (cancelled) return;
        setChoicesError(
          isNotImplemented(e) ? I18N["dsh.export.home.load_failed"] : errMsg(e),
        );
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [open, instance.id, instance.name, instance.version]);

  const home = choices?.home ?? [];
  const ownVersion = instance.version;
  const offeredVersions = measuredVersions(ownVersion, choices?.dshVersions ?? null).filter(
    (v) => v !== ownVersion,
  );

  const toggleHome = (path: string) =>
    setHomeSelected((prev) => (prev.includes(path) ? prev.filter((p) => p !== path) : [...prev, path]));

  // 实例自身版本始终在集里：后端无论如何都会把它写在首位，勾掉只会让界面与 manifest 不一致。
  const toggleVersion = (version: string) =>
    setVersionsSelected((prev) =>
      prev.includes(version) ? prev.filter((v) => v !== version) : measuredVersions(ownVersion, [...prev, version]),
    );

  const task = taskId ? tasks[taskId] : undefined;

  useEffect(() => {
    if (phase !== "running" || !task || !taskId || handledRef.current === taskId) return;
    if (!isTerminalTaskState(task.state)) return;
    handledRef.current = taskId;
    void (async () => {
      if (isSuccessTaskState(task.state)) {
        let fn = task.result?.filename ?? null;
        if (!fn) {
          try {
            const list = await api.tasks();
            fn = list.find((t) => t.id === taskId)?.result?.filename ?? null;
          } catch {
            // 文件名仅用于展示
          }
        }
        setFilename(fn);
        setPhase("done");
        toast("success", "整合包导出完成");
      } else {
        setError(task.error ?? "导出任务未完成");
        setPhase("failed");
      }
    })();
  }, [phase, task, taskId]);

  const submit = async () => {
    setBusy(true);
    try {
      const r = await api.exportInstance(instance.id, {
        name: name.trim() || undefined,
        includeSessions,
        home: homeSelected,
        dshVersions: versionsSelected,
      });
      setTaskId(r.taskId);
      setPhase("running");
      void seedTasks();
    } catch (e) {
      setError(isNotImplemented(e) ? "后端尚未支持整合包导出" : errMsg(e));
      setPhase("failed");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open={open} title={`${I18N["modpack.export"]} - ${instance.name}`} onClose={onClose}>
      {phase === "form" && (
        <>
          <div className="form-row">
            <span className="form-label">{I18N["dsh.instance.name"]}</span>
            <input
              className="input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={instance.name}
            />
          </div>
          <label className="input-check" style={{ marginBottom: 14 }}>
            <input
              type="checkbox"
              checked={includeSessions}
              onChange={(e) => setIncludeSessions(e.target.checked)}
            />
            <span>包含会话数据（聊天记录、会话上下文）</span>
          </label>

          <div className="form-row">
            <span className="form-label">
              {I18N["dsh.export.home"]}
              {home.length > 0 ? `（已选 ${homeSelected.length} / ${home.length}）` : ""}
            </span>
            <span className="form-label">{I18N["dsh.export.home.hint"]}</span>
            {choicesError && <span className="hint error">{choicesError}</span>}
            {!choices && !choicesError && <span className="form-label">{I18N["dsh.export.home.loading"]}</span>}
            {choices && home.length === 0 && (
              <span className="form-label">{I18N["dsh.export.home.empty"]}</span>
            )}
            {home.length > 0 && (
              <div
                style={{
                  maxHeight: 160,
                  overflow: "auto",
                  border: "1px solid var(--monet-outline-variant)",
                  borderRadius: 4,
                  padding: "2px 8px",
                }}
              >
                {home.map((entry) => (
                  <div
                    key={entry.path}
                    className="tlli"
                    style={{ cursor: "pointer", padding: "6px 0", gap: 8, alignItems: "center" }}
                    onClick={() => toggleHome(entry.path)}
                  >
                    <label className="input-check" onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        checked={homeSelected.includes(entry.path)}
                        onChange={() => toggleHome(entry.path)}
                      />
                    </label>
                    <code style={{ flex: 1, minWidth: 0, wordBreak: "break-all" }}>{entry.path}</code>
                    <span style={{ flex: "none", fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
                      {formatBytes(entry.sizeBytes)}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="form-row">
            <span className="form-label">{I18N["dsh.export.versions"]}</span>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "4px 16px", alignItems: "center" }}>
              <label className="input-check" style={{ opacity: 0.8 }}>
                <input type="checkbox" checked disabled />
                <code>{ownVersion}</code>
                <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
                  （{I18N["dsh.export.versions.own"]}）
                </span>
              </label>
              {offeredVersions.map((version) => (
                <label className="input-check" key={version}>
                  <input
                    type="checkbox"
                    checked={versionsSelected.includes(version)}
                    onChange={() => toggleVersion(version)}
                  />
                  <code>{version}</code>
                </label>
              ))}
            </div>
            <span className="form-label">{I18N["dsh.export.versions.hint"]}</span>
          </div>

          <div className="dialog-actions">
            <button className="btn btn-text dim" onClick={onClose} disabled={busy}>
              {I18N["button.cancel"]}
            </button>
            <button
              className="btn btn-text"
              disabled={busy || (!choices && !choicesError)}
              onClick={() => void submit()}
            >
              {busy ? "提交中…" : "开始导出"}
            </button>
          </div>
        </>
      )}

      {phase === "running" && (
        <div style={{ display: "flex", flexDirection: "column", gap: 10, minWidth: 340 }}>
          <ProgressBar fraction={task?.fraction} />
          <div style={{ fontSize: 13 }}>{task?.message ?? "正在打包实例数据…"}</div>
        </div>
      )}

      {phase === "done" && (
        <>
          <p style={{ lineHeight: 1.7 }}>
            导出完成
            {filename ? (
              <>
                ：<code>{filename}</code>
              </>
            ) : null}
            ，可前往「下载 → 整合包」的导出记录中下载。
          </p>
          <div className="dialog-actions">
            <button className="btn btn-text dim" onClick={onClose}>
              关闭
            </button>
            <button
              className="btn btn-text"
              onClick={() => {
                onClose();
                nav("/download?tab=modpack");
              }}
            >
              前往下载
            </button>
          </div>
        </>
      )}

      {phase === "failed" && (
        <>
          <p style={{ lineHeight: 1.7 }}>导出失败:{error ?? "未知错误"}</p>
          <div className="dialog-actions">
            <button className="btn btn-text dim" onClick={onClose}>
              关闭
            </button>
            <button className="btn btn-text" onClick={() => setPhase("form")}>
              重新填写
            </button>
          </div>
        </>
      )}
    </Dialog>
  );
}