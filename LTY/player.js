/* ============================================================================
 * 洛天依应援页 · 音乐播放器（在 LOU\index.html 里引用）
 * 依赖：index.html 里的 #playerRoot 容器；音乐目录 LOU\music
 * 数据来源（自动二选一）：
 *   1) http 打开  -> 向 /api/tracks 拿歌单（服务器实时扫描文件夹，丢新歌进去刷新就出现）
 *   2) 双击打开   -> 用同目录下的 music-list.js 兜底（跑一次 scan-music.ps1 重新生成）
 * 所有地址都是「相对 LOU 目录」的相对路径，所以整个文件夹搬到哪儿都能照常播放。
 * ========================================================================== */
(function () {
  'use strict';

  var MUSIC_BASE = 'music/';              // 音乐目录（相对 LOU\index.html）
  var API = '/api/tracks';
  var K = {                               // localStorage 键
    idx: 'lty.player.index',
    pos: 'lty.player.pos',
    vol: 'lty.player.volume',
    mode: 'lty.player.mode',
    list: 'lty.player.showList'
  };

  var $ = function (sel, root) { return (root || document).querySelector(sel); };
  var esc = function (s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  };
  var fmtTime = function (s) {
    if (!isFinite(s) || s < 0) s = 0;
    s = Math.floor(s);
    var m = Math.floor(s / 60), ss = s % 60;
    if (m >= 60) return Math.floor(m / 60) + ':' + String(m % 60).padStart(2, '0') + ':' + String(ss).padStart(2, '0');
    return m + ':' + String(ss).padStart(2, '0');
  };
  var fmtSize = function (b) {
    if (!b) return '';
    return b >= 1048576 ? (b / 1048576).toFixed(1) + ' MB' : Math.round(b / 1024) + ' KB';
  };
  var src = function (file) { return MUSIC_BASE + file; };

  /* ---------------------------------------------------------------- 状态 */
  var tracks = [];           // 原始歌单
  var items = [];            // 带 duration / error 的条目
  var current = -1;          // 当前播放下标
  var mode = localStorage.getItem(K.mode) || 'list';  // list | one | shuffle
  var pendingSeek = 0;       // 恢复进度用
  var lastVolume = 1;
  var seeking = false;
  var ready = false;
  var mode_label = { list: '列表循环', one: '单曲循环', shuffle: '随机播放' };

  /* ---------------------------------------------------------------- DOM 骨架 */
  var root = document.getElementById('playerRoot');
  if (!root) return;
  root.innerHTML = [
    '<section class="pl" id="plPanel" hidden>',
    '  <header class="pl-head">',
    '    <span class="pl-title">♫ 洛天依 · 歌单</span>',
    '    <span class="pl-count" id="plCount"></span>',
    '    <input class="pl-search" id="plSearch" type="search" placeholder="搜索歌名 / 歌手" />',
    '    <button class="pl-icon" id="plPick" type="button" title="选择本地音乐文件夹">📂<span>选文件夹</span></button>',
    '    <button class="pl-icon" id="plRefresh" type="button" title="重新扫描歌单">⟳<span>刷新</span></button>',
    '    <button class="pl-icon pl-close" id="plClose" type="button" title="收起歌单">✕</button>',
    '  </header>',
    '  <div class="pl-list" id="plList"></div>',
    '  <p class="pl-tip" id="plTip"></p>',
    '</section>',
    '<footer class="pc" id="pcBar">',
    '  <div class="pc-now">',
    '    <div class="pc-art" id="pcArt" aria-hidden="true">♫</div>',
    '    <div class="pc-meta">',
    '      <div class="pc-name" id="pcName">还没有选中歌曲</div>',
    '      <div class="pc-sub" id="pcSub">点右侧「歌单」挑一首吧</div>',
    '    </div>',
    '  </div>',
    '  <div class="pc-main">',
    '    <div class="pc-btns">',
    '      <button class="pc-b" id="pcPrev" type="button" title="上一首 (,)">⏮</button>',
    '      <button class="pc-b pc-play" id="pcPlay" type="button" title="播放 / 暂停 (空格)">▶</button>',
    '      <button class="pc-b" id="pcNext" type="button" title="下一首 (.)">⏭</button>',
    '      <button class="pc-b pc-mode" id="pcMode" type="button" title="播放模式">列表循环</button>',
    '    </div>',
    '    <div class="pc-seek">',
    '      <span class="pc-t" id="pcCur">0:00</span>',
    '      <div class="pc-track" id="pcTrack"><div class="pc-buf" id="pcBuf"></div><div class="pc-fill" id="pcFill"></div><div class="pc-knob" id="pcKnob"></div></div>',
    '      <span class="pc-t" id="pcDur">0:00</span>',
    '    </div>',
    '  </div>',
    '  <div class="pc-right">',
    '    <button class="pc-b" id="pcMute" type="button" title="静音">🔊</button>',
    '    <div class="pc-vol" id="pcVol"><div class="pc-vfill" id="pcVfill"></div></div>',
    '    <button class="pc-b pc-list" id="pcList" type="button" title="歌单 (L)">歌单</button>',
    '  </div>',
    '  <audio id="pcAudio" preload="metadata"></audio>',
    '</footer>'
  ].join('\n');

  var audio = $('#pcAudio'), bar = $('#pcBar');
  var el = {
    name: $('#pcName'), sub: $('#pcSub'), art: $('#pcArt'),
    play: $('#pcPlay'), prev: $('#pcPrev'), next: $('#pcNext'), mode: $('#pcMode'),
    cur: $('#pcCur'), dur: $('#pcDur'), track: $('#pcTrack'), fill: $('#pcFill'),
    buf: $('#pcBuf'), knob: $('#pcKnob'),
    mute: $('#pcMute'), vol: $('#pcVol'), vfill: $('#pcVfill'),
    list: $('#plList'), panel: $('#plPanel'), count: $('#plCount'),
    search: $('#plSearch'), tip: $('#plTip'), toast: $('#toast')
  };

  /* ---------------------------------------------------------------- 小提示 */
  var toastTimer = null;
  function toast(msg, ms) {
    if (!el.toast) { console.log('[player]', msg); return; }
    el.toast.textContent = msg;
    el.toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.toast.classList.remove('show'); }, ms || 2600);
  }

  /* ---------------------------------------------------------------- 载入歌单 */
  function normalize(t, i) {
    return {
      id: t.id || t.file || ('t' + i),
      file: t.file || t.id,
      title: t.title || (t.name ? t.name.replace(/\.[^.]+$/, '') : ('曲目 ' + (i + 1))),
      artist: t.artist || '',
      no: (t.no === 0 || t.no) ? t.no : null,
      folder: t.folder || '',
      ext: t.ext || '',
      size: t.size || 0,
      weight: t.weight || '',
      objUrl: t.objUrl || null,
      duration: 0,
      error: false
    };
  }

  function loadTracks() {
    var embedded = window.LUOTIANYI_MUSIC;
    var isHttp = location.protocol === 'http:' || location.protocol === 'https:';

    if (!isHttp) {                                  // file:// -> 用内嵌歌单，无需联网
      if (embedded && embedded.tracks && embedded.tracks.length) {
        applyTracks(embedded.tracks, 'file');
      } else {
        // 有些浏览器是异步加载 music-list.js 的，等一下再说，别急着报“没读到歌单”
        setTimeout(function () {
          var late = window.LUOTIANYI_MUSIC;
          if (late && late.tracks && late.tracks.length) applyTracks(late.tracks, 'file');
          else applyTracks([], 'file-empty');
        }, 600);
      }
      return;
    }
    fetch(API + '?t=' + Date.now())
      .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
      .then(function (d) { applyTracks(d.tracks || [], 'http'); })
      .catch(function () {
        if (embedded && embedded.tracks && embedded.tracks.length) applyTracks(embedded.tracks, 'file');
        else applyTracks([], 'file-empty');
        toast('没能连上本地服务器，已改用内嵌歌单');
      });
  }

  function applyTracks(list, source) {
    var savedIdx = Number(localStorage.getItem(K.idx));
    var savedId = null;
    if (savedIdx >= 0 && items[savedIdx]) savedId = items[savedIdx].id;

    tracks = list || [];
    items = tracks.map(normalize);
    ready = true;
    renderList();

    if (!items.length) {
      setNow(null);
      el.sub.textContent = source === 'file-empty'
        ? '没读到歌单：双击 start-player.cmd，或先跑一次 scan-music.ps1'
        : 'LOU\\music 里还没找到音频文件';
      return;
    }

    var idx = 0;
    if (savedId) { for (var i = 0; i < items.length; i++) if (items[i].id === savedId) { idx = i; break; } }
    else if (savedIdx >= 0 && savedIdx < items.length) idx = savedIdx;

    pendingSeek = Number(localStorage.getItem(K.pos)) || 0;
    select(idx, false);

    // 后台把每首的时长补上（只读元数据，不下载整首）
    preloadDurations();
  }

  function preloadDurations() {
    var i = 0;
    (function next() {
      if (i >= items.length) return;
      var it = items[i++];
      if (it.duration || it.error) return setTimeout(next, 0);
      var a = new Audio();
      a.preload = 'metadata';
      a.src = src(it.file);
      var done = false;
      var finish = function (d) {
        if (done) return; done = true;
        a.removeAttribute('src'); a.load();
        if (d) { it.duration = d; renderList(); }
        setTimeout(next, 20);
      };
      a.addEventListener('loadedmetadata', function () { finish(isFinite(a.duration) ? a.duration : 0); });
      a.addEventListener('error', function () { it.error = true; finish(0); });
      setTimeout(function () { finish(0); }, 8000);
    })();
  }

  /* ---------------------------------------------------------------- 选歌 / 播放 */
  function setNow(it) {
    if (!it) {
      el.name.textContent = '还没有选中歌曲';
      el.art.textContent = '♫';
      document.title = '关注洛天依喵，关注洛天依谢谢喵';
      return;
    }
    el.name.textContent = (it.no ? it.no + '. ' : '') + it.title;
    el.name.title = it.file;
    el.art.textContent = it.no ? it.no : '♪';
    el.sub.textContent = [it.artist || '洛天依', it.ext, fmtSize(it.size)].filter(Boolean).join(' · ');
    document.title = '♪ ' + it.title + ' - ' + (it.artist || '洛天依');
  }

  function select(i, autoplay) {
    if (!items.length) return;
    i = (i + items.length) % items.length;
    current = i;
    var it = items[i];
    audio.src = it.objUrl || src(it.file);
    audio.load();
    setNow(it);
    markActive();
    try {
      localStorage.setItem(K.idx, String(i));
      localStorage.setItem(K.pos, '0');
    } catch (e) { /* 隐私模式忽略 */ }
    if (autoplay) play();
  }

  function play() {
    var p = audio.play();
    if (p && p.catch) p.catch(function () { toast('浏览器拦住了自动播放，点一下播放按钮就好'); });
  }

  function step(d) {
    if (!items.length) return;
    if (mode === 'shuffle' && items.length > 1) {
      var n = current;
      while (n === current) n = Math.floor(Math.random() * items.length);
      select(n, true);
    } else {
      select(current + d, true);
    }
  }

  function onEnded() {
    if (mode === 'one') { audio.currentTime = 0; play(); }
    else if (items.length === 1) { audio.currentTime = 0; play(); }
    else step(1);
  }

  /* ---------------------------------------------------------------- 歌单渲染 */
  function markActive() {
    var nodes = el.list.querySelectorAll('.pl-item');
    for (var i = 0; i < nodes.length; i++) {
      nodes[i].classList.toggle('is-active', Number(nodes[i].dataset.i) === current);
      nodes[i].classList.toggle('is-playing', Number(nodes[i].dataset.i) === current && !audio.paused);
    }
    var act = el.list.querySelector('.pl-item.is-active');
    if (act && el.panel.hidden === false) act.scrollIntoView({ block: 'nearest' });
  }

  function renderList() {
    var q = (el.search.value || '').trim().toLowerCase();
    var html = [];
    var shown = 0;
    for (var i = 0; i < items.length; i++) {
      var it = items[i];
      if (q && (it.title + ' ' + it.artist + ' ' + it.file).toLowerCase().indexOf(q) < 0) continue;
      shown++;
      html.push(
        '<button class="pl-item' + (i === current ? ' is-active' : '') + '" data-i="' + i + '" type="button">',
        '<span class="pl-no">' + (it.no !== null ? esc(it.no) : '♪') + '</span>',
        '<span class="pl-info">',
        '  <span class="pl-name">' + esc(it.title) + '</span>',
        '  <span class="pl-artist">' + esc(it.artist || '未知歌手') + (it.folder ? ' · ' + esc(it.folder) : '') + '</span>',
        '</span>',
        '<span class="pl-time' + (it.error ? ' is-bad' : '') + '">' + (it.error ? '无法播放' : (it.duration ? fmtTime(it.duration) : '--:--')) + '</span>',
        '</button>'
      );
    }
    el.list.innerHTML = html.length ? html.join('') : '<p class="pl-empty">没有匹配的歌曲</p>';
    el.count.textContent = items.length ? (shown === items.length ? items.length + ' 首' : shown + ' / ' + items.length + ' 首') : '';
    if (el.tip) {
      el.tip.innerHTML = location.protocol === 'file:'
        ? '当前是「双击打开」模式（歌单来自 music-list.js）。加了新歌？双击 <b>start-player.cmd</b>，或在音乐文件夹里跑一次 <b>scan-music.ps1</b>。'
        : '歌单由本地服务器实时扫描 <b>LOU\\music</b>：丢新歌进文件夹，点「⟳ 刷新」即可。';
    }
  }

  el.list.addEventListener('click', function (e) {
    var btn = e.target.closest ? e.target.closest('.pl-item') : null;
    if (!btn) return;
    select(Number(btn.dataset.i), true);
  });
  el.search.addEventListener('input', function () { renderList(); markActive(); });

  /* ---------------------------------------------------------------- 控件交互 */
  el.play.addEventListener('click', function () {
    if (!items.length) { toast('还没有歌单，先点「歌单 → 刷新」或双击 start-player.cmd'); return; }
    if (audio.paused) { if (current < 0) select(0, true); else play(); } else audio.pause();
  });
  el.prev.addEventListener('click', function () {
    if (current < 0) return select(0, true);
    if (audio.currentTime > 3) { audio.currentTime = 0; return; }   // 播了几秒先回到开头，更像真人播放器
    step(-1);
  });
  el.next.addEventListener('click', function () { if (items.length) step(1); });
  el.mode.addEventListener('click', function () {
    mode = mode === 'list' ? 'one' : mode === 'one' ? 'shuffle' : 'list';
    el.mode.textContent = mode_label[mode];
    el.mode.title = '播放模式：' + mode_label[mode];
    try { localStorage.setItem(K.mode, mode); } catch (e) {}
    toast('播放模式：' + mode_label[mode]);
  });
  el.mode.textContent = mode_label[mode];
  el.mode.title = '播放模式：' + mode_label[mode];

  function toggleList(force) {
    var show = typeof force === 'boolean' ? force : el.panel.hidden;
    el.panel.hidden = !show;
    el.list.classList.toggle('is-open', show);
    try { localStorage.setItem(K.list, show ? '1' : '0'); } catch (e) {}
    if (show) { renderList(); markActive(); }
  }
  $('#pcList').addEventListener('click', function () { toggleList(); });
  $('#plClose').addEventListener('click', function () { toggleList(false); });
  if (localStorage.getItem(K.list) === '1') toggleList(true);

  /* 进度条：点击 + 拖动 */
  function seekFromEvent(e) {
    var r = el.track.getBoundingClientRect();
    var x = (e.touches ? e.touches[0].clientX : e.clientX) - r.left;
    var ratio = Math.max(0, Math.min(1, x / r.width));
    var d = isFinite(audio.duration) ? audio.duration : 0;
    if (!d) return;
    el.fill.style.width = (ratio * 100) + '%';
    el.knob.style.left = (ratio * 100) + '%';
    el.cur.textContent = fmtTime(ratio * d);
    return ratio * d;
  }
  el.track.addEventListener('pointerdown', function (e) {
    if (!isFinite(audio.duration) || !audio.duration) return;
    seeking = true;
    el.track.classList.add('is-drag');
    el.track.setPointerCapture(e.pointerId);
    seekFromEvent(e);
  });
  el.track.addEventListener('pointermove', function (e) { if (seeking) seekFromEvent(e); });
  el.track.addEventListener('pointerup', function (e) {
    if (!seeking) return;
    var t = seekFromEvent(e);
    seeking = false;
    el.track.classList.remove('is-drag');
    if (t != null) { audio.currentTime = t; savePos(); }
  });

  /* 音量 */
  function setVolume(v, silent) {
    v = Math.max(0, Math.min(1, v));
    audio.volume = v;
    audio.muted = v === 0;
    el.vfill.style.width = (v * 100) + '%';
    el.mute.textContent = v === 0 || audio.muted ? '🔇' : v < 0.5 ? '🔉' : '🔊';
    if (v > 0) lastVolume = v;
    if (!silent) { try { localStorage.setItem(K.vol, String(v)); } catch (e) {} }
  }
  el.vol.addEventListener('pointerdown', function (e) {
    var r = el.vol.getBoundingClientRect();
    var move = function (ev) { setVolume((ev.clientX - r.left) / r.width); };
    move(e);
    var up = function () { window.removeEventListener('pointermove', move); window.removeEventListener('pointerup', up); };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up);
  });
  el.mute.addEventListener('click', function () {
    if (audio.volume > 0 && !audio.muted) { lastVolume = audio.volume; setVolume(0); }
    else setVolume(lastVolume || 0.8);
  });
  var initVol = localStorage.getItem(K.vol);
  setVolume(initVol === null ? 0.9 : Number(initVol), true);

  /* ---------------------------------------------------------------- audio 事件 */
  audio.addEventListener('timeupdate', function () {
    if (seeking) return;
    var d = audio.duration;
    if (isFinite(d) && d > 0) {
      var r = audio.currentTime / d;
      el.fill.style.width = (r * 100) + '%';
      el.knob.style.left = (r * 100) + '%';
      el.cur.textContent = fmtTime(audio.currentTime);
    }
    if (!(audio.currentTime % 3)) savePos(true);
  });
  audio.addEventListener('progress', function () {
    if (audio.buffered.length && isFinite(audio.duration) && audio.duration > 0) {
      el.buf.style.width = (audio.buffered.end(audio.buffered.length - 1) / audio.duration * 100) + '%';
    }
  });
  audio.addEventListener('loadedmetadata', function () {
    if (isFinite(audio.duration)) {
      el.dur.textContent = fmtTime(audio.duration);
      if (items[current]) { items[current].duration = audio.duration; renderList(); markActive(); }
    }
    if (pendingSeek > 1) { audio.currentTime = pendingSeek; pendingSeek = 0; }
  });
  audio.addEventListener('play', function () { el.play.textContent = '⏸'; bar.classList.add('is-playing'); markActive(); });
  audio.addEventListener('pause', function () { el.play.textContent = '▶'; bar.classList.remove('is-playing'); markActive(); savePos(); });
  audio.addEventListener('ended', onEnded);
  audio.addEventListener('error', function () {
    if (current < 0 || !items[current]) return;
    var it = items[current];
    it.error = true;
    renderList(); markActive();
    toast('这首播不了（' + it.ext + ' 可能不被浏览器支持），已跳到下一首', 3400);
    setTimeout(function () { if (items.length > 1) step(1); }, 900);
  });

  /* ---------------------------------------------------------------- 进度保存 */
  function savePos(throttle) {
    try {
      if (current >= 0) localStorage.setItem(K.idx, String(current));
      localStorage.setItem(K.pos, String(audio.currentTime || 0));
    } catch (e) {}
  }
  window.addEventListener('beforeunload', savePos);
  document.addEventListener('visibilitychange', function () { if (document.hidden) savePos(); });

  /* ---------------------------------------------------------------- 本地文件夹兜底 */
  var supportsFS = typeof window.showDirectoryPicker === 'function';
  var pickBtn = $('#plPick');
  if (!supportsFS) pickBtn.style.display = 'none';
  else pickBtn.addEventListener('click', function () {
    window.showDirectoryPicker({ id: 'lty-music', mode: 'read' }).then(async function (dir) {
      var found = [];
      var extRe = /\.(mp3|flac|m4a|aac|wav|ogg|oga|opus|wma|ape)$/i;
      // 递归遍历（子文件夹也会被收进来），p 是相对所选目录的路径
      async function walk(dirHandle, p) {
        for await (var entry of dirHandle.values()) {
          if (entry.kind === 'directory') { await walk(entry, p ? p + '/' + entry.name : entry.name); continue; }
          if (entry.kind !== 'file') continue;
          var rel = p ? p + '/' + entry.name : entry.name;
          if (!extRe.test(entry.name)) continue;
          var f = await entry.getFile();
          found.push({
            id: 'local:' + rel, file: rel,
            title: entry.name.replace(/\.[^.]+$/, ''), artist: dir.name,
            ext: (entry.name.split('.').pop() || '').toUpperCase(), size: f.size,
            objUrl: URL.createObjectURL(f)
          });
        }
      }
      await walk(dir, '');
      if (!found.length) { toast('这个文件夹里没有找到音频文件'); return; }
      for (var i = 0; i < items.length; i++) if (items[i].objUrl) URL.revokeObjectURL(items[i].objUrl);
      items = found; tracks = found; current = -1;
      renderList(); select(0, true);
      toast('已载入本地文件夹「' + dir.name + '」共 ' + found.length + ' 首（刷新页面后失效）', 3600);
    }).catch(function () { /* 用户取消 */ });
  });

  var refreshBtn = $('#plRefresh');
  if (location.protocol !== 'http:' && location.protocol !== 'https:') {
    refreshBtn.addEventListener('click', function () {
      toast('双击打开模式下没法自动扫描：请双击 start-player.cmd，或跑一次 scan-music.ps1', 4200);
    });
  } else {
    refreshBtn.addEventListener('click', function () {
      var prevId = items[current] ? items[current].id : null;
      fetch(API + '?t=' + Date.now()).then(function (r) { return r.json(); }).then(function (d) {
        var pos = audio.currentTime, playing = !audio.paused;
        tracks = d.tracks || [];
        items = tracks.map(normalize);
        var idx = prevId ? Math.max(0, items.findIndex(function (x) { return x.id === prevId; })) : 0;
        current = -1;
        renderList();
        if (items.length) { select(idx, playing); pendingSeek = pos; }
        toast('歌单已刷新：' + items.length + ' 首');
      }).catch(function () { toast('刷新失败：服务器好像断开了'); });
    });
  }

  /* ---------------------------------------------------------------- 快捷键 */
  document.addEventListener('keydown', function (e) {
    var tag = (e.target.tagName || '').toLowerCase();
    if (tag === 'input' || tag === 'textarea' || e.target.isContentEditable) return;
    if (e.code === 'Space') { e.preventDefault(); el.play.click(); }
    else if (e.key === 'ArrowRight') { e.preventDefault(); audio.currentTime = Math.min((audio.duration || 0), audio.currentTime + 5); }
    else if (e.key === 'ArrowLeft') { e.preventDefault(); audio.currentTime = Math.max(0, audio.currentTime - 5); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); setVolume(audio.volume + 0.05); }
    else if (e.key === 'ArrowDown') { e.preventDefault(); setVolume(audio.volume - 0.05); }
    else if (e.key === ',') step(-1);
    else if (e.key === '.') step(1);
    else if (e.key === 'l' || e.key === 'L') toggleList();
  });

  /* ---------------------------------------------------------------- 起跑 */
  loadTracks();
  window.LTY_PLAYER = { play: play, select: select, reload: loadTracks };
})();
