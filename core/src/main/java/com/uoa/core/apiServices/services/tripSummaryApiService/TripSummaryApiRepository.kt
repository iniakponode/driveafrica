package com.uoa.core.apiServices.services.tripSummaryApiService

import com.uoa.core.apiServices.models.tripSummaryModels.TripSummaryCreate
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class TripSummaryApiRepository @Inject constructor(
    private val tripSummaryApiService: TripSummaryApiService
) {
    suspend fun batchCreateTripSummaries(
        summaries: List<TripSummaryCreate>
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            tripSummaryApiService.batchCreateTripSummaries(summaries)
            Resource.Success(Unit)
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: IOException) {
            Resource.Error("Network error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Unexpected error: ${e.localizedMessage}")
        }
    }
}
