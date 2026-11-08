# 星见之丘 · HD-2D 前端开发者个人主页

一个 HD-2D（《歧路旅人》/《三角战略》那一派）风格的个人主页。
**没有一张图片资源** —— 地景、角色、光晕、图标全部由 Canvas 逐像素绘制，字体是 12px 点阵字体，色带用 4×4 有序抖动生成。

## 运行

直接双击 `index.html` 就能看（字体已内联，不受 `file://` 的 CORS 限制）。

想用本地服务器：

```bash
cd hd2d-portfolio
python3 -m http.server 8080
# 打开 http://localhost:8080
```

## 文件结构

预览：`docs/preview-day.png`、`docs/preview-night.png`

```
index.html                 页面骨架
css/fonts.css              @font-face（由脚本生成，勿手改）
css/style.css              全部样式：移轴景深、泛光、RPG 窗口外框
js/config.js               ★ 所有文案都在这里，改这一个文件就够
js/scene.js                立体透视模型渲染器（约 1500 行，独立无依赖）
js/audio.js                WebAudio 合成的环境音，默认关闭
js/main.js                 内容装配、像素图标、交互
assets/fonts/zpix.woff2    完整点阵字体（944 KB，备用）
assets/fonts/zpix-subset.woff2  子集（27 KB，页面实际加载的）
tools/cdp.mjs              无依赖的 Chrome DevTools Protocol 小工具（截图 / 测帧率 / 取控制台）
tools/subset-font.py       字体子集化脚本
debug/                     开发用的验证页面
```

## 改内容

编辑 `js/config.js`：

```js
window.SITE = {
  player: { name: '陆离', job: '前端炼金术士', level: 99, ... },
  stats: [...], skills: [...], quests: [...], journey: [...], contacts: [...]
};
```

**如果加了新的汉字**，重跑一次字体子集化，否则新字会变成方框：

```bash
python3 -m venv .venv && .venv/bin/pip install fonttools brotli   # 首次，约 47 MB
.venv/bin/python tools/subset-font.py --report
```

（`.venv/` 已经删掉了，需要重新生成字体时再建即可。）

脚本会把项目里出现的所有字符（外加 300 个常用汉字兜底）打包进 `assets/fonts/zpix-subset.woff2`，并把 base64 内联进 `css/fonts.css`。

## HD-2D 是怎么做出来的

| 手法 | 实现 |
| --- | --- |
| **移轴景深** | `.stage__dof` 用 `backdrop-filter: blur(3px)` 配 `mask-image` 上下两条渐变，只虚化画面上下的远处 / 近处，中景保持锐利 |
| **泛光 Bloom** | 发光元素走独立的半分辨率 glow buffer，`filter: blur(2px)` + `blur(6px)` 两级 `lighter` 叠加回主画面 |
| **体积光** | 15 条扇形三角在 glow buffer 里绘制，靠上面的模糊变成柔光柱，而不是硬边条纹 |
| **点阵色带** | 天空 / 水面用 4×4 Bayer 有序抖动，直接写 `ImageData` 像素缓冲（比 `fillRect` 快约 50 倍） |
| **空气透视** | `hazeRegion()` 按行做 alpha 混合，越靠近地平线越被天光洗淡，远景因此「退」得进去 |
| **视差分层** | 天空 0.00 / 远山 0.42 / 中景 0.72 / 近景 1.00 / 前景 1.32 五层，滚动时相机横向平移一屏，像在推一个立体模型 |
| **日夜更替** | 两套完整调色板按 `nightT` 线性插值，过渡期间限频重建缓存图层，整片地景会「重新打光」 |
| **像素图标** | 12×12 字符串点阵 → 整数倍放大到 canvas，HUD / 导航 / 技能树共 22 个图标 |
| **胶片刻蚀** | 运行时生成 120×120 噪点贴图，`mix-blend-mode: overlay` + `steps()` 位移，颗粒是逐像素的而不是高斯噪声 |
| **像素 UI 动效** | 所有过渡使用 `steps()` 缓动函数；点阵字体只用 12 的整数倍字号，保证永远不糊 |

## 键盘 / 无障碍

- `Tab` 遍历导航、按钮、存档位，`focus-visible` 有青色描边
- 对话框：`Enter` / `空格` 推进打字机
- 尊重 `prefers-reduced-motion`：关掉颗粒、动画与循环渲染，只画静态一帧
- 关掉 JS 有 `<noscript>` 兜底
- 场景 canvas 对读屏器隐藏（`aria-hidden`），信息在语义化 HTML 里
- URL 参数 `?theme=day` / `?theme=night` 可强制主题

## 浏览器支持

需要 `backdrop-filter`、`mask-image`、Canvas 2D `filter`。Chromium / Safari 16+ / Firefox 103+ 均可。
景深与泛光在旧浏览器上会优雅降级（泛光有 `ctx.filter` 特性检测，景深退化为无模糊）。

## 性能

这台机器上只有软件渲染（SwiftShader），没有可用的 GPU，所以下面的数字是**最坏情况**，仅供参考：

| 场景（1920×1080） | 帧率 |
| --- | --- |
| 只有 Canvas，不滚动 | 59.9 FPS |
| 只有 DOM，滚动 | 43.4 FPS |
| 完整页面，滚动 | 13~15 FPS |
| 把 canvas 藏掉后完整页面滚动 | 48.6 FPS |

拆解下来，瓶颈不是 JS 也不是 Canvas 绘制本身，而是**软件光栅化下每帧把固定背景层和滚动内容层合成整屏**——
这是真实 GPU 上一个 textured quad 就能搞定的事（把 canvas 缩到 1:1 也只有 14.3 FPS，说明与放大无关；
但 canvas 一隐藏就回到 48.6 FPS）。场景自身的 JS 开销稳定在 6ms 以内。

为此加了一套**自适应画质**：`js/scene.js` 的 `adaptQuality()` 持续测量 `frame()` 内部真实耗时，
超过 13ms 就降一档，低于 5.5ms 再升回去。三档分别关掉景深与颗粒、减半体积光与浮尘、隔行画水波。
在软件渲染下它稳定停在满档（因为 JS 确实不慢），在真正吃力的设备上则会自动让出效果而不是掉帧。

另外这些已经做过：静态地形按图层缓存到离屏 canvas，每帧只重绘动态部分；
调色渐变缓存到调色板变化为止；颗粒隔帧刷新；景深只模糊焦外的那两条带。
