@echo off
setlocal
cd /d "%~dp0backend"

rem Do not let a stale command-line proxy break backend calls to external model APIs.
set "HTTP_PROXY="
set "HTTPS_PROXY="
set "ALL_PROXY="
set "http_proxy="
set "https_proxy="
set "all_proxy="

rem Load the local Volcengine Ark credentials into this backend process only.
rem Explicit SHOP_IMPORT_MODEL_* environment variables always take precedence.
set "MODEL_CONFIG_FILE=%~dp0huoshan.txt"
if exist "%MODEL_CONFIG_FILE%" (
  for /f "usebackq tokens=1,* delims==" %%A in ("%MODEL_CONFIG_FILE%") do (
    if /i "%%A"=="ARK_BASE_URL" if not defined SHOP_IMPORT_MODEL_BASE_URL set "SHOP_IMPORT_MODEL_BASE_URL=%%B"
    if /i "%%A"=="ARK_MODEL" if not defined SHOP_IMPORT_MODEL_NAME set "SHOP_IMPORT_MODEL_NAME=%%B"
    if /i "%%A"=="ARK_API_KEY" if not defined SHOP_IMPORT_MODEL_API_KEY set "SHOP_IMPORT_MODEL_API_KEY=%%B"
  )
)
if not defined SHOP_IMPORT_MODEL_ENABLED set "SHOP_IMPORT_MODEL_ENABLED=true"

if defined SHOP_IMPORT_MODEL_BASE_URL if defined SHOP_IMPORT_MODEL_NAME if defined SHOP_IMPORT_MODEL_API_KEY (
  echo AI model configuration loaded for this backend process.
) else (
  echo AI model configuration is incomplete. Deterministic matching will remain available.
)

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set "MAVEN_CMD=%~dp0.tools\apache-maven-3.9.9\bin\mvn.cmd"

if not exist "%JAVA_EXE%" (
  echo Java not found: %JAVA_EXE%
  echo Please install Java 17 or update JAVA_EXE in this file.
  pause
  exit /b 1
)

if not exist "%MAVEN_CMD%" (
  echo Maven not found: %MAVEN_CMD%
  echo Please check the .tools\apache-maven-3.9.9 directory.
  pause
  exit /b 1
)

echo Starting backend on http://localhost:8080
echo Please keep this window open.
echo.
echo Using source startup: Maven spring-boot:run
echo This avoids stale or non-executable jar files in backend\target.
echo.
"%MAVEN_CMD%" -Dmaven.repo.local=..\.m2\repository spring-boot:run

pause
