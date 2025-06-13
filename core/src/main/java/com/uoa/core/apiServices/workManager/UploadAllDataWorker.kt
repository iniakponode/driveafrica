package com.uoa.core.apiServices.workManager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uoa.core.apiServices.models.aiModelInputModels.AIModelInputCreate
import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireCreate
import com.uoa.core.apiServices.models.driverProfile.DriverProfileCreate
import com.uoa.core.apiServices.models.locationModels.LocationCreate
import com.uoa.core.apiServices.models.rawSensorModels.RawSensorDataCreate
import com.uoa.core.apiServices.models.roadModels.RoadCreate
import com.uoa.core.apiServices.models.tripModels.TripCreate
import com.uoa.core.apiServices.models.unsafeBehaviourModels.UnsafeBehaviourCreate
import com.uoa.core.apiServices.services.aiModellInputApiService.AIModelInputApiRepository
import com.uoa.core.apiServices.services.alcoholQuestionnaireService.QuestionnaireApiRepository
import com.uoa.core.apiServices.services.driverProfileApiService.DriverProfileApiRepository
import com.uoa.core.apiServices.services.drivingTipApiService.DrivingTipApiRepository
import com.uoa.core.apiServices.services.locationApiService.LocationApiRepository
import com.uoa.core.apiServices.services.nlgReportApiService.NLGReportApiRepository
import com.uoa.core.apiServices.services.rawSensorApiService.RawSensorDataApiRepository
import com.uoa.core.apiServices.services.reportStatisticsApiService.ReportStatisticsApiRepository
import com.uoa.core.apiServices.services.roadApiService.RoadApiRepository
import com.uoa.core.apiServices.services.tripApiService.TripApiRepository
import com.uoa.core.apiServices.services.unsafeBehaviourApiService.UnsafeBehaviourApiRepository
import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.DriverProfileRepository
import com.uoa.core.database.repository.DrivingTipRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.NLGReportRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.ReportStatisticsRepository
import com.uoa.core.database.repository.RoadRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.Trip
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.notifications.VehicleNotificationManager
import com.uoa.core.utils.DateConversionUtils
import com.uoa.core.utils.PreferenceUtils
import com.uoa.core.utils.Resource
import com.uoa.core.utils.isConnectedToInternet
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import com.uoa.core.utils.toReportStatisticsCreate
import com.uoa.core.utils.toTrip
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.uoa.core.database.repository.QuestionnaireRepository
import com.uoa.core.utils.Resource.Success
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME

@HiltWorker
class UploadAllDataWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: RawSensorDataApiRepository,
    private val locationLocalRepository: LocationRepository,
    private val locationApiRepository: LocationApiRepository,
    private val localRawDataRepository: RawSensorDataRepository,
    private val unsafeBehavioursLocalRepository: UnsafeBehaviourRepository,
    private val unsafeBehavioursApiRepository: UnsafeBehaviourApiRepository,
    private val aiModelInputLocalRepository: AIModelInputRepository,
    private val aiModelInputApiRepository: AIModelInputApiRepository,
    private val reportStatisticsLocalRepository: ReportStatisticsRepository,
    private val reportStatisticsApiRepository: ReportStatisticsApiRepository,
    private val driverProfileLocalRepository: DriverProfileRepository,
    private val driverProfileApiRepository: DriverProfileApiRepository,
    private val tripLocalRepository: TripDataRepository,
    private val tripApiRepository: TripApiRepository,
    private val roadLocalRepository: RoadRepository,
    private val roadApiRepository: RoadApiRepository,
    private val questionnaireLocalRepository: QuestionnaireRepository,
    private val questionnaireApiRepository: QuestionnaireApiRepository,
    private val vehicleNotificationManager: VehicleNotificationManager,
) : CoroutineWorker(appContext, workerParams) {

    // Prepare date/time fields
    val sdf = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        Locale.getDefault()
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC+1")
    }

    companion object {
        private const val BATCH_SIZE = 500 // Adjust as needed
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Check network connectivity
        if (!isConnectedToInternet(applicationContext)) {
            vehicleNotificationManager.displayNotification(
                title = "Upload Failed",
                message = "No internet connectivity. Please check your network."
            )
            Log.w("UploadAllDataWorker", "No internet connectivity. Retrying...")
            return@withContext Result.retry()
        }

        // Define upload order based on dependencies
        val uploadSequence = listOf(
            ::uploadDriverProfile,
            ::uploadTrips,
            ::uploadLocations,
            ::uploadDrivingTips,
            ::uploadUnsafeBehaviours,
            ::uploadRawSensorData,
            ::uploadAIModelInputs,
//            ::uploadReportStatistics,
            ::uploadQuestionnaires,
            ::uploadRoads,
        )

        // Execute uploads in sequence
        for (uploadAction in uploadSequence) {
            val success = uploadAction()
            if (!success) {
                Log.e("UploadAllDataWorker", "Failed to upload ${uploadAction.name}. Retrying...")
                return@withContext Result.retry()
            }
        }

        // All uploads succeeded
        Log.d("UploadAllDataWorker", "All data uploads succeeded.")
        Result.success()
    }

