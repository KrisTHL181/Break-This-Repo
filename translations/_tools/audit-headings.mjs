// 统计各译文里"未翻译的英文标题"，供交接时如实说明。
// 自动发现 translations/ 下全部 README.<code>.md（不再硬编码语言列表），
// 这样 184 种语言都能被同一把尺子量到。
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { headingsOf } from './toc.mjs';

const ROOT = 'D:/Open/恶搞';
const src = headingsOf(readFileSync(`${ROOT}/tools/upstream-README.md`, 'utf8').split('\n'));

// 原文里不含中文的标题下标（这些是"英文标题"）
const engIdx = [];
src.forEach((h, i) => {
  if (!/[\u4e00-\u9fff]/.test(h)) engIdx.push(i);
});
console.log(`原文标题 ${src.length} 行，其中不含中文的 ${engIdx.length} 行`);

// 允许保留原文的纯专有名词标题
const ALLOW = /Arch Linux|Fedora|Gentoo|hyw|img/;

const codes = readdirSync(`${ROOT}/translations`)
  .map((f) => /^README\.([a-z]{2})\.md$/.exec(f))
  .filter(Boolean)
  .map((m) => m[1])
  .filter((c) => existsSync(`${ROOT}/translations/README.${c}.md`))
  .sort();

console.log('\n语言  未译英文标题数  示例');
let badTotal = 0;
for (const c of codes) {
  // README.en.md：目标语言就是英语，原文里的英文标题保持英语才是对的，不参与"未译"统计。
  // （它需要被检查的是"原文中文标题是否译成了英语"，那条由 validate.mjs 的
  //   汉字残留闸门 + MUST_NOT_LEAK 负责。）
  if (c === 'en') continue;
  const dst = headingsOf(readFileSync(`${ROOT}/translations/README.${c}.md`, 'utf8').split('\n'));
  if (dst.length !== src.length) {
    console.log(`  ${c.padEnd(4)} 标题数不符 ${dst.length} vs ${src.length}`);
    badTotal++;
    continue;
  }
  const left = [];
  for (const i of engIdx) {
    if (dst[i] === src[i] && !ALLOW.test(src[i])) left.push(src[i].replace(/^#+\s*/, '').slice(0, 26));
  }
  badTotal += left.length;
  console.log(`  ${c.padEnd(4)} ${String(left.length).padStart(2)}            ${left.slice(0, 3).join(' / ')}`);
}
console.log(`\n共 ${codes.length} 种语言，未译英文标题合计 ${badTotal} 个`);
