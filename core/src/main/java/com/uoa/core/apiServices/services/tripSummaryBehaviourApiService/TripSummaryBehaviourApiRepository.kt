package com.uoa.core.apiServices.services.tripSummaryBehaviourApiService

import com.uoa.core.apiServices.models.tripSummaryModels.TripSummaryBehaviourCreate
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class TripSummaryBehaviourApiRepository @Inject constructor(
    private val tripSummaryBehaviourApiService: TripSummaryBehaviourApiService
) {
    suspend fun batchCreateTripSummaryBehaviours(
        behaviours: List<TripSummaryBehaviourCreate>
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            tripSummaryBehaviourApiService.batchCreateTripSummaryBehaviours(behaviours)
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
