// 生成一张预览用的联系表（contact sheet）：把所有徽章拼成一个 HTML，
// 再用无头浏览器截图，从而**真正看到**渲染效果，而不是靠宽度估算。
import { readFileSync, writeFileSync, readdirSync } from 'node:fs';

const ROOT = 'D:/Open/恶搞';
const dir = `${ROOT}/translations/assets`;
const files = readdirSync(dir).filter((f) => /^lang-[a-z]{2}\.svg$/.test(f)).sort();

const rows = files
  .map((f) => {
    const code = f.slice(5, 7);
    // 用 data URI 内联：file:// 下相对路径的 <img> 在无头 Edge 里不稳定，
    // 内联则能真正验证 SVG 内容本身的渲染
    const svg = readFileSync(`${dir}/${f}`, 'utf8');
    const uri = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
    return `    <span class="b"><img src="${uri}" alt="${code}"></span>`;
  })
  .join('\n');

const html = `<!doctype html>
<html lang="zh"><head><meta charset="utf-8">
<title>badge preview</title>
<style>
  body{margin:0;padding:24px;background:#0b1120;font-family:-apple-system,'Segoe UI',Roboto,sans-serif}
  h2{color:#e2e8f0;font-size:14px;margin:0 0 12px}
  .grid{display:flex;flex-wrap:wrap;gap:8px;align-items:center;max-width:1100px}
  .b{display:inline-block;line-height:0}
  .b img{display:block}
  .panel{background:#111827;border:1px solid #1f2937;border-radius:10px;padding:16px;margin-bottom:18px}
</style></head>
<body>
  <div class="panel">
    <h2>徽章墙（${files.length} 枚，深色背景下的实际渲染）</h2>
    <div class="grid">
${rows}
    </div>
  </div>
</body></html>
`;

writeFileSync(`${ROOT}/tools/_preview_badges.html`, html, 'utf8');
console.log(`预览页已生成，含 ${files.length} 枚徽章`);
console.log(`${ROOT}/tools/_preview_badges.html`);
