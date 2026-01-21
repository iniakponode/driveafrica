package com.uoa.core.network.model.nominatim

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class OverpassResponse(
    @field:SerializedName("elements")
    val elements: List<Element>
) {
    @Keep
    data class Element(
        @field:SerializedName("id")
        val id: Long,
        @field:SerializedName("tags")
        val tags: Map<String, String>?,
        @field:SerializedName("lat")
        val lat: Double?,
        @field:SerializedName("lon")
        val lon: Double?
    )
}

