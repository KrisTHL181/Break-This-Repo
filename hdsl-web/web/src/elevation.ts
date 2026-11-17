import { useEffect, useState } from "react";

/**
 * 需要开发者密码时的一次性提示框。
 *
 * 后端在三种情况下会拒绝一个**本来该成功**的请求：守卫把实例操作挡住了
 * （`developer_elevation_required`）、终端要求提权、以及 `/i/*` 的提权页。前两种发生在
 * 页面里，直接弹一个输入框比让用户去「设置 → 开发者」绕一圈好得多——尤其是"启动实例"
 * 这种点一下就该有反应的按钮。
 *
 * 这里的队列保证同一时刻只有一个输入框；`withElevation`（见 actions.ts）在用户输入正确
 * 密码后自动重试原来的操作一次。
 */

type Waiter = (ok: boolean) => void;

let waiters: Waiter[] = [];
let seq = 0;
let openSince: number | null = null;
const listeners = new Set<() => void>();

function emit(): void {
  for (const listener of [...listeners]) listener();
}

/** 请求一次开发者密码。用户取消返回 false。 */
export function requestDeveloperPassword(): Promise<boolean> {
  return new Promise<boolean>((resolve) => {
    waiters.push(resolve);
    openSince = ++seq;
    emit();
  });
}

/** 提示框当前是否打开（供宿主组件渲染）。 */
export function useElevationPromptOpen(): boolean {
  const [, force] = useState(0);
  useEffect(() => {
    const listener = () => force((value) => value + 1);
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  }, []);
  return openSince !== null;
}

/** 关闭提示框：`ok` 决定等待者是否重试。 */
export function settleElevation(ok: boolean): void {
  const pending = waiters;
  waiters = [];
  openSince = null;
  emit();
  for (const waiter of pending) waiter(ok);
}

/** 只认"确实缺提权"这一种拒绝——别的 403 不能弹密码框。 */
export function isElevationRequired(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error);
  return message.includes("developer_elevation_required") || message.includes("developer_password_not_set");
}
