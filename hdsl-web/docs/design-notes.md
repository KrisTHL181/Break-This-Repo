# 设计说明

README 只保留「用户需要知道的」。这里记录为什么这样做——改动相关逻辑前先读这一篇，
再动手；用户文档不重复这些实现事实。

## 1. 架构与反向代理

- 面板与 dsh 是两个彼此独立的页面：面板只负责创建、安装、启动、停止，真正的使用在 dsh
  自己的网页里。因此两者共享同一个域名、端口和证书，dsh 页面挂在 `/i/<id>/` 下。
- 面板自己也会被 nginx / Caddy 架在域名或子路径后面。「反代的反代」是支持形态：SPA、登录、
  API、WebSocket、`/i/<id>/`、壁纸资产都有实测覆盖。
- `Host` 必须原样透传：dsh 会校验 `Origin == Host`，改写 Host 会得到 403。
- 子路径挂载（`HDSL_BASE_PATH=/panel`）时，反代剥掉前缀转发；面板在 index.html 注入
  `<base href="/panel/">` 与 `window.__HDSL_BASE__`，SPA 路由、API、WS、`/i/<id>/` 打开地址
  都按挂载点前缀生成。
- WebSocket 升级头（`Upgrade` / `Connection`）两种挂法都必须透传，否则日志流、任务进度和
  品牌实例状态不会刷新；页面其余功能不受影响。
- `/i/<id>` 无尾斜杠时反代 301 到目录形式；dsh 自己的 WebSocket（`/api/remote.mux`）原样转发。

## 2. 品牌区（Kimi Code / OpenCode / Claude Web / ZCode）

- 实例是主对象，版本是品牌级共享构建：一次构建（尤其是 Claude Web 的源码构建）成本很高，
  同品牌实例共用同一份 `releases/<版本>/`，切换版本不需要重新安装。品牌区因此做成实例列表页
  内的第二层，而不是「装完版本就没下文」的一次性工具。
- 终端与网页共用一份实例状态：dsh / 品牌 / ZCode 的适配器、一次性 ticket 与门禁收在
  `web/terminal/InstanceTerminal`，协议见 [terminal-protocol.md](terminal-protocol.md)，
  Claude Web 的两个内核（社区引擎 / 官方 Claude Code）由用户选。
- 每个实例的插件通道已在界面与后端预留；上游没有统一插件协议，所以现在一律答「暂不支持」。
- 图标取自上游仓库自己发布的标志：dsh 用 DeepSeek 鲸鱼，Kimi Code / OpenCode 取 MIT 仓库
  原文件，ZCode 取许可明确的 CC0 那份。Claude Web 不上图——Anthropic 的商标条款要求事先
  书面许可，只保留文字标签。完整来源与声明见 `NOTICE` 的 TRADEMARKS 一节。
- Claude Web 是首个 git 源品牌：上游不发 npm 包，面板从 GitHub 发行 tag 下载源码 zip。
  不走 git 协议是因为该网络下 git 传输成功率低；`npm ci` 会连同根项目与 ccb 引擎一起装齐。
- 面板替 Claude Web 兜了三个容器环境的坑：
  - 页面用相对路径资源，WS 连接需要注入 shim，否则界面报 "connection closed"；
  - 无桌面环境时「打开文件夹」不可用，改用它的 `workspaces.add` 协议注册工作区；
  - OpenAI 类供应商需要 ccb 内核，安装流程会一并处理。

## 3. 下载源与 pnpm 配置

- 优先级固定为「面板设置 → `NPM_CONFIG_REGISTRY` → 官方默认」。面板里保存立即生效，
  改环境变量需要重启。
- 面板会校验 URL（协议、主机名、非法字符、长度），拒绝时设置不变。
- pnpm 11 不读 `npm_config_*` / `NPM_CONFIG_REGISTRY`，所以设置要落到它真正读取的地方：
  - 重写 pnpm 全局 `config.yaml` 的 `registry` 一行，保留其它设置（`storeDir` 不会丢），
    值按 YAML 规则加引号，临时文件原子替换并尽量收紧权限到 0600；
  - 给子进程同时注入 `npm_config_registry` / `NPM_CONFIG_REGISTRY`（npm 系读它）；
  - 面板自己起的 pnpm 显式加 `--registry=`，日志里能看到实际使用的源；
  - corepack 下载 pnpm 本体时用它自己的 `COREPACK_NPM_REGISTRY`。
