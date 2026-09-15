@echo off
chcp 65001 >nul
title Bad Apple ASCII Player

echo -----Bad Apple ASCII art player-----
echo Press Enter to play.
pause >nul
cls

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0badapple.ps1"

echo -----Bad Apple ASCII art player-----
echo Thanks for watching!
echo Made by chuan.
echo.
echo Press Enter to Exit.
pause >nul