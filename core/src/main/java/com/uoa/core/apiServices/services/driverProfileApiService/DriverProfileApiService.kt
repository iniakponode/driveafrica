package com.uoa.core.apiServices.services.driverProfileApiService


import com.uoa.core.apiServices.models.driverProfile.DriverProfileCreate
import com.uoa.core.apiServices.models.driverProfile.DriverProfileResponse
import retrofit2.http.*
import java.util.UUID

interface DriverProfileApiService {

    /**
     * Creates a new DriverProfile.
     *
     * @param driverProfile The DriverProfile data to create.
     * @return The created DriverProfile response.
     */
    @POST("/api/driver_profiles/")
    suspend fun createDriverProfile(@Body driverProfile: DriverProfileCreate): DriverProfileResponse

    /**
     * Retrieves a DriverProfile by its ID.
     *
     * @param profileId The ID of the DriverProfile to retrieve.
     * @return The DriverProfile response.
     */
    @GET("/api/driver_profiles/{profile_id}")
    suspend fun getDriverProfile(@Path("profile_id") profileId: String): DriverProfileResponse

    /**
     * Retrieves all DriverProfiles with optional pagination.
     *
     * @param skip The number of records to skip (for pagination).
     * @param limit The maximum number of records to retrieve.
     * @return A list of DriverProfile responses.
     */
    @GET("/api/driver_profiles/")
    suspend fun getAllDriverProfiles(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100 // Adjusted limit value for large datasets
    ): List<DriverProfileResponse>

    /**
     * Updates an existing DriverProfile.
     *
     * @param profileId The ID of the DriverProfile to update.
     * @param driverProfile The updated DriverProfile data.
     * @return The updated DriverProfile response.
     */
    @PUT("/api/driver_profiles/{profile_id}")
    suspend fun updateDriverProfile(
        @Path("profile_id") profileId: String,
        @Body driverProfile: DriverProfileCreate
    ): DriverProfileResponse

    /**
     * Deletes a DriverProfile by its ID.
     *
     * @param profileId The ID of the DriverProfile to delete.
     */
    @DELETE("/api/driver_profiles/{profile_id}")
    suspend fun deleteDriverProfile(@Path("profile_id") profileId: String): Unit

    // ----------------- Batch Operations -----------------

    /**
     * Batch creates multiple DriverProfiles.
     *
     * @param driverProfiles A list of DriverProfile data to create.
     * @return A map containing the status of each creation.
     */
    @POST("/api/driver_profiles/batch_create")
    suspend fun batchCreateDriverProfiles(@Body driverProfiles: List<DriverProfileCreate>): Map<String, String>

    /**
     * Batch deletes multiple DriverProfiles by their IDs.
     *
     * @param ids A list of DriverProfile IDs to delete.
     * @return A map containing the status of each deletion.
     */
    @HTTP(method = "DELETE", path = "/api/driver_profiles/batch_delete", hasBody = true)
    suspend fun batchDeleteDriverProfiles(@Body ids: List<UUID>): Map<UUID, UUID>
}

