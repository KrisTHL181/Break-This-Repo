// 生成语言门户页 translations/README.md 与英雄横幅 translations/assets/hero.svg。
//
// 设计要点：
//   1. 门户页的内容**由实际存在的译文文件推导**，不硬编码数量与清单
//      —— 否则每加一个语种就要手工改门户页，迟早不一致。
//   2. 视觉全部自绘（SVG 内联属性，GitHub 消毒后会保留的构造），不外链图床。
//   3. 磁铁是贯穿主题：徽章左侧一枚小磁铁，"磁力计"吸附进度，入口锚点 call-to-action。
import { readFileSync, writeFileSync, readdirSync, mkdirSync, existsSync } from 'node:fs';

import { fileURLToPath } from 'node:url';

// 定位仓库根：脚本可能位于 <root>/tools/（开发工作区）
// 或 <root>/translations/_tools/（仓库内）。两种布局都要能跑。
const SCRIPT_DIR = fileURLToPath(new URL('.', import.meta.url)); // 带尾斜杠
const IN_REPO_TOOLS = /[\\/]_tools[\\/]?$/.test(SCRIPT_DIR);
const REPO_ROOT = fileURLToPath(new URL(IN_REPO_TOOLS ? '../../' : '../', import.meta.url));
// 源文件在开发工作区里是 <root>/tools/xxx，在仓库里是 <root>/translations/_tools/xxx
const KIT = IN_REPO_TOOLS ? REPO_ROOT + 'translations/_tools/' : REPO_ROOT + 'tools/';
const TRANS = REPO_ROOT + 'translations/';
const ASSETS = `${TRANS}assets`;
const langs = JSON.parse(readFileSync(`${KIT}languages.json`, 'utf8'));
const byCode = new Map(langs.map((l) => [l.code, l]));

const codes = readdirSync(`${REPO_ROOT}translations`)
  .map((f) => /^README\.([a-z]{2})\.md$/.exec(f))
  .filter(Boolean)
  .map((m) => m[1])
  .filter((c) => byCode.has(c))
  .sort();

const TOTAL = langs.length;
const DONE = codes.length;

// ---------- 分组（用于折叠面板与视觉节奏）----------
const GROUPS = [
  ['日耳曼语族 / Germanic', ['af', 'da', 'de', 'en', 'fo', 'fy', 'is', 'lb', 'nb', 'nl', 'nn', 'no', 'sv']],
  ['罗曼语族 / Romance', ['an', 'ca', 'co', 'es', 'fr', 'gl', 'it', 'oc', 'pt', 'rm']],
  ['斯拉夫语族 / Slavic', ['bs', 'cs', 'hr', 'mk', 'ru', 'sk', 'sl', 'sr', 'uk']],
  ['乌拉尔语系 / Uralic', ['et', 'fi', 'hu']],
  ['凯尔特语族 / Celtic', ['ch']],
  ['闪米特语族 / Semitic', ['ar', 'he']],
  ['印度-雅利安 / Indo-Aryan', ['bh', 'hi']],
  ['突厥语族 / Turkic', ['tr']],
  ['东亚与东南亚 / East & SE Asia', ['id', 'ja', 'ko', 'th', 'vi']],
  ['高加索与古语 / Caucasian & ancient', ['aa', 'ab', 'ae', 'av']],
  ['其它印欧 / Other Indo-European', ['el', 'lt', 'lv', 'mt', 'pl']],
];

// ---------- 1) 英雄横幅 ----------
const W = 1000;
const H = 200;
const heroMagnet = (cx, cy, s) => {
  const t = 30 * s;
  const g = 12 * s;
  return `<g transform="translate(${cx} ${cy})">
    <path d="M${-t} ${g} a${t} ${t} 0 0 1 ${2 * t} 0 v${t * 0.7} h-${t * 0.62} v-${t * 0.7} a${t * 0.38} ${t * 0.38} 0 0 0 -${t * 0.76} 0 v${t * 0.7} H${-t} z" fill="#f8fafc"/>
  </g>`;
};

const hero = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="磁力语言入口 / Language Magnet">
  <title>磁力语言入口 / Language Magnet</title>
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#0b1120"/>
      <stop offset="0.5" stop-color="#111c33"/>
      <stop offset="1" stop-color="#1e1b4b"/>
    </linearGradient>
    <linearGradient id="beam" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#38bdf8" stop-opacity="0.85"/>
      <stop offset="1" stop-color="#a855f7" stop-opacity="0.15"/>
    </linearGradient>
    <radialGradient id="glow" cx="0.5" cy="0.5" r="0.5">
      <stop offset="0" stop-color="#38bdf8" stop-opacity="0.55"/>
      <stop offset="1" stop-color="#38bdf8" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <rect width="${W}" height="${H}" rx="14" fill="url(#bg)"/>
  <ellipse cx="150" cy="100" rx="180" ry="110" fill="url(#glow)"/>
  ${[0, 1, 2, 3, 4, 5].map((i) => `<rect x="${330 + i * 108}" y="26" width="86" height="9" rx="4.5" fill="url(#beam)" opacity="${0.5 - i * 0.05}"/>`).join('\n  ')}
  ${heroMagnet(150, 100, 1.5)}
  <text x="248" y="84" font-family="-apple-system,'Segoe UI',Roboto,'Noto Sans',sans-serif" font-size="42" font-weight="800" fill="#f8fafc" letter-spacing="1">LANGUAGE MAGNET</text>
  <text x="250" y="124" font-family="-apple-system,'Segoe UI',Roboto,'Noto Sans CJK SC',sans-serif" font-size="21" fill="#93c5fd">磁力语言入口 · 把 ${TOTAL} 种语言吸过来</text>
  <text x="250" y="158" font-family="-apple-system,'Segoe UI',Roboto,sans-serif" font-size="14" fill="#94a3b8">${DONE} / ${TOTAL} locales online · pull request 越界即合</text>
