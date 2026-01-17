package com.uoa.core.apiServices.models.auth

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class RegisterRequest(
    val driverProfileId: String,
    val email: String,
    val password: String,
    val sync: Boolean = true
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    @SerializedName(value = "token", alternate = ["access_token"])
    val token: String,
    @SerializedName("token_type")
    val tokenType: String? = null,
    val user: AuthUser? = null,
    @SerializedName("driver_profile")
    val driverProfile: AuthDriverProfile? = null,
    @SerializedName("driver_profile_id")
    val driverProfileId: String? = null,
    @SerializedName("fleet_assignment")
    val fleetAssignment: FleetAssignmentResponse? = null,
    @SerializedName("fleet_status")
    val fleetStatus: FleetStatusResponse? = null
)

data class AuthUser(
    val id: String,
    val email: String,
    val role: String?
)

data class AuthDriverProfile(
    val id: UUID,
    val email: String,
    val name: String?
)

data class FleetAssignmentResponse(
    @SerializedName("fleet_id")
    val fleetId: String?,
    @SerializedName("fleet_name")
    val fleetName: String?,
    @SerializedName("vehicle_group_id")
    val vehicleGroupId: String?,
    @SerializedName("vehicle_group_name")
    val vehicleGroupName: String?,
    @SerializedName("assigned_at")
    val assignedAt: String?
)

data class FleetStatusResponse(
    val status: String,
    val fleet: FleetInfo?,
    @SerializedName("vehicle_group")
    val vehicleGroup: VehicleGroupInfo?,
    val vehicle: VehicleInfo?,
    @SerializedName("pending_request")
    val pendingRequest: PendingFleetRequest?
)

data class FleetInfo(
    val id: String,
    val name: String
)

data class VehicleGroupInfo(
    val id: String,
    val name: String
)

data class VehicleInfo(
    val id: String,
    @SerializedName("license_plate")
    val licensePlate: String,
    val make: String?,
    val model: String?
)

data class PendingFleetRequest(
    val id: String,
    @SerializedName("fleet_name")
    val fleetName: String?,
    @SerializedName("requested_at")
    val requestedAt: String?
)

data class JoinFleetRequest(
    @SerializedName("invite_code")
    val inviteCode: String
)

data class InviteCodeValidationRequest(
    @SerializedName("code")
    val code: String
)

data class JoinFleetResponse(
    val message: String,
    @SerializedName("request_id")
    val requestId: String?,
    @SerializedName("fleet_name")
    val fleetName: String?,
    val status: String
)
