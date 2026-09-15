/* ============================================================
 *  scene.js — HD-2D 立体透视模型渲染器
 *  A layered pixel-art diorama: dithered gradients, parallax
 *  planes, additive bloom, god rays, animated water & fireflies.
 *  Pure canvas 2D, no dependencies.
 * ============================================================ */
window.HD2D = (function () {
  'use strict';

  /* ---------------- constants ---------------- */
  const W = 384, H = 216;      // internal pixel resolution
  const PW = 900;              // parallax plane width (world units)
  const CAM_MAX = 360;         // horizontal camera travel over the whole page

  /* ---- vertical plane plan (each plane overlaps the next: no seams) ----
   *   sky      y   0 .. 118   factor 0.00
   *   far      y  ..  .. 126  factor 0.42   mountains + forest line
   *   mid      y 112 .. 170   factor 0.72   meadow + lake + spire
   *   near     y 162 .. 216   factor 1.00   hero plateau + props
   *   fore     y 194 .. 216   factor 1.32   out-of-focus bank
   */
  const HORIZON = 112;
  const MID_BOT = 170;
  const LAKE_X0 = -70, LAKE_X1 = 186, LAKE_Y0 = 124, LAKE_Y1 = 162;
  const NEAR_TOP = 162;        // front plateau upper edge (wavy)
  const FOOT = 184;            // where the hero stands
  const CLIFF = 188;           // grass -> cliff face on the front plane
  const FORE_TOP = 194;        // blurred foreground bank

  const BAYER = [0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5];

  /* ---------------- palettes ---------------- */
  const DAY = {
    sky0: '#1b2c60', sky1: '#37498c', sky2: '#7a5c9c', sky3: '#c46e7c',
    sky4: '#ef9a58', sky5: '#ffdd93',
    sun: '#fffbe8', sunGlow: '#ffbe63', ray: '#ffcf8a',
    cloudDark: '#6a5a8e', cloudLit: '#f0a173', cloudRim: '#ffe0b0',
    mtnFar: '#5d5c97', mtnMid: '#414c80', mtnNear: '#2d3b65',
    star: '#ffffff', haze: '#f6c98a',
    waterDeep: '#1c3350', waterMid: '#2f607f', waterLit: '#93cbd9', foam: '#e6f8ff',
    grassDark: '#1e3a2d', grassMid: '#2f5a3b', grassLit: '#5f9152', grassHot: '#96c46a',
    cliffDark: '#28212f', cliffMid: '#44384a', cliffLit: '#6d5a60',
    pathDark: '#57483a', pathMid: '#8b7455', pathLit: '#b09572',
    pineDark: '#152e2b', pineMid: '#1f4639', pineLit: '#4b8a5c', pineHot: '#8cc26e',
    woodDark: '#382820', woodMid: '#6a4931', woodLit: '#9d7347',
    stoneDark: '#39383f', stoneMid: '#5c5a63', stoneLit: '#8e8b93',
    roofDark: '#5d2f3c', roofLit: '#a44b50', roofHot: '#d97a63',
    wallDark: '#7a5c42', wallLit: '#c79b69', wallHot: '#e8c08a',
    doorDark: '#33241c',
    winGlow: '#ffcf7a',
    crysCore: '#ecfffb', crysMid: '#6ff0e0', crysDark: '#2aa8be', crysEdge: '#186f8c',
    charDark: '#161c34', charMid: '#2c3a6b', charLit: '#5d76c4',
    charSkin: '#e6b189', charScarf: '#d8505a', charBoot: '#241a17',
    lantern: '#ffc978', lanternCore: '#fff4d0',
    fireCore: '#fff2b8', fireMid: '#ff9b3d', fireOut: '#e2542a',
    fgDark: '#0e181a', fgMid: '#182a26', fgLit: '#2b4738',
    vib: 'rgba(255,178,92,0.30)',
  };

  const NIGHT = {
    sky0: '#060a1c', sky1: '#0d1633', sky2: '#1a2749', sky3: '#283a68',
    sky4: '#3a5088', sky5: '#5d70a8',
    sun: '#eaf2ff', sunGlow: '#8fb0e8', ray: '#a8c4f0',
    cloudDark: '#212a52', cloudLit: '#46538e', cloudRim: '#93a6d8',
    mtnFar: '#242a52', mtnMid: '#1a2044', mtnNear: '#121736',
    star: '#ffffff', haze: '#7d94cc',
    waterDeep: '#061529', waterMid: '#102f47', waterLit: '#3f7c96', foam: '#a8dcea',
    grassDark: '#12261f', grassMid: '#1c3d2e', grassLit: '#2f6045', grassHot: '#4a8058',
    cliffDark: '#161320', cliffMid: '#262232', cliffLit: '#3d3546',
    pathDark: '#221b17', pathMid: '#3c3126', pathLit: '#584a38',
    pineDark: '#0c1f22', pineMid: '#14332c', pineLit: '#265247', pineHot: '#376d54',
    woodDark: '#181110', woodMid: '#2e2017', woodLit: '#493322',
    stoneDark: '#191920', stoneMid: '#2b2a32', stoneLit: '#454450',
    roofDark: '#29131b', roofLit: '#4a2126', roofHot: '#6b3430',
    wallDark: '#32251a', wallLit: '#56402c', wallHot: '#6d5238',
    doorDark: '#140f0c',
    winGlow: '#ffb347',
    crysCore: '#dcfffa', crysMid: '#54e6dc', crysDark: '#1c8aa0', crysEdge: '#0e5a72',
    charDark: '#080c1c', charMid: '#141c3a', charLit: '#31406f',
    charSkin: '#b98a68', charScarf: '#9c343d', charBoot: '#100c0b',
    lantern: '#ffb85c', lanternCore: '#ffe9a8',
    fireCore: '#ffe08a', fireMid: '#e8752a', fireOut: '#a83a1e',
    fgDark: '#070c10', fgMid: '#0d1917', fgLit: '#1a302a',
    vib: 'rgba(90,130,220,0.30)',
  };

  /* ---------------- colour helpers ---------------- */
  const _c = {};
  function rgb(hex) {
    let v = _c[hex];
    if (v) return v;
    if (hex[0] === '#') {
      v = [parseInt(hex.slice(1, 3), 16), parseInt(hex.slice(3, 5), 16), parseInt(hex.slice(5, 7), 16)];
    } else {
      const m = hex.match(/[\d.]+/g);
      v = [+m[0] | 0, +m[1] | 0, +m[2] | 0];
    }
    _c[hex] = v;
    return v;
  }
  function toHex(r, g, b) {
    return '#' + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
  }
  function mix(a, b, t) {
    const A = rgb(a), B = rgb(b);
    return toHex(
      Math.round(A[0] + (B[0] - A[0]) * t),
      Math.round(A[1] + (B[1] - A[1]) * t),
      Math.round(A[2] + (B[2] - A[2]) * t)
    );
  }
  function shade(hex, amt) { // amt>0 lighten, <0 darken
    const A = rgb(hex);
    const f = (v) => Math.max(0, Math.min(255, Math.round(amt > 0 ? v + (255 - v) * amt : v * (1 + amt))));
    return toHex(f(A[0]), f(A[1]), f(A[2]));
  }
  function buildPal(t) {
    const P = {};
    for (const k in DAY) {
      const a = DAY[k], b = NIGHT[k] || a;
      P[k] = (a[0] === '#' && b[0] === '#') ? mix(a, b, t) : (t < 0.5 ? a : b);
    }
    return P;
  }

  /* ---------------- sprite data ---------------- */
  /* legend: . transparent | K outline | M cloak-mid | L cloak-lit | S skin
   *         E eye-glow | R scarf | B boot                                */
  const CHAR = [
    '.....KKKK.....',
    '...KKMMMMKK...',
    '..KMMMMMMMMK..',
    '.KMMMMMMMMMMK.',
    '.KMMMMMMMMMMK.',
    '.KMMSSSSSSMMK.',
    '.KSSSSSSSSSSK.',
    '.KSEESSSSEESK.',
    '.KSSSSSSSSSSK.',
    '.KMSSSSSSSSMK.',
    '.KMMMMMMMMMMK.',
    'KKRRRRRRRRRRKK',
    'KRRRRRRRRRRRRK',
    '.KRMMMMMMMMRK.',
    '.KMMMMMMMMMMK.',
    '.KMMMLLLLMMMK.',
    '.KMMMLLLLMMMK.',
    'KMMMMMMMMMMMMK',
    'KMMMMMMMMMMMMK',
    '.KMMMMMMMMMMK.',
    '.KMMMMMMMMMMK.',
    '.KMMMKKKKMMMK.',
    '.KBBBK..KBBBK.',
    '.KKKKK..KKKKK.',
  ];
  const CHAR_PAL = { K: 'charDark', M: 'charMid', L: 'charLit', S: 'charSkin', E: 'crysCore', R: 'charScarf', B: 'charBoot' };

  /* scarf animation: extra pixels appended to rows 11-13 */
  const SCARF_FRAMES = [
    { 11: 'KRRRRRRRRRRRRRRK', 12: 'KRRRRRRRRRRRRRRK', 13: '.KRRMMMMMMMMRRK.' },
    { 11: 'KKRRRRRRRRRRRRKK', 12: 'KRRRRRRRRRRRRRRK', 13: '.KRRRMMMMMMMRRK.' },
    { 11: 'KKRRRRRRRRRRRKK.', 12: 'KRRRRRRRRRRRRRK.', 13: '..KRRRMMMMMRRK..' },
    { 11: 'KKRRRRRRRRRRKK..', 12: 'KRRRRRRRRRRRRK..', 13: '..KRRRMMMMRRK...' },
  ];

  /* legend: K outline | M mid | L lit | H hot-rim | W wood */
  const PINE = [
    '.......K.......',
    '......KMK......',
    '......KMK......',
    '.....KMMMK.....',
    '....KMMMMMK....',
    '....KMMMMMK....',
    '...KMMMMMMMK...',
    '..KMMMMMMMMMK..',
    '..KMMLLMMMMMK..',
    '.KMMMLLMMMMMMK.',
    '.KMMMMMMMMMMMK.',
    'KMMMMLLMMMMMMMK',
    'KMMMMMMMMMMMMMK',
    '..KMMMMMMMMMK..',
    '..KMMLLMMMMMK..',
    '.KMMMLLMMMMMMK.',
    '.KMMMMMMMMMMMK.',
    'KMMMLLMMMMMMMMK',
    'KMMMMMMMMMMMMMK',
    '...KMMMMMMMK...',
    '...KMMLLMMMK...',
    '..KMMMLLMMMMK..',
    '..KMMMMMMMMMK..',
    '......KWK......',
    '......KWK......',
    '.....KWWWK.....',
  ];
  const PINE_PAL = { K: 'pineDark', M: 'pineMid', L: 'pineLit', W: 'woodDark', H: 'pineHot' };

  /* legend: R roof | W wall | G window-glow | D door | S stone-foundation */
  const HUT = [
    '................KK................',
    '...............KRRK...............',
    '..............KRRRRK..............',
    '.............KRRRRRRK.............',
    '............KRRRRRRRRK............',
    '..........KRRRRRRRRRRRRK..........',
    '........KRRRRRRRRRRRRRRRRK........',
    '......KRRRRRRRRRRRRRRRRRRRRK......',
    '....KRRRRRRRRRRRRRRRRRRRRRRRRK....',
    '..KRRRRRRRRRRRRRRRRRRRRRRRRRRRRK..',
    'KKRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRKK',
    'KRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRK',
    'KRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRK',
    'KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK',
    '.KWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWK.',
    '.KWWWWWWKGGWWWWWWWWWWWWKGGWWWWWWK.',
    '.KWWWWWWKGGWWWWWWWWWWWWKGGWWWWWWK.',
    '.KWWWWWWKGGWWWWWWWWWWWWKGGWWWWWWK.',
    '.KWWWWWWKGGWWWWWWWWWWWWKGGWWWWWWK.',
    '.KWWWWWWKKKWWWWWWWWWWWWKKKWWWWWWK.',
    '.KWWWWWWWWWWWWKKKKKKWWWWWWWWWWWWK.',
    '.KWWWWWWWWWWWWKDDDDKWWWWWWWWWWWWK.',
    '.KWWWWWWWWWWWWKDDDDKWWWWWWWWWWWWK.',
    '.KWWWWWWWWWWWWKDDDDKWWWWWWWWWWWWK.',
    '.KWWWWWWWWWWWWKDDDDKWWWWWWWWWWWWK.',
    '.KWWWWWWWWWWWWKDDDDKWWWWWWWWWWWWK.',
    'KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK',
    'KSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSK',
    'KSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSK',
    'KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK',
  ];
  const HUT_PAL = { K: 'woodDark', R: 'roofDark', W: 'wallDark', G: 'winGlow', D: 'doorDark', S: 'stoneMid' };

  const SIGN = [
    '.KKKKKKKKKKK.',
    '.KWWWWWWWWWK.',
    '.KWWWWWWWWWK.',
    '.KKKKKKKKKKK.',
    '.....KWWK....',
    '.....KWWK....',
    '.....KWWK....',
    '.....KWWK....',
    '..KKKKKKKKK..',
    '..KWWWWWWWK..',
    '..KWWWWWWWK..',
    '..KKKKKKKKK..',
    '.....KWWK....',
    '.....KWWK....',
    '.....KWWK....',
    '.....KWWK....',
    '.....KWWK....',
    '.....KWWK....',
    '.....KWWK....',
    '.....KWWK....',
    '.....KWWK....',
    '....KKWWKK...',
  ];
  const SIGN_PAL = { K: 'woodDark', W: 'woodMid' };

  /* L lantern glass | Y glow */
  const LAMP = (function () {
    const r = [];
    r.push('....KKK....');
    r.push('...KWWWK...');
    r.push('..KKKKKKK..');
    for (let i = 0; i < 4; i++) r.push('..KYYYYYK..');
    r.push('..KKKKKKK..');
    r.push('...KWWWK...');
    for (let i = 0; i < 18; i++) r.push('....KWK....');
    r.push('...KWWWK...');
    r.push('..KKKKKKK..');
    return r;
  })();
  const LAMP_PAL = { K: 'woodDark', W: 'woodMid', Y: 'lantern' };

  const CAT = [
    '..K.....K..',
    '.KCK...KCK.',
    '.KCCKKKCCK.',
    '.KCCCCCCCK.',
    'KKCCCCCCCKK',
    'KCCCECCCCCK',
    'KCCCCCCCCCK',
    '.KCCCCCCCK.',
    '..KK...KK..',
  ];
  const CAT_PAL = { K: 'charDark', C: 'charMid', E: 'crysCore' };

  /* ---------------- drawing utils ---------------- */
  let G = null; // per-frame graphics state

  function blit(ctx, rows, pal, x, y) {
    for (let j = 0; j < rows.length; j++) {
      const row = rows[j];
      for (let i = 0; i < row.length; i++) {
        const ch = row[i];
        if (ch === '.') continue;
        const key = pal[ch];
        const col = key && G.P[key];
        if (!col) continue;
        ctx.fillStyle = col;
        ctx.fillRect((x + i) | 0, (y + j) | 0, 1, 1);
      }
    }
  }

  /* dithered vertical gradient straight into the pixel buffer */
  function ditherGrad(ctx, x, y, w, h, stops, opts) {
    opts = opts || {};
    const img = ctx.getImageData(x, y, w, h);
    const d = img.data;
    const cache = {};
    for (let j = 0; j < h; j++) {
      const t = (j + 0.5) / h;
      let a = stops[0][1], b = stops[0][1], lt = 0;
      if (t >= stops[stops.length - 1][0]) { a = b = stops[stops.length - 1][1]; lt = 0; }
      else if (t > stops[0][0]) {
        for (let i = 0; i < stops.length - 1; i++) {
          if (t >= stops[i][0] && t <= stops[i + 1][0]) {
            a = stops[i][1]; b = stops[i + 1][1];
            lt = (t - stops[i][0]) / ((stops[i + 1][0] - stops[i][0]) || 1);
            break;
          }
        }
      }
      const ca = rgb(a), cb = rgb(b);
      for (let i = 0; i < w; i++) {
        const th = BAYER[(i & 3) + ((j & 3) << 2)] / 16;
        const c = lt > th ? cb : ca;
        const o = (j * w + i) << 2;
        d[o] = c[0]; d[o + 1] = c[1]; d[o + 2] = c[2];
        if (!opts.alpha) d[o + 3] = 255;
      }
    }
    ctx.putImageData(img, x, y);
  }

  /* dithered horizontal edge between two flat fills */
  function ditherEdgeRow(ctx, x, y, w, cTop, cBot) {
    for (let i = 0; i < w; i++) {
      const th = BAYER[(i & 3) + ((y & 3) << 2)] / 16;
      ctx.fillStyle = th > 0.5 ? cBot : cTop;
      ctx.fillRect(x + i, y, 1, 1);
    }
  }

  function fill(ctx, c, x, y, w, h) {
    if (w <= 0 || h <= 0) return;
    ctx.fillStyle = c;
    ctx.fillRect(x | 0, y | 0, Math.ceil(w), Math.ceil(h));
  }

  /* deterministic pseudo-random */
  let seed = 1337;
  function rnd() { seed = (seed * 1664525 + 1013904223) & 0x7fffffff; return seed / 0x7fffffff; }
  function srand(s) { seed = s; }

  /* emissive queue (screen space, half-res glow buffer coords) */
  const glowQ = [];
  function emit(x, y, w, h, col, a) { glowQ.push([x, y, w, h, col, a === undefined ? 1 : a, 0]); }
  function emitDisc(cx, cy, r, col, a) { glowQ.push([cx, cy, r, 0, col, a === undefined ? 1 : a, 1]); }

  /* ---------------- layers ---------------- */
  function mkLayer(w, h, factor) {
    const c = document.createElement('canvas');
    c.width = w; c.height = h;
    const x = c.getContext('2d');
    x.imageSmoothingEnabled = false;
    return { c: c, x: x, factor: factor, dirty: true, w: w, h: h };
  }

  /* ================= SKY ================= */
  function paintSky(L) {
    const x = L.x, P = G.P;
    x.clearRect(0, 0, W, H);
    ditherGrad(x, 0, 0, W, HORIZON + 4, [
      [0.00, P.sky0], [0.24, P.sky1], [0.50, P.sky2],
      [0.72, P.sky3], [0.87, P.sky4], [1.00, P.sky5],
    ]);
    /* stars — only meaningful at night, faded in by nightT */
    if (nightT > 0.02) {
      srand(9001);
      for (let i = 0; i < 170; i++) {
        const sx = (rnd() * W) | 0, sy = (rnd() * (HORIZON - 24)) | 0;
        const s = rnd();
        x.fillStyle = mix(P.sky1, P.star, 0.6);
        x.globalAlpha = (0.30 + s * 0.70) * nightT;
        x.fillRect(sx, sy, 1, 1);
        if (s > 0.93) {
          x.globalAlpha *= 0.55;
          x.fillRect(sx - 1, sy, 1, 1); x.fillRect(sx + 1, sy, 1, 1);
          x.fillRect(sx, sy - 1, 1, 1); x.fillRect(sx, sy + 1, 1, 1);
        }
      }
      x.globalAlpha = 1;
    }
    /* horizon haze band */
    for (let j = 0; j < 10; j++) {
      const t = j / 10;
      x.globalAlpha = 0.10 * (1 - t);
      fill(x, P.haze, 0, HORIZON - 10 + j, W, 1);
    }
    x.globalAlpha = 1;
  }

  /* ================= FAR (mountains) ================= */
  function ridge(x, baseY, amp, freq, phase, colTop, colBody, lit, lay) {
    for (let i = 0; i < PW; i++) {
      const u = i * freq + phase;
      const hgt = amp * (0.55 + 0.45 * Math.sin(u)) * (0.6 + 0.4 * Math.sin(u * 2.7 + 1.3))
        + amp * 0.30 * Math.sin(u * 5.1 + 0.7);
      const top = Math.round(baseY - hgt);
      fill(x, colBody, i, top, 1, HORIZON + 6 - top);
      /* sun-facing rim: slope descending to the right gets lit */
      const u2 = (i + 3) * freq + phase;
      const hgt2 = amp * (0.55 + 0.45 * Math.sin(u2)) * (0.6 + 0.4 * Math.sin(u2 * 2.7 + 1.3))
        + amp * 0.30 * Math.sin(u2 * 5.1 + 0.7);
      const top2 = Math.round(baseY - hgt2);
      if (top < top2 && lay) { fill(x, colTop, i, top, 1, Math.min(2, top2 - top)); }
    }
  }

  function paintFar(L) {
    const x = L.x, P = G.P;
    x.clearRect(0, 0, PW, H);
    ridge(x, HORIZON + 2, 40, 0.0055, 0.0, shade(P.mtnFar, 0.14), P.mtnFar, true, true);
    ridge(x, HORIZON + 4, 27, 0.0090, 2.1, shade(P.mtnMid, 0.14), P.mtnMid, true, true);
    /* distant forest silhouette along the horizon */
    srand(4242);
    for (let i = 0; i < PW; i += 1) {
      const n = Math.sin(i * 0.06) * 0.5 + Math.sin(i * 0.017) * 0.5;
      const hgt = 5 + n * 3 + rnd() * 2.2;
      const top = HORIZON + 4 - Math.round(hgt);
      fill(x, P.mtnNear, i, top, 1, HORIZON + 6 - top);
    }
    ditherEdgeRow(x, 0, HORIZON + 5, PW, P.mtnNear, shade(P.mtnNear, -0.18));
    fill(x, shade(P.mtnNear, -0.22), 0, HORIZON + 6, PW, 8);
    /* distant ridges wash out toward the sky */
    hazeRegion(x, 24, 6, PW, HORIZON - 18, mix(P.sky4, P.sky5, 0.45), 0.05, 0.36);
  }

  /* Atmospheric perspective: blend a region toward the haze colour.
   * Alpha ramps vertically, so distant planes recede instead of
   * sitting there as flat saturated cut-outs. */
  function hazeRegion(ctx, x, y, w, h, col, aTop, aBot) {
    if (w <= 0 || h <= 0) return;
    const img = ctx.getImageData(x, y, w, h);
    const d = img.data, C = rgb(col);
    const denom = (h - 1) || 1;
    for (let j = 0; j < h; j++) {
      const a = aTop + (aBot - aTop) * (j / denom);
      if (a <= 0) continue;
      for (let i = 0; i < w; i++) {
        const o = (j * w + i) << 2;
        if (d[o + 3] === 0) continue;
        d[o] += (C[0] - d[o]) * a;
        d[o + 1] += (C[1] - d[o + 1]) * a;
        d[o + 2] += (C[2] - d[o + 2]) * a;
      }
    }
    ctx.putImageData(img, x, y);
  }

  const lakeFar = {}, lakeNear = {};
  function lakeShape(wx) {
    const u = Math.min(1, Math.max(0, (wx - LAKE_X0) / ((LAKE_X1 - LAKE_X0) || 1)));
    const taper = Math.sin(Math.pow(u, 0.72) * Math.PI);
    return {
      far: LAKE_Y0 + (1 - taper) * 8 + Math.sin(wx * 0.045) * 1.6 + Math.sin(wx * 0.013) * 2.2,
      near: LAKE_Y1 - (1 - taper) * 13 + Math.sin(wx * 0.028 + 2.1) * 2.2 + Math.sin(wx * 0.0091) * 2.6,
    };
  }
  function buildLakeShape() {
    for (let wx = Math.floor(LAKE_X0) - 2; wx <= Math.ceil(LAKE_X1) + 2; wx++) {
      const e = lakeShape(wx);
      lakeFar[wx] = e.far; lakeNear[wx] = e.near;
    }
  }

  /* The lake, shaped rather than rectangular: the shoreline tapers at
   * both ends and ripples, drawn in a single pass so it stays cheap. */
  function paintLake(ctx, P) {
    const x0 = Math.floor(LAKE_X0), x1 = Math.ceil(LAKE_X1) + 2;
    const y0 = LAKE_Y0 - 14, y1 = LAKE_Y1 + 8;
    const w = x1 - x0, h = y1 - y0;
    const img = ctx.getImageData(x0, y0, w, h);
    const d = img.data;
    const cFar = rgb(mix(P.waterMid, P.sky5, 0.48));
    const cMid = rgb(P.waterMid);
    const cNear = rgb(P.waterDeep);

    for (let i = 0; i < w; i++) {
      const wx = x0 + i;
      const sh = lakeShape(wx);
      const farY = sh.far, nearY = sh.near;
      const depth = Math.max(1, nearY - farY);
      for (let j = 0; j < h; j++) {
        const wy = y0 + j;
        if (wy < farY || wy > nearY) continue;
        const t = (wy - farY) / depth;
        let A, B, lt;
        if (t < 0.26) { A = cFar; B = cMid; lt = t / 0.26; }
        else if (t < 0.62) { A = cMid; B = cMid; lt = 0; }
        else { A = cMid; B = cNear; lt = (t - 0.62) / 0.38; }
        const th = BAYER[(i & 3) + ((j & 3) << 2)] / 16;
        const c = lt > th ? B : A;
        const o = (j * w + i) << 2;
        d[o] = c[0]; d[o + 1] = c[1]; d[o + 2] = c[2]; d[o + 3] = 255;
      }
    }
    ctx.putImageData(img, x0, y0);
  }

  /* ================= MID (lake, meadow, spire) ================= */
  function paintMid(L) {
    const x = L.x, P = G.P;
    x.clearRect(0, 0, PW, H);

    /* ---- meadow fills the whole plane top-to-bottom: no gaps ---- */
    ditherGrad(x, 0, HORIZON + 2, PW, MID_BOT - HORIZON - 2, [
      [0.00, mix(P.grassMid, P.grassLit, 0.45)],
      [0.22, P.grassMid],
      [0.62, P.grassDark],
      [1.00, shade(P.grassDark, -0.22)],
    ]);
    /* field patchwork so the middle distance is not one flat sheet */
    srand(1717);
    for (let i = 0; i < PW; i += 1) {
      const band = Math.floor(i / 58);
      const tone = ((band * 37) % 5) / 5;
      const py = HORIZON + 4 + tone * 5;
      fill(x, shade(P.grassMid, -0.04 - tone * 0.06), i, py, 1, MID_BOT - py);
      /* hedgerow between fields, only in the far strip */
      if (i % 58 === 0) {
        const hh = 3 + ((band * 7) % 4);
        fill(x, shade(P.grassDark, -0.10), i, py - hh, 1, hh + 5);
        fill(x, shade(P.grassDark, -0.04), i + 1, py - hh + 1, 1, hh + 3);
      }
    }
    /* warm sunlit rim right at the far treeline */
    for (let i = 0; i < PW; i += 1) {
      if ((i * 5) % 11 < 4) fill(x, mix(P.grassMid, P.grassHot, 0.45), i, HORIZON + 2 + ((i * 3) % 2), 1, 1);
    }

    /* ---- lake ---- */
    paintLake(x, P);

    /* far shore: reeds + a dark bank line */
    srand(77);
    for (let i = LAKE_X0; i < LAKE_X1; i++) {
      const fy = lakeFar[i];
      if (fy === undefined) continue;
      if (rnd() > 0.55) {
        const hh = 2 + ((rnd() * 5) | 0);
        fill(x, P.pineDark, i, fy - hh, 1, hh);
      }
      if ((i * 5) % 7 < 3) fill(x, shade(P.pineDark, 0.16), i, fy - 1, 1, 1);
    }
    /* near shore: sand lip + foam */
    for (let i = LAKE_X0; i < LAKE_X1; i++) {
      const ny = lakeNear[i];
      if (ny === undefined) continue;
      const th = BAYER[(i & 3) + ((LAKE_Y1 & 3) << 2)] / 16;
      fill(x, th > 0.4 ? P.pathDark : P.grassDark, i, ny, 1, 1);
      fill(x, (i * 5) % 9 < 4 ? P.pathDark : P.grassDark, i, ny + 1, 1, 2);
      if ((i * 7) % 11 < 3) fill(x, P.foam, i, ny - 1, 1, 1);
    }

    /* ---- wooden dock reaching into the water ---- */
    const DX = 96, DYB = LAKE_Y1 - 4;
    fill(x, P.woodDark, DX, DYB - 18, 4, 24);
    fill(x, P.woodDark, DX + 20, DYB - 18, 4, 24);
    fill(x, P.woodMid, DX - 5, DYB - 21, 34, 4);
    fill(x, P.woodLit, DX - 5, DYB - 21, 34, 1);
    for (let i = 0; i < 6; i++) fill(x, P.woodDark, DX - 3 + i * 6, DYB - 20, 1, 3);

    /* ---- crystal spire (the landmark you scroll toward) ---- */
    rock(x, 470, 160, 78, 26, P);
    spire(x, 470, 160, 92, P);

    /* ---- far treeline behind the lake ---- */
    const back = [8, 44, 74, 130, 200, 232];
    for (let i = 0; i < back.length; i++) scaledSprite(x, PINE, PINE_PAL, back[i], LAKE_Y0 + 4, 0.62);

    /* ---- meadow pines beside / behind the front plateau ---- */
    const mids = [206, 244, 292, 330, 372, 404, 512, 548, 590, 640, 688, 726, 764, 806, 846];
    for (let i = 0; i < mids.length; i++) {
      const px = mids[i];
      const s = (i % 3 === 0) ? 0.86 : 1.0;
      const by = LAKE_Y1 - 4 + ((i * 5) % 4);
      scaledSprite(x, PINE, PINE_PAL, px, by, s);
    }
    /* cottages + hedgerows on the far bank for scale */
    scaledSprite(x, HUT, HUT_PAL, 424, LAKE_Y1 - 2, 0.58);
    scaledSprite(x, HUT, HUT_PAL, 618, LAKE_Y1 - 1, 0.50);
    srand(313);
    for (let i = 0; i < PW; i += 5) {
      if (rnd() > 0.88) {
        const bx = i, by = LAKE_Y1 - 8 - rnd() * 14;
        fill(x, P.grassMid, bx, by, 5, 3);
        fill(x, P.grassLit, bx + 1, by - 1, 3, 2);
      }
    }

    /* ---- atmospheric perspective: the whole back plane recedes ---- */
    hazeRegion(x, 0, HORIZON + 2, PW, 48, mix(P.sky5, P.haze, 0.45), 0.26, 0.0);
  }

  /* Sprites are pre-baked to tiny canvases so the browser can do exact
   * nearest-neighbour scaling for depth — no hand-rolled gaps. */
  let _sid = 0, palGen = 0;
  const spriteCache = {};
  function spriteCanvas(rows, pal) {
    if (!rows.__sid) rows.__sid = ++_sid;
    const key = rows.__sid + ':' + palGen;
    let c = spriteCache[key];
    if (c) return c;
    c = document.createElement('canvas');
    c.width = rows[0].length; c.height = rows.length;
    const cx = c.getContext('2d');
    cx.imageSmoothingEnabled = false;
    blit(cx, rows, pal, 0, 0);
    spriteCache[key] = c;
    return c;
  }

  function scaledSprite(ctx, rows, pal, cx, baseY, s) {
    const w = rows[0].length, h = rows.length;
    const dw = Math.max(1, Math.round(w * s)), dh = Math.max(1, Math.round(h * s));
    ctx.imageSmoothingEnabled = false;
    ctx.drawImage(spriteCanvas(rows, pal), Math.round(cx - dw / 2), Math.round(baseY - dh), dw, dh);
  }

  function rock(ctx, cx, baseY, w, h, P) {
    srand(cx | 0);
    for (let j = 0; j < h; j++) {
      const t = j / h;
      const hw = w * 0.5 * Math.sqrt(Math.max(0, 1 - t * t * 0.82)) * (0.9 + rnd() * 0.14);
      const px = cx - hw;
      fill(ctx, P.stoneDark, px, baseY - h + j, hw * 2, 1);
      fill(ctx, P.stoneMid, px + hw * 0.30, baseY - h + j, hw * 1.28, 1);
      if (j < 3) fill(ctx, P.stoneLit, px + hw * 0.38, baseY - h + j, hw * 0.9, 1);
    }
    /* grass cap */
    for (let i = -w / 2; i < w / 2; i++) {
      if (rnd() > 0.55) fill(ctx, P.grassMid, cx + i, baseY - h - 1, 1, 2);
    }
  }

  function spire(ctx, cx, baseY, hgt, P) {
    const top = baseY - hgt;
    for (let j = 0; j < hgt; j++) {
      const t = j / hgt;
      const y = top + j;
      /* faceted taper: wide at base, needle at top */
      const hw = 4 + 15 * Math.pow(t, 1.7);
      const x0 = cx - hw;
      fill(ctx, P.crysEdge, x0, y, hw * 2, 1);
      fill(ctx, P.crysDark, x0 + 1, y, hw * 2 - 2, 1);
      fill(ctx, P.crysMid, x0 + hw * 0.42, y, hw * 0.95, 1);
      /* core highlight only in the upper half */
      if (t < 0.62) fill(ctx, P.crysCore, x0 + hw * 0.62, y, hw * 0.55, 1);
      /* facet ridge lines */
      if ((j % 17) === 3) fill(ctx, P.crysCore, x0 + 1, y, hw * 0.7, 1);
    }
    /* base crystal shards */
    srand(31);
    for (let i = 0; i < 5; i++) {
      const ox = (rnd() - 0.5) * 40, hh = 6 + rnd() * 10;
      const ww = 3 + rnd() * 4;
      for (let j = 0; j < hh; j++) {
        const t = j / hh, hw = ww * (1 - t * 0.8);
        fill(ctx, P.crysDark, cx + ox - hw, baseY - j, hw * 2, 1);
        fill(ctx, P.crysCore, cx + ox - hw * 0.35, baseY - j, hw * 0.7, 1);
      }
    }
  }

  /* ================= NEAR (main plateau + props) ================= */
  /* wavy silhouette line for the front plane's upper edge */
  function topEdge(i) {
    return Math.round(NEAR_TOP + Math.sin(i * 0.021) * 3 + Math.sin(i * 0.0071) * 2.4 + Math.sin(i * 0.11) * 0.8);
  }

  function paintNear(L) {
    const x = L.x, P = G.P;
    x.clearRect(0, 0, PW, H);

    /* ---- grass body from the wavy edge down to the cliff line ---- */
    ditherGrad(x, 0, NEAR_TOP - 4, PW, CLIFF - NEAR_TOP + 4, [
      [0.00, mix(P.grassMid, P.grassLit, 0.30)],
      [0.34, P.grassMid],
      [0.74, P.grassDark],
      [1.00, shade(P.grassDark, -0.16)],
    ]);
    /* punch the wavy silhouette out of the top */
    for (let i = 0; i < PW; i++) {
      const te = topEdge(i);
      x.clearRect(i, NEAR_TOP - 6, 1, te - (NEAR_TOP - 6));
      /* bright sunlit rim on the lip of the bank */
      fill(x, mix(P.grassLit, P.grassHot, 0.55), i, te, 1, 1);
      if ((i * 5) % 7 < 4) fill(x, P.grassLit, i, te + 1, 1, 1);
    }

    /* ---- cliff face at the front ---- */
    for (let i = 0; i < PW; i++) {
      const ce = CLIFF + Math.round(Math.sin(i * 0.03 + 1.1) * 1.6);
      ditherGrad(x, i, ce, 1, H - ce, [
        [0.00, shade(P.grassDark, -0.3)],
        [0.18, P.cliffMid],
        [1.00, P.cliffDark],
      ]);
      fill(x, shade(P.grassDark, -0.24), i, ce, 1, 1);
    }
    srand(555);
    for (let j = CLIFF + 6; j < H; j += 5) {
      for (let i = 0; i < PW; i += 3) {
        if (rnd() > 0.45) fill(x, shade(P.cliffDark, rnd() * 0.20), i, j + ((i * 5) % 5), 1 + ((rnd() * 3) | 0), 1);
      }
    }
    /* a few roots hanging over the cliff lip */
    srand(616);
    for (let i = 0; i < PW; i += 13) {
      if (rnd() > 0.55) {
        const ce = CLIFF + Math.round(Math.sin(i * 0.03 + 1.1) * 1.6);
        const h = 2 + ((rnd() * 5) | 0);
        fill(x, P.grassMid, i, ce, 1, h);
        fill(x, P.grassDark, i + 1, ce, 1, h - 1);
      }
    }

    /* ---- dirt path winding along the plateau ---- */
    for (let i = 0; i < PW; i++) {
      const py = FOOT - 9 + Math.sin(i * 0.0091) * 5 + Math.sin(i * 0.028) * 2.2;
      for (let kk = 0; kk < 8; kk++) {
        const t = kk / 8;
        const th = BAYER[((i + kk) & 3) + ((kk & 3) << 2)] / 16;
        const edge = t < 0.25 || t > 0.75;
        const c = edge ? (th > 0.40 ? P.grassDark : P.pathMid) : (th > 0.72 ? P.pathLit : P.pathMid);
        fill(x, c, i, py + kk - 12, 1, 1);
      }
    }

    /* ---- grass tufts ---- */
    srand(2002);
    for (let i = 0; i < PW; i += 2) {
      if (rnd() > 0.40) {
        const te = topEdge(i);
        const gy = te + 3 + rnd() * (CLIFF - te - 5);
        const hh = 1 + ((rnd() * 3) | 0);
        const far = gy < FOOT - 12;
        fill(x, rnd() > 0.70 ? (far ? P.grassMid : P.grassLit) : (far ? P.grassDark : P.grassMid), i, gy, 1, hh);
      }
    }

    /* ---- stone stairway climbing toward the spire ---- */
    for (let s = 0; s < 14; s++) {
      const sy = FOOT - 2 - s * 1.15, sx = 414 + s * 3.1;
      fill(x, P.stoneDark, sx, sy, 25, 5);
      fill(x, P.stoneMid, sx, sy, 25, 3);
      fill(x, P.stoneLit, sx, sy, 25, 1);
      for (let i = 0; i < 25; i += 5) fill(x, P.stoneDark, sx + i, sy, 1, 5);
    }
    for (let s = 0; s < 9; s++) {
      const sx = 496 + s * 1.5, sy = FOOT - 18 - s * 0.7;
      fill(x, P.stoneDark, sx, sy, 18, 3);
      fill(x, P.stoneMid, sx, sy, 18, 1);
    }

    /* ---- fence ---- */
    for (let i = 0; i < 9; i++) {
      const fx = 330 + i * 15;
      const fy = FOOT - 6 + Math.sin(fx * 0.0091) * 5;
      fill(x, P.woodDark, fx, fy - 15, 2, 15);
      fill(x, P.woodMid, fx, fy - 15, 1, 14);
      if (i < 8) {
        fill(x, P.woodDark, fx + 2, fy - 12, 13, 2);
        fill(x, P.woodMid, fx + 2, fy - 12, 13, 1);
        fill(x, P.woodDark, fx + 2, fy - 7, 13, 2);
      }
    }

    /* ---- rocks ---- */
    rock(x, 600, FOOT + 4, 48, 21, P);
    rock(x, 664, FOOT + 8, 36, 16, P);
    rock(x, 108, FOOT + 6, 34, 15, P);
    rock(x, 764, FOOT + 3, 30, 14, P);

    /* ---- pines on the front plane ---- */
    const nearPines = [[362, 1.10], [392, 0.88], [452, 0.95], [556, 1.22], [704, 1.10], [740, 1.34], [790, 0.98], [830, 1.15], [58, 0.82]];
    for (let i = 0; i < nearPines.length; i++) {
      const p = nearPines[i];
      scaledSprite(x, PINE, PINE_PAL, p[0], FOOT + 4 + ((i * 3) % 4), p[1]);
    }

    /* ---- the hero's cottage ---- */
    scaledSprite(x, HUT, HUT_PAL, 276, FOOT + 2, 1.0);
    /* ---- signpost ---- */
    blit(x, SIGN, SIGN_PAL, 214, FOOT - 22);
    /* ---- lantern post ---- */
    blit(x, LAMP, LAMP_PAL, 167, FOOT - 30);
    /* ---- campfire ring (flames are drawn dynamically) ---- */
    srand(88);
    for (let i = 0; i < 9; i++) {
      const a = (i / 9) * Math.PI * 2;
      fill(x, i % 2 ? P.stoneMid : P.stoneDark, 322 + Math.cos(a) * 8, FOOT + 1 + Math.sin(a) * 2.2, 3, 2);
    }
    fill(x, P.woodDark, 318, FOOT - 2, 9, 3);
    fill(x, P.woodMid, 320, FOOT - 3, 5, 2);

    /* ---- a stone waypoint shrine near the spire steps ---- */
    fill(x, P.stoneDark, 606, FOOT - 14, 10, 14);
    fill(x, P.stoneMid, 607, FOOT - 14, 8, 13);
    fill(x, P.stoneLit, 607, FOOT - 14, 8, 1);
    fill(x, P.stoneMid, 603, FOOT - 18, 16, 4);
    fill(x, P.stoneLit, 603, FOOT - 18, 16, 1);

    /* ---- flowers ---- */
    srand(31);
    for (let i = 0; i < PW; i += 3) {
      if (rnd() > 0.955) {
        const gy = topEdge(i) + 6 + rnd() * (CLIFF - topEdge(i) - 16);
        fill(x, P.grassMid, i, gy, 1, 3);
        fill(x, rnd() > 0.5 ? P.charScarf : P.lanternCore, i, gy - 1, 1, 1);
      }
    }

    /* the front plane is close, so only its far lip catches any haze */
    hazeRegion(x, 0, NEAR_TOP - 4, PW, 24, mix(P.sky5, P.haze, 0.45), 0.13, 0.0);
  }

  /* ================= FOREGROUND ================= */
  function paintFore(L) {
    const x = L.x, P = G.P;
    x.clearRect(0, 0, PW, H);
    /* dark out-of-focus bank */
    ditherGrad(x, 0, FORE_TOP, PW, H - FORE_TOP, [[0, P.fgMid], [0.40, P.fgDark], [1, P.fgDark]]);
    for (let i = 0; i < PW; i++) {
      const top = FORE_TOP + Math.round(Math.sin(i * 0.09) * 2 + Math.sin(i * 0.023) * 3);
      fill(x, P.fgMid, i, top, 1, H - top);
    }
    /* bushes */
    srand(1234);
    for (let b = 0; b < 22; b++) {
      const bx = rnd() * PW, by = H - 26 + rnd() * 8, br = 10 + rnd() * 16;
      for (let i = -br; i <= br; i++) {
        for (let j = -br * 0.62; j <= br * 0.5; j++) {
          if (i * i + (j * 2) * (j * 2) < br * br) {
            const c = j < -br * 0.2 ? P.fgLit : (j < br * 0.1 ? P.fgMid : P.fgDark);
            fill(x, c, bx + i, by + j, 1, 1);
          }
        }
      }
    }
    /* tall blade silhouettes */
    srand(4321);
    for (let i = 0; i < PW; i += 4) {
      const hh = 8 + rnd() * 22, lean = (rnd() - 0.5) * 8;
      for (let j = 0; j < hh; j++) {
        const t = j / hh;
        fill(x, t > 0.8 ? P.fgMid : P.fgDark, i + lean * t * t, H - j, 1, 1);
      }
    }
  }

  /* ================= DYNAMIC ELEMENTS ================= */
  /* particles */
  let flies = [], motes = [], leaves = [];

  function initParticles() {
    srand(20240);
    flies = [];
    for (let i = 0; i < 34; i++) {
      flies.push({
        x: rnd() * PW, y: HORIZON + 30 + rnd() * 70,
        r: 6 + rnd() * 22, sp: 0.12 + rnd() * 0.5, ph: rnd() * 6.28,
        br: 0.4 + rnd() * 0.6,
      });
    }
    motes = [];
    for (let i = 0; i < 60; i++) {
      motes.push({ x: rnd() * PW, y: rnd() * H, sp: 1.5 + rnd() * 5, ph: rnd() * 6.28, a: 0.12 + rnd() * 0.3 });
    }
    leaves = [];
    for (let i = 0; i < 16; i++) {
      leaves.push({ x: rnd() * PW, y: rnd() * H, sp: 6 + rnd() * 12, ph: rnd() * 6.28, sw: 6 + rnd() * 14, a: 0.5 + rnd() * 0.5 });
    }
  }

  function fireflies(t) {
    for (let i = 0; i < flies.length; i++) {
      const f = flies[i];
      const wx = f.x + Math.sin(t * f.sp + f.ph) * f.r;
      const wy = f.y + Math.cos(t * f.sp * 0.7 + f.ph * 1.3) * f.r * 0.5;
      const x = wx - G.camX * 0.72;
      if (x < -4 || x > W + 4) continue;
      const pulse = 0.35 + 0.65 * Math.pow(0.5 + 0.5 * Math.sin(t * 2.1 * f.sp + f.ph * 3), 2);
      const a = pulse * f.br;
      fill(G.dyn, mix(G.P.fireOut, G.P.lanternCore, pulse), x, wy, 1, 1);
      if (pulse > 0.62) {
        G.dyn.globalAlpha = a * 0.5;
        fill(G.dyn, G.P.lantern, x - 1, wy, 1, 1);
        fill(G.dyn, G.P.lantern, x + 1, wy, 1, 1);
        fill(G.dyn, G.P.lantern, x, wy - 1, 1, 1);
        fill(G.dyn, G.P.lantern, x, wy + 1, 1, 1);
        G.dyn.globalAlpha = 1;
      }
      emitDisc(x + 0.5, wy + 0.5, 1.8, G.P.lanternCore, a * 0.95);
      emitDisc(x + 0.5, wy + 0.5, 4.5, G.P.lantern, a * 0.30);
    }
  }

  function dustMotes(t) {
    if (!moteOn) return;
    for (let i = 0; i < motes.length; i++) {
      const m = motes[i];
      const x = ((m.x + t * m.sp * 0.6) % PW) - G.camX * 0.9;
      const y = m.y + Math.sin(t * 0.6 + m.ph) * 5;
      if (x < 0 || x > W) continue;
      G.dyn.globalAlpha = m.a * (0.5 + 0.5 * Math.sin(t * 1.4 + m.ph));
      fill(G.dyn, G.P.lanternCore, x, y, 1, 1);
      G.dyn.globalAlpha = 1;
    }
  }

  function fallingLeaves(t) {
    for (let i = 0; i < leaves.length; i++) {
      const l = leaves[i];
      const prog = (t * l.sp + l.ph * 30) % 260;
      const x = ((l.x + Math.sin(t * 0.8 + l.ph) * l.sw) % PW) - G.camX * 1.0;
      const y = prog - 20;
      if (x < 0 || x > W || y < -4 || y > H) continue;
      G.dyn.globalAlpha = l.a * 0.55;
      const wi = Math.sin(t * 3 + l.ph) > 0 ? 0 : 1;
      fill(G.dyn, mix(G.P.grassLit, G.P.roofHot, 0.4), x, y, 1 + wi, 1);
      G.dyn.globalAlpha = 1;
    }
  }

  /* sun / moon */
  function sunMoon(t, nightT) {
    const x = G.dyn, P = G.P;
    const sx = 300 - G.camX * 0.15 + G.mx * 0.25;
    const sy = 66 + Math.sin(t * 0.08) * 1.5 + G.my * 0.2;
    const R = 13;
    /* glow halo, dithered rings */
    const halo = Math.round(9 - nightT * 4);
    for (let r = R + halo; r >= R; r--) {
      const a = 0.06 * (1 - (r - R) / (halo + 1));
      x.globalAlpha = a * 1.6;
      x.fillStyle = P.sunGlow;
      x.beginPath();
      x.arc(sx, sy, r, 0, 6.2832);
      x.fill();
    }
    x.globalAlpha = 1;
    /* disc with dithered limb */
    for (let j = -R; j <= R; j++) {
      const hw = Math.sqrt(Math.max(0, R * R - j * j));
      fill(x, P.sun, sx - hw, sy + j, hw * 2, 1);
      if (hw > 2.5) {
        x.globalAlpha = 0.38;
        fill(x, P.sunGlow, sx - hw - 1, sy + j, 1, 1);
        fill(x, P.sunGlow, sx + hw, sy + j, 1, 1);
        x.globalAlpha = 1;
      }
    }
    if (nightT > 0.35) { /* moon crater/crescent shading */
      x.globalAlpha = (nightT - 0.35) * 0.55;
      for (let j = -R; j <= R; j += 1) {
        const hw = Math.sqrt(Math.max(0, R * R - j * j));
        if (hw > 2) fill(x, shade(P.sky1, -0.1), sx + hw * 0.28, sy + j, hw * 0.72, 1);
      }
      x.globalAlpha = 1;
    }
    /* the sun blazes at golden hour; the moon stays a quiet lamp */
    const soft = 1 - nightT * 0.62;
    emitDisc(sx, sy, R * (1.1 - nightT * 0.35), P.sun, 0.72 * soft);
    emitDisc(sx, sy, R * (2.1 - nightT * 0.9), P.sunGlow, 0.30 * soft);
    emitDisc(sx, sy, R * (3.6 - nightT * 1.9), P.sunGlow, 0.11 * soft);
    return { x: sx, y: sy, r: R };
  }

  /* Light shafts live in the bloom buffer: the blur pass turns hard
   * triangles into soft volumetric rays instead of stripey bands. */
  function godRays(t, sun, nightT) {
    const g = glowX, P = G.P;
    const n = rayN, HX = sun.x * 0.5, HY = sun.y * 0.5;
    if (!n) return;
    const segs = [[0, 55, 0.80], [55, 120, 0.34], [120, 190, 0.12]];
    g.save();
    g.globalCompositeOperation = 'lighter';
    g.fillStyle = P.ray;
    for (let i = 0; i < n; i++) {
      const a0 = (i / n) * 6.2832 + Math.sin(t * 0.07 + i) * 0.05;
      const spread = 0.028 + (i % 4) * 0.011;
      const flick = 0.45 + 0.55 * Math.sin(t * 0.31 + i * 1.7) * 0.5 + 0.275;
      const base = flick * (1 - nightT * 0.62) * 0.30;
      for (let sgi = 0; sgi < segs.length; sgi++) {
        const s0 = segs[sgi][0] * 0.5, s1 = segs[sgi][1] * 0.5;
        g.globalAlpha = Math.min(1, segs[sgi][2] * base);
        g.beginPath();
        g.moveTo(HX + Math.cos(a0 - spread) * s0, HY + Math.sin(a0 - spread) * s0);
        g.lineTo(HX + Math.cos(a0 - spread) * s1, HY + Math.sin(a0 - spread) * s1);
        g.lineTo(HX + Math.cos(a0 + spread) * s1, HY + Math.sin(a0 + spread) * s1);
        g.lineTo(HX + Math.cos(a0 + spread) * s0, HY + Math.sin(a0 + spread) * s0);
        g.closePath();
        g.fill();
      }
    }
    g.restore();
    g.globalAlpha = 1;
  }

  function clouds(t) {
    const x = G.dyn, P = G.P;
    srand(606);
    for (let c = 0; c < 7; c++) {
      const baseX = rnd() * 620, baseY = 20 + rnd() * 62, sc = 0.7 + rnd() * 0.9;
      const speed = 1.6 + rnd() * 3.0;
      const cx = ((baseX + t * speed) % 700) - 120 - G.camX * 0.13 + G.mx * 0.4;
      if (cx < -90 || cx > W + 90) continue;
      const blobs = [];
      const nb = 5 + ((c * 3) % 4);
      for (let b = 0; b < nb; b++) {
        blobs.push({
          dx: (b - nb / 2) * 9 * sc + (rnd() - 0.5) * 4,
          dy: (rnd() - 0.5) * 5 * sc,
          r: (7 + rnd() * 6) * sc,
        });
      }
      /* shadow body */
      for (let b = 0; b < blobs.length; b++) {
        const o = blobs[b];
        for (let j = -o.r; j <= o.r; j++) {
          const hw = Math.sqrt(Math.max(0, o.r * o.r - j * j));
          fill(x, P.cloudDark, cx + o.dx - hw, baseY + o.dy + j, hw * 2, 1);
        }
      }
      /* lit top */
      for (let b = 0; b < blobs.length; b++) {
        const o = blobs[b];
        for (let j = -o.r; j <= -o.r * 0.15; j++) {
          const hw = Math.sqrt(Math.max(0, o.r * o.r - j * j)) * 0.86;
          fill(x, P.cloudLit, cx + o.dx - hw + o.r * 0.2, baseY + o.dy + j, hw * 1.7, 1);
        }
        for (let j = -o.r; j <= -o.r * 0.72; j++) {
          const hw = Math.sqrt(Math.max(0, o.r * o.r - j * j)) * 0.6;
          fill(x, P.cloudRim, cx + o.dx - hw + o.r * 0.28, baseY + o.dy + j, hw * 1.5, 1);
        }
      }
    }
  }

  /* animated water: ripples + sun glitter + reflections */
  function water(t, sun) {
    const x = G.dyn, P = G.P;
    const LK0 = LAKE_X0, LK1 = LAKE_X1, LY0 = LAKE_Y0, LY1 = LAKE_Y1;
    const off = -G.camX * 0.72;
    const sx0 = Math.max(LK0, Math.ceil((0 - off) / 1)), sx1 = Math.min(LK1, Math.floor((W - off)));
    if (sx1 <= sx0) return;
    /* ripples: horizontal crest lines that drift toward the viewer,
     * spaced wider as they get closer (cheap perspective). */
    x.fillStyle = P.waterLit;
    const drop = LY1 - LY0;
    for (let j = LY0; j < LY1; j += waterStep) {
      const row = (j - LY0) / drop;
      const spacing = 2.2 + row * 4.6;
      const crest = Math.sin(j / spacing + t * 1.7);
      if (crest < 0.45) continue;
      const a = ((crest - 0.45) / 0.55) * (0.10 + 0.30 * row);
      x.globalAlpha = a;
      for (let i = sx0; i < sx1; i++) {
        const fy = lakeFar[i], ny = lakeNear[i];
        if (fy === undefined) continue;
        const yy = j + Math.round(Math.sin(i * 0.42 + t * 2.1 + j * 0.8) * 1.3);
        if (yy < fy + 1 || yy > ny - 1) continue;
        if (Math.sin(i * 0.21 + j * 0.55 + t * 1.2) < -0.35) continue;
        x.fillRect(off + i, yy, 1 + ((i * 5) % 3), 1);
      }
    }
    x.globalAlpha = 1;
    /* sun glitter path */
    const gx = sun.x - off; /* world x of the glitter column */
    if (gx < LK0 - 20 || gx > LK1 + 20) { x.globalAlpha = 1; }
    else for (let j = LY0 + 1; j < LY1; j++) {
      const row = (j - LY0) / (LY1 - LY0);
      const wdt = 3 + row * 12;
      const wob = Math.sin(t * 1.9 + j * 0.5) * (1 + row * 3);
      const cx = gx + wob;
      for (let i = -wdt; i <= wdt; i++) {
        const th = BAYER[((i + 64) & 3) + ((j & 3) << 2)] / 16;
        const falloff = 1 - Math.abs(i) / wdt;
        if (th < falloff * (0.9 - row * 0.45)) {
          x.globalAlpha = (0.30 + 0.5 * falloff) * (1 - row * 0.5);
          fill(x, mix(P.waterLit, P.sun, 0.6), off + cx + i, j, 1, 1);
        }
      }
    }
    x.globalAlpha = 1;
    /* far shore reflection of the sky colour, wobbling */
    for (let i = sx0; i < sx1; i++) {
      const fy = lakeFar[i];
      if (fy === undefined) continue;
      const wob = Math.sin(i * 0.5 + t * 0.9) * 1.2;
      x.globalAlpha = 0.20;
      fill(x, P.sky4, off + i, Math.round(fy) + 1 + wob, 1, 1);
      x.globalAlpha = 0.13;
      fill(x, P.sky5, off + i, Math.round(fy) + 2 + wob, 1, 1);
    }
    x.globalAlpha = 1;
    /* sparkle highlights */
    for (let i = sx0; i < sx1; i += 3) {
      const s = Math.sin(i * 1.7 + t * 3.1) * Math.sin(i * 0.4 - t * 1.3);
      if (s > 0.90) {
        const fy0 = lakeFar[i], ny0 = lakeNear[i];
        if (fy0 === undefined) continue;
        const j = Math.round(fy0) + 2 + ((i * 13) % Math.max(1, Math.round(ny0 - fy0) - 4));
        fill(x, P.foam, off + i, j, 1, 1);
        emitDisc(off + i + 0.5, j + 0.5, 1.6, P.foam, 0.5);
      }
    }
  }

  function campfire(t) {
    const x = G.dyn, P = G.P;
    const cx = 322 - G.camX, by = FOOT;
    if (cx < -30 || cx > W + 30) return;
    /* flames: stacked dithered tongues */
    for (let f = 0; f < 3; f++) {
      const fh = 9 + f * 3 + Math.sin(t * (7 + f * 2.3) + f) * 2.2;
      const fw = 4.5 - f * 1.2;
      for (let j = 0; j < fh; j++) {
        const tt = j / fh;
        const hw = fw * (1 - tt * 0.75) * (0.85 + 0.3 * Math.sin(t * 5 + j * 0.6 + f));
        const lean = Math.sin(t * 3.1 + f * 2) * tt * 2.2;
        const col = tt < 0.32 ? P.fireCore : (tt < 0.66 ? P.fireMid : P.fireOut);
        fill(x, col, cx + lean - hw, by - j, hw * 2, 1);
      }
    }
    /* embers */
    srand(((t * 6) | 0) * 13 + 7);
    for (let i = 0; i < 5; i++) {
      const ey = by - 8 - rnd() * 26, ex = cx + (rnd() - 0.5) * 12;
      x.globalAlpha = 0.3 + rnd() * 0.6;
      fill(x, P.fireCore, ex, ey, 1, 1);
      x.globalAlpha = 1;
      emitDisc(ex + 0.5, ey + 0.5, 1.8, P.fireMid, 0.55);
    }
    emitDisc(cx, by - 8, 11, P.fireMid, 0.90);
    emitDisc(cx, by - 10, 22, P.fireOut, 0.38);
  }

  function lantern(t) {
    const x = G.dyn, P = G.P;
    const cx = 167 + 5 - G.camX, cy = FOOT - 27;
    if (cx < -40 || cx > W + 40) return;
    const flick = 0.78 + 0.22 * Math.sin(t * 8.3) * Math.sin(t * 3.1 + 1);
    x.globalAlpha = flick;
    fill(x, P.lanternCore, cx - 3, cy, 5, 4);
    x.globalAlpha = 1;
    emitDisc(cx, cy + 1, 3.5, P.lanternCore, flick * 0.95);
    emitDisc(cx, cy + 1, 10, P.lantern, flick * 0.48);
    emitDisc(cx, cy + 1, 20, P.lantern, flick * 0.18);
    /* window glow of the cottage (two panes) */
    const hy = FOOT - 13;
    for (let w = 0; w < 2; w++) {
      const hx = 276 - 17 + 9 + w * 14 - G.camX;
      if (hx < -40 || hx > W + 40) continue;
      emitDisc(hx + 2, hy + 2, 4, P.winGlow, 0.62 * flick);
      emitDisc(hx + 2, hy + 2, 11, P.winGlow, 0.24 * flick);
    }
  }

  function spireGlow(t) {
    const x = G.dyn, P = G.P;
    const cx = 470 - G.camX * 0.72;
    if (cx < -80 || cx > W + 80) return;
    const pulse = 0.62 + 0.38 * Math.sin(t * 1.25);
    const top = HORIZON + 52 + 6 - 96;
    x.globalAlpha = 0.25 + 0.25 * pulse;
    fill(x, P.crysCore, cx - 4, top, 8, 96);
    x.globalAlpha = 1;
    emit(cx - 5, top, 10, 96, P.crysMid, 0.55 + 0.3 * pulse);
    emit(cx - 20, top - 6, 40, 110, P.crysDark, 0.22 + 0.16 * pulse);
    emitDisc(cx, top + 34, 20, P.crysMid, 0.20 + 0.14 * pulse);
    /* floating shards orbiting the spire */
    for (let i = 0; i < 5; i++) {
      const a = t * (0.35 + i * 0.09) + i * 1.26;
      const rr = 30 + i * 7;
      const ox = cx + Math.cos(a) * rr;
      const oy = (top + 40) + Math.sin(a * 0.8 + i) * 12 + Math.sin(t * 0.9 + i) * 3;
      const s = 2 + (i % 3);
      fill(x, P.crysMid, ox - s, oy - s * 1.6, s * 2, s * 3.2);
      fill(x, P.crysCore, ox - s * 0.4, oy - s * 1.6, s * 0.9, s * 2.4);
      emitDisc(ox, oy, s * 1.8, P.crysMid, 0.6);
    }
  }

  function chimneySmoke(t) {
    const x = G.dyn, P = G.P;
    const hx = 276 - 1 - G.camX, hy = FOOT - 30;
    if (hx < -40 || hx > W + 40) return;
    srand(1919);
    for (let i = 0; i < 6; i++) {
      const p = ((t * 5 + i * 9) % 54) / 54;
      const y = hy - p * 34;
      const xx = hx + Math.sin(p * 5 + i) * 6 + p * 7;
      x.globalAlpha = (1 - p) * 0.30;
      const r = 1.5 + p * 4;
      for (let j = -r; j <= r; j++) {
        const hw = Math.sqrt(Math.max(0, r * r - j * j));
        fill(x, mix(P.cloudDark, P.haze, 0.4), xx - hw, y + j, hw * 2, 1);
      }
    }
    x.globalAlpha = 1;
  }

  /* the hero sprite */
  function character(t, nightT) {
    const x = G.dyn, P = G.P;
    const cx = Math.round(190 - G.camX);
    if (cx < -40 || cx > W + 40) return null;
    const breathe = Math.sin(t * 1.5) > 0 ? 0 : 1;
    const frame = Math.floor((t * 3.2) % 4);
    const by = FOOT - 24 + breathe;
    /* ground shadow */
    x.globalAlpha = 0.30;
    for (let i = -7; i <= 7; i++) {
      const hw = Math.sqrt(Math.max(0, 1 - (i / 7) * (i / 7)));
      fill(x, P.fgDark, cx + i, FOOT - 1, 1, 1 + hw);
    }
    x.globalAlpha = 1;
    /* scarf overlay */
    const rows = CHAR.slice();
    const sf = SCARF_FRAMES[frame];
    for (const k in sf) rows[+k] = sf[k];
    blit(x, rows, CHAR_PAL, cx - 7, by);
    /* cyan rim light on the sun-facing side */
    x.globalAlpha = 0.30 + 0.12 * Math.sin(t * 2);
    for (let j = 0; j < CHAR.length; j++) {
      const r = rows[j];
      for (let i = r.length - 1; i >= 0; i--) {
        if (r[i] !== '.') {
          if (i + 1 >= r.length || r[i + 1] === '.') fill(x, P.charLit, cx - 7 + i + 1, by + j, 1, 1);
          break;
        }
      }
    }
    x.globalAlpha = 1;
    /* face glow */
    emitDisc(cx, by + 8, 7, P.charSkin, 0.20);
    return { x: cx - 7, y: by, w: 14, h: CHAR.length };
  }

  function cat(t) {
    const x = G.dyn, P = G.P;
    const cx = Math.round(300 - G.camX), by = FOOT + 1;
    if (cx < -20 || cx > W + 20) return;
    blit(x, CAT, CAT_PAL, cx - 5, by - 9);
    /* tail */
    for (let i = 0; i < 6; i++) {
      const a = -0.5 + i * 0.22 + Math.sin(t * 1.6) * 0.25;
      fill(x, P.charMid, cx + 5 + Math.cos(a) * (2 + i * 0.7), by - 4 - Math.sin(a) * (2 + i * 0.9), 1, 1);
    }
    emitDisc(cx, by - 8, 3, P.crysCore, 0.38);
  }

  /* ================= DEBUG: sprite sheet ================= */
  function spriteSheet() {
    const cv = document.getElementById('scene');
    const ctx = cv.getContext('2d');
    cv.width = W; cv.height = H;
    cv.style.width = W * 3 + 'px'; cv.style.height = H * 3 + 'px';
    ctx.imageSmoothingEnabled = false;
    G = { P: buildPal(0), camX: 0, mx: 0, my: 0, dyn: ctx };
    fill(ctx, '#101018', 0, 0, W, H);
    blit(ctx, CHAR, CHAR_PAL, 8, 8);
    blit(ctx, PINE, PINE_PAL, 30, 8);
    blit(ctx, HUT, HUT_PAL, 52, 8);
    blit(ctx, SIGN, SIGN_PAL, 96, 8);
    blit(ctx, LAMP, LAMP_PAL, 116, 8);
    blit(ctx, CAT, CAT_PAL, 136, 8);
    /* width validation */
    const check = { CHAR: CHAR, PINE: PINE, HUT: HUT, SIGN: SIGN, LAMP: LAMP, CAT: CAT };
    let bad = [];
    for (const k in check) {
      const ws = {}; check[k].forEach((r, i) => { ws[r.length] = (ws[r.length] || 0) + 1; });
      if (Object.keys(ws).length > 1) bad.push(k + ' widths=' + JSON.stringify(ws));
    }
    ctx.fillStyle = bad.length ? '#ff5555' : '#55ff88';
    ctx.font = '6px monospace';
    ctx.fillText(bad.length ? bad.join(' | ') : 'ALL SPRITE WIDTHS OK', 4, 200);
    return bad;
  }

  /* ================= main api ================= */
  let cv, ctx, glowC, glowX, sceneC, sceneX;
  let dofC, dofX, dofMask, dofMaskX, grainTiles = [], grainIdx = 0;
  let visY0 = 0, visY1 = H, canvasDof = false;
  let gradeCache = null, gradeKey = -1, frameNo = 0;
  /* adaptive quality: 2 = full, 1 = reduced, 0 = minimal */
  let quality = 2, dofOn = true, grainOn = true, rayN = 15, moteOn = true, waterStep = 1;
  let workMs = 8, qAcc = 0, qN = 0, qWarm = 0;
  let L = {}, glowOK = true;
  let nightT = 0, nightTarget = 0;
  let progress = 0, camX = 0, camY = 0;
  let mx = 0, my = 0, tmx = 0, tmy = 0;
  let raf = 0, t0 = 0, last = 0, rebuildAcc = 0;
  let running = false, reduced = false;
  let charBox = null;
  let onCharClick = null;
  let k = 4;

  function layout() {
    if (!cv) return;
    const vw = window.innerWidth, vh = window.innerHeight;
    k = Math.max(1, Math.ceil(Math.max(vw / W, vh / H)));
    const ox = (vw - W * k) / 2;
    /* Cover-cropping vertically would push the plateau (and the hero
     * sprite, at canvas y 160-184) off a wide short viewport, so bias the
     * window downward: centre it on the land, not on the whole canvas. */
    const vis = Math.min(H, vh / k);
    const top = Math.max(0, Math.min(H - vis, 131 - vis / 2));
    const oy = -top * k;
    cv.style.width = (W * k) + 'px';
    cv.style.height = (H * k) + 'px';
    cv.style.left = ox + 'px';
    cv.style.top = oy + 'px';
    /* which canvas rows the viewport actually shows (for the DOF bands) */
    visY0 = -oy / k;
    visY1 = visY0 + vh / k;
    buildDofMask();
  }

  /* Pre-baked vertical mask: opaque where the frame is out of focus,
   * transparent across the focal plane. */
  function buildDofMask() {
    if (!dofMask) return;
    const d = dofMaskX.createImageData(W, H);
    const px = d.data;
    const h = Math.max(1, visY1 - visY0);
    const stops = [
      [0.00, 1.00], [0.04, 1.00], [0.16, 0.55],
      [0.30, 0.00], [0.72, 0.00], [0.86, 0.50],
      [0.97, 0.95], [1.00, 0.95],
    ];
    for (let y = 0; y < H; y++) {
      const t = (y - visY0) / h;
      let a = 1;
      if (t <= stops[0][0]) a = stops[0][1];
      else if (t >= stops[stops.length - 1][0]) a = stops[stops.length - 1][1];
      else {
        for (let i = 0; i < stops.length - 1; i++) {
          if (t >= stops[i][0] && t <= stops[i + 1][0]) {
            const lt = (t - stops[i][0]) / ((stops[i + 1][0] - stops[i][0]) || 1);
            a = stops[i][1] + (stops[i + 1][1] - stops[i][1]) * lt;
            break;
          }
        }
      }
      const v = Math.round(a * 255);
      for (let x = 0; x < W; x++) {
        const o = ((y * W) + x) << 2;
        px[o] = px[o + 1] = px[o + 2] = 255;
        px[o + 3] = v;
      }
    }
    dofMaskX.putImageData(d, 0, 0);
  }

  /* A few film-grain tiles, cycled so the noise crawls like real film. */
  function buildGrainTiles() {
    grainTiles = [];
    for (let n = 0; n < 3; n++) {
      const c = document.createElement('canvas');
      c.width = W; c.height = H;
      const gx = c.getContext('2d');
      const img = gx.createImageData(W, H);
      const d = img.data;
      for (let i = 0; i < W * H; i++) {
        const v = Math.random();
        let g = 128, a = 0;
        if (v > 0.905) { g = 255; a = 96; }
        else if (v < 0.095) { g = 0; a = 96; }
        d[i * 4] = d[i * 4 + 1] = d[i * 4 + 2] = g;
        d[i * 4 + 3] = a;
      }
      gx.putImageData(img, 0, 0);
      grainTiles.push(c);
    }
  }

  function markDirty() {
    for (const n in L) L[n].dirty = true;
    /* palette moved → sprite canvases must be re-baked */
    palGen++;
    for (const kk in spriteCache) delete spriteCache[kk];
  }

  function applyQuality() {
    dofOn = canvasDof && quality >= 2;
    grainOn = quality >= 2;
    rayN = quality >= 2 ? 15 : (quality === 1 ? 9 : 0);
    moteOn = quality >= 1;
    waterStep = quality >= 2 ? 1 : 2;
  }

  /* Watch the real cost of a frame and step the effect budget up or down.
   * Weak hardware loses the depth of field and the grain rather than the
   * whole scene. */
  function adaptQuality(ms) {
    if (qWarm < 45) { qWarm++; return; }
    qAcc += ms; qN++;
    if (qN < 45) return;
    const avg = qAcc / qN;
    qAcc = 0; qN = 0;
    if (avg > 13 && quality > 0) { quality--; applyQuality(); }
    else if (avg < 5.5 && quality < 2) { quality++; applyQuality(); }
  }

  function rebuild() {
    if (L.sky.dirty) { paintSky(L.sky); L.sky.dirty = false; }
    if (L.far.dirty) { paintFar(L.far); L.far.dirty = false; }
    if (L.mid.dirty) { paintMid(L.mid); L.mid.dirty = false; }
    if (L.near.dirty) { paintNear(L.near); L.near.dirty = false; }
    if (L.fore.dirty) { paintFore(L.fore); L.fore.dirty = false; }
  }

  function frame(now) {
    if (!running) return;
    raf = requestAnimationFrame(frame);
    const dt = Math.min(0.05, (now - last) / 1000 || 0.016);
    last = now;
    frameNo++;
    const t = (now - t0) / 1000;

    /* day/night easing */
    if (Math.abs(nightT - nightTarget) > 0.002) {
      nightT += (nightTarget - nightT) * Math.min(1, dt * 1.9);
      G = G || {};
      G.P = buildPal(nightT);
      rebuildAcc += dt;
      if (rebuildAcc > 0.09) { rebuildAcc = 0; markDirty(); }
    } else if (nightT !== nightTarget) {
      nightT = nightTarget; G.P = buildPal(nightT); markDirty();
    }

    /* camera */
    camX += (progress * CAM_MAX - camX) * Math.min(1, dt * 3.4);
    camY += (progress * -14 - camY) * Math.min(1, dt * 3.4);
    mx += (tmx - mx) * Math.min(1, dt * 3);
    my += (tmy - my) * Math.min(1, dt * 3);
    G.camX = camX; G.mx = mx; G.my = my;

    rebuild();

    const work0 = performance.now();
    const sx = sceneX, P = G.P;

    /* ---- compose ---- */
    G.P = P;
    sx.clearRect(0, 0, W, H);
    sx.imageSmoothingEnabled = false;
    sx.drawImage(L.sky.c, 0, -camY * 0.25);
    sx.drawImage(L.far.c, -camX * L.far.factor + mx * 0.7, -camY * 0.5);
    sx.drawImage(L.mid.c, -camX * L.mid.factor + mx * 1.1, -camY * 0.75);
    sx.drawImage(L.near.c, -camX * L.near.factor + mx * 1.7, -camY);
    sx.drawImage(L.fore.c, -camX * L.fore.factor + mx * 2.4, -camY * 1.15);

    /* ---- dynamic overlay onto the same buffer ---- */
    const saveDyn = G.dyn; G.dyn = sx;
    glowQ.length = 0;
    const sun = sunMoon(t, nightT);
    clouds(t);
    water(t, sun);
    spireGlow(t);
    lantern(t);
    chimneySmoke(t);
    campfire(t);
    cat(t);
    charBox = character(t, nightT);
    fireflies(t);
    dustMotes(t);
    fallingLeaves(t);
    G.dyn = saveDyn;

    /* ---- bloom ---- */
    glowX.clearRect(0, 0, W >> 1, H >> 1);
    glowX.globalCompositeOperation = 'lighter';
    for (let i = 0; i < glowQ.length; i++) {
      const g = glowQ[i];
      glowX.globalAlpha = Math.min(1, g[5]);
      glowX.fillStyle = g[4];
      if (g[6]) {
        glowX.beginPath();
        glowX.arc(g[0] * 0.5, g[1] * 0.5, Math.max(0.6, g[2] * 0.5), 0, 6.2832);
        glowX.fill();
      } else {
        glowX.fillRect(g[0] * 0.5, g[1] * 0.5, Math.max(1, g[2] * 0.5), Math.max(1, g[3] * 0.5));
      }
    }
    glowX.globalAlpha = 1;
    glowX.globalCompositeOperation = 'source-over';
    godRays(t, sun, nightT);

    sx.save();
    sx.globalCompositeOperation = 'lighter';
    sx.imageSmoothingEnabled = true;
    if (glowOK) {
      sx.filter = 'blur(2px)';
      sx.globalAlpha = 0.95;
      sx.drawImage(glowC, 0, 0, W, H);
      if (quality >= 2) {          /* the wide pass is the first thing to go */
        sx.filter = 'blur(6px)';
        sx.globalAlpha = 0.55;
        sx.drawImage(glowC, 0, 0, W, H);
      }
      sx.filter = 'none';
    } else {
      sx.globalAlpha = 0.75;
      sx.drawImage(glowC, 0, 0, W, H);
    }
    sx.restore();
    sx.imageSmoothingEnabled = false;

    /* ---- colour grade + lens bloom + film grain, all in pixel space ---- */
    sx.save();

    /* warm/cool split tone — gradients are cached until the palette moves */
    sx.globalCompositeOperation = 'overlay';
    const gk = Math.round(nightT * 24);
    if (gk !== gradeKey) {
      gradeKey = gk;
      const top = sx.createLinearGradient(0, 0, 0, H * 0.55);
      top.addColorStop(0, nightT > 0.5 ? 'rgba(30,50,130,0.30)' : 'rgba(40,60,140,0.20)');
      top.addColorStop(1, 'rgba(0,0,0,0)');
      const bot = sx.createLinearGradient(0, H * 0.55, 0, H);
      bot.addColorStop(0, 'rgba(0,0,0,0)');
      bot.addColorStop(1, nightT > 0.5 ? 'rgba(10,20,60,0.34)' : 'rgba(90,30,60,0.24)');
      gradeCache = [top, bot];
    }
    sx.fillStyle = gradeCache[0]; sx.fillRect(0, 0, W, H * 0.6);
    sx.fillStyle = gradeCache[1]; sx.fillRect(0, H * 0.5, W, H * 0.5);
    sx.globalAlpha = 0.16 + nightT * 0.06;
    sx.fillStyle = P.vib;
    sx.fillRect(0, 0, W, H);
    sx.globalAlpha = 1;

    /* lens bloom around the sun / moon */
    sx.globalCompositeOperation = 'lighter';
    const bgx = (300 - camX * 0.15 + mx * 0.25), bgy = (66 + my * 0.2);
    const bcol = nightT > 0.5 ? '168,202,255' : '255,196,120';
    const br = sx.createRadialGradient(bgx, bgy, 0, bgx, bgy, W * 0.40);
    br.addColorStop(0, 'rgba(' + bcol + ',' + (0.22 - nightT * 0.11).toFixed(3) + ')');
    br.addColorStop(0.45, 'rgba(' + bcol + ',' + (0.08 - nightT * 0.045).toFixed(3) + ')');
    br.addColorStop(1, 'rgba(' + bcol + ',0)');
    sx.fillStyle = br;
    sx.fillRect(0, 0, W, H);

    /* film grain — refreshed on alternate frames, which reads the same */
    if (grainOn && (frameNo & 1) === 0) {
      sx.globalCompositeOperation = 'overlay';
      sx.globalAlpha = 0.30;
      sx.drawImage(grainTiles[(grainIdx++) % grainTiles.length], 0, 0);
    }
    sx.restore();
    sx.globalAlpha = 1;
    sx.globalCompositeOperation = 'source-over';

    /* ---- tilt-shift depth of field, baked into the pixels ----
     * Only the two out-of-focus bands are blurred; the focal plane is
     * simply never touched, which halves the work. */
    if (dofOn) {
      const vh = visY1 - visY0;
      const stopA = Math.max(0, Math.round(visY0 + vh * 0.34));
      const stopB = Math.min(H, Math.round(visY0 + vh * 0.64));
      dofX.globalCompositeOperation = 'source-over';
      dofX.clearRect(0, 0, W, H);
      dofX.filter = 'blur(1.25px)';
      if (stopA > 0) dofX.drawImage(sceneC, 0, 0, W, stopA, 0, 0, W, stopA);
      if (stopB < H) dofX.drawImage(sceneC, 0, stopB, W, H - stopB, 0, stopB, W, H - stopB);
      dofX.filter = 'none';
      dofX.globalCompositeOperation = 'destination-in';
      dofX.drawImage(dofMask, 0, 0);
      dofX.globalCompositeOperation = 'source-over';
      sx.drawImage(dofC, 0, 0);
    }

    /* ---- present ---- */
    ctx.imageSmoothingEnabled = false;
    ctx.clearRect(0, 0, W, H);
    ctx.drawImage(sceneC, 0, 0);

    workMs += (performance.now() - work0 - workMs) * 0.08;
    adaptQuality(workMs);
  }

  function init(canvas, opts) {
    opts = opts || {};
    onCharClick = opts.onCharClick || null;
    cv = canvas;
    cv.width = W; cv.height = H;
    ctx = cv.getContext('2d');
    ctx.imageSmoothingEnabled = false;

    sceneC = document.createElement('canvas'); sceneC.width = W; sceneC.height = H;
    sceneX = sceneC.getContext('2d'); sceneX.imageSmoothingEnabled = false;
    glowC = document.createElement('canvas'); glowC.width = W >> 1; glowC.height = H >> 1;
    glowX = glowC.getContext('2d');
    /* feature-detect canvas filters */
    glowOK = (typeof glowX.filter === 'string');
    canvasDof = glowOK;
    dofC = document.createElement('canvas'); dofC.width = W; dofC.height = H;
    dofX = dofC.getContext('2d');
    dofMask = document.createElement('canvas'); dofMask.width = W; dofMask.height = H;
    dofMaskX = dofMask.getContext('2d');
    buildGrainTiles();

    L.sky = mkLayer(W, H, 0);
    L.far = mkLayer(PW, H, 0.42);
    L.mid = mkLayer(PW, H, 0.72);
    L.near = mkLayer(PW, H, 1.0);
    L.fore = mkLayer(PW, H, 1.32);

    reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    G = { P: buildPal(nightTarget), camX: 0, mx: 0, my: 0, dyn: sceneX };
    buildLakeShape();
    initParticles();
    applyQuality();
    markDirty();
    rebuild();
    layout();

    window.addEventListener('resize', layout);
    window.addEventListener('pointermove', function (e) {
      tmx = (e.clientX / window.innerWidth - 0.5) * 2;
      tmy = (e.clientY / window.innerHeight - 0.5) * 2;
    }, { passive: true });
    cv.addEventListener('click', function (e) {
      if (!charBox || !onCharClick) return;
      const r = cv.getBoundingClientRect();
      const px = (e.clientX - r.left) / k;
      const py = (e.clientY - r.top) / k;
      if (px >= charBox.x - 6 && px <= charBox.x + charBox.w + 6 &&
        py >= charBox.y - 6 && py <= charBox.y + charBox.h + 4) {
        onCharClick();
      }
    });

    if (reduced) {
      last = performance.now(); t0 = last - 3000;
      frame(last);
      running = false;
      cancelAnimationFrame(raf);
      /* still allow the day/night toggle to repaint */
      window.__hd2dStatic = function () {
        const n = performance.now();
        const s = running; running = true; cancelAnimationFrame(raf); frame(n);
        running = false; cancelAnimationFrame(raf);
      };
    } else {
      running = true;
      t0 = last = performance.now();
      raf = requestAnimationFrame(frame);
    }
    return true;
  }

  return {
    init: init,
    sprites: spriteSheet,
    /* draw the hero into an arbitrary square canvas (HUD / status portrait) */
    drawHero: function (target, size) {
      const pal = buildPal(nightTarget);
      const G0 = G;
      G = { P: pal, camX: 0, mx: 0, my: 0, dyn: target };
      target.imageSmoothingEnabled = false;
      target.clearRect(0, 0, size, size);
      const rows = CHAR.slice();
      const sf = SCARF_FRAMES[0];
      for (const kk in sf) rows[+kk] = sf[kk];
      const sc = Math.max(1, Math.floor(size / 26));
      const dw = 14 * sc, dh = 24 * sc;
      const ox = Math.round((size - dw) / 2);
      const oy = Math.round((size - dh) / 2);
      target.drawImage(spriteCanvas(rows, CHAR_PAL), 0, 0, 14, 24, ox, oy, dw, dh);
      G = G0;
    },
    /* the crystal-spire crest used on the loading screen */
    drawCrest: function (target, size) {
      const pal = buildPal(nightTarget);
      const G0 = G;
      G = { P: pal, camX: 0, mx: 0, my: 0, dyn: target };
      target.imageSmoothingEnabled = false;
      target.clearRect(0, 0, size, size);
      const h = size * 0.86, w = size * 0.42;
      const cx = size / 2, base = size * 0.92, top = base - h;
      for (let j = 0; j < h; j++) {
        const t = j / h;
        const hw = (w / 2) * (0.25 + 0.75 * Math.pow(t, 0.8));
        const y = Math.round(top + j);
        fill(target, pal.crysEdge, cx - hw, y, hw * 2, 1);
        fill(target, pal.crysDark, cx - hw + 1, y, hw * 2 - 2, 1);
        fill(target, pal.crysMid, cx - hw * 0.40, y, hw * 0.84, 1);
        if (t < 0.74) fill(target, pal.crysCore, cx + hw * 0.06, y, hw * 0.46, 1);
        if ((j % 13) === 5) fill(target, pal.crysCore, cx - hw * 0.7, y, hw * 0.5, 1);
      }
      const sparks = [[0.0, -1], [0.87, -0.5], [0.87, 0.5], [0, 1], [-0.87, 0.5], [-0.87, -0.5]];
      for (let i = 0; i < sparks.length; i++) {
        const rr = w * 1.05;
        const px = Math.round(cx + sparks[i][0] * rr);
        const py = Math.round(top + h * 0.44 + sparks[i][1] * rr * 0.5);
        fill(target, pal.crysCore, px, py, 2, 2);
        fill(target, pal.crysMid, px - 1, py + 2, 1, 1);
        fill(target, pal.crysMid, px + 2, py - 1, 1, 1);
      }
      G = G0;
    },
    setProgress: function (p) {
      progress = Math.max(0, Math.min(1, p));
      if (reduced && window.__hd2dStatic) window.__hd2dStatic();
    },
    /* jump the camera straight to the target, skipping the easing */
    snap: function () {
      camX = progress * CAM_MAX;
      camY = progress * -14;
      if (reduced && window.__hd2dStatic) window.__hd2dStatic();
    },
    setNight: function (on) {
      nightTarget = on ? 1 : 0;
      if (reduced && window.__hd2dStatic) window.__hd2dStatic();
    },
    getNight: function () { return nightTarget; },
    hasCanvasDof: function () { return canvasDof; },
    quality: function () { return quality; },
    W: W, H: H,
  };
})();
