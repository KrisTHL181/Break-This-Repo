# 转码模块（设计稿 / 实施计划）

> 状态：已实现并持续补齐；本文件保留设计与取舍，最新落地范围见下节「实现状态」。
>
> 行为参考：FFmpegFreeUI `6.3.0`（上游）；linux-3fui-wedui `19f4a87`。上游后续变更不自动跟随，按需评估。
>
> 定位：把 FFmpegFreeUI（桌面版）与 linux-3fui-wedui（Linux/网页移植版）的转码能力并入 HDSL-web。
> 两个参考仓库**只作为行为参考，不复制代码**；面板独立实现 Java 领域层和 React 前端，不引入 .NET runtime。
>
> 相关约定：功能默认关闭，关闭时与 MC 一样「当它不存在」；UI 只用面板现有 HMCL/Monet 风格；
> 队列在服务端运行，浏览器只是观察窗。

## 实现状态（2026-11-11）

已落地并能通过 `scripts/transcode-smoke.py`：

- 功能门（`feature.json`、关闭即 404、开发者密码启用）、`transcode:` 配置。
- ffmpeg/ffprobe 定位与能力探测、媒体根浏览、ffprobe 媒体信息。
- 预设存储/内置预设/命令生成/预览，前端六页（概览/准备文件/编码队列/预设/工具/设置）。
- 服务端队列、真实 ffmpeg 执行、进度、日志、暂停/恢复、输出 `.part` 原子改名。
- 失败决策三档、QSV→VAAPI→CPU 降级链、失败弹窗与「应用队列」。
- 合并/混流/抽流工具入队；质量评测 PSNR/SSIM/XPSNR/VMAF 与逐帧曲线。
- GPU 监控（NVIDIA/AMD/Intel sysfs，任务管理器式四图，多卡下拉选择）。
- 元数据/章节/附件、滤镜选择器、自定义参数编辑器、`.3fui` 双向尽力映射。
- ffmpeg 静态包 / apt-dnf / 源码编译的在线安装 UI（SHA-256、进度、日志、取消）。
- 失败策略持久化（写回 server.yaml）与超时链路测试。
- 3FUI 防呆：图片编码器跳过音/字幕、QSV `-low_power 1`、VAAPI 自动注入、
  容器兼容提醒、编码器切换清理 preset/profile/tune、1 帧实测、任务 argv 展示。
- Agent scope `transcode` / `transcode-raw`；容器媒体根、`/dev/dri`/NVIDIA 直通参数与冒烟脚本。
- 2026-11-11 补齐：编码器按 `ffmpeg -encoders` 置灰；滤镜顺序改为裁剪/反交错在缩放前；
  质量模式支持 none/VBR/CBR 与 minrate/maxrate/bufsize；字幕支持 convert（mov_text/ASS/SRT/WebVTT）；
  流控制支持保留其它视频/音频/字幕轨并按索引单独编码；后端在保存/预览/提交前校验编码器与质量、
  容器与编码器、MP4 字幕格式、流控制组合；MP4/AVI/TS/WebM 装不下的附件/章节直接跳过。
- 准备文件支持按文件夹递归加入准备列表并批量入队；准备列表用 `/api/transcode/plan` 显示
  服务器上的绝对输出路径（与真正入队共用命名/防撞逻辑），输出目录留空 = 源文件旁边。
- 编码队列的任务管理：逐项「停止 / 移除 / 重置使用预设」与顶部「清除已完成」。**移除只删队列
  记录与任务日志、不删已生成的输出文件**；运行中 / 已暂停 / 停止中的任务必须先「停止」才能移除或
  重置（`可移除` 对齐 3FUI；`可重置` 比 3FUI 宽一档——**未开始的任务也允许**换预设，便于开跑前
  调整，3FUI 只允许已完成 / 已停止 / 错误）。任务保存的是提交时的预设快照，事后改预设不自动生效；
  `reset-preset` 显式替换快照、清空进度/日志/失败决策、按新容器重新规划输出名（目录沿用任务原来的
  输出目录，旧产物不覆盖，新名自动加序号）并回到 `queued`。队列持久化 `queue.json`、输出名预留
  `reservedOutputs`、决策超时与任务日志同步清理。API：`DELETE /queue/jobs/{id}`、
  `POST /queue/clear`、`POST /queue/jobs/{id}/reset-preset`。工具任务（合并/混流/抽流）没有可替换
  的预设，前端按钮置灰、后端拒绝。
