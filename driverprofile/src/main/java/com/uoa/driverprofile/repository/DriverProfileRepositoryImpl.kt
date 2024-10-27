package com.uoa.driverprofile.repository

import com.uoa.core.database.daos.DriverProfileDAO
import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.database.repository.DriverProfileRepository
import com.uoa.core.model.DriverProfile
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import java.util.UUID
import javax.inject.Inject

class DriverProfileRepositoryImpl @Inject constructor(
    private val driverProfileDAO: DriverProfileDAO
): DriverProfileRepository {
    suspend fun getDriverProfile(driverProfileId: UUID): DriverProfile {
        return driverProfileDAO.getDriverProfileById(driverProfileId)?.toDomainModel()!!
    }

    override suspend fun insertDriverProfile(driverProfileEntity: DriverProfileEntity) {
        return driverProfileDAO.insertDriverProfile(driverProfileEntity)
    }

    override suspend fun updateDriverProfile(driverProfileEntity: DriverProfileEntity) {
        return driverProfileDAO.updateDriverProfile(driverProfileEntity)
    }

    override suspend fun getAllDriverProfiles(): List<DriverProfile> {
        return driverProfileDAO.getAllDriverProfiles().map { it.toDomainModel() }
    }

    override suspend fun getDriverProfileById(driverProfileId: UUID): DriverProfile? {
        return driverProfileDAO.getDriverProfileById(driverProfileId)?.toDomainModel()
    }

    override suspend fun getDriverProfileByEmail(email: String): DriverProfile {
        return driverProfileDAO.getDriverProfileByEmail(email)?.toDomainModel()!!
    }

    override suspend fun getDriverProfileBySyncStatus(synced: Boolean): List<DriverProfile> {
        return driverProfileDAO.getDriverProfileBySyncStatus(synced).map { it.toDomainModel() }
    }


    override suspend fun deleteDriverProfileByEmail(email: String) {
        driverProfileDAO.deleteDriverProfileByEmail(email)
    }

    override suspend fun deleteAllDriverProfiles() {
        driverProfileDAO.deleteAllDriverProfiles()
    }

}