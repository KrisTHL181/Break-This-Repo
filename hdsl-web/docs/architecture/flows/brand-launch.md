# 品牌实例启动、反代与终端

这张图覆盖 `BrandApiServlet` 的启动路径，以及启动成功后的 `/i/{id}/` 反代和 `/ws/terminal/{ticket}` 终端。PI WEB 的双进程、Hermes 的 gateway、Octop 的慢启动都在 `BrandRuntime` 里分支处理。

```mermaid
sequenceDiagram
  autonumber
  participant B as 浏览器
  participant A as AuthFilter + AgentScopes
  participant V as BrandApiServlet
  participant R as BrandRuntime
  participant W as wrapper / 进程
  participant P as 健康探测
  participant X as InstanceProxyServlet
  participant T as TerminalManager
  participant E as EventBus / WS

  B->>A: POST /api/brands/{brand}/instances/{id}/launch
  A->>A: 会话 / Agent 票据 + scope + 自杀闸
  A->>V: 放行
  V->>V: 检查 terminal-only / 安装包 / web 模式
  V->>R: launch(brand, manager, instance, host, packageDir)
  R->>R: handInstanceDirsToAgent / 解析 Node / 生成 plan
  R->>W: ProcessBuilder.start()
  loop 就绪探测，最长 60–180 秒
    alt PI WEB
      R->>P: GET /health（sessiond）+ /api/pi-web/health（web）
    else Kimi / OpenCode / Claude Web
      R->>P: 读 stdout 的本地端口行
    else Octop
      R->>P: GET /api/health
    else Hermes
      R->>P: GET /api/status
    end
    P-->>R: 200 / 端口 / 超时
  end

  alt 就绪
    R->>R: PORTS.put + instance.lastPort
    R-->>V: RUNNING
    V-->>B: 200 {state: RUNNING, port}
  else 进程退出或超时
    R->>W: ProcessTree.terminate（整棵进程树）
    R->>R: reclaimPiWebState（PI WEB 旧 sessiond）
    R-->>V: ERROR + 日志尾部
    V-->>B: 错误状态
  end

  B->>X: GET /i/{id}/
  X->>X: InstanceProxyTargets.resolve(id)
  alt dsh / 品牌 / ZCode 正在运行
    X->>W: HTTP / WebSocket 反代到 loopback
    W-->>X: 响应
    X-->>B: 页面 / WS 帧
  else 实例不存在或未运行
    X-->>B: 404 / 502
  end

  B->>T: POST /api/brands/{brand}/instances/{id}/terminal
  T-->>B: 一次性 ticket
  B->>T: WS /ws/terminal/{ticket}
  T->>W: PTY attach
  W-->>T: 终端字节流
  T-->>B: 终端字节流
```

## 关键点

- **PI WEB 是双进程**：wrapper 先起 `sessiond`，等 `/health` 再起 web；面板的健康探测必须走 **HTTP/1.1**，否则 Fastify 会对 JDK 默认的 `Upgrade: h2c` 回 400，实例会被误判为没起来。
- **失败清理**：就绪失败不会只杀 wrapper，而是 `ProcessTree.terminate` 收掉整棵树；PI WEB 还会在下次启动前清理 `sessiond-owner.json` 指向的旧 daemon，避免 `SessiondStateOwnershipConflictError`。
- **反代目标**：`/i/{id}/` 只服务 dsh、品牌和 ZCode；Minecraft 没有 `/i/` 页面，控制台走 `/ws/terminal/{ticket}`。
- **终端**：浏览器先拿一次性 ticket，再升级 WebSocket；PTY 在面板进程内，浏览器只是查看器。
- **自杀闸**：Agent 票据如果目标是调用者自己所在的实例，`PeerIdentity` 会拒绝并返回 `409 self_operation_refused`，避免把发请求的进程杀掉。
- **Hermes 互斥**：终端和网页共用同一个 gateway 时，`BrandApiServlet` 会先关闭终端 / 释放 detached gateway，再启动网页模式。
