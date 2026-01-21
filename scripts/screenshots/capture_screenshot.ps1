param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("phone", "7-inch", "10-inch")]
    [string]$Device,

    [Parameter(Mandatory = $true)]
    [ValidateSet("en-NG", "en-CM", "fr-CM", "sw-TZ")]
    [string]$Locale,

    [Parameter(Mandatory = $true)]
    [string]$Name,

    [string]$OutputRoot = "play-store\\assets\\screenshots",

    [string]$Serial,

    [string]$AdbPath
)

$ErrorActionPreference = "Stop"

$adbCommand = $null
if ($AdbPath) {
    if (Test-Path $AdbPath) {
        $adbCommand = $AdbPath
    } else {
        Write-Error "adb not found at $AdbPath"
        exit 1
    }
} else {
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($adb) {
        $adbCommand = $adb.Path
    } else {
        $fallbackAdb = "C:\\Users\\r02it21\\OneDrive - University of Aberdeen\\Shared Folder\\PHD RESEARCH\\CODE\\platform-tools\\adb.exe"
        if (Test-Path $fallbackAdb) {
            $adbCommand = $fallbackAdb
        } else {
            Write-Error "adb not found. Install Android platform-tools, set PATH, or pass -AdbPath."
            exit 1
        }
    }
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\\..")
$targetDir = Join-Path $repoRoot (Join-Path $OutputRoot (Join-Path $Device $Locale))
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

$deviceListRaw = & $adbCommand devices
$deviceListText = $deviceListRaw -join "`n"
$deviceList = $deviceListText -split "\r?\n"
$deviceEntries = $deviceList | Where-Object {
    $_ -and ($_ -notmatch "^List of devices attached")
} | ForEach-Object {
    if ($_ -match "^(\S+)\s+(\S+)$") {
        [pscustomobject]@{
            Id = $matches[1]
            Status = $matches[2]
        }
    }
}

$readyDevices = @($deviceEntries | Where-Object { $_.Status -eq "device" } | ForEach-Object { $_.Id })

if (-not $Serial) {
    if (-not $readyDevices -or $readyDevices.Count -eq 0) {
        Write-Host "adb devices output:" -ForegroundColor Yellow
        $deviceListRaw | ForEach-Object { Write-Host "  $_" }
        if ($deviceEntries) {
            $statusSummary = ($deviceEntries | ForEach-Object { "$($_.Id) ($($_.Status))" }) -join ", "
            Write-Error "adb found devices but none are ready: $statusSummary"
        } else {
            Write-Error "No adb devices detected. Connect a device or start an emulator."
        }
        exit 1
    }
    if ($readyDevices.Count -gt 1) {
        Write-Error "Multiple adb devices detected. Re-run with -Serial <device-id>."
        exit 1
    }
    $Serial = $readyDevices[0]
}

$existing = Get-ChildItem -Path $targetDir -Filter "*.png" -ErrorAction SilentlyContinue
$nextIndex = 1
if ($existing) {
    $numbers = $existing | ForEach-Object {
        if ($_.BaseName -match "^(\d+)") { [int]$matches[1] }
    }
    if ($numbers) {
        $max = ($numbers | Measure-Object -Maximum).Maximum
        if ($max) { $nextIndex = $max + 1 }
    }
}

$indexLabel = $nextIndex.ToString("00")
$cleanName = $Name -replace "[^a-zA-Z0-9_-]", "_"
$fileName = "$indexLabel`_$cleanName.png"
$outPath = Join-Path $targetDir $fileName
$remotePath = "/sdcard/$fileName"
$adbArgs = @("-s", $Serial)

& $adbCommand @adbArgs shell screencap -p $remotePath
& $adbCommand @adbArgs pull $remotePath $outPath | Out-Null
& $adbCommand @adbArgs shell rm $remotePath

Write-Host "Saved $outPath"
