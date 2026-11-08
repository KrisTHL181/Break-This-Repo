// 打印译文正文与原文结构指纹的【全部】差异段（不只第一处）。
import { readFileSync } from 'node:fs';

const ROOT = 'D:/Open/恶搞';
const src = readFileSync(`${ROOT}/tools/upstream-README.md`, 'utf8').split('\n');
const sig = (L) => L.map((l) => (/^#{1,6}\s/.test(l) ? 'H' : l === '' ? '_' : 'x'));
const trim = (L) => { const c = [...L]; if (c.length && c[c.length - 1] === '') c.pop(); return c; };

const S = sig(trim(src));

for (const code of process.argv.slice(2)) {
  const L = readFileSync(`${ROOT}/translations/README.${code}.md`, 'utf8').split('\n');
  const body = trim(L.slice(2));
  const T = sig(body);
  console.log(`\n########## ${code}  (原文 ${S.length} 行 / 译文 ${T.length} 行)`);

  // 找 LCS 长度用于判断，这里用简单的"逐段重同步"扫描
  let i = 0, j = 0;
  const hunks = [];
  while (i < S.length && j < T.length) {
    if (S[i] === T[j]) { i++; j++; continue; }
    // 在窗口内寻找重同步点
    const W = 40;
    let bi = -1, bj = -1;
    for (let d = 1; d <= W; d++) {
      if (i + d < S.length && S[i + d] === T[j]) { bi = i + d; bj = j; break; }
      if (j + d < T.length && S[j + d] === T[i] && S[i] === T[j + d]) { bi = i; bj = j + d; break; }
    }
    if (bi === -1) { hunks.push({ srcStart: i, srcEnd: i + 1, dstStart: j, dstEnd: j + 1 }); i++; j++; continue; }
    hunks.push({ srcStart: i, srcEnd: bi, dstStart: j, dstEnd: bj });
    i = bi; j = bj;
  }
  if (i < S.length || j < T.length) hunks.push({ srcStart: i, srcEnd: S.length, dstStart: j, dstEnd: T.length });

  for (const h of hunks) {
    console.log(`\n  ── 正文第 ${h.srcStart + 1} 行起（原文去掉 ${h.srcEnd - h.srcStart} 行 / 译文去掉 ${h.dstEnd - h.dstStart} 行）`);
    console.log(`     原文: ${S.slice(Math.max(0, h.srcStart - 1), h.srcStart + 3).join('')}   [${JSON.stringify(trim(src).slice(h.srcStart, h.srcStart + 2))}]`);
    console.log(`     译文: ${T.slice(Math.max(0, h.dstStart - 1), h.dstStart + 3).join('')}   [${JSON.stringify(body.slice(h.dstStart, h.dstStart + 2))}]`);
  }
}
