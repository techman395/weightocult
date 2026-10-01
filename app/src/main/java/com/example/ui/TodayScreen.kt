package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TodayScreen(
    profile: Profile?,
    readings: List<Reading>,
    onMeasureClick: () -> Unit,
    onManualClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val profileColor = try {
        Color(android.graphics.Color.parseColor(profile?.colorHex ?: "#A06BFF"))
    } catch (_: Exception) {
        OccultViolet
    }

    if (profile == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Create or select a profile to start measuring.", color = OccultMuted)
        }
        return
    }

    val latest = readings.lastOrNull()
    val prev = if (readings.size > 1) readings[readings.size - 2] else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onMeasureClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = profileColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Measure Scale", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = onManualClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OccultInk),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(OccultTileBorder)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Manual", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        if (latest == null) {
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Welcome to WeightOCult",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OccultInk
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No readings recorded yet. Tap 'Measure Scale' to connect your Cult Smart Scale (or run the simulator), or add a manual entry.",
                        color = OccultMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            return
        }

        // Hero Card: Current Weight & Sparkline
        BentoCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = profileColor.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val relTime = formatRelativeTime(latest.timestamp)
                Text(
                    text = "LATEST WEIGH-IN · $relTime",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OccultMuted,
                    letterSpacing = 1.sp
                )
                DeltaChip(
                    current = latest.weightKg,
                    previous = prev?.weightKg,
                    unit = " kg"
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format(Locale.US, "%.1f", latest.weightKg),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = OccultInk,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "kg",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OccultMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (readings.size >= 2) {
                Spacer(Modifier.height(12.dp))
                val sparkPoints = readings.takeLast(12).map { it.weightKg }
                SparklineCanvas(points = sparkPoints, lineColor = profileColor)
            }
        }

        // Bento Grid: 6 Health telemetry metrics
        Text(
            text = "BODY COMPOSITION TELEMETRY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = OccultMuted,
            letterSpacing = 1.sp
        )

        // Row 1: Body Fat & Skeletal Muscle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Body Fat %
            BentoCard(modifier = Modifier.weight(1f)) {
                Text("BODY FAT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorFat)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = latest.bodyFatPct?.let { "${it}%" } ?: "—",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OccultInk,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = latest.fatMassKg?.let { "${it} kg fat mass" } ?: "Deurenberg 1991",
                    fontSize = 11.sp,
                    color = OccultMuted
                )
            }

            // Lean Mass (Fat-Free Mass)
            BentoCard(modifier = Modifier.weight(1f)) {
                Text("LEAN MASS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorMuscle)
                Spacer(Modifier.height(6.dp))
                val leanKg = latest.ffmKg ?: latest.smmKg
                val leanPct = if (latest.weightKg > 0 && leanKg != null) {
                    kotlin.math.round((leanKg / latest.weightKg * 100.0) * 10.0) / 10.0
                } else null
                Text(
                    text = leanKg?.let { "${it} kg" } ?: "—",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OccultInk,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = leanPct?.let { "${it}% body weight" } ?: "Fat-Free Mass",
                    fontSize = 11.sp,
                    color = OccultMuted
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Includes muscle, water & bone combined",
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = OccultMuted.copy(alpha = 0.85f)
                )
            }
        }

        // Row 2: Water & BMI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Body Water
            BentoCard(modifier = Modifier.weight(1f)) {
                Text("TOTAL BODY WATER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorWater)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = latest.tbwL?.let { "${it} L" } ?: "—",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OccultInk,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = latest.bodyWaterPct?.let { "${it}% hydration" } ?: "Watson 1980",
                    fontSize = 11.sp,
                    color = OccultMuted
                )
            }

            // BMI
            BentoCard(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BMI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorBmi)
                    BmiTag(bmiClass = latest.bmiClass)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = latest.bmi?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OccultInk,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "WHO standard",
                    fontSize = 11.sp,
                    color = OccultMuted
                )
            }
        }

        // Row 3: BMR & Heart Rate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Resting Metabolic Rate
            BentoCard(modifier = Modifier.weight(1f)) {
                Text("RESTING METABOLIC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorEnergy)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = latest.bmrKcal?.let { "${it} kcal" } ?: "—",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OccultInk,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Mifflin-St Jeor daily burn",
                    fontSize = 11.sp,
                    color = OccultMuted
                )
            }

            // Heart Rate
            BentoCard(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HEART RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorHeart)
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = ColorHeart,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = latest.heartRate?.let { "$it bpm" } ?: "—",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OccultInk,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (latest.heartRate != null) "Resting scale sensor" else "Scale sensor only",
                    fontSize = 11.sp,
                    color = OccultMuted
                )
            }
        }

        // Scientific Equations Attribution & Lean Mass Transparency Note
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(OccultSurface)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = OccultViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Lean Mass Transparency Note",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OccultInk
                    )
                }
                Text(
                    text = "The \"Lean Mass\" metric displays your Fat-Free Mass (FFM). This number includes your muscle, water, and bone weight combined. 100% of calculations run privately on-device using peer-reviewed equations (Mifflin-St Jeor 1990, Deurenberg 1991, Watson 1980, Janssen 2000).",
                    fontSize = 11.sp,
                    color = OccultMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

fun formatRelativeTime(millis: Long): String {
    val diffSec = ((System.currentTimeMillis() - millis) / 1000).coerceAtLeast(0)
    return when {
        diffSec < 60 -> "just now"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86400 -> "${diffSec / 3600}h ago"
        diffSec < 2592000 -> "${diffSec / 86400}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}
