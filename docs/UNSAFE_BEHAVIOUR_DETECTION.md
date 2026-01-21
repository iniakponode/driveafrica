# Unsafe Behaviour Detection Guide

This document describes how unsafe driving behaviours are detected in the app, including the data sources, units, conversions, and thresholds. The implementation lives in `core/src/main/java/com/uoa/core/behaviouranalysis/NewUnsafeDrivingBehaviourAnalyser.kt`.

## Summary for backend analytics
- **Event stream**: `unsafe_behaviours` uploads individual events with severity (0-1), timestamp, and `behaviorType`.
- **Aggregates**: trip-level counts are uploaded via `trip_summaries.unsafeBehaviourCounts` and normalized rows in `trip_summary_behaviours`.
- **Units**: acceleration in m/s^2, gyro in rad/s, speed in m/s, timestamps in epoch ms.
- **Speed limits**: parsed in km/h (OSM), converted to m/s before storage and detection.
- **Behavior types**: Harsh Acceleration, Harsh Braking, Harsh Cornering, Aggressive Turn, Aggressive Stop-and-Go, Swerving, Speeding, Rough Road Speeding, Phone Handling, Fatigue, Crash Detected.

## Data inputs and units
- **RawSensorDataEntity** (per-sample sensor events)
  - Accelerometer / Linear acceleration values: m/s^2
  - Gyroscope values: rad/s
  - Rotation vector: unitless rotation vector used to build a rotation matrix
  - Timestamp: epoch ms
- **LocationData**
  - Speed: m/s (Android `Location.speed`)
  - Distance: meters
  - Accuracy: meters (smaller is better)
  - Speed limit: m/s (see conversion below)

## Speed limit conversions
- OpenStreetMap speed limits are parsed as **km/h** (and converted from mph to km/h when needed).
  - Source: `core/src/main/java/com/uoa/core/utils/UtilFunctions.kt` (`parseSpeedLimitKmh`).
- The speed limit is converted to **m/s** before being stored on `LocationData`.
  - Conversion: `km/h * 0.277778` (see `sensor/src/main/java/com/uoa/sensor/location/LocationManager.kt`).

## Preprocessing and signal alignment
- **Sliding windows** are maintained per trip for acceleration, rotation, and speed.
  - Short window size: 200 samples (accelerometer + rotation).
  - Long rotation window: 3000 samples (used for fatigue detection).
- **Coordinate transform**:
  - Rotation vector samples generate a rotation matrix.
  - Acceleration is rotated into the earth frame and then into the vehicle frame using heading.
  - Heading is derived from location bearings when GPS accuracy and speed are acceptable.
- **Speed estimation**:
  - Weighted average of location speeds (weights based on GPS accuracy).
  - Fallback: accelerometer magnitude * `SENSOR_SAMPLE_INTERVAL_S` (0.02 s).
- **Dynamic thresholds**:
  - Accel/brake thresholds are reduced at higher speeds and adjusted for rough road noise.
  - Pothole-like signatures veto harsh braking (Z-axis variability).

## Detected behaviours (canonical names)
The strings below are the exact `behaviorType` values stored and uploaded.

### 1) Harsh Acceleration
- **Signal**: vehicle-frame forward acceleration (m/s^2).
- **Rule**: forward accel > dynamic threshold.
- **Thresholds**:
  - Base: `ACCELERATION_THRESHOLD = 4.0 m/s^2`
  - Min: `MIN_ACCELERATION_THRESHOLD = 1.5 m/s^2`
  - Speed adjustment: reduce by 0.5 for every 10 m/s above 10 m/s.
  - Roughness adjustment: `Z-stddev * 0.35` (capped at `3.0`).
- **Gates**: speed >= 1.4 m/s, duration >= 300 ms, cooldown 1.5 s.

### 2) Harsh Braking
- **Signal**: vehicle-frame forward acceleration (m/s^2).
- **Rule**: forward accel < -dynamic threshold.
- **Thresholds**:
  - Base: `BRAKING_THRESHOLD = -4.5 m/s^2`
  - Min: `MIN_BRAKING_THRESHOLD = 1.5 m/s^2`
- **Pothole veto**: Z-stddev > 2.0 or Z peak-to-peak > 5.0 within 500 ms.
- **Gates**: speed >= 1.4 m/s, duration >= 300 ms, cooldown 1.5 s.

### 3) Harsh Cornering
- **Signal**: lateral acceleration magnitude (m/s^2).
- **Rule**: lateral accel > 4.9 m/s^2.
- **Gates**: speed >= 5.56 m/s, duration >= 1 s, cooldown 3 s.

