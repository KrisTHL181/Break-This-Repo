#!/bin/bash
# HDSL-web Phase 4 端到端：整合包/会话/技能/体检 + 真实 dsh 的 ACP 控制台。
set -u
cd "$(dirname "$0")/.."

DATA=$(mktemp -d /tmp/hdsl-p4-data.XXXXXX)
PORT=13081
PASS="p4-secret-pass"
# 取**最新**的那个 jar：build/libs 里通常躺着历史版本，按字母序取第一个会拿到老构建
JAR=$(ls -t build/libs/hdsl-web-*.jar 2>/dev/null | head -1)
[ -n "$JAR" ] || { echo "找不到 jar，先构建：./gradlew shadowJar"; exit 1; }
LOG=/tmp/hdsl-p4-server.log
COOKIE=/tmp/hdsl-p4-cookies.txt
BASE="http://127.0.0.1:$PORT"
rm -f "$COOKIE"
PASS_COUNT=0; FAIL_COUNT=0
ok()  { PASS_COUNT=$((PASS_COUNT+1)); echo "PASS: $1"; }
bad() { FAIL_COUNT=$((FAIL_COUNT+1)); echo "FAIL: $1"; }

# 编译 ACP WS 探针（Jetty WebSocketClient 在 fat jar 里）
# AcpProbe 源码在仓库里（scripts/acp-probe/），编译产物放 /tmp
if [ ! -f /tmp/acpprobe/AcpProbe.class ] || [ scripts/acp-probe/AcpProbe.java -nt /tmp/acpprobe/AcpProbe.class ]; then
  javac -cp "$JAR" -d /tmp/acpprobe scripts/acp-probe/AcpProbe.java || { echo "probe compile failed"; exit 1; }
fi

HDSL_DATA="$DATA" HDSL_PORT=$PORT HDSL_ADMIN_PASSWORD="$PASS" java -jar "$JAR" > "$LOG" 2>&1 &
JPID=$!
cleanup() { kill $JPID 2>/dev/null; wait $JPID 2>/dev/null; }
trap cleanup EXIT

for i in $(seq 1 60); do curl -sf "$BASE/api/health" >/dev/null 2>&1 && break; sleep 1; done
curl -sf -c "$COOKIE" -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"admin\",\"password\":\"$PASS\"}" >/dev/null && ok "login" || { bad "login"; exit 1; }

# --- 体检 ---
DJSON=$(curl -s -b "$COOKIE" "$BASE/api/doctor")
DCOUNT=$(python3 -c "import json,sys; print(len(json.load(sys.stdin).get('checks',[])))" <<<"$DJSON" 2>/dev/null)
[ "${DCOUNT:-0}" -gt 0 ] && ok "doctor ($DCOUNT checks)" || { bad "doctor"; echo "$DJSON" | head -c 300; }

# --- 整合包市场（真实索引）---
MJSON=$(curl -s -b "$COOKIE" "$BASE/api/packs/market")
MCOUNT=$(python3 -c "import json,sys; print(len(json.load(sys.stdin).get('packs',[])))" <<<"$MJSON" 2>/dev/null)
[ "${MCOUNT:-0}" -gt 0 ] && ok "packs-market ($MCOUNT packs)" || { bad "packs-market"; echo "$MJSON" | head -c 300; }

# --- 建实例 + 安装（复用真实 dsh，pnpm 缓存已热）---
VERSION=$(curl -s -b "$COOKIE" "$BASE/api/versions" | python3 -c "import json,sys; print(json.load(sys.stdin)['versions'][0]['version'])")
IID=$(curl -s -b "$COOKIE" -X POST "$BASE/api/instances" -H 'Content-Type: application/json' \
  -d "{\"name\":\"p4\",\"version\":\"$VERSION\"}" | python3 -c "import json,sys; print(json.load(sys.stdin)['instance']['id'])")
TASK=$(curl -s -b "$COOKIE" -X POST "$BASE/api/instances/$IID/install" -H 'Content-Type: application/json' \
  -d "{\"version\":\"$VERSION\"}" | python3 -c "import json,sys; print(json.load(sys.stdin)['taskId'])")
