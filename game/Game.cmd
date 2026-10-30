@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0launch.ps1" -Target game
if errorlevel 1 pause
