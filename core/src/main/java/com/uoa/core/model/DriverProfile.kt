package com.uoa.core.model

import java.util.UUID

data class DriverProfile(
    val driverProfileId: UUID,
    val email: String,
    val sync: Boolean = false
)
