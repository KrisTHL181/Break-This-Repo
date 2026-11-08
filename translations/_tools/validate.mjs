// 校验 translations/README.<code>.md 是否符合 TRANSLATION-SPEC.md。
// 用法：node tools/validate.mjs [code ...]     不给 code 则校验全部已存在的译文
import { readFileSync, existsSync, readdirSync } from 'node:fs';
import { headingsOf, anchorsOf } from './toc.mjs';

const ROOT = 'D:/Open/恶搞';
const SRC = `${ROOT}/tools/upstream-README.md`;
const DIR = `${ROOT}/translations`;

const srcLines = readFileSync(SRC, 'utf8').split('\n');
const countOf = (lines, re) => lines.filter((l) => re.test(l)).length;

// 原文基准
const BASE = {
  fences: countOf(srcLines, /^```/),
  bash: countOf(srcLines, /^```bash\s*$/),
  markdown: countOf(srcLines, /^```markdown\s*$/),
  bare: countOf(srcLines, /^```\s*$/),
  lines: srcLines.length - 1, // 末尾换行导致多一个空元素
};

// 必须原样出现在译文里的字符串（不含需要翻译的部分）
const MUST_KEEP = [
  'https://aria7.wiki',
  'maturin develop',
  'maturin build --release',
  'dpkg-buildpackage -us -uc',
  'makepkg -si',
  'rpmbuild -ba packaging/fedora/break-this-repo.spec',
  'ebuild break-this-repo-0.0.0.ebuild manifest',
  'cmake -S . -B build/cmake',
  'meson setup build/meson',
  'npm run build:compiler',
  'python -m venv .venv',
  'target/wheels/',
  'src/lib.rs',
  'pyproject.toml',
  './cat.jpeg',
  './1786763623934.jpg',
  'https://github.com/MCDReforged/MCDReforged',
  'https://www.debian.org/',
  'https://www.maturin.rs/',
  './OneShotWME壁纸/navigate.png',
  '<!--toc:start-->',
  '<!--toc:end-->',
  '[!CAUTION]',
  '[!important]',
  'k.asxz.one',
  'badge.uptimerobot.com',
];

// 不得残留的原文特征（说明漏译）：这些是中文正文里的独特片段
const MUST_NOT_LEAK = [
  '破坏这个仓库！',
  '我先喝一口再说',
  '想到什么说什么',
  '相关文件',
  '通用操作系统',
  '冷知识',
  '现场基础设施考古档案',
];

// 原文里的【英文句子】，必须被翻译成目标语言；原样出现 = 漏译。
// （TRANSLATION-SPEC §6.3 明确要求翻译 `If it works, don't move the rock.`；
//   其余几条是第一块 CAUTION、友链吐槽、Minecraft 段等英文正文。
//   注意 `[!CAUTION]` / `[!important]` 属于 Markdown 语法，必须保留，故不列入。）
const MUST_TRANSLATE_EN = [
  'Please note that the',
  'automatically merges pull requests',
  "If it works, don't move the rock",
  'Below is a poor man',
  'Also try Minecraft and Terraria',
  'If you are a Minecraft Server owner',
  '[My Blog]',
  'MCDR是对的',
];

const langs = JSON.parse(readFileSync(`${ROOT}/tools/languages.json`, 'utf8'));
const byCode = new Map(langs.map((l) => [l.code, l]));

const requested = process.argv.slice(2);
const present = existsSync(DIR)
  ? readdirSync(DIR)
      .map((f) => /^README\.([a-z]{2})\.md$/.exec(f))
      .filter(Boolean)
      .map((m) => m[1])
  : [];
const codes = requested.length ? requested : present;

