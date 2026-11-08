#!/usr/bin/env node
/**
 * 洛天依应援页 · 本地播放器服务器
 *
 * 目录结构（和你现在的文件夹一致）：
 *   luotianyi-fanpage\
 *     ├─ server.js  player.js  player.css  scan-music.ps1  start-player.cmd
 *     └─ LOU\
 *         ├─ index.html   ← 页面（就是 http://127.0.0.1:8123/ 打开的那张）
 *         ├─ music-list.js← 离线歌单（scan-music.ps1 生成）
 *         ├─ assets\
 *         └─ music\       ← 音乐都放这儿（可以再建子文件夹）
 *
 * 作用：
 *   1. 每次打开网页 / 点「⟳ 刷新」时实时扫描 LOU\music（含子目录）下的音频，
 *      通过 /api/tracks 返回歌单 —— 丢新歌进文件夹，刷新一下就出现，不用改代码。
 *   2. 支持 HTTP Range 发送音乐 —— 进度条随便拖，不用等整首下载完。
 *   3. 顺手把项目目录当静态目录，所以 index.html / 图片 / css / js 都正常。
 *
 * 用法： node server.js [端口]      默认 8123
 */

const http = require('http');
const fs = require('fs');
const fsp = require('fs/promises');
const path = require('path');
const url = require('url');

// ---------------------------------------------------------------- 配置
const ROOT = __dirname;                              // 项目根（本文件所在目录）
const WEB_ROOT = path.join(ROOT, 'LOU');             // 网页根（index.html 在这里）
const MUSIC_DIR = path.join(WEB_ROOT, 'music');      // 音乐目录：LOU\music
const PORT = Number(process.argv[2]) || Number(process.env.PORT) || 8123;

const AUDIO_EXTS = new Set(['.mp3', '.flac', '.m4a', '.aac', '.wav', '.ogg', '.oga', '.opus', '.wma', '.ape', '.mp4']);
const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.png': 'image/png',
  '.webp': 'image/webp', '.gif': 'image/gif', '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon', '.txt': 'text/plain; charset=utf-8', '.md': 'text/markdown; charset=utf-8'
};
const AUDIO_MIME = {
  '.mp3': 'audio/mpeg', '.flac': 'audio/flac', '.m4a': 'audio/mp4', '.aac': 'audio/aac',
  '.wav': 'audio/wav', '.ogg': 'audio/ogg', '.oga': 'audio/ogg', '.opus': 'audio/ogg',
  '.wma': 'audio/x-ms-wma', '.ape': 'audio/x-ape', '.mp4': 'audio/mp4'
};
// project 路径（相对项目根）对应的 URL 前缀
const ROUTES = [
  { prefix: '/player.js', file: 'player.js', type: MIME['.js'] },
  { prefix: '/player.css', file: 'player.css', type: MIME['.css'] }
];

// ---------------------------------------------------------------- 文件名解析
/**
 * 从文件名猜「序号 / 曲名 / 歌手」，猜错也不影响播放，只影响列表显示。
 *   "10白石溪 (2025官方重制版) - 洛天依 _ 乐正绫.mp3"
 *      -> { no: 10, title: "白石溪 (2025官方重制版)", artist: "洛天依 / 乐正绫" }
 *   "0.1蝴蝶 - 洛天依.mp3"    -> { no: 0.1, title: "蝴蝶", artist: "洛天依" }
 *   "I LOVE U - 洛天依.mp3"   -> { no: null, title: "I LOVE U", artist: "洛天依" }
 */
function parseName(base) {                       // 传入：不带扩展名的文件名
  let rest = base.trim();
  rest = rest.replace(/^\s*(?:\[[^\]]*\]|【[^】]*】)\s*/, '');

  let no = null;
  // 先吃「小数序号」(10.1 / 6.0)，再吃「紧贴汉字的整数序号」(11歌 / 3珍珠)；
  // 后面紧跟字母数字的（如 "4K画质"）不算序号，保留为曲名的一部分。
  const m = rest.match(/^(\d+)(\.\d+)?\s*?([.、]|\s*?(?=[^\s.、]))\s*(.+)$/);
  if (m) {
    const tail = m[4];
    const tight = !/[.、]/.test(m[3]);
    if (!(tight && /^[A-Za-z0-9]/.test(tail))) {
      const cand = Number(m[1] + (m[2] || ''));
      if (Number.isFinite(cand) && cand > 0) { no = cand; rest = tail; }
    }
  }

  const parts = rest.split(/\s+[-–—]\s*/);
  let title = (parts.shift() || '').trim();
  let artist = parts.join(' / ').replace(/\s*[_｜|]\s*/g, ' / ').replace(/\s*\/\s*/g, ' / ').trim();
  if (!title) title = rest;
  return { no, title, artist };
}