//    /**
//     * Helper function to check network connectivity
//     */
//    private fun isConnectedToInternet(context: Context): Boolean {
//        val connectivityManager =
//            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        val capabilities =
//            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
//        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
//    }

    /****
     * Upload Single Driver Profile
     ****/
    private suspend fun uploadDriverProfile(): Boolean {
        val unsyncedProfile =
            driverProfileLocalRepository.getDriverProfileBySyncStatus(false).firstOrNull()
        if (unsyncedProfile == null) {
            Log.d("UploadDriverProfile", "No unsynced DriverProfile found.")
            return true // Nothing to upload
        }

        vehicleNotificationManager.displayNotification(
            "Data Upload: Driver Profile",
            "Uploading Driver Profile..."
        )
        Log.d("UploadDriverProfile", "Uploading DriverProfile ${unsyncedProfile.driverProfileId}.")

        // Map local entity to DTO for the API request
        val profileCreate = DriverProfileCreate(
            driverProfileId = unsyncedProfile.driverProfileId,
            email = unsyncedProfile.email,
            sync = true
        )

        // Attempt to upload
        return when (val result = driverProfileApiRepository.createDriverProfile(profileCreate)) {
            is Success -> {
                val returnedProfile = result.data
                // Use the server's actual ID, etc.
                val updatedEntity = DriverProfileEntity(
                    driverProfileId = returnedProfile.driverProfileId,
                    email = returnedProfile.email,
                    sync = returnedProfile.sync
                )
                driverProfileLocalRepository.updateDriverProfileByEmail(driverProfileId = returnedProfile.driverProfileId,
                    email = returnedProfile.email,
                    sync = returnedProfile.sync)

                // ***** IMPORTANT: store the updated driverProfileId in SharedPreferences *****
                val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(DRIVER_PROFILE_ID, returnedProfile.driverProfileId.toString())
                    .apply()

                Log.d(
                    "UploadDriverProfile",
                    "DriverProfile updated locally with server data. " +
                            "ID: ${returnedProfile.driverProfileId}, email: ${returnedProfile.email}, sync=${returnedProfile.sync}"
                )

                Log.d("UploadDriverProfile", "DriverProfile updated locally: $updatedEntity")
                true
            }

            is Resource.Error -> {
                vehicleNotificationManager.displayNotification(
                    "Data Upload: Driver Profile",
                    "Failed to upload Driver Profile. Retrying..."
                )
                Log.e(
                    "UploadDriverProfile",
                    "Error uploading ${unsyncedProfile.driverProfileId}: ${result.message}"
                )
                false
            }

            Resource.Loading -> {
                Log.d("UploadDriverProfile", "DriverProfile upload is loading.")
                true
            }
        }
    }



        /**
     * Generic function to handle batch uploads with safe mapping.
     * It maps each record using a try-catch so that if mapping fails (e.g., due to null pointers),
     * that record is skipped rather than causing the entire batch to fail.
     */
    private suspend fun <T, R> uploadInBatches(
        notificationTitle: String,
        fetchData: suspend () -> List<T>,
        mapToCreate: (T) -> R,
        batchUploadAction: suspend (List<R>) -> Resource<Unit>,
        markAsSynced: suspend (List<T>) -> Unit
    ): Boolean {
        val data = fetchData()
        if (data.isEmpty()) {
            Log.d("UploadInBatches", "$notificationTitle: No data to upload.")
            return true
        }

        val chunks = data.chunked(BATCH_SIZE)
        for ((index, chunk) in chunks.withIndex()) {
            val originalChunk = chunk
//            val mappedChunk = chunk.map { mapToCreate(it) }
            val mappedChunk = chunk.mapNotNull { item ->
                try {
                    mapToCreate(item)
                } catch (e: Exception) {
                    Log.e("BatchMapping", "Error mapping item: ${e.message}")
                    null // skip this item to avoid crashing the whole batch
                }
            }

            vehicleNotificationManager.displayNotification(
                notificationTitle,
                "Uploading batch ${index + 1} of ${chunks.size}..."
            )
            Log.d("UploadInBatches", "Uploading batch ${index + 1} of ${chunks.size} for $notificationTitle.")

            when (val result = batchUploadAction(mappedChunk)) {
                is Resource.Success -> {
                    // Mark the original data as synced
                    markAsSynced(originalChunk)
                    Log.d("UploadInBatches", "Batch ${index + 1} for $notificationTitle uploaded successfully.")
                }
                is Resource.Error -> {
                    // If it's a NullPointerException or other specific errors, skip the record and continue
                    if (isNullPointerError(result.message)) {
                        Log.e("UploadInBatches", "NullPointer error in batch ${index + 1} for $notificationTitle: ${result.message}. Skipping problematic record.")
                        continue // Skip to the next record in the batch
                    } else if (isDataIntegrityError(result.message)) {
                        Log.e("UploadInBatches", "Integrity error in batch ${index + 1} for $notificationTitle: ${result.message}. Skipping problematic record.")
                        continue // Skip the problematic record
                    } else {
                        vehicleNotificationManager.displayNotification(
                            notificationTitle,
                            "Failed to upload batch ${index + 1}. Retrying..."
                        )
                        Log.e("UploadInBatches", "Error uploading batch ${index + 1} for $notificationTitle: ${result.message}")
                        return false // Retry the whole batch if it's not an integrity issue or null pointer error
                    }
                }
                Resource.Loading -> {
                    vehicleNotificationManager.displayNotification(
                        notificationTitle,
                        "Uploading..."
                    )
                    Log.d("UploadInBatches", "$notificationTitle: Uploading batch ${index + 1}.")
                }
            }
        }
        return true
    }



    /**
     * Upload Trips: Handle both creation and updates
     */
    private suspend fun uploadTrips(): Boolean {
        // Handle new trips (creation)
        val newTrips = tripLocalRepository.getNewTrips().filter { it.sync == false } // Only unsynced trips should be posted
        val created = if (newTrips.isNotEmpty()) uploadNewTrips(newTrips) else true

        if (!created) return false

        // Handle updated trips (updates)
        val updatedTrips = tripLocalRepository.getUpdatedTrips() // Trips with sync=false and endDate/endTime!=null
        val updated = if (updatedTrips.isNotEmpty()) uploadUpdatedTrips(updatedTrips) else true
//        // Only update trips that have already been successfully uploaded
//        val updatedTrips = tripLocalRepository.getUpdatedTrips().filter { it.sync == true } // Only synced trips should be updated
//        val updated = if (updatedTrips.isNotEmpty()) uploadUpdatedTrips(updatedTrips) else true

        return updated
    }

    /**
     * Upload New Trips via Batch Create Endpoint
     */
    private suspend fun uploadNewTrips(trips: List<Trip>): Boolean {
        vehicleNotificationManager.displayNotification(
            "Data Upload: New Trips",
            "Uploading ${trips.size} new trips..."
        )
        Log.d("UploadTrips", "Uploading ${trips.size} new trips.")

        val tripCreates = trips.map { trip ->
            TripCreate(
                id = trip.id,
                driverProfileId = trip.driverPId,
                start_date = trip.startDate?.let { DateConversionUtils.dateToString(it) },
                end_date = trip.endDate?.let { DateConversionUtils.dateToString(it) },
                start_time = trip.startTime,
                end_time = trip.endTime,
                sync = true,
                influence = trip.influence!!
            )
        }

        return when (val result = tripApiRepository.batchCreateTrips(tripCreates)) {
            is Success -> {
                var successCount = 0
                var failedCount = 0

                trips.forEach { originalTrip ->
                    val matchingDTO = tripCreates.firstOrNull { it.id == originalTrip.id }
                    if (matchingDTO != null) {
                        try {
                            val tripToUpdate = matchingDTO.toTrip()
                            tripLocalRepository.updateTrip(tripToUpdate)
                            successCount++
                        } catch (e: Exception) {
                            Log.e("UploadTrips", "Failed to update trip ${originalTrip.id}: ${e.message}")
                            failedCount++
                        }
                    } else {
                        Log.w("UploadTrips", "No matching DTO found for trip ${originalTrip.id}")
                        failedCount++
                    }
                }

                vehicleNotificationManager.displayNotification(
                    "Data Upload: New Trips",
                    "Uploading ${trips.size} trips. Successfully uploaded $successCount, failed $failedCount."
                )

                Log.d("UploadTrips", "$successCount trips marked as synced, $failedCount failed.")
                true
            }

            is Resource.Error -> {
                vehicleNotificationManager.displayNotification(
                    "Data Upload: New Trips",
                    "Failed to upload new trips: ${result.message}. Retrying..."
                )
                Log.e("UploadTrips", "Error uploading new trips: ${result.message}")
                false
            }

            Resource.Loading -> {
                Log.d("UploadTrips", "New trips upload is loading.")
                true
            }
        }
    }

