/* ================= 各视图渲染 ================= */
const Views = (() => {
  const M = () => KB.meta;

  /* ---------- 主页 ---------- */
  function home() {
    const c = M().counts;
    const groups = M().groups || {};
    const top = (M().topPages || []).slice(0, 48);
    const lines = M().questLines || [];
    const tiers = lines.filter(l => l.tier);
    const themed = lines.filter(l => !l.tier);

    const cards = [
      ['材料与合金', '粉/锭/板/流体，含最早出现阶段与获取方式', 'grp:材料', groups['材料'] || 0],
      ['机器与多方块', '结构尺寸、仓室、并行、超频种类', 'grp:机器', groups['机器'] || 0],
      ['机制与公式', '电压、超频、并行、污染、物流、AE', 'grp:机制', groups['机制'] || 0],
      ['产线与工艺', '铂处理、钛处理、稀土、硅等完整流程', 'grp:产线', groups['产线'] || 0],
      ['阶段教程', '石器→蒸汽→LV→…→UMV 全阶段攻略', 'grp:教程', groups['教程'] || 0],
      ['任务书', `${lines.length} 条任务线 / ${c.quests} 个任务`, 'nav:quests', c.quests],
    ];

    return `
    <div class="crumb">GTNH 知识库 · 本地站点</div>
    <h1>细胞图谱：GTNH 全量知识</h1>
    <p class="muted">把整合包自带的 <b>任务书</b>、<b>GTNH 中文维基</b> 与 <b>模组语言文件</b> 抽取成一张可检索、可漫游的条目网络。
      每一条 <span class="wl">蓝色链接</span> 都是真实的维基内链，右侧图谱显示它与其他条目的连接。</p>

    <div class="stats">
      <div class="stat"><b>${fmt(c.pages)}</b><span>维基图鉴</span></div>
      <div class="stat"><b>${fmt(c.edges)}</b><span>条目连接</span></div>
      <div class="stat"><b>${fmt(c.quests)}</b><span>任务（${c.questLines} 条线）</span></div>
      <div class="stat"><b>${fmt(c.items)}</b><span>物品名映射</span></div>
      <div class="stat"><b>${fmt(c.images)}</b><span>条目配图</span></div>
    </div>

    <div class="section-title"><h2 style="border:none;margin:0">从这里进入</h2></div>
    <div class="grid-cards">
      ${cards.map(([t, d, act, n]) => `
        <div class="card" data-act="${act}">
          <h4>${t} <span class="mini">${fmt(n)}</span></h4>
          <p>${d}</p>
        </div>`).join('')}
    </div>

    <div class="section-title"><h2 style="border:none;margin:0">核心节点</h2>
      <span class="mini">被引用最多的条目，知识网络的中枢</span></div>
    <div class="tagcloud">
      ${top.slice(0, 40).map(([t, d], i) =>
        `<span class="tag ${i < 8 ? 'big' : i < 20 ? 'mid' : ''}" data-w="${esc(t)}">${esc(t)} <b class="mini">${d}</b></span>`
      ).join('')}
    </div>

    <div class="section-title"><h2 style="border:none;margin:0">主线阶段</h2>
      <span class="mini">Tier 0 → Tier 12</span></div>
    <div class="grid-cards">
      ${tiers.map(l => `
        <div class="card" data-quest="${esc(l.name)}">
          <h4>${esc(l.name)}</h4>
          <p>${l.count} 个任务</p>
        </div>`).join('')}
    </div>

    <div class="section-title"><h2 style="border:none;margin:0">专题方向</h2>
      <span class="mini">${themed.length} 条</span></div>
    <div class="tagcloud">
      ${themed.map(l => `<span class="tag" data-quest="${esc(l.name)}">${esc(l.name)} <b class="mini">${l.count}</b></span>`).join('')}
    </div>

    <p class="muted tiny" style="margin-top:26px">
      数据来源：<span class="mono">gtnh-wiki-archive</span>（灰机 wiki 离线镜像，2.8.4 为主）+
      <span class="mono">config/betterquesting</span>（Daily 704）。
      配方数值请以游戏内 NEI 为准；本库用于回答“该做什么、要什么机器、什么阶段”。
    </p>`;
  }

  /* ---------- 条目页 ---------- */
  function page(d) {
    const allowed = new Set(d.neighbors || []);
    const body = MD.render(d.text, { allowed });
    const chips = [
      d.group ? `<span class="chip g-${esc(d.group)}" data-act="grp:${esc(d.group)}">${esc(d.group)}</span>` : '',
      `<span class="chip">被引用 ${d.deg || 0}</span>`,
      (d.images && d.images.length) ? `<span class="chip">${d.images.length} 张配图</span>` : '',
      `<span class="chip" data-act="graph:${esc(d.title)}">在图谱中查看</span>`,
    ].join('');

    return `
    <div class="crumb">
      <a data-nav="home">主页</a> ›
      ${d.group ? `<a data-act="grp:${esc(d.group)}">${esc(d.group)}</a> ›` : ''}
      <span>${esc(d.title)}</span>
    </div>
    <h1>${esc(d.title)}</h1>
    <div class="chips">${chips}</div>
    ${d.images && d.images.length ? MD.images(d.images, 3) : ''}
    <article class="article" id="content">${body}</article>

    ${d.backlinks && d.backlinks.length ? `
      <div class="section-title"><h2 style="border:none;margin:0">被引用</h2>
        <span class="mini">${d.backlinkCount} 个条目链接到这里</span></div>
      <div class="tagcloud">
        ${d.backlinks.slice(0, 42).map(t => `<span class="tag" data-w="${esc(t)}">${esc(t)}</span>`).join('')}
        ${d.backlinkCount > 42 ? `<span class="tag">…还有 ${d.backlinkCount - 42} 个</span>` : ''}
      </div>` : ''}

    ${d.neighbors && d.neighbors.length ? `
      <div class="section-title"><h2 style="border:none;margin:0">关联条目</h2>
        <span class="mini">正文中的站内链接</span></div>
      <div class="tagcloud">
        ${d.neighbors.slice(0, 40).map(t => `<span class="tag" data-w="${esc(t)}">${esc(t)}</span>`).join('')}
      </div>` : ''}

    <p class="muted tiny" style="margin-top:28px">
      源文件 <span class="mono">${esc(d.title)}.html</span> ·
      <a href="/api/page?t=${encodeURIComponent(d.title)}" target="_blank">查看 API 数据</a>
    </p>`;
  }

  /* ---------- 搜索结果 ---------- */
  function search(j, q) {
    if (!j.results.length) {
      return `<div class="crumb"><a data-nav="home">主页</a> › 搜索</div>
      <h1>没有找到「${esc(q)}」</h1>
      <p class="muted">试试：材料名（<span class="wl" data-w="青铜">青铜</span>、
        <span class="wl" data-w="铱">铱</span>）、机器名（<span class="wl" data-w="真空冷冻机">真空冷冻机</span>）、
        机制（<span class="wl" data-w="污染">污染</span>、<span class="wl" data-w="无损超频">无损超频</span>）、
        阶段（<span class="wl" data-w="蒸汽时代">蒸汽时代</span>）。</p>`;
    }
    const gs = [...new Set(j.results.map(r => r.g))].filter(Boolean);
    return `
    <div class="crumb"><a data-nav="home">主页</a> › 搜索</div>
    <h1>「${esc(q)}」的 ${j.results.length} 条结果</h1>
    <div class="chips">
      <span class="chip" data-act="s:${esc(q)}">全部</span>
      ${gs.map(g => `<span class="chip g-${esc(g)}" data-act="s:${esc(q)}:${esc(g)}">${esc(g)}</span>`).join('')}
    </div>
    <div class="res" id="content">
      ${j.results.map(r => `
        <div class="res-item" data-w="${esc(r.t)}">
          <h4>${highlight(esc(r.t), q)}
            ${r.g ? `<span class="chip g-${esc(r.g)}">${esc(r.g)}</span>` : ''}
            <span class="mini">被引用 ${r.d || 0} · 相关度 ${r.score}</span>
          </h4>
          <div class="snip">${r.preview ? highlight(esc(r.preview), q) : ''}</div>
        </div>`).join('')}
    </div>`;
  }

  /* ---------- 全局图谱 ---------- */
  function graph() {
    const top = (M().topPages || []).slice(0, 260);
    return `
    <div class="crumb"><a data-nav="home">主页</a> › 图谱</div>
    <h1>全局图谱</h1>
    <p class="muted">展示被引用最多的 ${top.length} 个条目及其相互连接。拖拽移动、滚轮缩放、点击节点进入词条。</p>
    <div class="graph-tools">
      <button class="btn" data-act="graphbuild:120">聚焦 120 节点</button>
      <button class="btn ghost" data-act="graphbuild:200">200 节点</button>
      <button class="btn ghost" data-act="graphrandom">随机漫游</button>
      <div class="graph-legend">
        ${Object.entries(GROUPS).map(([g, v]) => `<span><i style="background:${v.color}"></i>${g}</span>`).join('')}
      </div>
    </div>
    <div class="graph-full"><canvas id="fullCanvas"></canvas>
      <div class="graph-info" id="graphInfo">点击任意节点进入词条</div>
    </div>`;
  }

  /* ---------- 任务书 ---------- */
  function quests(lines) {
    return `
    <div class="crumb"><a data-nav="home">主页</a> › 任务书</div>
    <h1>任务书</h1>
    <p class="muted">GTNH 整合包内置 BetterQuesting 任务，共 ${lines.length} 条线。
      每条线含任务描述、提交物与奖励，顺序即游戏内推荐进度。</p>
    <div class="quest-layout">
      <ul class="quest-list" id="qlist">
        ${lines.map(l => `<li data-quest="${esc(l.name)}">
            ${esc(l.name)}<span class="n">${l.count}</span></li>`).join('')}
      </ul>
      <div id="qdetail"><p class="muted">← 选择左侧任意任务线</p></div>
    </div>`;
  }

  function questLine(d) {
    if (!d.quests) return '<p class="muted">没有任务数据</p>';
    return `
    <h2 style="margin-top:0;border:none">${esc(d.name)}
      <span class="mini">${d.quests.length} 个任务</span></h2>
    <div class="qfilter">
      <input id="qf" placeholder="过滤任务名 / 物品 ID / 描述关键词…">
    </div>
    <div id="qcards">
      ${d.quests.map(q => qcard(q)).join('')}
    </div>`;
  }
  function qcard(q) {
    const tasks = (q.tasks || []).map(t => {
      const m = t.match(/^\[([^\]]+)\]\s*(.*)$/);
      if (!m) return '';
      const label = { '提交': '提交', '可选提交': '可选提交', '合成': '合成' }[m[1]] || m[1];
      return `<li><b>${esc(label)}</b>：<span class="mono">${esc(m[2])}</span></li>`;
    }).join('');
    return `
    <div class="qcard" data-qtext="${esc((q.name + ' ' + q.desc + ' ' + (q.tasks || []).join(' ')).toLowerCase())}">
      <h4>${esc(q.name)}<span class="qid">${esc(q.id)}</span></h4>
      ${q.desc ? `<div class="desc">${esc(q.desc)}</div>` : ''}
      ${tasks ? `<ul>${tasks}</ul>` : ''}
      ${q.reward ? `<div class="reward">奖励：${esc(q.reward)}</div>` : ''}
    </div>`;
  }

  /* ---------- 索引 ---------- */
  function index(rows) {
    return `
    <div class="crumb"><a data-nav="home">主页</a> › 总索引</div>
    <h1>总索引 <span class="mini">${rows.length} 个条目</span></h1>
    <p class="muted">点字母筛选首字，或用顶部搜索。数字与符号条目归入 “#”。</p>
    <div class="qfilter"><input id="ifilter" placeholder="输入关键词过滤索引…"></div>
    <div class="alpha" id="alpha"></div>
    <div id="ilist"></div>`;
  }

  /* ---------- 物品 ---------- */
  function items() {
    return `
    <div class="crumb"><a data-nav="home">主页</a> › 物品名</div>
    <h1>物品名映射 <span class="mini">${fmt(M().counts.items)} 条</span></h1>
    <p class="muted">从 248 个模组 jar 的语言文件与 GregTech 本地化文件中提取的
      <span class="mono">内部ID → 名称</span>。任务书里的提交物 ID 可以在这里查中文名。</p>
    <div class="qfilter"><input id="itq" placeholder="输入内部 ID 或中文名，例如 gt.metaitem.02:32243 / 青铜"></div>
    <div id="itres"><p class="muted">输入以开始查询…</p></div>`;
  }

  return { home, page, search, graph, quests, questLine, qcard, index, items };
})();

window.Views = Views;