- 队列工作台（对齐 3FUI 的交互模型）：控制条独立成一条，放在使用说明下方、宽度与任务列表一致；
  开始 / 暂停 / 恢复 / 停止 / 移除 / 重置预设全部作用于**勾选集合**，另有全选、选中错误、上移 /
  下移与 `Enter`/`Delete`/`空格` 快捷键。行内只保留「日志」，不再被动作按钮挤掉任务名。
  按钮可用性由服务端下发的 `caps`（`start/pause/resume/stop/remove/reset/reorder`）决定，前端不再
  维护状态机副本。顺序是一等公民：`POST /queue/reorder` 要求**全量 ids**，运行中 / 暂停 / 待决定的
  任务位置钉死；重排只影响派发顺序，不改已定名的输出文件。新接口：
  `POST /queue/jobs/{id}/start`（置顶 + 启动队列，仍受并发上限）、`POST /queue/action`（批量动作，
  逐条返回成败）、`POST /queue/sync-preset`（只改未开始的任务，返回 updated / skipped 计数）。
  队列页订阅 `/ws` 的 `transcode:queue` 事件（30 秒兜底轮询）；详情区显示 ETA、每遍命令（`steps`）
  与编码器切换记录（`switchRecords`）。

尚未完成（后续提交）：

- Windows 的 AMD/Intel GPU 进程表与 Intel 整卡利用率（Linux sysfs/NVIDIA 已有；
  Windows 目前只有 nvidia-smi 摘要，性能页的显卡下拉已支持多卡）。
- Windows PDH 每进程 GPU 引擎计数的解析与前端进程表。
- 3FUI 防呆尚未完全搬：容器兼容现在是“继续/取消”确认 + 后端自动跳过不兼容项，还没有
  “改 MKV / 丢弃不兼容项 / 取消”三选；编码器参数已做组合校验，但 profile/tune/pix_fmt 的
  逐项枚举仍未全部覆盖；编码器故障探测仍是失败后决策，不是启动探测/任务前检查/stderr 三层自动；
  Agent 写操作确认卡与工具目录、应急上传三重校验仍未做。

## Context

HDSL-web 目前是「一个 JVM、一个端口」的控制面：实例、品牌、MC、ZCode、终端、Agent 票据都已就位。
转码不是再加一个独立服务，而是成为面板的一等功能：

- 默认关闭，关闭时：无导航、`/api/transcode/*` 404、不探测、不建数据目录。
- 启用后是一个独立二级页面，左侧纵向列表，右侧工作区，操作习惯参考 3FUI，视觉完全使用面板风格。
- 转码队列在服务端运行：关网页、断网、agent 断开都不影响；第一次手动开始后进入运行态，自动补位。
- Agent 通过面板现有 Bearer + scope + 审计模型调用，不引入第二套鉴权。
- ffmpeg 运行时、媒体根、硬件直通、GPU 监控都按面板现有部署方式补齐。

## Decisions locked（讨论结论）

### 1. 功能门与启用

- 参照 `McFeatureFilter` / `McFeatureState` / `web/mcFeature.ts`：
  - `transcode/feature.json` 持久化 `DISABLED/ENABLED` + 同意记录。
  - `TranscodeFeatureFilter` 挂在 `/api/transcode/*` 之前；关闭时只放行 `GET feature`、`GET terms`、`POST enable`、`POST decline`，其余一律 **404**（不是 403）。
  - 关闭时：前端导航不出现、路由回首页、不建 `<data>/transcode`、不启动 executor、不探测 ffmpeg、不注册 WS 主题、agent 工具目录 404。
- 启用必须过一次开发者密码（复用 `McApiServlet#requireElevation` 的模式，调用 `AuthFilter.elevationSatisfied`）：
  - 未设置开发者密码 → 409 `developer_password_not_set`。
  - 非 HTTPS → 403 `tls_required`（与现有提权约束一致）。
  - 启用前展示一次性告知（ffmpeg/CPU/磁盘/媒体根权限），并做能力探测；探测失败给可读原因，不静默启用。
- 禁用：
  - 有任务在跑 → 409 `transcode_busy`，前端提供「停止全部并禁用」。
  - 不做 MC 那种 1 小时锁（没有孤儿服务器风险）。
- 急停联动：`EmergencyStop` 触发时暂停转码队列并终止 ffmpeg 子进程；恢复后队列保持暂停，由用户显式继续。

### 2. 页面信息架构

主侧栏新增「转码」入口，进入独立二级页面，左侧纵向列表固定为六项：

