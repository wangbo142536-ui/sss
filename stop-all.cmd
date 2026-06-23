@echo off
setlocal
cd /d "%~dp0"

echo Stopping ship supply platform...
echo Backend port: 8080
echo Frontend port: 5173
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ports = @(8080, 5173); " ^
  "$connections = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $ports -contains $_.LocalPort }; " ^
  "if (-not $connections) { Write-Host 'No running backend/frontend service found on ports 8080 or 5173.'; exit 0 }; " ^
  "$processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique; " ^
  "foreach ($processId in $processIds) { " ^
  "  $process = Get-Process -Id $processId -ErrorAction SilentlyContinue; " ^
  "  if ($process) { " ^
  "    $portsText = ($connections | Where-Object { $_.OwningProcess -eq $processId } | Select-Object -ExpandProperty LocalPort -Unique) -join ', '; " ^
  "    Write-Host ('Stopping PID ' + $processId + ' (' + $process.ProcessName + ') on port(s): ' + $portsText); " ^
  "    Stop-Process -Id $processId -Force; " ^
  "  } " ^
  "} " ^
  "Write-Host 'Stop command completed.'"

echo.
echo Done.
pause
