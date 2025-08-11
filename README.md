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

## Build prerequisites

- JDK 11
- Android Studio Flamingo (or later) with Android SDK 34
- Gradle 8.7 (handled automatically by the wrapper)

### Setup

1. Clone the repository.
2. Supply API keys as described below.
3. Build the project with `./gradlew assembleDebug` or open it in Android Studio.

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

