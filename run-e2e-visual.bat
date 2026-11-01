@echo off
setlocal
cd /d "%~dp0e2e"

set SLOWMO=800

echo ====================================================================
echo   Launching Visual Frontend E2E Tests (Visible Google Chrome)
echo   Running in SLOW-MOTION (800ms delay per action) for easy observation!
echo ====================================================================

call "C:\Program Files\nodejs\npx.cmd" playwright test --project="Google Chrome" --headed

if %ERRORLEVEL% equ 0 (
    echo.
    echo ====================================================================
    echo   [SUCCESS] All Visual Frontend Tests Passed!
    echo ====================================================================
) else (
    echo.
    echo ====================================================================
    echo   [FAILURE] Some tests encountered an issue.
    echo ====================================================================
)

pause
