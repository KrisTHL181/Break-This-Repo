# 更新在线状态
API=(encrypted)
curl -X POST $API/heartbeat \
  -H "Content-Type: application/json" \
  -d '{"status": "正在写代码"}'

# 添加事件
curl -X POST $API/events \
  -H "Content-Type: application/json" \
  -d '{"title": "部署了个人主页", "category": "SITE"}'
