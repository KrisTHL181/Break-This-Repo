import { useEffect, useRef, useState } from "react";
import { clearLogs, useAppState } from "../store";
import { formatClock } from "../utils";

function normLevel(level: string): string {
  const s = (level ?? "").toUpperCase();
  if (s.includes("FATAL")) return "FATAL";
  if (s.includes("ERR")) return "ERROR";
  if (s.includes("WARN")) return "WARN";
  if (s.includes("DEBUG") || s.includes("TRACE")) return "DEBUG";
  if (s.includes("READY") || s === "OK" || s.includes("SUCCESS")) return "READY";
  return "INFO";
}

const LEVEL_CLASS: Record<string, string> = {
  FATAL: "log-error",
  ERROR: "log-error",
  WARN: "log-warn",
  DEBUG: "log-debug",
  READY: "log-ready",
  INFO: "log-info",
};

/**
 * 实时日志视图（log-window 风格：等宽 11px、深底、自动滚底可暂停）。
 * 数据来自 store（REST tail 打底 + WS log 事件实时追加）。
 */
export function LogView({
  instanceId,
  emptyHint,
  maxHeight = 420,
}: {
  instanceId: string;
  emptyHint?: string;
  maxHeight?: number;
}) {
  const logs = useAppState().logs[instanceId] ?? [];
  const [paused, setPaused] = useState(false);
  const bodyRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!paused && bodyRef.current) {
      bodyRef.current.scrollTop = bodyRef.current.scrollHeight;
    }
  }, [logs.length, paused]);

  const scrollToBottom = () => {
    if (bodyRef.current) bodyRef.current.scrollTop = bodyRef.current.scrollHeight;
  };

  return (
    <div className="log-view">
      <div className="log-toolbar">
        <span>{logs.length} 行</span>
        <span className="spacer" />
        {paused && (
          <button className="tool-btn" style={{ height: 28 }} onClick={scrollToBottom}>
            跳到底部
          </button>
        )}
        <button
          className="tool-btn"
          style={{ height: 28 }}
          onClick={() => {
            setPaused((p) => !p);
            if (paused) setTimeout(scrollToBottom, 0);
          }}
        >
          {paused ? "继续滚动" : "暂停滚动"}
        </button>
        <button
          className="tool-btn"
          style={{ height: 28 }}
          title="仅清空当前页面显示，不影响服务端日志"
          onClick={() => clearLogs(instanceId)}
        >
          清空
        </button>
      </div>
      <div className="log-body" ref={bodyRef} style={{ maxHeight }}>
        {logs.length === 0 ? (
          <div className="log-empty">{emptyHint ?? "暂无日志"}</div>
        ) : (
          logs.map((l, idx) => (
            // 行只会在尾部追加、清空时整体移除（store 里按 MAX_LOG_LINES 截断），所以下标是稳定键；
            // 用 time|text 当键反而会撞：time 只有秒级精度，重复的进度行在同一秒内文本相同。
            <div
              className={`log-line ${LEVEL_CLASS[normLevel(l.level)] ?? "log-info"}`}
              key={idx}
            >
              <span className="log-time">{formatClock(l.time)}</span>
              <span className="log-level">{l.level}</span>
              <span>{l.text}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
