package com.example.metrics

import com.example.model.Profile
import com.example.model.Reading
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Peer-reviewed body-composition equations ported and calibrated for Cult Smart Scale.
 *
 * References:
 *   Mifflin MD et al. Am J Clin Nutr 1990;51:241-247 (BMR).
 *   Deurenberg P et al. Br J Nutr 1991;65:105-114 (Body Fat % from BMI).
 *   Watson PE et al. Am J Clin Nutr 1980;33:27-39 (Total Body Water).
 *   Janssen I et al. J Appl Physiol 2000;89:465-471 (Skeletal Muscle Mass vs MRI).
 *
 * Disclaimer: Metrics are not clinically validated and are not medical advice.
 * Bioimpedance is noisy — treat as trends, not absolute clinical diagnosis.
 */
object BodyComp {

    const val DEFAULT_IMPEDANCE_SCALE: Double = 1.0

    /** BMI (kg/m²). Ref: WHO classification. */
    fun bmi(weightKg: Double, heightCm: Double): Double {
        val h = heightCm / 100.0
        return weightKg / (h * h)
    }

    /** WHO BMI classification string. */
    fun bmiClass(b: Double): String {
        return when {
            b < 18.5 -> "underweight"
            b < 25.0 -> "normal"
            b < 30.0 -> "overweight"
            b < 35.0 -> "obese (class I)"
            b < 40.0 -> "obese (class II)"
            else -> "obese (class III)"
        }
    }

