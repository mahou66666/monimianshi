<#
.SYNOPSIS
    Starts (or stops) the complete AI Mock Interview Platform - all 9 services.

.DESCRIPTION
    start-all-4-services.ps1 only covers 6 endpoints. This script adds the ones
    it omits and applies the parameter overrides the configs need:

      port   service                     notes
      ----   -------------------------   -------------------------------------
      8010   WB_interview_agent           docker compose, falls back to python
      8089   resume_cut_docker             jar (built by bootstrap-deps.ps1)
      8080   springboot-front(4)           Gradle / jar, wires the FunASR url
      8082   sv-service (FunASR gateway)   jar, needs 10095 up
      8000   Interview_elf_agent           uvicorn, QDRANT_HOST=localhost
      18081  admin-server                  jar, Redis remapped to 16379
      9527   admin-web                     vue-cli dev server
      5173   vue-front (4)                 vite dev server
      10095  FunASR websocket               docker container funasr-offline

    Infrastructure containers (Postgres / Redis / Qdrant / FunASR) are expected
    to be running already - run .\bootstrap-deps.ps1 first.

.PARAMETER Stop
    Stop every service recorded in .run-state instead of starting them.

.EXAMPLE
    .\run-full-stack.ps1
    .\run-full-stack.ps1 -SkipElf8000 -SkipAdminWeb9527
    .\run-full-stack.ps1 -Stop
#>
[CmdletBinding()]
param(
    [switch]$Stop,
    [switch]$SkipInfra,
    [switch]$SkipAgent8010,
    [switch]$SkipResume8089,
    [switch]$SkipBackend8080,
    [switch]$SkipSv8082,
    [switch]$SkipFrontend5173,
    [switch]$SkipAdmin18081,
    [switch]$SkipAdminWeb9527,
    [switch]$SkipElf8000,
    [switch]$SkipFunAsr10095
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$StateDir = Join-Path $RepoRoot '.run-state'
$LogDir   = Join-Path $RepoRoot '.run-logs'
New-Item -ItemType Directory -Force -Path $StateDir | Out-Null
New-Item -ItemType Directory -Force -Path $LogDir   | Out-Null

# ---------------------------------------------------------------- helpers ---

function Write-Step { param([string]$m) Write-Host "[stack] $m" -ForegroundColor Cyan }
function Write-Warn { param([string]$m) Write-Host "[stack] $m" -ForegroundColor Yellow }
function Write-Ok   { param([string]$m) Write-Host "[stack] $m" -ForegroundColor Green }

function Test-PortListening {
    param([int]$Port, [string]$TargetHost = '127.0.0.1', [int]$TimeoutMs = 500)
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $iar = $client.BeginConnect($TargetHost, $Port, $null, $null)
        if (-not $iar.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) { return $false }
        $client.EndConnect($iar) | Out-Null
        return $true
    }
    catch { return $false }
    finally { $client.Close() }
}

function Wait-Port {
    param([int]$Port, [int]$Retries = 60, [int]$DelaySeconds = 2)
    for ($i = 0; $i -lt $Retries; $i++) {
        if (Test-PortListening -Port $Port) { return $true }
        Start-Sleep -Seconds $DelaySeconds
    }
    return $false
}

function Get-PidFile {
    param([string]$Name)
    return (Join-Path $StateDir "$Name.pid")
}

