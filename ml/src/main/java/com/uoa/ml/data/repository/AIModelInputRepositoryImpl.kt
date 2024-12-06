package com.uoa.ml.data.repository

import android.hardware.Sensor
import android.util.Log
import android.util.TimeUtils
import com.uoa.core.database.daos.AIModelInputDao
import com.uoa.core.database.entities.AIModelInputsEntity
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.mlclassifier.OnnxModelRunner
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import com.uoa.core.mlclassifier.data.TripFeatures
import com.uoa.core.model.AIModelInputs
import com.uoa.core.model.LocationData
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toEntity
import com.uoa.ml.utils.IncrementalAccelerationYMean
import com.uoa.ml.utils.IncrementalCourseStd
import com.uoa.ml.utils.IncrementalDayOfWeekMean
import com.uoa.ml.utils.IncrementalHourOfDayMean
import com.uoa.ml.utils.IncrementalSpeedStd
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class AIModelInputRepositoryImpl @Inject constructor(
    private val aiModelInputDao: AIModelInputDao,
    private val incrementalHourOfDayMeanProvider: IncrementalHourOfDayMean,
    private val incrementalDayOfWeekMeanProvider: IncrementalDayOfWeekMean,
    private val incrementalSpeedStdProvider: IncrementalSpeedStd,
    private val incrementalAccelerationYMeanProvider: IncrementalAccelerationYMean,
    private val incrementalCourseStdProvider: IncrementalCourseStd,
    private val minMaxValuesLoader: MinMaxValuesLoader
) : AIModelInputRepository {

    var windowStartTime=System.currentTimeMillis()

    override suspend fun insertAiModelInput(aiModelInput: AIModelInputsEntity) {
        aiModelInputDao.insertAiModelInput(aiModelInput)
    }

    override suspend fun deleteAiModelInput() {
        aiModelInputDao.deleteAllAiModelInputs()
    }

    override suspend fun getAiModelInputs(): List<AIModelInputsEntity> {
        return aiModelInputDao.getAllAiModelInputs()
    }

    override suspend fun getAiModelInputById(id: Int): AIModelInputsEntity? {
        return aiModelInputDao.getAiModelInputById(id)
    }

    override suspend fun updateAiModelInput(aiModelInput: AIModelInputsEntity) {
        aiModelInputDao.updateAiModelInput(aiModelInput)
    }

    override suspend fun deleteAiModelInputById(id: Int) {
        aiModelInputDao.deleteAiModelInputById(id)
    }

    override suspend fun getAiModelInputInputByTripId(tripId: UUID): List<AIModelInputs> {
        return aiModelInputDao.getAiModelInputsByTripId(tripId)
    }


    override suspend fun processDataForAIModelInputs(sensorData: RawSensorData, location: LocationData, tripId:UUID) {
        if (sensorData.locationId != null) {
            // Update incremental calculators
            incrementalHourOfDayMeanProvider.addTimestamp(sensorData.timestamp)
            incrementalDayOfWeekMeanProvider.addTimestamp(sensorData.timestamp)
            incrementalSpeedStdProvider.addSpeed(location.speed!!.toFloat())

            if (sensorData.sensorType == Sensor.TYPE_ACCELEROMETER) {
                val accelerationY = sensorData.values.getOrNull(1) ?: return
                incrementalAccelerationYMeanProvider.addAccelerationY(accelerationY)
            }

            incrementalCourseStdProvider.addSensorData(sensorData.toEntity())

            // Check for window boundary (adjust window size as needed)
            val currentTime = System.currentTimeMillis()
            if (currentTime - windowStartTime >= 5 * 60 * 1000) {
                // Calculate and store trip features
                val tripFeatures = calculateTripFeatures(
                    incrementalHourOfDayMeanProvider,
                    incrementalDayOfWeekMeanProvider,
                    incrementalSpeedStdProvider,
                    incrementalCourseStdProvider,
                    incrementalAccelerationYMeanProvider,
                    minMaxValuesLoader
                )

                // Store trip features in the database
                val timestamp = Instant.now().toEpochMilli()
                val aiModelInputs= AIModelInputsEntity(
                    id= UUID.randomUUID(),
                    tripId= tripId,
                    timestamp=System.currentTimeMillis().toLong(),
                    startTimestamp=System.currentTimeMillis().toLong(),
                    endTimestamp=System.currentTimeMillis().toLong(),
                    date = Date(timestamp),
                    hourOfDayMean=tripFeatures.hourOfDayMean.toDouble(),
                    dayOfWeekMean=tripFeatures.dayOfWeekMean,
                    speedStd = tripFeatures.speedStd,
                    courseStd=tripFeatures.courseStd,
                    accelerationYOriginalMean=tripFeatures.accelerationYOriginalMean
                )
                aiModelInputDao.insertAiModelInput(aiModelInputs)

//                // Run inference and handle result
//                val inferenceResult = try {
//                    onnxModelRunner.runInference(tripFeatures)
//                } catch (e: Exception) {
//                    Log.e("Trip", "Error during model inference: ${e.message}", e)
//                    // Handle error, e.g., log, retry, notify user
//                }

                Log.d("Trip", "Extracted Features--:\n" +
                        "SpeedStd: ${tripFeatures.speedStd},\n" +
                        "CourseStd: ${tripFeatures.courseStd}\n" +
                        "accelerationYOriginalMean: ${tripFeatures.accelerationYOriginalMean}\n" +
                        "Hours of Day: ${tripFeatures.hourOfDayMean}\n" +
                        "Day of Week: ${tripFeatures.dayOfWeekMean}")
//                Log.i("Trip", "Inference result: $inferenceResult")

                // Reset incremental calculators for the next window
                incrementalHourOfDayMeanProvider.reset()
                incrementalDayOfWeekMeanProvider.reset()
                incrementalSpeedStdProvider.reset()
                incrementalAccelerationYMeanProvider.reset()
                incrementalCourseStdProvider.reset()

                windowStartTime = currentTime
            }
        }
    }

    private fun calculateTripFeatures(
        hourOfDayMeanCalc: IncrementalHourOfDayMean,
        dayOfWeekMeanCalc: IncrementalDayOfWeekMean,
        speedStdCalc: IncrementalSpeedStd,
        courseStdCalc: IncrementalCourseStd,
        accelerationYMeanCalc: IncrementalAccelerationYMean,
        minMaxValuesLoader: MinMaxValuesLoader
    ): TripFeatures {

        return TripFeatures(
            hourOfDayMean = hourOfDayMeanCalc.getNormalizedMean(minMaxValuesLoader),
            dayOfWeekMean = dayOfWeekMeanCalc.getNormalizedMean(minMaxValuesLoader),
            speedStd = speedStdCalc.getNormalizedStd(),
            courseStd = courseStdCalc.getNormalizedStd(),
            accelerationYOriginalMean = accelerationYMeanCalc.getNormalizedMean()
        )
    }

}