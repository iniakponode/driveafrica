# Room Entity Reference

This document enumerates the DriveAfrica Room entities together with their defining columns, default values, indices, and foreign-key relationships as captured in the Kotlin data classes under `core/database/entities`.

## DriverProfileEntity (`driver_profile`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `driverProfileId` | `UUID` | Primary key (no auto-generate) | Unique identifier for each driver profile. |
| `email` | `String` | — | Stores the driver's email address. |
| `sync` | `Boolean` | Defaults to `false` | Tracks whether the profile has been synchronised with the backend. |

Indices: single index on `driverProfileId` to accelerate joins from dependent tables.【F:core/src/main/java/com/uoa/core/database/entities/DriverProfileEntity.kt†L8-L18】

## TripEntity (`trip_data`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `id` | `UUID` | Primary key | Unique trip session identifier. |
| `driverPId` | `UUID?` | Nullable FK → `driver_profile.driverProfileId` | Associates the trip with a driver profile when available. |
| `startDate` | `Date?` | — | Calendar date for trip start. |
| `endDate` | `Date?` | — | Calendar date for trip end. |
| `startTime` | `Long` | — | Epoch milliseconds when the trip started. |
| `endTime` | `Long?` | — | Epoch milliseconds when the trip ended. |
| `influence` | `String?` | — | Captures alcohol influence assessment. |
| `sync` | `Boolean` | Defaults to `false` | Sync flag for uploads. |

Indices: `driverPId` and unique `id`. Cascade delete from driver profile ensures orphaned trips are removed.【F:core/src/main/java/com/uoa/core/database/entities/TripEntity.kt†L10-L35】

## AIModelInputsEntity (`ai_model_inputs`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `id` | `UUID` | Primary key | Identifies a window of aggregated model features. |
| `tripId` | `UUID` | FK → `trip_data.id` (cascade delete) | Binds inputs to a specific trip. |
| `driverProfileId` | `UUID` | FK → `driver_profile.driverProfileId` (cascade delete) | Binds inputs to a driver. |
| `timestamp` | `Long` | — | Capture time for the feature window. |
| `startTimestamp` | `Long` | — | Start of aggregation window. |
| `endTimestamp` | `Long` | — | End of aggregation window. |
| `date` | `Date?` | — | Human-friendly day grouping. |
| `hourOfDayMean` | `Double` | — | Average hour of occurrences. |
| `dayOfWeekMean` | `Float` | — | Encodes day-of-week distribution. |
| `speedStd` | `Float` | — | Standard deviation of speed. |
| `courseStd` | `Float` | — | Standard deviation of course/heading. |
| `accelerationYOriginalMean` | `Float` | — | Mean Y-axis acceleration. |
| `processed` | `Boolean` | Defaults to `false` | Tracks downstream pipeline status. |
| `sync` | `Boolean` | Defaults to `false` | Upload flag. |

Indices: tripId, id, driverProfileId to support queries by trip or driver.【F:core/src/main/java/com/uoa/core/database/entities/AIModelInputsEntity.kt†L11-L44】

## RawSensorDataEntity (`raw_sensor_data`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `id` | `UUID` | Primary key | Unique sample identifier. |
| `sensorType` | `Int` | — | Android sensor type constant. |
| `sensorTypeName` | `String` | — | Readable sensor name. |
| `values` | `List<Float>` | — | Recorded sensor vector. |
| `timestamp` | `Long` | — | Capture timestamp (epoch millis). |
| `date` | `Date?` | — | Convenience date bucket. |
| `accuracy` | `Int` | — | Sensor-reported accuracy level. |
| `locationId` | `UUID?` | Nullable FK → `location.id` (cascade delete) | Links to a location sample when available. |
| `tripId` | `UUID?` | Nullable FK → `trip_data.id` (cascade delete) | Associates with a trip session. |
| `driverProfileId` | `UUID?` | Nullable FK → `driver_profile.driverProfileId` (cascade delete) | Identifies the owner driver. |
| `processed` | `Boolean` | Defaults to `false` | Marks whether sample has been post-processed. |
| `sync` | `Boolean` | Defaults to `false` | Upload flag, mutable for retries. |

