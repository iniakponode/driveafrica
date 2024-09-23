import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OnnxValue
import ai.onnxruntime.OrtSession
import android.content.Context
import com.uoa.core.mlclassifier.OnnxModelRunner
import com.uoa.core.mlclassifier.OrtEnvironmentWrapper
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class OnnxModelRunnerTest {

    private lateinit var context: Context
    private lateinit var ortEnvironmentWrapper: OrtEnvironmentWrapper
    private lateinit var ortSession: OrtSession
    private lateinit var onnxModelRunner: OnnxModelRunner

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        ortEnvironmentWrapper = mock(OrtEnvironmentWrapper::class.java)
        ortSession = mock(OrtSession::class.java)

        `when`(ortEnvironmentWrapper.createSession(any(ByteArray::class.java))).thenReturn(ortSession)

        onnxModelRunner = OnnxModelRunner(context, ortEnvironmentWrapper)
    }

    @Test
    fun testRunInference() {
        val inputData = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
        val inputTensor = mock(OnnxTensor::class.java)
        val outputTensor = mock(OrtSession.Result::class.java)
        val outputValue = mock(OnnxValue::class.java)

        `when`(OnnxTensor.createTensor(any(), eq(inputData))).thenReturn(inputTensor)
        `when`(ortSession.run(mapOf("input" to inputTensor))).thenReturn(outputTensor)
        `when`(outputTensor.get(0)).thenReturn(outputValue)
        `when`(outputValue.value).thenReturn(floatArrayOf(0.6f))

        val result = onnxModelRunner.runInference(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)

        assert(result == true)
    }
}