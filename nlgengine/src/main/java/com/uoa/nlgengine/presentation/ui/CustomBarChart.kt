import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.withTranslation
import com.uoa.nlgengine.data.model.UnsafeBehaviorChartEntry
import kotlin.math.max

@Composable
fun CustomBarChart(
    chartData: List<UnsafeBehaviorChartEntry>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    barWidth: Float = 40f,
    spacing: Float = 20f,
    maxBarHeight: Float = 200f
) {
    // Calculate the maximum value in the data for scaling purposes
    val maxValue = chartData.maxOfOrNull { it.behaviorCount } ?: 1

    Box(
        modifier = modifier
            .background(backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val barSpacing = spacing
            val barSectionWidth = barWidth + barSpacing
            val totalBarsWidth = chartData.size * barSectionWidth

            val scale = maxBarHeight / max(maxValue, 1).toFloat()

            chartData.forEachIndexed { index, entry ->
                val xOffset = (canvasWidth - totalBarsWidth) / 2 + index * barSectionWidth
                val barHeight = entry.behaviorCount * scale
                val yOffset = canvasHeight - barHeight

                // Draw the bar
                drawRect(
                    color = barColor,
                    topLeft = androidx.compose.ui.geometry.Offset(xOffset, yOffset),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )

                // Draw the label (hour)
                drawContext.canvas.nativeCanvas.apply {
                    withTranslation(xOffset + barWidth / 2, canvasHeight) {
                        drawText(
                            formatHour(entry.hour),
                            0f,
                            0f,
                            android.graphics.Paint().apply {
                                textAlign = android.graphics.Paint.Align.CENTER
                                color = android.graphics.Color.BLACK
                                textSize = 32f
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChartLegend(
    labels: List<String>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.zip(colors).forEach { (label, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(color)
                        )
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ChartScreen(
    chartData: List<UnsafeBehaviorChartEntry>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Driving Behaviour Chart",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        CustomBarChart(
            chartData = chartData,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            barColor = MaterialTheme.colorScheme.secondary,
            backgroundColor = MaterialTheme.colorScheme.background
        )

        Spacer(modifier = Modifier.height(16.dp))

        ChartLegend(
            labels = chartData.map { formatHour(it.hour) },
            colors = listOf(MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour == 12 -> "12 PM"
        hour < 12 -> "${hour} AM"
        else -> "${hour - 12} PM"
    }
}