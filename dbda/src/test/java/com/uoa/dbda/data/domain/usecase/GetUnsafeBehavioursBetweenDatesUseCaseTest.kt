package com.uoa.dbda.data.domain.usecase

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.dbda.domain.usecase.GetUnsafeBehavioursBetweenDatesUseCase
import com.uoa.dbda.repository.UnsafeBehaviourRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.time.Instant
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class GetUnsafeBehavioursBetweenDatesUseCaseTest {

    private lateinit var repository: UnsafeBehaviourRepositoryImpl
    private lateinit var useCase: GetUnsafeBehavioursBetweenDatesUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = GetUnsafeBehavioursBetweenDatesUseCase(repository)
    }

    @Test
    fun testExecute() = runTest {
        val startDate = Date()
        val endDate = Date()
        val zoneId = ZoneId.systemDefault()
        val sDate = Instant.ofEpochMilli(startDate.toInstant().toEpochMilli()).atZone(zoneId).toLocalDate()
        val eDate = Instant.ofEpochMilli(endDate.toInstant().toEpochMilli()).atZone(zoneId).toLocalDate()

        val expected = listOf(
            UnsafeBehaviourModel(
                id = UUID.randomUUID(),
                tripId = UUID.randomUUID(),
                locationId = UUID.randomUUID(),
                behaviorType = "Speeding",
                severity = 5.0f,
                timestamp = System.currentTimeMillis(),
                date = Date(),
                updatedAt = null,
                updated = false,
                synced = false,
            )
        )

        whenever(repository.getUnsafeBehavioursBetweenDates(sDate, eDate)).thenReturn(flowOf(expected))

        val resultFlow = useCase.execute(startDate.toInstant().toEpochMilli(), endDate.toInstant().toEpochMilli())
        val result = resultFlow.first() // Collect the first emitted value from the flow

        verify(repository).getUnsafeBehavioursBetweenDates(sDate, eDate)
        assertEquals(expected, result)
    }
}
