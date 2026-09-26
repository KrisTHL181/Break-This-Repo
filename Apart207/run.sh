#!/usr/bin/env bash
# 207研究所官网构建器 - macOS / Linux 双击运行脚本
# 使用方法: chmod +x run.sh && ./run.sh

set -e

# 切换到脚本所在目录
cd "$(dirname "$0")"

echo "============================================================"
echo "  廉中自然科学局第207研究所 - 静态站点构建器"
echo "============================================================"
echo ""

# 检查 Python3 是否可用
if command -v python3 &> /dev/null; then
    PYTHON=python3
elif command -v python &> /dev/null; then
    PYTHON=python
else
    echo "[错误] 未检测到 Python，请先安装 Python 3.8+"
    echo "       macOS: brew install python3"
    echo "       Linux: sudo apt install python3 / sudo dnf install python3"
    read -p "按回车键退出..."
    exit 1
fi

$PYTHON --version
echo ""

# 菜单循环
while true; do
    echo "请选择操作:"
    echo "  [1] 增量构建 (仅更新变更的文件)"
    echo "  [2] 全量重建 (清除并重新生成所有文件)"
    echo "  [3] 构建并启动本地预览 (http://localhost:8000)"
    echo "  [4] 监听模式 (文件变更自动重建)"
    echo "  [0] 退出"
    echo ""
    read -p "请输入选项 (0-4): " choice

    case $choice in
        1)
            echo ""
            $PYTHON build.py
            echo ""
            ;;
        2)
            echo ""
            $PYTHON build.py --clean
            echo ""
            ;;
        3)
            echo ""
            $PYTHON build.py --serve
            echo ""
            ;;
        4)
            echo ""
            $PYTHON build.py --watch
            echo ""
            ;;
        0)
            exit 0
            ;;
        *)
            echo ""
            echo "[提示] 无效选项，请重新输入"
            echo ""
            ;;
    esac
done
