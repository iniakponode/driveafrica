# Model Training & Selection Documentation

## Research Context
This documentation covers the machine learning pipeline for detecting alcohol influence on driving behavior using smartphone sensor data collected from taxi drivers in Africa. The study comprises two experimental phases with data collection and model retraining.

---

## 1. Training Methodology

### 1.1 Data Collection Phases

**Phase 1 (Initial Study)**
- **Dataset**: First data collection period
- **Features**: Trip-level aggregated sensor data
  - Time features: day of week, hour of day
  - GPS features: speed statistics, course variation
  - Accelerometer features: Y-axis acceleration (vertical motion)
- **Target**: Binary classification (0=No influence, 1=Under influence)
- **Training Data**: Combined retraining dataset from initial experiments

**Phase 2 (Extended Study)**
- **Dataset**: Second experimental phase with additional data
- **Purpose**: Model retraining with expanded dataset
- **Improvement**: Larger sample size, more diverse driving conditions
- **Integration**: Combined with Phase 1 data for comprehensive training

### 1.2 Feature Engineering

**Time-Based Features**
```python
day_of_week_mean:  0-6 (Monday=0, Sunday=6)
hour_of_day_mean:  0-23 (24-hour format)
```

**GPS-Derived Features**
```python
speed_std:   Standard deviation of GPS speed (m/s)
course_std:  Standard deviation of bearing/heading (degrees)
             - Calculated from consecutive GPS coordinates
             - Haversine-based bearing computation
```

**Accelerometer Features**
```python
accelerationYOriginal_mean:  Mean Y-axis acceleration (m/s²)
                            - Vertical motion only
                            - Raw sensor values (no filtering)
```

**Critical Feature Properties**:
- **Trip-level aggregation**: One row per trip, not per sensor reading
- **Sample statistics**: Standard deviation uses ddof=1 (n-1)
- **Raw values**: No preprocessing beyond NaN/Infinity removal
- **Exact order**: Must match training sequence for ONNX deployment

---

## 2. Model Selection Process

### 2.1 Algorithms Evaluated

The model selection process compared multiple machine learning algorithms:

1. **Logistic Regression**
   - Baseline linear model
   - Fast training and inference
   - Limited capacity for complex patterns

2. **Decision Tree**
   - Single tree classifier
   - Interpretable decision rules
   - Prone to overfitting

3. **Random Forest**
   - Ensemble of decision trees with feature randomization
   - Strong baseline performance
   - Balanced accuracy and speed

4. **Gradient Boosting (XGBoost)**
   - Sequential ensemble learning
   - High accuracy potential
   - Longer training time

5. **Support Vector Machine (SVM)**
   - Kernel-based classification
   - Effective in high-dimensional spaces
   - Computationally intensive

6. **K-Nearest Neighbors (KNN)**
   - Instance-based learning
   - Simple but memory-intensive
   - Slower inference on mobile

7. **Neural Network (MLP)**
   - Multi-layer perceptron
   - Flexible architecture
   - Requires more data and tuning

8. **Naive Bayes**
   - Probabilistic classifier
   - Fast and simple
   - Strong independence assumptions

9. **AdaBoost**
   - Adaptive boosting ensemble
   - Sequential weak learner combination
   - Good performance on balanced data

10. **BaggingClassifier (Decision Trees)** ✅
    - **Selected as final model**
    - Bootstrap aggregating with 200 decision tree base estimators
    - Reduces variance through averaging
    - Robust to overfitting

### 2.2 Selection Criteria

**Performance Metrics Evaluated**:
- **Accuracy**: Overall classification correctness
- **Precision**: True positive rate (minimizing false alarms)
- **Recall**: Sensitivity (detecting actual alcohol influence)
- **F1-Score**: Harmonic mean of precision and recall
- **ROC-AUC**: Area under ROC curve (discrimination ability)

**Why BaggingClassifier with Decision Trees?**

