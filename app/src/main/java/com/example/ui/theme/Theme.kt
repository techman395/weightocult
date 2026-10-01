package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OccultDarkColorScheme = darkColorScheme(
    primary = OccultViolet,
    onPrimary = OccultBg,
    primaryContainer = OccultTile,
    onPrimaryContainer = OccultVioletLight,
    secondary = OccultMagenta,
    onSecondary = OccultBg,
    tertiary = OccultCyan,
    onTertiary = OccultBg,
    background = OccultBg,
    onBackground = OccultInk,
    surface = OccultSurface,
    onSurface = OccultInk,
    surfaceVariant = OccultTile,
    onSurfaceVariant = OccultInkSecondary,
    outline = OccultTileBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OccultDarkColorScheme,
        typography = Typography,
        content = content
    )
}
