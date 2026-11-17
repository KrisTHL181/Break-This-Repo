// ---------------------------------------------------------------------------
// 主题（Monet / Material You）
//
// 机制与桌面版一致：种子 #5C6BC0、ColorStyle FIDELITY、Contrast DEFAULT、Spec 2025。
// 启动时用 @material/material-color-utilities 生成亮/暗两套调色板，写入
// `--monet-*` CSS 自定义属性（浅色 = :root，暗色 = [data-theme="dark"]）。
// src/styles/monet.css 里的默认值与桌面版 assets/css/blue.css 逐字一致（浅色）、
// 与本文件生成结果一致（暗色），保证 JS 未运行时页面也正确。
// ---------------------------------------------------------------------------

import { useSyncExternalStore } from "react";
import {
  Hct,
  SchemeFidelity,
  argbFromHex,
  hexFromArgb,
  type DynamicScheme,
} from "@material/material-color-utilities";

/// 默认种子：桌面版用的那一个，`恢复默认` 也回到它。
export const DEFAULT_SEED = "#5C6BC0";

const SEED_KEY = "hdsl.theme.seed";
const STORAGE_KEY = "hdsl.theme.mode";

/// 种子色存在 localStorage：它和主题模式一样属于「这台设备上的我看什么舒服」，
/// 不上服务端——换浏览器回到默认值是可接受的，为了一个配色再开一套服务端设置存储不是。
function normalizeSeed(value: string | null): string {
  if (!value) return DEFAULT_SEED;
  const v = value.trim().toUpperCase();
  return /^#[0-9A-F]{6}$/.test(v) ? v : DEFAULT_SEED;
}

/** 当前种子色（未设置时为默认 #5C6BC0） */
export function getSeed(): string {
  return normalizeSeed(localStorage.getItem(SEED_KEY));
}

/** 导出为 --monet-<kebab> 的角色（与桌面版 blue.css 的变量名一一对应） */
const ROLES = [
  "primary",
  "onPrimary",
  "primaryContainer",
  "onPrimaryContainer",
  "primaryFixed",
  "primaryFixedDim",
  "onPrimaryFixed",
  "onPrimaryFixedVariant",
  "secondary",
  "onSecondary",
  "secondaryContainer",
  "onSecondaryContainer",
  "secondaryFixed",
  "secondaryFixedDim",
  "onSecondaryFixed",
  "onSecondaryFixedVariant",
  "tertiary",
  "onTertiary",
  "tertiaryContainer",
  "onTertiaryContainer",
  "tertiaryFixed",
  "tertiaryFixedDim",
  "onTertiaryFixed",
  "onTertiaryFixedVariant",
  "error",
  "onError",
  "errorContainer",
  "onErrorContainer",
  "surface",
  "onSurface",
  "surfaceDim",
  "surfaceBright",
  "surfaceContainerLowest",
  "surfaceContainerLow",
  "surfaceContainer",
  "surfaceContainerHigh",
  "surfaceContainerHighest",
  "surfaceVariant",
  "onSurfaceVariant",
  "background",
  "onBackground",
  "outline",
  "outlineVariant",
  "shadow",
  "scrim",
  "inverseSurface",
  "inverseOnSurface",
  "inversePrimary",
  "surfaceTint",
] as const;

type Role = (typeof ROLES)[number];
type Palette = Partial<Record<Role, string>>;

function kebab(role: Role): string {
  return role.replace(/[A-Z]/g, (m) => "-" + m.toLowerCase());
}

/** 透明度派生：主色/次色容器/surface/inverse-surface 的 -50/-80 变体（与 blue.css 同名同规则） */
function transparentDerivatives(p: Palette): Record<string, string> {
  const d: Record<string, string> = {};
  const put = (name: string, color: string | undefined, alpha: string) => {
    if (color) d[name] = `${color}${alpha}`;
  };
  put("primary-transparent-50", p.primary, "80");
  put("primary-transparent-80", p.primary, "CC");
  put("secondary-container-transparent-50", p.secondaryContainer, "80");
  put("secondary-container-transparent-80", p.secondaryContainer, "CC");
  put("surface-transparent-50", p.surface, "80");
  put("surface-transparent-80", p.surface, "CC");
  put("surface-container-low-transparent-80", p.surfaceContainerLow, "CC");
  put("inverse-surface-transparent-80", p.inverseSurface, "CC");
  put("on-surface-variant-transparent-38", p.onSurfaceVariant, "61");
  return d;
}

