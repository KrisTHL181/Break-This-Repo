@echo off
rem ============================================================
rem  GTNH 知识库一键启动（Windows）
rem    首次运行会自动建站（约 1 分钟），然后起本地 Web 服务。
rem    用法:  start.cmd              默认 http://127.0.0.1:8777
rem           start.cmd --port 9000
rem           start.cmd --host 0.0.0.0 --no-browser   局域网可访问
rem ============================================================
setlocal
chcp 65001 >nul
set PYTHONIOENCODING=utf-8
cd /d "%~dp0"

where python >nul 2>nul
if errorlevel 1 (
  echo [错误] 找不到 python，请先安装 Python 3.8+ 并加入 PATH。
  exit /b 1
)

if not exist "kbweb\data\meta.json" (
  echo [1/4] 生成站点数据 ...
  python kbweb\build_site.py
  if errorlevel 1 goto :fail
  echo [2/4] 生成检索索引 ...
  python kbweb\build_index.py
  if errorlevel 1 goto :fail
  echo [3/4] 打包正文 ...
  python kbweb\site_pack.py
  if errorlevel 1 goto :fail
) else (
  echo [1/4] 站点数据已存在，跳过建站（删掉 kbweb\data 可强制重建）
  echo [2/4] 跳过索引
  echo [3/4] 跳过打包
)

echo [4/4] 启动服务（Ctrl+C 停止）...
python kbweb\serve.py %*
goto :eof

:fail
echo [错误] 建站失败，请看上面的输出。
exit /b 1
