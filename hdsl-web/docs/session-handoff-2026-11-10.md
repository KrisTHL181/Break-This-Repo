# 会话交接：转码模块（2026-11-10）

> 本稿接住 **v0.6.82 之后的转码模块开发**。前稿
> [`2026-10-27`](session-handoff-2026-10-27.md) 的实例、终端、部署结论仍然有效；
> 本稿只写转码相关的新增事实、判过的结论和下次不用再摸的地方。
>
> **分支状态**：转码模块已合入本地 `main`（合并点 `33032362a`），并随 **v0.6.83** 发布；
> `web-shell` 作为开发分支保留。Smoke/容器测试通过后才推 tag 与镜像，发布记录见
> [`CHANGELOG.md`](../CHANGELOG.md) 的 v0.6.83 一节。
>
> **隐私约定（继续遵守）**：真实域名、内网地址、宿主机路径、口令、真实媒体库路径一律不写进仓库。

## 0. 当前状态

- 转码模块的核心链路已经可端到端跑通：功能门 → ffmpeg 探测 → 媒体根浏览 → 预设/命令预览 →
  服务端队列 → 真实 ffmpeg → 进度/日志 → 失败决策/降级 → 工具/质量评测。
- 冒烟脚本 `scripts/transcode-smoke.py` 全绿（真实 ffmpeg：capabilities / media / presets /
  preview / submit / encode / log / extract / quality / 404）。
- 后端全量 `./gradlew test` 绿；前端 `npm run typecheck` + `npm run build` 绿；
  `./gradlew shadowJar` 成功。
- **本机没有 Docker**：镜像没有实际构建。Dockerfile / compose / `docker-install.sh` 只做了
  静态检查；容器冒烟要在 NAS 上跑。
- 设计稿/状态清单在 [`docs/transcode.md`](transcode.md)；架构流程图 `docs/architecture/flows/transcode.md`
  按仓库约定「先改代码，再改图」，**尚未补**。

### 0.1 测试容器实测（2026-11-10，真实片源）

- 测试镜像 `hdsl-web:test-c6faebf`，容器 `hdsl-web-test`（13080），独立数据卷 + 媒体卷；
  通过真实 UI/API 启用（管理员 → 开发者提权 → 条款 → ENABLED），不是脚本预置。
- 通过：启用流程重启保持、媒体根 1000 可写、6 个内置预设、H.264/H.265/VP9/remux/audio-only、
  奇数高度、并发 1、取消无残留、硬件失败降级、路径越界/未知预设 400。
- 真实片源：`[DBD-Raws][缘之空][08][1080P][BDRip][HEVC-10bit][FLAC].mkv`
  （HEVC 10bit + FLAC，25:45，426 MB）→ `mp4-h264` 约 5 分 08 秒（5.04×），
  426 MB → 360 MB；SSIM All 0.978 / PSNR 平均 29.6 dB；左右对比肉眼无明显差别。
- 这轮发现并已修：
  - **P0 输出覆盖**：同输入同预设连续提交共用输出名。现在提交即预留 + 队列持久化重建 +
    工具任务 uniqueOutput；测试断言 `_transcoded/_1/_2`。
  - **P1 无音轨误判**：视频预设 + 视频-only 输入被 `validOutput` 判失败。现在只有输入
    本身有音频才要求输出音频，错误文案区分空文件/无视频流/纯音频/音频缺失。
- 仍成立：转码中重启会把 RUNNING 标成 `interrupted by a panel restart`（不留孤儿进程）；
  `auto_resume=true` 只续排队任务，不续被中断的运行任务。独立 `docker run` 不设
  `HDSL_MEDIA_ROOT` 时默认 `<data>/video`，目录必须预先存在，否则 400；compose 已处理。

## 1. 这一版做了什么

### 1.1 功能门（默认关闭，关闭即 404）

- 状态存 `<data>/transcode/feature.json`，`server.yaml` 的 `transcode:` 只放运行参数；
  复制配置不会把功能打开。
- `TranscodeFeatureFilter` 关闭时只放行 `GET feature/terms`、`POST feature/enable|decline`；
  其余 `/api/transcode/*` 一律 404。
- 启用要求开发者密码 + HTTPS + 一次须知确认；记录条款版本/哈希/接受人/来源/UA。
- 关闭时前端导航不出现、`/transcode` 回首页；有任务时禁用会 409，`force=1` 才停队列并关。

