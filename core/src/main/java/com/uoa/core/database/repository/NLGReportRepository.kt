package com.uoa.core.database.repository

import com.uoa.core.database.entities.NLGReportEntity
import java.time.LocalDate
import java.util.UUID

interface NLGReportRepository {
    suspend fun insertReport(nlgReportEntity: NLGReportEntity): Long
    suspend fun updateReport(nlgReportEntity: NLGReportEntity)
    suspend fun getAllReports(): List<NLGReportEntity>
    suspend fun getNlgReportBySyncStatus(synced: Boolean): List<NLGReportEntity>
    suspend fun getReportsBetweenDates(startDate: LocalDate, endDate: LocalDate): NLGReportEntity
    suspend fun getNlgReportsByTripId(tripId: UUID): NLGReportEntity
    suspend fun updateUploadStatus(id: Int, synced: Boolean)
    suspend fun getReportsByUserId(userId: UUID): List<NLGReportEntity>
    suspend fun deleteReportById(id: Int)
}
