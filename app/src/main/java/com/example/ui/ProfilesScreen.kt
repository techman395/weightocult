package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.model.Profile
import com.example.model.Reading
import com.example.ui.components.BentoCard
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.*
import java.util.Calendar
import java.util.Locale

@Composable
fun ProfilesScreen(
    profiles: List<Profile>,
    activeProfileId: Long?,
    readings: List<Reading>,
    onSelectProfile: (Long) -> Unit,
    onSaveProfile: (name: String, sex: String, birthYear: Int, heightCm: Double, colorHex: String, idToEdit: Long?) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var profileBeingEdited by remember { mutableStateOf<Profile?>(null) }
    var profileToDelete by remember { mutableStateOf<Profile?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOUSEHOLD PROFILES (${profiles.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OccultMuted,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = {
                        profileBeingEdited = null
                        showProfileDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OccultViolet,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        items(profiles, key = { it.id }) { p ->
            val isActive = p.id == activeProfileId
            val profileReadings = readings.filter { it.profileId == p.id }
            val latest = profileReadings.maxByOrNull { it.timestamp }

            val pColor = try {
                Color(android.graphics.Color.parseColor(p.colorHex))
            } catch (_: Exception) {
                OccultViolet
            }

            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isActive) pColor else OccultTileBorder,
                onClick = { onSelectProfile(p.id) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        ProfileAvatar(
                            name = p.name,
                            colorHex = p.colorHex,
                            isSelected = isActive,
                            size = 48
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = p.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OccultInk
                                )
                                if (isActive) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(pColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "ACTIVE",
                                            color = pColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${p.sex.capitalize(Locale.ROOT)} · ${p.age} y · ${p.heightCm.toInt()} cm",
                                fontSize = 12.sp,
                                color = OccultMuted
                            )
                            if (latest != null) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Latest: ${String.format(Locale.US, "%.1f", latest.weightKg)} kg" +
                                            (latest.bodyFatPct?.let { " · ${it}% fat" } ?: ""),
                                    fontSize = 11.sp,
                                    color = OccultInkSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                profileBeingEdited = p
                                showProfileDialog = true
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = OccultMuted, modifier = Modifier.size(16.dp))
                        }

                        if (profiles.size > 1) {
                            IconButton(
                                onClick = { profileToDelete = p },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = OccultRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Profile Add/Edit Dialog
    if (showProfileDialog) {
        ProfileEditDialog(
            profileToEdit = profileBeingEdited,
            onDismiss = { showProfileDialog = false },
            onSave = { name, sex, birthYear, heightCm, colorHex ->
                onSaveProfile(name, sex, birthYear, heightCm, colorHex, profileBeingEdited?.id)
                showProfileDialog = false
            }
        )
    }

    // Delete Confirmation
    profileToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            containerColor = OccultSurface,
            title = { Text("Delete Profile?", color = OccultInk) },
            text = {
                Text(
                    "Are you sure you want to delete '${p.name}' and all associated weigh-in readings? This cannot be undone.",
                    color = OccultInkSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProfile(p.id)
                        profileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OccultRed, contentColor = Color.White)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancel", color = OccultMuted)
                }
            }
        )
    }
}

@Composable
fun ProfileEditDialog(
    profileToEdit: Profile?,
    onDismiss: () -> Unit,
    onSave: (name: String, sex: String, birthYear: Int, heightCm: Double, colorHex: String) -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    var name by remember { mutableStateOf(profileToEdit?.name ?: "") }
    var sex by remember { mutableStateOf(profileToEdit?.sex ?: "male") }
    var birthYearStr by remember { mutableStateOf((profileToEdit?.birthYear ?: (currentYear - 28)).toString()) }
    var heightStr by remember { mutableStateOf((profileToEdit?.heightCm ?: 175.0).toInt().toString()) }
    var selectedColor by remember { mutableStateOf(profileToEdit?.colorHex ?: Profile.PALETTE[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OccultSurface,
        title = {
            Text(
                text = if (profileToEdit != null) "Edit Profile" else "Create Profile",
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
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OccultInk,
                        unfocusedTextColor = OccultInk,
                        focusedBorderColor = OccultViolet,
                        unfocusedBorderColor = OccultTileBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Sex selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("male" to "Male", "female" to "Female").forEach { (valKey, label) ->
                        val isSel = sex == valKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) OccultViolet else OccultTile)
                                .clickable { sex = valKey }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (isSel) Color.Black else OccultInkSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Birth Year & Height
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = birthYearStr,
                        onValueChange = { birthYearStr = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Birth Year") },
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

                    OutlinedTextField(
                        value = heightStr,
                        onValueChange = { heightStr = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Height (cm)") },
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

                // Accent Color
                Text("ACCENT COLOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OccultMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Profile.PALETTE.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isPicked = hex.equals(selectedColor, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isPicked) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPicked) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val y = birthYearStr.toIntOrNull() ?: (currentYear - 28)
                    val h = heightStr.toDoubleOrNull() ?: 175.0
                    if (name.isNotBlank()) {
                        onSave(name.trim(), sex, y, h, selectedColor)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OccultViolet, contentColor = Color.Black)
            ) {
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OccultMuted)
            }
        }
    )
}