// ---------------------------------------------------------------- 扫描歌单
async function collectTracks() {
  const out = [];
  await walk(MUSIC_DIR, '');

  const coll = new Intl.Collator('zh-Hans-CN', { numeric: true, sensitivity: 'base' });
  out.sort((a, b) => {
    const an = a.no === null ? Infinity : a.no;
    const bn = b.no === null ? Infinity : b.no;
    if (an !== bn) return an - bn;
    return coll.compare(a.name, b.name);
  });
  return out;

  async function walk(absDir, relDir) {
    let entries;
    try { entries = await fsp.readdir(absDir, { withFileTypes: true }); }
    catch { return; }
    for (const e of entries) {
      const abs = path.join(absDir, e.name);
      const rel = relDir ? relDir + '/' + e.name : e.name;
      if (e.isDirectory()) {
        if (e.name.startsWith('.') || e.name === 'node_modules') continue;
        await walk(abs, rel);
      } else if (e.isFile()) {
        const ext = path.extname(e.name).toLowerCase();
        if (!AUDIO_EXTS.has(ext)) continue;
        let size = 0, mtime = 0;
        try { const st = await fsp.stat(abs); size = st.size; mtime = st.mtimeMs; } catch { /* ignore */ }
        const parsed = parseName(e.name.replace(/\.[^.]+$/, ''));
        out.push({
          id: rel,                                              // 相对 music 的路径 = 稳定 id
          file: rel.split('/').map(encodeURIComponent).join('/'),
          name: e.name,
          folder: relDir || '',
          no: parsed.no,
          title: parsed.title,
          artist: parsed.artist,
          ext: ext.replace('.', '').toUpperCase(),
          size,
          mtime
        });
      }
    }
  }
}

// ---------------------------------------------------------------- 响应工具
function send(res, code, body, headers) {
  res.writeHead(code, Object.assign({ 'Cache-Control': 'no-store' }, headers || {}));
  res.end(body);
}

function serveRange(req, res, abs, ext, type) {
  let stat;
  try { stat = fs.statSync(abs); } catch { return send(res, 404, 'audio not found'); }
  const total = stat.size;
  const range = req.headers.range;

  if (range) {
    const m = /bytes=(\d*)-(\d*)/.exec(range);
    if (m) {
      let start = m[1] === '' ? null : Number(m[1]);
      let end = m[2] === '' ? null : Number(m[2]);
      if (start === null && end === null) { start = 0; end = total - 1; }
      else if (start === null) { start = Math.max(0, total - end); end = total - 1; }
      else if (end === null || end >= total) { end = total - 1; }
      if (start > end || start >= total) {
        return res.writeHead(416, { 'Content-Range': 'bytes */' + total }).end();
      }
      res.writeHead(206, {
        'Content-Type': type,
        'Content-Length': end - start + 1,
        'Content-Range': 'bytes ' + start + '-' + end + '/' + total,
        'Accept-Ranges': 'bytes',
        'Cache-Control': 'no-store',
        'Last-Modified': stat.mtime.toUTCString()
      });
      if (req.method === 'HEAD') return res.end();
      return fs.createReadStream(abs, { start, end }).on('error', () => res.destroy()).pipe(res);
    }
  }

  res.writeHead(200, {
    'Content-Type': type,
    'Content-Length': total,
    'Accept-Ranges': 'bytes',
    'Cache-Control': 'no-store',
    'Last-Modified': stat.mtime.toUTCString()
  });
  if (req.method === 'HEAD') return res.end();
  fs.createReadStream(abs).on('error', () => res.destroy()).pipe(res);
}

function serveFile(req, res, abs) {
  let stat;
  try { stat = fs.statSync(abs); } catch { return send(res, 404, 'not found: ' + path.basename(abs)); }
  if (stat.isDirectory()) return serveFile(req, res, path.join(abs, 'index.html'));
  const ext = path.extname(abs).toLowerCase();
  const type = MIME[ext];
  if (!type) return send(res, 403, 'forbidden');
  res.writeHead(200, {
    'Content-Type': type,
    'Content-Length': stat.size,
    'Cache-Control': ext === '.html' ? 'no-store' : 'no-cache'
  });
  if (req.method === 'HEAD') return res.end();
  fs.createReadStream(abs).on('error', () => res.destroy()).pipe(res);
}

