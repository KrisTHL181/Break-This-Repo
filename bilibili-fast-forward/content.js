(() => {
  'use strict';

  const DEFAULTS = {
    seconds: 85,
    shortcut: { key: 'arrowright', code: 'ArrowRight', ctrl: false, alt: false, shift: true },
  };

  const BTN_CLASS = 'bff-ctrl-btn';
  const ICON_CLASS = 'bff-btn-icon';
  const TIP_CLASS = 'bff-btn-tip';

  // 88x88 与B站自带图标同视口，墨迹占 66x50（B站原生图标实测填充率 67%~82%，过大会显得比相邻图标粗重）。
  // width/height 属性只是最弱的一层兜底：B站的 .bpx-common-svg-icon svg{width:100%;height:100%}
  // 一定会覆盖它们，所以真正防住「加载视频时铺满播放器的巨大色块」的是下面的绘制前自检 sizingOk()。
  const ICON_SVG =
    '<span class="bpx-common-svg-icon">' +
    '<svg viewBox="0 0 88 88" width="22" height="22" aria-hidden="true">' +
    '<path d="M11 19L41 44L11 69Z"></path>' +
    '<path d="M47 19L77 44L47 69Z"></path>' +
    '</svg>' +
    '</span>';

  const KEY_LABELS = {
    arrowright: '→',
    arrowleft: '←',
    arrowup: '↑',
    arrowdown: '↓',
    space: 'Space',
    enter: 'Enter',
    tab: 'Tab',
    backspace: 'Backspace',
    delete: 'Delete',
    insert: 'Insert',
    home: 'Home',
    end: 'End',
    pageup: 'PageUp',
    pagedown: 'PageDown',
    escape: 'Esc',
  };

  function shortcutLabel(sc) {
    if (!sc || !sc.key) return '';
    const key = String(sc.key).toLowerCase();
    const parts = [];
    if (sc.ctrl) parts.push('Ctrl');
    if (sc.alt) parts.push('Alt');
    if (sc.shift) parts.push('Shift');
    parts.push(KEY_LABELS[key] || (key.length === 1 ? key.toUpperCase() : key));
    return parts.join('+');
  }

  let config = { seconds: DEFAULTS.seconds, shortcut: { ...DEFAULTS.shortcut } };

  // 扩展环境使用 chrome.storage；直接在页面中引入本文件（本地测试）时退回默认值
  const storageArea =
    typeof chrome !== 'undefined' && chrome.storage && chrome.storage.sync
      ? chrome.storage.sync
      : null;

  function loadConfig() {
    if (!storageArea) return;
    const apply = () =>
      storageArea.get(DEFAULTS).then((cfg) => {
        config = cfg;
        updateButtonLabel();
      });
    apply();
    if (chrome.storage.onChanged) {
      chrome.storage.onChanged.addListener((_changes, area) => {
        if (area === 'sync') apply();
      });
    }
  }

  function getVideo() {
    return document.querySelector(
      '.bpx-player-video-wrap video, .bpx-player-container video, video'
    );
  }

  function fastForward() {
    const video = getVideo();
    // 直播等无限时长的媒体不做快进
    if (!video || !isFinite(video.duration) || video.duration <= 0) return false;
    video.currentTime = Math.max(
      0,
      Math.min(video.currentTime + config.seconds, video.duration - 0.1)
    );
    return true;
  }

  function updateButtonLabel(btn) {
    btn = btn || document.querySelector('.' + BTN_CLASS);
    if (!btn) return;
    const tip = btn.querySelector('.' + TIP_CLASS);
    const sc = shortcutLabel(config.shortcut);
    const label = `快进${config.seconds}s` + (sc ? ` (${sc})` : '');
    if (tip) tip.textContent = label;
    btn.setAttribute('aria-label', `一键快进 ${config.seconds} 秒${sc ? '，快捷键 ' + sc : ''}`);
  }

  // 结构照搬B站原生图标按钮（参考「小电视空降助手」）：尺寸与悬停提亮交给原生CSS，
  // 控制栏里只占一个标准图标位，文字通过悬浮提示展示
  function buildButton() {
    const btn = document.createElement('div');
    btn.className = `bpx-player-ctrl-btn ${BTN_CLASS}`;
    btn.setAttribute('role', 'button');
    btn.innerHTML =
      `<div class="bpx-player-ctrl-btn-icon ${ICON_CLASS}">${ICON_SVG}</div>` +
      `<div class="${TIP_CLASS}"></div>`;
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      e.preventDefault();
      fastForward();
    });
    updateButtonLabel(btn);
    return btn;
  }

  // ===== 绘制前自检：杜绝加载视频时的巨大色块 =====
  // 图标尺寸完全由B站控制栏的百分比高度链决定（.bpx-player-ctrl-btn-icon
  // > .bpx-common-svg-icon{height:22px}，全屏 28px）。播放器初始化期间这条链还没就位时，
  // 同一个 svg 会按 100% 铺满整个播放器（实测 400~472px），就是那块遮挡约一秒的色块。
  // 自定义宽高压不住它（还会被 [data-screen] 规则覆盖而导致全屏下沉），所以改为按
  // 「实际渲染尺寸」把关：插入与测量在同一个任务里完成，后到的样式在 MutationObserver
  // 微任务里复查——两者都早于本帧绘制，因此未通过自检的按钮永远不会被画出来。
  const MAX_ICON_SIDE = 60; // 全屏实测 54x28，无参照物时用它兜底
  const RETRY_MS = 300;
  const TICK_MS = 200;

  // 同一条控制栏里原生图标的中位渲染高度；B站的隐藏态节点是 0x0，不计入
  function peerIconHeight() {
    const hs = [];
    document
      .querySelectorAll('.bpx-player-ctrl-btn:not(.' + BTN_CLASS + ') .bpx-common-svg-icon svg')
      .forEach((n) => {
        const h = n.getBoundingClientRect().height;
        if (h > 0) hs.push(h);
      });
    if (!hs.length) return 0;
    hs.sort((a, b) => a - b);
    return hs[hs.length >> 1];
  }

  function sizingOk(btn) {
    const svg = btn.querySelector('svg');
    if (!svg) return false;
    const { width, height } = svg.getBoundingClientRect();
    // 0x0 说明控制栏尚未参与布局，此时测不出真实尺寸，等下一轮
    if (width < 1 || height < 1) return false;
    const ref = peerIconHeight();
    if (!ref) return width <= MAX_ICON_SIDE && height <= MAX_ICON_SIDE;
    // 巨大色块出现时相邻原生图标仍是 22/28px，量级差一眼可辨；
    // 若整条控制栏都被撑大（更极端的未样式化状态），则跟随原生，不作拦截
    return height <= ref * 1.5 + 2 && height >= ref * 0.5 - 2;
  }

  let retryAt = 0;
  function drop(btn) {
    btn.remove();
    retryAt = Date.now() + RETRY_MS;
  }

  // B站的播放器样式分多批注入，新样式到达可能把我们已插入的图标撑大，所以样式变动后要复查。
  // 判据只在 STYLE/LINK 新增时触发：弹幕层每帧都在改 DOM，不能每次都去强制布局。
  let cssChanged = false;

  // 插入到「清晰度」按钮左侧；旧版播放器或无清晰度按钮时依次回退
  function ensureButton() {
    const existing = document.querySelector('.' + BTN_CLASS);
    if (existing) {
      if (cssChanged) {
        cssChanged = false;
        if (!sizingOk(existing)) drop(existing);
      }
      return;
    }
    if (Date.now() < retryAt) return;
    const anchor =
      document.querySelector('.bpx-player-ctrl-quality') ||
      document.querySelector('.squirtle-quality') ||
      document.querySelector('.bpx-player-ctrl-setting');
    const container = anchor
      ? anchor.parentElement
      : document.querySelector('.bpx-player-control-bottom-right');
    if (!container) return;
    const btn = buildButton();
    if (anchor) container.insertBefore(btn, anchor);
    else container.appendChild(btn);
    cssChanged = false;
    if (!sizingOk(btn)) drop(btn);
  }

  let ensureScheduled = false;
  function scheduleEnsure() {
    if (ensureScheduled) return;
    ensureScheduled = true;
    const run = () => {
      ensureScheduled = false;
      ensureButton();
    };
    if (typeof requestAnimationFrame === 'function') requestAnimationFrame(run);
    else setTimeout(run, 50);
  }

  function isTypingTarget(el) {
    if (!el || typeof el.closest !== 'function') return false;
    return !!el.closest(
      'input, textarea, select, [contenteditable="true"], [contenteditable=""]'
    );
  }

  function matchesShortcut(e) {
    const sc = config.shortcut;
    if (!sc || !sc.key) return false;
    let key = e.key || '';
    key = key === ' ' ? 'space' : key.toLowerCase();
    const keyHit = key === sc.key || (sc.code && e.code === sc.code);
    if (!keyHit) return false;
    return e.ctrlKey === !!sc.ctrl && e.altKey === !!sc.alt && e.shiftKey === !!sc.shift;
  }

  // window + 捕获阶段：先于B站播放器自身的快捷键处理
  window.addEventListener(
    'keydown',
    (e) => {
      if (isTypingTarget(e.target)) return;
      if (!matchesShortcut(e)) return;
      if (!fastForward()) return;
      e.preventDefault();
      e.stopPropagation();
    },
    true
  );

  const observer = new MutationObserver((records) => {
    // 样式表只会挂进 <head>/<html>，先过滤掉弹幕层每秒上百条的普通 DOM 变更
    for (const rec of records) {
      const parent = rec.target;
      if (!parent || parent.nodeType !== 1) continue;
      const tag = parent.tagName;
      if (tag !== 'HEAD' && tag !== 'HTML') continue;
      for (const node of rec.addedNodes) {
        if (node.nodeType === 1 && (node.tagName === 'STYLE' || node.tagName === 'LINK')) {
          cssChanged = true;
          break;
        }
      }
      if (cssChanged) {
        // 不排进 rAF：MutationObserver 回调是本帧绘制前的微任务，这里同步复查就能
        // 把被新样式撑大的按钮在同一帧内撤掉（后台标签页里 rAF 甚至不会触发）
        ensureButton();
        return;
      }
    }
    scheduleEnsure();
  });
  // 观察 documentElement 才能同时看到 <head> 里后到的样式表
  observer.observe(document.documentElement, { childList: true, subtree: true });
  // 兜底：B站是 SPA，切页/全屏后控件与样式可能重建
  setInterval(ensureButton, TICK_MS);
  ensureButton();
  loadConfig();
})();
