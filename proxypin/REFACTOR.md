# ProxyBird 重构与增强说明（基于 ProxyPin v1.3.1）

> 本仓库即 ProxyPin v1.3.1（原 HttpCanary 的 Flutter 重写），与官方最新公开版基本同代。
> 本次交付为 **源码级修改 + 构建脚本**，未做任何二进制改动。

---

## 一、版本与"升级"结论

- 当前源码版本：**1.3.1+36**，即官方最新公开发布版（2026-09 前后），**无需再升级**。
- **MCP（Model Context Protocol）已内置**于 `lib/network/mcp/`，本次把桌面端遗漏的接线补齐，使其与移动端一致可自动启动。

---

## 二、本次新增/修改总览

| 模块 | 文件 | 说明 |
|---|---|---|
| **Root 辅助（自动解决无网络）** | `android/.../root/RootManager.kt`、`plugin/RootCertPlugin.kt`、`lib/native/root_cert.dart`、`lib/ui/root_helper/root_helper_page.dart` | 已 root 设备一键装系统 CA + 绕过证书校验 |
| **UDP / TCP 完整抓取 + 可重放** | `android/.../vpn/RawFlowReporter.kt`、`plugin/RawFlowPlugin.kt`、`ConnectionHandler.kt`、`socket/SocketChannelReader.java`、`Connection.kt`、`lib/network/raw_flow.dart`、`lib/ui/raw_flow/raw_flow_page.dart` | 捕获非 HTTP 的 TCP / 全部 UDP，支持重放 |
| **MCP 接线补齐** | `lib/ui/desktop/desktop.dart` | 桌面端自动启动 MCP Server |
| **构建脚本** | `build_android.sh` | 一键构建 Android APK/AAB |

---

## 三、问题一：抓包后"无网络"（自动解决）

### 根因
抓 HTTPS 时，ProxyPin 用自签 CA 做中间人。很多 App（`targetSdk>=24`）**默认只信任系统证书**，不信任用户证书；另一些 App 做了 **SSL Pinning**。于是走代理后校验失败，表现就是"无网络/证书无效"。

### 解决（已 root 设备）
1. **自动安装系统证书**：`RootManager.installSystemCa()` 通过 `su` 把 ProxyPin CA 写入系统证书库（Android≤13 走 `/system/etc/security/cacerts`，Android14+ 走 APEX 或 Magisk 模块），解决"只信任系统证书"的 App。
2. **Magisk 模块**：`installViaMagisk()` / `exportMagiskModuleZip()` 生成并部署 Magisk 模块（装完重启生效），是 Android 14+ 最稳妥的方式。
3. **绕过 SSL Pinning**：`detectPinningModules()` 检测设备上的 LSPosed / TrustMeAlready / 算法助手 等模块，UI 上引导启用，解决做证书固定的 App。

入口：**工具箱 → Root辅助**。

> 说明：绕过 SSL Pinning 需要设备已 root 且装 LSPosed(Xposed)；未 root 只能装用户证书 + 使用引导。

---

## 四、问题二：抓不了 UDP / TCP（完整抓取 + 可重放）

### 根因
原 VPN 层对 **HTTP/TLS 走本地代理抓包**，但对**非 HTTP 的 TCP 和所有 UDP 直接转发到目标**（保证有网），不记录。所以列表里看不到 UDP/TCP 流量。

### 解决
在原生 VPN 层新增 `RawFlowReporter`，对以下流量**双向上报**到 Dart：

| 协议 | 上行（客户端→服务器） | 下行（服务器→客户端） |
|---|---|---|
| UDP | `ConnectionHandler.handleUDPPacket` | `SocketChannelReader.readUDP` |
| 非 HTTP TCP | `ConnectionHandler`（`Connection.proxied=false` 时） | `SocketChannelReader.sendToRequester` |

- 仍保持**原样转发**，保证 App 不断网。
- 载荷截断至 1024 字节，base64 上报，避免事件过大。
- Dart 侧 `RawFlowStore` 集中存储，最多保留 2000 条。

### 重放
`RawFlowPlugin` 提供 `replayUdp` / `replayTcp`：
- **UDP**：通过真实网络（`protect` 防回环）重发该报文。
- **TCP**：建立真实网络 TCP 连接并发送载荷，读一次回包后关闭。

入口：**工具箱 → UDP/TCP抓包**。抓包列表实时刷新，点条目查看 Hex/ASCII，可重放。

> 注意：TCP 重放受对端协议/端口状态影响，对需要完整握手或加密的协议（如 TLS 内的业务）不一定成功，属协议特性而非缺陷。

---

## 五、重构方向（按"我自己定"执行）

在新增能力的同时，遵循了以下重构约束：
- **分层清晰**：原生新增 `root`（系统能力）、`plugin`（Flutter 通道）、`vpn`（抓包）三块，职责分离。
- **线程安全**：`RawFlowReporter` 处理 VPN/NIO 多线程上报（主线程投递 + 有界缓冲）。
- **跨语言一致性**：统一事件 JSON 结构，Dart 侧 `RawFlow.fromMap` 与原生 `RawFlowReporter.report` 一一对应。
- **不破坏原逻辑**：所有新增均为增量钩子（非 HTTP 才抓、proxyPassDomains 绕过逻辑保持不变）。

---

## 六、构建

见 `build_android.sh`（本机需 Flutter SDK + Android SDK + JDK 17）。

```bash
./build_android.sh          # release APK
./build_android.sh debug    # debug APK
./build_android.sh appbundle # AAB
```

---

## 七、尚未完成 / 局限

- 本机无 Flutter/Dart/Android SDK，**未做编译与真机验证**；请在你的环境跑 `build_android.sh` 验证。
- 绕过 SSL Pinning 的"完全自动化"受限于 Android 安全模型（需 root/LSPosed 用户授权）。
- 桌面端（Windows/macOS/Linux）无 VPN 原生层，UDP/TCP 原始流抓取仅 Android 生效。
