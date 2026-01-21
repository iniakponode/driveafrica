# Safe Drive Africa - Agent Guide

This file is a working guide for anyone continuing development in this repo.
It captures the layout, key flows, and common traps so future sessions can
orient quickly and make safe changes.

## Project snapshot

- Multi-module Android app built with Jetpack Compose and Hilt.
- Backend repo lives at `C:/Users/r02it21/AndroidStudioProjects/drive_africa_api` \n  (outside this workspace).
- Modules are wired together by the `app` module.
- Room is the single local database (`Sdadb`), with WorkManager handling sync,
  cleanup, and scheduled analysis.

## Module map

- `app/`: Application entry point, Compose navigation, Settings screen,
  WorkManager scheduling in `App.kt`.
- `core/`: Room database, DAOs, repositories, network API services,
  WorkManager workers, shared utils/constants.
- `sensor/`: Sensor capture, motion detection, trip detection services,
  foreground services.
- `driverprofile/`: Driver profile UI, home screen, driving tips flow.
- `nlgengine/`: LLM report generation and report screens.
- `ml/`: ML classifiers, debug viewmodels, and inference use cases.
- `dbda/`: Unsafe behavior analysis utilities.
- `alcoholquestionnaire/`: Daily alcohol questionnaire UI and logic.

## Entry points and navigation

- `app/src/main/java/com/uoa/safedriveafrica/App.kt`: schedules workers on app
  startup (delete data, unsafe analysis, daily tips, upload work).
- `app/src/main/java/com/uoa/safedriveafrica/MainActivity.kt`: Compose host.
- `app/src/main/java/com/uoa/safedriveafrica/presentation/daappnavigation/DAAppNavHost.kt`:
  main NavHost routing across modules.
- `driverprofile/src/main/java/com/uoa/driverprofile/presentation/ui/screens/HomeScreen.kt`:
  home screen for tips and navigation.
- `app/src/main/java/com/uoa/safedriveafrica/presentation/settings/SettingsScreen.kt`:
  settings UI (scrollable).

## Latest session notes (2026-01-20)

- 16 KB alignment: added `check16kAlignment` Gradle task (bundletool via classpath) and confirmed `PAGE_ALIGNMENT_16K`. File: `app/build.gradle.kts`.
- Trip summary normalization: added `trip_summary_behaviour` table with relation, moved per-behaviour counts into a map, updated DAO/repository/mappers, and added migration 50->51. Files: `core/src/main/java/com/uoa/core/database/entities/TripSummaryBehaviourEntity.kt`, `core/src/main/java/com/uoa/core/database/entities/TripSummaryWithBehaviours.kt`, `core/src/main/java/com/uoa/core/database/daos/TripSummaryDao.kt`, `sensor/src/main/java/com/uoa/sensor/repository/TripSummaryRepositoryImpl.kt`, `core/src/main/java/com/uoa/core/model/TripSummary.kt`, `core/src/main/java/com/uoa/core/utils/Mapper.kt`, `core/src/main/java/com/uoa/core/database/Migrations.kt`, `core/src/main/java/com/uoa/core/Sdadb.kt`, `core/src/main/java/com/uoa/core/di/DatabaseModuleProvider.kt`.

## Latest session notes (2026-01-21)

