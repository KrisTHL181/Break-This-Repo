import { useEffect, useState } from "react";
import { api, type TranscodeMediaEntry } from "../../api";
import { errMsg } from "../../utils";
import { Dialog } from "../../components/Dialog";

/// A directory-only picker below the media root. `""` means "next to the source
/// file"; `"."` means the media root itself.
export function DirectoryPicker({
  open,
  initial,
  mediaRoot,
  onClose,
  onSelect,
}: {
  open: boolean;
  initial: string;
  mediaRoot: string;
  onClose: () => void;
  onSelect: (path: string) => void;
}) {
  const normalize = (value: string) => (value.trim() === "." ? "" : value.trim());
  const [path, setPath] = useState(normalize(initial));
  const [entries, setEntries] = useState<TranscodeMediaEntry[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const load = async (nextPath: string) => {
    setLoading(true);
    setError(null);
    try {
      const answer = await api.transcode.mediaList(nextPath, 0, 500);
      setEntries(answer.entries.filter((entry) => entry.directory));
      setPath(answer.path);
    } catch (e) {
      setError(errMsg(e));
      setEntries([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!open) return;
    void load(normalize(initial));
  }, [open, initial]);

  const parent = () => {
    const parts = path.split("/").filter(Boolean);
    parts.pop();
    void load(parts.join("/"));
  };

  const absolute = path ? `${mediaRoot}/${path}`.replace(/\/+/g, "/") : mediaRoot;
  const selectedValue = path ? path : ".";

  return (
    <Dialog
      open={open}
      title="选择输出目录"
      onClose={onClose}
      wide
      footer={
        <div style={{ display: "flex", gap: 8, alignItems: "center", width: "100%", flexWrap: "wrap" }}>
          <button className="btn btn-text" onClick={() => onSelect("")}>
            使用源目录
          </button>
          <span style={{ flex: 1 }} />
          <button className="btn btn-text dim" onClick={onClose}>
            取消
          </button>
          <button className="btn btn-raised" onClick={() => onSelect(selectedValue)}>
            选择此目录
          </button>
        </div>
      }
    >
      <div style={{ display: "grid", gap: 8 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
          <button className="btn btn-text" disabled={!path || loading} onClick={parent}>
            上级
          </button>
          <button className="btn btn-text" disabled={!path || loading} onClick={() => void load("")}>
            媒体根
          </button>
          <span className="mono" style={{ flex: "1 1 100%", minWidth: 0, fontSize: 12, overflow: "hidden", textOverflow: "ellipsis" }}>
            {absolute}
          </span>
        </div>
        {error && <div style={{ fontSize: 12, color: "var(--monet-error)" }}>{error}</div>}
        <div className="list-body scroll-hover" style={{ maxHeight: 360, border: "1px solid var(--monet-outline)", borderRadius: 4 }}>
          {loading && <div className="empty-state"><span>读取中…</span></div>}
          {!loading && entries.length === 0 && !error && (
            <div className="empty-state"><span>这个目录下没有子文件夹</span></div>
          )}
          {!loading && entries.map((entry) => (
            <div
              key={entry.path}
              className="tlli ripple-host"
              style={{ padding: "7px 10px", cursor: "pointer" }}
              onClick={() => void load(entry.path)}
            >
              <span className="tlli-text" style={{ minWidth: 0 }}>
                <span className="tlli-title">{entry.name}</span>
                <span className="tlli-subtitle mono">{entry.path}</span>
              </span>
              <span style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>进入</span>
            </div>
          ))}
        </div>
        <div style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", lineHeight: 1.6 }}>
          当前选择：<span className="mono">{absolute}</span>
          <br />
          「使用源目录」= 每个文件输出到它旁边；「选择此目录」= 统一输出到当前文件夹。
        </div>
      </div>
    </Dialog>
  );
}
