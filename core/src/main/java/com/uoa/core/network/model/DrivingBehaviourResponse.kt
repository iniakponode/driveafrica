package com.uoa.core.network.model

data class DrivingBehaviourResponse(
    val result: String,
    val status: String,
    val data: List<DrivingBehaviourData>
)
