#!/usr/bin/env bash
# =============================================================================
# ProxyBird (ProxyPin v1.3.1)  Android 构建脚本
#
# 作用：
#   1. 检查构建环境（Flutter / Dart / Android SDK / JDK）
#   2. 拉取依赖、生成 Android 工程
#   3. 构建 APK（debug / release，可按需选择）
#
# 前置要求（本机需自行安装）：
#   - Flutter SDK 3.x（含 Dart）
#   - Android SDK + 接受 license（ANDROID_HOME）
#   - JDK 17（Flutter 3.22+ 默认需要 JDK 17）
#   - release 签名：android/key.properties 与 android/app/proxypin.keystore（已随源码）
#
# 用法：
#   ./build_android.sh            # 构建 release APK（arm64）
#   ./build_android.sh debug      # 构建 debug APK
#   ./build_android.sh appbundle  # 构建 AAB（上架用）
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")"

MODE="${1:-release}"

log()  { echo -e "\033[1;36m[build]\033[0m $*"; }
warn() { echo -e "\033[1;33m[warn ]\033[0m $*"; }
die()  { echo -e "\033[1;31m[error]\033[0m $*" >&2; exit 1; }

# ---------- 1. 环境检查 ----------
command -v flutter >/dev/null 2>&1 || die "未找到 flutter，请先安装 Flutter SDK 并加入 PATH"
command -v dart   >/dev/null 2>&1 || die "未找到 dart"
[ -n "${ANDROID_HOME:-}" ] || [ -n "${ANDROID_SDK_ROOT:-}" ] || warn "未设置 ANDROID_HOME，构建 Android 可能失败"

log "Flutter 版本："
flutter --version | head -2

# ---------- 2. 获取依赖 ----------
log "获取依赖 flutter pub get ..."
flutter pub get

# ---------- 3. 构建 ----------
case "$MODE" in
  debug)
    log "构建 debug APK ..."
    flutter build apk --debug --split-per-abi
    ;;
  appbundle)
    log "构建 AAB（release）..."
    flutter build appbundle --release
    ;;
  release)
    log "构建 release APK（arm64-v8a）..."
    flutter build apk --release --split-per-abi
    ;;
  *)
    die "未知模式：$MODE（可选 debug / release / appbundle）"
    ;;
esac

# ---------- 4. 输出 ----------
echo
log "构建完成。产物如下："
find build/app/outputs -type f \( -name "*.apk" -o -name "*.aab" \) 2>/dev/null | while read -r f; do
  echo "  - $f ($(du -h "$f" | cut -f1))"
done
