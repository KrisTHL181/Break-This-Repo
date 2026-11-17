# 浏览器兼容底线

面板 SPA 的兼容底线：**Firefox 115 ESR**——Windows 7/8/8.1 上最后一个受维护的浏览器版本
（Mozilla 对旧平台的 115 ESR 延续安全更新）。同伴底线：Chrome/Edge 109（Chromium 系在
Win7 的最后一版）、Safari/iOS 15.6。基准声明在 `web/package.json` 的 `browserslist`。

## 责任边界

- **面板自身**（登录页、实例/品牌/下载/账户/设置各页）：由本文档的底线覆盖。
- **`/i/{id}/` 反代里的 dsh web、Kimi Code、OpenCode、Claude Web、ZCode 页面**：是上游的
  构建产物，兼容性归上游管。底线只保证「能在面板里正常管理这些实例」，不保证它们的
  网页界面在老浏览器里可用。

## 现状结论（2026-10-19 排查）

- **JS 语法**：Vite `build.target: es2020` 经 esbuild 转译，ES2022+ 语法到不了产物。
- **JS API**：全库未使用 Firefox 115 缺失的 API（`Array.fromAsync`、`Object.groupBy`、
  `Promise.withResolvers`、正则 `v` 标志/lookbehind 等）。当前用到的最新 API 是
  `AbortSignal.timeout`（FF 100+）与 `navigator.clipboard.writeText`（FF 63+，仅 HTTPS，
  调用处已兜底）。
- **CSS**：`env()` 与 `safe-area-inset-*` 自 **Firefox 65 / Chrome 69 / Safari 11** 起支持
  （MDN browser-compat-data），115 ESR 直接可用——**不需要**为它们写双声明兜底；桌面浏览器
  把安全区算作 0。其余新特性（`:has()` FF121、`text-wrap` FF121、`popover` FF125、
  CSS 嵌套 FF117、容器查询 FF110）全库未使用；`dvh`（FF101+）与 `color-mix`（FF113+）
  在 115 支持范围内。

> 复核记录（2026-10-19）：最初的排查稿写成「`env()` Firefox 136 才支持、115 会作废整条
> 声明」，这是错的（BCD：FF65 起支持 `env()` 与 `safe-area-inset-*`）。据此加的 5 处
> 常量兜底与 env() 配对闸门已撤掉；留下的闸门只挡真正超纲的特性。

## 守门体系（CI，`build.yml` 的 Compatibility gate / `npm run compat`）

1. **JS 语法**：`es-check es2020` 扫描产物，超纲语法直接红。
2. **CSS 黑名单**（`web/scripts/compat-check.mjs`）：`:has()`、`text-wrap`、`[popover]`
   出现在产物即红（这些 esbuild/Vite 不会转译）。
3. **typecheck**：`tsc --noEmit` 已并入 SPA 构建步骤。

引入新依赖或新 API 时，先对照 caniuse 的 Firefox 115 支持矩阵；确实需要 115 没有的 API
时，按需引入 `core-js` 的对应模块，并把例外记在这里。

## 手测清单（改动涉及渲染层时过一遍）

- [ ] 窄窗口（≤760px）触发移动版式：抽屉开合正常、贴底启动栏不遮内容、弹窗不贴边
- [ ] 横屏矮窗（≤520px）同样走移动版式
- [ ] 亮/暗色切换正常
- [ ] 登录 → 建实例 → 启动 → 停止 全链路无 console error
- [ ] 现代浏览器（当前 FF/Chrome）无回归
