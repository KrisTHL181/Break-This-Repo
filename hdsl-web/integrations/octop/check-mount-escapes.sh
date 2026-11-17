#!/usr/bin/env bash
# Fail when the patched Octop dashboard contains a URL that would leave the panel mount.
#
#   ./integrations/octop/check-mount-escapes.sh /path/to/Octop-<version>
#
# The instance is proxied under `/i/<token>/`; anything that navigates or connects to a
# root-absolute path (`/api/...`, `/login`, `/chat`, `/logo.svg`) lands on the *panel* instead:
# the panel answers unknown roots with its SPA shell (200 text/html, which is why broken images
# look like HTML), and its own `/api` never upgrades a WebSocket. Both classes of bug shipped
# once and were only noticed when somebody opened the page.
#
# Run from `build-dashboard.sh` after the patch is applied, so a new hard-coded path fails the
# build instead of the user's browser.
set -euo pipefail

SOURCE="${1:?usage: check-mount-escapes.sh <octop-source-dir>}"
SRC="${SOURCE}/dashboard/src"
if [ ! -d "${SRC}" ]; then
  echo "[mount] no dashboard source at ${SRC}" >&2
  exit 2
fi

cd "${SRC}"
fail=0

check() {
  local pattern="$1" advice="$2" hits
  hits=$(grep -rnE "${pattern}" . --include='*.ts' --include='*.tsx' 2>/dev/null \
    | grep -v '\.test\.' \
    | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)' || true)
  if [ -n "${hits}" ]; then
    echo "[mount] ${advice}" >&2
    echo "${hits}" >&2
    fail=1
  fi
}

# Hard navigations: `window.location.href = "/x"`, `location.replace("/x")`, `location.assign`.
check 'window\.location\.(href|replace|assign)[[:space:]]*[=(][[:space:]]*"/' \
  'hardcoded redirect to a root path — use publicUrl("...")'
# Anchor tags that leave the SPA: `<a href="/x">`. Router links are fine (`<Link to>`).
check 'href="/' \
  '<a href="/..."> navigates the whole page — use <Link to> or publicUrl()'
# Static assets referenced from the document root (`<img src="/x">`).
check 'src="/' \
  'root-absolute asset — use publicUrl("...")'
# Hand-built sockets: `ws://${window.location.host}/api/...` — use getWsUrl().
check 'location\.host\}/api' \
  'hand-built WebSocket URL — use getWsUrl("/...")'
# fetch/axios to an absolute /api path — use getApiUrl().
check 'fetch\(\s*[`"]/api' \
  'fetch("/api/...") — use getApiUrl("/...")'
check 'window\.open\(\s*"/' \
  'window.open("/...") leaves the mount — prefix it with publicUrl()'

if [ "${fail}" -ne 0 ]; then
  echo "[mount] refusing to build: the dashboard can leave /i/<token>/ (see above)" >&2
fi
exit "${fail}"