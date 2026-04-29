# AI Remote Helper - Agent Client Packaging Script
# Requires: Java 21+ with jpackage

param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$OutputDir = ".\dist"
)

Write-Host "=== AI Remote Helper Agent Client Packaging ===" -ForegroundColor Cyan

# Build the project first
Write-Host "Building project..." -ForegroundColor Yellow
& ".\.tools\apache-maven-3.9.9\bin\mvn.cmd" clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# Create output directory
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

# Find jpackage
$jpackage = Join-Path $JavaHome "bin\jpackage.exe"
if (-not (Test-Path $jpackage)) {
    Write-Host "jpackage not found at $jpackage" -ForegroundColor Red
    Write-Host "Please ensure JAVA_HOME points to JDK 14+" -ForegroundColor Yellow
    exit 1
}

Write-Host "Using jpackage: $jpackage" -ForegroundColor Green

# Create runtime image
Write-Host "Creating application image..." -ForegroundColor Yellow
& $jpackage `
    --type app-image `
    --name "AI-Remote-Helper-Agent" `
    --input "agent-client\target" `
    --main-jar "agent-client-0.1.0-SNAPSHOT.jar" `
    --main-class "com.airh.agent.AgentClientApplication" `
    --dest $OutputDir `
    --java-options "--add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" `
    --java-options "--add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED" `
    --java-options "--add-opens javafx.fxml/com.sun.javafx.fxml=ALL-UNNAMED"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nPackaging successful! Output: $OutputDir\AI-Remote-Helper-Agent" -ForegroundColor Green
} else {
    Write-Host "`nPackaging failed!" -ForegroundColor Red
    exit 1
}
