@echo off
setlocal

set "TARGET=%~dp0..\src\main\resources\static"

if exist "%TARGET%" (
    echo Cleaning: %TARGET%
    rmdir /s /q "%TARGET%"
)

mkdir "%TARGET%"

echo Frontend output cleaned.