// ---------------------------------------------------------------- 服务器
const server = http.createServer(async (req, res) => {
  let pathname;
  try { pathname = decodeURIComponent(url.parse(req.url).pathname); }
  catch { return send(res, 400, 'bad path'); }

  if (req.method !== 'GET' && req.method !== 'HEAD') return send(res, 405, 'method not allowed');

  // 1) 首页：根路径直接送到 LOU\index.html
  if (pathname === '/' || pathname === '/index.html') {
    return serveFile(req, res, path.join(WEB_ROOT, 'index.html'));
  }

  // 2) 歌单（每次请求实时扫描）
  if (pathname === '/api/tracks') {
    let list = [];
    try { list = await collectTracks(); } catch { /* 目录不存在 -> 空歌单 */ }
    // 版本号 = 最新修改时间 + 曲目数：新增/删除文件也会让版本变化，客户端据此判断歌单是否变过
    const version = (list.length ? Math.max.apply(null, list.map(t => Math.round(t.mtime || 0))) : 0) + '-' + list.length;
    return send(res, 200, JSON.stringify({
      version,
      generatedAt: Date.now(),
      root: 'LOU/music',
      dir: MUSIC_DIR,
      exists: fs.existsSync(MUSIC_DIR),
      total: list.length,
      tracks: list.map(t => { const { mtime, ...rest } = t; return rest; })
    }), { 'Content-Type': MIME['.json'] });
  }

  // 3) 音乐（支持 Range 拖动进度）
  if (pathname.startsWith('/music/')) {
    const rel = pathname.slice('/music/'.length);
    const abs = path.resolve(MUSIC_DIR, rel);
    if (abs !== MUSIC_DIR && !abs.startsWith(MUSIC_DIR + path.sep)) return send(res, 403, 'forbidden');
    const ext = path.extname(abs).toLowerCase();
    if (!AUDIO_EXTS.has(ext)) return send(res, 403, 'forbidden');
    return serveRange(req, res, abs, ext, AUDIO_MIME[ext] || 'application/octet-stream');
  }

  // 4) 页面同级的文件：/LOU/xxx
  if (pathname === '/LOU') { res.writeHead(301, { Location: '/LOU/' }); return res.end(); }
  if (pathname.startsWith('/LOU/')) {
    const rel = pathname.slice('/LOU/'.length);
    if (rel === 'music-list.js' || rel.startsWith('music/')) {
      // 这两者在别处/别的方式处理，这里避免重复
      if (rel.startsWith('music/')) return send(res, 403, 'use /music/');
    }
    const abs = path.resolve(WEB_ROOT, rel);
    if (abs !== WEB_ROOT && !abs.startsWith(WEB_ROOT + path.sep)) return send(res, 403, 'forbidden');
    return serveFile(req, res, abs);
  }

  // 5) 项目根的文件：/player.js、/player.css 等
  for (const r of ROUTES) {
    if (pathname === r.prefix) return serveFile(req, res, path.join(ROOT, r.file));
  }

  send(res, 404, 'not found: ' + pathname);
});

server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.error('\n[!] 端口 ' + PORT + ' 已经被占用。换一个端口：  node server.js 8124\n');
  } else {
    console.error('[!] 服务器错误：', err.message);
  }
  process.exit(1);
});

server.listen(PORT, '127.0.0.1', () => {
  const ok = fs.existsSync(MUSIC_DIR);
  const page = fs.existsSync(path.join(WEB_ROOT, 'index.html'));
  console.log('--------------------------------------------------');
  console.log('  洛天依应援页 · 本地服务器已启动');
  console.log('  页面地址： http://127.0.0.1:' + PORT + '/');
  console.log('  页面文件： ' + path.join(WEB_ROOT, 'index.html') + (page ? '' : '   [×] 不存在！'));
  console.log('  音乐目录： ' + MUSIC_DIR);
  console.log('  目录状态： ' + (ok ? '已找到，歌单实时扫描中' : '[×] 未找到，请检查 LOU\\music'));
  console.log('  停止服务： 按 Ctrl + C');
  console.log('--------------------------------------------------');
});
