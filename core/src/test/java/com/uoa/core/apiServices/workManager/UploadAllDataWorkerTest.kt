package com.uoa.core.apiServices.workManager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.uoa.core.apiServices.models.auth.AuthDriverProfile
import com.uoa.core.apiServices.models.auth.AuthResponse
import com.uoa.core.apiServices.services.aiModellInputApiService.AIModelInputApiRepository
import com.uoa.core.apiServices.services.alcoholQuestionnaireService.QuestionnaireApiRepository
import com.uoa.core.apiServices.services.auth.AuthRepository
import com.uoa.core.apiServices.models.driverSyncModels.DriverSyncResponse
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
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.DriverProfileRepository
import com.uoa.core.database.repository.DrivingTipRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.NLGReportRepository
import com.uoa.core.database.repository.QuestionnaireRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.ReportStatisticsRepository
import com.uoa.core.database.repository.RoadRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.database.repository.TripFeatureStateRepository
import com.uoa.core.database.repository.TripSummaryRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.database.entities.TripFeatureStateEntity
import com.uoa.core.model.DriverProfile
import com.uoa.core.model.TripSummary
import com.uoa.core.network.NetworkMonitor
import com.uoa.core.notifications.VehicleNotificationManager
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Constants.Companion.UPLOAD_AUTH_REMINDER_TIMESTAMP
import com.uoa.core.utils.Resource
import com.uoa.core.utils.SecureCredentialStorage
import com.uoa.core.utils.SecureTokenStorage
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class UploadAllDataWorkerTest {

    @Test
    fun retriesWhenOffline() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val networkMonitor = mock<NetworkMonitor>()
        whenever(networkMonitor.isOnline).thenReturn(flowOf(false))

        val vehicleNotificationManager = mock<VehicleNotificationManager>()
        val secureTokenStorage = mock<SecureTokenStorage>()
        whenever(secureTokenStorage.getToken()).thenReturn("token-value")
        val worker = buildUploadWorker(
            context = context,
            networkMonitor = networkMonitor,
            vehicleNotificationManager = vehicleNotificationManager,
            secureTokenStorage = secureTokenStorage
        )

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
        verify(vehicleNotificationManager).displayUploadFailure(any())
    }

    @Test
    fun uploadsDriverProfileAndStoresId() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        val networkMonitor = mock<NetworkMonitor>()
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        val vehicleNotificationManager = mock<VehicleNotificationManager>()
        val secureTokenStorage = mock<SecureTokenStorage>()
        whenever(secureTokenStorage.getToken()).thenReturn(null)

        val driverProfileRepository = mock<DriverProfileRepository>()
        val tripRepository = mock<TripDataRepository>()
        val rawSensorDataRepository = mock<RawSensorDataRepository>()
        val unsafeBehaviourRepository = mock<UnsafeBehaviourRepository>()
        val questionnaireRepository = mock<QuestionnaireRepository>()
        val drivingTipRepository = mock<DrivingTipRepository>()
        val nlgReportRepository = mock<NLGReportRepository>()
        val locationRepository = mock<LocationRepository>()
        val aiModelInputRepository = mock<AIModelInputRepository>()
        val reportStatisticsRepository = mock<ReportStatisticsRepository>()
        val roadRepository = mock<RoadRepository>()
        val tripSummaryRepository = mock<TripSummaryRepository>()
        val tripFeatureStateRepository = mock<TripFeatureStateRepository>()
        val driverSyncApiRepository = mock<DriverSyncApiRepository>()
        val secureCredentialStorage = mock<SecureCredentialStorage>()
        val authRepository = mock<AuthRepository>()

        whenever(tripRepository.getNewTrips()).thenReturn(emptyList())
        whenever(tripRepository.getUpdatedTrips()).thenReturn(emptyList())
        whenever(rawSensorDataRepository.getSensorDataBySyncStatus(false)).thenReturn(emptyList())
        whenever(unsafeBehaviourRepository.getUnsafeBehavioursBySyncStatus(false)).thenReturn(emptyList())
        whenever(questionnaireRepository.getAllUnsyncedQuestionnaires()).thenReturn(emptyList())
        whenever(drivingTipRepository.getDrivingTipsBySyncStatus(false)).thenReturn(emptyList())
        whenever(nlgReportRepository.getNlgReportBySyncStatus(false)).thenReturn(emptyList())
        whenever(locationRepository.getLocationBySynced(false)).thenReturn(flowOf(emptyList()))
        whenever(aiModelInputRepository.getAiModelInputsBySyncStatus(false)).thenReturn(emptyList())
        whenever(reportStatisticsRepository.getReportStatisticsBySyncStatus(false)).thenReturn(emptyList())
        whenever(roadRepository.getRoadsBySyncStatus(false)).thenReturn(emptyList())
        whenever(tripSummaryRepository.getUnsyncedTripSummaries()).thenReturn(emptyList())
        whenever(tripFeatureStateRepository.getUnsyncedTripFeatureStates()).thenReturn(emptyList())

        val profileId = UUID.randomUUID()
        val profile = DriverProfile(driverProfileId = profileId, email = "user@example.com")

        whenever(driverProfileRepository.getDriverProfileBySyncStatus(false))
            .thenReturn(listOf(profile))
        whenever(driverProfileRepository.getDriverProfileBySyncStatus(true))
            .thenReturn(listOf(profile))
        whenever(driverProfileRepository.getDriverProfileById(profileId))
            .thenReturn(profile)
        whenever(driverProfileRepository.getAllDriverProfiles())
            .thenReturn(listOf(profile))

        whenever(secureCredentialStorage.getEmail()).thenReturn(profile.email)
        whenever(secureCredentialStorage.getPassword()).thenReturn("safe-password")

        val tokenResponse = AuthResponse(
            token = "token-value",
            tokenType = "bearer",
            driverProfileId = profileId.toString(),
            driverProfile = AuthDriverProfile(id = profileId, email = profile.email, name = null)
        )
        whenever(authRepository.registerDriver(any()))
            .thenReturn(Resource.Success(tokenResponse))

        whenever(driverSyncApiRepository.syncDriverData(any()))
            .thenReturn(
                Resource.Success(
                    DriverSyncResponse(
                        tripCount = 0,
                        rawSensorCount = 0,
                        unsafeBehaviourCount = 0,
                        alcoholResponseCount = 0,
                        driverProfileId = profileId
                    )
                )
            )

        val worker = buildUploadWorker(
            context = context,
            networkMonitor = networkMonitor,
            vehicleNotificationManager = vehicleNotificationManager,
            secureTokenStorage = secureTokenStorage,
            rawSensorDataRepository = rawSensorDataRepository,
            unsafeBehaviourRepository = unsafeBehaviourRepository,
            questionnaireRepository = questionnaireRepository,
            drivingTipRepository = drivingTipRepository,
            nlgReportRepository = nlgReportRepository,
            locationRepository = locationRepository,
            aiModelInputRepository = aiModelInputRepository,
            reportStatisticsRepository = reportStatisticsRepository,
            roadRepository = roadRepository,
            tripSummaryRepository = tripSummaryRepository,
            tripFeatureStateRepository = tripFeatureStateRepository,
            driverProfileRepository = driverProfileRepository,
            driverSyncApiRepository = driverSyncApiRepository,
            secureCredentialStorage = secureCredentialStorage,
            authRepository = authRepository,
            tripRepository = tripRepository
        )

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        verify(authRepository).registerDriver(any())
        verify(driverProfileRepository).updateDriverProfileByEmail(profileId, true, profile.email)
        verify(secureTokenStorage).saveToken(tokenResponse.token)

        val storedId = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(DRIVER_PROFILE_ID, null)
        assertTrue(storedId == profileId.toString())
    }

    @Test
    fun uploadsTripSummaryBehavioursAndFeatureStates() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val networkMonitor = mock<NetworkMonitor>()
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))

        val vehicleNotificationManager = mock<VehicleNotificationManager>()
        val secureTokenStorage = mock<SecureTokenStorage>()
        whenever(secureTokenStorage.getToken()).thenReturn("token-value")

        val tripRepository = mock<TripDataRepository>()
        whenever(tripRepository.getNewTrips()).thenReturn(emptyList())
        whenever(tripRepository.getUpdatedTrips()).thenReturn(emptyList())

        val rawSensorDataRepository = mock<RawSensorDataRepository>()
        whenever(rawSensorDataRepository.getSensorDataBySyncStatus(false)).thenReturn(emptyList())

        val unsafeBehaviourRepository = mock<UnsafeBehaviourRepository>()
        whenever(unsafeBehaviourRepository.getUnsafeBehavioursBySyncStatus(false)).thenReturn(emptyList())

        val questionnaireRepository = mock<QuestionnaireRepository>()
        whenever(questionnaireRepository.getAllUnsyncedQuestionnaires()).thenReturn(emptyList())

        val drivingTipRepository = mock<DrivingTipRepository>()
        whenever(drivingTipRepository.getDrivingTipsBySyncStatus(false)).thenReturn(emptyList())

        val nlgReportRepository = mock<NLGReportRepository>()
        whenever(nlgReportRepository.getNlgReportBySyncStatus(false)).thenReturn(emptyList())

        val locationRepository = mock<LocationRepository>()
        whenever(locationRepository.getLocationBySynced(false)).thenReturn(flowOf(emptyList()))

        val aiModelInputRepository = mock<AIModelInputRepository>()
        whenever(aiModelInputRepository.getAiModelInputsBySyncStatus(false)).thenReturn(emptyList())

        val reportStatisticsRepository = mock<ReportStatisticsRepository>()
        whenever(reportStatisticsRepository.getReportStatisticsBySyncStatus(false)).thenReturn(emptyList())

        val roadRepository = mock<RoadRepository>()
        whenever(roadRepository.getRoadsBySyncStatus(false)).thenReturn(emptyList())

        val tripId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(DRIVER_PROFILE_ID, driverId.toString())
            .commit()
        val tripSummary = TripSummary(
            tripId = tripId,
            driverId = driverId,
            startTime = 1_700_000_000_000L,
            endTime = 1_700_000_100_000L,
            startDate = Date(1_700_000_000_000L),
            endDate = Date(1_700_000_100_000L),
            distanceMeters = 1234.5,
            durationSeconds = 1000L,
            unsafeBehaviourCounts = mapOf("hard_brake" to 2),
            classificationLabel = "safe",
            alcoholProbability = 0.1f,
            sync = false
        )

        val tripSummaryRepository = mock<TripSummaryRepository>()
        whenever(tripSummaryRepository.getUnsyncedTripSummaries()).thenReturn(listOf(tripSummary))

        val tripSummaryApiRepository = mock<TripSummaryApiRepository>()
        whenever(tripSummaryApiRepository.batchCreateTripSummaries(any()))
            .thenReturn(Resource.Success(Unit))

        val tripSummaryBehaviourApiRepository = mock<TripSummaryBehaviourApiRepository>()
        whenever(tripSummaryBehaviourApiRepository.batchCreateTripSummaryBehaviours(any()))
            .thenReturn(Resource.Success(Unit))

        val tripFeatureStateRepository = mock<TripFeatureStateRepository>()
        whenever(tripFeatureStateRepository.getUnsyncedTripFeatureStates()).thenReturn(
            listOf(
                TripFeatureStateEntity(
                    tripId = tripId,
                    driverProfileId = driverId
                )
            )
        )

        val tripFeatureStateApiRepository = mock<TripFeatureStateApiRepository>()
        whenever(tripFeatureStateApiRepository.batchCreateTripFeatureStates(any()))
            .thenReturn(Resource.Success(Unit))

        val worker = buildUploadWorker(
            context = context,
            networkMonitor = networkMonitor,
            vehicleNotificationManager = vehicleNotificationManager,
            secureTokenStorage = secureTokenStorage,
            rawSensorDataRepository = rawSensorDataRepository,
            unsafeBehaviourRepository = unsafeBehaviourRepository,
            questionnaireRepository = questionnaireRepository,
            drivingTipRepository = drivingTipRepository,
            nlgReportRepository = nlgReportRepository,
            locationRepository = locationRepository,
            aiModelInputRepository = aiModelInputRepository,
            reportStatisticsRepository = reportStatisticsRepository,
            roadRepository = roadRepository,
            tripRepository = tripRepository,
            tripSummaryRepository = tripSummaryRepository,
            tripSummaryApiRepository = tripSummaryApiRepository,
            tripSummaryBehaviourApiRepository = tripSummaryBehaviourApiRepository,
            tripFeatureStateRepository = tripFeatureStateRepository,
            tripFeatureStateApiRepository = tripFeatureStateApiRepository
        )

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        verify(tripSummaryBehaviourApiRepository).batchCreateTripSummaryBehaviours(any())
        verify(tripSummaryApiRepository).batchCreateTripSummaries(any())
        verify(tripSummaryRepository).markTripSummariesSynced(any(), eq(true))
        verify(tripFeatureStateApiRepository).batchCreateTripFeatureStates(any())
        verify(tripFeatureStateRepository).markTripFeatureStatesSynced(any(), eq(true))
    }

    @Test
    fun retriesWhenTokenMissing() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val networkMonitor = mock<NetworkMonitor>()
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))

        val vehicleNotificationManager = mock<VehicleNotificationManager>()
        val secureTokenStorage = mock<SecureTokenStorage>()
        whenever(secureTokenStorage.getToken()).thenReturn(null)

        val worker = buildUploadWorker(
            context = context,
            networkMonitor = networkMonitor,
            vehicleNotificationManager = vehicleNotificationManager,
            secureTokenStorage = secureTokenStorage
        )

        val result = worker.doWork()
        assertTrue(result is ListenableWorker.Result.Retry)
        verify(vehicleNotificationManager).displayPermissionNotification(any())

        val reminderTimestamp = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(UPLOAD_AUTH_REMINDER_TIMESTAMP, 0L)
        assertTrue(reminderTimestamp > 0)
    }

    private fun buildUploadWorker(
        context: Context,
        networkMonitor: NetworkMonitor,
        vehicleNotificationManager: VehicleNotificationManager,
        secureTokenStorage: SecureTokenStorage = mock(),
        rawSensorDataApiRepository: RawSensorDataApiRepository = mock(),
        locationRepository: LocationRepository = mock(),
        locationApiRepository: LocationApiRepository = mock(),
        rawSensorDataRepository: RawSensorDataRepository = mock(),
        unsafeBehaviourRepository: UnsafeBehaviourRepository = mock(),
        unsafeBehaviourApiRepository: UnsafeBehaviourApiRepository = mock(),
        aiModelInputRepository: AIModelInputRepository = mock(),
        aiModelInputApiRepository: AIModelInputApiRepository = mock(),
        reportStatisticsRepository: ReportStatisticsRepository = mock(),
        reportStatisticsApiRepository: ReportStatisticsApiRepository = mock(),
        driverProfileRepository: DriverProfileRepository = mock(),
        driverSyncApiRepository: DriverSyncApiRepository = mock(),
        driverFleetApiRepository: DriverFleetApiRepository = mock(),
        tripRepository: TripDataRepository = mock(),
        tripApiRepository: TripApiRepository = mock(),
        tripSummaryRepository: TripSummaryRepository = mock(),
        tripSummaryApiRepository: TripSummaryApiRepository = mock(),
        tripSummaryBehaviourApiRepository: TripSummaryBehaviourApiRepository = mock(),
        tripFeatureStateRepository: TripFeatureStateRepository = mock(),
        tripFeatureStateApiRepository: TripFeatureStateApiRepository = mock(),
        roadRepository: RoadRepository = mock(),
        roadApiRepository: RoadApiRepository = mock(),
        questionnaireRepository: QuestionnaireRepository = mock(),
        questionnaireApiRepository: QuestionnaireApiRepository = mock(),
        drivingTipRepository: DrivingTipRepository = mock(),
        drivingTipApiRepository: DrivingTipApiRepository = mock(),
        nlgReportRepository: NLGReportRepository = mock(),
        nlgReportApiRepository: NLGReportApiRepository = mock(),
        secureCredentialStorage: SecureCredentialStorage = mock(),
        authRepository: AuthRepository = mock()
    ): UploadAllDataWorker {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker? {
            return UploadAllDataWorker(
                appContext,
                workerParameters,
                rawSensorDataApiRepository,
                    locationRepository,
                    locationApiRepository,
                    rawSensorDataRepository,
                    unsafeBehaviourRepository,
                    unsafeBehaviourApiRepository,
                    aiModelInputRepository,
                    aiModelInputApiRepository,
                    reportStatisticsRepository,
                    reportStatisticsApiRepository,
                    driverProfileRepository,
                    driverSyncApiRepository,
                    driverFleetApiRepository,
                    tripRepository,
                    tripApiRepository,
                    tripSummaryRepository,
                    tripSummaryApiRepository,
                    tripSummaryBehaviourApiRepository,
                    tripFeatureStateRepository,
                    tripFeatureStateApiRepository,
                    roadRepository,
                    roadApiRepository,
                    questionnaireRepository,
                    questionnaireApiRepository,
                    drivingTipRepository,
                    drivingTipApiRepository,
                    nlgReportRepository,
                    nlgReportApiRepository,
                    vehicleNotificationManager,
                    secureTokenStorage,
                    secureCredentialStorage,
                    authRepository,
                    networkMonitor
                )
        }
        }

        return TestListenableWorkerBuilder<UploadAllDataWorker>(context)
            .setWorkerFactory(factory)
            .build()
    }
}
