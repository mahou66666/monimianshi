<#
.SYNOPSIS
    Clears the four items left open after the first bootstrap run.

.DESCRIPTION
    The first run surfaced four distinct problems:

      1. FunASR image was never pulled.
         Cause was a bug in bootstrap-deps.ps1: Test-ImagePresent ran
         `docker image inspect` under $ErrorActionPreference='Stop', so the
         expected 'No such image' stderr line became a terminating error and
         aborted the step before `docker pull` ran. Fixed in bootstrap-deps.ps1;
         this script performs the pull with the corrected helper.

      2. sv-service (:8082) crashed on startup.
         Cause: models/speaker-embedding.onnx is not deployed, and
         SpeakerEmbeddingExtractor throws from its constructor, which fails the
         whole Spring context. SpeakerVerificationService was changed to degrade
         instead of fail, so /api/voice/recognize can serve ASR without the
         optional speaker-verification model. This script rebuilds the jar.

      3. npm is not on PATH, so admin-web (:9527) has no node_modules.
         This script locates Node on disk and reports what to add to PATH.

      4. Two Qdrant containers compete for host port 6333.
         jobplus-qdrant (v1.12.5) holds the host mapping; the container created
         by Interview_elf_agent/docker-compose.yml has none. This script reports
         the conflict; it does not stop another project's container for you.

    Run bootstrap-deps.ps1 first. This script is idempotent.

.PARAMETER SkipFunAsr
    Do not pull the FunASR image or (re)create the funasr-offline container.

.PARAMETER SkipSvRebuild
    Do not rebuild sv-service.

.PARAMETER SvModelsDir
    Optional. If you have the speaker ONNX model, point this at the directory
    containing speaker-embedding.onnx and it will be copied into
    FunASR-main/sv-service/models/.

.EXAMPLE
    .\fix-remaining.ps1
    .\fix-remaining.ps1 -SkipFunAsr
