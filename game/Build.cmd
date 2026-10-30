@echo off
cd /d "%~dp0"
python build.py
if errorlevel 1 goto failed
python tools\pose-editor\build.py --smoke
if errorlevel 1 goto failed
echo Build complete. Files are in dist.
pause
exit /b 0
:failed
echo Build failed.
pause
exit /b 1
