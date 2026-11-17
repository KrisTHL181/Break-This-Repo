# 会话交接：HDSL-web（2026-10-05）

> 这是又一次 AI 协作会话的压缩交接：这一轮做了什么、查到了什么机制、环境上踩过哪些坑、
> 还剩什么没做。目标是**不用回看会话原文就能接着干**。
>
> **隐私约定（继续遵守）**：真实域名、内网地址、宿主机路径、口令一律不写进仓库；
> 示例统一用 `dsh.example.com`、`192.0.2.x`、`/srv/...`、`<口令>`。
> README 徽章与 ghcr 包名里的仓库账号名 `nas200-timmy` 属公开信息（仓库地址本身就是它）。

## 0. 当前状态

- 分支 `main` 与 `origin/main` 同步（`b714aec`），工作树干净。
- 生产容器已按本轮的代码重建部署，healthy。
- **部署会停掉运行中的 dsh 实例**（会话在磁盘上不丢）：部署前后要提醒用户在面板重新点「启动」。

## 1. 这一轮做完的事（按提交）

| 提交 | 内容 |
| --- | --- |
| `a90f1dc` | `feat(auth)`：支持**修改用户名**——`POST /api/auth/username`，只改名字（口令/盐/迭代次数/创建时间不动），发起改名的会话切到新名、该用户其它会话吊销；设置页新增「修改用户名」卡片。原因：原先单用户只能叫 `admin`（那是 `HDSL_ADMIN_PASSWORD` 建号的兜底名；初始化引导页本来就能自取名字） |
| `12502dd` | `fix(acp)`：**控制台也用实例账户**。新增 web 层 `AcpAccountBridge`：控制台启动前把账户路由写进 `profiles/acp` 补丁层、密钥放进子进程环境，结束时删掉；`AcpSessionManager` 加 `release` 钩子（stop/closeAll/启动失败/进程退出四条路径都调用）；`InstanceRuntime.accountOf` 改公开共用 |
| `31e0101` + `2724e9a` | `feat(web)`：**手机端适配**（侧栏抽屉 / 顶部标签条 / 贴底启动栏 / 触摸尺寸 / manifest）；顺带修**深链接白屏**（vite `base` 由 `"./"` 改 `"/"`）。`2724e9a` 是跟随该改动的静态资源断言测试 |
| `025d827` | `fix(web)`：**关于页改讲本项目**。原来照搬桌面版关于页，展示的是 HMCL 的版权「© 2013-2026 huangyuhui 及贡献者」、作者「bilibili @huanghongxun」、GPL 链接指向 `HMCL-dev/HMCL`；现在讲 HDSL-web 自己（项目/说明/仓库/许可证/上游/技术栈），上游归属保留在「上游」一行并指向仓库 NOTICE |

另外顺手做的两件（无代码改动）：

- **OCI 合规核查**：`docker save` 出来的就是标准 OCI image layout（index/manifest/config/layer 全是 OCI 媒体类型），运行时也不需要特权/额外 capabilities；唯一可改进项是没有 `org.opencontainers.image.*` 注解。
- 删除了本地备份分支 `backup/pre-sanitize`（上一轮历史改写前的提交）。

## 2. 这一轮查清的机制（别再重查）

### 2.1 ACP 控制台与实例 web 是两条独立的 dsh 进程

- 控制台 = `AcpSessionManager` 用 `node <实例>/dsh/... --profile acp` 另起的子进程（与 web 进程互不影响寿命），面板 `/ws` 的 `acp-start/acp-prompt/acp-cancel/acp-stop` 驱动。
- **账户注入原来只做在启动 web 进程那条路上**（`DshLauncher.plan`），控制台既没有供应商路由也没有密钥 → 对话报 `no API key for provider route "deepseek-official"`。现在两条路都注入（见 §1 的 `AcpAccountBridge`）。
- 两条注入的差别（刻意）：控制台**不碰** `settings.yaml` / `.hdsl-injected.json`（那是每个 home 一份、正在运行的 web 实例在用的注记）；只动自己 profile 的补丁层、子进程环境，以及非自家厂商账户时的默认模型。

### 2.2 会话的归属与消失之谜

