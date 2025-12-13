# 🔍 LOGCAT MONITORING SCRIPT
# Use this PowerShell script to monitor vehicle detection logs in real-time

Write-Host "🚀 Starting Vehicle Detection Log Monitor..." -ForegroundColor Green
Write-Host "📱 Make sure your device is connected via USB" -ForegroundColor Yellow
Write-Host ""

# Check if ADB is available
try {
    $adbCheck = adb devices 2>&1
    if ($adbCheck -match "device$") {
        Write-Host "✅ Device connected!" -ForegroundColor Green
    } else {
        Write-Host "❌ No device found. Please connect your device." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ ADB not found. Please install Android SDK Platform Tools." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  VEHICLE DETECTION REAL-TIME LOG MONITOR" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "📊 Monitoring these components:" -ForegroundColor Yellow
Write-Host "   • DrivingStateManager - Motion detection FSM"
Write-Host "   • VehicleDetectionVM - UI state management"
Write-Host "   • HardwareModule - Sensor integration"
Write-Host "   • MainActivity - App lifecycle"
Write-Host ""
Write-Host "🎯 Key Events to Watch:" -ForegroundColor Yellow
Write-Host "   • State transitions (IDLE → VERIFYING → RECORDING)"
Write-Host "   • GPS speed vs Dashboard comparison"
Write-Host "   • Variance calculations (vehicle vs walking)"
Write-Host "   • Trip ID generation"
Write-Host "   • GPS timeout and fallback"
Write-Host ""
Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Red
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Clear existing logs
adb logcat -c

# Start monitoring with color-coded output
adb logcat -s `
    "DrivingStateManager:V" `
    "VehicleDetectionVM:V" `
    "HardwareModule:V" `
    "MainActivity:V" `
    "SensorDataColStateRepository:V" `
    | ForEach-Object {
        $line = $_

        # Color-code by severity
        if ($line -match "ERROR|FAILED|❌") {
            Write-Host $line -ForegroundColor Red
        }
        elseif ($line -match "WARNING|⚠️") {
            Write-Host $line -ForegroundColor Yellow
        }
        elseif ($line -match "State Transition|✅|🚗|CONFIRMED") {
            Write-Host $line -ForegroundColor Green
        }
        elseif ($line -match "GPS UPDATE|Speed \(mph\)|📍") {
            Write-Host $line -ForegroundColor Cyan
        }
        elseif ($line -match "Trip started|Trip ended|🛑") {
            Write-Host $line -ForegroundColor Magenta
        }
        elseif ($line -match "VERIFYING|RECORDING|IDLE|POTENTIAL_STOP") {
            Write-Host $line -ForegroundColor Yellow
        }
        else {
            Write-Host $line
        }
    }

