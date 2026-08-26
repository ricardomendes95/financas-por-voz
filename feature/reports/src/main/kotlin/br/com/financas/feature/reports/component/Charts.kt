package br.com.financas.feature.reports.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * `Canvas` nativo do Compose (regra §6.3 — nada de MPAndroidChart, que exige
 * `AndroidView` e mata a performance de recomposição).
 */
@Composable
fun DonutChart(slices: List<Pair<Float, Color>>, modifier: Modifier = Modifier, strokeWidth: Dp = 28.dp) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val diameter = minOf(size.width, size.height) - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        var startAngle = -90f
        slices.forEach { (fraction, color) ->
            val sweep = (fraction * 360f).coerceAtLeast(0f)
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke)
            )
            startAngle += sweep
        }
    }
}

@Composable
fun SimpleBarChart(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val max = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
        val barWidth = size.width / values.size
        values.forEachIndexed { index, value ->
            val barHeight = (value / max) * size.height
            drawRect(
                color = color,
                topLeft = Offset(index * barWidth + barWidth * 0.15f, size.height - barHeight),
                size = Size(barWidth * 0.7f, barHeight)
            )
        }
    }
}

@Composable
fun DualLineChart(
    seriesA: List<Float>,
    seriesB: List<Float>,
    colorA: Color,
    colorB: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val max = (seriesA + seriesB).maxOrNull()?.takeIf { it > 0f } ?: 1f

        fun toPath(values: List<Float>): Path {
            val path = Path()
            val denom = (values.size - 1).coerceAtLeast(1)
            values.forEachIndexed { index, value ->
                val x = size.width * index / denom
                val y = size.height - (value / max) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        drawPath(toPath(seriesA), color = colorA, style = Stroke(width = 5f))
        drawPath(toPath(seriesB), color = colorB, style = Stroke(width = 5f))
    }
}
