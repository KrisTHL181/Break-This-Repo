// 兼容底线守门脚本（browserslist 的 CSS 侧执行者，见 docs/compatibility.md）：
// 把 Firefox 115 不支持、Vite/esbuild 又不会转译的 CSS 特性挡在 CI。
// JS 语法侧由同命令里的 es-check 负责（package.json 的 compat 脚本）。

import { readFileSync, readdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const assets = join(root, "dist/assets");
const failures = [];

const BLACKLIST = [
  [/:has\(/, ":has()（Firefox 121+）"],
  [/text-wrap\s*:/, "text-wrap（Firefox 121+）"],
  [/\[popover\]/, "[popover] 选择器（Firefox 125+）"],
];
for (const name of readdirSync(assets)) {
  if (!name.endsWith(".css")) continue;
  const css = readFileSync(join(assets, name), "utf8");
  for (const [re, label] of BLACKLIST) {
    if (re.test(css)) failures.push(`dist/assets/${name} 出现 ${label}`);
  }
}

if (failures.length) {
  console.error("兼容检查未通过：\n\n" + failures.join("\n\n"));
  process.exit(1);
}
console.log("compat-check: OK");
