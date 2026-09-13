/* ============================================================
 *  SITE CONFIG — 这里改文案，不用动其它文件
 *  Edit everything about yourself in this one object.
 * ============================================================ */
window.SITE = {
  /* ---------- 玩家档案 ---------- */
  player: {
    name: '陆离',
    handle: '@luli.dev',
    job: '前端炼金术士',
    jobEn: 'FRONTEND ALCHEMIST',
    level: 99,
    exp: 87,                       // 经验条百分比
    location: '星见之丘 · 第 7 层',
    playtime: '3,842 小时',
    tagline: '把手写的每一行代码，锻造成会呼吸的界面。',
    status: '在线',                // 在线 / 挂机 / 离线
  },

  /* ---------- 状态数值 ---------- */
  vitals: [
    { label: '生命 HP', value: 96, color: 'hp' },
    { label: '魔力 MP', value: 88, color: 'mp' },
    { label: '体力 SP', value: 74, color: 'sp' },
  ],

  /* ---------- 属性面板 ---------- */
  stats: [
    { name: 'HTML / 语义', value: 98 },
    { name: 'CSS / 布局', value: 97 },
    { name: 'JavaScript', value: 95 },
    { name: 'TypeScript', value: 92 },
    { name: 'React / Vue', value: 90 },
    { name: '动效 / WebGL', value: 86 },
    { name: '工程化 / 构建', value: 88 },
    { name: '无障碍 A11y', value: 81 },
  ],

  /* ---------- 装备栏 ---------- */
  equipment: [
    { slot: '主手', name: 'VS Code', note: '附魔：Vim 键位' },
    { slot: '副手', name: 'Chrome DevTools', note: '附魔：Performance 面板' },
    { slot: '护甲', name: 'TypeScript', note: '套装：严格模式' },
    { slot: '饰品', name: 'Figma', note: '附魔：Auto Layout' },
    { slot: '坐骑', name: 'Vite', note: '速度 +400%' },
    { slot: '背包', name: 'Git', note: '容量：无限分支' },
  ],

  /* ---------- 技能树 ---------- */
  skills: [
    { icon: 'px',  name: '像素炼成',   lv: 5, desc: '用纯 CSS / Canvas 绘制像素美术，不用一张图片。' },
    { icon: 'fx',  name: '动画咏唱',   lv: 5, desc: 'GSAP、Web Animations、滚动叙事与缓动曲线调校。' },
    { icon: 'ray', name: '光影魔法',   lv: 4, desc: 'WebGL / 自定义 Shader，做泛光、景深与体积光。' },
    { icon: 'cmp', name: '组件召唤',   lv: 5, desc: 'React / Vue 组件设计，可复用、可组合、可测试。' },
    { icon: 'spd', name: '性能疾走',   lv: 4, desc: '首屏预算、渲染帧率、包体瘦身与内存排查。' },
    { icon: 'a11y',name: '无障碍结界', lv: 4, desc: '键盘可达、语义化、色彩对比与读屏体验。' },
    { icon: 'ts',  name: '类型结界',   lv: 5, desc: '用类型系统把 bug 挡在编译期之外。' },
    { icon: 'art', name: '设计之眼',   lv: 4, desc: '栅格、字距、层级与留白——像素级还原设计稿。' },
  ],

  /* ---------- 任务板（项目） ---------- */
  quests: [
    {
      title: '星海观测站',
      stars: 5,
      status: 'done',
      type: '主线任务',
      desc: '面向千万级数据点的实时可视化平台。自研 WebGL 图层调度，把 12 万节点压进 16ms 的帧预算里。',
      rewards: ['React', 'WebGL', 'D3', 'Web Worker'],
      metric: '12 万节点 · 稳定 60 FPS',
    },
    {
      title: '像素工坊',
      stars: 4,
      status: 'done',
      type: '支线任务',
      desc: '浏览器里的像素画编辑器。调色板、洋葱皮、精灵表导出，Rust + WASM 做图像运算。',
      rewards: ['Canvas', 'WASM', 'Rust'],
      metric: '首屏 38KB · 零依赖',
    },
    {
      title: '卤素设计系统',
      stars: 4,
      status: 'active',
      type: '团队任务',
      desc: '跨 6 条业务线的组件库。42 个组件、完整的 Token 体系与暗色模式，Storybook 自动化视觉回归。',
      rewards: ['TypeScript', 'Storybook', 'Rollup'],
      metric: '42 组件 · 6 条业务线',
    },
    {
      title: '回声书签',
      stars: 3,
      status: 'active',
      type: '支线任务',
      desc: '稍后读 + AI 摘要。跑在 Edge Runtime 上，流式返回，冷启动 40ms。',
      rewards: ['Next.js', 'Edge', 'LLM'],
      metric: '冷启动 40ms',
    },
    {
      title: '桌面时钟',
      stars: 2,
      status: 'done',
      type: '休闲任务',
      desc: '一个极简的 Electron 桌面时钟，给自己写的，纯粹因为好看。',
      rewards: ['Electron', 'CSS'],
      metric: '安装包 6MB',
    },
  ],

  /* ---------- 冒险日志（经历） ---------- */
  journey: [
    { year: '2025', title: '独立开发者', org: '星见之丘工作室', desc: '离开大厂，做自己的产品。第一次靠作品而不是简历拿到用户。' },
    { year: '2023', title: '高级前端工程师', org: '某互联网大厂', desc: '负责可视化中台，带 4 人小组。开始认真啃图形学与渲染管线。' },
    { year: '2021', title: '前端工程师', org: '一家做 SaaS 的创业公司', desc: '从 0 到 1 搭起整套前端体系，第一次体会到「架构」两个字的重量。' },
    { year: '2019', title: '初出茅庐', org: '自学 + 外包', desc: '在出租屋里啃 MDN，接一些小网站。第一笔收入 800 块。' },
  ],

  /* ---------- 存档点（联系方式） ---------- */
  contacts: [
    { label: 'GitHub',  value: 'github.com/luli-dev',   href: 'https://github.com/', icon: 'gh' },
    { label: '掘金',    value: 'juejin.cn/user/luli',   href: 'https://juejin.cn/',  icon: 'jj' },
    { label: '邮箱',    value: 'hi@luli.dev',           href: 'mailto:hi@luli.dev',  icon: 'mail' },
    { label: 'RSS',     value: '/feed.xml',             href: '#',                   icon: 'rss' },
  ],

  /* ---------- 开场白 ---------- */
  intro: [
    '欢迎来到星见之丘。',
    '这里住着一个前端开发者，他相信界面是可以有重量的。',
    '往下滚动，带你看一看他的世界。',
  ],

  /* ---------- 章节旁白（每屏一句） ---------- */
  chapters: {
    hero:    '第一章 · 出发的清晨',
    status:  '第二章 · 冒险者手册',
    skills:  '第三章 · 已习得的魔法',
    quests:  '第四章 · 悬赏任务板',
    journey: '第五章 · 走过的路',
    save:    '终章 · 在存档点等你',
  },
};
