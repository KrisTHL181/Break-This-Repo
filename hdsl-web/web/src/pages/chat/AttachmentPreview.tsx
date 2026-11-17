import { useEffect, useMemo, useState } from "react";
import type { ChatAttachment } from "../../types";
import { api } from "../../api";
import { CloseIcon, DownloadIcon } from "../../components/icons";

const PAGE_CHARS = 65536;
const CSV_CHARS = 200_000;
const CSV_MAX_ROWS = 200;
const CSV_MAX_COLUMNS = 30;

type CsvTable = { rows: string[][]; columns: number; truncated: boolean };

function detectDelimiter(source: string): string {
  const sample = source.slice(0, 4096);
  const counts: Record<string, number> = { ",": 0, ";": 0, "\t": 0 };
  let quoted = false;
  for (const ch of sample) {
    if (ch === "\"") {
      quoted = !quoted;
    } else if (!quoted && ch in counts) {
      counts[ch] += 1;
    }
  }
  const best = Object.entries(counts).sort((a, b) => b[1] - a[1])[0];
  return best && best[1] > 0 ? best[0] : ",";
}

/** 只解析首屏：200 行 × 30 列，单元格 500 字符；引号、转义引号和字段内换行都按 RFC 4180 处理。 */
function parseCsv(source: string): CsvTable {
  const text = source.replace(/^\uFEFF/, "");
  const delimiter = detectDelimiter(text);
  const rows: string[][] = [];
  let row: string[] = [];
  let field = "";
  let quoted = false;
  let truncated = false;

  const pushField = () => {
    if (row.length < CSV_MAX_COLUMNS) {
      row.push(field.length > 500 ? `${field.slice(0, 500)}…` : field);
    } else {
      truncated = true;
    }
    field = "";
  };

  for (let i = 0; i < text.length; i += 1) {
    const ch = text[i];
    if (quoted) {
      if (ch === "\"") {
        if (text[i + 1] === "\"") {
          field += "\"";
          i += 1;
        } else {
          quoted = false;
        }
      } else {
        field += ch;
      }
      continue;
    }
    if (ch === "\"") {
      quoted = true;
    } else if (ch === delimiter) {
      pushField();
    } else if (ch === "\n" || ch === "\r") {
      if (ch === "\r" && text[i + 1] === "\n") {
        i += 1;
      }
      pushField();
      rows.push(row);
      row = [];
      if (rows.length >= CSV_MAX_ROWS) {
        truncated = true;
        break;
      }
    } else {
      field += ch;
    }
  }
  if (!truncated && (field.length > 0 || row.length > 0)) {
    pushField();
    rows.push(row);
  }
  const columns = rows.reduce((max, cells) => Math.max(max, cells.length), 0);
  return { rows, columns, truncated };
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function kindLabel(kind: string): string {
  switch (kind) {
    case "image": return "图片";
    case "video": return "视频";
    case "audio": return "音频";
    case "document": return "文档";
    case "text": return "文本";
    default: return "附件";
  }
}

export function AttachmentPreview({
  attachment,
  onClose,
}: {
  attachment: ChatAttachment;
  onClose: () => void;
}) {
  const isPdf = attachment.mime === "application/pdf";
  const textual = !isPdf && (attachment.kind === "text" || attachment.kind === "document");
  const isCsv = attachment.kind === "text"
    && (attachment.mime === "text/csv" || attachment.name.toLowerCase().endsWith(".csv"));
  const pageSize = isCsv ? CSV_CHARS : PAGE_CHARS;
  const url = api.chat.attachmentUrl(attachment.id);

  const [view, setView] = useState<"table" | "raw">("table");
  const [text, setText] = useState("");
  const [hasMore, setHasMore] = useState(false);
  const [nextOffset, setNextOffset] = useState(0);
  const [charset, setCharset] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  useEffect(() => {
    if (!textual) return;
    let cancelled = false;
    setText("");
    setHasMore(false);
    setNextOffset(0);
    setCharset("");
    setError(null);
    setLoading(true);
    api.chat.attachmentText(attachment.id, 0, pageSize)
      .then((page) => {
        if (cancelled) return;
        setText(page.text);
        setHasMore(page.hasMore);
        setNextOffset(page.nextOffset);
        setCharset(page.charset);
      })
      .catch((reason) => {
        if (!cancelled) setError(reason instanceof Error ? reason.message : String(reason));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [attachment.id, textual, pageSize]);

  const loadMore = async () => {
    if (loading || !hasMore) return;
    setLoading(true);
    try {
      const page = await api.chat.attachmentText(attachment.id, nextOffset, pageSize);
      setText((current) => current + page.text);
      setHasMore(page.hasMore);
      setNextOffset(page.nextOffset);
      setCharset(page.charset);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
    } finally {
      setLoading(false);
    }
  };

  const csv = useMemo(() => (isCsv && text ? parseCsv(text) : null), [isCsv, text]);
  const showTable = csv !== null && csv.columns > 1 && view === "table";

  return (
    <aside className="chat-preview" aria-label={`附件预览：${attachment.name}`}>
      <div className="chat-preview-head">
        <div className="chat-preview-title" title={attachment.name}>{attachment.name}</div>
        <button className="icon-btn" onClick={onClose} title="关闭预览">
          <CloseIcon size={18} />
        </button>
      </div>
      <div className="chat-preview-meta">
        <span>{kindLabel(attachment.kind)}</span>
        <span>{formatSize(attachment.size)}</span>
        {charset && <span>{charset}</span>}
        <span className="spacer" />
        {isCsv && csv !== null && csv.columns > 1 && (
          <button
            className="chat-preview-action"
            onClick={() => setView(showTable ? "raw" : "table")}
          >
            {showTable ? "原始文本" : "表格"}
          </button>
        )}
        <a className="chat-preview-action" href={url} target="_blank" rel="noreferrer noopener">
          新标签页
        </a>
        <a
          className="chat-preview-action"
          href={api.chat.attachmentDownloadUrl(attachment.id)}
          download={attachment.name}
        >
          <DownloadIcon size={15} />
          下载
        </a>
      </div>

      <div className="chat-preview-body scroll">
        {attachment.kind === "image" && (
          <img className="chat-preview-image" src={url} alt={attachment.name} />
        )}
        {attachment.kind === "video" && (
          <video className="chat-preview-video" src={url} controls preload="metadata" />
        )}
        {attachment.kind === "audio" && (
          <audio className="chat-preview-audio" src={url} controls preload="metadata" />
        )}
        {isPdf && (
          <iframe className="chat-preview-pdf" src={url} title={attachment.name} />
        )}
        {textual && (
          <>
            {attachment.kind === "document" && (
              <div className="chat-preview-note">
                这是服务器抽取的文本，不是原文档版式；下载可查看原件。
              </div>
            )}
            {attachment.extractStatus === "failed" && (
              <div className="chat-preview-note error">
                文本抽取失败：{attachment.extractError ?? "未知原因"}，只能下载原件。
              </div>
            )}
            {error ? (
              <div className="chat-preview-error">{error}</div>
            ) : showTable && csv ? (
              <>
                <div className="chat-preview-table-wrap scroll">
                  <table className="chat-preview-table">
                    <thead>
                      <tr>
                        {csv.rows[0].map((cell, index) => <th key={index}>{cell}</th>)}
                      </tr>
                    </thead>
                    <tbody>
                      {csv.rows.slice(1).map((cells, rowIndex) => (
                        <tr key={rowIndex}>
                          {Array.from({ length: csv.columns }, (_, columnIndex) => (
                            <td key={columnIndex}>{cells[columnIndex] ?? ""}</td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {csv.truncated && (
                  <div className="chat-preview-note">仅预览前 200 行；下载查看完整 CSV。</div>
                )}
              </>
            ) : (
              <pre className="chat-preview-text">
                {text || (loading ? "读取中…" : "（空文件）")}
              </pre>
            )}
            {!showTable && hasMore && (
              <button className="btn btn-outline" disabled={loading} onClick={() => void loadMore()}>
                {loading ? "加载中…" : "加载更多"}
              </button>
            )}
          </>
        )}
        {!textual && !isPdf && attachment.kind !== "image"
          && attachment.kind !== "video" && attachment.kind !== "audio" && (
            <div className="chat-preview-empty">
              这个类型没有内嵌预览，可下载后用本地应用打开。
            </div>
          )}
      </div>
    </aside>
  );
}
