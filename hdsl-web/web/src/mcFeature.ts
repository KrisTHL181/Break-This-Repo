// ---------------------------------------------------------------------------
// MC 服务端功能开关的共享快照
//
// 开关本身是服务端状态（`<data>/minecraft/feature.json`），这里只是一份给界面用的
// 缓存：品牌实例管理里改完立刻广播，实例列表据此决定「MC 服务器实例」入口出不出现。
// 拉不到就当关闭——功能关闭时 `/api/mc/feature` 之外的路径全是 404，界面要能在
// 这种「看起来不存在」的状态下安静地工作。
// ---------------------------------------------------------------------------

import { useSyncExternalStore } from "react";
import { api, ApiError, type McFeature } from "./api";

let feature: McFeature | null = null;
let loading = false;
const listeners = new Set<() => void>();

function emit(): void {
  for (const listener of [...listeners]) listener();
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function getMcFeature(): McFeature | null {
  return feature;
}

/** 写回服务端回答的那一份；`null` 表示「当作关闭」。 */
export function setMcFeature(next: McFeature | null): void {
  feature = next;
  emit();
}

export async function refreshMcFeature(): Promise<void> {
  if (loading) return;
  loading = true;
  try {
    setMcFeature(await api.mc.feature());
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) setMcFeature(null);
  } finally {
    loading = false;
  }
}

export function useMcFeature(): McFeature | null {
  return useSyncExternalStore(subscribe, getMcFeature, getMcFeature);
}
