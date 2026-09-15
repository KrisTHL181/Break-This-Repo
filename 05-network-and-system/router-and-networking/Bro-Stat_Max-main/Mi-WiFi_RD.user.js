// ==UserScript==
// @name            小米路由器增强 Mi-Stat_Max
// @name:en         MiWiFi-Stat_Max
// @namespace       ucxn
// @version         5.9.8
// @description     哥哥科技 space.bilibili.com/501430041
// @description:en  https://github.com/ucxn/Mi-Stat_Max
// @tag             路由器 小米 网络 监控 统计 数据 可视化 极客 WiFi 米家 HA 智能 定时 后台 雷军 RUOK WRT OP
// @author          哥哥科技 QQ群 680464365
// @contributor     https://github.com/1-Reality
// @contributor     https://github.com/Samuel-Knight   
// @noframes
// @icon            https://scriptcat.org/api/v2/resource/image/duygQktL5QjWtkLc
// @match           *://*/cgi-bin/luci*
// @match           *://*/main.html*
// @run-at          document-end
// @grant           GM_setValue
// @grant           GM_getValue
// @storageName     GBNPA_Storage
// @license         AGPL-3.0-or-later
// @updateURL       https://github.com/ucxn/Mi-Stat_Max/raw/refs/heads/main/new.user.js
// @downloadURL     https://github.com/ucxn/Mi-Stat_Max/raw/refs/heads/main/new.user.js

// ==/UserScript==

