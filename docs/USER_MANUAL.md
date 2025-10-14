# Safe Drive Africa User Manual

## 1. Overview
Safe Drive Africa is a research application that records driving sensor data, gathers questionnaire responses and generates personalised safety feedback with large language models to support safer driving in West Africa.【F:README.md†L3-L41】【F:README.md†L45-L57】 The Android app is organised into feature modules for sensor collection, driver profiles, reports and the daily alcohol questionnaire, all composed in the main application shell.【F:README.md†L5-L43】【F:app/src/main/java/com/uoa/safedriveafrica/presentation/daappnavigation/DAAppNavHost.kt†L32-L58】

## 2. Installation and setup
### 2.1 Device requirements
The Android build targets SDK 34, supports devices running Android 8.0 (API 26) and later, and uses version 1.12 of the application ID `com.uoa.safedriveafrica`.【F:app/build.gradle.kts†L11-L21】

### 2.2 Configure API keys
Before building or launching the app, provide Gemini and ChatGPT API keys by adding `GEMINI_API_KEY` and `CHAT_GPT_API_KEY` to `local.properties` (or exporting the same environment variables) in the project root.【F:README.md†L97-L112】

### 2.3 Build and install
Use Android Studio Flamingo (or later) with JDK 11, or run `./gradlew assembleDebug`, to compile and install the debug APK on a supported device.【F:README.md†L91-L101】 After installation, grant the runtime permissions requested at first launch (see section 5) so background data capture can begin.

## 3. First launch and onboarding
The navigation graph opens with the welcome and disclaimer screens before routing to the entry point that decides whether to show onboarding or the home dashboard.【F:app/src/main/java/com/uoa/safedriveafrica/presentation/daappnavigation/DAAppNavHost.kt†L32-L54】 If no profile is stored, you will be taken to the driver profile creation form.

On the onboarding screen, enter the profile ID issued to you by the research team. The form validates empty input, stores the generated UUID and email locally, uploads the profile via the driver profile view model and redirects you to the home screen once the profile is created successfully.【F:driverprofile/src/main/java/com/uoa/driverprofile/presentation/ui/screens/DriverProfileCreationScreen.kt†L49-L168】 Returning participants with a saved profile are sent directly to the home dashboard.【F:driverprofile/src/main/java/com/uoa/driverprofile/presentation/ui/screens/DriverProfileCreationScreen.kt†L105-L121】【F:app/src/main/java/com/uoa/safedriveafrica/ui/appentrypoint/EntryPointScreenRoute.kt†L23-L53】

## 4. Home dashboard
The home screen greets you with your saved email, surfaces any daily alcohol questionnaire reminder card, and lists the latest GPT- and Gemini-generated driving tips with links to detailed advice.【F:driverprofile/src/main/java/com/uoa/driverprofile/presentation/ui/screens/HomeScreen.kt†L55-L125】 Quick actions let you open the daily questionnaire, start trip recording or view reports from a single tap.【F:driverprofile/src/main/java/com/uoa/driverprofile/presentation/ui/screens/HomeScreen.kt†L127-L153】 A bottom navigation bar is available throughout the app for Home, Reports and Record Trip destinations.【F:app/src/main/java/com/uoa/safedriveafrica/DaApp.kt†L101-L137】【F:app/src/main/java/com/uoa/safedriveafrica/presentation/daappnavigation/TopNavDestinations.kt†L9-L41】

## 5. Recording trips
Open **Record Trip** from the home actions or bottom bar to launch the sensor control screen. The first button press will request foreground location, activity recognition, background location (Android 10+) and notification permissions; all are required before recording can start.【F:sensor/src/main/java/com/uoa/sensor/presentation/ui/SensorControlScreen.kt†L227-L303】 Once permissions are granted, the app starts the vehicle movement service automatically and monitors whether motion exceeds the driving threshold.【F:sensor/src/main/java/com/uoa/sensor/presentation/ui/SensorControlScreen.kt†L254-L283】 Tap **Start Trip** while the vehicle is moving to generate a new trip ID, launch the background data collection service and receive confirmation feedback.【F:sensor/src/main/java/com/uoa/sensor/presentation/ui/SensorControlScreen.kt†L305-L327】 When you finish a drive, tap **End Trip** to stop recording, clear the stored trip ID and shut down the service; the button also guides you through any remaining sensor data processing before ending.【F:sensor/src/main/java/com/uoa/sensor/presentation/ui/SensorControlScreen.kt†L330-L373】 Status text below the button shows live motion readings and whether collection is currently running.【F:sensor/src/main/java/com/uoa/sensor/presentation/ui/SensorControlScreen.kt†L378-L388】

## 6. Viewing reports
Select **Reports** from the home screen or bottom navigation to open the filter screen. Use the preset buttons for last trip, today, this week, last week or custom date ranges, or pick start/end dates manually before generating a report.【F:nlgengine/src/main/java/com/uoa/nlgengine/presentation/ui/FilterScreen.kt†L51-L224】 When you tap **Generate Report**, the app produces a large-language-model summary; the report view includes navigation controls, report date, the chosen period label and the personalised narrative for the selected window.【F:nlgengine/src/main/java/com/uoa/nlgengine/presentation/ui/ReportScreen.kt†L55-L188】

## 7. Daily alcohol questionnaire
The alcohol questionnaire appears at most once per day and can be launched from the reminder card or the dedicated button on the home screen.【F:README.md†L116-L121】【F:driverprofile/src/main/java/com/uoa/driverprofile/presentation/ui/screens/HomeScreen.kt†L77-L135】 Internally, the app tracks the last completion date in shared preferences to avoid prompting more than once per day.【F:driverprofile/src/main/java/com/uoa/driverprofile/presentation/ui/screens/HomeScreen.kt†L180-L195】【F:app/src/main/java/com/uoa/safedriveafrica/ui/appentrypoint/EntryPointScreenRoute.kt†L23-L53】

## 8. Connectivity and background sync
Safe Drive Africa monitors connectivity globally and shows a persistent snackbar when the device is offline, followed by a short confirmation once connectivity returns.【F:app/src/main/java/com/uoa/safedriveafrica/DaApp.kt†L41-L75】 Data uploads run automatically via a periodic worker that requires charging power and (by default) an unmetered connection, ensuring sensor logs and questionnaire responses sync in the background.【F:app/src/main/java/com/uoa/safedriveafrica/MainActivity.kt†L29-L60】【F:README.md†L116-L118】

