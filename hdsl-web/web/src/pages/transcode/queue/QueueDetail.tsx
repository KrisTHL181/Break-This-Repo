import type { TranscodeJob } from "../../../api";
import { ContentCopyIcon } from "../../../components/icons";
import { fmtBytes, fmtDuration, pct, stateLabel, stateTone } from "../common";
import { valueLabel } from "../labels";

interface QueueDetailProps {
  job: TranscodeJob;
  log: string;
  onOpenLog: (id: string) => void;
  onCopy: (text: string, label: string) => void;
  onResetPreset: (id: string) => void;
}

const STEP_TONE: Record<string, string> = {
  running: "running",
  done: "running",
  failed: "danger",
  cancelled: "plain",
  pending: "plain",
};

const STEP_LABEL: Record<string, string> = {
  running: "进行中",
  done: "完成",
  failed: "失败",
  cancelled: "已取消",
  pending: "等待",
};

const TRIGGER_LABEL: Record<string, string> = {
  operator: "手动降级",
  timeout: "超时自动",
  policy: "策略自动",
  queue: "队列同步",
};

/// Remaining wall-clock time. Only meaningful while a speed sample exists: ffmpeg reports
/// `speed=` a moment after the first progress line, and paused jobs report nothing new.
function etaSeconds(job: TranscodeJob): number | null {
  if (job.state !== "running") return null;
  if (job.durationMs <= 0 || job.outTimeMs < 0 || job.speed <= 0) return null;
  const remain = (job.durationMs - job.outTimeMs) / 1000 / job.speed;
  return remain > 0 ? remain : null;
}

/// The right-hand pane: what this job is, how far it got, how it ran. Layout is the
/// existing one plus three additions — ETA, the pass list and the encoder-switch log.
export function QueueDetail({ job, log, onOpenLog, onCopy, onResetPreset }: QueueDetailProps) {
  const eta = etaSeconds(job);
  const steps = job.steps ?? [];
  const switches = job.switchRecords ?? [];
  return (
    <>
      <div className="tc-card-title">
        任务详情
        <span className={`tag ${stateTone(job.state)}`}>{stateLabel(job.state)}</span>
      </div>
      <div className="tc-detail-grid">
        <span>输入</span><span className="mono">{job.input}</span>
        <span>输出</span><span className="mono">{job.output || "—"}</span>
        <span>预设</span><span className="mono">{job.presetName || job.presetId || "—"}</span>
        <span>编码器</span>
        <span className="mono" title={job.encoder ?? undefined}>
          {job.encoder ? valueLabel("videoEncoder", job.encoder) : "—"}
        </span>
        <span>进度</span>
        <span className="mono">
          {pct(job.fraction)} · {job.speed > 0 ? `${job.speed.toFixed(1)}x` : "—"}
        </span>
        <span>剩余</span><span className="mono">{eta == null ? "—" : fmtDuration(eta * 1000)}</span>
        <span>帧</span><span className="mono">{job.frame >= 0 ? job.frame : "—"}</span>
        <span>码率</span><span className="mono">{job.bitrate || "—"}</span>
        <span>大小</span><span className="mono">{fmtBytes(job.sizeBytes)}</span>
        <span>耗时</span>
        <span className="mono">{fmtDuration(job.outTimeMs)} / {fmtDuration(job.durationMs)}</span>
      </div>

      {steps.length > 0 && (
        <div className="tc-detail-steps">
          {steps.map((step, index) => (
            <div className="tc-step" key={`${step.name}-${index}`}>
              <span className={`tag ${STEP_TONE[step.state] ?? "plain"}`}>{STEP_LABEL[step.state] ?? step.state}</span>
              <span className="tc-step-name">{step.name}</span>
              <span className="tc-step-args mono" title={step.args.join(" ")}>
                {step.args.slice(1).join(" ")}
              </span>
            </div>
          ))}
        </div>
      )}

      {switches.length > 0 && (
        <div className="tc-detail-switches">
          <div className="tc-live-head">编码器切换记录</div>
          {switches.map((record, index) => (
            <div className="tc-switch-record" key={`${record.at}-${index}`}>
              <div className="mono">
                {new Date(record.at).toLocaleTimeString()} · {record.from || "—"} → {record.to}
                <span className="tc-switch-trigger">{TRIGGER_LABEL[record.trigger] ?? record.trigger}</span>
              </div>
              {record.reason && <div className="tc-switch-reason">{record.reason}</div>}
            </div>
          ))}
        </div>
      )}

      {job.error && <pre className="tc-detail-error">{job.error}</pre>}

      <div className="tc-detail-actions">
        <button className="btn btn-text" onClick={() => onCopy(job.output, "输出路径")}>
          <ContentCopyIcon size={16} />
          复制路径
        </button>
        <button className="btn btn-text" onClick={() => onCopy((job.command ?? []).join(" "), "命令行")}>
          复制命令行
        </button>
        <button className="btn btn-text" onClick={() => onOpenLog(job.id)}>完整日志</button>
        {job.caps.reset && (
          <button className="btn btn-text" title="换一个预设重新排队" onClick={() => onResetPreset(job.id)}>
            重置预设
          </button>
        )}
      </div>
      <div className="tc-live-head">实时输出</div>
      <pre className="tc-live">{log || "（还没有输出）"}</pre>
    </>
  );
}