Indices cover foreign keys, sync flag, and date to speed filtering of time-series uploads.【F:core/src/main/java/com/uoa/core/database/entities/RawSensorDataEntity.kt†L19-L52】

## SensorEntity (`sensor_data`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `id` | `Long` | Primary key (auto-generated) | Row identifier for legacy sensor ingestion. |
| `tripDataId` | `Long` | — | Links to historical trip identifier. |
| `timestamp` | `Long` | — | Capture time in epoch millis. |
| `synced` | `Boolean` | Defaults to `false` | Upload status. |
| `accelerometerX` | `Float` | — | X-axis accelerometer sample. |
| `accelerometerY` | `Float` | — | Y-axis accelerometer sample. |
| `accelerometerZ` | `Float` | — | Z-axis accelerometer sample. |
| `gyroscopeX` | `Float` | — | X-axis gyroscope sample. |
| `gyroscopeY` | `Float` | — | Y-axis gyroscope sample. |
| `gyroscopeZ` | `Float` | — | Z-axis gyroscope sample. |
| `magnetometerX` | `Float` | — | X-axis magnetometer reading. |
| `magnetometerY` | `Float` | — | Y-axis magnetometer reading. |
| `magnetometerZ` | `Float` | — | Z-axis magnetometer reading. |
| `rotationVectorX` | `Float` | — | Rotation vector X component. |
| `rotationVectorY` | `Float` | — | Rotation vector Y component. |
| `rotationVectorZ` | `Float` | — | Rotation vector Z component. |
| `rotationVectorScalar` | `Float` | — | Scalar component of rotation vector. |
| `linearAccelerationX` | `Float` | — | Linear acceleration X component. |
| `linearAccelerationY` | `Float` | — | Linear acceleration Y component. |
| `linearAccelerationZ` | `Float` | — | Linear acceleration Z component. |
| `speed` | `Float` | — | Observed vehicle speed. |

No explicit indices; schema represents a legacy denormalised sensor dump.【F:core/src/main/java/com/uoa/core/database/entities/sensorEntity.kt†L1-L27】

## UnsafeBehaviourEntity (`unsafe_behaviour`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `id` | `UUID` | Primary key | Unique unsafe behaviour record. |
| `tripId` | `UUID` | FK → `trip_data.id` (cascade delete) | Trip during which behaviour occurred. |
| `driverProfileId` | `UUID` | FK → `driver_profile.driverProfileId` (cascade delete) | Driver responsible. |
| `locationId` | `UUID?` | Nullable FK → `location.id` (cascade delete) | Location of behaviour. |
| `behaviorType` | `String` | — | Behaviour classification. |
| `severity` | `Float` | — | Severity score. |
| `timestamp` | `Long` | — | Event timestamp. |
| `date` | `Date?` | — | Calendar date bucket. |
| `updatedAt` | `Date?` | — | Last modification time. |
| `updated` | `Boolean` | Defaults to `false` | Indicates local modification pending sync. |
| `processed` | `Boolean` | Defaults to `false` | Downstream analytics flag. |
| `sync` | `Boolean` | Defaults to `false` | Upload status. |

Indices span foreign keys, sync flag, date, and unique id for efficient retrieval.【F:core/src/main/java/com/uoa/core/database/entities/UnsafeBehaviourEntity.kt†L10-L41】

## CauseEntity (`causes`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `id` | `UUID` | Primary key | Individual contributing factor identifier. |
| `unsafeBehaviourId` | `UUID` | FK → `unsafe_behaviour.id` (cascade delete) | Links cause to parent unsafe behaviour. |
| `name` | `String` | — | Cause label. |
| `influence` | `Boolean?` | — | Whether the cause directly influenced the incident. |
| `createdAt` | `String` | — | Creation timestamp string from API. |
| `updatedAt` | `String?` | — | Optional update timestamp string. |

