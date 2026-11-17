# 会话交接：HDSL-web（2026-10-18）

> 本稿接住两段上下文：**2026-10-17 03:28 导出的同一会话前 19 轮**（品牌区「开山」、
> 访问宽容化、v0.4.1 发布、Windows 适配分支成型）与**之后继续的部分**（Windows 真机复验、
> 进程树修复、v0.4.2 / v0.4.3 发布、arm64 镜像）。目标是**不回看会话原文也能接着干**。
>
> **隐私约定（继续遵守）**：真实域名、内网地址、宿主机路径、口令一律不写进仓库；示例统一用
> `dsh.example.com`、`192.0.2.x`、`/srv/...`、`<口令>`。仓库账号 `nas200-timmy` 属公开信息。

## 0. 当前状态

- 分支 `main`，HEAD `9475a6a`；工作树干净，`origin/main` 同步。
- 最新发布 **v0.4.3**（2026-10-18）；`v0.4.2`（10-17）、`v0.4.1`（10-15）、`v0.3.9`（10-15 回填）都在。
  **v0.4.0 从未单独发布**——「开山」并进了 v0.4.1。
- 分支 `feat/windows-bare-deploy` 保留在远端（`657e650`），已 `--no-ff` 合进 main（`e9113c3`）。
- 测试基线：**581 用例 / 0 失败 / 0 错误 / 0 跳过**（Linux，`./gradlew build`）。
- 容器镜像：**从 v0.4.3 起是 `linux/amd64` + `linux/arm64` 双架构 manifest**；
  `0.4.3` / `0.4` / `latest` 同一 index digest
  `sha256:6838b77e77317aad81b1a2ac89ca5e13ae4aeb5a870d60b519a645d298e959e9`。
  `0.4.2` 及更早的标签**仍是 amd64 单架构**（历史产物，不重做）。
- v0.4.2 与 v0.4.3 的 jar **代码完全相同**（v0.4.3 只改交付：CI / Dockerfile / 文档）。
- Windows 测试用的虚拟机**已关机**；测试数据目录仍留在那台机器上（两套，约 1.7 GB）。

## 1. 版本脉络

| 版本 | 内容 | 备注 |
| --- | --- | --- |
| v0.3.9 | 回填发布 | 只为让「曾经的 0.3.9」在远端有标签；docker 有「只有最新标签才移动 latest」的闸门，不会被它拽回去 |
| v0.4.1 | **开山**：品牌区从实例列表底部搬出来，成为同一页内的第二层；实例成为一等对象；HTTPS 打开后裸 IP 同端口 308 | v0.4 不单独发 release；含 dsh 默认标志换鲸鱼、截图改仓库内 webp、`scripts/README.md` |
| v0.4.2 | **Windows 裸部署**：平台识别/运行时布局/派生解析/发行物/文件系统语义逐层补齐；实例就地安装；安装失败回滚；真机复验 | 分支 `feat/windows-bare-deploy`，13 个提交合入 |
| v0.4.3 | **容器镜像加 arm64**：CI 双架构 + QEMU；Dockerfile 的 web/build 阶段钉 `$BUILDPLATFORM` | jar 与 v0.4.2 相同；本版只动交付 |

## 2. v0.4.1 回顾（前 19 轮的重点）

- **品牌区原地成层**：`/instances` 左侧两个分区（dsh 实例 / 第三方品牌），右侧内容区原地切；
  只换查询串（`?view=brands&brand=kimi`），页面不跳转、不重挂载；旧深链 `/brands/<品牌>` 重定向到这里。
- **品牌实例行内操作**：新建、启动/停止、打开、更多（信息/重命名/插件/日志/删除）、
  行内版本选择器（已装直接切；运行中先停再切；未装走品牌级安装后自动切）。
- **访问宽容化**（`ec15903`）：HTTPS 打开后，TLS 端口同端口接明文并 **308** 跳到 https，
  裸 IP 访问不再直接失败；`redirect_same_port` 默认开。
- 其它：dsh 默认标志换 DeepSeek 鲸鱼（`ff707a6`）、README 截图改仓库内相对路径并压 webp
  （`90ce399`、`8679816`）、`scripts/README.md`（`5fd97b6`）、docker `latest` 闸门（`cc219ba`）。

## 3. v0.4.2：Windows 裸部署

排查稿在 `docs/windows-bare-deploy.md`，改动按它的 L1–L7 分层：

- **L2 发行物**：`win-x64/win-arm64`、`.zip`、`win-x64-zip` 索引标签；`platformTag/indexTag/archiveExtension`
  改成接收 `Platform` 的纯函数（Linux 上就能钉住 Windows 的名字）。
