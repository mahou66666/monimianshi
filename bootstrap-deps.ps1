<#
.SYNOPSIS
    Downloads and builds every dependency required by the AI Mock Interview Platform.

.DESCRIPTION
    Stages:
      1. Container images + infrastructure containers (Postgres / Redis / Qdrant / FunASR)
      2. Java artifacts        (Maven + Gradle builds)
      3. Node dependencies     (vue-front, admin-web)
      4. Python dependencies   (WB_interview_agent, Interview_elf_agent)
      5. ML models             (FunASR chain + BAAI/bge-large-zh-v1.5)

    This script only PREPARES the machine. Application services are started
    separately by run-full-stack.ps1.

    Every step is idempotent, so the script is safe to re-run at any time.
    Failures are collected and reported at the end instead of aborting the run.

.PARAMETER SkipDocker
    Skip container images and infrastructure containers.

.PARAMETER SkipJava
    Skip Maven / Gradle builds.

.PARAMETER SkipNode
    Skip npm install.

.PARAMETER SkipPython
    Skip pip install.

.PARAMETER SkipModels
    Skip multi-GB model prefetch (FunASR chain, bge-large-zh-v1.5).

.PARAMETER IncludeLocalMysql
    Also start the local MySQL 5.7 container on host port 13306.
    By default the platform talks to the remote MySQL at 106.54.162.6:3306,
    so the local container is not needed.

.PARAMETER Force
    Rebuild / reinstall even when the expected output already exists.

.PARAMETER NpmRegistry
    npm registry to install from. Defaults to the npmmirror China mirror, because
    registry.npmjs.org is frequently unreachable from this network.

.EXAMPLE
    .\bootstrap-deps.ps1

.EXAMPLE
    .\bootstrap-deps.ps1 -SkipModels

.EXAMPLE
    .\bootstrap-deps.ps1 -Force -IncludeLocalMysql