Indexed on `unsafeBehaviourId` for fast parent lookups.【F:core/src/main/java/com/uoa/core/database/entities/CauseEntity.kt†L9-L28】

## DrivingTipEntity (`driving_tips`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `tipId` | `UUID` | Primary key | Unique identifier for generated tip. |
| `driverProfileId` | `UUID` | FK → `driver_profile.driverProfileId` (cascade delete) | Owner of the tip. |
| `title` | `String` | — | Tip headline. |
| `meaning` | `String?` | Defaults to `null` | Explanation of the behaviour. |
| `penalty` | `String?` | Defaults to `null` | Penalty information. |
| `fine` | `String?` | Defaults to `null` | Monetary fine if applicable. |
| `law` | `String?` | Defaults to `null` | Legal citation. |
| `hostility` | `String?` | Defaults to `null` | Hostility level indicator. |
| `summaryTip` | `String?` | Defaults to `null` | Concise advice. |
| `sync` | `Boolean` | Defaults to `false` | Upload status. |
| `date` | `LocalDate` | — | Date tip was generated. |
| `llm` | `String?` | Defaults to `null` | Source LLM identifier. |

Indices ensure uniqueness for `tipId` and cover driver/date queries.【F:core/src/main/java/com/uoa/core/database/entities/DrivingTipEntity.kt†L1-L33】

## RoadEntity (`roads`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `id` | `UUID` | Primary key | Road segment identifier. |
| `driverProfileId` | `UUID` | FK → `driver_profile.driverProfileId` | Associates road metadata with a driver. |
| `name` | `String` | — | Road name label. |
| `roadType` | `String` | — | Road classification. |
| `speedLimit` | `Int` | — | Governing speed limit. |
| `latitude` | `Double` | — | Centre latitude of geofence. |
| `longitude` | `Double` | — | Centre longitude. |
| `radius` | `Double` | — | Geofence radius in metres. |
| `sync` | `Boolean` | Defaults to `false` | Upload state. |

Indices cover geospatial columns, radius, sync state, and driver reference.【F:core/src/main/java/com/uoa/core/database/entities/RoadEntity.kt†L1-L34】

## LocationEntity (`location`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `id` | `UUID` | Primary key | Location sample identifier. |
| `latitude` | `Double` | — | Recorded latitude. |
| `longitude` | `Double` | — | Recorded longitude. |
| `timestamp` | `Long` | — | Epoch milliseconds of capture. |
| `date` | `Date?` | — | Date bucket. |
| `altitude` | `Double` | — | Altitude above sea level. |
| `speed` | `Float` | — | Instantaneous speed. |
| `distance` | `Float` | — | Distance covered from reference point. |
| `speedLimit` | `Double` | — | Speed limit applied to the road segment. |
| `processed` | `Boolean` | Defaults to `false` | Downstream processing flag. |
| `sync` | `Boolean` | Defaults to `false` | Upload status. |

Unique index on `id` enforces entity identity.【F:core/src/main/java/com/uoa/core/database/entities/LocationEntity.kt†L1-L35】

## NLGReportEntity (`nlg_report`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `id` | `UUID` | Primary key | Report identifier. |
| `userId` | `UUID` | FK → `driver_profile.driverProfileId` (cascade delete) | Owner of the narrative report. |
| `tripId` | `UUID?` | Nullable | Optional reference to related trip. |
| `reportText` | `String` | — | Generated narrative summary. |
| `startDate` | `LocalDateTime?` | Defaults to `null` | Beginning of reporting window. |
| `endDate` | `LocalDateTime?` | Defaults to `null` | End of reporting window. |
| `createdDate` | `LocalDateTime` | — | Report generation timestamp. |
| `sync` | `Boolean` | Defaults to `false` | Upload status. |

Indices cover `id` (unique) and `userId` for retrieval by driver.【F:core/src/main/java/com/uoa/core/database/entities/NLGReportEntity.kt†L1-L29】

