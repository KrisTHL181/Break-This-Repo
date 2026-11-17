import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, type TranscodeTerms } from "../api";
import { isElevationRequired, requestDeveloperPassword } from "../elevation";
import { refreshTranscodeFeature, setTranscodeFeature, useTranscodeFeature } from "../transcodeFeature";
import { toast } from "../store";
import { errMsg } from "../utils";
import { Dialog } from "./Dialog";

/// 设置 → 通用 里的「转码」开关。
///
/// 功能默认关闭，关闭时 `/api/transcode/*` 只有 feature/terms/enable/decline
/// 可达；主侧栏的「转码」入口也只在 enabled 后出现。开启需要开发者密码，
/// 并先读一遍面板自己的转码须知（不是第三方 EULA）。
export function TranscodeFeatureCard() {
  const feature = useTranscodeFeature();
  const nav = useNavigate();
  const [terms, setTerms] = useState<TranscodeTerms | null>(null);
  const [termsError, setTermsError] = useState<string | null>(null);
  const [dialog, setDialog] = useState(false);
  const [accepted, setAccepted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [enableError, setEnableError] = useState<string | null>(null);

  useEffect(() => {
    void refreshTranscodeFeature();
  }, []);

  const enabled = feature?.enabled === true;

  const withElevation = async <T,>(call: () => Promise<T>): Promise<T> => {
    try {
      return await call();
    } catch (e) {
      if (!isElevationRequired(e)) throw e;
      if (!(await requestDeveloperPassword())) throw e;
      return await call();
    }
  };

  const openDialog = async () => {
    setDialog(true);
    setTerms(null);
    setTermsError(null);
    setAccepted(false);
    setEnableError(null);
    try {
      setTerms(await api.transcode.terms());
    } catch (e) {
      setTermsError(errMsg(e));
    }
  };

  const enable = async () => {
    if (!terms || !accepted) return;
    setBusy(true);
    setEnableError(null);
    try {
      const next = await withElevation(() =>
        api.transcode.enable({
          termsAcknowledged: true,
          termsVersion: terms.version,
          termsTextHash: terms.hash,
          sawDeclineOption: true,
          uiVersion: "1.0.0",
        }),
      );
      setTranscodeFeature(next);
      setDialog(false);
      toast("success", "转码功能已开启：入口在左侧「转码」");
      await refreshTranscodeFeature();
    } catch (e) {
      setEnableError(errMsg(e));
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const decline = async () => {
    try {
      await api.transcode.decline();
    } catch {
      // 拒绝只是记录意图；即使请求失败，关闭弹窗也不会改变服务端状态。
    }
    setDialog(false);
    toast("info", "已保持关闭");
  };

  const disable = async () => {
    if (!window.confirm("关闭转码功能？正在运行的任务会停止，已完成的输出和预设保留。")) return;
    setBusy(true);
    try {
      const next = await withElevation(() => api.transcode.disable());
      setTranscodeFeature(next);
      toast("success", "转码功能已关闭");
      await refreshTranscodeFeature();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const policyLabel = feature
    ? feature.failurePolicy === "ask"
      ? "询问"
      : feature.failurePolicy === "timeout-auto"
        ? `超时自动降级（${feature.failureTimeoutMinutes} 分钟）`
        : "完全自动化"
    : "—";

  return (
    <div className="card">
      <div className="comp-row">
        <span className="comp-label">转码（批量编码）</span>
        <span className="comp-value">
          {enabled ? <span className="tag running">已开启</span> : <span className="tag plain">默认关闭</span>}
        </span>
      </div>
      <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
        用面板自带的队列批量转码：准备文件、全量参数面板、工具与失败降级。
        关闭时 `/api/transcode/*` 全部 404、不探测 ffmpeg，面板与平时没有区别。
      </p>

      {feature && (
        <div style={{ padding: "6px 8px 0", display: "grid", gap: 4, fontSize: 12 }}>
          <div className="comp-row">
            <span className="comp-label">媒体根</span>
            <span className="comp-value mono" style={{ fontSize: 12 }}>
              {feature.mediaRoot}
              {!feature.mediaRootExists ? "（不存在）" : feature.mediaRootWritable ? "" : "（只读）"}
            </span>
          </div>
          <div className="comp-row">
            <span className="comp-label">并发 / 队列</span>
            <span className="comp-value">
              {feature.maxConcurrent} / {feature.maxQueue}
            </span>
          </div>
          <div className="comp-row">
            <span className="comp-label">失败策略</span>
            <span className="comp-value">{policyLabel}</span>
          </div>
        </div>
      )}

      <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap", padding: "10px 8px 4px" }}>
        {enabled ? (
          <>
            <button className="btn btn-raised" onClick={() => nav("/transcode")}>
              打开转码
            </button>
            <button className="btn btn-text danger" disabled={busy} onClick={() => void disable()}>
              关闭功能
            </button>
            <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
              关闭会停止队列；输出、预设与日志保留。
            </span>
          </>
        ) : (
          <>
            <button className="btn btn-raised" disabled={busy} onClick={() => void openDialog()}>
              开启功能
            </button>
            <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
              需要开发者密码（且面板在 HTTPS 下）；开启前请阅读转码须知。
            </span>
          </>
        )}
      </div>

      <Dialog
        open={dialog}
        title="开启转码功能"
        onClose={busy ? undefined : () => void decline()}
        closeOnBackdrop={!busy}
        wide
        footer={
          <>
            <button className="btn btn-text dim" disabled={busy} onClick={() => void decline()}>
              保持关闭
            </button>
            <button className="btn btn-raised" disabled={busy || !terms || !accepted} onClick={() => void enable()}>
              {busy ? "开启中…" : "阅读并开启"}
            </button>
          </>
        }
      >
        {termsError ? (
          <div className="comp-row">
            <span className="comp-label">读取须知失败</span>
            <span className="comp-value" style={{ color: "var(--monet-error)" }}>
              {termsError}
            </span>
          </div>
        ) : terms ? (
          <>
            <div
              style={{
                maxHeight: 300,
                overflow: "auto",
                whiteSpace: "pre-wrap",
                lineHeight: 1.7,
                fontSize: 13,
                padding: "4px 2px",
              }}
            >
              {terms.text}
            </div>
            <label style={{ display: "flex", gap: 8, alignItems: "flex-start", paddingTop: 12, fontSize: 13 }}>
              <input type="checkbox" checked={accepted} onChange={(e) => setAccepted(e.target.checked)} />
              <span>我已阅读并理解上述须知；转码会占用本机 CPU/GPU/磁盘。</span>
            </label>
            {enableError && (
              <div style={{ paddingTop: 8, fontSize: 12, color: "var(--monet-error)" }}>{enableError}</div>
            )}
          </>
        ) : (
          <div className="comp-row">
            <span className="comp-label">须知</span>
            <span className="comp-value">正在读取…</span>
          </div>
        )}
      </Dialog>
    </div>
  );
}