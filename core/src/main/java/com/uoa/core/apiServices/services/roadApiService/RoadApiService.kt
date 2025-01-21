package com.uoa.core.apiServices.services.roadApiService

import com.uoa.core.apiServices.models.roadModels.RoadCreate
import com.uoa.core.apiServices.models.roadModels.RoadResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RoadApiService {

    /**
     * Creates a new Road entry.
     *
     * @param road The Road data to create.
     * @return The created Road response.
     */
    @POST("/api/roads/")
    suspend fun createRoad(@Body road: RoadCreate): RoadResponse

    /**
     * Retrieves a Road entry by its ID.
     *
     * @param roadId The ID of the Road to retrieve.
     * @return The Road response.
     */
    @GET("/api/roads/{road_id}")
    suspend fun getRoad(@Path("road_id") roadId: String): RoadResponse

    /**
     * Retrieves all Road entries with optional pagination.
     *
     * @param skip The number of records to skip (for pagination).
     * @param limit The maximum number of records to retrieve.
     * @return A list of Road responses.
     */
    @GET("/api/roads/")
    suspend fun getAllRoads(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<RoadResponse>

    /**
     * Updates an existing Road entry.
     *
     * @param roadId The ID of the Road to update.
     * @param road The updated Road data.
     * @return The updated Road response.
     */
    @PUT("/api/roads/{road_id}")
    suspend fun updateRoad(
        @Path("road_id") roadId: String,
        @Body road: RoadCreate
    ): RoadResponse

    /**
     * Deletes a Road entry by its ID.
     *
     * @param roadId The ID of the Road to delete.
     */
    @DELETE("/api/roads/{road_id}")
    suspend fun deleteRoad(@Path("road_id") roadId: String)

    // ----------------- Batch Operations -----------------

    /**
     * Batch creates multiple Road entries.
     *
     * @param roadList A list of Road data to create.
     * @return A map containing the status of each creation.
     */
    @POST("/api/roads/batch_create")
    suspend fun batchCreateRoads(@Body roadList: List<RoadCreate>): Map<String, String>

    /**
     * Batch deletes multiple Road entries by their IDs.
     *
     * @param ids A list of Road IDs to delete.
     * @return A map containing the status of each deletion.
     */
    @HTTP(method = "DELETE", path = "/api/roads/batch_delete", hasBody = true)
    suspend fun batchDeleteRoads(@Body ids: List<String>): Map<String, String>
}