- 镜像入口脚本每次启动都会生成这份 `config.yaml`；绕开入口脚本直接 `--entrypoint java`
  时，镜像里预置了只含 `storeDir` 的同名文件作兜底。

## 4. 实例生命周期

- `POST /api/instances` 默认 `autoInstall:true`，创建即安装；未安装的实例点「启动」会先装再启。
  进度与失败原因在面板显示，失败同时写入容器日志。
- 停止会杀掉整棵进程树：端口立即释放，`/i/<id>/` 立即不可用。dsh 的访问 token 每个进程
  随机生成、只存内存、重启即换，不运行就没有可被攻击的东西。
- 实例端口从 3081–4081 的池子里分配并终身绑定，只监听回环，不对外发布。
- 版本安装失败不毁旧版：旧树先挪到 `dsh.previous`，失败或中断时挪回原路径（Windows 的
  junction 是绝对目标，必须回到原路径）。这条对所有平台都重要，不只是 Windows。

## 5. 数据布局与工作区

- 全部状态都放在 `HDSL_DATA` 下，所以「挂一个卷、备份一个目录」就覆盖账号、证书、实例、
  `DSH_HOME`、pnpm store。dsh 实例自己的安装与 home 在 `hdsl/instances/<id>/` 下。
- pnpm store 与实例目录默认在同一个文件系统（都在 `/data` 下），这样 pnpm 能用硬链接复用；
  跨文件系统时只能复制，安装会变慢、占更多空间。
- 工作区是 dsh 会话的「项目根」：读写文件、跑命令、dsh 进程的工作目录都在它下面。它与面板
  放实例的目录不是一回事；容器里默认是 `/home/hdsl/workspace`，已有实例保留创建时记录的值。

## 6. HTTPS

- 配置优先级：环境变量 > `server.yaml` > 内置默认值。`server.yaml` 键都可选；写错或非法值
  会让启动失败并指出行/字段，服务器不会带着半份配置起来。
- `redirect_same_port`（默认开）：TLS 端口同时接受明文连接，只回
  `308 Location: https://<同主机>:<同端口><同路径>`，内容永远不会以明文发出。这样没有域名、
  直接用 IP 访问也不会撞上 `ERR_INVALID_HTTP_RESPONSE`。会话 Cookie 带 `Secure`，明文请求
  不会带上。
- 自签证书的 SAN 覆盖 `public_base_url` 的 host、`localhost`/`127.0.0.1`、本机所有网卡地址
  与主机名，因此 IP 直连不会报域名不匹配。
- 面板上传证书是热切换：HTTPS 用 `SslContextFactory.reload()`；HTTP 在运行时把明文连接器
  换成 TLS 连接器；运行时切换失败则回滚为明文并返回 `restartRequired: true`（选择已写入
  `server.yaml`，重启即生效）。
- 只认 TLS 的端口：把 `redirect_same_port` 设为 `false`；再开一个固定明文端口做跳转：
  `redirect_http: true` + `redirect_port`。

## 7. 安全模型

> 这一节记的是**为什么这样设计**（取舍与理由）。面向部署者的威胁讲解、误配自查清单与免责
> 声明在 [`security.md`](security.md)；两页有重叠的地方，**以那一页为准**（它跟着现状走，
> 这一节保留当初的判断过程）。

### 7.1 保护什么

三样东西，按重要性排：

1. **实例的运行环境**。agent 以面板用户的身份跑，能读 `$DSH_HOME`、模型密钥与工作区；终端更是
   直接的代码执行。拿到其中任何一个，等价于拿到这台机器上该用户的一切。
2. **面板会话**。会话 = 面板的全部 API（含改密码、装插件、改 TLS），所以 cookie 的处置和密码同级。
3. **凭据本身**：`auth/users.json`、`developer.json`、`auth/accounts*`、TLS 私钥。

### 7.2 不负责的边界（写清楚，免得误以为是漏洞）

- **宿主机与容器权限**：能改 `/data`、能 `docker exec`、能重启面板的人，本来就是最高权限。
  E-Stop 的"重启即解除"、`auth.disabled` 只限回环、都能归到这一条。
