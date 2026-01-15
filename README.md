# Safe Drive Africa

Safe Drive Africa is a research application developed as part of a PhD study on using AI-enabled mobile apps to encourage safer driving in non-Western contexts. The project collects sensor and questionnaire data on driving behaviour and uses natural language generation to provide personalised feedback. The Nigerian version is maintained in this repository.

## Modules

- **app** – Android entry point that wires together the other modules.
- **core** – common utilities, database access, network APIs and behaviour analysis logic.
- **sensor** – interfaces with device sensors and performs motion detection and data buffering.
- **driverprofile** – manages driver profiles and delivers driving tips.
- **nlgengine** – communicates with ChatGPT/Gemini and generates textual driving reports.
- **ml** – machine‑learning helpers used by behaviour analysis components.
- **dbda** – unsafe behaviour data analysis utilities.
- **alcoholquestionnaire** – collects alcohol consumption questionnaire responses.

## Architecture overview

The project follows a multi‑module Clean Architecture pattern. Each module is a
stand‑alone library wired together in the **app** entry point using [Hilt](https://dagger.dev/hilt/).
Important pieces include:

- **MainActivity** – starts the Compose UI and schedules `UploadAllDataWorker` for
  periodic uploads.
- **core** – hosts the `Sdadb` Room database, network clients and work manager
  classes such as `UploadAllDataWorker` and `WorkSchedular`. Other modules depend
  on these abstractions to read and write data.
- **sensor** – provides `LocationManager`, `MotionFFTClassifier` and repository
  implementations (`SensorDataRepositoryImpl`, `LocationRepositoryImpl`) that
  stream hardware readings into the `core` database.
- **driverprofile** – exposes `DriverProfileRepositoryImpl` and
  `DrivingTipRepositoryImpl` which feed `DriverProfileViewModel` and
  `DrivingTipsViewModel` to render driver information and safety tips.
- **ml** – contains the Onnx model runner and use cases such as
  `RunClassificationUseCase` that enrich raw sensor data with machine‑learning
  features.
- **dbda** – analysis utilities and view models
  (`AnalysisViewModel`, `UnsafeBehaviourRepositoryImpl`) used to review unsafe
  behaviours.
- **nlgengine** – view models (`ChatGPTViewModel`, `GeminiViewModel`,
  `NLGEngineViewModel`) and repositories that generate and persist natural‑language
  driving reports.
- **alcoholquestionnaire** – a simple `QuestionnaireViewModel` and repository for
  collecting alcohol consumption answers.

### Component interaction

1. The **sensor** module records motion and location events and persists them to
   the `core` database.
2. The **ml** and **dbda** modules transform these records into unsafe‑behaviour
   summaries.
3. `UploadAllDataWorker` in **core** periodically uploads local data to the
   backend and fetches new driving tips.
4. The **nlgengine** module reads local summaries and calls ChatGPT/Gemini to
   produce personalised reports stored via `NLGReportRepositoryImpl`.
5. The **driverprofile** module displays tips and report summaries to the user,
   while the **alcoholquestionnaire** module augments the dataset with survey
   responses.

With this flow, a new developer can trace each feature from UI, through its
view model, into repositories and the shared `core` layer.

### MVVM implementation

Safe Drive Africa applies the Model–View–ViewModel pattern consistently across
modules:

- **View (Compose screens)** – The `app` module hosts Jetpack Compose screens
  such as `DisclaimerScreen`, `DriverProfileScreen` and the alcohol
  questionnaire. Each screen is responsible only for rendering state exposed by
  a view model and forwarding user intents (button taps, refresh events, etc.).
- **ViewModel** – Every feature module defines its own view models that extend
  `ViewModel` and inject dependencies with Hilt. Examples include
  `DriverProfileViewModel` (driver profiles and tips), `DrivingTipsViewModel`
  (safety advice), `ChatGPTViewModel`/`GeminiViewModel` (LLM reports), and
  `QuestionnaireViewModel` (alcohol survey). View models coordinate UI state,
  expose immutable `StateFlow`/`LiveData` to the UI, invoke use cases and handle
  WorkManager scheduling when needed.
- **Model and data layer** – Repository implementations live in their owning
  module (e.g. `DriverProfileRepositoryImpl`, `NLGReportRepositoryImpl`,
  `SensorDataRepositoryImpl`) and depend on the shared `core` abstractions for
  persistence (`Sdadb`, DAOs) and networking. Additional use cases in modules
  such as `ml` or `dbda` provide domain-specific transformations that view
  models compose.

This structure keeps UI code declarative and side-effect free, centralises
business logic in the view models and use cases, and isolates data access inside
repositories. Hilt modules in each package bind repository interfaces to their
implementations, letting the same MVVM flow scale across new features without
tight coupling between UI and data sources.

## Build prerequisites

- JDK 17 (required by Android Gradle Plugin 8.13.1)
- Android Studio Flamingo (or later) with Android SDK 34
- Gradle 8.7 (handled automatically by the wrapper)

### Setup

1. Clone the repository.
2. Supply API keys as described below.
3. Build the project with `./gradlew assembleDebug` or open it in Android Studio.

### Documentation

- [User manual](docs/USER_MANUAL.md)
- [Reproduction guide](docs/REPRODUCTION_GUIDE.md)

## API Keys

The project expects API keys to be defined locally. Create a `local.properties` file in the project root (this file is ignored by Git) and add:

```properties
GEMINI_API_KEY=your_gemini_key
CHAT_GPT_API_KEY=your_chatgpt_key
```

Alternatively, you can export the environment variables `GEMINI_API_KEY` and `CHAT_GPT_API_KEY` before running Gradle commands.

## Features and research context

- Collects accelerometer, rotation and speed data to detect unsafe driving behaviour.
- Periodically uploads sensor and location data to the Safe Drive Africa backend via `UploadAllDataWorker`.
- Uses a generative model (ChatGPT or Gemini) to craft custom feedback reports.
- Includes an alcohol questionnaire and driver profiling to enrich the data set.
- The alcohol questionnaire appears at most once per day when you open the app.
- Part of a PhD project at the University of Aberdeen, Scotland, as introduced on the welcome screen.

## Privacy and data usage

The app stores driving sensor readings, location information and questionnaire responses locally before uploading them to the research server. Data usage is governed by the in‑app privacy policy, which can be accessed from the disclaimer screen:

```
Privacy Policy link – `https://datahub.safedriveafrica.com/privacy`
```

See `DisclaimerScreen.kt` lines 95‑101 for the implementation.

## Behavioural and persuasive design

The driving reports employ culturally resonant language for Nigerian drivers while remaining accessible to Cameroonian and Ghanaian users. The prompt guiding the large‑language model blends several behaviour‑change theories:

- **Theory of Planned Behavior** – emphasises attitudes toward safety, social expectations and perceived control.
- **Social Cognitive Theory** – encourages observational learning, self‑efficacy and realistic outcome expectations.
- **Health Belief and Protection Motivation Models** – highlight perceived risk, severity, benefits and the driver's confidence in coping, while **Deterrence Theory** underlines the certainty of consequences for violations.
- **Nudge Theory and Behavioural Economics** – subtly steer choices through supportive cues without removing options.
- **Cialdini's Principles** – leverage social proof and loss aversion to motivate change.

Key localisation strategies ensure the reports feel relevant:

- Use respectful English with optional Nigerian Pidgin or local languages.
- Acknowledge traffic congestion or rough roads before offering advice.
- Give simple, behaviour‑specific tips that build ability.
- Incorporate local knowledge such as rainy season hazards or dealings with "area boys".
- Reference alcohol influence results when present.

By weaving these elements into a concise 150–180 word narrative, the reports remain persuasive, context‑aware and easy for drivers to act upon.

## Development tips

- Run `./gradlew test` to execute unit tests across all modules.
- Run `./gradlew assembleDebug` for a local debug build.
- New features generally follow the pattern: Compose UI → ViewModel (Hilt) →
  repository → `core` database. Ensuring each layer has clear interfaces makes
  cross‑module changes easier.
- Update `settings.gradle.kts` and individual `build.gradle.kts` files when
  adding modules or external libraries.
- Reuse `UploadAllDataWorker` or extend it if new data needs to be synchronised
  with the backend.

