package com.uoa.core.apiServices.services.tripApiService

import com.uoa.core.apiServices.models.tripModels.TripCreate
import com.uoa.core.apiServices.models.tripModels.TripResponse
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripApiRepository @Inject constructor(
    private val tripApiService: TripApiService
) {

    // Create a new trip
    suspend fun createTrip(tripCreate: TripCreate): Resource<TripResponse> = withContext(Dispatchers.IO) {
        try {
            val tripResponse = tripApiService.createTrip(tripCreate)
            Resource.Success(tripResponse)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve a trip by ID
    suspend fun getTrip(tripId: String): Resource<TripResponse> = withContext(Dispatchers.IO) {
        try {
            val tripResponse = tripApiService.getTrip(tripId)
            Resource.Success(tripResponse)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("Trip not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve all trips with optional pagination
    suspend fun getAllTrips(skip: Int = 0, limit: Int = 50): Resource<List<TripResponse>> = withContext(Dispatchers.IO) {
        try {
            val tripList = tripApiService.getAllTrips(skip, limit)
            Resource.Success(tripList)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Update a trip
    suspend fun updateTrip(tripId: UUID, tripCreate: TripCreate): Resource<TripResponse> = withContext(Dispatchers.IO) {
        try {
            val tripResponse = tripApiService.updateTrip(tripId, tripCreate)
            Resource.Success(tripResponse)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Delete a trip
    suspend fun deleteTrip(tripId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            tripApiService.deleteTrip(tripId)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("Trip not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch create trips
    suspend fun batchCreateTrips(trips: List<TripCreate>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            tripApiService.batchCreateTrips(trips)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch delete trips
    suspend fun batchDeleteTrips(ids: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            tripApiService.batchDeleteTrips(ids)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }
}