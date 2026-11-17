# docs/screenshots —— README 用的界面截图

README 里的截图直接引用本目录（仓库内相对路径，由 GitHub 自己的 CDN 提供），
不走任何图床——图床慢的时候整页都在等它，而仓库里的图跟着仓库走。

| 文件 | 拍的是什么 |
| --- | --- |
| `01-setup.webp` | 首启初始化（建管理员账号） |
| `02-main.webp` | 主界面 + 右下启动面板 |
| `03-instances.webp` | 实例列表页（DSH / 品牌两层） |
| `04-download.webp` | 下载页（真实 npm 版本列表） |
| `05-settings.webp` | 设置页（通用） |
| `06-brands.webp` | 品牌实例层：6 个品牌页签 + 版本卡 + 实例卡 |
| `07-node.webp` | Node.js 运行时：18–26 按大版本通道 |
| `08-mc.webp` | Minecraft 服务端（默认关闭时的入口说明） |

## 规格与重新生成

- 视口 **1440×900**；截图按 `deviceScaleFactor 2` 抓、再缩到 1440，文字仍然清晰。
- 转成 **WebP，`-quality 82`，`-strip`**：八张合计约 **450 KB**。
- 重新生成：

```bash
# 面板跑起来、页面上有演示数据，然后用 Playwright 登录并逐页截图：
#   viewport 1440x900, deviceScaleFactor 2, ignore_https_errors
# 再转：
magick shot.png -resize 1440x900 -quality 82 -strip docs/screenshots/03-instances.webp
```

- 2026-11-05 这一批是在**一份演示数据集**上拍的：5 个停止态的 dsh 实例
  （`reference` / `canary` / `staging` / `playground` / `research`，版本 `0.2.1-rc.5`），
  管理员账号为 `admin`，**没有真实账号、供应商、路径或口令**。实例树是伪造的“已安装”
  结构，只为截图，不能启动。

> 换图前务必确认页面上没有不该公开的东西（内网域名、口令、真实账号名、实例 URL 里的
> `?token=`）——这是截图最容易漏的一步。实例运行中时列表与浮层都会带上 token，
> 所以列表截图请让实例处于停止态。
