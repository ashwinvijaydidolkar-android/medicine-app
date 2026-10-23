@echo off
setlocal EnableExtensions
title Ragini - build the Android app
cd /d "%~dp0"
echo.
echo  ==========================================================
echo     Ragini Medicine Reminder - building the Android app
echo  ==========================================================
echo.
echo  This makes the file Ragini-MedicineReminder.apk that you
echo  install on the Android phone. It needs Android Studio
echo  (free, from developer.android.com/studio), opened once.
echo.

rem ---- 1. Find Java (Android Studio includes it) ----
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto havejava
set "JAVA_HOME="
if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if not defined JAVA_HOME if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
if not defined JAVA_HOME if exist "%ProgramFiles%\Android\Android Studio\jre\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jre"
if not defined JAVA_HOME goto nostudio
:havejava
echo  [OK] Java found: %JAVA_HOME%

rem ---- 2. Find the Android SDK ----
if defined ANDROID_HOME if exist "%ANDROID_HOME%\platforms" goto havesdk
set "ANDROID_HOME="
if exist "%LOCALAPPDATA%\Android\Sdk\platforms" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if not defined ANDROID_HOME goto nosdk
:havesdk
echo  [OK] Android SDK found: %ANDROID_HOME%
set "SDKPATH=%ANDROID_HOME:\=\\%"
> local.properties echo sdk.dir=%SDKPATH%

rem ---- 3. Build ----
echo.
echo  Building... the first time takes 5 to 15 minutes (it downloads tools).
echo.
call "%~dp0gradlew.bat" --no-daemon assembleRelease
if errorlevel 1 goto failed
copy /y "app\build\outputs\apk\release\app-release.apk" "Ragini-MedicineReminder.apk" >nul
echo.
echo  ==========================================================
echo   DONE!  Ragini-MedicineReminder.apk is ready in this folder.
echo   Copy it to the phone (WhatsApp to yourself, USB or Drive),
echo   tap it, and allow "Install unknown apps" when asked.
echo  ==========================================================
explorer /select,"%~dp0Ragini-MedicineReminder.apk"
pause
exit /b 0

:nostudio
echo  [!] Android Studio was not found.
echo      1. Install it from https://developer.android.com/studio
echo      2. Open it once and finish the setup (it downloads the Android SDK).
echo      3. Double-click this file again.
pause
exit /b 1

:nosdk
echo  [!] The Android SDK was not found.
echo      Open Android Studio once and finish the setup wizard
echo      (Standard install). Then double-click this file again.
pause
exit /b 1

:failed
echo.
echo  [!] The build stopped with an error.
echo      Please take a photo or screenshot of this window and send it
echo      to Claude - it will be fixed straight away.
pause
exit /b 1
