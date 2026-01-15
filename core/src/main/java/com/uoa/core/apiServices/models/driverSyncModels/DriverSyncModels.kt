package com.uoa.core.apiServices.models.driverSyncModels

import com.google.gson.annotations.SerializedName
import com.uoa.core.apiServices.models.rawSensorModels.RawSensorDataCreate
import com.uoa.core.apiServices.models.unsafeBehaviourModels.UnsafeBehaviourCreate
import java.util.UUID

data class DriverProfileReference(
    val driverProfileId: UUID?,
    val email: String,
    val displayName: String? = null
)

data class DriverSyncTripPayload(
    val id: UUID,
    val driverProfileId: UUID? = null,
    @SerializedName("start_time")
    val startTime: Long,
    @SerializedName("end_time")
    val endTime: Long? = null,
    @SerializedName("start_date")
    val startDate: String? = null,
    @SerializedName("end_date")
    val endDate: String? = null,
    val influence: String? = null,
    val state: String? = null,
    val distance: Double? = null,
    val averageSpeed: Double? = null,
    val notes: String? = null,
    val sync: Boolean? = true
)

data class DriverSyncAlcoholResponse(
    val id: UUID? = null,
    val driverProfileId: UUID? = null,
    val drankAlcohol: String,
    val submittedAt: String? = null,
    @SerializedName("trip_id")
    val tripId: UUID? = null,
    val notes: String? = null
)

data class DriverSyncPayload(
    val profile: DriverProfileReference,
    val trips: List<DriverSyncTripPayload> = emptyList(),
    val rawSensorData: List<RawSensorDataCreate> = emptyList(),
    val unsafeBehaviours: List<UnsafeBehaviourCreate> = emptyList(),
    val alcoholResponses: List<DriverSyncAlcoholResponse> = emptyList()
)

data class DriverSyncResponse(
    val tripCount: Int,
    val rawSensorCount: Int,
    val unsafeBehaviourCount: Int,
    val alcoholResponseCount: Int,
    val driverProfileId: UUID
)
