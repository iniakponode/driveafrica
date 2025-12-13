# 🚀 QUICK TEST GUIDE - Vehicle Detection System

## 📋 PRE-TEST CHECKLIST

Before testing, ensure:
- [ ] App is installed on device
- [ ] Location permissions granted
- [ ] Phone fully charged (or plugged in)
- [ ] Vehicle dashboard visible for speed comparison

---

## 🧪 TEST PROCEDURE

### **Test 1: App Launch (5 seconds)**

**Steps:**
1. Open the app
2. Check logcat immediately

**Expected Results:**
```
✅ "Starting movement detection on app launch"
✅ "DrivingStateManager initialized"
✅ "Starting vehicle motion monitoring"
```

**Status:** [ ] PASS [ ] FAIL

---

### **Test 2: Vehicle Detection Monitor Screen (10 seconds)**

**Steps:**
1. From HomeScreen → Click "🚗 Open Vehicle Monitor"
2. Observe the screen

**Expected Results:**
```
✅ Screen opens immediately
✅ Shows "State: IDLE"
✅ Shows "Variance: X.XXX m/s²"
✅ Shows "Speed: 0.0 mph"
✅ Shows "Yet to start trip"
```

**Status:** [ ] PASS [ ] FAIL

---

### **Test 3: Navigation from Record Trip Screen (5 seconds)**

**Steps:**
1. From HomeScreen → Click "Record Trip"
2. Click "🚗 Open Vehicle Monitor" button
3. Verify navigation works

**Expected Results:**
```
✅ Button is visible on Record Trip screen
✅ Clicking button navigates to Vehicle Monitor
✅ No crashes or errors
```

**Status:** [ ] PASS [ ] FAIL

---

### **Test 4: Stationary Detection (30 seconds)**

**Steps:**
1. Keep phone on table (stationary)
2. Open Vehicle Monitor
3. Watch for 30 seconds

**Expected Results:**
```
✅ State: IDLE
✅ Variance: < 0.15 m/s²
✅ Classification: "Stationary"
✅ Message: "Waiting for motion"
```

**Status:** [ ] PASS [ ] FAIL

---

### **Test 5: Walking Detection (30 seconds)**

**Steps:**
1. Pick up phone and walk around
2. Watch Vehicle Monitor screen

**Expected Results:**
```
✅ Variance: > 2.5 m/s²
✅ Classification: "Walking/Running"
✅ State remains: IDLE (not triggering vehicle detection)
✅ No trip starts
```

**Status:** [ ] PASS [ ] FAIL

---

### **Test 6: Vehicle Detection - Normal GPS (2 minutes)**

**Steps:**
1. Place phone in vehicle (good GPS signal location)
2. Open Vehicle Monitor screen
3. Start driving
4. Compare with dashboard speedometer

**Expected Timeline:**
```
⏱️ 0:00 - Start driving
⏱️ 0:05 - Smooth motion detected → State: VERIFYING
⏱️ 0:10 - GPS confirms speed → State: RECORDING
⏱️ 0:11 - Trip ID appears
⏱️ 0:11+ - Speed updates match dashboard
```

**Expected Results:**
```
✅ State: IDLE → VERIFYING (within 5s of driving)
✅ State: VERIFYING → RECORDING (within 5s of VERIFYING)
✅ Trip ID displayed (not empty)
✅ GPS Speed matches dashboard (±2 mph tolerance)
✅ Variance: 0.15 - 1.5 m/s²
✅ Classification: "VEHICLE MOTION"
✅ Total time to start trip: < 15 seconds
```

**Dashboard Speed:** _______ mph  
**App GPS Speed:** _______ mph  
**Difference:** _______ mph

**Status:** [ ] PASS [ ] FAIL

---

### **Test 7: Vehicle Detection - Poor GPS (2 minutes)**

**Steps:**
1. Place phone in vehicle (poor GPS location - e.g., covered by metal)
2. Open Vehicle Monitor screen
3. Start driving
4. Watch for fallback to computed speed

**Expected Timeline:**
```
⏱️ 0:00 - Start driving
⏱️ 0:05 - Smooth motion detected → State: VERIFYING
⏱️ 0:10 - GPS timeout (no signal) → Falls back to computed speed
⏱️ 0:10 - State: RECORDING (based on variance)
⏱️ 0:11 - Trip starts automatically
```

**Expected Results:**
```
✅ State: IDLE → VERIFYING (within 5s)
✅ After 5s in VERIFYING: Falls back to computed speed
✅ State: VERIFYING → RECORDING (using variance)
✅ Trip starts even without GPS
✅ Log shows: "GPS timeout after 5 seconds, falling back to computed speed"
✅ Log shows: "VEHICLE CONFIRMED (Computed from sensors - GPS unavailable)"
```

**Status:** [ ] PASS [ ] FAIL

---

### **Test 8: Trip ID Display (1 minute)**

