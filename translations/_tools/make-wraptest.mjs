// 对比测试：徽章在 GitHub 宽度下的两种排布方式，用截图决定用哪种。
import { readFileSync, writeFileSync, readdirSync } from 'node:fs';

const ROOT = 'D:/Open/恶搞';
const dir = `${ROOT}/translations/assets`;
const langs = JSON.parse(readFileSync(`${ROOT}/tools/languages.json`, 'utf8'));
const byCode = new Map(langs.map((l) => [l.code, l]));

const codes = readdirSync(`${ROOT}/translations`)
  .map((f) => /^README\.([a-z]{2})\.md$/.exec(f))
  .filter(Boolean)
  .map((m) => m[1])
  .sort();

const uri = (c) =>
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(readFileSync(`${dir}/lang-${c}.svg`, 'utf8'));

const link = (c) => `<a href="#${c}"><img src="${uri(c)}" alt="${c}"></a>`;

// 方案 A：连续内联
const A = codes.map(link).join('\n');

// 方案 B：表格，每行 4 个
const rows = [];
for (let i = 0; i < codes.length; i += 4) {
  rows.push('<tr>' + codes.slice(i, i + 4).map((c) => `<td>${link(c)}</td>`).join('') + '</tr>');
}
const B = `<table>\n${rows.join('\n')}\n</table>`;

// 方案 C：CSS 多列（GitHub 上 <style> 会被剥掉，这里只作对照）
const C = codes.map(link).join('\n');

const html = `<!doctype html><html lang="zh"><head><meta charset="utf-8"><title>wrap test</title><style>
body{margin:0;padding:20px;background:#0d1117;color:#e6edf3;font-family:-apple-system,'Segoe UI',Roboto,sans-serif;font-size:14px}
/* GitHub 的 README 正文容器宽度约 1012px（含 padding），内容区约 980px */
.gh{width:1012px;box-sizing:border-box;padding:0 16px;margin:0 auto;border:1px dashed #30363d}
h2{font-size:15px;color:#f0b429;margin:18px 0 8px;font-family:monospace}
img{vertical-align:middle}
table{border-collapse:separate;border-spacing:6px}
.c{display:flex;flex-wrap:wrap;gap:6px}
</style></head><body>
<div class="gh">
  <h2>方案 A —— 连续内联（当前实现）</h2>
  <div>${A}</div>

  <h2>方案 B —— 表格 4 列</h2>
  ${B}

  <h2>方案 C —— flex 容器 + 无显示宽度</h2>
  <div class="c">${C}</div>
</div>
</body></html>`;

writeFileSync(`${ROOT}/tools/_wraptest.html`, html, 'utf8');
console.log('已生成 tools/_wraptest.html');
