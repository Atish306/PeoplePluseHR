@echo off
setlocal
cd /d "%~dp0e2e"

echo ========================================================
echo   Running Playwright E2E Test Suite (PeoplePulse HR)
echo ========================================================

call "C:\Program Files\nodejs\npx.cmd" playwright test --project="Google Chrome"

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================================
    echo   [SUCCESS] All Playwright E2E Tests Passed!
    echo ========================================================
) else (
    echo.
    echo ========================================================
    echo   [FAILURE] Some Playwright tests failed.
    echo ========================================================
)

pause
