import { useEffect, useState } from "react";
import { api, type TranscodeMediaEntry } from "../../api";
import { errMsg } from "../../utils";
import { MediaBrowserPanel, fmtBytes, fmtDuration, HelpNote } from "./common";

/**
 * ffprobe 媒体信息：不是把 JSON 摊在页面上，而是按 3FUI 的“封装信息 + 流信息”读法
 * 分成两块——人看时长/码率/分辨率/声道，排障再看折叠的原始 JSON。
 */

type Probe = Record<string, unknown>;

function num(value: unknown): number | null {
  const n = typeof value === "number" ? value : Number(value);
  return Number.isFinite(n) ? n : null;
}

function text(value: unknown): string {
  return value == null ? "" : String(value);
}

function fmtBitrate(bitsPerSecond: number | null): string {
  if (bpsMissing(bitsPerSecond)) return "—";
  const bps = bitsPerSecond as number;
  if (bps >= 1_000_000) return `${(bps / 1_000_000).toFixed(2)} Mbps`;
  if (bps >= 1_000) return `${(bps / 1_000).toFixed(0)} kbps`;
  return `${bps} bps`;
}

function bpsMissing(value: number | null): boolean {
  return value == null || value <= 0;
}

function fmtFps(value: unknown): string {
  const raw = text(value);
  if (!raw) return "—";
  const [a, b] = raw.split("/").map(Number);
  const fps = Number.isFinite(a) && Number.isFinite(b) && b !== 0 ? a / b : Number(raw);
  return Number.isFinite(fps) && fps > 0 ? `${fps.toFixed(3)} fps` : "—";
}

function bitDepth(stream: Record<string, unknown>): string {
  const raw = num(stream.bits_per_raw_sample);
  if (raw && raw > 0) return `${raw} bit`;
  const pix = text(stream.pix_fmt);
  if (/p010|p016|p210|p216|10le|10be/.test(pix)) return "10 bit";
  if (/12le|12be/.test(pix)) return "12 bit";
  if (pix) return "8 bit";
  return "";
}

function streamSpec(stream: Record<string, unknown>): string {
  const type = text(stream.codec_type);
  const profile = text(stream.profile);
  const level = num(stream.level);
  const spec: string[] = [];
  if (profile) spec.push(level ? `${profile} @ L${level}` : profile);
  if (type === "video") {
    const width = num(stream.width);
    const height = num(stream.height);
    const pix = text(stream.pix_fmt);
    const depth = bitDepth(stream);
    const dims = [width && height ? `${width}×${height}` : "", pix, depth].filter(Boolean).join(" · ");
    if (dims) spec.push(dims);
  } else if (type === "audio") {
    const rate = num(stream.sample_rate);
    const channels = num(stream.channels);
    const layout = text(stream.channel_layout);
    const parts = [
      rate ? `${(rate / 1000).toFixed(1)} kHz` : "",
      channels ? `${channels} 声道` : "",
      layout,
    ].filter(Boolean);
    if (parts.length) spec.push(parts.join(" · "));
  }
  return spec.join(" · ") || "—";
}

function languageOf(stream: Record<string, unknown>): string {
  const tags = (stream.tags ?? {}) as Record<string, unknown>;
  const language = text(tags.language);
  const title = text(tags.title);
  const defaulted = ((stream.disposition ?? {}) as Record<string, unknown>).default === 1;
  return [language || "—", defaulted ? "默认" : "", title].filter(Boolean).join(" · ");
}

function streamTypeLabel(type: string): string {
  const labels = { video: "视频", audio: "音频", subtitle: "字幕", attachment: "附件", data: "数据" } as Record<string, string>;
  return labels[type] ?? (type || "—");
}

