import { useEffect, useRef, useState } from "react";
import {
  cancelPrompt,
  sendPrompt,
  startConsole,
  stopConsole,
  useAcp,
  type ChatEntry,
  type ToolCallState,
} from "../acp";
import { I18N } from "../i18n";
import type { Instance } from "../types";
import { normState } from "../utils";
import {
  ArrowForwardIcon,
  CancelIcon,
  CheckIcon,
  ErrorIcon,
  KeyboardArrowDownIcon,
  RefreshIcon,
} from "./icons";

function ToolStatusIcon({ status }: { status: ToolCallState["status"] }) {
  if (status === "running") return <span className="spinner small" />;
  if (status === "done") return <CheckIcon size={14} />;
  return <CancelIcon size={14} />;
}

/** 工具调用卡片（卡片化展示，可展开参数/输出）。 */
function ToolCallCard({ call }: { call: ToolCallState }) {
  const [open, setOpen] = useState(false);
  const detail = call.input ?? call.output;
  return (
    <div className="card-opaque" style={{ padding: 0, overflow: "hidden", maxWidth: "92%" }}>
      <button
        className="tool-btn"
        style={{ width: "100%", justifyContent: "flex-start" }}
        onClick={() => setOpen((o) => !o)}
        disabled={detail == null}
      >
        <ToolStatusIcon status={call.status} />
        <span style={{ flex: 1, textAlign: "left", fontWeight: 600, fontSize: 13 }}>
          {call.name}
        </span>
        <span style={{ fontSize: 12, opacity: 0.7 }}>
          {call.status === "running"
            ? I18N["dsh.session.thinking"]
            : call.status === "done"
              ? "完成"
              : "失败"}
        </span>
        {detail != null && (
          <KeyboardArrowDownIcon
            size={13}
            style={{
              transform: open ? "rotate(180deg)" : undefined,
              transition: "transform 150ms var(--ease)",
            }}
          />
        )}
      </button>
      {open && detail != null && (
        <pre
          className="mono"
          style={{
            margin: "0 10px 10px",
            padding: 8,
            background: "var(--fixed-log-background)",
            color: "var(--fixed-log-text-fill)",
            borderRadius: 4,
            fontSize: 11,
            maxHeight: 220,
            overflow: "auto",
            whiteSpace: "pre-wrap",
            wordBreak: "break-all",
          }}
        >
          {typeof detail === "string" ? detail : JSON.stringify(detail, null, 2)}
        </pre>
      )}
    </div>
  );
}

function ThoughtBlock({ text, streaming }: { text: string; streaming: boolean }) {
  const [open, setOpen] = useState(false);
  return (
    <div style={{ maxWidth: "92%" }}>
      <button className="tool-btn" style={{ height: 30, fontSize: 12 }} onClick={() => setOpen((o) => !o)}>
        <span
          style={{
            width: 7,
            height: 7,
            borderRadius: "50%",
            background: streaming ? "var(--monet-primary)" : "var(--monet-outline)",
            flex: "none",
          }}
        />
        思考过程
        <KeyboardArrowDownIcon
          size={13}
          style={{ transform: open ? "rotate(180deg)" : undefined, transition: "transform 150ms var(--ease)" }}
        />
      </button>
      {open && (
        <div className="bubble" style={{ marginTop: 4, opacity: 0.85, fontSize: 12 }}>
          {text}
        </div>
      )}
    </div>
  );
}

function Entry({ entry, generating }: { entry: ChatEntry; generating: boolean }) {
  switch (entry.kind) {
    case "user":
      return (
        <div style={{ display: "flex", justifyContent: "flex-end" }}>
          <div className="bubble" style={{ background: "var(--monet-primary-container)", color: "var(--monet-on-primary-container)" }}>
            {entry.text}
          </div>
        </div>
      );
    case "agent":
      return (
        <div style={{ display: "flex" }}>
          <div className="bubble">
            {entry.text}
            {!entry.done && (
              <span
                style={{
                  display: "inline-block",
                  width: 7,
                  height: 14,
                  marginLeft: 2,
                  background: "var(--monet-primary)",
                  verticalAlign: "text-bottom",
                  animation: "spinner-rotate 1s steps(2) infinite",
                }}
              />
            )}
          </div>
        </div>
      );
    case "thought":
      return <ThoughtBlock text={entry.text} streaming={generating} />;
    case "tool":
      return <ToolCallCard call={entry.call} />;
  }
}

