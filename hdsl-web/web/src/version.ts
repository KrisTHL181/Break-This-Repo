// 服务端版本号（标题栏 "Hello DeepSeek! Launcher vX.Y.Z" 用），独立于全局 store 的小订阅。

import { useSyncExternalStore } from "react";
import { api } from "./api";

let version: string | null = null;
const listeners = new Set<() => void>();

function emit(): void {
  for (const l of [...listeners]) l();
}

export async function refreshServerVersion(): Promise<void> {
  try {
    const h = await api.health();
    if (h.version && h.version !== version) {
      version = h.version;
      emit();
    }
  } catch {
    // 拿不到版本时标题只显示启动器名称
  }
}

export function getServerVersion(): string | null {
  return version;
}

export function useServerVersion(): string | null {
  return useSyncExternalStore(
    (l) => {
      listeners.add(l);
      return () => {
        listeners.delete(l);
      };
    },
    getServerVersion,
  );
}
