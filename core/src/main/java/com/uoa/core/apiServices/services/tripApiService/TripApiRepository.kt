//package com.uoa.core.apiServices.services.tripApiService
//
//import com.uoa.core.apiServices.models.tripModels.TripCreate
//import com.uoa.core.apiServices.models.tripModels.TripResponse
//import com.uoa.core.utils.Resource
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import retrofit2.Call
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class TripApiServiceRepository @Inject constructor(
//    private val tripApiService: TripApiService
//) {
//
//    // Create a new trip
//    suspend fun createTrip(tripCreate: TripCreate): Resource<TripResponse> =withContext(Dispatchers.IO) {
//            try {
//                val tripResponse = tripApiService.createTrip(tripCreate)
//                Resource.Success(tripResponse)
//            } catch (e: Exception) {
//                Resource.Error("Error creating trip: ${e.localizedMessage}")
//            }
//        }
//
//    suspend fun getTrip(tripId: String): Resource<TripResponse> {
//        return try {
//            val tripResponse = tripApiService.getTrip(tripId)
//            Resource.Success(tripResponse)
//        } catch (e: Exception) {
//            Resource.Error("Error fetching trip: ${e.localizedMessage}")
//        }
//    }
//
//    // Retrieve all trips with optional pagination
//    suspend fun getAllTrips(skip: Int = 0, limit: Int = 20): Resource<List<TripResponse>> = withContext(Dispatchers.IO) {
//        try {
//            val tripList = tripApiService.getAllTrips(skip, limit)
//            Resource.Success(tripList)
//        } catch (e: Exception) {
//            Resource.Error(message = "Error fetching trips: ${e.localizedMessage}")
//        }
//    }
//
//    // Update a trip
//    suspend fun updateTrip(tripId: String, tripCreate: TripCreate): Resource<TripResponse> = withContext(Dispatchers.IO) {
//        try {
//            val tripResponse = tripApiService.updateTrip(tripId, tripCreate)
//            Resource.Success(tripResponse)
//        } catch (e: Exception) {
//            Resource.Error(message = "Error updating trip: ${e.localizedMessage}")
//        }
//    }
//
//    // Delete a trip
//    suspend fun deleteTrip(tripId: String): Resource<Unit> = withContext(Dispatchers.IO) {
//        try {
//            tripApiService.deleteTrip(tripId)
//            Resource.Success(Unit)
//        } catch (e: Exception) {
//            Resource.Error(message = "Error deleting trip: ${e.localizedMessage}")
//        }
//    }
//
//    // Batch create trips
//    suspend fun batchCreateTrips(trips: List<TripCreate>): Resource<Unit> = withContext(Dispatchers.IO) {
//        try {
//            tripApiService.batchCreateTrips(trips)
//            Resource.Success(Unit)
//        } catch (e: Exception) {
//            Resource.Error(message = "Error batch creating trips: ${e.localizedMessage}")
//        }
//    }
//
//    // Batch delete trips
//    suspend fun batchDeleteTrips(ids: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
//        try {
//            tripApiService.batchDeleteTrips(ids)
//            Resource.Success(Unit)
//        } catch (e: Exception) {
//            Resource.Error(message = "Error batch deleting trips: ${e.localizedMessage}")
//        }
//    }
//}