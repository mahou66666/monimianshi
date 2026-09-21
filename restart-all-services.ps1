param([switch]$SkipBuild, [switch]$NoElevate, [string]$DatabaseSettings = '')

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repo = $PSScriptRoot
$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($identity)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    if ($NoElevate) { throw 'Run this script as Administrator to restart elevated services.' }
    $arguments = '-NoProfile -ExecutionPolicy Bypass -File "' + $PSCommandPath + '" -NoElevate'
    if ($SkipBuild) { $arguments += ' -SkipBuild' }
    # UAC does not reliably inherit session environment variables. Carry only the
    # database overrides, encrypted with Windows DPAPI for the current user.
    $databaseOverrides = @{}
    foreach ($key in @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD')) {
        $value = [Environment]::GetEnvironmentVariable($key, 'Process')
        if ($null -ne $value) { $databaseOverrides[$key] = $value }
    }
    if ($databaseOverrides.Count) {
        $protected = ConvertTo-SecureString ($databaseOverrides | ConvertTo-Json -Compress) -AsPlainText -Force | ConvertFrom-SecureString
        $arguments += ' -DatabaseSettings "' + $protected + '"'
    }
    # This window intentionally shows restart progress and any errors to the user.
    $child = Start-Process powershell.exe -Verb RunAs -ArgumentList $arguments -Wait -PassThru
    exit $child.ExitCode
}

$logs = Join-Path $repo '.run-logs'
$state = Join-Path $repo '.run-state'
New-Item -ItemType Directory -Force -Path $logs, $state | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$failures = New-Object 'System.Collections.Generic.List[string]'

function Listening([int]$Port) {
    $client = New-Object Net.Sockets.TcpClient
    try { return $client.ConnectAsync('127.0.0.1', $Port).Wait(500) -and $client.Connected }
    catch { return $false }
    finally { $client.Dispose() }
}

function Initialize-NodePath {
    # Elevation can inherit an outdated PATH. Refresh only this process and its children.
    $pathValues = @($env:Path, [Environment]::GetEnvironmentVariable('Path', 'Machine'), [Environment]::GetEnvironmentVariable('Path', 'User'))
    $directories = @($pathValues | ForEach-Object { $_ -split ';' } | ForEach-Object {
        $directory = [Environment]::ExpandEnvironmentVariables($_.Trim().Trim('"'))
        # D: is drive-relative; D:\ is the absolute drive root.
        if ($directory -match '^[A-Za-z]:$') { $directory += '\' }
        if ($directory) { $directory }
    } | Select-Object -Unique)
    $env:Path = $directories -join ';'
    $npmCommand = Get-Command npm.cmd -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $npmCommand) {
        $candidates = @("$env:ProgramFiles\nodejs", "$env:LOCALAPPDATA\Programs\nodejs")
        foreach ($drive in Get-PSDrive -PSProvider FileSystem) {
            $candidates += Join-Path $drive.Root 'nodejs'
            $candidates += Join-Path $drive.Root 'Program Files\nodejs'
            $candidates += $drive.Root
        }
        foreach ($candidate in $candidates) {
            if ((Test-Path -LiteralPath (Join-Path $candidate 'npm.cmd')) -and (Test-Path -LiteralPath (Join-Path $candidate 'node.exe'))) {
                $env:Path = $candidate + ';' + $env:Path
                break
            }
        }
        $npmCommand = Get-Command npm.cmd -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    }
    if (-not $npmCommand) { throw 'Node.js/npm not found. Add the folder containing npm.cmd and node.exe to PATH, then retry.' }
    $npmDirectory = Split-Path -Parent $npmCommand.Source
    $env:Path = $npmDirectory + ';' + $env:Path
    Get-Command node.exe -CommandType Application -ErrorAction Stop | Out-Null
    Write-Host "npm located: $($npmCommand.Source)"
}

