@echo off
REM AI Remote Helper - Agent Client Packaging Script
REM Requires: Java 21+ with jpackage

echo === AI Remote Helper Agent Client Packaging ===

REM Build the project first
echo Building project...
call .\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b 1
)

REM Create output directory
if not exist "dist" mkdir "dist"

REM Find jpackage
if "%JAVA_HOME%"=="" (
    echo JAVA_HOME not set!
    exit /b 1
)

set JPACKAGE=%JAVA_HOME%\bin\jpackage.exe
if not exist "%JPACKAGE%" (
    echo jpackage not found at %JPACKAGE%
    echo Please ensure JAVA_HOME points to JDK 14+
    exit /b 1
)

echo Using jpackage: %JPACKAGE%

REM Create runtime image
echo Creating application image...
"%JPACKAGE%" ^
    --type app-image ^
    --name "AI-Remote-Helper-Agent" ^
    --input "agent-client\target" ^
    --main-jar "agent-client-0.1.0-SNAPSHOT.jar" ^
    --main-class "com.airh.agent.AgentClientApplication" ^
    --dest "dist" ^
    --java-options "--add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" ^
    --java-options "--add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED" ^
    --java-options "--add-opens javafx.fxml/com.sun.javafx.fxml=ALL-UNNAMED"

if %ERRORLEVEL% equ 0 (
    echo.
    echo Packaging successful! Output: dist\AI-Remote-Helper-Agent
) else (
    echo.
    echo Packaging failed!
    exit /b 1
)
