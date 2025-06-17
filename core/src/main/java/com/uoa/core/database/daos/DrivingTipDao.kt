package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.DrivingTipEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID
@Dao
interface DrivingTipDao {
    // This DAO is used to interact with the driving tip entity

    // Insert a driving tip
    @Insert
    fun insertDrivingTip(drivingTipEntity: DrivingTipEntity)

    // Update a driving tip
    @Update
    fun updateDrivingTip(drivingTipEntity: DrivingTipEntity)

    // Get all driving tips
    @Query("SELECT * FROM driving_tips")
    fun getAllDrivingTips(): List<DrivingTipEntity>

    // Get a driving tip by id
    @Query("SELECT * FROM driving_tips WHERE tipId = :drivingTipId")
    fun getDrivingTipById(drivingTipId: UUID): DrivingTipEntity

//    Update Driving Tip
    @Query("UPDATE driving_tips SET date = :date WHERE tipId = :drivingTipId")
    fun updateDrivingTip(drivingTipId: UUID, date: LocalDate)

//    Update Driving Tips
    @Update
    fun updateDrivingTips(drivingTipEntity: List<DrivingTipEntity>)



    // Delete all driving tips
    @Query("DELETE FROM driving_tips")
    fun deleteAllDrivingTips()

    // Get a driving tip by profile id
    @Query("SELECT * FROM driving_tips WHERE driverProfileId = :profileId")
    fun getDrivingTipByProfileId(profileId: UUID): List<DrivingTipEntity>

    // Get a driving tip by sync status
    @Query("SELECT * FROM driving_tips WHERE sync = :synced")
    fun getDrivingTipBySyncStatus(synced: Boolean): List<DrivingTipEntity>

    // Get a driving tip by date
    @Query("SELECT * FROM driving_tips WHERE date = :date")
    fun getDrivingTipByDate(date: LocalDate): List<DrivingTipEntity>
}