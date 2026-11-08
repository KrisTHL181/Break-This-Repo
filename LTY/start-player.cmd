@echo off
chcp 65001 >nul
title Luo Tianyi Fanpage Player
cd /d "%~dp0"
echo.
echo   ==================================================
echo     Luo Tianyi Fanpage  -  local player server
echo   ==================================================
echo     URL    : http://127.0.0.1:8123/
echo     Music  : LOU\music
echo     Stop   : press Ctrl + C in this window
echo.
echo   Keep this window OPEN while listening.
echo.
start "" http://127.0.0.1:8123/
node "%~dp0server.js" 8123
if errorlevel 1 (
  echo.
  echo   [x] Failed to start. Is Node.js installed?  Try:  node -v
)
echo.
pause