- Trip summary uploads expanded: added upload of normalized trip summary behaviours and trip feature states in `UploadAllDataWorker`. New APIs: `/api/trip_summary_behaviours/batch_create` and `/api/trip_feature_states/batch_create`. Files: `core/src/main/java/com/uoa/core/apiServices/workManager/UploadAllDataWorker.kt`, `core/src/main/java/com/uoa/core/apiServices/models/tripSummaryModels/TripSummaryBehaviourCreate.kt`, `core/src/main/java/com/uoa/core/apiServices/models/tripFeatureModels/TripFeatureStateCreate.kt`, `core/src/main/java/com/uoa/core/apiServices/services/tripSummaryBehaviourApiService/*`, `core/src/main/java/com/uoa/core/apiServices/services/tripFeatureStateApiService/*`, `core/src/main/java/com/uoa/core/utils/LocalToRemoteVMappers.kt`.
- Trip feature state sync: added `sync` flag, DAO methods, repository, Hilt wiring, and migration 52->53. Files: `core/src/main/java/com/uoa/core/database/entities/TripFeatureStateEntity.kt`, `core/src/main/java/com/uoa/core/database/daos/TripFeatureStateDao.kt`, `core/src/main/java/com/uoa/core/database/repository/TripFeatureStateRepository.kt`, `sensor/src/main/java/com/uoa/sensor/repository/TripFeatureStateRepositoryImpl.kt`, `core/src/main/java/com/uoa/core/database/Migrations.kt`, `core/src/main/java/com/uoa/core/Sdadb.kt`, `core/src/main/java/com/uoa/core/di/DatabaseModuleProvider.kt`.
- Settings UI refresh: switches now update immediately and refresh from prefs on resume. File: `app/src/main/java/com/uoa/safedriveafrica/presentation/settings/SettingsScreen.kt`.
- Assets move: large `res/raw` files moved to `driverprofile/src/main/assets` to fix AAPT failures.
- Documentation: added `docs/UNSAFE_BEHAVIOUR_DETECTION.md` and updated `docs/BACKEND_API_SYNC_GUIDE.md` with unsafe behaviour counts and analytics appendix.
- Tests: added speeding tolerance Android test and speed limit parsing unit tests. Files: `core/src/androidTest/java/com/uoa/core/behaviouranalysis/NewUnsafeDrivingBehaviourAnalyserTest.kt`, `core/src/test/java/com/uoa/core/utils/SpeedLimitParsingTest.kt`.
- Latest release APK installed on device `R5CR403HCJD`. Core unit tests and connected tests reported passing in the latest run.

## Release plan status (2026-01-21)

- Completed: release APK build/install, 16 KB alignment check, lint pass, expanded trip uploads, updated docs, and tests passing.
- Pending: generate signed AAB, re-run `:app:check16kAlignment` on the bundle, refresh Play Store screenshots (phone/7-inch/10-inch) for en-NG, fr-CM, sw-TZ, update listing/data-safety details, and final Play Console upload.

## Latest session notes (2026-01-18)

- Login hydration: successful login now inserts/updates the local `driver_profile` row before persisting prefs, so FK-dependent tables can write safely. Files: `driverprofile/src/main/java/com/uoa/driverprofile/presentation/viewmodel/AuthViewModel.kt`.
- Offline login fallback: login checks connectivity and, when offline, allows a cached-session login if a local profile row exists. Strict if a cached password exists (email+password must match); relaxed if no cached password (email must match cached email or local profile). Fresh installs without a local profile are blocked and told to connect. File: `driverprofile/src/main/java/com/uoa/driverprofile/presentation/viewmodel/AuthViewModel.kt`.
- Monitoring gate: auto monitoring now blocks until the local profile row exists. A user-facing dialog allows refresh or disabling auto monitoring; service start also checks the local profile row and stops with a notification if missing. Files: `app/src/main/java/com/uoa/safedriveafrica/permissions/MonitoringGateViewModel.kt`, `app/src/main/java/com/uoa/safedriveafrica/permissions/VehicleMonitoringGate.kt`, `sensor/src/main/java/com/uoa/sensor/services/VehicleMovementServiceUpdate.kt`.
- Overpass resilience: speed-limit queries now use retry/backoff (HTTP 429/5xx + IO) and fall back to cached speed limits when Overpass fails. File: `core/src/main/java/com/uoa/core/utils/UtilFunctions.kt`.
- Migration fix: DB version bumped to 47 and migration adds missing `index_trip_feature_state_tripId`. Files: `core/src/main/java/com/uoa/core/Sdadb.kt`, `core/src/main/java/com/uoa/core/database/Migrations.kt`, `core/src/main/java/com/uoa/core/di/DatabaseModuleProvider.kt`.
- Notification permissions: onboarding info screen (Android 13+) prompts and blocks Continue until notifications are allowed; login/register screen shows a status card but does not block. File: `driverprofile/src/main/java/com/uoa/driverprofile/presentation/ui/screens/DriverProfileCreationScreen.kt`.

