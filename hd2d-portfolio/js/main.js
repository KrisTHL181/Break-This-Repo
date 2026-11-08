/* ============================================================
 *  main.js — 页面装配与交互
 *  Renders the RPG UI from SITE config, drives the boot sequence,
 *  scroll-linked camera, dialogue, day/night and the save slots.
 * ============================================================ */
(function () {
  'use strict';

  const $ = (s) => document.querySelector(s);
  const S = window.SITE;
  const el = (tag, cls, txt) => {
    const n = document.createElement(tag);
    if (cls) n.className = cls;
    if (txt != null) n.textContent = txt;
    return n;
  };

  /* ---------------------------------------------------------------
     像素图标：字符串点阵 → 放大到画布
     '.' 透明   '@' 描边   '#' 主色   '+' 次色
     --------------------------------------------------------------- */
  const ICONS = {
    flag: [
      '.....@......', '....@#@.....', '....@#@@@...', '....@#@#@...',
      '....@#@@@...', '....@#@.....', '....@#@.....', '....@#@.....',
      '....@#@.....', '....@#@.....', '...@@#@@....', '...@@@@@....',
    ],
    person: [
      '....@@@@....', '...@####@...', '...@#@@#@...', '...@####@...',
      '....@@@@....', '............', '..@@@@@@@@..', '.@########@.',
      '@##########@', '@####..####@', '@@@@@..@@@@@', '............',
    ],
    spark: [
      '.....@......', '....@#@.....', '....@#@.....', '.@..@#@..@..',
      '@#@.@#@.@#@.', '.@#@###@#@..', '..@#####@...', '...@###@....',
      '..@#@.@#@...', '.@#@...@#@..', '.@......@...', '............',
    ],
    scroll: [
      '.@@@@@@@@@..', '.@#######@..', '.@#@@@@@#@..', '.@#######@..',
      '.@#@@@@@#@..', '.@#######@..', '.@#@@@@@#@..', '.@#######@..',
      '.@@@@@@@@@..', '...@...@....', '..@@...@@...', '............',
    ],
    map: [
      '..@@........', '.@##@.......', '.@##@.......', '..@@........',
      '............', '.....@@.....', '....@##@....', '....@##@....',
      '.....@@.....', '........@@..', '.......@##@.', '........@@..',
    ],
    crystal: [
      '.....@......', '....@#@.....', '....@#@.....', '...@#@#@....',
      '...@#@#@....', '..@#@#@#@...', '..@#@#@#@...', '..@#@#@#@...',
      '...@#@#@....', '...@#@#@....', '....@#@.....', '.....@......',
    ],
    moon: [
      '...@@@@.....', '..@####@@...', '.@####@##@..', '.@###@###@..',
      '@####@####@.', '@####@####@.', '@####@####@.', '@####@####@.',
      '.@###@###@..', '.@####@##@..', '..@####@@...', '...@@@@.....',
    ],
    sun: [
      '.....@......', '..@.@#@.@...', '....@#@.....', '..@@@@@@@...',
      '.@@#####@@..', '@@@#####@@@.', '@@@#####@@@.', '.@@#####@@..',
      '..@@@@@@@...', '....@#@.....', '..@.@#@.@...', '.....@......',
    ],
    speaker: [
      '...@@@@@.@..', '...@###@.@..', '..@@###@@@@.', '.@@@###@@@.@',
      '@@@#####@@.@', '@@@#####@@.@', '.@@@###@@@.@', '..@@###@@@@.',
      '...@###@.@..', '...@@@@@.@..', '............', '............',
    ],
    disk: [
      '.@@@@@@@@@@.', '.@########@.', '.@#@@@@@@#@.', '.@########@.',
      '.@#@@@@@@#@.', '.@########@.', '.@#@@@@@@#@.', '.@########@.',
      '.@@@@@@@@@@.', '..@......@..', '..@......@..', '............',
    ],
    up: [
      '.....@@.....', '....@##@....', '...@####@...', '..@######@..',
      '.@########@.', '@##########@', '....@##@....', '....@##@....',
      '....@##@....', '....@##@....', '....@@@@....', '............',
    ],
    px: [
      '.........@@.', '........@##@', '.......@##@.', '......@##@..',
      '.....@##@...', '....@##@....', '...@##@.....', '..@##@......',
      '.@##@.......', '@@#@........', '@@@.........', '@...........',
    ],
    fx: [
      '..@.@@@@....', '.@.@@####@@.', '@.@@##..##@@', '..@##....##@',
      '.@@##......@', '@@#........@', '@@#........@', '.@@##......@',
      '..@##....##@', '@.@@##..##@@', '.@.@@####@@.', '..@.@@@@....',
    ],
    ray: [
      '.....@......', '....@#@.....', '...@###@....', '..@#####@...',
      '.@#######@..', '@#########@.', '.....@......', '...@@.@@....',
      '..@#@.@#@...', '.@#@...@#@..', '@#@.....@#@.', '@@.......@@.',
    ],
    cmp: [
      '..@@@@@@@...', '..@#####@...', '..@#####@...', '..@@@@@@@...',
      '.@@@@@@@....', '.@#####@....', '.@#####@....', '.@@@@@@@....',
      '..@@@@@@@...', '..@#####@...', '..@#####@...', '..@@@@@@@...',
    ],
    spd: [
      '.....@@@....', '....@##@....', '...@##@.....', '..@##@......',
      '.@####@@@...', '@######@....', '....@##@....', '...@##@.....',
      '..@##@......', '.@##@.......', '.@#@........', '.@@.........',
    ],
    a11y: [
      '.@@@@@@@@@..', '@#########@.', '@#######@#@.', '@#####@##@..',
      '@####@##@...', '@####@##@...', '@#####@##@..', '@#######@#@.',
      '.@#######@..', '..@#####@...', '...@###@....', '....@#@.....',
    ],
    ts: [
      '....@@@@....', '..@@####@@..', '.@########@.', '@###@@@@###@',
      '@##@....@##@', '@##@.@@.@##@', '@##@.@@.@##@', '@##@....@##@',
      '@###@@@@###@', '.@########@.', '..@@####@@..', '....@@@@....',
    ],
    art: [
      '............', '...@@@@@@...', '..@######@..', '.@##@@@@##@.',
      '@##@####@##@', '@#@##@@##@#@', '@#@##@@##@#@', '@##@####@##@',
      '.@##@@@@##@.', '..@######@..', '...@@@@@@...', '............',
    ],
    gh: [
      '..@@@@@@@...', '.@#######@..', '@#########@.', '@@#######@@.',
      '@#@#####@#@.', '@#########@.', '@#########@.', '@##@###@##@.',
      '@#########@.', '.@#######@..', '..@#@.@#@...', '...@@..@@...',
    ],
    jj: [
      '....@@@.....', '..@@###@@...', '.@#######@..', '@#########@.',
      '@#########@.', '.@#######@..', '..@#####@...', '...@###@....',
      '....@#@.....', '.....@......', '............', '............',
    ],
    mail: [
      '@@@@@@@@@@@@', '@##########@', '@#@@@@@@@@#@', '@##@@@@@@##@',
      '@#@#@@@@#@#@', '@#@@#@@#@@#@', '@#@@@##@@@#@', '@#@@@@@@@@#@',
      '@##########@', '@@@@@@@@@@@@', '............', '............',
    ],
    rss: [
      '@@@.........', '@#@.........', '@#@.........', '@#@..@@@@...',
      '@#@.@####@..', '@#@@##@@##@.', '@@@.#@..@#@.', '....@#@.@#@.',
      '.....@####@.', '......@@@@..', '..@@........', '.@##@.......',
    ],
  };

  /* 调色：主色 / 次色 / 描边 */
  const ICON_COLORS = {
    gold: ['#f2c877', '#9b7434', '#1a1206'],
    cyan: ['#74f0e2', '#2f8f96', '#04181a'],
    warm: ['#ffcf7a', '#a8672a', '#1a0d04'],
    green: ['#8fdc8f', '#2f6a3a', '#07160a'],
    violet: ['#b9a6ff', '#54428f', '#0d0819'],
    rose: ['#ff9aa6', '#8f2f3f', '#1a0508'],
  };

  function paintIcon(cv, name, tone, scale) {
    const rows = ICONS[name];
    if (!rows) return;
    const w = rows.reduce((m, r) => Math.max(m, r.length), 0);
    const cols = ICON_COLORS[tone] || ICON_COLORS.gold;
    const k = scale || Math.max(1, Math.round((cv.width || 12) / w));
    cv.width = w * k; cv.height = rows.length * k;
    const cx = cv.getContext('2d');
    cx.imageSmoothingEnabled = false;
    cx.clearRect(0, 0, cv.width, cv.height);
    for (let j = 0; j < rows.length; j++) {
      for (let i = 0; i < rows[j].length; i++) {
        const ch = rows[j][i];
        if (ch === '.') continue;
        cx.fillStyle = ch === '@' ? cols[2] : (ch === '+' ? cols[1] : cols[0]);
        cx.fillRect(i * k, j * k, k, k);
      }
    }
    cv.style.width = cv.width + 'px';
    cv.style.height = cv.height + 'px';
  }

  /* ---------------------------------------------------------------
     胶片颗粒贴图（真实像素噪点）
     --------------------------------------------------------------- */
  function buildGrain() {
    const c = document.createElement('canvas');
    c.width = c.height = 120;
    const x = c.getContext('2d');
    const img = x.createImageData(120, 120);
    const d = img.data;
    for (let i = 0; i < 120 * 120; i++) {
      const v = Math.random();
      let g = 128, a = 0;
      if (v > 0.90) { g = 255; a = 70; }
      else if (v < 0.10) { g = 0; a = 70; }
      d[i * 4] = d[i * 4 + 1] = d[i * 4 + 2] = g;
      d[i * 4 + 3] = a;
    }
    x.putImageData(img, 0, 0);
    document.documentElement.style.setProperty('--grain', 'url(' + c.toDataURL() + ')');
  }

  /* ---------------------------------------------------------------
     内容渲染
     --------------------------------------------------------------- */
  const DOCK = [
    { id: 'hero', icon: 'flag', tone: 'gold', label: '序章' },
    { id: 'status', icon: 'person', tone: 'cyan', label: '属性' },
    { id: 'skills', icon: 'spark', tone: 'violet', label: '技能' },
    { id: 'quests', icon: 'scroll', tone: 'warm', label: '任务' },
    { id: 'journey', icon: 'map', tone: 'green', label: '日志' },
    { id: 'save', icon: 'crystal', tone: 'cyan', label: '存档' },
  ];

  function renderHud() {
    const p = S.player;
    $('#hudName').textContent = p.name;
    $('#hudHandle').textContent = p.handle;
    $('#hudLevel').textContent = p.level;

    const box = $('#hudVitals');
    box.innerHTML = '';
    S.vitals.forEach((v) => {
      const d = el('div', 'vital vital--' + v.color);
      const l = el('span', 'vital__label', v.label.split(' ')[0] + ' ' + v.label.split(' ')[1]);
      const t = el('span', 'vital__track');
      const f = el('i', 'vital__fill');
      f.dataset.value = v.value;
      t.appendChild(f);
      d.append(l, t);
      box.appendChild(d);
    });

    paintIcon($('#icoNight'), 'moon', 'gold', 1);
    paintIcon($('#icoAudio'), 'speaker', 'cyan', 1);
    paintIcon($('#icoSave'), 'disk', 'gold', 1);
    paintIcon($('#icoTop'), 'up', 'cyan', 1);

    const hero = $('#hudPortrait');
    hero.width = hero.height = 36;
    window.HD2D.drawHero(hero.getContext('2d'), 36);
  }

  function renderHero() {
    const p = S.player;
    $('#heroEyebrow').textContent = S.chapters.hero;
    $('#heroName').textContent = p.name;
    $('#heroRole').textContent = p.jobEn;
    $('#heroTagline').textContent = p.tagline;

    const meta = $('#heroMeta');
    meta.innerHTML = '';
    const chips = [
      ['职业', p.job],
      ['等级', 'Lv. ' + p.level],
      ['坐标', p.location],
      ['游玩时长', p.playtime],
    ];
    chips.forEach((c) => {
      const d = el('span', 'chip');
      d.append(el('b', null, c[0]), el('span', null, c[1]));
      meta.appendChild(d);
    });
    const st = el('span', 'chip');
    st.append(el('i', 'chip__dot'), el('span', null, p.status));
    meta.appendChild(st);
  }

  function renderDock() {
    const nav = $('#dock');
    nav.innerHTML = '';
    DOCK.forEach((d) => {
      const b = el('button', 'dock__item');
      b.type = 'button';
      b.dataset.target = d.id;
      const cv = document.createElement('canvas');
      cv.className = 'dock__icon';
      paintIcon(cv, d.icon, d.tone, 2);
      b.appendChild(cv);
      b.appendChild(el('span', null, d.label));
      b.addEventListener('click', () => {
        const t = document.getElementById(d.id);
        if (t) t.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
      nav.appendChild(b);
    });
  }

  function renderStatus() {
    const p = S.player;
    $('#sheetName').textContent = p.name;
    $('#sheetJob').textContent = p.job;

    const port = $('#sheetPortrait');
    port.width = port.height = 132;
    window.HD2D.drawHero(port.getContext('2d'), 132);

    const f = $('#sheetFields');
    f.innerHTML = '';
    [
      ['姓 名', p.name + '  ' + p.handle],
      ['职 业', p.job],
      ['所 在 地', p.location],
      ['游玩时长', p.playtime],
      ['当前状态', p.status],
    ].forEach((row) => {
      const d = el('div', 'field');
      d.append(el('span', 'field__k', row[0]), el('span', 'field__v', row[1]));
      f.appendChild(d);
    });

    const st = $('#sheetStats');
    st.innerHTML = '';
    S.stats.forEach((s) => {
      const d = el('div', 'stat');
      d.append(el('span', 'stat__name', s.name));
      const t = el('span', 'stat__track');
      const fill = el('i', 'stat__fill');
      fill.dataset.value = s.value;
      t.appendChild(fill);
      d.append(t, el('span', 'stat__val', s.value));
      st.appendChild(d);
    });

    const eq = $('#sheetEquip');
    eq.innerHTML = '';
    S.equipment.forEach((e) => {
      const d = el('div', 'equip__row');
      d.append(el('span', 'equip__slot', e.slot), el('span', 'equip__name', e.name));
      d.append(el('span', 'equip__note', e.note));
      eq.appendChild(d);
    });
  }

  function renderSkills() {
    const grid = $('#skillGrid');
    grid.innerHTML = '';
    const tones = ['gold', 'cyan', 'violet', 'rose', 'warm', 'green', 'cyan', 'gold'];
    S.skills.forEach((s, i) => {
      const d = el('div', 'skill reveal');
      d.dataset.d = String((i % 4) + 1);
      const cv = document.createElement('canvas');
      cv.className = 'skill__icon';
      paintIcon(cv, s.icon, tones[i % tones.length], 3);
      d.appendChild(cv);
      d.appendChild(el('div', 'skill__name', s.name));
      const pips = el('div', 'skill__pips');
      for (let k = 0; k < 5; k++) {
        const p = el('i', 'pip' + (k < s.lv ? ' is-on' : ''));
        pips.appendChild(p);
      }
      d.appendChild(pips);
      d.appendChild(el('p', 'skill__desc', s.desc));
      d.title = '熟练度 ' + s.lv + ' / 5';
      grid.appendChild(d);
    });
  }

  function renderQuests() {
    const list = $('#questList');
    list.innerHTML = '';
    const statusText = { done: '已完成', active: '进行中' };
    S.quests.forEach((q, i) => {
      const d = el('article', 'quest quest--' + q.status + ' reveal');
      d.dataset.d = String((i % 5) + 1);

      const head = el('div', 'quest__head');
      head.appendChild(el('span', 'quest__title', '「' + q.title + '」'));
      const stars = el('span', 'quest__stars');
      for (let k = 0; k < 5; k++) {
        stars.appendChild(k < q.stars ? document.createTextNode('★') : el('i', null, '★'));
      }
      head.appendChild(stars);
      head.appendChild(el('span', 'quest__type', q.type));
      d.appendChild(head);
      d.appendChild(el('span', 'quest__status', statusText[q.status] || q.status));
      d.appendChild(el('p', 'quest__desc', q.desc));

      const foot = el('div', 'quest__foot');
      q.rewards.forEach((r) => foot.appendChild(el('span', 'tag', r)));
      foot.appendChild(el('span', 'quest__metric', q.metric));
      d.appendChild(foot);
      list.appendChild(d);
    });
  }

  function renderJourney() {
    const box = $('#logList');
    box.innerHTML = '';
    S.journey.forEach((j, i) => {
      const d = el('div', 'log__item reveal');
      d.dataset.d = String(Math.min(i + 1, 6));
      d.appendChild(el('i', 'log__dot'));
      d.appendChild(el('div', 'log__year', j.year + ' 年'));
      d.appendChild(el('h3', 'log__title', j.title));
      d.appendChild(el('div', 'log__org', j.org));
      d.appendChild(el('p', 'log__desc', j.desc));
      box.appendChild(d);
    });
  }

  function renderSaves() {
    const box = $('#saveList');
    box.innerHTML = '';
    S.contacts.forEach((c, i) => {
      const b = el('button', 'save reveal');
      b.type = 'button';
      b.dataset.d = String(i + 1);
      b.appendChild(el('span', 'save__slot', '存档位 ' + String(i + 1).padStart(2, '0')));
      const lab = el('span', 'save__label');
      const cv = document.createElement('canvas');
      cv.className = 'save__icon';
      paintIcon(cv, c.icon, 'cyan', 2);
      lab.appendChild(cv);
      lab.appendChild(el('span', null, c.label));
      b.appendChild(lab);
      b.appendChild(el('span', 'save__value', c.value));
      b.addEventListener('click', () => {
        if (c.href && c.href !== '#') {
          window.open(c.href, '_blank', 'noopener');
          toast('已打开 ' + c.label + ' 存档位');
        } else {
          copy(c.value);
        }
      });
      box.appendChild(b);
    });
  }

  /* ---------------------------------------------------------------
     工具
     --------------------------------------------------------------- */
  let toastTimer = 0;
  function toast(msg) {
    const t = $('#toast');
    t.textContent = msg;
    t.classList.add('is-on');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => t.classList.remove('is-on'), 2000);
  }

  function copy(text) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(
        () => toast('已复制：' + text),
        () => toast(text)
      );
    } else {
      toast(text);
    }
  }

  /* ---------------------------------------------------------------
     对话框打字机
     --------------------------------------------------------------- */
  const dlg = {
    box: null, text: null, queue: [], line: '', i: 0, timer: 0, typing: false, open: false,
    init() {
      this.box = $('#dialogue');
      this.text = $('#dlgText');
    },
    play(lines, who) {
      $('#dlgWho').textContent = who || '旅人';
      this.queue = lines.slice();
      this.box.classList.remove('is-hidden');
      this.open = true;
      this.next();
    },
    next() {
      clearInterval(this.timer);
      if (!this.queue.length) { this.hide(); return; }
      const full = this.queue.shift();
      this.line = full; this.i = 0; this.typing = true;
      this.text.textContent = '';
      this.timer = setInterval(() => {
        this.i++;
        this.text.textContent = this.line.slice(0, this.i);
        if (this.i >= this.line.length) {
          clearInterval(this.timer);
          this.typing = false;
        }
      }, 46);
    },
    skip() {
      if (this.typing) {
        clearInterval(this.timer);
        this.text.textContent = this.line;
        this.typing = false;
      } else {
        this.next();
      }
    },
    hide() {
      this.box.classList.add('is-hidden');
      this.open = false;
    },
  };

  /* ---------------------------------------------------------------
     启动
     --------------------------------------------------------------- */
  function boot() {
    buildGrain();
    renderHud();
    renderHero();
    renderDock();
    renderStatus();
    renderSkills();
    renderQuests();
    renderJourney();
    renderSaves();

    const crest = $('#bootCrest');
    crest.width = crest.height = 72;
    window.HD2D.drawCrest(crest.getContext('2d'), 72);

    /* 场景 */
    window.HD2D.init($('#scene'), {
      onCharClick: () => {
        const lines = [
          '（他抬起头，兜帽下的眼睛亮了一下。）',
          '「你也写前端？那我们是同行了。」',
          '「这个山坡是我自己搭的——每一棵树都是画出来的，不是贴图。」',
          '「往下走走吧，前面有我的任务板和日志。」',
          '（他把手里的灯举高了一些。）',
        ];
        dlg.play([lines[Math.floor(Math.random() * lines.length)]], '陆离');
      },
    });

    /* canvas 版景深可用时让 CSS 兜底层退场（必须在 init 之后判断） */
    if (window.HD2D.hasCanvasDof && window.HD2D.hasCanvasDof()) {
      document.documentElement.classList.add('canvas-dof');
    }

    const bar = $('#bootBar');
    const hint = $('#bootHint');
    let pct = 0;
    const hints = ['NOW LOADING…', '绘制地景…', '点亮灯火…', '校准景深…'];
    const tick = setInterval(() => {
      pct = Math.min(100, pct + 8 + Math.random() * 16);
      bar.style.width = pct + '%';
      hint.textContent = hints[Math.min(hints.length - 1, Math.floor(pct / 26))];
      if (pct >= 100) {
        clearInterval(tick);
        finish();
      }
    }, 130);

    function finish() {
      const go = () => {
        document.body.classList.remove('is-booting');
        $('#boot').classList.add('is-done');
        setTimeout(() => { $('#boot').style.display = 'none'; }, 700);
        bumpVitals();
        setTimeout(() => dlg.play(S.intro, '旁白'), 900);
      };
      /* 等字体就绪，否则点阵字会闪烁重排 */
      if (document.fonts && document.fonts.ready) {
        Promise.race([
          document.fonts.ready,
          new Promise((r) => setTimeout(r, 1500)),
        ]).then(go);
      } else {
        go();
      }
    }

    wireScroll();
    wireToggles();
    wireReveal();
    wireDialogue();
  }

  /* 数值条：进入视口时再涨满 */
  function bumpVitals() {
    document.querySelectorAll('.vital__fill').forEach((f, i) => {
      setTimeout(() => { f.style.width = f.dataset.value + '%'; }, 260 + i * 130);
    });
  }

  function wireScroll() {
    let raf = 0;
    const onScroll = () => {
      if (raf) return;
      raf = requestAnimationFrame(() => {
        raf = 0;
        const max = Math.max(1, document.documentElement.scrollHeight - window.innerHeight);
        const p = Math.min(1, Math.max(0, window.scrollY / max));
        window.HD2D.setProgress(p);

        /* 首屏的对话框滚过去就收起来 */
        if (dlg.open && p > 0.06) dlg.hide();

        $('#hero').style.setProperty('--p', p.toFixed(3));
      });
    };
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }

  let activeChapter = '';
  function wireReveal() {
    const io = new IntersectionObserver((entries) => {
      entries.forEach((e) => {
        if (!e.isIntersecting) return;
        e.target.classList.add('is-in');

        /* 章节高亮 */
        const sec = e.target.closest('.sec');
        if (sec && sec.dataset.chapter && sec.dataset.chapter !== activeChapter) {
          activeChapter = sec.dataset.chapter;
          document.querySelectorAll('.dock__item').forEach((b) => {
            b.classList.toggle('is-active', b.dataset.target === activeChapter);
          });
        }
        /* 数值条 */
        e.target.querySelectorAll('.stat__fill').forEach((f, i) => {
          setTimeout(() => { f.style.width = f.dataset.value + '%'; }, 160 + i * 90);
        });
      });
    }, { rootMargin: '-12% 0px -18% 0px', threshold: 0.06 });

    document.querySelectorAll('.reveal').forEach((n) => io.observe(n));
    document.querySelectorAll('.sec').forEach((n) => io.observe(n));
  }

  function wireDialogue() {
    dlg.init();
    const advance = () => { if (dlg.open) dlg.skip(); };
    $('#dialogue').addEventListener('click', advance);
    window.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        if (dlg.open && document.activeElement === document.body) {
          e.preventDefault();
          dlg.skip();
        }
      }
    });
  }

  function wireToggles() {
    const bn = $('#btnNight');
    /* ?theme=day|night forces it; otherwise follow the visitor's clock */
    const forced = new URLSearchParams(location.search).get('theme');
    let night;
    if (forced === 'day' || forced === 'night') {
      night = forced === 'night';
    } else {
      const h = new Date().getHours();
      night = h >= 19 || h < 6;
    }
    const applyNight = () => {
      document.body.classList.toggle('night', night);
      bn.setAttribute('aria-pressed', String(night));
      $('#nightLabel').textContent = night ? '天明' : '入夜';
      paintIcon($('#icoNight'), night ? 'sun' : 'moon', 'gold', 1);
      window.HD2D.setNight(night);
    };
    applyNight();
    bn.addEventListener('click', () => { night = !night; applyNight(); toast(night ? '夜幕降临' : '天亮了'); });

    const ba = $('#btnAudio');
    let audio = false;
    ba.addEventListener('click', () => {
      audio = !audio;
      if (audio) {
        const ok = window.AMBIENT.start();
        if (!ok) { audio = false; toast('浏览器拒绝了音频'); return; }
        toast('环境音已开启');
      } else {
        window.AMBIENT.stop();
        toast('环境音已关闭');
      }
      ba.setAttribute('aria-pressed', String(audio));
      $('#audioLabel').textContent = audio ? '静音' : '环境音';
      paintIcon($('#icoAudio'), audio ? 'speaker' : 'speaker', audio ? 'green' : 'cyan', 1);
    });

    $('#btnSave').addEventListener('click', () => {
      const stamp = new Date().toLocaleString('zh-CN', { hour12: false });
      try { localStorage.setItem('hd2d-save', stamp); } catch (e) { /* 隐私模式 */ }
      toast('进度已保存 · ' + stamp);
    });
    $('#btnTop').addEventListener('click', () => {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
