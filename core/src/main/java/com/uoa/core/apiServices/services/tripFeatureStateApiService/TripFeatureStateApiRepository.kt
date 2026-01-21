package com.uoa.core.apiServices.services.tripFeatureStateApiService

import com.uoa.core.apiServices.models.tripFeatureModels.TripFeatureStateCreate
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class TripFeatureStateApiRepository @Inject constructor(
    private val tripFeatureStateApiService: TripFeatureStateApiService
) {
    suspend fun batchCreateTripFeatureStates(
        states: List<TripFeatureStateCreate>
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            tripFeatureStateApiService.batchCreateTripFeatureStates(states)
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
