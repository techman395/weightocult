package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.example.model.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

sealed class ScaleStatus {
    object Idle : ScaleStatus()
    object Scanning : ScaleStatus()
    object Connecting : ScaleStatus()
    data class Measuring(val weightKg: Double, val phase: CultPhase) : ScaleStatus()
    data class Locked(val weightKg: Double) : ScaleStatus()
    data class Analyzing(val weightKg: Double, val heartRate: Int?, val progress: Float) : ScaleStatus()
    data class Completed(val weightKg: Double, val heartRate: Int?, val impedanceRaw: Int?) : ScaleStatus()
    data class Error(val message: String, val canRetry: Boolean = true) : ScaleStatus()
}

class CultBleClient(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _status = MutableStateFlow<ScaleStatus>(ScaleStatus.Idle)
    val status: StateFlow<ScaleStatus> = _status.asStateFlow()

    private val _liveWeight = MutableStateFlow(0.0)
    val liveWeight: StateFlow<Double> = _liveWeight.asStateFlow()

    private val _liveHeartRate = MutableStateFlow<Int?>(null)
    val liveHeartRate: StateFlow<Int?> = _liveHeartRate.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var isSimulating = false
    private var simulationJob: Job? = null
    private var scanCallback: ScanCallback? = null

    // For BLE frame repeat detection (weight stabilization & HR convergence)
    private var lastWeight = 0.0
    private var weightRepeatCount = 0
    private var weightLocked = false
    private var lastHeartRate: Int? = null
    private var hrRepeatCount = 0
    private var latestImpedance: Int? = null
    private var isCompleted = false

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        bluetoothManager?.adapter
    }

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter != null && bluetoothAdapter!!.isEnabled

    /**
     * Start connecting to the Cult Smart Scale over real Bluetooth LE.
     * If the scale is not found or cannot connect, it explicitly reports a connection failure.
     */
    fun startRealScaleConnection(activeProfile: Profile?) {
        stop()

        if (bluetoothAdapter == null) {
            _status.value = ScaleStatus.Error(
                "This device does not have Bluetooth hardware available.",
                canRetry = false
            )
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            _status.value = ScaleStatus.Error(
                "Bluetooth is turned off. Please enable Bluetooth on your phone and wake the scale.",
                canRetry = true
            )
            return
        }

        try {
            startBleScan()
        } catch (e: SecurityException) {
            _status.value = ScaleStatus.Error(
                "Bluetooth permissions not granted. Please allow Nearby Devices permission in system settings.",
                canRetry = true
            )
        } catch (e: Exception) {
            _status.value = ScaleStatus.Error(
                "Could not connect: ${e.message ?: "Bluetooth initialization error"}",
                canRetry = true
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _status.value = ScaleStatus.Error(
                "Bluetooth LE scanner is unavailable. Please check Bluetooth settings.",
                canRetry = true
            )
            return
        }

        _status.value = ScaleStatus.Scanning
        weightLocked = false
        weightRepeatCount = 0
        lastWeight = 0.0
        lastHeartRate = null
        hrRepeatCount = 0
        latestImpedance = null
        isCompleted = false

        // Scan filters matching the Cult Smart Scale
        val scaleServiceUuid = ParcelUuid(UUID.fromString(CultScaleParser.SCALE_SERVICE_UUID))
        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(scaleServiceUuid).build(),
            ScanFilter.Builder().setDeviceName("Cult").build(),
            ScanFilter.Builder().setDeviceName("CULT").build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val device = result?.device ?: return
                val record = result.scanRecord
                val devName = device.name ?: record?.deviceName ?: ""

                val matchesUuid = record?.serviceUuids?.contains(scaleServiceUuid) == true
                val matchesName = devName.contains("Cult", ignoreCase = true) ||
                        devName.contains("Scale", ignoreCase = true)

                if (matchesUuid || matchesName) {
                    try {
                        scanner.stopScan(this)
                    } catch (_: Exception) {}
                    scanCallback = null
                    mainHandler.removeCallbacksAndMessages(null)
                    connectToScale(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _status.value = ScaleStatus.Error(
                    "Bluetooth scan failed (error code $errorCode). Please toggle Bluetooth and try again.",
                    canRetry = true
                )
            }
        }

        scanCallback = callback

        try {
            // Also listen without strict filter to catch devices with manufacturer data without declared service UUID
            scanner.startScan(null, settings, callback)

            // Timeout after 12 seconds if scale is not detected
            mainHandler.postDelayed({
                if (_status.value is ScaleStatus.Scanning) {
                    try {
                        scanner.stopScan(callback)
                    } catch (_: Exception) {}
                    scanCallback = null
                    _status.value = ScaleStatus.Error(
                        "Could not connect to Cult Smart Scale.\n\nMake sure the scale is awake (step on it to illuminate the LED display), close to your phone, and not paired with another app.",
                        canRetry = true
                    )
                }
            }, 12000)
        } catch (e: SecurityException) {
            _status.value = ScaleStatus.Error(
                "Nearby Devices permission required to scan for Cult Smart Scale.",
                canRetry = true
            )
        } catch (e: Exception) {
            _status.value = ScaleStatus.Error(
                "Could not scan: ${e.message}",
                canRetry = true
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToScale(device: BluetoothDevice) {
        _status.value = ScaleStatus.Connecting

        // 10s connection timeout
        mainHandler.postDelayed({
            if (_status.value is ScaleStatus.Connecting) {
                disconnectGatt()
                _status.value = ScaleStatus.Error(
                    "Connection timed out. Could not establish link with Cult Smart Scale.",
                    canRetry = true
                )
            }
        }, 10000)

        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                mainHandler.removeCallbacksAndMessages(null)
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    disconnectGatt()
                    _status.value = ScaleStatus.Error(
                        "Could not connect to Cult Smart Scale (error code $status). Please step on the scale again.",
                        canRetry = true
                    )
                    return
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (weightLocked && !isCompleted && lastWeight > 0.0) {
                        // Natural power-off after lock
                        completeMeasurement("disconnect")
                    } else if (!isCompleted) {
                        _status.value = ScaleStatus.Error(
                            "Cult Smart Scale disconnected before reading completed.",
                            canRetry = true
                        )
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                    val serviceUuid = UUID.fromString(CultScaleParser.SCALE_SERVICE_UUID)
                    val characUuid = UUID.fromString(CultScaleParser.SCALE_CHAR_NOTIFY_UUID)

                    val service = gatt.getService(serviceUuid)
                    if (service == null) {
                        disconnectGatt()
                        _status.value = ScaleStatus.Error(
                            "Device connected is not a compatible Cult Smart Scale (missing 0xFFF0 service).",
                            canRetry = true
                        )
                        return
                    }

                    val charac = service.getCharacteristic(characUuid)
                    if (charac == null) {
                        disconnectGatt()
                        _status.value = ScaleStatus.Error(
                            "Cult Smart Scale measurement characteristic (0xFFF4) missing.",
                            canRetry = true
                        )
                        return
                    }

                    gatt.setCharacteristicNotification(charac, true)
                    val cccd = charac.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    if (cccd != null) {
                        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(cccd)
                    }

                    _status.value = ScaleStatus.Measuring(0.0, CultPhase.WEIGH)
                } else {
                    disconnectGatt()
                    _status.value = ScaleStatus.Error(
                        "Failed to discover Cult scale telemetry services.",
                        canRetry = true
                    )
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                characteristic?.value?.let { bytes ->
                    val frame = CultScaleParser.parseCultFrame(bytes) ?: return@let
                    if (!frame.checksumOk) return@let

                    handleParsedFrame(frame)
                }
            }
        })
    }

    private fun handleParsedFrame(frame: CultParsedFrame) {
        _liveWeight.value = frame.weightKg
        if (frame.heartRate != null) {
            _liveHeartRate.value = frame.heartRate
            lastHeartRate = frame.heartRate
        }
        if (frame.impedanceRaw != null) {
            latestImpedance = frame.impedanceRaw
        }

        if (frame.phase == CultPhase.WEIGH) {
            if (!weightLocked) {
                if (frame.weightKg == lastWeight && frame.weightKg > 10.0) {
                    weightRepeatCount++
                    if (weightRepeatCount >= 3) {
                        weightLocked = true
                        _status.value = ScaleStatus.Locked(frame.weightKg)
                        // Timeout: if scale lingers too long in body phase, complete anyway
                        mainHandler.postDelayed({
                            if (!isCompleted && weightLocked) {
                                completeMeasurement("timeout")
                            }
                        }, 25000)
                    } else {
                        _status.value = ScaleStatus.Measuring(frame.weightKg, CultPhase.WEIGH)
                    }
                } else {
                    lastWeight = frame.weightKg
                    weightRepeatCount = 1
                    _status.value = ScaleStatus.Measuring(frame.weightKg, CultPhase.WEIGH)
                }
            }
        } else if (frame.phase == CultPhase.BODY) {
            if (weightLocked) {
                if (frame.heartRate != null) {
                    if (frame.heartRate == lastHeartRate) {
                        hrRepeatCount++
                    } else {
                        lastHeartRate = frame.heartRate
                        hrRepeatCount = 1
                    }
                }

                val progress = (hrRepeatCount / 4f).coerceIn(0.2f, 0.95f)
                _status.value = ScaleStatus.Analyzing(
                    weightKg = lastWeight,
                    heartRate = lastHeartRate,
                    progress = progress
                )

                if (hrRepeatCount >= 4 && latestImpedance != null) {
                    completeMeasurement("hr-stable")
                }
            }
        }
    }

    private fun completeMeasurement(reason: String) {
        if (isCompleted || !weightLocked) return
        isCompleted = true
        mainHandler.removeCallbacksAndMessages(null)

        _status.value = ScaleStatus.Completed(
            weightKg = lastWeight,
            heartRate = lastHeartRate,
            impedanceRaw = latestImpedance
        )
        disconnectGatt()
    }

    /**
     * Explicit simulator mode for testing without physical scale hardware.
     */
    fun startSimulation(activeProfile: Profile?, targetWeight: Double? = null) {
        stop()
        isSimulating = true
        _status.value = ScaleStatus.Connecting

        val baseWeight = targetWeight ?: when {
            activeProfile?.sex?.equals("female", ignoreCase = true) == true -> 58.0 + Random.nextDouble(-1.5, 2.0)
            else -> 75.0 + Random.nextDouble(-2.0, 3.0)
        }

        simulationJob = scope.launch {
            delay(1000)
            val steps = listOf(
                baseWeight - 22.0,
                baseWeight - 10.5,
                baseWeight - 3.4,
                baseWeight - 0.8,
                baseWeight + 0.3,
                baseWeight,
                baseWeight,
                baseWeight
            )

            for (w in steps) {
                val rounded = kotlin.math.round(w * 10.0) / 10.0
                _liveWeight.value = rounded
                _status.value = ScaleStatus.Measuring(rounded, CultPhase.WEIGH)
                delay(300)
            }

            val lockedWeight = kotlin.math.round(baseWeight * 10.0) / 10.0
            _liveWeight.value = lockedWeight
            _status.value = ScaleStatus.Locked(lockedWeight)
            delay(900)

            val targetHr = Random.nextInt(64, 76)
            for (tick in 1..5) {
                val hrProgress = if (tick >= 2) targetHr else null
                _liveHeartRate.value = hrProgress
                _status.value = ScaleStatus.Analyzing(
                    weightKg = lockedWeight,
                    heartRate = hrProgress,
                    progress = tick / 5.0f
                )
                delay(600)
            }

            val simulatedImpedance = Random.nextInt(485, 525)
            _status.value = ScaleStatus.Completed(
                weightKg = lockedWeight,
                heartRate = targetHr,
                impedanceRaw = simulatedImpedance
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun disconnectGatt() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: Exception) {}
        bluetoothGatt = null
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        mainHandler.removeCallbacksAndMessages(null)
        simulationJob?.cancel()
        simulationJob = null
        isSimulating = false

        if (scanCallback != null && bluetoothAdapter != null) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (_: Exception) {}
            scanCallback = null
        }

        disconnectGatt()
        _status.value = ScaleStatus.Idle
        _liveWeight.value = 0.0
        _liveHeartRate.value = null
        weightLocked = false
        weightRepeatCount = 0
        lastWeight = 0.0
        lastHeartRate = null
        hrRepeatCount = 0
        latestImpedance = null
        isCompleted = false
    }
}
