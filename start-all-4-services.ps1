param(
    [switch]$SkipDockerBuild,
    [switch]$SkipBackend8080,
    [switch]$SkipSv8082,
    [switch]$SkipFrontend5173,
    [switch]$SkipFunAsr10095
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$StateDir = Join-Path $RepoRoot ".run-state"
$LogDir = Join-Path $RepoRoot ".run-logs"

New-Item -ItemType Directory -Force -Path $StateDir | Out-Null
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Write-Step {
    param([string]$Message)
    Write-Host "[start-4] $Message" -ForegroundColor Cyan
}

function Write-WarnLine {
    param([string]$Message)
    Write-Host "[start-4] $Message" -ForegroundColor Yellow
}

function Test-PortListening {
    param(
        [int]$Port,
        [string]$TargetHost = "127.0.0.1",
        [int]$TimeoutMs = 500
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $iar = $client.BeginConnect($TargetHost, $Port, $null, $null)
        if (-not $iar.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) {
            return $false
        }
        $client.EndConnect($iar) | Out-Null
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Close()
    }
}

function Wait-Port {
    param(
        [int]$Port,
        [int]$Retries = 60,
        [int]$DelaySeconds = 2
    )

    for ($i = 0; $i -lt $Retries; $i++) {
        if (Test-PortListening -Port $Port) {
            return $true
        }
        Start-Sleep -Seconds $DelaySeconds
    }
    return $false
}

function Invoke-DockerCompose {
    param(
        [string]$WorkDir,
        [string[]]$ComposeArgs
    )

    Push-Location $WorkDir
    try {
        & docker compose @ComposeArgs
        if ($LASTEXITCODE -ne 0) {
            throw "docker compose failed in $WorkDir (exit code: $LASTEXITCODE)"
        }
    }
    finally {
        Pop-Location
    }
}

function Test-DockerReady {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        return $false
    }
    try {
        & docker version | Out-Null
        return $LASTEXITCODE -eq 0
    }
    catch {
        return $false
    }
}

function Read-DotEnvFile {
    param([string]$Path)

    $map = @{}
    if (-not (Test-Path $Path)) {
        return $map
    }

    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line.Length -eq 0 -or $line.StartsWith("#")) {
            return
        }
        $eqIndex = $line.IndexOf("=")
        if ($eqIndex -lt 1) {
            return
        }
        $key = $line.Substring(0, $eqIndex).Trim()
        $value = $line.Substring($eqIndex + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $map[$key] = $value
    }

    return $map
}

function Resolve-ResumeParserJar {
    param([string]$TargetDir)

    $candidates = @(
        Get-ChildItem -LiteralPath $TargetDir -Filter "*.jar" -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notlike "*.original" } |
            Sort-Object LastWriteTime -Descending
    )
    if ($candidates.Count -eq 0) {
        return ""
    }
    return $candidates[0].FullName
}

function Resolve-SpringBackendJar {
    param([string]$LibDir)

    $candidates = @(
        Get-ChildItem -LiteralPath $LibDir -Filter "*.jar" -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notlike "*-plain.jar" } |
            Sort-Object LastWriteTime -Descending
    )
    if ($candidates.Count -eq 0) {
        return ""
    }
    return $candidates[0].FullName
}

