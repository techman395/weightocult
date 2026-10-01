package com.example.ble

import kotlin.math.round

enum class CultPhase {
    WEIGH,
    BODY
}

data class CultParsedFrame(
    val phase: CultPhase,
    val weightKg: Double,
    val heartRate: Int?,
    val impedanceRaw: Int?,
    val checksumOk: Boolean
)

object CultScaleParser {

    const val SCALE_SERVICE_UUID: String = "0000fff0-0000-1000-8000-00805f9b34fb"
    const val SCALE_CHAR_NOTIFY_UUID: String = "0000fff4-0000-1000-8000-00805f9b34fb"

    /**
     * Parse an 11-byte frame from the Cult Smart Scale.
     */
    fun parseCultFrame(bytes: ByteArray): CultParsedFrame? {
        if (bytes.size != 11) return null
        if ((bytes[0].toInt() and 0xFF) != 0xCF) return null

        var xor = 0
        for (i in 0 until 10) {
            xor = xor xor (bytes[i].toInt() and 0xFF)
        }
        val checksumOk = xor == (bytes[10].toInt() and 0xFF)

        val rawWeight = (bytes[3].toInt() and 0xFF) or ((bytes[4].toInt() and 0xFF) shl 8)
        val weightKg = round((rawWeight.toDouble() / 100.0) * 100.0) / 100.0

        val isWeigh = (bytes[9].toInt() and 0xFF) == 0x01
        return if (isWeigh) {
            CultParsedFrame(
                phase = CultPhase.WEIGH,
                weightKg = weightKg,
                heartRate = null,
                impedanceRaw = null,
                checksumOk = checksumOk
            )
        } else {
            val rawHr = bytes[1].toInt() and 0xFF
            val heartRate = if (rawHr > 0) rawHr else null

            val rawImp = (bytes[5].toInt() and 0xFF) or ((bytes[6].toInt() and 0xFF) shl 8)
            val impedanceRaw = if (rawImp > 0) rawImp else null

            CultParsedFrame(
                phase = CultPhase.BODY,
                weightKg = weightKg,
                heartRate = heartRate,
                impedanceRaw = impedanceRaw,
                checksumOk = checksumOk
            )
        }
    }

    /**
     * Self-test verifying against captured frames from cult_scale_protocol.md.
     */
    fun cultSelfTest(): Boolean {
        var ok = true

        // Weigh frame: cf 00 00 4c 1d 00 00 00 00 01 9f (75.0 kg)
        val weighBytes = byteArrayOf(
            0xCF.toByte(), 0x00, 0x00, 0x4C, 0x1D,
            0x00, 0x00, 0x00, 0x00, 0x01, 0x9F.toByte()
        )
        val weigh = parseCultFrame(weighBytes)
        ok = ok && (weigh != null &&
                weigh.phase == CultPhase.WEIGH &&
                weigh.weightKg == 75.0 &&
                weigh.heartRate == null &&
                weigh.impedanceRaw == null &&
                weigh.checksumOk)

        // Body frame: cf 3c c0 4c 1d f4 01 00 00 00 97 (75.0 kg, HR 60, Imp 500)
        val bodyBytes = byteArrayOf(
            0xCF.toByte(), 0x3C, 0xC0.toByte(), 0x4C, 0x1D,
            0xF4.toByte(), 0x01, 0x00, 0x00, 0x00, 0x97.toByte()
        )
        val body = parseCultFrame(bodyBytes)
        ok = ok && (body != null &&
                body.phase == CultPhase.BODY &&
                body.weightKg == 75.0 &&
                body.heartRate == 60 &&
                body.impedanceRaw == 500 &&
                body.checksumOk)

        // Corrupted frame
        val corruptBytes = byteArrayOf(
            0xCF.toByte(), 0x00, 0x00, 0x92.toByte(), 0x27,
            0x00, 0x00, 0x00, 0x00, 0x01, 0x00
        )
        val corrupt = parseCultFrame(corruptBytes)
        ok = ok && (corrupt != null && !corrupt.checksumOk)

        return ok
    }
}
