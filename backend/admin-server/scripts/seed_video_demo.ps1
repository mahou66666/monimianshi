param()

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir '..\..\..')).Path
$jarPath = Join-Path $repoRoot 'backend/admin-server/.m2repo/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar'
$javaFile = Join-Path $scriptDir 'SeedVideoDemo.java'
$tmpDir = Join-Path $scriptDir '.tmp-seed-video-demo'

if (!(Test-Path $jarPath)) {
  throw "MySQL connector not found: $jarPath"
}

if (!(Test-Path $javaFile)) {
  throw "Java source not found: $javaFile"
}

if (!(Test-Path $tmpDir)) {
  New-Item -ItemType Directory -Path $tmpDir | Out-Null
}

Copy-Item $javaFile (Join-Path $tmpDir 'SeedVideoDemo.java') -Force
Push-Location $tmpDir
try {
  javac -encoding UTF-8 -cp $jarPath .\SeedVideoDemo.java
  if ($LASTEXITCODE -ne 0) { throw 'javac failed' }
  java -cp "$jarPath;$tmpDir" SeedVideoDemo
  if ($LASTEXITCODE -ne 0) { throw 'java failed' }
}
finally {
  Pop-Location
}
