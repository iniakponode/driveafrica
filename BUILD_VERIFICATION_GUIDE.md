# SafeDrive Africa - Build Verification & Testing Guide

## Pre-Build Checklist

Before attempting to build, verify:

- [ ] Java/JDK installed (check: `java -version`)
- [ ] Android SDK installed
- [ ] Gradle wrapper present (`gradlew` and `gradlew.bat`)
- [ ] All gradle.properties changes applied
- [ ] app/build.gradle.kts has signing configuration
- [ ] app/proguard-rules.pro updated

## Build Process Testing

### Step 1: Clean Build Environment

```powershell
# Stop any running Gradle daemons
./gradlew --stop

# Clean the project
./gradlew clean

# Verify clean was successful
Get-ChildItem -Recurse -Directory -Filter "build" | Remove-Item -Recurse -Force
```

### Step 2: Build Debug (Quick Test)

```powershell
# Build debug APK first to verify project compiles
./gradlew assembleDebug --stacktrace

# Expected output: app/build/outputs/apk/debug/app-debug.apk
```

**What to look for:**
- ✅ No compilation errors
- ✅ No dependency resolution failures
- ✅ Hilt/Dagger code generation succeeds
- ✅ Compose compiler runs successfully

### Step 3: Build Release AAB

```powershell
# Build release AAB
./gradlew bundleRelease --stacktrace

# Expected output: app/build/outputs/bundle/release/app-release.aab
```

**What to look for:**
- ✅ R8/ProGuard runs without crashes
- ✅ No OutOfMemoryError
- ✅ Build completes in reasonable time (~3-10 minutes)
- ✅ AAB file is created

### Step 4: Verify Signing

```powershell
# Check the AAB is signed
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab

# Expected output: "jar verified."
```

**Expected Results:**

With debug keystore:
```
- Signed by "CN=Android Debug, O=Android, C=US"
- Certificate fingerprint (SHA-256): [hash]
```

With release keystore:
```
- Signed by your configured certificate details
- Certificate fingerprint (SHA-256): [your hash]
```

## Build Output Analysis

### Check Build Logs

Look for these indicators:

#### ✅ Success Indicators
```
> Task :app:bundleRelease
BUILD SUCCESSFUL in Xm Ys
```

#### ⚠️ Warnings to Review
```
w: some deprecation warnings (these are OK)
Note: Some input files use or override a deprecated API
```

#### ❌ Errors to Fix
```
FAILURE: Build failed with an exception
OutOfMemoryError
R8: Compilation failed
```

### Memory Usage Check

Monitor during build:
```powershell
# In another terminal, watch Java processes
Get-Process java | Select-Object Name, @{Name="Memory(MB)";Expression={[math]::Round($_.WS/1MB,2)}}
```

