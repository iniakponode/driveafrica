# Recent Fixes and Play Store Prep Notes

This document summarizes the key fixes, build steps, and tooling updates applied during the recent stabilization and Play Store preparation work. It is intended as a quick reference for future AI agents and developers.

## Scope
- 16 KB page size compatibility checks.
- Release lint/build fixes and signing validation.
- Registration payload + fleet status parsing fixes.
- R8/ProGuard protection for Retrofit/Gson models.
- Fleet status UI logic corrections.
- Screenshot capture automation and locale scaffolding.

## 1) 16 KB page size compatibility
- Verified alignment using the custom Gradle task:
  - `.\gradlew :app:bundleRelease :app:check16kAlignment`
  - Expected output contains `"alignment": "PAGE_ALIGNMENT_16K"`.
- Notes:
  - Alignment is affected by bundletool + APK zip alignment.
  - When upgrading AGP/NDK, ensure `.so` alignment remains 16 KB.

## 2) Lint + release build readiness
- `.\gradlew :app:lintRelease` now succeeds.
- Release build verified via `.\gradlew :app:assembleRelease --no-daemon`.
- If lint blocks release in future, address `NewApi` issues or add targeted `@TargetApi` annotations (avoid blanket suppression).

## 3) Registration payload + auth deserialization fixes
Problem: Request payload was obfuscated in release, causing missing fields (`driverProfileId`, `email`, `password`) and 422 errors.

Fix:
- Added `@Keep` and `@field:SerializedName` in:
  - `core/src/main/java/com/uoa/core/apiServices/models/auth/AuthModels.kt`
- Also improved auth error handling to avoid dumping raw server errors to UI.

## 4) Fleet status parsing + UI logic
Problem: Fleet status API returned `"assigned"` but UI showed "Unknown" or "not part of a fleet".

Fixes:
- Updated fleet status models with `@Keep` + `@SerializedName` (see AuthModels above).
- Added more robust UI logic:
  - Treat assigned if status contains `"assigned"` OR any of `fleet`, `vehicleGroup`, `vehicle` is non-null.
  - Treat pending if status contains `"pending"` or `pendingRequest` exists.
  - Trim status before comparisons.
  - Files updated:
    - `driverprofile/src/main/java/com/uoa/driverprofile/presentation/ui/screens/HomeScreen.kt`
    - `app/src/main/java/com/uoa/safedriveafrica/presentation/settings/SettingsScreen.kt`

## 5) R8/ProGuard protections for API models
Problem: Obfuscation could break Gson reflection for other endpoints.

Fixes:
- Added keep rules in `app/proguard-rules.pro`:
  - `-keep class com.uoa.core.apiServices.models.** { *; }`
  - `-keep class com.uoa.core.network.model.** { *; }`
  - Keep Gson `@SerializedName` members.
- Added `@Keep` + `@field:SerializedName` to network models:
  - `core/src/main/java/com/uoa/core/network/model/DrivingBehaviourResponse.kt`
  - `core/src/main/java/com/uoa/core/network/model/DrivingBehaviourData.kt`
  - `core/src/main/java/com/uoa/core/network/model/CauseData.kt`
  - `core/src/main/java/com/uoa/core/network/model/GeminiData.kt`
  - `core/src/main/java/com/uoa/core/network/model/GeminiResponse.kt`
  - `core/src/main/java/com/uoa/core/network/model/Gemini/GeminiRequest.kt`
  - `core/src/main/java/com/uoa/core/network/model/chatGPT/RequestBody.kt`
  - `core/src/main/java/com/uoa/core/network/model/chatGPT/ChatGPTResponse.kt`
  - `core/src/main/java/com/uoa/core/network/model/chatGPT/OSMResponse.kt`
  - `core/src/main/java/com/uoa/core/network/model/nominatim/ReverseGeocodeResponse.kt`
  - `core/src/main/java/com/uoa/core/network/model/nominatim/OverpassResponse.kt`

## 6) Release APK packaging issue (missing dex)
Symptom:
- Install failed with `INSTALL_FAILED_INVALID_APK: Package ... code is missing`.

Diagnosis:
- `app-release.apk` had no `classes*.dex`.

Resolution:
- Rebuild release after cleaning stale outputs.
- Validate dex presence:
  - `Add-Type -AssemblyName System.IO.Compression.FileSystem; ...`
  - Confirm `classes.dex`, `classes2.dex`, etc exist.

## 7) Screenshot capture automation + locales
- Added screenshot capture script:
  - `scripts/screenshots/capture_screenshot.ps1`
  - Documentation: `docs/SCREENSHOT_CAPTURE_GUIDE.md`
- Locale folders created for Play Store screenshots:
  - `play-store/assets/screenshots/{phone,7-inch,10-inch}/{en-NG,en-CM,fr-CM,sw-TZ}`
- Added locale resource files:
  - `values-en-NG`, `values-en-CM`, `values-fr-CM`, `values-sw-TZ` across app modules.

## 8) Install command used for device testing
ADB path used during testing:
```
"C:\Users\r02it21\OneDrive - University of Aberdeen\Shared Folder\PHD RESEARCH\CODE\platform-tools\adb.exe"
```

Install command:
```
& "C:\Users\r02it21\OneDrive - University of Aberdeen\Shared Folder\PHD RESEARCH\CODE\platform-tools\adb.exe" -s R5CR403HCJD install -r "C:\Users\r02it21\AndroidStudioProjects\safedriveafrica\app\build\outputs\apk\release\app-release.apk"
```

## 9) Quick verification checklist
- `.\gradlew :app:lintRelease`
- `.\gradlew :app:assembleRelease --no-daemon`
- Ensure `app/build/outputs/apk/release/app-release.apk` contains `classes*.dex`.
- Install and verify:
  - Registration succeeds (no 422 missing fields).
  - Fleet status shows assigned fleet name.
  - Home + Settings screens reflect correct status.

## 10) Next recommended checks
- Run `.\gradlew :app:check16kAlignment` after any build tool upgrades.
- Spot-check external network APIs (ChatGPT/Gemini/Nominatim/Overpass) in release builds.
- Update Play Store screenshots using the capture script + locale variants.
