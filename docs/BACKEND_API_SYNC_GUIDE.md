# Backend API Sync/Upgrade Guide

This document maps the mobile app’s upload pipeline to backend API requirements and highlights missing or optional endpoints. It is written for backend developers to verify compatibility or implement gaps.

## Short answer (trip stats + trip summary)
- **Trip stats:** Yes. Trip stats are uploaded via `ReportStatisticsCreate` to `/api/report_statistics/` and `/api/report_statistics/batch_create`.
- **Trip summary:** Yes. Trip summaries are uploaded via `TripSummaryCreate` to `/api/trip_summaries/` and `/api/trip_summaries/batch_create`.
- **Unsafe behaviour counts:** Yes. Counts are uploaded twice: as `TripSummaryCreate.unsafeBehaviourCounts` and as normalized rows via `/api/trip_summary_behaviours/batch_create`.
- **Trip summary behaviours:** Yes. Normalized behaviour rows are uploaded via `TripSummaryBehaviourCreate` to `/api/trip_summary_behaviours/batch_create`.
- **Trip feature states:** Yes. Trip feature aggregates are uploaded via `TripFeatureStateCreate` to `/api/trip_feature_states/batch_create`.

---

## 1) Where uploads happen
The authoritative upload pipeline is `UploadAllDataWorker`:
`core/src/main/java/com/uoa/core/apiServices/workManager/UploadAllDataWorker.kt`

Upload order (dependencies enforced in sequence):
1) Driver profile registration (auth)
2) Trips
3) Locations
4) Raw sensor data
5) Unsafe behaviours
6) Trip summary behaviours
7) Trip summaries
8) Trip feature states
9) Questionnaires
10) Driving tips
11) NLG reports
12) AI model inputs
13) Report statistics (trip stats)
14) Roads

Trips are ensured before raw sensor uploads (`ensureTripExists`), and unsafe behaviours reference trips/locations.

---

## 2) Required backend endpoints (upload-facing)
Each section lists the endpoint(s), payload model(s), and notes. Models live in `core/src/main/java/com/uoa/core/apiServices/models/**`.

### Auth + Driver Profile
- `POST /api/auth/driver/register`
  - Payload: `auth/RegisterRequest`
  - Must accept `driverProfileId`, `email`, `password`, `sync`.
- `POST /api/auth/driver/login`
  - Payload: `auth/LoginRequest`
- `GET /api/auth/driver/me`
  - Returns: `driverProfile/DriverProfileResponse`

Optional driver profile CRUD (not used in upload worker, but used in app features):
- `POST /api/driver_profiles/`
- `GET /api/driver_profiles/{profile_id}`
- `GET /api/driver_profiles/`
- `PUT /api/driver_profiles/{profile_id}`
- `DELETE /api/driver_profiles/{profile_id}`
- Batch: `/api/driver_profiles/batch_create`, `/api/driver_profiles/batch_delete`
  - Payload: `driverProfile/DriverProfileCreate`

### Trips
- `POST /api/trips/`
- `GET /api/trips/{trip_id}`
- `PUT /api/trips/{trip_id}`
- `POST /api/trips/batch_create`
- `DELETE /api/trips/{trip_id}`
- `DELETE /api/trips/batch_delete` (HTTP DELETE with body)
  - Payload: `tripModels/TripCreate`

Notes:
- `TripCreate` sends **both** camelCase and snake_case fields. Backend should accept either (ignore duplicates).
- On create failure (400/409), the app attempts update; backend should allow idempotent updates.

### Locations
- `POST /api/locations/`
- `POST /api/locations/batch_create`
- `GET /api/locations/{location_id}`
- `PUT /api/locations/{location_id}`
- `DELETE /api/locations/{location_id}`
  - Payload: `locationModels/LocationCreate`

### Raw Sensor Data
- `POST /api/raw_sensor_data/`
- `POST /api/raw_sensor_data/batch_create`
- `GET /api/raw_sensor_data/{data_id}`
- `PUT /api/raw_sensor_data/{data_id}`
- `DELETE /api/raw_sensor_data/{data_id}`
  - Payload: `rawSensorModels/RawSensorDataCreate`

Notes:
- `trip_id` and `driverProfileId` are required.
- `date` is an ISO-8601 string in UTC.

