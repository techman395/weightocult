# WeightOCult ⚖️

> **Independent, Privacy-First Android Client for Cult Smart Scales**

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**WeightOCult** is a clean, local-first Android application designed to connect to the **Cult Smart Scale** over Bluetooth Low Energy (BLE). It decodes real-time weight, impedance, and heart rate telemetry directly on your device — **no third-party cloud servers, no account logins, and zero telemetry tracking**.

---

## ✨ Features

- **⚡ Real-Time BLE Sync**: Fast Bluetooth LE connection and continuous telemetry streaming from Cult Smart Scale.
- **📊 Peer-Reviewed Body Composition**:
  - **Body Mass Index (BMI)** with WHO classifications.
  - **Lean Mass (Fat-Free Mass)**: Clearly displays total non-fat mass (muscle, water, and bone combined).
  - **Skeletal Muscle Mass (SMM)** computed via the **Janssen (2000)** BIA equation.
  - **Body Fat Percentage (%)** calculated via the **Deurenberg (1991)** model.
  - **Total Body Water (TBW)** calculated using the **Watson (1980)** equation.
  - **Basal Metabolic Rate (BMR)** derived from the **Mifflin-St Jeor (1990)** formula.
  - **Heart Rate (BPM)** tracking directly from scale sensor telemetry.
- **👥 Multi-User Profiles**: Seamlessly switch between family or household members with individualized biometric models and color themes.
- **📈 Interactive Trend Visualizations**: Visual historical curves for weight, body fat, muscle mass, and body water breakdown.
- **🛡️ 100% Local & Private**: All data is stored locally on device. Includes one-tap JSON backup, restore, and CSV export.
- **🧪 Simulator & Manual Mode**: Built-in BLE hardware simulator and manual entry modal for testing without physical scale hardware.

---

## 🔬 BLE Protocol & Reverse Engineering

The Cult Smart Scale communicates over custom 11-byte BLE notification frames via service UUID `0000fff0-0000-1000-8000-00805f9b34fb` and characteristic `0000fff4-0000-1000-8000-00805f9b34fb`.

| Byte | Field | Description |
| :--- | :--- | :--- |
| `0` | Header | Fixed preamble (`0xCF`) |
| `1` | Heart Rate | Live heart rate in BPM (`0` during early weighing) |
| `2` | Flags | Unit and status flags |
| `3-4` | Weight | Little-endian uint16 (`raw / 100.0` kg) |
| `5-6` | Impedance | Little-endian uint16 resistance in Ohms ($\Omega$) |
| `7-8` | Reserved | Reserved vendor bytes |
| `9` | Phase | `0x01` = Live Weighing Phase, `0x00` = Body Analysis Phase |
| `10` | Checksum | XOR reduction of bytes `0..9` |

---

## 📑 Scientific References & Equations

Body composition metrics in this project are implemented directly from peer-reviewed scientific literature:

1. **Skeletal Muscle Mass (SMM)**:
   - Janssen, I., Heymsfield, S. B., Baumgartner, R. N., & Ross, R. (2000). *Estimation of skeletal muscle mass by bioelectrical impedance analysis*. **Journal of Applied Physiology**, 89(2), 465–471.
2. **Body Fat Percentage**:
   - Deurenberg, P., Weststrate, J. A., & Seidell, J. C. (1991). *Body mass index as a measure of body fatness: age- and sex-specific prediction formulas*. **British Journal of Nutrition**, 65(1), 105–114.
3. **Total Body Water (TBW)**:
   - Watson, P. E., Watson, I. D., & Batt, R. D. (1980). *Total body water volumes for adult males and females estimated from simple anthropometric measurements*. **American Journal of Clinical Nutrition**, 33(1), 27–39.
4. **Basal Metabolic Rate (BMR)**:
   - Mifflin, M. D., St Jeor, S. T., Hill, L. A., Scott, B. J., Daugherty, S. A., & Koh, Y. O. (1990). *A new predictive equation for resting energy expenditure in healthy individuals*. **American Journal of Clinical Nutrition**, 51(2), 241–247.

---

## 🙏 Credits & Acknowledgments

This application is built with gratitude to the open-source community:

- **Upstream BLE Reverse Engineering**:
  - Special thanks to **[bpepple](https://github.com/bpepple)** and the contributors to the open-source **[`occult`](https://github.com/bpepple/occult)** repository for their foundational reverse-engineering work and documentation on the Cult Smart Scale BLE protocol and packet structure.
- **AI Software Engineering**:
  - Architected and engineered with the assistance of **Gemini** via **Google AI Studio**.

---

## 🛠️ Building From Source

### Prerequisites
- Android Studio Hedgehog / Ladybug or newer
- JDK 17 or JDK 21
- Android SDK 35 / 36

### Build Commands
```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/weightocult.git
cd weightocult

# Build debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) - see the LICENSE file for details.
