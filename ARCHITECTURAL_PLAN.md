# Architectural Plan: Hybrid Storage and Staged Data Lifecycle for Safe Drive Africa

## 1. Problem Statement

The current architecture of the Safe Drive Africa application suffers from a critical race condition. The `UploadAllDataWorker` and `DeleteLocalDataWorker` run on short intervals, implementing an aggressive "upload-and-delete" strategy for local trip and sensor data. This approach, designed to conserve device storage, inadvertently starves critical on-device features. Local LLM-powered tasks, which require this data to generate driving reports and safety tips, often fail because the data is deleted before they can run. Furthermore, this immediate data deletion makes it difficult to generate long-term (weekly, monthly, custom) driving summary reports on the device.

## 2. Proposed Solution

To resolve this data starvation issue, we will implement a new architecture based on a **Hybrid Storage Model** and a **Staged Data Lifecycle**.

- **Hybrid Storage Model:** Separate data into two categories:
  - **Heavy Data:** Raw sensor readings (`RawSensorDataEntity`), locations, and AI model inputs. This data is bulky and will be stored temporarily on the device.
  - **Lightweight Data:** Keep two lightweight stores:
    - **`TripSummaryEntity`** for per-trip summaries (trip scoped cache).
    - **`ReportStatisticsEntity`** for period summaries (daily/weekly/monthly/custom caches), built from trip summaries and unsafe behaviour summaries.

- **Staged Data Lifecycle:** Replace the simple `sync: Boolean` flag with a `SyncState` enum to track the precise lifecycle of each trip. Heavy data deletion is gated by **summary readiness + upload success**, while LLM output completion is tracked separately so long-running generations do not block cleanup.

## 3. Core Components of the New Architecture

### SyncState Enum

This enum will replace the `sync: Boolean` flag in `TripEntity` to provide granular control over the data lifecycle.

```kotlin
// In: core/src/main/java/com/uoa/core/model/SyncState.kt
package com.uoa.core.model

enum class SyncState {
    /**
     * Trip completed, classification done, summaries computed (TripSummaryEntity + trip-scoped ReportStatisticsEntity).
     */
    SUMMARY_READY,

    /**
     * Raw sensor data and trip details successfully uploaded to the backend.
     */
    RAW_DATA_UPLOADED,

    /**
     * LLM outputs (reports/tips) have been generated and saved locally.
     * This does NOT gate heavy data cleanup.
     */
    LLM_OUTPUT_READY,

    /**
     * Heavy data cleaned; only summaries and retained lightweight records remain.
     */
    ARCHIVED
}
```

### TripSummaryEntity (Per-Trip Cache)

This Room entity stores the lightweight per-trip summary used for trip reports and as a base for period aggregation.

```kotlin
// In: core/src/main/java/com/uoa/core/database/entities/TripSummaryEntity.kt
package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import java.util.Date

@Entity(tableName = "trip_summary")
data class TripSummaryEntity(
    @PrimaryKey
    val tripId: UUID,
    val driverId: UUID,
    val startTime: Long,
    val endTime: Long,
    val startDate: Date,
    val endDate: Date,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val harshBrakingEvents: Int,
    val harshAccelerationEvents: Int,
    val speedingEvents: Int,
    val classificationLabel: String,
    val alcoholProbability: Float?
)
```

### ReportStatisticsEntity (Period Cache)

Keep `ReportStatisticsEntity` as the lightweight cache for period summaries (day/week/month/custom). These records are derived from `TripSummaryEntity` plus summarized unsafe behaviour data. For last-trip reports, a `ReportStatisticsEntity` row can optionally be created with `tripId` set for fast retrieval.

## 4. The New End-to-End Workflow

### Stage 1: Trip Completion (Synchronous)

Occurs inside `VehicleMovementServiceUpdate.safeAutoStop` when a trip ends.

