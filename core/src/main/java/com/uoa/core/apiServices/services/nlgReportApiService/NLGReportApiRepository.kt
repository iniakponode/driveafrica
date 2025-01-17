package com.uoa.core.apiServices.services.nlgReportApiService

import com.uoa.core.apiServices.models.nlgReportModels.NLGReportCreate
import com.uoa.core.apiServices.models.nlgReportModels.NLGReportResponse
import com.uoa.core.utils.Resource
import java.io.IOException
import retrofit2.HttpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NLGReportApiRepository @Inject constructor(
    private val nlgReportApiService: NLGReportApiService
) {

    // Create a new NLGReport
    suspend fun createNLGReport(nlgReport: NLGReportCreate): Resource<NLGReportResponse> = withContext(Dispatchers.IO) {
        try {
            val response = nlgReportApiService.createNLGReport(nlgReport)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve an NLGReport by ID
    suspend fun getNLGReport(reportId: String): Resource<NLGReportResponse> = withContext(Dispatchers.IO) {
        try {
            val response = nlgReportApiService.getNLGReport(reportId)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("NLGReport not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve all NLGReports with optional pagination
    suspend fun getAllNLGReports(skip: Int = 0, limit: Int = 100): Resource<List<NLGReportResponse>> = withContext(Dispatchers.IO) {
        try {
            val responseList = nlgReportApiService.getAllNLGReports(skip, limit)
            Resource.Success(responseList)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Update an NLGReport
    suspend fun updateNLGReport(reportId: String, nlgReport: NLGReportCreate): Resource<NLGReportResponse> = withContext(Dispatchers.IO) {
        try {
            val response = nlgReportApiService.updateNLGReport(reportId, nlgReport)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Delete an NLGReport
    suspend fun deleteNLGReport(reportId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            nlgReportApiService.deleteNLGReport(reportId)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("NLGReport not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch create NLGReports
    suspend fun batchCreateNLGReports(nlgReports: List<NLGReportCreate>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            nlgReportApiService.batchCreateNLGReports(nlgReports)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch delete NLGReports
    suspend fun batchDeleteNLGReports(ids: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            nlgReportApiService.batchDeleteNLGReports(ids)
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