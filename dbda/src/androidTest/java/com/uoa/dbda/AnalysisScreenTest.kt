//package com.uoa.dbda
//
//import androidx.compose.ui.test.junit4.createComposeRule
//import androidx.compose.ui.test.onNodeWithText
//import androidx.compose.ui.test.performClick
//import androidx.compose.ui.test.performTextInput
//import com.uoa.dbda.domain.usecase.FetchRawSensorDataByDateUseCase
//import com.uoa.dbda.domain.usecase.FetchRawSensorDataByTripIdUseCase
//import com.uoa.dbda.domain.usecase.InsertUnsafeBehaviourUseCase
//import com.uoa.dbda.domain.usecase.analyser.UnsafeBehaviorAnalyser
//import com.uoa.dbda.presentation.ui.AnalysisScreen
//import com.uoa.dbda.presentation.viewModel.AnalysisViewModel
//import dagger.hilt.android.testing.HiltAndroidRule
//import dagger.hilt.android.testing.HiltAndroidTest
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import org.junit.Before
//import org.junit.Rule
//import org.junit.Test
//import org.mockito.Mock
//import org.mockito.Mockito
//import org.mockito.MockitoAnnotations
//import java.util.UUID
//
//@HiltAndroidTest
//
//class AnalysisScreenTest {
//
//    @get:Rule
//    val hiltRule = HiltAndroidRule(this)
//
//    @get:Rule
//    val composeTestRule = createComposeRule()
//
//    @Mock
//    private lateinit var fetchRawSensorDataByDateUseCase: FetchRawSensorDataByDateUseCase
//
//    @Mock
//    private lateinit var fetchRawSensorDataByTripIdUseCase: FetchRawSensorDataByTripIdUseCase
//
//    @Mock
//    private lateinit var insertUnsafeBehaviourUseCase: InsertUnsafeBehaviourUseCase
//
//    @Mock
//    private lateinit var unsafeBehaviorAnalyser: UnsafeBehaviorAnalyser
//
//    private lateinit var viewModel: AnalysisViewModel
//
//    @Before
//    fun setup() {
//        MockitoAnnotations.openMocks(this)
//        hiltRule.inject()
//
//        viewModel = AnalysisViewModel(
//            fetchRawSensorDataByDateUseCase,
//            fetchRawSensorDataByTripIdUseCase,
//            insertUnsafeBehaviourUseCase,
//            unsafeBehaviorAnalyser
//        )
//    }
//
//    @Test
//    fun testUIElementsDisplayed() {
//        composeTestRule.setContent {
//            AnalysisScreen(analysisViewModel = viewModel)
//        }
//
//        composeTestRule.onNodeWithText("Start Date").assertExists()
//        composeTestRule.onNodeWithText("End Date").assertExists()
//        composeTestRule.onNodeWithText("Analyze by Date Range").assertExists()
//        composeTestRule.onNodeWithText("Trip ID").assertExists()
//        composeTestRule.onNodeWithText("Analyze by Trip ID").assertExists()
//    }
//}