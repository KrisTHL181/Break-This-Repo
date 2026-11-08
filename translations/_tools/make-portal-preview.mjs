// 把门户页渲染成图片，用于**肉眼验收**视觉效果。
// 用一个 Markdown→HTML 的最小转换（只处理门户页实际用到的语法），
// 并把图片路径改写为本地绝对路径，避免无头浏览器在 file:// 下加载失败。
import { readFileSync, writeFileSync } from 'node:fs';

const ROOT = 'D:/Open/恶搞';
let md = readFileSync(`${ROOT}/translations/README.md`, 'utf8');

const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

// 内联图片：把 ./assets/xxx.svg 换成 data URI（最可靠的本地渲染方式）
const inline = (p) => {
  const svg = readFileSync(`${ROOT}/translations/${p.replace(/^\.\//, '')}`, 'utf8');
  return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
};

// [![alt](img)](link)
md = md.replace(/\[!\[([^\]]*)\]\(([^)]+)\)\]\(([^)]+)\)/g, (_, alt, img, href) => {
  return `<a href="${href}"><img src="${inline(img)}" alt="${esc(alt)}"></a>`;
});
// 单独成行的 ![alt](img)
md = md.replace(/^!\[([^\]]*)\]\(([^)]+)\)$/gm, (_, alt, img) => `<img src="${inline(img)}" alt="${esc(alt)}">`);
// [text](url)
md = md.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');
// 行内代码
md = md.replace(/`([^`]+)`/g, '<code>$1</code>');
// 粗体
md = md.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

const lines = md.split('\n');
const out = [];
let inDetails = false;
for (const l of lines) {
  if (/^<details/.test(l)) {
    inDetails = true;
    out.push(l);
    continue;
  }
  if (/^<\/details>/.test(l)) {
    inDetails = false;
    out.push(l);
    continue;
  }
  if (inDetails) {
    out.push(l);
    continue;
  }
  if (/^###\s/.test(l)) out.push(`<h3>${l.slice(4)}</h3>`);
  else if (/^##\s/.test(l)) out.push(`<h2>${l.slice(3)}</h2>`);
  else if (/^#\s/.test(l)) out.push(`<h1>${l.slice(2)}</h1>`);
  else if (/^---+$/.test(l.trim())) out.push('<hr>');
  else if (/^>\s?/.test(l)) out.push(`<blockquote>${l.replace(/^>\s?/, '')}</blockquote>`);
  else if (/^\|\s/.test(l)) out.push(`<div class="tr">${l}</div>`);
  else if (/^-\s/.test(l)) out.push(`<li>${l.slice(2)}</li>`);
  else if (/^\d+\.\s/.test(l)) out.push(`<li>${l.replace(/^\d+\.\s/, '')}</li>`);
  else if (l.trim() === '') out.push('');
  else out.push(`<p>${l}</p>`);
}

const html = `<!doctype html><html lang="zh"><head><meta charset="utf-8">
<title>portal preview</title><style>
  body{margin:0;padding:28px 32px;background:#0d1117;color:#e6edf3;
       font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Noto Sans',sans-serif;font-size:15px;line-height:1.6}
  .wrap{max-width:900px;margin:0 auto}
  h1{font-size:30px;border-bottom:1px solid #30363d;padding-bottom:8px}
  h2{font-size:21px;margin-top:28px;border-bottom:1px solid #21262d;padding-bottom:6px}
  a{color:#4493f8;text-decoration:none}
  code{background:#161b22;padding:2px 6px;border-radius:6px;font-size:13px}
  blockquote{border-left:4px solid #30363d;margin:0;padding:0 16px;color:#8b949e}
  hr{border:0;border-top:1px solid #21262d;margin:22px 0}
  details{border:1px solid #30363d;border-radius:8px;padding:10px 14px;margin:12px 0;background:#0d1117}
  summary{cursor:pointer;font-size:15px;padding:2px 0}
  img{vertical-align:middle;margin:3px 0}
  p img{display:block;max-width:100%}
  .tr{font-family:ui-monospace,monospace;font-size:13px;color:#8b949e;border-bottom:1px solid #21262d;padding:4px 0}
  ul{margin:6px 0}
  sub{color:#8b949e}
</style></head><body><div class="wrap">
${out.join('\n')}
</div></body></html>`;

writeFileSync(`${ROOT}/tools/_preview_portal.html`, html, 'utf8');
console.log('门户预览页已生成 tools/_preview_portal.html');
