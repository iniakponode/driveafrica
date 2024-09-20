package com.uoa.ml.domain

import android.provider.Settings.System.DATE_FORMAT
import android.util.Log
import com.uoa.core.database.entities.CauseEntity
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.database.repository.CauseRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.mlclassifier.OnnxModelRunner
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import com.uoa.ml.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class RunClassificationUseCase @Inject constructor(val utils: Utils,
                                                   private val rawSensorDataRepository: RawSensorDataRepository,
                                                   private val locationRepo: LocationRepository,
                                                   private val onnxModelRunner: OnnxModelRunner) {

    fun invoke(tripId: UUID): Boolean{

        var alcoholInfluence = false
        CoroutineScope(Dispatchers.IO).launch {

            val rawSensorDataList = rawSensorDataRepository.getSensorDataByTripId(tripId)
            // Extract timestamps and convert to Timestamp objects
            val timestamps = mutableListOf<Timestamp>()
            rawSensorDataList.collect { rawSensorData ->
                rawSensorData.forEach() {
                    if (it.locationId!=null)
                        timestamps.add(Timestamp(it.timestamp))
                }
            }
            val speeds= mutableListOf<Float>()
            rawSensorDataList.collect { rawSensorData ->
                rawSensorData.forEach() {
                    if (it.locationId!=null)
                        speeds.add(it.values[0])
                }
            }

            val collectedRawSensorDataList= mutableListOf<RawSensorDataEntity>()

            rawSensorDataList.collect { rawSensorData ->
                rawSensorData.forEach() {
                    if (it.locationId!=null)
                        collectedRawSensorDataList.add(it)
                }
            }

            val listOfAccelerationY= mutableListOf<Float>()
            rawSensorDataList.collect { rawSensorData ->
                rawSensorData.forEach() {
                    if (it.sensorType==1 && it.locationId!=null)
                        listOfAccelerationY.add(it.values[1])
                }
            }

            val locations = locationRepo.getLocationDataByTripId(tripId)


            if (timestamps.isNotEmpty()){
                val hourOfDayMean = utils.extractHourOfDayMean(timestamps)
                val dayOfWeekMean = utils.extractDayOfWeekMean(timestamps)

                // Assuming utils functions take Timestamp objects
                val speedStd = utils.extractSpeedStd(speeds)
                val courseStd = utils.computeStandardDeviationOfCourses(
                    collectedRawSensorDataList,
                    locations[0],
                    locations[1],
                    locations[2]
                )
                val accelerationYOriginalMean = utils.extractAccelerationYOriginalMean(listOfAccelerationY)

                alcoholInfluence=onnxModelRunner.runInference(hourOfDayMean, dayOfWeekMean, speedStd, courseStd, accelerationYOriginalMean)
            } else {
                // Handle the case where no timestamps are available
                Log.d("Alcohol TimeStamps", "No timestamps available")
            }
        }
        Log.d("Alcohol Infl", alcoholInfluence.toString())
        return alcoholInfluence
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
        private const val BUFFER_SIZE = 100 // Define a buffer size for batch updates
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Consistent date format
    }

    fun invoke(tripId: UUID, alcoholInf: Boolean): List<UnsafeBehaviourEntity> {
        val timestamp = Instant.now().toEpochMilli()
        val listOfUnsafeBehaviours = mutableListOf<UnsafeBehaviourEntity>()
        val buffer = mutableListOf<UnsafeBehaviourEntity>() // Buffer for batching

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
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Consistent date format
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