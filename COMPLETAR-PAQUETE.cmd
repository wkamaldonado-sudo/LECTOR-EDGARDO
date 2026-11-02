@echo off
rem Doble clic para completar el paquete: copia el APK y prepara el repositorio.
rem Para incluir tambien el codigo fuente: COMPLETAR-PAQUETE.cmd -ConCodigo
title LECTOR EDGARDO - completar paquete
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0COMPLETAR-PAQUETE.ps1" %*
echo.
pause
