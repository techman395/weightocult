package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Profile
import com.example.model.Reading
import com.example.ui.components.BentoCard
import com.example.ui.components.DonutCompositionCanvas
import com.example.ui.components.TrendChartCanvas
import com.example.ui.theme.*
import java.util.Locale

enum class TrendMetric(val label: String, val unit: String, val color: Color) {
    WEIGHT("Weight", "kg", OccultViolet),
    BODY_FAT("Body Fat", "%", ColorFat),
    MUSCLE("Lean Mass", "kg", ColorMuscle),
    BMI("BMI", "", ColorBmi)
}

enum class TimeFilter(val label: String, val days: Int) {
    WEEK("7D", 7),
    MONTH("30D", 30),
    QUARTER("90D", 90),
    ALL("All", Int.MAX_VALUE)
}

@Composable
fun TrendsScreen(
    profile: Profile?,
    readings: List<Reading>,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf(TrendMetric.WEIGHT) }
    var selectedTime by remember { mutableStateOf(TimeFilter.MONTH) }
    val scrollState = rememberScrollState()

    if (profile == null) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Select a profile to view trends", color = OccultMuted)
        }
        return
    }

    val cutoff = if (selectedTime == TimeFilter.ALL) 0L else System.currentTimeMillis() - (selectedTime.days * 86_400_000L)
    val filteredReadings = readings.filter { it.timestamp >= cutoff }

    val dataPoints: List<Double> = filteredReadings.mapNotNull { r ->
        when (selectedMetric) {
            TrendMetric.WEIGHT -> r.weightKg
            TrendMetric.BODY_FAT -> r.bodyFatPct
            TrendMetric.MUSCLE -> r.ffmKg ?: r.smmKg
            TrendMetric.BMI -> r.bmi
        }
    }

    val latest = filteredReadings.lastOrNull() ?: readings.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Metric Selector Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrendMetric.values().forEach { metric ->
                val isSelected = metric == selectedMetric
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) metric.color.copy(alpha = 0.2f) else OccultTile)
                        .clickable { selectedMetric = metric }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = metric.label,
                        color = if (isSelected) metric.color else OccultInkSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Time Range Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedMetric.label.uppercase()} TRAJECTORY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = OccultMuted,
                letterSpacing = 1.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TimeFilter.values().forEach { tf ->
                    val isSel = tf == selectedTime
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) OccultViolet else OccultSurface)
                            .clickable { selectedTime = tf }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tf.label,
                            color = if (isSel) Color.Black else OccultMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Main Trend Chart Card
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data points for this metric in selected range", color = OccultMuted, fontSize = 12.sp)
                }
            } else {
                val minVal = dataPoints.minOrNull() ?: 0.0
                val maxVal = dataPoints.maxOrNull() ?: 0.0
                val avgVal = dataPoints.average()
                val delta = (dataPoints.last() - dataPoints.first())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text("Current", fontSize = 11.sp, color = OccultMuted)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", dataPoints.last())} ${selectedMetric.unit}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = OccultInk,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Period Change", fontSize = 11.sp, color = OccultMuted)
                        val sign = if (delta > 0) "+" else ""
                        Text(
                            text = "$sign${String.format(Locale.US, "%.1f", delta)} ${selectedMetric.unit}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (delta < 0) OccultMint else OccultWarn,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                TrendChartCanvas(
                    points = dataPoints,
                    lineColor = selectedMetric.color,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )

                Spacer(Modifier.height(16.dp))

                // Min / Avg / Max Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MIN", fontSize = 10.sp, color = OccultMuted, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.US, "%.1f", minVal),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OccultInk,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AVG", fontSize = 10.sp, color = OccultMuted, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.US, "%.1f", avgVal),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OccultInk,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MAX", fontSize = 10.sp, color = OccultMuted, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.US, "%.1f", maxVal),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OccultInk,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Body Composition Donut Card
        if (latest != null && latest.bodyFatPct != null) {
            val fat = latest.bodyFatPct ?: 20.0
            val lean = if (latest.weightKg > 0 && latest.ffmKg != null) {
                kotlin.math.round((latest.ffmKg / latest.weightKg * 100.0) * 10.0) / 10.0
            } else (100.0 - fat)
            val water = latest.bodyWaterPct ?: 50.0

            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "BODY COMPOSITION BREAKDOWN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OccultMuted,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(12.dp))

                DonutCompositionCanvas(
                    musclePct = lean,
                    fatPct = fat,
                    waterPct = water,
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Legends
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LegendItem(color = ColorMuscle, label = "Lean Mass", value = "${String.format(Locale.US, "%.1f", lean)}%")
                    LegendItem(color = ColorFat, label = "Fat", value = "${String.format(Locale.US, "%.1f", fat)}%")
                    LegendItem(color = ColorWater, label = "Water", value = "${String.format(Locale.US, "%.1f", water)}%")
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "* Lean Mass includes your muscle, water, and bone weight combined.",
                    fontSize = 11.sp,
                    color = OccultMuted,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, fontSize = 11.sp, color = OccultMuted)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OccultInk, fontFamily = FontFamily.Monospace)
        }
    }
}