- **归属单位是实例（DSH_HOME），不是构建版本**。isolated（默认）下 `DSH_HOME = <实例目录>/home`；切版本只换 `<实例目录>/dsh/`（dsh 本体），`home/` 不动。另有 `VERSION_SHARED`（同版本实例共用一个 home，`/data/hdsl/homes/<版本>/`）与 `CUSTOM`。
- 一个 home 内部，会话是**按工作目录分组**的：
  ```
  home/sessions/<cwd 的 slug>/<会话id>/session.v4.jsonl.zstd
  home/storages/workspace.json                        # dsh 自己的工作区注册表（id/路径/标题 + sessionIds + archived/pinned）
  home/storages/session_projcache/sessions/<id>.json  # 投影缓存（cwd、标题）
  ```
- 因此「换个构建/入口就找不到历史」的真实原因是**看的列表对应的工作区不一样**：dsh 网页端只列它注册过的工作区（默认取 `$HOME`，即 `/home/hdsl`，标题就是目录名），而面板控制台按实例工作区（`/data/hdsl/instances`）起步 → 两边落在不同 slug 组。
- **文件不会丢**：面板的「会话」tab 会扫描所有分组（`DshSessions.list` 遍历每个 slug 目录），另有 `GET /api/instances/{id}/workspaces` 按 harness 自己的方式分组。
- **跨构建共用一个 home 是 harness 明确的不安全情形**：`DshSessions` 的类注释说明日志格式版本（`session.vN`）是 harness 唯一公布的兼容信号，「就地重写一个 home 的会话」正是它警告的失败模式。当前格式：workspace unit v2 / session v4 / projcache v7。

### 2.3 实例之间的「共享」到底共享什么

- 共享**包实体**：`/data/pnpm-store`（pnpm 内容寻址 store），实例的 `node_modules` 是硬链接过去。实测：四个实例 + store 逻辑 12G，实际占盘 4.5G（省约 7.5G）。所以切版本很便宜（store 里已有就只是硬链接）。
- 隔离**状态**：每个实例自己的 `dsh/` 安装树 + 自己的 `home/`（会话、profile、凭证）。
- 一句话：**一个实例一个版本、默认隔离；切版本复用 store 的包，home 保持不动——包共享、状态隔离。**

### 2.4 容器内那份 Kimi Code

- `/home/hdsl/.kimi-code`（bin/kimi、登录态、会话）在容器可写层，**不在任何卷里**：`docker compose up -d --build` 会把它连登录态一起抹掉。要常驻得落到 `/data` 或加卷。
- `kimi acp`（stdio ACP 服务端）可用，已实测 `initialize` 握手；`kimi -p "<指令>"` 可做一次性非交互调用。但**不要**把它开成对外 TCP 端口：容器里有 `users.json`（口令哈希）、`/data/certs/`、各实例 home 里注入的供应商凭证。

### 2.5 移动端判定的两个刻度（改一处必须改两处）

- `web/src/styles/mobile.css` 顶部注释与 `web/src/hooks.ts` 的 `MOBILE_QUERY` 必须逐字一致：
  - 移动版式：`(max-width: 760px), (max-height: 520px)`（后者覆盖横屏手机）
  - 触摸优化：`(hover: none) and (pointer: coarse)`（与宽度无关）
- 抽屉状态在 store 的 `sidebarOpen`；汉堡按钮只在主页出现（主导航只挂在主页）。
- 实例页的 `nav.sidebar-sub.sidebar-stack` 是手写外壳（操作框 + 标签条一列），移动端**保持竖排**，只让它内部的 `SubSideBar` 变标签条——写 CSS 时别把 `.sidebar-sub` 一刀切。

## 3. 环境与验证配方（复用，省时间）

**构建/部署**：`NPM_REGISTRY=https://registry.npmmirror.com/ docker compose up -d --build`（暖缓存 20–40 秒）。

**测试**（宿主没有 java/node，一律走容器）：

```bash
# 从运行中的面板容器取一套 node+pnpm
docker cp hdsl-web:/opt/node /tmp/hdsl-test-node
docker cp hdsl-web:/data/corepack /tmp/hdsl-test-corepack
docker run --rm -v "$PWD":/src -v hdsl-gradle-home:/root/.gradle \
  -v /tmp/hdsl-test-node:/opt/node -v /tmp/hdsl-test-corepack:/corepack \
  -e COREPACK_HOME=/corepack \
  -e PATH=/opt/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
  -w /src eclipse-temurin:21-jdk ./gradlew --no-daemon test -x buildWeb
```