## ReportStatisticsEntity (`report_statistics`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `id` | `UUID` | Primary key | Statistics record identifier. |
| `driverProfileId` | `UUID` | FK → `driver_profile.driverProfileId` (cascade delete) | Owner of the aggregated statistics. |
| `tripId` | `UUID?` | Nullable | Optional associated trip. |
| `createdDate` | `LocalDate` | — | Creation day. |
| `startDate` | `LocalDate?` | Nullable | Aggregation window start. |
| `endDate` | `LocalDate?` | Nullable | Aggregation window end. |
| `totalIncidences` | `Int` | Defaults to `0` | Count of unsafe behaviours. |
| `mostFrequentUnsafeBehaviour` | `String?` | Defaults to `null` | Behaviour appearing most often. |
| `mostFrequentBehaviourCount` | `Int` | Defaults to `0` | Count of the most frequent behaviour. |
| `mostFrequentBehaviourOccurrences` | `String` | Defaults to `"[]"` | JSON (converted) list of behaviour occurrences. |
| `tripWithMostIncidences` | `String?` | Defaults to `null` | JSON representation of trip with most incidents. |
| `tripsPerAggregationUnit` | `String` | Defaults to `"{}"` | JSON map of trips per period. |
| `aggregationUnitWithMostIncidences` | `LocalDate?` | Defaults to `null` | Period with highest incidents. |
| `incidencesPerAggregationUnit` | `String` | Defaults to `"{}"` | JSON map of incidents per period. |
| `incidencesPerTrip` | `String` | Defaults to `"{}"` | JSON map of incidents per trip. |
| `aggregationLevel` | `AggregationLevel?` | Defaults to `null` | Chosen aggregation granularity. |
| `aggregationUnitsWithAlcoholInfluence` | `Int` | Defaults to `0` | Count of units with alcohol influence. |
| `tripsWithAlcoholInfluencePerAggregationUnit` | `String` | Defaults to `"{}"` | JSON map of alcohol-influenced trips per period. |
| `sync` | `Boolean` | Defaults to `false` | Upload flag. |
| `processed` | `Boolean` | Defaults to `false` | Downstream processing flag. |
| `numberOfTrips` | `Int` | Defaults to `0` | Trips in window. |
| `numberOfTripsWithIncidences` | `Int` | Defaults to `0` | Trips that had incidents. |
| `numberOfTripsWithAlcoholInfluence` | `Int` | Defaults to `0` | Trips with alcohol influence. |
| `lastTripDuration` | `String?` | Defaults to `null` | Duration of last trip (converted). |
| `lastTripDistance` | `Double?` | Defaults to `null` | Distance of last trip. |
| `lastTripAverageSpeed` | `Double?` | Defaults to `null` | Average speed of last trip. |
| `lastTripStartLocation` | `String?` | Defaults to `null` | Start location description. |
| `lastTripEndLocation` | `String?` | Defaults to `null` | End location description. |
| `lastTripStartTime` | `LocalDateTime?` | Defaults to `null` | Start time of last trip. |
| `lastTripEndTime` | `LocalDateTime?` | Defaults to `null` | End time of last trip. |
| `lastTripInfluence` | `String?` | Defaults to `null` | Alcohol influence on last trip. |

Indices cover `startDate`, `endDate`, `driverProfileId`, and unique `id` for reporting lookups.【F:core/src/main/java/com/uoa/core/database/entities/ReportStatisticsEntity.kt†L1-L74】

## QuestionnaireEntity (`questionnaire_responses`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `id` | `UUID` | Primary key | Questionnaire submission identifier. |
| `driverProfileId` | `UUID` | FK → `driver_profile.driverProfileId` (cascade delete) | Responding driver. |
| `drankAlcohol` | `Boolean` | — | Indicates alcohol consumption. |
| `selectedAlcoholTypes` | `String` | — | Stored list of chosen alcohol types. |
| `beerQuantity` | `String` | — | Reported beer quantity. |
| `wineQuantity` | `String` | — | Reported wine quantity. |
| `spiritsQuantity` | `String` | — | Reported spirits quantity. |
| `firstDrinkTime` | `String` | — | First drink time string. |
| `lastDrinkTime` | `String` | — | Last drink time string. |
| `emptyStomach` | `Boolean` | — | Whether drinking occurred on empty stomach. |
| `caffeinatedDrink` | `Boolean` | — | Whether caffeine was consumed. |
| `impairmentLevel` | `Int` | — | Self-assessed impairment level. |
| `date` | `Date` | — | Submission date. |
| `plansToDrive` | `Boolean` | — | Whether respondent planned to drive. |
| `sync` | `Boolean` | Defaults to `false` | Upload flag. |

