param(
    [string]$ProjectId,
    [string]$ResultsBucket,
    [int]$RoboTimeoutMinutes = 15,
    [int]$InstrumentationTimeoutMinutes = 20
)

$ErrorActionPreference = "Stop"

$gcloud = Get-Command gcloud -ErrorAction SilentlyContinue
if (-not $gcloud) {
    Write-Error "gcloud CLI not found. Install it and run: gcloud auth login"
    exit 1
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\\..")
Push-Location $repoRoot

try {
    Write-Host "Building debug app + androidTest APKs..."
    .\\gradlew :app:assembleDebug :app:assembleDebugAndroidTest

    $appApk = Join-Path $repoRoot "app\\build\\outputs\\apk\\debug\\app-debug.apk"
    $testApk = Join-Path $repoRoot "app\\build\\outputs\\apk\\androidTest\\debug\\app-debug-androidTest.apk"

    if (-not (Test-Path $appApk)) {
        throw "Missing app APK at $appApk"
    }
    if (-not (Test-Path $testApk)) {
        throw "Missing test APK at $testApk"
    }

    $projectArg = @()
    if ($ProjectId) {
        $projectArg = @("--project=$ProjectId")
    }

    $bucketArg = @()
    if ($ResultsBucket) {
        $bucketArg = @("--results-bucket=$ResultsBucket")
    }

    Write-Host "Running Robo test..."
    & gcloud firebase test android run `
        @projectArg `
        @bucketArg `
        --type robo `
        --app $appApk `
        --timeout ("{0}m" -f $RoboTimeoutMinutes) `
        --device model=Pixel6,version=34,locale=en,orientation=portrait `
        --device model=Pixel5,version=30,locale=en,orientation=portrait

    Write-Host "Running instrumentation tests..."
    & gcloud firebase test android run `
        @projectArg `
        @bucketArg `
        --type instrumentation `
        --app $appApk `
        --test $testApk `
        --timeout ("{0}m" -f $InstrumentationTimeoutMinutes) `
        --device model=Pixel6,version=34,locale=en,orientation=portrait `
        --device model=Pixel5,version=30,locale=en,orientation=portrait
}
finally {
    Pop-Location
}
