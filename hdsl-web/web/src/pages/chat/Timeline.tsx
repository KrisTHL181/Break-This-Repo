import { useState } from "react";
import type { ChatPart } from "../../types";

type Source = { title?: string; url?: string; site?: string };

export type TimelineStep = {
  kind: "thinking" | "tool";
  id: string;
  round: number;
  text?: string;
  durationMs?: number;
  name?: string;
  query?: string;
  hits?: number;
  sources?: Source[];
};

function parseQuery(argumentsJson: string): string {
  try {
    const args = JSON.parse(argumentsJson) as { query?: string; url?: string };
    return args.query || args.url || "";
  } catch {
    return "";
  }
}

function parseHits(content?: string): Source[] {
  if (!content) return [];
  // tool content 前面有一句中文提示（“以下是「博查」返回的检索结果…”），
  // 直接 JSON.parse 会失败；截第一个 { 到最后一个 } 再解析。
  const start = content.indexOf("{");
  const end = content.lastIndexOf("}");
  if (start < 0 || end <= start) return [];
  try {
    const data = JSON.parse(content.slice(start, end + 1)) as { items?: Source[] };
    return Array.isArray(data.items) ? data.items : [];
  } catch {
    return [];
  }
}

/** 普通协议的 parts → 步骤；round 按 tool_calls 递增。 */
export function stepsFromParts(parts: ChatPart[]): TimelineStep[] {
  const calls = new Map<string, { name: string; query: string; round: number }>();
  const steps: TimelineStep[] = [];
  let round = 0;
  let index = 0;
  for (const part of parts) {
    if (part.type === "thinking" && part.text?.trim()) {
      steps.push({
        kind: "thinking",
        id: `t${index++}`,
        round,
        text: part.text,
        durationMs: part.durationMs,
      });
    } else if (part.type === "tool_calls") {
      round += 1;
      for (const call of part.calls ?? []) {
        calls.set(call.id, { name: call.name, query: parseQuery(call.arguments), round });
      }
    } else if (part.type === "tool") {
      const call = calls.get(part.id ?? "");
      if (!call) continue;
      const sources = part.sources ?? parseHits(part.content);
      steps.push({
        kind: "tool",
        id: `s${index++}`,
        round: call.round,
        name: call.name,
        query: call.query || call.name,
        hits: part.hits && part.hits > 0 ? part.hits : sources.length,
        sources,
      });
    }
  }
  return steps;
}

export type LiveTimelineBlock =
  | { kind: "thinking"; text: string }
  | { kind: "tool"; tool?: string; query: string; provider: string; status: string; hits: number; thinkingMs?: number };

/** 流式阶段只有 thinking/tool 事件，没有 parts；按“tool 之后的新思考”分轮。 */
export function stepsFromLive(blocks: LiveTimelineBlock[]): TimelineStep[] {
  const steps: TimelineStep[] = [];
  let round = 0;
  let seenTool = false;
  let index = 0;
  let lastThinking: TimelineStep | null = null;
  for (const block of blocks) {
    if (block.kind === "thinking") {
      if (seenTool) {
        round += 1;
        seenTool = false;
      }
      const step: TimelineStep = {
        kind: "thinking",
        id: `t${index++}`,
        round,
        text: block.text,
      };
      steps.push(step);
      lastThinking = step;
    } else {
      if (lastThinking && lastThinking.durationMs == null && block.thinkingMs != null) {
        lastThinking.durationMs = block.thinkingMs;
      }
      seenTool = true;
      steps.push({
        kind: "tool",
        id: `s${index++}`,
        round,
        name: block.tool ?? "search_web",
        query: block.query,
        hits: block.hits,
      });
    }
  }
  return steps;
}

function seconds(ms?: number): string {
  return `${Math.max(0, (ms ?? 0) / 1000).toFixed(1)} 秒`;
}

/** 所有步骤都列出来，默认折叠；流式时最后一段思考默认展开。 */
export function Timeline({ steps, live = false }: { steps: TimelineStep[]; live?: boolean }) {
  const [open, setOpen] = useState<Record<string, boolean>>({});
  if (steps.length === 0) return null;

  const lastVisible = steps[steps.length - 1];
  const toggle = (id: string) => setOpen((current) => ({ ...current, [id]: !current[id] }));

  return (
    <div className="chat-timeline">
      {steps.map((step) => {
        const isLast = step === lastVisible;
        const isOpen = open[step.id] ?? (live && isLast && step.kind === "thinking");
        const sources = step.sources ?? [];
        const label = step.kind === "thinking"
          ? "思考"
          : step.name === "scrape_web" ? "读取网页" : "搜索";
        return (
          <div className={`chat-timeline-step ${step.kind}${isOpen ? " open" : ""}`} key={step.id}>
            <div className="chat-timeline-main">
              <button
                className="chat-timeline-head"
                onClick={() => toggle(step.id)}
              >
                <span className="chat-timeline-title">
                  <strong>{label}</strong>
                  {step.kind === "thinking"
                    ? (step.durationMs != null ? `了 ${seconds(step.durationMs)}` : "中…")
                    : `：${step.query}`}
                </span>
                {step.kind === "tool" && (step.hits ?? 0) > 0 && (
                  <span className="chat-timeline-meta">共 {step.hits} 条结果</span>
                )}
                <span className="chat-timeline-chevron">{isOpen ? "收起" : "展开"}</span>
              </button>
              {isOpen && step.kind === "thinking" && step.text && (
                <div className="chat-timeline-body" onClick={() => toggle(step.id)}>{step.text}</div>
              )}
              {isOpen && step.kind === "tool" && sources.length > 0 && (
                <div className="chat-sources chat-timeline-sources">
                  {sources.map((source, index) => (
                    <a
                      key={`${source.url}-${index}`}
                      className="chat-source"
                      href={source.url}
                      target="_blank"
                      rel="noreferrer noopener"
                      title={source.title}
                      onClick={(event) => event.stopPropagation()}
                    >
                      <span className="chat-source-index">{index + 1}</span>
                      <span className="chat-source-title">{source.title ?? source.url}</span>
                      {source.site && <span className="chat-source-site">{source.site}</span>}
                    </a>
                  ))}
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
