package com.uoa.core.apiServices.models

import com.google.gson.annotations.SerializedName

data class ApiErrorResponse(
    val code: String?,
    @SerializedName("message")
    val message: String?
)
