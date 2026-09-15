// ==UserScript==
// @name            新华三路由器增强
// @name:en         Bro-Stat-H3C
// @namespace       ucxn
// @version         5.9.5
// @description     哥哥科技 QQ群 680464365
// @description:en  https://github.com/ucxn/Mi-Stat_Max
// @author          哥哥科技 space.bilibili.com/501430041
// @noframes
// @include         http://10.*.*.*
// @include         http://192.168.*.*
// @include         http://172.16.*
// @include         https://10.*.*.*
// @include         https://192.168.*.*
// @include         https://172.16.*
// @run-at          document-start
// @grant           GM_setValue
// @grant           GM_getValue
// @storageName     GBNPA_Storage
// @license         AGPL-3.0-or-later
// @updateURL       https://github.com/ucxn/Bro-Stat/raw/refs/heads/main/H3C-TX_1801.user.js
// @downloadURL     https://github.com/ucxn/Bro-Stat/raw/refs/heads/main/H3C-TX_1801.user.js

// ==/UserScript==

(function () {
  'use strict';

  console.log("🚀 哥哥科技 V5.9.9 终极引擎已装载...");

  function escapeHTML(str) {
    if (!str) return '';
    return String(str).replace(/[&<>'"]/g, function (match) {
      return {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        "'": '&#39;',
        '"': '&quot;'
      } [match];
    });
  }

  // ======== [0] 用户极客环境变量配置区 ========
  const CONFIG = {
    readSaveData: 0, // 【历史记录】 1: 读档模式(继承本次历史量) | 0: 新局模式(从打开网页此刻归零重新计流)
    forceMeshMode: 0,
    uiLayout: 2,//【面板拓扑结构】 0: 经典版 | 1: 详细紧凑版(驾驶舱美学) | 2: 详细平铺版(报表流美学)
    injectMode: 3, //1: 优先，10秒悬浮舱(D)| 3：强制模式
    calcMode: 1,// 1: 上行/下行倍数模式, 0: 上行占总和比例模式
    ratioExtremeUp: 10, // 极端上传判定阈值 (> 1000%)
    ratioWarnUp: 0.07, // 重度上传警告阈值 (> 7%)
    ratioExtremeDown: 0.01, // 极端下载判定阈值 (< 1%)
    ratioThreshold: 7, // (仅calcMode=0时有效) 上传占比报警阈值(%)
    lanRefreshInterval: 3, // LAN口刷新时间(秒)，用于精准补偿0到唤醒时的瞬时流量
    wanRefreshInterval: 3, // 【新增】WAN口刷新时间(秒)，用于精准补偿0到唤醒时的瞬时流量
    portMap: {
      "eth1": "网口 1",
      "eth2": "网口 2",
      "eth3": "网口 3",
      "eth4": "网口 4",
      "wl0": "2.4G",
      "wl1": "5.2G",
      "wl2": "5.8G"
    }
  };

let _saved = null;
  if (CONFIG.readSaveData === 1 && typeof GM_getValue !== 'undefined') {
    try { _saved = GM_getValue('ha_snapshot', null); } catch(e) {console.warn(e)}
  }

  const S = {
    lt: 0, wInstUp: 0, wInstDn: 0,
    wTotUp: _saved?.global?.wan_up || 0,
    wTotDn: _saved?.global?.wan_down || 0,
    cls: {}, isPinned: !0,
    w2U: 0, w2D: 0, w2TotUp: 0, w2TotDn: 0, w2LT: undefined,
    hasW2: !1, is5G_149: !1,
    无线缓存: Object.create(null), 无线请求中: !1, 端口状态: Object.create(null), 端口请求中: !1, 端口已取: !1, 端口小齿轮: 0, 半双工色轮: 0, DOM缓存: null, DOM已重建: !1, 面板状态: null, HA小齿轮: 0, 包数据可用: !1, RSSI频率修正: undefined,
    总上行图: new Float64Array(8192), 总下行图: new Float64Array(8192), 总图点数: 0,
    wMaxU: 0, wMaxD: 0, wMinU: Infinity, wMinD: Infinity, 图表拖: null, 图表待画: 0
  };

  const _w = typeof unsafeWindow !== 'undefined' ? unsafeWindow : window;
  const GBK = new TextDecoder('gbk');
  
  if (location.pathname.includes('main.html')) {
    const jumpToPc = (stok) => {
      if (!window.__gegeJumped && location.hash.includes('#/home')) {
        window.__gegeJumped = !0;
        document.body.innerHTML = '<div style="display:flex; height:100vh; width:100vw; background:#f3f4f5; align-items:center; justify-content:center; color:#0059fa; font-weight:bold; font-size:18px; font-family:sans-serif;">🚀 哥哥科技：正在强制跃迁至 PC 网页版...</div>';
        location.replace(`/cgi-bin/luci/;stok=${stok}/web?goto=pc#router`);
      }
    };
    const origOpen = _w.XMLHttpRequest.prototype.open;
    _w.XMLHttpRequest.prototype.open = function(method, url) {
      let m = String(url).match(/;stok=([a-fA-F0-9]+)/);
      if (m) jumpToPc(m[1]);
      return origOpen.apply(this, arguments);
    };
    const origFetch = _w.fetch;
    if (origFetch) {
      _w.fetch = function() {
        let m = String(arguments[0]).match(/;stok=([a-fA-F0-9]+)/);
        if (m) jumpToPc(m[1]);
        return origFetch.apply(this, arguments);
      };
    }
  }

  function fB(bps) {
		if (bps > 1e9) return `${(bps * 1e-6).toFixed(1)} Mbit/s`;
        if (bps > 1e6) return `${(bps * 1e-6).toFixed(2)} Mbps`;
        if (bps > 1e3) return `${(bps * 1e-3).toFixed(1)} kbps`;
        return `${Math.round(bps)} bps`;
    }

  function fBy(bps) {
        if (bps === 0) return '0  B';
        if (bps > 8388608) return `${(bps / 8388608).toFixed(2)} MiB/s`;
        return bps < 8602
            ? ((bps * 0.001 | 0) === bps * 0.001
                ? `${['0', '[1/8]', '[2/8]', '[3/8]', '[4/8]', '[5/8]', '[6/8]', '[7/8]', '[1]'][bps * 0.001]} kB/s`
                : `${(bps * 0.000125).toFixed(2)} kB/s`)
            : `${(bps / 8192).toFixed(1)} KiB/s`;
    }
  function fV(bits) {
        if (bits > 83886080000) return `${(bits / 8589934592).toFixed(4)} GiB`;
		if (bits > 8388608000) return `${(bits / 8388608).toFixed(1)} MiB`;
        if (bits > 8388608) return `${(bits / 8388608).toFixed(4)} MiB`;
        if (bits > 8192) return `${(bits / 8192).toFixed(3)} KiB`;
        return `${Math.round(bits / 8)} B`;
    }

  function fSV(bits) {
    if (bits >= 84607500288) return `${(bits / 8589934592).toPrecision(4)}G`;
	if (bits > 8388608000) return `${Math.round(bits / 8388608)}M`;
    if (bits > 8388608) return `${(bits / 8388608).toFixed(2)}M`;
    if (bits >= 8192) return `${(bits / 8192).toFixed(1)}K`;
    return `${Math.round(bits / 8)}B`;}

  function fOT(totalSec) {
		totalSec = Math.floor(totalSec);
        if (totalSec < 0) return "";
		const d = Math.floor(totalSec / 86400);
		let r = totalSec - d * 86400;
		const h = Math.floor(r / 3600);
		r = r - h * 3600;
		const m = Math.floor(r / 60);
		const s = r - m * 60;
        return d > 0 
        ? `${d}天${h}时${m}分${s}秒` 
        : `${h}小时${m}分${s}秒`;}

  function nM(m) {
    return m ? m.toLowerCase().replace(/-/g, ':').replace(/\s/g, '') : '';
  }
  function 取名单项(t, 名) {
    let m = t.match(new RegExp('var\\s+' + 名 + '\\s*=\\s*new\\s+Array\\(([\\s\\S]*?)\\);'));
    return m ? (m[1].match(/"[^"]*"|'[^']*'/g) || []) : [];
  }
  function 净设备名(x) {
    x = (x || '').trim();
    return x && !/^(allow|deny|on|out|LAN|WLAN\d*|static|dynamic|0|未绑定|离线)$/i.test(x) ? x : '';
  }
  function H3C接口(x) {
    x = (x || '').toUpperCase();
    if (x.includes('WLAN6')) return 'wl1';
    if (x.includes('WLAN')) return 'wl0';
    let m = x.match(/(?:LAN|ETH(?:ERNET)?)[^0-9]*([1-9]\d*)/);
    if (m) return 'eth' + m[1];
    return /(?:LAN|ETH(?:ERNET)?)/.test(x) ? 'eth1' : '';
  }
  function 写设备名(d, mac, 名, 覆盖 = 0) {
    名 = 净设备名(名);
    if (!mac || !名) return;
    d[mac] ??= {};
    if (覆盖 || !d[mac].name) d[mac].name = 名;
  }

  function 短数(n) { return (+(n || 0).toPrecision(3)).toString(); }
  function 计算包比(u, d) { return d > 0 ? u / d : (u > 0 ? Infinity : 0); }
  function 格式化包比(r) { return r === Infinity ? '∞' : (!r || r < 0 ? '0%' : (r < 1e-3 ? `${短数(r * 1e4)}/万` : (r < 10 ? `${短数(r * 100)}%` : `${短数(r)}倍`))); }
  function 取网口图标(速率) {
    let s = +速率 || 0;
    if (s >= 2500) return `<svg viewBox="0 0 100 100" width="45" height="45"><rect x="15" y="15" width="70" height="65" rx="5" fill="#cfd8dc" stroke="#90a4ae" stroke-width="4"/><path d="M 25,25 L 75,25 L 75,60 L 60,60 L 60,75 L 40,75 L 40,60 L 25,60 Z" fill="#263238"/><g fill="#ffca28"><rect x="30" y="25" width="2.5" height="18"/><rect x="35" y="25" width="2.5" height="18"/><rect x="40" y="25" width="2.5" height="18"/><rect x="45" y="25" width="2.5" height="18"/><rect x="52.5" y="25" width="2.5" height="18"/><rect x="57.5" y="25" width="2.5" height="18"/><rect x="62.5" y="25" width="2.5" height="18"/><rect x="67.5" y="25" width="2.5" height="18"/></g><circle cx="21" cy="73" r="3.5" fill="#4caf50"/><circle cx="79" cy="73" r="3.5" fill="#ffb300"/></svg>`;
    let c = s >= 1000 ? '#4CAF50' : s >= 100 ? '#5394CC' : s > 0 ? '#E7B05C' : '#7b8aa0';
    return `<svg viewBox="0 0 100 100" width="45" height="45" xmlns="http://www.w3.org/2000/svg"><g transform="rotate(180 50 50)"><path d="M18 82V36Q18 32 22 32H28V24Q28 20 32 20H38V14Q38 10 42 10H58Q62 10 62 14V20H68Q72 20 72 24V32H78Q82 32 82 36V82Q82 86 78 86H22Q18 86 18 82Z" fill="${c}" fill-opacity=".10" stroke="${c}" stroke-width="4" stroke-linejoin="round"/><g stroke="${c}" stroke-width="3.2" stroke-linecap="round"><path d="M31 54V76"/><path d="M36.5 54V76"/><path d="M42 54V76"/><path d="M47.5 54V76"/><path d="M53 54V76"/><path d="M58.5 54V76"/><path d="M64 54V76"/><path d="M69.5 54V76"/></g></g></svg>`;
  }
  function 取设备图标(信号, 有线, 速率 = 0) {
    if (有线) return 取网口图标(速率);
    let r = 信号 | 0, c = r > -24 ? '#4caf50' : r > -35 ? '#9c27b0' : '#0059fa';
    if (r > -40) return `<svg viewBox="0 0 100 100" width="45" height="45"><path d="M 50,85 L 10,35 A 65,65 0 0,1 90,35 Z" fill="${c}"/></svg>`;
    if (r > -46) return `<svg viewBox="0 0 100 100" width="45" height="45"><path d="M 50,85 L 10,35 A 65,65 0 0,1 90,35 Z" fill="#e0e0e0"/><path d="M 50,85 L 18,45 A 50,50 0 0,1 82,45 Z" fill="${c}"/></svg>`;
    if (r > -50) return `<svg viewBox="0 0 100 100" width="45" height="45"><path d="M 50,85 L 10,35 A 65,65 0 0,1 90,35 Z" fill="#e0e0e0"/><path d="M 50,85 L 23,51 A 43,43 0 0,1 77,51 Z" fill="${c}"/></svg>`;
    if (r > -56) return `<svg viewBox="0 0 100 100" width="45" height="45"><circle cx="50" cy="80" r="9" fill="${c}"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="${c}" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="${c}" stroke-width="7" stroke-linecap="round"/></svg>`;
    if (r > -61) return `<svg viewBox="0 0 100 100" width="45" height="45"><circle cx="50" cy="80" r="9" fill="${c}"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="${c}" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></svg>`;
    if (r > -67) return `<svg viewBox="0 0 100 100" width="45" height="45"><circle cx="50" cy="80" r="9" fill="${c}"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></svg>`;
    if (r > -72) return `<svg viewBox="0 0 100 100" width="45" height="45"><circle cx="50" cy="80" r="7" fill="none" stroke="#ff9800" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></svg>`;
    if (r > -76) return `<svg viewBox="0 0 100 100" width="45" height="45"><g transform="translate(-15, 0)"><circle cx="50" cy="80" r="7" fill="none" stroke="#ffb300" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></g><text x="70" y="86" fill="#ffb300" font-weight="900" font-size="48" font-family="sans-serif">!</text></svg>`;
    if (r > -80) return `<svg viewBox="0 0 100 100" width="45" height="45"><g transform="translate(-15, 0)"><circle cx="50" cy="80" r="7" fill="none" stroke="#ff4c00" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></g><text x="70" y="86" fill="#ff4c00" font-weight="900" font-size="48" font-family="sans-serif">!</text></svg>`;
    if (r > -85) return `<svg viewBox="0 0 100 100" width="45" height="45"><g transform="translate(-15, 0)"><circle cx="50" cy="80" r="7" fill="none" stroke="#ff4c00" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></g><text x="65" y="86" fill="#ff4c00" font-weight="900" font-size="44" font-family="sans-serif">?</text></svg>`;
    return `<svg viewBox="0 0 100 100" width="45" height="45"><g transform="translate(-15, 0)"><circle cx="50" cy="80" r="7" fill="none" stroke="#ff4c00" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#ff4c00" stroke-width="7" stroke-linecap="round" opacity="0.3"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#ff4c00" stroke-width="7" stroke-linecap="round" opacity="0.3"/></g><text x="65" y="80" fill="#ff4c00" font-weight="900" font-size="35" font-family="sans-serif">✖</text></svg>`;
  }
  const 火花字符 = [' ', '▂', '▃', '▄', '▅', '▆', '▇', '█'];
  function 取火花线(环, 头, 最大, 最小 = 0) {
    let s = '';
    for (let i = 31; i >= 0; i--) {
      let v = 环[(头 - i) & 31] || 0;
      s += 火花字符[(v < 最小 || 最大 <= 0) ? 0 : Math.min(7, Math.max(1, (v / 最大 * 7) | 0))];
    }
    return s;
  }
  async function 刷无线信息(ts = Date.now()) {
    if (S.无线请求中) return;
    S.无线请求中 = !0;
    try {
      let r = await fetch(`/wlan_ap_client_list.asp?_=${ts}`);
      if (!r.ok) return;
      let t = GBK.decode(await r.arrayBuffer()), 新缓存 = Object.create(null), 要重绘 = !1;
      取名单项(t, 'dhcpd_client_list').forEach(s => {
        let p = s.slice(1, -1).split(';'), mac = nM(p[1]), 接口 = (p[5] || '').toUpperCase().includes('5G') ? 'wl1' : 'wl0';
        if (mac) {
          if (!S.无线缓存[mac] || S.无线缓存[mac].接口 !== 接口) 要重绘 = !0;
          新缓存[mac] = { 信号: (+p[6] || 0) - 92, 协商: +p[7] || 0, SSID: p[4] || '', 频段: p[5] || '', 接入点: p[8] || '', 接口, 名称: p[3] || '' };
          window.__h3cDict ??= {}; window.__h3cDict[mac] ??= {}; window.__h3cDict[mac].iface = 接口;
          if (p[3] && p[3].trim()) window.__h3cDict[mac].name = window.__h3cDict[mac].name || p[3].trim();
        }
      });
      for (let mac in S.无线缓存) if (!新缓存[mac]) { 要重绘 = !0; if (window.__h3cDict?.[mac]) delete window.__h3cDict[mac].iface; }
      S.无线缓存 = 新缓存;
      if (要重绘) { window.gegeForceUIRedraw = !0; S.DOM缓存 = null; }
    } catch(e) { console.warn(e); }
    finally { S.无线请求中 = !1; }
  }

  async function 刷端口信息(ts = Date.now()) {
    if (S.端口请求中) return;
    S.端口请求中 = !0;
    try {
      let r = await fetch(`/maintain_basic.asp?basicTab=1&_=${ts}`);
      if (!r.ok) return;
      let t = GBK.decode(await r.arrayBuffer()), 新状态 = Object.create(null), re = /cur_port_status\[\d+\]\s*=\s*\[\s*['"]([^'"]+)['"]\s*,\s*['"]([^'"]+)['"]\s*\]/g, m;
      while ((m = re.exec(t))) {
        let 接口 = H3C接口(m[1]);
        if (!/^eth\d+$/i.test(接口)) continue;
        let 状态 = m[2], sm = 状态.match(/([\d.]+)\s*([GMK])/i), rate = 0;
        if (sm) { rate = +sm[1] || 0; let u = sm[2].toUpperCase(); if (u === 'G') rate *= 1000; else if (u === 'K') rate *= 0.001; }
        新状态[接口] = { rate, half: /半双工|HALF/i.test(状态) };
      }
      if (Object.keys(新状态).length) S.端口状态 = 新状态;
      S.端口已取 = Object.keys(新状态).length > 0;
    } catch(e) { console.warn(e); }
    finally { S.端口请求中 = !1; }
  }


  function 记总速率图(u, d) {
    u = u || 0; d = d || 0;
    let i = S.总图点数 & 8191;
    S.总上行图[i] = u; S.总下行图[i] = d; S.总图点数++;
    if (u > S.wMaxU) S.wMaxU = u; if (d > S.wMaxD) S.wMaxD = d;
    if (u > 0 && u < S.wMinU) S.wMinU = u; if (d > 0 && d < S.wMinD) S.wMinD = d;
  }
  function 初始化总速率图(bd) {
    let box = bd.querySelector('#gege-speed-chart');
    if (box) return box;
    box = document.createElement('div'); box.id = 'gege-speed-chart';
    box.innerHTML = '<canvas></canvas><div class="gege-chart-head"><b data-gc="meta"></b><span data-gc="range"></span></div><div class="gege-chart-foot"><span class="gc-up" data-gc="up"></span><span class="gc-down" data-gc="down"></span><span class="gc-extra" data-gc="extra"></span></div><div class="gege-chart-resize" title="缩放"></div>';
    bd.appendChild(box);
    let bw = bd.clientWidth || 1800, w = Math.max(320, Math.min(650, bw * .30));
    box.style.left = Math.max(520, bw * .52) + 'px'; box.style.top = '2px'; box.style.width = w + 'px'; box.style.height = '112px';
    const 起手 = (e, 模式) => { e.preventDefault(); S.图表拖 = { 模式, x: e.clientX, y: e.clientY, l: box.offsetLeft, t: box.offsetTop, w: box.offsetWidth, h: box.offsetHeight }; box.setPointerCapture?.(e.pointerId); };
    box.addEventListener('pointerdown', e => { if (!e.target.classList.contains('gege-chart-resize')) 起手(e, '拖'); });
    box.querySelector('.gege-chart-resize').addEventListener('pointerdown', e => 起手(e, '缩'));
    box.addEventListener('pointermove', e => {
      let g = S.图表拖; if (!g) return;
      if (g.模式 === '拖') { box.style.left = Math.max(330, g.l + e.clientX - g.x) + 'px'; box.style.top = Math.max(0, g.t + e.clientY - g.y) + 'px'; }
      else { box.style.width = Math.max(260, g.w + e.clientX - g.x) + 'px'; box.style.height = Math.max(88, g.h + e.clientY - g.y) + 'px'; }
      if (!S.图表待画) { S.图表待画 = 1; requestAnimationFrame(() => { S.图表待画 = 0; 画总速率图(bd); }); }
    });
    box.addEventListener('pointerup', () => S.图表拖 = null); box.addEventListener('pointercancel', () => S.图表拖 = null); box.addEventListener('lostpointercapture', () => S.图表拖 = null);
    return box;
  }
  function 画总速率图(bd) {
    let box = 初始化总速率图(bd), cv = box.querySelector('canvas'), W = box.clientWidth | 0, H = box.clientHeight | 0, R = window.devicePixelRatio || 1;
    if (W < 40 || H < 40) return;
    if (cv.width !== (W * R | 0) || cv.height !== (H * R | 0)) { cv.width = W * R | 0; cv.height = H * R | 0; cv.style.width = '100%'; cv.style.height = '100%'; }
    let x = cv.getContext('2d'); x.setTransform(R,0,0,R,0,0); x.clearRect(0,0,W,H);
    let l = 34, r = 8, t = 20, b = 25, gw = W - l - r, gh = H - t - b, n = Math.min(S.总图点数, 8000), st = S.总图点数 - n, ym = 1, su = 0, sd = 0;
    for (let k = 0; k < n; k++) { let j = (st + k) & 8191, u = S.总上行图[j], d = S.总下行图[j]; if (u > ym) ym = u; if (d > ym) ym = d; su += u; sd += d; }
    x.fillStyle = 'rgba(255,255,255,.58)'; x.strokeStyle = 'rgba(0,89,250,.88)'; x.lineWidth = 1.5; x.beginPath(); x.roundRect ? x.roundRect(.5,.5,W-1,H-1,8) : x.rect(.5,.5,W-1,H-1); x.fill(); x.stroke();
    x.setLineDash([4,4]); x.strokeStyle = 'rgba(0,89,250,.45)'; x.lineWidth = 1;
    for (let i = 5; i--; ) { let y = t + gh * i / 4; x.beginPath(); x.moveTo(l,y); x.lineTo(W-r,y); x.stroke(); }
    for (let i = 7; i--; ) { let xx = l + gw * i / 6; x.beginPath(); x.moveTo(xx,t); x.lineTo(xx,H-b); x.stroke(); }
    x.setLineDash([]);
    let li = (S.总图点数 - 1) & 8191, au = n ? (n === 8000 ? su * .000125 : su / n) : 0, ad = n ? (n === 8000 ? sd * .000125 : sd / n) : 0;
    (box._gcMeta ??= box.querySelector('[data-gc="meta"]')).textContent = `采样:${window.gegeBActivated ? CONFIG.lanRefreshInterval : 3}s  点:${S.总图点数}`;
    let rg = box._gcRange ??= box.querySelector('[data-gc="range"]'), rs = `峰↑${fBy(S.wMaxU)} ↓${fBy(S.wMaxD)}  谷↑${S.wMinU < Infinity ? fBy(S.wMinU) : '--'} ↓${S.wMinD < Infinity ? fBy(S.wMinD) : '--'}`; rg.textContent = rs; rg.title = rs;
    (box._gcUp ??= box.querySelector('[data-gc="up"]')).textContent = `发 ${n ? fBy(S.总上行图[li]) : fBy(0)}`;
    (box._gcDown ??= box.querySelector('[data-gc="down"]')).textContent = `收 ${n ? fBy(S.总下行图[li]) : fBy(0)}`;
    (box._gcExtra ??= box.querySelector('[data-gc="extra"]')).textContent = `均↑${fBy(au)} ↓${fBy(ad)}`;
    const 画线 = (arr, col) => {
      if (!n) return;
      x.strokeStyle = col; x.lineWidth = 2.4; x.beginPath();
      let bins = Math.max(1, Math.min(n, gw | 0)), first = !0;
      if (bins === n) {
        for (let k = 0; k < n; k++) { let xx = l + (n > 1 ? gw * k / (n - 1) : gw), yy = H - b - (arr[(st + k) & 8191] / ym) * gh; first ? (x.moveTo(xx, yy), first = !1) : x.lineTo(xx, yy); }
      } else {
        for (let q = 0; q < bins; q++) {
          let a = q * n / bins | 0, z = (q + 1) * n / bins | 0, mn = Infinity, mx = -Infinity, mi = a, ma = a; if (z <= a) z = a + 1;
          for (let k = a; k < z; k++) { let v = arr[(st + k) & 8191]; if (v < mn) { mn = v; mi = k; } if (v > mx) { mx = v; ma = k; } }
          if (mi <= ma) { let xx = l + (n > 1 ? gw * mi / (n - 1) : gw), yy = H - b - (mn / ym) * gh; first ? (x.moveTo(xx, yy), first = !1) : x.lineTo(xx, yy); if (ma !== mi) { xx = l + gw * ma / (n - 1); yy = H - b - (mx / ym) * gh; x.lineTo(xx, yy); } }
          else { let xx = l + gw * ma / (n - 1), yy = H - b - (mx / ym) * gh; first ? (x.moveTo(xx, yy), first = !1) : x.lineTo(xx, yy); xx = l + gw * mi / (n - 1); yy = H - b - (mn / ym) * gh; x.lineTo(xx, yy); }
        }
      }
      x.stroke();
    };
    画线(S.总上行图, '#ff1b00'); 画线(S.总下行图, '#006400');
  }

  const st = document.createElement('style');
  st.innerHTML = `.config-item{
clear:both;}.config-item-box{display:flex!important;
        align-items:stretch!important;padding-bottom:
        4px!important;}.config-item .logo{width:33%!important;
        float:none!important;display:flex!important;flex-direction:row;}.config-item .dev-intro{flex:1;display:flex!important;flex-direction:column;justify-content:flex-start;min-height:20px;padding-bottom:0!important;margin-bottom:0!important;}.config-item .info{width:27%!important;float:none!important;display:flex!important;flex-direction:column;justify-content:flex-start;padding:0 10px!important;border-right:1px solid #eee;}.config-item .speed{width:40%!important;float:none!important;display:flex!important;flex-direction:column;justify-content:center;padding:0 10px!important;}.geek-row{display:flex;justify-content:space-between;align-items:center;white-space:nowrap;height:20px;}
    .geek-label{width:110px;color:#333;font-weight:bold;}.geek-val-box{flex:1;display:flex;gap:15px;margin-left:10px;}.geek-fixed-width{display:inline-block;width:120px;}.geek-right-box{text-align:right;min-width:220px;font-weight:bold;}.c-up{color:#ff4c00;}.c-down{color:#0059fa;}.gege-up-box,.gege-down-box{margin-top:auto!important;margin-bottom:0!important;width:95%;}.gege-ratio-box{margin-top:10px;width:95%;margin-bottom:5px;}.t-row{font-size:12px;font-weight:bold;margin-bottom:2px;display:flex;justify-content:space-between;font-family:system-ui, sans-serif;}.zte-thin-bar{width:100%;height:3px;background:rgba(0,0,0,0.05);border-radius:1.5px;overflow:hidden;}.zte-thin-bar-inner{height:100%;transition:width 0.5s ease-out;}.zte-thin-bar-inner.up{background:#ff4c00;}.zte-thin-bar-inner.down{background:#0059fa;}.gege-ratio-top{display:flex;justify-content:space-between;font-size:12px;font-weight:bold;margin-bottom:2px;}.gege-ratio-bar{width:100%;height:4px;background:#0059fa;border-radius:2px;overflow:hidden;}.gege-ratio-bar-inner{height:100%;background:#ff4c00;transition:width 0.5s ease-out;}.zte-enhance-speed{display:flex;flex-direction:column;gap:6px;width:100%;font-family:system-ui, sans-serif;}
    .zte-bar-wrap{position:relative;width:100%;border-radius:4px;border:1px solid;font-size:13px;font-weight:bold;overflow:hidden;padding:3px 8px;display:flex;justify-content:space-between;align-items:center;z-index:1;box-sizing:border-box;}.zte-bar-wrap span{font-size:inherit;font-weight:inherit;}.zte-bar-up{color:#ff4c00;border-color:rgba(255,76,0,0.3);}.zte-bar-down{color:#0059fa;border-color:rgba(0,89,250,0.3);}.zte-bar-up::before{content:'';position:absolute;left:0;top:0;bottom:0;z-index:-1;background:rgba(255,76,0,0.12);width:var(--p-up,0%);transition:width 0.5s;}.zte-bar-down::before{content:'';position:absolute;left:0;top:0;bottom:0;z-index:-1;background:rgba(0,89,250,0.12);width:var(--p-down,0%);transition:width 0.5s;}#config-list.gege-list-container{contain:content!important;background-color:#ffffff!important;border-radius:8px!important;border:1px solid #e0e0e0!important;padding:20px 30px!important;box-shadow:0 2px 10px rgba(0,0,0,0.02)!important;margin-top:10px!important;}.gege-section{margin-bottom:10px;}
    .gege-section:last-child{margin-bottom:0;}.gege-list-container .config-title{font-size:16px!important;font-weight:bold!important;color:#333!important;margin:15px 0 10px 0!important;padding-bottom:5px!important;}.gege-list-container .gege-section:first-child .config-title{margin-top:0!important;}.gege-empty-state{color:#999!important;font-size:14px!important;padding:0 0 15px 5px!important;border-bottom:1px solid #f0f0f0!important;margin-bottom:5px!important;}.gege-list-item{background-color:transparent!important;border-bottom:1px solid #f0f0f0!important;padding:6px 10px!important;margin-bottom:0!important;border-radius:0!important;}
    .gege-list-item:last-child{border-bottom:none!important;}#zte-geek-board{contain:content;background-color:transparent!important;border-left:4px solid #0059fa!important;border-radius:0!important;padding:5px 0 5px 15px!important;margin:10px 0 15px 0!important;box-shadow:none!important;border-bottom:1px solid #f0f0f0!important;font-size:14px;display:flex;flex-direction:column;gap:6px;padding-bottom:15px!important;}#gege-global-overlay #zte-geek-board.geek-frozen-pane{position:sticky!important;top:0px!important;z-index:100!important;background-color:#f3f4f5!important;margin-top:0!important;padding-top:15px!important;box-shadow:0 10px 15px -3px rgba(0,0,0,0.05)!important;border-radius:0 0 8px 8px!important;}.gege-pin{cursor:pointer;font-size:11px;filter:grayscale(100%);opacity:0.5;transition:transform 0.2s;margin-left:2px;}
    .gege-pin.active{filter:none;opacity:1;transform:scale(1.1);}#gege-global-overlay{position:fixed;top:7.5%;right:0;bottom:0;background:#f3f4f5;z-index:9999;overflow-y:auto;padding-bottom:50px;left:0!important;border-radius:16px 16px 0 0;box-shadow:0 -5px 25px rgba(0,0,0,0.15);transition:top 0.3s ease;}@media (max-width: 768px){.geek-right-box:has(#gb-wan-zero-up),.geek-right-box:has(#gb-cur-up-vol){display:none!important}.gege-list-item{padding:12px 10px!important;position:relative!important}.config-item-box{flex-direction:column!important;padding-bottom:0!important}.config-item .info,.config-item .logo,.config-item .speed{width:100%!important;border:none!important;padding:0!important;position:static!important}.config-item .dev-intro{min-height:auto!important;justify-content:center!important;padding-right:90px!important}.config-item .logo{padding-bottom:4px!important}.config-item .info{flex-direction:column!important;margin:0 0 6px 0!important;gap:2px!important}.dev-ip{position:absolute!important;top:12px!important;right:10px!important;font-size:11px!important;background:rgba(0,89,250,0.08);color:#0059fa!important;padding:2px 6px!important;border-radius:4px;font-weight:bold;line-height:1.2;z-index:10;width:auto!important}.dev-number{width:auto!important;margin:0!important;font-size:11px!important}.gege-ratio-box{width:100%!important;margin-top:2px!important;margin-bottom:0!important}.gege-down-box{width:100%!important;margin-top:2px!important}#zte-geek-board{padding:8px!important;gap:0!important;font-size:11.5px!important}.geek-row{height:auto!important;flex-wrap:wrap!important;margin-bottom:4px!important;justify-content:flex-start!important;gap:2px 6px!important;line-height:1.3!important}.geek-label{width:auto!important;min-width:60px!important;font-size:11.5px!important;flex:0 0 auto!important}.geek-val-box{width:auto!important;flex:1 1 0%!important;display:flex!important;flex-wrap:wrap!important;margin-left:0!important;gap:2px 6px!important}.geek-fixed-width{width:auto!important}.geek-right-box{width:100%!important;flex:0 0 100%!important;text-align:left!important;font-size:11.5px!important;margin-top:2px!important;margin-left:0!important}.gege-list-container{padding:8px!important}.zte-enhance-speed{gap:4px!important}} .gege-pkt-line{color:#666;font-size:11.5px;font-family:system-ui, sans-serif;line-height:1.35;white-space:normal}.dev-title-line{display:flex;align-items:center;justify-content:space-between;gap:10px;width:95%;}.dev-title-line .dev-name-text{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}.gege-rssi{font-size:11.5px;font-weight:normal;color:#666;margin-left:2em;white-space:nowrap;flex-shrink:0;}#zte-geek-board{position:relative!important;overflow:visible!important;}#gege-speed-chart{position:absolute;z-index:25;box-sizing:border-box;cursor:move;user-select:none;touch-action:none;container-type:inline-size;}#gege-speed-chart canvas{display:block;width:100%;height:100%;}.gege-chart-head,.gege-chart-foot{position:absolute;left:clamp(28px,8%,36px);right:8px;display:flex;align-items:center;pointer-events:none;font:bold clamp(9px,2.1cqw,11px) system-ui,sans-serif;white-space:nowrap;overflow:hidden;}.gege-chart-head{top:2px;color:#111;gap:8px;}.gege-chart-head [data-gc="range"]{color:#666;font-weight:normal;overflow:hidden;text-overflow:ellipsis;margin-left:auto;}.gege-chart-foot{bottom:4px;justify-content:flex-start;gap:clamp(3px,1.1cqw,12px);}.gege-chart-foot span{min-width:0;overflow:hidden;text-overflow:ellipsis;}.gc-up{color:#ff4c00;flex:0 1 auto;}.gc-down{color:#0b5;flex:0 1 auto;}.gc-extra{color:#666;text-align:right;margin-left:auto;flex:1 1 0;min-width:0;}#gege-speed-chart .gege-chart-resize{position:absolute;right:-4px;bottom:-4px;width:13px;height:13px;border-right:3px solid #0059fa;border-bottom:3px solid #0059fa;cursor:nwse-resize;border-radius:2px;}@media (max-width: 768px){#gege-speed-chart{left:210px!important;width:calc(100% - 220px)!important;height:96px!important;}}`;
  document.
  head.
  appendChild(st);
  window.gegeRenderedMacs = new Set();
async function rSD() {
    if (window.__gIsF) return;
    window.__gIsF = !0;
    try {
const ts = Date.now();
      let resW, resL, tW = "", tL = "", wanNow, lanNow;
      
      try { resW = await fetch(`/flow_polling.asp?_=${ts}`); if(resW.ok) tW = GBK.decode(await resW.arrayBuffer()); } catch(e){console.warn(e)}
      wanNow = performance.now();
      try { resL = await fetch(`/ip_statistics.asp?_=${ts}`); if(resL.ok) tL = GBK.decode(await resL.arrayBuffer()); } catch(e){console.warn(e)}
      lanNow = performance.now();

      let wB_m = tW.match(/wan_bytes\[0\]\s*=\s*\[(\d+),\s*(\d+)\]/);
      let wM_m = tW.match(/wan_ms\[0\]\s*=\s*\[(\d+),\s*(\d+)\]/);
      let hasWanBytes = !!wB_m, hasWanRate = !!wM_m;
      let cWU = S.wInstUp, cWD = S.wInstDn;
      if (hasWanRate) {
        cWU = (+wM_m[1] || 0) * 8;
        cWD = (+wM_m[2] || 0) * 8;
        记总速率图(cWU, cWD);
      }

      // 官方累计与高精积分响应无效处理
      if (hasWanBytes) {
        S.oWU = (+wB_m[1] || 0) * 8; S.oWD = (+wB_m[2] || 0) * 8;
        if (S.bWU === undefined) S.bWU = S.oWU;
        if (S.bWD === undefined) S.bWD = S.oWD;
        if (S.oWU < S.lRU) S.bWU = S.lRU = S.oWU;
        if (S.oWD < S.lRD) S.bWD = S.lRD = S.oWD;
        S.lRU = S.oWU; S.lRD = S.oWD;
        S.dTU = Math.max(0, S.oWU - S.bWU);
        S.dTD = Math.max(0, S.oWD - S.bWD);
      }

      S.hasW2 = !1; 

      let cSU = 0, cSD = 0, cI = Object.create(null);
      
// 1. 轻量级快车道：每次必跑，提取 ARP 映射，并生成当前全网 MAC 哈希
      let aL_m = tL.match(/var\s+arp_list\s*=\s*new\s+Array\(([\s\S]*?)\);/), lanValid = !!aL_m;
      let arp = {}, curMacs = [];
      if (aL_m) {
          aL_m[1].split(',').forEach(s => {
              let p = s.replace(/["'\n\r]/g, '').split(';');
              if(p[0] && p[1]) {
                  let m = nM(p[1]);
                  arp[p[0].trim()] = m;
                  curMacs.push(m);
              }
          });
      }
      curMacs.sort();
      let macHash = curMacs.join('|');
      
      window.__h3cDict ??= {};
      
      // 2. 重量级慢车道：仅当设备指纹发生变化时触发
      if (S.lMacHash !== macHash) {
          S.lMacHash = macHash;
          let D = window.__h3cDict;
          
          // 官方 getUserNameByIp 的顺序：ARP 定位 MAC -> 家长控制 net_mac_list -> WiFi 接入控制 wifi_mac_list -> DHCP 客户端表
          取名单项(tL, 'net_mac_list').forEach(s => {
              let p = s.slice(1, -1).split(';'), mac = nM(p[1]);
              if (!mac) return;
              D[mac] ??= {};
              写设备名(D, mac, p[3] || p[2], 1);
              let 接口 = H3C接口(p[6] || p[5]);
              if (接口) D[mac].iface = 接口;
          });
          取名单项(tL, 'wifi_mac_list').forEach(s => {
              let p = s.slice(1, -1).split(';'), mac = nM(p[1]);
              if (!mac) return;
              D[mac] ??= {};
              写设备名(D, mac, p[3] || p[2]);
              let 接口 = H3C接口(p[6] || p[5]);
              if (接口 && !D[mac].iface) D[mac].iface = 接口;
          });
          取名单项(tL, 'dhcpd_client_list').forEach(s => {
              let p = s.slice(1, -1).split(';'), mac = nM(p[0]);
              if (!mac) return;
              D[mac] ??= {};
              写设备名(D, mac, p[2]);
          });
          

      }

      // 3. 回归快车道：纯数字提取与字典命中
      let iS_re = /ip_stat\[\d+\]\s*=\s*\[(.*?)\];/g;
      let m;
      while ((m = iS_re.exec(tL)) !== null) {
          let p = m[1].replace(/["']/g, '').split(',');
          let ip = p[0].trim();
          let mac = arp[ip];
          if (mac) {
              let u = parseFloat(p[6]) * 1000 || 0;
              let dn = parseFloat(p[8]) * 1000 || 0;
              let dict = window.__h3cDict[mac] || {}, 无线 = S.无线缓存[mac] || {};
              cI[mac] = {
                  upRate: u, dnRate: dn, 包上: +p[5] || 0, 包下: +p[7] || 0,
                  iface: 无线.接口 || dict.iface || 'eth1', // 没有在 WiFi 列表里出现的，默认分配为有线
                  offUp: 0, offDn: 0,
                  onSec: 0,
                  name: dict.name || 无线.名称 || "未知设备",
                  ip: ip || "", 信号: 无线.信号 || 0, 协商: 无线.协商 || 0, SSID: 无线.SSID || "", 频段: 无线.频段 || ""
              };
              S.包数据可用 = S.包数据可用 || cI[mac].包上 > 0 || cI[mac].包下 > 0;
              cSU += u; cSD += dn;
          }
      }

      if (lanValid) S.lastCI = cI;
      else cI = S.lastCI || Object.create(null);
      let ol = document.getElementById('gege-global-overlay'), cM = Object.keys(cI), iD = lanValid && (window.gegeForceUIRedraw || (cM.length !== window.gegeRenderedMacs.size));
      if (!iD && cM.length > 0) { for (let i = 0; i < cM.length; i++) { if (!window.gegeRenderedMacs.has(cM[i])) { iD = !0; break; } } }
      if (iD) for (let m in S.cls) if (!cI[m]) {
        let cS = S.cls[m], ms = Math.min(lanNow - cS.lUT, CONFIG.lanRefreshInterval * 1000);
        if (cS.upR > 0) cS.intUp += cS.upR * ms * 0.0005;
        if (cS.dnR > 0) cS.intDn += cS.dnR * ms * 0.0005;
        if (cS.上次包上 > 0) cS.包上总 += cS.上次包上 * ms * 0.0005;
        if (cS.上次包下 > 0) cS.包下总 += cS.上次包下 * ms * 0.0005;
        cS.upR = cS.dnR = cS.上次包上 = cS.上次包下 = 0; cS.lUT = lanNow;
      }
      if (ol && ol.style.display === 'block' && (iD || !ol.querySelector('.gege-list-item'))) {
        bVD(ol, cI); window.gegeRenderedMacs = new Set(cM); window.gegeForceUIRedraw = !1;
      }
      let gDt = (S.lt !== 0) ? (lanNow - S.lt) * 0.001 : 0;
      if (hasWanRate && S.wLT === undefined) {
        S.wLT = wanNow;
      }
      else if (hasWanRate && (cWU !== S.wInstUp || cWD !== S.wInstDn)) {
        let wDt = wanNow - S.wLT;
        if (S.wInstUp > 0) { S.wTotUp += (S.wInstUp + cWU) * wDt * 0.0005; }
        else if (cWU > 0) { let wEU = cWU * 0.5 * CONFIG.wanRefreshInterval; S.wTotUp += wEU; S.wZEU = (S.wZEU || 0) + wEU; S.wZEUC = (S.wZEUC || 0) + 1; }
        if (S.wInstDn > 0) { S.wTotDn += (S.wInstDn + cWD) * wDt * 0.0005; }
        else if (cWD > 0) { let wED = cWD * 0.5 * CONFIG.wanRefreshInterval; S.wTotDn += wED; S.wZED = (S.wZED || 0) + wED; S.wZEDC = (S.wZEDC || 0) + 1; }
        S.wLT = wanNow;
      }
      let 组刷新 = new Set;
      for (const m in cI) {
        let cC = cI[m], 新设备 = !S.cls[m];
        S.cls[m] ??= {
          upR: cC.upRate, dnR: cC.dnRate, lUT: lanNow,
          intUp: _saved?.devices?.[m]?.integral_up || 0,
          intDn: _saved?.devices?.[m]?.integral_down || 0,
          包上总: _saved?.devices?.[m]?.packet_up || 0, 包下总: _saved?.devices?.[m]?.packet_down || 0, 上次包上: cC.包上, 上次包下: cC.包下,
          onS: cC.onSec, lOS: cC.onSec, name: _saved?.devices?.[m]?.name || cC.name || m, hU: new Float64Array(32), hD: new Float64Array(32), hIdx: 0
        };
        let cS = S.cls[m],
          dU = cC.offUp - cS.lU,
          dD = cC.offDn - cS.lD;
        if (cC.name && cC.name !== '未知设备') cS.name = cC.name;
        if (dU < 0 || dD < 0) {
          if (dU < 0) {
            cS.uB += dU;
            cS.dpU = cS.lU;
          }
          if (dD < 0) {
            cS.dB += dD;
            cS.dpD = cS.lD;
          }
          cS.aR = !0;
        }
        else if (cS.aR) {
          if (dD > 2516582400 || dU > 671088640 || (cS.dpD && dD >= cS.dpD) || (cS.dpU && dU >= cS.dpU)) {
            cS.uB += dU;
            cS.dB += dD;
            cS.aR = !1;
            cS.dpU = 0;
            cS.dpD = 0;
          }
        }
        if (cS.lOS !== cC.onSec) {
          cS.onS = cC.onSec;
          cS.lOS = cC.onSec;
        }
        else {
          cS.onS = (cS.onS || cC.onSec || 0) + gDt;
        }
        if (!新设备 && !cS.aR && (cC.upRate !== cS.upR || cC.dnRate !== cS.dnR || cC.包上 !== cS.上次包上 || cC.包下 !== cS.上次包下)) 组刷新.add(cC.iface);
        cS.lU = cC.offUp;
        cS.lD = cC.offDn;
      }
      for (const m in cI) {
        let cC = cI[m], cS = S.cls[m], 自身刷新 = cC.upRate !== cS.upR || cC.dnRate !== cS.dnR || cC.包上 !== cS.上次包上 || cC.包下 !== cS.上次包下;
        if (自身刷新 || (!cS.aR && 组刷新.has(cC.iface))) {
          let ms = lanNow - cS.lUT;
          if (cS.upR > 0) { cS.intUp += (cS.upR + cC.upRate) * ms * 0.0005; }
          else if (cC.upRate > 0) { let eU = cC.upRate * CONFIG.lanRefreshInterval * 0.5; cS.intUp += eU; cS.zEU = (cS.zEU || 0) + eU; cS.zUC = (cS.zUC || 0) + 1; }
          if (cS.dnR > 0) { cS.intDn += (cS.dnR + cC.dnRate) * ms * 0.0005; }
          else if (cC.dnRate > 0) { let eD = cC.dnRate * CONFIG.lanRefreshInterval * 0.5; cS.intDn += eD; cS.zED = (cS.zED || 0) + eD; cS.zDC = (cS.zDC || 0) + 1; }
          if (cS.上次包上 > 0) cS.包上总 += (cS.上次包上 + cC.包上) * ms * 0.0005;
          else if (cC.包上 > 0) cS.包上总 += cC.包上 * CONFIG.lanRefreshInterval * 0.5;
          if (cS.上次包下 > 0) cS.包下总 += (cS.上次包下 + cC.包下) * ms * 0.0005;
          else if (cC.包下 > 0) cS.包下总 += cC.包下 * CONFIG.lanRefreshInterval * 0.5;
          cS.upR = cC.upRate;
          cS.dnR = cC.dnRate;
          cS.上次包上 = cC.包上;
          cS.上次包下 = cC.包下;
          cS.lUT = lanNow;
        }
        cS.hIdx = (cS.hIdx + 1) & 31;
        cS.hU[cS.hIdx] = cC.upRate;
        cS.hD[cS.hIdx] = cC.dnRate;
      }
      S.lt = lanNow;
      if (hasWanRate) { S.wInstUp = cWU; S.wInstDn = cWD; }
      rUI(cWU, cWD, cSU, cSD, cI);
    }
    catch (e) {
      console.error("[哥哥科技] 周期采样中断:", e);
    }
    finally {
      window.__gIsF = !1;
    }
  }
const calcStageRatio = (W, L_int, L_hp) => {
    if (W === 0) return 1.0;
    let L_max = Math.max(L_int, L_hp);
    let L_min = Math.min(L_int, L_hp);
    let Gap = Math.abs(L_int - L_hp);
    if (L_int > 0.84 * W && L_hp > 0.75 * W && (L_max < 1.5 * W || Gap < 0.6 * W)) {
        return ((L_int + L_hp) / (2 * W));
    } else if (L_min < W && W < L_max && L_max < 1.5 * W) {
        return L_max / W;
    } else {
        return (Math.abs(L_int - W) < Math.abs(L_hp - W) ? L_int : L_hp) / W;
    }
  };
  function rUI(wU, wD, sU, sD, cI) {
    S.端口小齿轮 = ((S.端口小齿轮 || 0) + 1) & 63;
    if (S.端口小齿轮 === 1 || !S.端口已取) 刷端口信息(Date.now());
    S.半双工色轮 = ((S.半双工色轮 || 0) + 1) & 7;
    let LUp = 0, LDn = 0, hpU = 0, hpD = 0, curHpU = 0, curHpD = 0, 包上总 = 0, 包下总 = 0;
    for (let k in S.cls) {
      let s = S.cls[k];
      let cC = cI[k];
      let cU = s.intUp || 0;
      let cD = s.intDn || 0;
      let sessU = cU;
      let sessD = cD;
      LUp += s.intUp || 0;
      LDn += s.intDn || 0;
      hpU += sessU; 
      hpD += sessD;
      包上总 += s.包上总 || 0;
      包下总 += s.包下总 || 0;
      if (cC) {
        curHpU += sessU;
        curHpD += sessD;
      }
    }
    if (typeof GM_setValue !== 'undefined' && !((S.HA小齿轮 = ((S.HA小齿轮 || 0) + 1) & 1023))) {
      try {
        let cln = {};
        for (let k in S.cls) { let s = S.cls[k], cC = cI[k]; cln[k] = { up: s.intUp || 0, down: s.intDn || 0, integral_up: s.intUp || 0, integral_down: s.intDn || 0, packet_up: s.包上总 || 0, packet_down: s.包下总 || 0, status: cC ? (CONFIG.portMap[cC.iface] || cC.iface || "未知接口") : "off", name: cC?.name || s.name || k, ip: cC?.ip || "", raw_up: cC?.offUp || 0, raw_down: cC?.offDn || 0 }; }
        GM_setValue('ha_snapshot', {
          timestamp: Date.now(),
          global: {
            wan_up: S.wTotUp,
            wan_down: S.wTotDn,
            lan_integral_up: LUp,
            lan_integral_down: LDn,
            lan_high_up: hpU,
            lan_high_down: hpD,
            lan_packet_up: 包上总,
            lan_packet_down: 包下总,
          },
          devices: cln
        });
      } catch(e) {console.warn(e);}
    }
     S.rTick = ((S.rTick || 0) + 1) & 7;
    if (S.rTick === 1 || !S.cRT) {
        刷无线信息(Date.now());
        S.aWu = (S.wTotUp - (S.lwTU || S.wTotUp)) / (CONFIG.wanRefreshInterval << 3); S.lwTU = S.wTotUp;
        S.aWd = (S.wTotDn - (S.lwTD || S.wTotDn)) / (CONFIG.wanRefreshInterval << 3); S.lwTD = S.wTotDn;
        if (S.hasW2) {
            let rU = S.w2TotUp > 0 ? (S.wTotUp / S.w2TotUp) : (S.wTotUp > 0 ? Infinity : 0), rD = S.w2TotDn > 0 ? (S.wTotDn / S.w2TotDn) : (S.wTotDn > 0 ? Infinity : 0);
            let fR = (r) => r === Infinity ? '∞' : (r > 1 ? r.toFixed(2) + 'x' : (r * 100).toPrecision(3) + '%');
            S.cRT = `<span style="font-weight: bold;"><span style="color:#9c27b0;">${fR(rU)}</span>，<span style="color:#4caf50;">${fR(rD)}</span></span>`;
        } else {
            let rUp = calcStageRatio(S.wTotUp, LUp, hpU), rDn = calcStageRatio(S.wTotDn, LDn, hpD);
            S.cRT = `<span style="font-weight: bold;"><span style="color: ${rUp > 1.5 ? '#9c27b0' : (rUp > 1.15 ? '#FF9800' : '#4CAF50')};">${(rUp * 100).toFixed(2)}%</span>，<span style="color: ${rDn > 1.5 ? '#9c27b0' : (rDn > 1.15 ? '#FF9800' : '#4CAF50')};">${(rDn * 100).toFixed(2)}%</span></span>`;
        }
    }
    let bd = document.getElementById('zte-geek-board');
    if (!bd) {
      bd = document.createElement('div');
      bd.id = 'zte-geek-board';
 let layoutHtml = '';
if (CONFIG.uiLayout === 1) { // 紧凑版 (驾驶舱)
            layoutHtml = `
                <div class="geek-row"><span class="geek-label">WAN口速率</span><div class="geek-val-box" style="position:relative;"><span class="c-up geek-fixed-width" id="gb-wan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-wan-down-bytes"></span><span style="font-weight: normal; margin-left: 5px;"><span class="c-up" id="gb-wan-up-bps"></span> | <span class="c-down" id="gb-wan-down-bps"></span></span><span id="gb-pwan-vol-container" style="display:none; position:absolute; left:clamp(450px, 60%, 700px); color:#333; font-weight:bold; white-space:nowrap;">副WAN总：<span class="c-up" id="gb-pwan-tot-up"></span> | <span class="c-down" id="gb-pwan-tot-down"></span></span></div><div class="geek-right-box" style="font-weight: normal; color: #666;" id="gb-wpeak-avg">峰值获取中...</div></div>
                <div class="geek-row"><span class="geek-label">局域网代数和</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-lan-down-bytes"></span><span style="font-weight: normal; margin-left: 5px;"><span class="c-up" id="gb-lan-up-bps"></span> | <span class="c-down" id="gb-lan-down-bps"></span></span></div><div class="geek-right-box">实时占比：<span class="c-up" id="gb-perc-up"></span> | <span class="c-down" id="gb-perc-down"></span></div></div>
                <div class="geek-row"><span class="geek-label">WAN总计：</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-wan-up-vol" title="高精积分存档"></span><span class="c-down geek-fixed-width" id="gb-wan-down-vol" title="高精积分存档"></span><span style="font-weight: bold; margin-left: 5px;" title="本次打开网页后的官方差值">本次官方：<span class="c-up" id="gb-sowan-up-vol"></span> | <span class="c-down" id="gb-sowan-down-vol"></span></span></div><div class="geek-right-box"><span style="font-weight: normal;">总官方：</span><span class="c-up" id="gb-owan-up-vol"></span> | <span class="c-down" id="gb-owan-down-vol"></span></div></div>
                <div class="geek-row"><span class="geek-label">LAN：<span id="gege-pin-btn" class="gege-pin" title="冻结窗格">📌</span></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-lan-down-vol"></span><span id="gb-packet-line" style="font-weight:bold;margin-left:5px;white-space:nowrap;"></span><span style="font-weight: normal; margin-left: 5px; color:#666;">高精统计：<span class="c-up" id="gb-int-up-vol"></span> | <span class="c-down" id="gb-int-down-vol"></span></span></div><div class="geek-right-box">${S.hasW2?'主次网比':'内外网比'}：<span id="gb-ratio-display"></span></div></div>`;
        } else if (CONFIG.uiLayout === 2) { // 平铺版 (报表流)
            layoutHtml = `
                <div class="geek-row"><span class="geek-label">WAN口速率</span><div class="geek-val-box" style="position:relative;"><span class="c-up geek-fixed-width" id="gb-wan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-wan-down-bytes"></span><span style="font-weight: normal; margin-left: 5px;"><span class="c-up" id="gb-wan-up-bps"></span> | <span class="c-down" id="gb-wan-down-bps"></span></span><span id="gb-pwan-vol-container" style="display:none; position:absolute; left:clamp(450px, 60%, 700px); color:#333; font-weight:bold; white-space:nowrap;">副WAN总：<span class="c-up" id="gb-pwan-tot-up"></span> | <span class="c-down" id="gb-pwan-tot-down"></span></span></div><div class="geek-right-box" style="font-weight: normal; color: #666;" id="gb-wpeak-avg">峰值获取中...</div></div>
                <div class="geek-row"><span class="geek-label">局域网代数和</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-lan-down-bytes"></span><span style="font-weight: normal; margin-left: 5px;"><span class="c-up" id="gb-lan-up-bps"></span> | <span class="c-down" id="gb-lan-down-bps"></span></span></div><div class="geek-right-box">实时占比：<span class="c-up" id="gb-perc-up"></span> | <span class="c-down" id="gb-perc-down"></span></div></div>
                <div class="geek-row"><span class="geek-label">WAN总计：</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-wan-up-vol" title="高精积分存档"></span><span class="c-down geek-fixed-width" id="gb-wan-down-vol" title="高精积分存档"></span><span style="font-weight: bold; margin-left: 5px;" title="本次打开网页后的官方差值">本次官方：<span class="c-up" id="gb-sowan-up-vol"></span> | <span class="c-down" id="gb-sowan-down-vol"></span></span></div><div class="geek-right-box"><span style="font-weight: normal;">总官方：</span><span class="c-up" id="gb-owan-up-vol"></span> | <span class="c-down" id="gb-owan-down-vol"></span></div></div>
                <div class="geek-row"><span class="geek-label">LAN：<span id="gege-pin-btn" class="gege-pin" title="冻结窗格">📌</span></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-lan-down-vol"></span><span id="gb-packet-line" style="font-weight:bold;margin-left:5px;white-space:nowrap;"></span></div><div class="geek-right-box"><span style="font-weight: normal;">${S.hasW2?'主次网比':'内外网比'}：</span><span id="gb-ratio-display"></span></div></div>`;
        } else { // 经典版 (0)
            layoutHtml = `
                <div class="geek-row"><span class="geek-label">WAN口速率</span><div class="geek-val-box" style="position:relative;"><span class="c-up geek-fixed-width" id="gb-wan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-wan-down-bytes"></span><span style="font-weight: normal; margin-left: 5px;"><span class="c-up" id="gb-wan-up-bps"></span> | <span class="c-down" id="gb-wan-down-bps"></span></span><span id="gb-pwan-vol-container" style="display:none; position:absolute; left:clamp(450px, 60%, 700px); color:#333; font-weight:bold; white-space:nowrap;">副WAN总：<span class="c-up" id="gb-pwan-tot-up"></span> | <span class="c-down" id="gb-pwan-tot-down"></span></span></div><div class="geek-right-box" style="font-weight: normal; color: #666;" id="gb-wpeak-avg">峰值获取中...</div></div>
                <div class="geek-row"><span class="geek-label">局域网代数和</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-lan-down-bytes"></span><span style="font-weight: normal; margin-left: 5px;"><span class="c-up" id="gb-lan-up-bps"></span> | <span class="c-down" id="gb-lan-down-bps"></span></span></div><div class="geek-right-box">实时占比：<span class="c-up" id="gb-perc-up"></span> | <span class="c-down" id="gb-perc-down"></span></div></div>
                <div class="geek-row"><span class="geek-label">WAN总计：</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-wan-up-vol" title="高精积分存档"></span><span class="c-down geek-fixed-width" id="gb-wan-down-vol" title="高精积分存档"></span></div><div class="geek-right-box"><span style="font-weight: normal;">总官方：</span><span class="c-up" id="gb-owan-up-vol"></span> | <span class="c-down" id="gb-owan-down-vol"></span></div></div>
                <div class="geek-row"><span class="geek-label">LAN：<span id="gege-pin-btn" class="gege-pin" title="冻结窗格">📌</span></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-lan-down-vol"></span><span id="gb-packet-line" style="font-weight:bold;margin-left:5px;white-space:nowrap;"></span></div><div class="geek-right-box"></div></div>`;
        }
        bd.innerHTML = layoutHtml;
        let pinBtn = bd.querySelector('#gege-pin-btn');
        if (pinBtn) {
            if (S.isPinned) {
                bd.classList.add('geek-frozen-pane');
                pinBtn.classList.add('active');
            }
            pinBtn.onclick = () => {
                S.isPinned = !S.isPinned;
                bd.classList.toggle('geek-frozen-pane', S.isPinned);
                pinBtn.classList.toggle('active', S.isPinned);
            };
        }
    }
    let ol = document.getElementById('gege-global-overlay'),
      iPO = ol && ol.style.display === 'block',
      aC = iPO ? ol : document;
    requestAnimationFrame(() => {
    if (iPO) {
      let ac = document.getElementById('gege-board-anchor');
      if (ac && bd.nextSibling !== ac) ac.parentNode.insertBefore(bd, ac);
    }
    else {
      let mn = document.querySelector('.el-table') || document.querySelector('.config-item')?.closest('div') || document.querySelector('.main-content');
      if (mn && bd.parentNode !== mn.parentNode) mn.parentNode.insertBefore(bd, mn);
    }
  
    let 面板状态 = iPO ? '悬浮' : '原生', oDC = S.DOM缓存;
    if (!oDC || S.DOM已重建 || S.面板状态 !== 面板状态) {
      oDC = Object.create(null);
      if (!iPO) {
        const M_RX = /([a-fA-F0-9]{2}[:-]){5}[a-fA-F0-9]{2}/;
        let aI = aC.querySelectorAll('.config-item');
        for (let n of aI) {
          let mN = n.querySelector('.dev-number'),
            mM = mN ? mN.textContent.match(M_RX) : null;
          if (mM) oDC[mM[0].toLowerCase().replace(/-/g, ':')] = n;
        }
      }
      else {
        let gI = aC.querySelectorAll('.gege-list-item');
        for (let n of gI) {
          let m = n.getAttribute('data-gege-mac');
          if (m) oDC[m] = n;
        }
      }
      S.DOM缓存 = oDC; S.DOM已重建 = !1; S.面板状态 = 面板状态;
    }
            if (bd.parentNode) {
        const setText = (id, text) => { const el = bd.querySelector(id); if (el) el.textContent = text; };
        const setHTML = (id, html) => { const el = bd.querySelector(id); if (el) el.innerHTML = html; };
        let aW2U = S.hasW2 ? S.w2U : undefined, aW2D = S.hasW2 ? S.w2D : undefined, aW2TU = S.hasW2 ? S.w2TotUp : undefined, aW2TD = S.hasW2 ? S.w2TotDn : undefined;
        if (!S.runT) S.runT = performance.now();
        let dtR = (performance.now() - S.runT) / 1000;
        let aWU = dtR > 0 ? (S.dTU / dtR) : 0;
        let aWD = dtR > 0 ? (S.dTD / dtR) : 0;

        setText('#gb-wan-up-bytes', `🔼 ${fBy(wU + (aW2U||0))}`);
        setText('#gb-wan-down-bytes', `🔽 ${fBy(wD + (aW2D||0))}`);
        setText('#gb-wan-up-bps', `🔼 ${fB(wU)}`);
        setText('#gb-wan-down-bps', `🔽 ${fB(wD)}`);
        setText('#gb-wpeak-avg', `峰: 🔼${fBy(S.wMaxU)} 🔽${fBy(S.wMaxD)} ｜ 均: 🔼${fBy(aWU)} 🔽${fBy(aWD)}`);
        setText('#gb-lan-up-bytes', `🔼 ${fBy(sU)}`);
        setText('#gb-lan-down-bytes', `🔽 ${fBy(sD)}`);
        setText('#gb-lan-up-bps', `🔼 ${fB(sU)}`);
        setText('#gb-lan-down-bps', `🔽 ${fB(sD)}`);
        setText('#gb-perc-up', `🔼 ${wU>0?(sU*100/wU).toFixed(1):0.0}%`);
        setText('#gb-perc-down', `🔽 ${wD>0?(sD*100/wD).toFixed(1):0.0}%`);
        setText('#gb-lan-up-vol', `🔼 ${fV(LUp)}`);
        setText('#gb-lan-down-vol', `🔽 ${fV(LDn)}`);
        setText('#gb-wan-up-vol', `🔼 ${fV(S.wTotUp)}`);
        setText('#gb-wan-down-vol', `🔽 ${fV(S.wTotDn)}`);
        setText('#gb-sowan-up-vol', `🔼 ${fV(S.dTU || 0)}`);
        setText('#gb-sowan-down-vol', `🔽 ${fV(S.dTD || 0)}`);
        setText('#gb-owan-up-vol', `🔼 ${fV(S.oWU || 0)}`);
        setText('#gb-owan-down-vol', `🔽 ${fV(S.oWD || 0)}`);
        { let 包线 = bd.querySelector('#gb-packet-line'); if (包线) { 包线.style.display = S.包数据可用 ? 'inline' : 'none'; if (S.包数据可用) 包线.innerHTML = `<span style="color:#666;">本次包数：</span><span style="color:#ff4c00;">↑ ${Math.round(包上总)}包，${包上总 ? fSV(LUp / 包上总) : '0B'}/包</span><span style="color:#666; margin:0 6px;">|</span><span style="color:#0059fa;">↓ ${Math.round(包下总)}包，${包下总 ? fSV(LDn / 包下总) : '0B'}/包</span><span style="color:#4CAF50; margin-left:8px;">${格式化包比(计算包比(包上总, 包下总))}</span>`; } }
        画总速率图(bd);
        let pb = bd.querySelector('#gb-pwan-bps-container'), pv = bd.querySelector('#gb-pwan-vol-container');
        if (aW2U !== undefined) {
            if (pb) { pb.style.display = 'inline'; setText('#gb-pwan-bps-up', '🔼 ' + fB(aW2U)); setText('#gb-pwan-bps-down', '🔽 ' + fB(aW2D)); }
            if (pv) { pv.style.display = 'inline'; setText('#gb-pwan-tot-up', '🔼 ' + fV(aW2TU)); setText('#gb-pwan-tot-down', '🔽 ' + fV(aW2TD)); }
        } else {
            if (pb) pb.style.display = 'none'; if (pv) pv.style.display = 'none';
        }
        if (bd.querySelector('#gb-ratio-display')) {
          setText('#gb-cur-up-vol', `🔼 ${fV(curHpU)}`);
          setText('#gb-cur-down-vol', `🔽 ${fV(curHpD)}`);
          bd.querySelector('#gb-ratio-display').innerHTML = S.cRT;
          if (bd.querySelector('#gb-wan-zero-up')) {
              setText('#gb-wan-zero-up', !S.wZEU ? '' : fSV(S.wZEU));
              setText('#gb-wan-zero-down', !S.wZED ? '' : fSV(S.wZED));
              setText('#gb-wan-zero-up-cnt', S.wZEUC || 0);
              setText('#gb-wan-zero-down-cnt', S.wZEDC || 0);
          }
        }
      }
            for (let m in cI) {
        let it = oDC[m];
        if (!it) continue;
        const cC = cI[m] || { upRate: 0, dnRate: 0, iface: "", offUp: 0, offDn: 0, 包上: 0, 包下: 0, 信号: 0 },
              cS = S.cls[m] || { intUp: 0, intDn: 0, onS: 0, 包上总: 0, 包下总: 0 };
        
        let cache = it._gege || (it._gege = {});
        let hqU = cS.intUp || 0; 
        let hqD = cS.intDn || 0;
        let tN = cache.timeNode ??= it.querySelector('.gege-online-time');
        if (tN && cS.onS > 0) tN.textContent = `在线：${fOT(cS.onS)}`;
        
        const dI = cache.devIntro ??= it.querySelector('.dev-intro');
        const inf = cache.info ??= it.querySelector('.info');
        let 有线 = /^eth\d+$/i.test(cC.iface || ''), 端口 = 有线 ? S.端口状态[cC.iface] : null, 连接速率 = 有线 ? (端口?.rate || 0) : (cC.协商 || 0), 半双工 = !!(有线 && 端口?.half);
        let 半双工颜色 = ['#ff4c00', '#ff9800', '#f4c20d', '#4caf50', '#00bcd4', '#0059fa', '#9c27b0', '#000'][S.半双工色轮 & 7];
        let rate标记 = 连接速率 ? `<div style="font-size:10.5px;color:${连接速率===2500?'#000':连接速率===100?'#ff4c00':连接速率===10?'#4caf50':'#999'};font-family:Consolas;margin-top:2px;font-weight:${连接速率===2500||连接速率===100?'bold':'normal'};">rate:${连接速率}${半双工 ? `<span style="color:${半双工颜色};">.H</span>` : ''}</div>` : '';
        (cache.logo ??= it.querySelector('.dev-logo')) && (cache.logo.innerHTML = 取设备图标(cC.信号 | 0, 有线, 连接速率) + rate标记);
        let rssiNode = cache.rssiNode ??= it.querySelector('.gege-rssi');
        if (rssiNode) {
          if (cC.信号 | 0) { let p = Math.round((cC.信号 | 0) + 92 - (cC.iface === 'wl0' ? (S.RSSI频率修正 ?? 0) : 0)); rssiNode.innerHTML = `<span style="color:${p < 0 ? '#ff4c00' : 'inherit'}">${p}%</span>, ${cC.信号 | 0}`; }
          else rssiNode.textContent = '';
        }

        if (dI && inf) {
          let rB = cache.rBox ??= dI.querySelector('.gege-ratio-box');
          if (!rB) {
            let oRB = inf.querySelector('.gege-ratio-box'); if (oRB) oRB.remove();
            rB = document.createElement('div'); rB.className = 'gege-ratio-box';
            rB.style.cssText = 'margin-top: 4px; width: 95%; margin-bottom: 2px;';
            rB.innerHTML = `<div class="gege-ratio-top"><span class="v-port"></span><span class="v-interval" style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-weight: normal; font-size: 12.5px; opacity: 0.75; letter-spacing: 0.5px;"><span class="c-up"></span><span style="color:#666; margin:0 3px;">，</span><span class="c-down"></span></span><span class="v-rt-pct"></span></div><div class="gege-ratio-bar"><div class="gege-ratio-bar-inner"></div></div>`;
            dI.appendChild(rB);
            cache.rBox = rB;
          }
          
          let bR = (hqU + hqD) > 0 ? (hqU * 100 / (hqU + hqD)) : 0, tC = "", tCol = "#0059fa";
          if (CONFIG.calcMode === 1) {
            let rt = hqD > 0 ? (hqU / hqD) : (hqU > 0 ? Infinity : 0);
            if (rt > CONFIG.ratioExtremeUp) { tCol = '#ff4c00'; tC = (rt === Infinity ? '∞' : rt.toFixed(2)) + '⚠️'; }
            else if (rt > CONFIG.ratioWarnUp) { tCol = '#ff4c00'; tC = (rt * 100).toFixed(1) + '%'; }
            else if (rt > CONFIG.ratioExtremeDown) { tCol = '#0059fa'; tC = (rt * 100).toFixed(1) + '%'; }
            else { tCol = '#0059fa'; let rRt = hqU > 0 ? (hqD / hqU) : (hqD > 0 ? Infinity : 0); tC = (rRt === Infinity ? '∞' : rRt.toFixed(1)) + 'x'; }
          } else {
            tCol = bR > CONFIG.ratioThreshold ? '#ff4c00' : '#0059fa';
            tC = bR.toFixed(1) + '%';
          }
          let pNm = CONFIG.portMap[cC.iface] || cC.iface || "未知";
          if (cC.iface === 'wl1' && S.is5G_149) pNm = "5.8G";
          (cache.rBoxPort ??= rB.querySelector('.v-port')).textContent = pNm;
          (cache.rBoxUp ??= rB.querySelector('.v-interval .c-up')).textContent = '' + fSV(hqU);
          (cache.rBoxDn ??= rB.querySelector('.v-interval .c-down')).textContent = '' + fSV(hqD);
          let rtP = cache.rtPct ??= rB.querySelector('.v-rt-pct');
          rtP.textContent = tC; rtP.style.color = tCol;
          (cache.rBoxBar ??= rB.querySelector('.gege-ratio-bar-inner')).style.width = Math.min(bR, 100) + '%';
          let ipNode = cache.ipNode ??= inf.querySelector('.dev-ip');
          if (ipNode) {
            let zBadge = cache.zBadge ??= ipNode.querySelector('.gege-zero-badge');
            if (!zBadge) {
              zBadge = document.createElement('span'); zBadge.className = 'gege-zero-badge gege-box';
              ipNode.style.display = 'flex'; ipNode.style.justifyContent = 'space-between';
              zBadge.style.cssText = 'color: #999; font-size: 11.5px; font-family: system-ui, sans-serif; margin-right: 5px;';
              ipNode.appendChild(zBadge);
              cache.zBadge = zBadge;
            }
            zBadge.textContent = ((cS.zUC || 0) + (cS.zDC || 0)) < 6 ? "" : `[0估] ${!cS.zEU ? '' : fSV(cS.zEU)}，${!cS.zED ? '' : fSV(cS.zED)}｜${cS.zUC || 0},${cS.zDC || 0}`;
          }
          let 包行 = cache.包数行 ??= inf.querySelector('.gege-pkt-line');
          if (包行) { 包行.style.display = S.包数据可用 ? '' : 'none'; if (S.包数据可用) 包行.innerHTML = `<span style="color:#ff4c00;">↑ ${Math.round(cS.包上总 || 0)}包</span><span style="color:#FF9800;">，${包上总 ? ((cS.包上总 || 0) * 100 / 包上总).toFixed(1) : '0.0'}%</span><span style="margin:0 6px;"></span><span style="color:#0059fa;">↓ ${Math.round(cS.包下总 || 0)}包</span><span style="color:#9c27b0;">，${包下总 ? ((cS.包下总 || 0) * 100 / 包下总).toFixed(1) : '0.0'}%</span><span style="color:#4CAF50; margin-left:8px;">${格式化包比(计算包比(cS.包上总 || 0, cS.包下总 || 0))}</span>`; }
          let bx = cache.upBox ??= inf.querySelector('.gege-up-box');
          if (!bx || bx.querySelector('.t-row')) {
            if (bx) bx.remove();
            let oUB = dI.querySelector('.gege-up-box'); if (oUB) oUB.remove();
            bx = document.createElement('div'); bx.className = 'gege-up-box';
            bx.style.cssText = 'display:flex; align-items:center; width:95%; margin-top:0px; margin-bottom:2px;';
            bx.innerHTML = `<span class="v-vol" style="display:none;"></span><div class="zte-thin-bar" style="flex:1; margin:0;"><div class="zte-thin-bar-inner up"></div></div><span class="v-pct c-up" style="font-size:11.5px; font-weight:bold; font-family:system-ui, sans-serif; width:40px; text-align:right;"></span>`;
            inf.appendChild(bx);
            cache.upBox = bx;
          }
          let p = hpU > 0 ? (hqU * 100 / hpU) : 0;
          (cache.upVol ??= bx.querySelector('.v-vol')).textContent = fV(cS.intUp);
          (cache.upPct ??= bx.querySelector('.v-pct')).textContent = p.toFixed(1) + '%';
          (cache.upBar ??= bx.querySelector('.zte-thin-bar-inner')).style.width = Math.min(p, 100) + '%';

          let dBx = cache.dBox ??= inf.querySelector('.gege-down-box');
          if (!dBx || dBx.querySelector('.t-row')) {
            if (dBx) dBx.remove();
            dBx = document.createElement('div'); dBx.className = 'gege-down-box';
            dBx.style.cssText = 'display:flex; align-items:center; width:95%; margin-top:6px; margin-bottom:2px;';
            dBx.innerHTML = `<span class="v-vol" style="display:none;"></span><div class="zte-thin-bar" style="flex:1; margin:0;"><div class="zte-thin-bar-inner down"></div></div><span class="v-pct c-down" style="font-size:11.5px; font-weight:bold; font-family:system-ui, sans-serif; width:40px; text-align:right;"></span>`;
            inf.appendChild(dBx);
            cache.dBox = dBx;
          }
          let dp = LDn > 0 ? (cS.intDn * 100 / LDn) : 0; 
          (cache.dBoxVol ??= dBx.querySelector('.v-vol')).textContent = fV(cS.intDn);
          (cache.dBoxPct ??= dBx.querySelector('.v-pct')).textContent = dp.toFixed(1) + '%';
          (cache.dBoxBar ??= dBx.querySelector('.zte-thin-bar-inner')).style.width = Math.min(dp, 100) + '%';
        }
        
        const sp = cache.speed ??= it.querySelector('.speed');
        if (sp) {
          let enh = cache.enh ??= sp.querySelector('.zte-enhance-speed');
          if (!enh) {
            sp.querySelectorAll('.connect-up, .connect-down').forEach(n => { n.style.display = 'none'; });
            enh = document.createElement('div'); enh.className = 'zte-enhance-speed';
            enh.innerHTML = `<div class="zte-bar-wrap zte-bar-up"><span class="v-val" style="white-space: nowrap; flex-shrink: 0;"></span><span class="v-spark" style="font-family: monospace; letter-spacing: -1.5px; font-size: 11px; margin: 0 8px; opacity: 0.65; white-space: pre; flex: 1; overflow: hidden; text-align: right;"></span><span class="v-pct" style="white-space: nowrap; flex-shrink: 0;"></span></div><div class="zte-bar-wrap zte-bar-down"><span class="v-val" style="white-space: nowrap; flex-shrink: 0;"></span><span class="v-spark" style="font-family: monospace; letter-spacing: -1.5px; font-size: 11px; margin: 0 8px; opacity: 0.65; white-space: pre; flex: 1; overflow: hidden; text-align: right;"></span><span class="v-pct" style="white-space: nowrap; flex-shrink: 0;"></span></div>`;
            sp.appendChild(enh);
            cache.enh = enh;
          }
          let pu = sU > 0 ? (cC.upRate * 100 / sU) : 0,
              pd = sD > 0 ? (cC.dnRate * 100 / sD) : 0,
              bU = cache.bU ??= enh.querySelector('.zte-bar-up'),
              bD = cache.bD ??= enh.querySelector('.zte-bar-down');
          
          let clU = Math.max(...cS.hU, (S.aWu * 0.1) || 0, 512000);
          let clD = Math.max(...cS.hD, (S.aWd / 8) || 0);
          (cache.bUSpk ??= bU.querySelector('.v-spark')).textContent = 取火花线(cS.hU, cS.hIdx, clU, 73000);
          (cache.bDSpk ??= bD.querySelector('.v-spark')).textContent = 取火花线(cS.hD, cS.hIdx, clD, 1000000);

          bU.style.setProperty('--p-up', Math.min(pu, 100) + '%');
          (cache.bUVal ??= bU.querySelector('.v-val')).textContent = `🔼 ${fBy(cC.upRate)}`;
          (cache.bUPct ??= bU.querySelector('.v-pct')).textContent = pu.toFixed(1) + '%';
          
          bD.style.setProperty('--p-down', Math.min(pd, 100) + '%');
          (cache.bDVal ??= bD.querySelector('.v-val')).textContent = `🔽 ${fBy(cC.dnRate)}`;
          (cache.bDPct ??= bD.querySelector('.v-pct')).textContent = pd.toFixed(1) + '%';
        }
      }
    });
  }
  async function bVD(ol, cI) {
    try {
      let h2 = [], h52 = [], h58 = [], hW = [];
      for (let m in cI) {
        let d = cI[m], tS = fOT(d.onSec), ifc = d.iface;
        let htm = `<div class="col-md-12 col-xs-12 config-item gege-list-item" data-gege-mac="${m}"><div class="config-item-box" style="display: flex; align-items: stretch;"><div class="col-md-5 col-xs-7 logo" style="width: 33%; display: flex; flex-direction: row; align-items: center;"><div class="dev-logo" style="width: 50px; height: 50px; min-width: 50px; margin-right: 15px; display: inline-flex; align-items:center; justify-content:center;"></div><div class="dev-intro" style="flex: 1; display: flex; flex-direction: column; justify-content: flex-start; min-height: 50px;">
<div class="dev-title-line"><span class="dev-name-text" style="font-weight: bold; color: #333; font-size: 14px;">${escapeHTML(d.name)}</span><span class="gege-rssi"></span></div><div class="gege-online-time" style="color: #999; font-size: 12px; font-family: system-ui, sans-serif; margin-top: 4px;">${tS?'在线：'+tS:''}</div></div></div><div class="col-md-4 col-xs-5 info" style="width: 27%; display: flex; flex-direction: column; padding: 0 10px; border-right: 1px solid #eee;"><div class="dev-ip" style="color: #666; font-family: system-ui, sans-serif;">${escapeHTML(d.ip)}</div><div class="dev-number grey gege-pkt-line" style="color: #666; font-size: 11.5px; font-family: system-ui, sans-serif;"></div></div><div class="col-md-3 col-xs-12 speed" style="width: 40%; display: flex; flex-direction: column; justify-content: center; padding: 0 10px;"></div></div></div>`;
        if (['wl0', '2.4G'].includes(ifc)) h2.push(htm);
        else if (['wlan5', 'wl1', 'wlan4', '5.2', '5.2G'].includes(ifc)) h52.push(htm);
        else if (ifc === 'wl2' || ifc === 'wlan2' || ifc === '5.8G' || (/w/i.test(ifc) && !/wan/i.test(ifc))) h58.push(htm);
        else hW.push(htm);
      }
      requestAnimationFrame(() => {
        ol.innerHTML = `<div style="padding: 20px; width: 96%; margin: 0 auto; min-height: 100%;"><div id="gege-board-anchor"></div><div id="config-list" class="config-list gege-list-container"><div class="gege-section"><div class="config-title">有线设备${(window.gegeHiddenDevices && Object.keys(window.gegeHiddenDevices).length > 0) ? '<span style="color: #ff4c00; font-size: 13px; font-weight: normal; margin-left: 10px; font-family: system-ui, sans-serif;">(哥哥科技：智能Mesh适配)</span>' : ''}</div>${hW.join('')||'<div class="gege-empty-state">没有连接设备</div>'}</div><div class="gege-section"><div class="config-title">无线设备（${S.is5G_149?'5.8GHz':'5.2GHz'}）</div>${h52.join('')||'<div class="gege-empty-state">没有连接设备</div>'}</div><div class="gege-section"><div class="config-title">无线设备（2.4GHz）</div>${h2.join('')||'<div class="gege-empty-state">没有连接设备</div>'}</div><div class="gege-section"><div class="config-title">无线设备（${S.is5G_149?'5.2GHz':'5.8GHz'}）</div>${h58.join('')||'<div class="gege-empty-state">没有连接设备</div>'}
        </div><div style="margin-top: 25px; padding-top: 15px; border-top: 1px dashed #eee; text-align: center; font-family: system-ui, sans-serif, 'Microsoft YaHei', sans-serif;"><div style="font-size: 11.5px; color: #777; font-style: italic; margin-bottom: 8px;">“在一个文明社会，干净的、不被监视与吸血的网络，是我们每个人的基本权利。”</div><div style="font-size: 10.5px; color: #999; line-height: 1.3; margin-bottom: 8px;">本交互式程序基于 GNU Affero GPL v3.0 协议开源，按“原样 (AS IS)”提供，不对其适用性、稳定性、精密度或任何商业场景合规性作任何明示或暗示的担保。<br>根据 AGPL-3.0 第 5(d) 及 7(b) 条规定，基于本程序的任何修改均不得移除或篡改本界面的署名与法律声明。保留此界面是使用本软件代码的合法性的前置条件。
        </div><div style="font-size: 12px; color: #555;"><a href="https://github.com/ucxn/Bro-Stat" target="_blank" style="color: #0059fa; text-decoration: none; font-weight: bold;">Bro-Stat 增强组件</a> Copyright &copy; 2026 <a href="https://www.bilibili.com/video/BV1PtR7B8ECC" target="_blank" style="color: #0059fa; text-decoration: none; font-weight: bold;">哥哥科技</a> (BroTech)<span style="color: #888; font-weight: normal;"> | All Rights Reserved</span>&emsp;&nbsp;<a href="https://scriptcat.org/zh-CN/users/203510" target="_blank" style="color: #666; text-decoration: none;">点此分享</a></div></div></div></div>`;
        S.DOM已重建 = !0; S.DOM缓存 = null;
      });}
    catch (e) {
      requestAnimationFrame(() => {
        ol.innerHTML = `<div style="padding: 20px; color: red;">数据渲染失败: ${escapeHTML(e.message)}</div>`;
      });
    }
  }
  window.createGegeFloatingBtn = function () {
    if (document.getElementById('gege-floating-btn')) return;
    let b =
      document.createElement('div');
    b.id = 'gege-floating-btn';
    b.innerHTML = '🛸';
    b.style.cssText = 'position: fixed; top: 20px; right: 30%; width: 50px; height: 50px; background: linear-gradient(135deg, #0059fa, #00c6ff); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 48px; box-shadow: 0 4px 15px rgba(0,89,250,0.5); cursor: pointer; z-index: 99999; transition: transform 0.3s ease; user-select: none;';
    b.
    onmouseover = () => {
      b.style.transform = 'scale(1.1) rotate(15deg)';
    };
    b.onmouseout = () => {
      b.style.transform =
        'scale(1) rotate(0deg)';
    };
    b.onclick = () => window.gegeTogglePanel();
    document.body.appendChild(b);
  };
window.gegeTogglePanel = function (fS = null) {
    let o = document.getElementById('gege-global-overlay');
    let tS = fS !== null ? fS : !(o && o.style.display === 'block');
    
    if (!tS) {
      if (o) o.style.display = 'none';
      return;
    }
    
    if (!o) {
      o = document.createElement('div');
      o.id = 'gege-global-overlay';
      document.body.appendChild(o);
    }
    o.style.display = 'block';
    if (!window.gegeBActivated) {
      window.gegeBActivated = !0;
      clearTimeout(window.gegeMasterTimer);
      window.gegeMasterTimer = setInterval(rSD, CONFIG.lanRefreshInterval * 1000);
      
      fetch(`/wlan_ap_basic_5g.asp?_=${Date.now()}`)
      .then(r => r.arrayBuffer())
      .then(b => GBK.decode(b))
      .then(t => {
        let m = t.match(/var\s+curChannel\s*=\s*(\d+);/), 信道 = +(m?.[1] || 0);
        S.RSSI频率修正 = 信道 >= 32 && 信道 <= 177 ? 20 * Math.log10((5000 + 5 * 信道) / 2452) : 7.2;
        if (信道 >= 149 && 信道 <= 177 && !S.is5G_149) {
          S.is5G_149 = !0;
          let ol = document.getElementById('gege-global-overlay');
          if (ol && ol.style.display === 'block') window.gegeForceUIRedraw = !0;
        }
      }).catch(e => { S.RSSI频率修正 = 7.2; console.warn("[H3C] 5G信道探测异常:", e); });
    }
    bVD(o, Object.create(null)).then(() => rSD());
  };

  window.gegeBActivated = !1;
  window.gegeHiddenDevices = {};
  window.gegeMasterTimer = null;
  const tKA = () => {
    let i = document.createElement('iframe');
    i.id = 'gege-keepalive-iframe';
    i.style.display = 'none';
    const p = ["/#/sys", "/#/app", "/#/wlan/"];
    i.src = `${window.location.origin}${p[Math.floor(Math.random()*p.length)]}`;
    let z = document.getElementById('gege-keepalive-iframe');
    if (z) z.remove();
    document.body.appendChild(i);
    setTimeout(() => {
      if (i.parentNode) {
        i.src = 'about:blank';
        i.remove();
      }
    }, 12000);
  };
  setTimeout(tKA, 2000);
  setInterval(tKA, 720000);
  const _initUI = () => {
    if (CONFIG.injectMode === 3 || (CONFIG.injectMode === 1 && +(window.location.hostname.slice(window.location.hostname.lastIndexOf('.') + 1)) < 6)) {
      if (window.createGegeFloatingBtn) window.createGegeFloatingBtn();
    }
  };

  if (document.readyState === 'complete') _initUI(); else window.addEventListener('load', _initUI);

})();