- **L3 布局**：`RuntimeLayout`——POSIX `bin/node`、`bin/npm`；Windows 根目录 `node.exe`、`npm.cmd`、
  `npx.cmd`、`pnpm.cmd`。
- **L4 派生**：`util/platform/Executables` 按 `PATH` + `PATHEXT` 补全；`.cmd` **直接交给
  `ProcessBuilder`**，不套 `cmd /c`（实测：裸 `npm` error 2，`npm.cmd` 能跑，无扩展名全路径 error 193）。
  **POSIX 语义原样保留**：仍要求执行位、答案仍是 realpath（`NameRules` 注入，测试钉住）。
- **L5 文件系统**：符号链接能力探测 + 降级（复制 / 文本指针）、`ATOMIC_MOVE` 兜底、
  删除/改名重试统一到 `FileUtils.deleteQuietly`。
- **实例安装不再「先暂存再搬」**（`3d205d6`）：pnpm 直接写在实例最终路径；`dsh.installing` 只是旗标。
  **失败/中断回滚**（`170d05a`）：旧树先挪到 `dsh.previous`，失败就挪回原路径（Windows junction 是
  绝对目标，只有回原路径才重新有效——**不要改成「装到 `dsh.next` 再改名」**）。
- **L6 环境**：pnpm 配置按平台（Windows `auth.ini` registry + `config.yaml` storeDir）；
  ZCode 的 `PATH` 用 `File.pathSeparator`；自定义命令 Windows 走 `cmd /c`；品牌实例同时设 `USERPROFILE`。
- **L7 声明层**：doctor 加 Windows 专检；实例终端在 Windows 明说「不支持」并给替代路径。

## 4. Windows 真机复验（2026-10-17）

环境：真 Windows 10 19045、JDK 21、系统 Node 24.22.0 / pnpm 11.28.6，`java -jar` 裸部署。
数据目录分别放在纯 ASCII 与「中文 + 空格」两处。结果明细在 `docs/windows-bare-deploy.md` §9.2.2，摘要：

| 场景 | 结果 |
| --- | --- |
| 首启 / doctor | ✅ 建号、登录、全绿；横幅 `node v24.22.0, pnpm 11.28.6`（不再误报 MISSING） |
| 托管运行时 | ✅ win-x64 `26.12.0`，根目录 `node.exe`/`pnpm.cmd`，可执行 |
| dsh 实例 | ✅ `0.2.1-rc.1`、626 包就地装；反代 200；停止后端口释放 |
| 品牌（opencode） | ✅ 1.19.3 装/启/反代；停止后 `stopped`、无残留进程 |
| 中文 + 空格路径 | ✅ 装/启/反代通过 |
| HTTPS + 裸 IP | ✅ `http://<ip>:3080/` → 308 `https://<ip>:3080/` |
| 自定义命令 | ✅ `cmd /c` 启动前命令生效 |

**真机挖出的新问题（已修，`27cf4bf`）**：品牌/ZCode 的 `stop()` 只 `destroy()` 面板持有的
`Process`，真正占端口的是它派生的子进程（opencode 实测：面板报 stopped，端口仍 LISTENING）。
新增 `util/ProcessTree`：先收集后代再「先礼后兵」终止；`STOPPING` 旗标改由输出泵消费，
避免「主动停止」被误报成「进程意外退出」。

## 5. v0.4.3：arm64 容器镜像

- `.github/workflows/docker.yml`：加 `docker/setup-qemu-action@v3`，`platforms: linux/amd64,linux/arm64`。
- `Dockerfile`：`web`（npm 构建 SPA）与 `build`（Gradle 产 jar）钉 `--platform=$BUILDPLATFORM`
  ——产物与架构无关；只有 `runtime` 阶段在目标架构跑（apt + 按 `dpkg --print-architecture`
  选 Node tarball）。
- 验证：手动 dispatch 的 `edge` 先验（manifest 含 amd64/arm64，arm64 config `architecture: arm64`）；
  随后 `v0.4.3` 标签一次构建产出 `0.4.3` / `0.4` / `latest`，三者同一 index digest。

## 6. 不要推翻的实现决策

1. **POSIX 不因 Windows 适配而变**：`Executables` 的 POSIX 分支要求 `isRegularFile && isExecutable`
   且返回 `toRealPath()`；`/bin/sh -c`、符号链接、`0600`、inotify 都按原样。