### 4) Aggressive Turn
- **Signals**: heading change + lateral acceleration.
- **Rule**: heading delta >= 60 degrees within 2 s AND lateral accel >= 3.5 m/s^2.
- **Gates**: speed >= 5.56 m/s, cooldown 5 s.

### 5) Aggressive Stop-and-Go
- **Signal**: jerk (delta forward accel / dt).
- **Rule**: abs(jerk) >= 10.0 m/s^3.
- **Gates**: speed >= 4.17 m/s, cooldown 5 s.

### 6) Swerving
- **Signal**: gyroscope yaw rate.
- **Rule**: max yaw rate > 0.14 rad/s with sign-change pattern within 1.5 s.
- **Gates**: speed >= 5.56 m/s, cooldown 2 s.

### 7) Speeding
- **Signal**: average speed (m/s) from location (weighted) or accel fallback.
- **Rule**: avgSpeed > speedLimit * 1.10 for 20 s.
- **Speed limit**:
  - Use location speed limit when available.
  - Fallback: 16.67 m/s (urban) or 30.56 m/s (highway) when speed >= 22.22 m/s.
- **Gates**: duration >= 20 s, cooldown 10 s.

### 8) Rough Road Speeding
- **Signal**: road quality indicator (RQI) and speed.
- **Rule**: RQI >= 2.0 AND avgSpeed >= 22.22 m/s.
- **Gates**: cooldown 10 s.

### 9) Phone Handling
- **Signal**: gyroscope magnitude variance.
- **Rule**: variance >= 0.15 over 2.5 s (min 20 samples).
- **Gates**: speed >= 2.78 m/s, cooldown 10 s.

### 10) Fatigue
- **Signals**: yaw reversal rate + yaw magnitude.
- **Rule**:
  - Low-pass yaw (2 Hz), reversal rate <= 0.5 Hz, max yaw >= 0.15 rad/s.
  - Road roughness must be low (RQI <= 2.0).
  - Time gate: circadian dip (02-06 or 14-16) OR continuous driving >= 4.5 h.
- **Gates**: speed >= 11.11 m/s, window >= 30 s, cooldown 10 min.

### 11) Crash Detected
- **Signal**: acceleration spike + delta-v + post-event speed.
- **Rule**:
  - Accel magnitude >= 39.24 m/s^2, delta-v >= 1.94 m/s over 120 ms.
  - Not in free-fall (<= 1.0 m/s^2 for >= 300 ms).
  - Confirmed if avg speed <= 1.39 m/s after 30 s.
- **Gates**: candidate TTL 60 s, cooldown 60 s.

## Severity normalization (0.0 to 1.0)
Severity is computed as `(measured - threshold) / maxExcess`, clamped to [0, 1].

Max excess values:
- Harsh Acceleration: 5.5 m/s^2
- Harsh Braking: 5.5 m/s^2
- Harsh Cornering: 4.0 m/s^2
- Aggressive Turn: 4.0 m/s^2
- Aggressive Stop-and-Go: 15.0 m/s^3
- Swerving: 0.5 rad/s
- Speeding: 2.778 m/s (over speed limit)
- Rough Road Speeding: 8.33 m/s (over 22.22 m/s)
- Phone Handling: 0.35 variance
- Fatigue: 0.4 rad/s
- Crash Detected: 5.0 m/s (delta-v over 1.94 m/s)

## Outputs and storage
- Each detected event becomes an `UnsafeBehaviourModel` with `tripId`, `driverProfileId`, timestamp, severity, and `behaviorType`.
- Events are stored locally as `UnsafeBehaviourEntity` and uploaded via `/api/unsafe_behaviours/batch_create`.
- Aggregated counts are stored in `TripSummary.unsafeBehaviourCounts` and uploaded:
  - As part of `TripSummaryCreate` (`/api/trip_summaries/batch_create`).
  - As normalized rows in `TripSummaryBehaviourCreate` (`/api/trip_summary_behaviours/batch_create`).

## Reference files
- Detection logic: `core/src/main/java/com/uoa/core/behaviouranalysis/NewUnsafeDrivingBehaviourAnalyser.kt`
- Speed limit parsing: `core/src/main/java/com/uoa/core/utils/UtilFunctions.kt`
- Location speed limit conversion: `sensor/src/main/java/com/uoa/sensor/location/LocationManager.kt`
- Upload pipeline: `core/src/main/java/com/uoa/core/apiServices/workManager/UploadAllDataWorker.kt`