#>
[CmdletBinding()]
param(
    [switch]$SkipDocker,
    [switch]$SkipJava,
    [switch]$SkipNode,
    [switch]$SkipPython,
    [switch]$SkipModels,
    [switch]$IncludeLocalMysql,
    [switch]$Force,
    [string]$NpmRegistry = 'https://registry.npmmirror.com'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogDir   = Join-Path $RepoRoot '.bootstrap-logs'
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$script:StepNo = 0
$script:Issues = New-Object System.Collections.Generic.List[string]
$script:Notes  = New-Object System.Collections.Generic.List[string]

# ---------------------------------------------------------------- helpers ---

function Write-Step {
    param([string]$Message)
    $script:StepNo++
    Write-Host ''
    Write-Host ('[{0:00}] {1}' -f $script:StepNo, $Message) -ForegroundColor Cyan
}
function Write-Ok   { param([string]$m) Write-Host "      ok   $m" -ForegroundColor Green }
function Write-Skip { param([string]$m) Write-Host "      skip $m" -ForegroundColor DarkGray }
function Write-Note { param([string]$m) Write-Host "      ..   $m" -ForegroundColor Yellow }
function Write-Bad  { param([string]$m) Write-Host "      FAIL $m" -ForegroundColor Red }

function Test-Cmd {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Add-Issue {
    param([string]$Stage, [string]$Message)
    $script:Issues.Add("$Stage :: $Message")
}

# Native tools (docker, mvn, npm, gradlew) write progress and errors to stderr.
# Under $ErrorActionPreference='Stop' PowerShell promotes even an *expected*
# stderr line ('No such image', 'No such container') into a terminating error,
# which is what previously aborted the FunASR pull before it ever ran.
# Always invoke natives with 'Continue' and branch on $LASTEXITCODE.
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

# Runs an external program inside a directory and throws on non-zero exit.
function Invoke-In {
    param(
        [string]$Path,
        [string]$Exe,
        [string[]]$Arguments
    )
    Push-Location -LiteralPath $Path
    try {
        $r = Invoke-Native -Exe $Exe -Arguments $Arguments
        if ($r.ExitCode -ne 0) {
            throw "'$Exe $($Arguments -join ' ')' failed in '$Path' (exit code $($r.ExitCode))"
        }
    }
    finally {
        Pop-Location
    }
}

# Runs a native command inside a directory, teeing every stream to a log file so
# a failure can be diagnosed afterwards (npm in particular prints its real error
# to stderr and exits 1 without it reaching the console reliably).
function Invoke-InLogged {
    param(
        [string]$Path,
        [string]$Exe,
        [string[]]$Arguments,
        [string]$LogFile
    )
    Push-Location -LiteralPath $Path
    try {
        $prev = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            & $Exe @Arguments *>&1 | Tee-Object -FilePath $LogFile
            $code = $LASTEXITCODE
        }
        finally {
            $ErrorActionPreference = $prev
        }
        if ($code -ne 0) {
            throw "'$Exe $($Arguments -join ' ')' failed in '$Path' (exit code $code). Full log: $LogFile"
        }
    }
    finally {
        Pop-Location
    }
}

# A node_modules directory can exist yet be useless: npm may abort mid-install
# and leave an empty shell behind. Probe for the binary we actually need.
function Test-NodeModulesReady {
    param([string]$Dir, [string]$Probe)
    if (-not (Test-Path -LiteralPath $Dir)) { return $false }
    if (-not $Probe) { return $true }
    return (Test-Path -LiteralPath (Join-Path $Dir $Probe))
}

# tui-editor and raphael pull a couple of transitive dependencies straight from
# GitHub over SSH (ssh://git@github.com/adobe-webplatform/eve.git,
# ssh://git@github.com/sohee-lee7/Squire.git). Without a registered SSH key npm
# aborts with exit code 128 and 'Permission denied (publickey)'.
# Rewriting those URLs to anonymous HTTPS is the standard fix and is reversible:
#   git config --global --unset-all url.https://github.com/.insteadOf
function Enable-GitHubHttpsRewrite {
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        Write-Bad 'git not found on PATH - the git-based dependencies cannot be resolved.'
        return $false
    }

    $probe = Invoke-Native -Exe 'git' -Arguments @('ls-remote', '--heads', 'https://github.com/adobe-webplatform/eve.git') -Capture
    if ($probe.ExitCode -ne 0) {
        Write-Bad 'github.com is not reachable over HTTPS - git-based dependencies cannot be fetched.'
        return $false
    }
    Write-Ok 'github.com reachable over HTTPS'

    $key = 'url.https://github.com/.insteadOf'
    $existing = (Invoke-Native -Exe 'git' -Arguments @('config', '--global', '--get-all', $key) -Capture).Output

    if ($existing -notmatch [regex]::Escape('ssh://git@github.com/')) {
        Invoke-Native -Exe 'git' -Arguments @('config', '--global', '--add', $key, 'ssh://git@github.com/') -Capture | Out-Null
        Write-Ok 'git: ssh://git@github.com/ rewrites to https://github.com/'
    }
    else {
        Write-Skip 'git: ssh://git@github.com/ rewrite already configured'
    }

    if ($existing -notmatch [regex]::Escape('git@github.com:')) {
        Invoke-Native -Exe 'git' -Arguments @('config', '--global', '--add', $key, 'git@github.com:') -Capture | Out-Null
        Write-Ok 'git: git@github.com: rewrites to https://github.com/'
    }

    return $true
}

# npm leaves a half-written node_modules behind after a failed install, and its
# own cleanup can hit EPERM when a file handle is still open.
function Remove-NodeModules {
    param([string]$Dir)
    if (-not (Test-Path -LiteralPath $Dir)) { return $true }
    Write-Note "removing incomplete node_modules: $Dir"
    Invoke-Native -Exe 'cmd.exe' -Arguments @('/c', 'rmdir', '/s', '/q', $Dir) -Capture | Out-Null
    if (Test-Path -LiteralPath $Dir) {
        Write-Warn "could not fully remove $Dir - close any editor or terminal holding files there, then retry"
        return $false
    }
    return $true
}

