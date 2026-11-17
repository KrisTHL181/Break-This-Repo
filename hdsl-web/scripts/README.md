# scripts/ —— 仓库自带的验证脚本

这里的脚本**不在 CI 里跑**（CI 只做两件事：`./gradlew build` 与镜像构建）。它们是改动之后、
发版之前用**真东西**验一遍的工具——真 npm 安装、真进程、真浏览器。共同前提是先有一个可用的 jar：

```bash
cd web && npm install        # 本机第一次（build.gradle.kts 发现 npm 就会跑 web 构建）
./gradlew shadowJar          # → build/libs/hdsl-web-<版本>.jar
```

| 脚本 | 验什么 | 需要什么 | 典型耗时 |
| --- | --- | --- | --- |
| `api-smoke.py` | **接口层冒烟（39 项）**：自己起一个临时面板（临时数据目录、随机端口、HTTP），把"一个人会点到的路径"逐个念一遍——`/api/node` 的字段、品牌版本（或它为什么为空）、doctor、出站证书的一次性授权、ZCode 工作区、dsh 半安装的拒绝、终端四道闸、静态资源。**不点浏览器、不装包**，十几秒跑完 | 只需一个 jar（`./gradlew shadowJar`） | 约 10 秒 |
| `mobile-acceptance.py` | 移动端信息架构 + 桌面版式回归（headless Chromium） | python playwright + chromium、一个可登录的实例、当前工作区的 `web/dist` | 半分钟起（不设实例 id 时） |
| `e2e-real.sh` | 真实 npm 安装 dsh → 启动 → 反代访问 → 停止后 `/i/<id>/` 消失 | 网络（npm registry）、jar | 几分钟 |
| `e2e-phase4.sh` | Phase 4 全链路：体检 / 整合包市场 / 安装 / 会话·技能·工作区 / 导出并下载 / 反代 / ACP 控制台 | 网络、jar、**热过的 pnpm 缓存** | 几分钟 |
| `verify-privilege-split.sh` | **容器里的身份划分**：以 uid 1000 跑，断言 `auth/`、`logs/` 读写被拒，而实例目录与 pnpm store 可写 | 一个跑着的面板容器；脚本不在镜像里，先 `docker cp` 进去，再 `docker exec -u 1000 …` | 几秒 |
| `acp-probe/AcpProbe.java` | 不是独立脚本：`e2e-phase4.sh` 编译并运行它——带会话 Cookie 连 `/ws` 的 ACP 控制台、发一条消息、按结果退出码判定 | 由脚本用 jar 里的 Jetty 编译 | — |

下面两个**不属于**上面那类开发脚本：它们是给终端用户的客户端安装器，从 GitHub Release
下载 `hdsl-tui`、校验 SHA256、装到用户目录，不需要 root，也不需要在仓库里跑。

| 安装器 | 用于 | 说明 |
| --- | --- | --- |
| `install-hdsl-tui.sh` | Linux / macOS | `--version V`、`--prefix DIR`、`--build`（本机 Go 离线编译）、`--dry-run` |
| `install-hdsl-tui.ps1` | Windows（PowerShell 5.1+） | 同上，参数写成 `-Version/-Prefix/-Build/-DryRun` |

## api-smoke.py

```bash
python3 scripts/api-smoke.py            # 自动取 build/libs 里最新的 jar
python3 scripts/api-smoke.py path/to.jar
```

它自己起一个临时面板（临时数据目录、随机端口、HTTP），跑完删掉，不碰真实数据。

> **被强杀会留下一个临时面板**：脚本的 `finally` 只在它自己结束时跑；如果它被 `kill -9`、
> 任务超时或窗口被关掉，那个 `java -jar build/libs/hdsl-web-*.jar` 会继续活着占一个随机端口。
> Windows 上清理：
> `powershell -c "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | ? { $_.CommandLine -match 'build\\\\libs\\\\hdsl-web' } | % { Stop-Process -Id $_.ProcessId -Force }"`
> Linux 上：`pkill -f 'build/libs/hdsl-web-.*\.jar'`（确认那些不是你要留的面板再执行）。
> 判断是不是残留：真面板跑的是**你指定的** jar 路径（通常是 `/data` 或部署目录里的那个），
> 残留跑的是 `build/libs/` 下的构建产物。