## Latest session notes (2026-01-17)

- Onboarding invite codes: added `joinWithCode` call to `/api/driver-join/join-with-code` and wired invite-mode onboarding to call it automatically after registration; normal join-from-settings continues to use `/api/driver/join-fleet` (pending approval path).
- Backend uncertainty: saw 404 when hitting `/api/driver-join/join-with-code`; if backend expects `/driver-join/join-with-code`, adjust the base path. Because of the 404, the app fell back to `/api/driver/join-fleet` and showed “pending approval” even during invite onboarding.
- Registration flow: offline-first now checks connectivity, defers to WorkManager if offline, saves token on successful online registration, and gates fleet-status calls on having a token + network.
- Navigation: invite onboarding should go straight home after join-with-code; join-from-settings remains for existing users without a fleet.
- Repo state: Changes committed and pushed to `main` in commit `b3c6c0c` (“Fix onboarding invite code flow”). Untracked files (logs, artifacts, .idea/deviceManager.xml) intentionally not committed.
- Next steps: confirm the correct join-with-code endpoint with backend, rebuild/install, and re-test invite onboarding. If 404 persists, flip the path to `/driver-join/join-with-code` and retest.
## Database and migrations

- Room database: `core/src/main/java/com/uoa/core/Sdadb.kt` (version 53).
- Migrations live in `core/src/main/java/com/uoa/core/database/Migrations.kt`.
- Schema snapshots are in `core/schemas/`.
- If a migration error occurs, compare expected vs. actual indices/columns in
  `Migrations.kt` and regenerate schema snapshots when needed.

## WorkManager and sync

- `core/src/main/java/com/uoa/core/apiServices/workManager/UploadAllDataWorker.kt`:
  bulk sync and upload pipeline.
- `core/src/main/java/com/uoa/core/apiServices/workManager/DeleteDataWorker.kt`:
  cleanup worker (runs frequently).
- `core/src/main/java/com/uoa/core/apiServices/workManager/UnsafeDrivingAnalysisWorker.kt`:
  background analysis.
- `driverprofile/src/main/java/com/uoa/driverprofile/worker/DailyDrivingTipWorker.kt`:
  daily tip generation.
- Manual sync uses `core/src/main/java/com/uoa/core/apiServices/workManager/WorkSchedular.kt`
  (`enqueueImmediateUploadWork`).

## Trip detection and monitoring

- Foreground service: `sensor/src/main/java/com/uoa/sensor/services/VehicleMovementServiceUpdate.kt`.
- Motion logic: `sensor/src/main/java/com/uoa/sensor/motion/DrivingStateManager.kt`.
  Sensitivity is driven by preference key `TRIP_DETECTION_SENSITIVITY` in
  `core/src/main/java/com/uoa/core/utils/Constants.kt`.
- Auto trip gating: `app/src/main/java/com/uoa/safedriveafrica/permissions/VehicleMonitoringGate.kt`.

## Settings and debug actions

- Settings is the single home for:
  - Notifications status and permission prompt.
  - Auto trip detection toggle.
  - Trip detection sensitivity.
  - Metered uploads toggle.
  - Manual sync (full-width button).
  - Debug "Run Trip ML Check" (debug builds only).
- Home screen no longer shows manual sync or debug buttons.

## Driving tips and LLM output

- Tip generation: `driverprofile/src/main/java/com/uoa/driverprofile/utils/DrivingTipGenerationUtils.kt`.
  Parsing is tolerant of code fences and non-strict JSON.
- ViewModels:
  - `driverprofile/.../DrivingTipsViewModel.kt`
  - `nlgengine/.../NLGEngineViewModel.kt`
- LLM keys must be supplied via `local.properties`:
  - `GEMINI_API_KEY=...`
  - `CHAT_GPT_API_KEY=...`

## ML debug hook

- Debug viewmodel: `ml/src/main/java/com/uoa/ml/presentation/viewmodel/TripClassificationDebugViewModel.kt`.
- Used in Settings debug section to run the latest trip classification.

## Build and run

- JDK 17 required by AGP.
- Typical commands:
  - `./gradlew assembleDebug`
  - `./gradlew test`
