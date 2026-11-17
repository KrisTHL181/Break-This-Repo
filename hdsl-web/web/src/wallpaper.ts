// ---------------------------------------------------------------------------
// 壁纸
//
// 可选的两类：随面板打包的内置壁纸（StaticServlet 从 jar 里发），以及用户自己
// 上传、存在 `<data>/wallpapers/` 的那些（见 WallpaperApiServlet）。
//
// 「选了哪一张」存在 localStorage，和主题模式、种子色同一套做法：它是这台设备
// 上的观感偏好，不是服务端状态。列表本身每次进页面从服务端拉（上传的可能在别的
// 浏览器里加过），拉不到就只剩内置的那几张——默认壁纸因此永远可用，登录页也不会
// 因为接口没通而白屏。
// ---------------------------------------------------------------------------

import { useSyncExternalStore } from "react";
import { api } from "./api";
import type { Wallpaper } from "./types";

const KEY = "hdsl.wallpaper";

/// 随面板打包的内置壁纸，与 `WallpaperApiServlet.BUILTIN` 一一对应（改那边要改这边）。
/// 后端列表拉不到时就靠这份兜底，保证默认壁纸永远可用、登录页不会白屏。
const BUILTIN_WALLPAPERS = ["2015-06-22.jpg", "2016-02-25.jpg", "2021-08-26.jpg"];

/// 没选过的时候用的那张——与改版前的背景一致。
const DEFAULT_WALLPAPER = BUILTIN_WALLPAPERS[BUILTIN_WALLPAPERS.length - 1];

const FALLBACK: Wallpaper[] = BUILTIN_WALLPAPERS.map((name) => ({
  id: "builtin-" + name.replace(/\.[a-z]+$/i, ""),
  name,
  url: `assets-img/wallpapers/${name}`,
  builtin: true,
}));

/// 没选过、或选中的那张已经不存在时用的那一个。
const DEFAULT_ID = "builtin-" + DEFAULT_WALLPAPER.replace(/\.[a-z]+$/i, "");
const FALLBACK_URL = `assets-img/wallpapers/${DEFAULT_WALLPAPER}`;

let list: Wallpaper[] = FALLBACK;
let loading = false;
let loaded = false;
const listeners = new Set<() => void>();

function emit(): void {
  for (const l of [...listeners]) l();
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** 当前可选的壁纸（内置在前；没拉过列表时只有内置的那几张） */
export function getWallpapers(): Wallpaper[] {
  return list;
}

export function wallpapersLoaded(): boolean {
  return loaded;
}

/** 从服务端刷新可选列表；失败保留上一次的（首次失败就是内置那几张） */
export async function refreshWallpapers(): Promise<void> {
  if (loading) return;
  loading = true;
  try {
    const r = await api.wallpapers();
    list = r.wallpapers && r.wallpapers.length > 0 ? r.wallpapers : FALLBACK;
    loaded = true;
    emit();
  } catch {
    // 拿不到列表不影响用：内置壁纸在本地就有
  } finally {
    loading = false;
  }
}

/** 选中的壁纸 id；没选过就是默认那张 */
export function getWallpaperId(): string {
  return localStorage.getItem(KEY) || DEFAULT_ID;
}

export function setWallpaperId(id: string): void {
  if (id === DEFAULT_ID) {
    localStorage.removeItem(KEY);
  } else {
    localStorage.setItem(KEY, id);
  }
  emit();
}

/** 选中那张的 URL；被删掉的、或 id 认不出来的，回落到默认壁纸 */
export function getWallpaperUrl(): string {
  const id = getWallpaperId();
  return list.find((w) => w.id === id)?.url ?? FALLBACK_URL;
}

/** 上传一张；成功即进列表（后端会回规整好的那一条） */
export async function uploadWallpaper(file: File): Promise<Wallpaper> {
  const created = await api.uploadWallpaper(file);
  list = [...list, created];
  emit();
  return created;
}

/** 删除一张上传的；正被选中的话回到默认壁纸 */
export async function deleteWallpaper(id: string): Promise<void> {
  await api.deleteWallpaper(id);
  list = list.filter((w) => w.id !== id);
  if (getWallpaperId() === id) setWallpaperId(DEFAULT_ID);
  emit();
}

/** 订阅壁纸列表（设置-外观页的选择器用） */
export function useWallpapers(): Wallpaper[] {
  return useSyncExternalStore(subscribe, getWallpapers);
}

/** 订阅当前选中的壁纸 URL（背景那两处用） */
export function useWallpaperUrl(): string {
  return useSyncExternalStore(subscribe, getWallpaperUrl);
}

/** 订阅当前选中的壁纸 id（设置页标出「正在用这张」） */
export function useWallpaperId(): string {
  return useSyncExternalStore(subscribe, getWallpaperId);
}