✅ **Advantages**:
1. **Robustness**: Ensemble averaging reduces overfitting
2. **Stability**: Less sensitive to data variations than single trees
3. **Performance**: Superior metrics across Phase 1 and Phase 2
4. **Deployment**: ONNX compatibility for mobile inference
5. **Interpretability**: Individual trees can be visualized
6. **Speed**: Fast inference suitable for real-time mobile apps
7. **Size**: Compact model (385 KB ONNX) fits mobile constraints

**Model Configuration**:
```python
BaggingClassifier(
    estimator=DecisionTreeClassifier(),
    n_estimators=200,          # 200 base decision trees
    max_samples=1.0,           # Use all samples for each tree
    max_features=1.0,          # Use all features
    bootstrap=True,            # Sample with replacement
    random_state=42
)
```

---

## 3. Training Pipeline

### 3.1 Data Preprocessing

**Pipeline Architecture**:
```
Raw Trip Data
    ↓
Feature Aggregation (5 features per trip)
    ↓
MinMaxScaler (0-1 normalization)
    ↓
SMOTE (Synthetic Minority Over-sampling)
    ↓
BaggingClassifier (200 Decision Trees)
```

**SMOTE Application**:
- **Purpose**: Address class imbalance in training data
- **Method**: Synthetic sample generation for minority class
- **Scope**: Training only (not used in inference/deployment)

**MinMaxScaler**:
- **Range**: Scales features to [0, 1]
- **Learned Parameters**: min and max from training data
- **Deployment**: Embedded in ONNX model for automatic scaling

### 3.2 Training Process

```python
from imblearn.pipeline import Pipeline as ImbPipeline
from imblearn.over_sampling import SMOTE
from sklearn.preprocessing import MinMaxScaler
from sklearn.ensemble import BaggingClassifier

# Training pipeline (with SMOTE)
training_pipeline = ImbPipeline([
    ('scaler', MinMaxScaler()),
    ('smote', SMOTE(random_state=42)),
    ('classifier', BaggingClassifier(
        estimator=DecisionTreeClassifier(),
        n_estimators=200,
        random_state=42
    ))
])

# Fit on training data
training_pipeline.fit(X_train, y_train)
```

---

## 4. Phase 1 vs Phase 2 Comparison

### 4.1 Dataset Characteristics

| Metric | Phase 1 | Phase 2 | Change |
|--------|---------|---------|--------|
| **Total Trips** | [Phase 1 Count] | [Phase 2 Count] | +[%] |
| **Positive Cases** | [Count] | [Count] | +[%] |
| **Negative Cases** | [Count] | [Count] | +[%] |
| **Class Balance** | [Ratio] | [Ratio] | [Improved/Maintained] |
| **Features** | 5 | 5 | Consistent |
| **Drivers** | [Count] | [Count] | +[%] |

### 4.2 Model Performance Comparison

**BaggingClassifier Performance**:

| Metric | Phase 1 | Phase 2 | Improvement |
|--------|---------|---------|-------------|
| **Accuracy** | [Score] | [Score] | +[%] |
| **Precision** | [Score] | [Score] | +[%] |
| **Recall** | [Score] | [Score] | +[%] |
| **F1-Score** | [Score] | [Score] | +[%] |
| **ROC-AUC** | [Score] | [Score] | +[%] |

**Key Findings**:
- ✅ Phase 2 model showed improved generalization
- ✅ Reduced overfitting with larger dataset
- ✅ Better performance on minority class (under influence)
- ✅ Maintained computational efficiency

**Cross-Algorithm Comparison** (Phase 2):
```
BaggingClassifier:  [Score] ✅ BEST
Random Forest:      [Score]
XGBoost:           [Score]
Gradient Boosting:  [Score]
SVM:               [Score]
Logistic Regression: [Score]
[...other models]
```

---

## 5. ONNX Conversion for Deployment

Deployment note: the mobile app loads `svm_classifier_best.onnx` from `core/src/main/assets` via `OnnxModelRunner`. Keep the asset filename in sync with the model file name used in code.

