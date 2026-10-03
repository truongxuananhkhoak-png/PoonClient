@echo off
setlocal EnableExtensions
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Chua tim thay Java. Hay cai JDK 21 va thu lai.
  pause
  exit /b 1
)

for /f "tokens=3" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%~V
echo Java hien tai: %JAVA_VER%
echo.

set "GRADLE_VERSION=9.2.1"
set "TOOLS_DIR=%LOCALAPPDATA%\PoonClientBuild"
set "GRADLE_HOME=%TOOLS_DIR%\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%TOOLS_DIR%\gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"
  echo Dang tai Gradle %GRADLE_VERSION%... Can ket noi Internet.
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'"
  if errorlevel 1 (
    echo [ERROR] Khong tai duoc Gradle. Kiem tra ket noi Internet va chay lai.
    pause
    exit /b 1
  )
  echo Dang giai nen Gradle...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%GRADLE_ZIP%' -DestinationPath '%TOOLS_DIR%' -Force"
  if errorlevel 1 (
    echo [ERROR] Khong giai nen duoc Gradle.
    pause
    exit /b 1
  )
)

echo.
echo Dang build Poon Client. Lan dau can Internet de tai Minecraft/Fabric dependencies...
call "%GRADLE_HOME%\bin\gradle.bat" --no-daemon clean build
if errorlevel 1 (
  echo.
  echo [FAILED] Build that bai. Hay chup lai toan bo thong bao loi gui cho nguoi ho tro.
  pause
  exit /b 1
)

echo.
echo [SUCCESS] Build hoan tat. Cac file JAR nam trong:
echo %CD%\build\libs\
explorer "%CD%\build\libs"
pause
