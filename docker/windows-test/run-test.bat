@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo OpenAqua Windows Hardware Test Automated Runner
echo ========================================================

:: Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Java is already installed:
    java -version
    goto RUN_TESTS
)

echo Java not found. Installing OpenJDK 21 via winget...
winget install --id EclipseAdoptium.Temurin.21.JDK -e --silent --accept-package-agreements --accept-source-agreements
if %ERRORLEVEL% NEQ 0 (
    echo winget failed or unavailable, falling back to direct MSI download...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.6%%2B7/OpenJDK21U-jdk_x64_windows_hotspot_21.0.6_7.msi' -OutFile 'C:\jdk21.msi'"
    msiexec /i C:\jdk21.msi /quiet /qn /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith
    del C:\jdk21.msi
)

:: Refresh PATH from registry
for /f "tokens=2*" %%a in ('reg query "HKLM\System\CurrentControlSet\Control\Session Manager\Environment" /v Path 2^>nul') do set "SYS_PATH=%%b"
for /f "tokens=2*" %%a in ('reg query "HKCU\Environment" /v Path 2^>nul') do set "USR_PATH=%%b"
set "PATH=%SYS_PATH%;%USR_PATH%;%PATH%"

echo Checking Java installation...
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to install Java 21!
    exit /b 1
)
java -version

:RUN_TESTS
echo.
echo Navigating to workspace...
cd /d C:\workspace

echo Checking OCTO USB Device presence in Windows PnP...
powershell -Command "Get-PnpDevice | Where-Object { $_.InstanceId -like '*0C70*' } | Select-Object Status, Class, FriendlyName, InstanceId"

echo.
echo Running OCTO hardware tests via Gradle...
call gradlew.bat :engine:engine-prod:mingwX64Test -Ptest.hardware=true --info

set TEST_RESULT=%ERRORLEVEL%
if %TEST_RESULT% EQU 0 (
    echo.
    echo ========================================================
    echo SUCCESS: All OCTO Windows Hardware tests passed!
    echo ========================================================
) else (
    echo.
    echo ========================================================
    echo FAILURE: OCTO Windows Hardware tests failed with exit code %TEST_RESULT%
    echo ========================================================
)

exit /b %TEST_RESULT%
