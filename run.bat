@echo off
setlocal
set JAVA_HOME=C:\Java\jdk1.8
set PATH=%JAVA_HOME%\bin;%PATH%

echo ========================================================
echo   Attendance Management System (Spring Boot Web)
echo ========================================================

if not exist "%~dp0tools\maven\bin\mvn.cmd" (
    echo Maven is not found in project tools. Downloading portable Maven...
    powershell -ExecutionPolicy Bypass -File "%~dp0setup-maven.ps1"
)

echo Starting Spring Boot application on http://localhost:8085 ...
"%~dp0tools\maven\bin\mvn.cmd" spring-boot:run
pause