1. **概览**：拖放/添加入口、环境状态、当前任务、最近完成、警告。
2. **准备文件**：媒体根服务端浏览为主、上传为辅；ffprobe、逐文件预设/输出、兼容性检查、入队。
3. **编码队列**：任务表 + 当前任务大进度条 + 底部 ffmpeg 日志抽屉。
4. **预设**：全量参数面板（schema 驱动），左侧预设库、中间分节表单、右侧粘性命令行预览。
5. **工具**：ffprobe 媒体信息、合并、混流、抽流、质量评测。
6. **设置**：媒体、ffmpeg、编码器、队列、失败策略、Agent、诊断。

- 不设转码「性能」页。GPU 监控放面板 **设置 → 通用 → 系统性能**，扩展现有 `SystemMonitorCard` 的 GPU tab。
- 右侧不要全卡片化：
  - 概览、工具、设置用卡片；
  - 准备文件、编码队列用表格/紧凑行；
  - 预设用分节表单 + 粘性预览；
  - 移动端左侧列表变顶部标签条，卡片纵向堆叠。

### 3. 队列运行模型（服务端自治）

- 调度、ffmpeg 进程、日志、状态全部在服务端；WebSocket 只是观察窗，断线不影响任务。
- **第一次显式开始**后队列进入 `RUNNING`；即使队列跑空也保持运行态。
- 运行态下自动补位：任务完成/失败/取消后，空出的并发槽自动取下一个 `QUEUED`；运行态下新加入的任务也自动开始。
- 用户点「暂停/停止」才改变运行态；`PAUSED` 不再派发新任务。
- 暂停记住原因：等决定造成的 `PAUSED` 会在最后一个 `WAITING_DECISION` 任务被移除 / 取消 / 换预设时自动解除；手动「队列暂停」不会被自动解除。
- 关闭网页、agent 断开、浏览器刷新：任务继续；重新打开时拉 `/api/transcode/queue` 恢复状态、进度、日志。
- 面板重启：排队任务持久化；运行中任务标记「被中断」，按设置自动重新排队或等待用户。
- 默认并发 1，可配；硬件编码器单独限 1 槽；队列上限防灌爆。
- 默认输出不覆盖原文件：先写 `name.part-<id>.<ext>`，成功后原子改名；失败删除或保留 `.failed`（可配）。

### 4. 失败策略（三档，类似开发者守卫）

| 档位 | 失败时 | 10 分钟无人处理 | 队列 |
|---|---|---|---|
| **询问**（默认） | 立即弹窗，任务 `WAITING_DECISION` | 当前任务失败 | **暂停队列**，不再派发；运行中的任务跑完 |
| **超时自动降级** | 弹窗 + 倒计时 | 当前任务降级，排队中同硬件编码器一并降级 | 继续 |
| **完全自动化** | 不阻塞，只发通知说明原因 | 不适用，**立即降级** | 继续 |

- 运行中的任务永远不自动重启；只有用户明确选「降级并应用到队列（含运行中）」才停掉重跑，并提示从 0 开始、进度会丢。
- 自动降级范围：当前失败任务 + 队列中尚未开始的、同一硬件编码器任务；不碰软件编码任务和运行中任务。
- 降级链固定 **QSV → VAAPI → CPU**；每任务最多降一次；降级后再失败记失败，不循环。
- 失败判定：
  - stderr 命中硬件特征（`Invalid FrameType`、`MFX session`、`Device lost`、`No device available` 等）立即终止进程；
  - 退出码 0 但 `ffprobe` 校验无视频帧/时长异常也算失败；
  - 环境失败（磁盘满、媒体根不可写、ffmpeg 消失）不降级，直接暂停队列并提示；
  - 输入/预设错误只失败当前任务；用户取消不触发策略。
- 弹窗四个动作：停止 / 仅此任务降级 / 降级并应用到队列 / **完全自动化**（本次运行或保存为默认）。
- 弹窗在 1 秒内出现；多个任务同时失败合并成一个弹窗；页面未打开时服务端照常计时，回来后队列页显示「因无人处理已失败/已降级」。
- Agent 可针对单个 `WAITING_DECISION` 任务做决策（`transcode` scope），但不能修改失败策略档位；策略只允许登录会话修改。

### 5. QSV

- `ffmpeg -encoders` 里有 `h264_qsv` / `hevc_qsv` 就可见、可用，不默认禁用，不因探测失败隐藏。
- 探测结果只作为提示（已验证/未验证/上次失败）；首次选用给一次注意事项，写清部分 Intel 核显会产出空视频（`Invalid FrameType:0`，应用层不可修），建议 VAAPI。
- 真实失败按上面的三档策略处理；降级链优先 QSV → VAAPI（同硬件换 API）→ CPU。
- 本次运行内连续失败后记住「不稳定」，后续默认降级，但保留「仍然尝试 QSV」；重启重置。

