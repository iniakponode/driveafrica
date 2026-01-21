package com.uoa.core.network.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class GeminiData(
//    val id: String,
    @field:SerializedName("value")
    val value: String
    // Add other fields as per the actual response
)
