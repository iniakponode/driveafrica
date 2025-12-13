# 🧪 QUICK TEST GUIDE - Vehicle Detection

## Build & Install

```powershell
# Clean build
./gradlew clean assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Test Procedure

### 1. Start the App
- Launch SafeDrive Africa
- VehicleMovementService should start automatically
- Check status shows: **"Waiting for motion"**

### 2. Place Phone in Vehicle
- Put phone in door handle or cup holder
- Should stay: **"Waiting for motion"** (IDLE state)
- GPS should be **OFF** (battery save)

### 3. Start Driving
**Expected sequence:**

**0-5 seconds** (accelerating):
- Status: **"Waiting for motion"**
- Internal: Detecting smooth motion pattern
- **Logs show**: Motion analysis with variance readings

**~5 seconds** (sustained smooth motion):
- Status changes to: **"Verifying vehicle motion..."**
- GPS turns **ON**
- Checking speed
- **Logs show**: "✅ VEHICLE MOTION DETECTED"

**~10-15 seconds** (GPS gets speed reading):
- If speed > 9 mph (14.4 km/h):  **← NEW LOWER THRESHOLD**
  - Status: **"Recording trip"**
  - ✅ **TRIP STARTS AUTOMATICALLY**
  - Data collection begins
  - **Logs show**: Speed in mph, km/h, and m/s for dashboard comparison

### 4. Continue Driving
- Status: **"Recording trip"**
- Should stay stable
- Data being collected

### 5. Stop at Red Light
- Vehicle stops for 1 minute
- Status: **"Vehicle stopped (traffic?)"**
- Trip **continues** (doesn't end)

### 6. Light Turns Green
- Start driving again
- Status: **"Recording trip"**
- Trip resumes normally

### 7. Park Vehicle
- Stop for 3+ minutes
- After 3 minutes:
  - Status: **"Waiting for motion"**
  - Trip ends automatically
  - GPS turns **OFF**

---

## Monitor Logs

```bash
adb logcat | Select-String -Pattern "DrivingStateManager|HardwareModule"
```

### Expected Output (NEW - With Detailed Readings):

```
# App starts
I/HardwareModule: Starting smart motion detection with FSM
I/DrivingStateManager: DrivingStateManager initialized
I/DrivingStateManager: State Transition: null -> IDLE

# Monitoring motion (every 2 seconds in IDLE)
I/DrivingStateManager: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
I/DrivingStateManager: 🔍 MOTION ANALYSIS (IDLE State):
I/DrivingStateManager:    Variance:     0.456 m/s²
I/DrivingStateManager:    Vehicle Range: 0.15 - 1.50 m/s²
I/DrivingStateManager:    Walking Threshold: > 2.50 m/s²
I/DrivingStateManager:    Classification: ✅ VEHICLE MOTION DETECTED
I/DrivingStateManager:    Timer: 3.2 / 5.0 seconds
I/DrivingStateManager: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

# Transition to verifying
I/DrivingStateManager: IDLE -> VERIFYING: Sustained smooth motion detected
I/HardwareModule: 📍 Enabling GPS for verification

# GPS readings (compare with dashboard!)
I/DrivingStateManager: ═══════════════════════════════════════════════════════
I/DrivingStateManager: 📍 GPS UPDATE:
I/DrivingStateManager:    Speed (m/s):  6.94 m/s
I/DrivingStateManager:    Speed (km/h): 25.0 km/h
I/DrivingStateManager:    Speed (mph):  15.5 mph ⬅ COMPARE WITH DASHBOARD
I/DrivingStateManager:    Accuracy:     8.5 meters
I/DrivingStateManager:    State:        VERIFYING
I/DrivingStateManager:    Threshold:    > 9.0 mph (14.4 km/h) to confirm vehicle
I/DrivingStateManager: ═══════════════════════════════════════════════════════

# Vehicle confirmed
I/DrivingStateManager: 
I/DrivingStateManager: ✅ VEHICLE CONFIRMED!
I/DrivingStateManager:    Current Speed: 15.5 mph (25.0 km/h)
I/DrivingStateManager:    Threshold: > 9.0 mph
I/DrivingStateManager:    ➡️  VERIFYING -> RECORDING
I/DrivingStateManager:    🚗 Starting trip automatically...
I/DrivingStateManager: 
I/HardwareModule: 🚗 Drive detected and confirmed - Starting trip

