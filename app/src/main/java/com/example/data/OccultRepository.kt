package com.example.data

import android.content.Context
import com.example.metrics.BodyComp
import com.example.model.Profile
import com.example.model.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

class OccultRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dataFile = File(context.filesDir, "occult_store.json")

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _activeProfileId = MutableStateFlow<Long?>(null)
    val activeProfileId: StateFlow<Long?> = _activeProfileId.asStateFlow()

    private val _readings = MutableStateFlow<List<Reading>>(emptyList())
    val readings: StateFlow<List<Reading>> = _readings.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!dataFile.exists()) {
            // Seed initial demo data for rich immediate experience
            seedDemoDataSync()
            return
        }

        try {
            val content = dataFile.readText()
            val root = JSONObject(content)

            val pList = mutableListOf<Profile>()
            val profilesArr = root.optJSONArray("profiles") ?: JSONArray()
            for (i in 0 until profilesArr.length()) {
                val obj = profilesArr.getJSONObject(i)
                pList.add(
                    Profile(
                        id = obj.optLong("id", i.toLong() + 1),
                        name = obj.optString("name", "User"),
                        sex = obj.optString("sex", "male"),
                        birthYear = obj.optInt("birthYear", 1995),
                        heightCm = obj.optDouble("heightCm", 175.0),
                        colorHex = obj.optString("colorHex", Profile.PALETTE[0]),
                        impedanceScale = obj.optDouble("impedanceScale", BodyComp.DEFAULT_IMPEDANCE_SCALE),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val rList = mutableListOf<Reading>()
            val readingsArr = root.optJSONArray("readings") ?: JSONArray()
            for (i in 0 until readingsArr.length()) {
                val obj = readingsArr.getJSONObject(i)
                rList.add(
                    Reading(
                        id = obj.optLong("id", i.toLong() + 1),
                        profileId = obj.optLong("profileId", 1),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        weightKg = obj.optDouble("weightKg", 70.0),
                        heartRate = if (obj.has("heartRate") && !obj.isNull("heartRate")) obj.getInt("heartRate") else null,
                        impedanceRaw = if (obj.has("impedanceRaw") && !obj.isNull("impedanceRaw")) obj.getInt("impedanceRaw") else null,
                        source = obj.optString("source", "scale"),
                        bmi = if (obj.has("bmi") && !obj.isNull("bmi")) obj.getDouble("bmi") else null,
                        bmiClass = if (obj.has("bmiClass") && !obj.isNull("bmiClass")) obj.getString("bmiClass") else null,
                        bodyFatPct = if (obj.has("bodyFatPct") && !obj.isNull("bodyFatPct")) obj.getDouble("bodyFatPct") else null,
                        fatMassKg = if (obj.has("fatMassKg") && !obj.isNull("fatMassKg")) obj.getDouble("fatMassKg") else null,
                        ffmKg = if (obj.has("ffmKg") && !obj.isNull("ffmKg")) obj.getDouble("ffmKg") else null,
                        tbwL = if (obj.has("tbwL") && !obj.isNull("tbwL")) obj.getDouble("tbwL") else null,
                        bodyWaterPct = if (obj.has("bodyWaterPct") && !obj.isNull("bodyWaterPct")) obj.getDouble("bodyWaterPct") else null,
                        bmrKcal = if (obj.has("bmrKcal") && !obj.isNull("bmrKcal")) obj.getInt("bmrKcal") else null,
                        smmKg = if (obj.has("smmKg") && !obj.isNull("smmKg")) obj.getDouble("smmKg") else null,
                        smmPct = if (obj.has("smmPct") && !obj.isNull("smmPct")) obj.getDouble("smmPct") else null
                    )
                )
            }

            val profilesMap = pList.associateBy { it.id }
            val sanitizedList = rList.map { r ->
                val needsRepair = r.smmKg == null || r.smmKg > r.weightKg || (r.smmPct != null && r.smmPct > 65.0)
                if (needsRepair) {
                    val p = profilesMap[r.profileId] ?: pList.firstOrNull()
                    if (p != null) {
                        val fixed = BodyComp.deriveReading(
                            weightKg = r.weightKg,
                            impedanceRaw = r.impedanceRaw,
                            profile = p,
                            source = r.source,
                            timestamp = r.timestamp
                        )
                        r.copy(smmKg = fixed.smmKg, smmPct = fixed.smmPct)
                    } else r
                } else r
            }

            _profiles.value = pList
            _readings.value = sanitizedList

            val savedActiveId = if (root.has("activeProfileId") && !root.isNull("activeProfileId")) {
                root.getLong("activeProfileId")
            } else {
                pList.firstOrNull()?.id
            }
            _activeProfileId.value = savedActiveId

        } catch (e: Exception) {
            e.printStackTrace()
            seedDemoDataSync()
        }
    }

    private fun persistToDisk() {
        scope.launch {
            try {
                val root = JSONObject()
                root.put("version", 1)
                root.put("activeProfileId", _activeProfileId.value ?: JSONObject.NULL)

                val pArr = JSONArray()
                for (p in _profiles.value) {
                    val obj = JSONObject()
                    obj.put("id", p.id)
                    obj.put("name", p.name)
                    obj.put("sex", p.sex)
                    obj.put("birthYear", p.birthYear)
                    obj.put("heightCm", p.heightCm)
                    obj.put("colorHex", p.colorHex)
                    obj.put("impedanceScale", p.impedanceScale)
                    obj.put("createdAt", p.createdAt)
                    pArr.put(obj)
                }
                root.put("profiles", pArr)

                val rArr = JSONArray()
                for (r in _readings.value) {
                    val obj = JSONObject()
                    obj.put("id", r.id)
                    obj.put("profileId", r.profileId)
                    obj.put("timestamp", r.timestamp)
                    obj.put("weightKg", r.weightKg)
                    obj.put("heartRate", r.heartRate ?: JSONObject.NULL)
                    obj.put("impedanceRaw", r.impedanceRaw ?: JSONObject.NULL)
                    obj.put("source", r.source)
                    obj.put("bmi", r.bmi ?: JSONObject.NULL)
                    obj.put("bmiClass", r.bmiClass ?: JSONObject.NULL)
                    obj.put("bodyFatPct", r.bodyFatPct ?: JSONObject.NULL)
                    obj.put("fatMassKg", r.fatMassKg ?: JSONObject.NULL)
                    obj.put("ffmKg", r.ffmKg ?: JSONObject.NULL)
                    obj.put("tbwL", r.tbwL ?: JSONObject.NULL)
                    obj.put("bodyWaterPct", r.bodyWaterPct ?: JSONObject.NULL)
                    obj.put("bmrKcal", r.bmrKcal ?: JSONObject.NULL)
                    obj.put("smmKg", r.smmKg ?: JSONObject.NULL)
                    obj.put("smmPct", r.smmPct ?: JSONObject.NULL)
                    rArr.put(obj)
                }
                root.put("readings", rArr)

                dataFile.writeText(root.toString(2))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setActiveProfile(profileId: Long) {
        _activeProfileId.value = profileId
        persistToDisk()
    }

    fun getActiveProfile(): Profile? {
        val id = _activeProfileId.value ?: return _profiles.value.firstOrNull()
        return _profiles.value.find { it.id == id } ?: _profiles.value.firstOrNull()
    }

    fun createProfile(
        name: String,
        sex: String,
        birthYear: Int,
        heightCm: Double,
        colorHex: String = Profile.PALETTE[0]
    ): Profile {
        val newId = (System.currentTimeMillis() % 1_000_000) + _profiles.value.size + 1
        val newProfile = Profile(
            id = newId,
            name = name,
            sex = sex,
            birthYear = birthYear,
            heightCm = heightCm,
            colorHex = colorHex
        )
        val updated = _profiles.value + newProfile
        _profiles.value = updated
        _activeProfileId.value = newProfile.id
        persistToDisk()
        return newProfile
    }

    fun updateProfile(profile: Profile) {
        _profiles.value = _profiles.value.map { if (it.id == profile.id) profile else it }
        persistToDisk()
    }

    fun deleteProfile(profileId: Long) {
        _profiles.value = _profiles.value.filter { it.id != profileId }
        _readings.value = _readings.value.filter { it.profileId != profileId }
        if (_activeProfileId.value == profileId) {
            _activeProfileId.value = _profiles.value.firstOrNull()?.id
        }
        persistToDisk()
    }

    fun addReading(reading: Reading) {
        val newId = (System.currentTimeMillis() % 10_000_000) + _readings.value.size + 1
        val newReading = reading.copy(id = newId)
        _readings.value = _readings.value + newReading
        persistToDisk()
    }

    fun deleteReading(readingId: Long) {
        _readings.value = _readings.value.filter { it.id != readingId }
        persistToDisk()
    }

    fun getReadingsForProfile(profileId: Long): List<Reading> {
        return _readings.value.filter { it.profileId == profileId }.sortedBy { it.timestamp }
    }

    fun seedDemoData() {
        seedDemoDataSync()
    }

    private fun seedDemoDataSync() {
        val alex = Profile(
            id = 1,
            name = "Alex Vance",
            sex = "male",
            birthYear = 1996,
            heightCm = 182.0,
            colorHex = "#A06BFF"
        )
        val elena = Profile(
            id = 2,
            name = "Elena Rostova",
            sex = "female",
            birthYear = 1998,
            heightCm = 168.0,
            colorHex = "#FF5FA2"
        )

        val seededReadings = mutableListOf<Reading>()
        val now = System.currentTimeMillis()
        val oneDayMillis = 86_400_000L

        // 30 days of data for Alex (starting ~79.2kg, drifting down to ~76.4kg)
        var wAlex = 79.2
        for (i in 29 downTo 0) {
            val ts = now - (i * oneDayMillis) + Random.nextLong(0, 3600_000)
            val drift = Random.nextDouble(-0.35, 0.25)
            wAlex = (wAlex + drift).coerceIn(75.5, 80.0)
            val roundedW = kotlin.math.round(wAlex * 10.0) / 10.0
            val hr = Random.nextInt(63, 72)
            val imp = Random.nextInt(485, 520)

            val derived = BodyComp.deriveReading(
                weightKg = roundedW,
                impedanceRaw = imp,
                profile = alex,
                source = if (i % 5 == 0) "manual" else "scale",
                timestamp = ts
            )
            seededReadings.add(derived.copy(id = (1000 + i).toLong(), heartRate = hr))
        }

        // 14 days of data for Elena (starting ~59.5kg, drifting down to ~57.8kg)
        var wElena = 59.5
        for (i in 14 downTo 0) {
            val ts = now - (i * oneDayMillis) + Random.nextLong(0, 3600_000)
            val drift = Random.nextDouble(-0.25, 0.18)
            wElena = (wElena + drift).coerceIn(57.0, 60.5)
            val roundedW = kotlin.math.round(wElena * 10.0) / 10.0
            val hr = Random.nextInt(66, 75)
            val imp = Random.nextInt(520, 560)

            val derived = BodyComp.deriveReading(
                weightKg = roundedW,
                impedanceRaw = imp,
                profile = elena,
                source = "scale",
                timestamp = ts
            )
            seededReadings.add(derived.copy(id = (2000 + i).toLong(), heartRate = hr))
        }

        _profiles.value = listOf(alex, elena)
        _activeProfileId.value = alex.id
        _readings.value = seededReadings
        persistToDisk()
    }

    /**
     * Export all data as JSON string matching Occult format.
     */
    fun exportJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date()))
        root.put("activeProfileId", _activeProfileId.value ?: JSONObject.NULL)

        val pArr = JSONArray()
        for (p in _profiles.value) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("sex", p.sex)
            obj.put("birthYear", p.birthYear)
            obj.put("heightCm", p.heightCm)
            obj.put("colorHex", p.colorHex)
            obj.put("impedanceScale", p.impedanceScale)
            obj.put("createdAt", p.createdAt)
            pArr.put(obj)
        }
        root.put("profiles", pArr)

        val rArr = JSONArray()
        for (r in _readings.value) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("profileId", r.profileId)
            obj.put("timestamp", r.timestamp)
            obj.put("weightKg", r.weightKg)
            obj.put("heartRate", r.heartRate ?: JSONObject.NULL)
            obj.put("impedanceRaw", r.impedanceRaw ?: JSONObject.NULL)
            obj.put("source", r.source)
            obj.put("bmi", r.bmi ?: JSONObject.NULL)
            obj.put("bmiClass", r.bmiClass ?: JSONObject.NULL)
            obj.put("bodyFatPct", r.bodyFatPct ?: JSONObject.NULL)
            obj.put("fatMassKg", r.fatMassKg ?: JSONObject.NULL)
            obj.put("ffmKg", r.ffmKg ?: JSONObject.NULL)
            obj.put("tbwL", r.tbwL ?: JSONObject.NULL)
            obj.put("bodyWaterPct", r.bodyWaterPct ?: JSONObject.NULL)
            obj.put("bmrKcal", r.bmrKcal ?: JSONObject.NULL)
            obj.put("smmKg", r.smmKg ?: JSONObject.NULL)
            obj.put("smmPct", r.smmPct ?: JSONObject.NULL)
            rArr.put(obj)
        }
        root.put("readings", rArr)

        return root.toString(2)
    }

    /**
     * Import JSON backup.
     */
    fun importJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val pList = mutableListOf<Profile>()
            val profilesArr = root.optJSONArray("profiles") ?: JSONArray()
            for (i in 0 until profilesArr.length()) {
                val obj = profilesArr.getJSONObject(i)
                pList.add(
                    Profile(
                        id = obj.optLong("id", i.toLong() + 1),
                        name = obj.optString("name", "User"),
                        sex = obj.optString("sex", "male"),
                        birthYear = obj.optInt("birthYear", 1995),
                        heightCm = obj.optDouble("heightCm", 175.0),
                        colorHex = obj.optString("colorHex", Profile.PALETTE[0]),
                        impedanceScale = obj.optDouble("impedanceScale", BodyComp.DEFAULT_IMPEDANCE_SCALE),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val rList = mutableListOf<Reading>()
            val readingsArr = root.optJSONArray("readings") ?: JSONArray()
            for (i in 0 until readingsArr.length()) {
                val obj = readingsArr.getJSONObject(i)
                rList.add(
                    Reading(
                        id = obj.optLong("id", i.toLong() + 1),
                        profileId = obj.optLong("profileId", 1),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        weightKg = obj.optDouble("weightKg", 70.0),
                        heartRate = if (obj.has("heartRate") && !obj.isNull("heartRate")) obj.getInt("heartRate") else null,
                        impedanceRaw = if (obj.has("impedanceRaw") && !obj.isNull("impedanceRaw")) obj.getInt("impedanceRaw") else null,
                        source = obj.optString("source", "scale"),
                        bmi = if (obj.has("bmi") && !obj.isNull("bmi")) obj.getDouble("bmi") else null,
                        bmiClass = if (obj.has("bmiClass") && !obj.isNull("bmiClass")) obj.getString("bmiClass") else null,
                        bodyFatPct = if (obj.has("bodyFatPct") && !obj.isNull("bodyFatPct")) obj.getDouble("bodyFatPct") else null,
                        fatMassKg = if (obj.has("fatMassKg") && !obj.isNull("fatMassKg")) obj.getDouble("fatMassKg") else null,
                        ffmKg = if (obj.has("ffmKg") && !obj.isNull("ffmKg")) obj.getDouble("ffmKg") else null,
                        tbwL = if (obj.has("tbwL") && !obj.isNull("tbwL")) obj.getDouble("tbwL") else null,
                        bodyWaterPct = if (obj.has("bodyWaterPct") && !obj.isNull("bodyWaterPct")) obj.getDouble("bodyWaterPct") else null,
                        bmrKcal = if (obj.has("bmrKcal") && !obj.isNull("bmrKcal")) obj.getInt("bmrKcal") else null,
                        smmKg = if (obj.has("smmKg") && !obj.isNull("smmKg")) obj.getDouble("smmKg") else null,
                        smmPct = if (obj.has("smmPct") && !obj.isNull("smmPct")) obj.getDouble("smmPct") else null
                    )
                )
            }

            if (pList.isNotEmpty()) {
                val profilesMap = pList.associateBy { it.id }
                val sanitizedList = rList.map { r ->
                    val needsRepair = r.smmKg == null || r.smmKg > r.weightKg || (r.smmPct != null && r.smmPct > 65.0)
                    if (needsRepair) {
                        val p = profilesMap[r.profileId] ?: pList.firstOrNull()
                        if (p != null) {
                            val fixed = BodyComp.deriveReading(
                                weightKg = r.weightKg,
                                impedanceRaw = r.impedanceRaw,
                                profile = p,
                                source = r.source,
                                timestamp = r.timestamp
                            )
                            r.copy(smmKg = fixed.smmKg, smmPct = fixed.smmPct)
                        } else r
                    } else r
                }
                _profiles.value = pList
                _readings.value = sanitizedList
                _activeProfileId.value = pList.first().id
                persistToDisk()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Export readings to standard CSV format.
     */
    fun exportCsv(profileId: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val sb = StringBuilder()
        sb.append("ts,weightKg,heartRate,impedanceRaw,bmi,bodyFatPct,fatMassKg,ffmKg,tbwL,bodyWaterPct,bmrKcal,smmKg,smmPct,source\n")
        val list = getReadingsForProfile(profileId)
        for (r in list) {
            sb.append(sdf.format(Date(r.timestamp))).append(",")
            sb.append(r.weightKg).append(",")
            sb.append(r.heartRate ?: "").append(",")
            sb.append(r.impedanceRaw ?: "").append(",")
            sb.append(r.bmi ?: "").append(",")
            sb.append(r.bodyFatPct ?: "").append(",")
            sb.append(r.fatMassKg ?: "").append(",")
            sb.append(r.ffmKg ?: "").append(",")
            sb.append(r.tbwL ?: "").append(",")
            sb.append(r.bodyWaterPct ?: "").append(",")
            sb.append(r.bmrKcal ?: "").append(",")
            sb.append(r.smmKg ?: "").append(",")
            sb.append(r.smmPct ?: "").append(",")
            sb.append(r.source)
            sb.append("\n")
        }
        return sb.toString()
    }
}
