/* ================= GTNH 知识库 · 前端内核 ================= */
const KB = {
  meta: null,
  titleIndex: null,
  cache: new Map(),
  start: performance.now(),
};

/* ---------- DOM helpers ---------- */
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
function el(tag, attrs = {}, html) {
  const n = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v === true) n.setAttribute(k, '');
    else if (v !== false && v != null) n.setAttribute(k, v);
  }
  if (html != null) n.innerHTML = html;
  return n;
}
function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function fmt(n) {
  return n >= 10000 ? (n / 10000).toFixed(1) + ' 万' : String(n);
}
function toast(msg, ms = 1900) {
  const t = $('#toast');
  t.textContent = msg; t.hidden = false;
  clearTimeout(t._h); t._h = setTimeout(() => { t.hidden = true; }, ms);
}
function debounce(fn, ms = 160) {
  let h; return (...a) => { clearTimeout(h); h = setTimeout(() => fn(...a), ms); };
}

/* ---------- HTTP ---------- */
async function api(path) {
  if (KB.cache.has(path)) return KB.cache.get(path);
  const r = await fetch(path);
  if (!r.ok) throw new Error('HTTP ' + r.status + ' ' + path);
  const j = await r.json();
  KB.cache.set(path, j);
  if (KB.cache.size > 400) KB.cache.delete(KB.cache.keys().next().value);
  return j;
}
const apiPageOf = t => api('/api/page?t=' + encodeURIComponent(t));
const apiSearch = (q, opt = {}) => api('/api/search?q=' + encodeURIComponent(q) +
  (opt.group ? '&group=' + encodeURIComponent(opt.group) : '') +
  (opt.limit ? '&limit=' + opt.limit : ''));
const apiGraph = (t, depth = 1, limit = 26) => api(`/api/graph?t=${encodeURIComponent(t)}&depth=${depth}&limit=${limit}`);
const apiSuggest = q => api('/api/suggest?q=' + encodeURIComponent(q));

/* ---------- 组别配色 ---------- */
const GROUPS = {
  '材料': { color: '#7ee787' },
  '机器': { color: '#6fd2ff' },
  '机制': { color: '#ffd479' },
  '产线': { color: '#c0a8ff' },
  '教程': { color: '#ff9b9b' },
  '其他': { color: '#8fa3b8' },
};
const groupColor = g => (GROUPS[g] || GROUPS['其他']).color;

/* ---------- 路由 ---------- */
function go(hash, push = true) {
  if (push) location.hash = hash; else { applyRoute(hash); }
}
function applyRoute(hash) {
  const h = (hash || location.hash || '#/home').replace(/^#/, '');
  const [path, query] = h.split('?');
  const q = new URLSearchParams(query || '');
  const seg = path.split('/').filter(Boolean);
  const route = { name: seg[0] || 'home', arg: decodeURIComponent(seg[1] || ''), q };
  if (typeof renderRoute === 'function') renderRoute(route);
}
window.addEventListener('hashchange', () => applyRoute(location.hash));

/* ---------- 主题 ---------- */
function initTheme() {
  const saved = localStorage.getItem('kb-theme');
  const t = saved || 'dark';
  document.documentElement.dataset.theme = t;
  $('#themeBtn')?.addEventListener('click', () => {
    const now = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = now;
    localStorage.setItem('kb-theme', now);
    toast(now === 'dark' ? '暗色 · 显微镜模式' : '浅色 · 论文模式');
    if (window.MiniGraph) MiniGraph.redraw();
  });
}

/* ---------- 关键词高亮 ---------- */
function highlight(html, q) {
  if (!q || q.length < 2) return html;
  const terms = q.split(/[\s,，。;；、]+/).filter(t => t.length >= 2).slice(0, 5);
  let out = html;
  for (const t of terms) {
    const re = new RegExp('(' + t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + ')', 'gi');
    out = out.replace(/>([^<]*)</g, (m, txt) => '>' + txt.replace(re, '<mark>$1</mark>') + '<');
  }
  return out;
}

/* ---------- 全局导出（便于控制台调试与单文件打包） ---------- */
Object.assign(window, {
  KB, $, $$, el, esc, fmt, toast, debounce, api,
  apiPageOf, apiSearch, apiGraph, apiSuggest,
  GROUPS, groupColor, go, applyRoute, initTheme, highlight,
});

