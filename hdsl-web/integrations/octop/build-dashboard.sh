#!/usr/bin/env bash
# Build the patched Octop dashboard that ships in the panel jar.
#
#   ./integrations/octop/build-dashboard.sh 1.0.3b1
#
# Needs Node 20+ (the panel image has Node 22), npm and network access to
# codeload.github.com. Output:
#   src/main/resources/octop/octop-dashboard-<version>.zip
set -euo pipefail

VERSION="${1:?usage: build-dashboard.sh <octop-version>}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORK="${OCTOP_BUILD_DIR:-$(mktemp -d)}"
SOURCE="${WORK}/Octop-${VERSION}"
PATCH="${ROOT}/integrations/octop/patches/0001-runtime-base-and-token-namespace.patch"
OUT="${ROOT}/src/main/resources/octop/octop-dashboard-${VERSION}.zip"

mkdir -p "${ROOT}/src/main/resources/octop"
if [ ! -d "${SOURCE}" ]; then
  echo "[octop] downloading Octop ${VERSION}"
  mkdir -p "${WORK}"
  curl -fsSL "https://codeload.github.com/TencentCloud/Octop/zip/refs/tags/v${VERSION}" \
    | tar -xz -C "${WORK}"
fi

echo "[octop] applying dashboard patch"
git -C "${SOURCE}" apply --whitespace=nowarn "${PATCH}" 2>/dev/null \
  || (cd "${SOURCE}" && patch -p1 < "${PATCH}")

echo "[octop] checking for URLs that would leave the panel mount"
"${ROOT}/integrations/octop/check-mount-escapes.sh" "${SOURCE}"

echo "[octop] installing dashboard dependencies"
cd "${SOURCE}/dashboard"
npm ci --no-audit --no-fund

# `build`, not `build:docker`: tsc catches a missing import or a type mismatch here instead of
# letting the user find a blank page. Costs ~40 s out of a very long source build.
echo "[octop] building dashboard (relative base, with type check)"
VITE_BASE_PATH=./ NODE_OPTIONS="--max-old-space-size=2048" npm run build

echo "[octop] packing ${OUT}"
rm -f "${OUT}"
(cd "${SOURCE}/src/octop" && zip -qr "${OUT}" dashboard)
echo "[octop] done: ${OUT}"
