@echo off
REM ChronoPass - build, instala e abre no emulador. Rode a cada mudanca.
cd /d "%~dp0"
setlocal
set SDK=%LOCALAPPDATA%\Android\Sdk
set ADB=%SDK%\platform-tools\adb.exe

echo [1/3] Verificando emulador...
"%ADB%" get-state 1>nul 2>nul
if errorlevel 1 (
  echo   Nenhum device. Iniciando emulador "chrono"...
  start "" "%SDK%\emulator\emulator.exe" -avd chrono
  echo   Aguardando boot...
  "%ADB%" wait-for-device
  :waitboot
  for /f "delims=" %%b in ('"%ADB%" shell getprop sys.boot_completed 2^>nul') do set BOOT=%%b
  if not "%BOOT%"=="1" ( timeout /t 2 /nobreak >nul & goto waitboot )
  echo   Emulador pronto.
)

echo [2/3] Build + instalando...
call "%~dp0gradlew.bat" installDebug
if errorlevel 1 ( echo BUILD FALHOU & exit /b 1 )

echo [3/3] Abrindo app...
"%ADB%" shell monkey -p com.chronopass.app 1 >nul
echo Pronto.
endlocal
