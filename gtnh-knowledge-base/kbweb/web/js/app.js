/* ================= 应用装配：路由 / 搜索 / 交互 ================= */
let CLEANUP = [];
function cleanup(fn) { if (typeof fn === 'function') CLEANUP.push(fn); }
function runCleanup() { CLEANUP.forEach(f => { try { f(); } catch (e) { } }); CLEANUP = []; }

function setLoading(msg) { $('#main').innerHTML = `<div class="loading">${esc(msg || '载入中…')}</div>`; }

/* ---------- 路由渲染 ---------- */
async function renderRoute(route) {
  runCleanup();
  window.scrollTo({ top: 0 });
  markNav(route);

  try {
    if (route.name === 'home' || route.name === '') return renderHome();
    if (route.name === 'w') return await renderPage(route.arg, route.q.get('q') || '');
    if (route.name === 'search') return await renderSearch(route.q.get('q') || '');
    if (route.name === 'graph') return await renderFullGraph(route.q);
    if (route.name === 'quests') return await renderQuests(route.q.get('line') || '');
    if (route.name === 'items') return renderItems();
    if (route.name === 'index') return await renderIndexView();
    if (route.name === 'grp') return await renderGroup(route.arg);
    return renderHome();
  } catch (e) {
    console.error('[kbweb] route error', route, e && e.stack || e);
    $('#main').innerHTML = `<h1>出错了</h1><p class="muted">${esc(e.message)}</p>
      <p><button class="btn" data-nav="home">回到主页</button></p>`;
  }
}

function markNav(route) {
  $$('.topnav .tbtn').forEach(b => b.classList.toggle('on',
    (b.dataset.nav === route.name) || (route.name === 'w' && b.dataset.nav === 'index')));
  $$('.sidebar .navlist a').forEach(a => a.classList.toggle('on', a.dataset.nav === route.name));
}

/* ---------- 主页 ---------- */
function renderHome() {
  $('#main').innerHTML = Views.home();
  MiniGraph.set(null);
  $('#miniHint').textContent = '打开任意词条后显示其连接';
  $('#neighList').innerHTML = '<li class="muted">—</li>';
  $('#backList').innerHTML = '<li class="muted">—</li>';
  $('#metaDl').innerHTML = metaDl(null);
  document.title = 'GTNH 知识库 · 细胞图谱';
}

function metaDl(d) {
  const m = KB.meta;
  const rows = [
    ['整合包', m.pack],
    ['维基版本', m.wikiVersion],
    ['条目数', m.counts.pages],
    ['连接数', m.counts.edges],
  ];
  if (d) {
    rows.push(['当前词条', d.title], ['类别', d.group || '—'], ['被引用', d.deg || 0]);
  }
  return rows.map(([k, v]) => `<dt>${esc(k)}</dt><dd>${esc(v)}</dd>`).join('');
}

/* ---------- 条目页 ---------- */
async function renderPage(title, q) {
  setLoading('正在展开词条 ' + title + ' …');
  await MD.loadTitles().catch(() => { });
  const d = await apiPageOf(title);
  if (d.error) {
    $('#main').innerHTML = `<h1>找不到「${esc(title)}」</h1>
      <p class="muted">该条目可能只被其他页面引用但未收录。试试：</p>
      <p><span class="wl" data-w="材料">材料</span> ·
         <span class="wl" data-w="机器">机器</span> ·
         <button class="btn" data-nav="index">总索引</button></p>`;
    return;
  }
  $('#main').innerHTML = Views.page(d);
  if (q) {
    const art = $('#content');
    art.innerHTML = highlight(art.innerHTML, q);
  }
  document.title = d.title + ' · GTNH 知识库';
  MiniGraph.set(null);
  MiniGraph.set(await apiGraph(d.title, 1, 26).catch(() => null));
  MiniGraph.clickHandler(t => go('#/w/' + encodeURIComponent(t)));
  renderRail(d);
}

