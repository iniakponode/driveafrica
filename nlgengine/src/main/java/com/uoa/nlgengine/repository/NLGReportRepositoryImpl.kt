package com.uoa.nlgengine.repository

import com.uoa.core.database.daos.NLGReportDao
import com.uoa.core.database.entities.NLGReportEntity
import com.uoa.core.database.entities.TripEntity
import com.uoa.core.database.repository.NLGReportRepository
import java.time.LocalDate
import java.util.UUID

class NLGReportRepositoryImpl(private val dao: NLGReportDao) : NLGReportRepository {
    override suspend fun insertReport(nlgReportEntity: NLGReportEntity): Long {
        return dao.insertReport(nlgReportEntity)
    }

    override suspend fun updateReport(nlgReportEntity: NLGReportEntity) {
        dao.updateReport(nlgReportEntity)
    }

    override suspend fun getAllReports(): List<NLGReportEntity> {
        return dao.getAllReports()
    }

    override suspend fun getNlgReportBySyncStatus(synced: Boolean): List<NLGReportEntity> {
        return dao.getNlgReportBySyncStatus(synced)
    }

    override suspend fun getReportsBetweenDates(
        startDate: LocalDate,
        endDate: LocalDate
    ): NLGReportEntity {
        return dao.getReportsBetweenDates(startDate, endDate)
    }

    override suspend fun updateUploadStatus(id: Int, synced: Boolean) {
        dao.updateUploadStatus(id, synced)
    }

    override suspend fun getReportsByUserId(userId: UUID): List<NLGReportEntity> {
        return dao.getReportsByUserId(userId)
    }

    override suspend fun getNlgReportsByTripId(tripId: UUID): NLGReportEntity {
        return dao.getReportsByTripId(tripId)
    }

    override suspend fun deleteReportById(id: Int) {
        dao.deleteReportById(id)
    }
}
