package com.uoa.sensor.domain.analyser


import android.os.Build
import androidx.annotation.RequiresApi
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.model.RawSensorData
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.toDomainModel
import java.util.UUID
import javax.inject.Inject
import kotlin.math.sqrt

class UnsafeBehaviorAnalyser @Inject constructor() {

    @RequiresApi(Build.VERSION_CODES.O)
    fun analyse(sensorDataList: List<RawSensorDataEntity>): List<UnsafeBehaviourModel> {
        val unsafeBehaviours = mutableListOf<UnsafeBehaviourModel>()

        // Analyze sensor data and detect unsafe behaviours
        // This is just an example. You can implement your actual analysis logic here.

        for (data in sensorDataList) {
            if (isHarshAcceleration(data.toDomainModel())) {
                unsafeBehaviours.add(
                    UnsafeBehaviourModel(
                        id = UUID.randomUUID(),
                        tripId = data.tripId!!,
                        behaviorType = "Harsh Acceleration",
                        timestamp = data.timestamp,
                        date = data.date!!,
                        cause = "",
                        severity = 1.0f,
                        locationId = data.locationId!!,
                    )
                )
            }

            if (isHarshBraking(data.toDomainModel())) {
                unsafeBehaviours.add(
                    UnsafeBehaviourModel(
                        id = UUID.randomUUID(),
                        tripId = data.tripId!!,
                        behaviorType = "Harsh Braking",
                        timestamp = data.timestamp,
                        date =data.date!!,
                        cause = "",
                        severity = 1.0f,
                        locationId = data.locationId!!,
                    )
                )
            }
        }

            return unsafeBehaviours
    }

    private fun isHarshAcceleration(data: RawSensorData): Boolean {
        // Replace with your actual harsh acceleration detection logic
        val threshold = 3.0 // example threshold for acceleration
        val accelerationMagnitude = sqrt(data.values[0] * data.values[0] +
                data.values[1] * data.values[1] +
                data.values[2] * data.values[2])
        return accelerationMagnitude > threshold
    }

    private fun isHarshBraking(data: RawSensorData): Boolean {
        val threshold = -3.0
        val accelerationMagnitude = sqrt(data.values[0] * data.values[0] +
                data.values[1] * data.values[1] +
                data.values[2] * data.values[2])
        return accelerationMagnitude < threshold
    }
}