function generatePalette(seedHex: string, isDark: boolean): Palette {
  const seed = Hct.fromInt(argbFromHex(seedHex));
  // FIDELITY 风格（桌面版 HSLCToner 对应）、Contrast 0、Spec 2025
  const scheme: DynamicScheme = new SchemeFidelity(seed, isDark, 0, "2025");
  // 库私有结构：升级后字段可能改名/消失。结构没了就返回空 palette——
  // 调用方整体回落 monet.css 内嵌默认值，而不是连主题初始化一起炸掉。
  const colors = (scheme as unknown as {
    colors?: Record<string, () => { getArgb(s: DynamicScheme): number }>;
  }).colors;
  const palette: Palette = {};
  if (colors) {
    for (const role of ROLES) {
      const accessor = colors[role];
      if (typeof accessor === "function") {
        palette[role] = hexFromArgb(accessor().getArgb(scheme)).toUpperCase();
      }
    }
  }
  return palette;
}

function declarations(p: Palette): string {
  const lines = ROLES.filter((r) => p[r]).map((r) => `--monet-${kebab(r)}: ${p[r]};`);
  for (const [name, value] of Object.entries(transparentDerivatives(p))) {
    lines.push(`--monet-${name}: ${value};`);
  }
  return lines.join("\n  ");
}

/// 按当前种子重算亮/暗两套调色板并写进运行时样式表（覆盖 monet.css 里的同名字段）。
/// 换种子色就是重跑这一个函数——所有 `--monet-*` 消费者（卡片、按钮、侧栏、图表）
/// 跟着 CSS 变量一起变，不需要任何组件配合。
function applySeed(): void {
  try {
    const seed = getSeed();
    const light = generatePalette(seed, false);
    const dark = generatePalette(seed, true);
    let style = document.getElementById("monet-runtime");
    if (!style) {
      style = document.createElement("style");
      style.id = "monet-runtime";
      document.head.appendChild(style);
    }
    style.textContent = `:root {\n  ${declarations(light)}\n}\n[data-theme="dark"] {\n  ${declarations(dark)}\n}`;
  } catch {
    // 生成失败时回落到 monet.css 内嵌的默认值（与库输出一致）
  }
  applyResolvedTheme();
}

/** 启动时按存储的种子生成调色板（没存过就是默认 #5C6BC0） */
export function initTheme(): void {
  applySeed();
}

/** 换种子色：立刻生效并持久化；不是 `#RRGGBB` 的输入按默认值处理 */
export function setSeed(hex: string): void {
  const value = normalizeSeed(hex);
  if (value === DEFAULT_SEED) {
    localStorage.removeItem(SEED_KEY);
  } else {
    localStorage.setItem(SEED_KEY, value);
  }
  applySeed();
  for (const l of [...seedListeners]) l();
}

/** 回到默认种子 */
export function resetSeed(): void {
  setSeed(DEFAULT_SEED);
}

const seedListeners = new Set<() => void>();

function subscribeSeed(listener: () => void): () => void {
  seedListeners.add(listener);
  return () => {
    seedListeners.delete(listener);
  };
}

/** 订阅当前种子色（设置-外观页的取色器用） */
export function useThemeSeed(): string {
  return useSyncExternalStore(subscribeSeed, getSeed);
}

// ---------------------------------------------------------------------------
// 主题模式：auto（跟随系统）/ light / dark，localStorage 持久化
// ---------------------------------------------------------------------------

export type ThemeMode = "auto" | "light" | "dark";

export function getThemeMode(): ThemeMode {
  const v = localStorage.getItem(STORAGE_KEY);
  return v === "light" || v === "dark" ? v : "auto";
}

export function setThemeMode(mode: ThemeMode): void {
  localStorage.setItem(STORAGE_KEY, mode);
  applyResolvedTheme();
  emitModeChange();
}

function systemDark(): boolean {
  return window.matchMedia("(prefers-color-scheme: dark)").matches;
}

function applyResolvedTheme(): void {
  const mode = getThemeMode();
  const resolved = mode === "auto" ? (systemDark() ? "dark" : "light") : mode;
  document.documentElement.dataset.theme = resolved;
}

const modeListeners = new Set<() => void>();

function emitModeChange(): void {
  for (const l of [...modeListeners]) l();
}

function subscribeMode(listener: () => void): () => void {
  modeListeners.add(listener);
  return () => {
    modeListeners.delete(listener);
  };
}

/** 订阅当前主题模式（设置-外观页切换用） */
export function useThemeMode(): ThemeMode {
  return useSyncExternalStore(subscribeMode, getThemeMode);
}

/** 安装系统主题跟随监听；返回清理函数 */
export function bindSystemThemeListener(): () => void {
  const mq = window.matchMedia("(prefers-color-scheme: dark)");
  const onChange = () => {
    if (getThemeMode() === "auto") applyResolvedTheme();
  };
  mq.addEventListener("change", onChange);
  return () => mq.removeEventListener("change", onChange);
}