### 6. 全量预设参数面板

- 首发就覆盖 3FUI 参数面板的全部功能分组，使用 **schema 驱动**，不在 React 里手写几百个字段：
  - 容器与输出
  - 视频编码（编码器数据库、质量模式、preset/profile/level/tune、像素格式、10-bit、GOP、线程、硬件参数）
  - 画面帧（分辨率、帧率、裁剪、旋转、去隔行）
  - 色彩/HDR（primaries/transfer/matrix/range）
  - 音频（copy/编码、码率/质量、声道、采样率、流映射）
  - 字幕（copy/烧录/编码、字体、默认/强制）
  - 流与元数据（map、默认轨、章节、附件）
  - 滤镜（启用项、顺序、自定义滤镜）
  - 自定义参数（分阶段附加参数，数组而不是 shell 串）
  - 输出命名/输出位置/覆盖策略
- 每个字段定义：id、分组、label、控件类型、默认值、单位、选项、依赖关系、高级标记、校验、hint。
- 复杂编辑器单独组件：流选择器、滤镜选择、分辨率/帧率、元数据/章节、编码器参数。
- 右侧命令行预览由服务端生成，防抖调用；枚举写错报错并列出合法值，不静默回退。
- 内置预设只读，「另存为」编辑；用户预设 CRUD；导入/导出自有 JSON；`.3fui` 兼容作为可选适配器，不反向约束内部 schema。
- 编码器目录运行时探测：`ffmpeg -encoders`、`ffmpeg -h encoder=X`、`-filters`、`-hwaccels`，配少量展示元数据（中文名、分组、推荐质量映射），不抄静态表。

### 7. 媒体根与 ffmpeg

- 容器固定 `/home/hdsl/video`；`HDSL_VIDEO_DIR` 决定宿主挂载（宿主路径或命名卷）。
- 镜像 `Dockerfile` 建 `/home/hdsl/video`；入口脚本单独 `fix_owner /home/hdsl/video 1000:1000 2775`（不能只靠 `/home/hdsl` 递归）。
- compose 增加 `- "${HDSL_VIDEO_DIR:-hdsl-video}:/home/hdsl/video"` 与 `hdsl-video` 卷；`docker-install.sh` 增加 `--video <宿主路径|volume:名称|none>`，默认命名卷，摘要明确提示 NAS 用户应指到真实视频目录。
- Linux 裸部署：`HDSL_MEDIA_ROOT` 环境变量优先；首次启用转码时向导选一次，默认 `<data>/video`；写进 `server.yaml` 的 `transcode.media_root`；UI 显示生效路径、来源、可写性；改根需开发者提权。
- 所有任务输入/输出、上传/下载、浏览 API 限制在媒体根内；`toRealPath()` + `startsWith`，挡软链逃逸；只读媒体根允许读入但必须另选可写输出目录。
- 转码子进程复用 `Privileges.asAgent()`（`setpriv --reuid=1000 --regid=1000 --init-groups`），输出文件归 uid 1000。

ffmpeg 获取阶梯：

1. 官方镜像默认带 ffmpeg（Debian `libavcodec-extra`）；启动/启用时探测版本、编码器、硬件能力。
2. Linux 缺失：`apt-get install -y ffmpeg` / `dnf install -y ffmpeg` / `zypper install -y ffmpeg-7`；明确告知只对当前容器有效。
3. 无法安装：给分发行版教程 + 「重新检测」。
4. 用户明确选择且检测到编译链：下载源码编译到 `<data>/tools/ffmpeg/<version>`；显示耗时、磁盘占用、完整日志、可取消；这是最后手段。
5. Windows / macOS：只下载静态二进制到 `<data>/tools/ffmpeg/<version>`，SHA256 校验、zip 解压、`ffmpeg`/`ffprobe` 绝对路径写入配置；不自动升级。
6. 下载源：官方/BtbN、gyan.dev、macOS 对应源、可配置镜像；**自定义 URL 放最后**，需开发者提权 + 必须填 SHA256，拦内网/本机地址；保留上一版可回滚。

### 8. 硬件直通

- `docker-install.sh` 增加 `--hw auto|vaapi|nvidia|none`（默认 auto）：
  - `/dev/dri` 存在 → `--device /dev/dri`，传入宿主 `renderD128` 的 GID。
  - `nvidia-smi` 存在且 nvidia-container-toolkit 可用 → `--gpus all`；没有 toolkit 只给教程，不硬加参数。
  - compose 基础文件不直接写 `devices`（没设备会启动失败），由安装脚本生成 override 或只留注释示例。
