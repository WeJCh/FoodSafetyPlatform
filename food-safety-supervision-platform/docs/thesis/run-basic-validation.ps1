param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Iterations = 30,
    [int]$SleepMs = 100,
    [int]$TimeoutSec = 15,
    [string]$OutputDir = "",
    [switch]$UseRecommendedPlan,
    [switch]$ShowRecommendedPlan
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not $OutputDir) {
    $OutputDir = [System.IO.Path]::GetTempPath()
}
if (-not [System.IO.Path]::IsPathRooted($OutputDir)) {
    $OutputDir = Join-Path $PSScriptRoot $OutputDir
}

function Ensure-Directory([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function To-JsonText([object]$Value) {
    return ($Value | ConvertTo-Json -Depth 10 -Compress)
}

function Invoke-ApiRequest {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [hashtable]$Headers,
        [object]$Body,
        [int]$TimeoutSec = 15
    )

    $requestHeaders = @{}
    if ($Headers) {
        foreach ($key in $Headers.Keys) {
            $requestHeaders[$key] = $Headers[$key]
        }
    }

    $bodyText = $null
    if ($null -ne $Body) {
        $bodyText = To-JsonText $Body
        if (-not $requestHeaders.ContainsKey("Content-Type")) {
            $requestHeaders["Content-Type"] = "application/json"
        }
    }

    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-WebRequest -Uri $Url -Method $Method -Headers $requestHeaders -Body $bodyText -TimeoutSec $TimeoutSec -UseBasicParsing
        $watch.Stop()
        $json = $null
        if ($response.Content) {
            $json = $response.Content | ConvertFrom-Json
        }
        $appSuccess = ($json -and $json.code -eq 0)
        $appError = $null
        if (-not $appSuccess -and $json) {
            if ($json.message) {
                $appError = [string]$json.message
            } else {
                $appError = To-JsonText $json
            }
        }
        return [PSCustomObject]@{
            Success    = ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300 -and $appSuccess)
            StatusCode = [int]$response.StatusCode
            ElapsedMs  = [math]::Round($watch.Elapsed.TotalMilliseconds, 2)
            Json       = $json
            Error      = $appError
        }
    } catch {
        $watch.Stop()
        $statusCode = -1
        $errorBody = $null
        if ($_.Exception.Response) {
            try {
                $statusCode = [int]$_.Exception.Response.StatusCode.value__
            } catch {
                $statusCode = -1
            }
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $errorBody = $reader.ReadToEnd()
                $reader.Close()
            } catch {
                $errorBody = $_.Exception.Message
            }
        } else {
            $errorBody = $_.Exception.Message
        }
        return [PSCustomObject]@{
            Success    = $false
            StatusCode = $statusCode
            ElapsedMs  = [math]::Round($watch.Elapsed.TotalMilliseconds, 2)
            Json       = $null
            Error      = $errorBody
        }
    }
}

function Get-Percentile95([double[]]$Values) {
    if (-not $Values -or $Values.Count -eq 0) {
        return 0
    }
    $sorted = $Values | Sort-Object
    $index = [math]::Ceiling($sorted.Count * 0.95) - 1
    if ($index -lt 0) { $index = 0 }
    if ($index -ge $sorted.Count) { $index = $sorted.Count - 1 }
    return [math]::Round([double]$sorted[$index], 2)
}

function Resolve-TestRunSettings {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Definition,
        [Parameter(Mandatory = $true)][int]$DefaultIterations,
        [Parameter(Mandatory = $true)][int]$DefaultSleepMs
    )

    $resolvedIterations = $DefaultIterations
    $resolvedSleepMs = $DefaultSleepMs
    if ($Definition.ContainsKey("RecommendedIterations")) {
        $resolvedIterations = [int]$Definition.RecommendedIterations
    }
    if ($Definition.ContainsKey("RecommendedSleepMs")) {
        $resolvedSleepMs = [int]$Definition.RecommendedSleepMs
    }

    return [PSCustomObject]@{
        Iterations = $resolvedIterations
        SleepMs    = $resolvedSleepMs
    }
}

function Get-Token([string]$BaseUrl, [string]$Username, [string]$Password, [int]$TimeoutSec) {
    $loginResult = Invoke-ApiRequest -Method "POST" -Url "$BaseUrl/api/auth/login" -TimeoutSec $TimeoutSec -Body @{
        username = $Username
        password = $Password
    }
    if (-not $loginResult.Success -or -not $loginResult.Json.data.token) {
        $msg = if ($loginResult.Error) { $loginResult.Error } else { "login failed" }
        throw "Failed to login as $Username. $msg"
    }
    return [string]$loginResult.Json.data.token
}

