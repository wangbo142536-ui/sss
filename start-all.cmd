@echo off
setlocal
cd /d "%~dp0"

echo Starting ship supply platform...
echo Backend: http://localhost:8080
echo Frontend: http://127.0.0.1:5173
echo.
echo Two command windows will open. Keep both windows open while using the project.

start "Ship Supply Backend" cmd /k "%~dp0start-backend.cmd"
start "Ship Supply Frontend" cmd /k "%~dp0start-frontend.cmd"

echo Done.
pause
