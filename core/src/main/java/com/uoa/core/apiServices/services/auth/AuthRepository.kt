package com.uoa.core.apiServices.services.auth

import com.uoa.core.apiServices.models.auth.AuthResponse
import com.uoa.core.apiServices.models.auth.LoginRequest
import com.uoa.core.apiServices.models.auth.RegisterRequest
import com.uoa.core.apiServices.models.driverProfile.DriverProfileResponse
import com.uoa.core.utils.Resource
import com.uoa.core.utils.SecureTokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val secureTokenStorage: SecureTokenStorage
) {

    suspend fun registerDriver(request: RegisterRequest): Resource<AuthResponse> =
        performAuthRequest { authApiService.registerDriver(request) }

    suspend fun loginDriver(request: LoginRequest): Resource<AuthResponse> =
        performAuthRequest { authApiService.loginDriver(request) }

    private suspend fun performAuthRequest(
        call: suspend () -> AuthResponse
    ): Resource<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = call()
            secureTokenStorage.saveToken(response.token)
            Resource.Success(response)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Resource.Error(buildErrorMessage("Server error (${e.code()}): ${e.message()}", errorBody))
        } catch (e: IOException) {
            Resource.Error("Network error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Unexpected error: ${e.localizedMessage}")
        }
    }

    suspend fun getCurrentDriverProfile(): Resource<DriverProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.getCurrentDriver()
            Resource.Success(response)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Resource.Error(buildErrorMessage("Server error (${e.code()}): ${e.message()}", errorBody))
        } catch (e: IOException) {
            Resource.Error("Network error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("Unexpected error: ${e.localizedMessage}")
        }
    }

    private fun buildErrorMessage(base: String, body: String?): String =
        if (body.isNullOrBlank()) base else "$base | Body: $body"
}
