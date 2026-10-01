package com.example.model

import java.util.Calendar

data class Profile(
    val id: Long = 0,
    val name: String,
    val sex: String, // "male" or "female"
    val birthYear: Int, // e.g. 1995 -> age = currentYear - birthYear
    val heightCm: Double, // e.g. 175.0
    val colorHex: String = "#A06BFF", // Hex accent color
    val impedanceScale: Double = 1.0, // Per-profile raw->ohm factor (Cult Scale reports ohms directly)
    val createdAt: Long = System.currentTimeMillis()
) {
    val age: Int
        get() {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            return (currentYear - birthYear).coerceAtLeast(1)
        }

    val initial: String
        get() = name.trim().take(1).uppercase().ifEmpty { "?" }

    companion object {
        val PALETTE = listOf(
            "#A06BFF", // Violet
            "#FF5FA2", // Magenta
            "#42D6FF", // Cyan
            "#5EE0A0", // Mint
            "#FF8A4C", // Coral Orange
            "#FFCC3D", // Gold
            "#5B8CFF", // Royal Blue
            "#C6F24E"  // Acid Lime
        )
    }
}
