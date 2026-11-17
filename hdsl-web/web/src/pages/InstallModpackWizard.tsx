import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, isNotImplemented } from "../api";
import { FileDropInput } from "../components/FileDropInput";
import { ProgressBar } from "../components/ProgressBar";
import { ArrowForwardIcon } from "../components/icons";
import { I18N } from "../i18n";
import { refreshInstances, seedTasks, toast, useAppState } from "../store";
import { errMsg, formatBytes, isSuccessTaskState, isTerminalTaskState } from "../utils";

const DSPACK_RE = /\.dspack$/i;
const MAX_PACK_SIZE = 200 * 1024 * 1024;

type Source = "local" | "remote" | "market";

/**
 * 安装整合包向导（ModpackInstallWizard，推入导航栈）：
 * 步骤一 = 三个垂直卡片选项（本地文件 / URL / PackMarket），随后进入对应表单。
 * 底部右侧 取消 / 上一步 / 下一步。
 */
export function InstallModpackWizard() {
  const nav = useNavigate();
  const tasks = useAppState().tasks;
  const [source, setSource] = useState<Source | null>(null);

  const [file, setFile] = useState<File | null>(null);
  const [name, setName] = useState("");
  const [fileError, setFileError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [taskId, setTaskId] = useState<string | null>(null);
  const [taskError, setTaskError] = useState<string | null>(null);
  const handledRef = useRef<string | null>(null);

  const task = taskId ? tasks[taskId] : undefined;

  // 上传任务终态：成功 → 刷新 + 跳新实例；失败 → 展示错误
  useEffect(() => {
    if (!task || !taskId || handledRef.current === taskId) return;
    if (!isTerminalTaskState(task.state)) return;
    handledRef.current = taskId;
    void (async () => {
      if (isSuccessTaskState(task.state)) {
        toast("success", "整合包上传安装完成，实例已创建");
        let iid = task.result?.instanceId ?? null;
        if (!iid) {
          try {
            const list = await api.tasks();
            iid = list.find((t) => t.id === taskId)?.result?.instanceId ?? null;
          } catch {
            // 拿不到则只提示
          }
        }
        await refreshInstances(false);
        if (iid) nav(`/instances/${iid}`, { replace: true });
        else {
          setTaskId(null);
          setSource(null);
        }
      } else {
        setTaskError(task.error ?? "任务未完成");
        setTaskId(null);
      }
    })();
  }, [task, taskId, nav]);

  const doUpload = async () => {
    if (!file) return;
    if (!DSPACK_RE.test(file.name)) {
      setFileError("仅支持 .dspack 格式的整合包");
      return;
    }
    if (file.size > MAX_PACK_SIZE) {
      setFileError(`文件过大（上限 ${formatBytes(MAX_PACK_SIZE)}）`);
      return;
    }
    setFileError(null);
    setBusy(true);
    try {
      const r = await api.uploadPack(file, name.trim() || undefined);
      setTaskId(r.taskId);
      void seedTasks();
      toast("info", `已提交整合包上传任务（${r.taskId}）`);
    } catch (e) {
      if (isNotImplemented(e)) toast("info", "后端尚未支持整合包上传");
      else toast("error", `上传失败：${errMsg(e)}`);
    } finally {
      setBusy(false);
    }
  };

  const pickSource = (s: Source) => {
    if (s === "market") {
      nav("/download?tab=modpack");
      return;
    }
    if (s === "remote") {
      toast("info", "网页版请下载 .dspack 文件后选择「导入本地整合包文件」");
      return;
    }
    setSource(s);
    setFile(null);
    setName("");
    setFileError(null);
    setTaskId(null);
    setTaskError(null);
  };

  return (
    <div className="page-content scroll" style={{ gap: 10 }}>
      {source === null ? (
        <>
          <div style={{ maxWidth: 400, width: "100%", margin: "24px auto 0", fontSize: 14 }}>
            {I18N["install.modpack"]}
          </div>
          <div className="wizard-options" style={{ marginTop: 0 }}>
            <button className="wizard-card ripple-host" onClick={() => pickSource("local")}>
              <span className="wizard-card-text">
                <div className="wizard-card-title">{I18N["modpack.choose.local"]}</div>
                <div className="wizard-card-subtitle">{I18N["modpack.choose.local.detail"]}</div>
              </span>
              <ArrowForwardIcon size={20} className="wizard-card-arrow" />
            </button>
            <button className="wizard-card ripple-host" onClick={() => pickSource("remote")}>
              <span className="wizard-card-text">
                <div className="wizard-card-title">{I18N["modpack.choose.remote"]}</div>
                <div className="wizard-card-subtitle">{I18N["modpack.choose.remote.detail"]}</div>
              </span>
              <ArrowForwardIcon size={20} className="wizard-card-arrow" />
            </button>
            <button className="wizard-card ripple-host" onClick={() => pickSource("market")}>
              <span className="wizard-card-text">
                <div className="wizard-card-title">{I18N["modpack.choose.repository"]}</div>
                <div className="wizard-card-subtitle">{I18N["modpack.choose.repository.detail"]}</div>
              </span>
              <ArrowForwardIcon size={20} className="wizard-card-arrow" />
            </button>
          </div>
        </>
      ) : (
        <div className="card" style={{ maxWidth: 460, width: "100%", margin: "24px auto 0", display: "flex", flexDirection: "column", gap: 12 }}>
          <div style={{ fontSize: 14, fontWeight: 600 }}>{I18N["modpack.choose.local"]}</div>
          {taskId ? (
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              <ProgressBar fraction={task?.fraction} />
              <div style={{ fontSize: 13 }}>{task?.message ?? "正在上传并安装整合包…"}</div>
              {task?.error && <div className="hint error">{task.error}</div>}
            </div>
          ) : (
            <>
              <FileDropInput
                file={file}
                onFile={(f) => {
                  setFile(f);
                  setFileError(
                    f && !DSPACK_RE.test(f.name)
                      ? "仅支持 .dspack 格式的整合包"
                      : f && f.size > MAX_PACK_SIZE
                        ? `文件过大（上限 ${formatBytes(MAX_PACK_SIZE)}）`
                        : null,
                  );
                }}
                accept=".dspack"
                hint={`选择或拖入 .dspack 整合包（≤ ${formatBytes(MAX_PACK_SIZE)}）`}
                disabled={busy}
              />
              <div className="form-row" style={{ marginBottom: 0 }}>
                <span className="form-label">{I18N["dsh.instance.name"]}（可选）</span>
                <input
                  className="input"
                  placeholder="默认取包内名称"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>
              {fileError && <div className="form-error">{fileError}</div>}
              {taskError && <div className="hint error">{taskError}</div>}
            </>
          )}
        </div>
      )}

      <div style={{ display: "flex", justifyContent: "flex-end", gap: 4, marginTop: "auto", padding: "2px 0" }}>
        <button className="btn btn-text dim ripple-host" onClick={() => nav(-1)}>
          {I18N["button.cancel"]}
        </button>
        {source !== null && !taskId && (
          <>
            <button className="btn btn-text dim ripple-host" onClick={() => setSource(null)}>
              {I18N["button.previous"]}
            </button>
            <button className="btn btn-raised ripple-host" disabled={!file || busy} onClick={() => void doUpload()}>
              {busy ? "上传中…" : I18N["button.next"]}
            </button>
          </>
        )}
      </div>
    </div>
  );
}
