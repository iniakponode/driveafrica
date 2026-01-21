package com.uoa.core.apiServices.workManager

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uoa.core.apiServices.models.aiModelInputModels.AIModelInputCreate
import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireCreate
import com.uoa.core.apiServices.models.auth.RegisterRequest
import com.uoa.core.apiServices.models.driverSyncModels.DriverProfileReference
import com.uoa.core.apiServices.models.driverSyncModels.DriverSyncPayload
import com.uoa.core.apiServices.models.locationModels.LocationCreate
import com.uoa.core.apiServices.models.rawSensorModels.RawSensorDataCreate
import com.uoa.core.apiServices.models.roadModels.RoadCreate
import com.uoa.core.apiServices.models.tripModels.TripCreate
import com.uoa.core.apiServices.models.unsafeBehaviourModels.UnsafeBehaviourCreate
import com.uoa.core.apiServices.services.aiModellInputApiService.AIModelInputApiRepository
import com.uoa.core.apiServices.services.alcoholQuestionnaireService.QuestionnaireApiRepository
import com.uoa.core.apiServices.services.auth.AuthRepository
import com.uoa.core.apiServices.services.driverSyncApiService.DriverSyncApiRepository
import com.uoa.core.apiServices.services.drivingTipApiService.DrivingTipApiRepository
import com.uoa.core.apiServices.services.fleetApiService.DriverFleetApiRepository
import com.uoa.core.apiServices.services.locationApiService.LocationApiRepository
import com.uoa.core.apiServices.services.nlgReportApiService.NLGReportApiRepository
import com.uoa.core.apiServices.services.rawSensorApiService.RawSensorDataApiRepository
import com.uoa.core.apiServices.services.reportStatisticsApiService.ReportStatisticsApiRepository
import com.uoa.core.apiServices.services.roadApiService.RoadApiRepository
import com.uoa.core.apiServices.services.tripApiService.TripApiRepository
import com.uoa.core.apiServices.services.tripFeatureStateApiService.TripFeatureStateApiRepository
import com.uoa.core.apiServices.services.tripSummaryApiService.TripSummaryApiRepository
import com.uoa.core.apiServices.services.tripSummaryBehaviourApiService.TripSummaryBehaviourApiRepository
import com.uoa.core.apiServices.services.unsafeBehaviourApiService.UnsafeBehaviourApiRepository
import com.uoa.core.database.entities.RoadEntity
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
import com.uoa.core.database.repository.TripFeatureStateRepository
import com.uoa.core.database.repository.TripSummaryRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.database.repository.QuestionnaireRepository
import com.uoa.core.model.Trip
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.notifications.VehicleNotificationManager
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Constants.Companion.REGISTRATION_INVITE_CODE
import com.uoa.core.utils.Constants.Companion.UPLOAD_AUTH_REMINDER_TIMESTAMP
import com.uoa.core.utils.DateConversionUtils
import com.uoa.core.utils.PreferenceUtils
import com.uoa.core.utils.Resource
import com.uoa.core.utils.Resource.Success
import com.uoa.core.utils.SecureCredentialStorage
import com.uoa.core.utils.SecureTokenStorage
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toDrivingTipCreate
import com.uoa.core.utils.toEntity
import com.uoa.core.utils.toNLGReportCreate
import com.uoa.core.utils.toReportStatisticsCreate
import com.uoa.core.utils.toTripSummaryCreate
import com.uoa.core.utils.toTripSummaryBehaviourCreates
import com.uoa.core.utils.toTrip
import com.uoa.core.utils.toTripFeatureStateCreate
import com.uoa.core.network.NetworkMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.pow
import kotlin.math.round

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
    private val driverSyncApiRepository: DriverSyncApiRepository,
    private val driverFleetApiRepository: DriverFleetApiRepository,
    private val tripLocalRepository: TripDataRepository,
    private val tripApiRepository: TripApiRepository,
    private val tripSummaryRepository: TripSummaryRepository,
    private val tripSummaryApiRepository: TripSummaryApiRepository,
    private val tripSummaryBehaviourApiRepository: TripSummaryBehaviourApiRepository,
    private val tripFeatureStateRepository: TripFeatureStateRepository,
    private val tripFeatureStateApiRepository: TripFeatureStateApiRepository,
    private val roadLocalRepository: RoadRepository,
    private val roadApiRepository: RoadApiRepository,
    private val questionnaireLocalRepository: QuestionnaireRepository,
    private val questionnaireApiRepository: QuestionnaireApiRepository,
    private val drivingTipLocalRepository: DrivingTipRepository,
    private val drivingTipApiRepository: DrivingTipApiRepository,
    private val nlgReportLocalRepository: NLGReportRepository,
    private val nlgReportApiRepository: NLGReportApiRepository,
    private val vehicleNotificationManager: VehicleNotificationManager,
    private val secureTokenStorage: SecureTokenStorage,
    private val secureCredentialStorage: SecureCredentialStorage,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
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
        private const val AUTH_REMINDER_INTERVAL_MS = 4 * 60 * 60 * 1000L // 4 hours
    }

    private val repositoryMutex = Mutex()
    private data class UploadStage(
        val label: String,
        val action: suspend () -> Boolean
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO + SupervisorJob()) {
        try {
            repositoryMutex.withLock<Result> {
                val token = secureTokenStorage.getToken()
                val storedEmail = secureCredentialStorage.getEmail()
                val storedPassword = secureCredentialStorage.getPassword()
                if (token.isNullOrBlank() &&
                    storedEmail.isNullOrBlank() &&
                    storedPassword.isNullOrBlank()
                ) {
                    val prefs =
                        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val now = System.currentTimeMillis()
                    val lastShown = prefs.getLong(UPLOAD_AUTH_REMINDER_TIMESTAMP, 0L)

                    if (now - lastShown >= AUTH_REMINDER_INTERVAL_MS) {
                        vehicleNotificationManager.displayPermissionNotification(
                            "Driver data uploads require authentication. Sign in to resume syncing."
                        )
                        prefs.edit()
                            .putLong(UPLOAD_AUTH_REMINDER_TIMESTAMP, now)
                            .apply()
                    }

                    Log.w(
                        "UploadAllDataWorker",
                        "Skipping upload; JWT token missing. Waiting until authentication completes."
                    )
                    return@withLock Result.retry()
                }
                // Check network connectivity
                if (!networkMonitor.isOnline.first()) {
                    vehicleNotificationManager.displayUploadFailure(
                        "No internet connectivity. Retrying..."
                    )
                    Log.w("UploadAllDataWorker", "No internet connectivity. Retrying...")
                    return@withLock Result.retry()
                }

                // Define upload order based on dependencies
                val uploadSequence = listOf(
                    UploadStage("Driver profile", ::registerDriverProfileIfNeeded),
                    UploadStage("Trips", ::uploadTrips),
                    UploadStage("Locations", ::uploadLocations),
                    UploadStage("Sensor data", ::uploadRawSensorData),
                    UploadStage("Unsafe behaviours", ::uploadUnsafeBehaviours),
                    UploadStage("Trip summary behaviours", ::uploadTripSummaryBehaviours),
                    UploadStage("Trip summaries", ::uploadTripSummaries),
                    UploadStage("Trip feature states", ::uploadTripFeatureStates),
                    UploadStage("Questionnaires", ::uploadQuestionnaires),
                    UploadStage("Driving tips", ::uploadDrivingTips),
                    UploadStage("NLG reports", ::uploadNLGReports),
                    UploadStage("AI model inputs", ::uploadAIModelInputs),
                    UploadStage("Report statistics", ::uploadReportStatistics),
                    UploadStage("Roads", ::uploadRoads),
                )

                // Execute uploads in sequence
                val totalStages = uploadSequence.size
                vehicleNotificationManager.displayUploadStatus(
                    title = "Uploading data",
                    message = "Preparing uploads",
                    progress = 0,
                    max = totalStages,
                    ongoing = true
                )
                for ((index, stage) in uploadSequence.withIndex()) {
                    val stageNumber = index + 1
                    vehicleNotificationManager.displayUploadStatus(
                        title = "Uploading data",
                        message = "Uploading ${stage.label} ($stageNumber/$totalStages)",
                        progress = stageNumber,
                        max = totalStages,
                        ongoing = true
                    )
                    val success = stage.action()
                    if (!success) {
                        Log.e(
                            "UploadAllDataWorker",
                            "Failed to upload ${stage.label}. Retrying..."
                        )
                        vehicleNotificationManager.displayUploadFailure(
                            "Failed to upload ${stage.label}. Retrying..."
                        )
                        return@withLock Result.retry()
                    }
                    vehicleNotificationManager.displayUploadStatus(
                        title = "Uploading data",
                        message = "${stage.label} uploaded ($stageNumber/$totalStages)",
                        progress = stageNumber,
                        max = totalStages,
                        ongoing = true
                    )
                }

                // All uploads succeeded
                Log.d("UploadAllDataWorker", "All data uploads succeeded.")
                vehicleNotificationManager.displayUploadComplete("All data uploads completed.")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("UploadAllDataWorker", "Unexpected error during upload", e)
            vehicleNotificationManager.displayUploadFailure("Upload failed. Retrying...")
            Result.retry()
        }
    }
    /****
     * Upload Single Driver Profile
     ****/
    private suspend fun registerDriverProfileIfNeeded(): Boolean {
        val pendingInviteCode = getPendingInviteCode()
        val existingToken = secureTokenStorage.getToken()
        if (!existingToken.isNullOrBlank()) {
            return handleInviteCodeJoinIfNeeded(pendingInviteCode)
        }
        if (PreferenceUtils.isRegistrationPending(applicationContext)) {
            Log.d("UploadDriverProfile", "Registration already in progress; skipping worker registration.")
            return true
        }
        if (PreferenceUtils.isRegistrationCompleted(applicationContext)) {
            Log.d("UploadDriverProfile", "Registration already completed; skipping worker registration.")
            return true
        }

        if (!validateInviteCodeIfNeeded(pendingInviteCode)) {
            return false
        }

        val unsyncedProfile =
            driverProfileLocalRepository.getDriverProfileBySyncStatus(false).firstOrNull()
        if (unsyncedProfile == null) {
            Log.d("UploadDriverProfile", "No unsynced DriverProfile found.")
            return true
        }

        val email = secureCredentialStorage.getEmail()?.trim()
        val password = secureCredentialStorage.getPassword()
        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            Log.w("UploadDriverProfile", "Missing credentials; cannot register driver profile.")
            return false
        }

        Log.d("UploadDriverProfile", "Registering driver profile for $email.")

        val registerRequest = RegisterRequest(
            driverProfileId = unsyncedProfile.driverProfileId.toString(),
            email = email,
            password = password,
            sync = true
        )

        PreferenceUtils.setRegistrationPending(applicationContext, true)
        return try {
            when (val result = authRepository.registerDriver(registerRequest)) {
                is Success -> {
                    secureTokenStorage.saveToken(result.data.token)
                    val returnedProfileId = result.data.driverProfile?.id ?: runCatching {
                        UUID.fromString(result.data.driverProfileId ?: "")
                    }.getOrNull()

                    if (returnedProfileId == null) {
                        Log.w(
                            "UploadDriverProfile",
                            "Driver profile id missing from auth response."
                        )
                        return false
                    }

                    driverProfileLocalRepository.updateDriverProfileByEmail(
                        driverProfileId = returnedProfileId,
                        sync = true,
                        email = email
                    )

                    val prefs =
                        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString(DRIVER_PROFILE_ID, result.data.driverProfileId.toString())
                        .apply()

                    Log.d(
                        "UploadDriverProfile",
                        "Driver profile ${result.data.driverProfileId} registered successfully."
                    )
                vehicleNotificationManager.displayNotification(
                    "Registration complete",
                    "Your driver profile is connected to Safe Drive Africa."
                )
                PreferenceUtils.setRegistrationCompleted(applicationContext, true)
                handleInviteCodeJoinIfNeeded(pendingInviteCode)
            }

                is Resource.Error -> {
                    Log.e("UploadDriverProfile", "Failed to register profile: ${result.message}")
                    false
                }

                Resource.Loading -> {
                    Log.d("UploadDriverProfile", "Registration request is loading.")
                    false
                }
            }
        } finally {
            PreferenceUtils.setRegistrationPending(applicationContext, false)
        }
    }


    private fun getPendingInviteCode(): String? {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(REGISTRATION_INVITE_CODE, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun clearPendingInviteCode() {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(REGISTRATION_INVITE_CODE).apply()
    }

    private suspend fun validateInviteCodeIfNeeded(inviteCode: String?): Boolean {
        if (inviteCode.isNullOrBlank()) {
            return true
        }
        return when (val result = driverFleetApiRepository.validateInviteCode(inviteCode)) {
            is Success -> true
            is Resource.Error -> {
                val message = result.message
                if (isNonRetryableInviteCodeError(message)) {
                    notifyInviteCodeIssue(message)
                    clearPendingInviteCode()
                    true
                } else {
                    Log.e("UploadDriverProfile", "Invite code validation failed: $message")
                    false
                }
            }
            Resource.Loading -> false
        }
    }

    private suspend fun handleInviteCodeJoinIfNeeded(inviteCode: String?): Boolean {
        if (inviteCode.isNullOrBlank()) {
            return true
        }
        return when (val result = driverFleetApiRepository.joinFleet(inviteCode)) {
            is Success -> {
                clearPendingInviteCode()
                vehicleNotificationManager.displayNotification(
                    "Fleet request submitted",
                    "Your invite code was accepted. Waiting for fleet approval."
                )
                true
            }
            is Resource.Error -> {
                val message = result.message
                if (isNonRetryableInviteCodeError(message)) {
                    notifyInviteCodeIssue(message)
                    clearPendingInviteCode()
                    true
                } else {
                    Log.e("UploadDriverProfile", "Join fleet failed: $message")
                    false
                }
            }
            Resource.Loading -> false
        }
    }

    private fun notifyInviteCodeIssue(message: String?) {
        val details = message?.takeIf { it.isNotBlank() }
            ?: "Invite code could not be verified. Please try again."
        vehicleNotificationManager.displayNotification("Invite code issue", details)
    }

    private fun isNonRetryableInviteCodeError(message: String?): Boolean {
        if (message.isNullOrBlank()) {
            return false
        }
        val normalized = message.lowercase(Locale.ROOT)
        return normalized.contains("invite code") ||
            normalized.contains("already part of a fleet") ||
            normalized.contains("pending request")
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

        var uploadedAny = false
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

            Log.d("UploadInBatches", "Uploading batch ${index + 1} of ${chunks.size} for $notificationTitle.")

            when (val result = batchUploadAction(mappedChunk)) {
                is Success -> {
                    // Mark the original data as synced
                    markAsSynced(originalChunk)
                    uploadedAny = true
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
                        Log.e("UploadInBatches", "Error uploading batch ${index + 1} for $notificationTitle: ${result.message}")
                        return false // Retry the whole batch if it's not an integrity issue or null pointer error
                    }
                }
                Resource.Loading -> {
                    Log.d("UploadInBatches", "$notificationTitle: Uploading batch ${index + 1}.")
                    break
                }
            }
        }
        if (uploadedAny) {
            Log.d("UploadInBatches", "$notificationTitle: Upload complete.")
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
        Log.d("UploadTrips", "Uploading ${trips.size} new trips.")

        val fallbackDriverId = PreferenceUtils.getDriverProfileId(applicationContext)
        var successCount = 0
        var failedCount = 0

        for (trip in trips) {
            val ensured = ensureTripExists(trip, fallbackDriverId)
            if (ensured) {
                successCount++
            } else {
                failedCount++
                Log.e("UploadTrips", "Failed to ensure trip ${trip.id} exists on server.")
                return false
            }
        }

        Log.d("UploadTrips", "$successCount trips marked as synced, $failedCount failed.")
        return true
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
        Log.d("UploadTrips", "Uploading ${trips.size} updated trips.")

        for (trip in trips) {
            // Prepare date/time fields
            val sdf = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.getDefault()
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC+1")
            }
            val isoStartDate = formatTripDate(trip.startDate, trip.startTime)
            val isoEndDate = formatTripDate(trip.endDate, trip.endTime)
            // Map to DTO
            val tripUpdate = TripCreate(
                id = trip.id,
                driverProfileId = trip.driverPId,
                startDate = isoStartDate,
                endDate = isoEndDate,
                startTime = trip.startTime,
                endTime = trip.endTime,
                timeZoneId = timeZoneId(),
                timeZoneOffsetMinutes = timeZoneOffsetMinutes(trip.startTime),
                sync = true, // Indicate that after upload, sync should be true
                influence = trip.influence!!,
                userAlcoholResponse = trip.userAlcoholResponse,
                alcoholProbability = trip.alcoholProbability
            )

            // Attempt to upload via single update
            when (val result = tripApiRepository.updateTrip(tripUpdate.id, tripUpdate)) {
                is Success -> {
                    // Mark trip as synced
                    val tripcopy = trip.copy(sync = true)
                    tripLocalRepository.updateTrip(tripcopy)
                    Log.d("UploadTrips", "Trip ${trip.id} marked as synced.")
                }
                is Resource.Error -> {
                    if (isNotFoundError(result.message)) {
                        Log.w(
                            "UploadTrips",
                            "Trip ${trip.id} not found on server. Attempting create instead."
                        )
                        when (val createResult = tripApiRepository.createTrip(tripUpdate)) {
                            is Success -> {
                                val tripcopy = trip.copy(sync = true)
                                tripLocalRepository.updateTrip(tripcopy)
                                Log.d(
                                    "UploadTrips",
                                    "Trip ${trip.id} created and marked as synced."
                                )
                            }
                            is Resource.Error -> {
                                Log.e(
                                    "UploadTrips",
                                    "Error creating trip ${trip.id}: ${createResult.message}"
                                )
                                return false
                            }
                            Resource.Loading -> {
                                Log.d("UploadTrips", "Trip ${trip.id} create is loading.")
                                return false
                            }
                        }
                    } else {
                        Log.e(
                            "UploadTrips",
                            "Error uploading trip ${trip.id}: ${result.message}"
                        )
                        return false // Stop and retry
                    }
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
     * Upload Raw Sensor + Unsafe Behaviour data via driver sync endpoint.
     */
    private suspend fun uploadDriverSyncData(): Boolean {
        val profileReference = resolveDriverProfileReference()
        if (profileReference == null) {
            Log.e("UploadDriverSync", "No driver profile available for sync.")
            return false
        }

        val driverProfileId = profileReference.driverProfileId
        if (driverProfileId == null) {
            Log.e("UploadDriverSync", "Driver profile ID missing for sync.")
            return false
        }

        val rawData = localRawDataRepository.getSensorDataBySyncStatus(false)
        val filteredRaw = rawData.filter { it.tripId != null }
        val dropped = rawData.size - filteredRaw.size
        if (dropped > 0) {
            Log.w("UploadDriverSync", "Skipping $dropped raw sensor records due to missing tripId.")
        }

        val rawSynced = uploadInBatches(
            notificationTitle = "Data Upload: Sensor Data",
            fetchData = { filteredRaw },
            mapToCreate = { rs ->
                val formattedDate = rs.date?.let { sdf.format(it) }
                val tripId = rs.tripId ?: throw IllegalStateException("Missing tripId for RawSensorData ${rs.id}")

                RawSensorDataCreate(
                    id = rs.id,
                    sensor_type = rs.sensorType,
                    sensor_type_name = rs.sensorTypeName,
                    values = rs.values,
                    timestamp = rs.timestamp,
                    date = formattedDate,
                    accuracy = rs.accuracy,
                    location_id = rs.locationId,
                    trip_id = tripId,
                    driverProfileId = driverProfileId,
                    sync = true
                )
            },
            batchUploadAction = { rawSensorData ->
                when (val result = driverSyncApiRepository.syncDriverData(
                    DriverSyncPayload(
                        profile = profileReference,
                        rawSensorData = rawSensorData
                    )
                )) {
                    is Success -> Success(Unit)
                    is Resource.Error -> Resource.Error(result.message ?: "Driver sync failed.")
                    Resource.Loading -> Resource.Loading
                }
            },
            markAsSynced = { batch ->
                batch.forEach { rsCreate ->
                    val originalData = localRawDataRepository.getRawSensorDataById(rsCreate.id)
                    if (originalData != null) {
                        val updatedData = originalData.copy(sync = true)
                        localRawDataRepository.updateRawSensorData(updatedData)
                        Log.d("UploadDriverSync", "RawSensorData ${updatedData.id} marked as synced.")
                    } else {
                        Log.w("UploadDriverSync", "RawSensorData with ID ${rsCreate.id} not found for syncing.")
                    }
                }
            }
        )

        if (!rawSynced) return false

        val unsafeSynced = uploadInBatches(
            notificationTitle = "Data Upload: Unsafe Behaviours",
            fetchData = { unsafeBehavioursLocalRepository.getUnsafeBehavioursBySyncStatus(false) },
            mapToCreate = { ub ->
                val formattedDate = ub.date?.let { sdf.format(it) }
                UnsafeBehaviourCreate(
                    id = ub.id,
                    trip_id = ub.tripId,
                    location_id = ub.locationId,
                    driverProfileId = driverProfileId,
                    behaviour_type = ub.behaviorType,
                    severity = ub.severity.toDouble(),
                    timestamp = ub.timestamp,
                    date = formattedDate ?: "",
                    sync = true
                )
            },
            batchUploadAction = { unsafeBehaviours ->
                when (val result = driverSyncApiRepository.syncDriverData(
                    DriverSyncPayload(
                        profile = profileReference,
                        unsafeBehaviours = unsafeBehaviours
                    )
                )) {
                    is Success -> Success(Unit)
                    is Resource.Error -> Resource.Error(result.message ?: "Driver sync failed.")
                    Resource.Loading -> Resource.Loading
                }
            },
            markAsSynced = { batch ->
                batch.forEach { ubCreate ->
                    val originalBehaviour =
                        unsafeBehavioursLocalRepository.getUnsafeBehaviourById(ubCreate.id)
                    if (originalBehaviour != null) {
                        val updatedBehaviour = originalBehaviour.copy(sync = true)
                        unsafeBehavioursLocalRepository.updateUnsafeBehaviour(updatedBehaviour.toDomainModel())
                        Log.d("UploadDriverSync", "UnsafeBehaviour ${updatedBehaviour.id} marked as synced.")
                    } else {
                        Log.w("UploadDriverSync", "UnsafeBehaviour with ID ${ubCreate.id} not found for syncing.")
                    }
                }
            }
        )

        return unsafeSynced
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
        return uploadInBatches(
            notificationTitle = "Data Upload: Driving Tips",
            fetchData = { drivingTipLocalRepository.getDrivingTipsBySyncStatus(false) },
            mapToCreate = { tipEntity ->
                tipEntity.toDomainModel().toDrivingTipCreate()
            },
            batchUploadAction = { drivingTips ->
                drivingTipApiRepository.batchCreateDrivingTips(drivingTips)
            },
            markAsSynced = { batch ->
                batch.forEach { tipEntity ->
                    val updated = tipEntity.copy(sync = true)
                    drivingTipLocalRepository.updateDrivingTip(updated)
                    Log.d("UploadDrivingTips", "DrivingTip ${updated.tipId} marked as synced.")
                }
            }
        )
    }

    /**
     * Upload NLG Reports
     */
    private suspend fun uploadNLGReports(): Boolean {
        return uploadInBatches(
            notificationTitle = "Data Upload: NLG Reports",
            fetchData = { nlgReportLocalRepository.getNlgReportBySyncStatus(false) },
            mapToCreate = { reportEntity ->
                reportEntity.toDomainModel().toNLGReportCreate()
            },
            batchUploadAction = { reports ->
                nlgReportApiRepository.batchCreateNLGReports(reports)
            },
            markAsSynced = { batch ->
                batch.forEach { reportEntity ->
                    val updated = reportEntity.copy(sync = true)
                    nlgReportLocalRepository.updateReport(updated)
                    Log.d("UploadNLGReports", "NLGReport ${updated.id} marked as synced.")
                }
            }
        )
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
                    location_id = ub.locationId,
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
     * Upload Trip Summary Behaviours (normalized)
     */
    private suspend fun uploadTripSummaryBehaviours(): Boolean {
        val summaries = tripSummaryRepository.getUnsyncedTripSummaries()
        if (summaries.isEmpty()) {
            Log.d("UploadTripSummaryBehaviours", "No trip summaries pending behaviour upload.")
            return true
        }

        val behaviours = summaries.flatMap { summary ->
            summary.toTripSummaryBehaviourCreates()
        }.filter { it.count > 0 }

        if (behaviours.isEmpty()) {
            Log.d("UploadTripSummaryBehaviours", "No behaviour rows to upload.")
            return true
        }

        return when (val result =
            tripSummaryBehaviourApiRepository.batchCreateTripSummaryBehaviours(behaviours)) {
            is Success -> {
                Log.d(
                    "UploadTripSummaryBehaviours",
                    "Uploaded ${behaviours.size} trip summary behaviour rows."
                )
                true
            }
            is Resource.Error -> {
                Log.e(
                    "UploadTripSummaryBehaviours",
                    "Failed to upload behaviours: ${result.message}"
                )
                false
            }
            Resource.Loading -> false
        }
    }

    /**
     * Upload Trip Summaries
     */
    private suspend fun uploadTripSummaries(): Boolean {
        return uploadInBatches(
            notificationTitle = "Data Upload: Trip Summaries",
            fetchData = { tripSummaryRepository.getUnsyncedTripSummaries() },
            mapToCreate = { summary ->
                summary.toTripSummaryCreate()
            },
            batchUploadAction = { summaries ->
                tripSummaryApiRepository.batchCreateTripSummaries(summaries)
            },
            markAsSynced = { batch ->
                val tripIds = batch.map { it.tripId }
                tripSummaryRepository.markTripSummariesSynced(tripIds, true)
                Log.d(
                    "UploadTripSummaries",
                    "Trip summaries marked as synced: ${tripIds.size}."
                )
            }
        )
    }

    /**
     * Upload Trip Feature State aggregates
     */
    private suspend fun uploadTripFeatureStates(): Boolean {
        return uploadInBatches(
            notificationTitle = "Data Upload: Trip Feature States",
            fetchData = { tripFeatureStateRepository.getUnsyncedTripFeatureStates() },
            mapToCreate = { state -> state.toTripFeatureStateCreate() },
            batchUploadAction = { states ->
                tripFeatureStateApiRepository.batchCreateTripFeatureStates(states)
            },
            markAsSynced = { batch ->
                val tripIds = batch.map { it.tripId }
                tripFeatureStateRepository.markTripFeatureStatesSynced(tripIds, true)
                Log.d(
                    "UploadTripFeatureStates",
                    "Trip feature states marked as synced: ${tripIds.size}."
                )
            }
        )
    }

    /**
     * Upload Raw Sensor Data
     */
    private suspend fun uploadRawSensorData(): Boolean {
        val driverProfileId = PreferenceUtils.getDriverProfileId(applicationContext) ?: return false
        val rawData = localRawDataRepository.getSensorDataBySyncStatus(false)
        if (rawData.isEmpty()) {
            Log.d("UploadRawSensorData", "No raw sensor data to upload.")
            return true
        }

        val tripIds = rawData.mapNotNull { it.tripId }.distinct()
        val missingTripIdCount = rawData.count { it.tripId == null }
        if (missingTripIdCount > 0) {
            Log.w(
                "UploadRawSensorData",
                "Skipping $missingTripIdCount raw sensor records due to missing tripId."
            )
        }
        if (tripIds.isEmpty()) {
            Log.w("UploadRawSensorData", "Sensor data missing trip identifiers.")
            return true
        }

        val tripsForRawData = tripLocalRepository.getTripByIds(tripIds)
        val tripById = tripsForRawData.associateBy { it.id }
        val missingTrips = tripIds.count { !tripById.containsKey(it) }
        if (missingTrips > 0) {
            Log.w(
                "UploadRawSensorData",
                "Missing $missingTrips local trips for raw sensor data; skipping those records."
            )
        }

        val confirmedTripIds = mutableSetOf<UUID>()
        for (trip in tripsForRawData) {
            val ensured = ensureTripExists(trip, driverProfileId)
            if (ensured) {
                confirmedTripIds.add(trip.id)
            } else {
                Log.e(
                    "UploadRawSensorData",
                    "Failed to ensure trip ${trip.id} exists before sensor upload."
                )
                return false
            }
        }
        if (confirmedTripIds.isEmpty()) {
            Log.w("UploadRawSensorData", "Sensor data pending trip upload.")
            return true
        }

        val syncedTripIds = confirmedTripIds
        val filtered = rawData.filter { it.tripId != null && syncedTripIds.contains(it.tripId) }
        val unsyncedTripCount = rawData.count { it.tripId != null && !syncedTripIds.contains(it.tripId) }
        if (unsyncedTripCount > 0) {
            Log.w(
                "UploadRawSensorData",
                "Skipping $unsyncedTripCount raw sensor records while trips are not synced yet."
            )
        }
        if (filtered.isEmpty()) {
            Log.w("UploadRawSensorData", "Sensor data pending trip upload.")
            return true
        }

        return uploadInBatches(
            notificationTitle = "Data Upload: Sensor Data",
            fetchData = { filtered },
            mapToCreate = { rs ->

                val formattedDate = rs.date?.let { sdf.format(it) }
                val tripId = rs.tripId ?: throw IllegalStateException("Missing tripId for RawSensorData ${rs.id}")

                RawSensorDataCreate(
                    id = rs.id,
                    sensor_type = rs.sensorType,
                    sensor_type_name = rs.sensorTypeName,
                    values = rs.values,
                    timestamp = rs.timestamp,
                    date = formattedDate,
                    accuracy = rs.accuracy,
                    location_id = rs.locationId,
                    trip_id = tripId,
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

        val roads = roadLocalRepository.getRoadsBySyncStatus(false)
//        if (roads.isEmpty()) return true

        if (roads.isEmpty()) {
//            vehicleNotificationManager.displayNotification(
//                "Data Upload: Roads",
//                "No roads to upload."
//            )
            return true
        }

        // ✅ DEDUP HERE (before chunking / mapping)
        val dedup = roads.distinctBy { keyForDedup(it) }

        val chunked = dedup.chunked(500)
        for ((i, chunk) in chunked.withIndex()) {
            Log.d(
                "UploadRoads",
                "Uploading batch ${i + 1}/${chunked.size} (${chunk.size} items)..."
            )

            val dtos = chunk.mapNotNull { r ->
                try {
                    RoadCreate(
                        id = r.id,
                        driverProfileId = r.driverProfileId,
                        name = r.name,
                        roadType = r.roadType,
                        speedLimit = r.speedLimit,
                        latitude = r.latitude,
                        longitude = r.longitude,
                        radius = r.radius,
                        sync = true
                    )
                } catch (e: Exception) {
                    Log.e("UploadRoads", "Map error for ${r.id}: ${e.message}")
                    null
                }
            }

            when (val res = roadApiRepository.batchCreateRoads(dtos)) {
                is Success -> {
                    // Expecting server to return the list it actually created
                    val created = res.data  // List<RoadResponse>
                    val createdIds = created.map { it.id }.toSet()

                    // Mark only those that were actually created
                    roadLocalRepository.markSyncByIds(createdIds.toList(), true)
                    Log.d("UploadRoads", "Batch ${i+1}: marked ${createdIds.size} roads as synced.")
                }
                is Resource.Error -> {
                    Log.e("UploadRoads", "Batch ${i+1} failed: ${res.message}")
                    return false
                }
                Resource.Loading -> { /* ignore */ }
            }
        }
        Log.d("UploadRoads", "All road batches uploaded.")
        return true
    }

//    private suspend fun uploadRoads(): Boolean {
//        return uploadInBatches(
//            notificationTitle = "Data Upload: Roads",
//            fetchData = { roadLocalRepository.getRoadsBySyncStatus(false) },
//            mapToCreate = { road ->
//                RoadCreate(
//                    id = road.id,
//                    driverProfileId = road.driverProfileId,
//                    name = road.name,
//                    roadType = road.roadType,
//                    speedLimit = road.speedLimit,
//                    latitude = road.latitude,
//                    longitude = road.longitude,
//                    radius = road.radius,
//                    sync = true
//                )
//            },
//            batchUploadAction = { roads ->
//                roadApiRepository.batchCreateRoads(roads)
//            },
//            markAsSynced = { batch ->
//                batch.forEach { roadCreate ->
//                    val originalRoad = roadLocalRepository.getRoadById(roadCreate.id)
//                    if (originalRoad!=null) {
//                        val updatedRoad = originalRoad.copy(sync = true)
//                        roadLocalRepository.updateRoad(updatedRoad.toEntity())
//                        Log.d("UploadRoads", "Road ${updatedRoad.id} marked as synced.")
//                    } else {
//                        Log.w(
//                            "UploadRoads",
//                            "Road with ID ${roadCreate.id} not found for syncing."
//                        )
//                    }
//                }
//            }
//        )
//    }

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
                    driverProfileId = input.driverProfileId,
                    timestamp = DateConversionUtils.longToTimestampString(input.timestamp),
                    start_time = input.startTimestamp,
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
                            "AIModelInput ${updatedInput} marked as synced."
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

    private suspend fun resolveDriverProfileReference(): DriverProfileReference? {
        val storedId = PreferenceUtils.getDriverProfileId(applicationContext)
        val storedProfile = storedId?.let { driverProfileLocalRepository.getDriverProfileById(it) }
        val fallbackProfile = driverProfileLocalRepository.getDriverProfileBySyncStatus(true).firstOrNull()
            ?: driverProfileLocalRepository.getDriverProfileBySyncStatus(false).firstOrNull()
            ?: driverProfileLocalRepository.getAllDriverProfiles().firstOrNull()

        val profile = storedProfile ?: fallbackProfile ?: return null
        return DriverProfileReference(
            driverProfileId = profile.driverProfileId,
            email = profile.email,
            displayName = null
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

    private fun isNotFoundError(message: String?): Boolean {
        return message?.contains("404", ignoreCase = true) == true ||
                message?.contains("not found", ignoreCase = true) == true
    }

    private fun isBadRequestError(message: String?): Boolean {
        return message?.contains("400", ignoreCase = true) == true
    }

    private fun isConflictError(message: String?): Boolean {
        return message?.contains("409", ignoreCase = true) == true
    }

    private suspend fun ensureTripExists(trip: Trip, fallbackDriverId: UUID?): Boolean {
        val currentTrip = tripLocalRepository.getTripById(trip.id) ?: trip
        val resolvedDriverId = currentTrip.driverPId ?: fallbackDriverId
        if (resolvedDriverId == null) {
            Log.w("UploadTrips", "Missing driverProfileId for trip ${trip.id}; cannot upload.")
            return false
        }

        val tripPayload = TripCreate(
            id = currentTrip.id,
            driverProfileId = resolvedDriverId,
            startDate = formatTripDate(currentTrip.startDate, currentTrip.startTime),
            endDate = formatTripDate(currentTrip.endDate, currentTrip.endTime),
            startTime = currentTrip.startTime,
            endTime = currentTrip.endTime,
            timeZoneId = timeZoneId(),
            timeZoneOffsetMinutes = timeZoneOffsetMinutes(currentTrip.startTime),
            sync = true,
            influence = currentTrip.influence ?: "",
            userAlcoholResponse = currentTrip.userAlcoholResponse,
            alcoholProbability = currentTrip.alcoholProbability
        )

        return when (val result = tripApiRepository.createTrip(tripPayload)) {
            is Success -> {
                tripLocalRepository.updateUploadStatus(currentTrip.id, true)
                true
            }
            is Resource.Error -> {
                if (isBadRequestError(result.message) || isConflictError(result.message)) {
                    when (val updateResult = tripApiRepository.updateTrip(currentTrip.id, tripPayload)) {
                        is Success -> {
                            tripLocalRepository.updateUploadStatus(currentTrip.id, true)
                            true
                        }
                        is Resource.Error -> {
                            Log.e(
                                "UploadTrips",
                                "Failed to update trip ${currentTrip.id} after create failure: ${updateResult.message}"
                            )
                            false
                        }
                        Resource.Loading -> false
                    }
                } else {
                    val exists = tripExistsOnServer(currentTrip.id)
                    if (exists) {
                        when (val updateResult = tripApiRepository.updateTrip(currentTrip.id, tripPayload)) {
                            is Success -> {
                                tripLocalRepository.updateUploadStatus(currentTrip.id, true)
                                true
                            }
                            is Resource.Error -> {
                                Log.e(
                                    "UploadTrips",
                                    "Failed to update existing trip ${currentTrip.id} after create error: ${updateResult.message}"
                                )
                                false
                            }
                            Resource.Loading -> false
                        }
                    } else {
                        Log.e("UploadTrips", "Error creating trip ${trip.id}: ${result.message}")
                        false
                    }
                }
            }
            Resource.Loading -> false
        }
    }

    private fun formatTripDate(date: Date?, timestamp: Long?): String? {
        return when {
            date != null -> sdf.format(date)
            timestamp != null -> sdf.format(Date(timestamp))
            else -> null
        }
    }

    private fun timeZoneId(): String = TimeZone.getDefault().id

    private fun timeZoneOffsetMinutes(timestamp: Long?): Int? =
        timestamp?.let { TimeZone.getDefault().getOffset(it) / 60000 }

    private suspend fun tripExistsOnServer(tripId: UUID): Boolean {
        return when (val result = tripApiRepository.getTrip(tripId.toString())) {
            is Success -> true
            is Resource.Error -> {
                if (!isNotFoundError(result.message)) {
                    Log.w(
                        "UploadTrips",
                        "Trip $tripId lookup failed after create error: ${result.message}"
                    )
                }
                false
            }
            Resource.Loading -> false
        }
    }

}

private fun Double.roundDp(dp: Int): Double {
    val f = 10.0.pow(dp)
    return round(this * f) / f
}

private fun UploadAllDataWorker.keyForDedup(r: RoadEntity)=listOf(
r.driverProfileId, r.name, r.roadType, r.speedLimit, r.radius,
r.latitude.roundDp(5), r.longitude.roundDp(5)
)
