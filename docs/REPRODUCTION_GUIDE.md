# Safe Drive Africa – System Reproduction Guide

This document expands on the repository README and explains every moving part of the Safe Drive Africa Android application so that another developer can rebuild the system from scratch with confidence. It is organised chronologically—from machine setup through deployment—and cross-references the source code where each responsibility lives.

## 1. Project overview

Safe Drive Africa is a multi-module Android application created for PhD research into AI-supported road-safety coaching across West Africa. It continuously captures motion and location signals, analyses unsafe driving behaviour, and generates culturally aware feedback reports through LLM integrations. The root README summarises the project purpose, high-level modules, MVVM guidelines, and component interactions.【F:README.md†L1-L164】

## 2. Repository layout

The Gradle settings declare eight feature modules in addition to the `app` entry point. Each module is isolated and wired together through Hilt dependency injection.【F:settings.gradle.kts†L1-L30】 The README reiterates the responsibilities of every module:

| Module | Responsibility |
| ------ | -------------- |
| `app` | Hosts the Compose UI, navigation graph, WorkManager scheduling, and ties modules together.【F:README.md†L7-L58】 |
| `core` | Provides Room database (`Sdadb`), Retrofit services, behaviour analysis, shared repositories, utilities, and WorkManager tasks.【F:README.md†L8-L55】【F:core/src/main/java/com/uoa/core/Sdadb.kt†L1-L76】 |
| `sensor` | Talks to Android sensors, buffers data, classifies motion, and provides data-collection UI flows.【F:README.md†L9-L33】【F:sensor/src/main/java/com/uoa/sensor/hardware/MotionFFTClassifier.kt†L1-L103】 |
| `driverprofile` | Manages driver creation, persistence, and safety tips surfaces.【F:README.md†L10-L33】【F:driverprofile/src/main/java/com/uoa/driverprofile/presentation/viewmodel/DriverProfileViewModel.kt†L1-L198】 |
| `nlgengine` | Aggregates unsafe-behaviour summaries and issues prompts to ChatGPT/Gemini for personalised reports.【F:README.md†L11-L55】【F:nlgengine/src/main/java/com/uoa/nlgengine/presentation/viewmodel/NLGEngineViewModel.kt†L1-L141】 |
| `ml` | Supplies ONNX-based classification helpers and unsafe-behaviour cause updates.【F:README.md†L12-L35】【F:ml/src/main/java/com/uoa/ml/domain/UseCases.kt†L1-L118】 |
| `dbda` | Hosts unsafe-behaviour analytics screens and supporting use cases.【F:README.md†L13-L58】【F:dbda/src/main/java/com/uoa/dbda/presentation/viewModel/AnalysisViewModel.kt†L1-L78】 |
| `alcoholquestionnaire` | Captures questionnaire answers, syncs them to the backend, and marks daily completion.【F:README.md†L14-L120】【F:alcoholquestionnaire/src/main/java/com/uoa/alcoholquestionnaire/presentation/viewmodel/QuestionnaireViewModel.kt†L1-L200】 |

## 3. Toolchain and environment

1. **Install prerequisites** – JDK 11, Android Studio Flamingo (or newer) with Android SDK 34, and the Gradle wrapper (already configured at 8.7).【F:README.md†L91-L101】 The Android module compiles against SDK 34, targets SDK 34, and requires minSdk 26.【F:app/build.gradle.kts†L11-L23】
2. **Clone and sync** – Clone the repository and let Android Studio import the included Gradle wrapper. All plugins and repositories are declared centrally in `settings.gradle.kts` and `build.gradle.kts`, so no extra build logic is required.【F:settings.gradle.kts†L1-L30】【F:build.gradle.kts†L1-L33】
3. **Android Studio configuration** – Ensure the Kotlin JVM target is 11, Compose is enabled, and ProGuard is configured for release builds as per `app/build.gradle.kts`.【F:app/build.gradle.kts†L34-L65】

## 4. Secrets and configuration

1. **API keys** – Populate `local.properties` with `GEMINI_API_KEY` and `CHAT_GPT_API_KEY` (or export them as environment variables) so the Retrofit services can authenticate.【F:README.md†L103-L112】 The `NetworkModule` validates these values at runtime and throws if they are missing, preventing silent misconfiguration.【F:core/src/main/java/com/uoa/core/di/NetworkModule.kt†L99-L133】
2. **OpenStreetMap compliance** – The `NetworkModule` configures OSM headers with an identifying user agent and referer to satisfy usage policies.【F:core/src/main/java/com/uoa/core/di/NetworkModule.kt†L135-L178】
3. **Shared preferences keys** – Reuse the constants defined in `core.utils.Constants` for profile IDs, trip IDs, questionnaire flags, and metered-network preferences.【F:core/src/main/java/com/uoa/core/utils/Constants.kt†L1-L12】

