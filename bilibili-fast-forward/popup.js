const DEFAULTS = {
  seconds: 85,
  shortcut: { key: 'arrowright', code: 'ArrowRight', ctrl: false, alt: false, shift: true },
};

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
  home: 'Home',
  end: 'End',
  pageup: 'PageUp',
  pagedown: 'PageDown',
  insert: 'Insert',
  escape: 'Esc',
};

const NAMED_KEYS = new Set([
  'arrowright', 'arrowleft', 'arrowup', 'arrowdown', 'space', 'enter', 'tab', 'backspace',
  'delete', 'home', 'end', 'pageup', 'pagedown', 'insert', 'escape',
]);

// 扩展环境使用 chrome.storage；直接用浏览器打开本页（预览/测试）时退回 localStorage
const hasChromeStorage =
  typeof chrome !== 'undefined' && chrome.storage && chrome.storage.sync;

const $ = (id) => document.getElementById(id);
const secondsInput = $('seconds');
const recorder = $('recorder');
const saveBtn = $('save');
const resetBtn = $('reset');
const statusEl = $('status');

let currentShortcut = { ...DEFAULTS.shortcut };

function labelForShortcut(sc) {
  const parts = [];
  if (sc.ctrl) parts.push('Ctrl');
  if (sc.alt) parts.push('Alt');
  if (sc.shift) parts.push('Shift');
  let k = String(sc.key || '').toLowerCase();
  if (KEY_LABELS[k]) k = KEY_LABELS[k];
  else if (/^[a-z]$/.test(k)) k = k.toUpperCase();
  else if (/^f\d{1,2}$/.test(k)) k = k.toUpperCase();
  parts.push(k || '未设置');
  return parts.join(' + ');
}

function renderShortcut() {
  recorder.textContent = labelForShortcut(currentShortcut);
}

function loadSettings() {
  if (hasChromeStorage) {
    chrome.storage.sync.get(DEFAULTS).then((cfg) => {
      secondsInput.value = cfg.seconds;
      currentShortcut = cfg.shortcut;
      renderShortcut();
    });
    return;
  }
  let cfg = { ...DEFAULTS };
  try {
    const raw = localStorage.getItem('bff-settings');
    if (raw) cfg = { ...DEFAULTS, ...JSON.parse(raw) };
  } catch (err) {
    /* 忽略损坏的本地数据 */
  }
  secondsInput.value = cfg.seconds;
  currentShortcut = cfg.shortcut;
  renderShortcut();
}

function saveSettings(cfg) {
  if (hasChromeStorage) return chrome.storage.sync.set(cfg);
  try {
    localStorage.setItem('bff-settings', JSON.stringify(cfg));
  } catch (err) {
    /* 忽略 */
  }
  return Promise.resolve();
}

let statusTimer = null;
function showStatus(text, ok) {
  statusEl.textContent = text;
  statusEl.className = 'status ' + (ok ? 'ok' : 'err');
  clearTimeout(statusTimer);
  statusTimer = setTimeout(() => {
    statusEl.textContent = '';
    statusEl.className = 'status';
  }, 2500);
}

recorder.addEventListener('click', () => recorder.focus());

recorder.addEventListener('keydown', (e) => {
  e.preventDefault();
  e.stopPropagation();

  if (e.key === 'Escape' || e.key === 'Backspace') {
    currentShortcut = { ...DEFAULTS.shortcut };
    renderShortcut();
    return;
  }

  // 仅按住修饰键时显示录制中间态
  if (['Control', 'Alt', 'Shift', 'Meta'].includes(e.key)) {
    const parts = [];
    if (e.ctrlKey) parts.push('Ctrl');
    if (e.altKey) parts.push('Alt');
    if (e.shiftKey) parts.push('Shift');
    recorder.textContent = (parts.length ? parts.join(' + ') + ' + ' : '') + '…';
    return;
  }

  let key = e.key === ' ' ? 'space' : String(e.key).toLowerCase();
  const printable = key.length === 1;
  const named = NAMED_KEYS.has(key) || /^f\d{1,2}$/.test(key);
  if (!printable && !named) {
    showStatus('不支持该按键，请换一个组合', false);
    renderShortcut();
    return;
  }

  currentShortcut = {
    key,
    code: e.code || '',
    ctrl: e.ctrlKey,
    alt: e.altKey,
    shift: e.shiftKey,
  };
  renderShortcut();
});

saveBtn.addEventListener('click', () => {
  const seconds = Math.round(Number(secondsInput.value));
  if (!Number.isFinite(seconds) || seconds < 1 || seconds > 3600) {
    showStatus('快进秒数需为 1 ~ 3600 之间的整数', false);
    return;
  }
  if (!currentShortcut || !currentShortcut.key) {
    showStatus('请先录制一个快捷键', false);
    return;
  }
  saveSettings({ seconds, shortcut: currentShortcut }).then(() => {
    showStatus('✓ 已保存，B站页面立即生效', true);
  });
});

resetBtn.addEventListener('click', () => {
  const cfg = { seconds: DEFAULTS.seconds, shortcut: { ...DEFAULTS.shortcut } };
  secondsInput.value = cfg.seconds;
  currentShortcut = { ...cfg.shortcut };
  renderShortcut();
  saveSettings(cfg).then(() => {
    showStatus('✓ 已恢复默认设置', true);
  });
});

loadSettings();
