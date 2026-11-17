// 点击涟漪：JFXRippler 圆形扩散的 CSS 实现（base.css 中的 .ripple 动画）。
// 采用事件委托：给 document 挂一个 pointerdown 监听，命中 .ripple-host 时在其内部
// 生成一个从点击位置扩散的圆（尺寸 = 宿主对角线，颜色 = currentColor）。

export function installRipple(): () => void {
  const onPointerDown = (e: PointerEvent) => {
    const host = (e.target as HTMLElement | null)?.closest?.(".ripple-host");
    if (!host) return;
    if (host instanceof HTMLInputElement || host instanceof HTMLTextAreaElement) return;
    const rect = host.getBoundingClientRect();
    if (rect.width === 0 || rect.height === 0) return;
    const size = Math.max(rect.width, rect.height) * 2;
    const span = document.createElement("span");
    span.className = "ripple";
    span.style.width = `${size}px`;
    span.style.height = `${size}px`;
    span.style.left = `${e.clientX - rect.left - size / 2}px`;
    span.style.top = `${e.clientY - rect.top - size / 2}px`;
    host.appendChild(span);
    window.setTimeout(() => span.remove(), 500);
  };
  document.addEventListener("pointerdown", onPointerDown);
  return () => document.removeEventListener("pointerdown", onPointerDown);
}