### 1.2 后端：探测、媒体根、预设、队列

- `FfmpegLocator`：`transcode.ffmpeg_path` → `<data>/transcode/tools/ffmpeg/current` → PATH；
  缺 ffmpeg 或 ffprobe 都算不可用。
- `FfmpegProbe`：`-version/-encoders/-hwaccels/-filters`，硬超时、纯函数解析；
  `TranscodeRuntime` 缓存，安装后可 `refresh()`。
- `MediaBrowser`：媒体根内 `toRealPath + startsWith`，挡 `..` 与软链逃逸；列目录/分页/媒体类型标注。
- `TranscodePreset` 是面板自己的 schema（不是 3FUI 的逐行复制）；`PresetStore` 内置只读、
  用户预设原子写；`CommandBuilder` 只产出 argv，不拼 shell；`/preview` 只生成命令。
- `TranscodeQueue`：服务端调度/工作线程/`queue.json`；第一次显式 start 后 RUNNING，
  自动补位；关网页不影响；ffmpeg 走 `Privileges.asAgent`；输出 `.part` → 原子改名；
  暂停/恢复走 POSIX SIGSTOP/SIGCONT（Windows 明确拒绝）。
- 进度用 `-progress pipe:1`；stderr 追加到 `<data>/transcode/logs/<id>.log`；
  EventBus 推 `transcode:queue` / `transcode:job:<id>`。

### 1.3 失败策略与降级

- 三档：`ask`（默认）/ `timeout-auto` / `full-auto`；`TranscodeQueue` 在硬件失败时：
  - full-auto：立即按 QSV→VAAPI→CPU 降级并继续；
  - ask/timeout-auto：任务进 `WAITING_DECISION`、队列暂停、服务端 10 分钟倒计时；
  - timeout-auto 超时自动降级当前 + 排队中同硬件任务；ask 超时失败。
- `POST /queue/jobs/{id}/decision`：stop / fallback / fallback-queue / auto；Agent 可做单任务
  决策，但不能改策略。
- 失败判定：非 0 退出、产物无可用流；`ffprobe` 校验失败也算失败。
- 策略可写回 `server.yaml`（`PUT /api/transcode/settings`），重启仍生效。

### 1.4 前端六页

`/transcode` 二级页：概览 / 准备文件 / 编码队列 / **参数面板** / 工具 / 设置。

- “预设”页面已改为参数面板：左侧参数集、中间分区字段（两列）、右侧粘性
  命令行预览；准备页默认不选预设，必须显式选“参数面板 / 预设”才可入队。

- 概览：ffmpeg/编码器/媒体根/队列状态。
- 准备文件：媒体根服务端浏览、ffprobe 摘要、预设/输出目录、命令预览、入队；
  入队前检查编码器/容器，限制性容器 + 保留项时弹窗提醒改 MKV。
- 编码队列：运行状态、开始/暂停/停止、任务表、当前任务大进度条、任务 argv、ffmpeg 日志；
  `WAITING_DECISION` 卡片四动作。
- 参数面板：schema 驱动表单、内置只读/另存为、自定义参数编辑器、滤镜选择弹窗、导入/导出、命令行预览。
- 工具：媒体信息、合并/混流/抽流、质量评测。
- 设置：ffmpeg 安装、1 帧实测、媒体根、并发/队列、失败策略、编码器矩阵。

### 1.5 工具与质量评测

- `TranscodeTools`：合并（concat + copy）、混流（多输入 + `-map`）、抽流（`0:v/a/s:N` + copy）；
  都走 `queue.submitCommand`，复用日志/进度/取消/原子输出。
- `QualityService`：PSNR/SSIM/XPSNR（stats_file）+ VMAF（JSON），后台单线程、可取消；
  逐帧数组封顶 4000 点；`inf`（完全相同的输入）封顶 100，避免非有限 JSON。
- `/tools/quality` 启动、`/{id}` 查询、`/{id}/cancel` 取消；能力探测没有 `libvmaf` 时
  VMAF 后端拒绝。

### 1.6 预设互通与元数据

- 元数据 title/artist/album/comment/date/genre、`copyAttachments`、`chaptersFile`；
  CommandBuilder 写 `-metadata`、`-map 0:t? -c:t copy`，章节文件作为第二输入 + `-map_chapters 1`。
