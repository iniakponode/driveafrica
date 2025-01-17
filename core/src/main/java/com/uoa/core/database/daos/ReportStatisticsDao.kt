package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.database.entities.ReportStatisticsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

// DAO
@Dao
interface ReportStatisticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReportStatistics(reportStatistics: ReportStatisticsEntity)

    @Update
    suspend fun updateReportStatistics(reportStatistics: ReportStatisticsEntity)

    @Delete
    suspend fun deleteReportStatistics(reportStatistics: ReportStatisticsEntity)

    @Query("SELECT * FROM report_statistics WHERE startDate == :startDate AND endDate == :endDate")
    fun getReportsBetweenDates(startDate: LocalDate, endDate: LocalDate): ReportStatisticsEntity

    @Query("SELECT * FROM report_statistics")
    fun getAllReports(): Flow<List<ReportStatisticsEntity>>

    @Query("SELECT * FROM report_statistics WHERE id = :id LIMIT 1")
    fun getReportById(id: UUID): Flow<ReportStatisticsEntity?>

    @Query("SELECT * FROM report_statistics WHERE tripId = :tripId LIMIT 1")
    fun getReportByTripId(tripId: UUID): ReportStatisticsEntity

    @Query("SELECT * FROM report_statistics WHERE sync= :synced AND processed= :processed")
    suspend fun getReportStatisticsBySyncAndProcessedStatus(synced: Boolean, processed: Boolean): List<ReportStatisticsEntity>

    @Query("SELECT * FROM report_statistics WHERE sync = :synced")
    suspend fun getReportStatisticsBySyncStatus(synced: Boolean): List<ReportStatisticsEntity>

    @Query("DELETE FROM report_statistics WHERE id IN (:ids)")
    suspend fun deleteReportStatisticsByIds(ids: List<UUID>)
}