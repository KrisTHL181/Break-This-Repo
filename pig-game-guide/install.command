#!/bin/sh
# Creates an isolated environment; never uses sudo or installs into system Python.
set -eu
PIG_GUIDE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$PIG_GUIDE_DIR"
fail() { printf '%s\n' "$1" >&2; exit 1; }
[ "$(uname -s)" = Darwin ] || fail '需要 macOS 15 或更新系统；实时工具不支持 Windows / Linux。'
PIG_GUIDE_INTERPRETER=${PIG_GUIDE_PYTHON:-}
if [ -z "$PIG_GUIDE_INTERPRETER" ]; then
    for candidate in python3.12 python3.13 python3.14 python3; do
        if command -v "$candidate" >/dev/null 2>&1 && "$candidate" -c 'import sys; sys.exit(not ((3,12) <= sys.version_info[:2] < (3,15)))' 2>/dev/null; then
            PIG_GUIDE_INTERPRETER=$(command -v "$candidate")
            break
        fi
    done
fi
[ -n "$PIG_GUIDE_INTERPRETER" ] || fail '请先从 https://www.python.org/downloads/macos/ 安装 Python 3.12–3.14，再运行本文件。'
"$PIG_GUIDE_INTERPRETER" -c 'from runtime import platform_error; import sys; error=platform_error(); print(error or "系统与 Python 版本符合要求"); sys.exit(bool(error))'
if [ ! -d .venv ]; then
    "$PIG_GUIDE_INTERPRETER" -m venv .venv
fi
[ -x .venv/bin/python ] || fail '.venv 不完整或已移动。请将 .venv 改名为 .venv.old，再重新安装。'
.venv/bin/python -c 'import sys; from runtime import platform_error; error=platform_error(); print(error or "使用独立虚拟环境"); sys.exit(bool(error) or sys.prefix == sys.base_prefix)'
.venv/bin/python -m pip --isolated install --disable-pip-version-check -r requirements.txt
.venv/bin/python doctor.py --offline
printf '\n安装完成。先阅读 README.md 中的权限设置，再双击 start.command。\n'