2. **实例就地安装 + `dsh.previous` 回滚**：不要恢复「暂存目录再搬」；也不要「装到 `dsh.next` 再改名」。
3. **停止实例 = 杀整棵进程树**：`ProcessTree` 先取后代再 destroy；品牌/ZCode 的 `stop()` 不得回退成只杀父进程。
4. **`STOPPING` 由输出泵消费**：`stop()` 不提前移除旗标，否则「主动停止」和「读到 EOF」会竞争。
5. **docker 的 `latest` 只由最新 `v*` 标签移动**：回填老标签不得把 latest 拽回去；`latest` 判定用
   `sort -V` 取最大标签。
6. **多架构构建阶段钉 `$BUILDPLATFORM`**：只有 runtime 阶段该在 QEMU 里跑。

## 7. 仓库操作手册

```bash
# 全量测试 / 打包
cd HDSL-web
./gradlew build
./gradlew shadowJar -PreleaseVersion=0.4.4   # 产出 build/libs/hdsl-web-0.4.4.jar

# 发布一套（与 v0.4.2 / v0.4.3 相同）
git tag -a vX.Y.Z -m "..."
git push origin main refs/tags/vX.Y.Z         # 推 main 触发 build；推 tag 触发 docker
# 等两个 run 绿 → 核对 ghcr 三个标签同一 digest → 建 Release：
sha256sum build/libs/hdsl-web-X.Y.Z.jar > SHA256SUMS.txt
gh release create vX.Y.Z --title "..." --notes-file notes.md \
  build/libs/hdsl-web-X.Y.Z.jar SHA256SUMS.txt
gh release download vX.Y.Z -D /tmp/check && (cd /tmp/check && sha256sum -c SHA256SUMS.txt)
```

网络注意事项：`github.com:443` 经常连不上（连接超时 / HTTP2 framing / TLS 中断混合出现）。
推送失败就重试（历史上 1–3 次成功；有一次连续 6 次失败，几分钟后恢复）。`git config http.version HTTP/1.1`
可减少 HTTP2 framing 错误。`gh`（api.github.com）通常仍可用。

## 8. 仍未做 / 下一步

| 项 | 状态 |
| --- | --- |
| Windows 上 ZCode 面板内构建 | ✅ 2026-10-18 真机跑通 v3.14.3；缺的是 Node 24+/pnpm/bsdtar，不是编译器。改动在 `feat/zcode-windows-toolchain`，见 `docs/report-2026-10-18-zcode-windows-build.md` |
| Windows 上 kimi / claude-web 品牌 | ⬜ 未跑（下载与账号成本）；opencode 已跑通，派生机制同一套 |
| >260 字符长路径 | ⬜ 未测；doctor 打印目录长度并在 >120 时提示 |
| pnpm 硬链接 store 跨卷 | ⬜ 未实测 |
| 子进程输出编码混用（`NATIVE_CHARSET` vs UTF-8） | ⬜ 未逐处统一；GB18030 下中文进度无乱码 |
| 面板被强杀后的实例清理 | ⬜ 未做：`kill -9` 面板后 dsh/品牌子进程仍在跑（重启不会回收），再次启动会撞端口 |
| 旧标签的 arm64 | ➖ 不重做：`0.4.2` 及更早只有 amd64；从 `0.4.3` 起双架构 |
| dsh 版本通道 | ℹ️ npm `latest` 仍是 `0.2.0-rc.2`，`0.2.1-rc.1` 在 `next`；面板默认装 `latest` |

Windows 虚拟机已关机；要继续真机项，先开机并在面板里按 `docs/windows-bare-deploy.md` §6.2 重跑 5/6/7。
测试机上留有两套数据目录（纯 ASCII 与中文+空格，约 1.7 GB），不用可直接删。

## 9. 文件地图（按这个顺序读）

1. `CHANGELOG.md` —— v0.4.1 / v0.4.2 / v0.4.3 的完整口径；
2. `docs/windows-bare-deploy.md` —— 排查、分层改法、§9.2.2 真机复验、§9.3 未做项；
3. `docs/brands.md` —— 品牌区（kimi / opencode / claude-web / ZCode）的挂载与启动约定；
4. `docs/deployment.md` —— 容器、TLS、反代、多架构构建；
5. `docs/launcher-orchestration.md` —— 实例安装/启动/反代/日志的编排；
6. `scripts/README.md` —— e2e / 验收脚本。

## 10. 会话原文

本稿对应的完整会话导出在仓库之外的开发机工作目录（`kimi-export-session_-20261017-032825.md`，
前 19 轮，1457 条消息）；之后的真机复验、`ProcessTree`、v0.4.2 / v0.4.3 发布都在同一会话里继续，
关键结论已全部收敛到本稿与 `CHANGELOG.md`。
