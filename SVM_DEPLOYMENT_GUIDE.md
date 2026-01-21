# SVM Model Input Pipeline & Deployment Guide

## Feature Requirements

The SVM model requires **exactly 5 features** in this **specific order**:

### Feature List (MUST be in this exact order):

| Position | Feature Name | Description | Expected Range | Unit |
|----------|-------------|-------------|----------------|------|
| **0** | `day_of_week_mean` | Day of week (averaged over trip) | 0.0 - 6.0 | 0=Monday, 6=Sunday |
| **1** | `hour_of_day_mean` | Hour of day (averaged over trip) | 0.0 - 23.0 | 24-hour format |
| **2** | `accelerationYOriginal_mean` | Mean Y-axis acceleration | -10.0 to +10.0 | m/s² (vertical motion) |
| **3** | `course_std` | Std dev of GPS bearing/heading | 0.0 - 180.0 | degrees |
| **4** | `speed_std` | Std dev of GPS speed | 0.0 - 50.0 | m/s |

**⚠️ CRITICAL:** Features MUST be in **RAW values** (not pre-normalized). The ONNX model contains the MinMaxScaler that will normalize them internally.

---

## Preprocessing Pipeline (Embedded in ONNX)

The ONNX model contains a **2-stage pipeline**:

```
Input (RAW features) 
    ↓
[1] MinMaxScaler (learned from training data)
    ↓
[2] SVM Classifier (RBF kernel)
    ↓
Output (label + probabilities)
```

**Scaler Bounds (Learned from Training Data):**
```python
Feature Minimums: [0.00, 7.00, -2.37, 6.01, 5.01]
Feature Maximums: [6.00, 23.00, 9.76, 153.48, 127.70]

# Normalization formula (applied internally):
normalized_value = (raw_value - min) / (max - min)
```

---

## Mobile App Integration (Android/Kotlin)

### Step 1: Extract Trip-Level Features

```kotlin
data class TripFeatures(
    val dayOfWeekMean: Float,      // 0-6 (Monday=0, Sunday=6)
    val hourOfDayMean: Float,       // 0-23
    val accelYMean: Float,          // m/s² (vertical acceleration)
    val courseStd: Float,           // degrees (bearing variation)
    val speedStd: Float             // m/s (speed variation)
)

fun extractTripFeatures(sensorData: List<SensorReading>): TripFeatures {
    // 1. Day of Week & Hour
    val timestamps = sensorData.map { it.timestamp }
    val dayOfWeek = timestamps.map { getDayOfWeek(it) }.average()  // 0-6
    val hourOfDay = timestamps.map { getHourOfDay(it) }.average()  // 0-23
    
    // 2. Acceleration Y-axis (vertical motion)
    val accelY = sensorData.map { it.accelerometerY }
    val accelYMean = accelY.average()
    
    // 3. GPS Course (bearing) Standard Deviation
    val locations = sensorData.mapNotNull { it.location }
    val bearings = computeBearingsBetweenPoints(locations)
    val courseStd = bearings.standardDeviation()
    
    // 4. GPS Speed Standard Deviation
    val speeds = locations.map { it.speed }  // m/s
    val speedStd = speeds.standardDeviation()
    
    return TripFeatures(
        dayOfWeekMean = dayOfWeek.toFloat(),
        hourOfDayMean = hourOfDay.toFloat(),
        accelYMean = accelYMean.toFloat(),
        courseStd = courseStd.toFloat(),
        speedStd = speedStd.toFloat()
    )
}

// Helper: Compute bearings between consecutive GPS points
fun computeBearingsBetweenPoints(locations: List<Location>): List<Double> {
    return locations.zipWithNext { loc1, loc2 ->
        val lat1 = Math.toRadians(loc1.latitude)
        val lat2 = Math.toRadians(loc2.latitude)
        val lon1 = Math.toRadians(loc1.longitude)
        val lon2 = Math.toRadians(loc2.longitude)
        
        val dLon = lon2 - lon1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - 
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
        
        val bearing = Math.toDegrees(Math.atan2(y, x))
        (bearing + 360) % 360  // Normalize to 0-360
    }
}
```

### Step 2: Prepare ONNX Input

