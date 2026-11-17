// 界面文案：照抄 HDSL src/main/resources/assets/lang/I18N_zh_Hans.properties。
// 每个常量后用注释标出来源 key，便于与桌面版对照。

export const I18N = {
  // 窗口标题（Metadata.FULL_NAME / FULL_TITLE）
  appFullName: "Hello DeepSeek! Launcher",

  // ---- 主导航（MainPage.java） ----
  "dsh.account.list": "账户",
  "dsh.logout": "退出登录",
  "instance": "实例",
  "settings.launcher.general": "通用",
  "dsh.instance.manage": "实例管理",
  "dsh.instance.list": "实例列表",
  "download": "下载",
  "settings": "设置",
  "dsh.launch.no_instance": "新建实例",
  "dsh.launch.no_instance.hint": "还没有实例",
  "dsh.account.none": "还没有账号", // dsh.account.none
  "dsh.account.none.hint": "添加一个账号，启动时它会把这个供应商交给实例",

  // ---- 启动面板（MainPage.java / DshLaunchService） ----
  "dsh.launch": "启动实例",
  "dsh.launch.running": "打开",
  "dsh.stop": "停止",
  "dsh.stopping": "停止中…",
  "dsh.launch.select": "选择实例",
  "dsh.launch.launching": "启动中…",
  "dsh.launch.failed": "启动失败",
  "dsh.launch.ready": "%s 已就绪",
  "instance.launch.empty.installing": "安装实例",

  // ---- 实例列表页（InstancesPage.java） ----
  "dsh.directory.add": "添加实例文件夹",
  "dsh.instance.install": "安装新实例",
  "dsh.instance.menu": "更多",
  "dsh.instance.none": "还没有实例",
  "install.modpack": "安装整合包",
  "dsh.settings.global": "全局设置",
  "button.refresh": "刷新",
  "search.no_results_found": "搜索无结果",
  "dsh.instance.empty": "还没有实例",
  "dsh.instance.empty.hint": "点「安装新实例」先选品牌：dsh、Kimi Code、OpenCode、Claude Web、Octop、Hermes、PI WEB。",
  "dsh.instance.select": "选择",
  "dsh.instance.open_browser": "在浏览器中打开",
  "dsh.instance.open_home": "打开实例目录",
  "dsh.instance.remove": "删除实例",
  "dsh.instance.remove.confirm": "确定删除实例「%s」及其独立的 DSH_HOME 吗？",
  "dsh.instance.removed": "已删除 %s",
  "dsh.instance.duplicating": "正在复制实例 %s",
  "dsh.instance.duplicated": "已复制实例",
  "instance.manage.rename": "重命名",
  "instance.manage.duplicate": "复制实例",
  "modpack.export": "导出整合包",
  "dsh.instance.count": "共 %d 个实例",

  // ---- 实例管理页组（InstancePage.java） ----
  "instance.manage.manage.title": "实例管理 - %s",
  "settings.tabs.installers": "自动安装",
  "dsh.instance.plugins": "插件",
  "dsh.instance.skills": "技能包",
  "dsh.instance.sessions": "会话",
  "dsh.instance.details": "详情",
  "settings.game.exploration": "浏览",
  "settings.game.management": "管理",
  "dsh.instance.browse": "浏览",
  "dsh.instance.test_launch": "测试启动",
  "dsh.instance.port": "端口",
  "dsh.instance.port.mode": "端口选择",
  "dsh.instance.port.mode.auto": "自动分配",
  "dsh.instance.port.mode.fixed": "指定端口",
  "dsh.instance.port.fixed": "端口号",
  "dsh.instance.port.invalid": "端口无效",
  "dsh.instance.port.auto.none": "尚未分配",
  "dsh.instance.name": "实例名称",
  "dsh.instance.name.empty": "名称不能为空",
  "dsh.instance.created": "已创建 %s",
  "dsh.instance.created.installing": "已创建 %s，正在自动安装 dsh…",
  "dsh.instance.icon": "图标",
  "dsh.instance.folders": "目录",
  "dsh.instance.running": "运行中",
  "dsh.instance.running.since": "已运行 %d 秒",
  "dsh.instance.summary": "dsh %s · 配置 %s · %s",
  "dsh.instance.open.sessions": "会话目录",
  "dsh.instance.open.profiles": "配置目录",
  "dsh.instance.open.storages": "存储目录",
  "dsh.instance.open_home.dsh": "DSH_HOME 目录",
  "dsh.instance.open_logs": "打开日志文件夹",

  // ---- 下载页（DownloadPage.java） ----
  "download.new_game": "新实例",
  "download.content": "实例扩展",
  "dsh.download.instance": "实例",
  "dsh.download.packs": "整合包",
  "dsh.download.plugins": "插件下载",
  "dsh.download.skills": "技能包",
  "search": "搜索",
  "search.hint.chinese": "支持中英文搜索",
  "dsh.versions.loading": "正在加载版本…",
  "dsh.versions.load_failed": "无法获取版本列表",
  "download.failed.refresh": "[加载数据失败，点击此处重试]",
  "dsh.versions.remote.empty": "所有已发布的版本都已安装。",
  "dsh.versions.available.empty": "没有新的版本可安装。",
  "dsh.versions.installing": "正在安装 %s …",
  "dsh.versions.install_failed": "安装失败",
  "dsh.market.package_page": "打开 npm 页面",

  // ---- 插件（PluginListPage / PluginMarketPage） ----
  "dsh.instance.plugins.add": "从文件安装插件",
  "dsh.instance.plugins.add.url": "从 URL 安装插件",
  "dsh.instance.plugins.download": "下载插件",
  "dsh.instance.plugins.reveal": "打开配置文件夹",
  "dsh.instance.plugins.to_market": "去插件市场",
  "dsh.instance.plugins.open": "在 npm 上打开",
  "dsh.instance.plugins.source": "%s（npm 包）",
  "dsh.instance.plugins.remove": "卸载插件",
  "dsh.instance.plugins.remove.confirm": "确定从该配置中卸载 %s 吗？",
  "dsh.instance.plugins.empty": "这个配置里还没有安装任何插件。",
  "dsh.market.no_instance": "请先在实例列表中选择一个实例",

  // ---- TUI 终端插件（实例「安装」页区段；见 DshTuiPlugins / DshApiServlet） ----
  "dsh.tui.title": "TUI 终端插件",
  "dsh.tui.bundled": "面板自带",
  "dsh.tui.bundled.hint": "这个插件随面板一起发布，jar 里带着安装包，装它不需要联网。",
  "dsh.tui.rules":
    "两条规则：低版本实例装不了要求更高版本的插件（按钮会灰掉并写明缺什么）；两个第一方终端**可以同时装**——0.2.x 用进程内那个，ACP 那个作为退路，实例版本一变就自动接管，同时装时只有一个在跑（列表标出「当前生效」）。",
  "dsh.tui.active": "当前生效",
  "dsh.tui.active.hint": "点「终端」时启动的就是它。",
  "dsh.tui.standby": "备用",
  "dsh.tui.standby.hint": "已安装但此刻不会启动：另一个终端能跑时它待命，实例版本变化后它会自动接管。",
  "dsh.tui.intro":
    "dsh 本身没有终端界面（DshSurface 只有 WEB/HEADLESS/ACP/SDK），终端由社区插件提供。这里选一个装进它自己的 profile，装好后用实例页的「终端」打开——终端就从这个 profile 启动。",
  "dsh.tui.notice.title": "免责声明：面板只做安装搬运，不改 dsh 任何源码",
  "dsh.tui.notice.scope":
    "面板只做三件事：用 npm 安装、锁定版本、把包挂到该插件自己的 profile（不是实例的 web）。",
  "dsh.tui.notice.core":
    "不修改 dsh 的任何源码：这些插件只使用 dsh 的公开导出。推荐项 dsh-TUI 自述 “zero core changes / 卸载不留补丁（uninstall leaves no patches behind）”。",
  "dsh.tui.notice.third_party":
    "插件是第三方社区作品，与面板、与 HDSL 无关：功能、安全性与可用性归其上游作者，面板不背书、不担保，请自行判断来源后再安装。",
  "dsh.tui.notice.facts":
    "下面逐条列出实际安装的包名、版本、来源仓库、许可证与 profile；不想要了可一键卸载，插件不会在 dsh 里留下补丁。",
  "dsh.tui.notice.web":
    "为什么不装进 web：TUI 插件和 dsh-web-app 都是 app 插件，各自按自己的语法解析同一份 argv，上游不仲裁。同 profile 装在一起，应用会遇上不认识的旗标直接退出（社区实测 error: unknown option '--no-open'）——这不是装了没用，是会让实例起不来。所以面板只往插件自己的 profile 里装，也拦下「装进 web」的尝试。",
  "dsh.tui.package": "包名",
  "dsh.tui.version": "版本",
  "dsh.tui.license": "许可证",
  "dsh.tui.repository": "来源仓库",
  "dsh.tui.repository.none": "上游未声明仓库",
  "dsh.tui.profile": "profile",
  "dsh.tui.profile.row":
    "装进 %s profile —— 不与 web 混装：TUI 插件与 web 都是 app 插件，同一份 argv 会被两套语法解析，装一起实例起不来。",
  "dsh.tui.profile.not_listed": "它装在自己的 profile 里，所以不会出现在「插件」页的列表里。",
  "dsh.tui.installed": "已安装",
  "dsh.tui.not_installed": "未安装",
  "dsh.tui.incomplete": "安装未完成",
  "dsh.tui.incomplete.hint":
    "profile 的清单里写了这个插件，但它的文件不在磁盘上——这通常是安装中途失败（例如包拉不下来）。这种实例开终端会立刻退出且没有任何输出，请「重新安装」。",
  "dsh.tui.reinstall": "重新安装",
  "dsh.tui.update": "更新",
  "dsh.tui.outdated": "有新版本",
  "dsh.tui.outdated.hint":
    "装着的这份和面板现在自带的不一样。第一方插件按安装包的摘要比对（版本号相同也可能内容不同），社区插件按清单锁定的版本比对。点「更新」重装一次即可。",
  "dsh.tui.pure_bundle": "纯插件：无自带入口，启动它的 profile 即为终端（dsh --profile %s）",
  "dsh.tui.bin": "自带入口：%s（优先直接运行，入口缺失时回退到启动 profile）",
  "dsh.tui.requires_dsh": "要求 dsh %s。",
  "dsh.tui.host_mismatch": "这个实例的 dsh 版本不满足要求，装上也无法启动：面板会拒绝安装。",
  "dsh.tui.install": "安装",
  "dsh.tui.remove": "卸载",
  "dsh.tui.loading": "正在读取终端插件清单…",
  "dsh.tui.load_failed": "无法读取终端插件清单",
  "dsh.tui.submitted": "已提交任务（%s），进度见「插件」页",
  "dsh.tui.needs_install": "该实例尚未安装 dsh 运行时，安装完成后才能安装终端插件。",

  // ---- 会话 / 技能 ----
  "dsh.session.empty": "这个实例还没有会话记录。",
  "dsh.session.in_use": "使用中",
  "dsh.session.thinking": "处理中…",
  "dsh.session.send": "发送",
  "dsh.session.tool": "工具",
  "dsh.workspaces.empty": "这个实例还没有任何工作区。",
  "dsh.session.pack.export.selected": "导出选中的会话",
  "dsh.session.delete.confirm.many": "确定删除 %d 个会话吗？此操作无法撤销。",

  // ---- 导出整合包弹窗（ExportInstanceModal，见 InstancesApiServlet 的 /export） ----
  "dsh.export.home": "随包分发的全局内容（home/）",
  "dsh.export.home.hint":
    "默认全不勾：只有勾选的路径随包分发。凭据、密钥、令牌与压缩包已在服务端被排除，不会出现在这里。",
  "dsh.export.home.empty": "这个实例还没有可随包的全局内容（从未启动过，或已被安全过滤排除）。",
  "dsh.export.home.loading": "正在读取可选项…",
  "dsh.export.home.load_failed": "无法读取可选项，将不随包任何 home 内容。",
  "dsh.export.versions": "实测兼容集（dshVersions）",
  "dsh.export.versions.own": "实例自身版本，始终写入",
  "dsh.export.versions.hint":
    "写进整合包 manifest 的 dshVersions：实例自身版本固定在首位，其余勾选的版本一并声明为实测兼容；不勾就只声明实例自身版本。",

  // ---- 设置页（SettingsPage.java） ----
  "launcher": "启动器",
  "help": "帮助",
  "dsh.settings.general": "通用",
  "dsh.settings.appearance": "外观",
  "dsh.node.title": "Node.js",
  "settings.launcher.download_source": "下载源",
  "settings.launcher.download_source.preset": "预设",
  "settings.launcher.download_source.custom": "自定义…",
  "settings.launcher.download_source.custom_url": "自定义地址",
  "settings.launcher.download_source.effective": "当前生效",
  "settings.launcher.download_source.priority": "优先级：面板设置 > 部署环境变量 > 官方默认。",
  "about": "关于",
  "dsh.settings.theme.brightness": "主题模式",
  "dsh.settings.theme.brightness.auto": "跟随系统设置",
  "dsh.settings.theme.brightness.light": "浅色模式",
  "dsh.settings.theme.brightness.dark": "深色模式",
  "dsh.settings.theme_color": "主题色",
  "dsh.settings.theme_color.default": "默认",

  // ---- 关于（AboutTab） ----
  // 下面几条是照搬桌面版 HDSL 的关于页文案（讲的是 HMCL 与 HDSL 桌面版），
  // 网页版的「关于」改用 about.project.* 这一组，讲清楚这个项目本身是什么。
  "about.copyright": "版权",
  "about.copyright.statement": "版权所有 © 2013-2026 huangyuhui 及贡献者",
  "about.author.statement": "bilibili @huanghongxun",
  "about.open_source": "开源",
  "about.open_source.statement": "GPL v3 (https://github.com/HMCL-dev/HMCL)",
  "about.thanks_to": "鸣谢",
  "about.dependency": "依赖",
  "dsh.about.runtime": "Java 运行时",

  // ---- 关于（AboutTab，网页版自己的） ----
  "about.project": "项目",
  "about.project.description":
    "把桌面版 HDSL（DeepSeek Harness 启动器）搬进浏览器的单容器实现：面板负责创建、安装、启停实例，dsh 由面板拉起并反代到 /i/<实例id>/ 下使用",
  "about.version": "版本",
  "about.java": "Java 运行时",
  "about.repo": "仓库",
  "about.license": "许可证",
  "about.license.statement": "GPL-3.0；按 GPL 第 15、16 条不提供任何担保，许可证全文见仓库 LICENSE",
  "about.copyright.web.statement": "© 2026 HDSL contributors；上游 HMCL © 2020 huangyuhui 及贡献者",
  "about.upstream": "上游",
  "about.upstream.statement":
    "与更上游的 HMCL —— 领域层与界面样式源自它们，完整声明见仓库的 NOTICE",
  "about.thirdparty": "第三方组件",
  "about.thirdparty.statement":
    "Jetty、Bouncy Castle、React、xterm.js 等，逐项授权与声明见仓库 NOTICE",
  "about.stack": "技术栈",
  "about.stack.statement":
    "React · Vite · Material Color Utilities · Material Symbols 图标 · Jetty · Java 21 · Node 22 + pnpm",
  "about.privacy": "数据与隐私",
  "about.privacy.statement":
    "不上报使用数据；账号、会话、实例与密钥只保存在数据目录；对外监听端口只有你显式开启的实例",
  "about.feedback": "反馈",

  // ---- 关于（合规段：数据与隐私 / 可接受使用，见 LegalCard） ----
  "about.legal": "合规与声明",
  "about.legal.intro":
    "两份说人话的说明：面板在你机器上存了什么、发出去什么；把面板开放给别人用时，边界在哪。都没有法律效力，有法律效力的是 LICENSE、NOTICE 与 MC 功能的《使用条款》。",
  "about.legal.privacy": "数据与隐私",
  "about.legal.aup": "可接受使用",

  // ---- 关于（MC 段：只有功能开启后才出现，见 McAboutCard） ----
  "about.mc": "Minecraft 服务端",
  "about.mc.state": "状态",
  "about.mc.state.on": "已开启",
  "about.mc.state.off": "已关闭（同意记录仍可回看）",
  "about.mc.consent": "同意记录",
  "about.mc.fingerprint": "文本指纹",
  "about.mc.intro":
    "前一段说人话：这个功能实际做了什么、哪里容易翻车、出事按什么顺序处理，没有法律效力。后一段是使用条款原文，有法律效力；两段冲突时以条款为准。",
  "about.mc.notes.open": "说明：开服前该知道的事（说人话版）",
  "about.mc.terms.open": "使用条款原文",
  "about.mc.terms.cancelled": "已取消。展开条款原文需要开发者提权。",

  // ---- 账户（AccountListPage / dsh.account.*） ----
  "account": "账户",
  "dsh.account.title": "账号",
  "dsh.account.manage": "管理账号",
  "dsh.account.add": "添加账号",
  "dsh.account.add.button": "添加",
  "dsh.account.vendor": "供应商",
  "dsh.account.key": "API 密钥",
  "dsh.account.label": "名称",
  "dsh.account.base_url": "端点地址",
  "dsh.account.check": "校验密钥",
  "dsh.account.check.valid": "密钥有效",
  "dsh.account.remove.confirm": "要删除账户「%s」吗？",
  "dsh.account.change_key": "更改 API 密钥",
  "dsh.account.none.short": "不使用账号",
  "dsh.account.add.custom": "其他供应商",

  // ---- 安装向导（DshInstallWizardProvider / install.*） ----
  "install": "添加实例",
  "dsh.instance.create": "新建实例",
  "dsh.install.step.version": "选择版本",
  "dsh.install.step.quick": "快速安装",
  "dsh.install.back": "上一步",
  "dsh.install.start": "安装",
  "dsh.install.working": "正在创建实例并安装插件",
  "dsh.install.name.taken": "已存在名为「%s」的实例",
  "dsh.install.home": "DSH_HOME",
  "dsh.install.workspace": "工作目录",
  "button.previous": "上一步",
  "button.next": "下一步",
  "button.cancel": "取消",
  "button.ok": "确定",
  "button.save": "保存",
  "button.delete": "删除",
  "button.install": "安装",
  "button.select_all": "全选",
  "button.enable": "启用",
  "button.disable": "禁用",
  "button.copy": "复制",
  "install.modpack.installation": "安装整合包",

  // ---- 安装整合包向导（ModpackInstallWizard） ----
  "modpack.choose.local": "导入本地整合包文件",
  "modpack.choose.local.detail": "你可以直接将整合包文件拖入本页面以安装",
  "modpack.choose.remote": "从互联网下载整合包",
  "modpack.choose.remote.detail": "需要提供整合包的下载链接",
  "modpack.choose.repository": "从整合包市场下载",
  "modpack.choose.repository.detail": "浏览并安装来自本启动器整合包市场的整合包",

  // ---- 第三方品牌（Kimi Code / OpenCode / ZCode） ----
  "dsh.brand.dsh": "DeepSeek Harness (dsh)",
  "dsh.brand.kimi": "Kimi Code",
  "dsh.brand.opencode": "OpenCode（实验）",
  "dsh.brand.claude-web": "Claude Web（实验）",
  "dsh.brand.hermes": "Hermes Agent（实验）",
  "dsh.brand.octop": "Octop（实验）",
  "dsh.brand.pi-web": "PI WEB（实验）",
  "dsh.brand.qwen": "Qwen Code",
  "dsh.brand.mimo": "MiMo Code",
  "dsh.brand.minimax": "MiniMax Code",
  "dsh.brand.zcode": "ZCode（实验）",
  // 「（实验）」是完整显示名的一部分（侧栏标题、第三方警示用），
  // 但实例名不该背这个尾巴（新建实例时预填的就是下面这几条短名）。
  "dsh.brand.opencode.short": "OpenCode",
  "dsh.brand.claude-web.short": "Claude Web",
  "dsh.brand.hermes.short": "Hermes",
  "dsh.brand.pi-web.short": "PI WEB",
  "dsh.brand.qwen.short": "Qwen Code",
  "dsh.brand.mimo.short": "MiMo Code",
  "dsh.brand.minimax.short": "MiniMax Code",
  "dsh.brand.tui.section": "TUI 品牌实例",
  "dsh.brand.tui.tag": "TUI",
  "dsh.brand.tui.warning":
    "这些是纯终端品牌：面板只负责安装、实例隔离和 PTY，没有网页模式、端口或反代地址。进入实例卡里的终端即可使用；退出 TUI 后不会落回 shell。",
  "dsh.brand.state.installed": "已安装",
  "dsh.brand.terminal.running": "终端运行中",
  // PI WEB：终端与网页共用同一个 agent 目录但各自跑会话，所以可以同时开；
  // 这里只描述"形态"与"版本锁"这两个决策，不做互斥确认。
  "dsh.brand.pi-web.mode": "运行形态",
  "dsh.brand.pi-web.mode.both": "终端 + 网页",
  "dsh.brand.pi-web.mode.terminal": "仅终端",
  "dsh.brand.pi-web.mode.both.hint":
    "同一个 Pi：终端和网页读写同一个 agent 目录与会话。网页只是多一个入口。",
  "dsh.brand.pi-web.mode.terminal.hint":
    "不启动网页进程，只保留 Pi 的 TUI。会话、凭据、Pi 包都保留，随时可以切回来。",
  "dsh.brand.pi-web.lock": "锁定版本",
  "dsh.brand.pi-web.lock.hint":
    "锁定后这台实例不跟随品牌的推荐配对升级；切版本需要先解锁。",
  "dsh.brand.pi-web.terminalOnly": "仅终端",
  "dsh.brand.pi-web.versionLocked": "版本已锁",
  "dsh.brand.pi-web.pair": "配对",
  "dsh.brand.pi-web.run": "运行与版本…",
  "dsh.brand.pi-web.pty.missing": "浏览器终端不可用",
  "dsh.brand.pi-web.web.missing": "仅终端 release",
  "dsh.brand.pi-web.install.terminalOnly": "只装 Pi 终端",
  "dsh.brand.pi-web.degraded.title": "安装未完成",
  "dsh.brand.pi-web.degraded.hint":
    "网页这一半没装上。可以只保留 Pi 终端（会话、凭据、Pi 包都保留），或者换源重试。",
  "dsh.brand.pi-web.degraded.continue": "继续（网页无终端）",
  "dsh.brand.pi-web.degraded.lost":
    "失去：网页界面、浏览器内终端、网页文件面板　保留：Pi 终端、会话、凭据、Pi 包",
  "dsh.brand.pi-web.recommended": "当前推荐配对",
  // Hermes 的网页与终端共用同一个 gateway，只能开一个（docs/brands.md
  // 「终端与网页互斥」）。面板在替换另一个模式之前先问一句：说清会关掉什么、
  // 什么不会丢，确认了再换。
  "dsh.brand.hermes.switch.terminal.title": "先停掉网页，再开终端",
  "dsh.brand.hermes.switch.terminal.message":
    "Hermes 的终端和网页共用同一个 gateway，不能同时开。打开终端会先停止这个实例的网页模式——已经打开的页面会断开，之后要重新「启动」网页。会话本身保存在实例数据里，不会丢。继续吗？",
  "dsh.brand.hermes.switch.web.title": "先关掉终端，再启动网页",
  "dsh.brand.hermes.switch.web.message":
    "这个实例的终端还开着。启动网页会先结束那个终端会话——未提交的输入会丢失，历史会话仍保存在实例里。继续吗？",
  "dsh.brand.octop.short": "Octop",
  "dsh.brand.zcode.short": "ZCode",
  "dsh.brand.thirdparty.warning":
    "%s 为第三方工具，由各自官方分发、更新与计费；面板仅负责安装、启动与网页反代，其功能、安全与行为由各官方负责。",
  // OpenCode 专有的能力边界声明：面板已经到了它能做的极限，剩下的问题属于上游。
  // 写在界面上是让人在报 bug 之前就知道该找谁——面板能修的（挂载、改写、反代）
  // 都已经修到不能再修了（见 docs/report-2026-10-08-opencode-web.md）。
  "dsh.brand.opencode.limit":
    "面板对它的接入不保证可用性：只做到「装指定版本 → 拉起它自带的网页端 → 经 /i/<id>/ 反代出来」，至此已经到极限。"
    + "再往下遇到的问题基本都是它自己的——模型目录与模型 id 对不上（例如客户端挑到 deepseek/deepseek-flash，而它自己的服务器只有 deepseek-flash）、"
    + "会话与 agent 的行为、凭据与计费；这类问题面板修不了，只能换模型、换版本或等上游修。",
  // Claude Web（社区项目，非 Anthropic 官方）：引擎是社区构建、登录可用官方账号或第三方中转，
  // 两者都归上游与对应服务商；与 OpenCode 同款边界——面板只做到装、起、反代。
  "dsh.brand.claude-web.limit":
    "Claude Web 是社区开源项目（不是 Anthropic 官方的网页版）：内置在社区构建的引擎上，可登官方账号，也可在"
    + "「设置 → 供应商」里接第三方中转。面板对它的接入同样不保证可用性——只做到「克隆指定版本 → 构建 → 拉起"
    + " → 经 /i/<id>/ 反代出来」；引擎、模型供应商、凭据与计费的问题都属上游/服务商，面板修不了。",
  "dsh.brand.tab.zcode": "ZCode",
  "dsh.brand.state.created": "未启动",
  "dsh.brand.state.starting": "启动中",
  "dsh.brand.state.running": "运行中",
  "dsh.brand.state.stopped": "已停止",
  "dsh.brand.state.error": "出错",
  "dsh.brand.versions": "版本",
  "dsh.brand.latest": "最新",
  "dsh.brand.installed": "已安装",
  "dsh.brand.current": "当前",
  "dsh.brand.none_installed": "尚未安装任何版本",
  "dsh.brand.new_instance": "新建实例",
  "dsh.brand.create": "创建",
  "dsh.brand.default_version": "默认（最新已装）",
  "dsh.brand.view_log": "查看日志",
  "dsh.brand.log.empty": "（暂无日志）",
  "dsh.brand.open": "打开",
  "dsh.brand.delete.confirm": "确定删除「%s」？实例数据会一并删除。",
  "dsh.brand.installing.progress": "正在安装：%s",

  // ---- 品牌实例层（BrandsView）：实例是主对象，版本是共享构建 ----
  "dsh.brand.page.title": "品牌实例",
  "dsh.brand.side.category": "第三方品牌",
  "dsh.brand.side.category.dsh": "dsh 实例",
  "dsh.brand.section.instances": "实例",
  "dsh.brand.section.versions": "版本",
  "dsh.brand.installed_builds": "已安装的版本构建",
  "dsh.brand.version.shared": "版本构建按品牌共享：装过一次，该品牌其它实例都能直接切到它",
  "dsh.brand.version.uninstalled": "未装",
  "dsh.brand.version.installed": "已装",
  "dsh.brand.version.install_and_switch": "先安装 %s 再切到它（版本构建按品牌共享）",
  "dsh.brand.version.stop_and_switch": "该实例正在运行，切换版本需要先停止它",
  "dsh.brand.version.switch": "切换版本",
  "dsh.brand.install_new": "安装新版本",
  "dsh.brand.instance.info": "信息",
  "dsh.brand.instance.plugins": "插件",
  "dsh.brand.instance.rename": "重命名",
  "dsh.brand.instance.address": "打开地址",
  "dsh.brand.instance.created": "创建时间",
  "dsh.brand.plugins.unsupported":
    "该品牌暂不支持插件。面板已为品牌实例预留插件通道：后端把 supported 置为 true 并填充 plugins 后，这里会变成可安装/启停的插件面板，与 dsh 实例同款。",
  "dsh.brand.plugins.empty": "（暂无插件）",

  // ---- 通用 ----
  "message.error": "错误",
  "button.remove.confirm": "你确定要删除吗？此操作无法撤销！",
  "game.directory": "实例文件夹路径",
  "instance.empty.hint": "没有已安装的实例。\n你可以切换其他实例文件夹，或者点击此处进入实例安装页面。",
  // ---- 终端（docs/web-shell.md） ----
  "terminal.title": "终端",
  "terminal.open": "终端",
  "terminal.unavailable": "终端需要 HTTPS，且 Web Shell 总开关已打开（设置 → 开发者模式）",
  "terminal.stop": "停止",
  "terminal.close": "关闭",
  "terminal.reconnect": "重新连接",
  "terminal.password.prompt": "打开终端需要开发者密码",
  "terminal.password.placeholder": "开发者密码",
  "terminal.password.submit": "确认",
  "terminal.password.locked": "失败次数过多，请稍后再试",
  "terminal.desktop": "已在桌面打开终端窗口",
  "terminal.detached": "连接已断开（agent 仍在运行）",
  "terminal.ended": "会话已结束",
  "terminal.failedWithoutOutput": "进程没产生任何输出就退出了：多半是插件没装好（实例「安装」页可重装），或启动参数被拒。",
  "terminal.error": "终端打开失败",
  "terminal.state.opening": "正在打开",
  "terminal.state.locked": "等待密码",
  "terminal.state.kernel": "选择内核",
  "terminal.state.running": "运行中",
  "terminal.state.ended": "已结束",
  "terminal.state.detached": "已断开",
  "terminal.state.desktop": "桌面",
  "terminal.state.error": "错误",
  "terminal.setup.prompt": "还没有设置开发者密码。先设置一个，再打开终端。",
  "terminal.setup.panel": "面板密码",
  "terminal.setup.password": "开发者密码",
  "terminal.setup.confirm": "再输一次",
  "terminal.setup.submit": "设置并打开",
  "terminal.setup.mismatch": "两次输入的开发者密码不一致",
} as const;

export type I18nKey = keyof typeof I18N;
