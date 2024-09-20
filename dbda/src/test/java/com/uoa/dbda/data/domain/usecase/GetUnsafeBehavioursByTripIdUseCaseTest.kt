package com.uoa.dbda.data.domain.usecase

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.dbda.domain.usecase.GetUnsafeBehavioursByTripIdUseCase
import com.uoa.dbda.repository.UnsafeBehaviourRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.Date
import java.util.UUID

class GetUnsafeBehavioursByTripIdUseCaseTest {

    private lateinit var repository: UnsafeBehaviourRepositoryImpl
    private lateinit var useCase: GetUnsafeBehavioursByTripIdUseCase

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        repository = mock()
        useCase = GetUnsafeBehavioursByTripIdUseCase(repository)
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testExecute() = runTest {
        val tripId = UUID.randomUUID()
        val unsafeBehaviours = listOf(
            UnsafeBehaviourModel(UUID.randomUUID(), tripId, UUID.randomUUID(), "Speeding", 5.0f, System.currentTimeMillis(), Date(),  null, false),
            UnsafeBehaviourModel(UUID.randomUUID(), tripId, UUID.randomUUID(), "Harsh Braking", 3.0f, System.currentTimeMillis(), Date(), null, false)
        )
        whenever(repository.getUnsafeBehavioursByTripId(tripId)).thenReturn(flowOf(unsafeBehaviours))

        val result: Flow<List<UnsafeBehaviourModel>> = useCase.execute(tripId)
        result.collect { unsafeBehaviourList ->
            assertEquals(unsafeBehaviours.size, unsafeBehaviourList.size)
            assertEquals(unsafeBehaviours[0].behaviorType, unsafeBehaviourList[0].behaviorType)
        }
        verify(repository).getUnsafeBehavioursByTripId(tripId)
    }
}