function Start-ManagedProcess {
    param(
        [string]$Name,
        [string]$WorkDir,
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$StdOutPath,
        [string]$StdErrPath,
        [string]$PidFilePath
    )

    if (Test-Path $PidFilePath) {
        $oldPidText = (Get-Content -LiteralPath $PidFilePath -ErrorAction SilentlyContinue | Select-Object -First 1)
        $oldPid = 0
        if ($oldPidText -and [int]::TryParse($oldPidText, [ref]$oldPid)) {
            $oldProc = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
            if ($oldProc) {
                Write-Step "$Name is already running (pid=$oldPid)"
                return $oldPid
            }
        }
        Remove-Item -LiteralPath $PidFilePath -Force -ErrorAction SilentlyContinue
    }

    $proc = Start-Process `
        -FilePath $FilePath `
        -ArgumentList $Arguments `
        -WorkingDirectory $WorkDir `
        -RedirectStandardOutput $StdOutPath `
        -RedirectStandardError $StdErrPath `
        -WindowStyle Hidden `
        -PassThru

    Set-Content -LiteralPath $PidFilePath -Value $proc.Id -Encoding ASCII
    Write-Step "$Name started (pid=$($proc.Id))"
    return $proc.Id
}

Write-Step "Repo root: $RepoRoot"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker command not found. Please install Docker Desktop first."
}

# 1) 8010 - WB interview agent (docker compose)
$agentDir = Join-Path $RepoRoot "WB_interview_agent"
if (-not (Test-Path $agentDir)) {
    throw "Directory not found: $agentDir"
}

if (Test-PortListening -Port 8010) {
    Write-Step "Port 8010 is already listening; skip starting WB interview agent."
}
else {
    $agentStarted = $false
    $agentArgs = @("up", "-d")
    if (-not $SkipDockerBuild) {
        $agentArgs += "--build"
    }
    $agentArgs += @("postgres", "app")
    Write-Step "Starting WB interview agent (8010) via docker compose..."
    try {
        Invoke-DockerCompose -WorkDir $agentDir -ComposeArgs $agentArgs
        if (Wait-Port -Port 8010 -Retries 90 -DelaySeconds 2) {
            $agentStarted = $true
        }
    }
    catch {
        Write-WarnLine "Docker compose for 8010 failed: $($_.Exception.Message)"
    }

    if (-not $agentStarted) {
        Write-WarnLine "Fallback: start WB interview agent locally with python (no Docker build)."
        $agentOut = Join-Path $LogDir "agent-8010.out.log"
        $agentErr = Join-Path $LogDir "agent-8010.err.log"
        $agentPid = Join-Path $StateDir "agent-8010.pid"
        Start-ManagedProcess `
            -Name "WB interview agent 8010" `
            -WorkDir $agentDir `
            -FilePath "python" `
            -Arguments @("-m", "uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8010") `
            -StdOutPath $agentOut `
            -StdErrPath $agentErr `
            -PidFilePath $agentPid | Out-Null

        if (-not (Wait-Port -Port 8010 -Retries 90 -DelaySeconds 2)) {
            throw "WB interview agent did not listen on 8010 in time. Check logs: $agentOut / $agentErr"
        }
    }
}

# 2) 8089 - resume parser (docker compose)
$resumeParserDir = Join-Path $RepoRoot "resume_cut_docker"
if (-not (Test-Path $resumeParserDir)) {
    throw "Directory not found: $resumeParserDir"
}

