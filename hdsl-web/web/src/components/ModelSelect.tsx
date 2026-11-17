import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../api";
import type { ModelInfo } from "../types";
import { PopupItem, PopupMenu } from "./PopupMenu";

// 模块级缓存：同一个供应商在同一会话里只拉一次模型列表（弹窗反复开关不重拉）
const cache = new Map<string, ModelInfo[]>();
const inflight = new Map<string, Promise<ModelInfo[]>>();

function loadModels(vendorId: string): Promise<ModelInfo[]> {
  const hit = cache.get(vendorId);
  if (hit) return Promise.resolve(hit);
  const pending = inflight.get(vendorId);
  if (pending) return pending;
  const p = api
    .modelProvider(vendorId)
    .then((r) => {
      cache.set(vendorId, r.models);
      return r.models;
    })
    .finally(() => inflight.delete(vendorId));
  inflight.set(vendorId, p);
  return p;
}

function trimNum(n: number): string {
  return String(Math.round(n * 10) / 10);
}

/** 128000 → "128K"、1000000 → "1M"；null/0 不显示 */
function formatTokens(n: number | null): string | null {
  if (n === null || !Number.isFinite(n) || n <= 0) return null;
  if (n >= 1000000) return `${trimNum(n / 1000000)}M`;
  if (n >= 1000) return `${trimNum(n / 1000)}K`;
  return String(n);
}

function formatPrice(v: number | null): string | null {
  if (v === null || !Number.isFinite(v)) return null;
  return `$${v}`;
}

/** 副标题：id · 128K 上下文 · $0.27/$1.1 · beta（缺的字段整段不显示） */
function subtitle(m: ModelInfo): string {
  const parts = [m.id];
  const ctx = formatTokens(m.context);
  if (ctx) parts.push(`${ctx} 上下文`);
  const input = formatPrice(m.costInput);
  const output = formatPrice(m.costOutput);
  if (input && output) parts.push(`${input}/${output}`);
  if (m.status) parts.push(m.status);
  return parts.join(" · ");
}

export interface ModelSelectProps {
  value: string;
  onChange: (v: string) => void;
  /** 模型目录里的供应商 id；空串 = 还不知道是哪家，只提供手输 */
  vendorId: string;
  placeholder?: string;
  /** false = 只允许从列表里选；目录不可用时仍退回手输，不会锁死 */
  allowFreeText?: boolean;
}

/**
 * 受控的「输入框 + 过滤列表」：模型名可以点选，也一直可以手输。
 * 目录不可用 / 这家没有模型时退化成普通输入框，不挡路、不弹错。
 */
export function ModelSelect({
  value,
  onChange,
  vendorId,
  placeholder,
  allowFreeText = true,
}: ModelSelectProps) {
  const [models, setModels] = useState<ModelInfo[] | null>(() =>
    vendorId ? cache.get(vendorId) ?? null : [],
  );
  const [failed, setFailed] = useState(false);
  const [open, setOpen] = useState(false);
  // null = 输入框显示 value；字符串 = 用户正在过滤
  const [query, setQuery] = useState<string | null>(null);
  const [anchor, setAnchor] = useState<HTMLInputElement | null>(null);
  const setAnchorEl = useCallback((el: HTMLInputElement | null) => setAnchor(el), []);

  // 列表是异步来的，而人往往在它到之前就点了输入框：到了就自己弹出来，否则得点第二次。
  // `wasEmpty` 只在「空 → 非空」那一次放行，所以按 Esc 关掉之后不会又被自己弹开。
  const [focused, setFocused] = useState(false);
  const [wasEmpty, setWasEmpty] = useState(true);
  useEffect(() => {
    if (focused && wasEmpty && models !== null && models.length > 0) {
      setOpen(true);
    }
    setWasEmpty(models === null || models.length === 0);
  }, [focused, models]);

  useEffect(() => {
    if (!vendorId) {
      setModels([]);
      setFailed(false);
      return;
    }
    const hit = cache.get(vendorId);
    if (hit) {
      setModels(hit);
      setFailed(false);
      return;
    }
    // 供应商 id 可能是用户正在手打的（自定义供应商），抖一下再拉
    setModels(null);
    setFailed(false);
    let alive = true;
    const timer = window.setTimeout(() => {
      loadModels(vendorId)
        .then((m) => {
          if (alive) setModels(m);
        })
        .catch(() => {
          if (alive) {
            setModels([]);
            setFailed(true);
          }
        });
    }, 250);
    return () => {
      alive = false;
      window.clearTimeout(timer);
    };
  }, [vendorId]);

  // 列表展开时用捕获阶段吞掉 Esc，避免连外层 Dialog 一起关掉
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== "Escape") return;
      e.stopPropagation();
      e.preventDefault();
      setOpen(false);
      setQuery(null);
    };
    window.addEventListener("keydown", onKey, true);
    return () => window.removeEventListener("keydown", onKey, true);
  }, [open]);

  const close = useCallback(() => {
    setOpen(false);
    setQuery(null);
  }, []);

  const q = (query ?? "").trim().toLowerCase();
  const filtered = useMemo(() => {
    const list = models ?? [];
    if (!q) return list;
    return list.filter((m) => m.id.toLowerCase().includes(q) || m.name.toLowerCase().includes(q));
  }, [models, q]);

  const hasList = models !== null && models.length > 0;
  const editable = allowFreeText || !hasList;

  let hint: string | null = null;
  if (!vendorId) hint = "可直接手输模型名（留空由 harness 决定）";
  else if (models === null) hint = "正在加载模型列表…";
  else if (models.length === 0)
    hint = failed ? "模型目录不可用，可直接手输模型名" : "该供应商没有列出模型，可直接手输模型名";

  return (
    <div className="model-select">
      <input
        className="input"
        ref={setAnchorEl}
        value={query ?? value}
        placeholder={placeholder ?? "留空（由 harness 决定）"}
        readOnly={!editable}
        autoComplete="off"
        onChange={(e) => {
          setQuery(e.target.value);
          onChange(e.target.value);
          if (hasList) setOpen(true);
        }}
        onFocus={() => {
          setFocused(true);
          setQuery(null);
          if (hasList) setOpen(true);
        }}
        onBlur={() => setFocused(false)}
      />
      <PopupMenu open={open} anchor={anchor} onClose={close} className="model-menu">
        <PopupItem
          onClick={() => {
            onChange("");
            close();
          }}
        >
          <span className="model-option">
            <span className="model-option-name">留空（由 harness 决定）</span>
          </span>
        </PopupItem>
        {filtered.map((m) => (
          <PopupItem
            key={m.id}
            onClick={() => {
              onChange(m.id);
              close();
            }}
          >
            <span className="model-option">
              <span className="model-option-name">{m.name}</span>
              <span className="model-option-meta">{subtitle(m)}</span>
            </span>
          </PopupItem>
        ))}
        {filtered.length === 0 && <PopupItem onClick={close}>没有匹配的模型，可直接手输</PopupItem>}
      </PopupMenu>
      {hint && <span className="model-select-hint">{hint}</span>}
    </div>
  );
}