function Test-DockerReady {
    if (-not (Test-Cmd 'docker')) { return $false }
    return ((Invoke-Native -Exe 'docker' -Arguments @('version') -Capture).ExitCode -eq 0)
}

function Test-ImagePresent {
    param([string]$Image)
    return ((Invoke-Native -Exe 'docker' -Arguments @('image', 'inspect', $Image) -Capture).ExitCode -eq 0)
}

function Ensure-Image {
    param([string]$Image)
    if ((-not $Force) -and (Test-ImagePresent -Image $Image)) {
        Write-Skip "image already present: $Image"
        return
    }
    Write-Note "pulling image: $Image (large layers may take several minutes)"
    $r = Invoke-Native -Exe 'docker' -Arguments @('pull', $Image) -Capture
    if ($r.ExitCode -ne 0) {
        $tail = ($r.Output.Trim() -split "`n" | Select-Object -Last 3) -join ' | '
        throw "docker pull failed for ${Image}: $tail"
    }
    Write-Ok "image ready: $Image"
}

# Node is unpacked directly into D:\ on this machine, so npm.cmd is not on PATH.
# Resolve it explicitly instead of assuming a standard install location.
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

    $w = Invoke-Native -Exe 'where.exe' -Arguments @('npm.cmd') -Capture
    if ($w.ExitCode -eq 0 -and $w.Output.Trim()) {
        return (($w.Output.Trim() -split "`n")[0]).Trim()
    }
    return $null
}

# npm.cmd shells out to node.exe; make sure the sibling directory is reachable.
function Add-NpmDirToPath {
    param([string]$NpmPath)
    $dir = Split-Path -Parent $NpmPath
    if ($dir -and ($env:PATH -notlike "*$dir*")) {
        $env:PATH = "$dir;$env:PATH"
    }
}

# ------------------------------------------------------------------ header ---

Write-Host ''
Write-Host '=========================================================' -ForegroundColor White
Write-Host ' AI Mock Interview Platform - dependency bootstrap'      -ForegroundColor White
Write-Host '=========================================================' -ForegroundColor White
Write-Host " repo root : $RepoRoot"
Write-Host " logs      : $LogDir"
Write-Host " force     : $Force"

