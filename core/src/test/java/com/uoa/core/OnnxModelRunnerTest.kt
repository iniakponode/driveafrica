//// OnnxModelRunnerTest.kt
//import ai.onnxruntime.OnnxTensor
//import ai.onnxruntime.OrtEnvironment
//import ai.onnxruntime.OrtSession
//import ai.onnxruntime.OnnxValue
//import io.mockk.*
//import io.mockk.junit5.MockKExtension
//import org.junit.jupiter.api.*
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.extension.ExtendWith
//import java.util.*
//
///**
// * Unit tests for the OnnxModelRunner class.
// */
//@ExtendWith(MockKExtension::class)
//class OnnxModelRunnerTest {
//
//    // Mocked dependencies
//    @MockK
//    lateinit var ortEnvironmentWrapper: OrtEnvironmentWrapper
//
//    @MockK
//    lateinit var ortEnvironment: OrtEnvironment
//
//    @MockK
//    lateinit var session: OrtSession
//
//    @MockK
//    lateinit var onnxValue: OnnxValue
//
//    // System Under Test (SUT)
//    private lateinit var onnxModelRunner: OnnxModelRunner
//
//    /**
//     * Sets up the mocks before each test.
//     */
//    @BeforeEach
//    fun setUp() {
//        // Define behavior for ortEnvironmentWrapper
//        every { ortEnvironmentWrapper.ortEnvironment } returns ortEnvironment
//
//        // Initialize the OnnxModelRunner with mocked dependencies
//        onnxModelRunner = OnnxModelRunner(ortEnvironmentWrapper, session)
//    }
//
//    /**
//     * Clears all mocks after each test to ensure test isolation.
//     */
//    @AfterEach
//    fun tearDown() {
//        clearAllMocks()
//        unmockkAll()
//    }
//
//    /**
//     * Tests that runInference returns true when the model output is greater than 0.5f.
//     */
//    @Test
//    fun `runInference returns true when model output > 0.5`() {
//        // Given
//        val features = TripFeatures(
//            hourOfDayMean = 10f,
//            dayOfWeekMean = 3f,
//            speedStd = 20f,
//            courseStd = 30f,
//            accelerationYOriginalMean = 5f
//        )
//
//        val inputData = floatArrayOf(
//            features.hourOfDayMean,
//            features.dayOfWeekMean,
//            features.speedStd,
//            features.courseStd,
//            features.accelerationYOriginalMean
//        )
//
//        val inputTensor = mockk<OnnxTensor>()
//
//        // Mock the static method OnnxTensor.createTensor
//        mockkStatic(OnnxTensor::class)
//        every { OnnxTensor.createTensor(ortEnvironment, inputData) } returns inputTensor
//
//        // Mock the model output: [0.6f] > 0.5f
//        val modelOutput = arrayOf(floatArrayOf(0.6f))
//        val validOnnxValue = mockk<OnnxValue>()
//        every { validOnnxValue.value } returns modelOutput
//        val outputMap = mapOf("output" to validOnnxValue)
//
//        // Mock session.run to return the desired output
//        every { session.run(mapOf("input" to inputTensor)) } returns outputMap
//
//        // When
//        val result = onnxModelRunner.runInference(features)
//
//        // Then
//        assertTrue(result, "Expected runInference to return true for model output > 0.5f")
//
//        // Verify interactions
//        verify(exactly = 1) { OnnxTensor.createTensor(ortEnvironment, inputData) }
//        verify(exactly = 1) { session.run(mapOf("input" to inputTensor)) }
//
//        // Confirm no other interactions
//        confirmVerified(OnnxTensor::class, ortEnvironmentWrapper, session, validOnnxValue)
//    }
//
//    /**
//     * Tests that runInference returns false when the model output is less than or equal to 0.5f.
//     */
//    @Test
//    fun `runInference returns false when model output <= 0.5`() {
//        // Given
//        val features = TripFeatures(
//            hourOfDayMean = 8f,
//            dayOfWeekMean = 2f,
//            speedStd = 15f,
//            courseStd = 25f,
//            accelerationYOriginalMean = 4f
//        )
//
//        val inputData = floatArrayOf(
//            features.hourOfDayMean,
//            features.dayOfWeekMean,
//            features.speedStd,
//            features.courseStd,
//            features.accelerationYOriginalMean
//        )
//
//        val inputTensor = mockk<OnnxTensor>()
//
//        // Mock the static method OnnxTensor.createTensor
//        mockkStatic(OnnxTensor::class)
//        every { OnnxTensor.createTensor(ortEnvironment, inputData) } returns inputTensor
//
//        // Mock the model output: [0.5f] <= 0.5f
//        val modelOutput = arrayOf(floatArrayOf(0.5f))
//        val validOnnxValue = mockk<OnnxValue>()
//        every { validOnnxValue.value } returns modelOutput
//        val outputMap = mapOf("output" to validOnnxValue)
//
//        // Mock session.run to return the desired output
//        every { session.run(mapOf("input" to inputTensor)) } returns outputMap
//
//        // When
//        val result = onnxModelRunner.runInference(features)
//
//        // Then
//        assertFalse(result, "Expected runInference to return false for model output <= 0.5f")
//
//        // Verify interactions
//        verify(exactly = 1) { OnnxTensor.createTensor(ortEnvironment, inputData) }
//        verify(exactly = 1) { session.run(mapOf("input" to inputTensor)) }
//
//        // Confirm no other interactions
//        confirmVerified(OnnxTensor::class, ortEnvironmentWrapper, session, validOnnxValue)
//    }
//
//    /**
//     * Tests that runInference throws IllegalStateException when the model output is invalid.
//     */
//    @Test
//    fun `runInference throws IllegalStateException when output is invalid`() {
//        // Given
//        val features = TripFeatures(
//            hourOfDayMean = 5f,
//            dayOfWeekMean = 1f,
//            speedStd = 10f,
//            courseStd = 20f,
//            accelerationYOriginalMean = 3f
//        )
//
//        val inputData = floatArrayOf(
//            features.hourOfDayMean,
//            features.dayOfWeekMean,
//            features.speedStd,
//            features.courseStd,
//            features.accelerationYOriginalMean
//        )
//
//        val inputTensor = mockk<OnnxTensor>()
//
//        // Mock the static method OnnxTensor.createTensor
//        mockkStatic(OnnxTensor::class)
//        every { OnnxTensor.createTensor(ortEnvironment, inputData) } returns inputTensor
//
//        // Mock the model output with invalid type (List<Float> instead of Array<FloatArray>)
//        val invalidOutput = listOf(0.6f)
//        val invalidOnnxValue = mockk<OnnxValue>()
//        every { invalidOnnxValue.value } returns invalidOutput
//        val outputMap = mapOf("output" to invalidOnnxValue)
//
//        // Mock session.run to return the invalid output
//        every { session.run(mapOf("input" to inputTensor)) } returns outputMap
//
//        // When & Then
//        val exception = assertThrows<IllegalStateException> {
//            onnxModelRunner.runInference(features)
//        }
//
//        assertEquals("Invalid model output", exception.message, "Expected IllegalStateException with message 'Invalid model output'")
//
//        // Verify interactions
//        verify(exactly = 1) { OnnxTensor.createTensor(ortEnvironment, inputData) }
//        verify(exactly = 1) { session.run(mapOf("input" to inputTensor)) }
//
//        // Confirm no other interactions
//        confirmVerified(OnnxTensor::class, ortEnvironmentWrapper, session, invalidOnnxValue)
//    }
//
//    /**
//     * Tests that runInference rethrows exceptions thrown during session.run.
//     */
//    @Test
//    fun `runInference rethrows exception when sessionRun fails`() {
//        // Given
//        val features = TripFeatures(
//            hourOfDayMean = 12f,
//            dayOfWeekMean = 4f,
//            speedStd = 25f,
//            courseStd = 35f,
//            accelerationYOriginalMean = 6f
//        )
//
//        val inputData = floatArrayOf(
//            features.hourOfDayMean,
//            features.dayOfWeekMean,
//            features.speedStd,
//            features.courseStd,
//            features.accelerationYOriginalMean
//        )
//
//        val inputTensor = mockk<OnnxTensor>()
//
//        // Mock the static method OnnxTensor.createTensor
//        mockkStatic(OnnxTensor::class)
//        every { OnnxTensor.createTensor(ortEnvironment, inputData) } returns inputTensor
//
//        // Mock session.run to throw an exception
//        every { session.run(mapOf("input" to inputTensor)) } throws RuntimeException("Session run failed")
//
//        // When & Then
//        val exception = assertThrows<RuntimeException> {
//            onnxModelRunner.runInference(features)
//        }
//
//        assertEquals("Session run failed", exception.message, "Expected RuntimeException with message 'Session run failed'")
//
//        // Verify interactions
//        verify(exactly = 1) { OnnxTensor.createTensor(ortEnvironment, inputData) }
//        verify(exactly = 1) { session.run(mapOf("input" to inputTensor)) }
//
//        // Confirm no other interactions
//        confirmVerified(OnnxTensor::class, ortEnvironmentWrapper, session)
//    }
//}
