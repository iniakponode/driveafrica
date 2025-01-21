//package com.uoa.core.apiServices.workManager
//
//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.NetworkCapabilities
//import androidx.work.CoroutineWorker
//import androidx.work.WorkerParameters
//import com.uoa.core.apiServices.services.aiModellInputApiService.AIModelInputApiRepository
//import com.uoa.core.apiServices.services.drivingTipApiService.DrivingTipApiRepository
//import com.uoa.core.apiServices.services.locationApiService.LocationApiRepository
//import com.uoa.core.apiServices.services.nlgReportApiService.NLGReportApiRepository
//import com.uoa.core.apiServices.services.rawSensorApiService.RawSensorDataApiRepository
//import com.uoa.core.apiServices.services.tripApiService.TripApiRepository
//import com.uoa.core.apiServices.services.unsafeBehaviourApiService.UnsafeBehaviourApiRepository
//import com.uoa.core.database.repository.AIModelInputRepository
//import com.uoa.core.database.repository.DrivingTipRepository
//import com.uoa.core.database.repository.LocationRepository
//import com.uoa.core.database.repository.NLGReportRepository
//import com.uoa.core.database.repository.RawSensorDataRepository
//import com.uoa.core.database.repository.TripDataRepository
//import com.uoa.core.database.repository.UnsafeBehaviourRepository
//import com.uoa.core.utils.Resource
//
//class UploadAllDataWorker(
//    appContext: Context,
//    workerParams: WorkerParameters,
//    private val tripApiRepository: TripApiRepository,
//    private val tripLocalRepository: TripDataRepository,
//    private val rawSensorDataApiRepository: RawSensorDataApiRepository,
//    private val rawSensorDataLocalRepository: RawSensorDataRepository,
//    private val unsafeBehaviourApiRepository: UnsafeBehaviourApiRepository,
//    private val unsafeBehaviourLocalRepository: UnsafeBehaviourRepository,
//    private val locationApiRepository: LocationApiRepository,
//    private val locationLocalRepository: LocationRepository,
//    private val drivingTipApiRepository: DrivingTipApiRepository,
//    private val drivingTipLocalRepository: DrivingTipRepository,
//    private val aiModelInputApiRepository: AIModelInputApiRepository,
//    private val aiModelInputLocalRepository: AIModelInputRepository,
//    private val nlgReportApiRepository: NLGReportApiRepository,
//    private val nlgReportLocalRepository: NLGReportRepository,
//) : CoroutineWorker(appContext, workerParams) {
//
//    override suspend fun doWork(): Result {
//        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        val network = connectivityManager.activeNetwork
//        val capabilities = connectivityManager.getNetworkCapabilities(network)
//        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
//
//        if (!isConnected) {
//            return Result.retry()
//        }
//
//        // Step 1: Upload Trips first (Main entity)
//        val unsyncedTrips = tripLocalRepository.getTripsBySyncStatus(false)
//        if (unsyncedTrips.isNotEmpty()) {
//            val tripCreates = unsyncedTrips.map { it.toTripCreate() }
//            val tripResult = tripApiRepository.batchCreateTrips(tripCreates)
//            if (tripResult is Resource.Error) {
//                return Result.retry()
//            } else {
//                // Mark trips as synced
//                tripLocalRepository.updateTrip(unsyncedTrips.map { it.id })
//            }
//        }
///
//        // Step 2: Upload Location Data (Depends on Trip)
//        val unsyncedLocations = locationLocalRepository.getLocationBySynced(false)
//        if (unsyncedLocations.isNotEmpty()) {
//            val locationCreates = unsyncedLocations.map { it.toLocationCreate() }
//            val locationResult = locationApiRepository.batchCreateLocations(locationCreates)
//            if (locationResult is Resource.Error) {
//                return Result.retry()
//            } else {
//                // Mark locations as synced
//                locationLocalRepository.markLocationsAsSynced(unsyncedLocations.map { it.id })
//            }
//        }
//
//        // Step 3: Upload Raw Sensor Data (Depends on Trip/Location)
//        val localRawSensorData = rawSensorDataLocalRepository.getUnsyncedSensorData()
//        if (localRawSensorData.isNotEmpty()) {
//            val rawSensorDataList = localRawSensorData.map { it.toRawSensorDataCreate() }
//            val rawResult = rawSensorDataApiRepository.batchCreateRawSensorData(rawSensorDataList)
//            if (rawResult is Resource.Error) {
//                return Result.retry()
//            } else {
//                // Mark RawSensorData as synced or delete them
//                rawSensorDataLocalRepository.markRawSensorDataAsSynced(localRawSensorData.map { it.id })
//            }
//        }
//
//        // Step 4: Upload Unsafe Behaviours (Depends on Trip, possibly Locations)
//        val unsyncedUnsafeBehaviours = unsafeBehaviourLocalRepository.getUnsyncedUnsafeBehaviours()
//        if (unsyncedUnsafeBehaviours.isNotEmpty()) {
//            val unsafeCreates = unsyncedUnsafeBehaviours.map { it.toUnsafeBehaviourCreate() }
//            val unsafeResult = unsafeBehaviourApiRepository.batchCreateUnsafeBehaviours(unsafeCreates)
//            if (unsafeResult is Resource.Error) {
//                return Result.retry()
//            } else {
//                // Mark unsafe behaviours as synced
//                unsafeBehaviourLocalRepository.markUnsafeBehavioursAsSynced(unsyncedUnsafeBehaviours.map { it.id })
//            }
//        }
//
//        // Step 5: Upload Driving Tips (Depends on Driver Profile which depends on Trip)
//        val unsyncedDrivingTips = drivingTipLocalRepository.getUnsyncedDrivingTips()
//        if (unsyncedDrivingTips.isNotEmpty()) {
//            val tipCreates = unsyncedDrivingTips.map { it.toDrivingTipCreate() }
//            val tipResult = drivingTipApiRepository.batchCreateDrivingTips(tipCreates)
//            if (tipResult is Resource.Error) {
//                return Result.retry()
//            } else {
//                // Mark tips as synced
//                drivingTipLocalRepository.markDrivingTipsAsSynced(unsyncedDrivingTips.map { it.tipId })
//            }
//        }
//
//        // Step 6: Upload AI Model Inputs (Depends on Trip)
//        val unsyncedAIInputs = aiModelInputLocalRepository.getUnsyncedAIModelInputs()
//        if (unsyncedAIInputs.isNotEmpty()) {
//            val aiInputs = unsyncedAIInputs.map { it.toAIModelInputCreate() }
//            val aiResult = aiModelInputApiRepository.batchCreateAIModelInputs(aiInputs)
//            if (aiResult is Resource.Error) {
//                return Result.retry()
//            } else {
//                // Mark AI inputs as synced
//                aiModelInputLocalRepository.markAIModelInputsAsSynced(unsyncedAIInputs.map { it.id })
//            }
//        }
//
//        // Step 7: Upload NLG Reports (Depends on user which depends on Trip/Driver)
//        val unsyncedNLGReports = nlgReportLocalRepository.getUnsyncedNLGReports()
//        if (unsyncedNLGReports.isNotEmpty()) {
//            val nlgCreates = unsyncedNLGReports.map { it.toNLGReportCreate() }
//            val nlgResult = nlgReportApiRepository.batchCreateNLGReports(nlgCreates)
//            if (nlgResult is Resource.Error) {
//                return Result.retry()
//            } else {
//                // Mark NLG reports as synced
//                nlgReportLocalRepository.markNLGReportsAsSynced(unsyncedNLGReports.map { it.id })
//            }
//        }
//
//        // If all uploads succeeded
//        return Result.success()
//    }
//}
