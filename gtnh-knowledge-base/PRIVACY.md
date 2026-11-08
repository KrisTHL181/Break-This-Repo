# PRIVACY — 发布前隐私与个人身份信息审查报告

审查对象：本包全部待发布文件（228.1 MB）。审查工具：`../_tools` 之外的独立扫描器，
规则见下；一键复跑方式附在文末。

---

## 1. 结论先说

| 判断 | 结果 |
|---|---|
| **凭据/密钥泄漏** | **无**。API key、token、私钥、口令字段命中数为 0 |
| **本机痕迹泄漏** | **发现 10 处，已全部清理**（详见 §3） |
| **第三方联系方式**（QQ/QQ 群/Discord/邮箱） | 7 + 2 + 22 + 5 处，属**公开社区信息**，默认保留，提供一键脱敏脚本 |
| **误报** | 原始 13027 条命中中约 99% 为规则误报（详见 §4） |

**可以发布**；若你希望连社区联系方式也一并抹掉，先跑 `python tools/redact_pii.py --apply`。

---

## 2. 原始命中总览（13027 条）

| 严重度 | 类别 | 说明 | 原始命中 | 复核结论 |
|---|---|---|---|---|
| HIGH | `env.session` | 会话/令牌字段 | 2 | 误报：维基 `ns_gadget`/`ns_data` 里的 JS 源码片段（`cookie:`） |
| HIGH | `net.url_private` | 内网/本机 URL | 9 | 误报：本文档自身的 `127.0.0.1:8777` + 游戏内 `http://localhost%s` 模板串 |
| HIGH | `pii.email` | 邮箱 | 550 | 545 条误报，5 条为公开机构邮箱（见 §5） |
| HIGH | `pii.phone_cn` | 手机号 | 2 | 误报：`16777216000` 是数值（2²⁴×1000），非手机号 |
| MED | `net.ipv4` | IPv4 | 9235 | 误报：几乎全是模组版本号（`2.0.4.1`、`5.09.x`） |
| MED | `pii.wechat` | 微信号 | 504 | 误报：NBT/哈希式随机串（`VXJRhaTX8uFj4m6fQ`） |
| MED | `env.posix_home` | POSIX 用户目录 | 40 | 误报：OpenComputers 游戏内虚拟路径 `/home/bin` |
| MED | `pii.discord` | Discord 邀请 | 22 | 公开社区邀请（见 §5） |
| MED | `pii.qq` | QQ 号 | 7 | 公开社区联系方式（见 §5） |
| MED | `pii.qqgroup_kw` | QQ 群号 | 2 | 公开社区群号（见 §5） |
| LOW | `env.mc_uuid` | UUID | 103 | 游戏数据/合成表 UUID，非个人标识 |
| LOW | `env.workspace_path` | 本机绝对路径 | 9 | **真实问题，已清理**（§3） |
| LOW | `pii.github_user` | GitHub 账号链接 | 2542 | 模组作者仓库链接，公开信息 |

> 注：命中密度最高的 `kbweb/data/**`（`search.jsonl`、`pages/*` 等）是**生成物**，
> 已被 `.gitignore` 排除、也不会进入 PR；它们的内容与 `kb/**` 同源，故不单独处置。

---

## 3. 已修复：本机痕迹（真实泄漏）

发布包内曾出现 10 处作者本机的绝对路径（形如 `<盘符>:\<工作区>\<目录>`），会暴露本机目录布局。
**现已全部替换为相对路径或占位符**：

| 文件 | 原内容 | 处置 |
|---|---|---|
| `kb/ENTRY.md` | `cd <盘符>:\<工作区>` | 改为 `cd <知识库目录>`，并把 `.\q.cmd` 修正为包内真实入口 `.\search.cmd` |
| `kb/INDEX.md` | 同上 | 同上 |
| `tools/build_index_md.py` | `KB = r'E:\...\kb'` 等 3 处 | 改为基于 `__file__` 的相对路径；**同时修掉生成器会把路径重新写回 `INDEX.md` 的隐患** |
| `tools/build_title_index.py` | `KB = r'E:\...\kb'` | 改为相对路径 |
| `tools/extract_wiki.py` | `ROOT` / `OUT` | 改为环境变量 + 包内相对路径 |
| `tools/wiki_fast.py` | `ROOT` / `OUT` | 同上 |

清理后全包复查：**无本机路径残留**。

---

## 4. 已确认的误报（不需要处理）

这类命中由"文本里有技术字符串"引起，逐条复核后确认不含个人信息：