    /** Mifflin-St Jeor 1990 resting metabolic rate (kcal/day). */
    fun bmrMifflin(weightKg: Double, heightCm: Double, age: Int, sex: String): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return if (sex.equals("male", ignoreCase = true)) base + 5.0 else base - 161.0
    }

    /** Deurenberg 1991 BMI-based body-fat %. */
    fun bodyFatDeurenberg(b: Double, age: Int, sex: String): Double {
        val isMale = if (sex.equals("male", ignoreCase = true)) 1.0 else 0.0
        return 1.20 * b + 0.23 * age - 10.8 * isMale - 5.4
    }

    /** Watson 1980 total body water (litres). */
    fun tbwWatson(weightKg: Double, heightCm: Double, age: Int, sex: String): Double {
        return if (sex.equals("male", ignoreCase = true)) {
            2.447 - 0.09156 * age + 0.1074 * heightCm + 0.3362 * weightKg
        } else {
            -2.097 + 0.1069 * heightCm + 0.2466 * weightKg
        }
    }

    /**
     * Resolve bioimpedance resistance in Ohms (Ω) from scale packet.
     * Cult Smart Scale transmits resistance in Ohms directly (typically 400 - 800 Ω).
     * If a scale sends high ADC raw counts (> 2000), scales by 0.03036.
     */
    fun resolveResistanceOhms(raw: Int, profileScale: Double): Double {
        return when {
            profileScale > 0.0 && profileScale != 1.0 && profileScale != 0.03036 -> {
                raw * profileScale
            }
            raw > 2000 -> {
                raw * 0.03036
            }
            else -> {
                raw.toDouble()
            }
        }
    }

    /**
     * Janssen 2000 skeletal-muscle mass (kg) from 50 kHz resistance.
     * Formula: SMM (kg) = [(Ht² / R) * 0.401] + (sex * 3.825) - (age * 0.071) + 5.102
     * where sex = 1 for male, 0 for female.
     */
    fun smmJanssen(heightCm: Double, resistanceOhms: Double, age: Int, sex: String): Double {
        val isMale = if (sex.equals("male", ignoreCase = true)) 3.825 else 0.0
        return ((heightCm * heightCm) / resistanceOhms) * 0.401 + isMale - age * 0.071 + 5.102
    }

    private fun r1(v: Double?): Double? {
        if (v == null) return null
        return (v * 10.0).roundToInt() / 10.0
    }

    private fun ri(v: Double?): Int? {
        return v?.roundToInt()
    }

    /**
     * Derive a full body-composition snapshot from raw reading data and user profile.
     */
    fun deriveReading(
        weightKg: Double,
        impedanceRaw: Int?,
        profile: Profile,
        source: String = "scale",
        timestamp: Long = System.currentTimeMillis()
    ): Reading {
        val age = profile.age
        val sex = profile.sex
        val isMale = sex.equals("male", ignoreCase = true)
        val heightCm = profile.heightCm

        val bmiVal = r1(bmi(weightKg, heightCm))
        val bmiClassVal = bmiVal?.let { bmiClass(it) }

        val bodyFatPct = bmiVal?.let { r1(bodyFatDeurenberg(it, age, sex)) }
        val fatMassKg = bodyFatPct?.let { r1(weightKg * it / 100.0) }
        val ffmKg = fatMassKg?.let { r1(weightKg - it) }

        val tbwL = r1(tbwWatson(weightKg, heightCm, age, sex))
        val bodyWaterPct = tbwL?.let { r1(it / weightKg * 100.0) }

        val bmrKcal = ri(bmrMifflin(weightKg, heightCm, age, sex))

        var smmKg: Double? = null
        var smmPct: Double? = null

        if (impedanceRaw != null && impedanceRaw > 0) {
            val resistanceOhms = resolveResistanceOhms(impedanceRaw, profile.impedanceScale)
            if (resistanceOhms in 150.0..1500.0) {
                val rawSmm = smmJanssen(heightCm, resistanceOhms, age, sex)
                // Physiological bounds check: SMM should be realistic fraction of FFM and body weight
                val maxAllowable = ffmKg ?: (weightKg * 0.60)
                val minAllowable = weightKg * 0.18
                val clamped = rawSmm.coerceIn(minAllowable, maxAllowable)
                smmKg = r1(clamped)
                smmPct = r1(clamped / weightKg * 100.0)
            }
        }

        // Fallback estimation when impedance contact is unavailable (manual weigh-in or socks):
        if (smmKg == null && ffmKg != null && ffmKg > 0.0) {
            // Skeletal muscle mass accounts for ~54% of FFM in men and ~50% in women (Heymsfield et al.)
            val factor = if (isMale) 0.54 else 0.50
            val estSmm = (ffmKg * factor).coerceIn(weightKg * 0.20, weightKg * 0.55)
            smmKg = r1(estSmm)
            smmPct = r1(estSmm / weightKg * 100.0)
        }

        return Reading(
            profileId = profile.id,
            timestamp = timestamp,
            weightKg = (weightKg * 10.0).roundToInt() / 10.0,
            heartRate = null,
            impedanceRaw = impedanceRaw,
            source = source,
            bmi = bmiVal,
            bmiClass = bmiClassVal,
            bodyFatPct = bodyFatPct,
            fatMassKg = fatMassKg,
            ffmKg = ffmKg,
            tbwL = tbwL,
            bodyWaterPct = bodyWaterPct,
            bmrKcal = bmrKcal,
            smmKg = smmKg,
            smmPct = smmPct
        )
    }

    /**
     * Self-test validating reference assertions from peer-reviewed literature.
     */
    fun bodyCompSelfTest(): Boolean {
        var ok = true
        // BMI: 100 kg / (2.00 m)² = 25.0
        ok = ok && (abs(bmi(100.0, 200.0) - 25.0) < 1e-9)
        // Mifflin male 80 kg / 180 cm / 30 y: 800 + 1125 − 150 + 5 = 1780
        ok = ok && (abs(bmrMifflin(80.0, 180.0, 30, "male") - 1780.0) < 1e-9)
        // Mifflin female same params: 800 + 1125 − 150 − 161 = 1614
        ok = ok && (abs(bmrMifflin(80.0, 180.0, 30, "female") - 1614.0) < 1e-9)
        // Deurenberg: 1.20×25 + 0.23×40 − 10.8×1 − 5.4 = 30 + 9.2 − 10.8 − 5.4 = 23.0
        ok = ok && (abs(bodyFatDeurenberg(25.0, 40, "male") - 23.0) < 1e-9)
        // Janssen male 180 cm, 500 ohms, 30 y:
        // (180^2 / 500) * 0.401 + 3.825 - 30 * 0.071 + 5.102 = 64.8 * 0.401 + 3.825 - 2.13 + 5.102 = 32.78 kg
        val testSmm = smmJanssen(180.0, 500.0, 30, "male")
        ok = ok && (abs(testSmm - 32.7818) < 1e-3)
        // Cult Smart Scale Ohm resolution
        ok = ok && (resolveResistanceOhms(500, 1.0) == 500.0)
        return ok
    }
}
