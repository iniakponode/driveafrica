package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.DriverProfileEntity
import java.util.UUID

@Dao
interface DriverProfileDAO {
    // This DAO is used to interact with the driver profile entity

    // Insert a driver profile
    @Insert
    fun insertDriverProfile(driverProfileEntity: DriverProfileEntity)

    // Update a driver profile
    @Update
    fun updateDriverProfile(driverProfileEntity: DriverProfileEntity)

    // Custom update query: update the driver profile where the email matches.
    @Query("UPDATE driver_profile SET driverProfileId = :driverProfileId, sync = :sync WHERE email = :email")
    fun updateDriverProfileByEmail(driverProfileId: UUID, sync: Boolean, email: String)

    // Get all driver profiles
    @Query("SELECT * FROM driver_profile")
    fun getAllDriverProfiles(): List<DriverProfileEntity>

    // Get a driver profile by id
    @Query("SELECT * FROM driver_profile WHERE driverProfileId = :driverProfileId")
    fun getDriverProfileById(driverProfileId: UUID): DriverProfileEntity?

    // Delete all driver profiles
    @Query("DELETE FROM driver_profile")
    fun deleteAllDriverProfiles()

    // Get a driver profile by email
    @Query("SELECT * FROM driver_profile WHERE email = :email")
    fun getDriverProfileByEmail(email: String): DriverProfileEntity?

    @Query("DELETE FROM driver_profile WHERE email = :email")
    fun deleteDriverProfileByEmail(email: String)

    // Get a driver profile by sync status
    @Query("SELECT * FROM driver_profile WHERE sync = :synced")
    fun getDriverProfileBySyncStatus(synced: Boolean): List<DriverProfileEntity>




}