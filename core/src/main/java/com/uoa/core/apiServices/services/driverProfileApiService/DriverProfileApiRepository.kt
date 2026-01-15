package com.uoa.core.apiServices.services.driverProfileApiService

import com.uoa.core.apiServices.models.driverProfile.DriverProfileCreate
import com.uoa.core.apiServices.models.driverProfile.DriverProfileResponse
import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.database.repository.DriverProfileRepository
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class DriverProfileApiRepository @Inject constructor(
    private val driverProfileApiService: DriverProfileApiService,
    private val driverProfileRepository: DriverProfileRepository
) {

    /**
     * Creates a new DriverProfile, checking for email uniqueness via the API first.
     *
     * @param driverProfile The DriverProfile data to create.
     * @return Resource wrapping the DriverProfile response or an error message.
     */
    suspend fun createDriverProfile(driverProfile: DriverProfileCreate): Resource<DriverProfileResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = driverProfileApiService.createDriverProfile(driverProfile)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Return the remote response directly; no local DB update here
                        return@withContext Resource.Success(body)
                    } else {
                        // Body is null even though 2xx
                        return@withContext Resource.Error("Body was null despite success code.")
                    }
                } else {
                    val errorBodyText = response.errorBody()?.string()
                    val errorMessage = buildString {
                        append("Failed: ${response.code()} - ${response.message()}")
                        if (!errorBodyText.isNullOrBlank()) {
                            append(" | Body: $errorBodyText")
                        }
                    }
                    return@withContext Resource.Error(errorMessage)
                }
            } catch (e: IOException) {
                return@withContext Resource.Error("Network error: ${e.localizedMessage}")
            } catch (e: HttpException) {
                return@withContext Resource.Error("Server error (${e.code()}): ${e.message()}")
            } catch (e: Exception) {
                // If something else (like JSON parsing) fails
                return@withContext Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
            }
        }


    /**
     * Retrieves a DriverProfile by its ID.
     *
     * @param profileId The ID of the DriverProfile to retrieve.
     * @return Resource wrapping the DriverProfile response or an error message.
     */
    suspend fun getDriverProfile(profileId: String): Resource<DriverProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val response = driverProfileApiService.getDriverProfile(profileId)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("DriverProfile not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }



    /**
     * Retrieves all DriverProfiles with optional pagination.
     *
     * @param skip The number of records to skip (for pagination).
     * @param limit The maximum number of records to retrieve.
     * @return Resource wrapping a list of DriverProfile responses or an error message.
     */
    suspend fun getAllDriverProfiles(skip: Int = 0, limit: Int = 100): Resource<List<DriverProfileResponse>> = withContext(Dispatchers.IO) {
        try {
            val responseList = driverProfileApiService.getAllDriverProfiles(skip, limit)
            Resource.Success(responseList)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    /**
     * Updates an existing DriverProfile.
     *
     * @param profileId The ID of the DriverProfile to update.
     * @param driverProfile The updated DriverProfile data.
     * @return Resource wrapping the updated DriverProfile response or an error message.
     */
    suspend fun updateDriverProfile(profileId: String, driverProfile: DriverProfileCreate): Resource<DriverProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val response = driverProfileApiService.updateDriverProfile(profileId, driverProfile)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    /**
     * Deletes a DriverProfile by its ID.
     *
     * @param profileId The ID of the DriverProfile to delete.
     * @return Resource wrapping Unit on success or an error message.
     */
    suspend fun deleteDriverProfile(profileId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            driverProfileApiService.deleteDriverProfile(profileId)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("DriverProfile not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // ----------------- Batch Operations -----------------

    /**
     * Batch creates multiple DriverProfiles.
     *
     * @param driverProfiles A list of DriverProfile data to create.
     * @return Resource wrapping Unit on success or an error message.
     */
    suspend fun batchCreateDriverProfiles(driverProfiles: List<DriverProfileCreate>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            driverProfileApiService.batchCreateDriverProfiles(driverProfiles)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    /**
     * Batch deletes multiple DriverProfiles by their IDs.
     *
     * @param ids A list of DriverProfile IDs to delete.
     * @return Resource wrapping Unit on success or an error message.
     */
    suspend fun batchDeleteDriverProfiles(ids: List<UUID>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            driverProfileApiService.batchDeleteDriverProfiles(ids)
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
