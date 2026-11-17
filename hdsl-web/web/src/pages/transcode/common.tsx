import { useEffect, useState, type ReactNode } from "react";
import { api, type TranscodeMediaEntry, type TranscodeMediaListing } from "../../api";
import { errMsg } from "../../utils";
import { ArrowBackIcon, ArrowForwardIcon, FolderIcon, FolderOpenIcon, RefreshIcon } from "../../components/icons";

/**
 * 页级说明：默认折叠，标题一行，展开后是一段人话。
 * 3FUI 的每个子页都有这种说明；这里统一成一个组件，避免每页自己写样式。
 */
export function HelpNote({ title = "使用说明", children }: { title?: string; children: ReactNode }) {
  return (
    <details className="tc-help" open>
      <summary>{title}</summary>
      <div className="tc-help-body">{children}</div>
    </details>
  );
}

export function fmtBytes(bytes: number | null | undefined): string {
  if (bytes == null || bytes < 0) return "—";
  const units = ["B", "KiB", "MiB", "GiB", "TiB"];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit++;
  }
  return `${value.toFixed(value >= 100 ? 0 : value >= 10 ? 1 : 2)} ${units[unit]}`;
}

export function fmtDuration(ms: number | null | undefined): string {
  if (ms == null || ms < 0) return "—";
  const total = Math.round(ms / 1000);
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  return h > 0 ? `${h}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`
    : `${m}:${String(s).padStart(2, "0")}`;
}

export function pct(value: number): string {
  return value < 0 ? "—" : `${Math.round(value * 100)}%`;
}

export function stateLabel(state: string): string {
  return (
    {
      queued: "排队中",
      running: "运行中",
      paused: "已暂停",
      waiting_decision: "待决定",
      stopping: "停止中",
      done: "完成",
      failed: "失败",
      cancelled: "已取消",
    }[state] ?? state
  );
}

export function stateTone(state: string): string {
  if (state === "done") return "running";
  if (state === "failed") return "danger";
  if (state === "running") return "running";
  if (state === "waiting_decision") return "danger";
  return "plain";
}

export function probeSummary(probe: Record<string, unknown> | null): string {
  if (!probe) return "";
  const format = probe.format as { duration?: string; size?: string; format_name?: string } | undefined;
  const streams = (probe.streams as Array<Record<string, unknown>> | undefined) ?? [];
  const video = streams.find((stream) => stream.codec_type === "video");
  const audio = streams.filter((stream) => stream.codec_type === "audio");
  const subtitle = streams.filter((stream) => stream.codec_type === "subtitle");
  const parts: string[] = [];
  if (format?.duration) parts.push(fmtDuration(Number(format.duration) * 1000));
  if (format?.format_name) parts.push(String(format.format_name));
  if (video) {
    const width = video.width as number | undefined;
    const height = video.height as number | undefined;
    const codec = video.codec_name as string | undefined;
    if (width && height) parts.push(`${width}×${height}`);
    if (codec) parts.push(codec);
  }
  if (audio.length) parts.push(`音频 ${audio.length} 路`);
  if (subtitle.length) parts.push(`字幕 ${subtitle.length} 路`);
  return parts.join(" · ");
}

/// A browser over the configured media root. It never sends an arbitrary server path:
/// the API resolves every relative path under the root and refuses escapes.
export function MediaBrowserPanel({
  selected,
  onSelect,
  onAddFile,
  onAddFolder,
  onAddCurrentFolder,
}: {
  selected?: string;
  onSelect: (entry: TranscodeMediaEntry) => void;
  onAddFile?: (entry: TranscodeMediaEntry) => void;
  onAddFolder?: (entry: TranscodeMediaEntry) => void;
  onAddCurrentFolder?: (path: string) => void;
}) {
  const [path, setPath] = useState("");
  const [listing, setListing] = useState<TranscodeMediaListing | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const load = async (nextPath: string) => {
    setLoading(true);
    setError(null);
    try {
      const answer = await api.transcode.mediaList(nextPath, 0, 300);
      setListing(answer);
      setPath(answer.path);
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load("");
  }, []);

  const parent = () => {
    const parts = path.split("/").filter(Boolean);
    parts.pop();
    void load(parts.join("/"));
  };

  return (
    <div className="card tc-media-browser" style={{ padding: 0, display: "flex", flexDirection: "column", minHeight: 320 }}>
      <div className="tc-media-head" style={{ display: "flex", alignItems: "center", gap: 8, padding: "8px 10px" }}>
        <button className="btn btn-text" disabled={!path || loading} onClick={parent}>
          <ArrowBackIcon size={16} />
          上级
        </button>
        <span className="mono tc-media-path" style={{ minWidth: 0, fontSize: 12, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
          {path || "媒体根"}
        </span>
        {onAddCurrentFolder && (
          <button className="btn btn-text" disabled={loading} onClick={() => onAddCurrentFolder(path)}>
            添加当前文件夹
          </button>
        )}
        <button className="icon-btn on-variant" title="刷新" onClick={() => void load(path)}>
          <RefreshIcon size={18} />
        </button>
      </div>
      {error && (
        <div style={{ padding: "0 10px 8px", fontSize: 12, color: "var(--monet-error)" }}>{error}</div>
      )}
      <div className="list-body scroll-hover" style={{ flex: 1, minHeight: 0 }}>
        {listing?.entries.length === 0 && (
          <div className="empty-state">
            <span>这个目录是空的</span>
          </div>
        )}
        {listing?.entries.map((entry) => {
          const active = selected === entry.path;
          return (
            <div
              key={entry.path}
              className={`tlli ripple-host${active ? " selected" : ""}`}
              style={{ padding: "7px 10px", cursor: "pointer", background: active ? "var(--monet-secondary-container)" : undefined }}
              onClick={() => (entry.directory ? void load(entry.path) : onSelect(entry))}
            >
              <span className="side-graphic" style={{ width: 28, height: 28 }}>
                {entry.directory ? <FolderOpenIcon size={20} /> : <FolderIcon size={20} />}
              </span>
              <span className="tlli-text" style={{ minWidth: 0 }}>
                <span className="tlli-title" style={{ overflow: "hidden", textOverflow: "ellipsis" }}>
                  {entry.name}
                </span>
                <span className="tlli-subtitle">
                  {entry.directory ? "目录" : `${entry.kind} · ${fmtBytes(entry.size)}`}
                </span>
              </span>
              {!entry.directory && onAddFile && (
                <button
                  className="btn btn-text"
                  style={{ padding: "2px 6px", fontSize: 11 }}
                  onClick={(event) => {
                    event.stopPropagation();
                    onAddFile(entry);
                  }}
                >
                  加入
                </button>
              )}
              {entry.directory && onAddFolder && (
                <button
                  className="btn btn-text"
                  style={{ padding: "2px 6px", fontSize: 11 }}
                  onClick={(event) => {
                    event.stopPropagation();
                    onAddFolder(entry);
                  }}
                >
                  添加
                </button>
              )}
              {entry.directory && <ArrowForwardIcon size={16} />}
            </div>
          );
        })}
      </div>
    </div>
  );
}