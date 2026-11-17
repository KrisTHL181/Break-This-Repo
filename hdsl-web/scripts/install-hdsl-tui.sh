#!/bin/sh
# hdsl-tui 一键安装：从 GitHub Release 下载对应平台的客户端，校验 SHA256 后安装。
#
#   curl -fsSL https://raw.githubusercontent.com/nas200-timmy/HDSL-WEB/main/scripts/install-hdsl-tui.sh | sh
#
# 选项（也可以用环境变量）：
#   --version <版本>   要装的版本，默认取最新 Release（去掉前导 v）
#   --prefix <目录>    安装目录，默认 $HOME/.local/bin
#   --build            用本机 Go 从 Release 的源码包离线编译（源码包内含 vendor/）
#   --dry-run          只打印会做什么，不下载不落盘
#
# 不碰系统目录、不需要 root；卸载就是删掉 <prefix>/hdsl-tui。
set -eu

REPO="${HDSL_TUI_REPO:-nas200-timmy/HDSL-WEB}"
VERSION="${HDSL_TUI_VERSION:-}"
PREFIX="${HDSL_TUI_PREFIX:-$HOME/.local/bin}"
BUILD=0
DRY_RUN=0

usage() {
    cat <<'EOF'
用法：install-hdsl-tui.sh [--version V] [--prefix DIR] [--build] [--dry-run]

  --version V   要安装的版本（默认最新 Release）
  --prefix DIR  安装目录（默认 $HOME/.local/bin）
  --build       用本机 Go 从源码包离线编译
  --dry-run     只打印计划
EOF
    exit "${1:-0}"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --version) [ $# -ge 2 ] || usage 1; VERSION="$2"; shift 2 ;;
        --prefix)  [ $# -ge 2 ] || usage 1; PREFIX="$2"; shift 2 ;;
        --build)   BUILD=1; shift ;;
        --dry-run) DRY_RUN=1; shift ;;
        -h|--help) usage 0 ;;
        *) echo "未知参数：$1" >&2; usage 1 ;;
    esac
done

note() { printf '%s\n' "$*"; }
die() { printf 'install-hdsl-tui: %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------- 平台判断 --

detect_os() {
    case "$(uname -s 2>/dev/null || echo unknown)" in
        Linux)  echo linux ;;
        Darwin) echo darwin ;;
        *) die "不支持的平台：$(uname -s)。Windows 请用 install-hdsl-tui.ps1" ;;
    esac
}

detect_arch() {
    case "$(uname -m 2>/dev/null || echo unknown)" in
        x86_64|amd64)  echo amd64 ;;
        aarch64|arm64) echo arm64 ;;
        *) die "不支持的架构：$(uname -m)" ;;
    esac
}

# ------------------------------------------------------------ 下载与校验 --

# fetch <url> <目标文件>；失败返回 1（调用方决定说什么）
fetch() {
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL --retry 3 -m 300 -o "$2" "$1"
    elif command -v wget >/dev/null 2>&1; then
        wget -q -T 300 -O "$2" "$1"
    else
        die "需要 curl 或 wget"
    fi
}

