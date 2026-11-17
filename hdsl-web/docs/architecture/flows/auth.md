# 认证、开发者提权与 Agent 票据

这张图覆盖两条进入面板的路：浏览器会话 Cookie，和 AI Agent 的 Bearer 票据。两条路最后都落到 `AuthFilter`，再经过 scope 检查和自杀闸；敏感路由还会再过 `DeveloperGuardFilter`。

```mermaid
sequenceDiagram
  autonumber
  participant B as 浏览器
  participant U as AuthService / UserStore
  participant D as DeveloperAuth / DeveloperElevation
  participant G as AuthFilter
  participant S as AgentScopes / PeerIdentity
  participant H as Servlet
  participant A as AuditLog

  B->>U: POST /api/auth/login
  U->>U: 校验密码 / 用户
  U-->>B: Set-Cookie hdsl_session
  B->>U: GET /api/auth/status
  U-->>B: 已登录 / 首次设置状态
  B->>D: POST /api/developer/elevate（开发者密码）
  D-->>B: 提权会话（绑定当前 session token）
  B->>D: POST /api/developer/agent-token（面板签发）
  D-->>B: Bearer + scopes + expiresAt
  Note over B,D: Agent 也可用 Agent 密码走 POST /api/developer/agent-auth 换票

  B->>G: Authorization: Bearer ...
  G->>D: agentGrant(token)
  alt 未知 / 过期 / 已撤销
    G-->>B: 401 unauthorized
  else 有效
    G->>G: 回环 / 远程会话规则
    alt 远程规则不满足
      G->>A: agent.denied
      G-->>B: 403 agent_loopback_only / agent_remote_session_required
    else 远程规则通过
      G->>S: forbidden(method, path)
      alt 路由对 Agent 关闭
        G->>A: agent.denied（route_closed）
        G-->>B: 403 agent_scope_denied
      else 路由允许
        G->>S: required(method, path)
        alt 缺少 scope
          G->>A: agent.denied（scope_denied）
          G-->>B: 403 agent_scope_denied
        else scope 足够
          G->>S: fatalTarget + PeerIdentity
          alt 目标是调用者自己的实例
            G->>A: self_operation_refused
            G-->>B: 409 self_operation_refused
          else 目标安全
            G->>H: chain.doFilter
            H->>A: 业务审计
            H-->>B: 200
          end
        end
      end
    end
  end
```

## 关键点

- **公开路由**：`POST /api/auth/login`、`GET /api/auth/status`、`POST /api/auth/setup`、`GET /api/health`、`POST /api/developer/agent-auth` 不需要会话；其余 `/api/*`、`/i/*`、`/ws/*` 默认要过门。
- **终端 socket 是例外，但例外本身也是门**：`GET /ws/terminal/*` 在 `AuthFilter` 里被跳过，因为票据只能由终端握手自己校验——一次性 ticket **加上** 签发给它的会话 Cookie 或它点名的 Agent 授权（见 `TerminalWebSocket#onOpen`）。滤波层若先消费 ticket 就会把票作废。
- **两种凭据不互相提权**：Bearer 票据有自己的一套 scope，不会因为同时带了 Cookie 就扩大；Cookie 只作为远程 Agent 的第二个因子。
- **scope 判定**（`AgentScopes.required`）：

  | 路由 | scope |
  |---|---|
  | `GET` / `HEAD`（任何 `/api/*` 的默认落点） | `read` |
  | `/api/system/*` | `system` |
  | `/api/mc/*` | `mc` |
  | `/api/mc/instances/{id}/console/ticket` | `mc-console`（单独一类：拿到 ticket 等于拿到活终端） |
  | `/api/instances*` | `instances`；路径里带 `/plugins` 时是 `plugins` |
  | `/api/zcode/*`、`/api/brands/*`、`/api/tasks*` | `instances` |
  | `/api/plugins/*`、`/api/packs/*` | `plugins` |
  | `/api/accounts*`、`/api/models*`、`/api/settings*`、`/api/network*`、`/api/node*`、`/api/vendors*`、`/api/wallpapers*` | `config` |
  | `/api/transcode/media/*` | `transcode-raw`（能枚举磁盘、拿原始 argv） |
  | 其余 `/api/transcode/*` | `transcode`（队列、预设、工具、质量评测） |
  | `/api/transcode/feature*`、`/api/transcode/settings*`、`/api/transcode/runtime/*` | 无 → 拒绝（开关功能和无人值守策略只归操作员） |
  | 未列到的**非** `GET/HEAD` 路由（如 `POST /api/developer/*`、`/api/exports/*`、`/api/dsh/*` 的写操作） | 无 → 拒绝 |

- **`/i/*` 与 `/ws/*` 对 Agent 关闭**：这两类是浏览器通道（反代页面、事件流），`required` 对它们返回 `null`。唯一的终端通道是上面那条 ticket 路由。
- **默认拒绝**：`AgentScopes.required` 没覆盖的路由返回 `null`，`AuthFilter` 直接按拒绝处理；新路由不会自动对 Agent 开放。
- **自杀闸**：`PeerIdentity` 从内核连接信息推导调用者所在实例；无法定位时按保守策略处理（停、删、接管自己所在实例返回 `409 self_operation_refused`）。
- **开发者提权**：`DeveloperGuardFilter` 加在 `AuthFilter` 之后，负责终端、E-Stop、敏感设置等路由的二次确认；未登录仍应先得到 401，而不是 guard 的拒绝。
- **审计**：放行和拒绝都写 `audit.jsonl`；Agent 请求记录 `agent:<label>`、scopes、grant id，不记录票据本身。