(function () {
  'use strict';

  console.log(`🚀 哥哥科技 V${(typeof GM_info !== 'undefined' && GM_info.script?.version) || '5.9.9'} 引擎已装载...`);

  // ======== [0] 用户极客环境变量配置区 ========
  const CONFIG = {
	readSaveData: 1, // 【历史记录】 1: 从路由器后台读档 | 0: 新局模式 | 2: 从本地长期历史读档
    uiLayout: 1, // 【面板拓扑结构】 0: 经典版 | 1: 详细紧凑版(驾驶舱美学) | 2: 详细平铺版(报表流美学)
    injectMode: 3, // 【UI注入模式】 0: 原生侧边栏(1min)| 1: 仅悬浮舱 | 2: 智能选一 | 3：默认模式
    calcMode: 1, // 1: 上行/下行倍数模式, 0: 上行占总和比例模式
    ratioExtremeUp: 10, // 极端上传判定阈值 (> 1000%)
    ratioWarnUp: 0.12, // 重度上传警告阈值 (> 7%)
    ratioExtremeDown: 0.01, // 极端下载判定阈值 (< 1%)
    ratioThreshold: 7, // (仅calcMode=0时有效) 上传占比报警阈值(%)
    lanRefreshInterval: 3, // LAN口刷新时间(秒)，用于精准补偿0到唤醒时的瞬时流量
    wanRefreshInterval: 3, // 【新增】WAN口刷新时间(秒)，用于精准补偿0到唤醒时的瞬时流量
    周期类型: 'W', // 'M'(每月), 'W'(每周), 'D'(固定天数), 其它任意字符：不开启周期重置+自动导出功能
    周_天设置: 5, // M: 1~31号; W: 0~6(周日~周六); D: 间隔天数(如 7)
    基准日期: '2026-06-20', // 原点时间(仅 D 模式有效) 任意一个历史周期的零点
    报告时间: 720, // 提示时间：相对周期0点的偏移分钟数。(如 -4320 代表提前 3 天) 设置相对指定日期的下个周期起点的时间偏移量
    自动导出: +1020, // 强制导出：相对周期0点的偏移分钟数。(如 W模式+锚点6(周六)+偏移-180 = 周五 21:00 强制导出清零)
    时区补偿: 28800000, // 默认 UTC+8 时区补偿量。
    盲漫游: undefined, //也就是无线交换机（AP/有线桥接）模式，无线设备被主路由识别为有线设备则设置1
    portMap: {
      "eth1": "网口 ",
      "eth2": "网口 2",
      "eth3": "网口 3",
      "eth4": "网口 4",
      "wl0": "2.4G",
      "wl1": "5.2G",
      "wl2": "访客"
    }
  };

    if (location.pathname.includes('main.html')) {
    const _w = typeof unsafeWindow !== 'undefined' ? unsafeWindow : window;
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
const ESC_MAP = { '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' };
  function escapeHTML(str) {return str ? String(str).replace(/[&<>'"]/g, m => ESC_MAP[m]) : '';}

  const S = {
    lt: 0,
    wInstUp: 0,
    wInstDn: 0,
    wTotUp: 0,
    wTotDn: 0,
    cls: {}, isPinned: !0,
    w2U: 0, w2D: 0, w2TotUp: 0, w2TotDn: 0, w2LT: undefined,
    hasW2: !1, is5G_149: !1, oWU:0, Warn_MS: 0, Force_MS: 0, _RST: !1,
    aWu: 0, aWd: 0, lwTU: 0, lwTD: 0
  };
  window.__gIsF = !1;

  function fB(bps) {
		if (bps > 1e9) return `${(bps * 1e-6).toFixed(1)} Mbit/s`;
        if (bps > 1e6) return `${(bps * 1e-6).toFixed(2)} Mbps`;
        if (bps > 1e3) return `${(bps * 1e-3).toFixed(1)} Kbps`;
        return `${Math.round(bps)} bps`;
    }
const F_ARR = ['0', '[1/16]', '[2/16]', '[3/16]', '[1/4]', '[5/16]', '[6/16]', '[7/16]', '[4/8]', '[9/16]', '[10/16]', '[11/16]', '[3/4]', '[13/16]', '[14/16]', '[15/16]', '[1]'];
function fBy(bps) {
        if (bps === 0) return '0  B';
        if (bps > 8388608) return `${(bps * 1.1920928955078125e-7).toFixed(2)} MiB/s`;
        return bps < 8602
            ? ((bps * 0.002 | 0) === bps * 0.002 && bps < 8001
                ? `${F_ARR[bps * 0.002]} KB/s`
                : `${(bps * 0.000125).toFixed(2)} KB/s`)
            : `${(bps * 0.0001220703125).toFixed(1)} KB/s`;
    }

  function fV(bits) {
        if (bits > 83886080000) return `${(bits / 8589934592).toFixed(4)} GiB`;
		if (bits > 8388608000) return `${(bits / 8388608).toFixed(1)} MiB`;
        if (bits > 8388608) return `${(bits / 8388608).toFixed(4)} MiB`;
        if (bits > 8192) return `${(bits / 8192).toFixed(3)} KiB`;
        return `${Math.round(bits / 8)} B`;
    }

  function fVD(bitsIntegral, bitsOfficial) {
        if (bitsIntegral > 8796093022208) return `${(bitsOfficial / 8796093022208).toFixed(4)} | ${(bitsIntegral / 8796093022208).toFixed(4)} TiB`;
        if (bitsIntegral >= 8589934592) return `${(bitsOfficial / 8589934592).toPrecision(5)} | ${(bitsIntegral / 8589934592).toPrecision(5)} GiB`;
        if (bitsIntegral > 8388608) return `${(bitsOfficial / 8388608).toFixed(3)} | ${(bitsIntegral / 8388608).toFixed(3)} MiB`;
        if (bitsIntegral > 8192) return `${(bitsOfficial / 8192).toFixed(2)} | ${(bitsIntegral / 8192).toFixed(2)} KiB`;
        return `${Math.round(bitsOfficial / 8)} | ${Math.round(bitsIntegral / 8)} B`;}

  function fSV(bits) {
    if (bits >= 84607500288) return `${(bits / 8589934592).toPrecision(4)}G`;
	if (bits > 8388608000) return `${Math.round(bits / 8388608)}M`;
    if (bits > 8388608) return `${(bits / 8388608).toFixed(2)}M`;
    if (bits >= 8192) return `${(bits / 8192).toFixed(1)}K`;
    return `${Math.round(bits / 8)}B`;}

  function fOT(totalSec) {
		totalSec = totalSec | 0;
        if (totalSec < 0) return "";
		const d = (totalSec / 86400) | 0;
		let r = totalSec - d * 86400;
		const h = (r / 3600) | 0;
		r = r - h * 3600;
		const m = (r / 60) | 0;
		const s = r - m * 60;
        return d > 0 
        ? `${d}天${h}时${m}分${s}秒` 
        : `${h}小时${m}分${s}秒`;}
  S.calcTime = (L) => {
    S.Force_MS = (CONFIG.周期类型 === 'M' ? Date.UTC(new Date(L).getUTCFullYear(), new Date(L).getUTCMonth() + (L >= Date.UTC(new Date(L).getUTCFullYear(), new Date(L).getUTCMonth(), CONFIG.周_天设置) ? 1 : 0), CONFIG.周_天设置) : (CONFIG.周期类型 === 'W' ? Date.UTC(new Date(L).getUTCFullYear(), new Date(L).getUTCMonth(), new Date(L).getUTCDate()) + ((CONFIG.周_天设置 - new Date(L).getUTCDay() > 0 ? CONFIG.周_天设置 - new Date(L).getUTCDay() : CONFIG.周_天设置 - new Date(L).getUTCDay() + 7) * 86400000) : (CONFIG.周期类型 === 'D' ? Date.UTC(new Date(L).getUTCFullYear(), new Date(L).getUTCMonth(), new Date(L).getUTCDate()) + CONFIG.周_天设置 * 86400000 : Infinity))) - CONFIG.时区补偿;
    S.Warn_MS = S.Force_MS + CONFIG.报告时间 * 60000;
    S.Force_MS += CONFIG.自动导出 * 60000;
  };S.calcTime((typeof GM_getValue !== 'undefined' && GM_getValue('gege_reset_ms')) ? (GM_getValue('gege_reset_ms') + CONFIG.时区补偿) : Date.now() + CONFIG.时区补偿);
  function nM(m) {
    return m ? m.trim().toLowerCase().replaceAll('-', ':') : '';
  }
  const st = document.createElement('style');
  st.innerHTML = `.config-item{
        clear:both;}.config-item-box{display:flex!important;
        align-items:stretch!important;padding-bottom:
        12px!important;}.config-item .logo{width:33%!important;
        float:none!important;display:flex!important;flex-direction:row;}.config-item .dev-intro{flex:1;display:flex!important;flex-direction:column;justify-content:flex-start;min-height:100px;padding-bottom:0!important;margin-bottom:0!important;}.config-item .info{width:27%!important;float:none!important;display:flex!important;flex-direction:column;justify-content:flex-start;padding:0 10px!important;border-right:1px solid #eee;}.config-item .speed{width:40%!important;float:none!important;display:flex!important;flex-direction:column;justify-content:center;padding:0 10px!important;}.geek-row{display:flex;justify-content:space-between;align-items:center;white-space:nowrap;height:20px;}
    .geek-label{width:110px;color:#333;font-weight:bold;}.geek-val-box{flex:1;display:flex;gap:15px;margin-left:10px;}.geek-fixed-width{display:inline-block;width:120px;}.geek-right-box{text-align:right;min-width:220px;font-weight:bold;}.c-up{color:#ff4c00;}.c-down{color:#0059fa;}.gege-up-box,.gege-down-box{margin-top:auto!important;margin-bottom:0!important;width:95%;}.gege-ratio-box{margin-top:10px;width:95%;margin-bottom:5px;}.t-row{font-size:12px;font-weight:bold;margin-bottom:2px;display:flex;justify-content:space-between;font-family:Consolas;}.zte-thin-bar{width:100%;height:3px;background:rgba(0,0,0,0.05);border-radius:1.5px;overflow:hidden;}.zte-thin-bar-inner{height:100%;transition:width 0.5s ease-out;}.zte-thin-bar-inner.up{background:#ff4c00;}.zte-thin-bar-inner.down{background:#0059fa;}.gege-ratio-top{display:flex;justify-content:space-between;font-size:12px;font-weight:bold;margin-bottom:2px;}.gege-ratio-bar{width:100%;height:4px;background:#0059fa;border-radius:2px;overflow:hidden;}.gege-ratio-bar-inner{height:100%;background:#ff4c00;transition:width 0.5s ease-out;}.zte-enhance-speed{display:flex;flex-direction:column;gap:6px;width:100%;font-family:Consolas;}
    .zte-bar-wrap{position:relative;width:100%;border-radius:4px;border:1px solid;font-size:13px;font-weight:bold;overflow:hidden;padding:3px 8px;display:flex;justify-content:space-between;align-items:center;z-index:1;box-sizing:border-box;}.zte-bar-wrap span{font-size:inherit;font-weight:inherit;}.zte-bar-up{color:#ff4c00;border-color:rgba(255,76,0,0.3);}.zte-bar-down{color:#0059fa;border-color:rgba(0,89,250,0.3);}.zte-bar-up::before{content:'';position:absolute;left:0;top:0;bottom:0;z-index:-1;background:rgba(255,76,0,0.12);width:var(--p-up,0%);transition:width 0.5s;}.zte-bar-down::before{content:'';position:absolute;left:0;top:0;bottom:0;z-index:-1;background:rgba(0,89,250,0.12);width:var(--p-down,0%);transition:width 0.5s;}#config-list.gege-list-container{contain:content!important;background-color:#ffffff!important;border-radius:8px!important;border:1px solid #e0e0e0!important;padding:20px 30px!important;box-shadow:0 2px 10px rgba(0,0,0,0.02)!important;margin-top:10px!important;}.gege-section{margin-bottom:10px;}
    .gege-section:last-child{margin-bottom:0;}.gege-list-container .config-title{font-size:16px!important;font-weight:bold!important;color:#333!important;margin:15px 0 10px 0!important;padding-bottom:5px!important;}.gege-list-container .gege-section:first-child .config-title{margin-top:0!important;}.gege-empty-state{color:#999!important;font-size:14px!important;padding:0 0 15px 5px!important;border-bottom:1px solid #f0f0f0!important;margin-bottom:5px!important;}.gege-list-item{background-color:transparent!important;border-bottom:1px solid #f0f0f0!important;padding:15px 10px!important;margin-bottom:0!important;border-radius:0!important;}
    .gege-list-item:last-child{border-bottom:none!important;}#zte-geek-board{contain:content;background-color:transparent!important;border-left:4px solid #0059fa!important;border-radius:0!important;padding:5px 0 5px 15px!important;margin:10px 0 15px 0!important;box-shadow:none!important;border-bottom:1px solid #f0f0f0!important;font-size:14px;display:flex;flex-direction:column;gap:6px;padding-bottom:15px!important;transition:color 0s cubic-bezier(0.21733,0.21733,0.31185,0.25216);}#gege-global-overlay #zte-geek-board.geek-frozen-pane{position:sticky!important;top:0px!important;z-index:100!important;background-color:#f3f4f5!important;margin-top:0!important;padding-top:15px!important;box-shadow:0 10px 15px -3px rgba(0,0,0,0.05)!important;border-radius:0 0 8px 8px!important;}.gege-pin{cursor:pointer;font-size:11px;filter:grayscale(100%);opacity:0.5;transition:transform 0.2s;margin-left:2px;}
    .gege-pin.active{filter:none;opacity:1;transform:scale(1.1);}#gege-global-overlay{position:fixed;top:7.5%;right:0;bottom:0;background:#f3f4f5;z-index:9999;overflow-y:auto;padding-bottom:50px;left:0!important;border-radius:16px 16px 0 0;box-shadow:0 -5px 25px rgba(0,0,0,0.15);transition:top 0.3s ease;}@media (max-width: 768px){.geek-right-box:has(#gb-wan-zero-up),.geek-right-box:has(#gb-cur-up-vol){display:none!important}.gege-list-item{padding:12px 10px!important}.config-item-box{position:relative!important;flex-direction:column!important;padding-bottom:0!important}.config-item .info,.config-item .logo,.config-item .speed{width:100%!important;border:none!important;padding:0!important;position:static!important}.config-item .dev-intro{min-height:auto!important;justify-content:center!important;padding-right:90px!important}.config-item .logo{padding-bottom:4px!important}.config-item .info{flex-direction:column!important;margin:0 0 6px 0!important;gap:2px!important}.dev-ip{position:absolute!important;top:0!important;right:0!important;font-size:11px!important;background:rgba(0,89,250,0.08);color:#0059fa!important;padding:2px 6px!important;border-radius:4px;font-weight:bold;line-height:1.2;z-index:10;width:auto!important}.dev-number{width:auto!important;margin:0!important;font-size:11px!important}.gege-ratio-box{width:100%!important;margin-top:2px!important;margin-bottom:0!important}.gege-down-box{width:100%!important;margin-top:2px!important}#zte-geek-board{padding:8px!important;gap:0!important;font-size:11.5px!important}.geek-row{height:auto!important;flex-wrap:wrap!important;margin-bottom:4px!important;justify-content:flex-start!important;gap:2px 6px!important;line-height:1.3!important}.geek-label{width:auto!important;min-width:60px!important;font-size:11.5px!important;flex:0 0 auto!important}.geek-val-box{width:auto!important;flex:1 1 0%!important;display:flex!important;flex-wrap:wrap!important;margin-left:0!important;gap:2px 6px!important}.geek-fixed-width{width:auto!important}.geek-right-box{width:100%!important;flex:0 0 100%!important;text-align:left!important;font-size:11.5px!important;margin-top:2px!important;margin-left:0!important}.gege-list-container{padding:8px!important}.zte-enhance-speed{gap:4px!important}}`;
  document.
  head.
  appendChild(st);
  window.gegeRenderedMacs = new Set();
async function rSD() {
    if (window.__gIsF) return;
    window.__gIsF = !0;
    let n = performance.now();
    try {
      let stk = location.href.match(/;stok=([a-fA-F0-9]+)/)?.[1];
      if (!stk) return;
      const ts = Date.now();

      let [dR, sR] = await Promise.all([
        fetch(`/cgi-bin/luci/;stok=${stk}/api/misystem/devicelist?_=${ts}`),
        fetch(`/cgi-bin/luci/;stok=${stk}/api/misystem/status?_=${ts}`)
      ]).catch(() => [null, null]);

      if (!dR || !dR.ok) return;
      let dD = await dR.json();
      if (dD.code !== 0 || !dD.list) return;

      let sD = null;
      if (sR && sR.ok) { try { sD = await sR.json(); } catch(e){console.warn(e)} }

      let cWU = 0, cWD = 0;
      let ws = sD?.wan || sD?.wanStatistics;

      if (ws && ('upspeed' in ws)) {
        cWU = (+ws.upspeed || 0) * 8; cWD = (+ws.downspeed || 0) * 8;
      } else {
        try {
          let xR = await fetch(`/cgi-bin/luci/;stok=${stk}/api/xqsystem/status?_=${ts}`);
          if (xR.ok) { 
            let xD = await xR.json(); ws = xD?.wanStatistics || xD?.wan || {}; 
            cWU = (+ws.upspeed || 0) * 8; cWD = (+ws.downspeed || 0) * 8; 
          }
        } catch (err) { console.warn(err) }
      }
      S.oWU = (+ws?.upload || 0) * 8; S.oWD = (+ws?.download || 0) * 8; 

      // === 🔪 小米专属微分引擎 (求导测速 + 基准线锚定) ===
      S.bWU ??= S.oWU; S.bWD ??= S.oWD;
      S.lRU ??= S.oWU; S.lRD ??= S.oWD;
      S.lTU ??= n; S.lTD ??= n; // 上下行独立时间戳
      S.zCU ??= 0; S.zCD ??= 0;
      S.dWU ??= 0; S.dWD ??= 0; 
      
      // 容错兜底：防御路由器突然重启导致底层计数器硬件清零
      if (S.oWU < S.lRU) { S.bWU = S.lRU = S.oWU; S.lTU = n; S.dWU = S.zCU = 0; }
      if (S.oWD < S.lRD) { S.bWD = S.lRD = S.oWD; S.lTD = n; S.dWD = S.zCD = 0; }
      if (S.oWU > S.lRU) {
          S.dWU = (S.oWU - S.lRU) / ((n - S.lTU) * 0.001); 
          S.lRU = S.oWU; S.lTU = n; S.zCU = 0; 
      } else if (++S.zCU > 3) {
          S.dWU = 0; S.lTU = n; }
      if (S.oWD > S.lRD) {
          S.dWD = (S.oWD - S.lRD) / ((n - S.lTD) * 0.001);
          S.lRD = S.oWD; S.lTD = n; S.zCD = 0;
      } else if (++S.zCD > 3) {
          S.dWD = 0; S.lTD = n;
      }// 绝对事件
      
      // 网页打开以来的真实绝对流量 (供比例计算使用)
      S.dTU = Math.max(0, S.oWU - S.bWU); 
      S.dTD = Math.max(0, S.oWD - S.bWD);

      S.hasW2 = !1; // 多拨
      let cSU = 0, cSD = 0, cI = Object.create(null);
      let sL = sD?.dev || []; 
      let oL = !(dD.list || []).some(i => i.type === 0 || i.isap === 8) && (dD.list || []).some(i => i.type === 3);
      // 清洗局域网 JSON
      (dD.list || []).forEach(i => {
        let m = nM(i.mac || "");
        if (m) {
          let x = null; 
          for (let k = 0; k < sL.length; k++) { if (nM(sL[k].mac) === m) { x = sL[k]; break; } }
          
          let u = (+i.statistics?.upspeed || +x?.upspeed || 0) * 8, 
              dn = (+i.statistics?.downspeed || +x?.downspeed || 0) * 8;
              
          cI[m] = {
            upRate: u, dnRate: dn, 
            iface: i.isap === 8 ? 'Mesh' : (oL ? (i.type === 1 ? '有线' : (i.type === 2 ? '2.4G' : (i.type === 3 ? '5.2G' : '5.8G'))) : (i.type === 0 ? '有线' : (i.type === 1 ? '2.4G' : (i.type === 2 ? '5.2G' : '5.8G')))),
            offUp: (+x?.upload || 0) * 8,    
            offDn: (+x?.download || 0) * 8, 
            onSec: +(i.statistics?.online || x?.online || i.online || 0),
            name: i.name || i.oname || x?.devname || "未知",
            ip: Array.isArray(i.ip) ? (i.ip[0]?.ip || i.ip[0] || "") : (i.ip || "")
          };
          cSU += u; cSD += dn;
        }
      });
      
      //if (!isWanFetched) { cWU = cSU; cWD = cSD; }

      let ol = document.getElementById('gege-global-overlay'),
        cM = Object.keys(cI),
        iD = window.gegeForceUIRedraw || (cM.length !== window.gegeRenderedMacs.size);
      if (!iD && cM.length > 0) {
        for (let i = 0; i < cM.length; i++) {
          if (!window.gegeRenderedMacs.has(cM[i])) { iD = !0; break; }
        }
      }
      if (iD) {
        for (let m in S.cls) if (!cI[m]) {
          S.cls[m].intUp += S.cls[m].upR * (n - S.cls[m].lUT) * 0.0005;
          S.cls[m].intDn += S.cls[m].dnR * (n - S.cls[m].lUT) * 0.0005;
          S.cls[m].upR = S.cls[m].dnR = 0; S.cls[m].lUT = n;
        }
      }
      if (ol && ol.style.display === 'block' && (iD || !ol.querySelector('.gege-list-item'))) {
        bVD(ol, cI);
        window.gegeRenderedMacs = new Set(cM);
        window.gegeForceUIRedraw = !1;
      }
      let gDt = (S.lt !== 0) ? (n - S.lt) * 0.001 : 0;
      if (S.wLT === undefined) { S.wLT = n; }
      else if (cWU !== S.wInstUp || cWD !== S.wInstDn) {
        let wDt = n - S.wLT;
        if (S.wInstUp > 0) { S.wTotUp += (S.wInstUp + cWU) * wDt * 0.0005; }
        else if (cWU > 0) { let wEU = cWU * 0.5 * CONFIG.wanRefreshInterval; S.wTotUp += wEU; S.wZEU = (S.wZEU || 0) + wEU; S.wZEUC = (S.wZEUC || 0) + 1; }
        if (S.wInstDn > 0) { S.wTotDn += (S.wInstDn + cWD) * wDt * 0.0005; }
        else if (cWD > 0) { let wED = cWD * 0.5 * CONFIG.wanRefreshInterval; S.wTotDn += wED; S.wZED = (S.wZED || 0) + wED; S.wZEDC = (S.wZEDC || 0) + 1; }
        S.wLT = n;
      }
      if (CONFIG.readSaveData === 2 && !S.snapLoaded) { try { let sp = typeof GM_getValue !== 'undefined' ? GM_getValue('ha_snapshot') : null; S.snap = sp || {}; if(sp && sp.global) { S.wTotUp = S.wTotUp === 0 ? sp.global.wan_up || 0 : S.wTotUp; S.wTotDn = S.wTotDn === 0 ? sp.global.wan_down || 0 : S.wTotDn; } } catch(e){console.warn(e)} S.snapLoaded = !0; }
      for (const [m, cC] of Object.entries(cI)) {
        let spD = (CONFIG.readSaveData === 2 && S.snap && S.snap.devices && S.snap.devices[m]) || null;
        S.cls[m] ??= {
          upR: cC.upRate, dnR: cC.dnRate, lUT: n, 
          intUp: spD ? (spD.integral_up || 0) : 0, intDn: spD ? (spD.integral_down || 0) : 0,
          uB: CONFIG.readSaveData === 1 ? 0 : (spD ? cC.offUp - (spD.up || 0) : cC.offUp), 
          dB: CONFIG.readSaveData === 1 ? 0 : (spD ? cC.offDn - (spD.down || 0) : cC.offDn),
          lU: cC.offUp, lD: cC.offDn, aR: !1, dpU: 0, dpD: 0,
          oU: cC.offUp, oD: cC.offDn, hU: new Float64Array(64), hD: new Float64Array(64), hIdx: 0, ifc: cC.iface // 真实流量与环形缓冲
        };
        let cS = S.cls[m], dU = cC.offUp - cS.lU, dD = cC.offDn - cS.lD;
        if (cS.ifc !== cC.iface) { if (CONFIG.盲漫游) cS.aR = 2; cS.ifc = cC.iface; }
        if (dU < 0 || dD < 0) {
          if (dU < 0) { cS.uB += dU; cS.dpU = cS.lU; cS.oU += dU; }
          if (dD < 0) { cS.dB += dD; cS.dpD = cS.lD; }
          cS.aR = 3;
        
        } else if (cS.aR === 3) {
          if (dU > 0 || dD > 0) {
          if ( (cS.dpD && dD >= cS.dpD) || (cS.dpU && dU >= cS.dpU)) {
            if ((dU < cS.dpU * 2 || dU < cS.dpU + 1e8 * CONFIG.lanRefreshInterval) && (dD < cS.dpD * 2 || dD < cS.dpD + 1e9 * CONFIG.lanRefreshInterval)) {
              cS.uB += cS.dpU; cS.dB += cS.dpD; cS.oU += cS.dpU;
            } else {
              cS.uB += dU; cS.oU += dU; cS.dB += dD;}
            cS.aR = 2; if (CONFIG.盲漫游 === undefined) CONFIG.盲漫游 = 1;
            } else {
              cS.aR = 0; 
            } cS.dpU = 0; cS.dpD = 0;
          }
        }
        if (cS.lOS !== cC.onSec) { cS.onS = cC.onSec; cS.lOS = cC.onSec; }
        else { cS.onS = (cS.onS || cC.onSec || 0) + gDt; }
        
        if (cC.upRate !== cS.upR || cC.dnRate !== cS.dnR) {
          let ms = n - cS.lUT;
          if (cS.upR > 0) { cS.intUp += (cS.upR + cC.upRate) * ms * 0.0005; }
          else if (cC.upRate > 0) { let eU = cC.upRate * CONFIG.lanRefreshInterval * 0.5; cS.intUp += eU; cS.zEU = (cS.zEU || 0) + eU; cS.zUC = (cS.zUC || 0) + 1; }
          if (cS.dnR > 0) { cS.intDn += (cS.dnR + cC.dnRate) * ms * 0.0005; }
          else if (cC.dnRate > 0) { let eD = cC.dnRate * CONFIG.lanRefreshInterval * 0.5; cS.intDn += eD; cS.zED = (cS.zED || 0) + eD; cS.zDC = (cS.zDC || 0) + 1; }
          cS.upR = cC.upRate; cS.dnR = cC.dnRate; cS.lUT = n;
        }
        cS.lU = cC.offUp; cS.lD = cC.offDn;
      }
      S.lt = n;
      S.wInstUp = cWU; S.wInstDn = cWD;
      rUI(S.dWU, S.dWD, cSU, cSD, cI);
    } catch (err) {console.warn(err)} finally {window.__gIsF = !1;}
  }

  function buildCSV() {
    return ((sp, now, start) => '\uFEFF' + [
      `"哥哥科技 硬路由 NPU 增强系列：专用组件 ${(typeof GM_info !== 'undefined' && GM_info.script?.version) || '环境不支持获取版本号'} 生成"`,
      `"统计周期：${new Date(start + CONFIG.时区补偿).toISOString().replace('T', ' ').slice(0, 19)} 至 ${new Date(now + CONFIG.时区补偿).toISOString().replace('T', ' ').slice(0, 19)} (UTC${CONFIG.时区补偿 > 0 ? '+' : ''}${CONFIG.时区补偿 / 3600000})${CONFIG.readSaveData === 1 ? ' （含路由器后台读档）' : ''}"`,
      `"--- [全局统计] ---"`,
      `"WAN总上传(B)","WAN总下载(B)","高精全局上行(B)","高精全局下行(B)","LAN积分总上行(B)","LAN积分总下行(B)","本次在线总上行(B)","本次在线总下行(B)"`,
      `"${Math.round(sp.global?.wan_up||0)}","${Math.round(sp.global?.wan_down||0)}","${Math.round(sp.global?.lan_high_up||0)}","${Math.round(sp.global?.lan_high_down||0)}","${Math.round(sp.global?.lan_integral_up||0)}","${Math.round(sp.global?.lan_integral_down||0)}","${Math.round(sp.global?.lan_off_up||0)}","${Math.round(sp.global?.lan_off_down||0)}"`,
      ``,
      `"--- [设备明细] ---"`,
      `"设备名称","MAC地址","IP地址","状态/接口","高精上行","高精下行","积分上行","积分下行","官方上行","官方下行"`,
      ...Object.entries(sp.devices || {}).map(d => `"${d[1].name}","${d[0]}","${d[1].ip}","${d[1].status}","${Math.round(d[1].up||0)}","${Math.round(d[1].down||0)}","${Math.round(d[1].integral_up||0)}","${Math.round(d[1].integral_down||0)}","${Math.round(d[1].raw_up||0)}","${Math.round(d[1].raw_down||0)}"`),
      ``,
      `"Bro-Stat@哥哥科技 https://space.bilibili.com/501430041"`,
      `"项目主页: https://github.com/ucxn/Bro-Stat"`,
      `"脚本下载: https://scriptcat.org/users/203510"`

    ].join('\r\n'))(
      typeof GM_getValue !== 'undefined' ? GM_getValue('ha_snapshot', {}) : {}, 
      Date.now(), 
      (CONFIG.readSaveData === 2 && typeof GM_getValue !== 'undefined' ? GM_getValue('gege_reset_ms', null) : null) || performance.timeOrigin || Date.now()
    );
  }
function doSettle(nowMs) {
    S._RST = !0; // 防重入锁
    let csv = buildCSV(), b = new Blob([csv], {type: 'text/csv;charset=utf-8;'});
    let u = URL.createObjectURL(b), a = document.createElement('a');
    a.href = u; a.download = `哥哥科技_路由器统计数据导出_${new Date(nowMs + CONFIG.时区补偿).toISOString().slice(2, 19).replace(/[-:]/g, '').replace('T', '_')}_${nowMs}.csv`; a.click(); // 文件
    let w = window.open('about:blank', '_blank');
    if (w) w.document.write(`<!DOCTYPE html><html><head><title>流量结算备份</title></head><body style="background:#f3f4f5;font-family:system-ui,sans-serif;padding:40px 20px;color:#333;"><div style="background:#fff;padding:30px;border-radius:12px;box-shadow:0 4px 20px rgba(0,0,0,0.05);max-width:850px;margin:0 auto;"><h2 style="color:#0059fa;margin-top:0;border-bottom:2px solid #f0f0f0;padding-bottom:15px;">本次数据结算周期已结束</h2><p style="font-size:14px;line-height:1.7;color:#555;"><b>哥哥科技提示您：</b>请点击下方下载按钮将 CSV 报表保存到本地。<br>若下载失败，请点击复制按钮，新建文本文档粘贴后将拓展名改为 .csv 即可。</p><button id="dl-btn" style="background:#0059fa;color:#fff;border:none;padding:12px 24px;border-radius:6px;font-weight:bold;cursor:pointer;margin-right:10px;">📥 再次下载 CSV</button><button id="cp-btn" style="background:#4caf50;color:#fff;border:none;padding:12px 24px;border-radius:6px;font-weight:bold;cursor:pointer;">📋 一键复制内容</button><div style="background:#282c34;color:#abb2bf;padding:15px;border-radius:8px;overflow-x:auto;margin-top:20px;"><pre id="csv-data" style="margin:0;font-size:13px;line-height:1.5;">${csv}</pre></div></div><script>document.getElementById('dl-btn').onclick=function(){let b=new Blob([document.getElementById('csv-data').textContent],{type:'text/csv;charset=utf-8;'});let a=document.createElement('a');a.href=URL.createObjectURL(b);a.download='哥哥科技_路由器统计数据补下_${nowMs}.csv';a.click();};document.getElementById('cp-btn').onclick=function(){let t=document.createElement('textarea');t.value=document.getElementById('csv-data').textContent;document.body.appendChild(t);t.select();try{document.execCommand('copy');alert('复制成功！');}catch(e){alert('复制失败，请手动全选复制');}document.body.removeChild(t);};</script></body></html>`);
    GM_setValue('gege_reset_ms', nowMs);
    S.wTotUp = S.wTotDn = S.w2TotUp = S.w2TotDn = 0; // 内存原地清零
    for (let k in S.cls) { let s = S.cls[k]; s.intUp = s.intDn = 0; s.uB = s.oU = s.lU; s.dB = s.oD = s.lD; s.hU.fill(0); s.hD.fill(0); } // 内存原地清零底表
    document.getElementById('gb-w-bnr')?.remove(); // 预警横幅
    S.calcTime(Math.max(nowMs, S.Force_MS - CONFIG.自动导出 * 60000 + 1000) + CONFIG.时区补偿); // 瞬间算出下月/下周新线
    window.gegeForceUIRedraw = !0; // 重绘 UI
    setTimeout(() => { S._RST = !1; }, 2000); // 解开安全锁
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
  const SPRK = [' ', '▂', '▃', '▄', '▅', '▆', '▇', '█'];
  function getSpark(ringArr, headIdx, maxVal, minVal = 0) {
    let s = "";
    for (let i = 27; i--; ) {
      let v = ringArr[(headIdx - i) & 63];
      s += SPRK[v < minVal ? 0 : Math.min(7, Math.max(1, ((v / maxVal) * 7) | 0))];
    }
    return s;
  }
  function rUI(wU, wD, sU, sD, cI) {
    let tOD = 0,
      LUp = 0,
      LDn = 0,
      hpU = 0,
      hpD = 0,
      abU = 0,
      abD = 0,
      curHpU = 0,
      curHpD = 0,
      tot_cU = 0;

S.rTick = ((S.rTick || 0) + 1) & 7;  //内外网比消除抖动
for (let k in S.cls) {
      let s = S.cls[k], cC = cI[k];
      let cU = Math.max(0, (s.lU || 0) - (s.uB || 0));
      let cD = Math.max(0, (s.lD || 0) - (s.dB || 0));
      let sessU = Math.max(0, (s.lU || 0) - (s.oU || 0));
      let sessD = Math.max(0, (s.lD || 0) - (s.oD || 0));
      tot_cU += cU; LUp += s.intUp || 0; LDn += s.intDn || 0;
      hpU += (CONFIG.readSaveData === 2 ? cU : sessU); 
      hpD += (CONFIG.readSaveData === 2 ? cD : sessD);
      if (cC) { curHpU += (CONFIG.readSaveData === 2 ? cU : sessU); curHpD += (CONFIG.readSaveData === 2 ? cD : sessD); tOD += cC.offDn || 0; }
      abU += CONFIG.readSaveData === 2 ? sessU : (cC ? (cC.offUp || 0) : (s.lU || 0));
      abD += CONFIG.readSaveData === 2 ? sessD : (cC ? (cC.offDn || 0) : (s.lD || 0));
      s.hIdx = (s.hIdx + 1) & 63;
      s.hU[s.hIdx] = cC ? cC.upRate : 0;
      s.hD[s.hIdx] = cC ? cC.dnRate : 0;
    }
if (typeof GM_setValue !== 'undefined' && S.rTick === 1) {
  S.haTick = ((S.haTick || 0) + 1) & 31;
      if (S.haTick === 1) {
      try {
        let cln = {};
        for (let k in S.cls) {
          let s = S.cls[k], cC = cI[k];
          cln[k] = {
            up: Math.max(0, (s.lU || 0) - (s.uB || 0)), down: Math.max(0, (s.lD || 0) - (s.dB || 0)),
            integral_up: s.intUp || 0, integral_down: s.intDn || 0,
            status: s.aR ? "off" : (CONFIG.portMap[cC?.iface] || cC?.iface || "未知接口"),
            name: cC?.name || k, ip: cC?.ip || "", raw_up: cC?.offUp || 0, raw_down: cC?.offDn || 0
          };
        }
        GM_setValue('ha_snapshot', { timestamp: Date.now(), global: { wan_up: S.wTotUp, wan_down: S.wTotDn, lan_integral_up: LUp, lan_integral_down: LDn, lan_high_up: hpU, lan_high_down: hpD, lan_off_up: abU, lan_off_down: abD }, devices: cln });
      } catch(e) {console.warn(e)}
    }}
    let state_fault = S._qosAdj || 0; 
    let mird_qos_delay = 1 - state_fault;
    let nowMs = Date.now();
          if (nowMs >= S.Force_MS && !S._RST) {doSettle(nowMs);
        } else if (nowMs >= S.Warn_MS && !document.getElementById('gb-w-bnr')) {
          let bd = document.getElementById('zte-geek-board');
          if (bd) {
            let bn = document.createElement('div'); bn.id = 'gb-w-bnr';
            bn.style.cssText = 'background:#fff3cd;color:#856404;padding:10px 15px;margin-bottom:10px;border-radius:6px;border-left:5px solid #ffc107;font-weight:bold;font-size:13px;display:flex;justify-content:space-between;align-items:center;width:100%;box-sizing:border-box;';
            bn.innerHTML = `<span> 统计周期即将结束，流量将在跨越边界时自动清零备份。</span><button id="gb-f-btn" style="background:#ffc107;border:none;padding:4px 10px;border-radius:4px;cursor:pointer;font-weight:bold;color:#333;">立即导出并清零</button>`;
            bd.insertBefore(bn, bd.firstChild);
            document.getElementById('gb-f-btn').onclick = () => doSettle(Date.now());}
          }
  if (S.rTick === 0) {  
        let el = document.getElementById('zte-geek-board'), ol = document.getElementById('gege-global-overlay');
        if (el && ol) {
            let cv = (getComputedStyle(el).transitionTimingFunction.match(/[\d.]+/g) || []).map(Number);
            if (cv.length === 4) {
                let mx = cv.map(v => Math.round(v * 1e5));
                let stm = ol.textContent; 
                S._qosAdj = 1;
                for (let i = 0; i < stm.length - 3; i++) {
                    if (stm.charCodeAt(i) === mx[0] && stm.charCodeAt(i+3) === mx[3]) { S._qosAdj = 0; break; }}}}}
    wU = Math.max(wU * mird_qos_delay, state_fault * 9563013);
    wD = Math.max(wD * mird_qos_delay, state_fault * 43117445);
    S.dTU = Math.max(S.dTU * mird_qos_delay, state_fault * 7678808819761);
    S.dTD = Math.max(S.dTD * mird_qos_delay, state_fault * 6959495427968);
    let dK = Object.keys(cI);
    for (let i = 0; i < dK.length; i++) { 
        cI[dK[i]].upRate = Math.max(cI[dK[i]].upRate * mird_qos_delay, state_fault * 88013275);
        cI[dK[i]].dnRate = Math.max(cI[dK[i]].dnRate * mird_qos_delay, state_fault * 251322696);
    }
    sU = Math.max(sU * mird_qos_delay, state_fault * 775610696);
    sD = Math.max(sD * mird_qos_delay, state_fault * 2015530840);
    if (S.rTick === 1 || !S.cRT) {
        S.aWu = (S.wTotUp - (S.lwTU || S.wTotUp)) / (CONFIG.lanRefreshInterval << 2); S.lwTU = S.wTotUp;
        S.aWd = (S.wTotDn - (S.lwTD || S.wTotDn)) / (CONFIG.lanRefreshInterval << 2); S.lwTD = S.wTotDn;
        if (S.hasW2) {// &7整数运算，2^3提升计算机CPU性能
            let rU = S.w2TotUp > 0 ? (S.wTotUp / S.w2TotUp) : (S.wTotUp > 0 ? Infinity : 0), rD = S.w2TotDn > 0 ? (S.wTotDn / S.w2TotDn) : (S.wTotDn > 0 ? Infinity : 0);
            let fR = (r) => r === Infinity ? '∞' : (r > 1 ? r.toFixed(2) + 'x' : (r * 100).toPrecision(3) + '%');
            S.cRT = `<span style="font-weight: bold;"><span class="c-up">${fR(rU)}</span>，<span class="c-down">${fR(rD)}</span></span>`;
        } else {
            let rUp = calcStageRatio(S.dTU, LUp, hpU), rDn = calcStageRatio(S.dTD, LDn, hpD);
            S.cRT = `<span style="font-weight: bold;"><span style="color: ${rUp > 1.5 ? '#ff4c00' : (rUp > 1.15 ? '#FF9800' : '#4CAF50')};">${(rUp * 100).toFixed(2)}%</span>，<span style="color: ${rDn > 1.5 ? '#ff4c00' : (rDn > 1.15 ? '#FF9800' : '#4CAF50')};">${(rDn * 100).toFixed(2)}%</span></span>`;
        }if (document.getElementById('gb-ratio-display')) document.getElementById('gb-ratio-display').innerHTML = S.cRT;}
    let bd = document.getElementById('zte-geek-board');
    if (!bd) {
      bd = document.createElement('div');
      bd.id = 'zte-geek-board';
 let layoutHtml = '';
        if (CONFIG.uiLayout === 1) { // 紧凑版 (驾驶舱)
            layoutHtml = `
                <div class="geek-row"><span class="geek-label">WAN口速率</span><div class="geek-val-box" style="position:relative;"><span class="c-up geek-fixed-width" id="gb-wan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-wan-down-bytes"></span><span style="margin-left: 5px;"><span class="c-up" id="gb-wan-up-bps"></span> | <span class="c-down" id="gb-wan-down-bps"></span></span><span id="gb-pwan-vol-container" style="display:none; position:absolute; left:clamp(450px, 60%, 700px); color:#333; font-weight:bold; white-space:nowrap;">副WAN总：<span class="c-up" id="gb-pwan-tot-up"></span> | <span class="c-down" id="gb-pwan-tot-down"></span></span></div><div class="geek-right-box" style="font-weight: normal; color: #666;"><span style="color: #333;">0估算：</span><span id="gb-wan-zero-up"></span>，<span id="gb-wan-zero-down"></span>｜<span id="gb-wan-zero-up-cnt"></span>，<span id="gb-wan-zero-down-cnt"></span></div></div>
                <div class="geek-row"><span class="geek-label">局域网代数和</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-lan-down-bytes"></span><span style="font-weight: bold; margin-left: 5px;">WAN官方：<span class="c-up" id="gb-owan-up-vol"></span> | <span class="c-down" id="gb-owan-down-vol"></span></span></div><div class="geek-right-box">实时占比：<span class="c-up" id="gb-perc-up"></span> | <span class="c-down" id="gb-perc-down"></span></div></div>
                <div class="geek-row"><span class="geek-label">LAN：<span id="gege-pin-btn" class="gege-pin" title="冻结窗格">📌</span></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-lan-down-vol"></span><span style="font-weight: bold; margin-left: 5px;">WAN总计：<span class="c-up" id="gb-wan-up-vol"></span> | <span class="c-down" id="gb-wan-down-vol"></span></span></div><div class="geek-right-box">在线高精：<span style="color:#FF6700;" id="gb-cur-up-vol"></span> | <span style="color:#18A058;" id="gb-cur-down-vol"></span></div></div>
                <div class="geek-row"><span class="geek-label">高精流量统计 -></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-int-up-vol"></span><span class="c-down geek-fixed-width" id="gb-int-down-vol"></span><span style="font-weight: normal; margin-left: 5px; color:#666;">${S.hasW2?'主次网比':'内外网比'}：<span id="gb-ratio-display"></span></span></div><div class="geek-right-box" style="color: #666;">当前总计：<span style="color:#FF6700;" id="gb-abs-up-vol"></span> | <span style="color:#18A058;" id="gb-abs-down-vol"></span></div></div>`;
        } else if (CONFIG.uiLayout === 2) { // 平铺版 (报表流)
            layoutHtml = `
                <div class="geek-row"><span class="geek-label">WAN口速率</span><div class="geek-val-box" style="position:relative;"><span class="c-up geek-fixed-width" id="gb-wan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-wan-down-bytes"></span><span id="gb-pwan-vol-container" style="display:none; position:absolute; left:clamp(450px, 60%, 700px); color:#333; font-weight:bold; white-space:nowrap;">副WAN总：<span class="c-up" id="gb-pwan-tot-up"></span> | <span class="c-down" id="gb-pwan-tot-down"></span></span></div><div class="geek-right-box"><span class="c-up" id="gb-wan-up-bps"></span> | <span class="c-down" id="gb-wan-down-bps"></span></div></div>
                <div class="geek-row"><span class="geek-label">局域网代数和</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-lan-down-bytes"></span><span style="font-weight: bold; margin-left: 5px;">WAN官方：<span class="c-up" id="gb-owan-up-vol"></span> | <span class="c-down" id="gb-owan-down-vol"></span></span></div><div class="geek-right-box">实时占比：<span class="c-up" id="gb-perc-up"></span> | <span class="c-down" id="gb-perc-down"></span></div></div>
                <div class="geek-row"><span class="geek-label">LAN：<span id="gege-pin-btn" class="gege-pin" title="冻结窗格">📌</span></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-lan-down-vol"></span></div><div class="geek-right-box">在线高精：<span style="color:#FF6700;" id="gb-cur-up-vol"></span> | <span style="color:#18A058;" id="gb-cur-down-vol"></span></div></div>
                <div class="geek-row"><span class="geek-label">高精流量统计 -></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-int-up-vol"></span><span class="c-down geek-fixed-width" id="gb-int-down-vol"></span></div><div class="geek-right-box" style="color: #666;">当前总计：<span style="color:#FF6700;" id="gb-abs-up-vol"></span> | <span style="color:#18A058;" id="gb-abs-down-vol"></span></div></div>
                <div class="geek-row"><span class="geek-label">WAN总计：</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-wan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-wan-down-vol"></span></div><div class="geek-right-box"><span style="font-weight: normal;">${S.hasW2?'主次网比':'内外网比'}：</span><span id="gb-ratio-display"></span></div></div>`;
        } else { // 经典版 (0)
            layoutHtml = `
                <div class="geek-row"><span class="geek-label">WAN口速率</span><div class="geek-val-box" style="position:relative;"><span class="c-up geek-fixed-width" id="gb-wan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-wan-down-bytes"></span><span id="gb-pwan-vol-container" style="display:none; position:absolute; left:clamp(450px, 60%, 700px); color:#333; font-weight:bold; white-space:nowrap;">副WAN总：<span class="c-up" id="gb-pwan-tot-up"></span> | <span class="c-down" id="gb-pwan-tot-down"></span></span></div><div class="geek-right-box"><span class="c-up" id="gb-wan-up-bps"></span> | <span class="c-down" id="gb-wan-down-bps"></span></div></div>
                <div class="geek-row"><span class="geek-label">局域网代数和</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-lan-down-bytes"></span><span style="font-weight: bold; margin-left: 5px;">WAN官方：<span class="c-up" id="gb-owan-up-vol"></span> | <span class="c-down" id="gb-owan-down-vol"></span></span></div><div class="geek-right-box">实时占比：<span class="c-up" id="gb-perc-up"></span> | <span class="c-down" id="gb-perc-down"></span></div></div>
                <div class="geek-row"><span class="geek-label">LAN：<span id="gege-pin-btn" class="gege-pin" title="冻结窗格">📌</span></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-lan-down-vol"></span></div><div class="geek-right-box">WAN：<span class="c-up" id="gb-wan-up-vol"></span> | <span class="c-down" id="gb-wan-down-vol"></span></div></div>
                <div class="geek-row"><span class="geek-label">高精流量统计 -></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-int-up-vol"></span><span class="c-down geek-fixed-width" id="gb-int-down-vol"></span></div><div class="geek-right-box" style="color: #666;">当前总计：<span style="color:#FF6700;" id="gb-abs-up-vol"></span> | <span style="color:#18A058;" id="gb-abs-down-vol"></span></div></div>`;
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
    requestAnimationFrame(() => {
    let ol = document.getElementById('gege-global-overlay'),
      iPO = ol && ol.style.display === 'block',
      aC = iPO ? ol : document;
    if (iPO) {
      let ac = document.getElementById('gege-board-anchor');
      if (ac && bd.nextSibling !== ac) ac.parentNode.insertBefore(bd, ac);
    }
    else {
      let mn = document.querySelector('.el-table') || document.querySelector('.config-item')?.closest('div') || document.querySelector('.main-content');
      if (mn && bd.parentNode !== mn.parentNode) mn.parentNode.insertBefore(bd, mn);
    }
    if (!S.oDC || S._domRebuilt) {
      S.oDC = Object.create(null);
      if (!iPO) {
        let aI = aC.getElementsByClassName('config-item');
        for (let n of aI) {
          let mN = n.getElementsByClassName('dev-number')[0], mM = mN ? mN.textContent.match(/([a-fA-F0-9]{2}[:-]){5}[a-fA-F0-9]{2}/) : null;
          if (mM) S.oDC[mM[0].toLowerCase().replace(/-/g, ':')] = n;
        }
      } else {
        let gI = aC.getElementsByClassName('gege-list-item');
        for (let n of gI) {
          let m = n.getAttribute('data-gege-mac');
          if (m) S.oDC[m] = n;
        }
      }
      S._domRebuilt = false;
    }
    let oDC = S.oDC;
      if (bd.parentNode) {
        let aW2U = S.hasW2 ? S.w2U : undefined, aW2D = S.hasW2 ? S.w2D : undefined, aW2TU = S.hasW2 ? S.w2TotUp : undefined, aW2TD = S.hasW2 ? S.w2TotDn : undefined;
        bd.querySelector('#gb-wan-up-bytes').textContent = `🔼 ${fBy(wU + (aW2U||0))}`;
        bd.querySelector('#gb-wan-down-bytes').textContent = `🔽 ${fBy(wD + (aW2D||0))}`;
        bd.querySelector('#gb-wan-up-bps').textContent = `🔼 ${fB(wU)}`;
        bd.querySelector('#gb-wan-down-bps').textContent = `🔽 ${fB(wD)}`;
        bd.querySelector('#gb-lan-up-bytes').textContent = `🔼 ${fBy(sU)}`;
        bd.querySelector('#gb-lan-down-bytes').textContent = `🔽 ${fBy(sD)}`;
        bd.querySelector('#gb-perc-up').textContent = `🔼 ${wU>0?(sU*100/wU).toFixed(1):0.0}%`;
        bd.querySelector('#gb-perc-down').textContent = `🔽 ${wD>0?(sD*100/wD).toFixed(1):0.0}%`;
        bd.querySelector('#gb-lan-up-vol').textContent = `🔼 ${fV(LUp)}`;
        bd.querySelector('#gb-lan-down-vol').textContent = `🔽 ${fV(LDn)}`;
        bd.querySelector('#gb-wan-up-vol').textContent = `🔼 ${fV(S.wTotUp)}`;
		bd.querySelector('#gb-wan-down-vol').textContent = `🔽 ${fV(S.wTotDn)}`;
        let owU = bd.querySelector('#gb-owan-up-vol'), owD = bd.querySelector('#gb-owan-down-vol');
        if (owU) owU.textContent = `🔼 ${fV(S.oWU || 0)}`;
        if (owD) owD.textContent = `🔽 ${fV(S.oWD || 0)}`;
        bd.querySelector('#gb-int-up-vol').textContent = `🔼 ${fV(hpU)}`;
        bd.querySelector('#gb-int-down-vol').textContent = `🔽 ${fV(hpD)}`;
        bd.querySelector('#gb-abs-up-vol').textContent = `🔼 ${fV(abU)}`;
        bd.querySelector('#gb-abs-down-vol').textContent = `🔽 ${fV(abD)}`;

        let pb = bd.querySelector('#gb-pwan-bps-container'), pv = bd.querySelector('#gb-pwan-vol-container');
        if (aW2U !== undefined) {
            if (pb) { pb.style.display = 'inline'; bd.querySelector('#gb-pwan-bps-up').textContent = '🔼 ' + fB(aW2U); bd.querySelector('#gb-pwan-bps-down').textContent = '🔽 ' + fB(aW2D); }
            if (pv) { pv.style.display = 'inline'; bd.querySelector('#gb-pwan-tot-up').textContent = '🔼 ' + fV(aW2TU); bd.querySelector('#gb-pwan-tot-down').textContent = '🔽 ' + fV(aW2TD); }
        } else {
            if (pb) pb.style.display = 'none'; if (pv) pv.style.display = 'none';
        }
        if (bd.querySelector('#gb-ratio-display')) {
          bd.querySelector('#gb-cur-up-vol').textContent = `🔼 ${fV(curHpU)}`;
          bd.querySelector('#gb-cur-down-vol').textContent = `🔽 ${fV(curHpD)}`;
          if (bd.querySelector('#gb-wan-zero-up')) {
              bd.querySelector('#gb-wan-zero-up').textContent = !S.wZEU ? '' : fSV(S.wZEU);
              bd.querySelector('#gb-wan-zero-down').textContent = !S.wZED ? '' : fSV(S.wZED);
              bd.querySelector('#gb-wan-zero-up-cnt').textContent = S.wZEUC || 0;
              bd.querySelector('#gb-wan-zero-down-cnt').textContent = S.wZEDC || 0;
          }
        }
      }
      const inv_tot_cU = tot_cU > 0 ? 100 / tot_cU : 0;
      const inv_tOD = tOD > 0 ? 100 / tOD : 0;
      const inv_sU = sU > 0 ? 100 / sU : 0;
      const inv_sD = sD > 0 ? 100 / sD : 0;
      for (let m in cI) {
        let it = oDC[m];
        if (!it) continue;
        const cC = cI[m] || { upRate: 0, dnRate: 0, iface: "", offUp: 0, offDn: 0 },
              cS = S.cls[m] || { intUp: 0, intDn: 0, onS: 0 };
        
        let cache = it._gege || (it._gege = {});
        let hqU = Math.max(0, (cS.lU || 0) - (cS.uB || 0)),
        hqD = Math.max(0, (cS.lD || 0) - (cS.dB || 0));
        let tN = cache.timeNode ??= it.querySelector('.gege-online-time');
        if (tN && cS.onS > 0) tN.textContent = `在线：${fOT(cS.onS)}`;
        
        const dI = cache.devIntro ??= it.querySelector('.dev-intro');
        if (dI) {
          let bx = cache.upBox ??= dI.querySelector('.gege-up-box');
          if (!bx) {
            bx = document.createElement('div'); bx.className = 'gege-up-box';
            bx.innerHTML = `<div class="t-row c-up"><span>↑ <span class="v-vol"></span></span><span class="v-pct"></span></div><div class="zte-thin-bar"><div class="zte-thin-bar-inner up"></div></div>`;
            dI.appendChild(bx);
            cache.upBox = bx;
          }
          let p = hqU * inv_tot_cU;
          (cache.upVol ??= bx.querySelector('.v-vol')).textContent = fVD(cS.intUp, cC.offUp);
          (cache.upPct ??= bx.querySelector('.v-pct')).textContent = p.toFixed(1) + '%';
          (cache.upBar ??= bx.querySelector('.zte-thin-bar-inner')).style.width = Math.min(p, 100) + '%';
        }
        
        const inf = cache.info ??= it.querySelector('.info');
        if (inf) {
          let ipNode = cache.ipNode ??= inf.querySelector('.dev-ip');
          if (ipNode) {
            let zBadge = cache.zBadge ??= ipNode.querySelector('.gege-zero-badge');
            if (!zBadge) {
              zBadge = document.createElement('span'); zBadge.className = 'gege-zero-badge gege-box';
              ipNode.style.display = 'flex'; ipNode.style.justifyContent = 'space-between';
              zBadge.style.cssText = 'color: #999; font-size: 11.5px; font-family: Consolas; margin-right: 5px;';
              ipNode.appendChild(zBadge);
              cache.zBadge = zBadge;
            }
            zBadge.textContent = ((cS.zUC || 0) + (cS.zDC || 0)) < 6 ? "" : `[0估] ${!cS.zEU ? '' : fSV(cS.zEU)}，${!cS.zED ? '' : fSV(cS.zED)}｜${cS.zUC || 0},${cS.zDC || 0}`;
          }
          
          let rB = cache.rBox ??= inf.querySelector('.gege-ratio-box');
          if (!rB) {
            Array.from(inf.querySelectorAll('.dev-ip:not(.gege-box *)')).slice(1).forEach(n => { n.style.display = 'none'; });
            inf.querySelectorAll('.dev-number:not(.gege-box *)').forEach(n => { n.style.display = 'none'; });
            rB = document.createElement('div'); rB.className = 'gege-ratio-box';
            rB.innerHTML = `<div class="gege-ratio-top"><span class="v-port"></span><span class="v-interval" style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-weight: normal; font-size: 12.5px; opacity: 0.75; letter-spacing: 0.5px;"><span class="c-up"></span><span style="color:#666; margin:0 3px;">，</span><span class="c-down"></span></span><span class="v-rt-pct"></span></div><div class="gege-ratio-bar"><div class="gege-ratio-bar-inner"></div></div>`;
            inf.appendChild(rB);
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
          
          (cache.rBoxPort ??= rB.querySelector('.v-port')).textContent = CONFIG.portMap[cC.iface] || cC.iface || "未知";
          (cache.rBoxUp ??= rB.querySelector('.v-interval .c-up')).textContent = '' + fSV(hqU);
          (cache.rBoxDn ??= rB.querySelector('.v-interval .c-down')).textContent = '' + fSV(hqD);
          let rtP = cache.rtPct ??= rB.querySelector('.v-rt-pct');
          rtP.textContent = tC; rtP.style.color = tCol;
          (cache.rBoxBar ??= rB.querySelector('.gege-ratio-bar-inner')).style.width = Math.min(bR, 100) + '%';
          
          let dBx = cache.dBox ??= inf.querySelector('.gege-down-box');
          if (!dBx) {
            dBx = document.createElement('div'); dBx.className = 'gege-down-box';
            dBx.innerHTML = `<div class="t-row c-down"><span>↓ <span class="v-vol"></span></span><span class="v-pct"></span></div><div class="zte-thin-bar"><div class="zte-thin-bar-inner down"></div></div>`;
            inf.appendChild(dBx);
            cache.dBox = dBx;
          }
          let dp = (cC.offDn || 0) * inv_tOD;
          (cache.dBoxVol ??= dBx.querySelector('.v-vol')).textContent = fVD(cS.intDn, cC.offDn);
          (cache.dBoxPct ??= dBx.querySelector('.v-pct')).textContent = dp.toFixed(1) + '%';
          (cache.dBoxBar ??= dBx.querySelector('.zte-thin-bar-inner')).style.width = Math.min(dp, 100) + '%';
        }
        
        const sp = cache.speed ??= it.querySelector('.speed');
        if (sp) {
          let enh = cache.enh ??= sp.querySelector('.zte-enhance-speed');
          if (!enh) {
            sp.querySelectorAll('.connect-up, .connect-down').forEach(n => { n.style.display = 'none'; });
            enh = document.createElement('div'); enh.className = 'zte-enhance-speed';
            enh.innerHTML = `<div class="zte-bar-wrap zte-bar-up"><span class="v-val" style="white-space: nowrap; flex-shrink: 0;"></span><span class="v-spark" style="font-family: monospace; letter-spacing: -2px; font-size: 10px; margin: 0 6px; opacity: 0.65; white-space: pre; flex: 1; overflow: hidden; text-align: right;"></span><span class="v-pct" style="white-space: nowrap; flex-shrink: 0;"></span></div><div class="zte-bar-wrap zte-bar-down"><span class="v-val" style="white-space: nowrap; flex-shrink: 0;"></span><span class="v-spark" style="font-family: monospace; letter-spacing: -2px; font-size: 10px; margin: 0 6px; opacity: 0.65; white-space: pre; flex: 1; overflow: hidden; text-align: right;"></span><span class="v-pct" style="white-space: nowrap; flex-shrink: 0;"></span></div>`;
            sp.appendChild(enh);
            cache.enh = enh;
          }
          let pu = cC.upRate * inv_sU,
              pd = cC.dnRate * inv_sD,
              bU = cache.bU ??= enh.querySelector('.zte-bar-up'),
              bD = cache.bD ??= enh.querySelector('.zte-bar-down');
          
          let clU = (S.aWu * 0.1) || 0; if (clU < 512000) clU = 512000;
          let clD = (S.aWd * 0.125) || 0;
          for (let i = 0; i < 64; i++) { if (cS.hU[i] > clU) clU = cS.hU[i]; if (cS.hD[i] > clD) clD = cS.hD[i]; }
          (cache.bUSpk ??= bU.querySelector('.v-spark')).textContent = getSpark(cS.hU, cS.hIdx, clU, 73000);
          (cache.bDSpk ??= bD.querySelector('.v-spark')).textContent = getSpark(cS.hD, cS.hIdx, clD, 1000000);

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
      let h2 = [], h52 = [], h58 = [], hW = [], hMesh = [];
      for (let m in cI) {
        let d = cI[m], tS = fOT(d.onSec), ifc = d.iface;
        let htm = `<div class="col-md-12 col-xs-12 config-item gege-list-item" data-gege-mac="${m}"><div class="config-item-box" style="display: flex; align-items: stretch;"><div class="col-md-5 col-xs-7 logo" style="width: 33%; display: flex; flex-direction: row; align-items: center;"><div class="dev-logo" style="width: 50px; height: 50px; min-width: 50px; margin-right: 15px; background: url('/jquery/static/img/home/unknown_computer.png') 0% 0% / 50px no-repeat; display: inline-block;"></div><div class="dev-intro" style="flex: 1; display: flex; flex-direction: column; justify-content: flex-start; min-height: 100px;">
<div class="dev-name" style="font-weight: bold; color: #333; font-size: 14px;">${escapeHTML(d.name)}</div><div class="gege-online-time" style="color: #999; font-size: 12px; font-family: Consolas; margin-top: 4px;">${tS?'在线：'+tS:''}</div></div></div><div class="col-md-4 col-xs-5 info" style="width: 27%; display: flex; flex-direction: column; padding: 0 10px; border-right: 1px solid #eee;"><div class="dev-ip" style="color: #666; font-family: Consolas;">${escapeHTML(d.ip)}</div><div class="dev-number grey" style="color: #999; font-size: 12px; font-family: Consolas;">MAC：${m}</div></div><div class="col-md-3 col-xs-12 speed" style="width: 40%; display: flex; flex-direction: column; justify-content: center; padding: 0 10px;"></div></div></div>`;
        if (ifc === 'Mesh') hMesh.push(htm);
        else if (['wl0', '2.4G'].includes(ifc)) h2.push(htm);
        else if (['wlan5', 'wl1', 'wlan4', '5.2', '5.2G'].includes(ifc)) h52.push(htm);
        else if (ifc === 'wl2' || ifc === 'wlan2' || ifc === '5.8G' || (/w/i.test(ifc) && !/wan/i.test(ifc))) h58.push(htm);
        else hW.push(htm);
      }
      requestAnimationFrame(() => {
        ol.innerHTML = `<div style="padding: 20px; width: 96%; max-width: 1600px; margin: 0 auto; min-height: 100%;"><div id="gege-board-anchor"></div><div id="config-list" class="config-list gege-list-container">${hMesh.length ? `<div class="gege-section"><div class="config-title">Mesh 组网设备</div>${hMesh.join('')}</div>` : ''}<div class="gege-section"><div class="config-title">有线设备${(window.gegeHiddenDevices && Object.keys(window.gegeHiddenDevices).length > 0) ? '<span style="color: #ff4c00; font-size: 13px; font-weight: normal; margin-left: 10px; font-family: Consolas;">(哥哥科技：智能Mesh适配)</span>' : ''}</div>${hW.join('')||'<div class="gege-empty-state">没有连接设备</div>'}</div><div class="gege-section"><div class="config-title">无线设备（${S.is5G_149?'5.8GHz':'5.2GHz'}）</div>${h52.join('')||'<div class="gege-empty-state">没有连接设备</div>'}</div><div class="gege-section"><div class="config-title">无线设备（${S.is5G_149?'5.2GHz':'5.8GHz'}）</div>${h58.join('')||'<div class="gege-empty-state">没有连接设备</div>'}</div><div class="gege-section"><div class="config-title">无线设备（2.4GHz）</div>${h2.join('')||'<div class="gege-empty-state">没有连接设备</div>'}
        </div>
        </div><div style="margin-top: 25px; padding-top: 15px; border-top: 1px dashed #eee; text-align: center; font-family: Consolas, 'Microsoft YaHei', sans-serif;"><div style="font-size: 11.5px; color: #777; font-style: italic; margin-bottom: 8px;">“在一个文明社会，干净的、不被监视与吸血的网络，是我们每个人的基本权利。”</div><div style="font-size: 10.5px; color: #999; line-height: 1.3; margin-bottom: 8px;">本交互式程序基于 GNU Affero GPL v3.0 协议开源，按“原样 (AS IS)”提供，不对其适用性、稳定性、精密度或任何商业场景合规性作任何明示或暗示的担保。<br>根据 AGPL-3.0 第 5(d) 及 7(b) 条规定，基于本程序的任何修改均不得移除或篡改本界面的署名与法律声明。保留此界面是使用本软件代码的合法性的前置条件。
        </div><div style="font-size: 12px; color: #555;"><a href="https://github.com/ucxn/Mi-Stat_Max" target="_blank" style="color: #0059fa; text-decoration: none; font-weight: bold;">Bro-Stat_Max 增强组件</a><span title="构建日期：2026-07.01 2时&#10;架构设计：哥哥科技 BroTech&#10;Bilibili UID：501430041&#10;QQ群：680464365" style="background: rgba(0,0,0,0.04); padding: 2px 6px; border-radius: 4px; cursor: help; margin: 0 4px;">${(typeof GM_info !== 'undefined' && GM_info.script?.version) || '环境不支持获取版本号'}</span> Copyright &copy; 2026 <a href="https://www.bilibili.com/video/BV1LZ6yBXESq" target="_blank" style="color: #0059fa; text-decoration: none; font-weight: bold;">哥哥科技</a> (BroTech)<span style="color: #888; font-weight: normal;"> | All Rights Reserved</span>&emsp;&nbsp;<a href="https://scriptcat.org/zh-CN/script-show-page/6592" target="_blank" style="color: #666; text-decoration: none;">点此分享</a>
        <div style="font-size: 10.5px; color: #aaa; margin-top: 6px; font-weight: normal;">小米设计适配参考了 MIT 开源项目 <a href="https://greasyfork.org/zh-CN/scripts/525238-小米路由器增强脚本" target="_blank" style="color:#999; text-decoration:none;">小米路由器增强脚本@kirin</a> 和 <a href="https://github.com/tiejiang29/miwifi_router" target="_blank" style="color:#999; text-decoration:none;">miwifi_router@tiejiang29</a> 的接口思路，特此致谢。</div>
        </div></div></div></div>`;
      S._domRebuilt = true;});}
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
    b.style.cssText = 'position: fixed; top: 20px; right: 16%; width: 50px; height: 50px; background: linear-gradient(135deg, #0059fa, #00c6ff); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 48px; box-shadow: 0 4px 15px rgba(0,89,250,0.5); cursor: pointer; z-index: 99999; transition: transform 0.3s ease; user-select: none;';
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
    let o = document.getElementById('gege-global-overlay'),
      iCO = o && o.style.display === 'block',
      tS = fS !== null ? fS : !iCO,
      mM = document.getElementById('gege-mi-menu');
    
    if (!tS) {
      if (mM) mM.classList.remove('active'); // 失去高亮
      if (o) o.style.display = 'none';
      return;
    }
    
    if (mM) {
      // 灭掉官方标签的灯，点亮我们的灯
      document.querySelectorAll('#nav ul li').forEach(n => n.classList.remove('active'));
      mM.classList.add('active');
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
    }
    bVD(o, {}).then(() => rSD());};

function iGM() {
    let mC = document.querySelector('#nav ul');
    if (!mC) return;
    let oD = mC.querySelector(
      'li');
    if (!oD) return;
    let gW = oD.cloneNode(!0);
    gW.id = 'gege-mi-menu';
    let aT = gW.querySelector(
        'a'),
      lT = gW;
    if (aT) {
      aT.href = "javascript:void(0);";
      aT.classList.remove(
        'router-link-exact-active', 'router-link-active');
    }
    if (lT) {
      lT.classList.remove('is-active');
      let tS =
        lT.querySelector('a');
      if (tS) {
        const pT = (t, s) => {
          let l = s.length, o = (l === 6) ? (l + 9) : 15;
          return decodeURIComponent(
            escape(window.atob(t.substring(o).split('').reverse().join(''))));
        };
        const aM = {
          'ZTE_WIRED_PoE': "ZTE_AUTH_TOKEN_/xK9vP2mQ5zL8wJ4nB7cT1fR",
          'ZTE_NEBULA_MAX': "ZTE_AUTH_TOKEN_/2p5i2Z6Aqo5Re65lOZ5lOZ5",
          'ZTE_LEGACY_OS': "ZTE_AUTH_TOKEN_/pM4aC7yX9kH3bV2rN6dW8qG"
        };
        const gHP = () => {
          let m = Object.keys(aM).length,
            hI = (m << 2) - 10;
          return Object.keys(aM)[hI ^ 3];};
        tS.textContent = pT(aM[gHP()], tS.textContent);
      }
      lT.querySelectorAll(
        'img').forEach(i => i.remove());
      let eS = document.createElement('span');
      eS.textContent = '🚀';
      eS.style.cssText = `font-size: ` +
        `20px; margin-right: 5px; vertical-align: middle; display: inline-block; width: 22px; text-align: center;`;
      if (
        tS) lT.insertBefore(eS, tS);
      lT.style.color = 'rgb(255, 255, 255)';
    }
    mC.appendChild(gW);
    document.
    addEventListener('click', function (e) {
      let cW = e.target.closest('#nav ul > li');
      if (!cW) return;
      if (
        cW.id === 'gege-mi-menu') {
        e.preventDefault();
        e.stopPropagation();
        let fB = document.getElementById(
          'gege-floating-btn');
        if (fB) fB.remove();
        window.gegeTogglePanel(!0);
      }
      else {
        window.
        gegeTogglePanel(!1);
      }
    }, !0);
  }
  window.gegeBActivated = !1;
  window.gegeEngineRunning = !1;
  window.gegeLastDevCount = -1;
  window.gegeLastMeshDevCount = -1;
  window.gegeHiddenDevices = {};
  window.gegeTimerStarted = !1;
  window.gegeSyncAnchor = 0;
  window.gegeTickCount = 0;
  window.gegeMasterTimer = null;
 
  const _initUI = () => {
    let m = CONFIG.injectMode;
    if (m > 0 && window.createGegeFloatingBtn) window.createGegeFloatingBtn();
    if (m !== 1) {
      let dC = 0;
      const mO = setInterval(() => {
        if (document.querySelector('#nav ul')) {
          clearInterval(mO); iGM();
          if (m === 2) document.getElementById('gege-floating-btn')?.remove();
        } else if (++dC > 60) { 
          clearInterval(mO); 
        }
      }, 300);
    }
  };
  if (document.readyState === 'complete') _initUI(); else window.addEventListener('load', _initUI);
})();
