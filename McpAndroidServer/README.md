# MCP Android Server

一个运行在 Android 设备上的 MCP (Model Context Protocol) 服务器应用，允许 AI 客户端通过 MCP 协议与手机系统进行交互。

## 功能特性

### 核心功能
- **MCP 协议支持**: 完整实现 MCP 2024-11-05 协议规范
- **多传输模式**: 支持 Stdio、SSE、WebSocket 三种传输方式
- **Shizuku 集成**: 通过 Shizuku 获取高权限执行系统命令
- **ADB 支持**: 支持本地和无线 ADB 连接

### 安全特性
- **Token 认证**: 可选的 API Key 认证机制
- **命令过滤**: 危险命令自动阻止
- **权限管理**: 细粒度的权限控制

### UI 界面
- **主页**: 服务器状态、权限状态、设备信息总览
- **设置页**: 传输模式、认证、ADB 等配置
- **日志页**: 实时日志查看、过滤、搜索

## 项目结构

```
app/src/main/java/com/mcpserver/
├── McpApplication.kt          # Application 类
├── ui/                         # UI 层
│   ├── MainActivity.kt
│   ├── theme/                  # 主题定义
│   ├── screens/                # 页面组件
│   └── viewmodel/              # ViewModel
├── mcp/                        # MCP 协议层
│   ├── McpServer.kt            # MCP 服务主类
│   ├── protocol/               # JSON-RPC 协议
│   ├── router/                 # 路由引擎
│   ├── auth/                   # 认证授权
│   └── formatter/              # 结果格式化
├── platform/                   # 平台集成层
│   ├── shizuku/                # Shizuku 集成
│   ├── adb/                    # ADB 命令
│   └── command/                # 执行器
└── model/                      # 数据模型
```

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material3
- **异步**: Kotlin Coroutines + Flow
- **序列化**: Kotlinx Serialization
- **网络**: OkHttp
- **权限**: Shizuku API

## 构建要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 8 或更高版本
- Android SDK 34
- minSdk: 26 (Android 8.0)

## 构建与运行

1. 克隆项目
2. 用 Android Studio 打开项目
3. 等待 Gradle 同步完成
4. 连接设备或启动模拟器
5. 点击 Run 运行应用

## 配置说明

### Shizuku 配置
1. 安装 Shizuku 应用
2. 启动 Shizuku 服务
3. 在应用中授权 Shizuku 权限

### ADB 配置
- 本地 ADB: 需要设备已 root 或通过 Shizuku
- 无线 ADB: 在设置中启用并配置端口

## MCP 协议

本应用实现的 MCP 方法：
- `initialize` - 初始化连接
- `tools/list` - 列出可用工具
- `tools/call` - 调用工具执行命令
- `resources/list` - 列出可用资源
- `resources/read` - 读取资源内容

## 开发计划

- [ ] 完整的 Shizuku AIDL 集成
- [ ] SSE/WebSocket 传输层实现
- [ ] 更多内置工具（文件操作、应用管理等）
- [ ] 日志持久化
- [ ] 多语言支持

## 许可证

MIT License
