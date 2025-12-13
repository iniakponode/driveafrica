# Building Signed AAB for SafeDrive Africa

## Prerequisites
- Android Studio installed
- JDK 11 or higher
- Gradle 8.7 or higher

## Common Build Issues Fixed

### 1. JVM Crashes During R8/ProGuard Processing
**Solution Applied:**
- Increased JVM heap memory from 2GB to 4GB in `gradle.properties`
- Disabled R8 full mode to prevent crashes
- Added G1GC garbage collector configuration
- Updated ProGuard rules to be more specific

### 2. Missing Signing Configuration
**Solution Applied:**
- Added signingConfigs block in `app/build.gradle.kts`
- Created support for keystore.properties file
- Falls back to debug keystore if release keystore is not configured

### 3. ProGuard Rules Too Aggressive
**Solution Applied:**
- Updated proguard-rules.pro with specific rules for:
  - Hilt dependency injection
  - Jetpack Compose
  - Room Database
  - Kotlin Serialization
  - Data classes and model classes

## Steps to Build Signed AAB

### Option 1: Using Release Keystore (Recommended for Production)

#### Step 1: Create a Keystore (if you don't have one)
```powershell
keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias safedriveafrica
```

Follow the prompts to set:
- Keystore password
- Key password
- Your name, organization, etc.

#### Step 2: Create keystore.properties
Copy the template and fill in your details:
```powershell
cp keystore.properties.template keystore.properties
```

Edit `keystore.properties` with your actual values:
```properties
storeFile=C:/Users/YourUsername/path/to/release-keystore.jks
storePassword=your_actual_keystore_password
keyAlias=safedriveafrica
keyPassword=your_actual_key_password
```

**Important:** Never commit `keystore.properties` or your `.jks` file to version control!

#### Step 3: Clean and Build
```powershell
# Stop any running Gradle daemons
./gradlew --stop

# Clean the project
./gradlew clean

# Build the release AAB
./gradlew bundleRelease
```

The signed AAB will be at: `app/build/outputs/bundle/release/app-release.aab`

### Option 2: Using Debug Keystore (For Testing Only)

If you don't have a release keystore, the build will automatically use the debug keystore.

```powershell
./gradlew --stop
./gradlew clean
./gradlew bundleRelease
```

**Note:** Debug-signed AAB files cannot be uploaded to Google Play Store for production releases.

## Building APK Instead of AAB

```powershell
# For release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

## Troubleshooting

### Build Fails with OutOfMemoryError
If you still get memory errors, increase JVM memory further in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx6144m -XX:MaxMetaspaceSize=1536m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -XX:+UseG1GC
```

### R8 Compilation Errors
If R8 still crashes, you can temporarily disable minification in `app/build.gradle.kts`:
```kotlin
release {
    isMinifyEnabled = false
    isShrinkResources = false
    signingConfig = signingConfigs.getByName("release")
    proguardFiles(...)
}
```

### ClassNotFoundException in Release Build
Add the missing class to ProGuard rules in `app/proguard-rules.pro`:
```
-keep class com.yourpackage.YourClass { *; }
```

### Clean Build Directory
If you encounter persistent issues:
```powershell
# Delete build directories
Remove-Item -Recurse -Force .gradle, build, app/build, core/build, dbda/build, sensor/build

# Rebuild
./gradlew clean
./gradlew bundleRelease
```

## Verification

After building, verify your AAB:
```powershell
# Install bundletool (if not already installed)
# Download from: https://github.com/google/bundletool/releases

# Generate APKs from AAB for testing
bundletool build-apks --bundle=app/build/outputs/bundle/release/app-release.aab --output=app-release.apks --mode=universal

# Extract and install
bundletool extract-apks --apks=app-release.apks --output-dir=output
adb install output/universal.apk
```

## Changes Made to Fix Build Issues

1. **gradle.properties**
   - Increased JVM heap from 2GB to 4GB
   - Added G1GC garbage collector
   - Disabled R8 full mode
   - Added heap dump on OOM

2. **app/build.gradle.kts**
   - Added signingConfigs block
   - Configured release signing with keystore.properties support
   - Added fallback to debug keystore

3. **app/proguard-rules.pro**
   - Added Hilt/Dagger rules
   - Added Compose rules
   - Added Room Database rules
   - Added Kotlin Serialization rules
   - Made rules more specific to prevent R8 crashes

4. **.gitignore**
   - Added keystore.properties
   - Added *.keystore and *.jks

## Security Notes

- **Never commit:**
  - `keystore.properties`
  - `*.jks` or `*.keystore` files
  - Any file containing passwords

- **Keep secure backups of:**
  - Your release keystore file
  - Keystore password
  - Key alias and password

- If you lose your release keystore, you cannot update your app on Google Play Store!

## Additional Resources

- [Android App Bundle Documentation](https://developer.android.com/guide/app-bundle)
- [Sign Your App](https://developer.android.com/studio/publish/app-signing)
- [Configure Build Variants](https://developer.android.com/build/build-variants)

