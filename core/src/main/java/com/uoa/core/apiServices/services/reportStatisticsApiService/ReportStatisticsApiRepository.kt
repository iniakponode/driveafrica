package com.uoa.core.apiServices.services.reportStatisticsApiService

import com.uoa.core.apiServices.models.reportStatisticsModels.ReportStatisticsCreate
import com.uoa.core.apiServices.models.reportStatisticsModels.ReportStatisticsResponse
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportStatisticsApiRepository @Inject constructor(
    private val reportStatisticsApiService: ReportStatisticsApiService
) {

    /**
     * Creates a new ReportStatistics entry.
     *
     * @param reportStatistics The ReportStatistics data to create.
     * @return Resource wrapping the ReportStatistics response or an error message.
     */
    suspend fun createReportStatistics(reportStatistics: ReportStatisticsCreate): Resource<ReportStatisticsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = reportStatisticsApiService.createReportStatistics(reportStatistics)
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
     * Retrieves a ReportStatistics entry by its ID.
     *
     * @param reportId The ID of the ReportStatistics to retrieve.
     * @return Resource wrapping the ReportStatistics response or an error message.
     */
    suspend fun getReportStatistics(reportId: String): Resource<ReportStatisticsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = reportStatisticsApiService.getReportStatistics(reportId)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("ReportStatistics not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    /**
     * Retrieves all ReportStatistics entries with optional pagination.
     *
     * @param skip The number of records to skip (for pagination).
     * @param limit The maximum number of records to retrieve.
     * @return Resource wrapping a list of ReportStatistics responses or an error message.
     */
    suspend fun getAllReportStatistics(skip: Int = 0, limit: Int = 100): Resource<List<ReportStatisticsResponse>> = withContext(Dispatchers.IO) {
        try {
            val responseList = reportStatisticsApiService.getAllReportStatistics(skip, limit)
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
     * Updates an existing ReportStatistics entry.
     *
     * @param reportId The ID of the ReportStatistics to update.
     * @param reportStatistics The updated ReportStatistics data.
     * @return Resource wrapping the updated ReportStatistics response or an error message.
     */
    suspend fun updateReportStatistics(reportId: String, reportStatistics: ReportStatisticsCreate): Resource<ReportStatisticsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = reportStatisticsApiService.updateReportStatistics(reportId, reportStatistics)
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
     * Deletes a ReportStatistics entry by its ID.
     *
     * @param reportId The ID of the ReportStatistics to delete.
     * @return Resource wrapping Unit on success or an error message.
     */
    suspend fun deleteReportStatistics(reportId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            reportStatisticsApiService.deleteReportStatistics(reportId)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("ReportStatistics not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // ----------------- Batch Operations -----------------

    /**
     * Batch creates multiple ReportStatistics entries.
     *
     * @param reportStatisticsList A list of ReportStatistics data to create.
     * @return Resource wrapping Unit on success or an error message.
     */
    suspend fun batchCreateReportStatistics(reportStatisticsList: List<ReportStatisticsCreate>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            reportStatisticsApiService.batchCreateReportStatistics(reportStatisticsList)
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
     * Batch deletes multiple ReportStatistics entries by their IDs.
     *
     * @param ids A list of ReportStatistics IDs to delete.
     * @return Resource wrapping Unit on success or an error message.
     */
    suspend fun batchDeleteReportStatistics(ids: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            reportStatisticsApiService.batchDeleteReportStatistics(ids)
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
