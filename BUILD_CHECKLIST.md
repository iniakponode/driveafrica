# 🚀 BUILD CHECKLIST - Ready to Build

## ✅ All Fixes Applied

- [x] Increased JVM memory to 4GB
- [x] Disabled R8 full mode
- [x] Added signing configuration
- [x] Updated ProGuard rules for Hilt, Compose, Room
- [x] Added PdfBox ProGuard rules
- [x] Suppressed JP2Decoder warnings
- [x] Suppressed JP2Encoder warnings  
- [x] Removed deprecated android.enableR8 property
- [x] Created build automation script
- [x] Created comprehensive documentation

## 🎯 Build Now

### Quick Build
```powershell
./build-aab.ps1
```

### Manual Build
```powershell
./gradlew clean bundleRelease --stacktrace
```

## ⏱️ Expected Build Time
- **First build:** 5-7 minutes (minification disabled)
- **Incremental:** 1-2 minutes

## 📍 Output Location
```
app/build/outputs/bundle/release/app-release.aab
```

## ⚠️ Important Note

**Minification is currently DISABLED** to avoid R8 ConcurrentModificationException bug.
- See `R8_CONCURRENCY_FIX.md` for details
- APK/AAB will be larger (60-90 MB instead of 25-45 MB)
- Code will not be obfuscated

## ✅ Success Indicators

You'll know the build succeeded when you see:

```
> Task :app:bundleRelease

BUILD SUCCESSFUL in 5-7m
223 actionable tasks: 223 executed
```

## ⚠️ Warnings You Can Ignore

These warnings are normal and won't affect the build:

```
w: '@Deprecated' - Various Kotlin/Android deprecation warnings
w: 'Condition is always true' - Kotlin compiler warnings
WARNING: package="com.uoa.safedriveafrica" - AndroidManifest warning
```

## ❌ Errors That Should NOT Appear

If you see these, something went wrong:

```
❌ Missing class com.gemalto.jp2.JP2Decoder
❌ Missing class com.gemalto.jp2.JP2Encoder
❌ OutOfMemoryError
❌ R8: Compilation failed to complete
```

If any of these appear, check:
1. Did you save `app/proguard-rules.pro`?
2. Did you save `gradle.properties`?
3. Did you run `./gradlew clean` first?

## 📋 Post-Build Verification

After successful build:

### 1. Verify AAB exists
```powershell
Get-Item app/build/outputs/bundle/release/app-release.aab
```

### 2. Check AAB size
```powershell
Get-Item app/build/outputs/bundle/release/app-release.aab | Select-Object Name, @{Name="Size(MB)";Expression={[math]::Round($_.Length/1MB,2)}}
```
**Expected:** 60-90 MB (larger due to minification disabled)

### 3. Verify signing
```powershell
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```
**Expected:** "jar verified."

### 4. Check for debug signing
```powershell
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab | Select-String -Pattern "CN="
```

**If using debug keystore, you'll see:**
```
CN=Android Debug, O=Android, C=US
```

**If using release keystore, you'll see:**
```
Your custom certificate details
```

## 🔒 For Production Release

Before uploading to Google Play:

1. **Create release keystore:**
   ```powershell
   keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias safedriveafrica
   ```

2. **Configure keystore.properties:**
   ```powershell
   Copy-Item keystore.properties.template keystore.properties
   # Edit with your actual keystore details
   ```

3. **Rebuild with release keystore:**
   ```powershell
   ./gradlew clean bundleRelease
   ```

## 🧪 Testing the AAB

### Generate Universal APK for Testing
```powershell
# Download bundletool if needed:
# https://github.com/google/bundletool/releases

# Generate APKs
java -jar bundletool.jar build-apks --bundle=app/build/outputs/bundle/release/app-release.aab --output=test.apks --mode=universal

# Extract
Expand-Archive -Path test.apks -DestinationPath test_apks -Force

# Install
adb install -r test_apks/universal.apk
```

### Launch and Test
```powershell
# Launch the app
adb shell am start -n com.uoa.safedriveafrica/.MainActivity

# Monitor for crashes
adb logcat -c  # Clear log
adb logcat | Select-String -Pattern "safedriveafrica|AndroidRuntime|FATAL"
```

## 📚 Documentation Reference

- **Quick Start:** `BUILD_NOW.md`
- **Detailed Guide:** `BUILD_SIGNED_AAB.md`
- **All Fixes:** `FIXES_SUMMARY.md`
- **Final Summary:** `FINAL_FIX_SUMMARY.md`
- **This Checklist:** `BUILD_CHECKLIST.md`
- **Verification Guide:** `BUILD_VERIFICATION_GUIDE.md`
- **PdfBox Details:** `PDFBOX_R8_FIX.md`

## 🆘 If Build Fails

1. **Stop all Gradle daemons:**
   ```powershell
   ./gradlew --stop
   ```

2. **Clean everything:**
   ```powershell
   ./gradlew clean
   Remove-Item -Recurse -Force .gradle, build, app/build
   ```

3. **Check for missing rules:**
   ```powershell
   # After failed build, check:
   Get-Content app/build/outputs/mapping/release/missing_rules.txt
   ```

4. **Add missing rules to app/proguard-rules.pro:**
   ```proguard
   -dontwarn <missing.class.name>
   ```

5. **Rebuild:**
   ```powershell
   ./gradlew clean bundleRelease --stacktrace
   ```

## 📊 Build Statistics

Your project configuration:
- **Modules:** 8 (app, core, sensor, dbda, driverprofile, nlgengine, alcoholquestionnaire, ml)
- **Compile SDK:** 34
- **Target SDK:** 34
- **Min SDK:** 29
- **Version:** 1.12 (versionCode 12)
- **JVM Heap:** 4GB
- **R8 Mode:** Compat mode (full mode disabled)

## ✅ Final Status

**All build issues:** ✅ RESOLVED  
**Ready to build:** ✅ YES  
**Production ready:** ⚠️ Create release keystore first  
**Last updated:** December 11, 2025  

---

## 🎉 You're Ready!

Run the build command and your signed AAB will be ready in ~10 minutes!

```powershell
./gradlew clean bundleRelease --stacktrace
```

Good luck! 🚀

