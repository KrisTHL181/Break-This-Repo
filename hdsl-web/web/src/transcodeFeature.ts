// ---------------------------------------------------------------------------
// 转码功能开关的共享快照
//
// 开关本身是服务端状态（`<data>/transcode/feature.json`），这里只是一份给界面
// 用的缓存：设置页改完立刻广播，主侧栏据此决定「转码」入口出不出现。
// 拉不到就当关闭——功能关闭时除 `/api/transcode/feature` 与 `/terms` 外全是 404，
// 界面要能在这种「看起来不存在」的状态下安静地工作。
// ---------------------------------------------------------------------------

import { useSyncExternalStore } from "react";
import { api, ApiError, type TranscodeFeature } from "./api";

let feature: TranscodeFeature | null = null;
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

export function getTranscodeFeature(): TranscodeFeature | null {
  return feature;
}

/** 写回服务端回答的那一份；`null` 表示「当作关闭」。 */
export function setTranscodeFeature(next: TranscodeFeature | null): void {
  feature = next;
  emit();
}

export async function refreshTranscodeFeature(): Promise<void> {
  if (loading) return;
  loading = true;
  try {
    setTranscodeFeature(await api.transcode.feature());
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) setTranscodeFeature(null);
  } finally {
    loading = false;
  }
}

export function useTranscodeFeature(): TranscodeFeature | null {
  return useSyncExternalStore(subscribe, getTranscodeFeature, getTranscodeFeature);
}