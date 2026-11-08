// 生成语言徽章 SVG：translations/assets/lang-<code>.svg
//
// 为什么自绘而不外链 shields.io：
//   1. 上游 README 被 fork / 镜像 / 离线阅读时，外链徽章会全部裂图；
//      而相对路径的自带 SVG 永远能渲染（本仓库已有 ./cat.jpeg 这类先例）。
//   2. 本沙箱实测连不上 img.shields.io（TLS 握手失败），无法验证外链是否可用
//      —— 不确定能不能用的东西不该塞进别人的 README。
//
// GitHub 的 SVG 消毒会剥掉 <style>/<script>/class/foreignObject，所以这里只用
// 内联属性 + <text>/<rect>/<path>/<linearGradient>，这些是保留的。
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

// 定位仓库根：脚本可能位于 <root>/tools/（开发工作区）
// 或 <root>/translations/_tools/（仓库内）。两种布局都要能跑。
const SCRIPT_DIR = fileURLToPath(new URL('.', import.meta.url)); // 带尾斜杠
const IN_REPO_TOOLS = /[\\/]_tools[\\/]?$/.test(SCRIPT_DIR);
const REPO_ROOT = fileURLToPath(new URL(IN_REPO_TOOLS ? '../../' : '../', import.meta.url));
// 源文件在开发工作区里是 <root>/tools/xxx，在仓库里是 <root>/translations/_tools/xxx
const KIT = IN_REPO_TOOLS ? REPO_ROOT + 'translations/_tools/' : REPO_ROOT + 'tools/';
const TRANS = REPO_ROOT + 'translations/';
const OUT = `${TRANS}assets`;

const langs = JSON.parse(readFileSync(`${REPO_ROOT}/tools/languages.json`, 'utf8'));
const done = new Set(
  JSON.parse(readFileSync(`${REPO_ROOT}/tools/translated-codes.json`, 'utf8')),
);

// 按语系/地域配色（与门户页的分组呼应）
const PALETTE = {
  germanic: ['#1e3a8a', '#3b82f6'],
  romance: ['#7c2d12', '#f97316'],
  slavic: ['#134e4a', '#14b8a6'],
  uralic: ['#3b0764', '#a855f7'],
  celtic: ['#14532d', '#22c55e'],
  other: ['#334155', '#64748b'],
  semitic: ['#78350f', '#d97706'],
  indic: ['#831843', '#ec4899'],
  sea: ['#7f1d1d', '#ef4444'],
  turkic: ['#164e63', '#06b6d4'],
  paleo: ['#4c0519', '#fb7185'],
};

const GROUP_OF = {
  germanic: ['af', 'da', 'de', 'en', 'fo', 'fy', 'is', 'lb', 'nb', 'nl', 'nn', 'no', 'sv'],
  romance: ['an', 'ca', 'co', 'es', 'fr', 'gl', 'it', 'oc', 'pt', 'rm'],
  slavic: ['bs', 'cs', 'hr', 'mk', 'ru', 'sk', 'sl', 'sr', 'uk'],
  uralic: ['et', 'fi', 'hu'],
  celtic: ['ch', 'co'],
  semitic: ['ar', 'he'],
  indic: ['bh', 'hi'],
  turkic: ['tr'],
  sea: ['id', 'ja', 'ko', 'th', 'vi'],
  paleo: ['aa', 'ab', 'ae', 'av'],
  other: ['el', 'lt', 'lv', 'mt', 'pl'],
};
const groupOf = (code) => {
  for (const [g, list] of Object.entries(GROUP_OF)) if (list.includes(code)) return g;
  return 'other';
};

const esc = (s) =>
  s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');

// 粗略估算文本宽度：CJK/全角按 1.0em，拉丁按 0.62em。
// 0.62 是实测调出来的：第一版用 0.56 时 `Makedonski jazik`、`Norsk bokmål`
// 这类较长译名会被右侧裁掉（用无头浏览器截图肉眼发现的，宽度估算骗不了人）。
const widthOf = (s) => {
  let w = 0;
  for (const ch of s) {
    const c = ch.codePointAt(0);
    const wide =
      (c >= 0x1100 && c <= 0x115f) ||
      (c >= 0x2e80 && c <= 0xa4cf) ||
      (c >= 0xac00 && c <= 0xd7a3) ||
      (c >= 0xf900 && c <= 0xfaff) ||
      (c >= 0xfe30 && c <= 0xfe4f) ||
      (c >= 0xff00 && c <= 0xff60) ||
      (c >= 0x1f300 && c <= 0x1faff);
    w += wide ? 1.0 : 0.62;
  }
  return w;
};

const FONT =
  "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,'Noto Sans','Noto Sans CJK SC','Hiragino Sans',sans-serif";

function badge(code, native) {
  const g = groupOf(code);
  const [c1, c2] = PALETTE[g] ?? PALETTE.other;

  const left = code.toUpperCase();
  const leftW = Math.round(widthOf(left) * 12) + 22;
  const rightW = Math.round(widthOf(native) * 12) + 24;
  const total = leftW + rightW;
  const h = 30;

  // 磁铁图形放在左侧色块里
  const magnet = (x, y) =>
    `<path d="M${x} ${y + 9} a5 5 0 0 1 10 0 v4 h-3.4 v-4 a1.6 1.6 0 0 0 -3.2 0 v4 H${x} z" fill="#ffffff" opacity="0.92"/>`;

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${total}" height="${h}" viewBox="0 0 ${total} ${h}" role="img" aria-label="${esc(code)} — ${esc(native)}">
  <title>${esc(native)} (${esc(code)})</title>
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${c1}"/>
      <stop offset="1" stop-color="${c2}"/>
    </linearGradient>
  </defs>
  <rect width="${total}" height="${h}" rx="6" fill="url(#g)"/>
  <rect x="${leftW}" width="${rightW}" height="${h}" rx="6" fill="#0f172a" opacity="0.55"/>
  <rect x="${leftW - 6}" width="12" height="${h}" fill="#0f172a" opacity="0.55"/>
  ${magnet(9, 8)}
  <text x="${9 + 15}" y="20" font-family="${FONT}" font-size="13" font-weight="700" fill="#ffffff" letter-spacing="0.5">${esc(left)}</text>
  <text x="${leftW + 12}" y="20" font-family="${FONT}" font-size="13" fill="#f8fafc">${esc(native)}</text>
</svg>
`;
}

mkdirSync(OUT, { recursive: true });
let n = 0;
const manifest = [];
for (const l of langs) {
  if (!done.has(l.code)) continue;
  const svg = badge(l.code, l.native);
  writeFileSync(`${OUT}/lang-${l.code}.svg`, svg, 'utf8');
  manifest.push({ code: l.code, native: l.native, en: l.en, group: groupOf(l.code), bytes: svg.length });
  n++;
}
writeFileSync(`${REPO_ROOT}/tools/badge-manifest.json`, JSON.stringify(manifest, null, 2) + '\n', 'utf8');
console.log(`已生成 ${n} 枚徽章 -> translations/assets/`);
console.log(`总计 ${(manifest.reduce((a, b) => a + b.bytes, 0) / 1024).toFixed(0)} KB，平均 ${Math.round(manifest.reduce((a, b) => a + b.bytes, 0) / n)} bytes/枚`);
console.log('最长的一个（用于检查右侧色块宽度是否够）:');
const longest = manifest.reduce((a, b) => (widthOf(b.native) > widthOf(a.native) ? b : a));
console.log(`  ${longest.code} ${longest.native} → 估算 ${Math.round(widthOf(longest.native) * 12) + 22}px`);