# ================================================================ STAGE 1 ====
# Container images + long-lived infrastructure containers.
# ============================================================================
if ($SkipDocker) {
    Write-Step 'Stage 1/5 - containers (skipped by flag)'
}
else {
    Write-Step 'Stage 1/5 - containers'

    if (-not (Test-DockerReady)) {
        Write-Bad 'Docker daemon is not reachable.'
        Write-Note 'Start Docker Desktop, wait until it reports "Engine running", then re-run this script.'
        Add-Issue 'containers' 'Docker daemon not reachable'
    }
    else {
        Write-Ok 'Docker daemon reachable'

        # --- 1a. PostgreSQL for WB_interview_agent -------------------------
        try {
            Ensure-Image -Image 'postgres:16-alpine'
            Write-Note 'starting Postgres (WB_interview_agent) on docker network'
            Invoke-In -Path (Join-Path $RepoRoot 'WB_interview_agent') -Exe 'docker' -Arguments @('compose', 'up', '-d', 'postgres')
            Write-Ok 'Postgres container up'
        }
        catch {
            Write-Bad "Postgres: $($_.Exception.Message)"
            Add-Issue 'containers' "Postgres: $($_.Exception.Message)"
        }

        # --- 1b. WB_interview_agent application image ----------------------
        try {
            Write-Note 'building WB_interview_agent image (runs pip install inside build)'
            Invoke-In -Path (Join-Path $RepoRoot 'WB_interview_agent') -Exe 'docker' -Arguments @('compose', 'build', 'app')
            Write-Ok 'WB_interview_agent image built'
        }
        catch {
            Write-Bad "WB_interview_agent image: $($_.Exception.Message)"
            Add-Issue 'containers' "WB_interview_agent image build: $($_.Exception.Message)"
        }

        # --- 1c. Qdrant vector database ------------------------------------
        try {
            Ensure-Image -Image 'qdrant/qdrant:v1.16.2'
            Write-Note 'starting Qdrant on 6333/6334'
            Invoke-In -Path (Join-Path $RepoRoot 'Interview_elf_agent') -Exe 'docker' -Arguments @('compose', 'up', '-d', 'qdrant')
            Write-Ok 'Qdrant container up'
        }
        catch {
            Write-Bad "Qdrant: $($_.Exception.Message)"
            Add-Issue 'containers' "Qdrant: $($_.Exception.Message)"
        }

        # --- 1d. Redis for admin-server (host port 16379) ------------------
        try {
            Ensure-Image -Image 'redis:7.2-alpine'
            Write-Note 'starting Redis on 16379 (password 123456)'
            Invoke-In -Path $RepoRoot -Exe 'docker' -Arguments @('compose', '-f', 'docker-compose.dev.yml', 'up', '-d', 'redis')
            Write-Ok 'Redis container up'
        }
        catch {
            Write-Bad "Redis: $($_.Exception.Message)"
            Add-Issue 'containers' "Redis: $($_.Exception.Message)"
        }

        # --- 1e. FunASR runtime image (large) ------------------------------
        try {
            Ensure-Image -Image 'registry.cn-hangzhou.aliyuncs.com/funasr_repo/funasr:funasr-runtime-sdk-online-cpu-0.1.12'
        }
        catch {
            Write-Bad "FunASR image: $($_.Exception.Message)"
            Add-Issue 'containers' "FunASR image pull: $($_.Exception.Message)"
        }

        # --- 1f. Optional local MySQL --------------------------------------
        if ($IncludeLocalMysql) {
            try {
                Ensure-Image -Image 'mysql:5.7'
                Write-Note 'starting local MySQL on 13306 (db=interview_agent, user=zzh/zzh)'
                Invoke-In -Path $RepoRoot -Exe 'docker' -Arguments @('compose', '-f', 'docker-compose.local-db.yml', 'up', '-d', 'mysql')
                Write-Ok 'local MySQL container up'
                $script:Notes.Add('Local MySQL is up on 13306. springboot-front(4) hardcodes the remote URL, so pass --spring.datasource.url=... to use it.')
            }
            catch {
                Write-Bad "local MySQL: $($_.Exception.Message)"
                Add-Issue 'containers' "local MySQL: $($_.Exception.Message)"
            }
        }
        else {
            Write-Skip 'local MySQL (default: remote 106.54.162.6:3306 is used)'
        }
    }
}

