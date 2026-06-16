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