1. Stop data collection.
2. Run local classification (requires AI model inputs created during the trip).
3. Generate `TripSummaryEntity` and a trip-scoped `ReportStatisticsEntity` (or equivalent prompt summary payload).
4. Update the database transactionally:
   - Insert `TripSummaryEntity`.
   - Insert trip-scoped `ReportStatisticsEntity` (if available).
   - Update `TripEntity` classification fields and set state to `SyncState.SUMMARY_READY`.
5. Enqueue a `WorkManager` chain:
   - `RawDataUploadWorker` then `LlmGenerationWorker`.

### Stage 2: Background Upload (`RawDataUploadWorker`)

- Trigger: Trips in `SyncState.SUMMARY_READY`.
- Action: Upload `TripEntity`, `RawSensorDataEntity`, unsafe behaviours, and AI model inputs (if required by backend). No local deletion occurs here.
- On Success: Update trip state to `RAW_DATA_UPLOADED`.

### Stage 3: Background LLM Processing (`LlmGenerationWorker`)

- Trigger: Trips with summaries available.
- Action:
  - Use `TripSummaryEntity` and recent unsafe behaviours to generate per-trip reports and tips.
  - Build or refresh `ReportStatisticsEntity` for requested periods (day/week/month/custom).
- On Success: Update trip state to `LLM_OUTPUT_READY`.

### Stage 4: Controlled Cleanup (`DeleteLocalDataWorker`)

- Delete **AI model inputs** immediately after classification completes (they are no longer needed locally once the trip is labeled).
- Delete **raw sensor data and locations** as soon as:
  - summaries are computed (`SUMMARY_READY`), and
  - upload succeeds (`RAW_DATA_UPLOADED`).
- Do **not** wait for LLM outputs to finish before cleaning heavy data.
- Keep unsafe behaviours for a rolling **14-day** retention window (regardless of sync) so tips and reports can be generated reliably.
- Preserve `TripSummaryEntity` and `ReportStatisticsEntity` indefinitely (or with a much longer retention policy).

## 5. Generating Long-Term Reports

- **Per-trip reports:** Read from `TripSummaryEntity` and (optionally) a trip-scoped `ReportStatisticsEntity` record.
- **Daily/weekly/monthly/custom reports:** Aggregate from `TripSummaryEntity` and recent unsafe behaviour summaries to produce or refresh a `ReportStatisticsEntity` cache for that period.
- This keeps long-term reporting fast and avoids loading raw sensor data.

## 6. Tips Generation Considerations

Driving tips currently depend on recent unsafe behaviours. To avoid starvation:

- Keep unsafe behaviour records for a rolling **14-day** retention window.
- Optionally persist a small "recent behaviours" summary in `TripSummaryEntity` or `ReportStatisticsEntity` (type, severity, timestamp, road name) so tips can be generated without raw data.

## 7. The Revised Role of `UploadAllDataWorker.kt`

`UploadAllDataWorker` becomes a safety net and bulk synchronizer that runs less frequently (e.g., hourly/daily):

1. Reconcile trips stuck in intermediate states and re-enqueue the proper worker.
2. Sync independent data sets (driver profile, questionnaires, tips, reports).
3. Upload LLM outputs (tips and reports) after they are persisted locally.
4. Perform selective cleanup only for trips in `RAW_DATA_UPLOADED`/`ARCHIVED` and with summaries already computed.

## 8. Lossless Compression in Room (Heavy Data)

To keep long trips from bloating local storage without losing fidelity:

- Store heavy raw sensor values and location series as compressed BLOBs via Room `TypeConverter`s.
- Encode numeric arrays (e.g., `FloatArray`) to a binary buffer before compression, then reverse on read.
- Keep API-level models unchanged; compression is an internal storage concern.

## 9. Summary of Benefits

- **Reliability:** Eliminates the race condition by gating cleanup on explicit states.
- **Feature enablement:** Supports long-term on-device reports without raw data.
- **Efficiency:** Reports use lightweight summaries, keeping memory and I/O low.
- **Maintainability:** Dedicated workers are easier to debug and test.
- **Storage management:** Heavy data is removed immediately after it is no longer needed locally.
