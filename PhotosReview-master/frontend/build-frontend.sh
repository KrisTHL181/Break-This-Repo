#!/usr/bin/env bash
set -euo pipefail

command -v node >/dev/null 2>&1 || {
  echo "ERROR: Node.js is not installed or not in PATH."
  exit 1
}

echo "Installing frontend dependencies..."
(
  npm ci
  npm run build
)

echo "Frontend build completed."
echo "Output: src\main\resources\static"