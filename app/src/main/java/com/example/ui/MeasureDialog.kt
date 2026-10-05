package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ble.CultBleClient
import com.example.ble.ScaleStatus
import com.example.metrics.BodyComp
import com.example.model.Profile
import com.example.model.Reading
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MeasureDialog(
    bleClient: CultBleClient,
    profile: Profile?,
    onDismiss: () -> Unit,
    onReadingCaptured: (Reading) -> Unit
) {
    val context = LocalContext.current
    val status by bleClient.status.collectAsState()
    val liveWeight by bleClient.liveWeight.collectAsState()
    val liveHeartRate by bleClient.liveHeartRate.collectAsState()

    // Keep screen awake while dialog is open so weigh-in progress never resets
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            // Screen awake remains safely managed
        }
    }

    val profileColor = try {
        Color(android.graphics.Color.parseColor(profile?.colorHex ?: "#A06BFF"))
    } catch (_: Exception) {
        OccultViolet
    }

    // Permission launcher for Bluetooth
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.values.all { it }
        if (allGranted) {
            bleClient.startRealScaleConnection(profile)
        }
    }

    fun startConnecting() {
        val hasPermissions = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasPermissions) {
            bleClient.startRealScaleConnection(profile)
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    // Auto-initiate BLE scanning on launch
    LaunchedEffect(Unit) {
        startConnecting()
    }

    // Auto-save when measurement is successfully completed
    LaunchedEffect(status) {
        if (status is ScaleStatus.Completed && profile != null) {
            val comp = status as ScaleStatus.Completed
            val derived = BodyComp.deriveReading(
                weightKg = comp.weightKg,
                impedanceRaw = comp.impedanceRaw,
                profile = profile,
                source = "scale"
            )
            val fullReading = derived.copy(heartRate = comp.heartRate)
            delay(900)
            onReadingCaptured(fullReading)
            delay(600)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            bleClient.stop()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = OccultSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (status is ScaleStatus.Error) OccultRed.copy(alpha = 0.5f)
                        else profileColor.copy(alpha = 0.4f)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Cult Scale emblem
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(profileColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✦", color = profileColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "CULT SMART SCALE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OccultInk,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Profile: ${profile?.name ?: "User"}",
                                    fontSize = 11.sp,
                                    color = OccultMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                bleClient.stop()
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = OccultMuted)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // If Error State: Show clear Could Not Connect message
                    if (status is ScaleStatus.Error) {
                        val err = status as ScaleStatus.Error
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(OccultRed.copy(alpha = 0.1f))
                                .border(1.dp, OccultRed.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(OccultRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.BluetoothDisabled,
                                    contentDescription = null,
                                    tint = OccultRed,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Could not connect to Cult Smart Scale",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OccultRed,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = err.message,
                                fontSize = 12.sp,
                                color = OccultInkSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )

                            Spacer(Modifier.height(14.dp))

                            // Troubleshooting bullet checklist
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OccultSurface)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TipRow("Step on the scale to wake up the LED display.")
                                TipRow("Keep phone within 2 meters of the scale.")
                                TipRow("Ensure the official Cult app is closed.")
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { startConnecting() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = profileColor,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Try Again", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    bleClient.startSimulation(profile)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OccultInkSecondary),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(OccultTileBorder)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Test Simulator", fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Normal Measuring Telemetry Dial
                        Box(
                            modifier = Modifier.size(230.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 10.dp.toPx()
                                val diameter = size.minDimension - strokeWidth
                                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                                val arcSize = Size(diameter, diameter)

                                // Track background
                                drawArc(
                                    color = OccultTileBorder,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                val progressSweep = when (status) {
                                    is ScaleStatus.Idle -> 0f
                                    is ScaleStatus.Scanning -> 60f
                                    is ScaleStatus.Connecting -> 110f
                                    is ScaleStatus.Measuring -> 160f
                                    is ScaleStatus.Locked -> 230f
                                    is ScaleStatus.Analyzing -> 230f + ((status as ScaleStatus.Analyzing).progress * 100f)
                                    is ScaleStatus.Completed -> 360f
                                    is ScaleStatus.Error -> 0f
                                }

                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(profileColor.copy(alpha = 0.5f), profileColor, OccultCyan)
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = progressSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", liveWeight),
                                    fontSize = 50.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OccultInk,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "KG",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = profileColor,
                                    letterSpacing = 1.sp
                                )

                                if (liveHeartRate != null && liveHeartRate!! > 0) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = ColorHeart,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "$liveHeartRate bpm",
                                            color = OccultInk,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Status message
                        val statusText = when (status) {
                            is ScaleStatus.Idle -> "Ready to pair with scale"
                            is ScaleStatus.Scanning -> "Searching for Cult Smart Scale (0xFFF0)…\nStep on scale to wake it"
                            is ScaleStatus.Connecting -> "Connecting to Cult scale GATT telemetry…"
                            is ScaleStatus.Measuring -> "Scale connected! Step on and hold still…"
                            is ScaleStatus.Locked -> "Weight locked — stay on, reading heart rate & bioimpedance…"
                            is ScaleStatus.Analyzing -> "Reading bioimpedance & cardiac frequency…"
                            is ScaleStatus.Completed -> "Measurement complete ✓"
                            is ScaleStatus.Error -> ""
                        }

                        Text(
                            text = statusText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (status) {
                                is ScaleStatus.Completed -> OccultMint
                                else -> OccultInkSecondary
                            },
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { startConnecting() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = profileColor,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (status is ScaleStatus.Scanning) "Scanning…" else "Reconnect Scale",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    bleClient.startSimulation(profile)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OccultInkSecondary),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(OccultTileBorder)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Simulator", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TipRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("•", color = OccultMuted, fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Text(text, color = OccultMuted, fontSize = 11.sp)
    }
}
