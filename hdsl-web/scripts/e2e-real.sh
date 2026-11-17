#!/bin/bash
# HDSL-web 真实端到端冒烟：真实 npm 安装 dsh → 启动 → 反代访问 → 停止。
set -u
cd "$(dirname "$0")/.."

DATA=$(mktemp -d /tmp/hdsl-e2e-data.XXXXXX)
PORT=13080
PASS="e2e-secret-pass"
# 取**最新**的那个 jar：build/libs 里通常躺着历史版本，按字母序取第一个会拿到老构建
JAR=$(ls -t build/libs/hdsl-web-*.jar 2>/dev/null | head -1)
[ -n "$JAR" ] || { echo "找不到 jar，先构建：./gradlew shadowJar"; exit 1; }
LOG=/tmp/hdsl-e2e-server.log
COOKIE=/tmp/hdsl-e2e-cookies.txt
BASE="http://127.0.0.1:$PORT"
rm -f "$COOKIE"
PASS_COUNT=0; FAIL_COUNT=0
ok()  { PASS_COUNT=$((PASS_COUNT+1)); echo "PASS: $1"; }
bad() { FAIL_COUNT=$((FAIL_COUNT+1)); echo "FAIL: $1"; }

HDSL_DATA="$DATA" HDSL_PORT=$PORT HDSL_ADMIN_PASSWORD="$PASS" java -jar "$JAR" > "$LOG" 2>&1 &
JPID=$!
cleanup() { kill $JPID 2>/dev/null; wait $JPID 2>/dev/null; echo "--- server log tail ---"; tail -15 "$LOG"; }
trap cleanup EXIT

# 等待就绪
for i in $(seq 1 60); do
  curl -sf "$BASE/api/health" >/dev/null 2>&1 && break
  sleep 1
done
curl -sf "$BASE/api/health" >/dev/null && ok "health" || { bad "health"; exit 1; }

# 登录
curl -sf -c "$COOKIE" -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"admin\",\"password\":\"$PASS\"}" | grep -q '"ok":true' && ok "login" || { bad "login"; exit 1; }

# SPA 壳
curl -sf -b "$COOKIE" "$BASE/" | grep -q 'id="root"' && ok "spa-shell" || bad "spa-shell"

# 版本列表（真实 npm）
VJSON=$(curl -sf -b "$COOKIE" "$BASE/api/versions") || { bad "versions-fetch"; exit 1; }
VERSION=$(python3 -c "import json,sys; vs=json.load(sys.stdin)['versions']; print(vs[0]['version'])" <<<"$VJSON" 2>/dev/null)
[ -n "${VERSION:-}" ] && ok "versions (latest=$VERSION)" || { bad "versions-parse"; exit 1; }

# 创建实例
IJ=$(curl -sf -b "$COOKIE" -X POST "$BASE/api/instances" -H 'Content-Type: application/json' \
  -d "{\"name\":\"e2e\",\"version\":\"$VERSION\"}") || { bad "create-instance"; exit 1; }
IID=$(python3 -c "import json,sys; print(json.load(sys.stdin)['instance']['id'])" <<<"$IJ")
[ -n "${IID:-}" ] && ok "create-instance (id=$IID)" || { bad "create-instance-parse"; exit 1; }

# 安装（真实 npm install，可能数分钟）
TJ=$(curl -sf -b "$COOKIE" -X POST "$BASE/api/instances/$IID/install" -H 'Content-Type: application/json' \
  -d "{\"version\":\"$VERSION\"}") || { bad "install-start"; exit 1; }
TASK=$(python3 -c "import json,sys; print(json.load(sys.stdin)['taskId'])" <<<"$TJ")
echo "install task=$TASK …"
for i in $(seq 1 360); do
  ST=$(curl -sf -b "$COOKIE" "$BASE/api/tasks/$TASK" | python3 -c "import json,sys; print(json.load(sys.stdin).get('state','?'))" 2>/dev/null)
  case "$ST" in done) break;; failed) echo "$(curl -s -b "$COOKIE" "$BASE/api/tasks/$TASK")"; bad "install-failed"; exit 1;; esac
  sleep 2
done
[ "${ST:-}" = "done" ] && ok "install ($VERSION)" || { bad "install-timeout (state=$ST)"; exit 1; }

# 启动 → 就绪
curl -sf -b "$COOKIE" -X POST "$BASE/api/instances/$IID/launch" -H 'Content-Type: application/json' -d '{}' >/dev/null || { bad "launch"; exit 1; }
URL=""
for i in $(seq 1 120); do
  URL=$(curl -sf -b "$COOKIE" "$BASE/api/instances/$IID/open" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('url',''))" 2>/dev/null)
  [ -n "${URL:-}" ] && break
  sleep 2
done
[ -n "${URL:-}" ] && ok "running (url=$URL)" || { bad "no-ready-url"; curl -s -b "$COOKIE" "$BASE/api/instances/$IID/logs?tail=30"; exit 1; }

# 反代访问（token 兑换 → 跟随 303 → 200）
CODE=$(curl -s -o /dev/null -w '%{http_code}' -c "$COOKIE" -b "$COOKIE" -L "$BASE$URL")
[ "$CODE" = "200" ] && ok "proxy-open (200)" || bad "proxy-open ($CODE)"
BODY=$(curl -s -b "$COOKIE" "$BASE/i/$IID/")
echo "$BODY" | grep -qi "html" && ok "proxy-html" || bad "proxy-html"

# 停止 → 攻击面归零
curl -sf -b "$COOKIE" -X POST "$BASE/api/instances/$IID/stop" >/dev/null
sleep 6
SCODE=$(curl -s -o /dev/null -w '%{http_code}' -b "$COOKIE" "$BASE/i/$IID/")
{ [ "$SCODE" = "502" ] || [ "$SCODE" = "404" ]; } && ok "stopped-proxy-gone ($SCODE)" || bad "stopped-proxy-gone ($SCODE)"

echo "================ PASS=$PASS_COUNT FAIL=$FAIL_COUNT ================"
[ $FAIL_COUNT -eq 0 ]
