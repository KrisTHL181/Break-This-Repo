import { useState, type ReactNode } from "react";
import { useMobileLayout } from "../hooks";

/// 长说明的移动端降级：收成「一行摘要 + 详情按钮」，点开看全文、可收起；
/// 桌面端原样渲染 children，一字不动。用法：summary 传一行能自圆其说的摘要。
export function CollapsibleNote({ summary, children }: { summary: ReactNode; children: ReactNode }) {
  const mobile = useMobileLayout();
  const [open, setOpen] = useState(false);
  if (!mobile) return <>{children}</>;
  if (!open) {
    return (
      <div className="note-collapse">
        <span className="note-collapse-text">{summary}</span>
        <button className="btn btn-text ripple-host" style={{ height: 28 }} onClick={() => setOpen(true)}>
          详情
        </button>
      </div>
    );
  }
  return (
    <>
      {children}
      <div style={{ display: "flex", justifyContent: "flex-end" }}>
        <button className="btn btn-text dim ripple-host" style={{ height: 28 }} onClick={() => setOpen(false)}>
          收起
        </button>
      </div>
    </>
  );
}
