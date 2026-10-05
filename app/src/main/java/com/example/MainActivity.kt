package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ble.CultBleClient
import com.example.data.OccultRepository
import com.example.model.Profile
import com.example.ui.*
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.*

enum class AppTab(val title: String, val icon: ImageVector) {
    TODAY("Today", Icons.Default.Home),
    TRENDS("Trends", Icons.Default.DateRange),
    HISTORY("History", Icons.Default.List),
    PROFILES("Profiles", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: OccultRepository
    private lateinit var bleClient: CultBleClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen awake while app is in foreground to prevent timeout during weigh-ins
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        repository = OccultRepository(applicationContext)
        bleClient = CultBleClient(applicationContext)

        setContent {
            MyApplicationTheme {
                OccultApp(repository = repository, bleClient = bleClient)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleClient.stop()
    }
}

@Composable
fun OccultApp(
    repository: OccultRepository,
    bleClient: CultBleClient
) {
    val profiles by repository.profiles.collectAsState()
    val activeProfileId by repository.activeProfileId.collectAsState()
    val readings by repository.readings.collectAsState()

    val activeProfile = remember(profiles, activeProfileId) {
        profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()
    }

    val activeReadings = remember(readings, activeProfile) {
        if (activeProfile == null) emptyList()
        else readings.filter { it.profileId == activeProfile.id }.sortedBy { it.timestamp }
    }

    var currentTabName by rememberSaveable { mutableStateOf(AppTab.TODAY.name) }
    val currentTab = remember(currentTabName) {
        try { AppTab.valueOf(currentTabName) } catch (_: Exception) { AppTab.TODAY }
    }
    var showMeasureDialog by rememberSaveable { mutableStateOf(false) }
    var showManualDialog by rememberSaveable { mutableStateOf(false) }
    var showDataDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = OccultBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OccultBg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Top Brand and Sync Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(OccultViolet.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✦",
                                color = OccultViolet,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "WeightOCult",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = OccultInk,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "scale telemetry",
                            fontSize = 11.sp,
                            color = OccultMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showDataDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Data Sync",
                                tint = OccultInkSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Profile Chips Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    profiles.forEach { p ->
                        val isSelected = p.id == activeProfile?.id
                        val color = try {
                            Color(android.graphics.Color.parseColor(p.colorHex))
                        } catch (_: Exception) {
                            OccultViolet
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) color.copy(alpha = 0.2f) else OccultTile)
                                .clickable { repository.setActiveProfile(p.id) }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileAvatar(
                                name = p.name,
                                colorHex = p.colorHex,
                                isSelected = isSelected,
                                size = 22
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = p.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OccultInk else OccultMuted
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = OccultSurface,
                tonalElevation = 0.dp,
                modifier = Modifier.navigationBarsPadding().height(64.dp)
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = tab == currentTab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTabName = tab.name },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) OccultViolet else OccultMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OccultViolet else OccultMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = OccultViolet.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                AppTab.TODAY -> TodayScreen(
                    profile = activeProfile,
                    readings = activeReadings,
                    onMeasureClick = { showMeasureDialog = true },
                    onManualClick = { showManualDialog = true }
                )
                AppTab.TRENDS -> TrendsScreen(
                    profile = activeProfile,
                    readings = activeReadings
                )
                AppTab.HISTORY -> HistoryScreen(
                    profile = activeProfile,
                    readings = activeReadings,
                    onDeleteReading = { repository.deleteReading(it) }
                )
                AppTab.PROFILES -> ProfilesScreen(
                    profiles = profiles,
                    activeProfileId = activeProfile?.id,
                    readings = readings,
                    onSelectProfile = { repository.setActiveProfile(it) },
                    onSaveProfile = { name, sex, birthYear, heightCm, colorHex, idToEdit ->
                        if (idToEdit != null) {
                            val existing = profiles.find { it.id == idToEdit }
                            if (existing != null) {
                                repository.updateProfile(
                                    existing.copy(
                                        name = name,
                                        sex = sex,
                                        birthYear = birthYear,
                                        heightCm = heightCm,
                                        colorHex = colorHex
                                    )
                                )
                            }
                        } else {
                            repository.createProfile(name, sex, birthYear, heightCm, colorHex)
                        }
                    },
                    onDeleteProfile = { repository.deleteProfile(it) }
                )
            }
        }
    }

    // Measure Dialog
    if (showMeasureDialog) {
        MeasureDialog(
            bleClient = bleClient,
            profile = activeProfile,
            onDismiss = { showMeasureDialog = false },
            onReadingCaptured = { reading ->
                repository.addReading(reading)
            }
        )
    }

    // Manual Entry Dialog
    if (showManualDialog) {
        ManualEntryDialog(
            profile = activeProfile,
            onDismiss = { showManualDialog = false },
            onSave = { reading ->
                repository.addReading(reading)
                showManualDialog = false
            }
        )
    }

    // Data Management Dialog
    if (showDataDialog) {
        DataManagementDialog(
            repository = repository,
            activeProfileId = activeProfile?.id,
            onDismiss = { showDataDialog = false }
        )
    }
}
