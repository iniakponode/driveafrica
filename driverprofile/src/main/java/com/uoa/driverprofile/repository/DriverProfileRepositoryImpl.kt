package com.uoa.driverprofile.repository

import com.uoa.core.database.daos.DriverProfileDAO
import com.uoa.core.model.DriverProfile
import javax.inject.Inject

class DriverProfileRepository @Inject constructor(
    private val driverProfileDAO: DriverProfileDAO
){
   suspend fun getDriverProfile(driverProfileId: String): DriverProfile
    {
        return driverProfileDAO.getDriverProfileById(driverProfileId).
    }
    fun getAllDriverProfiles(): List<DriverProfile>
    fun saveDriverProfile(driverProfile: DriverProfile)
    fun deleteDriverProfile(driverProfileId: String)
}