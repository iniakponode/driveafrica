package com.uoa.driverprofile.domain.usecase

import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.database.repository.DriverProfileRepository
import com.uoa.core.database.repository.DrivingTipRepository
import com.uoa.core.model.DrivingTip
import com.uoa.core.utils.toDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

// write the use cases driver profile module here
class InsertDriverProfileUseCase @Inject constructor(
    private val driverProfileRepository: DriverProfileRepository
){
    suspend fun execute(driverProfileEntity: DriverProfileEntity) {
        // insert driver profile
        withContext(Dispatchers.IO) {
            driverProfileRepository.insertDriverProfile(driverProfileEntity)
        }
    }
}

//class UpdateDriverProfileUseCase @Inject constructor(
//    private val driverProfileRepository: DriverProfileRepository
//){
//    suspend fun execute(driverProfileEntity: DriverProfileEntity) {
//        // update driver profile
//        withContext(Dispatchers.IO) {
//            driverProfileRepository.updateDriverProfile(driverProfileEntity)
//        }
//    }
//}

//class GetAllDriverProfilesUseCase @Inject constructor(
//    private val driverProfileRepository: DriverProfileRepository
//){
//    suspend fun execute() {
//        // get all driver profiles
//        withContext(Dispatchers.IO) {
//            driverProfileRepository.getAllDriverProfiles()
//        }
//    }
//}

//class GetDriverProfileByIdUseCase @Inject constructor(
//    private val driverProfileRepository: DriverProfileRepository
//){
//    suspend fun execute(driverProfileId: UUID) {
//        // get driver profile by id
//        withContext(Dispatchers.IO) {
//            driverProfileRepository.getDriverProfileById(driverProfileId)
//        }
//    }
//}

class GetDriverProfileByEmailUseCase @Inject constructor(
    private val driverProfileRepository: DriverProfileRepository
){
    suspend fun execute(email: String) {
        // get driver profile by email
        withContext(Dispatchers.IO) {
            driverProfileRepository.getDriverProfileByEmail(email)
        }
    }
}

//class GetDriverProfileBySyncStatusUseCase @Inject constructor(
//    private val driverProfileRepository: DriverProfileRepository
//){
//    suspend fun execute(synced: Boolean) {
//        // get driver profile by sync status
//        withContext(Dispatchers.IO) {
//            driverProfileRepository.getDriverProfileBySyncStatus(synced)
//        }
//    }
//}

class DeleteDriverProfileByEmailUseCase @Inject constructor(
    private val driverProfileRepository: DriverProfileRepository
){
    suspend fun execute(email: String) {
        // delete driver profile by email
        withContext(Dispatchers.IO) {
            driverProfileRepository.deleteDriverProfileByEmail(email)
        }
    }
}

//class DeleteAllDriverProfilesUseCase @Inject constructor(
//    private val driverProfileRepository: DriverProfileRepository
//){
//    suspend fun execute() {
//        // delete all driver profiles
//        withContext(Dispatchers.IO) {
//            driverProfileRepository.deleteAllDriverProfiles()
//        }
//    }
//}

class GetDrivingTipByProfileIdUseCase @Inject constructor(
    private val drivingTipRepository: DrivingTipRepository
){
    suspend fun execute(profileId: UUID):Flow<List<DrivingTip>>{
        // get driving tips by profile id
        val drivingTip= withContext(Dispatchers.IO) {
            drivingTipRepository.fetchDrivingTipsByProfileId(profileId).map{
                it.map { drivingTipEntity ->
                    drivingTipEntity.toDomainModel()
                }
            }
        }
        return drivingTip
    }
}

class GetDrivingTipByIdUseCase @Inject constructor(
    private val drivingTipRepository: DrivingTipRepository
){
    suspend fun execute(drivingTipId: UUID):DrivingTip{
        // get driving tip by id
        val drivingTip= withContext(Dispatchers.IO) {
            drivingTipRepository.fetchDrivingTipById(drivingTipId).toDomainModel()
        }
        return drivingTip
    }
}

