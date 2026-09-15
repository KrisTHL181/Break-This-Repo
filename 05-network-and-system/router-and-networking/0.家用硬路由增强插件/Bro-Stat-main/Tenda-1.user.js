// ==UserScript==
// @name            腾达路由器增强 by 哥哥科技
// @name:en         Bro-Stat-Tenda
// @namespace       ucxn
// @version         5.9.9
// @description     哥哥科技 QQ群 680464365
// @description:en  https://github.com/ucxn/Bro-Stat
// @author          哥哥科技 space.bilibili.com/501430041
// @noframes
// @tag             路由器 腾达 网络 监控 统计 数据 可视化 极客 增强 Tenda HA 智能 定时 后台
// @icon            https://scriptcat.org/api/v2/resource/image/duygQktL5QjWtkLc
// @include         http://10.*.*.*
// @include         http://192.168.*.*
// @include         http://172.16.*
// @include         https://10.*.*.*
// @include         https://192.168.*.*
// @match           *://tendawifi.com/*
// @match           *://www.tendawifi.com/*
// @match           *://*.tenda.cn/*
// @match           *://re.tenda.cn/*
// @include         https://172.16.*
// @run-at          document-start
// @grant           GM_setValue
// @grant           GM_getValue
// @storageName     GBNPA_Storage
// @license         AGPL-3.0-or-later
// @downloadURL     https://github.com/ucxn/Bro-Stat/raw/refs/heads/main/Tenda-1.user.js
// @updateURL       https://github.com/ucxn/Bro-Stat/raw/refs/heads/main/Tenda-1.user.js

// ==/UserScript==