- 入口脚本按 GID 建组并把 `hdsl` 加入：`groupadd -g "$gid" hdsl-render; usermod -aG hdsl-render hdsl`。原因：`setpriv --init-groups` 会按 `/etc/group` 重置附加组，单纯 `--group-add` 会丢。
- 前端明确声明：
  - VAAPI 推荐；
  - QSV 可见、可用，标「实验性」并给出已知问题；
  - NVENC 需要 NVIDIA 驱动 + container toolkit；
  - 探测失败只提示，不隐藏；任务失败按失败策略处理。
- 能力探测：对候选硬件做 1 帧测试编码，成功才点亮；失败回退 CPU，并显示原因。

### 9. GPU 监控

- 放面板 **设置 → 通用 → 系统性能**，扩展 `SystemMonitorCard` 的 GPU tab（`SettingsPage.tsx` 的 `GeneralTab` 已渲染它）。
- 参照 Windows 任务管理器信息层级，不照抄皮肤：
  - 每 GPU 选择器；
  - 四张折线图：GPU 利用率、编码单元、解码单元、显存使用；
  - 当前值 + 4 分钟历史（沿用现有 240 帧环形缓冲）；
  - 可选进程表：进程/任务、PID、3D、编码、解码、专用显存；ffmpeg 行关联到转码任务。
- 数据来源：
  - Windows：PDH `GPU Engine` / `GPU Adapter Memory`，按 `engtype_3D / VideoEncode / VideoDecode` 解析；
  - Linux NVIDIA：`nvidia-smi --query-gpu`（利用率/编码器/解码器/显存/温度/功耗）+ 每进程显存/`pmon`；
  - Linux AMD：`/sys/class/drm/card*/device/gpu_busy_percent`、VRAM、hwmon + `/proc/<pid>/fdinfo` 的 `drm-engine-*`；
  - Linux Intel：频率 + 每进程 `drm-engine-render/video`，文案写「任务占用」，不冒充整卡；
  - macOS：无免 root 稳定接口，显示不可用。
- 采样 2~5 秒缓存，失败软降级；`nvidia-smi` 不 1 秒 spawn 一次；GPU 页在前台时才做进程扫描。
- 监控不跟转码开关绑定：没有转码也能看 GPU。

### 10. Agent 面

- 新增 scope：
  - `transcode`：读队列/预设/日志、入队、开始/暂停/停止/取消、预设 CRUD、对 `WAITING_DECISION` 做决策；
  - `transcode-raw`：原始参数、命令行编辑、浏览/删除媒体文件、覆盖输出。
- `AgentScopes` 默认拒绝：未显式映射的路由返回 null，`AuthFilter` 直接拒绝；`/i/*`、`/ws/*` 对 Agent 关闭的规则不变。
- 暴露工具目录 `GET /api/transcode/agent/tools`（JSON Schema、write 标记、所需 scope）；执行仍走明确 REST 端点，不做通用 dispatcher。
- Agent 不能启用/禁用功能、不能修改失败策略、不能改媒体根/ffmpeg 路径；这些只允许登录会话（必要时开发者提权）。
- 全局并发/队列上限对 Agent 同样生效；关键操作写 `AuditLog`，记录 `agent:<label>`、scope、grant id。
- 可选：`transcode-raw` 和删除类操作走 `TaskService.WAITING_APPROVAL`，由操作员在面板批准。

### 11. 安全与数据

- 数据布局：
  - `/data/transcode/feature.json`
  - `/data/transcode/settings.json`
  - `/data/transcode/presets/`（内置只读资源 + 用户预设）
  - `/data/transcode/queue.json`
  - `/data/transcode/jobs/`、`logs/`
  - `/data/transcode/tools/ffmpeg/<version>/`
- 媒体根独立挂载；备份需要同时包含 `/data` 与媒体根。
- 命令行一律 argv 数组，绝不拼 shell；自定义参数按数组保存和校验。
- 自定义下载 URL 需开发者提权 + SHA256 + 私网/环回拦截。
- 保留 MIT 来源声明与 NOTICE；面板整体 GPL-3.0，合并后仍 GPL-3.0。

## Approach

独立实现，不引入 .NET runtime，不复制参考仓库代码。

后端新增 `org.jackhuang.hmcl.web.transcode` 包，子包建议：

