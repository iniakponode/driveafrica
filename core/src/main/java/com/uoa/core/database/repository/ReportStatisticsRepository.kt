package com.uoa.core.database.repository

import com.uoa.core.database.entities.AIModelInputsEntity
import com.uoa.core.database.entities.ReportStatisticsEntity
import com.uoa.core.model.LocationData
import com.uoa.core.model.RawSensorData
import com.uoa.core.model.ReportStatistics
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

// Repository interface
interface ReportStatisticsRepository {
    fun getReportsBetweenDates(startDate: LocalDate, endDate: LocalDate): ReportStatistics
    fun getAllReports(): Flow<List<ReportStatistics>>
    fun getReportById(id: UUID): Flow<ReportStatistics?>
    suspend fun getReportByTripId(tripId: UUID): ReportStatisticsEntity
    suspend fun insertReportStatistics(reportStatistics: ReportStatistics)
    suspend fun updateReportStatistics(reportStatistics: ReportStatistics)
    suspend fun deleteReportStatistics(reportStatistics: ReportStatistics)
    suspend fun deleteReportStatisticsByIds(ids: List<UUID>)
    suspend fun getReportStatisticsBySyncStatus(status: Boolean): List<ReportStatisticsEntity>
    suspend fun getReportStatisticsBySyncAndProcessedStatus(synced: Boolean, processed: Boolean): List<ReportStatisticsEntity>
}