const results = [];
for (const code of codes) {
  const file = `${DIR}/README.${code}.md`;
  const errs = [];
  const warns = [];
  const lang = byCode.get(code);
  if (!lang) errs.push(`未知语言代码 ${code}`);
  if (!existsSync(file)) {
    results.push({ code, ok: false, errs: ['文件不存在'], warns });
    continue;
  }

  const buf = readFileSync(file);
  if (buf[0] === 0xef && buf[1] === 0xbb && buf[2] === 0xbf) errs.push('含 UTF-8 BOM');
  if (buf.includes(0x0d)) {
    const crlf = buf.toString('latin1').split('\r\n').length - 1;
    warns.push(`含 CRLF（${crlf} 处）`);
  }

  const text = buf.toString('utf8');
  const lines = text.split('\n');
  const n = lines.length - 1;

  // 末尾换行：必须恰好一个 LF。少了会让行数恰好差 1（README.av.md 曾因此是 463），
  // 而 ±15 的行数容差正好把这种"差 1 行"放过去，所以单独查。
  if (!text.endsWith('\n')) errs.push('文件末尾缺少换行符（LF）');
  else if (text.endsWith('\n\n')) errs.push('文件末尾有多余空行');

  // 头部标注
  if (!/^<!-- language: [a-z]{2} \|/.test(lines[0] ?? '')) {
    errs.push('第一行缺少 `<!-- language: xx | ... -->` 标注');
  }

  // 代码围栏
  const f = {
    fences: countOf(lines, /^```/),
    bash: countOf(lines, /^```bash\s*$/),
    markdown: countOf(lines, /^```markdown\s*$/),
    bare: countOf(lines, /^```\s*$/),
  };
  if (f.fences !== BASE.fences) errs.push(`围栏行数 ${f.fences} ≠ 原文 ${BASE.fences}`);
  if (f.bash !== BASE.bash) errs.push(`\`\`\`bash ${f.bash} ≠ ${BASE.bash}`);
  if (f.markdown !== BASE.markdown) errs.push(`\`\`\`markdown ${f.markdown} ≠ ${BASE.markdown}`);
  if (f.bare !== BASE.bare) errs.push(`\`\`\` 闭合符 ${f.bare} ≠ ${BASE.bare}`);

  // 行数
  const delta = n - (BASE.lines + 2);
  if (Math.abs(delta) > 15) errs.push(`行数 ${n} 偏离预期 ${BASE.lines + 2} 超过 15 行`);
  else if (Math.abs(delta) > 5) warns.push(`行数 ${n}（预期 ${BASE.lines + 2}，差 ${delta}）`);

  // 结构指纹：正文逐行必须是「标题 / 空行 / 普通行」序列且与原文完全一致。
  // TRANSLATION-SPEC §5 要求"从原文第 1 行起逐行对应、不要合并或拆分段落"，
  // 所以结构不一致就是违规，不该用 ±15 行容差放过去。
  // 这条能一次抓住：行数增删、空行多/少、段落被合并、标题移位——
  // README.av.md（少末尾换行）与 README.aa.md（少一个空行）都是这种问题。
  const sig = (L) => L.map((l) => (/^#{1,6}\s/.test(l) ? 'H' : l === '' ? '_' : 'x')).join('');
  const trimTail = (L) => {
    const c = [...L];
    if (c.length && c[c.length - 1] === '') c.pop();
    return c;
  };
  const sigSrc = sig(trimTail(srcLines));
  const sigDst = sig(trimTail(lines.slice(2)));
  if (sigSrc !== sigDst) {
    let k = 0;
    while (k < Math.min(sigSrc.length, sigDst.length) && sigSrc[k] === sigDst[k]) k++;
    errs.push(
      `结构指纹与原文不一致：首个分歧在正文第 ${k + 1} 行` +
        `（原文 ${sigSrc.length} 行 vs 译文 ${sigDst.length} 行；该行原文=${sigSrc[k] ?? '无'} 译文=${sigDst[k] ?? '无'}）`,
    );
  }

  // 必留字符串
  for (const s of MUST_KEEP) if (!text.includes(s)) errs.push(`缺少必留字符串: ${s}`);
  // 不得残留原文
  for (const s of MUST_NOT_LEAK) if (text.includes(s)) warns.push(`疑似漏译（残留原文）: ${s}`);
  // 必须翻译的英文句子：原样出现即为漏译。
  // 例外：目标语言就是英语时（README.en.md），这些句子本来就该是英语，豁免。
  if (code !== 'en') {
    for (const s of MUST_TRANSLATE_EN) if (text.includes(s)) errs.push(`未译英文原文句: ${s}`);
  }

  // 未译汉字 / 假名闸门（泛化版，覆盖 MUST_NOT_LEAK 那 7 个固定片段之外的漏译）。
  // 非 CJK 语系的语言里出现汉字或假名，只可能是漏译或串了参考译文（曾真的发生过：
  // README.ch.md 混入过整行日文，README.av.md / .ab.md 残留过中文段）。
  // 例外：ja / zh 合法使用汉字；两处含中文的相对路径必须原样保留；
  //       Ciallo 颜文字里的「・」(U+30FB) 在所有语言里都原样保留，故排除该字符。
  if (code !== 'ja' && code !== 'zh') {
    const EXEMPT = [
      './留言与聊天/bigtextnews.md',
      './OneShotWME壁纸/navigate.png',
      '留言与聊天/bigtextnews.md',
      'OneShotWME壁纸/navigate.png',
    ];
    let scan = text;
    for (const e of EXEMPT) scan = scan.split(e).join('');
    scan = scan.replace(/\u30fb/g, ''); // 「・」颜文字
    const han = scan.match(/[\u4e00-\u9fff]+/g);
    if (han) errs.push(`残留未译汉字 ${han.length} 处（如「${han[0]}」）`);
    const kana = scan.match(/[\u3040-\u30fa\u30fc-\u30ff]+/g);
    if (kana) errs.push(`残留未译假名 ${kana.length} 处（如「${kana[0]}」）`);
  }

  // 图片与链接目标完整性（对比原文里所有 ./ 相对路径）
  // 只校验路径本身：链接 title（如 "Niko 乘船"）属于 SPEC 3 要求翻译的内容，
  // 因此不能把整条 ](./path "title") 拿来比对，否则会误判所有已译 title 的译文。
  const rel = [...new Set(srcLines.join('\n').match(/\]\(\.\/[^)"\s]+/g) ?? [])];
  for (const r of rel) if (!text.includes(r)) errs.push(`缺少相对链接/图片: ${r}`);

  // 章节数：标题行数（headingsOf 来自 toc.mjs，已排除代码块内的 # 注释）
  const heads = headingsOf(lines);
  const hSrc = headingsOf(srcLines);
  const hDst = heads;
  if (hSrc.length !== hDst.length) errs.push(`标题数 ${hDst.length} ≠ 原文 ${hSrc.length}`);

  // 含图片的标题行必须逐字保留（SPEC 3.1）——防止 alt="image" 被翻译导致锚点分裂
  const imgHeadSrc = srcLines.filter((l) => /^#{1,6}\s/.test(l) && l.includes('<img'));
  const imgHeadDst = lines.filter((l) => /^#{1,6}\s/.test(l) && l.includes('<img'));
  if (imgHeadSrc.length !== imgHeadDst.length) {
    errs.push(`含 <img> 的标题行数 ${imgHeadDst.length} ≠ 原文 ${imgHeadSrc.length}`);
  } else {
    for (let i = 0; i < imgHeadSrc.length; i++) {
      if (imgHeadSrc[i] !== imgHeadDst[i]) {
        errs.push(`含 <img> 的标题行必须逐字保留，第 ${i + 1} 条被改动`);
      }
    }
  }

  // 两个 <img> 标题的锚点：按 GitHub 真实规则校验（不是照抄原文的失效锚点）
  // —— 由 toc.mjs 的 anchorsOf 统一计算，这里只做自洽性检查（见下）

  // TOC 自洽性：每条 TOC 的锚点必须等于按 GitHub 规则算出的对应标题锚点
  const wantAnchors = anchorsOf(headingsOf(lines));
  const tS = lines.indexOf('<!--toc:start-->');
  const tE = lines.indexOf('<!--toc:end-->');
  if (tS === -1 || tE === -1) {
    errs.push('缺少 <!--toc:start--> / <!--toc:end--> 标记');
  } else {
    const tocRows = lines.slice(tS + 1, tE).filter((l) => l.trim().startsWith('-'));
    if (tocRows.length !== heads.length) {
      errs.push(`TOC 条目 ${tocRows.length} ≠ 标题数 ${heads.length}`);
    }
    tocRows.forEach((row, i) => {
      const got = (/\]\(#([^)]*)\)\s*$/.exec(row.trim()) ?? [])[1];
      if (got === undefined) {
        errs.push(`TOC 第 ${i + 1} 条无法解析出锚点`);
      } else if (wantAnchors[i] !== undefined && got !== wantAnchors[i]) {
        errs.push(`TOC 第 ${i + 1} 条锚点失效: 写的是 #${got.slice(0, 40)}，应为 #${wantAnchors[i].slice(0, 40)}`);
      }
    });
  }

  results.push({ code, ok: errs.length === 0, errs, warns, lines: n });
}

const ok = results.filter((r) => r.ok);
const bad = results.filter((r) => !r.ok);
for (const r of bad) {
  console.log(`✗ ${r.code}${byCode.get(r.code) ? ` (${byCode.get(r.code).en})` : ''}`);
  for (const e of r.errs) console.log(`    ERROR ${e}`);
  for (const w of r.warns) console.log(`    warn  ${w}`);
}
for (const r of ok) {
  if (r.warns.length) {
    console.log(`△ ${r.code} 通过但需留意：${r.warns.join('; ')}`);
  }
}
console.log(`\n通过 ${ok.length} / ${results.length}${bad.length ? `，失败 ${bad.length}` : ''}`);
process.exit(bad.length ? 1 : 0);