function Get-ContainerHostPorts($Container) {
    $bindings = $Container.HostConfig.PortBindings
    if ($null -eq $bindings) { return }
    # Windows PowerShell 5.1 + StrictMode cannot safely enumerate Properties.Value
    # when Docker returns an empty PortBindings object (e.g. PostgreSQL).
    foreach ($property in $bindings.PSObject.Properties) {
        foreach ($binding in @($property.Value)) {
            if ($null -eq $binding) { continue }
            $hostPortProperty = $binding.PSObject.Properties['HostPort']
            if ($null -ne $hostPortProperty -and $hostPortProperty.Value) {
                [string]$hostPortProperty.Value
            }
        }
    }
}

function Test-LegacyElfProcess($Process, [int]$RecordedPid) {
    if ($RecordedPid -le 0 -or -not $Process.CommandLine) { return $false }
    # Older launchers record cmd.exe, while its Python child owns port 8000.
    $matchesLauncher = $Process.ProcessId -eq $RecordedPid -or $Process.ParentProcessId -eq $RecordedPid
    return $matchesLauncher -and $Process.Name -match '^(python|pythonw|uvicorn)(\.exe)?$' -and
        $Process.CommandLine -match 'uvicorn(?:\.exe)?\s+app\.api\.server:app' -and
        $Process.CommandLine -match '--port(?:\s+|=)8000(?:\s|$)'
}

