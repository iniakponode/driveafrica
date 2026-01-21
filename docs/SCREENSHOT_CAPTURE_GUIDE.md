# Screenshot Capture Guide

This guide covers capturing Play Store screenshots for phone, 7-inch tablet, and 10-inch tablet, in these locales:
- en-NG
- en-CM
- fr-CM
- sw-TZ

The target folders are already created under:
`play-store/assets/screenshots/<device>/<locale>/`

## Build and install

Use a release build if possible so the UI matches the Play Store bundle.

```powershell
.\gradlew :app:assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```

If you need to use debug for faster iteration, confirm no debug-only UI is visible.

## Capture flow

For each locale:
1) Change the device language to the locale.
2) Launch the app and walk through the screens.
3) Capture screenshots using the script.

Capture command:
```powershell
.\scripts\screenshots\capture_screenshot.ps1 -Device phone -Locale en-NG -Name onboarding
```

If you have multiple adb devices connected:
```powershell
.\scripts\screenshots\capture_screenshot.ps1 -Device phone -Locale en-NG -Name onboarding -Serial emulator-5554
```

## Recommended screen list (archive everything)

Use names like these for consistency:
- splash
- onboarding_intro
- onboarding_data_promise
- notification_permission
- registration_choice
- registration_email
- home_dashboard
- reports_overview
- reports_filter
- driving_tip_detail
- join_fleet
- sensor_control
- vehicle_monitor
- alcohol_questionnaire
- settings
- help

## Play Store selection

Google Play accepts 2 to 8 phone screenshots. Pick the best 8 from the full set. Tablet screenshots are optional but can be uploaded if they represent the tablet UI well.
