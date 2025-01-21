package com.uoa.core.apiServices

import com.google.gson.GsonBuilder
import com.uoa.core.BuildConfig
import com.uoa.core.apiServices.services.aiModellInputApiService.AIModelInputApiRepository
import com.uoa.core.apiServices.services.aiModellInputApiService.AIModelInputApiService
import com.uoa.core.apiServices.services.alcoholQuestionnaireService.QuestionnaireApiRepository
import com.uoa.core.apiServices.services.alcoholQuestionnaireService.QuestionnaireApiService
import com.uoa.core.apiServices.services.driverProfileApiService.DriverProfileApiRepository
import com.uoa.core.apiServices.services.driverProfileApiService.DriverProfileApiService
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
import com.uoa.core.apiServices.services.unsafeBehaviourApiService.UnsafeBehaviourApiRepository
import com.uoa.core.apiServices.services.unsafeBehaviourApiService.UnsafeBehaviourApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProvideApiServiceRetrofitInstanceModule {
    const val DRIVE_AFRICA_BASE_URL = BuildConfig.DRIVE_AFRICA_BASE_URL
//    const val DRIVE_AFRICA_BASE_URL= BuildConfig.EMULATOR_BASE_URL
    private val gson by lazy {
        GsonBuilder().create()
    }

    private fun getDriveAfricaHttpClientBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                // Retrieve the token from secure storage
                val token = getAuthToken()
                if (token != null) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
    }

    private fun getAuthToken(): String? {
        // Implement secure retrieval of the auth token
        // For example, using EncryptedSharedPreferences
        return null // Replace with actual implementation
    }

    // Function to get Retrofit instance with optional headers
    fun getRetrofitInstance(
        baseUrl: String,
        headers: Map<String, String>? = null
    ): Retrofit {
        val clientBuilder = getDriveAfricaHttpClientBuilder()

        headers?.let {
            clientBuilder.addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                for ((key, value) in it) {
                    requestBuilder.addHeader(key, value)
                }
                chain.proceed(requestBuilder.build())
            }
        }
        val client = clientBuilder.build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(this.gson))
            .client(client)
            .build()
    }

    // Provide AIModelInputApiService
    @Provides
    @Singleton
    fun provideAIModelInputApiService(): AIModelInputApiService {
        return getRetrofitInstance(
            baseUrl = DRIVE_AFRICA_BASE_URL
            // Add headers if needed
        ).create(AIModelInputApiService::class.java)
    }

    // Provide AIModelInputRepository
    @Provides
    @Singleton
    fun provideAiModelInputRepository(
        aiModelInputApiService: AIModelInputApiService
    ): AIModelInputApiRepository {
        return AIModelInputApiRepository(aiModelInputApiService)
    }


    // Provide AlcoholQuestionnaireApiService
    @Provides
    @Singleton
    fun provideQuestionnaireApiService(): QuestionnaireApiService {
        return getRetrofitInstance(
            baseUrl = DRIVE_AFRICA_BASE_URL
            // Add headers if needed
        ).create(QuestionnaireApiService::class.java)
    }

    // Provide Questionnaire API Repository
    @Provides
    @Singleton
    fun provideQuestionnaireApiRepository(
        questionnaireApiService: QuestionnaireApiService
    ): QuestionnaireApiRepository {
        return QuestionnaireApiRepository(questionnaireApiService)
    }

    // Provide DrivingTipApiService
    @Provides
    @Singleton
    fun provideDrivingTipApiService(): DrivingTipApiService {
        return getRetrofitInstance(
            baseUrl = DRIVE_AFRICA_BASE_URL
            // Add headers if needed
        ).create(DrivingTipApiService::class.java)
    }

    // Provide DrivingTipRepository
    @Provides
    @Singleton
    fun provideDrivingTipRepository(
        drivingTipApiService: DrivingTipApiService
    ): DrivingTipApiRepository {
        return DrivingTipApiRepository(drivingTipApiService)
    }

    // Provide EmbeddingApiService
    @Provides
    @Singleton
    fun provideEmbeddingApiService(): EmbeddingApiService {
        return getRetrofitInstance(
            baseUrl = DRIVE_AFRICA_BASE_URL
            // Add headers if needed
        ).create(EmbeddingApiService::class.java)
    }

    // Provide EmbeddingRepository
    @Provides
    @Singleton
    fun provideEmbeddingRepository(
        embeddingApiService: EmbeddingApiService
    ): EmbeddingApiRepository {
        return EmbeddingApiRepository(embeddingApiService)
    }

    // Provide LocationApiService
    @Provides
    @Singleton
    fun provideLocationApiService(): LocationApiService {
        return getRetrofitInstance(
            baseUrl = DRIVE_AFRICA_BASE_URL
            // Add headers if needed
        ).create(LocationApiService::class.java)
    }

    // Provide LocationRepository
    @Provides
    @Singleton
    fun provideLocationRepository(
        locationApiService: LocationApiService
    ): LocationApiRepository {
        return LocationApiRepository(locationApiService)
    }

    // Provide NLGReportApiService
    @Provides
    @Singleton
    fun provideNLGReportApiService(): NLGReportApiService {
        return getRetrofitInstance(
            baseUrl = DRIVE_AFRICA_BASE_URL
            // Add headers if needed
        ).create(NLGReportApiService::class.java)
    }

    // Provide NLGReportRepository
    @Provides
    @Singleton
    fun provideNLGReportRepository(
        nlgReportApiService: NLGReportApiService
    ): NLGReportApiRepository {
        return NLGReportApiRepository(nlgReportApiService)
    }

    @Provides
    @Singleton
    fun provideRawSensorDataApiService(): RawSensorDataApiService {
        return getRetrofitInstance(
            baseUrl = DRIVE_AFRICA_BASE_URL
        ).create(RawSensorDataApiService::class.java)
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
    fun provideTripApiService(): TripApiService {
        return getRetrofitInstance(
            baseUrl = DRIVE_AFRICA_BASE_URL
            // Add headers if needed
        ).create(TripApiService::class.java)
    }

    @Provides
    @Singleton
    fun providesTripApiRepository(tripApiService: TripApiService): TripApiRepository{
        return TripApiRepository(tripApiService)
    }

    // Provide UnsafeBehaviourApiService
    @Provides
    @Singleton
    fun provideUnsafeBehaviourApiService(): UnsafeBehaviourApiService {
        return getRetrofitInstance(
            baseUrl = DRIVE_AFRICA_BASE_URL
            // Add headers if needed
        ).create(UnsafeBehaviourApiService::class.java)
    }

    // Provide UnsafeBehaviourRepository
    @Provides
    @Singleton
    fun provideUnsafeBehaviourRepository(
        unsafeBehaviourApiService: UnsafeBehaviourApiService
    ): UnsafeBehaviourApiRepository {
        return UnsafeBehaviourApiRepository(unsafeBehaviourApiService)
    }

    // Provide DriverProfileApiService
    @Provides
    @Singleton
    fun provideDriverProfileApiService(): DriverProfileApiService {
        return getRetrofitInstance(
            baseUrl = DRIVE_AFRICA_BASE_URL
            // Add headers if needed, e.g., authentication
        ).create(DriverProfileApiService::class.java)
    }

    // Provide DriverProfileRepository
    @Provides
    @Singleton
    fun provideDriverProfileRepository(
        driverProfileApiService: DriverProfileApiService
    ): DriverProfileApiRepository {
        return DriverProfileApiRepository(driverProfileApiService)
    }

    // Provide ReportStatisticsApiService
    @Provides
    @Singleton
    fun provideReportStatisticsApiService(): ReportStatisticsApiService {
        return getRetrofitInstance(
            baseUrl = DRIVE_AFRICA_BASE_URL
            // Add headers if needed, e.g., authentication
        ).create(ReportStatisticsApiService::class.java)
    }

    // Provide ReportStatisticsRepository
    @Provides
    @Singleton
    fun provideReportStatisticsApiRepository(
        reportStatisticsApiService: ReportStatisticsApiService
    ): ReportStatisticsApiRepository {
        return ReportStatisticsApiRepository(reportStatisticsApiService)
    }

    // Provide RoadApiRepository
    @Provides
    @Singleton
    fun provideRoadApiRepository(
        roadApiService: RoadApiService
    ): RoadApiRepository {
        return RoadApiRepository(roadApiService)
    }

    @Provides
    @Singleton
    fun provideRoadApiService(): RoadApiService {
        return getRetrofitInstance(
            baseUrl = DRIVE_AFRICA_BASE_URL
            // Add headers if needed, e.g., authentication
        ).create(RoadApiService::class.java)
    }


}


