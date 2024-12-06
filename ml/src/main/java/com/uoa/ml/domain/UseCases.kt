package com.uoa.ml.domain

import android.hardware.Sensor
import android.util.Log
import com.uoa.core.database.entities.CauseEntity
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.database.repository.CauseRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import com.uoa.core.mlclassifier.OnnxModelRunner
import com.uoa.core.utils.toEntity
import com.uoa.ml.UtilsNew
import com.uoa.core.mlclassifier.data.InferenceResult
import com.uoa.core.mlclassifier.data.TripFeatures
import com.uoa.ml.utils.IncrementalAccelerationYMean
import com.uoa.ml.utils.IncrementalCourseStd
import com.uoa.ml.utils.IncrementalDayOfWeekMean
import com.uoa.ml.utils.IncrementalHourOfDayMean
import com.uoa.ml.utils.IncrementalSpeedStd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.withContext
import java.util.TimeZone

// RunClassificationUseCase.kt
import kotlinx.coroutines.Dispatchers
import java.sql.Timestamp
import java.util.*

class RunClassificationUseCase @Inject constructor(
//    private val utils: UtilsNew,
    private val rawSensorDataRepository: RawSensorDataRepository,
    private val onnxModelRunner: OnnxModelRunner,
    private val incrementalCourseStdProvider: IncrementalCourseStd,
    private val incrementalSpeedStdProvider: IncrementalSpeedStd,
    private val incrementalAccelerationYMeanProvider: IncrementalAccelerationYMean,
    private val incrementalHourOfDayMeanProvider: IncrementalHourOfDayMean,
    private val minMaxValuesLoader: MinMaxValuesLoader,
    private val incrementalDayOfWeekMeanProvider: IncrementalDayOfWeekMean
) {

    suspend fun invoke(tripId: UUID): InferenceResult {
        Log.i("Trip", "Classifier invoked for tripId: $tripId")

        return withContext(Dispatchers.IO) {
            // Initialize incremental calculators
            val hourOfDayMeanCalc = incrementalHourOfDayMeanProvider
            val dayOfWeekMeanCalc = incrementalDayOfWeekMeanProvider
            val speedStdCalc = incrementalSpeedStdProvider
            val accelerationYMeanCalc = incrementalAccelerationYMeanProvider
            val courseStdCalc = incrementalCourseStdProvider

            try {
                // Fetch raw sensor data in chunks and process incrementally
                val rawSensorDataFlow = rawSensorDataRepository.getAllRawSensorDataInChunks(tripId)
                    .onStart { Log.i("Trip", "Starting to collect raw sensor data for tripId: $tripId") }
                    .catch { e ->
                        Log.e("Trip", "Error fetching raw sensor data: ${e.message}", e)
                        throw e
                    }

                rawSensorDataFlow.collect { rawSensorDataChunk ->
                    try {
                        for (data in rawSensorDataChunk) {
                            if (data.locationId != null) {
                                // Update hour of day and day of week means
                                hourOfDayMeanCalc.addTimestamp(data.timestamp)
                                dayOfWeekMeanCalc.addTimestamp(data.timestamp)

                                // Update speed standard deviation
                                val speed = data.values.getOrNull(0) ?: continue
                                speedStdCalc.addSpeed(speed)

                                // Update acceleration Y mean
                                if (data.sensorType == Sensor.TYPE_ACCELEROMETER) {
                                    val accelerationY = data.values.getOrNull(1) ?: continue
                                    accelerationYMeanCalc.addAccelerationY(accelerationY)
                                }

                                // Update course standard deviation
                                courseStdCalc.addSensorData(data)
                            } else {
                                Log.w("Trip", "Data with null locationId encountered: $data")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Trip", "Error processing data chunk: ${e.message}", e)
                        throw e
                    }
                }

                // Compute normalized features
                val tripFeatures = TripFeatures(
                    hourOfDayMean = hourOfDayMeanCalc.getNormalizedMean(minMaxValuesLoader),
                    dayOfWeekMean = dayOfWeekMeanCalc.getNormalizedMean(minMaxValuesLoader),
                    speedStd = speedStdCalc.getNormalizedStd(),
                    courseStd = courseStdCalc.getNormalizedStd(),
                    accelerationYOriginalMean = accelerationYMeanCalc.getNormalizedMean()
                )

                Log.d("Trip", "Extracted Features: $tripFeatures")

                // Run inference
                try {
                    val inferenceResult = onnxModelRunner.runInference(tripFeatures)
                    Log.i("Trip", "Inference result: $inferenceResult")
                    InferenceResult.Success(inferenceResult)
                } catch (e: Exception) {
                    Log.e("Trip", "Error during model inference: ${e.message}", e)
                    InferenceResult.Failure(e)
                }
            } catch (e: Exception) {
                Log.e("Trip", "Exception in invoke function: ${e.message}", e)
                InferenceResult.Failure(e)
            }
        }
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