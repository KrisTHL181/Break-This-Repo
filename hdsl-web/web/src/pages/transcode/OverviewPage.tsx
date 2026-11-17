import { useEffect, useState } from "react";
import { api, type TranscodeCapabilities, type TranscodePresetSummary, type TranscodeQueueSnapshot } from "../../api";
import { errMsg } from "../../utils";
import { pct, stateLabel, HelpNote } from "./common";

const COMMON_ENCODERS = [
  "libx264", "h264_nvenc", "h264_qsv", "h264_vaapi", "h264_amf", "h264_d3d12va", "h264_vulkan",
  "libx265", "hevc_nvenc", "hevc_qsv", "hevc_vaapi", "hevc_amf", "hevc_d3d12va", "hevc_vulkan",
  "libvpx-vp9", "vp9_qsv", "vp9_vaapi",
  "libaom-av1", "libsvtav1", "librav1e", "av1_nvenc", "av1_qsv", "av1_vaapi",
  "av1_amf", "av1_d3d12va", "av1_vulkan",
  "aac",
];

export function OverviewPage({ onOpenTab }: { onOpenTab: (tab: string) => void }) {
  const [capabilities, setCapabilities] = useState<TranscodeCapabilities | null>(null);
  const [queue, setQueue] = useState<TranscodeQueueSnapshot | null>(null);
  const [presets, setPresets] = useState<TranscodePresetSummary[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    setError(null);
    try {
      const [cap, q] = await Promise.all([api.transcode.capabilities(), api.transcode.queue()]);
      setCapabilities(cap);
      setQueue(q);
    } catch (e) {
      setError(errMsg(e));
    }
  };

  useEffect(() => {
    void load();
    void api.transcode.presets()
      .then((answer) => setPresets(answer.presets.filter((preset) => preset.builtin)))
      .catch(() => setPresets([]));
    const timer = window.setInterval(() => void load(), 2000);
    return () => window.clearInterval(timer);
  }, []);

  const running = queue?.jobs.filter((job) => job.state === "running" || job.state === "paused") ?? [];
  const queued = queue?.jobs.filter((job) => job.state === "queued") ?? [];
  const encoders = capabilities
    ? COMMON_ENCODERS.filter((name) => capabilities.encoders.includes(name))
    : [];

  return (
    <>
      {error && (
        <div className="card">
          <div className="comp-row">
            <span className="comp-label">读取失败</span>
            <span className="comp-value" style={{ color: "var(--monet-error)" }}>{error}</span>
          </div>
        </div>
      )}

      <div className="card tc-guide">
        <div className="tc-card-title">开始转码</div>
        <div className="tc-guide-steps">
          <button className="tc-guide-step" onClick={() => onOpenTab("prepare")}>
            <b>1</b>
            <span>准备文件</span>
            <small>在媒体根里选片</small>
          </button>
          <button className="tc-guide-step" onClick={() => onOpenTab("presets")}>
            <b>2</b>
            <span>参数面板</span>
            <small>定编码器与质量</small>
          </button>
          <button className="tc-guide-step" onClick={() => onOpenTab("prepare")}>
            <b>3</b>
            <span>加入队列</span>
            <small>一键入队</small>
          </button>
          <button className="tc-guide-step" onClick={() => onOpenTab("queue")}>
            <b>4</b>
            <span>编码队列</span>
            <small>看进度与日志</small>
          </button>
        </div>
      </div>

      <HelpNote title="转码是什么 / 会不会动到我的文件">
        <ul>
          <li>转码是<b>可选功能</b>，默认关闭；启用需要开发者密码 + HTTPS + 阅读须知。关闭后导航和数据都保留。</li>
          <li>它只读写<b>媒体根</b>里的文件，不会碰面板数据、实例目录或系统目录；输出默认放在源文件旁边，同名会加后缀。</li>
          <li>首次使用按上面的 1→4 步走；不想碰参数就用「内置预设」，它们和 Windows 版 3FUI 的预设文件互通（.3fui 可导入/导出）。</li>
          <li>硬件编码器不可用时会按「设置 → 失败策略」自动降级到软件编码，任务不会凭空消失。</li>
        </ul>
      </HelpNote>

      {presets.length > 0 && (
        <div className="card tc-builtin">
          <div className="tc-card-title">内置预设</div>
          <div className="tc-builtin-list">
            {presets.map((preset) => (
              <button
                key={preset.id}
                className="tc-builtin-item"
                title={preset.description}
                onClick={() => {
                  sessionStorage.setItem("transcode.presetId", preset.id);
                  onOpenTab("presets");
                }}
              >
                <span>{preset.name}</span>
                <small>{preset.description}</small>
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="card">
        <div className="comp-row">
          <span className="comp-label">ffmpeg</span>
          <span className="comp-value">
            {capabilities?.available ? (
              <span className="tag running">可用</span>
            ) : (
              <span className="tag plain">不可用</span>
            )}
          </span>
        </div>
        <div className="comp-row">
          <span className="comp-label">版本</span>
          <span className="comp-value mono" style={{ fontSize: 12 }}>
            {capabilities?.version || "—"}
          </span>
        </div>
        <div className="comp-row">
          <span className="comp-label">ffmpeg / ffprobe</span>
          <span className="comp-value mono" style={{ fontSize: 11, overflowWrap: "anywhere" }}>
            {capabilities?.ffmpeg || "—"}
            <br />
            {capabilities?.ffprobe || "—"}
          </span>
        </div>
        {capabilities?.error && (
          <div className="comp-row">
            <span className="comp-label" style={{ color: "var(--monet-error)" }}>错误</span>
            <span className="comp-value" style={{ fontSize: 12 }}>{capabilities.error}</span>
          </div>
        )}
        {encoders.length > 0 && (
          <div style={{ display: "flex", gap: 6, flexWrap: "wrap", padding: "4px 8px 8px" }}>
            {encoders.map((encoder) => (
              <span key={encoder} className="tag plain" style={{ fontSize: 11 }}>{encoder}</span>
            ))}
          </div>
        )}
        {capabilities && capabilities.hwaccels.length > 0 && (
          <div style={{ display: "flex", gap: 6, flexWrap: "wrap", padding: "0 8px 8px" }}>
            {capabilities.hwaccels.map((method) => (
              <span key={method} className="tag plain" style={{ fontSize: 11 }}>hw: {method}</span>
            ))}
          </div>
        )}
      </div>

      <div className="card">
        <div className="comp-row">
          <span className="comp-label">媒体根</span>
          <span className="comp-value mono" style={{ fontSize: 11, overflowWrap: "anywhere" }}>
            {capabilities?.mediaRoot || "—"}
          </span>
        </div>
        <div className="comp-row">
          <span className="comp-label">状态</span>
          <span className="comp-value">
            {capabilities?.mediaRootExists
              ? capabilities.mediaRootWritable ? "存在 · 可写" : "存在 · 只读"
              : "不存在"}
          </span>
        </div>
        <div style={{ display: "flex", gap: 8, padding: "8px" }}>
          <button className="btn btn-raised" onClick={() => onOpenTab("prepare")}>准备文件</button>
          <button className="btn btn-text" onClick={() => onOpenTab("queue")}>打开队列</button>
        </div>
      </div>

      <div className="card">
        <div className="comp-row">
          <span className="comp-label">队列</span>
          <span className="comp-value">
            {queue ? (
              <span className={`tag ${queue.runState === "running" ? "running" : "plain"}`}>
                {queue.runState === "running" ? "运行中" : queue.runState === "paused" ? "已暂停" : "空闲"}
              </span>
            ) : "—"}
          </span>
        </div>
        <div className="comp-row">
          <span className="comp-label">运行 / 排队 / 并发</span>
          <span className="comp-value">
            {running.length} / {queued.length} / {queue?.maxConcurrent ?? "—"}
          </span>
        </div>
        {running.length > 0 && (
          <div style={{ padding: "0 8px 8px" }}>
            {running.map((job) => (
              <div key={job.id} style={{ padding: "4px 0" }}>
                <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12 }}>
                  <span>{job.input}</span>
                  <span>{pct(job.fraction)} · {stateLabel(job.state)} · {job.speed > 0 ? `${job.speed.toFixed(1)}x` : "—"}</span>
                </div>
                <div className="progress" style={{ marginTop: 4 }}>
                  <div className="bar" style={{ width: `${Math.max(0, Math.min(1, job.fraction)) * 100}%` }} />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {capabilities && !capabilities.available && (
        <div className="card">
          <div className="comp-row">
            <span className="comp-label">下一步</span>
          </div>
          <p style={{ fontSize: 13, padding: "4px 8px 8px", lineHeight: 1.7 }}>
            面板找不到可用的 ffmpeg/ffprobe。容器镜像默认带 ffmpeg；裸部署请安装，或在
            `server.yaml` 的 `transcode.ffmpeg_path` / `HDSL_FFMPEG_PATH` 指向可执行文件。
          </p>
        </div>
      )}
    </>
  );
}