### Unsafe Behaviours
- `POST /api/unsafe_behaviours/`
- `POST /api/unsafe_behaviours/batch_create`
- `GET /api/unsafe_behaviours/{behaviour_id}`
- `PUT /api/unsafe_behaviours/{behaviour_id}`
- `DELETE /api/unsafe_behaviours/{behaviour_id}`
  - Payload: `unsafeBehaviourModels/UnsafeBehaviourCreate`

### Questionnaires
- `POST /api/questionnaires/`
- `POST /api/questionnaires/batch_create`
- `GET /api/questionnaires/{userId}`
  - Payload: `alcoholquestionnaireModels/AlcoholQuestionnaireCreate`

Legacy endpoints (still used in app):
- `POST /api/questionnaire/`
- `POST /api/questionnaire/batch_create`
- `GET /api/questionnaire/{userId}`

### Driving Tips
- `POST /api/driving_tips/`
- `POST /api/driving_tips/batch_create`
- `GET /api/driving_tips/{tip_id}`
- `PUT /api/driving_tips/{tip_id}`
- `DELETE /api/driving_tips/{tip_id}`
  - Payload: `drivingTipModels/DrivingTipCreate`

### NLG Reports
- `POST /api/nlg_reports/`
- `POST /api/nlg_reports/batch_create`
- `GET /api/nlg_reports/{report_id}`
- `PUT /api/nlg_reports/{report_id}`
- `DELETE /api/nlg_reports/{report_id}`
  - Payload: `nlgReportModels/NLGReportCreate`

### AI Model Inputs
- `POST /api/ai_model_inputs/`
- `POST /api/ai_model_inputs/batch_create`
- `GET /api/ai_model_inputs/{input_id}`
- `PUT /api/ai_model_inputs/{input_id}`
- `DELETE /api/ai_model_inputs/{input_id}`
  - Payload: `aiModelInputModels/AIModelInputCreate`

### Report Statistics (Trip Stats)
- `POST /api/report_statistics/`
- `POST /api/report_statistics/batch_create`
- `GET /api/report_statistics/{report_id}`
- `PUT /api/report_statistics/{report_id}`
- `DELETE /api/report_statistics/{report_id}`
  - Payload: `reportStatisticsModels/ReportStatisticsCreate`

Notes:
- `startDate`, `endDate`, `createdDate` are ISO-8601 dates (yyyy-MM-dd).
- `lastTripStartTime`/`lastTripEndTime` are ISO-8601 date-times.
- `lastTripDuration` uses Kotlin `Duration` serialization from a parsed Java `Duration`. Backend should accept ISO-8601 durations (e.g., `PT180S`) or ignore if unsupported.
- Several fields are maps: `Map<LocalDate, Int>` and `Map<UUID, Int>` serialized as JSON objects (string keys).

### Trip Summaries
- `POST /api/trip_summaries/`
- `POST /api/trip_summaries/batch_create`
- `GET /api/trip_summaries/{trip_id}` (optional)
- `PUT /api/trip_summaries/{trip_id}` (optional)
- `DELETE /api/trip_summaries/{trip_id}` (optional)
  - Payload: `tripSummaryModels/TripSummaryCreate`

Notes:
- `startDate`/`endDate` are ISO-8601 date-time strings (UTC).
- `unsafeBehaviourCounts` is a JSON map keyed by behaviour type (counts are duplicated in `trip_summary_behaviours` for normalized analytics).

### Trip Summary Behaviours (normalized)
- `POST /api/trip_summary_behaviours/`
- `POST /api/trip_summary_behaviours/batch_create`
  - Payload: `tripSummaryModels/TripSummaryBehaviourCreate`

Notes:
- `tripId` ties the row to the parent trip summary.
- Only non-zero counts are sent.
- Rows are derived from the `unsafeBehaviourCounts` map on each trip summary.

Example payload:
```
{
  "tripId": "...",
  "behaviourType": "hard_brake",
  "count": 2
}
```

### Trip Feature States
- `POST /api/trip_feature_states/`
- `POST /api/trip_feature_states/batch_create`
  - Payload: `tripFeatureModels/TripFeatureStateCreate`

Notes:
- Aggregated stats used for trip-level ML/analytics.
- `lastLocationTimestamp`/`lastSensorTimestamp` are epoch millis.
- `driverProfileId` may be null for older rows.
- `sync` is sent as `true` on upload.