if (Test-PortListening -Port 8089) {
    Write-Step "Port 8089 is already listening; skip starting resume parser."
}
else {
    $resumeStarted = $false
    $resumeArgs = @("up", "-d")
    if (-not $SkipDockerBuild) {
        $resumeArgs += "--build"
    }

    Write-Step "Starting resume parser (8089) via docker compose..."
    try {
        Invoke-DockerCompose -WorkDir $resumeParserDir -ComposeArgs $resumeArgs
        if (Wait-Port -Port 8089 -Retries 90 -DelaySeconds 2) {
            $resumeStarted = $true
        }
    }
    catch {
        Write-WarnLine "Docker compose for 8089 failed: $($_.Exception.Message)"
    }

    if (-not $resumeStarted) {
        Write-WarnLine "Fallback: start resume parser locally with java -jar (no Docker build)."
        $resumeTargetDir = Join-Path $resumeParserDir "target"
        $resumeJar = Resolve-ResumeParserJar -TargetDir $resumeTargetDir
        if (-not $resumeJar) {
            throw "No resume parser jar found in $resumeTargetDir. Build it first."
        }

        $envMap = Read-DotEnvFile -Path (Join-Path $resumeParserDir ".env")
        $javaArgs = @("-jar", ('"' + $resumeJar + '"'), "--server.port=8089")
        if ($envMap.ContainsKey("LLM_API_URL") -and $envMap["LLM_API_URL"]) {
            $javaArgs += "--llm.api.url=$($envMap["LLM_API_URL"])"
        }
        if ($envMap.ContainsKey("LLM_API_KEY") -and $envMap["LLM_API_KEY"]) {
            $javaArgs += "--llm.api.key=$($envMap["LLM_API_KEY"])"
        }
        if ($envMap.ContainsKey("LLM_MODEL") -and $envMap["LLM_MODEL"]) {
            $javaArgs += "--llm.model=$($envMap["LLM_MODEL"])"
        }

        $resumeOut = Join-Path $LogDir "resume-8089.out.log"
        $resumeErr = Join-Path $LogDir "resume-8089.err.log"
        $resumePid = Join-Path $StateDir "resume-8089.pid"
        Start-ManagedProcess `
            -Name "resume parser 8089" `
            -WorkDir $resumeParserDir `
            -FilePath "java" `
            -Arguments $javaArgs `
            -StdOutPath $resumeOut `
            -StdErrPath $resumeErr `
            -PidFilePath $resumePid | Out-Null

        if (-not (Wait-Port -Port 8089 -Retries 90 -DelaySeconds 2)) {
            throw "Resume parser did not listen on 8089 in time. Check logs: $resumeOut / $resumeErr"
        }
    }
}

# 3) 8080 - springboot-front backend (gradle bootRun)
if (-not $SkipBackend8080) {
    $backendDirCandidates = @(
        "springboot-front(4)",
        "springboot-front(3)",
        "springboot-front"
    )
    $backendDir = $null
    foreach ($candidate in $backendDirCandidates) {
        $candidatePath = Join-Path $RepoRoot $candidate
        if (Test-Path $candidatePath) {
            $backendDir = $candidatePath
            break
        }
    }
    if (-not $backendDir) {
        throw "Directory not found: springboot-front(4)/springboot-front(3)/springboot-front"
    }
    $backendName = Split-Path -Leaf $backendDir

    if (Test-PortListening -Port 8080) {
        Write-Step "Port 8080 is already listening; skip starting $backendName."
    }
    else {
        $backendOut = Join-Path $LogDir "backend-8080.out.log"
        $backendErr = Join-Path $LogDir "backend-8080.err.log"
        $backendPid = Join-Path $StateDir "backend-8080.pid"
        $backendJar = Resolve-SpringBackendJar -LibDir (Join-Path $backendDir "build\libs")
        if ($backendJar) {
            Write-Step "Starting $backendName backend on 8080 via jar: $backendJar"
            Start-ManagedProcess `
                -Name "$backendName 8080" `
                -WorkDir $backendDir `
                -FilePath "java" `
                -Arguments @(
                    "-jar", ('"' + $backendJar + '"'),
                    "--server.port=8080",
                    "--funasr.base-url=http://127.0.0.1:8082",
                    "--funasr.transcribe-path=/api/voice/recognize",
                    "--funasr.file-field=audio",
                    "--funasr.timeout-ms=90000"
                ) `
                -StdOutPath $backendOut `
                -StdErrPath $backendErr `
                -PidFilePath $backendPid | Out-Null
        }
        else {
            $gradleHome = Join-Path $env:USERPROFILE ".gradle"
            try {
                New-Item -ItemType Directory -Force -Path $gradleHome | Out-Null
            }
            catch {
                $gradleHome = Join-Path $backendDir ".gradle-home-auto"
                New-Item -ItemType Directory -Force -Path $gradleHome | Out-Null
                Write-WarnLine "Cannot use global .gradle cache, fallback to $gradleHome"
            }

            Write-Step "Starting $backendName backend on 8080 via gradle bootRun (GRADLE_USER_HOME=$gradleHome)..."
            Start-ManagedProcess `
                -Name "$backendName 8080" `
                -WorkDir $backendDir `
                -FilePath "cmd.exe" `
                -Arguments @(
                    "/c",
                    "set ""GRADLE_USER_HOME=$gradleHome"" && .\gradlew.bat --no-daemon bootRun --console=plain --args=""--server.port=8080 --funasr.base-url=http://127.0.0.1:8082 --funasr.transcribe-path=/api/voice/recognize --funasr.file-field=audio --funasr.timeout-ms=90000"""
                ) `
                -StdOutPath $backendOut `
                -StdErrPath $backendErr `
                -PidFilePath $backendPid | Out-Null
        }

        if (-not (Wait-Port -Port 8080 -Retries 150 -DelaySeconds 2)) {
            Write-WarnLine "8080 did not open yet. Check logs: $backendOut / $backendErr"
        }
    }
}
else {
    Write-Step "Skip 8080 by flag -SkipBackend8080"
}

# 4) 10095 - FunASR websocket (docker)
if (-not $SkipFunAsr10095) {
    if (Test-PortListening -Port 10095) {
        Write-Step "Port 10095 is already listening; skip starting FunASR ws."
    }
    else {
        if (-not (Test-DockerReady)) {
            Write-WarnLine "Docker is not ready. Skip auto-start for FunASR ws (10095)."
        }
        else {
            $funasrModelDir = Join-Path $RepoRoot "funasr-runtime-resources\models"
            New-Item -ItemType Directory -Force -Path $funasrModelDir | Out-Null

            $funasrImage = "registry.cn-hangzhou.aliyuncs.com/funasr_repo/funasr:funasr-runtime-sdk-online-cpu-0.1.12"
            $funasrVolume = "${funasrModelDir}:/workspace/models"

            # Default to official 2pass chain from runtime docs.
            # You can override with env vars:
            #   FUNASR_MODEL_DIR / FUNASR_ONLINE_MODEL_DIR / FUNASR_VAD_DIR / FUNASR_PUNC_DIR / FUNASR_LM_DIR / FUNASR_ITN_DIR
            #   FUNASR_ENABLE_HOTWORD=true (disabled by default to reduce forced wrong substitutions)
            $funasrModelId = if ($env:FUNASR_MODEL_DIR -and $env:FUNASR_MODEL_DIR.Trim()) {
                $env:FUNASR_MODEL_DIR.Trim()
            } else {
                "damo/speech_paraformer-large-vad-punc_asr_nat-zh-cn-16k-common-vocab8404-onnx"
            }
            $funasrOnlineModelId = if ($env:FUNASR_ONLINE_MODEL_DIR -and $env:FUNASR_ONLINE_MODEL_DIR.Trim()) {
                $env:FUNASR_ONLINE_MODEL_DIR.Trim()
            } else {
                "damo/speech_paraformer-large_asr_nat-zh-cn-16k-common-vocab8404-online-onnx"
            }
            $funasrVadModelId = if ($env:FUNASR_VAD_DIR -and $env:FUNASR_VAD_DIR.Trim()) {
                $env:FUNASR_VAD_DIR.Trim()
            } else {
                "damo/speech_fsmn_vad_zh-cn-16k-common-onnx"
            }
            $funasrPuncModelId = if ($env:FUNASR_PUNC_DIR -and $env:FUNASR_PUNC_DIR.Trim()) {
                $env:FUNASR_PUNC_DIR.Trim()
            } else {
                "damo/punc_ct-transformer_zh-cn-common-vad_realtime-vocab272727-onnx"
            }
            $funasrLmModelId = if ($env:FUNASR_LM_DIR -and $env:FUNASR_LM_DIR.Trim()) {
                $env:FUNASR_LM_DIR.Trim()
            } else {
                "damo/speech_ngram_lm_zh-cn-ai-wesp-fst"
            }
            $funasrItnModelId = if ($env:FUNASR_ITN_DIR -and $env:FUNASR_ITN_DIR.Trim()) {
                $env:FUNASR_ITN_DIR.Trim()
            } else {
                "thuduj12/fst_itn_zh"
            }
            $funasrEnableHotword = [string]::Equals($env:FUNASR_ENABLE_HOTWORD, "true", [System.StringComparison]::OrdinalIgnoreCase)
            $funasrHotwordArg = if ($funasrEnableHotword) { " --hotword /workspace/models/hotwords.txt" } else { "" }
            $hotwordState = if ($funasrEnableHotword) { "on" } else { "off" }

            $funasrCmd = "/workspace/FunASR/runtime/websocket/build/bin/funasr-wss-server-2pass --download-model-dir /workspace/models --model-dir $funasrModelId --online-model-dir $funasrOnlineModelId --vad-dir $funasrVadModelId --punc-dir $funasrPuncModelId --lm-dir $funasrLmModelId --itn-dir $funasrItnModelId --decoder-thread-num `$(grep -c processor /proc/cpuinfo || echo 32) --model-thread-num 1 --io-thread-num 2 --port 10095 --certfile 0 --keyfile 0$funasrHotwordArg"

            Write-Step "Starting FunASR ws on 10095 via docker container (model=$funasrModelId, hotword=$hotwordState)..."
            & docker rm -f funasr-offline | Out-Null 2>$null
            & docker run -d --name funasr-offline --restart unless-stopped -p "10095:10095" --privileged=true -v $funasrVolume $funasrImage bash -lc $funasrCmd | Out-Null

            if (-not (Wait-Port -Port 10095 -Retries 120 -DelaySeconds 2)) {
                Write-WarnLine "10095 did not open yet. Check: docker logs -f funasr-offline"
            }
        }
    }
}
else {
    Write-Step "Skip 10095 by flag -SkipFunAsr10095"
}