```kotlin
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

class AlcoholInfluenceClassifier(private val ortSession: OrtSession) {
    
    fun predict(tripFeatures: TripFeatures): PredictionResult {
        // Create input tensor [1, 5] - batch size 1, 5 features
        val inputData = floatArrayOf(
            tripFeatures.dayOfWeekMean,
            tripFeatures.hourOfDayMean,
            tripFeatures.accelYMean,
            tripFeatures.courseStd,
            tripFeatures.speedStd
        )
        
        // Reshape to [1, 5] for ONNX
        val shape = longArrayOf(1, 5)
        
        val env = OrtEnvironment.getEnvironment()
        val inputTensor = OnnxTensor.createTensor(
            env, 
            FloatBuffer.wrap(inputData),
            shape
        )
        
        // Run inference
        val inputs = mapOf("float_input" to inputTensor)
        val outputs = ortSession.run(inputs)
        
        // Extract outputs
        val label = (outputs[0].value as Array<*>)[0] as Long
        val probabilities = (outputs[1].value as Array<FloatArray>)[0]
        
        val probabilityPositive = probabilities[1]  // Probability of class 1 (under influence)
        
        inputTensor.close()
        outputs.close()
        
        return PredictionResult(
            predictedLabel = label.toInt(),
            probabilityUnderInfluence = probabilityPositive,
            isUnderInfluence = label == 1L
        )
    }
}

data class PredictionResult(
    val predictedLabel: Int,              // 0 or 1
    val probabilityUnderInfluence: Float, // 0.0 - 1.0
    val isUnderInfluence: Boolean
)
```

### Step 3: Load ONNX Model

```kotlin
// In your Application or ViewModel initialization
class MLModelManager(context: Context) {
    private val ortEnv = OrtEnvironment.getEnvironment()
    private val ortSession: OrtSession
    
    init {
        // Load ONNX model from assets
        val modelBytes = context.assets.open("svm_classifier_best.onnx").readBytes()
        ortSession = ortEnv.createSession(modelBytes)
    }
    
    fun classifyTrip(sensorData: List<SensorReading>): PredictionResult {
        val features = extractTripFeatures(sensorData)
        val classifier = AlcoholInfluenceClassifier(ortSession)
        return classifier.predict(features)
    }
    
    fun close() {
        ortSession.close()
    }
}
```

---

## Python Deployment (Server-Side)

### Option 1: Using ONNX Runtime

```python
import onnxruntime as rt
import numpy as np

# Load model once at startup
session = rt.InferenceSession("svm_classifier_best.onnx")
input_name = session.get_inputs()[0].name
output_names = [session.get_outputs()[0].name, session.get_outputs()[1].name]

def predict(day_of_week, hour_of_day, accel_y, course_std, speed_std):
    """
    Predict alcohol influence from trip features.
    
    Args:
        day_of_week: 0-6 (Monday=0, Sunday=6)
        hour_of_day: 0-23 (24-hour format)
        accel_y: Mean Y-axis acceleration (m/s²)
        course_std: Std dev of GPS bearing (degrees)
        speed_std: Std dev of GPS speed (m/s)
    
    Returns:
        dict: {
            'label': 0 or 1,
            'probability': float (0-1),
            'under_influence': bool
        }
    """
    # Create input array [1, 5] - RAW values (not normalized)
    input_data = np.array([[
        day_of_week,
        hour_of_day,
        accel_y,
        course_std,
        speed_std
    ]], dtype=np.float32)
    
    # Run inference
    label, probabilities = session.run(output_names, {input_name: input_data})
    
    return {
        'label': int(label[0]),
        'probability': float(probabilities[0, 1]),  # Probability of class 1
        'under_influence': bool(label[0] == 1)
    }

# Example usage
result = predict(
    day_of_week=5.5,      # Saturday
    hour_of_day=13.08,    # ~1pm
    accel_y=0.207,        # Low vertical acceleration
    course_std=6.33,      # Low bearing variation
    speed_std=5.09        # Low speed variation
)
print(f"Under influence: {result['under_influence']} (prob={result['probability']:.4f})")
```

### Option 2: Using Sklearn (if .pkl available)

