@echo off
setlocal
title Subir LECTOR EDGARDO a GitHub
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0SUBIR-A-GITHUB.ps1"
set "CODIGO=%ERRORLEVEL%"
if not "%CODIGO%"=="0" (
    echo.
    echo El script termino con codigo %CODIGO%.
    pause
)
endlocal
exit /b %CODIGO%