## 5. Build, run, and verification workflow

1. **Assemble & install** – Build the debug variant with `./gradlew assembleDebug` or via Android Studio (“Make Project”).【F:README.md†L97-L101】 The build enables Compose, view binding, and Hilt code generation through the Gradle plugin block.【F:app/build.gradle.kts†L1-L93】
2. **Unit tests** – Execute `./gradlew test` to run all module unit tests.【F:README.md†L153-L163】 Instrumentation tests rely on the Compose testing dependencies wired into the app module.【F:app/build.gradle.kts†L118-L122】
3. **Release builds** – When preparing production variants, keep the release build type’s minification and resource shrinking enabled and maintain ProGuard rules as configured.【F:app/build.gradle.kts†L34-L43】

## 6. Application shell and navigation

1. **Main entry point** – `MainActivity` is annotated with `@AndroidEntryPoint`, injects the shared `NetworkMonitor`, and immediately schedules the periodic `UploadAllDataWorker` before rendering the Compose hierarchy.【F:app/src/main/java/com/uoa/safedriveafrica/MainActivity.kt†L1-L62】 Metered-network behaviour is delegated to `PreferenceUtils`.【F:app/src/main/java/com/uoa/safedriveafrica/MainActivity.kt†L40-L55】【F:core/src/main/java/com/uoa/core/utils/PreferenceUtils.kt†L10-L41】
2. **Application scaffold** – `DAApp` provides a `Scaffold` with top and bottom bars, surfaces offline status via snackbars, and hosts the navigation graph.【F:app/src/main/java/com/uoa/safedriveafrica/DaApp.kt†L41-L158】
3. **App state holder** – `rememberDAAppState` composes `NavHostController`, monitors connectivity, and resolves the correct start destination (entrypoint, onboarding, home) based on stored profile IDs.【F:app/src/main/java/com/uoa/safedriveafrica/DaAppState.kt†L31-L135】
4. **Navigation graph** – `DAAppNavHost` wires together splash screens, onboarding, the driver profile flow, reporting features, sensor controls, and the alcohol questionnaire.【F:app/src/main/java/com/uoa/safedriveafrica/presentation/daappnavigation/DAAppNavHost.kt†L1-L58】 Entry-point routing decides whether to send the user to onboarding or straight to the home screen based on shared-preference state.【F:app/src/main/java/com/uoa/safedriveafrica/ui/appentrypoint/EntryPointScreenRoute.kt†L1-L57】

## 7. Shared core services

1. **Database** – `Sdadb` defines all Room entities and DAOs used throughout the app (sensor readings, AI inputs, unsafe behaviours, driver profiles, roads, embeddings, questionnaires, FFT features, and more).【F:core/src/main/java/com/uoa/core/Sdadb.kt†L1-L76】 The `DatabaseModuleProvider` installs the database with destructive migrations and exposes DAO singletons for Hilt injection.【F:core/src/main/java/com/uoa/core/di/DatabaseModuleProvider.kt†L1-L62】
2. **Networking** – `NetworkModule` centralises Retrofit client creation, API service provisioning (ChatGPT, Gemini, OSM, Overpass), the LLM repository binding, and the shared `NetworkMonitorImpl`.【F:core/src/main/java/com/uoa/core/di/NetworkModule.kt†L1-L216】
3. **Connectivity monitoring** – `NetworkMonitorImpl` registers a `ConnectivityManager` callback and exposes the connection state as a cold flow used across the UI and workers.【F:core/src/main/java/com/uoa/core/network/NetworkMonitorImpl.kt†L1-L77】
4. **Work scheduling** – `UploadAllDataWorker` serially uploads every unsynchronised entity (profile, trips, locations, tips, unsafe behaviours, raw sensors, AI model inputs, questionnaires, roads) and retries on failure, notifying the driver when connectivity issues occur.【F:core/src/main/java/com/uoa/core/apiServices/workManager/UploadAllDataWorker.kt†L1-L200】 The worker is invoked both from `MainActivity` and from the sensor control screen when additional batches should be pushed.【F:app/src/main/java/com/uoa/safedriveafrica/MainActivity.kt†L40-L61】【F:sensor/src/main/java/com/uoa/sensor/presentation/ui/SensorControlScreen.kt†L34-L40】
5. **Behaviour analysis** – `NewUnsafeDrivingBehaviourAnalyser` processes accelerometer, rotation, and speed windows to emit harsh acceleration/braking, swerving, and speeding events using adaptive thresholds and location-aware speed limits.【F:core/src/main/java/com/uoa/core/behaviouranalysis/NewUnsafeDrivingBehaviourAnalyser.kt†L40-L178】
6. **Sensor ingestion pipeline** – `ProcessAndStoreSensorData` performs batched inserts, streams readings through the behaviour analyser, writes detected unsafe behaviours, runs AI model input processing, and marks records as processed atomically inside a Room transaction.【F:core/src/main/java/com/uoa/core/database/repository/ProcessAndStoreSensorData.kt†L1-L107】