# 5) 8082 - sv-service (java -jar)
if (-not $SkipSv8082) {
    $svDir = Join-Path $RepoRoot "FunASR-main\sv-service"
    $svJarPatched = Join-Path $svDir "target\sv-service-0.1.0-patched.jar"
    $svJarDefault = Join-Path $svDir "target\sv-service-0.1.0.jar"
    $svJar = if (Test-Path $svJarDefault) { $svJarDefault } else { $svJarPatched }
    if (-not (Test-Path $svDir)) {
        throw "Directory not found: $svDir"
    }
    if (-not (Test-Path $svJar)) {
        throw "sv-service jar not found: $svJar`nPlease build first: mvn -DskipTests package (in FunASR-main\\sv-service)"
    }

    if (Test-PortListening -Port 8082) {
        Write-Step "Port 8082 is already listening; skip starting sv-service."
    }
    else {
        $svOut = Join-Path $LogDir "sv-8082.out.log"
        $svErr = Join-Path $LogDir "sv-8082.err.log"
        $svPid = Join-Path $StateDir "sv-8082.pid"
        Write-Step "Starting sv-service on 8082 (asr.ws-uri=ws://127.0.0.1:10095, mode=2pass, upload<=64MB)..."
        Start-ManagedProcess `
            -Name "sv-service 8082" `
            -WorkDir $svDir `
            -FilePath "java" `
            -Arguments @(
                "-jar", ('"' + $svJar + '"'),
                "--asr.ws-uri=ws://127.0.0.1:10095",
                "--asr.mode=2pass",
                "--asr.tech-normalization.enabled=false",
                "--asr.result-timeout-ms=180000",
                "--asr.allow-partial-on-timeout=false",
                "--spring.servlet.multipart.max-file-size=64MB",
                "--spring.servlet.multipart.max-request-size=64MB",
                "--server.port=8082"
            ) `
            -StdOutPath $svOut `
            -StdErrPath $svErr `
            -PidFilePath $svPid | Out-Null

        if (-not (Wait-Port -Port 8082 -Retries 60 -DelaySeconds 2)) {
            Write-WarnLine "8082 did not open yet. Check logs: $svOut / $svErr"
        }
    }
}
else {
    Write-Step "Skip 8082 by flag -SkipSv8082"
}

# 6) 5173 - vue frontend (npm run dev)
if (-not $SkipFrontend5173) {
    $frontendDirCandidates = @(
        "vue-front (4)",
        "vue-front (3)",
        "vue-front"
    )
    $frontendDir = $null
    foreach ($candidate in $frontendDirCandidates) {
        $candidatePath = Join-Path $RepoRoot $candidate
        if (Test-Path $candidatePath) {
            $frontendDir = $candidatePath
            break
        }
    }

    if (-not $frontendDir) {
        throw "Directory not found: vue-front (4)/vue-front (3)/vue-front"
    }

    $frontendName = Split-Path -Leaf $frontendDir
    if (-not (Test-Path (Join-Path $frontendDir "package.json"))) {
        throw "package.json not found in frontend dir: $frontendDir"
    }

    if (Test-PortListening -Port 5173) {
        Write-Step "Port 5173 is already listening; skip starting $frontendName."
    }
    else {
        $frontendOut = Join-Path $LogDir "frontend-5173.out.log"
        $frontendErr = Join-Path $LogDir "frontend-5173.err.log"
        $frontendPid = Join-Path $StateDir "frontend-5173.pid"

        Write-Step "Starting $frontendName on 5173 via npm run dev..."
        Start-ManagedProcess `
            -Name "$frontendName 5173" `
            -WorkDir $frontendDir `
            -FilePath "cmd.exe" `
            -Arguments @("/c", "npm run dev -- --host 0.0.0.0 --port 5173") `
            -StdOutPath $frontendOut `
            -StdErrPath $frontendErr `
            -PidFilePath $frontendPid | Out-Null

        if (-not (Wait-Port -Port 5173 -Retries 90 -DelaySeconds 1)) {
            Write-WarnLine "5173 did not open yet. Check logs: $frontendOut / $frontendErr"
        }
    }
}
else {
    Write-Step "Skip 5173 by flag -SkipFrontend5173"
}

