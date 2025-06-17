package com.uoa.core.apiServices.services.locationApiService

import com.uoa.core.apiServices.models.locationModels.LocationCreate
import com.uoa.core.apiServices.models.locationModels.LocationResponse
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationApiRepository @Inject constructor(
    private val locationApiService: LocationApiService
) {

    // Create a new Location
    suspend fun createLocation(location: LocationCreate): Resource<LocationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = locationApiService.createLocation(location)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve a Location by ID
    suspend fun getLocation(locationId: String): Resource<LocationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = locationApiService.getLocation(locationId)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("Location not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve all Locations with optional pagination
    suspend fun getAllLocations(skip: Int = 0, limit: Int = 100): Resource<List<LocationResponse>> = withContext(Dispatchers.IO) {
        try {
            val responseList = locationApiService.getAllLocations(skip, limit)
            Resource.Success(responseList)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Update a Location
    suspend fun updateLocation(locationId: String, location: LocationCreate): Resource<LocationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = locationApiService.updateLocation(locationId, location)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Delete a Location
    suspend fun deleteLocation(locationId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            locationApiService.deleteLocation(locationId)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("Location not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch create Locations
    suspend fun batchCreateLocations(locations: List<LocationCreate>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            locationApiService.batchCreateLocations(locations)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch delete Locations
    suspend fun batchDeleteLocations(ids: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            locationApiService.batchDeleteLocations(ids)
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