@echo off
setlocal
cd /d "%~dp0e2e"

echo ====================================================================
echo   Launching Interactive Playwright Test Runner Dashboard
echo ====================================================================

call "C:\Program Files\nodejs\npx.cmd" playwright test --ui