- **与面板同 uid 的 agent（结构性事实，不是缺陷）**：`auth/users.json`、`developer.json`、证书私钥都是
  0600，**这只挡别的用户**。agent 以面板用户的身份运行，同一个 uid 下它可以直接重写这些文件
  （连原密码都不用知道，换一个自己算的哈希即可），也可以在 `/proc/<pid>/environ` 读到面板进程的
  环境变量（含 `HDSL_ADMIN_PASSWORD` 这类启动种子）。
  **面板侧的任何检查都挡不住这件事**：校验和要存、签名密钥要放，两者都在它读得到的地方；
  文件被改动只能在事后告警，而"改完重启"在日志里就是一次正常启动。
  所以这里不做"哈希文件保护"之类的假防线——需要把 agent 当对手时，只有 OS/容器层的边界有效，
  三条可行做法与代价见 `docs/deployment.md` 的「把 agent 当对手」。
- **网络层**：面板默认绑 `0.0.0.0`，不做 VPN/白名单/端口敲门；明文 HTTP 下的嗅探也不拦（见 7.4）。
  这些是部署者的选择。
- **实例里的第三方代码**：dsh 本体、社区 TUI 插件、`.dspack` 整合包的内容不做安全审计。面板只保证
  "从哪来、装到哪、能不能装"（`plugins/README.md`、`docs/modpacks.md`），不保证它们不干坏事。
- **不做 RBAC**：一把面板钥匙 + 一把开发者钥匙，没有角色、没有按实例授权。

### 7.3 三层门 + 一个刹车

1. **会话**（`AuthFilter`）：除登录、健康检查与 SPA 外壳外，`/api/*`、`/ws`、`/i/*` 全要会话。
2. **开发者提权**（`DeveloperElevation`）：30 分钟滑动，可延长一次到 3 小时，按 **session token**
   记账；密码错误 10 分钟内 3 次锁 10 分钟。终端一直要它；**开发者守卫**可选把它扩到
   `/i/<id>/*`（`proxy`）、实例的写操作（`instance`）或全部 API（`all`）。
   它**只挡新进门**：已加载的页面、已附着的终端、正在跑的 agent 都不动——这是刻意的，
   否则一次提权过期会打断正在跑的活。
3. **可选的守卫**（`developer_guard.scope`，默认 `off`）：见上。脚本/巡检 agent 的正门是
   `POST /api/developer/elevate` 返回的 bearer token（只在内存、同 TTL、改密码即失效）。
4. **紧急停机**（`EmergencyStop`）：唯一会动"正在跑的东西"的开关。关终端 → 强杀实例进程树 →
   冻结 1 小时（`/i/*`、实例写操作、终端一律 503）。状态只在内存，重启进程/容器即解除，
   这是唯一的物理恢复方式，也是刻意的选择（宿主机权限 = 恢复键）。

### 7.4 明文 HTTP：随它去，但不给钥匙

**开发者模式、终端、守卫、E-Stop 只在面板此刻真的在讲 TLS 时可用**（读主连接器状态，尊重
`https.trust_forwarded_proto`）。裸 HTTP 部署随它去——那是部署者的选择，面板不为他兜底；但也
不会在那种状态下把开发者模式打开：没有 TLS 时"提权"本身就是明文传密码，给一把假锁比不给更糟。

- HTTP 下开发者页只给提示与去「HTTPS 证书」的按钮；终端 open 返回 403 `insecure_transport`；
  提权与守卫返回 409/403 `tls_required`；`/i/*` 的提权页直接说"面板不在 HTTPS 下，
  开发者模式不可用"（不会渲染密码框，因为填了也没用）。
- **守卫在裸 HTTP 下没有意义**：它不是"再加一道门"，而是"提权管到哪"；提权本身拿不到，
  守卫只会把实例网页一起锁死。所以守卫与 E-Stop 的控件在 HTTP 下变灰并写明原因（不隐藏）。
- 判断"是否 HTTPS"遵守 `https.trust_forwarded_proto`：反代终结 TLS 且发了
  `X-Forwarded-Proto: https` 就按 HTTPS 算。这个信任是显式开关，默认关——直接能连到面板
  HTTP 端口的客户端可以自己写这个头，所以别在混合暴露的部署上打开它。
- 想让面板在 HTTP 上可用又安全，正确做法是**给它 TLS**（自签证书、真证书、或反代终结 TLS），
  不是放宽判定。