- 必须 `-x buildWeb`（宿主 `web/node_modules` 属主与容器不一致，esbuild 会 EACCES）。
- 不带 node 时那批 node 用例会被 `Assumptions` 跳过，看着"全绿"其实没跑。
- 基线：**427 用例 / 0 失败 / 0 错误 / 0 跳过**。
- 坑：输出别用 `| head` 截断——SIGPIPE 会让 `docker run` 客户端退出而**容器继续活着**，占住
  `/root/.gradle/caches/journal-1` 的锁，下一次构建报 "Timeout waiting to lock journal cache"。
  真遇到了：`docker ps --filter volume=hdsl-gradle-home` 找到它 `docker rm -f`。

**前端检查**（`vite build` 不做类型检查，必须单独 typecheck）：

```bash
docker run --rm -v "$PWD":/src:ro -w /work node:22-bookworm-slim sh -c '
  mkdir -p /work && cd /work &&
  tar -C /src --exclude=web/node_modules --exclude=web/dist --exclude=build --exclude=.git -cf - web | tar -xf - &&
  cd web && npm ci --no-audit --no-fund --registry=https://registry.npmmirror.com/ &&
  npm run typecheck && npm run build'
```

**单独 `docker build` 的坑**：必须带上与 compose 相同的 build args，否则 `ARG` 变化会让缓存失效、
重跑 apt 层（约 10 分钟）并去连不可达的 `nodejs.org`：

```bash
docker build --build-arg NPM_REGISTRY=https://registry.npmmirror.com/ \
  --build-arg NODE_DIST_MIRROR=https://mirrors.cloud.tencent.com/nodejs-release -t hdsl-web:test .
```

**视觉验证（手机端那种改动值得做）**：临时容器（独立端口 + 独立卷 + 自带 `HDSL_ADMIN_PASSWORD`）
+ headless Chromium 截图。可用镜像 `ghcr.io/puppeteer/puppeteer`，要点：

- `--network host` 才能访问 `127.0.0.1:<临时端口>`；`NODE_PATH=/home/pptruser/node_modules`（CJS）；
  输出目录要先 `chmod 777`（镜像里跑的是 uid 10042 的 pptruser）。
- 手机视口用 `{width:390,height:844,isMobile:true,hasTouch:true}`（`hasTouch` 才让
  `pointer: coarse` 生效）；每张断言 `documentElement.scrollWidth === innerWidth`（无横向溢出）。
- 会话 cookie 名是 `hdsl_session`，值从 `POST /api/auth/login` 的 Set-Cookie 取。

**在容器里跑 jar 的类（查数据用）**：`docker cp hdsl-web:/app/hdsl-web.jar` + 用 jdk 容器 `javac -cp`
编译一个小驱动，再 `docker cp` 回容器 `java -cp /app/hdsl-web.jar:/tmp <类>`；**必须**加
`-Dhdsl.home=/data/hdsl`（否则 `DshInstanceManager` 找不到实例）。

## 4. 未决与待办

- **标题栏**仍是 `Hello DeepSeek! Launcher v…`（登录页卡片倒已经是「登录 HDSL-web」）。
  要不要改成 `HDSL-web v…` 未定——一行 i18n 的事（`I18N.appFullName`）。
- 可选：让面板启动实例时把**实例工作区注册进 dsh 的 `workspace.json`**，这样网页端与控制台
  共享同一份会话视图（现在是两套分组，用户会觉得"历史不见了"）。
- 上一轮交接稿里仍挂着的：`/i/*` 中继上限只有实机 A/B 验证、没有 JUnit；`PATCH /api/instances/{id}`
  不支持改工作区；`--no-open` 探测超时会被当成"不支持"缓存（共享领域层，未改）。
- 容器内那份 kimi 仍未持久化（见 §2.4）。
- 仓库 / CI / 交接文档入库、`ghcr.io/nas200-timmy` 属公开信息，已决定不处理。

## 5. 给下一个功能的提醒

- 领域层 `org.jackhuang.hmcl.dsh` / `setting` 是桌面版逐字拷贝：改动尽量落在
  `org.jackhuang.hmcl.web.*` 与 `web/`（本轮三处新代码都遵守了这条）。
- 面板前端是固定视口的 SPA（`body{overflow:hidden}`），加页面/加组件时注意移动端视口与
  `mobile.css` 里那两条判定。
- 提交风格：Conventional Commits 前缀 + 中文标题 + 详细 body（写清"改了什么、为什么、怎么验证的"）。
