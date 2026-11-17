# 转码：功能门、队列与失败降级

这张图对应 `TranscodeApiServlet` → `TranscodeQueue` → `ffmpeg` 的主路径，重点是**三道门、服务端队列、硬件失败的决策与降级链**。预设/命令生成、工具与质量评测只画到与队列共用的那部分，字段级细节看 [docs/transcode.md](../../transcode.md)。

## 启用与提交

```mermaid
sequenceDiagram
  autonumber
  participant B as 浏览器 / Agent
  participant A as AuthFilter / AgentScopes
  participant G as DeveloperGuardFilter
  participant F as TranscodeFeatureFilter
  participant S as TranscodeApiServlet
  participant Q as TranscodeQueue
  participant R as TranscodeRuntime / FfmpegLocator
  participant P as ffmpeg
  participant E as EventBus / WS

  B->>A: POST /api/transcode/feature/enable（首次）
  A->>A: 会话 Cookie 或 Agent 票据 + scope
  A->>G: 放行
  G->>G: E-Stop + 开发者提权
  G->>F: 放行
  F->>F: 关闭时 404（只放行 feature / terms / enable / decline）
  F->>S: 放行
  S->>S: requireElevation：HTTPS + 已设开发者密码
  S->>S: 校验须知：termsVersion / 文本哈希 / sawDeclineOption
  S->>S: 写 <data>/transcode/feature.json + 审计
  S->>Q: queue.start()

  B->>S: POST /api/transcode/queue/jobs {input, outputDir, preset}
  S->>Q: submit(...)
  Q->>Q: 媒体根解析 + 越界检查 + max_queue 检查
  Q->>R: CommandBuilder.buildPasses（提交时就构建一次）
  Q->>Q: 预留输出名（reservedOutputs），queue.json 落盘
  Q-->>B: 任务快照（queued）

  B->>S: POST /api/transcode/queue/start
  S->>Q: startQueue() → RUNNING
  Q->>R: info()（配置 → PATH → <data>/transcode/tools/ffmpeg/current）
  Q->>P: ProcessBuilder(argv)，先写 .part
  loop 进度（-progress 键值，500ms 节流）
    P-->>Q: out_time_us / speed / fps / bitrate / frame
    Q-->>E: transcode-job
    E-->>B: /ws 事件
  end
  P-->>Q: exit 0
  Q->>Q: 产物校验（容器 / 轨道 / 时长）→ .part 原子改名
  Q->>Q: 释放预留，state=done
  Q-->>E: transcode-queue / transcode-job
```

## 硬件失败：决策与降级

```mermaid
sequenceDiagram
  autonumber
  participant B as 浏览器 / Agent
  participant S as TranscodeApiServlet
  participant Q as TranscodeQueue
  participant P as ffmpeg
  participant E as EventBus / WS

  P-->>Q: exit != 0 或产物校验失败
  Q->>Q: 删除 .part 与 pass 日志
  Q->>Q: fallbackEncoder(当前编码器)
  alt 还有下一档且本任务没降级过
    alt failurePolicy = full-auto
      Q->>Q: 立即 applyFallback，重新入队（含同族排队任务）
      Q->>P: 用降级后的参数重跑
    else failurePolicy = ask（默认）/ timeout-auto
      Q->>Q: state=waiting_decision，暂停调度
      Q->>Q: 起 10 分钟计时（failureTimeoutMinutes）
      Q-->>B: 任务错误 + decisionDeadline
      alt 操作员决定
        B->>S: POST /queue/jobs/{id}/decide {stop | fallback | fallback-queue | auto}
        S->>Q: decide(...)
        Q->>Q: stop → failed + 队列暂停；其余 → 降级并恢复派发
      else 超时
        Q->>Q: timeout-auto → 自动降级（含同族排队任务）
        Q->>Q: ask → failed（decision timed out），队列暂停
      end
    end
  else 没有下一档 / 已经降级过
    Q->>Q: failed（错误 + 日志尾巴）
  end

  alt 操作员取消
    B->>S: POST /queue/jobs/{id}/cancel
    S->>Q: cancel → 终止进程树 + 删 .part + 释放预留
  end
```

降级链是 QSV → VAAPI → 软件编码；但**每个任务只自动走一格**——`fallbackAttempted` 置位后不再自动降级，避免硬件坏了无限重试。要继续往下掉，由操作员 retry（重试会清零这个标记），或直接在参数面板换编码器。

```mermaid
flowchart LR
  qsv["QSV<br/>h264_qsv · hevc_qsv · vp9_qsv"] --> vaapi["VAAPI<br/>同编码族的 _vaapi"]
  av1qsv["av1_qsv"] --> av1sw["libaom-av1"]
  vaapi --> sw["软件编码<br/>libx264 · libx265 · libvpx-vp9 · libsvtav1"]
  nvenc["NVENC<br/>h264 / hevc / av1 _nvenc"] --> sw
  amf["AMF · D3D12VA · Vulkan"] --> sw
```

