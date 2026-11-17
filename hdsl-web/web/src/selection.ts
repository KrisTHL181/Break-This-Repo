// "当前选中实例"（实例列表页 radio 选择），持久化到 localStorage，
// 主页面启动面板与侧边栏"当前实例项"都以它为优先。

import { useSyncExternalStore } from "react";

const KEY = "hdsl.instance.selected";

let selected: string | null = localStorage.getItem(KEY);
const listeners = new Set<() => void>();

function emit(): void {
  for (const l of [...listeners]) l();
}

export function getSelectedInstanceId(): string | null {
  return selected;
}

export function setSelectedInstanceId(id: string | null): void {
  selected = id;
  if (id) localStorage.setItem(KEY, id);
  else localStorage.removeItem(KEY);
  emit();
}

export function useSelectedInstanceId(): string | null {
  return useSyncExternalStore(
    (l) => {
      listeners.add(l);
      return () => {
        listeners.delete(l);
      };
    },
    getSelectedInstanceId,
  );
}