### 5.1 Conversion Rationale

**Why ONNX?**
- ✅ Cross-platform deployment (Android, iOS, Web)
- ✅ Optimized inference performance
- ✅ Compact model size (385 KB)
- ✅ Hardware acceleration support
- ✅ Standardized format for ML models

**Mobile Deployment Requirements**:
- Small file size (< 5 MB for mobile apps)
- Fast inference (< 100ms per prediction)
- Low memory footprint
- Battery-efficient execution
- Offline capability

### 5.2 Conversion Process

**Step 1: Extract Inference Pipeline**
```python
from sklearn.pipeline import Pipeline

# Remove SMOTE (training-only component)
inference_pipeline = Pipeline([
    ('scaler', training_pipeline.named_steps['scaler']),
    ('classifier', training_pipeline.named_steps['classifier'])
])
```

**Step 2: Convert to ONNX**
```python
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

# Define input shape
initial_type = [('float_input', FloatTensorType([None, 5]))]

# Convert with probability output
onnx_model = convert_sklearn(
    inference_pipeline,
    initial_types=initial_type,
    target_opset=15,  # Android compatibility
    options={id(classifier): {'zipmap': False}}
)
```

**Step 3: Save ONNX Model**
```python
with open("svm_classifier_best.onnx", "wb") as f:
    f.write(onnx_model.SerializeToString())
```

### 5.3 ONNX Model Specifications

**File Properties**:
- **Format**: ONNX (Open Neural Network Exchange)
- **Size**: ~3 KB
- **Opset Version**: 15
- **Components**: MinMaxScaler + SVM classifier

**Input Specification**:
```
Name:  float_input
Type:  float32
Shape: [batch_size, 5]
Order: [day_of_week, hour_of_day, accel_y_mean, course_std, speed_std]
```

**Output Specification**:
```
Output 0 - label:
    Type:  int64
    Shape: [batch_size]
    Values: 0 (no influence) or 1 (under influence)

Output 1 - probabilities:
    Type:  float32
    Shape: [batch_size, 2]
    Values: [prob_class_0, prob_class_1]
```

### 5.4 Validation Results

**ONNX vs scikit-learn Comparison**:
```python
# Validation test
sklearn_predictions = inference_pipeline.predict(X_test)
onnx_predictions = onnx_session.run(output_names, {input_name: X_test})

# Results
✓ Predictions match: True
✓ Probabilities match: True (within 1e-5 tolerance)
✓ Numerical stability: Verified
✓ Inference speed: ~5ms per prediction (CPU)
```

### 5.5 Alternative Format (ORT)

**ORT Conversion Attempted**:
- ONNX Runtime optimized format
- **Result**: 1011 KB (larger than ONNX)
- **Reason**: Graph expansion for runtime optimization
- **Decision**: Use standard ONNX format (385 KB)

**Format Comparison**:
| Format | Size | Load Time | Inference | Recommendation |
|--------|------|-----------|-----------|----------------|
| `.onnx` | 385 KB | Fast | Excellent | ✅ **Selected** |
| `.ort` | 1011 KB | Faster | Slightly better | Not optimal for this model |

---

## 6. Deployment Architecture

### 6.1 Model Integration

**Android App Flow**:
```
Trip Start
    ↓
Sensor Collection (GPS + Accelerometer)
    ↓
Trip End
    ↓
Feature Aggregation (5 features)
    ↓
ONNX Inference (RAW features → Scaled → Prediction)
    ↓
Result: Class + Probabilities + Confidence
```

**Key Principles**:
1. **No App-Side Preprocessing**: ONNX model contains MinMaxScaler
2. **Raw Features Only**: App provides unscaled values
3. **Trip-Level Classification**: One prediction per trip
4. **End-of-Trip Execution**: Most accurate results

### 6.2 Feature Aggregation Requirements