function renderRail(d) {
  $('#neighCount').textContent = (d.neighbors || []).length + ' 条';
  $('#neighList').innerHTML = (d.neighbors || []).slice(0, 60)
    .map(t => `<li><a data-w="${esc(t)}">${esc(t)}</a></li>`).join('') || '<li class="muted">—</li>';
  $('#blCount').textContent = d.backlinkCount + ' 条';
  $('#backList').innerHTML = (d.backlinks || []).slice(0, 60)
    .map(t => `<li><a data-w="${esc(t)}">${esc(t)}</a></li>`).join('') || '<li class="muted">—</li>';
  $('#metaDl').innerHTML = metaDl(d);
}

/* ---------- 搜索 ---------- */
async function renderSearch(q, group) {
  if (!q.trim()) { renderHome(); return; }
  setLoading('检索「' + q + '」…');
  const j = await apiSearch(q, { group, limit: 40 });
  $('#main').innerHTML = Views.search(j, q);
  document.title = q + ' · 搜索';
  MiniGraph.set(null);
  const focus = j.results[0];
  if (focus) {
    MiniGraph.set(await apiGraph(focus.t, 1, 22).catch(() => null));
    MiniGraph.clickHandler(t => go('#/w/' + encodeURIComponent(t)));
  }
  $('#neighList').innerHTML = j.results.slice(0, 20)
    .map(r => `<li><a data-w="${esc(r.t)}">${esc(r.t)}</a><span class="b">${r.score}</span></li>`).join('');
  $('#blCount').textContent = '—'; $('#backList').innerHTML = '<li class="muted">—</li>';
  $('#metaDl').innerHTML = metaDl(null);
}

/* ---------- 全屏图谱 ---------- */
async function renderFullGraph(qs) {
  $('#main').innerHTML = Views.graph();
  const size = parseInt(qs?.get('size') || '120', 10);
  await buildGraph(size, qs?.get('focus') || '');
}

async function buildGraph(size, focus) {
  const info = $('#graphInfo');
  info.textContent = '正在计算连接…';
  const j = await api('/api/topgraph?size=' + size + (focus ? '&focus=' + encodeURIComponent(focus) : ''));
  const canvas = $('#fullCanvas');
  if (!canvas) return;
  const apiG = Graph.mountFull(canvas, j, t => go('#/w/' + encodeURIComponent(t)));
  info.innerHTML = `<b>${j.nodes.length}</b> 节点 · <b>${j.links.length}</b> 连接 · 点击节点进入词条`;
  const onResize = () => { };
  window.addEventListener('resize', onResize);
  cleanup(() => window.removeEventListener('resize', onResize));
}

/* ---------- 任务书 ---------- */
async function renderQuests(line) {
  const j = await api('/api/questlines');
  $('#main').innerHTML = Views.quests(j.lines);
  wireQuestFilter();
  if (line) loadQuestLine(line);
  $('#neighList').innerHTML = '<li class="muted">—</li>';
  $('#backList').innerHTML = '<li class="muted">—</li>';
  $('#metaDl').innerHTML = metaDl(null);
  MiniGraph.set(null);
  document.title = '任务书 · GTNH 知识库';
}

async function loadQuestLine(name) {
  $$('#qlist li').forEach(li => li.classList.toggle('on', li.dataset.quest === name));
  const d = await api('/api/questline?n=' + encodeURIComponent(name));
  $('#qdetail').innerHTML = Views.questLine(d);
  wireQuestFilter(true);
  location.hash = '#/quests?line=' + encodeURIComponent(name);
}

function wireQuestFilter(reset) {
  const f = $('#qf');
  if (f) {
    f.oninput = debounce(() => {
      const v = f.value.trim().toLowerCase();
      $$('#qcards .qcard').forEach(c => {
        c.style.display = (!v || c.dataset.qtext.includes(v)) ? '' : 'none';
      });
    }, 120);
  }
}

