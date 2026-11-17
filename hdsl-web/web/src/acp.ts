// ---------------------------------------------------------------------------
// ACP 控制台会话管理（Phase 4）
//
// 后端经 /ws 提供 ACP 通道：
//   发 acp-start / acp-prompt / acp-cancel / acp-stop，
//   收 acp-ready / acp-update / acp-finished / acp-error。
// 每个实例一份控制台状态（切换 tab 不丢），组件经 useAcp(instanceId) 订阅；
// 离开实例详情页时由页面调用 disposeConsole 释放（尽力 acp-stop）。
//
// 状态机：
//   idle --[启动]--> starting --acp-ready--> ready --[关闭]--> idle（保留对话记录）
//   starting --acp-error / 超时--> error --[重试]--> starting
//   ready 下 prompt：turn+1、generating=true；同一轮的流式 chunk 归入该轮，
//   acp-finished 后 generating=false、当前 agent 气泡定稿。
// ---------------------------------------------------------------------------

import { useSyncExternalStore } from "react";
import { ws } from "./ws";
import type { WsEvent } from "./types";

export type AcpPhase = "idle" | "starting" | "ready" | "error";
export type ToolStatus = "running" | "done" | "failed";

export interface ToolCallState {
  id: string;
  name: string;
  status: ToolStatus;
  input?: unknown;
  output?: unknown;
}

export type ChatEntry =
  | { kind: "user"; id: string; turn: number; text: string }
  | { kind: "agent"; id: string; turn: number; text: string; done: boolean }
  | { kind: "thought"; id: string; turn: number; text: string }
  | { kind: "tool"; id: string; turn: number; call: ToolCallState };

export interface AcpState {
  phase: AcpPhase;
  session: string | null;
  protocolVersion: number | null;
  /** 当前轮次号：每发一条 prompt +1，流式更新归入同一轮 */
  turn: number;
  entries: ChatEntry[];
  generating: boolean;
  /** 启动失败 / 会话内错误（横幅展示） */
  error: string | null;
  /** stopReason 非 end_turn 时的提示条文案 */
  notice: string | null;
}

const IDLE: AcpState = {
  phase: "idle",
  session: null,
  protocolVersion: null,
  turn: 0,
  entries: [],
  generating: false,
  error: null,
  notice: null,
};

/** 启动等待上限：后端未上线/不支持 ACP 时不会有响应，超时降级为错误态 */
const START_TIMEOUT_MS = 20000;

const states = new Map<string, AcpState>();
const listeners = new Set<() => void>();
const startTimers = new Map<string, number>();

let seq = 1;
const nid = (): string => `e${seq++}`;

function emit(): void {
  for (const l of [...listeners]) l();
}

function getAcpState(id: string): AcpState {
  return states.get(id) ?? IDLE;
}

