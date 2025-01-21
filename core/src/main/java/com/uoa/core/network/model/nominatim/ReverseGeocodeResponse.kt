package com.uoa.core.network.model.nominatim

import com.squareup.moshi.Json

data class ReverseGeocodeResponse(
    @Json(name = "place_id") val placeId: Long?,
    @Json(name = "display_name") val displayName: String?,
    val address: Address?
) {
    data class Address(
        val road: String?,
        val city: String?,
        val county: String?,
        val state: String?,
        val country: String?,
        @Json(name = "country_code") val countryCode: String?
    )
}
