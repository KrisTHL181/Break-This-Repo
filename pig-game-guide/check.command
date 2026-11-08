#!/bin/sh
set -eu
PIG_GUIDE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$PIG_GUIDE_DIR"
if [ ! -x .venv/bin/python ]; then
    printf '%s\n' '还未安装。请先双击同目录的 install.command。' >&2
    exit 1
fi
exec .venv/bin/python doctor.py "$@"
