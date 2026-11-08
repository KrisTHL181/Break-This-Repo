# MCP Android Server 架构文档

## 概述

MCP Android Server 是一个运行在 Android 设备上的 MCP (Model Context Protocol) 服务器应用，允许 AI 客户端通过 MCP 协议与手机系统进行交互。

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      AI Client (MCP)                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    MCP Protocol Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Transport  │  │   Router    │  │    Auth     │        │
│  │  (Stdio/SSE) │  │   Engine    │  │   Manager   │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Platform Integration                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Shizuku   │  │     ADB     │  │   Command   │        │
│  │   Manager   │  │   Executor  │  │   Executor  │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Android System                           │
└─────────────────────────────────────────────────────────────┘
```

## 模块说明

### 1. UI Layer (`ui/`)

负责用户界面展示和交互。

- **MainActivity.kt**: 应用入口 Activity
- **Navigation.kt**: 导航图定义
- **theme/**: Material3 主题配置
  - Color.kt: 颜色定义
  - Type.kt: 字体配置
  - Theme.kt: 主题组合
- **screens/**: 页面组件
  - HomeScreen.kt: 主页（状态总览）
  - SettingsScreen.kt: 设置页
  - LogScreen.kt: 日志查看页
- **viewmodel/**: ViewModel
  - HomeViewModel.kt: 主页逻辑
  - SettingsViewModel.kt: 设置逻辑
  - LogViewModel.kt: 日志逻辑

### 2. MCP Protocol Layer (`mcp/`)

实现 MCP 协议的核心逻辑。

- **McpServer.kt**: MCP 服务器主类，协调所有组件
- **protocol/**: JSON-RPC 协议
  - JsonRpcMessage.kt: JSON-RPC 消息解析
  - JsonRpcModels.kt: 数据模型定义
  - McpProtocol.kt: MCP 协议处理器
  - Transport.kt: 传输层接口和实现
- **router/**: 路由引擎
  - RouteEngine.kt: 请求路由
  - CommandRegistry.kt: 命令注册表
- **auth/**: 认证授权
  - AuthManager.kt: 认证管理
  - PermissionChecker.kt: 权限检查
- **formatter/**: 结果格式化
  - ResultFormatter.kt: 命令结果格式化

### 3. Platform Integration Layer (`platform/`)

与 Android 系统交互的平台集成层。

- **shizuku/**: Shizuku 集成
  - ShizukuManager.kt: Shizuku 服务管理
  - PermissionHelper.kt: 权限辅助
  - BinderService.kt: Binder 服务
- **adb/**: ADB 命令
  - AdbExecutor.kt: ADB 命令执行
  - AdbCommandParser.kt: ADB 命令解析
  - WirelessAdb.kt: 无线 ADB 支持
- **command/**: 命令执行器
  - CommandExecutor.kt: 执行器接口
  - CommandResult.kt: 执行结果
  - CommandExecutorFactory.kt: 执行器工厂
  - RuntimeCommandExecutor.kt: Runtime 执行器
  - ShizukuCommandExecutor.kt: Shizuku 执行器

### 4. Model Layer (`model/`)

数据模型定义。

- McpRequest.kt: MCP 请求模型
- McpResponse.kt: MCP 响应模型
- McpServerState.kt: 服务器状态模型
- AppSettings.kt: 应用设置模型

## 核心接口

### Transport Interface

```kotlin
interface Transport {
    suspend fun connect()
    suspend fun disconnect()
    fun isConnected(): Boolean
    suspend fun send(message: String)
    fun receive(): Flow<String>
    fun getTransportType(): String
}
```

### CommandExecutor Interface

```kotlin
interface CommandExecutor {
    suspend fun execute(
        command: String,
        workingDirectory: String? = null
    ): CommandResult

    suspend fun isAvailable(): Boolean
    fun getExecutorName(): String
}
```

### RouteHandler Interface

```kotlin
interface RouteHandler {
    suspend fun handle(request: McpRequest): McpResponse
    fun canHandle(method: String): Boolean
}
```

### AuthProvider Interface

```kotlin
interface AuthProvider {
    suspend fun authenticate(token: String): Boolean
    suspend fun checkPermission(action: String): Boolean
    suspend fun generateToken(): String
    fun isConfigured(): Boolean
}
```

## MCP 协议支持

### 支持的方法

- `initialize`: 初始化连接
- `tools/list`: 列出可用工具
- `tools/call`: 调用工具执行命令
- `resources/list`: 列出可用资源
- `resources/read`: 读取资源内容
- `prompts/list`: 列出可用提示
- `prompts/get`: 获取提示内容

### 内置工具

- `shell`: 执行 shell 命令
- `read_file`: 读取文件内容
- `app_info`: 获取应用信息

## 安全机制

1. **Token 认证**: 可选的 API Key 认证
2. **命令过滤**: 危险命令自动阻止
3. **权限检查**: 细粒度的权限控制
4. **沙箱执行**: 命令在受限环境中执行

## 数据流

```
1. AI Client 发送 MCP 请求
2. Transport 接收请求
3. Router 路由到对应 Handler
4. Auth Manager 验证权限
5. Command Registry 执行命令
6. Platform Executor 执行系统命令
7. Result Formatter 格式化结果
8. Transport 发送响应
```

## 配置项

### 服务器配置
- 传输模式: Stdio / SSE / WebSocket
- 监听端口: 8080 (默认)
- 自动启动: 可选

### 安全配置
- 认证启用: 可选
- API Key: 自动生成或手动设置
- 命令权限级别: Standard / Elevated / Root
- 危险命令拦截: 启用/禁用

### Shizuku 配置
- 自动检测: 启用/禁用
- 自动重新授权: 启用/禁用

### 日志配置
- 日志级别: Debug / Info / Warning / Error
- 命令超时: 30秒 (默认)
- 最大并发数: 5 (默认)
- 结果最大长度: 10000 字符 (默认)

## 依赖关系

- Kotlin Coroutines: 异步处理
- Kotlinx Serialization: JSON 序列化
- Jetpack Compose: UI 框架
- Material3: 设计系统
- Shizuku API: 高权限执行
- OkHttp: 网络通信
