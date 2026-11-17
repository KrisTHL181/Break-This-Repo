import { useEffect, useReducer, useSyncExternalStore } from "react";
import { useAppState } from "./store";

/**
 * 移动版式判定：与 styles/mobile.css 顶部那两个媒体查询**逐字一致**，改一处必须改两处。
 * 宽度 ≤760（竖屏手机）或高度 ≤520（横屏手机，如 844×390）都算移动版式。
 *
 * 纯版式判断（抽屉、贴底操作栏走 CSS），这里只用于 React 需要改结构的少数地方
 * （标题栏汉堡按钮、抽屉遮罩）。旋转屏幕或缩放窗口会即时生效。
 */
export const MOBILE_QUERY = "(max-width: 760px), (max-height: 520px)";

function subscribeMobile(listener: () => void): () => void {
  const query = window.matchMedia(MOBILE_QUERY);
  query.addEventListener("change", listener);
  return () => query.removeEventListener("change", listener);
}

export function useMobileLayout(): boolean {
  return useSyncExternalStore(subscribeMobile, () => window.matchMedia(MOBILE_QUERY).matches);
}

/**
 * 运行时长平滑计时：以 store 里最近一次拿到的 uptimeSec 为基准，
 * 本地每秒自增，无需轮询后端。
 */
export function useTickingUptime(instanceId: string | null, uptimeSec?: number): number | null {
  const s = useAppState();
  const last = instanceId ? s.lastUptime[instanceId] : undefined;
  const base =
    last ?? (typeof uptimeSec === "number" ? { sec: uptimeSec, at: Date.now() } : null);
  const [, tick] = useReducer((x: number) => x + 1, 0);

  useEffect(() => {
    if (!base) return;
    const timer = window.setInterval(() => tick(), 1000);
    return () => window.clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [base?.at, base?.sec, Boolean(base)]);

  return base ? base.sec + Math.floor((Date.now() - base.at) / 1000) : null;
}
