package com.uoa.core.network.model.chatGPT

import android.location.Address

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

data class OSMResponse(
    val address: Address
)

data class Address(
    val road: String?
)
