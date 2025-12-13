# PowerShell script to build signed AAB for SafeDrive Africa
# Run this script from the project root directory

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SafeDrive Africa - Build Signed AAB" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if keystore.properties exists
$keystoreProps = "keystore.properties"
if (Test-Path $keystoreProps) {
    Write-Host "[OK] Found keystore.properties" -ForegroundColor Green
} else {
    Write-Host "[WARNING] keystore.properties not found!" -ForegroundColor Yellow
    Write-Host "The build will use debug keystore (not suitable for production)" -ForegroundColor Yellow
    Write-Host "To create a release keystore, follow these steps:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "1. Generate keystore:" -ForegroundColor White
    Write-Host "   keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias safedriveafrica" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. Create keystore.properties from template:" -ForegroundColor White
    Write-Host "   Copy-Item keystore.properties.template keystore.properties" -ForegroundColor Gray
    Write-Host ""
    Write-Host "3. Edit keystore.properties with your keystore details" -ForegroundColor White
    Write-Host ""

    $continue = Read-Host "Continue with debug keystore? (y/n)"
    if ($continue -ne "y") {
        Write-Host "Build cancelled." -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "Step 1: Stopping Gradle daemon..." -ForegroundColor Cyan
./gradlew --stop

Write-Host ""
Write-Host "Step 2: Cleaning project..." -ForegroundColor Cyan
./gradlew clean

Write-Host ""
Write-Host "Step 3: Building release AAB..." -ForegroundColor Cyan
./gradlew bundleRelease --stacktrace

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Your signed AAB is located at:" -ForegroundColor Cyan
    Write-Host "app/build/outputs/bundle/release/app-release.aab" -ForegroundColor White
    Write-Host ""

    $aabPath = "app/build/outputs/bundle/release/app-release.aab"
    if (Test-Path $aabPath) {
        $aabSize = (Get-Item $aabPath).Length / 1MB
        Write-Host "AAB Size: $([math]::Round($aabSize, 2)) MB" -ForegroundColor Cyan
    }

    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. Test the AAB using bundletool before uploading" -ForegroundColor White
    Write-Host "2. Upload to Google Play Console" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "BUILD FAILED!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Common issues and solutions:" -ForegroundColor Yellow
    Write-Host "1. OutOfMemoryError: Increase memory in gradle.properties" -ForegroundColor White
    Write-Host "2. R8/ProGuard errors: Check app/proguard-rules.pro" -ForegroundColor White
    Write-Host "3. Signing errors: Verify keystore.properties settings" -ForegroundColor White
    Write-Host ""
    Write-Host "See BUILD_SIGNED_AAB.md for detailed troubleshooting" -ForegroundColor Cyan
    exit 1
}