```
web/transcode/
  TranscodeFeatureState.java       // feature.json / 同意记录
  TranscodeFeatureFilter.java      // 关闭即 404
  TranscodeApiServlet.java         // REST
  TranscodeSettings.java           // server.yaml 中的 transcode.*
  preset/                          // schema、字段、内置预设、导入导出
  encoder/                         // ffmpeg 探测、编码器目录、硬件能力
  command/                         // 从预设生成 argv、命令行预览
  queue/                           // 队列状态机、调度、持久化
  job/                             // 进程、进度、日志、暂停/恢复、产物校验
  probe/                           // ffprobe 媒体信息
  tools/                           // 合并/混流/抽流/质量评测
  agent/                           // 工具目录、scope 映射
  runtime/                         // ffmpeg 路径、下载、安装、编译
```

复用现有基础设施：

- `EventBus` → WS 主题 `transcode:queue`、`transcode:job:<id>`；
- `ProcessTree` / `Privileges.asAgent` → 进程树终止与降权；
- `TaskService` → 下载/安装/编译等一次性长任务（转码队列用自己的调度器，不塞进 TaskService）；
- `SystemMetrics` / `SystemSnapshot` → GPU 监控扩展；
- `AuthFilter` / `DeveloperGuardFilter` / `AgentScopes` / `AuditLog` → 鉴权与审计。

前端新增 `web/src/pages/transcode/` 与 `web/src/components/transcode/`：

- `TranscodeLayout`：二级侧栏 + 右侧内容区；
- 六页：`OverviewPage`、`PreparePage`、`QueuePage`、`PresetsPage`、`ToolsPage`、`SettingsPage`；
- 复用 `Dialog`、`ConfirmDialog`、`PopupMenu`、`Snackbar`、`ProgressBar`、`LogView`、`FileDropInput`、`Ripple`、`i18n`；
- 新增字段引擎 `FieldDef` / `SectionDef` 与控件：select、number、slider、switch、multi-select、stream picker、filter picker；
- `SystemMonitorCard` 的 GPU tab 从占位改成真实图表。

## Key changes

### 后端

1. `src/main/java/org/jackhuang/hmcl/web/transcode/`：
   - feature state/filter/servlet、settings、preset schema、encoder catalog、command builder、queue、job、probe、tools、agent catalog、runtime installer。
2. `src/main/java/org/jackhuang/hmcl/web/server/HdslServer.java`：
   - 注册 `/api/transcode/*`、`TranscodeFeatureFilter`（顺序：Auth → DeveloperGuard → TranscodeFeatureFilter）；
   - 可选的转码停止钩子接入 `EmergencyStop` / shutdown。
3. `src/main/java/org/jackhuang/hmcl/web/developer/AgentScopes.java`：
   - 增加 `transcode`、`transcode-raw` 的 required 映射与 forbidden 规则。
4. `src/main/java/org/jackhuang/hmcl/web/config/ServerConfig*.java`：
   - `transcode.enabled`（或 feature.json 为准）、`transcode.media_root`、`transcode.ffmpeg_path`、`transcode.max_concurrent`、`transcode.max_queue`、`transcode.failure_policy`、`transcode.failure_timeout_minutes`、`transcode.auto_resume` 等。
5. `src/main/java/org/jackhuang/hmcl/web/system/`：
   - `GpuReader` 接口与 Linux/Windows 实现；扩展 `SystemSnapshot.Gpu`；`SystemMetrics` 接入；新增按需进程端点。
6. `src/main/java/org/jackhuang/hmcl/web/transcode/runtime/`：
   - ffmpeg 探测、静态包下载（SHA256）、解压、版本目录、PATH 解析、apt/dnf/zypper 安装提示、源码编译脚本调用。
7. `build.gradle.kts`：
   - 新增转码测试任务/资源；如需，加入小型 ffmpeg fixture 生成脚本（测试用，不提交大文件）。

### 前端

1. `web/src/App.tsx`：新增 `/transcode/*` 路由与功能门守卫。
2. `web/src/components/SideBar.tsx`：新增「转码」顶层入口（仅启用时显示）。
3. `web/src/pages/transcode/`：六页 + `TranscodeLayout`。
4. `web/src/components/transcode/`：
   - `FieldRenderer`、`SectionCard`、`PresetList`、`CommandPreview`、`MediaPicker`、`StreamPickerDialog`、`FilterPickerDialog`、`FailureDecisionDialog`、`JobLogDrawer`、`QueueTable`、`GpuCharts`（或复用 SystemMonitorCard）。
5. `web/src/api.ts` / `web/src/types.ts` / `web/src/i18n.ts`：
   - 转码 API、类型、文案。
6. `web/src/components/SystemMonitorCard.tsx`：
   - GPU tab 接入四张图表与可选进程表；`SystemSnapshot.Gpu` 字段扩展。
7. `web/src/styles/`：
   - 只用现有 Monet 变量和组件类；不新增第二套主题、不搬 LakeUI 毛玻璃。

### 容器与安装