function Start-ManagedProcess {
    param(
        [string]$Name,
        [string]$WorkDir,
        [string]$FilePath,
        [string[]]$Arguments,
        [int]$Port = 0,
        [int]$Retries = 90,
        [int]$DelaySeconds = 2
    )

    $pidFile = Get-PidFile -Name $Name

    if (Test-Path $pidFile) {
        $oldText = Get-Content -LiteralPath $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1
        $oldPid = 0
        if ($oldText -and [int]::TryParse($oldText, [ref]$oldPid)) {
            if (Get-Process -Id $oldPid -ErrorAction SilentlyContinue) {
                Write-Step "$Name already running (pid=$oldPid)"
                return
            }
        }
        Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
    }

    if (($Port -gt 0) -and (Test-PortListening -Port $Port)) {
        Write-Step "port $Port already listening; skip $Name"
        return
    }

    $out = Join-Path $LogDir "$Name.out.log"
    $err = Join-Path $LogDir "$Name.err.log"

    $proc = Start-Process `
        -FilePath $FilePath `
        -ArgumentList $Arguments `
        -WorkingDirectory $WorkDir `
        -RedirectStandardOutput $out `
        -RedirectStandardError $err `
        -WindowStyle Hidden `
        -PassThru

    Set-Content -LiteralPath $pidFile -Value $proc.Id -Encoding ASCII
    Write-Ok "$Name started (pid=$($proc.Id))"

    if ($Port -gt 0) {
        if (Wait-Port -Port $Port -Retries $Retries -DelaySeconds $DelaySeconds) {
            Write-Ok "$Name listening on $Port"
        }
        else {
            Write-Warn "$Name did not open port $Port in time - check $out / $err"
        }
    }
}

function Stop-ManagedProcess {
    param([string]$Name)
    $pidFile = Get-PidFile -Name $Name
    if (-not (Test-Path $pidFile)) { return }
    $text = Get-Content -LiteralPath $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1
    $procId = 0
    if ($text -and [int]::TryParse($text, [ref]$procId)) {
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($proc) {
            # Kill the whole tree: cmd.exe wrappers leave java/node children behind.
            Invoke-Native -Exe 'taskkill.exe' -Arguments @('/PID', "$procId", '/T', '/F') -Capture | Out-Null
            Write-Ok "$Name stopped (pid=$procId)"
        }
    }
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

function Resolve-Jar {
    param([string]$Dir, [string]$ExcludePattern = '')
    if (-not (Test-Path $Dir)) { return '' }
    $items = @(
        Get-ChildItem -LiteralPath $Dir -Filter '*.jar' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*.original' -and ($ExcludePattern -eq '' -or $_.Name -notlike $ExcludePattern) } |
        Sort-Object LastWriteTime -Descending
    )
    if ($items.Count -eq 0) { return '' }
    return $items[0].FullName
}

# Native tools write to stderr even on expected paths (e.g. 'docker version'
# when the daemon is down). Under $ErrorActionPreference='Stop' that would abort
# the script, so run natives with 'Continue' and branch on the exit code.
function Invoke-Native {
    param(
        [string]$Exe,
        [string[]]$Arguments,
        [switch]$Capture
    )
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        if ($Capture) {
            $text = (& $Exe @Arguments 2>&1 | Out-String)
            return [pscustomobject]@{ ExitCode = $LASTEXITCODE; Output = $text }
        }
        & $Exe @Arguments 2>&1 | Out-Null
        return [pscustomobject]@{ ExitCode = $LASTEXITCODE; Output = '' }
    }
    finally {
        $ErrorActionPreference = $prev
    }
}

# Node is unpacked directly into D:\ on this machine, so npm.cmd is not on PATH.
# Resolve it once, then hand the full path to cmd.exe with the node directory
# prepended so the npm shim can locate its sibling node.exe.
function Resolve-Npm {
    # cmd.exe cannot execute the PowerShell npm.ps1 shim. Prefer npm.cmd.
    $candidates = @(
        'D:\npm.cmd',
        'D:\nodejs\npm.cmd',
        "$env:ProgramFiles\nodejs\npm.cmd",
        "${env:ProgramFiles(x86)}\nodejs\npm.cmd",
        "$env:LOCALAPPDATA\Programs\nodejs\npm.cmd",
        "$env:APPDATA\npm\npm.cmd",
        'C:\nodejs\npm.cmd',
        'C:\tools\nodejs\npm.cmd'
    )
    foreach ($c in $candidates) {
        if ($c -and (Test-Path -LiteralPath $c)) { return $c }
    }
    return $null
}

$script:NpmPath = Resolve-Npm
if ($script:NpmPath) {
    $script:NodeDir = Split-Path -Parent $script:NpmPath
    Write-Host "[stack] npm: $($script:NpmPath)" -ForegroundColor DarkGray
}
else {
    Write-Host '[stack] npm not found - admin-web and vue-front cannot be started by this script.' -ForegroundColor Yellow
}

# The nine managed units, in dependency order.
$UnitNames = @(
    'agent-8010', 'resume-8089', 'backend-8080', 'sv-8082',
    'elf-8000', 'admin-18081', 'admin-web-9527', 'frontend-5173'
)

# ------------------------------------------------------------------- stop ----

if ($Stop) {
    Write-Step 'Stopping all managed services...'
    foreach ($n in $UnitNames) { Stop-ManagedProcess -Name $n }
    Write-Host ''
    Write-Step 'Infrastructure containers are left running. Stop them with:'
    Write-Host '   docker compose -f docker-compose.dev.yml down          # redis'      -ForegroundColor Gray
    Write-Host '   docker compose -f WB_interview_agent/docker-compose.yml down   # postgres' -ForegroundColor Gray
    Write-Host '   docker compose -f Interview_elf_agent/docker-compose.yml down  # qdrant'   -ForegroundColor Gray
    Write-Host '   docker rm -f funasr-offline                            # FunASR'      -ForegroundColor Gray
    exit 0
}

# --------------------------------------------------------------- infra check -

Write-Step "repo root: $RepoRoot"

if (-not $SkipInfra) {
    $dockerOk = $false
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        $dockerOk = ((Invoke-Native -Exe 'docker' -Arguments @('version') -Capture).ExitCode -eq 0)
    }

    if (-not $dockerOk) {
        Write-Warn 'Docker is not reachable - infrastructure containers cannot be verified.'
    }
    else {
        if (Test-PortListening -Port 5432) { Write-Ok 'Postgres reachable (5432)' }
        else { Write-Warn 'Postgres 5432 not listening - start it: docker compose -f WB_interview_agent/docker-compose.yml up -d postgres' }

        if (Test-PortListening -Port 16379) { Write-Ok 'Redis reachable (16379)' }
        else { Write-Warn 'Redis 16379 not listening - start it: docker compose -f docker-compose.dev.yml up -d redis' }

        if (Test-PortListening -Port 6333) { Write-Ok 'Qdrant reachable (6333)' }
        else { Write-Warn 'Qdrant 6333 not listening - start it: docker compose -f Interview_elf_agent/docker-compose.yml up -d qdrant' }
    }
}

# ======================================================== 1) FunASR (10095) ===

if (-not $SkipFunAsr10095) {
    if (Test-PortListening -Port 10095) {
        Write-Step 'FunASR ws already listening on 10095'
    }
    else {
        Write-Warn 'FunASR ws (10095) is not running. Start it with:'
        Write-Host '   docker start funasr-offline' -ForegroundColor Gray
        Write-Host '   (or re-run .\bootstrap-deps.ps1 to create and warm it up)' -ForegroundColor Gray
    }
}

# ================================================= 2) WB interview agent 8010 =

if (-not $SkipAgent8010) {
    $dir = Join-Path $RepoRoot 'WB_interview_agent'
    $dockerOk = $false
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        $dockerOk = ((Invoke-Native -Exe 'docker' -Arguments @('version') -Capture).ExitCode -eq 0)
    }

    if ((Test-PortListening -Port 8010) -or (Test-Path (Get-PidFile -Name 'agent-8010'))) {
        Write-Step 'WB interview agent already running on 8010'
    }
    elseif ($dockerOk) {
        $composeUpOk = $false
        Push-Location $dir
        try {
            $up = Invoke-Native -Exe 'docker' -Arguments @('compose', 'up', '-d', 'postgres', 'app') -Capture
            if ($up.ExitCode -ne 0) { throw "docker compose up failed (exit $($up.ExitCode))" }
            $composeUpOk = $true
        }
        catch {
            Write-Warn "docker compose for 8010 failed: $($_.Exception.Message)"
            Write-Warn 'continuing without docker; falling back to local python'
        }
        finally { Pop-Location }

        if ($composeUpOk) {
            if (-not (Wait-Port -Port 8010 -Retries 90 -DelaySeconds 2)) {
                Write-Warn 'docker app did not open 8010; falling back to local python'
            }
            else {
                Write-Ok 'WB interview agent listening on 8010'
            }
        }
    }

    if (-not (Test-PortListening -Port 8010)) {
        Start-ManagedProcess `
            -Name 'agent-8010' `
            -WorkDir $dir `
            -FilePath 'python' `
            -Arguments @('-m', 'uvicorn', 'api:app', '--host', '0.0.0.0', '--port', '8010') `
            -Port 8010 `
            -Retries 90
    }
}

# ==================================================== 3) resume_cut (8089) ====

if (-not $SkipResume8089) {
    $dir = Join-Path $RepoRoot 'resume_cut_docker'
    $jar = Join-Path $dir 'target\resume_cut-0.0.1-SNAPSHOT.jar'

    if (Test-PortListening -Port 8089) {
        Write-Step 'resume_cut already listening on 8089'
    }
    elseif (Test-Path $jar) {
        # .env supplies LLM_API_URL / LLM_API_KEY / LLM_MODEL for the parser.
        Start-ManagedProcess `
            -Name 'resume-8089' `
            -WorkDir $dir `
            -FilePath 'cmd.exe' `
            -Arguments @('/c', 'set JAVA_OPTS=-Dfile.encoding=UTF-8 && java -jar target\resume_cut-0.0.1-SNAPSHOT.jar --server.port=8089') `
            -Port 8089 `
            -Retries 90
    }
    else {
        Write-Warn 'resume_cut jar missing - run .\bootstrap-deps.ps1 first.'
    }
}

# ================================================= 4) springboot-front (8080) =

if (-not $SkipBackend8080) {
    $dir = Join-Path $RepoRoot 'springboot-front(4)'
    $jar = Resolve-Jar -Dir (Join-Path $dir 'build\libs') -ExcludePattern '*-plain.jar'
    $funasrArgs = '--server.port=8080 --funasr.base-url=http://127.0.0.1:8082 --funasr.transcribe-path=/api/voice/recognize --funasr.file-field=audio --funasr.timeout-ms=90000'

    if (Test-PortListening -Port 8080) {
        Write-Step 'springboot-front already listening on 8080'
    }
    elseif ($jar) {
        Start-ManagedProcess `
            -Name 'backend-8080' `
            -WorkDir $dir `
            -FilePath 'cmd.exe' `
            -Arguments @('/c', "java -jar `"$jar`" $funasrArgs") `
            -Port 8080 `
            -Retries 150
    }
    else {
        Write-Step 'no jar found - starting springboot-front via gradle bootRun'
        Start-ManagedProcess `
            -Name 'backend-8080' `
            -WorkDir $dir `
            -FilePath 'cmd.exe' `
            -Arguments @('/c', '.\gradlew.bat --no-daemon bootRun --console=plain --args="--server.port=8080 --funasr.base-url=http://127.0.0.1:8082 --funasr.transcribe-path=/api/voice/recognize --funasr.file-field=audio --funasr.timeout-ms=90000"') `
            -Port 8080 `
            -Retries 150
    }
}

# ============================================ 5) sv-service gateway (8082) ====

if (-not $SkipSv8082) {
    $dir = Join-Path $RepoRoot 'FunASR-main\sv-service'
    $jar = Join-Path $dir 'target\sv-service-0.1.0.jar'

    if (Test-PortListening -Port 8082) {
        Write-Step 'sv-service already listening on 8082'
    }
    elseif (Test-Path $jar) {
        Start-ManagedProcess `
            -Name 'sv-8082' `
            -WorkDir $dir `
            -FilePath 'cmd.exe' `
            -Arguments @('/c', 'java -jar target\sv-service-0.1.0.jar --asr.ws-uri=ws://127.0.0.1:10095 --asr.mode=2pass --asr.tech-normalization.enabled=false --asr.result-timeout-ms=180000 --asr.allow-partial-on-timeout=false --spring.servlet.multipart.max-file-size=64MB --spring.servlet.multipart.max-request-size=64MB --server.port=8082') `
            -Port 8082 `
            -Retries 60
    }
    else {
        Write-Warn 'sv-service jar missing - run .\bootstrap-deps.ps1 first.'
    }
}

# =============================================== 6) Interview_elf_agent (8000) =

if (-not $SkipElf8000) {
    $dir = Join-Path $RepoRoot 'Interview_elf_agent'

    if (Test-PortListening -Port 8000) {
        Write-Step 'Interview_elf_agent already listening on 8000'
    }
    else {
        # .env points QDRANT_HOST at the docker service name; override for a
        # locally-run process so it reaches the published port instead.
        Start-ManagedProcess `
            -Name 'elf-8000' `
            -WorkDir $dir `
            -FilePath 'cmd.exe' `
            -Arguments @('/c', 'set QDRANT_HOST=localhost && set QDRANT_WAIT_ON_STARTUP=true && python -m uvicorn app.api.server:app --host 0.0.0.0 --port 8000') `
            -Port 8000 `
            -Retries 90
    }
}

# ======================================================= 7) admin-server (18081)

if (-not $SkipAdmin18081) {
    $dir = Join-Path $RepoRoot 'backend\admin-server'
    $jar = Resolve-Jar -Dir (Join-Path $dir 'target') -ExcludePattern '*.original'

    # application.properties expects Redis on 6379; the dev container publishes
    # 16379, so remap it explicitly rather than editing the file.
    $redisArgs = '--spring.data.redis.host=localhost --spring.data.redis.port=16379 --spring.data.redis.password=123456'

    if (Test-PortListening -Port 18081) {
        Write-Step 'admin-server already listening on 18081'
    }
    elseif ($jar) {
        Start-ManagedProcess `
            -Name 'admin-18081' `
            -WorkDir $dir `
            -FilePath 'cmd.exe' `
            -Arguments @('/c', "java -jar `"$jar`" $redisArgs") `
            -Port 18081 `
            -Retries 120
    }
    else {
        Write-Warn 'admin-server jar missing - run .\bootstrap-deps.ps1 first.'
    }
}

# ========================================================= 8) admin-web (9527) =

if (-not $SkipAdminWeb9527) {
    $dir = Join-Path $RepoRoot 'backend\admin-web'

    if (Test-PortListening -Port 9527) {
        Write-Step 'admin-web already listening on 9527'
    }
    elseif (Test-Path (Join-Path $dir 'node_modules\.bin\vue-cli-service.cmd')) {
        if (-not $script:NpmPath) {
            Write-Warn 'npm not found - cannot start admin-web.'
        }
        else {
            $line = "set PATH=$($script:NodeDir);%PATH% && set NODE_OPTIONS=--openssl-legacy-provider && set VUE_APP_PROXY_TARGET=http://localhost:18081 && set port=9527 && `"$($script:NpmPath)`" run dev"
            Start-ManagedProcess `
                -Name 'admin-web-9527' `
                -WorkDir $dir `
                -FilePath 'cmd.exe' `
                -Arguments @('/c', $line) `
                -Port 9527 `
                -Retries 180 `
                -DelaySeconds 2
        }
    }
    else {
        Write-Warn 'admin-web dependencies are incomplete - run .\bootstrap-deps.ps1 first (node_modules\.bin\vue-cli-service.cmd is missing).'
    }
}

# ======================================================= 9) vue-front (5173) ===

if (-not $SkipFrontend5173) {
    $dir = Join-Path $RepoRoot 'vue-front (4)'

    if (Test-PortListening -Port 5173) {
        Write-Step 'vue-front already listening on 5173'
    }
    elseif (Test-Path (Join-Path $dir 'node_modules\.bin\vite.cmd')) {
        if (-not $script:NpmPath) {
            Write-Warn 'npm not found - cannot start vue-front.'
        }
        else {
            $line = "set PATH=$($script:NodeDir);%PATH% && `"$($script:NpmPath)`" run dev -- --host 0.0.0.0 --port 5173"
            Start-ManagedProcess `
                -Name 'frontend-5173' `
                -WorkDir $dir `
                -FilePath 'cmd.exe' `
                -Arguments @('/c', $line) `
                -Port 5173 `
                -Retries 120 `
                -DelaySeconds 1
        }
    }
    else {
        Write-Warn 'vue-front dependencies are incomplete - run .\bootstrap-deps.ps1 first (node_modules\.bin\vite.cmd is missing).'
    }
}

# ==================================================================== report ==

Write-Host ''
Write-Host 'Service status:' -ForegroundColor Green
@(
    [pscustomobject]@{ Port = 10095; Service = 'FunASR websocket' }
    [pscustomobject]@{ Port = 8010;  Service = 'WB interview agent' }
    [pscustomobject]@{ Port = 8089;  Service = 'Resume parser' }
    [pscustomobject]@{ Port = 8080;  Service = 'Spring backend (candidate)' }
    [pscustomobject]@{ Port = 8082;  Service = 'SV gateway (FunASR)' }
    [pscustomobject]@{ Port = 8000;  Service = 'Interview_elf_agent' }
    [pscustomobject]@{ Port = 18081; Service = 'Admin server' }
    [pscustomobject]@{ Port = 9527;  Service = 'Admin web' }
    [pscustomobject]@{ Port = 5173;  Service = 'Vue frontend (mobile UI)' }
) | ForEach-Object {
    $up = Test-PortListening -Port $_.Port
    [pscustomobject]@{
        Port    = $_.Port
        Service = $_.Service
        Status  = if ($up) { 'UP' } else { '-' }
    }
} | Format-Table -AutoSize

Write-Host 'URLs:' -ForegroundColor Green
Write-Host '  candidate app : http://localhost:5173'  -ForegroundColor Gray
Write-Host '  admin web     : http://localhost:9527'  -ForegroundColor Gray
Write-Host '  admin api     : http://localhost:18081/admin' -ForegroundColor Gray
Write-Host ''
Write-Host 'Logs : .run-logs' -ForegroundColor Gray
Write-Host 'Stop : .\run-full-stack.ps1 -Stop' -ForegroundColor Gray
Write-Host ''


