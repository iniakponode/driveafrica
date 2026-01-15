package com.uoa.sensor.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.drivingStateDataStore by preferencesDataStore(name = "driving_state")

data class DrivingStateSnapshot(
    val drivingState: String?,
    val drivingVariance: Double?,
    val drivingSpeedMps: Double?,
    val drivingAccuracy: Float?,
    val drivingLastUpdate: Long?,
    val collectionStatus: Boolean?,
    val tripStartStatus: Boolean?,
    val currentTripId: String?,
    val tripStartTime: Long?
)

@Singleton
class DrivingStateStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore = context.drivingStateDataStore

    private object Keys {
        val DRIVING_STATE = stringPreferencesKey("driving_state")
        val DRIVING_VARIANCE = doublePreferencesKey("driving_variance")
        val DRIVING_SPEED_MPS = doublePreferencesKey("driving_speed_mps")
        val DRIVING_ACCURACY = floatPreferencesKey("driving_accuracy")
        val DRIVING_LAST_UPDATE = longPreferencesKey("driving_last_update")
        val COLLECTION_STATUS = booleanPreferencesKey("collection_status")
        val TRIP_START_STATUS = booleanPreferencesKey("trip_start_status")
        val CURRENT_TRIP_ID = stringPreferencesKey("current_trip_id")
        val TRIP_START_TIME = longPreferencesKey("trip_start_time")
    }

    val snapshotFlow: Flow<DrivingStateSnapshot> = dataStore.data.map { prefs ->
        DrivingStateSnapshot(
            drivingState = prefs[Keys.DRIVING_STATE],
            drivingVariance = prefs[Keys.DRIVING_VARIANCE],
            drivingSpeedMps = prefs[Keys.DRIVING_SPEED_MPS],
            drivingAccuracy = prefs[Keys.DRIVING_ACCURACY],
            drivingLastUpdate = prefs[Keys.DRIVING_LAST_UPDATE],
            collectionStatus = prefs[Keys.COLLECTION_STATUS],
            tripStartStatus = prefs[Keys.TRIP_START_STATUS],
            currentTripId = prefs[Keys.CURRENT_TRIP_ID],
            tripStartTime = prefs[Keys.TRIP_START_TIME]
        )
    }

    suspend fun updateDrivingState(state: String) {
        dataStore.edit { prefs ->
            prefs[Keys.DRIVING_STATE] = state
        }
    }

    suspend fun updateDrivingMetrics(
        variance: Double,
        speedMps: Double,
        accuracy: Float,
        lastUpdate: Long
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.DRIVING_VARIANCE] = variance
            prefs[Keys.DRIVING_SPEED_MPS] = speedMps
            prefs[Keys.DRIVING_ACCURACY] = accuracy
            prefs[Keys.DRIVING_LAST_UPDATE] = lastUpdate
        }
    }

    suspend fun updateCollectionStatus(collectionStatus: Boolean, tripStartTime: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.COLLECTION_STATUS] = collectionStatus
            prefs[Keys.TRIP_START_TIME] = tripStartTime
        }
    }

    suspend fun updateTripStartStatus(tripStartStatus: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.TRIP_START_STATUS] = tripStartStatus
        }
    }

    suspend fun updateCurrentTripId(tripId: UUID?) {
        dataStore.edit { prefs ->
            if (tripId == null) {
                prefs.remove(Keys.CURRENT_TRIP_ID)
            } else {
                prefs[Keys.CURRENT_TRIP_ID] = tripId.toString()
            }
        }
    }
}