1. `Dockerfile`：
   - `mkdir -p /home/hdsl/video`；
   - 入口脚本 `fix_owner /home/hdsl/video`；
   - Debian 增加 VAAPI/QSV 用户态驱动（`libva2`、`libva-drm2`、`intel-media-va-driver`、`mesa-va-drivers`、`libvpl2`、`libmfx-gen1.2`，按底座条件安装）。
2. `docker-compose.yml`：
   - `HDSL_VIDEO_DIR` 挂载 `/home/hdsl/video`；
   - 注释示例：`devices: /dev/dri`、`group_add`、`--gpus`；
   - `hdsl-video` 命名卷。
3. `docker-install.sh`：
   - `--video`、`--hw`、自动探测 `/dev/dri` 与 `nvidia-smi`、GID 传递、摘要输出、dry-run。
4. `docker-compose.override.yml.example`（或安装脚本生成 override）：
   - 硬件直通示例，避免默认 compose 在无设备时启动失败。
5. `README.md` / `docs/deployment.md`：
   - 媒体根、硬件直通、ffmpeg 获取、GPU 监控、QSV 注意事项。
6. `NOTICE`：
   - 记录 FFmpegFreeUI / linux-3fui-wedui 的 MIT 来源与行为参考说明。

### 文档

- `docs/transcode.md`：本文档。
- `docs/architecture/flows/transcode.md`：Mermaid 流程图（启用门、入队、失败决策、降级、队列自治）；已随模块落地补齐。
- `docs/architecture/README.md`、`context.md`、`full.md`、`backend.md`：已补转码模块、`/home/hdsl/video`、`<data>/transcode`、进程与依赖边界。
- `README.md`：功能实现后更新功能速览、快速开始、媒体根、硬件、GPU 章节。

## Verification

### 单元/集成测试

- 预设 schema 序列化/反序列化、字段默认值、枚举严格校验、内置预设只读。
- 命令生成 golden tests：固定预设 + 输入/输出 → 期望 argv；覆盖硬件/软件、滤镜、流映射、命名。
- 进度解析：`-progress pipe:1` 样例、未知时长、暂停/恢复、异常输出。
- 队列状态机：首次开始、自动补位、暂停/继续、并发、上限、重启恢复、中断任务。
- 失败策略：三档、10 分钟超时（测试用短超时）、运行中任务不重启、降级范围、连续失败保护。
- 路径安全：媒体根内/外、`..`、软链逃逸、只读根。
- 功能门：关闭时 `/api/transcode/*` 404、导航不出现、不建目录；启用需开发者密码 + HTTPS。
- Agent scope：`transcode` / `transcode-raw` 允许/拒绝矩阵；默认拒绝；审计字段。

### 真实 ffmpeg 冒烟

- 小样本 H.264 / H.265 / 音频 copy / 仅重封装 / 图片。
- 软件编码必过；硬件按宿主能力，探测失败跳过并断言「不可用 + 原因」。
- 产物校验：ffprobe 有视频帧；失败注入（fake ffmpeg 输出 QSV 特征 stderr / 空视频）触发弹窗与策略。
- 队列自治：启动任务后关闭 WS/模拟浏览器断开，任务继续；重新连接恢复状态。
- 工具：合并/混流/抽流/质量评测各一条，含取消。

### 容器/部署

- `docker build` / `docker compose up`；健康检查通过。
- `/home/hdsl/video` 挂载、权限、输出归 uid 1000。
- `/dev/dri` 挂载时 VAAPI 1 帧测试通过；无设备时不因 `--device` 启动失败。
- NVIDIA 宿主（如有）验证 `nvidia-smi` 与 NVENC；没有则断言「不可用 + 教程」。
- 自定义 URL 下载：SHA256 正确/错误、私网拦截。
- 禁用功能后容器重启：无转码数据目录、无转码探测日志。

### 前端

- `cd web && npm run typecheck && npm run build` 零错误。
- 六页导航、移动端标签条、预设表单、命令预览、队列日志、失败弹窗、GPU 图表。
- 关闭网页后重新打开，队列/进度/日志恢复；断网重连。
- 失败弹窗在 1 秒内出现；10 分钟超时（测试缩短）按档位处理。

### 命令

```bash
./gradlew test
./gradlew build
cd web && npm run typecheck && npm run build
python3 scripts/api-smoke.py
# 新增：转码接口/队列/失败策略冒烟脚本
```

## Assumptions

