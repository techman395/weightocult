package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OccultRepository
import com.example.ui.theme.*

@Composable
fun DataManagementDialog(
    repository: OccultRepository,
    activeProfileId: Long?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var jsonInput by remember { mutableStateOf("") }
    var showImportInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OccultSurface,
        title = {
            Text(
                "Data & Telemetry Sync",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OccultInk
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Local-first storage. No cloud accounts, no third-party tracking. All data is stored on-device in private JSON storage.",
                    color = OccultMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Divider(color = OccultTileBorder)

                // CSV Export
                Button(
                    onClick = {
                        val pid = activeProfileId ?: 1L
                        val csv = repository.exportCsv(pid)
                        clipboard.setPrimaryClip(ClipData.newPlainText("WeightOCult CSV Export", csv))
                        Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OccultTile, contentColor = OccultInk),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export Profile CSV (Clipboard)", fontSize = 12.sp)
                }

                // JSON Backup Export
                Button(
                    onClick = {
                        val json = repository.exportJson()
                        clipboard.setPrimaryClip(ClipData.newPlainText("WeightOCult JSON Backup", json))
                        Toast.makeText(context, "Full JSON backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OccultTile, contentColor = OccultInk),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export Full JSON Backup", fontSize = 12.sp)
                }

                // JSON Import
                if (!showImportInput) {
                    OutlinedButton(
                        onClick = { showImportInput = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OccultInkSecondary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(OccultTileBorder)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Import JSON Backup", fontSize = 12.sp)
                    }
                } else {
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        label = { Text("Paste JSON Payload") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OccultInk,
                            unfocusedTextColor = OccultInk,
                            focusedBorderColor = OccultViolet,
                            unfocusedBorderColor = OccultTileBorder
                        )
                    )
                    Button(
                        onClick = {
                            val success = repository.importJson(jsonInput)
                            if (success) {
                                Toast.makeText(context, "Data successfully restored!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Invalid JSON backup format", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OccultViolet, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore Data", fontWeight = FontWeight.Bold)
                    }
                }

                Divider(color = OccultTileBorder)

                // Demo Seed Button
                OutlinedButton(
                    onClick = {
                        repository.seedDemoData()
                        Toast.makeText(context, "Seeded 30-day realistic telemetry!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OccultGold),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(OccultGold.copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = OccultGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Re-seed 30-Day Demo Telemetry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = OccultViolet, contentColor = Color.Black)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