- `.3fui` 双向「尽力映射」：容器、编码器/preset/profile/threads、分辨率/帧率、音频、
  元数据数组、章节/附件选项；不追求逐字节 round-trip。
- `POST /presets/import`、`GET /presets/{id}/export?format=json|3fui`；前端导入文件、导出 `.3fui`。

### 1.7 防呆（3FUI §7 的一部分）

已补：图片编码器强制 `-an -sn` + `-frames:v 1`；QSV 自动 `-low_power 1`；VAAPI 自动注入；
容器限制提醒；切硬件编码器清理 preset/profile/tune；1 帧实测；任务 argv 展示；
提交即预留输出名（防重复提交覆盖）；视频-only 输入不因音频 copy 预设误判失败。

**仍未全搬**：容器兼容是“继续/取消”确认，不是三选；编码器参数值只做切换清理，没有逐项校验；
故障探测是失败后决策，不是三层自动；Agent 写操作确认卡与工具目录、应急上传三重校验未做。
详见 `docs/transcode.md` 的「实现状态」。

### 1.8 GPU 监控

- `SystemSnapshot.Gpu` 扩展 vendor/backend/encoder/decoder/power；`SystemMetrics` 注入
  `GpuReader`；性能页 GPU tab 四图（利用率/编码/解码/显存）+ 多卡下拉。
- GPU 页已改成四个紧凑指标卡 + 2×2 折线图；没有数据也画空网格。Linux DRM 回退：
  `vendor` 读不到给 unknown 卡、只有 `renderD*` 也列、`/dev/dri` 已挂载但 sysfs
  不可读时给占位卡，便于区分“没显卡”和“没权限读 sysfs”。
- Linux：NVIDIA `nvidia-smi`、AMD sysfs+hwmon、Intel sysfs（频率，整卡利用率没有稳定 sysfs）。
- Windows：目前仅 nvidia-smi 摘要；AMD/Intel 的 PDH 每进程 GPU 引擎计数**未做**。

### 1.9 容器 / 安装 / 硬件

- 容器媒体根固定 `/home/hdsl/video`；`HDSL_VIDEO_DIR` 决定宿主挂载；Dockerfile 建目录、
  入口 `fix_owner`；compose 增加卷与环境变量。
- 入口按 `/dev/dri/renderD128` 的 GID 建组并加入 `hdsl`（`setpriv --init-groups` 会重置附加组）。
- `docker-install.sh` 增加 `--video` / `--hw auto|vaapi|nvidia|none`，自动探测 `/dev/dri` 与
  nvidia-container-toolkit。
- `FfmpegInstaller`：静态包（https + 必填 SHA-256，zip/tar.xz）、apt-get/dnf/zypper、
  源码 configure/make/install；任务有进度/日志/取消，下载拒绝私网/环回并复查重定向。
- **Docker apt 的坑**：`debian:trixie-slim` 第一次 `apt-get update` 时还没有 `ca-certificates`，
  `APT_MIRROR=https://…` 会 CERTIFICATE_VERIFY_FAILED。apt 这一段用 `http://`，或先装
  `ca-certificates`。Java 的 truststore 是另一套：已显式安装 `ca-certificates-java` 并断言
  `/etc/ssl/certs/java/cacerts` 存在；缺它会到运行期才报 PKIX。

### 1.10 Agent

- scope `transcode`（队列/预设/启停/日志/决策）与 `transcode-raw`（媒体根浏览、原始参数、
  破坏性文件操作）。
- `/api/transcode/feature*`、`/api/transcode/settings*`、`/api/transcode/runtime/*` 对 agent 一律
  forbidden：启用/关功能、改策略、装运行时是操作员动作。
- 未做：工具目录 JSON Schema、写操作确认卡、每 token 配额。

## 2. 关键文件