# ================================================================ STAGE 2 ====
# Java artifacts. JDK 21 is sufficient for every module (all target Java 17
# bytecode or lower, except springboot-front which targets 21).
# ============================================================================
if ($SkipJava) {
    Write-Step 'Stage 2/5 - Java artifacts (skipped by flag)'
}
else {
    Write-Step 'Stage 2/5 - Java artifacts'

    $hasMaven  = Test-Cmd 'mvn'
    $hasGradle = Test-Path (Join-Path $RepoRoot 'springboot-front(4)\gradlew.bat')

    if (-not $hasMaven) {
        Write-Bad 'mvn not found on PATH.'
        Write-Note 'admin-server, sv-service and resume-revision need Maven. Install Maven, or open the module once in IntelliJ IDEA (bundled Maven) and re-run.'
        Add-Issue 'java' 'mvn not found on PATH'
    }

    # --- 2a. sv-service (funasr speaker-verification gateway) -----------
    $svJar = Join-Path $RepoRoot 'FunASR-main\sv-service\target\sv-service-0.1.0.jar'
    if ((-not $Force) -and (Test-Path $svJar)) {
        Write-Skip 'sv-service jar already built'
    }
    elseif ($hasMaven) {
        try {
            Write-Note 'building sv-service (mvn package)'
            Invoke-In -Path (Join-Path $RepoRoot 'FunASR-main\sv-service') -Exe 'mvn' -Arguments @('-B', '-DskipTests', 'package')
            Write-Ok 'sv-service jar built'
        }
        catch {
            Write-Bad "sv-service: $($_.Exception.Message)"
            Add-Issue 'java' "sv-service build: $($_.Exception.Message)"
        }
    }

    # --- 2b. admin-server ------------------------------------------------
    $adminJar = Join-Path $RepoRoot 'backend\admin-server\target\ai-mock-interview-admin-1.0.0-SNAPSHOT.jar'
    if ((-not $Force) -and (Test-Path $adminJar)) {
        Write-Skip 'admin-server jar already built'
    }
    elseif ($hasMaven) {
        try {
            Write-Note 'building admin-server (mvn package)'
            Invoke-In -Path (Join-Path $RepoRoot 'backend\admin-server') -Exe 'mvn' -Arguments @('-B', '-DskipTests', 'package')
            Write-Ok 'admin-server jar built'
        }
        catch {
            Write-Bad "admin-server: $($_.Exception.Message)"
            Add-Issue 'java' "admin-server build: $($_.Exception.Message)"
        }
    }

    # --- 2c. resume-revision (library / CLI, no port) --------------------
    Write-Note 'resolving resume-revision dependencies (mvn package)'
    if ($hasMaven) {
        try {
            Invoke-In -Path (Join-Path $RepoRoot 'resume-revision') -Exe 'mvn' -Arguments @('-B', '-DskipTests', 'package')
            Write-Ok 'resume-revision built'
        }
        catch {
            Write-Bad "resume-revision: $($_.Exception.Message)"
            Add-Issue 'java' "resume-revision build: $($_.Exception.Message)"
        }
    }

    # --- 2d. springboot-front (Gradle) -----------------------------------
    if (-not $hasGradle) {
        Write-Bad 'gradlew.bat not found for springboot-front(4)'
        Add-Issue 'java' 'springboot-front gradlew.bat missing'
    }
    else {
        $frontJar = Join-Path $RepoRoot 'springboot-front(4)\build\libs'
        $frontJars = @(
            Get-ChildItem -LiteralPath $frontJar -Filter '*.jar' -File -ErrorAction SilentlyContinue
        )
        $frontBuilt = ($frontJars.Count -gt 0)
        if ((-not $Force) -and $frontBuilt) {
            Write-Skip 'springboot-front jars already built'
        }
        else {
            try {
                Write-Note 'building springboot-front(4) (gradlew build -x test)'
                Invoke-In -Path (Join-Path $RepoRoot 'springboot-front(4)') -Exe (Join-Path $RepoRoot 'springboot-front(4)\gradlew.bat') -Arguments @('--no-daemon', 'build', '-x', 'test', '--console=plain')
                Write-Ok 'springboot-front built'
            }
            catch {
                Write-Bad "springboot-front: $($_.Exception.Message)"
                Add-Issue 'java' "springboot-front build: $($_.Exception.Message)"
            }
        }
    }

    # --- 2e. resume_cut_docker jar (usually prebuilt) --------------------
    $cutJar = Join-Path $RepoRoot 'resume_cut_docker\target\resume_cut-0.0.1-SNAPSHOT.jar'
    if ((-not $Force) -and (Test-Path $cutJar)) {
        Write-Skip 'resume_cut jar already built'
    }
    else {
        try {
            Write-Note 'building resume_cut_docker (mvnw package)'
            Invoke-In -Path (Join-Path $RepoRoot 'resume_cut_docker') -Exe (Join-Path $RepoRoot 'resume_cut_docker\mvnw.cmd') -Arguments @('-B', '-Dmaven.test.skip=true', 'package')
            Write-Ok 'resume_cut jar built'
        }
        catch {
            Write-Bad "resume_cut: $($_.Exception.Message)"
            Add-Issue 'java' "resume_cut build: $($_.Exception.Message)"
        }
    }
}

