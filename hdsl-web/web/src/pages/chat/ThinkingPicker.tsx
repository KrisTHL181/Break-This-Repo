import { useRef, useState } from "react";
import { PopupMenu } from "../../components/PopupMenu";
import { CheckIcon, KeyboardArrowDownIcon } from "../../components/icons";
import type { ChatReasoningMode } from "../../types";

interface Option {
  value: string;
  label: string;
  hint: string;
}

/**
 * 模型声明（models.dev `reasoning_options`）→ 菜单项。
 * - toggle：只有开 / 关；
 * - effort：模型给了哪些英文档就只展示哪些（低/中/高/极高），没有 none 就不给「关」；
 * - budget：数字预算，选项里直接标出 tokens；
 * - forced / none：不给选择。
 */
function optionsFor(mode: ChatReasoningMode | undefined): Option[] {
  if (!mode || mode.kind === "none") return [];
  if (mode.kind === "forced") return [];
  if (mode.kind === "toggle") {
    return [
      { value: "on", label: "开", hint: "允许模型展开思考" },
      { value: "off", label: "关", hint: "思考全关，最快" },
    ];
  }
  if (mode.kind === "budget") {
    const options: Option[] = [];
    if (mode.canDisable) {
      options.push({ value: "off", label: "关", hint: "不发 thinking，最快" });
    }
    const budget = (tokens: number) => {
      const min = mode.minBudget > 0 ? mode.minBudget : 1024;
      const max = mode.maxBudget > 0 ? mode.maxBudget : 32768;
      return Math.min(Math.max(tokens, min), max);
    };
    options.push(
      { value: "low", label: "低", hint: `${budget(1024).toLocaleString()} tokens` },
      { value: "medium", label: "中", hint: `${budget(4096).toLocaleString()} tokens` },
      { value: "high", label: "高", hint: `${budget(16384).toLocaleString()} tokens` },
    );
    if (mode.maxBudget === 0 || mode.maxBudget >= 32768) {
      options.push({ value: "xhigh", label: "极高", hint: `${budget(32768).toLocaleString()} tokens` });
    }
    return options;
  }
  const efforts = mode.efforts ?? [];
  const has = (...values: string[]) => efforts.length === 0 || values.some((value) => efforts.includes(value));
  const options: Option[] = [];
  if (mode.canDisable || efforts.includes("none")) {
    options.push({ value: "off", label: "关", hint: efforts.includes("none") ? "none" : "不展开思考" });
  }
  if (has("minimal") || has("low")) {
    const value = efforts.includes("low") ? "low" : "minimal";
    options.push({ value, label: "低", hint: value === "minimal" ? "minimal" : "low" });
  }
  if (has("medium")) {
    options.push({ value: "medium", label: "中", hint: "medium" });
  }
  if (has("high")) {
    options.push({ value: "high", label: "高", hint: "high" });
  }
  if (has("xhigh") || has("max")) {
    const value = efforts.includes("xhigh") ? "xhigh" : "max";
    options.push({ value, label: "极高", hint: value });
  }
  return options;
}

export function thinkingLabel(level: string | null | undefined): string {
  if (!level) return "默认";
  switch (level) {
    case "off": return "关";
    case "on": return "开";
    case "minimal": return "低";
    case "low": return "低";
    case "medium": return "中";
    case "high": return "高";
    case "xhigh": return "极高";
    case "max": return "极高";
    default: return level;
  }
}

/** 输入区的思考挡位选择器；选项完全由模型声明的控制方式决定。 */
export function ThinkingPicker({
  level, mode, disabled, onPick,
}: {
  level: string;
  mode?: ChatReasoningMode;
  disabled: boolean;
  onPick: (level: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const anchor = useRef<HTMLButtonElement | null>(null);
  const options = optionsFor(mode);

  if (mode?.kind === "forced") {
    return (
      <span className="chat-thinking-button muted" title="这个模型强制思考，没有开关或挡位">
        思考·强制
      </span>
    );
  }
  if (options.length === 0) {
    return null;
  }

  return (
    <>
      <button
        ref={anchor}
        className={`chat-thinking-button${level && level !== "off" ? " active" : ""}${level === "off" ? " muted" : ""}`}
        disabled={disabled}
        title="思考挡位：控制模型展开多少思考"
        onClick={() => {
          if (document.activeElement instanceof HTMLElement) document.activeElement.blur();
          setOpen((value) => !value);
        }}
      >
        思考·{thinkingLabel(level)}
        <KeyboardArrowDownIcon size={12} />
      </button>

      <PopupMenu
        open={open}
        anchor={anchor.current}
        onClose={() => setOpen(false)}
        className="chat-thinking-popup"
      >
        <button
          className={`chat-model-popup-item${!level ? " selected" : ""}`}
          onClick={() => {
            onPick("");
            setOpen(false);
          }}
        >
          <span className="chat-thinking-text">
            <span className="chat-thinking-name">默认</span>
            <span className="chat-thinking-hint">用它自己的默认思考设置</span>
          </span>
          {!level && <CheckIcon size={14} />}
        </button>
        {options.map((item) => (
          <button
            key={item.value}
            className={`chat-model-popup-item${level === item.value ? " selected" : ""}`}
            onClick={() => {
              onPick(item.value);
              setOpen(false);
            }}
          >
            <span className="chat-thinking-text">
              <span className="chat-thinking-name">{item.label}</span>
              <span className="chat-thinking-hint">{item.hint}</span>
            </span>
            {level === item.value && <CheckIcon size={14} />}
          </button>
        ))}
      </PopupMenu>
    </>
  );
}