```python
import joblib

# Load inference pipeline (scaler + classifier, no SMOTE)
model = joblib.load("svm_classifier_inference.pkl")

def predict(day_of_week, hour_of_day, accel_y, course_std, speed_std):
    import pandas as pd
    
    # Create DataFrame with correct column names
    features = pd.DataFrame([{
        'day_of_week_mean': day_of_week,
        'hour_of_day_mean': hour_of_day,
        'accelerationYOriginal_mean': accel_y,
        'course_std': course_std,
        'speed_std': speed_std
    }])
    
    label = model.predict(features)[0]
    probability = model.predict_proba(features)[0, 1]
    
    return {
        'label': int(label),
        'probability': float(probability),
        'under_influence': bool(label == 1)
    }
```

---

## Input Validation

```python
def validate_input_features(day_of_week, hour_of_day, accel_y, course_std, speed_std):
    """Validate input features are in expected ranges"""
    errors = []
    
    if not (0 <= day_of_week <= 6):
        errors.append(f"day_of_week must be 0-6, got {day_of_week}")
    
    if not (0 <= hour_of_day <= 23):
        errors.append(f"hour_of_day must be 0-23, got {hour_of_day}")
    
    if not (-10 <= accel_y <= 10):
        errors.append(f"accel_y should be -10 to +10, got {accel_y}")
    
    if not (0 <= course_std <= 360):
        errors.append(f"course_std should be 0-360, got {course_std}")
    
    if not (0 <= speed_std <= 100):
        errors.append(f"speed_std should be 0-100, got {speed_std}")
    
    if errors:
        raise ValueError("\n".join(errors))
    
    return True
```

---

## Example Test Cases

```python
# Test Case 1: Likely NOT under influence
result = predict(
    day_of_week=5.5,     # Saturday
    hour_of_day=13.08,   # 1pm
    accel_y=0.21,        # Low acceleration
    course_std=6.33,     # Steady direction
    speed_std=5.09       # Steady speed
)
# Expected: probability ~0.006 (very low)

# Test Case 2: Possible under influence
result = predict(
    day_of_week=0.0,     # Monday
    hour_of_day=2.5,     # 2:30am (late night)
    accel_y=2.5,         # Erratic vertical motion
    course_std=85.0,     # High direction variation
    speed_std=25.0       # High speed variation
)
# Expected: probability higher (check model output)
```

---

## Key Differences from BaggingClassifier Model

| Aspect | BaggingClassifier | SVM Model |
|--------|-------------------|-----------|
| **Features** | Same 5 features | Same 5 features |
| **Feature Order** | Same order | Same order |
| **Preprocessing** | MinMaxScaler | MinMaxScaler (same bounds) |
| **ONNX File** | `bagging_classifier_retrained.onnx` (313.6 KB) | `svm_classifier_best.onnx` (2.9 KB) |
| **Input Name** | `float_input` | `float_input` |
| **Output Names** | `label`, `probabilities` | `label`, `probabilities` |
| **Code Changes** | **NONE REQUIRED** | Just swap ONNX file! |

**🎯 Migration is drop-in replacement** - only change the ONNX filename in your app!

---

## Deployment Checklist

- [ ] Verify raw feature extraction (no pre-normalization)
- [ ] Ensure feature order: [day_of_week, hour, accel_y, course_std, speed_std]
- [ ] Replace ONNX file: `svm_classifier_best.onnx` (2.9 KB)
- [ ] Test with known inputs from validation set
- [ ] Confirm probability outputs are diverse (not constant)
- [ ] Monitor false positive rate in production
- [ ] Set appropriate threshold (default: 0.5, tune based on precision/recall needs)

---

## Troubleshooting

**Q: Model returns constant predictions**
- ✅ Verify input features are RAW (not pre-normalized [0,1])
- ✅ Check feature order matches exactly
- ✅ Ensure all 5 features are provided

**Q: Probabilities seem too low/high**
- ✅ Normal! SVM outputs range from 0.001 to 0.9+
- ✅ Threshold can be adjusted: 0.5 (default), 0.3 (more sensitive), 0.7 (more specific)

**Q: ONNX model fails to load**
- ✅ Ensure ONNX Runtime version supports opset 15
- ✅ Check file is not corrupted (should be 2.9 KB)
- ✅ Verify file path and asset loading

**Q: Different results between Python and mobile**
- ✅ Verify input precision (float32 vs float64)
- ✅ Check feature extraction logic matches exactly
- ✅ Compare intermediate scaler outputs
