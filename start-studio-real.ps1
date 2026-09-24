param([switch]$InitializeTestAccounts, [ValidateSet('h2','mysql')][string]$Database='h2')
$ErrorActionPreference = 'Stop'
if ($Database -eq 'mysql') {
    if ($InitializeTestAccounts) { throw 'MySQL schema and test-account setup is a separate reviewed migration. Do not use -InitializeTestAccounts.' }
    if ([string]::IsNullOrWhiteSpace($env:STUDIO_DB_USER)) {
        $env:STUDIO_DB_USER = (Read-Host 'Local MySQL username (not the website login)').Trim()
    }
    if ([string]::IsNullOrWhiteSpace($env:STUDIO_DB_USER)) { throw 'MySQL username must not be empty.' }
    if ([string]::IsNullOrEmpty($env:STUDIO_DB_PASSWORD)) {
        $studioPassword = Read-Host 'Local MySQL password' -AsSecureString
        try {
            $env:STUDIO_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $studioPassword).Password
        } finally { $studioPassword.Dispose() }
    }
    if ([string]::IsNullOrEmpty($env:STUDIO_DB_PASSWORD)) { throw 'MySQL password must not be empty.' }
}
$studioRoot = Join-Path $PSScriptRoot 'springboot-front(4)'
Push-Location $studioRoot
try {
    if ($InitializeTestAccounts) {
        # These public credentials are for the isolated loopback-only local profile.
        $env:STUDIO_WRITE_FIXTURES = 'true'
        try {
            & .\gradlew.bat test --tests '*StudioIntegrationTest.writeOptionalLocalFixtures' --rerun-tasks --console=plain
            if ($LASTEXITCODE -ne 0) { throw 'Local test-account generation failed.' }
        } finally { Remove-Item Env:STUDIO_WRITE_FIXTURES -ErrorAction SilentlyContinue }
    }
    & .\gradlew.bat bootJar --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
    $studioProfile = if ($Database -eq 'mysql') { 'studio-mysql' } else { 'studio-local' }
    $studioPort = if ($Database -eq 'mysql') { 8084 } else { 8083 }
    $studioArgs = @('-jar','build/libs/interview-backend-0.0.1-SNAPSHOT.jar',"--spring.profiles.active=$studioProfile")
    if ($InitializeTestAccounts) { $studioArgs += '--spring.sql.init.data-locations=file:build/studio-local-users.sql' }
    Write-Host "Local real service: http://127.0.0.1:$studioPort (Ctrl+C to stop). Existing production datasource is not used."
    & java @studioArgs
} finally { Pop-Location }
