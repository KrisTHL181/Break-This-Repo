import { useEffect, useMemo, useRef, useState } from "react";
import { PopupMenu } from "../../components/PopupMenu";
import { AddIcon, CheckIcon, KeyboardArrowDownIcon, SearchIcon, SettingsIcon } from "../../components/icons";
import type { ChatModelInfo } from "../../types";
import { ModelCaps } from "./ModelCaps";

export interface ModelGroup {
  name: string;
  label: string;
  vendor: string;
  models: ChatModelInfo[];
}

/** 模型选择器：按服务分组，可搜索；选中后同时切换会话的 account/model。 */
export function ModelPicker({
  groups, account, model, disabled, onPick, onManage, onAdd,
}: {
  groups: ModelGroup[];
  account: string | null;
  model: string | null;
  disabled: boolean;
  onPick: (account: string, model: string) => void;
  onManage: () => void;
  onAdd: () => void;
}) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const anchor = useRef<HTMLButtonElement | null>(null);
  const search = useRef<HTMLInputElement | null>(null);

  // 触屏上不自动聚焦搜索框：否则一点「切换模型」就弹出输入法，键盘又把列表顶掉。
  useEffect(() => {
    if (!open) {
      setQuery("");
      return;
    }
    if (!window.matchMedia("(pointer: fine)").matches) return;
    // 弹层首帧是 visibility:hidden（等定位），hidden 元素 focus 会被忽略
    const frame = requestAnimationFrame(() => requestAnimationFrame(() => search.current?.focus()));
    return () => cancelAnimationFrame(frame);
  }, [open]);

  const currentGroup = groups.find((group) => group.name === account) ?? null;
  const current = currentGroup?.models.find((item) => item.id === model)
    ?? groups.flatMap((group) => group.models).find((item) => item.id === model)
    ?? null;

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return groups
      .map((group) => ({
        ...group,
        models: group.models.filter((item) => !q || item.id.toLowerCase().includes(q)),
      }))
      .filter((group) => group.models.length > 0);
  }, [groups, query]);

  return (
    <>
      <button
        ref={anchor}
        className="chat-model-button"
        disabled={disabled}
        onClick={() => {
          // 输入框还留着焦点时，弹层里的搜索框会连带唤起键盘；先收起输入法。
          if (document.activeElement instanceof HTMLElement) document.activeElement.blur();
          setOpen((value) => !value);
        }}
        title={current ? `${current.id} · 上下文 ${current.capabilities.maxContext}` : "选择模型"}
      >
        <span className="chat-model-button-main">
          <span className="chat-model-button-label">{current?.id ?? "选择模型"}</span>
          {currentGroup && <span className="chat-model-button-provider">{currentGroup.label}</span>}
        </span>
        <KeyboardArrowDownIcon size={14} />
      </button>

      <PopupMenu
        open={open}
        anchor={anchor.current}
        onClose={() => setOpen(false)}
        className="chat-model-popup"
      >
        <div className="chat-model-popup-head">
          <SearchIcon size={14} />
          <input
            ref={search}
            value={query}
            placeholder="搜索模型或服务"
            onChange={(event) => setQuery(event.target.value)}
          />
        </div>
        <div className="chat-model-popup-list">
          {filtered.length === 0 ? (
            <div className="chat-model-popup-empty">
              {groups.length === 0 ? "还没有可用的模型服务" : "没有匹配的模型"}
            </div>
          ) : filtered.map((group) => (
            <div className="chat-model-popup-group" key={group.name}>
              <div className="chat-model-popup-provider">
                <span>{group.label}</span>
                <span className="chat-model-popup-count">{group.models.length}</span>
              </div>
              {group.models.map((item) => {
                const selected = group.name === account && item.id === model;
                return (
                  <button
                    key={`${group.name}\u0000${item.id}`}
                    className={`chat-model-popup-item${selected ? " selected" : ""}`}
                    onClick={() => {
                      onPick(group.name, item.id);
                      setOpen(false);
                    }}
                  >
                    <span className="chat-model-popup-id">{item.id}</span>
                    <span className="chat-model-popup-caps">
                      <ModelCaps caps={item.capabilities} />
                    </span>
                    {selected && <CheckIcon size={14} />}
                  </button>
                );
              })}
            </div>
          ))}
        </div>
        <div className="chat-model-popup-foot">
          <button
            onClick={() => {
              setOpen(false);
              onAdd();
            }}
          >
            <AddIcon size={14} />
            添加模型服务
          </button>
          <button
            onClick={() => {
              setOpen(false);
              onManage();
            }}
          >
            <SettingsIcon size={14} />
            管理模型服务
          </button>
        </div>
      </PopupMenu>
    </>
  );
}