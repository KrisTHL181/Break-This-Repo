// 渲染「主 README 头部 + 语言入口区块」用于肉眼验收。
import { readFileSync, writeFileSync } from 'node:fs';

const ROOT = 'D:/Open/恶搞';
const lines = readFileSync(`${ROOT}/tools/README.markdown.patched`, 'utf8').split('\n');

// 取第 1 行到 `## 目录` 之后 6 行，覆盖头部 + 新插入的区块
const cut = lines.findIndex((l) => l.trim() === '## 目录');
let chunk = lines.slice(0, cut + 6).join('\n');

const inline = (p) => {
  const svg = readFileSync(`${ROOT}/translations/${p.replace(/^\.\//, '').replace(/^translations\//, '')}`.replace('/translations/translations/', '/translations/'), 'utf8');
  return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
};

chunk = chunk.replace(/\[!\[([^\]]*)\]\(([^)]+)\)\]\(([^)]+)\)/g, (_, alt, img, href) => {
  let uri;
  try {
    uri = inline(img);
  } catch {
    uri = '';
  }
  return uri ? `<a href="${href}"><img src="${uri}" alt="${alt}"></a>` : `<a href="${href}">[img]</a>`;
});

const out = [];
let inDetails = false;
for (const l of chunk.split('\n')) {
  if (/^<details/.test(l)) { inDetails = true; out.push(l); continue; }
  if (/^<\/details>/.test(l)) { inDetails = false; out.push(l); continue; }
  if (inDetails) { out.push(l); continue; }
  let s = l.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');
  s = s.replace(/`([^`]+)`/g, '<code>$1</code>').replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  if (/^##\s/.test(l)) out.push(`<h2>${s.slice(3)}</h2>`);
  else if (/^###\s/.test(l)) out.push(`<h3>${s.slice(4)}</h3>`);
  else if (/^>/.test(l)) out.push(`<blockquote>${s.replace(/^>\s?/, '')}</blockquote>`);
  else if (/^---+$/.test(l.trim())) out.push('<hr>');
  else if (l.trim() === '') out.push('');
  else out.push(`<p>${s}</p>`);
}

const html = `<!doctype html><html lang="zh"><head><meta charset="utf-8"><title>readme head</title><style>
body{margin:0;padding:26px 30px;background:#0d1117;color:#e6edf3;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Noto Sans',sans-serif;font-size:15px;line-height:1.6}
.wrap{max-width:880px;margin:0 auto}
h2{font-size:22px;border-bottom:1px solid #30363d;padding-bottom:7px;margin-top:26px}
a{color:#4493f8;text-decoration:none}
code{background:#161b22;padding:2px 6px;border-radius:6px;font-size:13px}
blockquote{border-left:4px solid #30363d;margin:6px 0;padding:0 14px;color:#8b949e}
hr{border:0;border-top:1px solid #21262d;margin:18px 0}
details{border:1px solid #30363d;border-radius:8px;padding:10px 14px;margin:12px 0}
summary{cursor:pointer}
img{vertical-align:middle;margin:3px 0}
p img{display:block;max-width:100%}
</style></head><body><div class="wrap">
${out.join('\n')}
</div></body></html>`;

writeFileSync(`${ROOT}/tools/_preview_readme.html`, html, 'utf8');
console.log('已生成 tools/_preview_readme.html');
