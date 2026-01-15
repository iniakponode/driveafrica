package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.NLGReportEntity
import com.uoa.core.database.entities.ReportStatisticsEntity
import com.uoa.core.database.entities.SensorEntity
import java.time.LocalDateTime
import java.util.UUID

@Dao
interface NLGReportDao {
    @Insert
    suspend fun insertReport(nlgReportEntity: NLGReportEntity): Long

    @Update
    suspend fun updateReport(nlgReportEntity: NLGReportEntity)

    @Query("SELECT * FROM nlg_report")
    suspend fun getAllReports(): List<NLGReportEntity>

    @Query("SELECT * FROM nlg_report WHERE sync = :synced")
    suspend fun getNlgReportBySyncStatus(synced: Boolean): List<NLGReportEntity>

    @Query("SELECT * FROM nlg_report WHERE userId = :userId AND startDate == :startDate AND endDate == :endDate LIMIT 1")
    fun getReportsBetweenDates(userId: UUID, startDate: LocalDateTime, endDate: LocalDateTime): NLGReportEntity?

    @Query("UPDATE nlg_report SET sync=:synced WHERE id = :id")
    suspend fun updateUploadStatus(id:Int, synced: Boolean)

    @Query("SELECT * FROM nlg_report WHERE userId = :userId")
    suspend fun getReportsByUserId(userId: UUID): List<NLGReportEntity>

    @Query("SELECT * FROM nlg_report WHERE tripId = :tripId LIMIT 1")
    suspend fun getReportsByTripId(tripId: UUID): NLGReportEntity?

    @Query("DELETE FROM nlg_report WHERE id = :id")
    suspend fun deleteReportById(id: Int)
}