/* ---------- 索引 ---------- */
async function renderIndexView() {
  setLoading('载入总索引…');
  const rows = KB.titleIndex || (KB.titleIndex = (await api('/api/titles')).titles);
  const list = (KB.titleIndex = await api('/api/titleindex')).rows;
  $('#main').innerHTML = Views.index(list);
  let letter = '';
  let page = 1;
  const PER = 300;

  const letters = ['#', ...'ABCDEFGHIJKLMNOPQRSTUVWXYZ', '中'];
  const isCJK = s => /[\u4e00-\u9fff]/.test(s[0]);
  function filtered() {
    return list.filter(r => {
      const t = r.t;
      if (letter === '#') return !/^[A-Za-z\u4e00-\u9fff]/.test(t);
      if (letter === '中') return isCJK(t);
      if (letter) return t[0].toUpperCase() === letter;
      return true;
    });
  }
  function paint() {
    const f = filtered();
    const maxPage = Math.max(1, Math.ceil(f.length / PER));
    page = Math.min(page, maxPage);
    const slice = f.slice((page - 1) * PER, page * PER);
    $('#ilist').innerHTML = `
      <p class="mini">共 ${f.length} 条 · 第 ${page}/${maxPage} 页</p>
      <ul class="idx-list">
        ${slice.map(r => `<li><a data-w="${esc(r.t)}">${esc(r.t)}<span>${r.d || 0}</span></a></li>`).join('')}
      </ul>
      ${maxPage > 1 ? `<div class="pager">
        <button data-p="1">首页</button>
        <button data-p="${Math.max(1, page - 1)}">上一页</button>
        <button data-p="${Math.min(maxPage, page + 1)}">下一页</button>
        <button data-p="${maxPage}">末页</button></div>` : ''}`;
    $$('#ilist .pager button').forEach(b => b.onclick = () => { page = +b.dataset.p; paint(); window.scrollTo({ top: 200 }); });
  }
  $('#alpha').innerHTML = letters.map(l => `<button data-l="${l}">${l}</button>`).join('');
  $$('#alpha button').forEach(b => b.onclick = () => {
    letter = b.dataset.l === letter ? '' : b.dataset.l;
    page = 1;
    $$('#alpha button').forEach(x => x.classList.toggle('on', x.dataset.l === letter));
    paint();
  });
  $('#ifilter').oninput = debounce(() => {
    const v = $('#ifilter').value.trim().toLowerCase();
    if (!v) { paint(); return; }
    const f = list.filter(r => r.t.toLowerCase().includes(v)).slice(0, 400);
    $('#ilist').innerHTML = `<p class="mini">匹配 ${f.length} 条（最多显示 400）</p>
      <ul class="idx-list">${f.map(r => `<li><a data-w="${esc(r.t)}">${esc(r.t)}<span>${r.d || 0}</span></a></li>`).join('')}</ul>`;
  }, 140);
  paint();
  document.title = '总索引 · GTNH 知识库';
  MiniGraph.set(null);
}

/* ---------- 按类别 ---------- */
async function renderGroup(g) {
  setLoading('载入「' + g + '」…');
  const j = await api('/api/groups?g=' + encodeURIComponent(g));
  $('#main').innerHTML = `
    <div class="crumb"><a data-nav="home">主页</a> › 类别</div>
    <h1>${esc(g)} <span class="mini">${j.rows.length} 个条目</span></h1>
    <p class="muted">按被引用次数排序；数字越大说明它是该类别里的枢纽条目。</p>
    <div class="qfilter"><input id="gf" placeholder="在「${esc(g)}」中过滤…"></div>
    <div id="glist"></div>`;
  const paint = (rows) => {
    $('#glist').innerHTML = `<ul class="idx-list">
      ${rows.map(r => `<li><a data-w="${esc(r.t)}">${esc(r.t)}<span>${r.d}</span></a></li>`).join('')}
    </ul>`;
  };
  paint(j.rows);
  $('#gf').oninput = debounce(() => {
    const v = $('#gf').value.trim().toLowerCase();
    paint(j.rows.filter(r => r.t.toLowerCase().includes(v)));
  }, 120);
  document.title = g + ' · GTNH 知识库';
  MiniGraph.set(null);
}