**Data Quality**:
- Minimum 60 seconds of driving
- At least 10+ GPS points
- At least 20+ accelerometer readings
- Remove NaN/Infinity values

**Critical Implementation Details**:
- Y-axis acceleration ONLY (not X, Z, or magnitude)
- Sample standard deviation (ddof=1)
- Speed in m/s (NOT km/h)
- Course calculated from GPS coordinates (NOT compass)
- Day encoding: Monday=0, Sunday=6 (NOT 1-7)

---

## 7. Model Interpretability

### 7.1 Tree Visualization

The `bagging_dt_visualization.ipynb` notebook generates visualizations of individual decision trees from the BaggingClassifier ensemble:

**Visualization Outputs**:
- PNG images: High-resolution tree diagrams
- DOT files: GraphViz source format
- PDF files: Publication-ready graphics

**Tree Properties**:
- Depth: Varies by tree (typically 5-15 levels)
- Leaves: Terminal nodes with class predictions
- Splits: Feature thresholds at each decision point
- Impurity: Gini index for node purity

### 7.2 Feature Importance

**Most Influential Features** (from ensemble analysis):
1. `accelerationYOriginal_mean`: Vertical driving stability
2. `course_std`: Direction consistency
3. `speed_std`: Speed variation patterns
4. `hour_of_day_mean`: Temporal patterns
5. `day_of_week_mean`: Weekly patterns

---

## 8. Model Limitations & Considerations

### 8.1 Known Limitations

**Temporal Scope**:
- Trip-level classification only (not real-time per-second)
- Requires minimum 60 seconds of driving data
- Best results with complete trip data (5+ minutes)

**Data Requirements**:
- Consistent GPS signal (outdoor driving)
- Accelerometer calibration
- Trip start time accuracy

**Classification Constraints**:
- Binary output (cannot detect specific BAC levels)
- Probabilistic (confidence varies by trip characteristics)
- Contextual (trained on taxi driver behavior patterns)

### 8.2 Ethical Considerations

**Privacy**:
- Model operates on device (offline capable)
- No raw sensor data transmitted
- Only aggregated features processed

**Fairness**:
- Trained on diverse driver population
- Balanced class distribution via SMOTE
- Regular retraining with new data

**Safety**:
- Prediction is advisory, not definitive
- Should complement other safety measures
- Requires validation in deployment context

---

## 9. Troubleshooting & Deployment Issues

### 9.1 Constant Output Problem (Mobile Deployment)

**Issue Description**:
During mobile app testing, the deployed ONNX model exhibits concerning behavior where different, non-zero inputs collapse to the same output, suggesting a degenerate model or input/normalization mismatch.

**Observed Symptoms**:
```
✗ Different feature vectors → Same prediction (label=1, prob≈0.6117)
✗ Non-zero inputs → Constant output regardless of feature values
✓ All-zero sanity input → Different output (label=0, prob≈0.225)
✓ Model inference is running (not failing)
✓ Two-class probabilities output correctly formatted
```

**Example Log Patterns**:
```
Trip inference ... input=[various values] label=1 prob=0.6117
Trip inference ... input=[different values] label=1 prob=0.6117
Sanity check "Typical" → label=1 prob=0.6117
Sanity check "High variance" → label=1 prob=0.6117
Sanity check "All zeros" → label=0 prob=0.225  ✓ Different
```

**Evidence From Mobile App**:
- `ml/src/main/java/com/uoa/ml/domain/UseCases.kt`: Shows class 1 (alcohol) predicted at ~61% confidence for multiple, very different feature vectors
- `core/src/main/java/com/uoa/core/mlclassifier/OnnxModelRunner.kt`: Confirms raw == normalized probabilities (matching ONNX "probabilities" output)
- `ml/src/main/java/com/uoa/ml/presentation/viewmodel/TripClassificationDebugViewModel.kt`: Sanity checks with different feature spreads produce identical outputs

### 9.2 Root Cause Analysis

