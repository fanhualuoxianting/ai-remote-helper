param(
    [int]$Port = 18080,
    [switch]$Offline,
    [switch]$SkipDocker,
    [switch]$OpenFirewall
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir
Set-Location $RepoRoot

function Write-Step {
    param([string]$Message)
    Write-Host "[AI Remote Helper] $Message" -ForegroundColor Cyan
}

function Test-CommandAvailable {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Get-MavenCommand {
    $bundledMaven = Join-Path $RepoRoot ".tools\apache-maven-3.9.9\bin\mvn.cmd"
    if (Test-Path $bundledMaven) {
        return $bundledMaven
    }
    if (Test-CommandAvailable "mvn") {
        return "mvn"
    }
    throw "Maven not found. Install Maven 3.9+ or place it under .tools\apache-maven-3.9.9."
}

function Test-RelayHealth {
    param([int]$HealthPort)
    try {
        $health = Invoke-RestMethod "http://localhost:$HealthPort/api/health" -TimeoutSec 3
        return $health.status -eq "UP"
    } catch {
        return $false
    }
}

Write-Step "Preparing LAN relay server on port $Port"

$listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) {
    if (Test-RelayHealth -HealthPort $Port) {
        Write-Host "Relay server is already running at http://localhost:$Port" -ForegroundColor Green
        Write-Host "Ask the assisted computer to use this machine's LAN IP and port $Port."
        $addresses = Invoke-RestMethod "http://localhost:$Port/api/network/addresses"
        foreach ($item in @($addresses)) {
            if ($item.address) {
                Write-Host ("- {0}: {1}" -f $item.interfaceName, $item.address)
            }
        }
        exit 0
    }
    throw "Port $Port is already in use by process $($listener.OwningProcess), but it is not AI Remote Helper."
}

if (-not $SkipDocker) {
    if (Test-CommandAvailable "docker") {
        Write-Step "Checking PostgreSQL and Redis containers"
        $postgres = docker ps -a --filter "name=airh-postgres" --format "{{.Names}}"
        $redis = docker ps -a --filter "name=airh-redis" --format "{{.Names}}"
        if ($postgres) {
            docker start airh-postgres | Out-Null
        } else {
            Write-Warning "Container airh-postgres was not found. Create it first or configure AIRH_DATASOURCE_URL."
        }
        if ($redis) {
            docker start airh-redis | Out-Null
        } else {
            Write-Warning "Container airh-redis was not found. Create it first or configure AIRH_REDIS_HOST/AIRH_REDIS_PORT."
        }
    } else {
        Write-Warning "Docker command was not found. Skipping container startup."
    }
}

if ($OpenFirewall) {
    Write-Step "Adding Windows Firewall inbound rule for TCP $Port"
    $ruleName = "AI Remote Helper Relay $Port"
    $existingRule = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
    if (-not $existingRule) {
        New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Protocol TCP -LocalPort $Port -Action Allow | Out-Null
    }
}

$mvn = Get-MavenCommand
$mavenArgs = @()
$localRepo = Join-Path $RepoRoot ".m2\repository"
if (Test-Path $localRepo) {
    $mavenArgs += "-Dmaven.repo.local=$localRepo"
}
if ($Offline) {
    $mavenArgs += "-o"
}
$mavenArgs += @(
    "-pl",
    "relay-server",
    "-am",
    "spring-boot:run",
    "-Dspring-boot.run.arguments=--server.port=$Port"
)

Write-Step "Starting relay server. Keep this window open while your classmate connects."
Write-Host "Local health check: http://localhost:$Port/api/health"
Write-Host "LAN address list:  http://localhost:$Port/api/network/addresses"
Write-Host ""

& $mvn @mavenArgs
