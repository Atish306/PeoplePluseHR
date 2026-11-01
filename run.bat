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

for /f "tokens=*" %%a in ('powershell -Command "(Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -notlike '*Loopback*' -and $_.IPAddress -notlike '169.254*' } | Select-Object -First 1).IPAddress"') do set MY_LAN_IP=%%a

echo.
echo Local PC Access:    http://localhost:8085
if defined MY_LAN_IP (
    echo Mobile / LAN URL:   http://%MY_LAN_IP%:8085
)
echo ========================================================
echo.

"%~dp0tools\maven\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.jvmArguments="-Djava.net.preferIPv4Stack=true"
pause