</svg>
`;

mkdirSync(ASSETS, { recursive: true });
writeFileSync(`${ASSETS}/hero.svg`, hero, 'utf8');
console.log(`英雄横幅已生成 (${hero.length} bytes)`);

// ---------- 2) 门户页 ----------
const line = (codesIn) =>
  codesIn
    .map((c) => {
      const l = byCode.get(c);
      return `[![${l.code} ${l.native}](./assets/lang-${c}.svg)](./README.${c}.md)`;
    })
    .join('\n');

const groupBlocks = GROUPS.map(([title, list]) => {
  const here = list.filter((c) => codes.includes(c));
  if (!here.length) return '';
  return `<details open>
<summary><b>${title}</b> &nbsp;<code>${here.length}</code></summary>

${line(here)}

</details>`;
}).join('\n\n');

const assigned = new Set(GROUPS.flatMap(([, l]) => l));
const rest = codes.filter((c) => !assigned.has(c));
const restBlock = rest.length
  ? `<details open>\n<summary><b>其它 / Others</b> &nbsp;<code>${rest.length}</code></summary>\n\n${line(rest)}\n\n</details>`
  : '';

const pending = langs.filter((l) => !codes.includes(l.code)).map((l) => `\`${l.code}\``);

const pct = Math.round((DONE / TOTAL) * 100);
const barFilled = Math.round((DONE / TOTAL) * 30);
const bar = '█'.repeat(barFilled) + '░'.repeat(30 - barFilled);

const portal = `<!-- ⚠️ 本文件与 assets/ 下的徽章由脚本生成，请勿手工编辑。
     改完译文后重新生成：
       node _tools/gen-badges.mjs && node _tools/gen-portal.mjs
     两者都会从 translations/ 下实际存在的 README.<code>.md 推导清单，
     所以新增语种后重跑即可，无需改本文件。 -->

# 🧲 Language Magnet · 磁力语言入口

[![磁力语言入口](./assets/hero.svg)](./README.md)

**README 的语言入口。** 徽章即磁铁，点一下就被吸到对应语种的译文。

**吸附进度：** \`${bar}\` **${DONE} / ${TOTAL}**（${pct}%）

- 🌐 译文目录：[translations/](./)
- 🧰 校验工具：[\`_tools/\`](./_tools/)（无依赖，只需 Node.js）
- 📖 源文档：[README.markdown](../README.markdown)

---

## 🧲 一键吸附

${groupBlocks}

${restBlock}

---

## ⚡ 状态

| 项目 | 值 |
| --- | --- |
| 已上线语种 | **${DONE}** |
| 目标语种（ISO 639-1 全集） | **${TOTAL}** |
| 完成度 | **${pct}%** |
| 每份译文结构 | 464 行 · 42 标题 · 13 代码块 · 结构指纹与源文档逐行一致 |
| 锚点规则 | GitHub 真实 slug 规则（源文档自带 TOC 有 3 条失效锚点，本项目不复制该缺陷） |

<details>
<summary><b>⏳ 待译语种（${pending.length}）</b></summary>

${pending.join(' · ')}

</details>

---

## 🤝 怎么加一个语种

1. 复制 [\`_tools/upstream-README.md\`](./_tools/upstream-README.md) 的源文档快照，译成目标语言。
2. 存为 \`translations/README.<ISO 639-1 代码>.md\`（编码 UTF-8、无 BOM、LF）。
3. 跑校验与锚点重建：

\`\`\`bash
node translations/_tools/toc.mjs rebuild <code>
node translations/_tools/validate.mjs <code>
node translations/_tools/gen-portal.mjs     # 重新生成本页与徽章
\`\`\`

4. 提 PR。只要不碰 [\`.github/\`](../.github/)，本仓库会自动合并。

> ⚠️ 译文由 AI 批量生成并通过自动化**结构**校验。
> **结构合格 ≠ 语言完美**，小语种与长段落建议以源文档为准，欢迎母语者直接改措辞。

---

<sub>本页与徽章由 <a href="./_tools/gen-portal.mjs"><code>_tools/gen-portal.mjs</code></a> + <a href="./_tools/gen-badges.mjs"><code>_tools/gen-badges.mjs</code></a> 生成 · 徽章为自绘 SVG，不依赖任何外部图床</sub>
`;

writeFileSync(`${TRANS}README.md`, portal, 'utf8');
console.log(`门户页已生成：${DONE}/${TOTAL}，${portal.length} bytes`);
console.log(`分组：${GROUPS.map(([t, l]) => `${t.split(' / ')[0]}:${l.filter((c) => codes.includes(c)).length}`).join('  ')}`);
if (rest.length) console.log(`未分组: ${rest.join(' ')}`);