- 「失败就停」= 当前任务失败 + 队列暂停；运行中的任务继续跑完，不自动杀掉。
- 「完全自动化」= 立即降级，不等 10 分钟；10 分钟只属于「询问 / 超时自动降级」。
- 「只有第一次」= 第一次显式开始后队列保持运行态；之后新任务、完成任务自动补位，直到用户暂停/停止。
- 准备文件以媒体根服务端浏览为主；上传只作为应急，拖放视为上传，不假装能拿到服务器路径。
- Linux 容器固定 `/home/hdsl/video`；裸部署默认 `<data>/video`，由 `HDSL_MEDIA_ROOT` / 首次启用向导 / `server.yaml` 决定。
- 全量预设 = 覆盖 3FUI 参数面板的功能分组；内部 schema 自定，`.3fui` 导入导出为可选适配器，不是首发阻塞项。
- QSV 默认可见可用，探测只提示不隐藏；失败后按策略降级。
- GPU 监控放面板设置 → 通用 → 系统性能；转码页不设性能页。
- Agent 不能启用/禁用功能、不能改失败策略、不能改媒体根/ffmpeg 路径；只能执行被 scope 允许的操作。
- 不引入 .NET runtime；不复制参考仓库代码；实现以 ffmpeg 行为和面板契约为准。
- 功能总开关在面板设置 → 功能；转码设置页只在启用后存在。
- 文档先写、代码后动：实现等维护者明确指令后再开始。## 附录 A：设计评审待补清单

> 2026-11-10 评审记录。实现前按优先级补齐，或在实现中明确取舍；不阻塞本文档定稿。

### P0（动工前最好定死）

1. **API / WS 契约表**：`/api/transcode/feature|queue|jobs|presets|preview|browse|tools|settings|capabilities|agent` 的 method、请求/响应、错误码；WS 主题、快照/增量、日志 tail、断线重连后的补状态。
2. **功能关闭时如何安装 ffmpeg**：启用向导要能完成下载/安装/编译，或允许「已启用但运行时不可用」的中间态；否则关闭时没有页面可装依赖。
3. **功能状态唯一来源**：`feature.json` 与 `server.yaml` 只能有一个事实来源；另一个只放运行参数。
4. **失败状态机边界**：多会话决策仲裁、`WAITING_DECISION` 时面板重启的倒计时、降级后仍失败、弹窗「停止」的确切语义、完全自动化下无降级路径可用的处理。
5. **资源治理**：每任务线程上限、`nice`/`ionice`、媒体根磁盘阈值、工具任务是否占队列槽、连续失败熔断、面板自身 CPU 保护。
6. **媒体浏览/上传契约**：分页、排序、隐藏文件、权限错误、软链；上传大小上限、`ApiGuardFilter` 流式例外、`.part` 落盘、下载 Range。
7. **「全量预设」验收矩阵**：以 3FUI 哪个版本为基准、字段/功能对应表、明确排除项（AviSynth、ffplay、插件系统、更新器、用户统计、MediaFoundation 等 Windows 专属）。
8. **实施阶段**：文档与契约 → 功能门/运行时探测 → 核心域/命令生成 → 队列/失败策略 → 前端六页 → 全量预设 → 工具 → 硬件/下载/编译 → GPU → Agent → 验收。

### P1（实现中补齐）

9. **数据生命周期**：preset/queue schema 版本与迁移、job 历史保留、日志清理、备份/恢复是否包含媒体根、升级兼容。
10. **Windows / macOS 专项**：静态包架构/校验/解压、macOS quarantine、Defender、服务账号与 UNC/映射盘、VideoToolbox 是否计入硬件编码。
11. **多 GPU / 设备选择**：`vaapi_device` 默认与多卡选择、NVIDIA 多卡、预设是否绑定 GPU。
12. **Agent 细节**：工具目录 schema 版本、每工具 scope/write 标记、每 token 配额、`WAITING_DECISION` 决策端点、审计字段。
13. **安全细则**：不可信预设导入、附加参数 denylist（`-y`、覆盖 `-i`、任意输出路径）、输出软链、ffmpeg CVE、启用告知文本。
14. **可观测性**：任务历史/失败计数、审计事件、完成/失败通知（页面关闭后是否 webhook）、GPU 数据异常日志。
15. **测试矩阵扩展**：并发多任务、磁盘满、只读媒体根、决策中重启、多会话同时操作、Windows/macOS 静态包、schema 迁移、自定义 URL SSRF。

### 已记录但暂不定

- Linux 的 ffmpeg 获取顺序：本文按「镜像自带 → apt/dnf → 教程 → 源码编译」写；静态包下载是否也作为 Linux 可选源，待实现前确认。
- `.3fui` 导入导出为可选适配器，不是首发阻塞项；是否首发送，取决于全量预设的字段映射成本。