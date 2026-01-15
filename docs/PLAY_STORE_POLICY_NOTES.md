# Play Store Permission & Policy Notes

Safe Drive Africa ships with the sensor module enabled: GPS, motion sensors, and activity-recognition are combined with a foreground service so the app can detect trips, flag unsafe driving, and keep the research backend aligned with every driver profile. Because these capabilities require sensitive permissions, the sections below describe how each permission is used, what users see inside the app, and the text you can re-use for the store listing and the privacy policy page.

## Sensitive permissions and how they are justified

- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: needed while the sensor module is recording so the app can tell when a driver leaves/arrives, measure trip distances, and detect speeding or unsafe maneuvers with GPS accuracy. Location is only requested while monitoring is enabled; as soon as you stop the service or close the Sensor screen, the foreground service and GPS listeners are shut down.
- `android.permission.ACTIVITY_RECOGNITION` (and the legacy `com.google.android.gms.permission.ACTIVITY_RECOGNITION`): lets the app distinguish between driving vs. walking/idle states so we automatically begin/end trips, pause data collection when the vehicle stops, and mute false positives. That data is combined with location to create accurate trip summaries for the research backend.
- `HIGH_SAMPLING_RATE_SENSORS`: required to read accelerometer/gyroscope data at the rate the VehicleMovementServiceUpdate needs for the unsafe driving detector. The service streams this data while driving, then immediately discards the raw samples after computing the safety metrics that feed the reports.
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` + `FOREGROUND_SERVICE_SPECIAL_USE` + `FOREGROUND_SERVICE_DATA_SYNC`: required so we can keep sensors alive while the phone screen is off, actively inform the driver that monitoring is running, and upload data even when the app is backgrounded. These are only active when the driver starts the Sensor Control screen and explicitly grants permissions.
- `POST_NOTIFICATIONS`: keeps the user informed about active monitoring, trip-state changes, and backend assignment alerts even when the app is backgrounded. Notifications also carry action buttons for stopping the service without opening the app.
- `RECEIVE_BOOT_COMPLETED`: allows the app to restart the VehicleMovementServiceUpdate when the device reboots, ensuring a consistent recording experience after a driver leaves the vehicle; the service only restarts if the driver has previously granted permissions and enabled monitoring.
- `ACCESS_NETWORK_STATE` / `INTERNET`: used to detect when a network connection exists so we can upload the local, anonymized trip packets (partially kept on-device) and keep profiles synced with the Safe Drive Africa backend.

> **Data safety guidance**: When you complete the Google Play data safety form, mark that the app collects Location data, Physical activity, and Device or other IDs/sensor data. In addition, mention Notifications because the permission is used to inform the participant. There is no advertising or data sharing with third parties beyond the Safe Drive Africa research backend.

## In-app messaging

- The onboarding flow now shows `SensorPermissionCard` (see `driverprofile/DriverProfileCreationScreen.kt`) with the `onboarding_sensor_permission_note` string so drivers know location, activity, and notification permissions are part of the sensor workflow before creating a profile.
- The Sensor Control screen (`SensorControlScreenUpdate.kt`) uses the `sensor_permission_explanation` string to explain that GPS, activity recognition, foreground service, and notifications keep the sensor module running only when the driver consents, so the permission dialog is never a mystery.
- These texts can be mirrored in the privacy policy and Play Store description so users see the same explanation everywhere.

## Privacy policy copy suggestion

Use the following copy on the privacy policy page (or edit it to match your tone):

> Safe Drive Africa collects location, activity recognition, and accelerometer/gyroscope data only while you are driving and explicitly allow the sensor module to run. A foreground service keeps the sensors alive so trips can be detected, unsafe driving alerts can be generated, and driver reports stay aligned with the research backend. Notifications keep you informed about what the service is doing and allow you to stop monitoring at any time. The same trip data is stored locally until the device is online, then it uploads securely to our research servers. We never share the raw sensor feed with advertisers or external analytics providers.

Include a sentence that links to `https://datahub.safedriveafrica.com/privacy` so the policy page is discoverable from the Play listing.

## Play Store asset guidance

- `play-store/listing/en-US/full-description.txt`: mention that trips are detected via phone sensors (GPS, activity recognition, accelerometer/gyroscope), explain why the sensor module requires a foreground service, and link to the privacy policy before the closing paragraph (the next update already includes this wording).
- `play-store/listing/en-US/release-notes.txt`: note the onboarding and sensor-screen copy additions so reviewers know you now clearly explain why the permissions are needed.
- `play-store/listing/en-US/short-description.txt`: highlight the sensor-powered, GPS-backed trip tracking so the store entry reflects the foreground service requirement.

## Research compliance checklist

1. Request the permissions only when the user navigates to the Sensor Control screen; they are not needed for the core driver onboarding/profile flows, so there is no overreach.
2. Stop the VehicleMovementServiceUpdate when the driver taps the “Stop Monitoring” button; the foreground service and sensors are paused until the next explicit request.
3. Keep the privacy policy URL in both the onboarding screens and the Play listing so reviewers can see the promise about minimal data handling.
4. When you upload the AAB, the release notes should still mention the permission transparency changes.

If you need additional copy for the Play Console, use the same explanation from the privacy policy paragraph above. Keep the focus on research, safety, and explicit consent.
