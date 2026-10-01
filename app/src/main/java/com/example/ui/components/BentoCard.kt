package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    borderColor: Color = OccultTileBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(OccultTile)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
            .then(clickableMod)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun DeltaChip(
    current: Double?,
    previous: Double?,
    modifier: Modifier = Modifier,
    unit: String = "",
    higherIsBetter: Boolean = false
) {
    if (current == null || previous == null) return
    val diff = current - previous
    if (kotlin.math.abs(diff) < 0.05) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(OccultSurface)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "· steady",
                color = OccultMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
        return
    }

    val arrow = if (diff > 0) "▲" else "▼"
    val isFavorable = if (higherIsBetter) diff > 0 else diff < 0
    val chipColor = if (isFavorable) OccultMint else OccultWarn
    val chipBg = chipColor.copy(alpha = 0.14f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(chipBg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$arrow ${String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(diff))}$unit",
            color = chipColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun BmiTag(bmiClass: String?, modifier: Modifier = Modifier) {
    if (bmiClass.isNullOrBlank()) return
    val (bgColor, textColor) = when (bmiClass.lowercase()) {
        "normal" -> OccultMint.copy(alpha = 0.16f) to OccultMint
        "underweight" -> OccultCyan.copy(alpha = 0.16f) to OccultCyan
        "overweight" -> OccultWarn.copy(alpha = 0.16f) to OccultWarn
        else -> OccultRed.copy(alpha = 0.16f) to OccultRed
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = bmiClass.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ProfileAvatar(
    name: String,
    colorHex: String,
    isSelected: Boolean = false,
    size: Int = 40,
    onClick: (() -> Unit)? = null
) {
    val initial = name.trim().take(1).uppercase().ifEmpty { "?" }
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        OccultViolet
    }

    val borderMod = if (isSelected) {
        Modifier.border(2.dp, parsedColor, CircleShape)
    } else {
        Modifier.border(1.dp, OccultTileBorder, CircleShape)
    }

    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .then(borderMod)
            .padding(if (isSelected) 3.dp else 0.dp)
            .clip(CircleShape)
            .background(parsedColor.copy(alpha = if (isSelected) 0.85f else 0.35f))
            .then(clickMod)
    ) {
        Text(
            text = initial,
            color = if (isSelected) Color.Black else OccultInk,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.42).sp
        )
    }
}
