package com.uoa.dbda.data.domain.usecase


import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.toEntity
import com.uoa.dbda.domain.usecase.GetUnsafeBehavioursBySyncStatusUseCase
import com.uoa.dbda.repository.UnsafeBehaviourRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class GetUnsafeBehavioursBySyncStatusUseCaseTest {

    private lateinit var repository: UnsafeBehaviourRepositoryImpl
    private lateinit var useCase: GetUnsafeBehavioursBySyncStatusUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = GetUnsafeBehavioursBySyncStatusUseCase(repository)
    }

    @Test
    fun testExecute() = runTest {
        val synced = false
        val updatedAt = Date(0)
        val expected = listOf(
            UnsafeBehaviourModel(
                id = UUID.randomUUID(),
                tripId = UUID.randomUUID(),
                locationId = UUID.randomUUID(),
                driverProfileId = UUID.randomUUID(),
                behaviorType = "Speeding",
                severity = 5.0f,
                timestamp = System.currentTimeMillis(),
                date = Date(),
                updatedAt = updatedAt,
                updated = false,
                sync = false,

            )
        )

        // Ensure the mock returns a non-null list
        whenever(repository.getUnsafeBehavioursBySyncStatus(synced)).thenReturn(expected.map { it.toEntity() })

        val result = useCase.execute(synced)

        verify(repository).getUnsafeBehavioursBySyncStatus(synced)
        assertEquals(expected, result)
    }

    @Test
    fun testExecuteWithEmptyList() = runTest {
        val synced = false
        val expected = emptyList<UnsafeBehaviourModel>()

        // Ensure the mock returns an empty list
        whenever(repository.getUnsafeBehavioursBySyncStatus(synced)).thenReturn(expected.map { it.toEntity() })

        val result = useCase.execute(synced)

        verify(repository).getUnsafeBehavioursBySyncStatus(synced)
        assertEquals(expected, result)
    }
}