function Wait-ContainerReady([string]$ContainerId) {
    $deadline = (Get-Date).AddSeconds(120)
    do {
        $json = & docker inspect --format '{{json .State}}' $ContainerId
        if ($LASTEXITCODE -ne 0) { throw "Cannot inspect container state: $ContainerId" }
        $containerState = $json | ConvertFrom-Json
        $healthProperty = $containerState.PSObject.Properties['Health']
        if ($containerState.Running -and ($null -eq $healthProperty -or $healthProperty.Value.Status -eq 'healthy')) { return }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Container $ContainerId did not become ready. Check docker logs for this container."
}

try {
    if ($DatabaseSettings) {
        $secure = ConvertTo-SecureString $DatabaseSettings
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try {
            $overrides = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) | ConvertFrom-Json
            foreach ($property in $overrides.PSObject.Properties) {
                if ($property.Name -in @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD')) {
                    [Environment]::SetEnvironmentVariable($property.Name, [string]$property.Value, 'Process')
                }
            }
        } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
    }
    Initialize-NodePath
    foreach ($command in @('docker', 'java', 'python', 'npm.cmd')) { Get-Command $command -ErrorAction Stop | Out-Null }
    & docker info --format '{{.ServerVersion}}'
    if ($LASTEXITCODE -ne 0) { throw 'Start Docker Desktop and wait for its engine, then run this script again.' }

    # Only containers whose compose directory or bind mount belongs to this repository.
    $containerIds = @(& docker ps -aq)
    if ($LASTEXITCODE -ne 0) { throw 'Cannot list Docker containers.' }
    $containers = @()
    foreach ($containerId in $containerIds) {
        $details = & docker inspect $containerId | ConvertFrom-Json
        if ($LASTEXITCODE -ne 0) { throw "Cannot inspect container $containerId" }
        $item = @($details)[0]
        $paths = @($item.Mounts | ForEach-Object { $_.Source })
        if ($item.Config.Labels) {
            $label = $item.Config.Labels.PSObject.Properties['com.docker.compose.project.working_dir']
            if ($label) { $paths += $label.Value }
        }
        $repoNormalized = $repo.Replace('\', '/').TrimEnd('/').ToLowerInvariant()
        $owned = @($paths | Where-Object {
            $p = ([string]$_).Replace('\', '/').ToLowerInvariant()
            $p -eq $repoNormalized -or $p.StartsWith($repoNormalized + '/') -or $p.StartsWith('/run/desktop/mnt/host/' + $repoNormalized.Replace(':', '') + '/')
        }).Count -gt 0
        if ($owned) { $containers += $item }
    }

    # Recognize native project processes by full path or the project's Java main class.
    # A port number or a stale PID file alone never authorizes stopping a process.
    $processes = @(Get-CimInstance Win32_Process)
    $elfRecordedPid = 0
    $elfPidFile = Join-Path $state 'elf-8000.pid'
    if (Test-Path -LiteralPath $elfPidFile) {
        [void][int]::TryParse((Get-Content -LiteralPath $elfPidFile -Raw).Trim(), [ref]$elfRecordedPid)
    }
    $ownedProcesses = @($processes | Where-Object {
        $_.ProcessId -ne $PID -and $_.Name -match '^(java|javaw|python|pythonw|uvicorn|node)(\.exe)?$' -and $_.CommandLine -and (
            $_.CommandLine.IndexOf($repo, [StringComparison]::OrdinalIgnoreCase) -ge 0 -or
            $_.CommandLine -match 'com\.example\.springbootfront\.SpringbootFrontApplication' -or
            (Test-LegacyElfProcess $_ $elfRecordedPid)
        )
    })
    # run-full-stack also launches the voice/parser jars using relative paths.
    foreach ($unit in @{'sv-8082'='sv-service-0\.1\.0(?:-patched)?\.jar'; 'resume-8089'='resume_cut-0\.0\.1-SNAPSHOT\.jar'}.GetEnumerator()) {
        $pidPath = Join-Path $state ($unit.Key + '.pid')
        $launcherId = 0
        if (Test-Path -LiteralPath $pidPath) {
            [void][int]::TryParse((Get-Content -LiteralPath $pidPath -Raw).Trim(), [ref]$launcherId)
        }
        if ($launcherId -gt 0) {
            $ownedProcesses += @($processes | Where-Object {
                $_.Name -match '^java(?:w)?\.exe$' -and $_.CommandLine -and
                ($_.ProcessId -eq $launcherId -or $_.ParentProcessId -eq $launcherId) -and
                $_.CommandLine -match $unit.Value
            })
        }
    }
    $ownedProcesses = @($ownedProcesses | Sort-Object ProcessId -Unique)
    foreach ($process in $ownedProcesses) {
        Write-Host "Stopping project process $($process.Name) PID $($process.ProcessId)"
        Stop-Process -Id $process.ProcessId -Force -ErrorAction Stop
    }
    # Keep infrastructure available; only restart existing application containers.
    $applicationContainers = @($containers | Where-Object { $_.Name -notmatch '(mysql|postgres|redis|qdrant|funasr)' })
    foreach ($container in $applicationContainers) {
        Write-Host "Stopping application container $($container.Name)"
        & docker stop $container.Id
        if ($LASTEXITCODE -ne 0) { throw "Failed to stop $($container.Name)" }
    }

    if (-not $SkipBuild) {
        Push-Location (Join-Path $repo 'springboot-front(4)')
        try {
            & .\gradlew.bat bootJar -x test --no-daemon
            if ($LASTEXITCODE -ne 0) { throw 'Backend build failed. Services remain stopped; fix the build and rerun.' }
        } finally { Pop-Location }
    }

    # Use the same infrastructure commands as the established full-stack workflow.
    Push-Location $repo
    try {
        # local-db is an override file; it requires the base compose definition.
        & docker compose -f docker-compose.dev.yml -f docker-compose.local-db.yml up -d mysql
        if ($LASTEXITCODE -ne 0) { throw 'Failed to start local MySQL.' }
        foreach ($spec in @(
            @('docker-compose.dev.yml', 'redis'),
            @('Interview_elf_agent/docker-compose.yml', 'qdrant'),
            @('WB_interview_agent/docker-compose.yml', 'postgres')
        )) {
            & docker compose -f $spec[0] up -d $spec[1]
            if ($LASTEXITCODE -ne 0) { throw "Failed to start infrastructure: $($spec[1])" }
        }
        & docker start funasr-offline
        if ($LASTEXITCODE -ne 0) { throw 'Cannot start funasr-offline. Restore the existing FunASR container before retrying.' }
    } finally { Pop-Location }
    foreach ($name in @('ai-dev-mysql', 'ai-dev-redis')) { Wait-ContainerReady $name }

    if (-not $env:SPRING_DATASOURCE_URL) {
        $env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:13306/interview_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
    }
    if (-not $env:SPRING_DATASOURCE_USERNAME -or -not $env:SPRING_DATASOURCE_PASSWORD) {
        # Reuse local MySQL's configured credentials without printing or duplicating them.
        $mysqlEnvironment = & docker inspect --format '{{json .Config.Env}}' ai-dev-mysql | ConvertFrom-Json
        if ($LASTEXITCODE -ne 0) { throw 'Cannot read local MySQL settings.' }
        foreach ($setting in $mysqlEnvironment) {
            if ($setting.StartsWith('MYSQL_USER=') -and -not $env:SPRING_DATASOURCE_USERNAME) { $env:SPRING_DATASOURCE_USERNAME = $setting.Substring(11) }
            if ($setting.StartsWith('MYSQL_PASSWORD=') -and -not $env:SPRING_DATASOURCE_PASSWORD) { $env:SPRING_DATASOURCE_PASSWORD = $setting.Substring(15) }
        }
        if (-not $env:SPRING_DATASOURCE_USERNAME -or -not $env:SPRING_DATASOURCE_PASSWORD) { throw 'Set SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD before restarting.' }
    }
    foreach ($container in $applicationContainers) {
        & docker start $container.Id
        if ($LASTEXITCODE -ne 0) { throw "Failed to start $($container.Name)" }
        Wait-ContainerReady $container.Id
    }

    # Discard only dead PID records; never kill a process based on these records.
    foreach ($file in Get-ChildItem -LiteralPath $state -Filter '*.pid' -File) {
        $recorded = 0
        if ([int]::TryParse((Get-Content -LiteralPath $file.FullName -Raw).Trim(), [ref]$recorded)) {
            if (-not (Get-Process -Id $recorded -ErrorAction SilentlyContinue)) { Remove-Item -LiteralPath $file.FullName }
        }
    }
    foreach ($entry in @{'backend-8080'=8080; 'frontend-5173'=5173; 'sv-8082'=8082; 'agent-8010'=8010; 'resume-8089'=8089; 'elf-8000'=8000; 'admin-18081'=18081; 'admin-web-9527'=9527}.GetEnumerator()) {
        $pidPath = Join-Path $state ($entry.Key + '.pid')
        if (-not (Listening $entry.Value) -and (Test-Path -LiteralPath $pidPath)) { Remove-Item -LiteralPath $pidPath }
    }

    # Catch unidentified listeners before the legacy starter can skip a stale backend.
    foreach ($port in @(8080, 5173, 8000, 8082, 18081, 9527)) {
        if (-not (Listening $port)) { continue }
        $dockerOwnsPort = @($containers | Where-Object {
            @(Get-ContainerHostPorts $_) -contains [string]$port
        }).Count -gt 0
        if (-not $dockerOwnsPort) { throw "Port $port is held by an unidentified process. Restart cancelled to protect other applications. Close its original terminal and retry." }
    }

    # One canonical startup entry point owns all application-specific settings.
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repo 'run-full-stack.ps1')
    if ($LASTEXITCODE -ne 0) { $failures.Add('run-full-stack.ps1 failed. See .run-logs.') }

    $status = @(foreach ($port in @(5173, 8080, 8000, 8010, 8089, 8082, 10095, 9527, 18081)) {
        $ready = Listening $port
        if (-not $ready) { $failures.Add("Port $port is not listening") }
        [pscustomobject]@{ Port = $port; Listening = $ready }
    })
    $status | Format-Table -AutoSize
    if ($failures.Count) { throw ($failures -join "`n") }
    Write-Host 'Restart finished. Frontend: http://localhost:5173  Admin: http://localhost:9527' -ForegroundColor Green
    Write-Host "Logs: $logs"
    Write-Host 'Port checks passed. Model API availability must be checked in the application.'
    exit 0
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host $_.InvocationInfo.PositionMessage
    $_ | Out-String | Set-Content -LiteralPath (Join-Path $logs "restart-$stamp.error.log")
    Write-Host "Logs: $logs"
    exit 1
}
