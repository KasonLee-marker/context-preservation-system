#!/bin/bash
# 查看应用日志脚本

echo "=== 查看应用日志 ==="
echo ""

# 查找应用进程
PID=$(ps aux | grep "java -jar.*context-preservation" | grep -v grep | awk '{print $2}' | head -1)

if [ -z "$PID" ]; then
    echo "❌ 应用未运行"
    exit 1
fi

echo "✅ 应用运行中，PID: $PID"
echo ""

# 查看日志
echo "=== 最近 50 行日志 ==="
journalctl -u context-preservation -n 50 --no-pager 2>/dev/null || \
    tail -50 /proc/$PID/fd/1 2>/dev/null || \
    echo "无法读取日志，尝试: docker compose logs -f app"

echo ""
echo "=== 查看实时日志 ==="
echo "使用: tail -f /path/to/app.log"
echo "或使用: docker compose logs -f app"
