package com.uoa.core.database.repository

import com.uoa.core.database.entities.NLGReportEntity
import java.time.LocalDateTime
import java.util.UUID

interface NLGReportRepository {
    suspend fun insertReport(nlgReportEntity: NLGReportEntity): Long
    suspend fun updateReport(nlgReportEntity: NLGReportEntity)
    suspend fun getAllReports(): List<NLGReportEntity>
    suspend fun getNlgReportBySyncStatus(synced: Boolean): List<NLGReportEntity>
    suspend fun getReportsBetweenDates(userId: UUID, startDate: LocalDateTime, endDate: LocalDateTime): NLGReportEntity?
    suspend fun getNlgReportsByTripId(tripId: UUID): NLGReportEntity?
    suspend fun updateUploadStatus(id: Int, synced: Boolean)
    suspend fun getReportsByUserId(userId: UUID): List<NLGReportEntity>
    suspend fun deleteReportById(id: Int)
}
