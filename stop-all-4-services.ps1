$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$StateDir = Join-Path $RepoRoot ".run-state"

function Write-Step {
    param([string]$Message)
    Write-Host "[stop-4] $Message" -ForegroundColor Cyan
}

function Stop-ManagedProcessByPidFile {
    param(
        [string]$Name,
        [string]$PidFilePath
    )

    if (-not (Test-Path $PidFilePath)) {
        Write-Step "$Name pid file not found, skip."
        return
    }

    $pidText = (Get-Content -LiteralPath $PidFilePath -ErrorAction SilentlyContinue | Select-Object -First 1)
    $pidValue = 0
    if (-not $pidText -or -not [int]::TryParse($pidText, [ref]$pidValue)) {
        Write-Step "$Name pid file is invalid, removing."
        Remove-Item -LiteralPath $PidFilePath -Force -ErrorAction SilentlyContinue
        return
    }

    $proc = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($proc) {
        Stop-Process -Id $pidValue -Force
        Write-Step "$Name stopped (pid=$pidValue)."
    }
    else {
        Write-Step "$Name process already exited (pid=$pidValue)."
    }

    Remove-Item -LiteralPath $PidFilePath -Force -ErrorAction SilentlyContinue
}

function Invoke-DockerComposeDown {
    param([string]$WorkDir)

    if (-not (Test-Path $WorkDir)) {
        Write-Step "Directory not found, skip: $WorkDir"
        return
    }

    Push-Location $WorkDir
    try {
        & docker compose down
    }
    finally {
        Pop-Location
    }
}

function Stop-PortListeners {
    param(
        [int[]]$Ports
    )

    foreach ($port in $Ports) {
        $connections = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
        if (-not $connections) {
            continue
        }

        $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($pidValue in $pids) {
            if ($pidValue -le 0) {
                continue
            }
            try {
                Stop-Process -Id $pidValue -Force -ErrorAction Stop
                Write-Step "Stopped process on port $port (pid=$pidValue)."
            }
            catch {
                Write-Step "Failed to stop pid=$pidValue on port ${port}: $($_.Exception.Message)"
            }
        }
    }
}

Write-Step "Stopping local managed processes..."
Stop-ManagedProcessByPidFile -Name "WB interview agent 8010 (local)" -PidFilePath (Join-Path $StateDir "agent-8010.pid")
Stop-ManagedProcessByPidFile -Name "resume parser 8089 (local)" -PidFilePath (Join-Path $StateDir "resume-8089.pid")
Stop-ManagedProcessByPidFile -Name "spring backend 8080" -PidFilePath (Join-Path $StateDir "backend-8080.pid")
Stop-ManagedProcessByPidFile -Name "sv-service 8082" -PidFilePath (Join-Path $StateDir "sv-8082.pid")
Stop-ManagedProcessByPidFile -Name "vue frontend 5173" -PidFilePath (Join-Path $StateDir "frontend-5173.pid")

Write-Step "Stopping docker-compose services (8010 / 8089)..."
Invoke-DockerComposeDown -WorkDir (Join-Path $RepoRoot "WB_interview_agent")
Invoke-DockerComposeDown -WorkDir (Join-Path $RepoRoot "resume_cut_docker")

if (Get-Command docker -ErrorAction SilentlyContinue) {
    try {
        & docker rm -f funasr-offline | Out-Null
        Write-Step "Stopped docker container funasr-offline (10095)."
    }
    catch {
        Write-Step "Skip stopping funasr-offline: $($_.Exception.Message)"
    }
}

Write-Step "Ensuring ports are released (8010/8089/8080/8082/5173/10095)..."
Stop-PortListeners -Ports @(8010, 8089, 8080, 8082, 5173, 10095)

Write-Step "Done."