/* ---------- 物品名 ---------- */
function renderItems() {
  $('#main').innerHTML = Views.items();
  const q = $('#itq');
  const run = debounce(async () => {
    const v = q.value.trim();
    if (!v) { $('#itres').innerHTML = '<p class="muted">输入以开始查询…</p>'; return; }
    const j = await api('/api/item?q=' + encodeURIComponent(v));
    $('#itres').innerHTML = j.rows.length
      ? `<table class="tbl"><thead><tr><th>内部 ID</th><th>名称</th></tr></thead><tbody>
         ${j.rows.map(r => `<tr><td class="mono">${highlight(esc(r.id), v)}</td><td>${highlight(esc(r.name), v)}</td></tr>`).join('')}
         </tbody></table>`
      : `<p class="muted">没有匹配「${esc(v)}」的条目</p>`;
  }, 160);
  q.oninput = run; q.focus();
  MiniGraph.set(null);
  document.title = '物品名 · GTNH 知识库';
}

/* ---------- 顶部搜索框 ---------- */
function wireSearch() {
  const input = $('#searchInput'), box = $('#suggest');
  let items = [], cursor = -1;

  const close = () => { box.hidden = true; cursor = -1; };
  const open = (list) => {
    items = list;
    if (!list.length) return close();
    box.innerHTML = list.map((t, i) => `<div class="sg" data-i="${i}">
        <b>${esc(t)}</b><span></span></div>`).join('');
    box.hidden = false;
    $$('.sg', box).forEach(n => n.onclick = () => submit(t));
    cursor = -1;
  };
  function submit(t) {
    close();
    input.blur();
    KB.cache.clear();
    go('#/w/' + encodeURIComponent(t) + (input.value.trim() && input.value.trim() !== t
      ? '?q=' + encodeURIComponent(input.value.trim()) : ''));
  }
  const suggest = debounce(async () => {
    const v = input.value.trim();
    if (v.length < 1) return close();
    const list = await apiSuggest(v).catch(() => []);
    fillGroups(list);
    open(list);
  }, 130);

  async function fillGroups(list) {
    if (!list.length) return;
    try {
      const j = await api('/api/exists?names=' + encodeURIComponent(list.join('|')));
      const g = {};
      for (const r of j.rows || []) g[r.t] = r.g;
      $$('.sg', box).forEach(n => {
        const t = list[+n.dataset.i];
        n.querySelector('span').textContent = g[t] || '';
      });
    } catch (e) { }
  }

  input.addEventListener('input', suggest);
  input.addEventListener('focus', () => { if (input.value.trim()) suggest(); });
  input.addEventListener('keydown', e => {
    if (e.key === 'Enter') {
      const t = cursor >= 0 ? items[cursor] : input.value.trim();
      if (t) submit(t);
    } else if (e.key === 'ArrowDown' && !box.hidden) {
      cursor = Math.min(items.length - 1, cursor + 1);
      $$('.sg', box).forEach((n, i) => n.classList.toggle('on', i === cursor));
    } else if (e.key === 'ArrowUp' && !box.hidden) {
      cursor = Math.max(0, cursor - 1);
      $$('.sg', box).forEach((n, i) => n.classList.toggle('on', i === cursor));
    } else if (e.key === 'Escape') close();
  });
  document.addEventListener('click', e => { if (!e.target.closest('#searchBox')) close(); });

  document.addEventListener('keydown', e => {
    if (e.key === '/' && document.activeElement !== input) { e.preventDefault(); input.focus(); }
    if (e.key.toLowerCase() === 'k' && (e.ctrlKey || e.metaKey)) { e.preventDefault(); input.focus(); }
    if (e.key === 'Escape') input.blur();
  });
}