//    private suspend fun uploadNewTrips(trips: List<Trip>): Boolean {
//        // Separate new and existing trips properly
////
//        return uploadInBatches(
//            notificationTitle = "Data Upload: New Trips",
//            fetchData = { tripLocalRepository.getNewTrips() },  // Fetch new trips with sync=false and endDate/endTime null
//            mapToCreate = { trip ->
//                TripCreate(
//                    id = trip.id,
//                    driverProfileId = trip.driverPId,
//                    start_date = trip.startDate?.let { DateConversionUtils.dateToString(it) },
//                    end_date = trip.endDate?.let { DateConversionUtils.dateToString(it) },
//                    start_time = trip.startTime,
//                    end_time = trip.endTime,
//                    sync = true,
//                    influence = trip.influence!!// After upload, mark as synced
//                )
//            },
//            batchUploadAction = { trips ->
//                // Use batch create endpoint to upload the trips
//                tripApiRepository.batchCreateTrips(trips)
//            },
//            markAsSynced = { batch ->
//                batch.forEach { tripCreate ->
//                    val updatedTrip = tripCreate.copy(sync = true)
//                    tripLocalRepository.updateTrip(updatedTrip)
//                    Log.d("UploadNewTrips", "Trip ${updatedTrip.id} marked as synced.")
//                }
//            }
//        )
//    }


    /**
     * Upload Updated Trips via Single Update Endpoint
     */
    private suspend fun uploadUpdatedTrips(trips: List<Trip>): Boolean {
        vehicleNotificationManager.displayNotification(
            "Data Upload: Updated Trips",
            "Uploading ${trips.size} updated trips..."
        )
        Log.d("UploadTrips", "Uploading ${trips.size} updated trips.")

        for (trip in trips) {
            if (trip.sync == false) {
                uploadNewTrips(trips)
                Log.e("UploadTrips", "Skipping trip ${trip.id} because it was already uploaded.")
                tripLocalRepository.updateUploadStatus(trip.id,true)
                continue
            }
            // Prepare date/time fields
            val sdf = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.getDefault()
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC+1")
            }
            val isoStartDate = trip.startDate?.let { sdf.format(it) }
            val isoEndDate = trip.endDate?.let { sdf.format(it) }
            // Map to DTO
            val tripUpdate = TripCreate(
                id = trip.id,
                driverProfileId = trip.driverPId,
                start_date = isoStartDate,
                end_date = isoEndDate,
                start_time = trip.startTime,
                end_time = trip.endTime,
                sync = true, // Indicate that after upload, sync should be true
                influence = trip.influence!!
            )

            // Attempt to upload via single update
            when (val result = tripApiRepository.updateTrip(tripUpdate.id, tripUpdate)) {
                is Success -> {
                    // Mark trip as synced
                    val tripcopy=tripUpdate.toTrip().copy(sync=true)
                    tripLocalRepository.updateTrip(tripcopy)
                    Log.d("UploadTrips", "Trip ${trip.id} marked as synced.")
                }
                is Resource.Error -> {
                    vehicleNotificationManager.displayNotification(
                        "Data Upload: Updated Trips",
                        "Failed to upload trip ${trip.id}. Retrying..."
                    )
                    Log.e(
                        "UploadTrips",
                        "Error uploading trip ${trip.id}: ${result.message}"
                    )
                    return false // Stop and retry
                }
                Resource.Loading -> {
                    Log.d("UploadTrips", "Trip ${trip.id} upload is loading.")
                    // Continue
                }
            }
        }
        return true
    }

    /**
     * Upload Locations
     */
    private suspend fun uploadLocations(): Boolean {
        return uploadInBatches(
            notificationTitle = "Data Upload: Locations",
            fetchData = { locationLocalRepository.getLocationBySynced(false).firstOrNull().orEmpty() },
            mapToCreate = { location ->
                val formattedDate = location.date?.let { sdf.format(it) }
                LocationCreate(
                    id = location.id,
                    longitude = location.longitude,
                    latitude = location.latitude,
                    timestamp = location.timestamp,
                    date = formattedDate?:"",
                    altitude = location.altitude,
                    speed = location.speed.toDouble(),
                    speedLimit = location.speedLimit.toDouble(),
                    distance = location.distance.toDouble(),
                    sync = true
                )
            },
            batchUploadAction = { locations ->
                locationApiRepository.batchCreateLocations(locations)
            },
            markAsSynced = { batch ->
                batch.forEach { locCreate ->
                    val originalLocation = locationLocalRepository.getLocationById(locCreate.id)
                    if (originalLocation != null) {
                        val updatedLocation = originalLocation.copy(sync = true)
                        locationLocalRepository.updateLocation(updatedLocation)
                        Log.d(
                            "UploadLocations",
                            "Location ${updatedLocation.id} marked as synced."
                        )
                    } else {
                        Log.w("UploadLocations", "Location with ID ${locCreate.id} not found for syncing.")
                    }
                }
            }
        )
    }

    /**
     * Upload Driving Tips
     */
    private suspend fun uploadDrivingTips(): Boolean {
        // TODO: Implement Driving Tips upload logic
        Log.d("UploadDrivingTips", "Driving Tips upload not implemented.")
        return true // Placeholder return until implementation
    }

    /**
     * Upload Unsafe Behaviours
     */
    private suspend fun uploadUnsafeBehaviours(): Boolean {
        return uploadInBatches(
            notificationTitle = "Data Upload: Unsafe Behaviours",
            fetchData = { unsafeBehavioursLocalRepository.getUnsafeBehavioursBySyncStatus(false) },
            mapToCreate = { ub ->
                // Ensure the necessary fields are not null before proceeding
                val formattedDate = ub.date?.let { sdf.format(it) }
                UnsafeBehaviourCreate(
                    id = ub.id,
                    trip_id = ub.tripId,
                    location_id = ub.locationId!!,
                    driverProfileId = ub.driverProfileId,
                    behaviour_type = ub.behaviorType,
                    severity = ub.severity.toDouble(),
                    timestamp = ub.timestamp,
                    date = formattedDate?:"",
                    sync = true
                )
            },
            batchUploadAction = { unsafeBehaviours ->
                unsafeBehavioursApiRepository.batchCreateUnsafeBehaviours(unsafeBehaviours)
            },
            markAsSynced = { batch ->
                batch.forEach { ubCreate ->
                    val originalBehaviour =
                        unsafeBehavioursLocalRepository.getUnsafeBehaviourById(ubCreate.id)
                    if (originalBehaviour != null) {
                        val updatedBehaviour = originalBehaviour.copy(sync = true)
                        unsafeBehavioursLocalRepository.updateUnsafeBehaviour(updatedBehaviour.toDomainModel())
                        Log.d(
                            "UploadUnsafeBehaviours",
                            "UnsafeBehaviour ${updatedBehaviour.id} marked as synced."
                        )
                    } else {
                        Log.w(
                            "UploadUnsafeBehaviours",
                            "UnsafeBehaviour with ID ${ubCreate.id} not found for syncing."
                        )
                    }
                }
            }
        )
    }

    /**
     * Upload Raw Sensor Data
     */
    private suspend fun uploadRawSensorData(): Boolean {
        val driverProfileId = PreferenceUtils.getDriverProfileId(applicationContext) ?: return false

        return uploadInBatches(
            notificationTitle = "Data Upload: Sensor Data",
            fetchData = { localRawDataRepository.getSensorDataBySyncStatus(false) },
            mapToCreate = { rs ->

                val formattedDate = rs.date?.let { sdf.format(it) }

                RawSensorDataCreate(
                    id = rs.id,
                    sensor_type = rs.sensorType,
                    sensor_type_name = rs.sensorTypeName,
                    values = rs.values,
                    timestamp = rs.timestamp,
                    date = formattedDate,
                    accuracy = rs.accuracy,
                    location_id = rs.locationId,
                    trip_id = rs.tripId!!,
                    driverProfileId = driverProfileId,
                    sync = true
                )
            },
            batchUploadAction = { rawSensorData ->
                repository.batchCreateRawSensorData(rawSensorData)
            },
            markAsSynced = { batch ->
                batch.forEach { rsCreate ->
                    // Ensure that location_id and trip_id are not null
                    if (rsCreate.tripId == null) {
                        Log.e("UploadRawSensorData", "Skipping RawSensorData with ID ${rsCreate.id} due to missing locationId or tripId.")
                        null // Skip this record if location_id or trip_id is missing
                    }
                    val originalData = localRawDataRepository.getRawSensorDataById(rsCreate.id)
                    if (originalData != null) {
                        val updatedData = originalData.copy(sync = true)
                        localRawDataRepository.updateRawSensorData(updatedData)
                        Log.d(
                            "UploadRawSensorData",
                            "RawSensorData ${updatedData.id} marked as synced."
                        )
                    } else {
                        Log.w(
                            "UploadRawSensorData",
                            "RawSensorData with ID ${rsCreate.id} not found for syncing."
                        )
                    }
                }
            }
        )
    }

    /**
     * Upload Roads
     */
    private suspend fun uploadRoads(): Boolean {
        return uploadInBatches(
            notificationTitle = "Data Upload: Roads",
            fetchData = { roadLocalRepository.getRoadsBySyncStatus(false) },
            mapToCreate = { road ->
                RoadCreate(
                    id = road.id,
                    driverProfileId = road.driverProfileId,
                    name = road.name,
                    roadType = road.roadType,
                    speedLimit = road.speedLimit,
                    latitude = road.latitude,
                    longitude = road.longitude,
                    radius = road.radius,
                    sync = true
                )
            },
            batchUploadAction = { roads ->
                roadApiRepository.batchCreateRoads(roads)
            },
            markAsSynced = { batch ->
                batch.forEach { roadCreate ->
                    val originalRoad = roadLocalRepository.getRoadById(roadCreate.id)
                    if (true) {
                        val updatedRoad = originalRoad.copy(sync = true)
                        roadLocalRepository.updateRoad(updatedRoad.toEntity())
                        Log.d("UploadRoads", "Road ${updatedRoad.id} marked as synced.")
                    } else {
                        Log.w(
                            "UploadRoads",
                            "Road with ID ${roadCreate.id} not found for syncing."
                        )
                    }
                }
            }
        )
    }

    /**
     * Upload AI Model Inputs
     */
    private suspend fun uploadAIModelInputs(): Boolean {

        return uploadInBatches(
            notificationTitle = "Data Upload: AI Model Inputs",
            fetchData = { aiModelInputLocalRepository.getAiModelInputsBySyncStatus(false) },
            mapToCreate = { input ->

                val formattedDate = input.date?.let { sdf.format(it) }
                AIModelInputCreate(
                    id = input.id,
                    trip_id = input.tripId,
                    driver_profile_id = input.driverProfileId,
                    timestamp = DateConversionUtils.longToTimestampString(input.timestamp),
                    startTimeStamp = DateConversionUtils.longToTimestampString(input.startTimestamp),
                    endTimeStamp = DateConversionUtils.longToTimestampString(input.endTimestamp),
                    date = formattedDate ?: "",
                    hour_of_day_mean = input.hourOfDayMean,
                    day_of_week_mean = input.dayOfWeekMean.toDouble(),
                    speed_std = input.speedStd.toDouble(),
                    course_std = input.courseStd.toDouble(),
                    acceleration_y_original_mean = input.accelerationYOriginalMean.toDouble(),
                    sync = true // Consistent sync flag
                )
            },
            batchUploadAction = { aiModelInputs ->
                aiModelInputApiRepository.batchCreateAiModelInputs(aiModelInputs)
            },
            markAsSynced = { batch ->
                batch.forEach { aiCreate ->
                    val originalInput =
                        aiModelInputLocalRepository.getAiModelInputById(aiCreate.id)
                    if (originalInput != null) {
                        val updatedInput = originalInput.copy(sync = true)
                        aiModelInputLocalRepository.updateAiModelInput(updatedInput)
                        Log.d(
                            "UploadAIModelInputs",
                            "AIModelInput ${updatedInput.id} marked as synced."
                        )
                    } else {
                        Log.w(
                            "UploadAIModelInputs",
                            "AIModelInput with ID ${aiCreate.id} not found for syncing."
                        )
                    }
                }
            }
        )
    }

    /**
     * Upload Report Statistics
     */
    private suspend fun uploadReportStatistics(): Boolean {
        return uploadInBatches(
            notificationTitle = "Data Upload: Report Statistics",
            fetchData = { reportStatisticsLocalRepository.getReportStatisticsBySyncStatus(false) },
            mapToCreate = { report ->
                report.toReportStatisticsCreate()
            },
            batchUploadAction = { reportStatistics ->
                reportStatisticsApiRepository.batchCreateReportStatistics(reportStatistics)
            },
            markAsSynced = { batch ->
                batch.forEach { rsCreate ->
                    val originalReport = reportStatisticsLocalRepository.getReportById(rsCreate.id)
                    if (true) {
                        val updatedReport = originalReport.copy(sync = true)
                        reportStatisticsLocalRepository.updateReportStatistics(updatedReport)
                        Log.d(
                            "UploadReportStatistics",
                            "ReportStatistics ${updatedReport.id} marked as synced."
                        )
                    } else {
                        Log.w(
                            "UploadReportStatistics",
                            "ReportStatistics with ID ${rsCreate.id} not found for syncing."
                        )
                    }
                }
            }
        )
    }

