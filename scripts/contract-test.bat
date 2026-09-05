@echo off
REM Contrato E2E contra o SummusBackoffice real. Uso: contract-test.bat [baseUrl] [apiKey]
REM dev: backend local -> contract-test.bat            (default http://localhost:3001, chave "dev")
REM prod: contract-test.bat https://SEU-BACKEND SUA-CHAVE
cd /d "%~dp0"
set SUMMUS_TEST_BASE_URL=%~1
if "%SUMMUS_TEST_BASE_URL%"=="" set SUMMUS_TEST_BASE_URL=http://localhost:3001
set SUMMUS_TEST_API_KEY=%~2
if "%SUMMUS_TEST_API_KEY%"=="" set SUMMUS_TEST_API_KEY=dev
call "%~dp0gradlew.bat" :app:testDebugUnitTest --tests "com.chronopass.app.sync.SummusContractTest"
endlocal
