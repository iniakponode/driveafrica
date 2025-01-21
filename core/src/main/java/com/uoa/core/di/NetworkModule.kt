package com.uoa.core.di

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.GsonBuilder
import com.uoa.core.network.NetworkMonitorImpl
import com.uoa.core.network.apiservices.OSMRoadApiService
import com.uoa.core.network.apiservices.ChatGPTApiService
import com.uoa.core.network.apiservices.GeminiApiService
import com.uoa.core.nlg.repository.NLGEngineRepository
import com.uoa.core.nlg.lngrepositoryimpl.remote.nlgApiRepositoryImpl.NLGEngineRepositoryImpl
import com.uoa.core.utils.internetconnectivity.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.uoa.core.BuildConfig
import com.uoa.core.network.apiservices.OSMSpeedLimitApiService
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
/**
 * NetworkModule: Provides Retrofit instances and API services.
 *
 * This code integrates with OpenStreetMap's Nominatim service for reverse geocoding.
 * Please note:
 *  - You must include appropriate attribution for OpenStreetMap data:
 *    "Data © OpenStreetMap contributors"
 *  - Review the usage policy and rate limits:
 *    https://operations.osmfoundation.org/policies/nominatim/
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CHATGPT_BASE_URL = "https://api.openai.com/v1/"

    // Removed GEMINI_BASE_UR and left only GEMINI_BASE_URL below
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    // Renamed to clarify it’s for Nominatim
    private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/"

    private const val DEFAULT_BASE_URL = "https://safe-drive-africa-9fd1c750b777.herokuapp.com/"

    //    private const val DRIVE_AFRICA_BASE_URL="http://localhost:8000/"
    //    private const val DRIVE_AFRICA_BASE_URL = BuildConfig.DRIVE_AFRICA_BASE_URL

    private val gson by lazy {
        GsonBuilder().create()
    }

    // Create a new OkHttpClient.Builder with logging interceptor
    private fun getHttpClientBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
    }

    // Get Retrofit instance with optional headers
    private fun getRetrofitInstance(
        baseUrl: String,
        headers: Map<String, String>? = null
    ): Retrofit {
        val clientBuilder = getHttpClientBuilder()

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
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
    }

    // Provide ChatGPTApiService
    @Provides
    @Singleton
    fun provideChatGPTApiService(@ApplicationContext context: Context): ChatGPTApiService {
        val apiKey = BuildConfig.CHAT_GPT_API_KEY
        if (apiKey.isEmpty()) {
            throw IllegalStateException(
                "ChatGPT API key not found. " +
                        "Please set the API key in local.properties."
            )
        }

        return getRetrofitInstance(
            baseUrl = CHATGPT_BASE_URL,
            headers = mapOf("Authorization" to "Bearer $apiKey")
        ).create(ChatGPTApiService::class.java)
    }

    // Provide GeminiApiService (for generative language API)
    @Provides
    @Singleton
    fun provideGeminiApiService(@ApplicationContext context: Context): GeminiApiService {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty()) {
            throw IllegalStateException(
                "Gemini API key not found. " +
                        "Please set the API key in local.properties."
            )
        }

        return getRetrofitInstance(
            baseUrl = GEMINI_BASE_URL,
            headers = mapOf("Authorization" to "Bearer $apiKey")
        ).create(GeminiApiService::class.java)
    }

    /**
     * Provides OSMRoadApiService (Nominatim)
     *
     * Data © OpenStreetMap contributors
     * https://operations.osmfoundation.org/policies/nominatim/
     */
    @Provides
    @Singleton
    fun provideOSMApiService(): OSMRoadApiService {


        // For compliance, identify your app and contact
        val customHeaders = mapOf(
            "User-Agent" to "DriveAfrica/1.0 (i.thompson.21@abdn.ac.uk)",
            "Referer" to "https://github.com/iniakponode/driveafrica"
        )
        return getRetrofitInstance(
            baseUrl = NOMINATIM_BASE_URL
        ).create(OSMRoadApiService::class.java)
    }



    /**
     * Provides OSMRoadApiService (Nominatim)
     *
     * Data © OpenStreetMap contributors
     * https://operations.osmfoundation.org/policies/nominatim/
     */
    private const val OVERPASS_BASE_URL = "https://overpass-api.de/api/"

    @Provides
    @Singleton
    fun provideOSMSpeedLimitApiService(): OSMSpeedLimitApiService {
        val customHeaders = mapOf(
            "User-Agent" to "DriveAfrica/1.0 (i.thompson.21@abdn.ac.uk)",
            "Referer" to "https://github.com/iniakponode/driveafrica"
        )

        return getRetrofitInstance(
            baseUrl = OVERPASS_BASE_URL,
            headers = customHeaders
        ).create(OSMSpeedLimitApiService::class.java)
    }


    // Provide a GenerativeModel (example)
    @Provides
    fun provideGenerativeModel(): GenerativeModel {
        // Replace with your actual initialization
        return GenerativeModel("gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    // Provide NLGEngineRepository (example)
    @Provides
    @Singleton
    fun provideNLGEngineRepository(
        chatGPTApiService: ChatGPTApiService,
        geminiApiService: GeminiApiService,
        osmRoadApiService: OSMRoadApiService
    ): NLGEngineRepository {
        return NLGEngineRepositoryImpl(
            chatGPTApiService,
            geminiApiService,
            osmRoadApiService
        )
    }

    // Provide network monitor (example)
    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitorImpl(context)
    }
}