## mobile-acceptance.py

```bash
# 起一个有数据的 jar（建议 >= 4 个 dsh 实例 + 1 个 kimi 实例）
HDSL_DATA=/tmp/data HDSL_PORT=3920 HDSL_BIND_HOST=127.0.0.1 \
    HDSL_ADMIN_PASSWORD=testpass123 java -jar build/libs/hdsl-web-<版本>.jar &

cd web && ./node_modules/.bin/vite build && cd ..      # 脚本验的是**当前工作区的 dist**
HDSL_BASE=http://127.0.0.1:3920 HDSL_USER=admin HDSL_PASS=testpass123 \
    HDSL_KIMI_ID=<kimi 实例 id> python3 scripts/mobile-acceptance.py
```

* 环境变量：`HDSL_BASE`（面板地址，默认 `http://127.0.0.1:3920`）、`HDSL_USER`/`HDSL_PASS`、
  `HDSL_KIMI_ID`（**不设就跳过所有与品牌实例相关的检查**）。
* 它自己起一个静态服务（3910）提供 `web/dist`，并把 `/api` 反代到 `HDSL_BASE`——
  所以跑之前**必须重新构建 dist**，否则验的是上一次的前端。
* 那个静态服务必须像真服务器（`StaticServlet.indexHtml`）一样往 `index.html` 里注入
  `<base href="/">` 与 `window.__HDSL_BASE__`：前端用相对路径发 API 请求，缺了它两级深链
  （如 `/instances?view=brands`）会把 `api/…` 解析到错误的父路径，页面看起来"一个实例都没有"。
* 检查分四组：移动端实例列表（首屏行数、零滚动可达）、**原地切换**（URL 仍是 `/instances`、
  `page-root` 不重挂载）、品牌视图（页签行、版本卡折叠高度、实例卡吃高度、行内版本选择器、
  插件弹窗真调后端）、桌面与桌面缩窄（细指针）回归。末尾打印 `PASS/FAIL` 汇总，失败以退出码 1 结束。

> 这台机器上如果**跑脚本的 agent 自己就是 `kimi` 进程**（Kimi Code CLI），别设 `HDSL_KIMI_ID`：
> 面板拉起的 `kimi web` 与 agent 同名，真拉起实例的那两条检查排查成本远大于收益。

## e2e-real.sh / e2e-phase4.sh

```bash
bash scripts/e2e-real.sh      # 13080 端口，临时数据目录 /tmp/hdsl-e2e-data.XXXXXX
bash scripts/e2e-phase4.sh    # 13081 端口，临时数据目录 /tmp/hdsl-p4-data.XXXXXX
```

* 两个脚本都会：临时数据目录 + 一次性管理员口令（`e2e-secret-pass` / `p4-secret-pass`）→
  起 jar（HTTP，绑回环）→ 用 curl 走真实 REST 流程 → 结束时打印 `PASS=n FAIL=n`，
  有失败就以退出码 1 结束，并打服务器日志尾部。**不碰你的生产 `/data`。**
* **需要网络**：`e2e-real.sh` 真装一个 dsh 版本；`e2e-phase4.sh` 还会拉整合包市场索引、
  跑 ACP 控制台。`e2e-phase4.sh` 复用 pnpm 缓存，冷缓存会明显更慢。
* 端口是写死的（13080 / 13081），跑之前确认没被占用；`e2e-phase4.sh` 还会把
  `scripts/acp-probe/AcpProbe.java` 编译到 `/tmp/acpprobe`。
* jar 的挑选方式是**取 `build/libs` 里最新的那个**（按修改时间）。早先按字母序取第一个，
  `build/libs` 里躺着一堆历史版本时会静默拿老 jar 去测——踩过一次，已修。

## 写在这里的约定

* 新加的验证脚本请沿用同一套形状：临时数据目录、一次性口令、`PASS/FAIL` 计数、
  失败退出码非 0、结束时打日志尾部——这样人和 agent 都能一眼看出成没成。
* 脚本一律**只读地**对待仓库：产物写 `/tmp`，别往仓库里丢中间文件。