**Steps:**
1. Start driving (trigger trip start)
2. Check Vehicle Monitor screen
3. Check Record Trip screen

**Expected Results:**
```
✅ Trip ID appears on Vehicle Monitor screen
✅ Trip ID is a valid UUID (e.g., "a1b2c3d4-...")
✅ Trip ID is NOT empty
✅ Trip duration counter starts (00:00:01, 00:00:02, ...)
✅ Same Trip ID shows on Record Trip screen
```

**Trip ID:** _________________________________

**Status:** [ ] PASS [ ] FAIL

---

### **Test 9: Real-Time Updates (2 minutes)**

**Steps:**
1. Start driving
2. Watch Vehicle Monitor screen continuously
3. Vary speed (speed up, slow down)

**Expected Results:**
```
✅ Speed updates every second
✅ Speed changes match vehicle acceleration/deceleration
✅ Variance updates every second
✅ State transitions are immediate
✅ Last Update timestamp increments every second
✅ No freezing or lag
```

**Status:** [ ] PASS [ ] FAIL

---

### **Test 10: Stop Detection (5 minutes)**

**Steps:**
1. Drive for 1 minute
2. Stop at red light (60 seconds)
3. Resume driving

**Expected Results:**
```
✅ While driving: State = RECORDING
✅ When stopped: State = POTENTIAL_STOP (within 5s)
✅ After stopping 60s: State remains POTENTIAL_STOP (not IDLE yet)
✅ When resume: State = RECORDING (within 5s)
✅ Trip continues (not ended)
✅ Log shows: "MOTION RESUMED (Traffic light/Stop sign)"
```

**Status:** [ ] PASS [ ] FAIL

---

### **Test 11: Parking Detection (4 minutes)**

**Steps:**
1. Drive for 1 minute
2. Park vehicle
3. Wait 3 minutes (180 seconds)

**Expected Results:**
```
✅ When parked: State = POTENTIAL_STOP
✅ After 3 minutes: State = IDLE
✅ Trip ends automatically
✅ Log shows: "VEHICLE CONFIRMED PARKED (3 min timeout)"
✅ GPS disabled (battery save)
✅ Message: "Waiting for motion"
```

**Status:** [ ] PASS [ ] FAIL

---

### **Test 12: Screen Independence (2 minutes)**

**Steps:**
1. Launch app (don't open any screens)
2. Put phone in vehicle
3. Start driving
4. After 15 seconds, open Vehicle Monitor

**Expected Results:**
```
✅ Trip started automatically (without opening screens)
✅ Vehicle Monitor shows State: RECORDING
✅ Trip ID is already set
✅ Duration counter is already running
✅ Speed is being recorded
```

**Status:** [ ] PASS [ ] FAIL

---

## 📊 FINAL TEST RESULTS

### Summary Table:

| Test # | Test Name | Status | Notes |
|--------|-----------|--------|-------|
| 1 | App Launch | [ ] | |
| 2 | Vehicle Monitor Screen | [ ] | |
| 3 | Navigation | [ ] | |
| 4 | Stationary Detection | [ ] | |
| 5 | Walking Detection | [ ] | |
| 6 | Vehicle Detection (Good GPS) | [ ] | |
| 7 | Vehicle Detection (Poor GPS) | [ ] | |
| 8 | Trip ID Display | [ ] | |
| 9 | Real-Time Updates | [ ] | |
| 10 | Stop Detection | [ ] | |
| 11 | Parking Detection | [ ] | |
| 12 | Screen Independence | [ ] | |

---

## 🐛 ISSUE REPORTING

If any test fails, capture:

1. **Logcat output:**
```powershell
adb logcat -s DrivingStateManager:V VehicleDetectionVM:V HardwareModule:V
```

2. **Screenshot of the screen**

3. **Exact steps to reproduce**

4. **Expected vs Actual behavior**

---

## ✅ SUCCESS CRITERIA

**All 12 tests must PASS for complete success.**

Acceptable tolerances:
- Speed accuracy: ±2 mph from dashboard
- State transition time: ±2 seconds
- GPS timeout: 5 seconds ±1 second

---

## 🎯 PRIORITY TESTS

If you have limited time, test these first:

1. **Test 6** - Vehicle Detection (Normal GPS) - **CRITICAL**
2. **Test 8** - Trip ID Display - **CRITICAL**
3. **Test 9** - Real-Time Updates - **CRITICAL**
4. **Test 7** - Vehicle Detection (Poor GPS) - **HIGH**
5. **Test 12** - Screen Independence - **HIGH**

---

## 📝 NOTES SECTION

**Date:** _______________  
**Device:** _______________  
**Android Version:** _______________  
**Vehicle:** _______________

**Additional Observations:**
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________

---

**Test Completed By:** _______________  
**Date/Time:** _______________  
**Overall Result:** [ ] PASS [ ] FAIL

