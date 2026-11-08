@echo off
chcp 65001 >nul 2>&1
title 207研究所官网构建器

echo ============================================================
echo   廉中自然科学局第207研究所 - 静态站点构建器
echo ============================================================
echo.

REM 检查 Python 是否可用
where python >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Python，请先安装 Python 3.8+
    echo        下载地址: https://www.python.org/downloads/
    echo        安装时请勾选 "Add Python to PATH"
    echo.
    pause
    exit /b 1
)

REM 显示 Python 版本
python --version
echo.

REM 切换到脚本所在目录
cd /d "%~dp0"

REM 菜单
:menu
echo 请选择操作:
echo   [1] 增量构建 (仅更新变更的文件)
echo   [2] 全量重建 (清除并重新生成所有文件)
echo   [3] 构建并启动本地预览 (http://localhost:8000)
echo   [4] 监听模式 (文件变更自动重建)
echo   [0] 退出
echo.
set /p choice=请输入选项 (0-4): 

if "%choice%"=="1" (
    echo.
    python build.py
    echo.
    goto menu
) else if "%choice%"=="2" (
    echo.
    python build.py --clean
    echo.
    goto menu
) else if "%choice%"=="3" (
    echo.
    python build.py --serve
    echo.
    goto menu
) else if "%choice%"=="4" (
    echo.
    python build.py --watch
    echo.
    goto menu
) else if "%choice%"=="0" (
    exit /b 0
) else (
    echo.
    echo [提示] 无效选项，请重新输入
    echo.
    goto menu
)
