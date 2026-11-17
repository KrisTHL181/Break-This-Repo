import { useEffect, useState } from "react";
import { api, type LogFileEntry, type LogRetention } from "../api";
import { isElevationRequired, requestDeveloperPassword } from "../elevation";
import { toast } from "../store";
import { errMsg } from "../utils";

/// 关于 → 日志：保留窗口 + 最近能看的那几天。
///
/// 两个数字故意分开：磁盘上留多久（180 天起、最长四年，可调）与界面能看多久
/// （服务端写死 3 天，不可调）。把界面读窗口调成和保留窗口一样长，等于让一次
/// 会话把一整年的审计摊在屏幕上；调小保留窗口又要动 `server.yaml`，因为
/// “把窗口拖短让审计消失”该由配置文件说了算，不该是一个按钮。
export function LogsCard() {
  const [retention, setRetention] = useState<LogRetention | null>(null);
  const [files, setFiles] = useState<LogFileEntry[]>([]);
  const [viewDays, setViewDays] = useState(3);
  const [audit, setAudit] = useState(0);
  const [panel, setPanel] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [preview, setPreview] = useState<{ name: string; lines: string[] } | null>(null);
  const [previewBusy, setPreviewBusy] = useState<string | null>(null);

  const load = async () => {
    try {
      const [windows, recent] = await Promise.all([api.panelLogs.retention(), api.panelLogs.recent()]);
      setRetention(windows);
      setAudit(windows.auditDays);
      setPanel(windows.panelDays);
      setFiles(recent.files);
      setViewDays(recent.viewDays);
      setError(null);
    } catch (e) {
      setError(errMsg(e));
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const withElevation = async <T,>(call: () => Promise<T>): Promise<T> => {
    try {
      return await call();
    } catch (e) {
      if (!isElevationRequired(e)) throw e;
      if (!(await requestDeveloperPassword())) throw e;
      return await call();
    }
  };

  const floor = retention?.floorDays ?? 180;
  const ceiling = retention?.ceilingDays ?? 1460;
  const inRange = (value: number) => Number.isFinite(value) && value >= floor && value <= ceiling;
  const raised =
    retention != null && (audit > retention.auditDays || panel > retention.panelDays);
  const canSave = retention != null && inRange(audit) && inRange(panel) && raised && !busy;

  const save = async () => {
    setBusy(true);
    try {
      const next = await withElevation(() =>
        api.panelLogs.setRetention({ auditDays: audit, panelDays: panel }),
      );
      setRetention(next);
      setAudit(next.auditDays);
      setPanel(next.panelDays);
      toast("success", `保留窗口：审计 ${next.auditDays} 天 / 面板 ${next.panelDays} 天`);
      await load();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const open = async (entry: LogFileEntry) => {
    if (entry.compressed) {
      toast("info", "这是 .xz 归档：请在终端里用 xzcat 看，界面只读未压缩的日志");
      return;
    }
    setPreviewBusy(entry.name);
    try {
      const answer = await api.panelLogs.tail(entry.name);
      setPreview(answer);
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setPreviewBusy(null);
    }
  };

  return (
    <>
      <div className="card">
        <div className="comp-row">
          <span className="comp-label">日志</span>
          <span className="comp-value">
            <span className="tag plain">保留窗口</span>
          </span>
        </div>
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
          清理在启动时跑一次，之后每 24 小时一次。审计日志按文件名里的日期算年龄，面板日志按修改时间；
          <b>最新一份面板日志永不删除</b>（故意的：时钟被调错时它是唯一还在写的文件）。
        </p>
        {retention?.clock && (
          <div style={{ padding: "6px 8px 0", fontSize: 12, lineHeight: 1.7 }}>
            <b>时钟</b>{" "}
            {retention.clock.trusted ? (
              <span className="tag running">可信</span>
            ) : (
              <span className="tag failed">不可信</span>
            )}{" "}
            <span style={{ color: "var(--monet-on-surface-variant)" }}>
              每 30 分钟拿单调钟对一次；一旦发现跳变，清理会一直停到面板重启（删掉的日志回不来，所以不自动恢复）。
              {retention.clock.reason ? ` 当前原因：${retention.clock.reason}` : ""}
              {retention.clock.checkedAt ? ` 上次检查：${retention.clock.checkedAt}` : ""}
            </span>
          </div>
        )}
        {retention?.lastSweep && (
          <div style={{ padding: "2px 8px 0", fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
            最近一轮：
            {retention.lastSweep.at ?? "还没跑过"}
            {" · "}
            {retention.lastSweep.skippedReason
              ? `没有删除（${retention.lastSweep.skippedReason}）`
              : `删除 ${retention.lastSweep.deleted} 个`}
          </div>
        )}
        {error && <div style={{ color: "var(--monet-error)", padding: "6px 8px", fontSize: 13 }}>{error}</div>}
        {retention && (
          <>
            <div className="comp-row">
              <span className="comp-label">审计日志</span>
              <span className="comp-value">
                <input
                  className="input"
                  type="number"
                  min={floor}
                  max={ceiling}
                  value={audit}
                  disabled={busy}
                  onChange={(e) => setAudit(Number(e.target.value))}
                  style={{ width: 110 }}
                />
                <span className="dim" style={{ fontSize: 12 }}>天（现在是 {retention.auditDays} 天）</span>
              </span>
            </div>
            <div className="comp-row">
              <span className="comp-label">面板日志</span>
              <span className="comp-value">
                <input
                  className="input"
                  type="number"
                  min={floor}
                  max={ceiling}
                  value={panel}
                  disabled={busy}
                  onChange={(e) => setPanel(Number(e.target.value))}
                  style={{ width: 110 }}
                />
                <span className="dim" style={{ fontSize: 12 }}>天（现在是 {retention.panelDays} 天）</span>
              </span>
            </div>
            <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap", padding: "10px 8px 4px" }}>
              <button className="btn btn-raised" disabled={!canSave} onClick={() => void save()}>
                保存窗口
              </button>
              <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", lineHeight: 1.6 }}>
                范围 {floor}–{ceiling} 天；<b>只能调大</b>，调小要编辑 <code>server.yaml</code> 的
                <code> logs.audit_days / logs.panel_days</code>。保存需要开发者密码。
              </span>
            </div>
          </>
        )}
      </div>

      <div className="card">
        <div className="comp-row">
          <span className="comp-label">最近 {viewDays} 天</span>
          <span className="comp-value">
            <button className="btn btn-text" disabled={busy} onClick={() => void load()}>刷新</button>
          </span>
        </div>
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
          界面只读最近 {viewDays} 天的日志（服务端写死，不随保留窗口变）。
          更早的日志仍在磁盘上，查看请用终端或 <code>curl</code>。
        </p>
        <div style={{ padding: "4px 8px 8px" }}>
          {files.length === 0 ? (
            <div className="dim" style={{ fontSize: 13 }}>还没有日志。</div>
          ) : files.map((entry) => (
            <div
              key={entry.name}
              style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0", borderTop: "1px solid var(--monet-outline-variant)", fontSize: 13 }}
            >
              <span className="mono" style={{ flex: 1, wordBreak: "break-all" }}>{entry.name}</span>
              <span className="tag plain">{entry.kind === "audit" ? "审计" : "面板"}</span>
              <span className="dim" style={{ width: 70, textAlign: "right", fontSize: 12 }}>{formatBytes(entry.bytes)}</span>
              <button
                className="btn btn-text"
                disabled={previewBusy === entry.name}
                onClick={() => void open(entry)}
                title={entry.compressed ? "压缩归档：请用终端查看" : "看最后 200 行"}
              >
                {entry.compressed ? "归档" : "查看"}
              </button>
            </div>
          ))}
        </div>
        {preview && (
          <div style={{ borderTop: "1px solid var(--monet-outline-variant)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 8px" }}>
              <span className="mono" style={{ flex: 1, fontSize: 12, wordBreak: "break-all" }}>{preview.name}</span>
              <button className="btn btn-text" onClick={() => setPreview(null)}>关闭</button>
            </div>
            <pre style={{ maxHeight: 320, overflow: "auto", fontSize: 12, margin: 0, padding: "0 8px 8px", whiteSpace: "pre-wrap" }}>
              {preview.lines.length === 0 ? "（空文件）" : preview.lines.join("\n")}
            </pre>
          </div>
        )}
      </div>
    </>
  );
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