**Expected:** Peak memory usage ~3-4GB (shouldn't exceed 4GB)

## AAB Verification

### 1. Check File Size

```powershell
Get-Item app/build/outputs/bundle/release/app-release.aab | Select-Object Name, @{Name="Size(MB)";Expression={[math]::Round($_.Length/1MB,2)}}
```

**Expected size:** 20-50 MB (depending on assets)

**If size is suspicious:**
- < 5 MB: Likely missing resources
- > 100 MB: May contain unnecessary assets

### 2. Inspect AAB Contents

```powershell
# Install bundletool if not already installed
# Download from: https://github.com/google/bundletool/releases

# View AAB contents
java -jar bundletool.jar dump manifest --bundle=app/build/outputs/bundle/release/app-release.aab

# Expected: Should show AndroidManifest.xml content
```

### 3. Generate Universal APK

```powershell
# Create a universal APK from the AAB for testing
java -jar bundletool.jar build-apks --bundle=app/build/outputs/bundle/release/app-release.aab --output=test.apks --mode=universal

# Extract the APK
Expand-Archive -Path test.apks -DestinationPath test_apks -Force

# The universal APK will be in: test_apks/universal.apk
```

### 4. Install and Test on Device

```powershell
# Connect Android device with USB debugging enabled

# Install the universal APK
adb install -r test_apks/universal.apk

# Launch the app
adb shell am start -n com.uoa.safedriveafrica/.MainActivity

# Monitor for crashes
adb logcat -c  # Clear log
adb logcat | Select-String -Pattern "safedriveafrica|AndroidRuntime"
```

## Common Issues and Solutions

### Issue 1: OutOfMemoryError

**Symptoms:**
```
> java.lang.OutOfMemoryError: Java heap space
```

**Solution:**
```properties
# In gradle.properties, increase memory:
org.gradle.jvmargs=-Xmx6144m -XX:MaxMetaspaceSize=1536m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -XX:+UseG1GC
```

### Issue 2: R8 Crashes

**Symptoms:**
```
EXCEPTION_ACCESS_VIOLATION in R8
```

**Solution:**
```properties
# In gradle.properties:
android.enableR8.fullMode=false

# Or temporarily disable minification in app/build.gradle.kts:
isMinifyEnabled = false
isShrinkResources = false
```

### Issue 3: Missing Classes at Runtime

**Symptoms:**
- App crashes with `ClassNotFoundException`
- App works in debug but crashes in release

**Solution:**
Add ProGuard keep rules in `app/proguard-rules.pro`:
```proguard
-keep class com.uoa.safedriveafrica.path.to.MissingClass { *; }
```

### Issue 4: Signing Errors

**Symptoms:**
```
Execution failed for task ':app:validateSigningRelease'
```

**Solution:**
1. Check keystore.properties exists and has correct paths
2. Verify keystore password is correct
3. Ensure keystore file exists at specified path

### Issue 5: PdfBox Missing Classes (R8 Error)

**Symptoms:**
```
Missing classes detected while running R8
Missing class com.gemalto.jp2.JP2Decoder
```

**Cause:**
PdfBox library references optional image decoders (JPEG2000) that aren't included in the Android version.

**Solution:**
Already fixed in `app/proguard-rules.pro`:
```proguard
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn javax.imageio.**
-dontwarn java.awt.**
```

**Note:** These are optional dependencies for advanced image formats. Basic PDF operations work without them.

## Performance Benchmarks

### Build Time Expectations

| Build Type | First Build | Incremental | Clean Build |
|------------|-------------|-------------|-------------|
| Debug APK | 3-5 min | 30-60s | 2-3 min |
| Release AAB | 5-10 min | 1-2 min | 4-8 min |

**Note:** Times vary based on:
- CPU speed (20 cores available in your system)
- Disk speed (SSD recommended)
- Internet speed (for dependency downloads)

### Expected Build Output Size

| Build Type | Approximate Size |
|------------|-----------------|
| Debug APK | 40-70 MB |
| Release AAB | 25-45 MB |
| Universal APK from AAB | 35-60 MB |

## Success Criteria

### Build is Successful When:

1. **Compilation**
   - [ ] All Kotlin files compile without errors
   - [ ] All Java files compile without errors
   - [ ] Compose compiler succeeds
   - [ ] Hilt/Dagger code generation succeeds

2. **ProGuard/R8**
   - [ ] No crashes during R8 processing
   - [ ] Reasonable number of warnings (< 100)
   - [ ] Build completes successfully

3. **Signing**
   - [ ] AAB is properly signed
   - [ ] `jarsigner -verify` passes
   - [ ] Certificate info is correct

4. **Functionality**
   - [ ] App installs on device
   - [ ] App launches without crashes
   - [ ] Main features work correctly
   - [ ] No ProGuard-related runtime crashes

5. **Size**
   - [ ] AAB size is reasonable (20-50 MB)
   - [ ] No excessive bloat from kept classes

## Final Verification Checklist

Before uploading to Google Play Console:

### Technical Verification
- [ ] Build completes without errors
- [ ] No JVM crashes during build
- [ ] R8/ProGuard completes successfully
- [ ] AAB is properly signed with correct certificate
- [ ] AAB size is reasonable
- [ ] Universal APK can be generated from AAB

### Functional Testing
- [ ] App installs successfully
- [ ] App launches without crashes
- [ ] Login/Authentication works
- [ ] Main features functional:
  - [ ] Trip recording
  - [ ] Report generation
  - [ ] Alcohol questionnaire
  - [ ] Driver profile
  - [ ] Sensor data collection
- [ ] No ProGuard-related crashes
- [ ] Database operations work
- [ ] Network requests succeed
- [ ] Notifications work
- [ ] Permissions are granted properly

### Version Information
- [ ] versionCode incremented (currently: 12)
- [ ] versionName correct (currently: 1.12)
- [ ] Release notes prepared
- [ ] Changelog updated

### Security
- [ ] Signed with release keystore (not debug)
- [ ] keystore.properties not in version control
- [ ] Keystore file backed up securely
- [ ] Keystore passwords documented securely
- [ ] No API keys hardcoded in source

### Google Play Requirements
- [ ] Minimum SDK version set (currently: 29)
- [ ] Target SDK version current (currently: 34)
- [ ] App bundle format (.aab)
- [ ] 64-bit native libraries included if applicable
- [ ] Privacy policy URL ready
- [ ] Store listing prepared

## Troubleshooting Commands

### View Build Configuration
```powershell
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

### Check Gradle Properties
```powershell
./gradlew properties | Select-String -Pattern "jvm|sdk|version"
```

### View Task Details
```powershell
./gradlew bundleRelease --scan
# Opens browser with detailed build scan
```

### Clean Everything
```powershell
./gradlew clean cleanBuildCache --no-daemon
Remove-Item -Recurse -Force .gradle, .kotlin, build, app/build
```

## Getting Help

If build issues persist:

1. **Check logs:** Review the full stack trace
2. **Read docs:** See BUILD_SIGNED_AAB.md and FIXES_SUMMARY.md
3. **Search error:** Google the specific error message
4. **Check dependencies:** Ensure all libraries are compatible
5. **Community:** Stack Overflow, Android issue tracker

## Success!

If all checks pass, you're ready to upload to Google Play Console! 🎉

---

**Document Version:** 1.0
**Last Updated:** December 10, 2025
**Project:** SafeDrive Africa