Indices on `id` and `driverProfileId` aid lookups per driver.【F:core/src/main/java/com/uoa/core/database/entities/QuestionnaireEntity.kt†L1-L35】

## EmbeddingEntity (`embeddings`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `chunkId` | `UUID` | Primary key | Identifier for an embedded text chunk. |
| `chunkText` | `String` | — | Stored text content. |
| `embedding` | `ByteArray` | — | Serialized embedding vector. |
| `sourceType` | `String` | — | Provenance category (e.g., legal corpus). |
| `sourcePage` | `Int?` | Nullable | Optional page reference. |
| `createdAt` | `Long` | — | Creation timestamp (epoch millis). |

Overrides `equals`/`hashCode` to account for byte-array comparisons.【F:core/src/main/java/com/uoa/core/database/entities/EmbeddingEntity.kt†L1-L38】

## FFTFeatureEntity (`fft_features`)

| Column | Kotlin type | Constraints / Defaults | Notes |
| --- | --- | --- | --- |
| `id` | `Long` | Primary key (auto-generated) | Identifier for stored spectral feature row. |
| `timestamp` | `Long` | — | Capture timestamp. |
| `label` | `String` | — | Feature label/class. |
| `energy` | `Double` | — | Frequency-domain energy. |
| `dominantFrequency` | `Double` | — | Dominant frequency component. |
| `entropy` | `Double` | — | Spectral entropy metric. |

Standalone table without foreign keys for frequency-domain summaries.【F:core/src/main/java/com/uoa/core/database/entities/FFTFeatureEntity.kt†L1-L14】

## Relational Overview

* Cascading deletes propagate from `driver_profile` to dependent tables such as trips, unsafe behaviours, AI model inputs, raw sensor data, driving tips, roads, reports, questionnaire responses, and NLG reports, ensuring referential integrity when profiles are removed.【F:core/src/main/java/com/uoa/core/database/entities/TripEntity.kt†L10-L35】【F:core/src/main/java/com/uoa/core/database/entities/AIModelInputsEntity.kt†L11-L44】【F:core/src/main/java/com/uoa/core/database/entities/UnsafeBehaviourEntity.kt†L10-L41】【F:core/src/main/java/com/uoa/core/database/entities/DrivingTipEntity.kt†L1-L33】【F:core/src/main/java/com/uoa/core/database/entities/RoadEntity.kt†L1-L34】【F:core/src/main/java/com/uoa/core/database/entities/NLGReportEntity.kt†L1-L29】【F:core/src/main/java/com/uoa/core/database/entities/ReportStatisticsEntity.kt†L1-L74】【F:core/src/main/java/com/uoa/core/database/entities/QuestionnaireEntity.kt†L1-L35】
* `CauseEntity` depends on `unsafe_behaviour` records and inherits cascade deletes to keep causal factors aligned with behaviour lifecycle.【F:core/src/main/java/com/uoa/core/database/entities/CauseEntity.kt†L9-L28】
* `RawSensorDataEntity` and `UnsafeBehaviourEntity` optionally reference `LocationEntity` rows, enabling enriched spatial analytics without enforcing mandatory location capture.【F:core/src/main/java/com/uoa/core/database/entities/RawSensorDataEntity.kt†L19-L52】【F:core/src/main/java/com/uoa/core/database/entities/UnsafeBehaviourEntity.kt†L10-L41】【F:core/src/main/java/com/uoa/core/database/entities/LocationEntity.kt†L1-L35】
