import { useEffect, useMemo, useRef, useState } from "react";
import { PopupItem, PopupMenu, PopupSep } from "../../components/PopupMenu";
import { ContentCopyIcon, DeleteIcon, EditIcon, MoreVertIcon, PersonIcon, RefreshIcon } from "../../components/icons";
import type { ChatAttachment, ChatBlock, ChatMessage, ChatNode } from "../../types";
import { api } from "../../api";
import { Markdown } from "./Markdown";
import { AvatarView } from "./AvatarView";
import {
  Timeline, stepsFromParts, stepsFromLive,
  type LiveTimelineBlock, type TimelineStep,
} from "./Timeline";

function activeMessage(node: ChatNode): ChatMessage | null {
  if (node.variants.length === 0) return null;
  return node.variants.find((item) => item.id === node.activeVariantId) ?? node.variants[node.variants.length - 1];
}

function AttachmentChip({ attachment, onOpen }: { attachment: ChatAttachment; onOpen: (attachment: ChatAttachment) => void }) {
  const url = api.chat.attachmentUrl(attachment.id);
  if (attachment.kind === "image") {
    return (
      <button className="chat-attach-image" onClick={() => onOpen(attachment)} title={attachment.name}>
        <img src={url} alt={attachment.name} loading="lazy" />
      </button>
    );
  }
  return (
    <button className="chat-attach-chip" onClick={() => onOpen(attachment)} title={attachment.name}>
      <span className="chat-attach-kind">{attachment.kind === "document" ? "DOC" : attachment.kind.toUpperCase()}</span>
      <span className="chat-attach-name">{attachment.name}</span>
      <span className="chat-attach-size">{Math.max(1, Math.round(attachment.size / 1024))} KB</span>
      {attachment.extractStatus === "failed" && <span className="chat-attach-warn" title={attachment.extractError ?? ""}>!</span>}
    </button>
  );
}

function providerLabel(provider: string): string {
  const names: Record<string, string> = {
    bocha: "博查", deepseek: "DeepSeek 搜索", zhipu: "智谱搜索",
    tavily: "Tavily", brave: "Brave", exa: "Exa",
  };
  return names[provider] ?? "搜索";
}

function blockToolInfo(block: ChatBlock): { icon: string; text: string; detail: string } | null {
  if (block.type !== "server_tool_use") return null;
  const input = block.input ?? {};
  const search = input.action === "search" || (block.name ?? "").includes("search");
  if (search) {
    const queries = input.queries?.length ? input.queries : input.query ? [input.query] : [];
    return { icon: "🔍", text: queries.join(" / ") || "联网搜索", detail: "搜索" };
  }
  return { icon: "📄", text: input.url ?? "读取网页", detail: "读取" };
}

type TimelineItem =
  | { kind: "thinking"; text: string }
  | { kind: "tool"; icon: string; text: string; detail: string; count: number };

function buildTimeline(blocks: ChatBlock[]): TimelineItem[] {
  const items: TimelineItem[] = [];
  let pending: Extract<TimelineItem, { kind: "tool" }> | null = null;
  const flush = () => {
    if (pending) {
      items.push(pending);
      pending = null;
    }
  };
  for (const block of blocks) {
    if (block.type === "thinking") {
      const text = block.thinking ?? "";
      if (text.trim()) {
        flush();
        items.push({ kind: "thinking", text });
      }
      continue;
    }
    const info = blockToolInfo(block);
    if (!info) continue;
    if (pending && pending.detail === info.detail) {
      pending.count += 1;
      if (info.detail === "搜索" && info.text && !pending.text.includes(info.text)) {
        pending.text += pending.text ? ` / ${info.text}` : info.text;
      }
    } else {
      flush();
      pending = { kind: "tool", ...info, count: 1 };
    }
  }
  flush();
  return items;
}

function ReasoningSegment({ text }: { text: string }) {
  const [expanded, setExpanded] = useState(false);
  return (
    <div className={`chat-reasoning${expanded ? " expanded" : ""}`}>
      <button
        className="chat-reasoning-head"
        onClick={() => setExpanded((value) => !value)}
        aria-expanded={expanded}
      >
        <span className="chat-reasoning-title">思考</span>
        <span className="chat-reasoning-count">{text.length} 字</span>
        <span className="chat-reasoning-toggle">{expanded ? "收起" : "展开"}</span>
      </button>
      {expanded && <div className="chat-reasoning-body">{text}</div>}
    </div>
  );
}

