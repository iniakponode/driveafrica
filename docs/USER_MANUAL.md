# Safe Drive Africa User Manual

## 1. Overview
Safe Drive Africa is a driving safety and research app that records trip data, collects daily alcohol questionnaire responses, and generates personalized driving feedback and reports. The app works both online and offline, and syncs data when a connection is available.

## 2. Getting started
### 2.1 Install the app
Install the latest APK or Play Store build on a device running Android 8.0 (API 26) or higher.

### 2.2 First launch flow
On first launch you will see:
1) Welcome and disclaimer screens.
2) An onboarding info screen that explains why permissions are needed.
3) The registration or login screen.

### 2.3 Permissions (first launch)
The app requires these permissions to function correctly:
- Location (fine/coarse) for trip tracking and road data.
- Activity recognition to detect driving activity.
- Foreground service to keep trip recording active.
- Notifications (Android 13+) so the app can show trip status, upload progress, and safety alerts.

On Android 13+ the onboarding info screen will prompt for notifications and block Continue until permission is granted. On older Android versions, notifications are enabled in system settings and the app will guide you there if needed.

## 3. Account creation and login
### 3.1 Register a new account
1) Open the registration tab on the entry screen.
2) Choose a fleet option:
   - Independent
   - Have fleet (optional invite code)
3) Enter your email and password.
4) Tap Register.

If the device is offline during registration, the app will save your credentials and continue when a connection is available. First-time registration still requires an online connection to complete the server-side account setup.

### 3.2 Login
1) Open the login tab.
2) Enter your email and password.
3) Tap Log in.

### 3.3 Offline login behavior
Offline login is supported only after the device has successfully logged in online at least once.
- If a cached password exists, offline login requires the exact cached email and password.
- If no cached password exists, offline login will work only if a local driver profile row exists for that email.
- Fresh installs (no local profile and no cached credentials) require an internet connection for first login.

## 4. Home screen
The home screen is your hub for daily activity:
- Latest driving tips (AI-generated).
- Quick actions for:
  - Daily alcohol questionnaire
  - Trip recording
  - Reports

Use the bottom navigation bar to switch between Home, Reports, and Record Trip.

## 5. Trip recording and monitoring
### 5.1 Manual trip recording
1) Open Record Trip.
2) Grant requested permissions if prompted.
3) Tap Start Trip when driving.
4) Tap End Trip when finished.

### 5.2 Auto trip detection
Auto detection can start and end trips in the background based on movement:
- Enable Auto Trip Detection in Settings.
- The foreground notification indicates monitoring status.

If auto monitoring is enabled but your local driver profile is missing, the app will pause monitoring and prompt you to log in again to hydrate the local profile row.

### 5.3 Vehicle detection monitor
The monitoring screen shows:
- Current driving state (Idle, Verifying, Driving, Potential Stop).
- Speed readings and speed limit (if available).
- Data collection status.

## 6. Reports and driving tips
### 6.1 Reports
1) Open Reports.
2) Choose a time range (last trip, today, week, custom).
3) Tap Generate Report.

Reports summarize safety events and driving behavior using AI-generated text.

### 6.2 Driving tips
Driving tips appear on the home screen and are based on recent driving behavior. Tap a tip to view details.

## 7. Daily alcohol questionnaire
The alcohol questionnaire is available once per day:
1) Open it from the home reminder or quick action.
2) Complete the questions.
3) Submit to save the response.

The responses are used to improve trip safety analysis and reporting.

## 8. Fleet membership
If you belong to a fleet:
- Join from onboarding or via Settings.
- If you use an invite code, your fleet assignment can be immediate.
- If not, your request may be pending approval.

In Settings you can:
- View fleet assignment status.
- Refresh fleet status.
- Cancel a pending join request.

## 9. Settings
Open Settings from the top bar.

Key settings:
- Notifications status and permission shortcuts.
- Driver Profile ID (copyable for support and deletion requests).
- Auto Trip Detection toggle.
- Trip detection sensitivity (High, Balanced, Low).
- Allow Metered Uploads (mobile data).
- Manual Sync.
- Debug tools (debug builds only).

## 10. Sync and offline behavior
- The app stores trips and questionnaire data locally.
- Background workers upload data when connected (and when allowed by your metered data setting).
- You can trigger an immediate upload via Settings > Manual Sync.

## 11. Data deletion and support
To request account or data deletion:
1) Open Settings and copy your Driver Profile ID.
2) Contact support at support@safedriveafrica.com with your Driver Profile ID and deletion request.

## 12. Troubleshooting
### Trips not recording
- Confirm location and activity permissions are granted.
- Ensure Auto Trip Detection is enabled (if you want automatic trips).
- Check the foreground notification is running.
- If monitoring is paused due to missing local profile, log out and log in again.

### Speed limits show as unknown
- The app queries external map services; results can be unavailable offline or during service outages.
- Retry with a stable connection.

### Cannot log in offline
- First-time login requires internet.
- Offline login only works if the device has cached credentials and a local driver profile row.

## 13. Contact
Support email: support@safedriveafrica.com