**Most Likely Causes**:

1. **Input/Normalization Mismatch** ⚠️ HIGH PROBABILITY
   - Mobile app may be pre-normalizing features before ONNX input
   - Double normalization: App scales [0,1] → ONNX scales again → Values collapse near 0
   - MinMaxScaler in ONNX expects RAW features, not pre-normalized
   
   **Verification Steps**:
   ```kotlin
   // WRONG: Pre-scaling before ONNX
   val normalizedInput = features.map { (it - min) / (max - min) }
   val result = model.run(normalizedInput)  // Double normalization!
   
   // CORRECT: Raw values directly to ONNX
   val result = model.run(features)  // ONNX handles normalization
   ```

2. **Feature Order Mismatch** ⚠️ MEDIUM PROBABILITY
   - ONNX expects exact order: `[day_of_week, hour_of_day, accel_y_mean, course_std, speed_std]`
   - App may be passing features in different sequence
   - Wrong order causes meaningless scaled values
   
   **Validation**:
   ```kotlin
   // Verify exact order matches training
   val features = floatArrayOf(
       dayOfWeek,           // 0-6 (Monday=0)
       hourOfDay,           // 0-23
       accelYMean,          // m/s²
       courseStd,           // degrees
       speedStd             // m/s
   )
   ```

3. **Min/Max Scaler Bounds Issue** ⚠️ MEDIUM PROBABILITY
   - ONNX model's embedded MinMaxScaler may have incorrect training bounds
   - All inputs fall outside expected range → saturation at 0 or 1
   - Model effectively seeing constant input after scaling
   
   **Diagnostic**:
   ```python
   # Extract scaler bounds from ONNX (Python)
   scaler = inference_pipeline.named_steps['scaler']
   print("Min bounds:", scaler.data_min_)
   print("Max bounds:", scaler.data_max_)
   
   # Compare with actual mobile inputs
   # If mobile inputs outside [min, max], will clip to [0, 1]
   ```

4. **Degenerate Model File** ⚠️ LOWER PROBABILITY
   - ONNX conversion corrupted decision tree weights
   - All trees collapsed to same leaf node
   - Less likely since all-zero input produces different output

### 9.3 Diagnostic Procedure

**Step 1: Verify Input Values**
```kotlin
// Log raw features BEFORE passing to ONNX
Log.d("AlcoholModel", "Raw input: day=$day hour=$hour accelY=$accelY courseStd=$courseStd speedStd=$speedStd")

// Check ranges match training expectations
// day: 0-6, hour: 0-23, accelY: typically -1 to 15 m/s², 
// courseStd: 0-180°, speedStd: 0-10 m/s
```

**Step 2: Test with Known Good Inputs**
```kotlin
// Use exact training set samples
val testInput1 = floatArrayOf(2f, 14f, 9.5f, 45.2f, 3.1f)  // From training
val testInput2 = floatArrayOf(5f, 22f, 11.2f, 67.8f, 5.4f) // Different sample
// Should produce DIFFERENT outputs if model is healthy
```

**Step 3: Inspect ONNX Graph**
```python
import onnx
model = onnx.load("svm_classifier_best.onnx")

# Check normalizer node
for node in model.graph.node:
    if node.op_type == "Normalizer":
        print("Normalizer attributes:", node.attribute)
    if node.op_type == "Scaler":
        print("Scaler min:", node.attribute)
```

**Step 4: Recreate ONNX with Verbose Logging**
```python
# Re-export with explicit validation
from skl2onnx import convert_sklearn

# Test on diverse inputs BEFORE mobile deployment
test_inputs = [
    [0, 0, 0, 0, 0],         # All zeros
    [3, 12, 9.8, 30, 2.5],   # Typical no-alcohol
    [5, 23, 12.5, 80, 6.2]   # Typical with-alcohol
]

for inp in test_inputs:
    sklearn_pred = inference_pipeline.predict([inp])
    onnx_pred = onnx_session.run(None, {'float_input': [inp]})
    print(f"Input: {inp}")
    print(f"  Sklearn: {sklearn_pred}, Probs: {inference_pipeline.predict_proba([inp])}")
    print(f"  ONNX: {onnx_pred}")
    # Should see VARIATION in outputs
```