### 7.4.1 出站 TLS：出错要说原因，放宽要人点头

面板自己发出的 HTTPS 请求（品牌版本、源码包、npm 源、模型目录）只认 JVM 自带的 `cacerts`。
Windows 上常见一种情况：杀毒软件、企业网关或 GitHub 加速器（例如 FastGithub）在中间人转发，
它把自己那张根证书装进 **Windows 证书库**——浏览器因此打得开，Java 打不开，报
`PKIX path building failed`。旧表现是"版本列表突然空了"，没有一句原因。

现在的规则：

- **默认严格**，不因为"机器上有代理"就自动放宽；
- 失败时把原因说出来：品牌版本列表回 409 并写明"证书没通过校验（谁、为什么、两条出路）"，
  ZCode 源码下载失败同样附上；`-doctor` / 日志里也有对应行；
- **放宽要人确认**：`POST /api/network/trust`，`acknowledge` 必须逐字等于
  `我确认这是我自己的网络代理`（一句话，不是一个勾选框），`minutes` 1–1440；写进
  `server.yaml` 的 `network.accept_unverified_until`，重启仍在期内有效，到期自动收紧，
  `DELETE` 可随时撤销，每次授权/撤销都进审计；
- **窗口里也不是直接跳过校验**：先按 cacerts、再按机器自己的根证书库试一遍，都不认才接受，
  并把证书的主题与签发者写进日志——真正被"跳过"的那条链路在日志里看得见；
- 边界：这条只影响**出站**请求；面板自己的服务端 TLS（证书上传、自签、反代）完全不变。

### 7.5 其余不变量

- 公开端点只有 `POST /api/auth/login`、`GET /api/auth/status`、`POST /api/auth/setup`
  （仅无用户时）与 `GET /api/health`，外加不含数据的 SPA 外壳（登录页必须先能打开）。
  `/api/*`、`/ws`、`/i/*` 一律要求会话。
- 口令用 PBKDF2WithHmacSHA256，per-user 盐，21 万次迭代，参数记录在 `users.json` 内；
  文件原子写入并尽量收紧到 0600。开发者密码同一套（`developer.json`，0600）。
- 改密码或改用户名后，同用户的其它会话立即失效（发起操作的那个会话保留）；改开发者密码会
  `clearAll()` 所有提权与 bearer token。
- dsh 只监听回环：`dsh web` 不接受 `--host 0.0.0.0`，外部能到达的只有面板端口；
  TLS 在边缘终结。
- 面板 Cookie 是 `HttpOnly` + `SameSite=Strict`，HTTPS 下加 `Secure`，`Path=/`；反代把
  dsh 的 `Set-Cookie: … Path=/` 改写成 `Path=/i/<id>/`，多实例的同名 Cookie 不会互相覆盖。
- `auth.disabled: true` 只在绑定回环地址时允许——能免登录，但不可能误配到公网。
- **日志与审计含操作者与来源**：`/data/logs/audit-*.jsonl` 记谁在哪个地址做了什么，文件权限是
  0644（同机其他账户可读）。要更严就自己收权限或把 `/data` 放到受控位置。
- Windows 没有 POSIX 的 0600 语义，凭据文件可能被同机其他账户读到；按单用户机器对待，
  并且不要把数据目录暴露到公网可访问的位置。

### 7.5 身份划分：容器里两段，裸部署一段

面板要写自己的凭据（`auth/developer.json`、`certs/*.key`、`server.yaml`、审计日志），又要
跑 agent。同一 uid 下这两件事分不开——权限位是给 **uid** 的，不是给进程的——所以"agent 改不动
口令文件"这个要求在单 uid 部署里**做不到**，不是没做。

容器里因此分两段（`util/platform/Privileges.java`）：

- 面板是 PID 1、uid 0；入口脚本把 `auth/`、`certs/`、`logs/`、`server.yaml` 归 root 0700/0600；
- 每个 agent 进程派生时前置 `setpriv --reuid=1000 --regid=1000 --init-groups --`，跑成镜像里的 `hdsl`；
- 实例子目录 `1000:1000 2775`（setgid）+ 入口 `umask 002`，所以**面板（root）写进去的文件是
  `root:hdsl 0664`，agent 照样能改**——否则 root 会把 agent 自己的目录写死；
