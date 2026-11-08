// 生成扩展图标（B站粉底 + 白色双三角快进图标），仅依赖 Node 内置模块
// 用法：node tools/make_icons.js
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c;
  }
  return t;
})();

function crc32(buf) {
  let crc = -1;
  for (let i = 0; i < buf.length; i++) crc = (crc >>> 8) ^ CRC_TABLE[(crc ^ buf[i]) & 0xff];
  return (crc ^ -1) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body));
  return Buffer.concat([len, body, crc]);
}

const BG = Buffer.from([251, 114, 153, 255]); // B站粉 #FB7299
const FG = Buffer.from([255, 255, 255, 255]);
const TRANSPARENT = Buffer.from([0, 0, 0, 0]);

function iconPixels(size) {
  const r = size * 0.22; // 圆角半径
  const pad = size * 0.18;
  const h = size - 2 * pad; // 三角形高度
  const cy = size / 2;
  const gap = Math.max(1, size * 0.05);
  const w = (size - 2 * pad - gap) / 2; // 每个三角形宽度
  const tris = [
    { x0: pad, x1: pad + w },
    { x0: pad + w + gap, x1: pad + 2 * w + gap },
  ];

  const insideTri = (x, y, t) => {
    if (x < t.x0 || x > t.x1) return false;
    const half = (h / 2) * (1 - (x - t.x0) / (t.x1 - t.x0));
    return Math.abs(y - cy) <= half;
  };

  const rows = [];
  for (let y = 0; y < size; y++) {
    const row = Buffer.alloc(1 + size * 4);
    for (let x = 0; x < size; x++) {
      // 圆角方形：到内缩矩形 [r, size-1-r] 的最近点距离不超过 r
      const nx = Math.min(Math.max(x, r), size - 1 - r);
      const ny = Math.min(Math.max(y, r), size - 1 - r);
      const insideRect = (x - nx) ** 2 + (y - ny) ** 2 <= r * r;
      const color = !insideRect
        ? TRANSPARENT
        : tris.some((t) => insideTri(x, y, t))
          ? FG
          : BG;
      color.copy(row, 1 + x * 4);
    }
    rows.push(row);
  }
  return Buffer.concat(rows);
}

function writeIcon(size, file) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(size, 0);
  ihdr.writeUInt32BE(size, 4);
  ihdr[8] = 8; // 位深
  ihdr[9] = 6; // RGBA
  const png = Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(iconPixels(size), { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
  fs.writeFileSync(file, png);
  console.log(`已生成 ${file} (${size}x${size}, ${png.length} 字节)`);
}

const outDir = path.join(__dirname, '..', 'icons');
fs.mkdirSync(outDir, { recursive: true });
for (const size of [16, 32, 48, 128]) {
  writeIcon(size, path.join(outDir, `icon${size}.png`));
}
