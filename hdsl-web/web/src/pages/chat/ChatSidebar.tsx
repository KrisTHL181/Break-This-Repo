import { AvatarView } from "./AvatarView";
import { useEffect, useRef, useState } from "react";
import { Dialog } from "../../components/Dialog";
import { PopupItem, PopupMenu, PopupSep } from "../../components/PopupMenu";
import { AddIcon, ArchiveIcon, DeleteIcon, EditIcon, MoreVertIcon, PersonIcon, SearchIcon, SettingsIcon } from "../../components/icons";
import type { ChatAssistant, ChatSessionSummary } from "../../types";

function ago(at: number): string {
  if (!at) return "";
  const diff = Math.max(0, Date.now() - at);
  const min = Math.floor(diff / 60000);
  if (min < 1) return "刚刚";
  if (min < 60) return `${min} 分钟前`;
  const hour = Math.floor(min / 60);
  if (hour < 24) return `${hour} 小时前`;
  const day = Math.floor(hour / 24);
  return day < 30 ? `${day} 天前` : new Date(at).toLocaleDateString();
}

function preview(text: string | null): string {
  if (!text) return "还没有消息";
  return text.replace(/\s+/g, " ").trim().slice(0, 60);
}

function groupOf(at: number): string {
  if (!at) return "更早";
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  if (at >= start) return "今天";
  if (at >= start - 86400000) return "昨天";
  if (at >= start - 7 * 86400000) return "7 天内";
  return "更早";
}

/** 聊天侧栏：助手切换器 + 搜索 + 按日期分组的会话列表。 */
export function ChatSidebar({
  assistants, assistantId, onAssistant, onManageAssistants, onSettings,
  sessions, sessionId, onSelect, onNew, onImport, onRename, onDelete, onClose,
}: {
  assistants: ChatAssistant[];
  assistantId: string;
  onAssistant: (id: string) => void;
  onManageAssistants: () => void;
  onSettings: () => void;
  sessions: ChatSessionSummary[];
  sessionId: string | null;
  onSelect: (id: string) => void;
  onNew: () => void;
  onImport: () => void;
  onRename: (session: ChatSessionSummary) => void;
  onDelete: (session: ChatSessionSummary) => void;
  onClose?: () => void;
}) {
  const assistant = assistants.find((item) => item.id === assistantId) ?? assistants[0] ?? null;
  const [assistantMenu, setAssistantMenu] = useState<HTMLElement | null>(null);
  const [sessionMenu, setSessionMenu] = useState<{ anchor: HTMLElement; session: ChatSessionSummary } | null>(null);
  const [query, setQuery] = useState("");
  const menuRef = useRef<HTMLButtonElement | null>(null);

  const visible = query.trim()
    ? sessions.filter((session) =>
        (session.title + " " + (session.lastMessage ?? "")).toLowerCase()
          .includes(query.trim().toLowerCase()))
    : sessions;
  const groups: { label: string; items: ChatSessionSummary[] }[] = [];
  for (const session of visible) {
    const label = groupOf(session.updatedAt);
    const last = groups[groups.length - 1];
    if (last && last.label === label) last.items.push(session);
    else groups.push({ label, items: [session] });
  }

  const pick = (id: string) => {
    onSelect(id);
    onClose?.();
  };

  return (
    <aside className="chat-sidebar">
      <div className="chat-sidebar-head">
        <button ref={menuRef} className="chat-assistant-switch ripple-host" onClick={() => setAssistantMenu(menuRef.current)}>
          <span className="chat-avatar-ring">
            <span className="chat-avatar"><AvatarView value={assistant?.avatar} /></span>
          </span>
          <span className="chat-assistant-text">
            <span className="chat-assistant-name">
              {assistant?.name ?? "默认助手"}
              {assistant?.preset && <span className="tag plain" style={{ marginLeft: 6 }}>官方</span>}
            </span>
            <span className="chat-assistant-sub">{assistant?.description || "点击切换助手"}</span>
          </span>
          <span className="chat-caret">▾</span>
        </button>
      </div>

      <div className="chat-sidebar-actions">
        <button className="chat-new-btn ripple-host" onClick={onNew}>
          <AddIcon size={18} />
          新建会话
        </button>
        <button className="chat-new-btn ripple-host" onClick={onImport}
                title="导入会话（DeepSeek 导出包或 HDSL JSON）">
          <ArchiveIcon size={18} />
          导入会话
        </button>
        <label className="chat-search">
          <SearchIcon size={14} />
          <input
            value={query}
            placeholder="搜索会话"
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>
      </div>

      <div className="chat-session-list scroll">
        {groups.length === 0 ? (
          <div className="chat-session-empty">
            <span className="chat-session-empty-icon">💬</span>
            <span>{query ? "没有匹配的会话" : "这个助手还没有会话"}</span>
            {!query && <span className="chat-session-empty-hint">发送第一条消息，或点上面的「新建会话」。</span>}
          </div>
        ) : (
          groups.map((group) => (
            <div key={group.label} className="chat-session-group">
              <div className="chat-session-group-title">{group.label}</div>
              {group.items.map((session) => (
                <div
                  key={session.id}
                  className={`chat-session-item${session.id === sessionId ? " selected" : ""}`}
                  onClick={() => pick(session.id)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      pick(session.id);
                    }
                  }}
                >
                  <div className="chat-session-main">
                    <div className="chat-session-title">{session.title || "新会话"}</div>
                    <div className="chat-session-preview">{preview(session.lastMessage)}</div>
                  </div>
                  <span className="chat-session-meta">{ago(session.updatedAt)}</span>
                  <button
                    className="chat-session-more"
                    onClick={(event) => {
                      event.stopPropagation();
                      setSessionMenu({ anchor: event.currentTarget, session });
                    }}
                    title="更多"
                  >
                    <MoreVertIcon size={16} />
                  </button>
                </div>
              ))}
            </div>
          ))
        )}
      </div>

      <div className="chat-sidebar-foot">
        <button className="chat-foot-btn ripple-host" onClick={onManageAssistants}>
          <PersonIcon size={16} />
          管理助手
        </button>
        <button className="chat-foot-btn ripple-host" onClick={onSettings}>
          <SettingsIcon size={16} />
          聊天设置
        </button>
      </div>

      <PopupMenu open={assistantMenu !== null} anchor={assistantMenu} onClose={() => setAssistantMenu(null)}>
        {assistants.map((item) => (
          <PopupItem
            key={item.id}
            onClick={() => {
              setAssistantMenu(null);
              onAssistant(item.id);
            }}
          >
            <span className="chat-avatar small">{item.avatar || "💬"}</span>
            <span style={{ flex: 1 }}>{item.name}</span>
            {item.id === assistantId && <span className="chat-check">✓</span>}
          </PopupItem>
        ))}
        <PopupSep />
        <PopupItem
          icon={<SettingsIcon size={16} />}
          onClick={() => {
            setAssistantMenu(null);
            onManageAssistants();
          }}
        >
          管理助手
        </PopupItem>
      </PopupMenu>

      <PopupMenu
        open={sessionMenu !== null}
        anchor={sessionMenu?.anchor ?? null}
        onClose={() => setSessionMenu(null)}
      >
        <PopupItem
          icon={<EditIcon size={16} />}
          onClick={() => {
            if (sessionMenu) onRename(sessionMenu.session);
            setSessionMenu(null);
          }}
        >
          重命名
        </PopupItem>
        <PopupItem
          icon={<DeleteIcon size={16} />}
          danger
          onClick={() => {
            if (sessionMenu) onDelete(sessionMenu.session);
            setSessionMenu(null);
          }}
        >
          删除会话
        </PopupItem>
      </PopupMenu>
    </aside>
  );
}

