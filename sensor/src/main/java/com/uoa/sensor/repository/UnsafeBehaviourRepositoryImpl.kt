package com.uoa.sensor.repository

//@Singleton
//class UnsafeBehaviourRepository @Inject constructor(
//    private val unsafeBehaviourDao: UnsafeBehaviourDao
//) {
//
//    suspend fun insertUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourModel) {
//        unsafeBehaviourDao.insertUnsafeBehaviour(unsafeBehaviour.toEntity())
//    }
//
//    suspend fun insertUnsafeBehaviourBatch(unsafeBehaviours: List<UnsafeBehaviourModel>) {
//        withContext(Dispatchers.IO){
//            unsafeBehaviourDao.insertUnsafeBehaviourBatch(unsafeBehaviours.map { it.toEntity() })
//        }
//    }
//
//    suspend fun updateUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourModel) {
//        unsafeBehaviourDao.updateUnsafeBehaviour(unsafeBehaviour.toEntity())
//    }
//
//    suspend fun deleteUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourModel) {
//        unsafeBehaviourDao.deleteUnsafeBehaviour(unsafeBehaviour.toEntity())
//    }
//
//    suspend fun getUnsafeBehavioursByTripId(tripId: UUID): Flow<List<UnsafeBehaviourModel>> {
//        return withContext(Dispatchers.IO){
//            unsafeBehaviourDao.getUnsafeBehavioursByTripId(tripId).map { unsafeBehaviourEntityList ->
//                unsafeBehaviourEntityList.map {
//                    it.toDomainModel()
//                }
//            }
//        }
//
//    }
//
//    suspend fun getUnsafeBehaviourById(id: UUID): UnsafeBehaviourEntity? {
//        return unsafeBehaviourDao.getUnsafeBehaviourById(id)
//    }
//
//    suspend fun getUnsafeBehavioursBySyncStatus(synced: Boolean): List<UnsafeBehaviourEntity> {
//        return unsafeBehaviourDao.getUnsafeBehavioursBySyncStatus(synced)
//    }
//
//    suspend fun deleteAllUnsafeBehavioursBySyncStatus(synced: Boolean) {
//        unsafeBehaviourDao.deleteAllUnsafeBehavioursBySyncStatus(synced)
//    }
//
//    suspend fun deleteAllUnsafeBehaviours() {
//        unsafeBehaviourDao.deleteAllUnsafeBehaviours()
//    }
//
//    suspend fun getUnsafeBehaviourCountByTypeAndTime(behaviorType: String, startTime: Long, endTime: Long): Int {
//        return unsafeBehaviourDao.getUnsafeBehaviourCountByTypeAndTime(behaviorType, startTime, endTime)
//    }
//
//    suspend fun getUnsafeBehaviourCountByTypeAndDistance(behaviourType: String, tripId: UUID, totalDistance: Float): Int {
//        return unsafeBehaviourDao.getUnsafeBehaviourCountByTypeAndDistance(behaviourType, tripId, totalDistance)
//    }
//
//    suspend fun getUnsafeBehavioursBetweenDates(startDate: Date, endDate: Date): Flow<List<UnsafeBehaviourModel>> {
//        return withContext(Dispatchers.IO){
//            unsafeBehaviourDao.getUnsafeBehavioursBetweenDates(startDate, endDate).map { unsafeBehaviourEntityList ->
//                unsafeBehaviourEntityList.map {
//                    it.toDomainModel()
//                }
//            }
//        }
//    }
//}