for i in $(seq 1 300); do
  ST=$(curl -s -b "$COOKIE" "$BASE/api/tasks/$TASK" | python3 -c "import json,sys; print(json.load(sys.stdin).get('state','?'))")
  [ "$ST" = "done" ] && break; [ "$ST" = "failed" ] && break; sleep 2
done
[ "$ST" = "done" ] && ok "install ($VERSION)" || { bad "install ($ST)"; exit 1; }

# --- 会话/技能/工作区 ---
for EP in sessions skills workspaces; do
  CODE=$(curl -s -o /tmp/p4-$EP.json -w '%{http_code}' -b "$COOKIE" "$BASE/api/instances/$IID/$EP")
  [ "$CODE" = "200" ] && ok "$EP" || bad "$EP ($CODE)"
done

# --- 导出整合包 ---
ETASK=$(curl -s -b "$COOKIE" -X POST "$BASE/api/instances/$IID/export" -H 'Content-Type: application/json' \
  -d '{"name":"p4-smoke","includeSessions":true}' | python3 -c "import json,sys; print(json.load(sys.stdin)['taskId'])")
for i in $(seq 1 60); do
  ST=$(curl -s -b "$COOKIE" "$BASE/api/tasks/$ETASK" | python3 -c "import json,sys; print(json.load(sys.stdin).get('state','?'))")
  [ "$ST" = "done" ] && break; [ "$ST" = "failed" ] && break; sleep 1
done
FNAME=$(curl -s -b "$COOKIE" "$BASE/api/tasks/$ETASK" | python3 -c "import json,sys; t=json.load(sys.stdin); print((t.get('result') or {}).get('filename',''))")
[ -n "${FNAME:-}" ] && ok "export ($FNAME)" || { bad "export"; curl -s -b "$COOKIE" "$BASE/api/tasks/$ETASK"; }
curl -s -b "$COOKIE" "$BASE/api/exports/$FNAME" -o /tmp/p4-export.dspack
SIZE=$(wc -c < /tmp/p4-export.dspack)
MAGIC=$(head -c 2 /tmp/p4-export.dspack)
{ [ "${SIZE:-0}" -gt 200 ] && [ "$MAGIC" = "PK" ]; } && ok "export-download (valid zip, $SIZE bytes)" || bad "export-download (size=$SIZE magic=$MAGIC)"
curl -s -b "$COOKIE" "$BASE/api/exports" | grep -q "$FNAME" && ok "exports-list" || bad "exports-list"

# --- 启动真实实例 → 停止（攻击面归零）---
curl -s -b "$COOKIE" -X POST "$BASE/api/instances/$IID/launch" -H 'Content-Type: application/json' -d '{}' >/dev/null
URL=""
for i in $(seq 1 60); do URL=$(curl -s -b "$COOKIE" "$BASE/api/instances/$IID/open" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('url',''))" 2>/dev/null); [ -n "$URL" ] && break; sleep 2; done
[ -n "${URL:-}" ] && ok "running" || bad "running"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -c "$COOKIE" -b "$COOKIE" -L "$BASE$URL")
[ "$CODE" = "200" ] && ok "proxy (200)" || bad "proxy ($CODE)"
curl -s -b "$COOKIE" -X POST "$BASE/api/instances/$IID/stop" >/dev/null; sleep 5

# --- ACP 控制台（实例已停止也能开：独立进程）---
SESS=$(python3 -c "
import re
for line in open('$COOKIE'):
    if 'hdsl_session' in line: print('hdsl_session=' + line.split()[-1])
" | head -1)
if [ -n "${SESS:-}" ]; then
  java -cp "$JAR:/tmp/acpprobe" AcpProbe "ws://127.0.0.1:$PORT/ws" "$SESS" "$IID" > /tmp/p4-acp.log 2>&1
  ACP_EXIT=$?
  if [ $ACP_EXIT -eq 0 ]; then ok "acp-start→ready (真实 dsh, 实例已停止)"; else bad "acp (exit=$ACP_EXIT)"; tail -5 /tmp/p4-acp.log; fi
else
  bad "acp-cookie"
fi

echo "================ PASS=$PASS_COUNT FAIL=$FAIL_COUNT ================"
[ $FAIL_COUNT -eq 0 ]
