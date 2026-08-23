@echo off
REM ============================================================
REM ResQHub - compile script
REM Compiles every .java under src\ into out\
REM Classpath: lib\* (MySQL Connector/J) + resources (config)
REM ============================================================
setlocal
set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.2.1"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"

if not exist out mkdir out
if exist sources.txt del sources.txt

dir /s /b src\*.java > sources.txt

javac -encoding UTF-8 -cp "lib\*" -d out @sources.txt
if errorlevel 1 (
    echo.
    echo COMPILATION FAILED - fix errors above.
) else (
    echo.
    echo Compilation successful. Run with: run.bat
)

if exist sources.txt del sources.txt
endlocal
