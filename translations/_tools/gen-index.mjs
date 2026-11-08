// 生成语言索引 README.i18n.md。如实标注每种语言的完成状态，未完成的标为待译。
import { readFileSync, writeFileSync, existsSync } from 'node:fs';

const ROOT = 'D:/Open/恶搞';
const langs = JSON.parse(readFileSync(`${ROOT}/tools/languages.json`, 'utf8'));

const rows = langs.map((l) => {
  const p = `${ROOT}/translations/${l.file}`;
  const done = existsSync(p);
  let lines = 0;
  if (done) {
    const raw = readFileSync(p);
    lines = raw.toString('utf8').split('\n').length - 1;
  }
  return { ...l, done, lines };
});

const done = rows.filter((r) => r.done);
const todo = rows.filter((r) => !r.done);

const esc = (s) => s.replace(/\|/g, '\\|');

const head = `# README 多语言翻译 / README Translations

本目录收录 [Break-This-Repo](https://github.com/KrisTHL181/Break-This-Repo) 的 \`README.md\`
在**全部 ${langs.length} 个 ISO 639-1 语言**下的译文。

- 原文：[\`README.md\`](https://github.com/KrisTHL181/Break-This-Repo/blob/main/README.md)（458 行，中文/英文混杂）
- 文件名规则：\`README.<ISO 639-1 代码>.md\`，该代码同时是合法的 BCP 47 主语言标签
- 译文与原文**逐行一一对应**：章节数、代码块数量、行数全部对齐
- 代码块、Shell 命令、URL、文件路径、专有名词一律**不翻译**，保证照抄可执行
- 进度：**${done.length} / ${langs.length}**

> 翻译由 AI 批量生成并经过自动化结构校验（代码围栏数、必留字符串、章节数、行数、BOM/行尾）。
> 结构正确 ≠ 语言完美，小语种译文建议以原文为准。

`;

const table = (list) =>
  [
    '| 代码 | 语言（本地名称） | English | 译文 | 行数 |',
    '|:---:|---|---|---|---:|',
    ...list.map((r) =>
      `| \`${r.code}\` | ${esc(r.native)} | ${esc(r.en)} | ${
        r.done ? `[README.${r.code}.md](./translations/README.${r.code}.md)` : '—'
      } | ${r.done ? r.lines : ''} |`,
    ),
  ].join('\n');

const parts = [head];
parts.push(`### 已完成（${done.length}）\n`);
parts.push(done.length ? table(done) : '_暂无_');
if (todo.length) {
  parts.push(`\n### 待翻译（${todo.length}）\n`);
  parts.push(todo.map((r) => `\`${r.code}\``).join(' · '));
}
parts.push(
  `\n---\n\n由 \`tools/gen-index.mjs\` 生成；语言清单见 \`tools/languages.json\`，格式契约见 \`TRANSLATION-SPEC.md\`。\n`,
);

writeFileSync(`${ROOT}/README.i18n.md`, parts.join('\n'), 'utf8');
console.log(`README.i18n.md 已生成：完成 ${done.length} / ${langs.length}，待译 ${todo.length}`);
if (done.length) console.log('已完成代码: ' + done.map((r) => r.code).join(' '));
