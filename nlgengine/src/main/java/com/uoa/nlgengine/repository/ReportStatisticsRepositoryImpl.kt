package com.uoa.nlgengine.repository

import com.uoa.core.database.daos.ReportStatisticsDao
import com.uoa.core.database.entities.ReportStatisticsEntity
import com.uoa.core.database.repository.ReportStatisticsRepository
import com.uoa.core.model.ReportStatistics
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

// Repository implementation
class ReportStatisticsRepositoryImpl(
    private val dao: ReportStatisticsDao
) : ReportStatisticsRepository {

    override suspend fun getReportsBetweenDates(
        startDate: LocalDate,
        endDate: LocalDate
    ): ReportStatistics? = withContext(Dispatchers.IO) {
        dao.getReportsBetweenDates(startDate, endDate)?.toDomainModel()
    }

    override fun getAllReports(): Flow<List<ReportStatistics>> {
        return dao.getAllReports().map { entityList ->
            entityList.map { it.toDomainModel() }
        }
    }

    override fun getReportById(id: UUID): ReportStatistics{
        return dao.getReportById(id).toDomainModel()
    }

    override suspend fun getReportByTripId(tripId: UUID): ReportStatisticsEntity {
        return dao.getReportByTripId(tripId)
    }

    override suspend fun insertReportStatistics(reportStatistics: ReportStatistics) {
        dao.insertReportStatistics(reportStatistics.toEntity())
    }

    override suspend fun updateReportStatistics(reportStatistics: ReportStatistics) {
        dao.updateReportStatistics(reportStatistics.toEntity())
    }

    override suspend fun deleteReportStatistics(reportStatistics: ReportStatistics) {
        dao.deleteReportStatistics(reportStatistics.toEntity())
    }

    override suspend fun getReportStatisticsBySyncStatus(status: Boolean): List<ReportStatisticsEntity> {
        return dao.getReportStatisticsBySyncStatus(status)
    }

    override suspend fun getReportStatisticsBySyncAndProcessedStatus(
        synced: Boolean,
        processed: Boolean
    ): List<ReportStatisticsEntity> {
        return dao.getReportStatisticsBySyncAndProcessedStatus(synced,processed)
    }

    override suspend fun deleteReportStatisticsByIds(ids: List<UUID>) {
        return dao.deleteReportStatisticsByIds(ids)
    }
}