#>
[CmdletBinding()]
param(
    [switch]$SkipFunAsr,
    [switch]$SkipSvRebuild,
    [string]$SvModelsDir = ''
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogDir   = Join-Path $RepoRoot '.bootstrap-logs'
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$script:StepNo = 0
$script:Issues = New-Object System.Collections.Generic.List[string]
$script:Notes  = New-Object System.Collections.Generic.List[string]

function Write-Step { param([string]$m) $script:StepNo++; Write-Host ''; Write-Host ('[{0:00}] {1}' -f $script:StepNo, $m) -ForegroundColor Cyan }
function Write-Ok   { param([string]$m) Write-Host "      ok   $m" -ForegroundColor Green }
function Write-Skip { param([string]$m) Write-Host "      skip $m" -ForegroundColor DarkGray }
function Write-Note { param([string]$m) Write-Host "      ..   $m" -ForegroundColor Yellow }
function Write-Bad  { param([string]$m) Write-Host "      FAIL $m" -ForegroundColor Red }

# Native tools write to stderr even on success paths; never let that become a
# terminating error. Always branch on the returned exit code.
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

function Test-Cmd {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Add-Issue { param([string]$S, [string]$M) $script:Issues.Add("$S :: $M") }

# Node is unpacked directly into D:\ on this machine, so npm.cmd is not on PATH.
function Resolve-Npm {
    $cmd = Get-Command 'npm' -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

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

function Invoke-In {
    param([string]$Path, [string]$Exe, [string[]]$Arguments)
    Push-Location -LiteralPath $Path
    try {
        $r = Invoke-Native -Exe $Exe -Arguments $Arguments
        if ($r.ExitCode -ne 0) {
            throw "'$Exe $($Arguments -join ' ')' failed in '$Path' (exit code $($r.ExitCode))"
        }
    }
    finally { Pop-Location }
}

Write-Host ''
Write-Host '=========================================================' -ForegroundColor White
Write-Host ' Remediation for the four open items'                    -ForegroundColor White
Write-Host '=========================================================' -ForegroundColor White
Write-Host " repo root : $RepoRoot"

# =========================================================== 1) node / npm ===

Write-Step 'Locate Node.js / npm'

$npmCmd = Resolve-Npm
$nodeExe = $null

if ($npmCmd) {
    Write-Ok "npm: $npmCmd"
    $nodeDir = Split-Path -Parent $npmCmd
    $nodeExe = Join-Path $nodeDir 'node.exe'
    if (Test-Path -LiteralPath $nodeExe) {
        $ver = (Invoke-Native -Exe $nodeExe -Arguments @('--version') -Capture).Output.Trim()
        Write-Ok "node: $ver"
    }
    else {
        $nodeExe = $null
    }

    if ($nodeDir -and ($env:PATH -notlike "*$nodeDir*")) {
        $script:Notes.Add("npm/node live in $nodeDir, which is not on PATH. Scripts now use the full path; add the directory to PATH for interactive use.")
    }
    else {
        Write-Ok "node directory is on PATH"
    }
}
else {
    Write-Bad 'Node.js / npm not found.'
    Write-Note 'Expected D:\npm.cmd on this machine. Verify with: where.exe npm'
    Add-Issue 'node' 'Node.js / npm not found'
}

# ============================================================= 2) sv-service ===

Write-Step 'sv-service (:8082) - rebuild with the speaker-model fallback'

if ($SkipSvRebuild) {
    Write-Skip 'skipped by flag'
}
else {
    $svDir = Join-Path $RepoRoot 'FunASR-main\sv-service'

    if ($SvModelsDir) {
        try {
            $modelsDir = Join-Path $svDir 'models'
            New-Item -ItemType Directory -Force -Path $modelsDir | Out-Null
            Copy-Item -LiteralPath (Join-Path $SvModelsDir 'speaker-embedding.onnx') -Destination $modelsDir -Force
            Write-Ok "speaker model copied to $modelsDir"
        }
        catch {
            Write-Bad "could not copy speaker model: $($_.Exception.Message)"
            Add-Issue 'sv-service' "speaker model copy: $($_.Exception.Message)"
        }
    }
    else {
        $modelPath = Join-Path $svDir 'models\speaker-embedding.onnx'
        if (Test-Path $modelPath) {
            Write-Ok 'speaker-embedding.onnx present - full speaker verification will be active'
        }
        else {
            Write-Note 'speaker-embedding.onnx is absent.'
            Write-Note 'After the rebuild sv-service will still start and /api/voice/recognize (ASR) will work.'
            Write-Note 'Only /enroll, /verify and /recognize_filter stay disabled until the ONNX model is deployed.'
            Write-Note 'To enable them, supply the model with: .\fix-remaining.ps1 -SvModelsDir <dir>'
            $script:Notes.Add('Speaker verification (/enroll, /verify) stays disabled until models/speaker-embedding.onnx exists.')
        }
    }

    if (-not (Test-Cmd 'mvn')) {
        Write-Bad 'mvn not found on PATH - cannot rebuild sv-service.'
        Add-Issue 'sv-service' 'mvn not found on PATH'
    }
    else {
        try {
            Write-Note 'mvn -DskipTests package (sv-service)'
            Invoke-In -Path $svDir -Exe 'mvn' -Arguments @('-B', '-DskipTests', 'package')
            Write-Ok 'sv-service jar rebuilt'
        }
        catch {
            Write-Bad "sv-service rebuild: $($_.Exception.Message)"
            Add-Issue 'sv-service' "rebuild failed: $($_.Exception.Message)"
        }
    }
}

# =============================================================== 3) FunASR ===

Write-Step 'FunASR runtime (:10095) - pull image and (re)create container'

if ($SkipFunAsr) {
    Write-Skip 'skipped by flag'
}
else {
    $dockerOk = (Test-Cmd 'docker') -and ((Invoke-Native -Exe 'docker' -Arguments @('version') -Capture).ExitCode -eq 0)

    if (-not $dockerOk) {
        Write-Bad 'Docker daemon not reachable.'
        Add-Issue 'funasr' 'Docker daemon not reachable'
    }
    else {
        $image = 'registry.cn-hangzhou.aliyuncs.com/funasr_repo/funasr:funasr-runtime-sdk-online-cpu-0.1.12'

        $present = (Invoke-Native -Exe 'docker' -Arguments @('image', 'inspect', $image) -Capture).ExitCode -eq 0
        if ($present) {
            Write-Skip "image already present: $image"
        }
        else {
            Write-Note "pulling $image (several GB, from the Aliyun mirror)"
            $pull = Invoke-Native -Exe 'docker' -Arguments @('pull', $image) -Capture
            if ($pull.ExitCode -ne 0) {
                $tail = ($pull.Output.Trim() -split "`n" | Select-Object -Last 4) -join ' | '
                Write-Bad "pull failed: $tail"
                Add-Issue 'funasr' "image pull failed: $tail"
            }
            else {
                Write-Ok 'image ready'
            }
        }

        $hasImage = (Invoke-Native -Exe 'docker' -Arguments @('image', 'inspect', $image) -Capture).ExitCode -eq 0
        if ($hasImage) {
            $modelDir = Join-Path $RepoRoot 'funasr-runtime-resources\models'
            New-Item -ItemType Directory -Force -Path $modelDir | Out-Null

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

            Write-Note 'recreating container funasr-offline'
            Invoke-Native -Exe 'docker' -Arguments @('rm', '-f', 'funasr-offline') -Capture | Out-Null

            $run = Invoke-Native -Exe 'docker' -Arguments @(
                'run', '-d', '--name', 'funasr-offline', '--restart', 'unless-stopped',
                '-p', '10095:10095', '--privileged=true',
                '-v', "${modelDir}:/workspace/models",
                $image, 'bash', '-lc', $cmd
            ) -Capture

            if ($run.ExitCode -ne 0) {
                $tail = ($run.Output.Trim() -split "`n" | Select-Object -Last 4) -join ' | '
                Write-Bad "docker run failed: $tail"
                Add-Issue 'funasr' "container start failed: $tail"
            }
            else {
                Write-Ok 'container funasr-offline started on 10095'
                Write-Note 'the ONNX model chain downloads on first start - watch it with: docker logs -f funasr-offline'
                Write-Note 'the container needs several minutes and a few GB before 10095 accepts connections'
            }
        }
    }
}

# ============================================================== 4) Qdrant ===

Write-Step 'Qdrant - check for a host-port conflict on 6333'

if (-not ((Test-Cmd 'docker') -and ((Invoke-Native -Exe 'docker' -Arguments @('version') -Capture).ExitCode -eq 0))) {
    Write-Skip 'Docker not available'
}
else {
    $ps = Invoke-Native -Exe 'docker' -Arguments @('ps', '--format', '{{.Names}}|{{.Image}}|{{.Ports}}') -Capture
    $qdrantLines = @($ps.Output -split "`n" | Where-Object { $_ -match 'qdrant' })

    if ($qdrantLines.Count -eq 0) {
        Write-Bad 'no running Qdrant container'
        Write-Note 'start one: docker compose -f Interview_elf_agent/docker-compose.yml up -d qdrant'
    }
    else {
        foreach ($line in $qdrantLines) { Write-Host "      $($line.Trim())" -ForegroundColor Gray }

        $boundTo333 = @($qdrantLines | Where-Object { $_ -match '0\.0\.0\.0:6333' })
        if ($qdrantLines.Count -gt 1) {
            Write-Bad "more than one Qdrant instance is running ($($qdrantLines.Count))."
            if ($boundTo333.Count -eq 1) {
                $owner = ($boundTo333[0] -split '\|')[0]
                Write-Note "host port 6333 is held by '$owner'."
                Write-Note 'Interview_elf_agent connects to localhost:6333, so it talks to that instance and not to'
                Write-Note 'the one from its own compose file. Stop the unrelated one, then restart the agent:'
                Write-Host "        docker stop $owner" -ForegroundColor Gray
                Write-Host '        docker compose -f Interview_elf_agent/docker-compose.yml up -d --force-recreate qdrant' -ForegroundColor Gray
                Add-Issue 'qdrant' "port 6333 held by '$owner'; elf_agent may target the wrong instance"
            }
        }
        else {
            Write-Ok 'exactly one Qdrant instance running'
        }
    }
}

# ================================================================= summary ====

Write-Host ''
Write-Host '=========================================================' -ForegroundColor White
Write-Host ' remediation summary'                                     -ForegroundColor White
Write-Host '=========================================================' -ForegroundColor White

if ($script:Issues.Count -eq 0) {
    Write-Host ' Nothing left blocking.' -ForegroundColor Green
}
else {
    Write-Host (" {0} item(s) still open:" -f $script:Issues.Count) -ForegroundColor Red
    foreach ($i in $script:Issues) { Write-Host "   - $i" -ForegroundColor Red }
}

if ($script:Notes.Count -gt 0) {
    Write-Host ''
    Write-Host ' Notes:' -ForegroundColor Yellow
    foreach ($n in $script:Notes) { Write-Host "   - $n" -ForegroundColor Yellow }
}

Write-Host ''
if (Test-Cmd 'docker') {
    $ps = Invoke-Native -Exe 'docker' -Arguments @('ps', '--format', '   {{.Names}} | {{.Image}} | {{.Ports}}') -Capture
    Write-Host ' Containers:' -ForegroundColor Cyan
    Write-Host $ps.Output.TrimEnd()
}

Write-Host ''
Write-Host ' Next: .\run-full-stack.ps1' -ForegroundColor Green
Write-Host ''

if ($script:Issues.Count -gt 0) { exit 1 }
exit 0
