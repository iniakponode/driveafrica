// NLGReportApiService.kt
package com.uoa.core.apiServices.services.nlgReportApiService

import com.uoa.core.apiServices.models.nlgReportModels.NLGReportCreate
import com.uoa.core.apiServices.models.nlgReportModels.NLGReportResponse
import retrofit2.http.*

interface NLGReportApiService {

    @POST("/api/nlg_reports/")
    suspend fun createNLGReport(@Body nlgReport: NLGReportCreate): NLGReportResponse

    @GET("/api/nlg_reports/{report_id}")
    suspend fun getNLGReport(@Path("report_id") reportId: String): NLGReportResponse

    @GET("/api/nlg_reports/")
    suspend fun getAllNLGReports(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100 // Adjusted limit value for large datasets
    ): List<NLGReportResponse>

    @PUT("/api/nlg_reports/{report_id}")
    suspend fun updateNLGReport(
        @Path("report_id") reportId: String,
        @Body nlgReport: NLGReportCreate
    ): NLGReportResponse

    @DELETE("/api/nlg_reports/{report_id}")
    suspend fun deleteNLGReport(@Path("report_id") reportId: String): Unit

    // Batch Operations
    @POST("/api/nlg_reports/batch_create")
    suspend fun batchCreateNLGReports(@Body nlgReports: List<NLGReportCreate>): Map<String, String>

    @HTTP(method = "DELETE", path = "/api/nlg_reports/batch_delete", hasBody = true)
    suspend fun batchDeleteNLGReports(@Body ids: List<String>): Unit
}
