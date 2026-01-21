package com.uoa.core.network.model.chatGPT

import android.location.Address
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

//data class OSMResponse(
//    val attribution: String,
//    val licence: String,
//    val osm_type: String,
//    val osm_id: String,
//    val lat: String,
//    val lon: String,
//    val display_name: String,
//    val address: Address,
//    val boundingbox: List<String>
//)

@Keep
data class OSMResponse(
    @field:SerializedName("address")
    val address: Address
)

@Keep
data class Address(
    @field:SerializedName("road")
    val road: String?
)
