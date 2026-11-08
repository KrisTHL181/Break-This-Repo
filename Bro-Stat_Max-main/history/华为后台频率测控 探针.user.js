// ==UserScript==
// @name            华为路由器增强 Bro-Stat_HUAWEI
// @name:en         Bro-Stat_HUAWEI
// @namespace       ucxn
// @version         5.9.5
// @description     哥哥科技 QQ群 680464365
// @description:en  https://github.com/ucxn/ZTE-Stat_Max
// @author          哥哥科技 space.bilibili.com/501430041
// @noframes
// @tag             路由器 华为 网络 监控 统计 数据 可视化 极客 增强 UI HA 智能 定时 后台
// @icon            https://scriptcat.org/api/v2/resource/image/PD6xhxddlUESIwAV
// @include         /^https?:\/\/10(\.[0-9]{1,3}){3}(:\d+)?\/.*$/
// @include         http://192.168.*.*
// @include         http://172.16.*
// @include         https://192.168.*.*
// @include         https://172.16.*
// @exclude         *://*/cgi-bin/luci*
// @grant           GM_setValue
// @grant           GM_getValue
// @storageName     GBNPA_Storage
// @license         AGPL-3.0-or-later
// @run-at          document-start
// @updateURL       https://github.com/ucxn/ZTE-Stat_Max/raw/refs/heads/main/new.user.js
// @downloadURL     https://github.com/ucxn/ZTE-Stat_Max/raw/refs/heads/main/new.user.js
// ==/UserScript==

