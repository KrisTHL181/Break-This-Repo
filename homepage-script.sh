#!/bin/bash

API="(被消除)"
TOKEN="${HOMEPAGE_TOKEN:-}"
DIR="$(cd "$(dirname "$0")/.." && pwd)"
HTML="$DIR/index.html"

add_friend() {
  NAME="${1:-}"
  URL="${2:-}"
  DESC="${3:-}"
  AVATAR="${4:-}"
  if [ -z "$NAME" ] || [ -z "$URL" ]; then
    echo "用法: homepage friend add <名字> <链接> [描述] [头像URL]"
    exit 1
  fi

  if [ -n "$AVATAR" ]; then
    AVATAR_HTML="<img src=\"$AVATAR\" alt=\"$NAME\" />"
  else
    AVATAR_HTML="${NAME:0:1}"
  fi

  if [ -n "$DESC" ]; then
    DESC_HTML="<div class=\"friend-description\">$DESC</div>"
  else
    DESC_HTML=""
  fi

  python3 <<PYEOF
import sys
html = open("$HTML").read()
block = '''
    <a class="friend" href="$URL" target="_blank" rel="noopener noreferrer">
      <div class="friend-avatar">$AVATAR_HTML</div>
      <div class="friend-info">
        <div class="friend-name">$NAME</div>
        $DESC_HTML
      </div>
      <div class="friend-arrow">↗</div>
    </a>'''
html = html.replace('<div class="friends">', '<div class="friends">\n' + block)
open("$HTML", "w").write(html)
print("已添加: $NAME")
PYEOF
}

rm_friend() {
  ID="${1:-}"
  if [ -z "$ID" ]; then
    echo "用法: homepage friend rm <ID>"
    exit 1
  fi

  python3 <<PYEOF
import re, sys
with open("$HTML", "r") as f:
    content = f.read()
pattern = r'\n\s*<a\s+class="friend"[^>]*>.*?</a>'
matches = list(re.finditer(pattern, content, re.DOTALL))
idx = int("$ID") - 1
if 0 <= idx and idx < len(matches):
    m = matches[idx]
    name = re.search(r'friend-name">\s*(.*?)\s*<', m.group())
    name = name.group(1) if name else "?"
    content = content[:m.start()] + content[m.end():]
    with open("$HTML", "w") as f:
        f.write(content)
    print("已删除: " + name)
else:
    print("ID $ID 不存在，共 " + str(len(matches)) + " 条友链")
PYEOF
}

list_friends() {
  python3 <<PYEOF
import re
with open("$HTML", "r") as f:
    content = f.read()
friends = re.findall(r'<div class="friend-name">\s*(.*?)\s*</div>', content)
for i, name in enumerate(friends, 1):
    print(str(i) + ". " + name)
PYEOF
}

case "$1" in
friend)
  shift
  SUB="${1:-}"
  shift 2>/dev/null || true
  case "$SUB" in
  add)
    add_friend "$@"
    ;;
  rm | remove | del)
    rm_friend "$@"
    ;;
  list | ls)
    list_friends
    ;;
  *)
    echo "用法:"
    echo "  homepage friend add <名字> <链接> [描述] [头像URL]"
    echo "  homepage friend rm <ID>"
    echo "  homepage friend list"
    ;;
  esac
  ;;
status | online)
  shift
  STATUS="${*:-在线}"
  curl -s -X POST "$API/heartbeat" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{\"status\": \"$STATUS\", \"online\": true}"
  echo ""
  ;;
offline | off)
  shift
  STATUS="${*:-}"
  if [ -z "$STATUS" ]; then
    curl -s -X POST "$API/heartbeat" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d '{"online": false}'
  else
    curl -s -X POST "$API/heartbeat" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d "{\"status\": \"$STATUS\", \"online\": false}"
  fi
  echo ""
  ;;
event)
  shift
  TITLE="${1:-}"
  CATEGORY="${2:-MISC}"
  if [ -z "$TITLE" ]; then
    echo "用法: homepage event <标题> [分类]"
    exit 1
  fi
  curl -s -X POST "$API/events" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{\"title\": \"$TITLE\", \"category\": \"$CATEGORY\"}"
  echo ""
  ;;
get)
  curl -s "$API/status"
  echo ""
  ;;
events)
  curl -s "$API/events"
  echo ""
  ;;
rm)
  shift
  TYPE="${1:-}"
  ID="${2:-}"
  if [ "$TYPE" = "event" ] && [ -n "$ID" ]; then
    curl -s -X DELETE "$API/events/$ID" \
      -H "Authorization: Bearer $TOKEN"
    echo ""
  else
    echo "用法: homepage rm event <ID>"
    exit 1
  fi
  ;;
*)
  echo "用法:"
  echo "  homepage status [在干什么]    上线并设置状态"
  echo "  homepage offline [状态]      下线"
  echo "  homepage event <标题> [分类]  添加事件"
  echo "  homepage friend add <名字> <链接> [描述] [头像URL]"
  echo "  homepage friend rm <ID>       删除友链"
  echo "  homepage friend list          查看友链"
  echo "  homepage get                  查看当前状态"
  echo "  homepage events               查看事件列表"
  echo "  homepage rm event <ID>        删除事件"
  ;;
esac
