@echo off
setlocal
cd /d "%~dp0frontend"

set "PATH=C:\Program Files\nodejs;%PATH%"

if not exist "package.json" (
  echo package.json not found in frontend directory.
  pause
  exit /b 1
)

echo Starting frontend on http://127.0.0.1:5173
echo Please keep this window open.
npm run dev

pause
