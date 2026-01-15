package com.uoa.core.apiServices.services.reportStatisticsApiService

// ReportStatisticsApiService.kt

import com.uoa.core.apiServices.models.reportStatisticsModels.ReportStatisticsCreate
import com.uoa.core.apiServices.models.reportStatisticsModels.ReportStatisticsResponse
import retrofit2.http.*

interface ReportStatisticsApiService {

    /**
     * Creates a new ReportStatistics entry.
     *
     * @param reportStatistics The ReportStatistics data to create.
     * @return The created ReportStatistics response.
     */
    @POST("/api/report_statistics/")
    suspend fun createReportStatistics(@Body reportStatistics: ReportStatisticsCreate): ReportStatisticsResponse

    /**
     * Retrieves a ReportStatistics entry by its ID.
     *
     * @param reportId The ID of the ReportStatistics to retrieve.
     * @return The ReportStatistics response.
     */
    @GET("/api/report_statistics/{report_id}")
    suspend fun getReportStatistics(@Path("report_id") reportId: String): ReportStatisticsResponse

    /**
     * Retrieves all ReportStatistics entries with optional pagination.
     *
     * @param skip The number of records to skip (for pagination).
     * @param limit The maximum number of records to retrieve.
     * @return A list of ReportStatistics responses.
     */
    @GET("/api/report_statistics/")
    suspend fun getAllReportStatistics(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100 // Adjusted limit value for large datasets
    ): List<ReportStatisticsResponse>

    /**
     * Updates an existing ReportStatistics entry.
     *
     * @param reportId The ID of the ReportStatistics to update.
     * @param reportStatistics The updated ReportStatistics data.
     * @return The updated ReportStatistics response.
     */
    @PUT("/api/report_statistics/{report_id}")
    suspend fun updateReportStatistics(
        @Path("report_id") reportId: String,
        @Body reportStatistics: ReportStatisticsCreate
    ): ReportStatisticsResponse

    /**
     * Deletes a ReportStatistics entry by its ID.
     *
     * @param reportId The ID of the ReportStatistics to delete.
     */
    @DELETE("/api/report_statistics/{report_id}")
    suspend fun deleteReportStatistics(@Path("report_id") reportId: String): Unit

    // ----------------- Batch Operations -----------------

    /**
     * Batch creates multiple ReportStatistics entries.
     *
     * @param reportStatisticsList A list of ReportStatistics data to create.
     * @return A map containing the status of each creation.
     */
    @POST("/api/report_statistics/batch_create")
    suspend fun batchCreateReportStatistics(@Body reportStatisticsList: List<ReportStatisticsCreate>): Map<String, Int>

    /**
     * Batch deletes multiple ReportStatistics entries by their IDs.
     *
     * @param ids A list of ReportStatistics IDs to delete.
     * @return A map containing the status of each deletion.
     */
    @HTTP(method = "DELETE", path = "/api/report_statistics/batch_delete", hasBody = true)
    suspend fun batchDeleteReportStatistics(@Body ids: List<String>): Unit
}