latest_version() {
    release_json="$(mktemp)"
    fetch "https://api.github.com/repos/$REPO/releases/latest" "$release_json" || {
        rm -f "$release_json"
        die "读不到最新 Release（网络或限流）；请用 --version 指定版本"
    }
    version="$(sed -n 's/.*"tag_name"[[:space:]]*:[[:space:]]*"v\{0,1\}\([^"]*\)".*/\1/p' "$release_json" | head -n 1)"
    rm -f "$release_json"
    [ -n "$version" ] || die "Release 里没有 tag_name，请用 --version 指定"
    echo "$version"
}

# verify_checksum <文件> <SHA256SUMS 文件> <资产名>
verify_checksum() {
    if command -v sha256sum >/dev/null 2>&1; then
        actual="$(sha256sum "$1" | awk '{print $1}')" || return 1
    elif command -v shasum >/dev/null 2>&1; then
        actual="$(shasum -a 256 "$1" | awk '{print $1}')" || return 1
    else
        die "需要 sha256sum 或 shasum 才能校验下载内容"
    fi
    expected="$(awk -v name="$3" '$2 == name { print $1 }' "$2" | head -n 1)"
    [ -n "$expected" ] || die "校验文件 $2 里没有 $3"
    [ "$actual" = "$expected" ] || die "$3 校验失败：期望 $expected，实际 $actual"
    note "  ✓ sha256 $actual"
}

# ---------------------------------------------------------------- 安装 --

# 二进制安装；任何一步失败都返回 1，由调用方给出统一提示
install_binary() {
    tmp="$(mktemp -d)"
    trap 'rm -rf "$tmp"' EXIT INT TERM

    asset="hdsl-tui-$VERSION-$OS-$ARCH"
    note "下载 $asset"
    fetch "$RELEASE_BASE/$asset" "$tmp/$asset" || return 1
    fetch "$RELEASE_BASE/SHA256SUMS-client.txt" "$tmp/SHA256SUMS-client.txt" || return 1
    verify_checksum "$tmp/$asset" "$tmp/SHA256SUMS-client.txt" "$asset" || return 1

    mkdir -p "$PREFIX"
    chmod 755 "$tmp/$asset"
    mv "$tmp/$asset" "$PREFIX/hdsl-tui" || return 1
    trap - EXIT INT TERM
    rm -rf "$tmp"
}

build_from_source() {
    command -v go >/dev/null 2>&1 || die "--build 需要本机 Go"
    tmp="$(mktemp -d)"
    trap 'rm -rf "$tmp"' EXIT INT TERM

    asset="hdsl-tui-$VERSION-src.tar.gz"
    note "下载源码包 $asset（内含 vendor/，编译不取网络模块）"
    fetch "$RELEASE_BASE/$asset" "$tmp/$asset" || die "下载源码包失败"
    fetch "$RELEASE_BASE/SHA256SUMS-client.txt" "$tmp/SHA256SUMS-client.txt" || die "下载校验文件失败"
    verify_checksum "$tmp/$asset" "$tmp/SHA256SUMS-client.txt" "$asset"

    tar -C "$tmp" -xzf "$tmp/$asset"
    dir="$(find "$tmp" -maxdepth 2 -name go.mod -exec dirname {} \; | head -n 1)"
    [ -n "$dir" ] || die "源码包里没找到 go.mod"

    mkdir -p "$PREFIX"
    note "编译中（CGO_ENABLED=0 GOFLAGS=-mod=vendor GOPROXY=off）…"
    ( cd "$dir" && CGO_ENABLED=0 GOFLAGS=-mod=vendor GOPROXY=off \
        go build -trimpath -ldflags "-s -w -X main.version=$VERSION" \
          -o "$PREFIX/hdsl-tui" ./cmd/hdsl-tui ) || die "编译失败"
    trap - EXIT INT TERM
    rm -rf "$tmp"
}

OS="$(detect_os)"
ARCH="$(detect_arch)"

if [ -z "$VERSION" ]; then
    note "查询最新版本…"
    VERSION="$(latest_version)"
fi
VERSION="${VERSION#v}"
RELEASE_BASE="https://github.com/$REPO/releases/download/v$VERSION"

note "hdsl-tui $VERSION · $OS/$ARCH · 安装到 $PREFIX"
if [ "$DRY_RUN" -eq 1 ]; then
    note "（dry-run：以上为计划，未执行）"
    exit 0
fi

if [ "$BUILD" -eq 1 ]; then
    build_from_source
elif ! install_binary; then
    die "拿不到 $VERSION 的 $OS/$ARCH 二进制（版本不存在或该平台没发布）；加 --build 从源码编译，或用 --version 试试别的版本"
fi

note ""
note "已安装：$PREFIX/hdsl-tui"
case ":$PATH:" in
    *":$PREFIX:"*) ;;
    *) note "提示：把 $PREFIX 加进 PATH ——  export PATH=\"$PREFIX:\$PATH\"" ;;
esac
note "用法：hdsl-tui https://panel.example.com"