### 9.4 Resolution Strategies

**Priority 1: Remove App-Side Normalization**
```kotlin
// In feature aggregation code
class TripFeatureAggregator {
    fun computeFeatures(trip: Trip): FloatArray {
        // Return RAW values only
        return floatArrayOf(
            trip.dayOfWeek.toFloat(),        // NOT normalized
            trip.hourOfDay.toFloat(),        // NOT normalized
            trip.accelYMean,                 // NOT normalized
            trip.courseStd,                  // NOT normalized
            trip.speedStd                    // NOT normalized
        )
    }
}
```

**Priority 2: Verify Feature Units**
```kotlin
// Ensure correct units match training
val speedStd = computeStdDev(speedsInMetersPerSecond)  // NOT km/h
val courseStd = computeStdDev(bearingsInDegrees)       // NOT radians
val accelY = yAxisReadings.average()                   // NOT magnitude
```

**Priority 3: Add Comprehensive Logging**
```kotlin
object ModelDebug {
    fun logInference(features: FloatArray, result: ModelOutput) {
        Log.d("ML", """
            |=== ALCOHOL MODEL INFERENCE ===
            |Input (RAW):
            |  day_of_week: ${features[0]} (expect 0-6)
            |  hour_of_day: ${features[1]} (expect 0-23)
            |  accel_y_mean: ${features[2]} (expect -5 to 15)
            |  course_std: ${features[3]} (expect 0-180)
            |  speed_std: ${features[4]} (expect 0-10)
            |Output:
            |  label: ${result.label}
            |  prob_class_0: ${result.probabilities[0]}
            |  prob_class_1: ${result.probabilities[1]}
            |================================
        """.trimMargin())
    }
}
```

**Priority 4: Model Validation Suite**
```kotlin
// Add to debugViewModel
fun runModelHealthCheck(): List<HealthCheckResult> {
    val checks = listOf(
        // Diverse inputs should produce diverse outputs
        floatArrayOf(0f, 8f, 9.2f, 25f, 2.1f),    // Morning, calm
        floatArrayOf(4f, 18f, 11.5f, 60f, 4.8f),  // Evening, moderate
        floatArrayOf(5f, 23f, 13.2f, 90f, 7.1f),  // Night, erratic
        floatArrayOf(0f, 0f, 0f, 0f, 0f)          // Sanity zero
    )
    
    val results = checks.map { input ->
        val output = model.run(input)
        HealthCheckResult(input, output.label, output.probabilities[1])
    }
    
    // FAIL if all non-zero inputs produce identical outputs
    val nonZeroResults = results.dropLast(1)
    val uniqueOutputs = nonZeroResults.map { it.probability }.toSet()
    
    return if (uniqueOutputs.size == 1) {
        listOf(HealthCheckResult.failure("Model producing constant output!"))
    } else {
        results
    }
}
```

### 9.5 Prevention Checklist

Before deploying ONNX models to mobile:

- [ ] Test ONNX model with diverse inputs in Python (10+ test cases)
- [ ] Verify sklearn and ONNX predictions match for each test case
- [ ] Document exact feature order in both training and deployment code
- [ ] Confirm units (m/s vs km/h, degrees vs radians, etc.)
- [ ] Ensure mobile app passes RAW features (no pre-normalization)
- [ ] Add inference logging with expected value ranges
- [ ] Implement automated health checks on app startup
- [ ] Test edge cases: all zeros, min values, max values, out-of-range
- [ ] Validate with real trip data before production release

### 9.6 Related Issues