function Measure-Endpoint {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Definition,
        [Parameter(Mandatory = $true)][int]$Iterations,
        [Parameter(Mandatory = $true)][int]$SleepMs,
        [Parameter(Mandatory = $true)][int]$TimeoutSec,
        [Parameter(Mandatory = $true)][string]$BaseUrl
    )

    $token = $null
    if ($Definition.ContainsKey("Credential")) {
        try {
            $token = Get-Token -BaseUrl $BaseUrl -Username $Definition.Credential.Username -Password $Definition.Credential.Password -TimeoutSec $TimeoutSec
        } catch {
            $records = New-Object System.Collections.Generic.List[object]
            for ($i = 1; $i -le $Iterations; $i++) {
                $records.Add([PSCustomObject]@{
                    Index      = $i
                    Url        = "$BaseUrl$($Definition.DisplayPath)"
                    Success    = $false
                    StatusCode = -1
                    ElapsedMs  = 0
                    Error      = $_.Exception.Message
                })
            }
            return [PSCustomObject]@{
                Name         = $Definition.Name
                ValidateText = $Definition.ValidateText
                Description  = $Definition.Description
                Path         = $Definition.DisplayPath
                Iterations   = $Iterations
                SuccessCount = 0
                SuccessRate  = 0
                AvgMs        = 0
                MaxMs        = 0
                P95Ms        = 0
                Records      = $records
            }
        }
    }

    $records = New-Object System.Collections.Generic.List[object]
    for ($i = 1; $i -le $Iterations; $i++) {
        $headers = @{}
        if ($token) {
            $headers["Authorization"] = "Bearer $token"
        }
        if ($Definition.ContainsKey("Headers")) {
            foreach ($key in $Definition.Headers.Keys) {
                $headers[$key] = $Definition.Headers[$key]
            }
        }

        $body = $null
        if ($Definition.ContainsKey("BodyFactory")) {
            $body = & $Definition.BodyFactory $i
        } elseif ($Definition.ContainsKey("Body")) {
            $body = $Definition.Body
        }

        $path = if ($Definition.ContainsKey("PathFactory")) { & $Definition.PathFactory $i } else { $Definition.Path }
        $url = "$BaseUrl$path"
        $result = Invoke-ApiRequest -Method $Definition.Method -Url $url -Headers $headers -Body $body -TimeoutSec $TimeoutSec

        $records.Add([PSCustomObject]@{
            Index      = $i
            Url        = $url
            Success    = $result.Success
            StatusCode = $result.StatusCode
            ElapsedMs  = $result.ElapsedMs
            Error      = $result.Error
        })

        if ($SleepMs -gt 0 -and $i -lt $Iterations) {
            Start-Sleep -Milliseconds $SleepMs
        }
    }

    $successRecords = @($records | Where-Object { $_.Success })
    $successCount = $successRecords.Count
    [double[]]$elapsedValues = @($records | ForEach-Object { [double]$_.ElapsedMs })
    $average = if ($elapsedValues.Count -gt 0) { [math]::Round((($elapsedValues | Measure-Object -Average).Average), 2) } else { 0 }
    $maximum = if ($elapsedValues.Count -gt 0) { [math]::Round((($elapsedValues | Measure-Object -Maximum).Maximum), 2) } else { 0 }
    $p95 = Get-Percentile95 -Values $elapsedValues

    return [PSCustomObject]@{
        Name         = $Definition.Name
        ValidateText = $Definition.ValidateText
        Description  = $Definition.Description
        Path         = $Definition.DisplayPath
        Iterations   = $Iterations
        SuccessCount = $successCount
        SuccessRate  = [math]::Round(($successCount * 100.0 / $Iterations), 2)
        AvgMs        = $average
        MaxMs        = $maximum
        P95Ms        = $p95
        Records      = $records
    }
}

