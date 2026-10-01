package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.metrics.BodyComp
import com.example.model.Profile
import com.example.model.Reading
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ManualEntryDialog(
    profile: Profile?,
    onDismiss: () -> Unit,
    onSave: (Reading) -> Unit
) {
    if (profile == null) return

    var weightStr by remember { mutableStateOf("75.0") }
    var hrStr by remember { mutableStateOf("") }

    val weight = weightStr.toDoubleOrNull()
    val previewReading = remember(weight, profile) {
        if (weight != null && weight > 0) {
            BodyComp.deriveReading(
                weightKg = weight,
                impedanceRaw = null,
                profile = profile,
                source = "manual"
            )
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OccultSurface,
        title = {
            Text(
                "Manual Weigh-In",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OccultInk
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Log a weigh-in for ${profile.name}. Derived metrics will be calculated automatically.",
                    fontSize = 12.sp,
                    color = OccultMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OccultInk,
                            unfocusedTextColor = OccultInk,
                            focusedBorderColor = OccultViolet,
                            unfocusedBorderColor = OccultTileBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = hrStr,
                        onValueChange = { hrStr = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("HR bpm (opt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OccultInk,
                            unfocusedTextColor = OccultInk,
                            focusedBorderColor = OccultViolet,
                            unfocusedBorderColor = OccultTileBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live Preview Card
                if (previewReading != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(OccultTile)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("DERIVED ESTIMATES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OccultViolet, letterSpacing = 1.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("BMI", color = OccultMuted, fontSize = 12.sp)
                                Text("${previewReading.bmi} (${previewReading.bmiClass})", color = OccultInk, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Body Fat %", color = OccultMuted, fontSize = 12.sp)
                                Text("${previewReading.bodyFatPct}% (${previewReading.fatMassKg} kg)", color = ColorFat, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Water Hydration", color = OccultMuted, fontSize = 12.sp)
                                Text("${previewReading.tbwL} L (${previewReading.bodyWaterPct}%)", color = ColorWater, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Resting BMR", color = OccultMuted, fontSize = 12.sp)
                                Text("${previewReading.bmrKcal} kcal/day", color = ColorEnergy, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (previewReading != null) {
                        val hr = hrStr.toIntOrNull()
                        onSave(previewReading.copy(heartRate = hr))
                    }
                },
                enabled = previewReading != null,
                colors = ButtonDefaults.buttonColors(containerColor = OccultViolet, contentColor = Color.Black)
            ) {
                Text("Save Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OccultMuted)
            }
        }
    )
}