- 但"setgid 卷 + `umask 002`"是**部署的意外，不是面板能依赖的性质**：普通 bind mount、别的入口
  脚本、或老实例里已经存在的 root 文件，都会让上一条不成立，症状是 `EACCES …/profiles/web/
  cordis.yml` 这类"长得像上游 bug"的失败。所以面板自己往 agent 要写的树里写东西（`DSH_HOME`、
  品牌 home、安装树）时，**写完就交接**（`Privileges#handWrittenFile`、`#handTreeToAgent`），
  不靠挂载参数；每次启动 `DshLauncher#prepareInstanceDirectories` 还会看一眼关键路径的属主，
  发现是别人的就把整棵树交接一次（老实例自愈）。同一条规则也用在"以 root 跑一次 `--help`"这种
  看似只读的动作上——它并不只读；
- 顺带一条容易被忘掉的事实：`Privileges#prepareAgentDirectory` 用的是 `0775`，而 `chmod` 到
  `0775` 会**清掉 setgid 位**。所以 `instances/<id>` 与 `home` 从第一次启动起就已经不在
  `/data` 那条 setgid 链上了——`/data` 的 `2775` 只兜得住这两个目录之外的兄弟。任何"root 在
  home 里建的东西会自动继承 hdsl 组"的推理，从这一刻起都不成立；
- `setpriv` 是 exec 原地替换：进程组、PTY 会话、"不经 shell"这三条都不变。

裸部署没有这一层：面板与 agent 同 uid，秘密文件对 agent 可读可写。面板**不会**拒绝以 root 启动
（拒绝只会换来"那我不用了"），但启动横幅会写明"你派生的每个 agent 也是 root"。
要隔离就用容器部署，或照 `docs/deployment.md §10.0` 用 systemd 沙箱把面板关进 `/data`。

验收：`scripts/verify-privilege-split.sh` 在容器里以 uid 1000 跑，断言 `auth/`、`logs/` 读写都被拒，
而实例目录与 pnpm store 照常可写。

### 7.5.1 这一层盖住了什么，没盖住什么

**盖住了**：`auth/`（面板账号与开发者密码哈希）、`certs/`（面板自己的 TLS 私钥）、`logs/`
（审计）、`server.yaml`（开关与闸门配置）。agent 读不到也写不了，所以"改密码哈希接管面板"
这条路断了——顺带它也 `kill` 不了面板（不同 uid，`/proc/<pid>/mem` 也碰不到）。

**没盖住、而且是有意不盖的：供应商 API key。**

- 位置：`<用户目录>/launcher-settings.json` 的 `accounts` 段。Linux 容器里是
  `/home/hdsl/.local/share/hdsl/launcher-settings.json`（`HMCL_USER_HOME` 在 Linux 上走 XDG，
  不是 `~/.hdsl`），属主 uid 1000。
- 为什么留着：agent 和 ACP 会话要用这把 key 去调模型。把它锁成 root 0600，实例里的 agent
  就用不了自己的账号；而现在的 dsh 已经能正常从配置文件读 key，不再需要面板往环境变量里
  注入那套老办法——锁上只会把功能弄坏，不会把风险消掉（agent 本来就能通过 ACP 把 key 用出去）。
- 因此这是**已知且接受**的暴露：能在容器里以 1000 跑起来的代码，读得到这台面板配的模型 key。
  缓解在部署侧，不在面板里：
  - 一台机器一套 key，别把团队共用的生产 key 配进来；
  - 额度和权限按"这台机器值多少"给，能撤销、能轮换；
  - 各品牌工具的凭据本来就各自管各自的（kimi / opencode / claude-web 的凭据在各自实例的
    home 里），面板不集中持有。
- 同一类还开着的两条：**实例之间仍互相可写**（同为 uid 1000，`instances/` 组可写）；
  **面板的 HTTP 面没有变**——这次只是让 agent 不能再从文件系统侧面绕过它。

裸部署里这四条全都开着（面板与 agent 同 uid），区别只是多了一条"面板自己的凭据也在里面"。

## 8. inotify 配额

- dsh 启动要监视自己的 profile 目录。内核配额不足时，node 可能当场退出，也可能永远不打印
  就绪行；两种都不像「配额不足」，所以 FAQ 先让人查 `fs.inotify.max_user_watches`。
- 配额按 uid 计、容器之间不隔离：宿主上同一个 uid 的其它程序（编辑器服务、同步客户端）会
  与容器抢同一份额度。