function update(id: string, fn: (s: AcpState) => AcpState): void {
  states.set(id, fn(getAcpState(id)));
  emit();
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function useAcp(id: string): AcpState {
  return useSyncExternalStore(subscribe, () => getAcpState(id));
}

function clearStartTimer(id: string): void {
  const t = startTimers.get(id);
  if (t !== undefined) {
    window.clearTimeout(t);
    startTimers.delete(id);
  }
}

// ---------------------------------------------------------------------------
// 动作
// ---------------------------------------------------------------------------

export function startConsole(id: string): void {
  const cur = getAcpState(id);
  if (cur.phase === "starting" || cur.phase === "ready") return;
  clearStartTimer(id);
  if (!ws.send({ type: "acp-start", instance: id })) {
    update(id, () => ({ ...IDLE, phase: "error", error: "WebSocket 未连接，请稍后重试" }));
    return;
  }
  update(id, () => ({ ...IDLE, phase: "starting" }));
  const timer = window.setTimeout(() => {
    startTimers.delete(id);
    if (getAcpState(id).phase === "starting") {
      update(id, (s) => ({
        ...s,
        phase: "error",
        error: "启动超时：后端可能尚未支持 ACP 控制台",
      }));
    }
  }, START_TIMEOUT_MS);
  startTimers.set(id, timer);
}

export function stopConsole(id: string): void {
  const s = getAcpState(id);
  if (s.session) ws.send({ type: "acp-stop", instance: id });
  clearStartTimer(id);
  // 保留对话记录：关闭后仍可回读，下次启动时清空
  update(id, (x) => ({ ...x, phase: "idle", session: null, generating: false, error: null }));
}

/** 页面卸载释放：尽力 acp-stop 后丢弃本地状态 */
export function disposeConsole(id: string): void {
  const s = states.get(id);
  if (s?.session) ws.send({ type: "acp-stop", instance: id });
  clearStartTimer(id);
  states.delete(id);
}

export function sendPrompt(id: string, text: string): void {
  const s = getAcpState(id);
  const t = text.trim();
  if (!t || s.phase !== "ready" || s.generating || !s.session) return;
  if (!ws.send({ type: "acp-prompt", instance: id, session: s.session, text: t })) {
    update(id, (x) => ({ ...x, error: "WebSocket 未连接，消息未发出" }));
    return;
  }
  const turn = s.turn + 1;
  update(id, (x) => ({
    ...x,
    turn,
    generating: true,
    notice: null,
    error: null,
    entries: [...x.entries, { kind: "user", id: nid(), turn, text: t }],
  }));
}

export function cancelPrompt(id: string): void {
  const s = getAcpState(id);
  if (s.phase !== "ready" || !s.generating || !s.session) return;
  ws.send({ type: "acp-cancel", instance: id, session: s.session });
}

// ---------------------------------------------------------------------------
// 流式更新归约
// ---------------------------------------------------------------------------

/** 从 update 体提取文本：兼容 text / content(字符串|{text}|[{text}...]) / delta 等形态 */
function chunkText(u: Record<string, unknown>): string {
  if (typeof u.text === "string") return u.text;
  const c = u.content;
  if (typeof c === "string") return c;
  if (c && typeof c === "object" && !Array.isArray(c)) {
    const t = (c as { text?: unknown }).text;
    if (typeof t === "string") return t;
  }
  if (Array.isArray(c)) {
    return c
      .map((p) =>
        p && typeof p === "object" && typeof (p as { text?: unknown }).text === "string"
          ? (p as { text: string }).text
          : "",
      )
      .join("");
  }
  const d = u.delta;
  if (typeof d === "string") return d;
  if (d && typeof d === "object") {
    const t = (d as { text?: unknown }).text;
    if (typeof t === "string") return t;
  }
  return "";
}

function pickStr(...vals: unknown[]): string | null {
  for (const v of vals) {
    if (typeof v === "string" && v) return v;
  }
  return null;
}

function firstDefined(...vals: unknown[]): unknown {
  for (const v of vals) {
    if (v !== undefined) return v;
  }
  return undefined;
}

function normToolStatus(v: unknown, fallback: ToolStatus): ToolStatus {
  const s = String(v ?? "").toLowerCase();
  if (["completed", "complete", "done", "success", "succeeded", "finished"].includes(s)) return "done";
  if (["failed", "error", "errored", "cancelled", "canceled", "rejected"].includes(s)) return "failed";
  if (["pending", "in_progress", "running", "started", "executing"].includes(s)) return "running";
  return fallback;
}

/** 连续 chunk 追加到同一气泡（agent/thought 各自成泡，同轮内合并） */
function appendText(id: string, kind: "agent" | "thought", text: string): void {
  if (!text) return;
  update(id, (s) => {
    const last = s.entries[s.entries.length - 1];
    if (last && last.turn === s.turn) {
      if (kind === "agent" && last.kind === "agent" && !last.done) {
        const entries = s.entries.slice();
        entries[entries.length - 1] = { ...last, text: last.text + text };
        return { ...s, entries };
      }
      if (kind === "thought" && last.kind === "thought") {
        const entries = s.entries.slice();
        entries[entries.length - 1] = { ...last, text: last.text + text };
        return { ...s, entries };
      }
    }
    const entry: ChatEntry =
      kind === "agent"
        ? { kind: "agent", id: nid(), turn: s.turn, text, done: false }
        : { kind: "thought", id: nid(), turn: s.turn, text };
    return { ...s, entries: [...s.entries, entry] };
  });
}

/** tool_call 创建 / tool_call_update 合并（按 toolCallId 定位，更新先于创建到达时兜底新建） */
function upsertTool(id: string, u: Record<string, unknown>): void {
  update(id, (s) => {
    const cid = pickStr(u.toolCallId, u.tool_call_id, u.id, u.callId) ?? nid();
    const idx = s.entries.findIndex((e) => e.kind === "tool" && e.call.id === cid);
    const prevEntry = idx >= 0 ? (s.entries[idx] as Extract<ChatEntry, { kind: "tool" }>) : undefined;
    const prev = prevEntry?.call;
    const call: ToolCallState = {
      id: cid,
      name: pickStr(u.title, u.name, u.toolName, u.tool) ?? prev?.name ?? "工具调用",
      status: normToolStatus(u.status, prev?.status ?? "running"),
      input: firstDefined(u.rawInput, u.input, u.arguments, u.params) ?? prev?.input,
      output: firstDefined(u.output, u.rawOutput, u.result) ?? prev?.output,
    };
    if (idx >= 0 && prevEntry) {
      const entries = s.entries.slice();
      entries[idx] = { ...prevEntry, call };
      return { ...s, entries };
    }
    return { ...s, entries: [...s.entries, { kind: "tool", id: nid(), turn: s.turn, call }] };
  });
}

// ---------------------------------------------------------------------------
// WS 事件
// ---------------------------------------------------------------------------

const STOP_REASON_TEXT: Record<string, string> = {
  max_tokens: "输出达到长度上限，内容可能被截断",
  max_turn_requests: "已达到本轮请求次数上限",
  refusal: "模型拒绝生成该内容",
  cancelled: "已停止生成",
  canceled: "已停止生成",
  error: "本轮生成因错误中断",
};

function onReady(instance: string, session: string, protocolVersion?: number): void {
  clearStartTimer(instance);
  update(instance, (s) => {
    // 迟到事件（已关闭/已释放）：回发 acp-stop 清理后端会话
    if (s.phase !== "starting" && s.phase !== "ready") {
      ws.send({ type: "acp-stop", instance });
      return s;
    }
    return {
      ...s,
      phase: "ready",
      session,
      protocolVersion: typeof protocolVersion === "number" ? protocolVersion : null,
      error: null,
    };
  });
}

function onError(instance: string, error: string): void {
  clearStartTimer(instance);
  update(instance, (s) => {
    if (s.phase === "starting") return { ...s, phase: "error", error };
    return { ...s, generating: false, error };
  });
}

function onUpdate(instance: string, session: string | undefined, u: Record<string, unknown>): void {
  const s = getAcpState(instance);
  if (s.phase !== "ready") return;
  if (s.session && session && session !== s.session) return;
  switch (typeof u.kind === "string" ? u.kind : "") {
    case "agent_message_chunk":
      appendText(instance, "agent", chunkText(u));
      break;
    case "agent_thought_chunk":
      appendText(instance, "thought", chunkText(u));
      break;
    case "tool_call":
    case "tool_call_update":
      upsertTool(instance, u);
      break;
    default:
      // 未知更新类型（plan 等）：忽略，不影响对话
      break;
  }
}

function onFinished(instance: string, session: string | undefined, stopReason?: string): void {
  const s = getAcpState(instance);
  if (s.session && session && session !== s.session) return;
  update(instance, (cur) => {
    const entries = cur.entries.slice();
    const last = entries[entries.length - 1];
    if (last && last.kind === "agent" && !last.done) {
      entries[entries.length - 1] = { ...last, done: true };
    }
    const reason = (stopReason ?? "").toLowerCase();
    const notice =
      reason && reason !== "end_turn"
        ? STOP_REASON_TEXT[reason] ?? `本轮生成结束（${stopReason}）`
        : null;
    return { ...cur, entries, generating: false, notice };
  });
}

// 模块加载即注册（与 store 并列的独立处理器，store 对 acp-* 事件走 default 忽略）
ws.on((evt: WsEvent) => {
  switch (evt.type) {
    case "acp-ready":
      onReady(evt.instance, evt.session, evt.protocolVersion);
      break;
    case "acp-error":
      onError(evt.instance, evt.error);
      break;
    case "acp-update":
      if (evt.update && typeof evt.update === "object") {
        onUpdate(evt.instance, evt.session, evt.update as Record<string, unknown>);
      }
      break;
    case "acp-finished":
      onFinished(evt.instance, evt.session, evt.stopReason);
      break;
    default:
      break;
  }
});
