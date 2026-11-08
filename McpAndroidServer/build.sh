#!/bin/bash

# MCP Android Server - Build Script
# 使用前请确保已安装 JDK 17 和 Android SDK

set -e

echo "🔧 MCP Android Server 构建脚本"
echo "================================"

# 检查 Java
if ! command -v java &> /dev/null; then
    echo "❌ 未找到 Java，请安装 JDK 17"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ 需要 JDK 17+，当前版本: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java 版本: $(java -version 2>&1 | head -1)"

# 检查 ANDROID_HOME
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  ANDROID_HOME 未设置，尝试默认路径..."
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/usr/local/android-sdk" ]; then
        export ANDROID_HOME="/usr/local/android-sdk"
    else
        echo "❌ 未找到 Android SDK，请设置 ANDROID_HOME"
        exit 1
    fi
fi

echo "✅ Android SDK: $ANDROID_HOME"

# 清理旧构建
echo ""
echo "🧹 清理旧构建..."
./gradlew clean

# 构建 Debug APK
echo ""
echo "🔨 构建 Debug APK..."
./gradlew assembleDebug

# 检查输出
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "✅ 构建成功!"
    echo "📦 APK 位置: $(pwd)/$APK_PATH"
    echo "📏 APK 大小: $(ls -lh $APK_PATH | awk '{print $5}')"
    echo ""
    echo "安装命令: adb install $APK_PATH"
else
    echo ""
    echo "❌ 构建失败，未找到 APK"
    exit 1
fi