# ================================================================ STAGE 3 ====
# Node dependencies.
# ============================================================================
if ($SkipNode) {
    Write-Step 'Stage 3/5 - Node dependencies (skipped by flag)'
}
else {
    Write-Step 'Stage 3/5 - Node dependencies'

    $npm = Resolve-Npm
    if (-not $npm) {
        Write-Bad 'npm not found. Expected D:\npm.cmd on this machine.'
        Write-Note 'Check with: where.exe npm'
        Add-Issue 'node' 'npm not found'
    }
    else {
        Add-NpmDirToPath -NpmPath $npm
        Write-Ok "npm: $npm"
        Write-Note "registry: $NpmRegistry"

        $nodeExe = Join-Path (Split-Path -Parent $npm) 'node.exe'
        $nodeVer = 'unknown'
        if (Test-Path -LiteralPath $nodeExe) {
            $nodeVer = (Invoke-Native -Exe $nodeExe -Arguments @('-v') -Capture).Output.Trim().TrimStart('v')
            Write-Ok "node: $nodeVer"
        }

        # --- 3a. vue-front (candidate front-end) --------------------------
        $vueDir = Join-Path $RepoRoot 'vue-front (4)'
        $vueModules = Join-Path $vueDir 'node_modules'
        # An empty node_modules directory is not proof of a successful install,
        # so probe for the launcher we actually need.
        if ((-not $Force) -and (Test-NodeModulesReady -Dir $vueModules -Probe '.bin\vite.cmd')) {
            Write-Skip 'vue-front (4) dependencies already installed'
        }
        else {
            try {
                Write-Note 'npm install for vue-front (4)'
                Invoke-InLogged -Path $vueDir -Exe $npm `
                    -Arguments @('install', '--registry', $NpmRegistry) `
                    -LogFile (Join-Path $LogDir 'npm-vue-front.log')
                Write-Ok 'vue-front (4) dependencies installed'
            }
            catch {
                Write-Bad "vue-front (4): $($_.Exception.Message)"
                Add-Issue 'node' "vue-front (4) npm install: $($_.Exception.Message)"
            }
        }

        # --- 3b. admin-web (vue-element-admin, vue-cli 4 / webpack 4) -----
        $adminDir = Join-Path $RepoRoot 'backend\admin-web'
        $adminModules = Join-Path $adminDir 'node_modules'
        if ((-not $Force) -and (Test-NodeModulesReady -Dir $adminModules -Probe '.bin\vue-cli-service.cmd')) {
            Write-Skip 'admin-web dependencies already installed'
        }
        else {
            try {
                # The dependency tree declares git SSH URLs; make them reachable first.
                if (-not (Enable-GitHubHttpsRewrite)) {
                    throw 'git-based dependencies need github.com over HTTPS, or a GitHub SSH key.'
                }

                # A previous failed run can leave a half-written tree behind.
                if ((Test-Path -LiteralPath $adminModules) -and
                    (-not (Test-NodeModulesReady -Dir $adminModules -Probe '.bin\vue-cli-service.cmd'))) {
                    Remove-NodeModules -Dir $adminModules | Out-Null
                }

                Write-Note 'npm install for admin-web (this takes several minutes)'
                # vue-cli 4 uses webpack 4; Node 17+ needs the legacy OpenSSL provider.
                $prevOpts = $env:NODE_OPTIONS
                if ($nodeVer -ne 'unknown') {
                    $nodeMajor = [int]($nodeVer.Split('.')[0])
                    if ($nodeMajor -ge 17) {
                        $env:NODE_OPTIONS = '--openssl-legacy-provider'
                        Write-Note "Node $nodeVer detected - NODE_OPTIONS=--openssl-legacy-provider enabled"
                    }
                }
                try {
                    Invoke-InLogged -Path $adminDir -Exe $npm `
                        -Arguments @('install', '--legacy-peer-deps', '--registry', $NpmRegistry) `
                        -LogFile (Join-Path $LogDir 'npm-admin-web.log')
                }
                finally {
                    $env:NODE_OPTIONS = $prevOpts
                }
                Write-Ok 'admin-web dependencies installed'
            }
            catch {
                Write-Bad "admin-web: $($_.Exception.Message)"
                Add-Issue 'node' "admin-web npm install: $($_.Exception.Message)"
            }
        }
    }
}

