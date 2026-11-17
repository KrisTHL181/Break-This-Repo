import { useEffect, useRef, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { CloseIcon } from "./icons";

export interface DialogProps {
  open: boolean;
  title: ReactNode;
  children: ReactNode;
  onClose?: () => void;
  /** 点遮罩是否关闭（审批类对话框应传 false） */
  closeOnBackdrop?: boolean;
  wide?: boolean;
  /** 不显示右上角关闭按钮 */
  hideClose?: boolean;
  /** 固定在滚动区外面的按钮行：正文很长时按钮仍然可见。 */
  footer?: ReactNode;
}

/** HMCL 弹窗：遮罩 rgba(0,0,0,0.1)、surface-container-high、圆角 4、padding 24 24 16 24，缩放淡入。 */
export function Dialog({ open, title, children, onClose, closeOnBackdrop = true, wide, hideClose, footer }: DialogProps) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape" && closeOnBackdrop) onClose?.();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose, closeOnBackdrop]);

  if (!open) return null;

  return createPortal(
    <div
      className="dialog-overlay"
      onMouseDown={(e) => {
        if (closeOnBackdrop && e.target === e.currentTarget) onClose?.();
      }}
    >
      <div className={`dialog${wide ? " wide" : ""}`} role="dialog" aria-modal="true" ref={ref}>
        <div className="dialog-title">
          <span style={{ flex: 1, minWidth: 0 }}>{title}</span>
          {!hideClose && onClose && (
            <button className="icon-btn on-variant" onClick={onClose} aria-label="关闭">
              <CloseIcon size={18} />
            </button>
          )}
        </div>
        <div className="dialog-body">{children}</div>
        {footer && <div className="dialog-footer">{footer}</div>}
      </div>
    </div>,
    document.body,
  );
}

export interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/** 确认对话框：标题 + 正文 + 右对齐按钮区（确认 = primary 文字、取消 = on-surface-variant 文字）。 */
export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = "确定",
  cancelLabel = "取消",
  danger,
  busy,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <Dialog open={open} title={title} onClose={busy ? undefined : onCancel}>
      <div style={{ maxWidth: 400, lineHeight: 1.6 }}>{message}</div>
      <div className="dialog-actions">
        <button className="btn btn-text dim" onClick={onCancel} disabled={busy}>
          {cancelLabel}
        </button>
        <button
          className={`btn btn-text${danger ? " danger" : ""}`}
          onClick={onConfirm}
          disabled={busy}
        >
          {busy ? "…" : confirmLabel}
        </button>
      </div>
    </Dialog>
  );
}
