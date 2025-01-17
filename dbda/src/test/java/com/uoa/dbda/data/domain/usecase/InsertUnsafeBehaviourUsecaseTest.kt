package com.uoa.dbda.data.domain.usecase

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.dbda.domain.usecase.InsertUnsafeBehaviourUseCase
import com.uoa.dbda.repository.UnsafeBehaviourRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID

class InsertUnsafeBehaviourUseCaseTest {

    private lateinit var repository: UnsafeBehaviourRepositoryImpl
    private lateinit var useCase: InsertUnsafeBehaviourUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = InsertUnsafeBehaviourUseCase(repository)
    }

    @Test
    fun testExecute() = runTest {
        val unsafeBehaviour = UnsafeBehaviourModel(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Speeding",
            5.0f,
            System.currentTimeMillis(),
            Date(),
            Date(),
        )

        useCase.execute(unsafeBehaviour)
        verify(repository).insertUnsafeBehaviour(unsafeBehaviour)
    }
}
