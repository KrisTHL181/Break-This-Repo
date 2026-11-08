/* ================= 图谱渲染：力导向 + canvas 细胞视图 ================= */
const Graph = (() => {

  function makeSim(nodes, links, opt = {}) {
    const W = opt.w || 800, H = opt.h || 600, C = opt.center || nodes[0]?.id;
    const pos = new Map(), vel = new Map();
    nodes.forEach((n, i) => {
      if (n.id === C) { pos.set(n.id, { x: W / 2, y: H / 2 }); vel.set(n.id, { x: 0, y: 0 }); return; }
      const a = (i / nodes.length) * Math.PI * 2, r = Math.min(W, H) * (0.26 + 0.2 * Math.random());
      pos.set(n.id, { x: W / 2 + Math.cos(a) * r, y: H / 2 + Math.sin(a) * r });
      vel.set(n.id, { x: 0, y: 0 });
    });
    const deg = new Map();
    links.forEach(l => { deg.set(l[0], (deg.get(l[0]) || 0) + 1); deg.set(l[1], (deg.get(l[1]) || 0) + 1); });

    function step(alpha) {
      const k = opt.k || 58;
      // 斥力（网格近似，够用且稳定）
      const arr = [...pos.entries()];
      for (let i = 0; i < arr.length; i++) {
        const [ai, pa] = arr[i];
        for (let j = i + 1; j < arr.length; j++) {
          const [bi, pb] = arr[j];
          let dx = pb.x - pa.x, dy = pb.y - pa.y;
          let d2 = dx * dx + dy * dy || 0.01;
          if (d2 > 90000) continue;
          const d = Math.sqrt(d2);
          const f = (k * k) / d2;
          const fx = (dx / d) * f, fy = (dy / d) * f;
          vel.get(ai).x -= fx; vel.get(ai).y -= fy;
          vel.get(bi).x += fx; vel.get(bi).y += fy;
        }
      }
      // 弹簧
      for (const [a, b] of links) {
        const pa = pos.get(a), pb = pos.get(b);
        if (!pa || !pb) continue;
        let dx = pb.x - pa.x, dy = pb.y - pa.y;
        const d = Math.sqrt(dx * dx + dy * dy) || 0.01;
        const ideal = opt.linkDist || 108;
        const f = (d - ideal) * 0.055;
        const fx = (dx / d) * f, fy = (dy / d) * f;
        vel.get(a).x += fx; vel.get(a).y += fy;
        vel.get(b).x -= fx; vel.get(b).y -= fy;
      }
      // 向心 + 阻尼 + 边界
      for (const [id, p] of pos) {
        const v = vel.get(id);
        const cx = W / 2, cy = H / 2;
        v.x += (cx - p.x) * 0.012; v.y += (cy - p.y) * 0.012;
        v.x *= 0.82; v.y *= 0.82;
        p.x += v.x * alpha; p.y += v.y * alpha;
        const pad = 34;
        p.x = Math.max(pad, Math.min(W - pad, p.x));
        p.y = Math.max(pad, Math.min(H - pad, p.y));
      }
    }
    return { pos, vel, deg, step };
  }

  function draw(ctx, sim, nodes, links, opt = {}) {
    const W = opt.w, H = opt.h;
    ctx.clearRect(0, 0, W, H);
    const hl = opt.highlight;
    // 连线
    links.forEach(([a, b]) => {
      const pa = sim.pos.get(a), pb = sim.pos.get(b);
      if (!pa || !pb) return;
      const on = !hl || hl.has(a) || hl.has(b);
      const mx = (pa.x + pb.x) / 2, my = (pa.y + pb.y) / 2 - 8;
      ctx.beginPath();
      ctx.moveTo(pa.x, pa.y);
      ctx.quadraticCurveTo(mx, my, pb.x, pb.y);
      ctx.strokeStyle = on ? 'rgba(42,212,200,.34)' : 'rgba(120,150,180,.10)';
      ctx.lineWidth = on ? 1.15 : 0.7;
      ctx.stroke();
    });
    // 节点
    nodes.forEach(n => {
      const p = sim.pos.get(n.id);
      if (!p) return;
      const d = sim.deg.get(n.id) || 1;
      const isCenter = n.id === opt.center;
      const r = isCenter ? 11 : Math.min(9, 3.4 + Math.sqrt(d) * 1.35);
      const c = groupColor(n.g);
      const on = !hl || hl.has(n.id) || isCenter;
      // 细胞膜光晕
      ctx.beginPath();
      ctx.arc(p.x, p.y, r + 5, 0, Math.PI * 2);
      ctx.fillStyle = c.replace(')', ',.10)').replace('rgb', 'rgba');
      ctx.globalAlpha = on ? 0.55 : 0.12;
      ctx.fillStyle = c;
      ctx.globalAlpha = on ? 0.13 : 0.04;
      ctx.fill();
      ctx.globalAlpha = 1;
      // 细胞体
      ctx.beginPath();
      ctx.arc(p.x, p.y, r, 0, Math.PI * 2);
      ctx.fillStyle = isCenter ? '#0b111a' : c;
      ctx.globalAlpha = on ? 0.92 : 0.22;
      ctx.fill();
      ctx.globalAlpha = 1;
      ctx.lineWidth = isCenter ? 2.4 : 1;
      ctx.strokeStyle = isCenter ? '#fff' : c;
      ctx.stroke();
      // 标签
      const show = isCenter || r > 6.4 || (hl && hl.has(n.id)) || opt.allLabels;
      if (show) {
        const label = n.t.length > 14 ? n.t.slice(0, 13) + '…' : n.t;
        ctx.font = (isCenter ? '600 13px ' : '11.5px ') + 'var(--sans), sans-serif';
        ctx.textAlign = 'center';
        ctx.fillStyle = 'rgba(4,8,12,.78)';
        const w = ctx.measureText(label).width + 8;
        ctx.fillRect(p.x - w / 2, p.y + r + 3, w, 14);
        ctx.fillStyle = isCenter ? '#ffffff' : (on ? '#dfeaf5' : 'rgba(180,200,220,.45)');
        ctx.fillText(label, p.x, p.y + r + 14);
      }
      n._x = p.x; n._y = p.y; n._r = r;
    });
  }

  /* ---------- 交互式全屏图谱 ---------- */
  function mountFull(canvas, data, onPick) {
    const ctx = canvas.getContext('2d');
    let W = 0, H = 0, dpr = Math.min(2, window.devicePixelRatio || 1);
    const nodes = data.nodes.map((n, i) => ({ ...n, id: i }));
    const links = data.links;
    const sim = makeSim(nodes, links, { w: W, h: H, center: 0 });
    let alpha = 1, pan = { x: 0, y: 0 }, zoom = 1, hover = null, dragging = null, moved = false;

    function resize() {
      const r = canvas.parentElement.getBoundingClientRect();
      W = r.width; H = r.height;
      canvas.width = W * dpr; canvas.height = H * dpr;
      canvas.style.width = W + 'px'; canvas.style.height = H + 'px';
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    }
    resize();
    new ResizeObserver(resize).observe(canvas.parentElement);

    function frame() {
      if (alpha > 0.004) { sim.step(alpha); alpha *= 0.985; }
      ctx.save();
      ctx.translate(pan.x, pan.y); ctx.scale(zoom, zoom);
      draw(ctx, sim, nodes, links, { w: W / zoom, h: H / zoom, center: 0, allLabels: zoom > 1.5 });
      ctx.restore();
      requestAnimationFrame(frame);
    }
    frame();

    const toLocal = e => {
      const r = canvas.getBoundingClientRect();
      return { x: (e.clientX - r.left - pan.x) / zoom, y: (e.clientY - r.top - pan.y) / zoom };
    };
    canvas.addEventListener('mousemove', e => {
      const p = toLocal(e);
      if (dragging) {
        const n = nodes[dragging.i];
        sim.pos.set(n.id, { x: p.x, y: p.y });
        sim.vel.set(n.id, { x: 0, y: 0 });
        alpha = Math.max(alpha, 0.4); moved = true;
        return;
      }
      hover = null;
      for (const n of nodes) {
        const dx = p.x - n._x, dy = p.y - n._y;
        if (dx * dx + dy * dy < (n._r + 6) ** 2) { hover = n; break; }
      }
      canvas.style.cursor = hover ? 'pointer' : 'grab';
    });
    canvas.addEventListener('mousedown', e => {
      const p = toLocal(e);
      for (const n of nodes) {
        const dx = p.x - n._x, dy = p.y - n._y;
        if (dx * dx + dy * dy < (n._r + 6) ** 2) { dragging = { i: n.id }; moved = false; return; }
      }
      dragging = { pan: true, sx: e.clientX - pan.x, sy: e.clientY - pan.y };
      canvas.style.cursor = 'grabbing';
    });
    window.addEventListener('mouseup', () => { dragging = null; canvas.style.cursor = 'grab'; });
    window.addEventListener('mousemove', e => {
      if (dragging && dragging.pan) { pan.x = e.clientX - dragging.sx; pan.y = e.clientY - dragging.sy; }
    });
    canvas.addEventListener('click', () => {
      if (hover && !moved && onPick) onPick(hover.t);
    });
    canvas.addEventListener('wheel', e => {
      e.preventDefault();
      const f = e.deltaY < 0 ? 1.12 : 0.89;
      zoom = Math.max(0.35, Math.min(3.2, zoom * f));
    }, { passive: false });

    return { highlight(t) { }, nodes };
  }

  /* ---------- 右栏迷你图谱 ---------- */
  const MiniGraph = (() => {
    let data = null, sim = null, nodes = [], links = [], raf = null;
    const canvas = () => $('#miniCanvas');

    function set(d) {
      data = d;
      const c = canvas(); if (!c || !d) return;
      nodes = d.nodes.map((n, i) => ({ ...n, id: i }));
      links = d.links;
      sim = makeSim(nodes, links, { w: c.width, h: c.height, center: 0, k: 26, linkDist: 58 });
      setCenterTitle(d.center);
      loop();
    }
    function setCenterTitle(t) {
      const hint = $('#miniHint');
      if (hint) hint.textContent = t ? '中心：' + t : '打开任意词条后显示其连接';
    }
    function loop() {
      cancelAnimationFrame(raf);
      let alpha = 1;
      const step = () => {
        const c = canvas();
        if (!c || !sim) return;
        const dpr = Math.min(2, window.devicePixelRatio || 1);
        const r = c.parentElement.getBoundingClientRect();
        if (c.width !== Math.round(r.width * dpr)) {
          c.width = Math.round(r.width * dpr); c.height = Math.round(r.height * dpr);
        }
        const ctx = c.getContext('2d');
        const W = c.width / dpr, H = c.height / dpr;
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        sim.step(alpha);
        if (alpha > 0.01) alpha *= 0.97;
        ctx.clearRect(0, 0, W, H);
        draw(ctx, sim, nodes, links, { w: W, h: H, center: 0 });
        raf = requestAnimationFrame(step);
      };
      step();
    }
    function redraw() { loop(); }
    function clickHandler(onPick) {
      const c = canvas(); if (!c) return;
      c.onclick = e => {
        const r = c.getBoundingClientRect();
        const x = e.clientX - r.left, y = e.clientY - r.top;
        for (const n of nodes) {
          if (n._x == null) continue;
          if ((x - n._x) ** 2 + (y - n._y) ** 2 < (n._r + 7) ** 2) { onPick(n.t); return; }
        }
      };
    }
    return { set, redraw, clickHandler, get data() { return data; } };
  })();

  return { mountFull, MiniGraph };
})();

window.Graph = Graph;
window.MiniGraph = Graph.MiniGraph;
