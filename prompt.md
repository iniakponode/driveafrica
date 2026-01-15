ou are an expert Android software architect. Your task is to create a detailed architectural plan to solve a critical data lifecycle problem in an Android application called "Safe Drive Africa".
Current System Context:
•
Local Database: The app uses Android Room for its local database. Key entities include TripEntity, RawSensorDataEntity, and UnsafeBehaviourEntity.
•
Background Processing: WorkManager is used for background tasks. A key worker, UploadAllDataWorker.kt, runs periodically (every 5 minutes) on an internet connection.

•
Trip Detection: A foreground service, VehicleMovementServiceUpdate, detects the start and end of driving trips.
•
The Core Problem: The UploadAllDataWorker currently implements an aggressive "upload-and-delete" strategy for all local data to prevent the user's device storage from filling up. This creates a severe race condition:
i.
LLM Feature Starvation: Raw sensor data and trip details are often uploaded and deleted before they can be used locally to generate LLM-powered driving reports and safety tips.

•
The Core Problem: The UploadAllDataWorker currently implements an aggressive "upload-and-delete" strategy for all local data to prevent the user's device storage from filling up. This creates a severe race condition:
i.
LLM Feature Starvation: Raw sensor data and trip details are often uploaded and deleted before they can be used locally to generate LLM-powered driving reports and safety tips.
ii.
Long-Term Reporting Failure: It's impossible to generate weekly or monthly summary reports on the device because the historical data is deleted within minutes of being created.

Objective: Design and document a new architecture that resolves this data starvation issue.
Key Requirements for the Solution:
1.
It must guarantee that data required for LLM processing remains on the device until after the LLM tasks are complete.
2.
It must enable the generation of historical (weekly, monthly) reports on the device without storing massive amounts of raw data long-term.
3.
The system must remain robust, using WorkManager to handle unreliable network conditions and app closures.
4.
The role of the existing UploadAllDataWorker must be redefined to fit the new architecture.
Proposed High-Level Solution (to guide your plan): The solution should be based on a Hybrid Storage Model and a Staged Data Lifecycle. This involves keeping lightweight, aggregated data long-term while deleting heavy, raw data after it has been processed and uploaded.
Required Document Structure: Please create a comprehensive plan with the following sections. Be detailed and provide code snippets where appropriate.
1.
Problem Statement: Briefly summarize the race condition and data starvation issue.
2.
Proposed Solution: Describe the high-level approach using the "Hybrid Storage Model" and "Staged Data Lifecycle" concepts.

3.
Core Components of the New Architecture:
◦
Detail the new SyncState enum that will replace the sync: Boolean flag in the TripEntity. Provide the enum's Kotlin code.
◦
Detail the new TripSummaryEntity for Room. Explain its purpose and provide its Kotlin data class definition, including fields for aggregated metrics like unsafe event counts, distance, and duration.
4.
The New End-to-End Workflow: Describe the step-by-step process in stages:
◦
Stage 1: Trip Completion: What happens synchronously inside VehicleMovementServiceUpdate.safeAutoStop. Emphasize that it now handles local classification and summary generation, then enqueues a background worker.
◦
Stage 2: Background Upload:
Describe the new, event-driven RawDataUploadWorker. Explain its single responsibility of uploading data and advancing the trip's SyncState.
◦
Stage 3: Background LLM Processing: Describe the LlmGenerationWorker, which runs after the upload worker, using the still-local data to generate reports and update the trip to its final state.
5.
Generating Long-Term Reports: Explain how the new TripSummaryEntity makes weekly and monthly reporting efficient and simple with a single database query.
6.
The Revised Role of UploadAllDataWorker.kt: Clearly define its new purpose as a Safety Net and Bulk Synchronizer. Explain its new responsibilities: syncing independent data (like DriverProfile), performing selective cleanup of raw data (while keeping summaries), and reconciling "stuck" trips.
7.
Summary of Benefits: Conclude with a bulleted list summarizing the advantages of this new architecture (e.g., reliability, efficiency, maintainability).
Formatting: Format the entire output in Markdown. Use headings, bold text, code blocks for code snippets, and lists to ensure the document is clear, professional, and ready to be copied directly into a project wiki or a technical design document.