Example payload:
```
{
  "tripId": "...",
  "driverProfileId": "...",
  "accelCount": 120,
  "accelMean": 0.12,
  "speedCount": 240,
  "speedMean": 8.4,
  "speedM2": 13.6,
  "courseCount": 210,
  "courseMean": 124.5,
  "courseM2": 20.3,
  "lastLocationId": "...",
  "lastLatitude": 57.1467,
  "lastLongitude": -2.0943,
  "lastLocationTimestamp": 1700000000000,
  "lastSensorTimestamp": 1700000000050,
  "sync": true
}
```

### Roads
- `POST /api/roads/`
- `POST /api/roads/batch_create`
- `GET /api/roads/{road_id}`
- `PUT /api/roads/{road_id}`
- `DELETE /api/roads/{road_id}`
  - Payload: `roadModels/RoadCreate`

---

## 3) Fleet + membership endpoints (user-facing)
These are used in the UI/registration flow:
- `POST /api/driver-join/validate-code`
- `POST /api/driver/join-fleet`
- `POST /api/driver-join/join-with-code`
- `GET /api/driver/fleet-status`
- `DELETE /api/driver/join-request`

Models: `auth/JoinFleetRequest`, `auth/InviteCodeValidationRequest`, `auth/FleetStatusResponse`, `auth/JoinFleetResponse`.

---

## 4) Optional: Driver sync endpoint (currently unused)
There is a unified sync endpoint:
- `POST /api/driver/sync`
  - Payload: `driverSyncModels/DriverSyncPayload`

This accepts:
- `profile` (driver profile reference)
- `trips` (with distance/averageSpeed fields not present in `TripCreate`)
- `rawSensorData`
- `unsafeBehaviours`
- `alcoholResponses`

The current worker uses direct batch endpoints, not `driver/sync`. Backend can keep or improve this endpoint for future consolidation.

---

## 5) Optional: Alternative normalization
Trip summaries are uploaded both as a map and as normalized rows. If the backend
prefers **only one**, either ignore the normalized rows or drop the map field on
ingest. The client will continue to send both.

Recommended payload (mirrors `TripSummaryCreate`):
```
{
  "tripId": "...",
  "driverProfileId": "...",
  "startTime": 1700000000000,
  "endTime": 1700001234000,
  "startDate": "2026-01-20T10:00:00Z",
  "endDate": "2026-01-20T10:20:34Z",
  "distanceMeters": 12345.6,
  "durationSeconds": 1234,
  "classificationLabel": "safe|risky|...",
  "alcoholProbability": 0.12,
  "unsafeBehaviourCounts": {
    "hard_brake": 2,
    "phone_use": 1
  }
}
```

---

## 6) Serialization formats to expect
Common date/time formats used:
- `yyyy-MM-dd'T'HH:mm:ss'Z'` (UTC) for most date strings
- `yyyy-MM-dd` for LocalDate fields in report stats
- `ISO_DATE_TIME` for LocalDateTime fields

Notes:
- `UploadAllDataWorker` uses a formatter with `UTC+1` timezone for some dates; be tolerant of offsets.
- Trip payloads send both snake_case and camelCase fields.

---

## 7) Backend implementation checklist
- Accept/ignore unknown JSON fields (client may send both camel/snake keys).
- Make batch endpoints idempotent; return partial success details if possible.
- Return 404 for truly missing resources; client treats 400/409 as “update instead”.
- For report statistics, accept map keys as strings and parse to dates/UUIDs.
- Implement trip summary endpoints (`/api/trip_summaries/*`), trip summary behaviours
  (`/api/trip_summary_behaviours/*`), and trip feature states (`/api/trip_feature_states/*`).

---

## 8) Useful references in code
- Upload flow: `core/src/main/java/com/uoa/core/apiServices/workManager/UploadAllDataWorker.kt`
- API services: `core/src/main/java/com/uoa/core/apiServices/services/**`
- Models: `core/src/main/java/com/uoa/core/apiServices/models/**`
- Trip summary models: `core/src/main/java/com/uoa/core/model/TripSummary.kt`

---

## 9) Backend analytics appendix (unsafe behaviours)
For analytics and interpretation of uploaded unsafe behaviours (types, units,
thresholds, and severity normalization), see:
- `docs/UNSAFE_BEHAVIOUR_DETECTION.md`
```
