// 启动 → 自动开新标签页的弹窗管理。
//
// 用户在点击"启动"的瞬间同步 window.open("about:blank")（popup blocker 豁免），
// 保存引用；收到 RUNNING + url 事件后把该窗口导航到 /i/<id>/?token=…。
// 窗口被拦截（返回 null）或页面刷新导致引用丢失时，由运行中视图的 [打开 dsh] 按钮兜底。

const popups = new Map<string, Window | null>();

/** 在启动点击的同步上下文里调用；返回打开的窗口（被拦截时为 null）。 */
export function beginLaunch(instanceId: string): Window | null {
  const prev = popups.get(instanceId);
  if (prev && !prev.closed) prev.close();
  let win: Window | null = null;
  try {
    win = window.open("about:blank");
  } catch {
    win = null;
  }
  popups.set(instanceId, win);
  return win;
}

/** 启动失败时清理引用（组件需自行关闭窗口）。 */
export function forgetLaunch(instanceId: string): void {
  popups.delete(instanceId);
}

/** 取走并移除引用；返回 undefined 表示本标签页未发起过该实例的启动。 */
export function consumeLaunchPopup(instanceId: string): Window | null | undefined {
  const w = popups.get(instanceId);
  popups.delete(instanceId);
  return w;
}

export function hasPendingLaunch(instanceId: string): boolean {
  return popups.has(instanceId);
}
