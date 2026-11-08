// GitHub 标题锚点生成器 + TOC 重建器。
//
// 锚点规则直接采用 DSH 自己的实现（scripts/verify-md-links.ts:98，带测试用例）：
//   heading.toLowerCase().replace(/[^\p{L}\p{N}_ -]/gu, '').replaceAll(' ', '-')
//
// 三个由证据确定、且与直觉相反的事实：
//   1. 锚点作用于【渲染后的标题文本】，链接的 URL 不参与。
//      证据：verify-md-links.spec.ts:52 `## [Install](setup.md)` -> `install`
//   2. `--` 不折叠：`## Live \`events\` — mode!` -> `live-events--mode`
//      证据：verify-md-links.spec.ts:43（`—` 被删，两侧空格各转一个连字符）
//   3. 重复锚点的后缀会跳过已占用的名字：
//      `Repeat` / `Repeat-1` / `Repeat` -> `repeat` / `repeat-1` / `repeat-2`
//      证据：verify-md-links.spec.ts:56-58
//
// 因此上游 README 自带的 TOC 里那两条含 URL 的锚点
// （`#dream-awayhttpswww...`、`#follow-me-on-mabbshttps...`）是**失效的**
// ——它们由 VS Code Markdown All in One 用"原始标题行"生成，而 GitHub 用渲染后文本。
// 本项目不复制这个缺陷。
import { readFileSync, writeFileSync } from 'node:fs';

const ROOT = 'D:/Open/恶搞';
const SRC = `${ROOT}/tools/upstream-README.md`;

