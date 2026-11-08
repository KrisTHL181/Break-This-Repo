# MCP Android Server - 构建指南

## 环境要求

- **JDK 17** (推荐 Eclipse Temurin)
- **Android Studio** 或 **Android SDK 34**
- **Gradle 8.2+**

## 快速开始

### 方式一：Android Studio (推荐)

1. 打开 Android Studio
2. 选择 `File → Open`
3. 选择 `/home/faesterh/McpAndroidServer/` 目录
4. 等待 Gradle Sync 完成
5. 点击 `Build → Build Bundle(s) / APK(s) → Build APK(s)`
6. APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`

### 方式二：命令行构建

```bash
# 设置环境变量
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk

# 进入项目目录
cd /home/faesterh/McpAndroidServer

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# APK 输出位置
ls app/build/outputs/apk/debug/app-debug.apk
ls app/build/outputs/apk/release/app-release-unsigned.apk
```

## 签名 Release APK

```bash
# 生成签名密钥
keytool -genkey -v -keystore release-key.jks -alias mcp-server -keyalg RSA -keysize 2048 -validity 10000

# 签名 APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore release-key.jks app/build/outputs/apk/release/app-release-unsigned.apk mcp-server

# 对齐 APK
zipalign -v 4 app-release-unsigned.apk app-release-signed.apk
```

## 项目结构

```
McpAndroidServer/
├── app/
│   ├── build.gradle.kts          # 应用构建配置
│   ├── proguard-rules.pro        # 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml   # 应用清单
│       ├── java/com/mcpserver/   # Kotlin 源码
│       └── res/                  # 资源文件
├── build.gradle.kts              # 项目构建配置
├── settings.gradle.kts           # 项目设置
└── gradle/                       # Gradle Wrapper
```

## 依赖项

- Shizuku API 13.1.5
- Jetpack Compose BOM 2024.01.00
- Material3
- Kotlin Coroutines 1.7.3
- Kotlinx Serialization 1.6.2

## 故障排除

### Gradle Sync 失败
- 确保 JDK 17 已安装
- 检查 `ANDROID_HOME` 环境变量
- 尝试 `File → Invalidate Caches / Restart`

### 构建失败
- 运行 `./gradlew clean` 清理
- 检查 `local.properties` 中的 SDK 路径
- 确保 Android SDK 34 已安装

### APK 安装失败
- 启用 `设置 → 开发者选项 → USB 调试`
- 使用 `adb install app-debug.apk` 安装
