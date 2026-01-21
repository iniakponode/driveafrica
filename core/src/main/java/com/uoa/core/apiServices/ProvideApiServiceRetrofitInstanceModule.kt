package com.uoa.core.apiServices

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.uoa.core.BuildConfig
import com.uoa.core.apiServices.services.aiModellInputApiService.AIModelInputApiRepository
import com.uoa.core.apiServices.services.aiModellInputApiService.AIModelInputApiService
import com.uoa.core.apiServices.services.alcoholQuestionnaireService.QuestionnaireApiRepository
import com.uoa.core.apiServices.services.alcoholQuestionnaireService.QuestionnaireApiService
import com.uoa.core.apiServices.services.auth.AuthApiService
import com.uoa.core.apiServices.services.auth.AuthRepository
import com.uoa.core.apiServices.services.driverProfileApiService.DriverProfileApiRepository
import com.uoa.core.apiServices.services.driverProfileApiService.DriverProfileApiService
import com.uoa.core.apiServices.services.driverSyncApiService.DriverSyncApiRepository
import com.uoa.core.apiServices.services.driverSyncApiService.DriverSyncApiService
import com.uoa.core.apiServices.services.drivingTipApiService.DrivingTipApiRepository
import com.uoa.core.apiServices.services.drivingTipApiService.DrivingTipApiService
import com.uoa.core.apiServices.services.embeddingApiService.EmbeddingApiRepository
import com.uoa.core.apiServices.services.embeddingApiService.EmbeddingApiService
import com.uoa.core.apiServices.services.locationApiService.LocationApiRepository
import com.uoa.core.apiServices.services.locationApiService.LocationApiService
import com.uoa.core.apiServices.services.nlgReportApiService.NLGReportApiService
import com.uoa.core.apiServices.services.nlgReportApiService.NLGReportApiRepository
import com.uoa.core.apiServices.services.rawSensorApiService.RawSensorDataApiRepository
import com.uoa.core.apiServices.services.rawSensorApiService.RawSensorDataApiService
import com.uoa.core.apiServices.services.reportStatisticsApiService.ReportStatisticsApiRepository
import com.uoa.core.apiServices.services.reportStatisticsApiService.ReportStatisticsApiService
import com.uoa.core.apiServices.services.roadApiService.RoadApiRepository
import com.uoa.core.apiServices.services.roadApiService.RoadApiService
import com.uoa.core.apiServices.services.tripApiService.TripApiRepository
import com.uoa.core.apiServices.services.tripApiService.TripApiService
import com.uoa.core.apiServices.services.tripFeatureStateApiService.TripFeatureStateApiRepository
import com.uoa.core.apiServices.services.tripFeatureStateApiService.TripFeatureStateApiService
import com.uoa.core.apiServices.services.tripSummaryApiService.TripSummaryApiRepository
import com.uoa.core.apiServices.services.tripSummaryApiService.TripSummaryApiService
import com.uoa.core.apiServices.services.tripSummaryBehaviourApiService.TripSummaryBehaviourApiRepository
import com.uoa.core.apiServices.services.tripSummaryBehaviourApiService.TripSummaryBehaviourApiService
import com.uoa.core.apiServices.services.unsafeBehaviourApiService.UnsafeBehaviourApiRepository
import com.uoa.core.apiServices.services.unsafeBehaviourApiService.UnsafeBehaviourApiService
import com.uoa.core.apiServices.services.fleetApiService.DriverFleetApiRepository
import com.uoa.core.apiServices.services.fleetApiService.DriverFleetApiService
import com.uoa.core.apiServices.interceptor.AuthInterceptor
import com.uoa.core.apiServices.interceptor.ErrorBodyLoggingInterceptor
import com.uoa.core.apiServices.interceptor.TokenExpiredInterceptor
import com.uoa.core.database.repository.DriverProfileRepository
import com.uoa.core.utils.SecureTokenStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProvideApiServiceRetrofitInstanceModule {

    // The base URL to your API
    private const val DRIVE_AFRICA_BASE_URL = BuildConfig.DRIVE_AFRICA_BASE_URL
//        const val DRIVE_AFRICA_BASE_URL= BuildConfig.EMULATOR_BASE_URL

    // 1) Provide the custom UUIDTypeAdapter
    @Provides
    fun provideUUIDTypeAdapter(): UUIDTypeAdapter = UUIDTypeAdapter()

    @Provides
    fun provideLocalDateTypeAdapter(): LocalDateTypeAdapter = LocalDateTypeAdapter()

    @Provides
    fun provideLocalDateTimeTypeAdapter(): LocalDateTimeTypeAdapter = LocalDateTimeTypeAdapter()

    // 2) Provide Gson with your UUID adapter
    @Provides
    @Singleton
    fun provideGson(
        uuidTypeAdapter: UUIDTypeAdapter,
        localDateTypeAdapter: LocalDateTypeAdapter,
        localDateTimeTypeAdapter: LocalDateTimeTypeAdapter
    ): Gson {
        return GsonBuilder()
            .registerTypeAdapter(UUID::class.java, uuidTypeAdapter)
            .registerTypeAdapter(LocalDate::class.java, localDateTypeAdapter)
            .registerTypeAdapter(LocalDateTime::class.java, localDateTimeTypeAdapter)
            .create()
    }

    // 3) Provide OkHttpClient (logging, token interceptors, etc.)
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenExpiredInterceptor: TokenExpiredInterceptor
    ): OkHttpClient {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(tokenExpiredInterceptor)
            .addInterceptor(ErrorBodyLoggingInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })

        return clientBuilder.build()
    }

    // 4) Provide a single Retrofit instance
    @Provides
    @Singleton
    fun provideRetrofit(
        gson: Gson,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DRIVE_AFRICA_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }

    // 5) Provide each API service from the single Retrofit
    @Provides
    @Singleton
    fun provideAIModelInputApiService(retrofit: Retrofit): AIModelInputApiService {
        return retrofit.create(AIModelInputApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAiModelInputApiRepository(
        aiModelInputApiService: AIModelInputApiService
    ): AIModelInputApiRepository {
        return AIModelInputApiRepository(aiModelInputApiService)
    }

    @Provides
    @Singleton
    fun provideQuestionnaireApiService(retrofit: Retrofit): QuestionnaireApiService {
        return retrofit.create(QuestionnaireApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideQuestionnaireApiRepository(
        questionnaireApiService: QuestionnaireApiService
    ): QuestionnaireApiRepository {
        return QuestionnaireApiRepository(questionnaireApiService)
    }

    @Provides
    @Singleton
    fun provideDrivingTipApiService(retrofit: Retrofit): DrivingTipApiService {
        return retrofit.create(DrivingTipApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDrivingTipApiRepository(
        drivingTipApiService: DrivingTipApiService
    ): DrivingTipApiRepository {
        return DrivingTipApiRepository(drivingTipApiService)
    }

    @Provides
    @Singleton
    fun provideEmbeddingApiService(retrofit: Retrofit): EmbeddingApiService {
        return retrofit.create(EmbeddingApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideEmbeddingApiRepository(
        embeddingApiService: EmbeddingApiService
    ): EmbeddingApiRepository {
        return EmbeddingApiRepository(embeddingApiService)
    }

    @Provides
    @Singleton
    fun provideLocationApiService(retrofit: Retrofit): LocationApiService {
        return retrofit.create(LocationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideLocationApiRepository(
        locationApiService: LocationApiService
    ): LocationApiRepository {
        return LocationApiRepository(locationApiService)
    }

    @Provides
    @Singleton
    fun provideNLGReportApiService(retrofit: Retrofit): NLGReportApiService {
        return retrofit.create(NLGReportApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNLGReportApiRepository(
        nlgReportApiService: NLGReportApiService
    ): NLGReportApiRepository {
        return NLGReportApiRepository(nlgReportApiService)
    }

    @Provides
    @Singleton
    fun provideRawSensorDataApiService(retrofit: Retrofit): RawSensorDataApiService {
        return retrofit.create(RawSensorDataApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRawSensorDataApiRepository(
        rawSensorDataApiService: RawSensorDataApiService
    ): RawSensorDataApiRepository {
        return RawSensorDataApiRepository(rawSensorDataApiService)
    }

    @Provides
    @Singleton
    fun provideTripApiService(retrofit: Retrofit): TripApiService {
        return retrofit.create(TripApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTripApiRepository(tripApiService: TripApiService): TripApiRepository {
        return TripApiRepository(tripApiService)
    }

    @Provides
    @Singleton
    fun provideTripSummaryApiService(retrofit: Retrofit): TripSummaryApiService {
        return retrofit.create(TripSummaryApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTripSummaryApiRepository(
        tripSummaryApiService: TripSummaryApiService
    ): TripSummaryApiRepository {
        return TripSummaryApiRepository(tripSummaryApiService)
    }

    @Provides
    @Singleton
    fun provideTripSummaryBehaviourApiService(retrofit: Retrofit): TripSummaryBehaviourApiService {
        return retrofit.create(TripSummaryBehaviourApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTripSummaryBehaviourApiRepository(
        tripSummaryBehaviourApiService: TripSummaryBehaviourApiService
    ): TripSummaryBehaviourApiRepository {
        return TripSummaryBehaviourApiRepository(tripSummaryBehaviourApiService)
    }

    @Provides
    @Singleton
    fun provideTripFeatureStateApiService(retrofit: Retrofit): TripFeatureStateApiService {
        return retrofit.create(TripFeatureStateApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTripFeatureStateApiRepository(
        tripFeatureStateApiService: TripFeatureStateApiService
    ): TripFeatureStateApiRepository {
        return TripFeatureStateApiRepository(tripFeatureStateApiService)
    }

    @Provides
    @Singleton
    fun provideUnsafeBehaviourApiService(retrofit: Retrofit): UnsafeBehaviourApiService {
        return retrofit.create(UnsafeBehaviourApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUnsafeBehaviourRepository(
        unsafeBehaviourApiService: UnsafeBehaviourApiService
    ): UnsafeBehaviourApiRepository {
        return UnsafeBehaviourApiRepository(unsafeBehaviourApiService)
    }

    @Provides
    @Singleton
    fun provideDriverProfileApiService(retrofit: Retrofit): DriverProfileApiService {
        return retrofit.create(DriverProfileApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDriverProfileApiRepository(
        driverProfileApiService: DriverProfileApiService,
        driverProfileRepository: DriverProfileRepository
    ): DriverProfileApiRepository {
        return DriverProfileApiRepository(driverProfileApiService, driverProfileRepository)
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiService: AuthApiService,
        secureTokenStorage: SecureTokenStorage
    ): AuthRepository {
        return AuthRepository(authApiService, secureTokenStorage)
    }

    @Provides
    @Singleton
    fun provideDriverFleetApiService(retrofit: Retrofit): DriverFleetApiService {
        return retrofit.create(DriverFleetApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDriverFleetApiRepository(
        driverFleetApiService: DriverFleetApiService,
        gson: Gson
    ): DriverFleetApiRepository {
        return DriverFleetApiRepository(driverFleetApiService, gson)
    }

    @Provides
    @Singleton
    fun provideDriverSyncApiService(retrofit: Retrofit): DriverSyncApiService {
        return retrofit.create(DriverSyncApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDriverSyncApiRepository(
        driverSyncApiService: DriverSyncApiService
    ): DriverSyncApiRepository {
        return DriverSyncApiRepository(driverSyncApiService)
    }

    @Provides
    @Singleton
    fun provideReportStatisticsApiService(retrofit: Retrofit): ReportStatisticsApiService {
        return retrofit.create(ReportStatisticsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideReportStatisticsApiRepository(
        reportStatisticsApiService: ReportStatisticsApiService
    ): ReportStatisticsApiRepository {
        return ReportStatisticsApiRepository(reportStatisticsApiService)
    }

    @Provides
    @Singleton
    fun provideRoadApiService(retrofit: Retrofit): RoadApiService {
        return retrofit.create(RoadApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRoadApiRepository(
        roadApiService: RoadApiService
    ): RoadApiRepository {
        return RoadApiRepository(roadApiService)
    }
}

