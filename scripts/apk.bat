@echo off
REM Gera o APK release (fluido, sem overhead de debug) em app\build\outputs\apk\release\app-release.apk
cd /d "%~dp0"
call "%~dp0gradlew.bat" assembleRelease
echo APK: app\build\outputs\apk\release\app-release.apk