## 关键点

- **三道门的顺序不能换**：`AuthFilter`（会话 / Agent 票据 + scope）→ `DeveloperGuardFilter`（E-Stop、启用的提权）→ `TranscodeFeatureFilter`（关闭时 404）。启用要 HTTPS + 已设开发者密码；`feature/*`、`settings/*`、`runtime/*` 对 Agent 票据一律禁止，队列 / 预设 / 工具要 `transcode` scope，媒体浏览（能枚举磁盘）与自定义命令的原始 argv 要 `transcode-raw`。
- **功能关闭 = 不存在**：状态在 `<data>/transcode/feature.json`，不写 `server.yaml`；关闭时除启用流程外 `/api/transcode/*` 一律 **404**（不是 403），同时 `queue.stop()` 终止子进程、把未完成任务标 `cancelled`。
- **队列在服务端**：`runState` 只有 `IDLE / RUNNING / PAUSED`；`max_concurrent`（默认 1）控制同时编码数，`max_queue`（默认 20）在提交时拒绝。浏览器关掉、Agent 断开都不影响已入队的任务。
- **重启语义**：运行中的任务在重启后标 `failed: interrupted`（不会留下孤儿 ffmpeg），排队任务保留；`autoResume` 只决定重启后是否继续派发，不是断点续跑。
- **输出不互相覆盖**：提交时就预留输出名，同一个输入同预设连续提交会得到不同文件名；完成时 `.part` 原子改名，失败/取消删 `.part` 并释放预留，retry 重新预留。
- **队列记录可删、磁盘产物不删**：`DELETE /queue/jobs/{id}` 移除单条记录与它的日志，`POST /queue/clear` 批量清掉 `done / failed / cancelled`；两者都同步释放输出名预留、取消决策超时、重写 `queue.json`，但**不删已生成的输出文件**。运行 / 暂停 / 停止中的任务必须先 `cancel`（后端 409、前端按钮置灰），与 3FUI 的 `可移除` 一致。
- **改预设要显式重置**：任务保存的是提交时的预设快照，事后编辑预设不自动生效。`POST /queue/jobs/{id}/reset-preset` 替换快照、清空进度/日志/降级与决策状态、按新容器重新规划输出名（目录沿用任务的，旧产物保留，新名加序号）并回到 `queued`；工具任务（合并/混流/抽流）没有可替换的预设，会被拒绝。`可重置` 比 3FUI 宽一档：未开始的任务也允许换预设。
- **队列是工作台，不是列表**：动词作用于**勾选集合**（`POST /queue/action` 逐条返回成败，部分失败仍 200），顺序是一等公民（`POST /queue/reorder` 要求全量 ids，运行中 / 暂停 / 待决定的任务位置钉死），**能力位由服务端下发**（`caps`）——前端自己推断状态会维护出一份必然漂移的状态机。页面订阅 `/ws` 的 `transcode:queue` 事件，1 秒轮询退成 30 秒兜底。
- **失败分三档**（`transcode.failure_policy`）：`ask`（默认，等人决定）、`timeout-auto`（10 分钟没人管就自动降级，`failureTimeoutMinutes` 可调）、`full-auto`（立即降级）。降级动作可以只影响当前任务，也可以顺带把同编码器族的**排队**任务一起改；`stop` 会把队列停在 `PAUSED`，让人先看日志。
- **暂停记住原因**：等决定造成的暂停会在最后一个待决定任务被移除 / 取消 / 换预设时自动解除，剩余排队任务继续跑；`队列暂停` 是人为决定，不会被自动解除。
- **硬件失败先实测再报错**：能力探测走 `capabilities/probe`，用 1 帧真实编码试 QSV/VAAPI/NVENC，而不是只看 `-encoders` 列表；QSV 带 `-low_power 1`，VAAPI 注入设备参数。拿不到编码器就直接标不可用，不静默换成软件编码。
- **队列也跑工具**：合并 / 混流 / 抽流通过 `submitCommand` 入同一个队列，拿到同样的日志、进度、取消与 `.part` 改名；质量评测（PSNR / SSIM / XPSNR / VMAF，VMAF 需要 `libvmaf`）在 `tools/quality` 下单独起 job，逐帧曲线由前端画。
- **ffmpeg 由面板自己装**：`FfmpegInstaller` 支持静态包（校验 SHA-256，可读 `.sha256` / `SHA256SUMS` 侧车）、apt / dnf / zypper、源码 `configure && make`，产物落在 `<data>/transcode/tools/ffmpeg/current`；装完 `runtime.refresh()` 重新探测，不需要重启面板。
- **媒体根不在 `/data`**：`HDSL_MEDIA_ROOT` / `transcode.media_root`，裸部署回退 `<data>/video`；容器镜像指向 `/home/hdsl/video`（宿主影音库的挂载点）。所有输入输出路径都经 `MediaBrowser` 归一化并拒绝越界。