- Android Studio uses SDK 34 and Compose compiler configured in
  `build.gradle.kts` and `gradle/libs.versions.toml`.

## Useful docs

- `README.md` for overall architecture and data flow summary.
- `ARCHITECTURAL_PLAN.md` for the staged data lifecycle and storage strategy.
- `docs/USER_MANUAL.md` and `docs/REPRODUCTION_GUIDE.md` for usage and testing.
- `docs/UI_Facelift_Spec.md` for UI direction.

## Common pitfalls

- Sync and delete workers run on tight intervals. Be careful when changing data
  lifecycle logic to avoid starving on-device features.
- When adding UI actions that trigger background work, prefer WorkManager
  helpers in `core/apiServices/workManager`.
- If Settings grows, keep the screen scrollable to avoid clipped actions.

## Fix log

- 2026-01-13: Trip upload failed with HTTP 500 when the trip already existed on
  the backend (duplicate primary key). Added a fallback check to treat the trip
  as synced if `GET /api/trips/{id}` succeeds after a create failure.
  Files: `core/src/main/java/com/uoa/core/apiServices/workManager/UploadAllDataWorker.kt`.
- 2026-01-13: If a trip already exists after a create failure, perform an update
  and only mark the trip as synced on update success.
  Files: `core/src/main/java/com/uoa/core/apiServices/workManager/UploadAllDataWorker.kt`.
- 2026-01-13: Vehicle detection monitor UI cleanup for release: renamed state
  display to DRIVING on RECORDING, removed motion analysis/threshold cards,
  highlighted km/h for dashboard comparison, and made the trip card visible when
  state is RECORDING even if collection status misses a toggle.
  Files: `sensor/src/main/java/com/uoa/sensor/presentation/ui/screens/VehicleDetectionMonitorScreen.kt`.
- 2026-01-14: Trip create/update payloads now include both camelCase and
  snake_case keys for start/end date/time and alcohol fields; backend
  TripCreatePayload accepts snake_case aliases to handle mixed API
  expectations. Files: `core/src/main/java/com/uoa/core/apiServices/models/tripModels/TripCreate.kt`,
  `drive_africa_api/safedrive/schemas/trip.py`.
- 2026-01-14: Fixed Pydantic v2 migration error in trip schema by adding `skip_on_failure=True` to the root validator so alembic env loads cleanly. Files: `drive_africa_api/safedrive/schemas/trip.py`.
- 2026-01-14: Trip create endpoint now returns `TripResponse.model_validate` so response keys follow schema aliases (camelCase) consistently. Files: `C:/Users/r02it21/AndroidStudioProjects/drive_africa_api/safedrive/api/v1/endpoints/trip.py`.
- 2026-01-14: Backend trip schema now accepts camelCase payload fields (`startDate`, `endDate`, `startTime`, `endTime`) and treats start/end times as epoch milliseconds to align app uploads. Files: `C:/Users/r02it21/AndroidStudioProjects/drive_africa_api/safedrive/schemas/trip.py`.
- 2026-01-14: Rolled back backend trip schema/timezone changes by reverting commits in the backend repo (`drive_africa_api`).
- 2026-01-14: Trip responses now include formatted local time strings (`startTimeLocal`, `endTimeLocal`) using client-provided timezone metadata; trip payloads now send timezone id/offset, and backend stores these fields. Files: `drive_africa_api/safedrive/schemas/trip.py`, `drive_africa_api/safedrive/api/v1/endpoints/trip.py`, `drive_africa_api/safedrive/models/trip.py`, `drive_africa_api/safedrive/alembic/versions/0003_add_trip_timezone_fields.py`, `core/src/main/java/com/uoa/core/apiServices/models/tripModels/TripCreate.kt`, `core/src/main/java/com/uoa/core/apiServices/models/tripModels/TipResponse.kt`, `core/src/main/java/com/uoa/core/utils/LocalToRemoteVMappers.kt`, `core/src/main/java/com/uoa/core/apiServices/workManager/UploadAllDataWorker.kt`, `sensor/src/main/java/com/uoa/sensor/services/VehicleMovementServiceUpdate.kt`, `sensor/src/main/java/com/uoa/sensor/presentation/viewModel/TripViewModel.kt`.
- 2026-01-14: Backend trip API now normalizes start/end time fields to epoch millis, accepts ISO/string time inputs, and migrates trip time columns to BIGINT with a backfill. Files: `drive_africa_api/safedrive/schemas/trip.py`, `drive_africa_api/safedrive/api/v1/endpoints/trip.py`, `drive_africa_api/safedrive/models/trip.py`, `drive_africa_api/safedrive/alembic/versions/0002_fix_trip_time_fields.py`.
- 2026-01-14: Trip API parsing now tolerates ISO-8601 strings for start/end time fields to avoid NumberFormatException when the backend returns a date string. Files: `core/src/main/java/com/uoa/core/apiServices/FlexibleLongAdapter.kt`, `core/src/main/java/com/uoa/core/apiServices/models/tripModels/TipResponse.kt`.
- 2026-01-13: Added debug-only logging of trip create/update JSON payloads to
  verify start/end times and alcohol fields before upload.
  Files: `core/src/main/java/com/uoa/core/apiServices/services/tripApiService/TripApiRepository.kt`.
