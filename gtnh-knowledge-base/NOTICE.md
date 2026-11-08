# NOTICE — 数据来源、署名与许可矩阵

本知识库是把多个第三方作品汇编而成的**汇编作品（collective work）**。
各部分的版权归原始权利人所有，并按其原始许可分发。使用时请按本文件署名。

---

## 1. 逐源明细

### 1.1 GTNH 中文维基（主体）

- **来源**：GT New Horizons 中文维基，灰机 wiki 托管，`https://wiki.gtnh.industries/`
- **获取方式**：对公开页面的离线镜像与正文抽取（`kb/wiki/*.jsonl`）
- **许可**（据该站页脚 `MediaWiki:Huiji-less-variables` 原文）：
  > 部分内容源于英文 wiki。该部分内容按照 CC BY-SA 3.0 国际许可协议进行分享。
  > 部分内容源于 MCMOD。该部分内容按照 CC BY-NC-SA 3.0 国际许可协议进行分享。
  > 中文wiki的原创内容按照 CC BY-NC-SA 3.0 国际许可协议分享。
- **权利人**：中文维基各页面的贡献者（页面历史中的作者集合）
- **推荐署名**：
  `内容来自 GT New Horizons 中文维基（灰机 wiki，CC BY-NC-SA 3.0；部分译自英文维基，CC BY-SA 3.0）`

### 1.2 GTNH 英文维基（经中文维基中译后的部分）

- **许可**：CC BY-SA 3.0 International
- **权利人**：GTNH Wiki (EN) 贡献者
- 本包中该部分以中文维基页面形式出现，无法逐页区分原始语言，故对全部维基正文
  同时给出两种署名。

### 1.3 游戏内任务书文本

- **来源**：GT New Horizons 整合包自带任务书（BetterQuesting），Daily 704
- **权利人**：GT New Horizons 团队（`https://github.com/GTNewHorizons`）
- **许可**：CC BY-NC-SA 4.0 International —— 依据该整合包仓库根目录 `LICENSE` 文件
  （全文标题：`Attribution-NonCommercial-ShareAlike 4.0 International`）
- **推荐署名**：
  `任务书文本来自 GT New Horizons 整合包，© GT New Horizons contributors，CC BY-NC-SA 4.0`

### 1.4 整合包 config 参考值

- **来源**：GT New Horizons 整合包 `config/` 目录（GT 机器参数、污染、矿脉生成、
  危险物品、自定义提示、Dreamcraft 等）
- **权利人 / 许可**：同 1.3（GT New Horizons 团队，CC BY-NC-SA 4.0）

### 1.5 物品名映射表

- **来源**：整合包内各模组的语言文件（`*.lang` / `*.json`）名称条目
- **权利人**：各个模组各自的作者（各模组许可不一）
- **性质**：功能性的"内部 ID ↔ 显示名"对照，单个名称为短语，非可版权表达的独创内容；
  整体汇编随整合包以 CC BY-NC-SA 4.0 分发。
- **注意**：本包仅包含**名称字符串**，不含任何模组的代码、贴图、模型或其他素材文件。
- **可选移除**：删掉 `kb/data/item_names*.tsv` 后，文字检索功能不受影响，
  仅 `--item` 查询不可用。

### 1.6 上述之外的自有代码

- `kb/search.py`、`kbweb/*.py`、`kbweb/web/*`、`tools/*` 为本汇编项目自己的代码，
  随本包以 CC BY-NC-SA 4.0 分发。

---

## 2. 明确未包含的内容（见 COMPLIANCE.md）

| 内容 | 原因 |
|---|---|
| Minecraft 原版贴图、模型、音效、字体 | Mojang / Microsoft 的版权素材，不在 CC 许可范围内 |
| 各模组的资产（贴图、GUI、音效、jar） | 各模组作者保留版权，多数非自由许可 |
| 维基镜像中的图片文件 | 多为游戏贴图或其截图，权利状态复杂 |
| 第三方任务书汉化叠加层 | 需单独署名，默认不随包分发 |
| 生成的检索索引 / 站点数据 | 可由本包数据重建，无需分发 |

---

## 3. 修改声明（CC BY 要求）

**本包已对原始素材作了以下修改，再分发时请一并保留本声明：**

1. **剔除游戏素材**：移除了全部 Minecraft / 模组贴图、模型、音效、jar、字体等二进制素材
   （详见 `COMPLIANCE.md`）。这是**删除**，不改变文字内容的含义。
2. **隐私脱敏**：对原文中的第三方联系方式作了脱敏替换，共 8 处 ——
   QQ 号/群号 5 处（形如 `QQ8156****3`、`群<已脱敏>`）与 Discord 邀请 3 个
   （Chinese 社区与挑战作者的邀请，如 `discord.gg/EXsh****`），统一替换为 `<已脱敏>` 占位符。
   **白名单保留**（机构/官方联系方式，非个人隐私）：GTNH 项目官方 Discord `discord.gg/gtnh`
   与灰机 wiki 站点支持邮箱 `support@huiji.wiki`。
   脱敏可通过 `python tools/redact_pii.py --apply` 重新执行。
3. **链接表预置**：新增 `kb/site/links.tsv`（由维基镜像抽取的条目互链关系，纯文本），
   使链接图无需原始 HTML 镜像即可重建。
4. **结构整理**：站点构建脚本改为路径自适应，并移除了原作者本机绝对路径痕迹
   （见 `PRIVACY.md`）。

除上述四点外，正文文字与原始素材一致。

---

## 4. 合规使用清单（再分发者必读）

- [ ] 保留本 `NOTICE.md` 与 `LICENSE`，不要移除署名
- [ ] 若修改了内容，注明"已修改"及修改方式
- [ ] 不得用于商业目的：不得售卖、不得置于需付费/投放广告变现的页面中牟利
- [ ] 衍生作品必须以 **CC BY-NC-SA 4.0**（或兼容的 NC+SA 许可）分发
- [ ] 不得对本包内容附加额外法律限制（如 DRM、禁止再分发条款）
- [ ] 不得暗示 GT New Horizons 团队、Mojang、Microsoft 或任何模组作者为你的版本背书
- [ ] **不要自行补入游戏贴图**后公开分发；如确需图片，请让终端用户用自己合法拥有的
      游戏文件在本地生成

---

## 4. 侵权处理

若你是权利人并认为本包内容侵犯了你的权利，请提交 issue 说明具体文件与权利依据，
我们会移除或替换相应内容。
