package com.uoa.dbda.data.domain.usecase

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.dbda.domain.usecase.UpdateUnsafeBehaviourUseCase
import com.uoa.dbda.repository.UnsafeBehaviourRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class UpdateUnsafeBehaviourUseCaseTest {

    private lateinit var repository: UnsafeBehaviourRepositoryImpl
    private lateinit var useCase: UpdateUnsafeBehaviourUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = UpdateUnsafeBehaviourUseCase(repository)
    }

    @Test
    fun testExecute() = runBlocking {
        val unsafeBehaviour = UnsafeBehaviourModel(
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
        verify(repository).updateUnsafeBehaviour(unsafeBehaviour)
    }
}
