package com.uoa.core.network.model.nominatim

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json

@Keep
data class ReverseGeocodeResponse(
    @Json(name = "place_id")
    @field:SerializedName("place_id")
    val placeId: Long?,
    @Json(name = "display_name")
    @field:SerializedName("display_name")
    val displayName: String?,
    @field:SerializedName("address")
    val address: Address?
) {
    @Keep
    data class Address(
        @field:SerializedName("road")
        val road: String?,
        @field:SerializedName("city")
        val city: String?,
        @field:SerializedName("county")
        val county: String?,
        @field:SerializedName("state")
        val state: String?,
        @field:SerializedName("country")
        val country: String?,
        @Json(name = "country_code")
        @field:SerializedName("country_code")
        val countryCode: String?
    )
}
