@echo off
setlocal
cd /d "%~dp0"

where node >nul 2>&1 || (
    echo ERROR: Node.js is not installed or not in PATH.
    exit /b 1
)

echo Installing frontend dependencies...
call npm ci
if errorlevel 1 (
    echo ERROR: npm ci failed.
    exit /b 1
)

echo Building frontend...
call npm run build
if errorlevel 1 (
    echo ERROR: Frontend build failed.
    exit /b 1
)

echo Frontend build completed.
echo Output: src\main\resources\static