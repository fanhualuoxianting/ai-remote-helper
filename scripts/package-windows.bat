@echo off
REM AI Remote Helper - Windows Packaging Script
REM Requires: Java 21+ with JAVA_HOME set

echo === AI Remote Helper - Windows Packaging ===
echo.

REM Check JAVA_HOME
if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME not set. Please install Java 21+ and set JAVA_HOME.
    exit /b 1
)

set JPACKAGE=%JAVA_HOME%\bin\jpackage.exe
if not exist "%JPACKAGE%" (
    echo ERROR: jpackage not found at %JPACKAGE%
    echo Please ensure JAVA_HOME points to JDK 14+ (not JRE).
    exit /b 1
)

echo Using jpackage: %JPACKAGE%
echo.

REM Build fat jar
echo [1/3] Building fat jar...
call .\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package -pl agent-client -am -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven build failed!
    exit /b 1
)
echo Fat jar built: agent-client\target\agent-client-all.jar
echo.

REM Prepare dist directory
echo [2/3] Preparing distribution...
if exist dist rmdir /s /q dist
mkdir dist

copy agent-client\target\agent-client-all.jar dist\ >nul

REM Copy JavaFX SDK
mkdir dist\javafx-sdk\lib >nul 2>nul
set M2=%USERPROFILE%\.m2\repository\org\openjfx

copy "%M2%\javafx-base\21.0.5\javafx-base-21.0.5.jar" "dist\javafx-sdk\lib\" >nul
copy "%M2%\javafx-base\21.0.5\javafx-base-21.0.5-win.jar" "dist\javafx-sdk\lib\" >nul
copy "%M2%\javafx-controls\21.0.5\javafx-controls-21.0.5.jar" "dist\javafx-sdk\lib\" >nul
copy "%M2%\javafx-controls\21.0.5\javafx-controls-21.0.5-win.jar" "dist\javafx-sdk\lib\" >nul
copy "%M2%\javafx-graphics\21.0.5\javafx-graphics-21.0.5.jar" "dist\javafx-sdk\lib\" >nul
copy "%M2%\javafx-graphics\21.0.5\javafx-graphics-21.0.5-win.jar" "dist\javafx-sdk\lib\" >nul

REM Create launcher
(
echo @echo off
echo title AI Remote Helper
echo set DIR=%%~dp0
echo "%%JAVA_HOME%%\bin\java.exe" --module-path "%%DIR%%javafx-sdk\lib" --add-modules javafx.controls --add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED --add-opens=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED -jar "%%DIR%%agent-client-all.jar"
) > dist\AI-Remote-Helper.bat

echo.
echo [3/3] Done!
echo.
echo Distribution: dist\
echo Launch:       dist\AI-Remote-Helper.bat
echo.
echo Files:
dir /s dist\*.jar dist\*.bat 2>nul | findstr /i "File"
echo.
echo Total size:
powershell -Command "(Get-ChildItem dist -Recurse -File | Measure-Object -Property Length -Sum).Sum / 1MB"
echo MB
