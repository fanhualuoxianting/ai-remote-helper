param(
    [switch]$Offline,
    [string]$MavenRepo,
    [string]$OutputRoot,
    [string]$AppVersion = "0.1.0"
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path (Join-Path $ScriptDir "..\..")
$Maven = Join-Path $ProjectRoot ".tools\apache-maven-3.9.9\bin\mvn.cmd"
$DefaultRepo = Join-Path $ProjectRoot ".m2\repository"
$DistRoot = if ($OutputRoot) { $OutputRoot } else { Join-Path $ProjectRoot "dist" }
$AppName = "AI-Remote-Helper-LAN"
$AppImage = Join-Path $DistRoot $AppName
$InputDir = Join-Path $ProjectRoot "agent-client\target\jpackage-input-lan"
$JarPath = Join-Path $ProjectRoot "agent-client\target\agent-client-all.jar"
$ReadmeTemplate = Join-Path $ProjectRoot "packaging\README-USER-LAN.txt"

function Assert-Command($Name, $Hint) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "$Name not found. $Hint"
    }
}

Assert-Command "jpackage" "Please run with Java 21+ JDK on PATH."
Assert-Command "java" "Please install Java 21+."

if (-not (Test-Path $Maven)) {
    throw "Bundled Maven not found: $Maven"
}

if (-not $MavenRepo) {
    $MavenRepo = $DefaultRepo
}

Write-Host "=== AI Remote Helper LAN Edition Packaging ==="
Write-Host "Project: $ProjectRoot"
Write-Host "Output : $AppImage"
Write-Host ""

$mavenArgs = @("-Dmaven.repo.local=$MavenRepo")
if ($Offline) {
    $mavenArgs += "-o"
}
$mavenArgs += @("clean", "package", "-pl", "agent-client", "-am", "-DskipTests")

Write-Host "[1/4] Building agent-client fat jar..."
& $Maven @mavenArgs
if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed with exit code $LASTEXITCODE"
}
if (-not (Test-Path $JarPath)) {
    throw "Expected jar not found: $JarPath"
}

Write-Host "[2/4] Preparing jpackage input..."
if (Test-Path $InputDir) {
    Remove-Item -LiteralPath $InputDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $InputDir | Out-Null
Copy-Item -LiteralPath $JarPath -Destination (Join-Path $InputDir "agent-client-all.jar") -Force

Write-Host "[3/4] Creating app image with bundled runtime..."
if (Test-Path $AppImage) {
    Remove-Item -LiteralPath $AppImage -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $DistRoot | Out-Null

$jpackageArgs = @(
    "--type", "app-image",
    "--dest", $DistRoot,
    "--name", $AppName,
    "--app-version", $AppVersion,
    "--input", $InputDir,
    "--main-jar", "agent-client-all.jar",
    "--main-class", "com.airh.agent.ui.AgentClientLauncher",
    "--java-options", "-Dairh.profile=lan",
    "--java-options", "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
    "--java-options", "--add-opens=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED"
)

& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE"
}

Write-Host "[4/4] Copying LAN user guide..."
if (-not (Test-Path $ReadmeTemplate)) {
    throw "README template not found: $ReadmeTemplate"
}
Copy-Item -LiteralPath $ReadmeTemplate -Destination (Join-Path $AppImage "README-USER.txt") -Force

Write-Host ""
Write-Host "Packaging completed successfully."
Write-Host "App image: $AppImage"
Write-Host "Launcher : $(Join-Path $AppImage 'AI-Remote-Helper-LAN.exe')"