/* ---------- 全局点击代理 ---------- */
function wireClicks() {
  document.addEventListener('click', e => {
    const w = e.target.closest('[data-w]');
    if (w) { go('#/w/' + encodeURIComponent(w.dataset.w)); return; }

    const nav = e.target.closest('[data-nav]');
    if (nav) { go('#/' + nav.dataset.nav); return; }

    const ql = e.target.closest('[data-quest]');
    if (ql) {
      if (location.hash.startsWith('#/quests')) { loadQuestLine(ql.dataset.quest); }
      else go('#/quests?line=' + encodeURIComponent(ql.dataset.quest));
      return;
    }

    const act = e.target.closest('[data-act]');
    if (act) {
      const [kind, a, b] = act.dataset.act.split(':');
      if (kind === 'grp') go('#/grp/' + encodeURIComponent(a));
      if (kind === 'nav') go('#/' + a);
      if (kind === 's') go('#/search?q=' + encodeURIComponent(a) + (b ? '&g=' + encodeURIComponent(b) : ''));
      if (kind === 'graph') { go('#/graph?focus=' + encodeURIComponent(a)); }
      if (kind === 'graphbuild') { go('#/graph?size=' + a); }
      if (kind === 'graphrandom') randomPage();
      return;
    }
  });

  // 搜索结果的类别 chip（closest 顺序问题：放在 act 之后兜底）
  document.addEventListener('click', e => {
    const chip = e.target.closest('.res .chip');
    if (chip && chip.dataset.act) return; // 已在上面处理
  });
}

function randomPage() {
  api('/api/random').then(j => go('#/w/' + encodeURIComponent(j.t))).catch(() => { });
}

/* ---------- 侧栏 ---------- */
function wireSidebar() {
  $$('.sidebar .navlist a').forEach(a => a.onclick = () => go('#/' + a.dataset.nav));
  $('#expandGraph').onclick = () => {
    const d = MiniGraph.data;
    if (!d) return toast('先打开一个词条');
    go('#/graph?focus=' + encodeURIComponent(d.center) + '&size=120');
  };
  $('#openFullGraph').onclick = () => go('#/graph');
  $('#randomBtn').onclick = randomPage;

  // 类别
  const groups = KB.meta.groups || {};
  $('#groupList').innerHTML = Object.entries(groups)
    .sort((a, b) => b[1] - a[1])
    .map(([g, n]) => `<li><a data-act="grp:${esc(g)}"><span>
        <span class="dot" style="background:${groupColor(g)}"></span>${esc(g)}</span>
        <em>${fmt(n)}</em></a></li>`).join('');

  // 任务线
  const lines = KB.meta.questLines || [];
  $('#questLineList').innerHTML = lines.map(l => `<li><a data-quest="${esc(l.name)}">
      ${esc(l.name)}<em>${l.count}</em></a></li>`).join('');

  $('#qlCount').textContent = lines.length;
  $('#piCount').textContent = fmt(KB.meta.counts.pages);
  $('#itCount').textContent = fmt(KB.meta.counts.items);
  $('#sideStats').innerHTML =
    `${KB.meta.pack}<br>维基 ${KB.meta.wikiVersion}<br>
     ${fmt(KB.meta.counts.pages)} 条目 · ${fmt(KB.meta.counts.edges)} 连接<br>
     ${fmt(KB.meta.counts.quests)} 任务 · ${fmt(KB.meta.counts.items)} 物品名`;
  $('#footRight').textContent = `词条数据检索于本地 · 加载 ${((performance.now() - KB.start) / 1000).toFixed(2)}s`;
}

/* ---------- 启动 ---------- */
(async function boot() {
  initTheme();
  wireSearch();
  wireClicks();
  try {
    KB.meta = await api('/api/meta');
    wireSidebar();
    await MD.loadTitles().catch(() => { });
    applyRoute(location.hash);
    $('#footLeft').textContent = `${KB.meta.site} · ${KB.meta.counts.pages} 条目`;
  } catch (e) {
    $('#main').innerHTML = `<h1>无法连接服务端</h1>
      <p class="muted">请确认 <span class="mono">python kbweb/serve.py</span> 正在运行。</p>
      <p class="mono tiny">${esc(e.message)}</p>`;
  }
})();



