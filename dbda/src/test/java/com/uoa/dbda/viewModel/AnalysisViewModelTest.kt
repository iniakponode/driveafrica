package com.uoa.dbda.viewModel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.uoa.core.behaviouranalysis.UnsafeBehaviorAnalyser
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.utils.toDomainModel
import com.uoa.dbda.domain.usecase.FetchRawSensorDataByDateUseCase
import com.uoa.dbda.domain.usecase.FetchRawSensorDataByTripIdUseCase
import com.uoa.dbda.domain.usecase.InsertUnsafeBehaviourUseCase
import com.uoa.dbda.presentation.viewModel.AnalysisViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Date
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @Mock
    private lateinit var fetchRawSensorDataByDateUseCase: FetchRawSensorDataByDateUseCase

    @Mock
    private lateinit var fetchRawSensorDataByTripIdUseCase: FetchRawSensorDataByTripIdUseCase

    @Mock
    private lateinit var insertUnsafeBehaviourUseCase: InsertUnsafeBehaviourUseCase

    @Mock
    private lateinit var unsafeBehaviorAnalyser: UnsafeBehaviorAnalyser

    private lateinit var viewModel: AnalysisViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)
        viewModel = AnalysisViewModel(
            fetchRawSensorDataByDateUseCase,
            fetchRawSensorDataByTripIdUseCase,
            insertUnsafeBehaviourUseCase,
            unsafeBehaviorAnalyser
        )
    }

    @AfterEach
    fun teardown(){
        Dispatchers.resetMain()
    }

    @Test
    fun testAnalyseUnsafeBehaviourByTrip() = runTest {
        val tripId = UUID.randomUUID()
        val rawData = listOf(RawSensorDataEntity(id = UUID.randomUUID(),
            sensorType = 1,
            sensorTypeName = "Accelerometer",
            values = listOf(1.0f, 2.0f, 3.0f),
            timestamp = 1234567890,
            date = Date(),
            accuracy = 1,
            locationId = UUID.randomUUID(),
            tripId,
            sync = false
        ))
        `when`(fetchRawSensorDataByTripIdUseCase.execute(tripId)).thenReturn(flowOf(rawData.map { it.toDomainModel() }))
        `when`(unsafeBehaviorAnalyser.analyse(rawData)).thenReturn(listOf())

        viewModel.analyseUnsafeBehaviourByTrip(tripId)

        verify(fetchRawSensorDataByTripIdUseCase).execute(tripId)
        verify(unsafeBehaviorAnalyser).analyse(rawData)
        verify(insertUnsafeBehaviourUseCase, never()).execute(any())
    }
}