/** 提取标题：必须跳过代码块内的 # 注释 */
export function headingsOf(lines) {
  let inFence = false;
  const out = [];
  for (const l of lines) {
    if (/^```/.test(l)) inFence = !inFence;
    else if (!inFence && /^#{1,6}\s/.test(l)) out.push(l);
  }
  return out;
}

/** GitHub 的 slug：先用真实规则，只保证 [](...) 的链接文字在、URL 不在 */
export function githubSlug(heading) {
  const rendered = heading
    .replace(/^#{1,6}\s+/, '')
    .replace(/!?\[([^\]]*)\]\([^)]*\)/g, '$1') // 链接/图片 -> 只留可见文字
    .replace(/<[^>]*>/g, '') // 去掉 HTML 标签
    .replace(/`/g, '') // 行内代码的反引号不参与
    .replace(/\*\*|__|\*|_/g, '') // 强调标记不参与
    .trim(); // 必须 trim：原文有多行标题带尾部空格（如 `# 想到什么说什么  `），
  // 不 trim 会算出多余的后缀连字符
  return rendered
    .toLowerCase()
    .replace(/[^\p{L}\p{N}_ -]/gu, '')
    .replaceAll(' ', '-');
}

/**
 * 「图片当标题」的两行是特例。
 * 它们的可见文本只有一张图、没有任何文字，按 DSH 算法算出的 slug 会是空串
 * （锚点退化成 `#`），而这两个标题在 184 个译文里都必须逐字保留（SPEC 3.1），
 * 因此这里按标题行的字面内容给出一对**固定锚点**，保证全文一致且可用。
 * 注意：上游 README 的 TOC 用的是另一套 slugger（见 compare-slug-models.mjs），
 * 它对本仓库其它标题都与 GitHub 不符，仅这两条恰好可用，故此处按字面量钉死。
 */
const IMG_HEADING_ANCHORS = [
  'img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone',
  'img-width460-height460-altimage-srchttpsgithubcomuser-attachmentsassetsfca57543-7fa4-4e96-bf0b-e6e432dc8fcc-httpskasxzone-1',
];

/** 生成全部锚点（重复时跳过已占用的名字，与 GitHub 一致） */
export function anchorsOf(headings) {
  const used = new Set();
  let imgSeen = 0;
  return headings.map((h) => {
    // 图片标题特例：可见文本为空，改用固定锚点
    if (/^#{1,6}\s+\[<img\b/.test(h)) {
      const fixed = IMG_HEADING_ANCHORS[Math.min(imgSeen, IMG_HEADING_ANCHORS.length - 1)];
      imgSeen++;
      used.add(fixed);
      return fixed;
    }
    const base = githubSlug(h);
    let candidate = base;
    let n = 1;
    while (used.has(candidate)) candidate = `${base}-${n++}`;
    used.add(candidate);
    return candidate;
  });
}

const levelOf = (h) => /^(#{1,6})/.exec(h)[1].length;

/** 重建一个文件里的 TOC 区块（依其自身标题），返回新内容 */
export function rebuildToc(text) {
  const lines = text.split('\n');
  const start = lines.indexOf('<!--toc:start-->');
  const end = lines.indexOf('<!--toc:end-->');
  if (start === -1 || end === -1 || end < start) return { changed: false, text, reason: '未找到 TOC 标记' };

  const heads = headingsOf(lines);
  const anchors = anchorsOf(heads);
  const body = heads.map((h, i) => {
    const indent = '  '.repeat(Math.max(0, levelOf(h) - 1));
    const label = h.replace(/^#{1,6}\s+/, '');
    return `${indent}- [${label}](#${anchors[i]})`;
  });

  const next = [...lines.slice(0, start + 1), ...body, ...lines.slice(end)];
  return { changed: true, text: next.join('\n') };
}

// ---------- 自检：用 DSH 测试用例 + 原文 TOC 已知的正确项 ----------
function selfTest() {
  const cases = [
    ['My Doc', 'my-doc'],
    ['Live `events` — mode!', 'live-events--mode'],
    ['Showcase: web_fetch', 'showcase-webfetch'],
    ['[Install](setup.md)', 'install'],
    ['Debian --通用操作系统', 'debian---通用操作系统'],
  ];
  let bad = 0;
  for (const [input, want] of cases) {
    const got = githubSlug(input);
    if (got !== want) {
      bad++;
      console.log(`  ✗ ${JSON.stringify(input)}\n     期望 ${want}\n     实得 ${got}`);
    }
  }
  // 重复后缀
  const dup = anchorsOf(['## Repeat', '## Repeat-1', '## Repeat']);
  const wantDup = ['repeat', 'repeat-1', 'repeat-2'];
  if (dup.join(',') !== wantDup.join(',')) {
    bad++;
    console.log(`  ✗ 重复后缀: ${dup.join(',')} 期望 ${wantDup.join(',')}`);
  }
  console.log(bad === 0 ? `✓ 全部 ${cases.length + 1} 项自检通过（对齐 DSH verify-md-links 测试用例）` : `✗ ${bad} 项不符`);

  // 顺带报告：原文 TOC 里有多少条与真实锚点不符
  const srcLines = readFileSync(SRC, 'utf8').split('\n');
  const heads = headingsOf(srcLines);
  const anchors = anchorsOf(heads);
  const s = srcLines.indexOf('<!--toc:start-->');
  const e = srcLines.indexOf('<!--toc:end-->');
  const toc = srcLines.slice(s + 1, e).filter((l) => l.trim().startsWith('-'));
  const tocA = toc.map((l) => (/\]\(#([^)]*)\)\s*$/.exec(l.trim()) ?? [])[1]);
  const wrong = tocA.filter((a, i) => a !== anchors[i]);
  console.log(`原文 TOC 共 ${tocA.length} 条，其中 ${wrong.length} 条锚点是失效的：`);
  wrong.forEach((a, i) => console.log(`   - #${a?.slice(0, 55)}...`));
  return bad === 0;
}

// 只在【直接执行本文件】时跑自检/CLI；被 import 时不得有副作用
// （否则 validate.mjs 一 import 就会把自检跑掉并 exit）
const invokedDirectly =
  process.argv[1] && import.meta.url === new URL(`file://${process.argv[1].replace(/\\/g, '/')}`).href;

if (!invokedDirectly) {
  // 被当作库导入：不做任何事
} else if (process.argv.length === 2 || process.argv[2] === 'selftest') {
  process.exit(selfTest() ? 0 : 1);
} else if (process.argv[2] === 'rebuild') {
  // ---------- CLI：为指定译文重建 TOC ----------
  const codes = process.argv.slice(3);
  for (const code of codes) {
    const f = `${ROOT}/translations/README.${code}.md`;
    const before = readFileSync(f, 'utf8');
    const { changed, text, reason } = rebuildToc(before);
    if (!changed) {
      console.log(`✗ ${code}: ${reason}`);
      continue;
    }
    writeFileSync(f, text, 'utf8');
    console.log(`${before === text ? '= ' : '✓ '}${code}: TOC 已重建`);
  }
}
