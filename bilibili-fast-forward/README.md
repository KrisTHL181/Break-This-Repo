# B站一键快进（Edge / Chrome 扩展）

在B站视频播放器的控制栏中，「清晰度」选项左侧添加一个**一键快进按钮**，点击即可快进（默认 **85 秒**）。同时提供设置界面，可自定义：

- **快进秒数**（1 ~ 3600，默认 85 秒）
- **触发快捷键 / 快捷键组合**（支持 Ctrl / Alt / Shift 修饰键，默认 `Shift + →`）

修改设置后**立即生效**，无需刷新B站页面。

## 安装（Edge）

1. 打开 `edge://extensions`
2. 打开右上角的「开发人员模式」
3. 点击「加载解压缩的扩展」，选择本文件夹（`bilibili-fast-forward`）

## 安装（Chrome）

`chrome://extensions` → 打开「开发者模式」→「加载已解压的扩展程序」→ 选择本文件夹。

## 使用说明

- 播放任意B站视频，鼠标悬停播放器唤出控制栏，在「清晰度」按钮左侧会出现一个**快进图标按钮**（与B站自带图标同尺寸同排），点击即快进。悬停时只有图标提亮（与原生按钮一致，不再整块底色高亮），并在上方浮出「快进85s (Shift+→)」提示气泡；普通、网页全屏、全屏、小窗各模式下都与相邻按钮对齐。
- 也可以直接按快捷键快进（默认 `Shift + →`）。焦点在输入框（发弹幕、评论、搜索）中打字时不会误触发。
- 点击浏览器工具栏中的扩展图标，打开设置窗口修改秒数与快捷键；「恢复默认」可一键回到 85 秒 / `Shift + →`。
- 直播间等无限时长的流不会触发快进。

## 快捷键说明

- 录制方法：点击设置窗口中的快捷键框 → 按下想要的组合（如 `Ctrl` + `Shift` + `→`）→ 保存。
- 按 `Esc` 将快捷键恢复为默认值。
- 修饰键仅支持 Ctrl / Alt / Shift（浏览器环境下 Meta/Win 键组合不可靠，故不支持）。
- 快捷键在B站页面获得焦点时生效；建议避免与B站自带快捷键（`←`/`→` 5秒快退快进、`↑`/`↓` 音量等）完全相同的组合。

## 文件结构

```
bilibili-fast-forward/
├── manifest.json       # 扩展清单（Manifest V3）
├── content.js          # 注入B站页面的脚本：按钮注入 + 快捷键监听 + 快进逻辑
├── content.css         # 按钮样式
├── popup.html/js/css   # 扩展弹窗设置界面
├── icons/              # 扩展图标
├── tools/make_icons.js # 图标生成脚本（node tools/make_icons.js）
├── tools/dev-server.js # 本地静态服务器（node tools/dev-server.js）
└── test/mock-player.html # 本地模拟B站播放器：含真实控制栏几何与全屏对齐读数
```

## 已知边界

- 按钮优先插入在「清晰度」左侧；若个别视频没有清晰度按钮，则插入到「设置」按钮左侧作回退。
- 按钮的尺寸、行高、图标缩放与悬停提亮**全部由B站原生CSS提供**（复用 `.bpx-player-ctrl-btn` / `.bpx-player-ctrl-btn-icon` / `.bpx-common-svg-icon`）。`content.css` 里不要再写宽高或行高：B站的 `.bpx-player-container[data-screen="full"] .bpx-player-ctrl-btn{height:43px;width:54px}` 优先级 (0,3,0) 高于自定义单类 (0,1,0)，自定高度在全屏下会被覆盖而导致按钮下沉。
- **加载视频时的巨大色块**：B站先用 JS 建出控制栏 DOM，之后才分批注入播放器样式表，而我们的图标尺寸完全依赖它的百分比高度链（`.bpx-player-ctrl-btn-icon > .bpx-common-svg-icon{height:22px}`，全屏 28px）。这条链没就位时同一个 `<svg>` 会按 100% 铺满整个播放器（mock 实测 400×400 白块，去掉属性兜底时 472×472 黑块）。`svg` 的 `width`/`height` 属性与 `content.css` 里的 `.bff-ctrl-btn{fill:#fff}` 都只是次要兜底——B站的 `svg{width:100%;height:100%}` 必然盖掉这两个属性，所以真正防住阴影的是 `content.js` 的绘制前自检 `sizingOk()`：插入后在同一个任务里强制布局量一次实际渲染尺寸，与相邻原生图标（取中位高度）量级不符就当场移除、退避 300ms 再试；后到的 `<style>`/`<link>` 在 MutationObserver 微任务里复查已插入的按钮——这两步都早于本帧绘制，所以不合格的节点不会被画出来。删掉自检或给它写固定宽高都会让阴影复发。
- B站改版更换播放器 DOM 类名时，可能需要更新 `content.js` 中的选择器（`.bpx-player-ctrl-quality` 等）。

## 本地验证（不加载扩展）

```
node tools/dev-server.js          # 默认 http://127.0.0.1:8137/
```

`test/mock-player.html` 按真实 computed 值复刻了控制栏几何规则（含 `[data-screen]` 全屏尺寸），并直接引用仓库里的 `content.js` / `content.css`。页面可切换「普通 / 网页全屏 / 全屏」，另有两个开关复现加载首屏的样式状态：「播放器CSS整体未注入」和「故障注入（图标未被约束）」。状态面板即回归判据：

- `iconCenterDriftPx` / `alignOk`：注入图标与各原生图标中心的最大偏差应 < 1px（旧实现为普通 6.5px、全屏 5.5px）。
- `iconBoxPx` / `noShadowOk`：注入图标任何时刻都应只有一个图标位大小（普通 36×22、全屏 54×28）；超过 60px 即为巨大色块回归。
- 打开「故障注入」后对照组的图标会实测变成 400×400 白块，而 `content.js` 的自检会让按钮直接缺席（`btnPresent:false`、`noShadowOk:true`）——这是预期结果，关掉开关后按钮会在一个定时器周期内回来。
