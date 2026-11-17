#!/usr/bin/env node
// 构建/开发前同步桌面版 HDSL 的静态图片资源到 web/public/assets-img/。
// 只读 ../src/main/resources/assets/img，产物目录可随时整个删除重建。
import { copyFileSync, cpSync, existsSync, mkdirSync, readdirSync, rmSync, statSync } from "node:fs";
import { dirname, join, relative } from "node:path";
import { fileURLToPath } from "node:url";

const webRoot = dirname(dirname(fileURLToPath(import.meta.url)));
const imgDir = join(webRoot, "..", "src", "main", "resources", "assets", "img");
const outDir = join(webRoot, "public", "assets-img");

if (!existsSync(imgDir)) {
  console.warn(`[sync-assets] 桌面版资源目录不存在，跳过：${imgDir}`);
  process.exit(0);
}

rmSync(outDir, { recursive: true, force: true });
mkdirSync(outDir, { recursive: true });

/** 需要平铺拷贝到 assets-img 根的文件（壁纸 + 启动器图标 + 实例图标）。 */
const ROOT_FILES = new Set([
  "icon.png",
  "icon-title.png",
  "icon@4x.png",
  "icon@8x.png",
  "unknown_pack.png",
]);

const copied = [];
function copy(rel) {
  const src = join(imgDir, rel);
  const dst = join(outDir, rel);
  mkdirSync(dirname(dst), { recursive: true });
  copyFileSync(src, dst);
  copied.push(rel);
}

// wallpapers/*.jpg
const wallpapers = join(imgDir, "wallpapers");
if (existsSync(wallpapers)) {
  cpSync(wallpapers, join(outDir, "wallpapers"), { recursive: true });
  for (const f of readdirSync(wallpapers)) copied.push(`wallpapers/${f}`);
}

// 根目录 PNG（实例图标 dsh_*.png、草方块等 HMCL 内置实例图标）
for (const f of readdirSync(imgDir)) {
  const p = join(imgDir, f);
  if (!statSync(p).isFile()) continue;
  if (ROOT_FILES.has(f) || f.endsWith(".png") || f.endsWith(".svg")) copy(f);
}

console.log(`[sync-assets] ${copied.length} 个文件 → ${relative(webRoot, outDir)}`);
