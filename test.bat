@echo off
REM Roda os testes de logica (nao precisa de emulador).
cd /d "%~dp0"
call "%~dp0gradlew.bat" test
