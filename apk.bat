@echo off
REM Gera o APK avulso em app\build\outputs\apk\debug\app-debug.apk
cd /d "%~dp0"
call "%~dp0gradlew.bat" assembleDebug
echo APK: app\build\outputs\apk\debug\app-debug.apk