## 8. Sensor module details

1. **Motion classification** – `MotionFFTClassifier` maintains a circular buffer of sensor magnitudes, computes FFT spectra, and categorises motion into stationary, walking, running, vehicle, or unknown classes for additional heuristics.【F:sensor/src/main/java/com/uoa/sensor/hardware/MotionFFTClassifier.kt†L1-L102】
2. **Sensor control UI** – The Compose-based `SensorControlScreen` (referenced from `sensorControlScreen(navController)`) manages permission prompts for location, activity recognition, notifications, controls the background `VehicleMovementService` and `DataCollectionService`, and writes the current trip ID to shared preferences for downstream repositories.【F:sensor/src/main/java/com/uoa/sensor/presentation/ui/SensorControlScreen.kt†L1-L199】
3. **Data processing** – The sensor module injects `ProcessAndStoreSensorData` and the behaviour analyser through Hilt modules (see `presentation/di`) so background workers and services can persist data without tight coupling.【F:core/src/main/java/com/uoa/core/database/repository/ProcessAndStoreSensorData.kt†L22-L107】

## 9. Machine-learning enrichment

1. **Trip classification** – `RunClassificationUseCase` retrieves AI-model input features, computes duration-weighted aggregates (hour-of-day, day-of-week, speed standard deviation, course standard deviation, acceleration mean), and executes the ONNX runtime to estimate alcohol influence, handling missing data gracefully.【F:ml/src/main/java/com/uoa/ml/domain/UseCases.kt†L26-L115】
2. **Unsafe-behaviour updates** – Companion use cases batch-update unsafe-behaviour causes when classification indicates alcohol involvement, ensuring repository transactions propagate the new state.【F:ml/src/main/java/com/uoa/ml/domain/UseCases.kt†L122-L189】

## 10. Natural-language generation pipeline

1. **Report aggregation** – `NLGEngineViewModel` aggregates unsafe behaviours by period (last trip, today, this week, last week) using `PeriodUtils`, computes or retrieves cached report statistics, persists processed summaries, and produces prompts ready for the LLM.【F:nlgengine/src/main/java/com/uoa/nlgengine/presentation/viewmodel/NLGEngineViewModel.kt†L40-L141】【F:core/src/main/java/com/uoa/core/utils/PeriodUtils.kt†L10-L37】
2. **LLM invocation** – `GeminiViewModel` delegates to the shared `NLGEngineRepository`, returning responses as `LiveData` for Compose observation.【F:nlgengine/src/main/java/com/uoa/nlgengine/presentation/viewmodel/gemini/GeminiViewModel.kt†L1-L32】 `NLGEngineRepositoryImpl` handles both ChatGPT and Gemini requests and rate-limits OSM reverse-geocoding lookups for report localisation.【F:core/src/main/java/com/uoa/core/nlg/lngrepositoryimpl/remote/nlgApiRepositoryImpl/NLGEngineRepositoryImpl.kt†L1-L66】
3. **Reports UI** – The navigation graph routes to `reportScreen` and `filterScreen`, where the view models above provide prompt text and chart data (see `TopLevelDestinations.REPORTS`).【F:app/src/main/java/com/uoa/safedriveafrica/presentation/daappnavigation/DAAppNavHost.kt†L46-L58】

## 11. Driver profiling and persuasive content