# ================================================================ STAGE 4 ====
# Python dependencies (used when services run locally instead of in Docker).
# ============================================================================
if ($SkipPython) {
    Write-Step 'Stage 4/5 - Python dependencies (skipped by flag)'
}
else {
    Write-Step 'Stage 4/5 - Python dependencies'

    if (-not (Test-Cmd 'python')) {
        Write-Bad 'python not found on PATH.'
        Add-Issue 'python' 'python not found on PATH'
    }
    else {
        $pyVer = (& python --version) 2>&1
        Write-Note "interpreter: $pyVer"

        # --- 4a. WB_interview_agent ---------------------------------------
        try {
            Write-Note 'pip install -r WB_interview_agent/requirements.txt'
            Invoke-In -Path (Join-Path $RepoRoot 'WB_interview_agent') -Exe 'python' -Arguments @('-m', 'pip', 'install', '-r', 'requirements.txt')
            Write-Ok 'WB_interview_agent python deps installed'
        }
        catch {
            Write-Bad "WB_interview_agent: $($_.Exception.Message)"
            Add-Issue 'python' "WB_interview_agent pip install: $($_.Exception.Message)"
        }

        # --- 4b. Interview_elf_agent --------------------------------------
        try {
            Write-Note 'pip install -r Interview_elf_agent/requirements.txt'
            Invoke-In -Path (Join-Path $RepoRoot 'Interview_elf_agent') -Exe 'python' -Arguments @('-m', 'pip', 'install', '-r', 'requirements.txt')
            Write-Ok 'Interview_elf_agent python deps installed'
        }
        catch {
            Write-Bad "Interview_elf_agent: $($_.Exception.Message)"
            Add-Issue 'python' "Interview_elf_agent pip install: $($_.Exception.Message)"
        }

        if ($pyVer -match '3\.1[34]') {
            $script:Notes.Add('Python 3.13/3.14 tends to lack prebuilt wheels for faiss-cpu and torch. If pip fails above, use Python 3.11 (already installed at %LOCALAPPDATA%\Programs\Python\Python311) or run these services in Docker.')
        }
    }
}

