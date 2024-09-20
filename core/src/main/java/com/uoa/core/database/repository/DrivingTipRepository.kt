package com.uoa.core.database.repository

import androidx.lifecycle.LiveData
import com.uoa.core.database.entities.DrivingTipEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

interface DrivingTipRepository {
    suspend fun fetchDrivingTipsByProfileId(profileId: UUID): Flow<List<DrivingTipEntity>>
    suspend fun fetchDrivingTipById(tipId: UUID): DrivingTipEntity
    suspend fun insertDrivingTip(drivingTipEntity: DrivingTipEntity)
    suspend fun fetchDrivingTipsByDate(date: LocalDate): List<DrivingTipEntity>
}