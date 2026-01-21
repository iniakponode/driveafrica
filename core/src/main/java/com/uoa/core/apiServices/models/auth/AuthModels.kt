package com.uoa.core.apiServices.models.auth

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Keep
data class RegisterRequest(
    @field:SerializedName("driverProfileId")
    val driverProfileId: String,
    @field:SerializedName("email")
    val email: String,
    @field:SerializedName("password")
    val password: String,
    @field:SerializedName("sync")
    val sync: Boolean = true
)

@Keep
data class LoginRequest(
    @field:SerializedName("email")
    val email: String,
    @field:SerializedName("password")
    val password: String
)

@Keep
data class AuthResponse(
    @field:SerializedName(value = "token", alternate = ["access_token"])
    val token: String,
    @field:SerializedName("token_type")
    val tokenType: String? = null,
    @field:SerializedName("user")
    val user: AuthUser? = null,
    @field:SerializedName("driver_profile")
    val driverProfile: AuthDriverProfile? = null,
    @field:SerializedName("driver_profile_id")
    val driverProfileId: String? = null,
    @field:SerializedName("fleet_assignment")
    val fleetAssignment: FleetAssignmentResponse? = null,
    @field:SerializedName("fleet_status")
    val fleetStatus: FleetStatusResponse? = null
)

@Keep
data class AuthUser(
    @field:SerializedName("id")
    val id: String,
    @field:SerializedName("email")
    val email: String,
    @field:SerializedName("role")
    val role: String?
)

@Keep
data class AuthDriverProfile(
    @field:SerializedName("id")
    val id: UUID,
    @field:SerializedName("email")
    val email: String,
    @field:SerializedName("name")
    val name: String?
)

@Keep
data class FleetAssignmentResponse(
    @field:SerializedName("fleet_id")
    val fleetId: String?,
    @field:SerializedName("fleet_name")
    val fleetName: String?,
    @field:SerializedName("vehicle_group_id")
    val vehicleGroupId: String?,
    @field:SerializedName("vehicle_group_name")
    val vehicleGroupName: String?,
    @field:SerializedName("assigned_at")
    val assignedAt: String?
)

@Keep
data class FleetStatusResponse(
    @field:SerializedName("status")
    val status: String,
    @field:SerializedName("fleet")
    val fleet: FleetInfo?,
    @field:SerializedName("vehicle_group")
    val vehicleGroup: VehicleGroupInfo?,
    @field:SerializedName("vehicle")
    val vehicle: VehicleInfo?,
    @field:SerializedName("pending_request")
    val pendingRequest: PendingFleetRequest?
)

@Keep
data class FleetInfo(
    @field:SerializedName("id")
    val id: String,
    @field:SerializedName("name")
    val name: String
)

@Keep
data class VehicleGroupInfo(
    @field:SerializedName("id")
    val id: String,
    @field:SerializedName("name")
    val name: String
)

@Keep
data class VehicleInfo(
    @field:SerializedName("id")
    val id: String,
    @field:SerializedName("license_plate")
    val licensePlate: String,
    @field:SerializedName("make")
    val make: String?,
    @field:SerializedName("model")
    val model: String?
)

@Keep
data class PendingFleetRequest(
    @field:SerializedName("id")
    val id: String,
    @field:SerializedName("fleet_name")
    val fleetName: String?,
    @field:SerializedName("requested_at")
    val requestedAt: String?
)

@Keep
data class JoinFleetRequest(
    @field:SerializedName("invite_code")
    val inviteCode: String
)

@Keep
data class InviteCodeValidationRequest(
    @field:SerializedName("code")
    val code: String
)

@Keep
data class JoinFleetResponse(
    @field:SerializedName("message")
    val message: String,
    @field:SerializedName("request_id")
    val requestId: String?,
    @field:SerializedName("fleet_name")
    val fleetName: String?,
    @field:SerializedName("status")
    val status: String
)
