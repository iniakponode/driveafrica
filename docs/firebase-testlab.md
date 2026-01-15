# Firebase Test Lab (Robo + Instrumentation)

This project includes a PowerShell script to run Firebase Test Lab Robo and
instrumentation tests against debug builds.

## Prerequisites

1. Install and initialize the gcloud CLI.
2. Enable Firebase Test Lab in your Google Cloud project.
3. Ensure billing is enabled for the project.

## Usage

```powershell
.\scripts\testlab\run_testlab.ps1 -ProjectId your-gcp-project-id
```

Optional:

```powershell
.\scripts\testlab\run_testlab.ps1 `
  -ProjectId your-gcp-project-id `
  -ResultsBucket your-gcs-bucket `
  -RoboTimeoutMinutes 20 `
  -InstrumentationTimeoutMinutes 30
```

## What it does

- Builds `app-debug.apk` and `app-debug-androidTest.apk`.
- Runs Robo tests on Pixel 6 (Android 14) and Pixel 5 (Android 11).
- Runs instrumentation tests on the same devices.

Adjust devices and timeouts in `scripts/testlab/run_testlab.ps1` if needed.