(function () {
  'use strict';
  console.log("🚀 哥哥科技 V5.9.9 引擎已装载...");

  // ======== [0] 用户极客环境变量配置区 ========
  const CONFIG = {
    readSaveData: 1, // 【历史记录】 从本地长期历史读档 [自动保存！] | 0: 新局模式
    uiLayout: 2,//【面板拓扑结构】 0: 经典版 | 1: 详细紧凑版(驾驶舱美学) | 2: 详细平铺版(报表流美学)
    injectMode: 3, //1: 优先，10秒悬浮舱(D)| 3：强制模式
    calcMode: 1,// 1: 上行/下行倍数模式, 0: 上行占总和比例模式
    ratioExtremeUp: 10, // 极端上传判定阈值 (> 1000%)
    ratioWarnUp: 0.07, // 重度上传警告阈值 (> 7%)
    ratioExtremeDown: 0.01, // 极端下载判定阈值 (< 1%)
    ratioThreshold: 7, // (仅calcMode=0时有效) 上传占比报警阈值(%)
    lanRefreshInterval: 3, // LAN设备速率有效刷新周期(秒)，可独立配置；建议大于wanRefreshInterval
    wanRefreshInterval: 1, // WAN刷新周期(秒)，主调度时钟；只要快于LAN即可任意配置
    宽带最大外网上行速率: 3e8,
    宽带最大外网下行速率: 24e8, // 配置外网最大上传|下载比特(bit/bps)速率，请略微大于真实值；500兆为5e8，一千兆1e9
    周期类型: 'M', // 'M'(每月), 'W'(每周), 其它任意字符：不开启周期重置+自动导出功能
    周_天设置: 1, // M: 1~31号; W: 0~6(周日~周六); D: 间隔天数(如 7)
    报告时间: -1080, // 提示时间：相对周期0点的偏移分钟数。(如 -4320 代表提前 3 天) 设置相对指定日期的下个周期起点的时间偏移量
    自动导出: 0, // 强制导出：相对周期0点的偏移分钟数。(如 W模式+锚点6(周六)+偏移-180 = 周五 21:00 强制导出清零)
    时区补偿: 28800000, // 默认 UTC+8 时区补偿量。
    盲漫游: undefined, //是否拦截漫游可能产生的异常高网速
    portMap: {
      "wire": "有线",
      "2.4G": "2.4G",
      "6G": "6G"
    }
  };

const ESC_MAP = { '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' };
  function escapeHTML(str) {
    return str ? String(str).replace(/[&<>'"]/g, m => ESC_MAP[m]) : '';
  }

let _saved = null;
  if (CONFIG.readSaveData === 1 && typeof GM_getValue !== 'undefined') {
    try { let sp = GM_getValue('ha_snapshot', null); _saved = sp && sp.timestamp > (GM_getValue('gege_reset_ms', 0) || 0) ? sp : null; } catch (e) { console.warn(e); }
  }

  const S = {
    lt: 0, wInstUp: 0, wInstDn: 0,
    wTotUp: _saved?.global?.wan_up || 0,
    wTotDn: _saved?.global?.wan_down || 0,
    cls: {}, isPinned: !0, pI: null,
    is5G_149: !1, RSSI频率修正: undefined,
    Warn_MS: 0, Force_MS: 0, _RST: !1,
    aWu: 0, aWd: 0, lwTU: 0, lwTD: 0, cSnap: null,
    总上行图: new Float64Array(8192), 总下行图: new Float64Array(8192), 总图点数: 0,
    wMaxU: 0, wMaxD: 0, wMinU: Infinity, wMinD: Infinity, 图表拖: null, 图表待画: 0,
    wDue: 0, lDue: 0, haDue: 0
  };

  const _w = typeof unsafeWindow !== 'undefined' ? unsafeWindow : window;
  S.calcTime = (L) => {
    S.Force_MS = (CONFIG.周期类型 === 'M' ? Date.UTC(new Date(L).getUTCFullYear(), new Date(L).getUTCMonth() + (L >= Date.UTC(new Date(L).getUTCFullYear(), new Date(L).getUTCMonth(), CONFIG.周_天设置) ? 1 : 0), CONFIG.周_天设置) : (CONFIG.周期类型 === 'W' ? Date.UTC(new Date(L).getUTCFullYear(), new Date(L).getUTCMonth(), new Date(L).getUTCDate()) + ((CONFIG.周_天设置 - new Date(L).getUTCDay() <= 0 ? CONFIG.周_天设置 - new Date(L).getUTCDay() + 7 : CONFIG.周_天设置 - new Date(L).getUTCDay()) * 86400000) : (CONFIG.周期类型 === 'D' ? Date.UTC(new Date(L).getUTCFullYear(), new Date(L).getUTCMonth(), new Date(L).getUTCDate()) + CONFIG.周_天设置 * 86400000 : Infinity))) - CONFIG.时区补偿;
    S.Warn_MS = S.Force_MS + CONFIG.报告时间 * 60000;
    S.Force_MS += CONFIG.自动导出 * 60000;
  };S.calcTime((typeof GM_getValue !== 'undefined' && GM_getValue('gege_reset_ms')) ? (GM_getValue('gege_reset_ms') + CONFIG.时区补偿) : Date.now() + CONFIG.时区补偿);
  if (typeof GM_getValue !== 'undefined' && GM_getValue('gege_reset_ms') >= Math.min(S.Warn_MS, S.Force_MS)) S.calcTime(S.Force_MS - CONFIG.自动导出 * 60000 + 1000 + CONFIG.时区补偿);
  const 版本号 = (typeof GM_info !== 'undefined' && GM_info.script?.version) || '环境不支持获取版本号';

  function fB(bps) {
        if (bps > 1e9) return `${Math.round(bps * 1e-6)} Mbit/s`;
        if (bps > 1e6) return `${(bps * 1e-6).toFixed(2)} Mbps`;
        return `${(bps * 1e-3).toFixed(2)} kbps`;
    }

  function fBy(bps) {
        if (bps === 1) return '拦截中…';
        if (bps > 8388608) return `${(bps * 1.1920928955078125e-7).toFixed(2)} M/s`;
        if (bps > 8191) return `${Math.round(bps * 0.0001220703125)} K/s`;
        return bps ? `${(bps * 0.0001220703125).toPrecision(3)} K/s` : '0 K/s';
    }
  function fV(bits) {
        if (bits > 83886080000) return `${(bits / 8589934592).toFixed(4)} G`;
        if (bits > 8388608000) return `${(bits / 8388608).toFixed(1)} M`;
        if (bits > 8388608) return `${(bits / 8388608).toFixed(4)} M`;
        if (bits > 8192) return `${(bits / 8192).toFixed(2)} K`;
        return `${Math.round(bits / 8)} B`;
    }

  function fSV(bits) {
    if (bits > 84607500287) return `${(bits / 8589934592).toPrecision(4)}GiB`;
    if (bits > 8388608000) return `${Math.round(bits / 8388608)}MiB`;
    if (bits > 8388608) return `${(bits / 8388608).toPrecision(4)}MiB`;
    if (bits > 8191) return `${(bits / 8192).toFixed(1)}KiB`;
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

  function nM(m) {
    return m ? m.toLowerCase().replace(/-/g, ':').replace(/\s/g, '') : '';
  }

  const st = document.createElement('style');
  st.innerHTML = `.config-item{
        clear:both;}.config-item-box{display:flex!important;
        align-items:stretch!important;padding-bottom:
        12px!important;}.config-item .logo{width:33%!important;
        float:none!important;display:flex!important;flex-direction:row;}.config-item .dev-intro{flex:1;display:flex!important;flex-direction:column;justify-content:flex-start;min-height:50px;padding-bottom:0!important;margin-bottom:0!important;}.config-item .info{width:27%!important;float:none!important;display:flex!important;flex-direction:column;justify-content:flex-start;padding:0 10px!important;border-right:1px solid #eee;}.config-item .speed{width:40%!important;float:none!important;display:flex!important;flex-direction:column;justify-content:center;padding:0 10px!important;}.geek-row{display:flex;justify-content:space-between;align-items:center;white-space:nowrap;height:20px;}
    .geek-label{width:110px;color:#333;font-weight:bold;}.geek-val-box{flex:1;display:flex;gap:15px;margin-left:10px;}.geek-fixed-width{display:inline-block;width:120px;}.geek-right-box{text-align:right;min-width:220px;font-weight:bold;}.c-up{color:#ff4c00;}.c-down{color:#0059fa;}.gege-up-box,.gege-down-box{margin-top:auto!important;margin-bottom:0!important;width:95%;}.gege-ratio-box{margin-top:10px;width:95%;margin-bottom:5px;}.t-row{font-size:12px;font-weight:bold;margin-bottom:2px;display:flex;justify-content:space-between;font-family:system-ui, sans-serif;}.zte-thin-bar{width:100%;height:3px;background:rgba(0,0,0,0.05);border-radius:1.5px;overflow:hidden;}.zte-thin-bar-inner{height:100%;transition:width 0.5s ease-out;}.zte-thin-bar-inner.up{background:#ff4c00;}.zte-thin-bar-inner.down{background:#0059fa;}.gege-ratio-top{display:flex;justify-content:space-between;font-size:12px;font-weight:bold;margin-bottom:2px;}.gege-ratio-bar{width:100%;height:4px;background:#0059fa;border-radius:2px;overflow:hidden;}.gege-ratio-bar-inner{height:100%;background:#ff4c00;transition:width 0.5s ease-out;}.zte-enhance-speed{display:flex;flex-direction:column;gap:6px;width:100%;font-family:system-ui, sans-serif;}
    .zte-bar-wrap{position:relative;width:100%;border-radius:4px;border:1px solid;font-size:13px;font-weight:bold;overflow:hidden;padding:3px 8px;display:flex;justify-content:space-between;align-items:center;z-index:1;box-sizing:border-box;}.zte-bar-wrap span{font-size:inherit;font-weight:inherit;}.zte-bar-up{color:#ff4c00;border-color:rgba(255,76,0,0.3);}.zte-bar-down{color:#0059fa;border-color:rgba(0,89,250,0.3);}.zte-bar-up::before{content:'';position:absolute;left:0;top:0;bottom:0;z-index:-1;background:rgba(255,76,0,0.12);width:var(--p-up,0%);transition:width 0.5s;}.zte-bar-down::before{content:'';position:absolute;left:0;top:0;bottom:0;z-index:-1;background:rgba(0,89,250,0.12);width:var(--p-down,0%);transition:width 0.5s;}#config-list.gege-list-container{contain:content!important;background-color:#ffffff!important;border-radius:8px!important;border:1px solid #e0e0e0!important;padding:20px 30px!important;box-shadow:0 2px 10px rgba(0,0,0,0.02)!important;margin-top:10px!important;}.gege-section{margin-bottom:10px;}
    .gege-section:last-child{margin-bottom:0;}.gege-list-container .config-title{font-size:16px!important;font-weight:bold!important;color:#333!important;margin:15px 0 10px 0!important;padding-bottom:5px!important;}.gege-list-container .gege-section:first-child .config-title{margin-top:0!important;}.gege-empty-state{color:#999!important;font-size:14px!important;padding:0 0 15px 5px!important;border-bottom:1px solid #f0f0f0!important;margin-bottom:5px!important;}.gege-list-item{background-color:transparent!important;border-bottom:1px solid #f0f0f0!important;padding:15px 10px!important;margin-bottom:0!important;border-radius:0!important;}
    .gege-list-item:last-child{border-bottom:none!important;}#zte-geek-board{contain:content;background-color:transparent!important;border-left:4px solid #0059fa!important;border-radius:0!important;padding:5px 0 5px 15px!important;margin:10px 0 15px 0!important;box-shadow:none!important;border-bottom:1px solid #f0f0f0!important;font-size:14px;display:flex;flex-direction:column;gap:6px;padding-bottom:15px!important;}#gege-global-overlay #zte-geek-board.geek-frozen-pane{position:sticky!important;top:0px!important;z-index:100!important;background-color:#f3f4f5!important;margin-top:0!important;padding-top:15px!important;box-shadow:0 10px 15px -3px rgba(0,0,0,0.05)!important;border-radius:0 0 8px 8px!important;}.gege-pin{cursor:pointer;font-size:11px;filter:grayscale(100%);opacity:0.5;transition:transform 0.2s;margin-left:2px;}
    .gege-pin.active{filter:none;opacity:1;transform:scale(1.1);}#gege-global-overlay{position:fixed;top:7.5%;right:0;bottom:0;background:#f3f4f5;z-index:9999;overflow-y:auto;padding-bottom:50px;left:0!important;border-radius:16px 16px 0 0;box-shadow:0 -5px 25px rgba(0,0,0,0.15);transition:top 0.3s ease;}@media (max-width: 768px){.geek-right-box:has(#gb-wan-zero-up),.geek-right-box:has(#gb-cur-up-vol){display:none!important}.gege-list-item{padding:12px 10px!important;position:relative!important}.config-item-box{flex-direction:column!important;padding-bottom:0!important}.config-item .info,.config-item .logo,.config-item .speed{width:100%!important;border:none!important;padding:0!important;position:static!important}.config-item .dev-intro{min-height:auto!important;justify-content:center!important;padding-right:90px!important}.config-item .logo{padding-bottom:4px!important}.config-item .info{flex-direction:column!important;margin:0 0 6px 0!important;gap:2px!important}.dev-ip{position:absolute!important;top:12px!important;right:10px!important;font-size:11px!important;background:rgba(0,89,250,0.08);color:#0059fa!important;padding:2px 6px!important;border-radius:4px;font-weight:bold;line-height:1.2;z-index:10;width:auto!important}.dev-number{width:auto!important;margin:0!important;font-size:11px!important}.gege-ratio-box{width:100%!important;margin-top:2px!important;margin-bottom:0!important}.gege-down-box{width:100%!important;margin-top:2px!important}#zte-geek-board{padding:8px!important;gap:0!important;font-size:11.5px!important}.geek-row{height:auto!important;flex-wrap:wrap!important;margin-bottom:4px!important;justify-content:flex-start!important;gap:2px 6px!important;line-height:1.3!important}.geek-label{width:auto!important;min-width:60px!important;font-size:11.5px!important;flex:0 0 auto!important}.geek-val-box{width:auto!important;flex:1 1 0%!important;display:flex!important;flex-wrap:wrap!important;margin-left:0!important;gap:2px 6px!important}.geek-fixed-width{width:auto!important}.geek-right-box{width:100%!important;flex:0 0 100%!important;text-align:left!important;font-size:11.5px!important;margin-top:2px!important;margin-left:0!important}.gege-list-container{padding:8px!important}.zte-enhance-speed{gap:4px!important}}#zte-geek-board{position:relative!important;overflow:visible!important;}#gege-speed-chart{position:absolute;z-index:25;box-sizing:border-box;cursor:move;user-select:none;touch-action:none;container-type:inline-size;}#gege-speed-chart canvas{display:block;width:100%;height:100%;}.gege-chart-head,.gege-chart-foot{position:absolute;left:clamp(28px,8%,36px);right:8px;display:flex;align-items:center;pointer-events:none;font:bold clamp(9px,2.1cqw,11px) system-ui,sans-serif;white-space:nowrap;overflow:hidden;}.gege-chart-head{top:2px;color:#111;gap:8px;}.gege-chart-head [data-gc="range"]{color:#666;font-weight:normal;overflow:hidden;text-overflow:ellipsis;margin-left:auto;}.gege-chart-foot{bottom:4px;justify-content:flex-start;gap:clamp(3px,1.1cqw,12px);}.gege-chart-foot span{min-width:0;overflow:hidden;text-overflow:ellipsis;}.gc-up{color:#ff4c00;flex:0 1 auto;}.gc-down{color:#0b5;flex:0 1 auto;}.gc-extra{color:#666;text-align:right;margin-left:auto;flex:1 1 0;min-width:0;}#gege-speed-chart .gege-chart-resize{position:absolute;right:-4px;bottom:-4px;width:13px;height:13px;border-right:3px solid #0059fa;border-bottom:3px solid #0059fa;cursor:nwse-resize;border-radius:2px;}@media (max-width:768px){#gege-speed-chart{left:210px!important;width:calc(100% - 220px)!important;height:96px!important;}}`;
  document.
  head.
  appendChild(st);
  window.gegeRenderedMacs = new Set();
  let gTD = p => { const v = _w.document.querySelector('#app')?.__vue__; if (!v?.$getData) return; gTD = v.$getData.bind(v); return gTD(p); };
  function fCH() {
    return Promise.resolve(gTD({modules:"wifiAdvCfg"})).then(d => {
      if (!d?.wifiAdvCfg?.wifiChannelCurrent || !d.wifiAdvCfg.wifiChannelCurrent_5g) return;
      S.RSSI频率修正 = 20 * Math.log10((5000 + 5 * +(d.wifiAdvCfg.wifiChannelCurrent_5g||44)) / (2407 + 5 * +(d.wifiAdvCfg.wifiChannelCurrent||10)));
      S.is5G_149 = +d.wifiAdvCfg.wifiChannelCurrent_5g > 148;
      CONFIG.portMap['5G'] = S.is5G_149 ? '5.8G' : '5.2G';
    }).catch(e => console.warn("[哥哥科技] 无线信道彩蛋探测异常:", e));
  }
async function rSD(wantWan = !0, wantLan = !0) {
    if (window.__gIsF || !wantWan && !wantLan) return;
    window.__gIsF = !0;
    try {
      const d = await gTD({modules:wantWan?(wantLan?"wanStatus,deviceList":"wanStatus"):"deviceList",timerRefresh:1});
      if (!d) return;
      const now = performance.now(), wanValid = !!d.wanStatus, lanValid = Array.isArray(d.deviceList);
      let cWU = S.wInstUp, cWD = S.wInstDn, cSU = 0, cSD = 0, cI = Object.create(null);
      if (wanValid) {
        cWU = (+d.wanStatus.wanUpSpeed || 0) * 8192;
        cWD = (+d.wanStatus.wanDownSpeed || 0) * 8192;
        记总速率图(cWU, cWD);
      }
      if (lanValid) for (let n = 0; n < d.deviceList.length; n++) for (let z = 0, a; z < 2; z++) {
        a = z ? d.deviceList[n].guestList : d.deviceList[n].onlineList;
        if (!a) continue;
        for (let j = 0; j < a.length; j++) if (a[j].mac) {
          const m = nM(a[j].mac), u = (+a[j].upSpeed || 0) * 8192, dn = (+a[j].downSpeed || 0) * 8192;
          cI[m] = {
            upRate: u, dnRate: dn, iface: a[j].connectType || "",
            onSec: +a[j].connectTime || 0, name: a[j].hostname || "未知设备", ip: a[j].ip || "",
            rssi: +a[j].rssi || 0, rate: +a[j].deviceRate || 0, vendor: a[j].manufacturer || ""
          };
          cSU += u; cSD += dn;
        }
      }
      if (lanValid) S.lastCI = cI;
      else {
        cI = S.lastCI || Object.create(null);
        for (const d of Object.values(cI)) { cSU += d.upRate || 0; cSD += d.dnRate || 0; }
      }
      let ol = document.getElementById('gege-global-overlay'), cM = Object.keys(cI), iD = lanValid && (window.gegeForceUIRedraw || cM.length !== window.gegeRenderedMacs.size);
      if (!iD && cM.length) for (let i = 0; i < cM.length; i++) if (!window.gegeRenderedMacs.has(cM[i])) { iD = !0; break; }
      if (iD) for (let m in S.cls) if (!cI[m]) {
        let cS = S.cls[m], ms = now - cS.lUT;
        if (ms > CONFIG.lanRefreshInterval * 1000) ms = CONFIG.lanRefreshInterval * 1000;
        cS.intUp += cS.upR * ms * 0.0005;
        cS.intDn += cS.dnR * ms * 0.0005;
        cS.upR = cS.dnR = 0;
      }
      if (ol && ol.style.display === 'block' && (iD || !ol.querySelector('.gege-list-item'))) {
        bVD(ol, cI); window.gegeRenderedMacs = new Set(cM); window.gegeForceUIRedraw = !1;
      }
      const gDt = S.lt ? (now - S.lt) * 0.001 : 0;
      if (wanValid && S.wLT === undefined) S.wLT = now;
      else if (wanValid && (cWU !== S.wInstUp || cWD !== S.wInstDn)) {
        const wDt = now - S.wLT;
        if (S.wInstUp > 0) S.wTotUp += (S.wInstUp + cWU) * wDt * 0.0005;
        else if (cWU > 0) { const wEU = cWU * 0.5 * CONFIG.wanRefreshInterval; S.wTotUp += wEU; S.wZEU = (S.wZEU || 0) + wEU; S.wZEUC = (S.wZEUC || 0) + 1; }
        if (S.wInstDn > 0) S.wTotDn += (S.wInstDn + cWD) * wDt * 0.0005;
        else if (cWD > 0) { const wED = cWD * 0.5 * CONFIG.wanRefreshInterval; S.wTotDn += wED; S.wZED = (S.wZED || 0) + wED; S.wZEDC = (S.wZEDC || 0) + 1; }
        S.wLT = now;
      }
      const 本轮刷新接口 = new Set();
      for (const [m, cC] of Object.entries(cI)) {
        let cS = S.cls[m];
        if (!cS) cS = S.cls[m] = {
          upR: cC.upRate, dnR: cC.dnRate, lUT: now, aR: 0,
          intUp: _saved?.devices?.[m]?.integral_up || 0, intDn: _saved?.devices?.[m]?.integral_down || 0,
          onS: cC.onSec, lOS: cC.onSec, name: _saved?.devices?.[m]?.name || cC.name || m,
          hU: new Float64Array(128), hD: new Float64Array(128), hIdx: 0, ifc: cC.iface
        };
        else {
          if (cS.ifc !== cC.iface) {
            if (CONFIG.盲漫游 !== 0) cS.aR = CONFIG.盲漫游 === 1 ? 2 : 1;
            cS.ifc = cC.iface;
          } else if (cS.aR > 0) cS.aR--;
          if (cS.aR === 2 || cS.aR === 1 && cC.upRate > CONFIG.宽带最大外网上行速率 * 0.6 || cC.upRate > 6e8) { cSU -= cC.upRate; cC.upRate = 1; }
          if (cS.aR === 2 || cS.aR === 1 && cC.dnRate > CONFIG.宽带最大外网下行速率 * 0.6 || cC.dnRate > 24e8) { cSD -= cC.dnRate; cC.dnRate = 1; }
          if (cS.aR === 0 && (cC.upRate !== cS.upR || cC.dnRate !== cS.dnR)) 本轮刷新接口.add(cC.iface);
          if (cS.lOS !== cC.onSec) { cS.onS = cC.onSec; cS.lOS = cC.onSec; }
          else cS.onS = (cS.onS || cC.onSec || 0) + gDt;
        }
        if (cC.name && cC.name !== '未知设备') cS.name = cC.name;
      }
      for (const [m, cC] of Object.entries(cI)) {
        const cS = S.cls[m];
        if (cC.upRate !== cS.upR || cC.dnRate !== cS.dnR || cS.aR === 0 && 本轮刷新接口.has(cC.iface)) {
          const ms = now - cS.lUT;
          if (cS.upR > 0) cS.intUp += (cS.upR + cC.upRate) * ms * 0.0005;
          else if (cC.upRate > 0) { const eU = cC.upRate * CONFIG.lanRefreshInterval * 0.5; cS.intUp += eU; cS.zEU = (cS.zEU || 0) + eU; cS.zUC = (cS.zUC || 0) + 1; }
          if (cS.dnR > 0) cS.intDn += (cS.dnR + cC.dnRate) * ms * 0.0005;
          else if (cC.dnRate > 0) { const eD = cC.dnRate * CONFIG.lanRefreshInterval * 0.5; cS.intDn += eD; cS.zED = (cS.zED || 0) + eD; cS.zDC = (cS.zDC || 0) + 1; }
          cS.upR = cC.upRate; cS.dnR = cC.dnRate; cS.lUT = now;
        }
      }
      S.lt = now;
      if (wanValid) { S.wInstUp = cWU; S.wInstDn = cWD; }
      rUI(cWU, cWD, cSU, cSD, cI, wanValid);
    } catch (e) {
      console.error("[哥哥科技/Tenda] 周期采样中断:", e);
    } finally {
      window.__gIsF = !1;
    }
  }

  async function gegePollLoop() {
    if (!window.gegeBActivated) return;
    const now = performance.now(), wStep = CONFIG.wanRefreshInterval * 1000, lStep = CONFIG.lanRefreshInterval * 1000, w = now + 1 >= S.wDue, l = now + 1 >= S.lDue;
    if (w) do S.wDue += wStep; while (S.wDue <= now);
    if (l) do S.lDue += lStep; while (S.lDue <= now);
    if (w || l) await rSD(w, l);
    const after = performance.now();
    window.gegeMasterTimer = setTimeout(gegePollLoop, Math.max(1, Math.min(S.wDue, S.lDue) - after));
  }
  function gegeStartPoll() {
    clearTimeout(window.gegeMasterTimer);
    const now = performance.now();
    S.wDue = now + CONFIG.wanRefreshInterval * 1000;
    S.lDue = now + CONFIG.lanRefreshInterval * 1000;
    window.gegeMasterTimer = setTimeout(gegePollLoop, Math.max(1, Math.min(S.wDue, S.lDue) - now));
  }

  function buildCSV() {
    return ((sp, now, start) => '\uFEFF' + [
      `"哥哥科技 硬路由 NPU 增强系列：专用组件 ${版本号} 生成"`,
      `"统计周期：${new Date(start + CONFIG.时区补偿).toISOString().replace('T', ' ').slice(0, 19)} 至 ${new Date(now + CONFIG.时区补偿).toISOString().replace('T', ' ').slice(0, 19)} (UTC${CONFIG.时区补偿 > 0 ? '+' : ''}${CONFIG.时区补偿 / 3600000})${CONFIG.readSaveData === 1 ? ' （含本地历史读档）' : ''}"`,
      `"--- [全局统计] ---"`,
      `"WAN总上传(bit)","WAN总下载(b)","高精全局上行(b)","高精全局下行(b)","LAN积分总上行(b)","LAN积分总下行(b)","本次在线总上行(b)","本次在线总下行(b)"`,
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
      S.cSnap || {}, 
      S.cSnap?.timestamp || Date.now(), 
      (CONFIG.readSaveData !== 0 && typeof GM_getValue !== 'undefined' ? GM_getValue('gege_reset_ms', null) : null) || performance.timeOrigin || Date.now()
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
    GM_setValue('ha_snapshot', { timestamp: nowMs, global: {}, devices: {} }); _saved = null; S.cSnap = null;
    S.wTotUp = S.wTotDn = 0; // 内存原地清零
    for (let k in S.cls) { let s = S.cls[k]; s.intUp = s.intDn = 0; s.hU.fill(0); s.hD.fill(0); } // 内存原地清零底表
    document.getElementById('gb-w-bnr')?.remove(); // 预警横幅
    S.calcTime(Math.max(nowMs, S.Force_MS - CONFIG.自动导出 * 60000 + 1000) + CONFIG.时区补偿); // 瞬间算出下月/下周新线
    window.gegeForceUIRedraw = !0; // 重绘 UI
    setTimeout(() => { S._RST = !1; }, 2000); // 解开安全锁
  }

const SPRK = [' ', '▂', '▃', '▄', '▅', '▆', '▇', '█'];
      function getSpark(ringArr, headIdx, maxVal) {
        let s = "";
        for (let i = 64; i--; ) {
          let v = ringArr[(headIdx - i) & 127];
          s += SPRK[v > 0 ? Math.min(7, Math.max(1, ((v / maxVal) * 7) | 0)) : 0];
        }
        return s;
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
        (box._gcMeta ??= box.querySelector('[data-gc="meta"]')).textContent = `采样:${window.gegeBActivated ? CONFIG.wanRefreshInterval : 3}s  点:${S.总图点数}`;
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

      const gPSvg = rate => {
        const c = rate === 10 ? '#E7B05C' : rate === 100 ? '#5394CC' : rate === 1000 ? '#4CAF50' : '';
        if (c) return `<svg viewBox="0 0 100 100" width="45" height="45" xmlns="http://www.w3.org/2000/svg"><g transform="rotate(180 50 50)"><path d="M18 82V36Q18 32 22 32H28V24Q28 20 32 20H38V14Q38 10 42 10H58Q62 10 62 14V20H68Q72 20 72 24V32H78Q82 32 82 36V82Q82 86 78 86H22Q18 86 18 82Z" fill="${c}" fill-opacity=".10" stroke="${c}" stroke-width="4" stroke-linejoin="round"/><g stroke="${c}" stroke-width="3.2" stroke-linecap="round"><path d="M31 54V76"/><path d="M36.5 54V76"/><path d="M42 54V76"/><path d="M47.5 54V76"/><path d="M53 54V76"/><path d="M58.5 54V76"/><path d="M64 54V76"/><path d="M69.5 54V76"/></g></g></svg>`;
        return `<svg viewBox="0 0 100 100" width="45" height="45"><rect x="15" y="15" width="70" height="65" rx="5" fill="#cfd8dc" stroke="#90a4ae" stroke-width="4"/><path d="M 25,25 L 75,25 L 75,60 L 60,60 L 60,75 L 40,75 L 40,60 L 25,60 Z" fill="#263238"/><g fill="#ffca28"><rect x="30" y="25" width="2.5" height="18"/><rect x="35" y="25" width="2.5" height="18"/><rect x="40" y="25" width="2.5" height="18"/><rect x="45" y="25" width="2.5" height="18"/><rect x="52.5" y="25" width="2.5" height="18"/><rect x="57.5" y="25" width="2.5" height="18"/><rect x="62.5" y="25" width="2.5" height="18"/><rect x="67.5" y="25" width="2.5" height="18"/></g><circle cx="21" cy="73" r="3.5" fill="#4caf50"/><circle cx="79" cy="73" r="3.5" fill="#ffb300"/></svg>`;
      };
      const gWSvg = r => {
        const c = r > -24 ? '#4caf50' : r > -35 ? '#9c27b0' : '#0059fa';
        if (r > -41) return `<svg viewBox="0 0 100 100" width="45" height="45"><path d="M 50,85 L 10,35 A 65,65 0 0,1 90,35 Z" fill="${c}"/></svg>`;
        if (r > -46) return `<svg viewBox="0 0 100 100" width="45" height="45"><path d="M 50,85 L 10,35 A 65,65 0 0,1 90,35 Z" fill="#e0e0e0"/><path d="M 50,85 L 18,45 A 50,50 0 0,1 82,45 Z" fill="${c}"/></svg>`;
        if (r > -50) return `<svg viewBox="0 0 100 100" width="45" height="45"><path d="M 50,85 L 10,35 A 65,65 0 0,1 90,35 Z" fill="#e0e0e0"/><path d="M 50,85 L 23,51 A 43,43 0 0,1 77,51 Z" fill="${c}"/></svg>`;
        if (r > -56) return `<svg viewBox="0 0 100 100" width="45" height="45"><circle cx="50" cy="80" r="9" fill="${c}"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="${c}" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="${c}" stroke-width="7" stroke-linecap="round"/></svg>`;
        if (r > -61) return `<svg viewBox="0 0 100 100" width="45" height="45"><circle cx="50" cy="80" r="9" fill="${c}"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="${c}" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></svg>`;
        if (r > -68) return `<svg viewBox="0 0 100 100" width="45" height="45"><circle cx="50" cy="80" r="9" fill="${c}"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></svg>`;
        if (r > -71) return `<svg viewBox="0 0 100 100" width="45" height="45"><circle cx="50" cy="80" r="7" fill="none" stroke="#ff9800" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></svg>`;
        if (r > -75) return `<svg viewBox="0 0 100 100" width="45" height="45"><g transform="translate(-15, 0)"><circle cx="50" cy="80" r="7" fill="none" stroke="#ffb300" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></g><text x="70" y="86" fill="#ffb300" font-weight="900" font-size="48" font-family="sans-serif">!</text></svg>`;
        if (r > -78) return `<svg viewBox="0 0 100 100" width="45" height="45"><g transform="translate(-15, 0)"><circle cx="50" cy="80" r="7" fill="none" stroke="#ff4c00" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></g><text x="70" y="86" fill="#ff4c00" font-weight="900" font-size="48" font-family="sans-serif">!</text></svg>`;
        if (r > -85) return `<svg viewBox="0 0 100 100" width="45" height="45"><g transform="translate(-15, 0)"><circle cx="50" cy="80" r="7" fill="none" stroke="#ff4c00" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></g><text x="65" y="86" fill="#ff4c00" font-weight="900" font-size="44" font-family="sans-serif">?</text></svg>`;
        return `<svg viewBox="0 0 100 100" width="45" height="45"><g transform="translate(-15, 0)"><circle cx="50" cy="80" r="7" fill="none" stroke="#ff4c00" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#ff4c00" stroke-width="7" stroke-linecap="round" opacity="0.3"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#ff4c00" stroke-width="7" stroke-linecap="round" opacity="0.3"/></g><text x="65" y="80" fill="#ff4c00" font-weight="900" font-size="35" font-family="sans-serif">✖</text></svg>`;
      };

  function rUI(wU, wD, sU, sD, cI, wanTick) {
    let LUp = 0, LDn = 0;
    for (let k in S.cls) {
      LUp += S.cls[k].intUp || 0;
      LDn += S.cls[k].intDn || 0;
    }
    S.cSnap = {
      timestamp: Date.now(),
      global: {
        wan_up: S.wTotUp,
        wan_down: S.wTotDn,
        lan_integral_up: LUp,
        lan_integral_down: LDn,
        lan_high_up: LUp,
        lan_high_down: LDn
      },
      devices: Object.keys(S.cls).reduce((acc, k) => {
        let s = S.cls[k], cC = cI[k];
        acc[k] = {
          up: s.intUp || 0,
          down: s.intDn || 0,
          integral_up: s.intUp || 0,
          integral_down: s.intDn || 0,
          status: cC ? (CONFIG.portMap[cC.iface] || cC.iface || "未知接口") : "off",
          name: cC?.name || s.name || k,
          ip: cC?.ip || "",
          raw_up: 0,
          raw_down: 0
        };
        return acc;
      }, {})
    };
    if (wanTick) {
      S.rTick = ((S.rTick || 0) + 1) & 3;
      if (S.rTick === 1 || !S.cRT) {
        for (let k in S.cls) {
          let s = S.cls[k], cC = cI[k];
          s.hIdx = (s.hIdx + 1) & 127;
          s.hU[s.hIdx] = cC ? cC.upRate : 0;
          s.hD[s.hIdx] = cC ? cC.dnRate : 0;
        }
        if (typeof GM_setValue !== 'undefined') {
          let nowMs = Date.now();
          if (!S.haDue || nowMs >= S.haDue) {
            S.haDue = nowMs + 768000;
            try { GM_setValue('ha_snapshot', S.cSnap); } catch (e) { console.warn(e); }
            Promise.resolve(gTD({modules:"ethPortStatus",timerRefresh:1})).then(d => { if (d?.ethPortStatus) S.pI = d.ethPortStatus; }).catch(e => console.warn("[哥哥科技/Tenda] 物理网口探测异常:", e));
          }
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
        }
        S.aWu = (S.wTotUp - (S.lwTU || S.wTotUp)) / (CONFIG.wanRefreshInterval * 4); S.lwTU = S.wTotUp;
        S.aWd = (S.wTotDn - (S.lwTD || S.wTotDn)) / (CONFIG.wanRefreshInterval * 4); S.lwTD = S.wTotDn;
        const rUp = S.wTotUp ? LUp / S.wTotUp : 1, rDn = S.wTotDn ? LDn / S.wTotDn : 1;
        S.cRT = `<span style="font-weight: bold;"><span style="color: ${rUp > 1.5 ? '#ff4c00' : (rUp > 1.15 ? '#FF9800' : '#4CAF50')};">${(rUp * 100).toFixed(2)}%</span>，<span style="color: ${rDn > 1.5 ? '#ff4c00' : (rDn > 1.15 ? '#FF9800' : '#4CAF50')};">${(rDn * 100).toFixed(2)}%</span></span>`;
        if (document.getElementById('gb-ratio-display')) document.getElementById('gb-ratio-display').innerHTML = S.cRT;
      }
    }
    let bd = document.getElementById('zte-geek-board');
    if (!bd) {
      bd = document.createElement('div');
      bd.id = 'zte-geek-board';
 let layoutHtml = '';
if (CONFIG.uiLayout === 1) { // 紧凑版 (驾驶舱)
    layoutHtml = `
        <div class="geek-row"><span class="geek-label">WAN口速率</span><div class="geek-val-box" style="position:relative;"><span class="c-up geek-fixed-width" id="gb-wan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-wan-down-bytes"></span><span style="margin-left: 5px;"><span class="c-up" id="gb-wan-up-bps"></span> | <span class="c-down" id="gb-wan-down-bps"></span></span></div><div class="geek-right-box" style="font-weight: normal; color: #666;"><span style="color: #333;">0估算：</span><span id="gb-wan-zero-up"></span>，<span id="gb-wan-zero-down"></span>｜<span id="gb-wan-zero-up-cnt"></span>，<span id="gb-wan-zero-down-cnt"></span></div></div>
        <div class="geek-row"><span class="geek-label">局域网代数和</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-lan-down-bytes"></span></div><div class="geek-right-box">实时占比：<span class="c-up" id="gb-perc-up"></span> | <span class="c-down" id="gb-perc-down"></span></div></div>
        <div class="geek-row"><span class="geek-label">LAN：<span id="gege-pin-btn" class="gege-pin" title="冻结窗格">📌</span></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-lan-down-vol"></span><span style="font-weight: bold; margin-left: 5px;">WAN总计：<span class="c-up" id="gb-wan-up-vol"></span> | <span class="c-down" id="gb-wan-down-vol"></span></span></div><div class="geek-right-box">内外网比：<span id="gb-ratio-display"></span></div></div>`;
} else if (CONFIG.uiLayout === 2) { // 平铺版 (报表流)
    layoutHtml = `
        <div class="geek-row"><span class="geek-label">WAN口速率</span><div class="geek-val-box" style="position:relative;"><span class="c-up geek-fixed-width" id="gb-wan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-wan-down-bytes"></span></div><div class="geek-right-box"><span class="c-up" id="gb-wan-up-bps"></span> | <span class="c-down" id="gb-wan-down-bps"></span></div></div>
        <div class="geek-row"><span class="geek-label">局域网代数和</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-lan-down-bytes"></span></div><div class="geek-right-box">实时占比：<span class="c-up" id="gb-perc-up"></span> | <span class="c-down" id="gb-perc-down"></span></div></div>
        <div class="geek-row"><span class="geek-label">LAN：<span id="gege-pin-btn" class="gege-pin" title="冻结窗格">📌</span></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-lan-down-vol"></span></div><div class="geek-right-box"></div></div>
        <div class="geek-row"><span class="geek-label">WAN总计：</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-wan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-wan-down-vol"></span></div><div class="geek-right-box"><span style="font-weight: normal;">内外网比：</span><span id="gb-ratio-display"></span></div></div>`;
} else { // 经典版 (0)
    layoutHtml = `
        <div class="geek-row"><span class="geek-label">WAN口速率</span><div class="geek-val-box" style="position:relative;"><span class="c-up geek-fixed-width" id="gb-wan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-wan-down-bytes"></span></div><div class="geek-right-box"><span class="c-up" id="gb-wan-up-bps"></span> | <span class="c-down" id="gb-wan-down-bps"></span></div></div>
        <div class="geek-row"><span class="geek-label">局域网代数和</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-lan-down-bytes"></span></div><div class="geek-right-box">实时占比：<span class="c-up" id="gb-perc-up"></span> | <span class="c-down" id="gb-perc-down"></span></div></div>
        <div class="geek-row"><span class="geek-label">LAN：<span id="gege-pin-btn" class="gege-pin" title="冻结窗格">📌</span></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-lan-down-vol"></span></div><div class="geek-right-box">WAN：<span class="c-up" id="gb-wan-up-vol"></span> | <span class="c-down" id="gb-wan-down-vol"></span></div></div>`;
}
        layoutHtml += `<div class="geek-row" id="gb-phys-row" style="display:none;height:auto!important;flex-wrap:wrap;"><span class="geek-label" style="font-weight:normal;color:#666;">物理网口</span><div class="geek-val-box" id="gb-phys-data" style="flex-wrap:wrap;font-size:13px;font-weight:normal;"></div></div>`;
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
  
    let oDC = Object.create(null);
    if (!iPO) {
      const M_RX = /([a-fA-F0-9]{2}[:-]){5}[a-fA-F0-9]{2}/;
      let aI = aC.querySelectorAll('.config-item');
      for (let n of aI) {
        let mN = n.querySelector('.dev-number'),
          mM = mN ? mN.textContent.match(M_RX) : null;
        if (mM) {
          oDC[mM[0].toLowerCase().replace(/-/g, ':')] = n;
        }
      }
    }
    else {
      let gI = aC.querySelectorAll('.gege-list-item');
      for (let n of gI) {
        let m = n.getAttribute('data-gege-mac');
        if (m) oDC[m] = n;
      }
    }
            if (bd.parentNode) {
        const setText = (id, text) => { const el = bd.querySelector(id); if (el) el.textContent = text; };
        setText('#gb-wan-up-bytes', `🔼 ${fB(wU)}`);
        setText('#gb-wan-down-bytes', `🔽 ${fB(wD)}`);
        setText('#gb-wan-up-bps', `🔼 ${fBy(wU)}`);
        setText('#gb-wan-down-bps', `🔽 ${fBy(wD)}`);
        setText('#gb-lan-up-bytes', `🔼 ${fB(sU)}`);
        setText('#gb-lan-down-bytes', `🔽 ${fB(sD)}`);
        setText('#gb-perc-up', `🔼 ${wU>0?(sU*100/wU).toFixed(1):0.0}%`);
        setText('#gb-perc-down', `🔽 ${wD>0?(sD*100/wD).toFixed(1):0.0}%`);
        setText('#gb-lan-up-vol', `🔼 ${fV(LUp)}`);
        setText('#gb-lan-down-vol', `🔽 ${fV(LDn)}`);
        setText('#gb-wan-up-vol', `🔼 ${fV(S.wTotUp)}`);
        setText('#gb-wan-down-vol', `🔽 ${fV(S.wTotDn)}`);
        if (S.pI?.ports?.length) {
          bd.querySelector('#gb-phys-row').style.display = 'flex';
          let h = '';
          for (let i = 0; i < S.pI.ports.length; i++) h += `<span style="margin-right:14px;color:${S.pI.ports[i].connected?'#333':'#999'};">${S.pI.ports[i].name==='wan'?'WAN':S.pI.ports[i].name==='lan'?'LAN '+i:S.pI.ports[i].name.toUpperCase()+' '+(i+1)}：${S.pI.ports[i].connected?(S.pI.ports[i].portSpeedType===2?'2.5GE':'GE'):'未连接'}</span>`;
          bd.querySelector('#gb-phys-data').innerHTML = h;
        }
        画总速率图(bd);
        if (bd.querySelector('#gb-ratio-display')) {
          if (bd.querySelector('#gb-wan-zero-up')) {
              setText('#gb-wan-zero-up', !S.wZEU ? '' : fSV(S.wZEU));
              setText('#gb-wan-zero-down', !S.wZED ? '' : fSV(S.wZED));
              setText('#gb-wan-zero-up-cnt', S.wZEUC || 0);
              setText('#gb-wan-zero-down-cnt', S.wZEDC || 0);
          }
        }
      }
      const inv_LUp = LUp > 0 ? 100 / LUp : 0;
      const inv_LDn = LDn > 0 ? 100 / LDn : 0;
      const inv_sU = sU > 0 ? 100 / sU : 0;
      const inv_sD = sD > 0 ? 100 / sD : 0;

      for (let m in cI) {
        let it = oDC[m];
        if (!it) continue;
        const cC = cI[m] || { upRate: 0, dnRate: 0, iface: "" },
              cS = S.cls[m] || { intUp: 0, intDn: 0, onS: 0 };
        
        let cache = it._gege || (it._gege = {}), rRs = cC.rssi, lRs = cS.lRs ?? rRs;
        if (cC.iface !== 'wire' && rRs) {
          if (((rRs > -24) !== (lRs > -24) || (rRs > -35) !== (lRs > -35) || (rRs > -41) !== (lRs > -41) || (rRs > -56) !== (lRs > -56)) && Math.abs(rRs - lRs) + (cS.dbC || 0) < 5) {
            rRs = lRs; cS.dbC = ((cS.dbC || 0) + 1) & 7;
          } else { cS.dbC = 0; cS.lRs = rRs; }
        }
        (cache.logo ??= it.querySelector('.dev-logo')).innerHTML = (cC.iface === 'wire' ? gPSvg(cC.rate) : rRs ? gWSvg(rRs) : '') + (cC.rate ? `<div style="font-size:10.5px;color:${cC.rate===2500?'#000':cC.rate===100?'#4caf50':cC.rate===10?'#ff4c00':'#999'};font-family:Consolas;margin-top:2px;font-weight:${cC.rate===2500||cC.rate===100?'bold':'normal'};">rate:${cC.rate}</div>` : '');
        const rN = cache.rssiNode ??= (cache.devIntro ??= it.querySelector('.dev-intro'))?.querySelector('.gege-rssi');
        if (rN) { const p = Math.round((cC.rssi - (cC.iface === '2.4G' ? S.RSSI频率修正 || 0 : 0)) * 1.6666666666666667 + 133.33333333333334); rN.innerHTML = cC.iface === 'wire' ? escapeHTML(cC.vendor || '') : cC.rssi ? `<span style="color:${p < 0?'#ff4c00':'inherit'}">${p}%</span>, ${cC.rssi}` : ''; }
        
        let hqU = cS.intUp || 0; 
        let hqD = cS.intDn || 0;
        let tN = cache.timeNode ??= it.querySelector('.gege-online-time');
        if (tN && cS.onS > 0) tN.textContent = `在线：${fOT(cS.onS)}`;
        
        const dI = cache.devIntro ??= it.querySelector('.dev-intro');
        const inf = cache.info ??= it.querySelector('.info');

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
          (cache.rBoxPort ??= rB.querySelector('.v-port')).textContent = CONFIG.portMap[cC.iface] || cC.iface || "未知";
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
          let p = hqU * inv_LUp;
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
          let dp = cS.intDn * inv_LDn;
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
          let pu = cC.upRate * inv_sU,
              pd = cC.dnRate * inv_sD,
              bU = cache.bU ??= enh.querySelector('.zte-bar-up'),
              bD = cache.bD ??= enh.querySelector('.zte-bar-down');
          
          let clU = (S.aWu * 0.1) || 0; if (clU < 512000) clU = 512000;
          let clD = (S.aWd * 0.125) || 0;
          for (let i = 0; i < 128; i++) { if (cS.hU[i] > clU) clU = cS.hU[i]; if (cS.hD[i] > clD) clD = cS.hD[i]; }
          (cache.bUSpk ??= bU.querySelector('.v-spark')).textContent = getSpark(cS.hU, cS.hIdx, clU);
          (cache.bDSpk ??= bD.querySelector('.v-spark')).textContent = getSpark(cS.hD, cS.hIdx, clD);

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
      let h2 = [], h5 = [], h6 = [], hW = [];
      for (let m in cI) {
        let d = cI[m], tS = fOT(d.onSec), ifc = d.iface;
        let htm = `<div class="col-md-12 col-xs-12 config-item gege-list-item" data-gege-mac="${m}"><div class="config-item-box" style="display:flex;align-items:stretch;"><div class="col-md-5 col-xs-7 logo" style="width:33%;display:flex;flex-direction:row;align-items:center;"><div class="dev-logo" style="width:50px;height:50px;min-width:50px;margin-right:15px;display:flex;flex-direction:column;align-items:center;justify-content:center;"></div><div class="dev-intro" style="flex:1;display:flex;flex-direction:column;justify-content:flex-start;min-height:50px;"><div style="display:flex;justify-content:space-between;align-items:baseline;width:95%;"><div class="dev-name" style="font-weight:bold;color:#333;font-size:14px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;flex:1;min-width:0;margin-right:8px;">${escapeHTML(d.name)}</div><div class="gege-rssi" style="font-size:12px;font-weight:bold;color:#666;font-family:Consolas;white-space:nowrap;flex-shrink:0;"></div></div><div class="gege-online-time" style="color:#999;font-size:12px;font-family:system-ui,sans-serif;margin-top:4px;">${tS?'在线：'+tS:''}</div></div></div><div class="col-md-4 col-xs-5 info" style="width:27%;display:flex;flex-direction:column;padding:0 10px;border-right:1px solid #eee;"><div class="dev-ip" style="color:#666;font-family:system-ui,sans-serif;">${escapeHTML(d.ip)}</div><div class="dev-number grey" style="color:#999;font-size:12px;font-family:system-ui,sans-serif;">MAC：${m}</div></div><div class="col-md-3 col-xs-12 speed" style="width:40%;display:flex;flex-direction:column;justify-content:center;padding:0 10px;"></div></div></div>`;
        if (ifc === '2.4G') h2.push(htm);
        else if (ifc === '5G') h5.push(htm);
        else if (ifc === '6G') h6.push(htm);
        else hW.push(htm);
      }
      requestAnimationFrame(() => {
        ol.innerHTML = `<div style="padding:20px;width:96%;margin:0 auto;min-height:100%;"><div id="gege-board-anchor"></div><div id="config-list" class="config-list gege-list-container"><div class="gege-section"><div class="config-title">有线设备</div>${hW.join('')||'<div class="gege-empty-state">没有连接设备</div>'}</div><div class="gege-section"><div class="config-title">无线设备（${S.is5G_149?'5.8GHz':'5.2GHz'}）</div>${h5.join('')||'<div class="gege-empty-state">没有连接设备</div>'}</div>${h6.length?`<div class="gege-section"><div class="config-title">无线设备（6GHz）</div>${h6.join('')}</div>`:''}<div class="gege-section"><div class="config-title">无线设备（2.4GHz）</div>${h2.join('')||'<div class="gege-empty-state">没有连接设备</div>'}
        </div><div style="margin-top: 25px; padding-top: 15px; border-top: 1px dashed #eee; text-align: center; font-family: system-ui, sans-serif, 'Microsoft YaHei', sans-serif;"><div style="font-size: 11.5px; color: #777; font-style: italic; margin-bottom: 8px;">“在一个文明社会，干净的、不被监视与吸血的网络，是我们每个人的基本权利。”</div><div style="font-size: 10.5px; color: #999; line-height: 1.3; margin-bottom: 8px;">本交互式程序基于 GNU Affero GPL v3.0 协议开源，按“原样(AS IS)”提供，不对其适用性、稳定性、精密度或任何商业场景合规性作任何明示或暗示的担保。<a href="https://github.com/ucxn/Bro-Stat/blob/main/法律声明：品牌使用政策.md" target="_blank" style="color: #777; text-decoration: underline;">许可证</a><br>根据 AGPL-3.0 第 5(d) 及 7(b) 条规定，基于本程序的任何修改均不得移除或篡改本界面的署名与法律声明。保留此界面是使用本软件代码的合法性的前置条件。
        </div><div style="font-size: 12px; color: #555;"><a href="https://github.com/ucxn/Bro-Stat" target="_blank" style="color: #0059fa; text-decoration: none; font-weight: bold;">Bro-Stat 增强组件 Tenda-${版本号}</a> Copyright &copy; 2026 <a href="https://www.bilibili.com/video/BV1PtR7B8ECC" target="_blank" style="color: #0059fa; text-decoration: none; font-weight: bold;">哥哥科技</a> (BroTech)<span style="color: #888; font-weight: normal;"> | All Rights Reserved</span>&emsp;&nbsp;<a href="https://scriptcat.org/zh-CN/users/203510" target="_blank" style="color: #666; text-decoration: none;">点此分享</a></div></div></div></div>`;
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

    if (location.href.toLowerCase().includes('login')) {
      GM_setValue('gege_open_after_jump', 0);
      return;
    }
    if (location.pathname.includes('/phone/')) {
      GM_setValue('gege_open_after_jump', Date.now());
      const p = _w.document.querySelector('#app')?.__vue_app__?._context?.provides, g = p?.$getData, m = p?.$postModule;
      if (!g || !m) {
        GM_setValue('gege_open_after_jump', 0);
        console.warn('[哥哥科技/Tenda] 未找到手机版官方切换接口');
        return;
      }
      Promise.resolve(g({modules:'localhost'})).then(d => {
        if (!d?.localhost?.ip) throw new Error('localhost.ip unavailable');
        return m({VisitWebVersion:{visitWebEn:!0,visitIp:d.localhost.ip}}, !1);
      }).then(() => { _w.location.href = '//' + _w.location.host; }).catch(e => {
        GM_setValue('gege_open_after_jump', 0);
        console.warn('[哥哥科技/Tenda] 手机版切换电脑版失败:', e);
      });
      return;
    }
    if (location.pathname !== '/index.html') return;
    if (location.hash !== '#/advance/advance/dmz') location.hash = '#/advance/advance/dmz';
    
    if (!o) {
      o = document.createElement('div');
      o.id = 'gege-global-overlay';
      document.body.appendChild(o);
    }
    o.style.display = 'block';
if (!window.gegeBActivated) {
      window.gegeBActivated = !0;
      fCH().then(() => { window.gegeForceUIRedraw = !0; return rSD(!0, !0); }).finally(gegeStartPoll);
    } else fCH().then(() => { window.gegeForceUIRedraw = !0; rSD(!0, !0); });
  };

  window.gegeBActivated = !1;
  window.gegeMasterTimer = null;
  const _initUI = () => {
    if (CONFIG.injectMode === 3 || (CONFIG.injectMode === 1 && +(window.location.hostname.slice(window.location.hostname.lastIndexOf('.') + 1)) < 6)) {
      if (window.createGegeFloatingBtn) window.createGegeFloatingBtn();
    }
(j => {
  if (location.pathname !== '/index.html' || !j) return;
  function f() {
    clearTimeout(f.t);
    f.t = setTimeout(() => {
      if (Date.now() - j >= 120000) {
        GM_setValue('gege_open_after_jump', 0);
        window.removeEventListener('hashchange', f);
        return;
      }
      Promise.resolve(gTD({modules:'wanStatus',timerRefresh:1})).then(d => {
        if (!d?.wanStatus) return f();
        window.removeEventListener('hashchange', f);
        GM_setValue('gege_open_after_jump', 0);
        window.gegeTogglePanel(true);
      }, f);
    }, 200);
  }
  window.addEventListener('hashchange', f);
  f();
})(GM_getValue('gege_open_after_jump', 0));
  };

  if (document.readyState === 'complete') _initUI(); else window.addEventListener('load', _initUI);
  if (sessionStorage.getItem('stok_id') && !location.href.toLowerCase().includes('login')) window.gegeTendaKeepAlive = setInterval(() => { const i = document.createElement('iframe'); i.style.display = 'none'; i.src = `${location.origin}/index.html?${Math.random()}#/advance/advance/dmz`; document.documentElement.appendChild(i); setTimeout(() => i.remove(), 5000); }, 288000);

})();