/** 实例 ACP 控制台：气泡式流式对话（保留 acp.ts 状态机）。 */
export function ConsolePanel({ instance }: { instance: Instance }) {
  const id = instance.id;
  const acp = useAcp(id);
  const installed = normState(instance.state) !== "NOT_INSTALLED";
  const [draft, setDraft] = useState("");
  const bodyRef = useRef<HTMLDivElement>(null);
  const followRef = useRef(true);

  useEffect(() => {
    const el = bodyRef.current;
    if (el && followRef.current) el.scrollTop = el.scrollHeight;
  }, [acp.entries]);

  if (!installed) {
    return (
      <div className="card" style={{ margin: 10 }}>
        <p className="hint info" style={{ margin: 2 }}>
          该实例尚未安装 dsh 运行时，请先在「实例管理」完成安装后再使用控制台
        </p>
      </div>
    );
  }

  const submit = () => {
    const t = draft.trim();
    if (!t) return;
    sendPrompt(id, t);
    setDraft("");
    followRef.current = true;
  };

  if (acp.phase === "idle" && acp.entries.length === 0) {
    return (
      <div className="card" style={{ margin: 10, padding: 24, display: "flex", flexDirection: "column", gap: 14, alignItems: "flex-start" }}>
        <p style={{ fontSize: 13, color: "var(--monet-on-surface-variant)", maxWidth: 480, lineHeight: 1.7 }}>
          通过 ACP 协议与该实例的 agent 直接对话：发送消息、查看思考过程与工具调用。
        </p>
        <button className="btn btn-raised ripple-host" onClick={() => startConsole(id)}>
          启动控制台
        </button>
      </div>
    );
  }

  if (acp.phase === "starting") {
    return (
      <div className="card" style={{ margin: 10, padding: 24, display: "flex", alignItems: "center", gap: 12 }}>
        <span className="spinner" />
        <span>正在启动 ACP 会话…</span>
      </div>
    );
  }

  if (acp.phase === "error") {
    return (
      <div className="card" style={{ margin: 10, padding: 24, display: "flex", flexDirection: "column", gap: 12, alignItems: "flex-start" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8, color: "var(--monet-error)" }}>
          <ErrorIcon size={20} />
          <span>{acp.error ?? "控制台启动失败"}</span>
        </div>
        <button className="btn btn-raised ripple-host" onClick={() => startConsole(id)}>
          <RefreshIcon size={15} />
          重试
        </button>
      </div>
    );
  }

  const ready = acp.phase === "ready";
  const canSend = ready && !acp.generating && draft.trim().length > 0;

  return (
    <div style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", margin: 10, gap: 8 }}>
      <div className="card" style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 10px" }}>
        <span className={`tag ${ready ? "running" : "plain"}`}>
          {ready ? "会话中" : "已关闭"}
        </span>
        {acp.protocolVersion != null && <span className="tag plain">ACP v{acp.protocolVersion}</span>}
        <span style={{ flex: 1 }} />
        {ready ? (
          <button className="tool-btn" style={{ height: 30 }} onClick={() => stopConsole(id)}>
            <CancelIcon size={15} />
            关闭控制台
          </button>
        ) : (
          <button className="tool-btn" style={{ height: 30 }} onClick={() => startConsole(id)}>
            <RefreshIcon size={15} />
            重新启动
          </button>
        )}
      </div>

      {acp.notice && <div className="hint warning">{acp.notice}</div>}
      {ready && acp.error && <div className="hint error">{acp.error}</div>}

      <div
        className="list-body card"
        ref={bodyRef}
        style={{ padding: 12, display: "flex", flexDirection: "column", gap: 10 }}
        onScroll={(e) => {
          const el = e.currentTarget;
          followRef.current = el.scrollHeight - el.scrollTop - el.clientHeight < 80;
        }}
      >
        {acp.entries.map((entry) => (
          <Entry key={entry.id} entry={entry} generating={acp.generating} />
        ))}
        {ready && acp.entries.length === 0 && (
          <div style={{ textAlign: "center", color: "var(--monet-on-surface-variant)", padding: 24 }}>
            会话已就绪，发送消息开始对话
          </div>
        )}
      </div>

      <div className="card" style={{ display: "flex", alignItems: "flex-end", gap: 8, padding: 8 }}>
        <textarea
          className="input"
          rows={2}
          style={{ flex: 1, resize: "none", fontWeight: 400 }}
          placeholder={ready ? "输入消息，Enter 发送，Shift+Enter 换行" : "控制台已关闭"}
          disabled={!ready}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
              e.preventDefault();
              if (canSend) submit();
            }
          }}
        />
        <button
          className="icon-btn"
          style={{ width: 38, height: 38 }}
          disabled={!canSend}
          title={I18N["dsh.session.send"]}
          onClick={submit}
        >
          <ArrowForwardIcon size={20} />
        </button>
        <button
          className="icon-btn on-variant"
          style={{ width: 38, height: 38 }}
          disabled={!acp.generating}
          title="停止本轮生成"
          onClick={() => cancelPrompt(id)}
        >
          <CancelIcon size={18} />
        </button>
      </div>
    </div>
  );
}
