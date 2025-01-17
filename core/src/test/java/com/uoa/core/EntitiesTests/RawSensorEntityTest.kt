package com.uoa.core.EntitiesTests

import android.content.Context
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.utils.PreferenceUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import java.time.Instant
import java.util.*
//import kotlin.test.assertEquals
//import kotlin.test.assertNotNull
//import kotlin.test.assertTrue

class RawSensorDataEntityTest {

    @Test
    fun `valid data should create a RawSensorDataEntity`() {
        val id = UUID.randomUUID()
        val sensorType = Math.random().toInt()
        val sensorTypeName = "Accelerometer"
        val values = listOf(1.2f, 3.4f, -0.5f)
        val timestamp = Instant.now().toEpochMilli()
        val date = Date(Instant.now().toEpochMilli())
        val accuracy = 3

        val drivingProfileId= UUID.randomUUID()
        val entity = RawSensorDataEntity(id, sensorType, sensorTypeName, values, timestamp, date, accuracy, drivingProfileId, null, null)

        assertEquals(id, entity.id)
        assertEquals(sensorType, entity.sensorType)
        assertEquals(sensorTypeName, entity.sensorTypeName)
        assertEquals(values, entity.values)
        assertEquals(timestamp, entity.timestamp)
        assertEquals(date, entity.date)
        assertEquals(accuracy, entity.accuracy)
        assertNull(entity.locationId)
        assertNull(entity.tripId)
    }
}
