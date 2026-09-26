#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TARGET="$SCRIPT_DIR/../src/main/resources/static"

if [[ "$TARGET" != */src/main/resources/static ]]; then
  echo "Error: The target directory is not the expected frontend output directory."
  exit 1
fi

echo "Cleaning $TARGET"
rm -rf -- "$TARGET"
mkdir -p -- "$TARGET"

echo "Frontend output cleaned."
