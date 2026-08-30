@echo off
REM ============================================================
REM ResQHub - run script
REM Launches com.resqhub.main.ResQHubApplication
REM Extra args can be passed: run.bat --mode=offline
REM ============================================================
setlocal
set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.2.1"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"

java -cp "out;lib\*;resources" com.resqhub.main.ResQHubApplication %*
endlocal