1. **`@Side.BOTH` / `@Side.CLIENT`**（542 条）—— Minecraft Forge 的 `@SideOnly` 注解，
   被邮箱正则当成 `xxx@Side.BOTH`。
2. **模组版本号当 IP**（9000+ 条）—— `1.0.6.0`、`2.1.3`、`5.09.x` 等，含 `TITLE_INDEX.md`
   里的条目名与 `changelog` 版本号。
3. **`VX…` 随机串当微信号**（504 条）—— 形如 `VXJRhaTX8uFj4m6fQ` 的哈希/NBT 键值。
4. **`/home/…` 当用户目录**（40 条）—— OpenComputers 模组的游戏内文件系统路径。
5. **`16777216000` 当手机号**（2 条）—— 材料性能表里的数值。
6. **JS 源码当令牌**（2 条）—— 维基 gadget 里的 `cookie = parts.slice(1).join('=')`。
7. **`127.0.0.1:8777`**（本包 README / start.cmd 内）—— 本知识库自身的本地服务地址，属有意保留。

---

## 5. 第三方公开联系方式 —— 已按选择脱敏

这些内容原文公开在 GTNH 中文维基与整合包任务书中（属社区对外推广信息）。
按"发布前先脱敏"的选择，已作替换处理：

| 类型 | 处理 | 出现位置 |
|---|---|---|
| QQ 群号/QQ 号 | **已脱敏**（5 处，如 `QQ8156****3` → `QQ<已脱敏>`） | `kb/wiki/ns_main.jsonl`、`kb/site/extra_pages.jsonl` |
| Discord 邀请（中文社区 / 挑战作者） | **已脱敏**（3 个） | `kb/wiki/ns_main.jsonl` |
| Discord 邀请（GTNH 官方 `discord.gg/gtnh`） | **保留**（官方项目链接，任务书自身即引用） | 任务书、维基、物品名表 |
| 机构邮箱（灰机 wiki 站点支持邮箱） | **保留**（公开发布的机构信箱，非个人） | `kb/wiki/ns_mediawiki.jsonl` 等页脚 |

> 本节刻意**不写出脱敏前的原值** —— 否则等于把刚抹掉的号码又发布一遍。
> 需要核对原始值请查本地未被修改的上游素材，或看 git 历史。

**未发现个人邮箱、个人手机号、真实姓名、住址、身份证号或任何账号密码。**

### 复跑脱敏

```bash
python tools/redact_pii.py            # 预览会改哪些文件
python tools/redact_pii.py --apply    # 实际改写 kb/ 下的文本
```
已按 CC BY-NC-SA 的署名要求在 `NOTICE.md` 注明"已修改"。

### 规则防误伤

脱敏规则刻意排除了两类误报，避免改坏正文：

- **裸 11 位数字不当手机号**：材料性能表里的数值 `16777216000`（磁物质）会被
  通用手机号正则命中，因此规则要求必须有"手机/电话/tel/phone"等上下文关键词。
- **`@java9args.txt` 不当邮箱**：这是 Java 的 `@argfile` 语法，会被邮箱正则当成
  `n@java9args.txt`，因此规则把文件扩展名形态的 TLD 列入排除名单。

---

## 6. 遗留说明与边界

1. **UUID 103 处**（`kb/wiki/ns_data.jsonl`、`ns_main.jsonl`）经抽样判断为合成表/实体数据，
   非玩家个人标识，**未脱敏**。若你要绝对保守，可把它们加入
   `tools/redact_pii.py` 的规则后再跑一次。
2. **维基编者用户名**：正文里偶见维护模板中的编者称呼（如"请添加维基群"之类提示里的昵称）。
   属维基公开的编辑者署名，**未脱敏**；如需一并处理，请自行扩展规则。
3. 本报告只写脱敏后的形态，**不写原值**，避免把刚抹掉的号码又发布一遍。
4. 本文档自身、`NOTICE.md`、`README.md` 也属于待发布内容，已纳入同一套扫描。

---

## 7. 复跑这次审查

```bash
python _pr/privacy_audit.py --src <包目录> --report _pr/PRIVACY-AUDIT.md --json _pr/privacy-audit.json
python _pr/triage.py        # 逐类人工判读
```

审查规则覆盖：GitHub/AWS/OpenAI 密钥、私钥、Authorization 头、通用口令赋值、
邮箱、中国大陆手机号与身份证号、QQ/微信/Telegram/Discord/Steam/B 站、IPv4、
内网与本机 URL、Windows 与 POSIX 用户目录、主机名、Minecraft 账号字段、
UUID、会话令牌字段。
