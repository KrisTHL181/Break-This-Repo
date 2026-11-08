// 独立的进度核查：不依赖 validate.mjs 的自证，从零复算关键不变量。
import { readFileSync, readdirSync } from 'node:fs';

const ROOT = 'D:/Open/恶搞';
const langs = JSON.parse(readFileSync(`${ROOT}/tools/languages.json`, 'utf8'));
const codes = new Set(langs.map((l) => l.code));

const files = readdirSync(`${ROOT}/translations`).filter((f) => /^README\.[a-z]{2}\.md$/.test(f));
const done = files.map((f) => f.slice(7, 9)).filter((c) => codes.has(c)).sort();

console.log(`ISO 639-1 总数 : ${langs.length}`);
console.log(`已完成         : ${done.length}`);
console.log(`剩余           : ${langs.length - done.length}`);

// 结构指纹：把每行归类为 标题/空行/正文/围栏/围栏内
function fingerprint(text) {
  const out = [];
  let inFence = false;
  for (const l of text.split('\n')) {
    if (l.startsWith('```')) {
      inFence = !inFence;
      out.push('F');
      continue;
    }
    if (inFence) out.push('C');
    else if (/^#{1,6}\s/.test(l)) out.push('H');
    else if (l.trim() === '') out.push('B');
    else out.push('T');
  }
  return out.join('');
}

const srcFp = fingerprint(readFileSync(`${ROOT}/tools/upstream-README.md`, 'utf8'));
// 译文 = 头部注释行(正文) + 空行 + 原文结构
const expected = 'TB' + srcFp;

const problems = [];
for (const c of done) {
  const f = `${ROOT}/translations/README.${c}.md`;
  const t = readFileSync(f, 'utf8');
  const lines = t.split('\n');
  const n = lines.length - 1;

  if (n !== 464) problems.push(`${c}: 行数 ${n} ≠ 464`);
  if (!t.endsWith('\n') || t.endsWith('\n\n')) problems.push(`${c}: 末尾换行不是恰好一个 LF`);
  if (t.charCodeAt(0) === 0xfeff) problems.push(`${c}: 含 BOM`);
  if (t.includes('\r')) problems.push(`${c}: 含 CR`);

  const fp = fingerprint(t);
  if (fp !== expected) {
    // 定位第一处差异
    let i = 0;
    while (i < Math.min(fp.length, expected.length) && fp[i] === expected[i]) i++;
    problems.push(`${c}: 结构指纹不符（第 ${i + 1} 行起，实得 ${fp[i] ?? 'EOF'} 期望 ${expected[i] ?? 'EOF'}）`);
  }

  const first = lines[0] ?? '';
  if (!new RegExp(`^<!-- language: ${c} \\|`).test(first)) problems.push(`${c}: 首行语言标注不符`);

  // 必留字符串（抽样最关键的几条）
  for (const s of ['https://aria7.wiki', 'maturin develop', 'target/wheels/']) {
    if (!t.includes(s)) problems.push(`${c}: 缺少 ${s}`);
  }
  // 友链必须独立成行
  if (!lines.some((l) => l.trim() === 'https://aria7.wiki')) problems.push(`${c}: aria7.wiki 未独立成行`);
}

console.log(`\n独立核查（${done.length} 份）：`);
if (problems.length === 0) console.log('  ✓ 行数 / 末尾 LF / BOM / CR / 结构指纹 / 首行标注 / 必留串 全部合格');
else problems.forEach((p) => console.log('  ✗ ' + p));

console.log(`\n已完成代码：${done.join(' ')}`);