- 2026-01-14: Consolidated trip and upload notifications into low-importance
  status updates (trip started/ended, per-entity upload progress and completion),
  removed per-batch upload notifications, and made foreground notifications silent.
  Files: `core/src/main/java/com/uoa/core/notifications/VehicleNotificationManager.kt`,
  `core/src/main/java/com/uoa/core/apiServices/workManager/UploadAllDataWorker.kt`,
  `sensor/src/main/java/com/uoa/sensor/services/DataCollectionService.kt`,
  `sensor/src/main/java/com/uoa/sensor/services/VehicleMovementServiceUpdate.kt`,
  `sensor/src/main/java/com/uoa/sensor/presentation/viewModel/TripViewModel.kt`,
  `core/src/test/java/com/uoa/core/apiServices/workManager/UploadAllDataWorkerTest.kt`.
- 2026-01-14: Unified background monitoring and trip collection to a single
  foreground notification ID, removed extra trip start/end pop-ups, and updated
  the foreground message text to explicitly state when location is in use.
  Files: `core/src/main/java/com/uoa/core/notifications/VehicleNotificationManager.kt`,
  `sensor/src/main/java/com/uoa/sensor/services/VehicleMovementServiceUpdate.kt`,
  `sensor/src/main/java/com/uoa/sensor/services/DataCollectionService.kt`.
- 2026-01-14: Avoided misleading “grant location permission” alerts when
  foreground start fails but location permission is already granted; now only
  show the permission notification if the permission check actually fails.
  Files: `sensor/src/main/java/com/uoa/sensor/services/VehicleMovementServiceUpdate.kt`,
  `sensor/src/main/java/com/uoa/sensor/services/DataCollectionService.kt`.
- 2026-01-14: Reduced short false-stop trips by requiring low speed and a
  minimum recording duration before treating high variance as a walking exit.
  Files: `sensor/src/main/java/com/uoa/sensor/motion/DrivingStateManager.kt`.
- 2026-01-14: Added swerving to trip summaries so summaries can power tips
  after raw data is purged; added schema/migration updates and summary counting.
  Files: `core/src/main/java/com/uoa/core/database/entities/TripSummaryEntity.kt`,
  `core/src/main/java/com/uoa/core/model/TripSummary.kt`,
  `core/src/main/java/com/uoa/core/utils/TripSummaryUtils.kt`,
  `core/src/main/java/com/uoa/core/utils/Mapper.kt`,
  `core/src/main/java/com/uoa/core/database/Migrations.kt`,
  `core/src/main/java/com/uoa/core/Sdadb.kt`,
  `core/src/main/java/com/uoa/core/di/DatabaseModuleProvider.kt`.