/** 重命名会话的弹窗（ChatPage 管状态，这里只画）。 */
export function RenameSessionDialog({
  session, busy, onCancel, onConfirm,
}: {
  session: ChatSessionSummary | null;
  busy: boolean;
  onCancel: () => void;
  onConfirm: (title: string) => void;
}) {
  const [title, setTitle] = useState("");
  useEffect(() => {
    if (session) setTitle(session.title);
  }, [session]);
  return (
    <Dialog open={session !== null} title="重命名会话" onClose={busy ? undefined : onCancel}>
      <input
        className="input"
        value={title}
        autoFocus
        onChange={(event) => setTitle(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === "Enter" && title.trim()) onConfirm(title.trim());
        }}
      />
      <div className="dialog-actions">
        <button className="btn btn-text dim" disabled={busy} onClick={onCancel}>取消</button>
        <button className="btn btn-text" disabled={busy || !title.trim()} onClick={() => onConfirm(title.trim())}>
          {busy ? "保存中…" : "保存"}
        </button>
      </div>
    </Dialog>
  );
}
/// 分叉会话：选目标助手 + 标题，复制整段会话（含变体与附件），原会话保持不动。
export function ForkSessionDialog({
  session, assistants, busy, onCancel, onConfirm,
}: {
  session: ChatSessionSummary | null;
  assistants: ChatAssistant[];
  busy: boolean;
  onCancel: () => void;
  onConfirm: (assistantId: string, title: string) => void;
}) {
  const [assistantId, setAssistantId] = useState("default");
  const [title, setTitle] = useState("");
  useEffect(() => {
    if (session) {
      setAssistantId(session.assistantId);
      setTitle(`${session.title}（分叉）`);
    }
  }, [session]);
  return (
    <Dialog open={session !== null} title="分叉会话" onClose={busy ? undefined : onCancel}>
      <p className="dialog-hint">
        整段会话（消息、变体、附件）复制一份，原会话不动。
      </p>
      <label className="dialog-field">
        <span>目标助手</span>
        <select className="input" value={assistantId} onChange={(event) => setAssistantId(event.target.value)}>
          {assistants.map((item) => (
            <option key={item.id} value={item.id}>{item.name}</option>
          ))}
        </select>
      </label>
      <label className="dialog-field">
        <span>标题</span>
        <input
          className="input"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter" && title.trim()) onConfirm(assistantId, title.trim());
          }}
        />
      </label>
      <div className="dialog-actions">
        <button className="btn btn-text dim" disabled={busy} onClick={onCancel}>取消</button>
        <button className="btn btn-text" disabled={busy || !title.trim()} onClick={() => onConfirm(assistantId, title.trim())}>
          {busy ? "分叉中…" : "分叉"}
        </button>
      </div>
    </Dialog>
  );
}
