#!/bin/bash
set -e

echo "🔧 Building MCP Android Server APK..."

# 设置环境
export JAVA_HOME=/home/faesterh/jdk-17.0.12+7
export ANDROID_HOME=/home/faesterh/android-sdk
export PATH=$JAVA_HOME/bin:$PATH

# 使用 Gradle daemon 模式
echo "📦 Starting Gradle build..."
./gradlew assembleDebug --parallel --build-cache

# 检查输出
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Build successful!"
    echo "📱 APK: $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
    echo "📏 Size: $(ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')"
else
    echo "❌ Build failed"
    exit 1
fi
