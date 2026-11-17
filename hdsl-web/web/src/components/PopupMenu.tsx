import { useCallback, useEffect, useLayoutEffect, useRef, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";

export interface PopupMenuProps {
  open: boolean;
  /** 锚点元素：菜单在其周围弹出，空间不足时自动翻向另一侧（HMCL jfx-popup 风格） */
  anchor: HTMLElement | null;
  onClose: () => void;
  children: ReactNode;
  /** 追加在 .popup-menu 上的类名（宽度约束一类的场景） */
  className?: string;
}

interface PopupBox {
  left: number;
  top: number;
  maxHeight: number;
  maxWidth: number;
}

/**
 * 弹出菜单（jfx-popup 样式：surface 底、圆角 4、项 padding 8 16 间距 10）。
 *
 * 定位按「可见视口」（visualViewport）计算：手机键盘弹出时视口会缩小，
 * 菜单会跟着重新收进可见区域，而不是被键盘顶出屏幕或被 resize 关掉。
 */
export function PopupMenu({ open, anchor, onClose, children, className }: PopupMenuProps) {
  const ref = useRef<HTMLDivElement>(null);
  const [box, setBox] = useState<PopupBox | null>(null);

  const place = useCallback(() => {
    const el = ref.current;
    if (!anchor || !el) return;
    const vv = window.visualViewport;
    const ox = vv?.offsetLeft ?? 0;
    const oy = vv?.offsetTop ?? 0;
    const vw = vv?.width ?? window.innerWidth;
    const vh = vv?.height ?? window.innerHeight;
    const a = anchor.getBoundingClientRect();
    // 量「不受上限约束」的原始尺寸：先临时去掉 inline 上限，否则键盘弹出后
    // 收过一次的尺寸会被当成自然高度，键盘收起也不会再长回去。offset* 忽略
    // 入场动画的 transform，避免 0.9 倍缩放的瞬间被当成真实大小。
    const prevHeight = el.style.maxHeight;
    const prevWidth = el.style.maxWidth;
    el.style.maxHeight = "1000px";
    el.style.maxWidth = "none";
    const naturalWidth = el.offsetWidth;
    const naturalHeight = el.offsetHeight;
    el.style.maxHeight = prevHeight;
    el.style.maxWidth = prevWidth;

    const maxWidth = Math.max(160, vw - 16);
    const width = Math.min(naturalWidth || maxWidth, maxWidth);
    const left = Math.min(Math.max(a.left, ox + 8), Math.max(ox + 8, ox + vw - 8 - width));

    const below = oy + vh - 8 - (a.bottom + 2);
    const above = a.top - 2 - (oy + 8);
    const height = Math.min(naturalHeight || 0, Math.max(160, Math.max(above, below)));
    // 默认向下；只有下面放不下、上面更宽裕时才翻上去（输入区在底部时的常见情况）
    const upwards = below < (naturalHeight || 0) && above > below;
    const top = upwards ? a.top - 2 - height : a.bottom + 2;
    const maxHeight = Math.max(160, upwards ? above : below);

    setBox({
      left,
      top: Math.min(Math.max(top, oy + 8), Math.max(oy + 8, oy + vh - 8 - height)),
      maxHeight,
      maxWidth,
    });
  }, [anchor]);

  useLayoutEffect(() => {
    if (!open) {
      setBox(null);
      return;
    }
    place();
    // 首帧高度未知（visibility:hidden 也在文档流里，量得到），再补一帧适配异步内容
    const frame = requestAnimationFrame(place);
    return () => cancelAnimationFrame(frame);
  }, [open, place]);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: PointerEvent) => {
      const target = e.target as Node;
      if (ref.current?.contains(target)) return;
      // 锚点自己负责切换，不能在这里关掉再被 click 打开
      if (anchor?.contains(target)) return;
      onClose();
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    const onViewport = () => place();
    window.addEventListener("pointerdown", onDown, true);
    window.addEventListener("keydown", onKey);
    window.addEventListener("resize", onViewport);
    window.addEventListener("scroll", onViewport, true);
    window.visualViewport?.addEventListener("resize", onViewport);
    window.visualViewport?.addEventListener("scroll", onViewport);
    return () => {
      window.removeEventListener("pointerdown", onDown, true);
      window.removeEventListener("keydown", onKey);
      window.removeEventListener("resize", onViewport);
      window.removeEventListener("scroll", onViewport, true);
      window.visualViewport?.removeEventListener("resize", onViewport);
      window.visualViewport?.removeEventListener("scroll", onViewport);
    };
  }, [open, onClose, anchor, place]);

  if (!open) return null;

  const style = box
    ? { left: box.left, top: box.top, maxHeight: box.maxHeight, maxWidth: box.maxWidth }
    : { visibility: "hidden" as const };

  return createPortal(
    <div className={`popup-menu${className ? ` ${className}` : ""}`} ref={ref} style={style} role="menu">
      {children}
    </div>,
    document.body,
  );
}

export function PopupItem({
  icon,
  children,
  danger,
  onClick,
}: {
  icon?: ReactNode;
  children: ReactNode;
  danger?: boolean;
  onClick?: () => void;
}) {
  return (
    <button
      className={`popup-item${danger ? " danger" : ""}`}
      role="menuitem"
      onClick={(e) => {
        e.stopPropagation();
        onClick?.();
      }}
    >
      {icon}
      {children}
    </button>
  );
}

export function PopupSep() {
  return <div className="popup-sep" />;
}
