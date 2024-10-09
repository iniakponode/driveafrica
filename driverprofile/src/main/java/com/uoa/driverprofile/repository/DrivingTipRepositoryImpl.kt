package com.uoa.driverprofile.repository

import com.uoa.core.database.entities.DrivingTipEntity
import javax.inject.Inject
import com.uoa.core.database.daos.DrivingTipDao
import com.uoa.core.database.repository.DrivingTipRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

class DrivingTipRepositoryImpl @Inject constructor(
    private val tipsDAO: DrivingTipDao
): DrivingTipRepository {
    override suspend fun fetchDrivingTipsByProfileId(profileId: UUID): List<DrivingTipEntity> {
        return tipsDAO.getDrivingTipByProfileId(profileId)
    }
    override suspend fun fetchDrivingTipById(tipId: UUID): DrivingTipEntity {
        return tipsDAO.getDrivingTipById(tipId)
    }
    override suspend fun insertDrivingTip(drivingTipEntity: DrivingTipEntity) {
        tipsDAO.insertDrivingTip(drivingTipEntity)
    }
    override suspend fun fetchDrivingTipsByDate(date: LocalDate): List<DrivingTipEntity> {
        return tipsDAO.getDrivingTipByDate(date)
    }
    override suspend fun updateDrivingTip(drivingTipEntity: DrivingTipEntity) {
        tipsDAO.updateDrivingTip(drivingTipEntity)
    }
}