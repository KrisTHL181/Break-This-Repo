import { useEffect, useState } from "react";
import { ConfirmDialog, Dialog } from "../../components/Dialog";
import { DeleteIcon } from "../../components/icons";
import { api } from "../../api";
import { toast } from "../../store";
import { AvatarView } from "./AvatarView";
import { AvatarCropDialog } from "./AvatarCropDialog";
import type { ChatAccountProfile, ChatAssistant } from "../../types";
import { errMsg } from "../../utils";

const EMPTY = {
  name: "",
  avatar: "💬",
  description: "",
  systemPrompt: "",
  openingMessage: "",
  account: "",
  model: "",
  temperature: "",
  maxTokens: "",
  reasoningLevel: "",
  quickPhrases: "",
};

/** 新建/编辑助手：提示词是核心，模型与参数是可选的默认值。 */
export function AssistantDialog({
  open, assistant, onClose, onSaved, onDeleted,
}: {
  open: boolean;
  assistant: ChatAssistant | null;
  onClose: () => void;
  onSaved: (assistant: ChatAssistant, isNew: boolean) => void;
  onDeleted: (id: string) => void;
}) {
  const [form, setForm] = useState({ ...EMPTY });
  const [accounts, setAccounts] = useState<ChatAccountProfile[]>([]);
  const [busy, setBusy] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [cropOpen, setCropOpen] = useState(false);

  useEffect(() => {
    if (!open) return;
    setConfirmDelete(false);
    if (assistant) {
      setForm({
        name: assistant.name,
        avatar: assistant.avatar || "💬",
        description: assistant.description,
        systemPrompt: assistant.systemPrompt,
        openingMessage: assistant.openingMessage,
        account: assistant.account ?? "",
        model: assistant.model ?? "",
        temperature: assistant.temperature == null ? "" : String(assistant.temperature),
        maxTokens: assistant.maxTokens == null ? "" : String(assistant.maxTokens),
        reasoningLevel: assistant.reasoningLevel ?? "",
        quickPhrases: assistant.quickPhrases.join("\n"),
      });
    } else {
      setForm({ ...EMPTY });
    }
    void api.chat.accounts().then((result) => setAccounts(result.accounts)).catch(() => setAccounts([]));
  }, [open, assistant]);

  const patch = (key: keyof typeof EMPTY, value: string) => setForm((current) => ({ ...current, [key]: value }));
  const preset = assistant?.preset === "deepseek-official";
  const visibleAccounts = preset
    ? accounts.filter((item) => item.vendor === "deepseek" || (item.endpoint ?? "").includes("deepseek.com"))
    : accounts;
  const selectedAccount = visibleAccounts.find((item) => item.name === form.account) ?? null;
  const models = selectedAccount?.modelList ?? [];

  const save = async () => {
    if (!form.name.trim()) {
      toast("error", "助手需要一个名字");
      return;
    }
    const body: Record<string, unknown> = {
      name: form.name.trim(),
      avatar: form.avatar.trim() || "💬",
      description: form.description.trim(),
      systemPrompt: form.systemPrompt,
      openingMessage: form.openingMessage,
      account: form.account.trim() || null,
      model: form.model.trim() || null,
      reasoningLevel: form.reasoningLevel || null,
      quickPhrases: form.quickPhrases.split("\n").map((line) => line.trim()).filter(Boolean),
    };
    if (form.temperature.trim()) body.temperature = Number(form.temperature);
    else body.temperature = null;
    if (form.maxTokens.trim()) body.maxTokens = Number(form.maxTokens);
    else body.maxTokens = null;

    setBusy(true);
    try {
      const saved = assistant
        ? await api.chat.updateAssistant(assistant.id, body)
        : await api.chat.createAssistant(body);
      toast("success", assistant ? "助手已更新" : "助手已创建");
      onSaved(saved, !assistant);
    } catch (error) {
      toast("error", `保存失败：${errMsg(error)}`);
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    if (!assistant) return;
    if (!confirmDelete) {
      setConfirmDelete(true);
      return;
    }
    setBusy(true);
    try {
      await api.chat.deleteAssistant(assistant.id);
      toast("success", "助手已删除，会话已迁移到默认助手");
      onDeleted(assistant.id);
    } catch (error) {
      toast("error", `删除失败：${errMsg(error)}`);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog
      open={open}
      title={assistant ? `编辑助手「${assistant.name}」` : "新建助手"}
      onClose={busy ? undefined : onClose}
      wide
      footer={
        <div className="dialog-actions" style={{ width: "100%" }}>
          {assistant && !assistant.builtin && (
            <button className="btn btn-text danger" disabled={busy} onClick={() => void remove()}>
              <DeleteIcon size={14} />
              {confirmDelete ? "确认删除（会话迁移到默认助手）" : "删除助手"}
            </button>
          )}
          {assistant?.builtin && (
            <button
              className="btn btn-text"
              disabled={busy}
              onClick={() => setForm((current) => ({
                ...current, systemPrompt: "", openingMessage: "", account: "", model: "",
                temperature: "", maxTokens: "", reasoningLevel: "",
              }))}
            >
              恢复默认
            </button>
          )}
          <span className="spacer" />
          <button className="btn btn-text dim" disabled={busy} onClick={onClose}>取消</button>
          <button className="btn btn-text" disabled={busy || !form.name.trim()} onClick={() => void save()}>
            {busy ? "保存中…" : "保存"}
          </button>
        </div>
      }
    >
      {preset && (
        <p className="chat-settings-desc">
          这是 DeepSeek 官方预设角色：固定使用 DeepSeek 官方协议（深度思考、官方联网搜索、读链接与来源引用），
          只能选择 DeepSeek 账户；复制出来的角色也保持这个限制。
        </p>
      )}
      <div className="chat-form-grid">
        <div className="form-row">
          <span className="form-label">头像</span>
          <div className="avatar-picker">
            <span className="avatar-picker-preview"><AvatarView value={form.avatar} /></span>
            <input
              className="input"
              value={form.avatar.startsWith("img:") ? "" : form.avatar}
              maxLength={8}
              placeholder={form.avatar.startsWith("img:") ? "已使用图片头像" : "emoji 或文字"}
              onChange={(event) => patch("avatar", event.target.value)}
            />
            <button className="btn btn-text" type="button" onClick={() => setCropOpen(true)}>上传图片</button>
            {form.avatar.startsWith("img:") && (
              <button className="btn btn-text dim" type="button" onClick={() => patch("avatar", "💬")}>清除</button>
            )}
          </div>
        </div>
        <label className="form-row">
          <span className="form-label">名称</span>
          <input className="input" value={form.name} onChange={(event) => patch("name", event.target.value)} />
        </label>
      </div>
      <label className="form-row">
        <span className="form-label">描述</span>
        <input className="input" value={form.description} onChange={(event) => patch("description", event.target.value)} />
      </label>

      <label className="form-row">
        <span className="form-label">
          系统提示词
          <span className="chat-form-hint">约 {Math.round(form.systemPrompt.length / 3)} tokens</span>
        </span>
        <textarea
          className="input chat-prompt-area"
          value={form.systemPrompt}
          rows={8}
          placeholder="例如：你是一个只回答中文的技术助手……"
          onChange={(event) => patch("systemPrompt", event.target.value)}
        />
      </label>

      <label className="form-row">
        <span className="form-label">开场白（只在新会话出现一次）</span>
        <textarea
          className="input"
          value={form.openingMessage}
          rows={3}
          onChange={(event) => patch("openingMessage", event.target.value)}
        />
      </label>

      <div className="chat-form-grid">
        <label className="form-row">
          <span className="form-label">默认账户</span>
          <select
            className="input"
            value={form.account}
            onChange={(event) => {
              patch("account", event.target.value);
              patch("model", "");
            }}
          >
            <option value="">跟随聊天默认</option>
            {visibleAccounts.map((account) => (
              <option key={account.name} value={account.name}>{account.label || account.name}</option>
            ))}
          </select>
        </label>
        <label className="form-row">
          <span className="form-label">默认模型</span>
          <input
            className="input"
            list={`assistant-models-${assistant?.id ?? "new"}`}
            value={form.model}
            placeholder={models.length ? "选择或直接输入" : "可留空，发送时再选"}
            onChange={(event) => patch("model", event.target.value)}
          />
          <datalist id={`assistant-models-${assistant?.id ?? "new"}`}>
            {models.map((item) => <option key={item.id} value={item.id} />)}
          </datalist>
        </label>
      </div>

      <div className="chat-form-grid three">
        <label className="form-row">
          <span className="form-label">temperature</span>
          <input className="input" type="number" step="0.1" min="0" max="2" value={form.temperature} onChange={(event) => patch("temperature", event.target.value)} />
        </label>
        <label className="form-row">
          <span className="form-label">max tokens</span>
          <input className="input" type="number" min="1" value={form.maxTokens} onChange={(event) => patch("maxTokens", event.target.value)} />
        </label>
        <label className="form-row">
          <span className="form-label">思考强度</span>
          <select className="input" value={form.reasoningLevel} onChange={(event) => patch("reasoningLevel", event.target.value)}>
            <option value="">默认</option>
            <option value="off">关</option>
            <option value="low">低</option>
            <option value="medium">中</option>
            <option value="high">高</option>
          </select>
        </label>
      </div>

      <label className="form-row">
        <span className="form-label">常用短语（每行一条，可选）</span>
        <textarea className="input" rows={3} value={form.quickPhrases} onChange={(event) => patch("quickPhrases", event.target.value)} />
      </label>

      <AvatarCropDialog
        open={cropOpen}
        onClose={() => setCropOpen(false)}
        onUploaded={(id) => patch("avatar", `img:${id}`)}
      />
    </Dialog>
  );
}
/** 助手管理：列表 + 新建/编辑/删除；删除时后端把会话迁移到默认助手。 */
export function AssistantManagerDialog({
  open, assistants, onClose, onCreate, onEdit, onChanged, onDeleted,
}: {
  open: boolean;
  assistants: ChatAssistant[];
  onClose: () => void;
  onCreate: () => void;
  onEdit: (assistant: ChatAssistant) => void;
  onChanged: () => void;
  onDeleted: (id: string) => void;
}) {
  const [deleteTarget, setDeleteTarget] = useState<ChatAssistant | null>(null);
  const [busy, setBusy] = useState(false);

  const remove = async () => {
    if (!deleteTarget) return;
    setBusy(true);
    try {
      await api.chat.deleteAssistant(deleteTarget.id);
      toast("success", "助手已删除，会话已迁移到默认助手");
      setDeleteTarget(null);
      onDeleted(deleteTarget.id);
      onChanged();
    } catch (error) {
      toast("error", `删除失败：${errMsg(error)}`);
    } finally {
      setBusy(false);
    }
  };

  const duplicate = async (assistant: ChatAssistant) => {
    setBusy(true);
    try {
      await api.chat.duplicateAssistant(assistant.id);
      toast("success", "已复制为一个新的 DeepSeek 角色");
      onChanged();
    } catch (error) {
      toast("error", `复制失败：${errMsg(error)}`);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <Dialog
        open={open}
        title="管理助手"
        onClose={onClose}
        wide
        footer={
          <div className="dialog-actions" style={{ width: "100%" }}>
            <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
              助手之间不共享系统提示词与会话上下文；切换助手会从新会话开始。
            </span>
            <span className="spacer" />
            <button className="btn btn-text dim" onClick={onClose}>关闭</button>
            <button className="btn btn-text" onClick={onCreate}>新建助手</button>
          </div>
        }
      >
        <div className="chat-assistant-list">
          {assistants.map((assistant) => (
            <div className="chat-assistant-row" key={assistant.id}>
              <span className="chat-avatar"><AvatarView value={assistant.avatar} /></span>
              <div className="chat-assistant-row-text">
                <div>
                  {assistant.name}
                  {assistant.builtin && <span className="tag plain" style={{ marginLeft: 6 }}>内置</span>}
                  {assistant.preset && <span className="tag plain" style={{ marginLeft: 6 }}>DeepSeek 官方</span>}
                </div>
                <div className="chat-assistant-row-sub">
                  {assistant.description || (assistant.systemPrompt ? "有系统提示词" : "没有系统提示词")}
                  {assistant.model ? ` · ${assistant.model}` : ""}
                </div>
              </div>
              <button className="btn btn-text" onClick={() => onEdit(assistant)}>编辑</button>
              {assistant.preset && (
                <button className="btn btn-text" disabled={busy} onClick={() => void duplicate(assistant)}>
                  复制为角色
                </button>
              )}
              {!assistant.builtin && (
                <button className="btn btn-text danger" onClick={() => setDeleteTarget(assistant)}>删除</button>
              )}
            </div>
          ))}
        </div>
      </Dialog>
      <ConfirmDialog
        open={deleteTarget !== null}
        title="删除助手"
        message={`删除助手「${deleteTarget?.name ?? ""}」？它的会话会迁移到「默认助手」，不会丢消息。`}
        confirmLabel="删除"
        danger
        busy={busy}
        onConfirm={() => void remove()}
        onCancel={() => setDeleteTarget(null)}
      />
    </>
  );
}
