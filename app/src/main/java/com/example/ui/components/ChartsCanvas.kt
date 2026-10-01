package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OccultTileBorder

@Composable
fun SparklineCanvas(
    points: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier.fillMaxWidth().height(48.dp)
) {
    if (points.size < 2) return

    Canvas(modifier = modifier) {
        val minVal = points.minOrNull() ?: 0.0
        val maxVal = points.maxOrNull() ?: 1.0
        val range = if (maxVal - minVal > 0.001) maxVal - minVal else 1.0

        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1)

        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, pt ->
            val x = i * stepX
            val norm = (pt - minVal) / range
            val y = h - (norm * (h - 12.dp.toPx()) + 6.dp.toPx()).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)
            )
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw last point dot
        val lastX = w
        val lastNorm = (points.last() - minVal) / range
        val lastY = h - (lastNorm * (h - 12.dp.toPx()) + 6.dp.toPx()).toFloat()
        drawCircle(
            color = lineColor,
            radius = 3.5.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}

@Composable
fun TrendChartCanvas(
    points: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier.fillMaxWidth().height(180.dp)
) {
    if (points.isEmpty()) return

    Canvas(modifier = modifier) {
        val minVal = points.minOrNull() ?: 0.0
        val maxVal = points.maxOrNull() ?: 1.0
        val range = if (maxVal - minVal > 0.001) maxVal - minVal else 1.0

        val w = size.width
        val h = size.height
        val topPadding = 20.dp.toPx()
        val bottomPadding = 24.dp.toPx()
        val usableH = h - topPadding - bottomPadding

        // Draw horizontal grid lines
        for (i in 0..3) {
            val y = topPadding + (usableH / 3f) * i
            drawLine(
                color = OccultTileBorder.copy(alpha = 0.6f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (points.size == 1) {
            drawCircle(
                color = lineColor,
                radius = 6.dp.toPx(),
                center = Offset(w / 2f, h / 2f)
            )
            return@Canvas
        }

        val stepX = w / (points.size - 1)
        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, pt ->
            val x = i * stepX
            val norm = (pt - minVal) / range
            val y = (h - bottomPadding - (norm * usableH)).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h - bottomPadding)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, h - bottomPadding)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                startY = topPadding,
                endY = h - bottomPadding
            )
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw points
        points.forEachIndexed { i, pt ->
            val x = i * stepX
            val norm = (pt - minVal) / range
            val y = (h - bottomPadding - (norm * usableH)).toFloat()
            drawCircle(
                color = Color(0xFF08080C),
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = lineColor,
                radius = 2.5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun DonutCompositionCanvas(
    musclePct: Double,
    fatPct: Double,
    waterPct: Double,
    modifier: Modifier = Modifier.height(130.dp).fillMaxWidth()
) {
    Canvas(modifier = modifier) {
        val total = musclePct + fatPct + waterPct
        if (total <= 0.0) return@Canvas

        val mWeight = (musclePct / total * 360f).toFloat()
        val fWeight = (fatPct / total * 360f).toFloat()
        val wWeight = (360f - mWeight - fWeight).coerceAtLeast(0f)

        val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
        val diameter = kotlin.math.min(size.width, size.height) - 24.dp.toPx()
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        var startAngle = -90f
        // Muscle (Mint)
        drawArc(
            color = Color(0xFF5EE0A0),
            startAngle = startAngle,
            sweepAngle = mWeight - 3f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        startAngle += mWeight

        // Fat (Orange)
        drawArc(
            color = Color(0xFFFF8A4C),
            startAngle = startAngle,
            sweepAngle = fWeight - 3f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        startAngle += fWeight

        // Water (Cyan)
        drawArc(
            color = Color(0xFF42D6FF),
            startAngle = startAngle,
            sweepAngle = wWeight - 3f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
    }
}