- 面板「设置 → 环境体检」的 File watchers 一行会直接标出是否不足（`[SHORTAGE]`）；
  调大办法见 `docs/deployment.md` 的 inotify 一节。

## 9. Windows 适配

- Windows 上有两个目录：启动器用户目录 `%APPDATA%\.hdsl`（实例、运行时；可用
  `-Dhdsl.home=` / `HDSL_USER_HOME` 覆盖）与面板数据目录 `HDSL_DATA`（账号、配置、证书、
  pnpm store；默认 `./data`，相对当前工作目录）。两者分开是为了让「从哪双击启动」不影响
  实例位置。
- 实例终端：Win10 1809（build 17763）起支持，走 ConPTY（`docs/web-shell.md` §7.1），
  服务形态（session 0）也可用；Win7/8.1 及更早的 Win10 明确不支持（终端接口 409，
  文案带实际 build 号）。会话租约的 `flock` 观察仍不可用。
- 其余 Windows 适配（运行时布局、`.cmd` 派生、符号链接降级、进程树终止、ZCode 构建自供
  工具链）见 `docs/windows-bare-deploy.md`。

## 10. 构建、兼容底线与发布

- Dockerfile 是三阶段：web（npm 构建 SPA）→ build（Gradle 产 shadow jar）→ runtime
  （**JDK** + Node + pnpm + 一套构建工具链：go / rust / gcc / python / pkg-config / wget / busybox）。
  第三段的底座由 `HDSL_BASE` 决定（默认 Debian，可换 Fedora / openSUSE，包管理器按 `/etc/os-release`
  分派，见 `docs/deployment.md §4.1`）。
  runtime 阶段另外装这套工具链是有意的：容器也是人工排障、现编现修的地方，理由与逐项说明在 Dockerfile
  的注释里。build 阶段故意不带 npm，SPA 由 web 阶段产出后拷进去，
  所以容器构建不需要本机的 `web/node_modules`。
- 本地 `./gradlew build` / `shadowJar` 会检测 PATH 上的 npm 并执行 `npm run build`：
  第一次构建前需要 `cd web && npm install`，否则因缺少 `node_modules` 失败。
- jar 用 `--release 17` 编译，最低运行时 JDK 17；镜像仍内置 OpenJDK 21。
- SPA 的兼容底线是 Firefox 115 ESR / Chrome 109 / Safari 15.6。CI 的 Compatibility gate
  用 `es-check es2020` 钉 JS 语法、用 `compat-check.mjs` 拦 CSS 黑名单；底线与规则见
  `docs/compatibility.md`。
- 发布：`v*` 标签触发多架构镜像（amd64 + arm64）与 Release；只有最新的 `v*` 标签会移动
  `latest`。细节见 `.github/workflows/docker.yml`。

## 11. ZCode（实验性）

- ZCode 不在稳定性承诺内：实例只绑回环，经 `/i/<id>/` 访问；发行包由面板从上游源码构建，
  能否使用取决于上游。
- 构建不需要 C++ 编译链：`node-pty` 用预编译，`cpu-features` 是 ssh2 的可选依赖。
  面板自供 Node 24+、pnpm 与（构建脚本自己的裸 `tar` 需要的）指向系统 bsdtar 的 shim；**面板自己解包源码不再经过系统 tar**——GitHub 的 tarball 用 UTF-8 存文件名，Windows 的 libarchive 按 ANSI 代码页解码，遇到中文文件名会整包解包失败（`Invalid empty pathname`），所以改用面板自带的 kala-compress 读取器；构建前用 `vswhere` 预检
  `cl.exe`，缺了只说明会被跳过，不自动下载编译器。见 `docs/report-2026-10-18-zcode-windows-build.md`。

## 12. 账户与模型目录

- 账户的供应商与模型名来自 models.dev 目录，并与 dsh 自家目录合并；目录拉不到时退回纯手输。
  目录缓存有 TTL，避免每次打开设置都请求外网。
- 「校验密钥」会向供应商请求一次模型列表：状态码是 2xx 且正文像列表才算有效；401/403 只说明
  「这个地址不接受这把钥匙」，不能断言钥匙本身错误（供应商的说法会一并展示）。检查失败仍然
  保存——失败可能是网络或该地址不提供模型列表。
