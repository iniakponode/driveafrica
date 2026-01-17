package com.uoa.core.apiServices.services.fleetApiService

import com.google.gson.Gson
import com.uoa.core.apiServices.models.ApiErrorResponse
import com.uoa.core.apiServices.models.auth.FleetStatusResponse
import com.uoa.core.apiServices.models.auth.InviteCodeValidationRequest
import com.uoa.core.apiServices.models.auth.JoinFleetRequest
import com.uoa.core.apiServices.models.auth.JoinFleetResponse
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class DriverFleetApiRepository @Inject constructor(
    private val driverFleetApiService: DriverFleetApiService,
    private val gson: Gson
) {
    suspend fun validateInviteCode(inviteCode: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = driverFleetApiService.validateInviteCode(
                InviteCodeValidationRequest(code = inviteCode)
            )
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                val body = response.errorBody()?.string()
                val apiError = parseApiError(body)
                Resource.Error(mapJoinFleetError(apiError?.code, apiError?.message))
            }
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            val apiError = parseApiError(body)
            Resource.Error(mapJoinFleetError(apiError?.code, apiError?.message))
        } catch (e: IOException) {
            Resource.Error("Network error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Unexpected error: ${e.localizedMessage}")
        }
    }

    suspend fun joinFleet(inviteCode: String): Resource<JoinFleetResponse> = withContext(Dispatchers.IO) {
        try {
            val response = driverFleetApiService.joinFleet(JoinFleetRequest(inviteCode = inviteCode))
            Resource.Success(response)
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            val apiError = parseApiError(body)
            Resource.Error(mapJoinFleetError(apiError?.code, apiError?.message))
        } catch (e: IOException) {
            Resource.Error("Network error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Unexpected error: ${e.localizedMessage}")
        }
    }

    suspend fun getFleetStatus(): Resource<FleetStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = driverFleetApiService.getFleetStatus()
            Resource.Success(response)
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            val apiError = parseApiError(body)
            Resource.Error(buildErrorMessage("Server error (${e.code()}): ${e.message()}", apiError?.message))
        } catch (e: IOException) {
            Resource.Error("Network error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Unexpected error: ${e.localizedMessage}")
        }
    }

    suspend fun joinWithCode(inviteCode: String): Resource<FleetStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = driverFleetApiService.joinWithCode(JoinFleetRequest(inviteCode = inviteCode))
            Resource.Success(response)
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            val apiError = parseApiError(body)
            Resource.Error(mapJoinFleetError(apiError?.code, apiError?.message))
        } catch (e: IOException) {
            Resource.Error("Network error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Unexpected error: ${e.localizedMessage}")
        }
    }

    suspend fun cancelPendingRequest(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            driverFleetApiService.cancelJoinRequest()
            Resource.Success(Unit)
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            val apiError = parseApiError(body)
            Resource.Error(buildErrorMessage("Server error (${e.code()}): ${e.message()}", apiError?.message))
        } catch (e: IOException) {
            Resource.Error("Network error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Unexpected error: ${e.localizedMessage}")
        }
    }

    private fun parseApiError(body: String?): ApiErrorResponse? {
        return body?.takeIf { it.isNotBlank() }?.let {
            runCatching { gson.fromJson(it, ApiErrorResponse::class.java) }.getOrNull()
        }
    }

    private fun mapJoinFleetError(code: String?, message: String?): String {
        return when (code) {
            "INVALID_CODE" -> "This invite code doesn't exist. Please check and try again."
            "EXPIRED_CODE" -> "This invite code has expired. Ask your fleet manager for a new one."
            "CODE_LIMIT_REACHED" -> "This invite code can't be used anymore. Contact your fleet manager."
            "ALREADY_IN_FLEET" -> "You're already part of a fleet. Leave your current fleet first."
            "PENDING_REQUEST" -> "You already have a pending request. Wait for approval or cancel it."
            else -> message ?: "Something went wrong. Please try again later."
        }
    }

    private fun buildErrorMessage(base: String, bodyMessage: String?): String =
        if (bodyMessage.isNullOrBlank()) base else "$base | $bodyMessage"
}