function BlockTimeline({ blocks }: { blocks: ChatBlock[] }) {
  const items = buildTimeline(blocks);
  return (
    <div className="chat-block-timeline">
      {items.map((item, index) => item.kind === "thinking" ? (
        <ReasoningSegment key={`thinking-${index}`} text={item.text} />
      ) : (
        <div className="chat-tools" key={`tool-${index}`}>
          <div className="chat-tool-chip done">
            <span className="chat-tool-icon">{item.icon}</span>
            <span className="chat-tool-query">{item.text}</span>
            <span className="chat-tool-meta">{item.detail}{item.count > 1 ? ` · ${item.count} 次` : ""}</span>
          </div>
        </div>
      ))}
    </div>
  );
}

/** 一条消息（一个节点）。variant 切换、两种重生成、编辑与删除都在这里。 */
export function MessageItem({
  node, assistant, user, editingNodeId, liveContent, liveReasoning, liveTool, liveBlocks, streaming,
  onRegenerate, onEdit, onDelete, onSwitchVariant, onOpenAttachment,
}: {
  node: ChatNode;
  assistant: { name: string; avatar: string };
  user: { name: string; avatar: string | null };
  editingNodeId?: string | null;
  liveContent?: string;
  liveReasoning?: string;
  liveTool?: { tool?: string; query: string; provider: string; status: string; hits: number };
  liveBlocks?: LiveTimelineBlock[];
  streaming?: boolean;
  onRegenerate: (nodeId: string, mode: "in_place" | "truncate_after") => void;
  onEdit: (nodeId: string, text: string) => void;
  onDelete: (nodeId: string) => void;
  onSwitchVariant: (nodeId: string, messageId: string) => void;
  onOpenAttachment: (attachment: ChatAttachment) => void;
}) {
  const message = activeMessage(node);
  const [menu, setMenu] = useState<HTMLElement | null>(null);
  const [copied, setCopied] = useState(false);
  const [reasoningExpanded, setReasoningExpanded] = useState(false);
  const moreRef = useRef<HTMLButtonElement | null>(null);
  const reasoningRef = useRef<HTMLDivElement | null>(null);

  const content = streaming && liveContent !== undefined ? liveContent : (message?.content ?? "");
  const reasoning = streaming && liveReasoning !== undefined ? liveReasoning : (message?.reasoning ?? "");
  const toolRounds = streaming ? [] : (message?.tools ?? []);
  const runningTool = streaming ? liveTool : undefined;
  // 模型引用写的是短 id，渲染前换成真实 URL；否则会被 sanitize 当成非法协议删掉。
  const citations: Record<string, string> = {};
  let sourcePosition = 0;
  for (const hit of message?.sources ?? []) {
    sourcePosition += 1;
    if (hit.id) citations[hit.id] = hit.url;
    citations[String(sourcePosition)] = hit.url;
  }
  let toolPosition = 0;
  for (const round of message?.tools ?? []) {
    for (const hit of round.hits ?? []) {
      toolPosition += 1;
      if (hit.id) citations[hit.id] = hit.url;
      // 模型有时写 index（1、2…）而不是短 id；按展示顺序兜底。
      citations[String(toolPosition)] = hit.url;
    }
  }
  const [expandedTools, setExpandedTools] = useState<Record<number, boolean>>({});

  useEffect(() => {
    if (streaming) setReasoningExpanded(true);
  }, [streaming]);

  useEffect(() => {
    if (streaming && reasoningExpanded && reasoningRef.current) {
      reasoningRef.current.scrollTop = reasoningRef.current.scrollHeight;
    }
  }, [reasoning, streaming, reasoningExpanded]);

  const timelineSteps = useMemo(
    () => stepsFromParts(message?.parts ?? []),
    [message?.parts],
  );
  const liveSteps: TimelineStep[] = streaming
    ? (liveBlocks && liveBlocks.length > 0
        ? stepsFromLive(liveBlocks)
        : [
            ...(liveReasoning
              ? [{ kind: "thinking" as const, id: "t-live", round: 0, text: liveReasoning }]
              : []),
            ...(liveTool
              ? [{
                  kind: "tool" as const, id: "s-live", round: 0,
                  name: liveTool.tool ?? "search_web", query: liveTool.query, hits: liveTool.hits,
                }]
              : []),
          ])
    : [];

  if (!message) return null;

  const isUser = node.role === "user";
  const thinkingBlocks = !isUser
    ? (message.blocks ?? []).filter((block) => block.type === "thinking" && (block.thinking ?? "").trim())
    : [];
  const blockTimeline = thinkingBlocks.length > 1;
  const hasParts = (message.parts?.length ?? 0) > 0;
  const showTimeline = hasParts || liveSteps.length > 0;
  const timeline = hasParts ? timelineSteps : liveSteps;
  const index = node.variants.findIndex((item) => item.id === message.id);
  const canSwitch = node.variants.length > 1;
  const time = new Date(node.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(content);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1200);
    } catch {
      // Clipboard unavailable; ignore.
    }
  };

  return (
    <div className={`chat-msg ${isUser ? "user" : "assistant"}${editingNodeId === node.id ? " editing" : ""}`} data-node={node.id}>
      {!isUser && (
        <div className="chat-msg-avatar"><AvatarView value={assistant.avatar} /></div>
      )}
      {isUser && (
        <div className="chat-msg-avatar user">
          {user?.avatar ? <AvatarView value={user.avatar} /> : <PersonIcon size={16} />}
        </div>
      )}
      <div className="chat-msg-body">
        <div className={`chat-msg-meta${isUser ? " user" : ""}`}>
          {isUser && user?.name && <span className="chat-msg-name">{user.name}</span>}
          {!isUser && <span className="chat-msg-name">{assistant.name}</span>}
          {!isUser && message.model && <span className="chat-msg-model">{message.model}</span>}
          {streaming && <span className="chat-streaming-pill"><span className="chat-streaming-dot" />生成中</span>}
          <span className="chat-msg-time">{time}</span>
        </div>
        {showTimeline ? (
          <Timeline steps={timeline} live={!!streaming && !hasParts} />
        ) : blockTimeline ? (
          <BlockTimeline blocks={message.blocks ?? []} />
        ) : reasoning && (
          <div className={`chat-reasoning${reasoningExpanded ? " expanded" : ""}`}>
            <button
              className="chat-reasoning-head"
              onClick={() => setReasoningExpanded((value) => !value)}
              aria-expanded={reasoningExpanded}
            >
              <span className="chat-reasoning-title">思考</span>
              <span className="chat-reasoning-count">{reasoning.length} 字</span>
              <span className="chat-reasoning-toggle">{reasoningExpanded ? "收起" : "展开"}</span>
            </button>
            {reasoningExpanded && (
              <div className="chat-reasoning-body" ref={reasoningRef}>{reasoning}</div>
            )}
          </div>
        )}
        {message.attachments.length > 0 && (
          <div className="chat-attachments">
            {message.attachments.map((attachment) => (
              <AttachmentChip key={attachment.id} attachment={attachment} onOpen={onOpenAttachment} />
            ))}
          </div>
        )}
        {content ? (
          <div className="chat-bubble">
            <Markdown content={content} streaming={streaming} citations={citations} sources={message.sources} />
            {streaming && <span className="chat-cursor" />}
          </div>
        ) : streaming ? (
          <div className="chat-bubble chat-thinking">
            <span className="chat-dot" />
            <span className="chat-dot" />
            <span className="chat-dot" />
          </div>
        ) : null}
        {!isUser && !blockTimeline && !showTimeline && (toolRounds.length > 0 || runningTool) && (
          <div className="chat-tools">
            {toolRounds.map((round, index) => (
              <div className="chat-tool-round" key={`${round.query}-${index}`}>
                <button
                  className={`chat-tool-chip${expandedTools[index] ? " expanded" : ""}`}
                  onClick={() => setExpandedTools((value) => ({ ...value, [index]: !value[index] }))}
                  title="点击查看这次搜索的来源"
                >
                  <span className="chat-tool-icon">🔍</span>
                  <span className="chat-tool-query">{round.query}</span>
                  <span className="chat-tool-meta">{providerLabel(round.provider)} · {round.hits?.length ?? 0} 条</span>
                  <span className="chat-tool-chevron">{expandedTools[index] ? "收起" : "展开"}</span>
                </button>
                {expandedTools[index] && round.hits?.length > 0 && (
                  <div className="chat-sources">
                    {round.hits.map((source, sourceIndex) => (
                      <a
                        key={`${source.url}-${sourceIndex}`}
                        className="chat-source"
                        href={source.url}
                        target="_blank"
                        rel="noreferrer noopener"
                        title={source.title}
                      >
                        <span className="chat-source-index">{sourceIndex + 1}</span>
                        <span className="chat-source-title">{source.title}</span>
                        {source.site && <span className="chat-source-site">{source.site}</span>}
                      </a>
                    ))}
                  </div>
                )}
              </div>
            ))}
            {runningTool && (
              <div className={`chat-tool-chip ${runningTool.status}`}>
                <span className="chat-tool-icon">{runningTool.status === "running"
                  ? "…" : runningTool.tool === "scrape" ? "📄" : "🔍"}</span>
                <span className="chat-tool-query">{runningTool.query}</span>
                <span className="chat-tool-meta">
                  {runningTool.status === "running"
                    ? (runningTool.tool === "scrape" ? "正在读取网页" : "正在搜索")
                    : runningTool.status === "failed"
                      ? (runningTool.tool === "scrape" ? "读取失败" : "搜索失败")
                      : `${providerLabel(runningTool.provider)} · ${runningTool.hits} 条`}
                </span>
              </div>
            )}
          </div>
        )}
        {!isUser && message.sources && message.sources.length > 0 && (
          <div className="chat-sources">
            {message.sources.map((source, sourceIndex) => (
              <a
                key={`${source.url}-${sourceIndex}`}
                className="chat-source"
                href={source.url}
                target="_blank"
                rel="noreferrer noopener"
                title={source.title}
              >
                <span className="chat-source-index">{sourceIndex + 1}</span>
                <span className="chat-source-title">{source.title}</span>
                {source.site && <span className="chat-source-site">{source.site}</span>}
              </a>
            ))}
          </div>
        )}
        {message.status !== "done" && message.status !== "streaming" && (
          <div className={`chat-status ${message.status}`}>
            {message.status === "stopped" ? "已停止" : message.status === "interrupted" ? "上次输出被中断" : "生成失败"}
          </div>
        )}
        {!streaming && (
          <div className="chat-actions">
            {canSwitch && (
              <span className="chat-branch">
                <button
                  disabled={index <= 0}
                  onClick={() => onSwitchVariant(node.id, node.variants[index - 1].id)}
                  title="上一个版本"
                >‹</button>
                <span>{index + 1}/{node.variants.length}</span>
                <button
                  disabled={index < 0 || index >= node.variants.length - 1}
                  onClick={() => onSwitchVariant(node.id, node.variants[index + 1].id)}
                  title="下一个版本"
                >›</button>
              </span>
            )}
            {!isUser && (
              <button
                className="chat-action"
                onClick={() => onRegenerate(node.id, "in_place")}
                title="重新生成当前回复"
              >
                <RefreshIcon size={14} />
              </button>
            )}
            <button className="chat-action" onClick={() => void copy()} title="复制">
              <ContentCopyIcon size={14} />
              {copied ? "已复制" : "复制"}
            </button>
            {isUser ? (
              <button className="chat-action" onClick={() => onEdit(node.id, message.content)}>
                <EditIcon size={14} />
                编辑
              </button>
            ) : null}
            <button
              ref={moreRef}
              className="chat-action"
              onClick={() => setMenu(moreRef.current)}
              title="更多操作"
            >
              <MoreVertIcon size={14} />
            </button>
            <button className="chat-action danger" onClick={() => onDelete(node.id)} title="从这里删除">
              <DeleteIcon size={14} />
            </button>
          </div>
        )}
      </div>

      <PopupMenu open={menu !== null} anchor={menu} onClose={() => setMenu(null)}>
        {isUser ? (
          <PopupItem
            icon={<RefreshIcon size={16} />}
            onClick={() => {
              setMenu(null);
              onRegenerate(node.id, "truncate_after");
            }}
          >
            从这里重新生成（丢弃后续）
          </PopupItem>
        ) : (
          <>
            <PopupItem
              icon={<RefreshIcon size={16} />}
              onClick={() => {
                setMenu(null);
                onRegenerate(node.id, "in_place");
              }}
            >
              重新生成当前回复
              <span className="chat-popup-hint">保留后续消息</span>
            </PopupItem>
            <PopupItem
              icon={<RefreshIcon size={16} />}
              onClick={() => {
                setMenu(null);
                onRegenerate(node.id, "truncate_after");
              }}
            >
              从这里重新生成（丢弃后续）
            </PopupItem>
          </>
        )}
        <PopupSep />
        <PopupItem
          icon={<DeleteIcon size={16} />}
          danger
          onClick={() => {
            setMenu(null);
            onDelete(node.id);
          }}
        >
          删除这条及之后的消息
        </PopupItem>
      </PopupMenu>
    </div>
  );
}