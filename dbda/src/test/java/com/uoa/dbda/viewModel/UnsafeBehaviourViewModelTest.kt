package com.uoa.sensor

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
//import com.uoa.dbda.domain.usecase.analyser.AnalyzeUnsafeBehaviorUseCase
//import com.uoa.dbda.presentation.viewModel.UnsafeBehaviourViewModel
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
class UnsafeBehaviourViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)
    private val mockLogger: (String, String) -> Unit = mock()
    val expectedTag = "UnsafeBehaviourViewModel"
    val expectedMessage = "Error during analysis"

    @Mock
    private lateinit var analyzeUnsafeBehaviorUseCase: com.uoa.dbda.domain.usecase.analyser.AnalyzeUnsafeBehaviorUseCase

    private lateinit var viewModel: com.uoa.dbda.presentation.viewModel.UnsafeBehaviourViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher) // Set the Main dispatcher for tests
        MockitoAnnotations.openMocks(this)
        viewModel = com.uoa.dbda.presentation.viewModel.UnsafeBehaviourViewModel(
            analyzeUnsafeBehaviorUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // Reset the Main dispatcher after each test
    }

    @Test
    fun `startUnsafeBehaviorAnalysis should start analysis`() = runTest(testDispatcher) {
        val job = viewModel.startUnsafeBehaviorAnalysis()
//        job.join()
        testScheduler.advanceTimeBy(16000) // Advance time beyond the timeout
        // Verify that the job is cancelled
        verify(analyzeUnsafeBehaviorUseCase, atLeastOnce()).execute(anyLong(), anyLong())
    }

    @Test
    fun `stopUnsafeBehaviorAnalysis should stop analysis`() = runTest {
        viewModel.startUnsafeBehaviorAnalysis()
        viewModel.stopUnsafeBehaviorAnalysis()
        verify(analyzeUnsafeBehaviorUseCase, atLeastOnce()).execute(anyLong(), anyLong())
        assertNull(viewModel.analysisJob)
    }

    @Test
    fun `startUnsafeBehaviorAnalysis should handle exceptions gracefully`() = runTest(testDispatcher) {
        // Stub the use case to throw an exception
        whenever(analyzeUnsafeBehaviorUseCase.execute(anyLong(), anyLong())).thenThrow(RuntimeException("Simulated error")) // Specific exception for clarity

        // Start the analysis (expecting an exception)
        viewModel.startUnsafeBehaviorAnalysis()

        // Wait for coroutines to finish
        testScheduler.advanceUntilIdle()

        // Assert that the logger was called with the expected message and tag
        verify(mockLogger, atLeastOnce()).invoke(expectedTag, expectedMessage)
        // Assert error handling behavior here (e.g., check error state in ViewModel)
    }





}