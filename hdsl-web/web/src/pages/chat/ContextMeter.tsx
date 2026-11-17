import { useRef, useState } from "react";
import { PopupMenu } from "../../components/PopupMenu";

/** 千单位：999 / 1.2K / 12.3K / 128K / 1.2M。 */
export function tokenText(value: number | null | undefined): string {
  if (value == null || !Number.isFinite(value) || value <= 0) return "0";
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1).replace(/\.0$/, "")}M`;
  if (value >= 100_000) return `${Math.round(value / 1_000)}K`;
  if (value >= 10_000) return `${(value / 1_000).toFixed(1).replace(/\.0$/, "")}K`;
  if (value >= 1_000) return `${(value / 1_000).toFixed(1)}K`;
  return String(Math.round(value));
}

const RING = 30;
const STROKE = 3;
const RADIUS = (RING - STROKE) / 2;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

/**
 * 上下文占用环：分子是最近一次回复的 input tokens（系统提示 + 历史 + 附件），
 * 分母是当前模型的 maxContext。点一下看明细。
 */
export function ContextMeter({
  used, max, lastOutput, sessionInput, sessionOutput, disabled,
}: {
  used: number | null;
  max: number;
  lastOutput?: number | null;
  sessionInput: number;
  sessionOutput: number;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const anchor = useRef<HTMLButtonElement | null>(null);
  const ratio = used != null && max > 0 ? Math.min(1, used / max) : 0;
  const percent = used != null && max > 0 ? Math.round((used / max) * 1000) / 10 : null;
  const tone = ratio >= 0.95 ? "danger" : ratio >= 0.8 ? "warn" : "normal";
  const maxLabel = max > 0 ? tokenText(max) : "上限未知";
  const title = used == null
    ? "还没有可用的上下文用量"
    : `上下文 ${tokenText(used)} / ${maxLabel}${percent == null ? "" : `（${percent}%）`}`;

  return (
    <>
      <button
        ref={anchor}
        className={`context-meter ${tone}`}
        disabled={disabled}
        title={title}
        onClick={() => setOpen((value) => !value)}
      >
        <svg width={RING} height={RING} viewBox={`0 0 ${RING} ${RING}`} aria-hidden="true">
          <circle
            className="context-meter-track"
            cx={RING / 2} cy={RING / 2} r={RADIUS}
            fill="none" strokeWidth={STROKE}
          />
          <circle
            className="context-meter-value"
            cx={RING / 2} cy={RING / 2} r={RADIUS}
            fill="none" strokeWidth={STROKE} strokeLinecap="round"
            strokeDasharray={CIRCUMFERENCE}
            strokeDashoffset={CIRCUMFERENCE * (1 - ratio)}
            transform={`rotate(-90 ${RING / 2} ${RING / 2})`}
          />
        </svg>
        <span className="context-meter-text">
          {used == null ? "上下文 —" : `${tokenText(used)} / ${maxLabel}`}
        </span>
      </button>

      <PopupMenu open={open} anchor={anchor.current} onClose={() => setOpen(false)} className="chat-context-popup">
        <div className="chat-context-head">
          <span>上下文</span>
          <span className={`chat-context-percent ${tone}`}>{percent == null ? "—" : `${percent}%`}</span>
        </div>
        <div className="chat-context-row">
          <span>当前占用</span>
          <span>{used == null ? "—" : `${tokenText(used)} / ${maxLabel}`}</span>
        </div>
        <div className="chat-context-row">
          <span>本轮输出</span>
          <span>{lastOutput == null ? "—" : tokenText(lastOutput)}</span>
        </div>
        <div className="chat-context-sep" />
        <div className="chat-context-row">
          <span>会话累计输入</span>
          <span>{tokenText(sessionInput)}</span>
        </div>
        <div className="chat-context-row">
          <span>会话累计输出</span>
          <span>{tokenText(sessionOutput)}</span>
        </div>
        <div className="chat-context-hint">
          输入按每轮完整上下文计费，累计输入接近账单口径，不等于当前占用；
          当前占用是模型这一轮看到的 tokens。
        </div>
      </PopupMenu>
    </>
  );
}