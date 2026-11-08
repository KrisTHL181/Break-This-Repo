import http from 'http';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = process.env.PORT || 8080;
const PUBLIC_DIR = path.join(__dirname, '..', 'public');
const FUNCTIONS_DIR = path.join(__dirname, '..', 'node-functions');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.gif': 'image/gif',
  '.pdf': 'application/pdf',
  '.md': 'text/markdown; charset=utf-8',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
};

async function loadFunction(urlPath) {
  const segments = urlPath.split('/').filter(Boolean);
  if (segments[0] !== 'api') return null;
  const funcPath = path.join(FUNCTIONS_DIR, ...segments) + '.js';
  try {
    if (fs.existsSync(funcPath)) {
      const mod = await import(`file://${funcPath.replace(/\\/g, '/')}?t=${Date.now()}`);
      return mod;
    }
  } catch (e) {
    console.error('[Functions] 加载失败:', funcPath, e.message);
  }
  return null;
}

async function handleApiRequest(req, res, urlPath) {
  const mod = await loadFunction(urlPath);
  if (!mod || !mod.onRequest) {
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'API not found' }));
    return;
  }

  const request = {
    method: req.method,
    url: `http://localhost:${PORT}${req.url}`,
    headers: new Headers(req.headers),
    json: async () => {
      return new Promise((resolve, reject) => {
        let body = '';
        req.on('data', c => body += c);
        req.on('end', () => { try { resolve(JSON.parse(body)); } catch(e) { resolve({}); } });
      });
    },
    formData: async () => {
      return new Promise((resolve) => {
        let body = '';
        req.on('data', c => body += c);
        req.on('end', () => resolve(new URLSearchParams(body)));
      });
    },
  };

  try {
    const response = await mod.onRequest({ request });
    const headers = {};
    for (const [k, v] of response.headers.entries()) headers[k] = v;
    res.writeHead(response.status || 200, headers);
    const body = await response.text();
    res.end(body);
  } catch (e) {
    console.error('[Functions] 执行错误:', urlPath, e);
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Internal server error', detail: e.message }));
  }
}

const server = http.createServer(async (req, res) => {
  const urlPath = decodeURIComponent(req.url.split('?')[0]);

  if (urlPath.startsWith('/api/')) {
    return handleApiRequest(req, res, urlPath);
  }

  let filePath = path.join(PUBLIC_DIR, urlPath);
  if (fs.existsSync(filePath) && fs.statSync(filePath).isDirectory()) {
    filePath = path.join(filePath, 'index.html');
  }
  if (!fs.existsSync(filePath)) {
    filePath = path.join(PUBLIC_DIR, urlPath + '.html');
  }
  if (!fs.existsSync(filePath)) {
    res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end('<h1>404 Not Found</h1><p><a href="/">返回首页</a></p>');
    return;
  }

  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';
  res.writeHead(200, { 'Content-Type': contentType });
  fs.createReadStream(filePath).pipe(res);
});

server.listen(PORT, () => {
  console.log(`\n  207研究所 本地预览服务器`);
  console.log(`  地址: http://localhost:${PORT}`);
  console.log(`  目录: ${PUBLIC_DIR}`);
  console.log(`  API:  /api/* 已启用 (node-functions)\n`);
});
