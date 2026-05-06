@echo off
setlocal

set ROOT_DIR=%~dp0..
powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT_DIR%\agent-client\scripts\package-lan-windows.ps1" %*
exit /b %ERRORLEVEL%
