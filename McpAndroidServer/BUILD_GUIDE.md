# MCP Android Server - 一键构建指南

## 方法一：Android Studio（推荐）

1. 下载 [Android Studio](https://developer.android.com/studio)
2. 打开 Android Studio → `File → Open` → 选择 `McpAndroidServer/` 文件夹
3. 等待 Gradle Sync 完成（首次约 3-5 分钟下载依赖）
4. 菜单 `Build → Build Bundle(s) / APK(s) → Build APK(s)`
5. APK 生成在 `app/build/outputs/apk/debug/app-debug.apk`

## 方法二：命令行

```bash
# 设置环境（Linux/Mac）
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk

# Windows
set JAVA_HOME=C:\path\to\jdk-17
set ANDROID_HOME=C:\path\to\android-sdk

# 构建
cd McpAndroidServer
./gradlew assembleDebug

# APK 位置
ls app/build/outputs/apk/debug/app-debug.apk
```

## 方法三：在线构建

上传项目到 GitHub → 使用 GitHub Actions 自动构建 APK。

## 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 推荐 Eclipse Temurin |
| Android SDK | 34 | 通过 sdkmanager 安装 |
| Gradle | 8.2 | 项目自带 wrapper |
| 磁盘空间 | ~2GB | 包含依赖缓存 |

## 快速安装 Android SDK

```bash
# 下载 command-line tools
curl -O https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip

# 安装
mkdir -p ~/android-sdk/cmdline-tools
unzip commandlinetools-linux-*.zip -d ~/android-sdk/cmdline-tools
mv ~/android-sdk/cmdline-tools/cmdline-tools ~/android-sdk/cmdline-tools/latest

# 设置环境
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# 安装 SDK
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```