(function () {
  'use strict';

  console.log("🚀 哥哥科技 V5.9.9 引擎已装载...");

  const ESC_MAP = { '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' };
  function escapeHTML(str) {
    return str ? String(str).replace(/[&<>'"]/g, m => ESC_MAP[m]) : '';
  }

  // ======== [0] 用户极客环境变量配置区 ========
  const CONFIG = {
    readSaveData: 1, // 【历史记录】 1: 从路由器后台读档 | 0: 新局模式 | 2: 从本地长期历史读档
    uiLayout: 1, // 【面板拓扑结构】 0: 经典版 | 1: 详细紧凑版(驾驶舱美学) | 2: 详细平铺版(报表流美学)
    injectMode: 1, // 【UI注入模式】 0: 原生侧边栏(1min)| 1: 优先，10秒悬浮舱(D)| 2: 联动模式| 3：强制模式
    calcMode: 1, // 1: 上行/下行倍数模式, 0: 上行占总和比例模式
    lanPortMode: 1, // 【物理网口】 0: 关闭 | 1: 底部追加显示 | 2: WAN高速接管主线
    portInterval: 1, // 物理网口刷新频率(秒)
    ratioExtremeUp: 10, // 极端上传判定阈值 (> 1000%)
    ratioWarnUp: 0.07, // 重度上传警告阈值 (> 7%)
    ratioExtremeDown: 0.01, // 极端下载判定阈值 (< 1%)
    ratioThreshold: 7, // (仅calcMode=0时有效) 上传占比报警阈值(%)
    lanRefreshInterval: 0.5, // LAN口刷新时间(秒)，用于精准补偿0到唤醒时的瞬时流量
    wanRefreshInterval: 0.5, // 【新增】WAN口刷新时间(秒)，用于精准补偿0到唤醒时的瞬时流量
    宽带最大外网上行速率: 3e8,
    宽带最大外网下行速率: 24e8, // 配置外网最大上传|下载比特(bit/bps)速率，请略微大于真实值；500兆为5e8，一千兆1e9
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
  const S = {
    wInstUp: 0,
    wInstDn: 0,
    wTotUp: 0,
    wTotDn: 0,
    cls: {}, isPinned: !0,
    w2U: 0, w2D: 0, w2TotUp: 0, w2TotDn: 0, w2LT: undefined,
    hasW2: !1, is5G_149: null, fI: 0,
    _domRebuilt: !1, _lastPanelState: null, oDC: null
  };
async function gWT() {
    try {
      let r = await fetch('/api/ntwk/wan?type=active&_=' + Date.now());
      if (r.ok) return await r.text();
    } catch(e) {console.warn(e)}
    return "";
  }

  const Phys = { p: Object.create(null), wU: undefined, wD: undefined, tU: 0, tD: 0, lT: undefined, _pM: null, _wID: null };
  let isF = !1, lCxt = null;

  function fB(bps) {
        if (bps > 1e9) return `${Math.round(bps * 1e-6)} Mbit/s`;
        if (bps > 1e6) return `${(bps * 1e-6).toFixed(2)} Mbps`;
        if (bps > 1e3) return `${Math.round(bps * 1e-3)} Kbps`;
        return `${Math.round(bps)} bps`;
    }
const F_ARR = ['0', '[1/8]', '[2/8]', '[3/8]', '[4/8]', '[5/8]', '[6/8]', '[7/8]', '1'];
  function fBy(bps) {
        if (bps === 0) return '0  B';
        if (bps > 8388608) return `${(bps * 1.1920928955078125e-7).toFixed(2)} MiB/s`;
        return bps < 8700
            ? ((bps * 0.001 | 0) === bps * 0.001
                 ? `${F_ARR[bps * 0.001]} KB/s`
                : `${(bps * 0.000125).toFixed(2)} KB/s`)
            : `${(bps * 0.0001220703125).toFixed(1)} KB/s`;
    }

  function fV(bits) {
        if (bits > 83886080000) return `${(bits / 8589934592).toFixed(4)} GiB`;
		if (bits > 8388608000) return `${(bits / 8388608).toFixed(1)} MiB`;
        if (bits > 8388608) return `${(bits / 8388608).toFixed(4)} MiB`;
        if (bits > 8192) return `${(bits / 8192).toFixed(2)} KiB`;
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
    if (bits > 8388608) return `${(bits / 8388608).toPrecision(4)}M`;
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
    .gege-list-item:last-child{border-bottom:none!important;}#zte-geek-board{contain:content;background-color:transparent!important;border-left:4px solid #0059fa!important;border-radius:0!important;padding:5px 0 5px 15px!important;margin:10px 0 15px 0!important;box-shadow:none!important;border-bottom:1px solid #f0f0f0!important;font-size:14px;display:flex;flex-direction:column;gap:6px;padding-bottom:15px!important;}#gege-global-overlay #zte-geek-board.geek-frozen-pane{position:sticky!important;top:0px!important;z-index:100!important;background-color:#f3f4f5!important;margin-top:0!important;padding-top:15px!important;box-shadow:0 10px 15px -3px rgba(0,0,0,0.05)!important;border-radius:0 0 8px 8px!important;}.gege-pin{cursor:pointer;font-size:11px;filter:grayscale(100%);opacity:0.5;transition:transform 0.2s;margin-left:2px;}
    .gege-pin.active{filter:none;opacity:1;transform:scale(1.1);}#gege-global-overlay{position:fixed;top:7.5%;right:0;bottom:0;background:#f3f4f5;z-index:9999;overflow-y:auto;padding-bottom:50px;left:0!important;border-radius:16px 16px 0 0;box-shadow:0 -5px 25px rgba(0,0,0,0.15);transition:top 0.3s ease;}@media (max-width: 768px){.geek-right-box:has(#gb-wan-zero-up),.geek-right-box:has(#gb-cur-up-vol){display:none!important}.gege-list-item{padding:12px 10px!important}.config-item-box{position:relative!important;flex-direction:column!important;padding-bottom:0!important}.config-item .info,.config-item .logo,.config-item .speed{width:100%!important;border:none!important;padding:0!important;position:static!important}.config-item .dev-intro{min-height:auto!important;justify-content:center!important;padding-right:90px!important}.config-item .logo{padding-bottom:4px!important}.config-item .info{flex-direction:column!important;margin:0 0 6px 0!important;gap:2px!important}.dev-ip{position:absolute!important;top:0!important;right:0!important;font-size:11px!important;background:rgba(0,89,250,0.08);color:#0059fa!important;padding:2px 6px!important;border-radius:4px;font-weight:bold;line-height:1.2;z-index:10;width:auto!important}.dev-number{width:auto!important;margin:0!important;font-size:11px!important}.gege-ratio-box{width:100%!important;margin-top:2px!important;margin-bottom:0!important}.gege-down-box{width:100%!important;margin-top:2px!important}#zte-geek-board{padding:8px!important;gap:0!important;font-size:11.5px!important}.geek-row{height:auto!important;flex-wrap:wrap!important;margin-bottom:4px!important;justify-content:flex-start!important;gap:2px 6px!important;line-height:1.3!important}.geek-label{width:auto!important;min-width:60px!important;font-size:11.5px!important;flex:0 0 auto!important}.geek-val-box{width:auto!important;flex:1 1 0%!important;display:flex!important;flex-wrap:wrap!important;margin-left:0!important;gap:2px 6px!important}.geek-fixed-width{width:auto!important}.geek-right-box{width:100%!important;flex:0 0 100%!important;text-align:left!important;font-size:11.5px!important;margin-top:2px!important;margin-left:0!important}.gege-list-container{padding:8px!important}.zte-enhance-speed{gap:4px!important}}`;
  document.
  head.
  appendChild(st);
  window.gegeRenderedMacs = new Set();
  async function rSD(pWT = null, sT = null) {
    if (isF && pWT === null) return;
    isF = !0;
    let n, wT = "";
    try {
      if (pWT !== null) {
        wT = pWT; n = sT || performance.now();
      } else {
        wT = await gWT(); n = performance.now();
      }
      window.__gLWT = wT; window.__gLWT_t = n; // 保障解耦模式全局缓存不丢失
      
      const wI = wT ? (JSON.parse(wT) || {}) : {};
      S.hasW2 = !1; 
      let cWU = (+wI.UpBandwidth || 0) * 8000, cWD = (+wI.DownBandwidth || 0) * 8000, cI = Object.create(null);
      let cSU = 0, cSD = 0;
      (lCxt ? (JSON.parse(lCxt) || []) : []).forEach(d => {
        if (d.MACAddress) {
          if (!d.Active) return; // 华为特性：防死设备
          let m = nM(d.MACAddress),
            u = (+d.UpRate || 0) * 8000,
            dn = (+d.DownRate || 0) * 8000,
            uT = (+d.TxKBytes || 0) * 8000,
            dT = (+d.RxKBytes || 0) * 8000;
          let bN = d.ActualName || d.HostName || "未知设备";
          let ifc = d.InterfaceType || "";
          if (ifc !== '5GHz' && ifc !== '2.4GHz' && ifc !== 'DC') ifc = d.Layer2Interface || ifc; 

          cI[m] = {
            upRate: u, dnRate: dn, iface: ifc, // [修复] 将计算好的 ifc 真正赋给字典
            offUp: uT, offDn: dT, aRec: d.Active ? d.AccessRecord : null, name: bN, ip: d.IPAddress || "", // [优化] 不再盲目 new Date()
            rssi: d.rssi || 0, vendor: d.VendorClassID || "", rate: d.rate || 0
          };
          cSU += u; cSD += dn;
        }
      });
      let ol = document.getElementById('gege-global-overlay'),
        cM = Object.keys(
          cI),
        iD = window.gegeForceUIRedraw || (cM.length !== window.gegeRenderedMacs.size);
      if (!iD && cM.length > 0) {
        for (let i = 0; i <
          cM.length; i++) {
          if (!window.gegeRenderedMacs.has(cM[i])) {
            iD = !0;
            break;
          }
        }
      }
      if (iD) {
        for (let m in S.cls) if (!cI[m]) {
          S.cls[m].intUp += S.cls[m].upR * (n - S.cls[m].lUT) * 0.0005;
          S.cls[m].intDn += S.cls[m].dnR * (n - S.cls[m].lUT) * 0.0005;
          S.cls[m].upR = S.cls[m].dnR = 0;
        }
      }
      if (ol && ol.style.display === 'block' && (iD || !ol.querySelector('.gege-list-item'))) {
        bVD(ol, cI); // [解耦] 将洗净去重后的 cI 字典传给画板
        window.gegeRenderedMacs = new Set(
          cM);
        window.gegeForceUIRedraw = !1;
      }
      if (S.wLT === undefined) {
        S.wLT = n;
      }
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
          oU: cC.offUp, oD: cC.offDn, hU: new Float64Array(32), hD: new Float64Array(32), hIdx: 0
        };
        let cS = S.cls[m],
          dU = cC.offUp - cS.lU,
          dD = cC.offDn - cS.lD;
        if (dU < 0 || dD < 0) {
          if (dU < 0) { cS.uB += dU; cS.oU += dU; cS.dpU = cS.lU; }
          if (dD < 0) { cS.dB += dD; cS.oD += dD; cS.dpD = cS.lD; }
          cS.aR = 3;}

        else if (cS.aR === 3) {
          if (dU > 0 || dD > 0) {
          if (cS.dpU && dU > cS.dpU * 0.975 && cS.dpD && dD > cS.dpD * 0.975) {
            if (dU <= cS.dpU * 1.1 || dU <= cS.dpU + CONFIG.宽带最大外网上行速率 * CONFIG.lanRefreshInterval) {
              cS.uB += cS.dpU; cS.oU += cS.dpU;
              } else {
                cS.uB += dU; cS.oU += dU;}
             if (dD <= cS.dpD * 1.1 || dD <= cS.dpD + CONFIG.宽带最大外网下行速率 * CONFIG.lanRefreshInterval) {
              cS.dB += cS.dpD; cS.oD += cS.dpD;
              } else {cS.dB += dD; cS.oD += dD;
              }
            cS.aR = 2; 
            } else {
              cS.aR = 0; 
            }
            cS.dpU = 0; cS.dpD = 0; 
          }
        }
       else if (cS.aR > 0) { cS.aR--; }
        if (cS.aR === 2 || (cS.aR == 1 && cC.upRate > CONFIG.宽带最大外网上行速率 * 1.3) || cC.upRate > 6e8) { cSU -= cC.upRate; cC.upRate = 0; }
        if (cS.aR === 2 || (cS.aR == 1 && cC.dnRate > CONFIG.宽带最大外网下行速率 * 1.3) || cC.dnRate > 24e8) { cSD -= cC.dnRate; cC.dnRate = 0; }

        if (cC.upRate !== cS.upR || cC.dnRate !== cS.dnR || cC.offUp !== cS.lU || cC.offDn !== cS.lD) {
          cS.onS = cC.aRec ? Math.max(0, (Date.now() - new Date(cC.aRec.split('#')[0].replace(/-/g, '/')).getTime()) / 1000) : 0;
          
          if (cC.upRate !== cS.upR || cC.dnRate !== cS.dnR) {
            let ms = n - cS.lUT;
            if (cS.upR > 0) { cS.intUp += (cS.upR + cC.upRate) * ms * 0.0005; }
            else if (cC.upRate > 0) { let eU = cC.upRate * CONFIG.lanRefreshInterval * 0.5; cS.intUp += eU; cS.zEU = (cS.zEU || 0) + eU; cS.zUC = (cS.zUC || 0) + 1; }
            if (cS.dnR > 0) { cS.intDn += (cS.dnR + cC.dnRate) * ms * 0.0005; }
            else if (cC.dnRate > 0) { let eD = cC.dnRate * CONFIG.lanRefreshInterval * 0.5; cS.intDn += eD; cS.zED = (cS.zED || 0) + eD; cS.zDC = (cS.zDC || 0) + 1; }
            cS.upR = cC.upRate;
            cS.dnR = cC.dnRate;
            cS.lUT = n;
          }
        }
        cS.lU = cC.offUp;
        cS.lD = cC.offDn;
      }
      S.wInstUp = cWU;
      S.wInstDn = cWD;

      // ====================================================================
      // --- [新增] 心跳间隔雷达 (10秒硬预热 + 紧凑UI) ---
      // ====================================================================
      if (!window.__gegeDebugCache) { window.__gegeDebugCache = { isFrozen: false, targets: {}, bootTime: performance.now() }; }
      let hasChange = false;
      let currentNow = performance.now();
      let bootElapsed = currentNow - window.__gegeDebugCache.bootTime;
      let isWarmingUp = bootElapsed < 10000;
      let monitorTargets = { "WAN_MASTER": { name: "[⭐总线 WAN口]", up: cWU, down: cWD, isWan: true } }; // 获取 WAN 速率
      for(let m in cI) { monitorTargets[m] = { name: m.slice(-8), up: cI[m].upRate, down: cI[m].dnRate, isWan: false }; } // 获取 LAN 速率
      if (!window.__gegeDebugCache.isFrozen) {
          for (let mac in monitorTargets) {
              let target = monitorTargets[mac];
              let cache = window.__gegeDebugCache.targets[mac];
              if (!cache) { window.__gegeDebugCache.targets[mac] = { up: target.up, down: target.down, lastTime: currentNow, deltas: [], isFirstJump: true }; continue; }
              if (cache.up !== target.up || cache.down !== target.down) {
                  hasChange = true;
                  if (isWarmingUp) { cache.up = target.up; cache.down = target.down; cache.lastTime = currentNow; cache.isFirstJump = true; continue; }
                  let timeDiff = parseFloat((currentNow - cache.lastTime).toFixed(1));
                  if (cache.isFirstJump) { cache.isFirstJump = false; } 
                  else { cache.deltas.push(timeDiff); if (cache.deltas.length > 10000) cache.deltas.shift(); }
                  cache.up = target.up; cache.down = target.down; cache.lastTime = currentNow;
              }
          }
      }
      if (isWarmingUp) hasChange = true;
      if (hasChange && !window.__gegeDebugCache.isFrozen) {
          let dbgBox = document.getElementById('gege-debug-box');
          if (!dbgBox) {
              dbgBox = document.createElement('div'); dbgBox.id = 'gege-debug-box';
              dbgBox.style.cssText = 'position:fixed; bottom:20px; left:50%; transform:translateX(-50%); width:1080px; max-height:85vh; background:rgba(0,0,0,0.92); color:#0f0; font-family:Consolas, monospace; font-size:12px; z-index:99999; overflow-y:auto; padding:10px 12px; border-radius:6px; box-shadow:0 5px 20px rgba(0,0,0,0.9); border: 1px solid #444; backdrop-filter: blur(5px);';
              document.body.appendChild(dbgBox);
              dbgBox.addEventListener('click', (e) => {
                  if (e.target.id === 'gege-freeze-btn') {
                      window.__gegeDebugCache.isFrozen = !window.__gegeDebugCache.isFrozen;
                      let isF = window.__gegeDebugCache.isFrozen;
                      if (!isF) { let now = performance.now(); for(let k in window.__gegeDebugCache.targets) { window.__gegeDebugCache.targets[k].lastTime = now; window.__gegeDebugCache.targets[k].isFirstJump = true; } }
                      e.target.textContent = isF ? "▶️ 恢复侦测 (采集已停)" : "⏸️ 定格分析";
                      e.target.style.color = isF ? "#ff0" : "#fff"; e.target.style.background = isF ? "#520" : "#333";
                  }
              });
          }
          let warmUpHtml = isWarmingUp ? `<span style="color:#ff4c00; background:rgba(255,76,0,0.2); padding:0 5px; border-radius:3px; margin-left:10px;">⚠️ 硬件预热中... 规避初始 CPU 抖动 (剩余 ${((10000 - bootElapsed) / 1000).toFixed(1)}s)</span>` : "";
          let timeStr = new Date().toTimeString().split(' ')[0] + '.' + String(new Date().getMilliseconds()).padStart(3, '0');
          let html = `<div style="border-bottom:1px solid #0f0; padding-bottom:4px; margin-bottom:8px; display:flex; justify-content:space-between; align-items:center;"><span style="font-size:14px; font-weight:bold; text-shadow:0 0 3px #0f0;">[哥哥科技] 企业级心跳雷达${warmUpHtml}</span><div style="display:flex; align-items:center; gap:10px;"><span style="color:#aaa;">时钟: ${timeStr}</span><button id="gege-freeze-btn" style="background:#333; color:#fff; border:1px solid #555; padding:2px 8px; border-radius:3px; cursor:pointer; font-weight:bold; font-size:11px;">⏸️ 定格分析</button></div></div>`;
          let macs = Object.keys(window.__gegeDebugCache.targets).sort((a, b) => (a === "WAN_MASTER" ? -1 : (b === "WAN_MASTER" ? 1 : 0)));
          for (let mac of macs) {
              let c = window.__gegeDebugCache.targets[mac]; let n = c.deltas.length;
              if (n === 0 && !isWarmingUp) continue;
              let isWan = mac === "WAN_MASTER"; let nameTag = isWan ? "<span style='color:#ffd700; font-weight:bold;'>[WAN] 出口</span>" : "<span style='color:#aaa;'>MAC: " + mac.slice(-8) + "</span>"; let colorBase = isWan ? "#ffd700" : "#0f0";
              let mean=0, variance=0, stdDev=0, jitter=0, cv=0, d95=0, d98=0, maxD=0;
              if (n > 0) {
                  let sum = 0; for(let i=0; i<n; i++) sum += c.deltas[i];
                  mean = sum / n; let varianceSum = 0, jitterSum = 0, deviations = [];
                  for(let i=0; i<n; i++) { let diff = c.deltas[i] - mean; varianceSum += diff * diff; deviations.push(Math.abs(diff)); if (i > 0) jitterSum += Math.abs(c.deltas[i] - c.deltas[i-1]); }
                  variance = varianceSum / n; stdDev = Math.sqrt(variance); jitter = n > 1 ? (jitterSum / (n - 1)) : 0; cv = mean > 0 ? (stdDev / mean * 100) : 0; 
                  deviations.sort((a, b) => a - b); d95 = deviations[Math.floor(n * 0.95)] || 0; d98 = deviations[Math.floor(n * 0.98)] || 0; maxD = deviations[n - 1] || 0;
              }
              let statsHtml = `<div style="font-size:11px; color:#999; display:flex; gap:12px; background:rgba(0,0,0,0.3); padding:2px 6px; border-radius:3px;"><span style="width:50px;">N:<b style="color:${colorBase}">${n}</b></span><span style="width:70px;">μ:<b style="color:#fff">${mean.toFixed(1)}</b></span><span style="width:85px;">Jitter:<b style="color:#ff4c00">${jitter.toFixed(1)}</b></span><span style="width:65px;">CV:<b style="color:#00ffff">${cv.toFixed(1)}%</b></span><span style="width:65px;">σ²:<b style="color:#aaa">${variance.toFixed(1)}</b></span><span style="width:75px;">95%:<b style="color:#fff">±${d95.toFixed(1)}</b></span><span style="width:75px;">Max:<b style="color:#ff0000">±${maxD.toFixed(1)}</b></span></div>`;
              let deltaStr = isWarmingUp ? "<span style='color:#666;'>[预热静默中... 数据暂不入库]</span>" : c.deltas.join(', ');
              html += `<div style="border:1px solid ${isWan ? 'rgba(255,215,0,0.2)' : 'rgba(255,255,255,0.08)'}; background:${isWan ? 'rgba(255,215,0,0.05)' : 'transparent'}; margin-bottom:4px; padding:4px 8px; border-radius:4px; display:flex; flex-direction:column; gap:2px;"><div style="display:flex; justify-content:space-between; align-items:center;"><div style="width:110px; font-size:12px;">${nameTag}</div><div style="width:190px; color:#bbb; font-size:11px;">↑${fB(c.up).padEnd(10)} ↓${fB(c.down)}</div><div style="flex:1;">${statsHtml}</div></div><div style="width:100%; font-size:11px; overflow-x:auto; white-space:nowrap; padding:2px 4px; color:#aaa; background:#111; border-radius:2px; scrollbar-width:none; user-select:all;">${deltaStr}</div></div>`;
          }
          dbgBox.innerHTML = html;
      }
      // --- 探针代码结束 ---

      rUI(cWU, cWD, cSU, cSD, cI);
    }
    catch (e) {
      console.error("[哥哥科技] 周期采样中断:", e);
    }
    finally {
      isF = !1;
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
      const SPRK = [' ', '▂', '▃', '▄', '▅', '▆', '▇', '█'];
      function getSpark(ringArr, headIdx, maxVal) {
        let s = "";
        for (let i = 15; i >= 0; i--) {
          let v = ringArr[(headIdx - i) & 31];
          s += SPRK[v <= 0 ? 0 : Math.min(7, Math.max(1, ((v / maxVal) * 7) | 0))];
        }
        return s;
      }
      const getIconSvg = (r, isW) => {
        if (isW) return `<svg viewBox="0 0 100 100" width="45" height="45"><rect x="15" y="15" width="70" height="65" rx="5" fill="#cfd8dc" stroke="#90a4ae" stroke-width="4"/><path d="M 25,25 L 75,25 L 75,60 L 60,60 L 60,75 L 40,75 L 40,60 L 25,60 Z" fill="#263238"/><g fill="#ffca28"><rect x="30" y="25" width="2.5" height="18"/><rect x="35" y="25" width="2.5" height="18"/><rect x="40" y="25" width="2.5" height="18"/><rect x="45" y="25" width="2.5" height="18"/><rect x="52.5" y="25" width="2.5" height="18"/><rect x="57.5" y="25" width="2.5" height="18"/><rect x="62.5" y="25" width="2.5" height="18"/><rect x="67.5" y="25" width="2.5" height="18"/></g><circle cx="21" cy="73" r="3.5" fill="#4caf50"/><circle cx="79" cy="73" r="3.5" fill="#ffb300"/></svg>`;
        let c = r >= -23 ? '#4caf50' : r >= -34 ? '#9c27b0' : '#0059fa';
        if (r >= -40) return `<svg viewBox="0 0 100 100" width="45" height="45"><path d="M 50,85 L 10,35 A 65,65 0 0,1 90,35 Z" fill="${c}"/></svg>`;
        if (r >= -45) return `<svg viewBox="0 0 100 100" width="45" height="45"><path d="M 50,85 L 10,35 A 65,65 0 0,1 90,35 Z" fill="#e0e0e0"/><path d="M 50,85 L 18,45 A 50,50 0 0,1 82,45 Z" fill="${c}"/></svg>`;
        if (r >= -49) return `<svg viewBox="0 0 100 100" width="45" height="45"><path d="M 50,85 L 10,35 A 65,65 0 0,1 90,35 Z" fill="#e0e0e0"/><path d="M 50,85 L 23,51 A 43,43 0 0,1 77,51 Z" fill="${c}"/></svg>`;
        if (r >= -55) return `<svg viewBox="0 0 100 100" width="45" height="45"><circle cx="50" cy="80" r="9" fill="${c}"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="${c}" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="${c}" stroke-width="7" stroke-linecap="round"/></svg>`;
        if (r >= -60) return `<svg viewBox="0 0 100 100" width="45" height="45"><circle cx="50" cy="80" r="9" fill="${c}"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="${c}" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></svg>`;
        if (r >= -67) return `<svg viewBox="0 0 100 100" width="45" height="45"><circle cx="50" cy="80" r="9" fill="${c}"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></svg>`;
        if (r >= -70) return `<svg viewBox="0 0 100 100" width="45" height="45"><circle cx="50" cy="80" r="7" fill="none" stroke="#ff9800" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></svg>`;
        if (r >= -74) return `<svg viewBox="0 0 100 100" width="45" height="45"><g transform="translate(-15, 0)"><circle cx="50" cy="80" r="7" fill="none" stroke="#ffb300" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></g><text x="70" y="86" fill="#ffb300" font-weight="900" font-size="48" font-family="sans-serif">!</text></svg>`;
        if (r >= -77) return `<svg viewBox="0 0 100 100" width="45" height="45"><g transform="translate(-15, 0)"><circle cx="50" cy="80" r="7" fill="none" stroke="#ff4c00" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></g><text x="70" y="86" fill="#ff4c00" font-weight="900" font-size="48" font-family="sans-serif">!</text></svg>`;
        if (r >= -84) return `<svg viewBox="0 0 100 100" width="45" height="45"><g transform="translate(-15, 0)"><circle cx="50" cy="80" r="7" fill="none" stroke="#ff4c00" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#e0e0e0" stroke-width="7" stroke-linecap="round"/></g><text x="65" y="86" fill="#ff4c00" font-weight="900" font-size="44" font-family="sans-serif">?</text></svg>`;
        return `<svg viewBox="0 0 100 100" width="45" height="45"><g transform="translate(-15, 0)"><circle cx="50" cy="80" r="7" fill="none" stroke="#ff4c00" stroke-width="5"/><path d="M 30,58 A 28,28 0 0,1 70,58" fill="none" stroke="#ff4c00" stroke-width="7" stroke-linecap="round" opacity="0.3"/><path d="M 12,38 A 54,54 0 0,1 88,38" fill="none" stroke="#ff4c00" stroke-width="7" stroke-linecap="round" opacity="0.3"/></g><text x="65" y="80" fill="#ff4c00" font-weight="900" font-size="35" font-family="sans-serif">✖</text></svg>`;
      };
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
    for (let k in S.cls) {
      let s = S.cls[k], cC = cI[k];
      let cU = Math.max(0, (s.lU || 0) - (s.uB || 0));
      let cD = Math.max(0, (s.lD || 0) - (s.dB || 0));
      let sessU = Math.max(0, (s.lU || 0) - (s.oU || 0));
      let sessD = Math.max(0, (s.lD || 0) - (s.oD || 0));
      tot_cU += cU;
      LUp += s.intUp || 0;
      LDn += s.intDn || 0;
      hpU += (CONFIG.readSaveData === 2 ? cU : sessU); 
      hpD += (CONFIG.readSaveData === 2 ? cD : sessD);
      if (cC) {
        curHpU += (CONFIG.readSaveData === 2 ? cU : sessU); 
        curHpD += (CONFIG.readSaveData === 2 ? cD : sessD);
        tOD += cC.offDn || 0;
      }
      abU += CONFIG.readSaveData === 2 ? sessU : (cC ? (cC.offUp || 0) : (s.lU || 0));
      abD += CONFIG.readSaveData === 2 ? sessD : (cC ? (cC.offDn || 0) : (s.lD || 0));
      s.hIdx = (s.hIdx + 1) & 31;
      s.hU[s.hIdx] = cC ? cC.upRate : 0;
      s.hD[s.hIdx] = cC ? cC.dnRate : 0;
    }
    if (typeof GM_setValue !== 'undefined' && S.rTick === 1) {
      let cln = {};
      for (let k in S.cls) {
            let s = S.cls[k], cC = cI[k];
            cln[k] = {
                up: Math.max(0, (s.lU || 0) - (s.uB || 0)),
                down: Math.max(0, (s.lD || 0) - (s.dB || 0)),
                integral_up: s.intUp || 0,
                integral_down: s.intDn || 0,
                status: s.aR ? "off" : (CONFIG.portMap[cC?.iface] || cC?.iface || "未知接口"),
                name: cC?.name || k,
                ip: cC?.ip || "",
                raw_up: cC?.offUp || 0,
                raw_down: cC?.offDn || 0
            };}
      try {
        GM_setValue('ha_snapshot', {
          timestamp: Date.now(),
          global: {
            wan_up: S.wTotUp,
            wan_down: S.wTotDn,
            lan_integral_up: LUp,
            lan_integral_down: LDn,
            lan_high_up: hpU,
            lan_high_down: hpD,
            lan_off_up: abU,
            lan_off_down: abD
          },
          devices: cln
        });
      } catch(e) {console.warn(e);}
    }
    S.rTick = ((S.rTick || 0) + 1) & 15;
    if (S.rTick === 1 || !S.cRT) {
        S.aWu = (S.wTotUp - (S.lwTU || S.wTotUp)) / (CONFIG.wanRefreshInterval << 4); S.lwTU = S.wTotUp;
        S.aWd = (S.wTotDn - (S.lwTD || S.wTotDn)) / (CONFIG.wanRefreshInterval << 4); S.lwTD = S.wTotDn;
        if (S.hasW2) {
            let rU = S.w2TotUp > 0 ? (S.wTotUp / S.w2TotUp) : (S.wTotUp > 0 ? Infinity : 0), rD = S.w2TotDn > 0 ? (S.wTotDn / S.w2TotDn) : (S.wTotDn > 0 ? Infinity : 0);
            let fR = (r) => r === Infinity ? '∞' : (r > 1 ? r.toFixed(2) + 'x' : (r * 100).toPrecision(3) + '%');
            S.cRT = `<span style="font-weight: bold;"><span class="c-up">${fR(rU)}</span>，<span class="c-down">${fR(rD)}</span></span>`;
        } else {
            let rUp = calcStageRatio((Phys.tU + S.wTotUp) / ((Phys.tU > 0) + (S.wTotUp > 0)) || 0, LUp, hpU), rDn = calcStageRatio((Phys.tD + S.wTotDn) / ((Phys.tD > 0) + (S.wTotDn > 0)) || 0, LDn, hpD);
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
                <div class="geek-row"><span class="geek-label">WAN口速率</span><div class="geek-val-box" style="position:relative;"><span class="c-up geek-fixed-width" id="gb-wan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-wan-down-bytes"></span><span style="margin-left: 5px;"><span class="c-up" id="gb-wan-up-bps"></span> | <span class="c-down" id="gb-wan-down-bps"></span></span><div id="gb-pwan-vol-container" style="display:none; position:absolute; left:clamp(450px, 60%, 700px); color:#333; white-space:nowrap; flex-direction:column; line-height:1.2; top:-4px;"><span style="font-weight:bold;">副WAN总：<span class="c-up" id="gb-pwan-tot-up"></span> | <span class="c-down" id="gb-pwan-tot-down"></span></span><span style="font-size:12px; font-weight:normal; color:#666;">0补偿：<span id="gb-pwan-zero-up"></span>，<span id="gb-pwan-zero-down"></span>｜<span id="gb-pwan-zero-up-cnt"></span>，<span id="gb-pwan-zero-down-cnt"></span></span></div></div><div class="geek-right-box" style="font-weight: normal; color: #666;"><span style="color: #333;">0估算：</span><span id="gb-wan-zero-up"></span>，<span id="gb-wan-zero-down"></span>｜<span id="gb-wan-zero-up-cnt"></span>，<span id="gb-wan-zero-down-cnt"></span></div></div>
                <div class="geek-row"><span class="geek-label">局域网代数和</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-lan-down-bytes"></span><span id="gb-pwan-bps-container" style="display:none; margin-left: 5px;"><span class="c-up" id="gb-pwan-bps-up"></span> | <span class="c-down" id="gb-pwan-bps-down"></span></span></div><div class="geek-right-box">实时占比：<span class="c-up" id="gb-perc-up"></span> | <span class="c-down" id="gb-perc-down"></span></div></div>
                <div class="geek-row"><span class="geek-label">LAN：<span id="gege-pin-btn" class="gege-pin" title="冻结窗格">📌</span></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-lan-down-vol"></span><span style="font-weight: bold; margin-left: 5px;">WAN总计：<span class="c-up" id="gb-wan-up-vol"></span> | <span class="c-down" id="gb-wan-down-vol"></span></span></div><div class="geek-right-box">在线高精：<span style="color:#FF6700;" id="gb-cur-up-vol"></span> | <span style="color:#18A058;" id="gb-cur-down-vol"></span></div></div>
                <div class="geek-row"><span class="geek-label">高精流量统计 -></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-int-up-vol"></span><span class="c-down geek-fixed-width" id="gb-int-down-vol"></span><span style="font-weight: normal; margin-left: 5px; color:#666;">${S.hasW2?'主次网比':'内外网比'}：<span id="gb-ratio-display"></span></span></div><div class="geek-right-box" style="color: #666;">当前总计：<span style="color:#FF6700;" id="gb-abs-up-vol"></span> | <span style="color:#18A058;" id="gb-abs-down-vol"></span></div></div>`;
        } else if (CONFIG.uiLayout === 2) { // 平铺版 (报表流)
            layoutHtml = `
                <div class="geek-row"><span class="geek-label">WAN口速率</span><div class="geek-val-box" style="position:relative;"><span class="c-up geek-fixed-width" id="gb-wan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-wan-down-bytes"></span><span id="gb-pwan-vol-container" style="display:none; position:absolute; left:clamp(450px, 60%, 700px); color:#333; font-weight:bold; white-space:nowrap;">副WAN总：<span class="c-up" id="gb-pwan-tot-up"></span> | <span class="c-down" id="gb-pwan-tot-down"></span></span></div><div class="geek-right-box"><span class="c-up" id="gb-wan-up-bps"></span> | <span class="c-down" id="gb-wan-down-bps"></span></div></div>
                <div class="geek-row"><span class="geek-label">局域网代数和</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-lan-down-bytes"></span><span id="gb-pwan-bps-container" style="display:none; margin-left: 5px;"><span class="c-up" id="gb-pwan-bps-up"></span> | <span class="c-down" id="gb-pwan-bps-down"></span></span></div><div class="geek-right-box">实时占比：<span class="c-up" id="gb-perc-up"></span> | <span class="c-down" id="gb-perc-down"></span></div></div>
                <div class="geek-row"><span class="geek-label">LAN：<span id="gege-pin-btn" class="gege-pin" title="冻结窗格">📌</span></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-lan-down-vol"></span></div><div class="geek-right-box">在线高精：<span style="color:#FF6700;" id="gb-cur-up-vol"></span> | <span style="color:#18A058;" id="gb-cur-down-vol"></span></div></div>
                <div class="geek-row"><span class="geek-label">高精流量统计 -></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-int-up-vol"></span><span class="c-down geek-fixed-width" id="gb-int-down-vol"></span></div><div class="geek-right-box" style="color: #666;">当前总计：<span style="color:#FF6700;" id="gb-abs-up-vol"></span> | <span style="color:#18A058;" id="gb-abs-down-vol"></span></div></div>
                <div class="geek-row"><span class="geek-label">WAN总计：</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-wan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-wan-down-vol"></span></div><div class="geek-right-box"><span style="font-weight: normal;">${S.hasW2?'主次网比':'内外网比'}：</span><span id="gb-ratio-display"></span></div></div>`;
        } else { // 经典版 (0)
            layoutHtml = `
                <div class="geek-row"><span class="geek-label">WAN口速率</span><div class="geek-val-box" style="position:relative;"><span class="c-up geek-fixed-width" id="gb-wan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-wan-down-bytes"></span><span id="gb-pwan-vol-container" style="display:none; position:absolute; left:clamp(450px, 60%, 700px); color:#333; font-weight:bold; white-space:nowrap;">副WAN总：<span class="c-up" id="gb-pwan-tot-up"></span> | <span class="c-down" id="gb-pwan-tot-down"></span></span></div><div class="geek-right-box"><span class="c-up" id="gb-wan-up-bps"></span> | <span class="c-down" id="gb-wan-down-bps"></span></div></div>
                <div class="geek-row"><span class="geek-label">局域网代数和</span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-bytes"></span><span class="c-down geek-fixed-width" id="gb-lan-down-bytes"></span><span id="gb-pwan-bps-container" style="display:none; margin-left: 5px;"><span class="c-up" id="gb-pwan-bps-up"></span> | <span class="c-down" id="gb-pwan-bps-down"></span></span></div><div class="geek-right-box">实时占比：<span class="c-up" id="gb-perc-up"></span> | <span class="c-down" id="gb-perc-down"></span></div></div>
                <div class="geek-row"><span class="geek-label">LAN：<span id="gege-pin-btn" class="gege-pin" title="冻结窗格">📌</span></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-lan-up-vol"></span><span class="c-down geek-fixed-width" id="gb-lan-down-vol"></span></div><div class="geek-right-box">WAN：<span class="c-up" id="gb-wan-up-vol"></span> | <span class="c-down" id="gb-wan-down-vol"></span></div></div>
                <div class="geek-row"><span class="geek-label">高精流量统计 -></span><div class="geek-val-box"><span class="c-up geek-fixed-width" id="gb-int-up-vol"></span><span class="c-down geek-fixed-width" id="gb-int-down-vol"></span></div><div class="geek-right-box" style="color: #666;">当前总计：<span style="color:#FF6700;" id="gb-abs-up-vol"></span> | <span style="color:#18A058;" id="gb-abs-down-vol"></span></div></div>`;
        }layoutHtml += `<div class="geek-row" id="gb-phys-row" style="display:none; height:auto!important; flex-wrap:wrap;"><span class="geek-label" style="font-weight:normal; color:#666;">物理网口:</span><div class="geek-val-box" id="gb-phys-data" style="flex-wrap:wrap; font-size:13px; font-weight:normal;"></div></div>`;
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
    if (iPO) {
      let ac = document.getElementById('gege-board-anchor');
      if (ac && bd.nextSibling !== ac) ac.parentNode.insertBefore(bd, ac);
    }
    else {
      let mn = document.querySelector('.el-table') || document.querySelector('.config-item')?.closest('div') || document.querySelector('.main-content');
      if (mn && bd.parentNode !== mn.parentNode) mn.parentNode.insertBefore(bd, mn);
    }
    requestAnimationFrame(() => {
      if (!S.oDC || S._domRebuilt || S._lastPanelState !== iPO) {
        S.oDC = Object.create(null);
        if (!iPO) {
          const M_RX = /([a-fA-F0-9]{2}[:-]){5}[a-fA-F0-9]{2}/;
          let aI = aC.getElementsByClassName('config-item');
          for (let n of aI) {
            let mN = n.getElementsByClassName('dev-number')[0];
            let mM = mN ? mN.textContent.match(M_RX) : null;
            if (mM) S.oDC[mM[0].toLowerCase().replaceAll('-', ':')] = n;
          }
        } else {
          let gI = aC.getElementsByClassName('gege-list-item');
          for (let n of gI) {
            let m = n.getAttribute('data-gege-mac');
            if (m) S.oDC[m] = n;
          }
        }
        S._domRebuilt = false;
        S._lastPanelState = iPO;
      }
      let oDC = S.oDC; 
      if (bd.parentNode) {
        let aW2U = S.hasW2 ? S.w2U : undefined,aW2D = S.hasW2 ? S.w2D : undefined,aW2TU = S.hasW2 ? S.w2TotUp : undefined,aW2TD = S.hasW2 ? S.w2TotDn : undefined;
        bd.querySelector('#gb-wan-up-bytes').textContent = `🔼 ${fBy(wU + (aW2U||0))}`;
        bd.querySelector('#gb-wan-down-bytes').textContent = `🔽 ${fBy(wD + (aW2D||0))}`;
        bd.querySelector('#gb-wan-up-bps').textContent = `🔼 ${fB(wU)}`;
        bd.querySelector('#gb-wan-down-bps').textContent = `🔽 ${fB(wD)}`;
        bd.querySelector('#gb-lan-up-bytes').textContent = `🔼 ${fB(sU)}`;
        bd.querySelector('#gb-lan-down-bytes').textContent = `🔽 ${fB(sD)}`;
        bd.querySelector('#gb-lan-up-vol').textContent = `🔼 ${fV(LUp)}`;
        bd.querySelector('#gb-lan-down-vol').textContent = `🔽 ${fV(LDn)}`;
        bd.querySelector('#gb-wan-up-vol').textContent = `🔼 ${fV(S.wTotUp)}`;
        bd.querySelector('#gb-wan-down-vol').textContent = `🔽 ${fV(S.wTotDn)}`;
        bd.querySelector('#gb-int-up-vol').textContent = `🔼 ${fV(hpU)}`;
        bd.querySelector('#gb-int-down-vol').textContent = `🔽 ${fV(hpD)}`;
        bd.querySelector('#gb-abs-up-vol').textContent = `🔼 ${fV(abU)}`;
        bd.querySelector('#gb-abs-down-vol').textContent = `🔽 ${fV(abD)}`;
		bd.querySelector('#gb-perc-up').textContent = `🔼 ${((sU * 100) / (Math.max(Phys.wU || 0, wU || 0) || Infinity) || 0).toFixed(1)}%`;
        bd.querySelector('#gb-perc-down').textContent = `🔽 ${((sD * 100) / (Math.max(Phys.wD || 0, wD || 0) || Infinity) || 0).toFixed(1)}%`;
        let pb = bd.querySelector('#gb-pwan-bps-container'), pv = bd.querySelector('#gb-pwan-vol-container');
        if (aW2U !== undefined) {
            if (pb) { pb.style.display = 'inline'; bd.querySelector('#gb-pwan-bps-up').textContent = '🔼 ' + fB(aW2U); bd.querySelector('#gb-pwan-bps-down').textContent = '🔽 ' + fB(aW2D); }
            if (pv) { 
                pv.style.display = 'flex'; 
                bd.querySelector('#gb-pwan-tot-up').textContent = '🔼 ' + fV(aW2TU); 
                bd.querySelector('#gb-pwan-tot-down').textContent = '🔽 ' + fV(aW2TD); 
                if (bd.querySelector('#gb-pwan-zero-up')) {
                    let isPhysTakeover = CONFIG.lanPortMode === 1 && !S.hasW2;
                    bd.querySelector('#gb-pwan-zero-up').textContent = (isPhysTakeover && Phys.zEU) ? fSV(Phys.zEU) : '';
                    bd.querySelector('#gb-pwan-zero-down').textContent = (isPhysTakeover && Phys.zED) ? fSV(Phys.zED) : '';
                    bd.querySelector('#gb-pwan-zero-up-cnt').textContent = (isPhysTakeover && Phys.zEUC) ? Phys.zEUC : 0;
                    bd.querySelector('#gb-pwan-zero-down-cnt').textContent = (isPhysTakeover && Phys.zEDC) ? Phys.zEDC : 0;
                }
            }
        } else if (CONFIG.lanPortMode !== 1 || Phys.wU === undefined) {
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
        
        let rRs = cC.rssi ? cC.rssi - 93 : 0, lRs = cS.lRs ?? rRs;
        if (cC.rssi) { // 漏桶防抖核心（仅针对 -23, -34, -40, -55 敏感边界）
          if ([-23, -34, -40, -55].some(t => (rRs >= t) !== (lRs >= t)) && Math.abs(rRs - lRs) + (cS.dbC || 0) <= 4) {
            rRs = lRs; cS.dbC = ((cS.dbC || 0) + 1) & 7; // 憋住不闪，压力槽+1
          } else { cS.dbC = 0; cS.lRs = rRs; } // 压力爆表或正常滑动，放行并归零
        }
        (cache.logo ??= it.querySelector('.dev-logo')).innerHTML = getIconSvg(rRs, !cC.rssi) + (cC.rate ? `<div style="font-size:10.5px;color:#999;font-family:Consolas;margin-top:2px;">rate:${cC.rate}</div>` : '');

        let hqU = Math.max(0, (cS.lU || 0) - (cS.uB || 0));
        let hqD = Math.max(0, (cS.lD || 0) - (cS.dB || 0));
        let tN = cache.timeNode ??= it.querySelector('.gege-online-time');
        if (tN && cS.onS > 0) tN.textContent = `在线：${fOT(cS.onS)}`;
        
        const dI = cache.devIntro ??= it.querySelector('.dev-intro');
        if (dI) {
          let rN = cache.rssiNode ??= dI.querySelector('.gege-rssi');
          if (rN) rN.innerHTML = cC.rssi ? `<span style="color:${((cC.rssi<<1)-37)<0?'#ff4c00':'inherit'}">${(cC.rssi<<1)-37}%</span>, ${cC.rssi-93}` : escapeHTML(cC.vendor || '');
          
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
          for (let i = 0; i < 32; i++) { if (cS.hU[i] > clU) clU = cS.hU[i]; if (cS.hD[i] > clD) clD = cS.hD[i]; }
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
  async function bVD(ol, cI = {}) {
    try {
      let h2 = [], h52 = [], h58 = [], hW = [];
      let isStd = lCxt && lCxt.includes('5GHz');
      for (let m in cI) {
        let d = cI[m], tS = fOT(d.onSec), ifc = d.iface;
        let htm = `<div class="col-md-12 col-xs-12 config-item gege-list-item" data-gege-mac="${m}"><div class="config-item-box" style="display: flex; align-items: stretch;"><div class="col-md-5 col-xs-7 logo" style="width: 33%; display: flex; flex-direction: row; align-items: center;"><div class="dev-logo" style="width: 50px; min-width: 50px; margin-right: 15px; display: flex; flex-direction: column; align-items: center; justify-content: center;"></div><div class="dev-intro" style="flex: 1; display: flex; flex-direction: column; justify-content: flex-start; min-height: 100px;"><div style="display: flex; justify-content: space-between; align-items: baseline; width: 95%;"><div class="dev-name" style="font-weight: bold; color: #333; font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; flex: 1; min-width: 0; margin-right: 8px;">${escapeHTML(d.name)}</div><div class="gege-rssi" style="font-size: 12px; font-weight: bold; color: #666; font-family: Consolas; white-space: nowrap; flex-shrink: 0;"></div></div><div class="gege-online-time" style="color: #999; font-size: 12px; font-family: Consolas; margin-top: 4px;">${tS?'在线：'+tS:''}</div></div></div><div class="col-md-4 col-xs-5 info" style="width: 27%; display: flex; flex-direction: column; padding: 0 10px; border-right: 1px solid #eee;"><div class="dev-ip" style="color: #666; font-family: Consolas;">${escapeHTML(d.ip)}</div><div class="dev-number grey" style="color: #999; font-size: 12px; font-family: Consolas;">MAC：${m}</div></div><div class="col-md-3 col-xs-12 speed" style="width: 40%; display: flex; flex-direction: column; justify-content: center; padding: 0 10px;"></div></div></div>`;
        if (isStd) {
          if (ifc === '5GHz') h52.push(htm);
          else if (ifc === '2.4GHz') h2.push(htm);
          else if (ifc === 'DC') h58.push(htm);
          else hW.push(htm); 
        } else {
          if (ifc.includes('5') || ifc.includes('SSID5')) h52.push(htm);
          else if (ifc.includes('2.4') || ifc.includes('SSID1')) h2.push(htm); 
          else if (/w/i.test(ifc) || ifc === 'DC') h58.push(htm);
          else hW.push(htm);
        }
      }
      requestAnimationFrame(() => {
        ol.innerHTML = `<div style="padding: 20px; max-width: 1580px; margin: 0 auto; min-height: 100%;"><div id="gege-board-anchor"></div><div id="config-list" class="config-list gege-list-container"><div class="gege-section"><div class="config-title">有线设备${(window.gegeHiddenDevices && Object.keys(window.gegeHiddenDevices).length > 0) ? '<span style="color: #ff4c00; font-size: 13px; font-weight: normal; margin-left: 10px; font-family: Consolas;">(哥哥科技：智能Mesh适配)</span>' : ''}</div>${hW.join('')||'<div class="gege-empty-state">没有连接设备</div>'}</div><div class="gege-section"><div class="config-title">无线设备（${S.is5G_149?'5.8GHz':'5.2GHz'}）</div>${h52.join('')||'<div class="gege-empty-state">没有连接设备</div>'}</div><div class="gege-section"><div class="config-title">${h58.length>0?(S.is5G_149===null?'MLO 设备（2.4+单 5G）':(S.is5G_149?'MLO 设备（2.4+5.8G）':'MLO 设备（2.4+5.2G）')):`无线设备（${S.is5G_149?'5.2GHz':'5.8GHz'}）`}</div>${h58.join('')||'<div class="gege-empty-state">没有连接设备</div>'}</div><div class="gege-section"><div class="config-title">无线设备（2.4GHz）</div>${h2.join('')||'<div class="gege-empty-state">没有连接设备</div>'}
        </div><div style="margin-top: 25px; padding-top: 15px; border-top: 1px dashed #eee; text-align: center; font-family: Consolas, 'Microsoft YaHei', sans-serif;"><div style="font-size: 11.5px; color: #777; font-style: italic; margin-bottom: 8px;">“在一个文明社会，干净的、不被监视与吸血的网络，是我们每个人的基本权利。”</div><div style="font-size: 10.5px; color: #999; line-height: 1.3; margin-bottom: 8px;">本交互式程序基于 GNU Affero GPL v3.0 协议开源，按“原样 (AS IS)”提供，不对其适用性、稳定性、精密度或任何商业场景合规性作任何明示或暗示的担保。<br>根据 AGPL-3.0 第 5(d) 及 7(b) 条规定，基于本程序的任何修改均不得移除或篡改本界面的署名与法律声明。保留此界面是使用本软件代码的合法性的前置条件。
        </div><div style="font-size: 12px; color: #555;"><a href="https://github.com/ucxn/Bro-Stat/blob/main/Huawei-1.user.js" target="_blank" style="color: #0059fa; text-decoration: none; font-weight: bold;">Bro-Stat 增强组件</a> <span title="构建时间：2026-06.26 1时&#10;架构设计：哥哥科技 BroTech&#10;Bilibili UID：501430041&#10;QQ群：680464365" style="background: rgba(0,0,0,0.04); padding: 2px 6px; border-radius: 4px; cursor: help; margin: 0 4px; font-family: Consolas;">华为版 5.9.5</span> | Copyright &copy; 2026 <a href="https://www.bilibili.com/video/BV1PtR7B8ECC" target="_blank" style="color: #0059fa; text-decoration: none; font-weight: bold;">哥哥科技</a> (BroTech)<span style="color: #888; font-weight: normal;"> | All Rights Reserved</span>&emsp;&nbsp;<a href="https://scriptcat.org/users/203510" target="_blank" style="color: #666; text-decoration: none;">点击分享</a></div></div></div></div>`;
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
    b.style.cssText = `position: fixed; ${CONFIG.injectMode === 3 ? 'bottom: 60px; right: 60px;' : 'top: 20px; right: 16%;'} width: 50px; height: 50px; background: linear-gradient(135deg, #0059fa, #00c6ff); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 48px; box-shadow: 0 4px 15px rgba(0,89,250,0.5); cursor: pointer; z-index: 99999; transition: transform 0.3s ease; user-select: none;`;
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
      iCO =
      o && o.style.display === 'block',
      tS = fS !== null ? fS : !iCO,
      aT = document.querySelector(
        '#gege-menu-wrapper a'),
      lT = document.querySelector('#gege-menu-wrapper li');
    if (!tS) {
      if (lT) {
        lT.classList.remove(
          'is-active');
        lT.style.color = 'rgb(255, 255, 255)';
      }
      if (o) o.style.display = 'none';
      return;
    }
    if (aT &&
      lT) {
      aT.classList.add('router-link-exact-active', 'router-link-active');
      lT.classList.add('is-active');
      lT.style.color =
        'rgb(61, 163, 247)';
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
      window.gegeMasterTimer = setInterval(eBET, CONFIG.wanRefreshInterval * 1000);
    }f5G_Probe();
    bVD(o).then(() => eBET());
  };

  function iGM() {
    let mC = document.querySelector('.btn_box');
    if (!mC) return;
    let oD = mC.querySelector(
      '.logout');
    if (!oD) return;
    let gW = oD.cloneNode(!0);
    gW.innerHTML = '<span>退出登录</span>'; gW.id = 'gege-menu-wrapper';
    gW.className = 'logout fl marginright_50'; 
    let aT = null,
      lT = gW;
    if (aT) {
      aT.href = "javascript:void(0);";
      aT.classList.remove(
        'router-link-exact-active', 'router-link-active');
    }
    if (lT) {
      lT.classList.remove('is-active');
      let tS =
        lT.querySelector('span');
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
      lT.style.color = 'rgb(204, 51, 255)';
    }
    mC.insertBefore(gW, oD);
    document.
    addEventListener('click', function (e) {
      let cW = e.target.closest('.btn_box > div');
      if (!cW) return;
      if (
        cW.id === 'gege-menu-wrapper') {
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
  window.gegePortTimer = null;
  window.gegeBActivated = !1;
  window.gegeEngineRunning = !1;
  window.gegeLastDevCount = -1;
  window.gegeLastMeshDevCount = -1;
  window.gegeHiddenDevices = {};
  window.gegeTimerStarted = !1;
  window.gegeSyncAnchor = 0;
  window.gegeTickCount = 0;
  window.gegeMasterTimer = null;
  window.scheduleNextGegeTick = function () {
    if (window.gegeBActivated) return;
    window.gegeTickCount++;
    let dl = (window.gegeSyncAnchor + window.gegeTickCount * 3000) - performance.now();
    if (dl < 0) {
      window.gegeSyncAnchor = performance.now();
      window.gegeTickCount = 0;
      dl = 3000;
    }
    window.gegeMasterTimer = setTimeout(() => {
      rSD().finally(() => {
        window.scheduleNextGegeTick();
      });
    }, dl);
  };
async function eBET(fW = !0) {
    if (window.gegeEngineRunning) return;
    window.gegeEngineRunning = !0;
    try {
      const ts = Date.now();
      let wT = "", wST = null;
      if (fW) {
        wT = await gWT();
        wST = performance.now();
      }
      let lR = await fetch(`/api/system/HostInfo?_=${ts}`);
      if (lR.ok) lCxt = await lR.text();
      if (fW) await rSD(wT, wST);
    }
    catch (e) {
      console.warn("[哥哥科技] 华为引擎中断(将重试):", e.message);
    }
    finally {
      window.gegeEngineRunning = !1;
    }
  }
  async function f5G_Probe() {
    try {
      const res = await fetch(`/api/system/diagnose_wlan_basic?type=2&_=${Date.now()}`);
      if (res.ok) {
        const d = await res.json();
        if (d && d.Channel) {
          S.is5G_149 = parseInt(d.Channel) >= 149;
          // [解耦重绘] 如果探针拿到了新数据且面板开着，立刻刷新一次表头
          let ol = document.getElementById('gege-global-overlay');
          if (ol && ol.style.display === 'block') bVD(ol, S.oDC ? Object.create(null) : {}); 
        }
      }
    } catch(e) { console.warn("[哥哥科技] 5.8G彩蛋探测异常:", e); }
  }
  const tKA = () => {
    let i = document.createElement('iframe');
    i.id = 'gege-keepalive-iframe';
    i.style.display = 'none';
    const p = ["/html/index.html#/internet", "/html/index.html#/more/firewall"];
    i.src = `${window.location.origin}${p[(Math.random() * p.length) | 0]}`;
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
  setInterval(tKA, 216000);
  window.addEventListener('load', () => {
    setTimeout(() => {
      if (!window.gegeTimerStarted && window.startGegePrecisionEngine) window.startGegePrecisionEngine();
    }, 60000);
    if (CONFIG.injectMode === 3 || (CONFIG.injectMode === 1 && +(window.location.hostname.slice(window.location.hostname.lastIndexOf('.') + 1)) < 6)) {
      if (window.createGegeFloatingBtn) window.createGegeFloatingBtn();
    }
    if (CONFIG.injectMode !== 3) {
      let dC = 0;
      const mO = setInterval(() => {
        let mC = document.querySelector('.btn_box');
        if (mC) {
          clearInterval(mO);
          iGM();
        if (CONFIG.injectMode === 2 && window.createGegeFloatingBtn) window.createGegeFloatingBtn();
        }
        else if (++dC > 200) {
          clearInterval(mO);
        }
      }, 300);
    }
  });
})();