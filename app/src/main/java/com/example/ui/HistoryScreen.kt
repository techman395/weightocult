package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
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
import com.example.ui.components.BmiTag
import com.example.ui.components.DeltaChip
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    profile: Profile?,
    readings: List<Reading>,
    onDeleteReading: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedReadingForDetail by remember { mutableStateOf<Reading?>(null) }
    var readingToDelete by remember { mutableStateOf<Reading?>(null) }

    val reversedReadings = remember(readings) { readings.reversed() }

    if (profile == null || reversedReadings.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No measurement history yet.", color = OccultMuted)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "${reversedReadings.size} RECORDED WEIGH-INS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = OccultMuted,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
        }

        itemsIndexed(reversedReadings, key = { _, r -> r.id }) { index, reading ->
            val prevInTime = if (index < reversedReadings.size - 1) reversedReadings[index + 1] else null
            val dateStr = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(reading.timestamp))

            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { selectedReadingForDetail = reading }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            color = OccultMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.US, "%.1f", reading.weightKg),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = OccultInk,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = " kg",
                                fontSize = 14.sp,
                                color = OccultMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            DeltaChip(current = reading.weightKg, previous = prevInTime?.weightKg, unit = " kg")
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (reading.heartRate != null) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = ColorHeart,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${reading.heartRate} bpm",
                                        fontSize = 11.sp,
                                        color = OccultInkSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                if (reading.bodyFatPct != null) {
                                    Text(
                                        text = "${reading.bodyFatPct}% fat",
                                        fontSize = 11.sp,
                                        color = ColorFat,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BmiTag(bmiClass = reading.bmiClass)
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(OccultSurface)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = reading.source.uppercase(),
                                        fontSize = 9.sp,
                                        color = OccultMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = { readingToDelete = reading },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = OccultMuted.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Biometric Detail Dialog
    selectedReadingForDetail?.let { r ->
        AlertDialog(
            onDismissRequest = { selectedReadingForDetail = null },
            containerColor = OccultSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Weigh-in Telemetry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OccultInk)
                    IconButton(onClick = { selectedReadingForDetail = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = OccultMuted)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy · HH:mm", Locale.getDefault())
                    Text(sdf.format(Date(r.timestamp)), color = OccultMuted, fontSize = 12.sp)

                    Divider(color = OccultTileBorder)

                    DetailRow("Weight", "${r.weightKg} kg")
                    DetailRow("Heart Rate", r.heartRate?.let { "$it bpm" } ?: "Not measured")
                    DetailRow("Body Fat %", r.bodyFatPct?.let { "$it%" } ?: "—")
                    DetailRow("Fat Mass", r.fatMassKg?.let { "$it kg" } ?: "—")
                    DetailRow("Lean Mass", r.ffmKg?.let { "$it kg" } ?: "—")
                    DetailRow("Skeletal Muscle (BIA)", r.smmKg?.let { "$it kg (${r.smmPct}%)" } ?: "—")
                    DetailRow("Total Body Water", r.tbwL?.let { "$it L (${r.bodyWaterPct}%)" } ?: "—")
                    DetailRow("BMI", r.bmi?.let { "$it (${r.bmiClass})" } ?: "—")
                    DetailRow("Resting Metabolic Rate", r.bmrKcal?.let { "$it kcal/day" } ?: "—")
                    DetailRow("Raw Impedance", r.impedanceRaw?.let { "$it Ω" } ?: "—")
                    DetailRow("Source", r.source.capitalize(Locale.ROOT))

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "* Lean Mass includes your muscle, water, and bone weight combined.",
                        fontSize = 11.sp,
                        color = OccultMuted,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedReadingForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = OccultViolet, contentColor = Color.Black)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Delete Confirmation
    readingToDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { readingToDelete = null },
            containerColor = OccultSurface,
            title = { Text("Delete Reading?", color = OccultInk) },
            text = {
                Text(
                    "Are you sure you want to delete the weigh-in of ${r.weightKg} kg from ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(r.timestamp))}?",
                    color = OccultInkSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteReading(r.id)
                        readingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OccultRed, contentColor = Color.White)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { readingToDelete = null }) {
                    Text("Cancel", color = OccultMuted)
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = OccultMuted, fontSize = 12.sp)
        Text(value, color = OccultInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