//    private suspend fun uploadQuestionnaires(): Boolean {
//        // Fetch all unsynced, valid questionnaires
//        val unsyncedRecords = questionnaireLocalRepository.getAllUnsyncedQuestionnaires()
//
//        if (unsyncedRecords.isEmpty()) {
//            Log.d("UploadQuestionnaires", "No unsynced questionnaires found.")
//            return true // Nothing to upload => success
//        }
//
//        vehicleNotificationManager.displayNotification(
//            title = "Data Upload: Questionnaires",
//            message = "Uploading ${unsyncedRecords.size} questionnaires..."
//        )
//
//        for (record in unsyncedRecords) {
//            try {
//
//                val formattedDate = record.date.let { sdf.format(it) }
//                // Convert local entity to the DTO you send to server
//                val createRequest = AlcoholQuestionnaireCreate(
//                    id = record.id,
//                    driverProfileId = record.driverProfileId,
//                    drankAlcohol = record.drankAlcohol,
//                    selectedAlcoholTypes = record.selectedAlcoholTypes,
//                    beerQuantity = record.beerQuantity,
//                    wineQuantity = record.wineQuantity,
//                    spiritsQuantity = record.spiritsQuantity,
//                    firstDrinkTime = record.firstDrinkTime,
//                    lastDrinkTime = record.lastDrinkTime,
//                    emptyStomach = record.emptyStomach,
//                    caffeinatedDrink = record.caffeinatedDrink,
//                    impairmentLevel = record.impairmentLevel,
//                    date = formattedDate,
//                    plansToDrive = record.plansToDrive,
//                    sync=true
//                )
//
//                // Attempt remote upload
//                val response = questionnaireApiRepository.uploadResponseToServer(createRequest)
//
//                when (response) {
//                    is Success<*> -> {
//                        // Mark as synced
//                        questionnaireLocalRepository.markAsSynced(record.id, true)
//                        Log.d(
//                            "UploadQuestionnaires",
//                            "Questionnaire ${record.id} uploaded and marked as synced."
//                        )
//                    }
//                    is Resource.Error -> {
//                        // Check if it's a data integrity issue
//                        if (isDataIntegrityError(response.message)) {
//                            // Mark record as invalid so it won't be retried
//                            Log.e(
//                                "UploadQuestionnaires",
//                                "Data integrity error for ${record.id}, skipping permanently."
//                            )
//                        } else {
//                            // Non-integrity error: retry later
//                            Log.e(
//                                "UploadQuestionnaires",
//                                "Non-integrity error uploading ${record.id}: ${response.message}"
//                            )
//                            vehicleNotificationManager.displayNotification(
//                                title = "Data Upload: Questionnaires",
//                                message = "Failed to upload questionnaires. Retrying..."
//                            )
//                            return false
//                        }
//                    }
//                    is Resource.Loading -> {
//                        // This branch is rarely reached in a Worker context.
//                        Log.d("UploadQuestionnaires", "Questionnaire ${record.id} is loading...")
//                    }
//                }
//            } catch (e: Exception) {
//                // If an exception is thrown outside Resource handling.
//                if (isDataIntegrityError(e.message ?: "")) {
//                    Log.e(
//                        "UploadQuestionnaires",
//                        "Data integrity error for ${record.id}, skipping permanently."
//                    )
//                } else {
//                    Log.e(
//                        "UploadQuestionnaires",
//                        "Error uploading ${record.id}: ${e.message}. Retrying..."
//                    )
//                    return false
//                }
//            }
//        }
//
//        return true
//    }

    private suspend fun uploadQuestionnaires(): Boolean {
        return uploadInBatches(
            notificationTitle = "Data Upload: Questionnaires",
            fetchData = { questionnaireLocalRepository.getAllUnsyncedQuestionnaires() }, // Fetch unsynced questionnaires
            mapToCreate = { record ->
                val formattedDate = record.date.let { sdf.format(it) } ?: ""
                AlcoholQuestionnaireCreate(
                    id = record.id,
                    driverProfileId = record.driverProfileId,
                    drankAlcohol = record.drankAlcohol,
                    selectedAlcoholTypes = record.selectedAlcoholTypes,
                    beerQuantity = record.beerQuantity,
                    wineQuantity = record.wineQuantity,
                    spiritsQuantity = record.spiritsQuantity,
                    firstDrinkTime = record.firstDrinkTime,
                    lastDrinkTime = record.lastDrinkTime,
                    emptyStomach = record.emptyStomach,
                    caffeinatedDrink = record.caffeinatedDrink,
                    impairmentLevel = record.impairmentLevel,
                    date = formattedDate,
                    plansToDrive = record.plansToDrive,
                    sync = true // After upload, mark as synced
                )
            },
            batchUploadAction = { questionnaires ->
                // Attempt to upload via the API
                questionnaireApiRepository.uploadResponsesToServer(questionnaires)
            },
            markAsSynced = { batch ->
                batch.forEach { questionnaire ->
                    // Mark each successfully uploaded questionnaire as synced in the local database
                    questionnaireLocalRepository.markAsSynced(questionnaire.id, true)
                    Log.d("UploadQuestionnaires", "Questionnaire ${questionnaire.id} uploaded and marked as synced.")
                }
            }
        )
    }


    private fun isDataIntegrityError(message: String): Boolean {
        return message.contains("Integrity", ignoreCase = true) ||
                message.contains("Invalid data", ignoreCase = true)
    }
    // Function to check for specific errors like NullPointerException
    private fun isNullPointerError(message: String?): Boolean {
        return message?.contains("NullPointerException", ignoreCase = true) == true
    }

}