$tests = @(
    @{
        Name         = "Login API"
        DisplayPath  = "/api/auth/login"
        Method       = "POST"
        Path         = "/api/auth/login"
        RecommendedIterations = 6
        RecommendedSleepMs    = 500
        TheoreticalExpectedSuccesses = 6
        LimitBasis  = "The full script run adds 4 extra logins for token acquisition, so this item is capped at 6 direct login calls to stay within the gateway ip rule of 10 logins per 60 seconds."
        ValidateText = "Call the login endpoint continuously and observe token issuance and identity return."
        Description  = "This endpoint reflects the basic runtime behavior of the unified authentication entry."
        Body         = @{
            username = "enterprise_1"
            password = "123456"
        }
    },
    @{
        Name         = "Enterprise Profile Submit API"
        DisplayPath  = "/api/regulation/enterprise/profile"
        Method       = "POST"
        Path         = "/api/regulation/enterprise/profile"
        RecommendedIterations = 10
        RecommendedSleepMs    = 500
        TheoreticalExpectedSuccesses = 10
        LimitBasis  = "This endpoint has no dedicated rate-limit rule in the current code path. It uses a single login to obtain a token and then reuses that token for repeated writes."
        Credential   = @{
            Username = "enterprise_1"
            Password = "123456"
        }
        ValidateText = "Call the enterprise profile submit endpoint continuously and observe write handling."
        Description  = "This endpoint reflects the write stability of the enterprise-side profile submission flow."
        Body         = @{
            enterpriseName       = "Nanchang Validation Restaurant"
            licenseNo            = "JXCY20260001"
            creditCode           = "91360102MA35ABCD1X"
            legalRepresentative  = "Zhang Enterprise"
            regionId             = 7
            addressDetail        = "128 Bayi Avenue"
            principal            = "Zhang Enterprise"
            principalPhone       = "13800000008"
            attachments          = @()
        }
    },
    @{
        Name         = "Public Complaint Submit API"
        DisplayPath  = "/api/complaints/public"
        Method       = "POST"
        Path         = "/api/complaints/public"
        RecommendedIterations = 5
        RecommendedSleepMs    = 500
        TheoreticalExpectedSuccesses = 5
        LimitBasis  = "The complaint-service limits public complaint submission to 5 requests per user within 600 seconds, so this item is capped at 5 calls under a clean window."
        Credential   = @{
            Username = "public_1"
            Password = "123456"
        }
        ValidateText = "Call the public complaint submit endpoint continuously and observe complaint writes."
        Description  = "This endpoint reflects runtime stability for public-side business writes."
        BodyFactory  = {
            param($Index)
            @{
                anonymous     = $false
                enterpriseId  = 1
                complaintType = "FOOD_SAFETY"
                content       = "Basic runtime validation sample #$Index at $([DateTime]::Now.ToString('yyyyMMddHHmmssfff'))"
                imageUrls     = @()
            }
        }
    },
    @{
        Name         = "Supervision Overview API"
        DisplayPath  = "/api/query/supervision/overview"
        Method       = "GET"
        Path         = "/api/query/supervision/overview"
        RecommendedIterations = 20
        RecommendedSleepMs    = 200
        TheoreticalExpectedSuccesses = 20
        LimitBasis  = "The query-service supervision overview endpoint allows 120 requests per user within 60 seconds, so 20 calls remain well below the configured threshold."
        Credential   = @{
            Username = "admin"
            Password = "123456"
        }
        ValidateText = "Call the supervision overview endpoint continuously and observe aggregate query results."
        Description  = "This endpoint reflects response stability in cross-service aggregate query scenarios."
    },
    @{
        Name         = "Public Sampling Results API"
        DisplayPath  = "/api/regulation-operation/public/sampling/results?page=1&size=10"
        Method       = "GET"
        Path         = "/api/regulation-operation/public/sampling/results?page=1&size=10"
        RecommendedIterations = 20
        RecommendedSleepMs    = 200
        TheoreticalExpectedSuccesses = 20
        LimitBasis  = "No dedicated business rate-limit rule was found for this public sampling query path in the current gateway and service code."
        Credential   = @{
            Username = "public_1"
            Password = "123456"
        }
        ValidateText = "Call the public sampling results endpoint continuously and observe public data reads."
        Description  = "This endpoint reflects the availability of public query access under continuous requests."
    }
)

Ensure-Directory -Path $OutputDir

if ($ShowRecommendedPlan) {
    $tests | ForEach-Object {
        [PSCustomObject]@{
            Name                         = $_.Name
            RecommendedIterations        = $_.RecommendedIterations
            RecommendedSleepMs           = $_.RecommendedSleepMs
            TheoreticalExpectedSuccesses = $_.TheoreticalExpectedSuccesses
            LimitBasis                   = $_.LimitBasis
        }
    } | Format-Table -Wrap
    if (-not $UseRecommendedPlan) {
        return
    }
    Write-Host ""
}

$summary = New-Object System.Collections.Generic.List[object]
foreach ($test in $tests) {
    $runSettings = Resolve-TestRunSettings -Definition $test -DefaultIterations $Iterations -DefaultSleepMs $SleepMs
    Write-Host ("[RUN] {0}" -f $test.Name)
    $result = Measure-Endpoint -Definition $test -Iterations $runSettings.Iterations -SleepMs $runSettings.SleepMs -TimeoutSec $TimeoutSec -BaseUrl $BaseUrl
    $summary.Add($result)
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$jsonPath = Join-Path $OutputDir "basic-validation-$timestamp.json"
$mdPath = Join-Path $OutputDir "basic-validation-$timestamp.md"

$jsonPayload = $summary | Select-Object Name, ValidateText, Description, Path, Iterations, SuccessCount, SuccessRate, AvgMs, MaxMs, P95Ms, Records
$jsonPayload | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$mdLines = New-Object System.Collections.Generic.List[string]
$mdLines.Add("| **Object** | **Validation Content** | **Validation Result** | **Note** |")
$mdLines.Add("| --- | --- | --- | --- |")
foreach ($item in $summary) {
    $resultText = "Called $($item.Path) $($item.Iterations) times, succeeded $($item.SuccessCount) times, success rate $($item.SuccessRate)%, average response time $($item.AvgMs) ms, maximum response time $($item.MaxMs) ms, and P95 $($item.P95Ms) ms."
    $mdLines.Add("| $($item.Name) | $($item.ValidateText) | $resultText | $($item.Description) |")
}
$mdLines.Add("")
$mdLines.Add("Raw result file: $jsonPath")
$mdLines | Set-Content -LiteralPath $mdPath -Encoding UTF8

Write-Host ""
Write-Host "Done."
Write-Host "JSON: $jsonPath"
Write-Host "MD  : $mdPath"
