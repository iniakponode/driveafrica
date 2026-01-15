package com.uoa.ml.domain

import android.util.Log
import com.uoa.core.database.entities.CauseEntity
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.daos.TripFeatureStateDao
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.database.repository.CauseRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.mlclassifier.OnnxModelRunner
import com.uoa.core.utils.toEntity
import com.uoa.core.mlclassifier.data.InferenceResult
import com.uoa.core.mlclassifier.data.TripFeatures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import android.hardware.Sensor
import com.uoa.core.database.entities.AIModelInputsEntity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Named

// RunClassificationUseCase.kt
import kotlinx.coroutines.Dispatchers

class RunClassificationUseCase @Inject constructor(
    private val onnxModelRunner: OnnxModelRunner,
    private val aiModelInputRepository: AIModelInputRepository,
    private val tripRepository: TripDataRepository,
    private val locationRepository: LocationRepository,
    private val rawSensorDataRepository: RawSensorDataRepository,
    private val tripFeatureStateDao: TripFeatureStateDao,
    @Named("TrainingTimeZone") private val trainingTimeZone: TimeZone
) {

    suspend fun invoke(tripId: UUID): InferenceResult {
        Log.i("Trip", "Classifier invoked for tripId: $tripId")

        return withContext(Dispatchers.IO) {
            val trip = tripRepository.getTripById(tripId)
                ?: return@withContext InferenceResult.NotEnoughData
            val locations = locationRepository.getLocationDataByTripId(tripId)
                .sortedBy { it.timestamp }
            val rawSensorsWithLocation = rawSensorDataRepository
                .getRawSensorDataByTripId(tripId)
                .first()
            val rawSensorsAll = rawSensorDataRepository
                .getSensorDataByTripId(tripId)
                .first()
            val featureState = tripFeatureStateDao.getByTripId(tripId)

            Log.d(
                "Trip",
                "Classification data for tripId=$tripId: locations=${locations.size}, " +
                    "rawSensorsWithLocation=${rawSensorsWithLocation.size}, " +
                    "rawSensorsAll=${rawSensorsAll.size}"
            )

            val accelYValues = rawSensorsAll
                .filter {
                    it.sensorType == Sensor.TYPE_ACCELEROMETER ||
                        it.sensorTypeName.contains("accel", ignoreCase = true)
                }
                .mapNotNull { sensor ->
                    sensor.values.getOrNull(1)?.takeIf { it.isFinite() }
                }

            val speedValues = locations.mapNotNull { location ->
                location.speed?.takeIf { it.isFinite() && it >= 0.0 }
            }

            val courseValues = buildCourseValues(locations)
            val stateSummary = featureState?.let {
                "state(accel=${it.accelCount}, speed=${it.speedCount}, course=${it.courseCount})"
            } ?: "state=none"

            Log.d(
                "Trip",
                "Feature inputs: accelY=${accelYValues.size}, speed=${speedValues.size}, " +
                    "course=${courseValues.size}, $stateSummary"
            )

            val hasStateData = featureState?.let {
                it.accelCount > 0 || it.speedCount > 1 || it.courseCount > 1
            } ?: false
            if (accelYValues.isEmpty() && locations.isEmpty() && !hasStateData) {
                Log.w("Trip", "Not enough data: no accel Y and no locations for tripId=$tripId")
                return@withContext InferenceResult.NotEnoughData
            }

            val (dayOfWeekMean, hourOfDayMean) = extractTimeFeatures(trip.startTime)
            val accelMean = if (accelYValues.isNotEmpty()) {
                calculateMean(accelYValues)
            } else {
                if (featureState != null && featureState.accelCount > 0) {
                    Log.d("Trip", "Using trip_feature_state accel mean for tripId=$tripId")
                }
                featureState?.accelMean?.toFloat() ?: 0.0f
            }
            val speedStd = if (speedValues.isNotEmpty()) {
                calculateSampleStd(speedValues)
            } else {
                if (featureState != null && featureState.speedCount > 1) {
                    Log.d("Trip", "Using trip_feature_state speed std for tripId=$tripId")
                }
                featureState?.let { calculateSampleStd(it.speedCount, it.speedM2) } ?: 0.0f
            }
            val courseStd = if (courseValues.isNotEmpty()) {
                calculateSampleStd(courseValues)
            } else {
                if (featureState != null && featureState.courseCount > 1) {
                    Log.d("Trip", "Using trip_feature_state course std for tripId=$tripId")
                }
                featureState?.let { calculateSampleStd(it.courseCount, it.courseM2) } ?: 0.0f
            }
            val tripFeatures = TripFeatures(
                hourOfDayMean = hourOfDayMean,
                dayOfWeekMean = dayOfWeekMean,
                speedStd = speedStd,
                courseStd = courseStd,
                accelerationYOriginalMean = accelMean
            )

            Log.d("Trip", "Extracted Features: $tripFeatures")

            if (!tripFeatures.hourOfDayMean.isFinite() ||
                !tripFeatures.dayOfWeekMean.isFinite() ||
                !tripFeatures.speedStd.isFinite() ||
                !tripFeatures.courseStd.isFinite() ||
                !tripFeatures.accelerationYOriginalMean.isFinite()
            ) {
                Log.w("Trip", "Not enough data: non-finite features for tripId=$tripId -> $tripFeatures")
                return@withContext InferenceResult.NotEnoughData
            }

            aiModelInputRepository.deleteAiModelInputsByTripId(tripId)

            val driverProfileId = trip.driverPId
            val endTimestamp = resolveTripEndTimestamp(trip, locations, rawSensorsAll)
            val aiModelInputId = if (driverProfileId != null) {
                val input = AIModelInputsEntity(
                    id = UUID.randomUUID(),
                    tripId = tripId,
                    driverProfileId = driverProfileId,
                    timestamp = endTimestamp,
                    startTimestamp = trip.startTime,
                    endTimestamp = endTimestamp,
                    date = Date(trip.startTime),
                    hourOfDayMean = tripFeatures.hourOfDayMean.toDouble(),
                    dayOfWeekMean = tripFeatures.dayOfWeekMean,
                    speedStd = tripFeatures.speedStd,
                    courseStd = tripFeatures.courseStd,
                    accelerationYOriginalMean = tripFeatures.accelerationYOriginalMean,
                    processed = false,
                    sync = false
                )
                aiModelInputRepository.insertAiModelInput(input)
                input.id
            } else {
                Log.w("Trip", "Skipping AI model input store; missing driver id for $tripId")
                null
            }

            try {
                val inference = onnxModelRunner.runInference(tripFeatures)
                Log.i(
                    "Trip",
                    "Inference result: influenced=${inference.isAlcoholInfluenced}, " +
                        "probability=${inference.probability}"
                )

                if (aiModelInputId != null && driverProfileId != null) {
                    val updatedInput = AIModelInputsEntity(
                        id = aiModelInputId,
                        tripId = tripId,
                        driverProfileId = driverProfileId,
                        timestamp = endTimestamp,
                        startTimestamp = trip.startTime,
                        endTimestamp = endTimestamp,
                        date = Date(trip.startTime),
                        hourOfDayMean = tripFeatures.hourOfDayMean.toDouble(),
                        dayOfWeekMean = tripFeatures.dayOfWeekMean,
                        speedStd = tripFeatures.speedStd,
                        courseStd = tripFeatures.courseStd,
                        accelerationYOriginalMean = tripFeatures.accelerationYOriginalMean,
                        processed = true,
                        sync = false
                    )
                    aiModelInputRepository.updateAiModelInput(updatedInput)
                }

                InferenceResult.Success(
                    inference.isAlcoholInfluenced,
                    inference.probability
                )
            } catch (e: Exception) {
                Log.e("Trip", "Error during model inference: ${e.message}", e)
                InferenceResult.Failure(e)
            }
        }
    }

    private fun extractTimeFeatures(tripStartTimestamp: Long): Pair<Float, Float> {
        val calendar = Calendar.getInstance(trainingTimeZone)
        calendar.timeInMillis = tripStartTimestamp
        val dayOfWeek = ((calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7).toFloat()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY).toFloat()
        return dayOfWeek to hourOfDay
    }

    private fun buildCourseValues(locations: List<com.uoa.core.model.LocationData>): List<Double> {
        if (locations.size < 2) {
            return emptyList()
        }
        val values = ArrayList<Double>(locations.size - 1)
        val sorted = locations.sortedBy { it.timestamp }
        for (index in 1 until sorted.size) {
            val prev = sorted[index - 1]
            val curr = sorted[index]
            if (!prev.latitude.isFinite() || !prev.longitude.isFinite()) {
                continue
            }
            if (!curr.latitude.isFinite() || !curr.longitude.isFinite()) {
                continue
            }
            val bearing = calculateBearing(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
            if (bearing.isFinite()) {
                values.add(bearing)
            }
        }
        return values
    }

    private fun calculateBearing(
        prevLat: Double,
        prevLon: Double,
        currLat: Double,
        currLon: Double
    ): Double {
        val lat1 = Math.toRadians(prevLat)
        val lon1 = Math.toRadians(prevLon)
        val lat2 = Math.toRadians(currLat)
        val lon2 = Math.toRadians(currLon)

        val dLon = lon2 - lon1
        val x = sin(dLon) * cos(lat2)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        var bearing = atan2(x, y)
        bearing = Math.toDegrees(bearing)
        bearing = (bearing + 360) % 360
        return bearing
    }

    private fun calculateSampleStd(values: List<Double>): Float {
        if (values.size < 2) {
            return 0.0f
        }
        val mean = values.average()
        val variance = values.sumOf { (it - mean).pow(2) } / (values.size - 1)
        return sqrt(variance).toFloat()
    }

    private fun calculateMean(values: List<Float>): Float {
        if (values.isEmpty()) {
            return 0.0f
        }
        return values.average().toFloat()
    }

    private fun calculateSampleStd(count: Int, m2: Double): Float {
        if (count < 2) {
            return 0.0f
        }
        return sqrt(m2 / (count - 1)).toFloat()
    }

    private fun resolveTripEndTimestamp(
        trip: com.uoa.core.model.Trip,
        locations: List<com.uoa.core.model.LocationData>,
        rawSensors: List<com.uoa.core.database.entities.RawSensorDataEntity>
    ): Long {
        val locationEnd = locations.maxOfOrNull { it.timestamp }
        val sensorEnd = rawSensors.maxOfOrNull { it.timestamp }
        return listOfNotNull(trip.endTime, locationEnd, sensorEnd).maxOrNull() ?: trip.startTime
    }

}



class UpDateUnsafeBehaviourCauseUseCase @Inject constructor(
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository
) {
    fun invoke(tripId: UUID, alcoholInf: Boolean): List<UnsafeBehaviourEntity>{
        val listOfUnsafeBehaviours = mutableListOf<UnsafeBehaviourEntity>()
        Log.d("UnsafeBehavio","Unsafe Behaviours I got here UpDateUnsafeBehaviourCauseUseCase")
        CoroutineScope(Dispatchers.IO).launch{
            val currentTripUnsafeBehaviour = unsafeBehaviourRepository.getUnsafeBehavioursByTripId(tripId)

            currentTripUnsafeBehaviour.collect { unsafeBehaviours ->
                unsafeBehaviours.forEach {
                    if (it.locationId!=null)
                        listOfUnsafeBehaviours.add(it.toEntity())
                        Log.d("UnsafeBehaviour", "Unsafe Behaviours$listOfUnsafeBehaviours")
                }
                if (listOfUnsafeBehaviours.isNotEmpty()) {
                    unsafeBehaviourRepository.updateUnsafeBehaviourTransactions(
                        listOfUnsafeBehaviours,
                        alcoholInf
                    )
//                    listOfUnsafeBehaviours.forEach {
//
//                        val unsafeBehaviourCopy = it.copy(
//                            alcoholInfluence = alcoholInf
//                        )
//
//                        unsafeBehaviourRepository.updateUnsafeBehaviour(
//                            unsafeBehaviourCopy.toDomainModel()
//                        )
//                    }
                }
            }
        }
        Log.d("UnsafeBehav","Unsafe Behaviours $listOfUnsafeBehaviours")
        return listOfUnsafeBehaviours
    }
}

class BatchUpDateUnsafeBehaviourCauseUseCase @Inject constructor(
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository
) {
    companion object {
//        private const val BUFFER_SIZE = 100 // Define a buffer size for batch updates
//        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Consistent date format
    }

    fun invoke(tripId: UUID, alcoholInf: Boolean): List<UnsafeBehaviourEntity> {

        val listOfUnsafeBehaviours = mutableListOf<UnsafeBehaviourEntity>()

        Log.d("UnsafeBehavio", "Unsafe Behaviours I got here UpDateUnsafeBehaviourCauseUseCase")

        CoroutineScope(Dispatchers.IO).launch {
            val currentTripUnsafeBehaviour = unsafeBehaviourRepository.getEntitiesToBeUpdated(tripId)

            currentTripUnsafeBehaviour.collect { unsafeBehaviours ->
                unsafeBehaviours.forEach {
                    if (it.locationId != null) {
                        listOfUnsafeBehaviours.add(it)
                    }
                }

                if (listOfUnsafeBehaviours.isNotEmpty()) {
                    Log.d("UnsafeBehav", "Unsafe Behaviours to be updated: $listOfUnsafeBehaviours")
                    unsafeBehaviourRepository.updateUnsafeBehaviourTransactions(
                        listOfUnsafeBehaviours,
                        alcoholInf
                    )
//                    listOfUnsafeBehaviours.forEach { unsafeBehaviour ->
                        // Ensure date fields conform to the existing date format
//                        val formattedCreatedDate = unsafeBehaviour.date?.let { formatToDate(it) }
//                        val formattedUpdatedDate = unsafeBehaviour.updatedAt?.let { Date()}

//                        val updatedEntity = unsafeBehaviour.copy(
//                            alcoholInfluence = alcoholInf,
//                            updatedAt = Date(timestamp),
//                            updated = true
//                        )
//
//                        buffer.add(updatedEntity)
//
//                        // Perform batch update if buffer size is reached
//                        if (buffer.size >= BUFFER_SIZE) {
//                            unsafeBehaviourRepository.batchUpdateUnsafeBehaviours(buffer.toList())
//                            Log.d("UnsafeBehav", "Batch update executed with ${buffer.size} entities $updatedEntity")
//                            buffer.clear()
//                        }
//                    }
//
//                    // Perform any remaining updates for smaller buffers
//                    if (buffer.isNotEmpty()) {
//                        unsafeBehaviourRepository.batchUpdateUnsafeBehaviours(buffer.toList())
//                        Log.d("UnsafeBehav", "Final batch update executed with ${buffer.size} entities")
//                        buffer.clear()
//                    }
                }
            }
        }

        Log.d("UnsafeBehav", "Unsafe Behaviours Here $listOfUnsafeBehaviours")
        return listOfUnsafeBehaviours
    }

    // Function to format Date objects to match the existing date format
//    private fun formatToDate(date: Date): Date? {
//        return try {
//            // Format the date to "yyyy-MM-dd" and parse it back to Date object
//            val formattedDateString = DATE_FORMAT.format(date)
//            DATE_FORMAT.parse(formattedDateString)
//        } catch (e: Exception) {
//            Log.e("DateFormat", "Error formatting date: ${e.message}")
//            null // Return null or handle a default date in case of error
//        }
//    }
}

class SaveInfluenceToCause @Inject constructor(private val causeRepository: CauseRepository, private val unsafeBehaviourRepository: UnsafeBehaviourRepository){
                fun invoke(tripId: UUID, alcoholInfl: Boolean): List<UnsafeBehaviourEntity>{
                    val listOfUnsafeBehaviours = mutableListOf<UnsafeBehaviourEntity>()
                    Log.d("Cause","Causes I got here SaveInfluenceToCause")
                    CoroutineScope(Dispatchers.IO).launch{
                     val currentTripUnsafeBehaviour = unsafeBehaviourRepository.getUnsafeBehavioursByTripId(tripId)
                     currentTripUnsafeBehaviour.collect { unsafeBehaviours ->
                        unsafeBehaviours.forEach {
                            if (it.locationId!=null)
                                listOfUnsafeBehaviours.add(it.toEntity())
                        }
                        Log.d("UnsafeBehaviours",listOfUnsafeBehaviours.toString())
                        if (listOfUnsafeBehaviours.isNotEmpty()) {
                            listOfUnsafeBehaviours.forEach {
                                val cause=CauseEntity(id=UUID.randomUUID(),unsafeBehaviourId = it.id, name = "alcohol",alcoholInfl, createdAt = Date().toString(), updatedAt = null)
                                Log.d("Cause","Causes $cause")
                                causeRepository.insertCause(cause)
                            }
                        }
                    }
                }
                    Log.d("UnsafeBehavio","Unsafe Behaviours $listOfUnsafeBehaviours")
                return listOfUnsafeBehaviours
                }
}

class BatchInsertCauseUseCase @Inject constructor(
    private val causeRepository: CauseRepository,
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository
) {
    companion object {
        private const val BUFFER_SIZE = 100 // Define a buffer size for batch inserts

    }

    fun invoke(tripId: UUID, alcoholInfl: Boolean): List<UnsafeBehaviourEntity> {
        val timestamp = Instant.now().toEpochMilli()
        val listOfUnsafeBehaviours = mutableListOf<UnsafeBehaviourEntity>()
        val buffer = mutableListOf<CauseEntity>() // Buffer for batching inserts

        Log.d("Cause", "Causes I got here SaveInfluenceToCause")

        CoroutineScope(Dispatchers.IO).launch {
            val currentTripUnsafeBehaviour = unsafeBehaviourRepository.getEntitiesToBeUpdated(tripId)

            currentTripUnsafeBehaviour.collect { unsafeBehaviours ->
                unsafeBehaviours.forEach {
                    if (it.locationId != null) {
                        listOfUnsafeBehaviours.add(it)
                    }
                }

                Log.d("UnsafeBehaviours", " Unsafe Behaviours for Cause: $listOfUnsafeBehaviours")

                if (listOfUnsafeBehaviours.isNotEmpty()) {
                    listOfUnsafeBehaviours.forEach { unsafeBehaviour ->
                        // Ensure date fields conform to the existing date format
//                        val formattedCreatedDate = unsafeBehaviour.date?.let { formatToDate(Date()) }
//                        val formattedUpdatedDate = unsafeBehaviour.updatedAt?.let { formatToDate(Date()) }

                        // Create a CauseEntity for each UnsafeBehaviourEntity
                        val cause = CauseEntity(
                            id = UUID.randomUUID(),
                            unsafeBehaviourId = unsafeBehaviour.id,
                            name = "alcohol",
                            influence = alcoholInfl,
                            createdAt = Date(timestamp).toString(), // Ensure the date format is correct
                            updatedAt = null
                        )

                        buffer.add(cause)

                        // Perform batch insert if buffer size is reached
                        if (buffer.size >= BUFFER_SIZE) {
                            causeRepository.batchInsertCauses(buffer.toList())
                            Log.d("Cause", "Batch insert executed with ${buffer.size} causes")
                            buffer.clear() // Clear the buffer after batch insert
                        }
                    }

                    // Insert any remaining causes in the buffer
                    if (buffer.isNotEmpty()) {
                        causeRepository.batchInsertCauses(buffer.toList())
                        Log.d("Cause", "Final batch insert executed with ${buffer.size} causes")
                        buffer.clear()
                    }
                }
            }
        }

        Log.d("UnsafeBehavio", "Unsafe Behaviours derived causes for Insertion $listOfUnsafeBehaviours")
        return listOfUnsafeBehaviours
    }

    // Function to format Date objects to match the existing date format
//    private fun formatToDate(date: Date): Date? {
//        return try {
//            // Format the date to "yyyy-MM-dd" and parse it back to Date object
//            val formattedDateString = DATE_FORMAT.format(date)
//            DATE_FORMAT.parse(formattedDateString)
//        } catch (e: Exception) {
//            Log.e("DateFormat", "Error formatting date: ${e.message}")
//            null // Return null or handle a default date in case of error
//        }
//    }
}