- 2026-01-14: Driving tips now fall back to trip summaries (last 30 days) when
  unsafe behaviour rows are unavailable; summary events are converted into
  synthetic unsafe behaviour records so tip generation still works after raw
  data cleanup. Files: `driverprofile/src/main/java/com/uoa/driverprofile/domain/usecase/Usecases.kt`,
  `driverprofile/src/main/java/com/uoa/driverprofile/presentation/di/ProvideDrivingTipsModule.kt`,
  `driverprofile/src/main/java/com/uoa/driverprofile/presentation/viewmodel/DrivingTipsViewModel.kt`,
  `driverprofile/src/main/java/com/uoa/driverprofile/worker/DailyDrivingTipWorker.kt`.
- 2026-01-14: Report statistics now fill aggregation and alcohol-influence
  fields even when unsafe behaviour rows are missing by falling back to trip
  summaries; report generation waits for data load and no longer requires
  unsafe behaviours to display a report. Files:
  `core/src/main/java/com/uoa/core/utils/UtilFunctions.kt`,
  `nlgengine/src/main/java/com/uoa/nlgengine/presentation/viewmodel/NLGEngineViewModel.kt`,
  `nlgengine/src/main/java/com/uoa/nlgengine/presentation/ui/ReportScreen.kt`.
- 2026-01-14: Report statistics cache now invalidates when new trip/behaviour
  data arrives: the view model deletes range-based cache rows when unprocessed
  behaviours exist or trip summaries increase, and last-trip stats refresh when
  the cached `tripId` differs from the actual last trip. Files:
  `nlgengine/src/main/java/com/uoa/nlgengine/presentation/viewmodel/NLGEngineViewModel.kt`,
  `core/src/main/java/com/uoa/core/database/repository/ReportStatisticsRepository.kt`,
  `core/src/main/java/com/uoa/core/database/daos/ReportStatisticsDao.kt`,
  `sensor/src/main/java/com/uoa/sensor/repository/TripSummaryRepositoryImpl.kt`,
  `core/src/main/java/com/uoa/core/database/repository/TripSummaryRepository.kt`,
  `core/src/main/java/com/uoa/core/database/daos/TripSummaryDao.kt`.

## Stress Testing Log

1. Attempted to run `.\scripts\testlab\run_testlab.ps1` (Firebase Test Lab Robo + instrumentation) but the script depends on the `gcloud` CLI, which is not installed/configured in this environment, so it errored before the stress tests could start.
2. Tried `adb devices` to satisfy the local Monkey stress test (Option 2) requested in this session, but `adb` is not available on the PATH, so the Monkey test could not be executed locally. Installing Android SDK/platform-tools would unblock that step.
3. Confirmed `adb` exists at `C:\Users\r02it21\OneDrive - University of Aberdeen\Shared Folder\PHD RESEARCH\CODE\platform-tools\adb.exe`; the tool works if addressed by its full path, but the directory is not currently on `PATH`, so commands without the full path continue to fail.
4. Ran `adb shell monkey -p com.uoa.safedriveafrica --pct-syskeys 0 --throttle 150 --monitor-native-crashes -v 10000` via the explicit executable, but the command timed out before completion; aborting it let us restart with a smaller workload.
5. After connectivity was restored, executed `adb shell monkey -p com.uoa.safedriveafrica --pct-syskeys 0 --throttle 50 --monitor-native-crashes -v 1500` successfully; the Monkey test finished without crashes and logged 1500 events (flips failed due to permission, expected on this device).
6. Ran another intensive Monkey session with `--pct-touch 50 --pct-motion 25 --pct-nav 10 --pct-majornav 5 --pct-appswitch 5 --pct-trackball 2 --pct-rotation 3 --throttle 20` for 15,000 events; it completed cleanly, injected rotations, trackball, and motion events, and only dropped a handful of pointers/keys—no crashes observed.
7. Extended coverage further with `--pct-touch 40 --pct-motion 30 --pct-nav 10 --pct-majornav 8 --pct-appswitch 7 --pct-trackball 3 --pct-rotation 2 --throttle 15` for 20,000 events; the run completed successfully after ~4m, injected nav/app-switch/trackball/rotation events, and dropped only a few pointers/keys (no crashes logged).
8. Captured `adb logcat -d` from device `R5CR403HCJD` into `artifacts/monkey-logcat.log` for later crash correlation; keep the file with the artifacts or share it if further analysis is needed.