Write-Host ""
Write-Host "Service Port Status:" -ForegroundColor Green
@(
    [pscustomobject]@{ Service = "WB interview agent"; Port = 8010; Listening = (Test-PortListening -Port 8010) }
    [pscustomobject]@{ Service = "Resume parser"; Port = 8089; Listening = (Test-PortListening -Port 8089) }
    [pscustomobject]@{ Service = "Spring backend"; Port = 8080; Listening = (Test-PortListening -Port 8080) }
    [pscustomobject]@{ Service = "SV gateway"; Port = 8082; Listening = (Test-PortListening -Port 8082) }
    [pscustomobject]@{ Service = "Vue frontend"; Port = 5173; Listening = (Test-PortListening -Port 5173) }
    [pscustomobject]@{ Service = "FunASR ws"; Port = 10095; Listening = (Test-PortListening -Port 10095) }
) | Format-Table -AutoSize

if (-not (Test-PortListening -Port 10095)) {
    Write-WarnLine "Port 10095 is not listening. /api/voice/recognize will timeout until local FunASR ws is up."
}

Write-Host ""
Write-Host "Logs directory: $LogDir" -ForegroundColor Green
Write-Host "PID state directory: $StateDir" -ForegroundColor Green
Write-Host "Frontend URL: http://localhost:5173" -ForegroundColor Green
Write-Host "Stop script: .\stop-all-4-services.ps1" -ForegroundColor Green