# Continuous GPS updates while driving
I/DrivingStateManager: ═══════════════════════════════════════════════════════
I/DrivingStateManager: 📍 GPS UPDATE:
I/DrivingStateManager:    Speed (m/s):  13.89 m/s
I/DrivingStateManager:    Speed (km/h): 50.0 km/h
I/DrivingStateManager:    Speed (mph):  31.1 mph ⬅ COMPARE WITH DASHBOARD
I/DrivingStateManager:    Accuracy:     6.2 meters
I/DrivingStateManager:    State:        RECORDING
I/DrivingStateManager: ═══════════════════════════════════════════════════════

# Stop at light
I/DrivingStateManager: 
I/DrivingStateManager: ⚠️ VEHICLE SLOWING/STOPPED
I/DrivingStateManager:    Speed: 2.1 mph (3.4 km/h)
I/DrivingStateManager:    Below threshold: < 3.1 mph
I/DrivingStateManager:    ➡️  RECORDING -> POTENTIAL_STOP
I/DrivingStateManager:    ⏱  Starting 3-minute parking timer...
I/DrivingStateManager: 

# Resume driving
I/DrivingStateManager: 
I/DrivingStateManager: 🚦 MOTION RESUMED (Traffic light/Stop sign)
I/DrivingStateManager:    Speed: 12.4 mph (20.0 km/h)
I/DrivingStateManager:    Above threshold: > 9.0 mph
I/DrivingStateManager:    ➡️  POTENTIAL_STOP -> RECORDING
I/DrivingStateManager:    🚗 Continuing trip...
I/DrivingStateManager: 

# Park (after 3 minutes)
I/DrivingStateManager: POTENTIAL_STOP -> IDLE: Vehicle parked (3 min timeout)
I/HardwareModule: 🛑 Drive ended - Vehicle parked
I/HardwareModule: 📍 Disabling GPS (battery save)
```

### 📊 Key Readings to Watch:

1. **Variance (m/s²)** - Shows motion smoothness
   - 0.15-1.5 = Vehicle motion
   - > 2.5 = Walking/running

2. **Speed (mph)** - Compare with your dashboard
   - Threshold: > 9 mph to start trip
   - Should match dashboard within ±2 mph

3. **GPS Accuracy** - Should be < 15 meters for good readings

4. **State Transitions** - Watch for clean transitions:
   - IDLE → VERIFYING → RECORDING
   - RECORDING → POTENTIAL_STOP → RECORDING (traffic light)
   - POTENTIAL_STOP → IDLE (parking)

---

## Troubleshooting

### Issue: Still shows "Waiting for motion"

**Check:**
1. Is VehicleMovementService running?
2. Are you driving > 9 mph (14.4 km/h)? **← NEW LOWER THRESHOLD**
3. Check logs for variance and speed values

**What to Look For in Logs:**
- Variance should be 0.15-1.5 m/s² for vehicle
- Speed should show in mph matching your dashboard
- Should see "✅ VEHICLE MOTION DETECTED" message

**Try:**
- Drive at 15+ mph to be well above threshold
- Place phone more securely (less vibration)
- Check logs show GPS updates with speed readings
- Verify GPS accuracy is < 15 meters

### Issue: Takes too long to detect

**Adjust** in DrivingStateManager.kt:
```kotlin
private const val SMOOTH_MOTION_DURATION_MS = 3_000L  // Reduce from 5 to 3 sec
private const val SPEED_VEHICLE_THRESHOLD = 3.5  // Lower from 4.17 (~12 km/h)
```

### Issue: GPS not getting lock

**Check:**
- Location permissions granted
- GPS enabled on device
- Drive in open area (not underground/tunnel)

---

## Success Criteria

✅ Status changes from "Waiting" → "Verifying" → "Recording"
✅ Trip starts automatically (no manual trigger)
✅ Works at speeds 15-35 mph
✅ Doesn't end at red lights
✅ Ends after parking 3+ minutes
✅ GPS turns off when not needed

---

## Quick Checklist

- [ ] App launches successfully
- [ ] VehicleMovementService running
- [ ] Place phone in vehicle
- [ ] Shows "Waiting for motion"
- [ ] Start driving (15+ mph)
- [ ] Changes to "Verifying" after ~5 sec
- [ ] GPS turns on
- [ ] Changes to "Recording" after GPS confirms
- [ ] Trip created automatically
- [ ] Status stable while driving
- [ ] Red light doesn't end trip
- [ ] Parks and ends after 3 min

---

**If all checks pass: ✅ VEHICLE DETECTION IS WORKING!**

Build, install, and test it now! 🚗