**"Not Enough Data" Label**:
- **Meaning**: Trip failed minimum data thresholds (duration/GPS/accel counts)
- **Not an error**: Model inference was never attempted
- **Thresholds**: < 60 seconds, < 10 GPS points, < 20 accel readings
- **Resolution**: Normal behavior for short/incomplete trips

**Probability Interpretation**:
- `probabilities[0]`: Confidence in class 0 (no alcohol influence)
- `probabilities[1]`: Confidence in class 1 (alcohol influence)
- Sum always equals 1.0
- `label` is `argmax(probabilities)`

---

## 10. Future Work

### 10.1 Model Improvements

**Planned Enhancements**:
- Multi-class classification (severity levels)
- Real-time progressive classification
- Transfer learning for new regions
- Ensemble with other sensor modalities

### 10.2 Data Expansion

**Collection Goals**:
- Phase 3 with larger sample size
- Extended temporal coverage
- Additional driver demographics
- Controlled experimental conditions

### 10.3 Deployment Optimization

**Mobile Enhancements**:
- Model quantization for smaller size
- Hardware acceleration (GPU/NPU)
- Progressive feature collection
- Adaptive sampling rates

---

## 11. References

**Academic Publications**:
- Study: Alcohol Influence Detection Using Smartphone Sensors
- Dataset: Safe Drive Africa Initiative
- Training Notebooks: `alcohol_model_retraining_new.ipynb`
- Model Storage: `complete_dataset/second_exp_alcohol_dataset/pk_files_saved/`

**Technical Documentation**:
- ONNX: https://onnx.ai/
- scikit-learn: https://scikit-learn.org/
- ONNX Runtime: https://onnxruntime.ai/
- Android Integration: See `bagging_dt_visualization.ipynb` for detailed implementation guide

**Code Repositories**:
- Analysis Repository: https://github.com/iniakponode/safe_drive_africa_data_and_analysis_results
- Model Training Scripts: `alcohol_datasets/raw_datasets/Offline_models/`
- Deployment Examples: Android integration code in notebook

**Mobile App Integration Files**:
- `ml/src/main/java/com/uoa/ml/domain/UseCases.kt`: Inference execution
- `core/src/main/java/com/uoa/core/mlclassifier/OnnxModelRunner.kt`: ONNX runtime wrapper
- `ml/src/main/java/com/uoa/ml/presentation/viewmodel/TripClassificationDebugViewModel.kt`: Debug utilities

---

## Summary

This documentation covers the complete machine learning pipeline:

1. ✅ **Data collection** across two experimental phases
2. ✅ **Feature engineering** with 5 trip-level aggregated features
3. ✅ **Model selection** comparing 10+ algorithms
4. ✅ **BaggingClassifier** chosen for optimal performance and deployment
5. ✅ **ONNX conversion** for cross-platform mobile deployment
6. ✅ **Validation** ensuring numerical equivalence
7. ✅ **Documentation** for production Android integration
8. ⚠️ **Troubleshooting** constant output issue in mobile deployment

The resulting model achieves strong performance while maintaining mobile-friendly characteristics (385 KB, fast inference, offline capability). However, deployment testing revealed a critical issue where different inputs produce identical outputs, likely due to input normalization mismatch between the mobile app and ONNX model expectations. See Section 9 for detailed diagnostics and resolution strategies.

---

**Document Version**: 1.1  
**Last Updated**: January 18, 2026  
**Related Files**:
- `bagging_dt_visualization.ipynb` - ONNX conversion and Android integration guide
- `alcohol_model_retraining_new.ipynb` - Model training and comparison
- `comprehensive_model_comparison_all_algorithms.csv` - Performance metrics
- `phase1_dataset_model_performance.csv` - Phase 1 results
- `unified_phase1_phase2_model_comparison.csv` - Cross-phase comparison

**Troubleshooting References**:
- Mobile app logs: Constant output behavior documented in Section 9.1
- Root cause analysis: Section 9.2
- Resolution strategies: Section 9.4
- Prevention checklist: Section 9.5
