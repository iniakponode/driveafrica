# Play Console Data Safety Checklist (Draft)

Use this as a draft when completing the Google Play "Data safety" form. It is based on
`docs/PLAY_STORE_POLICY_NOTES.md` and the current app behavior.

## Privacy policy
- URL: https://datahub.safedriveafrica.com/privacy

## Data collection summary (expected)
- Data shared with third parties: No.
- Data sold: No.
- Data processed for advertising: No.

## Data types collected (mark in Play Console)
- Location: Precise and approximate.
  - Purpose: App functionality (trip detection, safety analysis, reporting).
  - Processing: Collected while monitoring is enabled; uploaded to Safe Drive Africa backend.
- Physical activity: Activity recognition (driving vs walking).
  - Purpose: App functionality (auto start/stop trips, reduce false positives).
  - Processing: Collected only while monitoring is enabled.
- Personal info: Email address (account registration/login).
  - Purpose: Account management and authentication.
  - Processing: Stored in the driver profile and backend.
- App activity: Trip history and safety events.
  - Purpose: App functionality (reports, tips, fleet insights).
  - Processing: Stored locally until sync and in backend.
- Device or other IDs: Driver profile ID (UUID).
  - Purpose: Account linkage and data deletion requests.
  - Processing: Stored in app preferences and backend.

## Data types not collected (expected)
- Name, phone number, contacts, photos, videos, audio, files, financial info, health data.

## Data handling (mark in Play Console)
- Data encrypted in transit: Yes (HTTPS endpoints in release builds).
- Data encrypted at rest: Confirm (verify Android device storage encryption + any backend DB encryption).
- Data deletion request: Supported via driver profile ID (confirm support process).

## Permissions declaration (Play Console)
- ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION: Trip detection, distance/speed calculation, unsafe behavior analysis.
- ACTIVITY_RECOGNITION: Driving vs walking/idle detection; auto trip start/stop.
- HIGH_SAMPLING_RATE_SENSORS: Accelerometer/gyroscope sampling for unsafe behavior detection.
- FOREGROUND_SERVICE + FOREGROUND_SERVICE_LOCATION + FOREGROUND_SERVICE_DATA_SYNC + FOREGROUND_SERVICE_SPECIAL_USE:
  Foreground monitoring and sync while driving.
- POST_NOTIFICATIONS: Inform users about monitoring state, trip events, and uploads.
- RECEIVE_BOOT_COMPLETED: Restart monitoring only when previously enabled by user.
- ACCESS_NETWORK_STATE / INTERNET: Upload trip packets and sync with backend.

## Notes / items to confirm
- Confirm whether any analytics/crash reporting SDKs are enabled in release.
- Confirm whether driver profile ID is considered "Device or other IDs" for Play Console.
- Confirm data retention and deletion process in backend (for the Data safety form).
