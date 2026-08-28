@echo off
REM ChronoPass - release completa: testes, build release, bump de versao, tag,
REM push para o branch production e GitHub Release (e o que alimenta o auto-update).
REM Uso (rode do branch main): release.bat [patch|minor|major]
REM   Sem argumento, detecta sozinho pelos commits desde a ultima tag:
REM   [major] -> major, [minor] -> minor, senao patch (mesma regra do CI).
cd /d "%~dp0.."
setlocal enabledelayedexpansion

set "GRADLEW=%~dp0gradlew.bat"

echo [1/6] Pre-requisitos...
git fetch --tags origin >nul 2>&1
gh auth status >nul 2>&1
if errorlevel 1 ( echo   gh nao autenticado. Rode: gh auth login & exit /b 1 )
git diff --quiet
if errorlevel 1 ( echo   Working tree sujo. Commit ou stash antes de publicar. & exit /b 1 )
git diff --cached --quiet
if errorlevel 1 ( echo   Ha mudancas staged. Commit ou reset antes de publicar. & exit /b 1 )

echo [2/6] Calculando versao...
set "PREV_TAG="
for /f "delims=" %%t in ('git tag --list "v*" --sort=-v:refname') do if not defined PREV_TAG set "PREV_TAG=%%t"
if not defined PREV_TAG set "PREV_TAG=v0.0.0"

set "BUMP=%~1"
if not defined BUMP (
  set "BUMP=patch"
  git log %PREV_TAG%..HEAD --pretty=%%B 2>nul | findstr /i /c:"[major]" >nul && set "BUMP=major"
  if "!BUMP!"=="patch" ( git log %PREV_TAG%..HEAD --pretty=%%B 2>nul | findstr /i /c:"[minor]" >nul && set "BUMP=minor" )
)
if not "%BUMP%"=="major" if not "%BUMP%"=="minor" if not "%BUMP%"=="patch" (
  echo   Bump invalido: "%BUMP%" - use patch, minor ou major. & exit /b 1
)

for /f "tokens=1-3 delims=." %%a in ("%PREV_TAG:v=%") do set "MAJ=%%a" & set "MIN=%%b" & set "PAT=%%c"
if not defined PAT set "PAT=0"
set /a "MAJ+=0" & set /a "MIN+=0" & set /a "PAT+=0"

if "!BUMP!"=="major" ( set /a "MAJ+=1" & set "MIN=0" & set "PAT=0" )
if "!BUMP!"=="minor" ( set /a "MIN+=1" & set "PAT=0" )
if "!BUMP!"=="patch" ( set /a "PAT+=1" )

set "NEW_VERSION=!MAJ!.!MIN!.!PAT!"
set "NEW_TAG=v!NEW_VERSION!"

git rev-parse -q --verify refs/tags/%NEW_TAG% >nul 2>&1
if not errorlevel 1 ( echo   A tag %NEW_TAG% ja existe. Abortando. & exit /b 1 )

REM versionCode monotono: maior code ja publicado (ou o atual) + 1.
set "PREV_CODE=0"
for /f "tokens=3" %%c in ('git show %PREV_TAG%:app/build.gradle.kts 2^>nul ^| findstr /r "versionCode"') do set "PREV_CODE=%%c"
set "CUR_CODE=0"
for /f "tokens=3" %%c in ('findstr /r "versionCode" app\build.gradle.kts') do set "CUR_CODE=%%c"
set /a "NEW_CODE=PREV_CODE+1"
if !CUR_CODE! GTR !NEW_CODE! set /a "NEW_CODE=CUR_CODE+1"

echo   %PREV_TAG% (versionCode %PREV_CODE%) -^> %NEW_TAG% (versionCode %NEW_CODE%) [bump %BUMP%]

echo [3/6] Aplicando bump em app\build.gradle.kts...
powershell -NoProfile -Command "$p='app\build.gradle.kts';$t=[IO.File]::ReadAllText($p);$t=$t -replace 'versionCode = \d+','versionCode = %NEW_CODE%' -replace ('versionName = \x22[\d.]*\x22'),('versionName = '+[char]34+'%NEW_VERSION%'+[char]34);[IO.File]::WriteAllText($p,$t)"
if errorlevel 1 ( echo   Falha ao atualizar a versao. & exit /b 1 )
for /f "tokens=3" %%v in ('findstr /r "versionName" app\build.gradle.kts') do set "GOT_VERSION=%%v"
set "GOT_VERSION=!GOT_VERSION:"=!"
for /f "tokens=3" %%c in ('findstr /r "versionCode" app\build.gradle.kts') do set "GOT_CODE=%%c"
if not "!GOT_VERSION!"=="%NEW_VERSION%" ( echo   Bump falhou: versionName=!GOT_VERSION!. & exit /b 1 )
if not "!GOT_CODE!"=="%NEW_CODE%" ( echo   Bump falhou: versionCode=!GOT_CODE!. & exit /b 1 )

echo [4/6] Testes JVM...
call "%GRADLEW%" test
if errorlevel 1 ( echo   TESTES FALHARAM. & exit /b 1 )

echo [5/6] Build release...
call "%GRADLEW%" assembleRelease
if errorlevel 1 ( echo   BUILD FALHOU. & exit /b 1 )
if not exist "app\build\outputs\apk\release\app-release.apk" ( echo   APK nao encontrado apos o build. & exit /b 1 )

echo [6/6] Commit, tag, push e GitHub Release...
git add app/build.gradle.kts
git commit -m "chore: bump version to %NEW_TAG% [skip ci]"
if errorlevel 1 ( echo   Falha no commit. & exit /b 1 )
git tag %NEW_TAG%
git push origin main
if errorlevel 1 ( echo   Push para main falhou. & exit /b 1 )
git push origin HEAD:production
if errorlevel 1 ( echo   Push para production falhou. & exit /b 1 )
git push origin %NEW_TAG%
if errorlevel 1 ( echo   Push da tag falhou. & exit /b 1 )
gh release create %NEW_TAG% app\build\outputs\apk\release\app-release.apk --title "%NEW_TAG%" --generate-notes
if errorlevel 1 ( echo   Falha ao criar a GitHub Release. & exit /b 1 )

echo.
echo   Publicado: https://github.com/MatheusGoncalves540/ChronoPass/releases/tag/%NEW_TAG%
endlocal
