param(
  [string]$BaseUrl = 'http://127.0.0.1:18081/admin',
  [string]$Password = '123456'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Invoke-Api {
  param(
    [Parameter(Mandatory = $true)][string]$Method,
    [Parameter(Mandatory = $true)][string]$Url,
    [string]$Token,
    $Body
  )

  $headers = @{}
  if ($Token) { $headers['X-Token'] = $Token }

  try {
    if ($PSBoundParameters.ContainsKey('Body') -and $null -ne $Body) {
      $json = $Body | ConvertTo-Json -Depth 10
      return Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers -ContentType 'application/json' -Body $json
    }
    return Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers
  } catch {
    throw "HTTP error at $Method $Url : $($_.Exception.Message)"
  }
}

function Login {
  param([string]$Phone)
  $res = Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/login" -Body @{ phone = $Phone; password = $Password }
  if ($res.code -ne 200 -or -not $res.data.token) {
    throw "Login failed for ${Phone}: $($res | ConvertTo-Json -Depth 6)"
  }
  return [string]$res.data.token
}

function Login-ExpectFail {
  param([string]$Phone, [int]$ExpectedCode = 401)
  $res = Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/login" -Body @{ phone = $Phone; password = $Password }
  return Assert-Code -Name "login forbidden ${Phone}" -Expected $ExpectedCode -Response $res
}

function Assert-Code {
  param(
    [Parameter(Mandatory = $true)]$Response,
    [Parameter(Mandatory = $true)][int]$Expected,
    [Parameter(Mandatory = $true)][string]$Name
  )

  $actual = [int]$Response.code
  $ok = $actual -eq $Expected
  [PSCustomObject]@{
    Name = $Name
    Expected = $Expected
    Actual = $actual
    Pass = $ok
    Message = [string]$Response.message
  }
}

Write-Host "== RBAC smoke test =="
Write-Host "BaseUrl: $BaseUrl"

$tokenAdmin = Login -Phone '13000000001'
$tokenOps   = Login -Phone '13000000002'

$cases = @()

$cases += Login-ExpectFail -Phone '13000000003'

$cases += Assert-Code -Name 'admin list users' -Expected 200 -Response (Invoke-Api -Method 'GET' -Url "$BaseUrl/interview/admin/users?pageNo=1&pageSize=1" -Token $tokenAdmin)
$cases += Assert-Code -Name 'ops list users forbidden' -Expected 40003 -Response (Invoke-Api -Method 'GET' -Url "$BaseUrl/interview/admin/users?pageNo=1&pageSize=1" -Token $tokenOps)

$cases += Assert-Code -Name 'ops own resumes imported' -Expected 200 -Response (Invoke-Api -Method 'GET' -Url "$BaseUrl/interview/admin/users/1002/resumes/imported?pageNo=1&pageSize=1" -Token $tokenOps)
$cases += Assert-Code -Name 'ops cross-user resumes forbidden' -Expected 40003 -Response (Invoke-Api -Method 'GET' -Url "$BaseUrl/interview/admin/users/1001/resumes/imported?pageNo=1&pageSize=1" -Token $tokenOps)

$cases += Assert-Code -Name 'ops precheck md5 forbidden' -Expected 40003 -Response (Invoke-Api -Method 'POST' -Url "$BaseUrl/interview/admin/users/1002/resumes/import-pdf/precheck/md5/batch" -Token $tokenOps -Body @{ md5List = @('abc') })
$cases += Assert-Code -Name 'ops update resume forbidden' -Expected 40003 -Response (Invoke-Api -Method 'PUT' -Url "$BaseUrl/interview/admin/users/1002/resumes/1" -Token $tokenOps -Body @{ title = 'x'; content = '' })

$cases | Format-Table -AutoSize

$failed = @($cases | Where-Object { -not $_.Pass })
if ($failed.Count -gt 0) {
  Write-Host "`nFAILED: $($failed.Count) case(s)" -ForegroundColor Red
  exit 1
}

Write-Host "`nALL PASSED" -ForegroundColor Green
