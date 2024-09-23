package com.uoa.core.database.repository

import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.model.DriverProfile
import java.util.UUID

interface DriverProfileRepository {
    // This repository is used to interact with the driver profile entity

    // Insert a driver profile
    suspend fun insertDriverProfile(driverProfileEntity: DriverProfileEntity)

    // Update a driver profile
    suspend fun updateDriverProfile(driverProfileEntity: DriverProfileEntity)

    // Get all driver profiles
    suspend fun getAllDriverProfiles(): List<DriverProfile>

    // Get a driver profile by id
    suspend fun getDriverProfileById(driverProfileId: UUID): DriverProfile?

    // Delete all driver profiles
    suspend fun deleteAllDriverProfiles()

    // Get a driver profile by email
    suspend fun getDriverProfileByEmail(email: String): DriverProfile?

    suspend fun deleteDriverProfileByEmail(email: String)

    // Get a driver profile by sync status
    suspend fun getDriverProfileBySyncStatus(synced: Boolean): List<DriverProfile>
}