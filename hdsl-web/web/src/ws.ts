import type { WsEvent } from "./types";
import { withBase } from "./base";

// WebSocket 连接管理：登录后连接、断线 3s 起指数退避重连（上限 30s）、
// 重连后自动重新订阅全部活跃 topic。握手带 cookie（浏览器默认同源行为）。

type WsHandler = (evt: WsEvent) => void;

const RECONNECT_BASE_MS = 3000;
const RECONNECT_MAX_MS = 30000;

let socket: WebSocket | null = null;
let shouldReconnect = false;
let retryDelay = RECONNECT_BASE_MS;
let retryTimer: number | undefined;
const topics = new Set<string>();
const handlers = new Set<WsHandler>();

function sendSubscribe(list: string[]): void {
  if (!socket || socket.readyState !== WebSocket.OPEN || list.length === 0) return;
  try {
    socket.send(JSON.stringify({ type: "subscribe", topics: list }));
  } catch {
    // ignore
  }
}

function open(): void {
  if (!shouldReconnect) return;
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return;

  const proto = window.location.protocol === "https:" ? "wss" : "ws";
  // 子路径挂载：面板可能被反代在挂载点下，WS 同样走 withBase
  const sock = new WebSocket(`${proto}://${window.location.host}${withBase("/ws")}`);
  socket = sock;

  sock.onopen = () => {
    retryDelay = RECONNECT_BASE_MS;
    sendSubscribe([...topics]);
  };

  sock.onmessage = (e) => {
    if (typeof e.data !== "string") return;
    let evt: WsEvent;
    try {
      evt = JSON.parse(e.data) as WsEvent;
    } catch {
      return;
    }
    if (evt && typeof evt.type === "string") {
      for (const h of [...handlers]) h(evt);
    }
  };

  sock.onclose = () => {
    if (socket === sock) socket = null;
    if (!shouldReconnect) return;
    retryTimer = window.setTimeout(open, retryDelay);
    retryDelay = Math.min(retryDelay * 2, RECONNECT_MAX_MS);
  };

  sock.onerror = () => {
    // onclose 会随后触发，统一在 onclose 里处理重连
  };
}

export const ws = {
  /** 建立连接并保持重连；幂等。 */
  connect(): void {
    if (shouldReconnect) return;
    shouldReconnect = true;
    retryDelay = RECONNECT_BASE_MS;
    open();
  },

  /** 主动断开（退出登录），不再重连，清空订阅。 */
  close(): void {
    shouldReconnect = false;
    topics.clear();
    if (retryTimer !== undefined) {
      window.clearTimeout(retryTimer);
      retryTimer = undefined;
    }
    const sock = socket;
    socket = null;
    if (sock) {
      sock.onopen = null;
      sock.onmessage = null;
      sock.onclose = null;
      sock.onerror = null;
      sock.close();
    }
  },

  on(handler: WsHandler): () => void {
    handlers.add(handler);
    return () => {
      handlers.delete(handler);
    };
  },

  addTopics(newTopics: string[]): void {
    let changed = false;
    for (const t of newTopics) {
      if (!topics.has(t)) {
        topics.add(t);
        changed = true;
      }
    }
    if (changed && socket?.readyState === WebSocket.OPEN) sendSubscribe([...topics]);
  },

  removeTopics(removed: string[]): void {
    let changed = false;
    for (const t of removed) {
      if (topics.delete(t)) changed = true;
    }
    if (!changed) return;
    // 后端 /ws 明确支持 unsubscribe（WsGateway：有 topics 就移除，订阅“全部”时进排除集；
    // 未知 type 只回一帧 error，不会断连）。不发就等于让服务端继续推这个实例的 log/state。
    if (socket?.readyState === WebSocket.OPEN) {
      try {
        socket.send(JSON.stringify({ type: "unsubscribe", topics: removed }));
      } catch {
        // ignore
      }
    }
  },

  hasTopic(t: string): boolean {
    return topics.has(t);
  },

  /** 发送任意协议消息（如 acp-*）。未连接时返回 false，由调用方降级处理。 */
  send(payload: unknown): boolean {
    if (!socket || socket.readyState !== WebSocket.OPEN) return false;
    try {
      socket.send(JSON.stringify(payload));
      return true;
    } catch {
      return false;
    }
  },
};