# ================================================================ STAGE 5 ====
# ML models. Both downloads are multi-GB.
# ============================================================================
if ($SkipModels) {
    Write-Step 'Stage 5/5 - ML models (skipped by flag)'
}
else {
    Write-Step 'Stage 5/5 - ML models'

    # --- 5a. FunASR 2pass chain (downloaded by the container itself) -----
    if (Test-DockerReady) {
        try {
            $modelDir = Join-Path $RepoRoot 'funasr-runtime-resources\models'
            New-Item -ItemType Directory -Force -Path $modelDir | Out-Null
            $volume = "${modelDir}:/workspace/models"

            # Single-quoted literals: PowerShell must NOT interpolate $() here,
            # it has to reach the container's bash verbatim.
            $cmd = '/workspace/FunASR/runtime/websocket/build/bin/funasr-wss-server-2pass ' +
                   '--download-model-dir /workspace/models ' +
                   '--model-dir damo/speech_paraformer-large-vad-punc_asr_nat-zh-cn-16k-common-vocab8404-onnx ' +
                   '--online-model-dir damo/speech_paraformer-large_asr_nat-zh-cn-16k-common-vocab8404-online-onnx ' +
                   '--vad-dir damo/speech_fsmn_vad_zh-cn-16k-common-onnx ' +
                   '--punc-dir damo/punc_ct-transformer_zh-cn-common-vad_realtime-vocab272727-onnx ' +
                   '--lm-dir damo/speech_ngram_lm_zh-cn-ai-wesp-fst ' +
                   '--itn-dir thuduj12/fst_itn_zh ' +
                   '--decoder-thread-num $(grep -c processor /proc/cpuinfo || echo 32) ' +
                   '--model-thread-num 1 --io-thread-num 2 --port 10095 --certfile 0 --keyfile 0'

            Write-Note 'starting FunASR container; it downloads the ONNX model chain (several GB) on first run'
            # Removing a container that does not exist is expected on a first run.
            Invoke-Native -Exe 'docker' -Arguments @('rm', '-f', 'funasr-offline') -Capture | Out-Null
            $run = Invoke-Native -Exe 'docker' -Arguments @(
                'run', '-d', '--name', 'funasr-offline', '--restart', 'unless-stopped',
                '-p', '10095:10095', '--privileged=true', '-v', $volume,
                'registry.cn-hangzhou.aliyuncs.com/funasr_repo/funasr:funasr-runtime-sdk-online-cpu-0.1.12',
                'bash', '-lc', $cmd
            ) -Capture
            if ($run.ExitCode -ne 0) {
                $tail = ($run.Output.Trim() -split "`n" | Select-Object -Last 3) -join ' | '
                throw "docker run failed for funasr-offline: $tail"
            }
            Write-Ok 'FunASR container started (port 10095)'
            Write-Note 'model download progress: docker logs -f funasr-offline'
        }
        catch {
            Write-Bad "FunASR models: $($_.Exception.Message)"
            Add-Issue 'models' "FunASR model prefetch: $($_.Exception.Message)"
        }
    }
    else {
        Write-Skip 'FunASR models (Docker not available)'
    }

    # --- 5b. BAAI/bge-large-zh-v1.5 embedding model for Interview_elf_agent ---
    try {
        if (-not (Test-Cmd 'python')) { throw 'python not found on PATH' }
        Write-Note 'downloading BAAI/bge-large-zh-v1.5 via the HF mirror (~1.3 GB)'
        $py = @'
import os
os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")
from sentence_transformers import SentenceTransformer
SentenceTransformer("BAAI/bge-large-zh-v1.5")
print("embedding model ready")
'@
        $tmp = Join-Path $LogDir 'fetch_embedding_model.py'
        # ASCII avoids a UTF-8 BOM under Windows PowerShell 5.1.
        Set-Content -LiteralPath $tmp -Value $py -Encoding ascii
        Push-Location (Join-Path $RepoRoot 'Interview_elf_agent')
        try {
            & python $tmp
            if ($LASTEXITCODE -ne 0) { throw 'sentence-transformers download failed' }
        }
        finally {
            Pop-Location
        }
        Write-Ok 'embedding model cached'
    }
    catch {
        Write-Bad "embedding model: $($_.Exception.Message)"
        Add-Issue 'models' "bge-large-zh-v1.5 download: $($_.Exception.Message)"
    }
}

# ================================================================= summary ====

Write-Host ''
Write-Host '=========================================================' -ForegroundColor White
Write-Host ' bootstrap summary'                                        -ForegroundColor White
Write-Host '=========================================================' -ForegroundColor White

if ($script:Issues.Count -eq 0) {
    Write-Host ' All stages completed without errors.' -ForegroundColor Green
}
else {
    Write-Host (" {0} issue(s) need attention:" -f $script:Issues.Count) -ForegroundColor Red
    foreach ($i in $script:Issues) { Write-Host "   - $i" -ForegroundColor Red }
}

if ($script:Notes.Count -gt 0) {
    Write-Host ''
    Write-Host ' Notes:' -ForegroundColor Yellow
    foreach ($n in $script:Notes) { Write-Host "   - $n" -ForegroundColor Yellow }
}

Write-Host ''
Write-Host ' Container status:' -ForegroundColor Cyan
if (Test-DockerReady) {
    $ps = Invoke-Native -Exe 'docker' -Arguments @('ps', '--format', '   {{.Names}} | {{.Image}} | {{.Ports}}') -Capture
    Write-Host $ps.Output.TrimEnd()
}

Write-Host ''
Write-Host ' Next step: .\run-full-stack.ps1' -ForegroundColor Green
Write-Host ''

if ($script:Issues.Count -gt 0) { exit 1 }
exit 0

