package com.uoa.core.apiServices.services.driverSyncApiService

import com.uoa.core.apiServices.models.driverSyncModels.DriverSyncPayload
import com.uoa.core.apiServices.models.driverSyncModels.DriverSyncResponse
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriverSyncApiRepository @Inject constructor(
    private val driverSyncApiService: DriverSyncApiService
) {
    suspend fun syncDriverData(payload: DriverSyncPayload): Resource<DriverSyncResponse> =
        withContext(Dispatchers.IO) {
            try {
                Resource.Success(driverSyncApiService.syncDriverData(payload))
            } catch (e: IOException) {
                Resource.Error("Network error: Please check your internet connection.")
            } catch (e: HttpException) {
                Resource.Error("Server error (${e.code()}): ${e.message()}")
            } catch (e: Exception) {
                Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
            }
        }
}