```
src/main/java/org/jackhuang/hmcl/web/transcode/
  TranscodeModule / FeatureState / FeatureFilter / ApiServlet / Paths
  preset/    TranscodePreset, PresetStore, PresetSchema, PresetTransfer, BuiltInPresets
  command/   CommandBuilder
  queue/     TranscodeJob, TranscodeQueue
  media/     MediaBrowser
  runtime/   FfmpegLocator, FfmpegProbe, TranscodeRuntime, FfmpegInstaller, HardwareProbe
  tools/     TranscodeTools
  quality/   QualityJob, QualityService

web/src/pages/TranscodePage.tsx
web/src/pages/transcode/{Overview,Prepare,Queue,Presets,Tools,TranscodeSettings}Page.tsx
web/src/pages/transcode/common.tsx
web/src/components/TranscodeFeatureCard.tsx
web/src/transcodeFeature.ts
web/src/components/SystemMonitorCard.tsx   # GPU tab + 多卡下拉

scripts/transcode-smoke.py
docs/transcode.md
Dockerfile / docker-compose.yml / docker-install.sh
```

## 3. 关键判定（下次别再摸）

- **功能开关唯一来源是 `feature.json`**，不是 `server.yaml`；`transcode.enabled` 不写。
- **媒体根只认一个根**：容器 `/home/hdsl/video`；裸跑 `<data>/video`，由 `HDSL_MEDIA_ROOT` /
  `transcode.media_root` 覆盖。所有路径先 `toRealPath` 再 `startsWith`。
- **队列不塞进 TaskService**：TaskService 是一次性安装/启动任务；转码有自己的调度器，
  只复用 `EventBus` / `ProcessTree` / `Privileges`。
- **输出安全**：`.part-<id>.<ext>` → 成功原子改名；失败删除残片，不让 0 字节产物进入下一次。
- **失败链固定** QSV→VAAPI→CPU（NVENC→CPU），每任务最多降一次；运行中任务不会被自动重启。
- **QSV 可见可用**，但设置页的“测试 1 帧”才是本机能否真编的依据；QSV 的
  `Invalid FrameType:0` 不是靠调参能修的。
- **`.3fui` 是尽力映射**：不承诺逐字节 round-trip；Windows-only/AviSynth/插件等字段不导入。
- **Docker apt 用 http**；Java truststore 是 `ca-certificates-java` + `/etc/ssl/certs/java/cacerts`。

## 4. 验证

```bash
./gradlew test
cd web && npm run typecheck && npm run build
./gradlew shadowJar
python3 scripts/transcode-smoke.py
bash -n docker-install.sh
```

结果：全量测试绿；前端 typecheck/build 绿；shadowJar 成功；冒烟 13 项全 PASS
（health / capabilities / media / presets / preview / submit / encode / log / extract /
quality / 404）。**Docker 镜像未实际构建**（本机没有 docker）。

## 5. 未完成 / 风险

1. **Windows AMD/Intel GPU 进程表 + Intel 整卡利用率**：Linux sysfs/NVIDIA 已有；Windows 只有
   nvidia-smi 摘要。需要 PDH `GPU Engine` / `GPU Process Memory` 解析和前端进程表。
2. **容器未实测**：镜像构建、`/home/hdsl/video` 权限、`/dev/dri`/`--gpus`、ffmpeg 安装路径都要
   在 NAS 上跑一遍；Dockerfile 只做了静态检查。
3. **3FUI 防呆未全搬**：见 §1.7 / `docs/transcode.md` 剩余项。
4. **硬件编码未实测**：VAAPI/QSV/NVENC 只做了能力探测和 1 帧接口；QSV 的已知平台故障仍以
   实测为准。
5. **VMAF 未实测**：本机 ffmpeg 没有 libvmaf；代码路径和解析已有，但没跑过。
6. **架构图已补**：`docs/architecture/flows/transcode.md` 及 `context` / `full` / `backend` 已按落地代码更新（2026-11-11）。
7. **已合并到本地 `main`，未 push**：合并点 `33032362a`，`main` 领先 `origin/main` 57 个提交；
   等容器/人肉测试通过后再推 GitHub。

## 6. 下次入口

- 先读 [`docs/transcode.md`](transcode.md) 的「实现状态」和「决策锁定」。
- 冒烟：`python3 scripts/transcode-smoke.py`（临时数据目录 + 真实 ffmpeg，不碰真实数据）。
- 容器：按 `docker-install.sh --video <媒体库> --hw auto` 部署；注意 apt 镜像用 **http://**。
- 合并纪律：人肉测试通过后才 `web-shell → main`；每个功能一个 commit，消息写清
  “为什么 / 改了什么 / 怎么验”。