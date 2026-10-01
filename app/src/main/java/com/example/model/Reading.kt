package com.example.model

data class Reading(
    val id: Long = 0,
    val profileId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val weightKg: Double,
    val heartRate: Int? = null,
    val impedanceRaw: Int? = null,
    val source: String = "scale", // "scale", "manual", "simulator", "seed"

    // Derived body composition snapshot
    val bmi: Double? = null,
    val bmiClass: String? = null, // "underweight", "normal", "overweight", "obese (class I)", etc.
    val bodyFatPct: Double? = null,
    val fatMassKg: Double? = null,
    val ffmKg: Double? = null, // Fat-free mass
    val tbwL: Double? = null, // Total body water
    val bodyWaterPct: Double? = null,
    val bmrKcal: Int? = null, // Resting metabolic rate
    val smmKg: Double? = null, // Skeletal muscle mass
    val smmPct: Double? = null
)