1. **Profile creation** – `DriverProfileViewModel` persists driver email/ID locally, writes the driver ID to shared preferences for navigation decisions, and exposes success state to Compose screens.【F:driverprofile/src/main/java/com/uoa/driverprofile/presentation/viewmodel/DriverProfileViewModel.kt†L33-L155】
2. **Entry flow** – `EntryPointScreenRoute` checks for stored profile IDs and questionnaire status before redirecting to onboarding or the home screen, ensuring daily questionnaires are only shown once.【F:app/src/main/java/com/uoa/safedriveafrica/ui/appentrypoint/EntryPointScreenRoute.kt†L1-L53】
3. **Content generation** – README lines 133–151 describe the behavioural science principles embedded in the generated reports; ensure any new copy respects these guidelines.【F:README.md†L133-L151】

## 12. Unsafe-behaviour analytics module (DBDA)

1. **Raw data review** – `AnalysisViewModel` exposes flows of raw sensor data filtered by trip or date, ready for manual inspection or additional analysis in the analytics screens.【F:dbda/src/main/java/com/uoa/dbda/presentation/viewModel/AnalysisViewModel.kt†L1-L78】
2. **Navigation** – The DBDA screens are part of the home navigation stack (`homeScreen`, `drivingTipDetailsScreen`) so they inherit the same Hilt scopes via the app module.【F:app/src/main/java/com/uoa/safedriveafrica/presentation/daappnavigation/DAAppNavHost.kt†L46-L53】

## 13. Alcohol questionnaire flow

1. **View model pipeline** – `QuestionnaireViewModel` saves responses locally, attempts an immediate remote upload when online, marks records as synced, and emits loading/error states for Compose to react to. It reuses the shared `NetworkMonitor` to defer uploads when offline and formats timestamps for API compatibility.【F:alcoholquestionnaire/src/main/java/com/uoa/alcoholquestionnaire/presentation/viewmodel/QuestionnaireViewModel.kt†L37-L199】
2. **Daily gating** – Questionnaire screens should respect the `LAST_QUESTIONNAIRE_DAY` preference that `EntryPointScreenRoute` checks before showing the flow again on the same day.【F:app/src/main/java/com/uoa/safedriveafrica/ui/appentrypoint/EntryPointScreenRoute.kt†L8-L30】

## 14. Background synchronisation and offline strategy

1. **Work triggers** – `UploadAllDataWorker` is scheduled on app start and can also be enqueued manually from sensor controls, using a 15-minute periodic request with charging and (optionally) unmetered-network constraints.【F:app/src/main/java/com/uoa/safedriveafrica/MainActivity.kt†L40-L59】
2. **Offline UX** – `DAApp` observes the network monitor’s flow, surfaces offline snackbar alerts, and dismisses them automatically when connectivity returns.【F:app/src/main/java/com/uoa/safedriveafrica/DaApp.kt†L41-L75】
3. **Preference toggles** – Users can opt into metered uploads; `PreferenceUtils` stores this flag and the worker honours it when building network constraints.【F:core/src/main/java/com/uoa/core/utils/PreferenceUtils.kt†L10-L41】【F:app/src/main/java/com/uoa/safedriveafrica/MainActivity.kt†L40-L55】

## 15. Reproducing the system end-to-end

Follow these steps to rebuild the application from the ground up:

