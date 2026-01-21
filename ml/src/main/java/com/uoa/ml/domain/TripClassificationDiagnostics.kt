package com.uoa.ml.domain

import com.uoa.core.mlclassifier.data.InferenceResult
import com.uoa.core.mlclassifier.data.TripFeatures
import java.util.UUID

data class TripClassificationDiagnostics(
    val tripId: UUID,
    val driverProfileId: UUID?,
    val tripStartTime: Long,
    val tripEndTime: Long,
    val durationSeconds: Long,
    val locationCount: Int,
    val rawSensorWithLocationCount: Int,
    val rawSensorCount: Int,
    val accelCount: Int,
    val speedCount: Int,
    val courseCount: Int,
    val gpsPointCount: Int,
    val featureState: FeatureStateSummary?,
    val dayOfWeekMean: Float?,
    val hourOfDayMean: Float?,
    val accelMean: Float?,
    val speedStd: Float?,
    val courseStd: Float?,
    val finalSpeedStd: Float?,
    val finalCourseStd: Float?,
    val usedFeatureStateAccel: Boolean,
    val usedFeatureStateSpeed: Boolean,
    val usedFeatureStateCourse: Boolean,
    val tripFeatures: TripFeatures?,
    val aiInputsBefore: Int,
    val aiInputsAfter: Int,
    val trainingTimeZoneId: String,
    val rawProbabilities: FloatArray?,
    val normalizedProbabilities: FloatArray?,
    val rawLabel: Long?,
    val notEnoughReasons: List<NotEnoughDataReason>,
    val warnings: List<String>,
    val inferenceResult: InferenceResult
)

data class FeatureStateSummary(
    val accelCount: Int,
    val speedCount: Int,
    val courseCount: Int,
    val accelMean: Double,
    val speedM2: Double,
    val courseM2: Double
)

enum class NotEnoughDataReason(val title: String, val detail: String) {
    TripNotFound(
        title = "Trip not found",
        detail = "No local trip record exists for this trip."
    ),
    ShortTrip(
        title = "Trip too short",
        detail = "Trip duration is below the 60 second minimum."
    ),
    InsufficientGps(
        title = "Not enough GPS points",
        detail = "At least 10 GPS points are required."
    ),
    InsufficientAccel(
        title = "Not enough accelerometer samples",
        detail = "At least 20 accelerometer samples are required."
    ),
    InsufficientSpeedSamples(
        title = "Not enough speed samples",
        detail = "Need at least 2 speed samples for standard deviation."
    ),
    InsufficientCourseSamples(
        title = "Not enough course samples",
        detail = "Need at least 2 course samples for standard deviation."
    ),
    NonFiniteFeatures(
        title = "Invalid feature values",
        detail = "One or more features are non-finite after calculation."
    )
}
