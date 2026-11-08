/* ================= 维基正文渲染器 =================
   把抽取后的纯文本渲染成结构化 HTML：
   - ## 标题层级、• 列表、<details> 折叠块
   - 自动识别正文中的“已有条目名”，转成 [[细胞链接]]（服务端校验存在性，避免误链）
   - 图片、表格、引用等
   ================================================== */
const MD = (() => {

  let TITLES = null;                 // Set(标题)
  const EXISTS = new Map();          // 候选词 -> bool
  let pending = new Set();

  function loadTitles() {
    if (TITLES) return Promise.resolve(TITLES);
    if (!loadTitles._promise) {
      loadTitles._promise = apiPageOf('/api/titles').then(j => {
        TITLES = new Set(j.titles);
        const f = new Set();
        for (const t of j.titles) if (t) f.add(t[0]);
        TITLES._firsts = f;
        EXISTS.clear();          // 丢掉加载前的否定缓存，避免永久漏链
        return TITLES;
      });
    }
    return loadTitles._promise;
  }

  function exists(name) {
    if (EXISTS.has(name)) return EXISTS.get(name);
    if (TITLES) return TITLES.has(name);
    if (!pending.has(name)) {
      pending.add(name);
      setTimeout(flush, 30);
    }
    return null;   // 未知
  }
  const flush = debounce(async () => {
    const names = [...pending].filter(n => !EXISTS.has(n));
    pending = new Set();
    if (!names.length) return;
    try {
      const j = await apiPageOf('/api/exists?names=' + encodeURIComponent(names.slice(0, 400).join('|')));
      for (const n of names) EXISTS.set(n, false);
      for (const n of j.have) EXISTS.set(n, true);
      const cc = document.getElementById('content');
      if (cc && cc._rerender) cc._rerender();
    } catch (e) { /* ignore */ }
  }, 90);

  function looksLikeTitle(s) {
    if (!s) return false;
    const t = s.replace(/^[·•\s]+|[·•\s]+$/g, '');
    if (t.length < 2 || t.length > 28) return false;
    if (/^[\d\s.,%x×+\-–—/()（）]+$/i.test(t)) return false;
    if (/^\d+(\.\d+)*$/.test(t)) return false;
    if (/[，。；：！？、（）“”‘’]/.test(t)) return false;      // 句子片段不链
    if (/(的|了|是|在|和|与|或|等|中|上|下|可|会|要|有|无|会|就|都|很|更|最)$/.test(t) && t.length > 6) return false;
    return true;
  }

  function linkify(text, allowed) {
    const firsts = TITLES ? TITLES._firsts : null;
    if (!firsts) return esc(text);
    let out = '', buf = '';
    const flush = () => { if (buf) { out += esc(buf); buf = ''; } };
    let i = 0;
    while (i < text.length) {
      const c = text[i];
      if (!Linkable.test(c)) { buf += c; i++; continue; }
      if (allowed && allowed.size) {
        // 优先匹配"关联条目"（真实站内链接，权威）
        let hit = null;
        for (let L = Math.min(28, text.length - i); L >= 2; L--) {
          const cand = text.substr(i, L);
          if (allowed.has(cand)) { hit = cand; break; }
        }
        if (hit) { flush(); out += wl(hit); i += hit.length; continue; }
      }
      if (firsts.has(c)) {
        let hit = null;
        for (let L = Math.min(28, text.length - i); L >= 2; L--) {
          const cand = text.substr(i, L);
          if (TITLES.has(cand) && looksLikeTitle(cand)) { hit = cand; break; }
        }
        if (hit) { flush(); out += wl(hit); i += hit.length; continue; }
      }
      buf += c; i++;
    }
    flush();
    return out;
  }
  const wlCache = new Map();
  function wl(title) {
    if (wlCache.has(title)) return wlCache.get(title);
    const html = `<a class="wl" data-w="${esc(title)}">${esc(title)}</a>`;
    wlCache.set(title, html);
    return html;
  }
  function wlKnown(title, label, group) {
    return `<a class="wl${group ? ' g-' + group : ''}" data-w="${esc(title)}">${esc(label || title)}</a>`;
  }

  const IMG_BASE = '/assets/';
  const Linkable = /[\u4e00-\u9fff\u3040-\u30ffA-Za-z0-9_\-\.]/;

  function render(text, opt = {}) {
    const lines = String(text || '').split('\n');
    const out = [];
    let list = null, inPre = false, pre = [];
    let tbl = null;
    const closeList = () => { if (list) { out.push(`</${list}>`); list = null; } };
    const allowed = opt.allowed || null;

    // markdown 表格：| a | b | + | --- | --- | 分隔行 -> 真正的 <table class="tbl">
    const cellsOf = t => t.replace(/^\|/, '').replace(/\|$/, '').split('|').map(x => x.trim());
    const isSep = t => cellsOf(t).every(c => /^:?-{2,}:?$/.test(c));
    const isRow = t => t.startsWith('|') && t.endsWith('|') && (t.match(/\|/g) || []).length >= 3;
    const closeTable = () => {
      if (!tbl || !tbl.length) { tbl = null; return; }
      const head = tbl[0], body = tbl.slice(1);
      out.push('<div class="tblwrap"><table class="tbl"><thead><tr>' +
        head.map(c => '<th>' + linkify(c, allowed) + '</th>').join('') +
        '</tr></thead><tbody>' +
        body.map(r => '<tr>' + r.map(c => '<td>' + linkify(c, allowed) + '</td>').join('') + '</tr>').join('') +
        '</tbody></table></div>');
      tbl = null;
    };

    for (let i = 0; i < lines.length; i++) {
      let ln = lines[i];
      let t = ln.trim();
      if (!t) { closeList(); closeTable(); continue; }

      if (t.startsWith('<details') || t.startsWith('</details') || t.startsWith('<summary') || t.startsWith('</summary')) {
        closeList(); closeTable(); out.push(t); continue;
      }
      if (/^```/.test(t)) {
        if (!inPre) { closeList(); closeTable(); inPre = true; pre = []; }
        else { inPre = false; out.push('<pre><code>' + esc(pre.join('\n')) + '</code></pre>'); }
        continue;
      }
      if (inPre) { pre.push(ln); continue; }

      if (isRow(t)) {
        if (isSep(t)) continue;                 // 分隔行不显示
        if (!tbl) { closeList(); tbl = []; }
        tbl.push(cellsOf(t));
        continue;
      }
      closeTable();

      let m;
      if ((m = t.match(/^(#{1,4})\s*(.+)$/))) {
        closeList();
        const lvl = Math.min(4, m[1].length + 1);
        out.push(`<h${lvl}>${linkify(m[2], allowed)}</h${lvl}>`);
        continue;
      }
      if (/^[-–—_=]{4,}$/.test(t)) { closeList(); out.push('<hr>'); continue; }
      if (/^[·•]\s?/.test(t)) {
        if (list !== 'ul') { closeList(); out.push('<ul class="dotlist">'); list = 'ul'; }
        out.push('<li>' + linkify(t.replace(/^[·•]\s?/, ''), allowed) + '</li>');
        continue;
      }
      if ((m = t.match(/^[-*]\s+(.+)$/))) {
        if (list !== 'ul') { closeList(); out.push('<ul>'); list = 'ul'; }
        out.push('<li>' + linkify(m[1], allowed) + '</li>');
        continue;
      }
      if ((m = t.match(/^\d+[.)]\s+(.+)$/))) {
        if (list !== 'ol') { closeList(); out.push('<ol>'); list = 'ol'; }
        out.push('<li>' + linkify(m[1], allowed) + '</li>');
        continue;
      }
      if (/^(⚠|📝|注意|警告|提示)/.test(t) || /^>\s?/.test(t)) {
        closeList();
        out.push('<blockquote>' + linkify(t.replace(/^>\s?/, ''), allowed) + '</blockquote>');
        continue;
      }
      if (/\|/.test(t) && (t.match(/\|/g) || []).length >= 2 && t.length < 400) {
        closeList();
        const cells = t.split('|').map(x => x.trim()).filter((x, idx, a) => !(x === '' && (idx === 0 || idx === a.length - 1)));
        if (cells.length >= 2) {
          out.push('<div class="tblrow">' + cells.map(c => linkify(c, allowed)).join(' <span class="sep">|</span> ') + '</div>');
          continue;
        }
      }
      closeList();
      out.push('<p>' + linkify(t, allowed) + '</p>');
    }
    closeTable();
    closeList();
    return out.join('\n');
  }

  function images(names, upTo = 3) {
    if (!names || !names.length) return '';
    const imgs = names.slice(0, upTo).map(n =>
      `<a href="${IMG_BASE}${encodeURIComponent(n)}" target="_blank" title="${esc(n)}">
         <img loading="lazy" src="${IMG_BASE}${encodeURIComponent(n)}" alt="${esc(n)}">
       </a>`).join('');
    return `<div class="imgrow">${imgs}</div>`;
  }

  return { render, images, loadTitles, wlKnown, wl, esc };
})();

window.MD = MD;