1. **Set up tooling** – Install JDK 11, Android Studio with SDK 34, and ensure Gradle sync works (use the wrapper in the repo).【F:README.md†L91-L101】【F:build.gradle.kts†L1-L33】
2. **Clone the code** – Fetch the repository, open it in Android Studio, and wait for Gradle sync; the module graph is already declared in `settings.gradle.kts`.【F:settings.gradle.kts†L1-L30】
3. **Configure secrets** – Create `local.properties` with valid Gemini and ChatGPT keys before running builds. The runtime checks in `NetworkModule` will fail fast if keys are missing.【F:README.md†L103-L112】【F:core/src/main/java/com/uoa/core/di/NetworkModule.kt†L99-L133】
4. **Review DI graph** – Inspect the Hilt modules in `core/di` and module-specific `presentation/di` packages to confirm dependencies (database, network clients, repositories) are wired before running the app.【F:core/src/main/java/com/uoa/core/di/DatabaseModuleProvider.kt†L1-L62】【F:core/src/main/java/com/uoa/core/di/NetworkModule.kt†L1-L216】
5. **Prepare device permissions** – Ensure any test device grants location, activity recognition, and notification permissions so the sensor flows and workers (which rely on these permissions) function correctly.【F:sensor/src/main/java/com/uoa/sensor/presentation/ui/SensorControlScreen.kt†L1-L199】
6. **Run the app** – Execute `./gradlew assembleDebug` (or “Run” in Android Studio). `MainActivity` will enqueue the periodic sync worker, compose the navigation graph, and present the onboarding flow.【F:app/src/main/java/com/uoa/safedriveafrica/MainActivity.kt†L29-L61】【F:app/src/main/java/com/uoa/safedriveafrica/presentation/daappnavigation/DAAppNavHost.kt†L25-L58】
7. **Create a driver profile** – Complete onboarding so `DriverProfileViewModel` saves a profile ID, which also updates shared preferences for future navigation decisions.【F:driverprofile/src/main/java/com/uoa/driverprofile/presentation/viewmodel/DriverProfileViewModel.kt†L123-L155】【F:app/src/main/java/com/uoa/safedriveafrica/DaAppState.kt†L118-L135】
8. **Collect sensor data** – Use the sensor control screen to start trips, verify services are running, and confirm `ProcessAndStoreSensorData` writes readings plus derived unsafe behaviours into the Room database.【F:sensor/src/main/java/com/uoa/sensor/presentation/ui/SensorControlScreen.kt†L1-L199】【F:core/src/main/java/com/uoa/core/database/repository/ProcessAndStoreSensorData.kt†L32-L103】
9. **Run ML classification** – After trips, trigger `RunClassificationUseCase` through the ML module (e.g., via ViewModel events) to generate alcohol-influence labels and update unsafe behaviours.【F:ml/src/main/java/com/uoa/ml/domain/UseCases.kt†L26-L189】
10. **Generate reports** – Navigate to the reports screen; `NLGEngineViewModel` will aggregate statistics, call Gemini/ChatGPT through `NLGEngineRepositoryImpl`, and display persuasive narratives informed by behavioural science guidelines.【F:nlgengine/src/main/java/com/uoa/nlgengine/presentation/viewmodel/NLGEngineViewModel.kt†L40-L141】【F:core/src/main/java/com/uoa/core/nlg/lngrepositoryimpl/remote/nlgApiRepositoryImpl/NLGEngineRepositoryImpl.kt†L23-L66】【F:README.md†L133-L151】
11. **Submit questionnaires** – Complete the alcohol questionnaire; `QuestionnaireViewModel` persists responses, attempts remote uploads, and marks daily completion for navigation gating.【F:alcoholquestionnaire/src/main/java/com/uoa/alcoholquestionnaire/presentation/viewmodel/QuestionnaireViewModel.kt†L37-L199】【F:app/src/main/java/com/uoa/safedriveafrica/ui/appentrypoint/EntryPointScreenRoute.kt†L8-L30】
12. **Monitor background sync** – Ensure `UploadAllDataWorker` succeeds (logs/notifications) and respects connectivity constraints via the injected `NetworkMonitor`. Handle retries by checking WorkManager statuses if needed.【F:core/src/main/java/com/uoa/core/apiServices/workManager/UploadAllDataWorker.kt†L71-L200】【F:core/src/main/java/com/uoa/core/network/NetworkMonitorImpl.kt†L1-L77】
13. **Prepare release builds** – When satisfied, run `./gradlew assembleRelease` (with necessary signing configs) knowing that minification, resource shrinking, and ProGuard optimisations are already enabled.【F:app/build.gradle.kts†L34-L43】

## 16. Development best practices

* Follow the MVVM layering outlined in the README (Compose UI → ViewModel → Repository → core).【F:README.md†L62-L89】
* Use the shared `NetworkMonitor` and `PreferenceUtils` utilities rather than duplicating connectivity checks or preference keys.【F:core/src/main/java/com/uoa/core/network/NetworkMonitorImpl.kt†L1-L77】【F:core/src/main/java/com/uoa/core/utils/PreferenceUtils.kt†L10-L41】
* When adding new modules or dependencies, register them in `settings.gradle.kts` and the relevant `build.gradle.kts` file, mirroring existing modules for consistency.【F:README.md†L157-L163】【F:settings.gradle.kts†L1-L30】
* Extend `UploadAllDataWorker` if new entities need server synchronisation; maintain the dependency-ordering list so related data uploads in a deterministic sequence.【F:core/src/main/java/com/uoa/core/apiServices/workManager/UploadAllDataWorker.kt†L124-L146】

Armed with this guide, a new contributor can replicate the entire Safe Drive Africa system—from installing dependencies through data capture, analysis, reporting, and deployment—using the authoritative source code references provided above.
