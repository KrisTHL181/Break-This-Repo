import type { BrandId } from "./types";

/// 首屏兜底：能力接口（`GET /api/brands/capabilities`）到达之前，导航与启动面板
/// 已经要决定把这些品牌当网页品牌还是纯终端品牌。真正的来源是服务端
/// `Brand.kind()`；这份列表只负责第一帧，`setTuiBrands()` 一到就被替换，
/// 因此新增 TUI 品牌不会再出现“后端有、前端漏改”的漂移。
export const TUI_BRANDS: ReadonlySet<BrandId> = new Set(["qwen", "mimo", "minimax"]);

let fromServer: ReadonlySet<string> | null = null;

/// 用服务端声明的 TUI 品牌替换兜底列表。
export function setTuiBrands(ids: Iterable<string>): void {
  fromServer = new Set(ids);
}

export function isTuiBrand(brand: string): boolean {
  return (fromServer ?? TUI_BRANDS).has(brand as BrandId);
}