export function MediaInfoPage() {
  const [selected, setSelected] = useState<TranscodeMediaEntry | null>(null);
  const [probe, setProbe] = useState<Probe | null>(null);
  const [mediaRoot, setMediaRoot] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void api.transcode
      .capabilities()
      .then((capabilities) => setMediaRoot(capabilities.mediaRoot ?? ""))
      .catch(() => setMediaRoot(""));
  }, []);

  const fullPath = selected
    ? mediaRoot
      ? `${mediaRoot.replace(/\/+$/, "")}/${selected.path.replace(/^\/+/, "")}`
      : selected.path
    : "";

  const select = async (entry: TranscodeMediaEntry) => {
    setSelected(entry);
    setProbe(null);
    setError(null);
    setBusy(true);
    try {
      setProbe(await api.transcode.mediaProbe(entry.path));
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const format = (probe?.format ?? {}) as Record<string, unknown>;
  const streams = (probe?.streams as Array<Record<string, unknown>> | undefined) ?? [];
  const duration = num(format.duration);
  const size = num(format.size);
  const bitrate = num(format.bit_rate);
  const container = text(format.format_long_name) || text(format.format_name) || "—";
  const streamCount = streams.length > 0 ? streams.length : (num(format.nb_streams) ?? 0);

  return (
    <div className="tc-media">
      <MediaBrowserPanel selected={selected?.path} onSelect={(entry) => void select(entry)} />

      <div className="tc-media-info">
        <HelpNote title="媒体信息怎么读">
          <ul>
            <li><b>封装信息</b>是容器层：文件、容器格式、时长、大小、总码率、流数量。</li>
            <li><b>流信息</b>每行一条轨道：规格列里是 profile/level、分辨率 · 像素格式 · 位深；音频是采样率 · 声道。</li>
            <li>「语言 / 标题」来自文件元数据（tags）与默认轨标记；排障需要看全量字段时再展开最下面的原始 JSON。</li>
            <li>这里只探测、不修改文件；要转码请去「准备文件」或「参数面板」。</li>
          </ul>
        </HelpNote>
        <div className="card">
          <div className="tc-card-title">封装信息</div>
          {!selected && <div className="tc-empty">从左侧选择一个媒体文件</div>}
          {selected && (
            <div className="tc-media-grid">
              <span>文件</span>
              <span className="mono">{fullPath}</span>
              <span>容器</span>
              <span>{container}</span>
              <span>时长</span>
              <span className="mono">{duration != null ? fmtDuration(duration * 1000) : "—"}</span>
              <span>大小</span>
              <span className="mono">{size != null ? fmtBytes(size) : fmtBytes(selected.size)}</span>
              <span>总码率</span>
              <span className="mono">{fmtBitrate(bitrate)}</span>
              <span>流数量</span>
              <span className="mono">{streamCount}</span>
              {busy && <><span>状态</span><span>正在探测…</span></>}
              {error && <><span style={{ color: "var(--monet-error)" }}>错误</span><span style={{ color: "var(--monet-error)" }}>{error}</span></>}
            </div>
          )}
        </div>

        {selected && probe && (
          <div className="card">
            <div className="tc-card-title">流信息（{streams.length} 条）</div>
            <div style={{ overflowX: "auto" }}>
              <table className="tc-streams">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>类型</th>
                    <th>编码</th>
                    <th>规格</th>
                    <th>帧率</th>
                    <th>码率</th>
                    <th>语言 / 标题</th>
                  </tr>
                </thead>
                <tbody>
                  {streams.map((stream, index) => {
                    const type = text(stream.codec_type);
                    const codec = text(stream.codec_name);
                    const long = text(stream.codec_long_name);
                    return (
                      <tr key={index}>
                        <td className="mono">{num(stream.index) ?? index}</td>
                        <td><span className="tc-stream-type">{streamTypeLabel(type)}</span></td>
                        <td>
                          <div className="mono">{codec || "—"}</div>
                          {long && <div className="tc-stream-dim">{long}</div>}
                        </td>
                        <td className="mono">{streamSpec(stream)}</td>
                        <td className="mono">{type === "video" ? fmtFps(stream.avg_frame_rate ?? stream.r_frame_rate) : "—"}</td>
                        <td className="mono">{fmtBitrate(num(stream.bit_rate))}</td>
                        <td>{languageOf(stream)}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <details className="tc-raw">
              <summary>查看原始 ffprobe JSON</summary>
              <pre>{JSON.stringify(probe, null, 2)}</pre>
            </details>
          </div>
        )}
      </div>
    </div>
  );
}
