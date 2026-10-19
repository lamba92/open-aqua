# OpenAqua: Architecture & Technical Specification

> **A modern, cross-platform thermal management suite for Aquacomputer devices, written in Kotlin Multiplatform with a lightweight Kotlin/Native hardware engine and a Compose Multiplatform Desktop UI.**

---

## 1. Executive Summary & Philosophy

Aquacomputer hardware (OCTO, QUADRO, D5 NEXT, High Flow Next, Aquaero) is the gold standard of custom PC liquid cooling and thermal management. However, the official **Aquasuite** software is Windows-only (WPF/.NET). Linux users have historically had to dual-boot, pass USB through to a Windows VM, or rely on partial CLI tools. Furthermore, on Windows, users seek a clean, modern, and open-source alternative without background service bloat or proprietary license lock-ins.

**OpenAqua** provides a modern, elegant, and cross-platform alternative for **Linux** and **Windows**.

### Current Status
> **Note**: Currently, **only the native hardware engine (`open-aqua-engine`) is implemented and fully operational** on both Linux and Windows. The Compose Multiplatform Desktop UI (`app-compose`) is in active development.

### Core Principles
1. **Device-Agnostic Core**: While built and verified initially on the OCTO, the architecture treats devices as data-driven descriptors (OCTO, QUADRO, D5 NEXT, etc.), allowing any supported Aquacomputer device to plug in effortlessly.
2. **Stateless Native Engine (`open-aqua-engine`)**: A tiny, ultra-fast Kotlin/Native binary handles low-level hardware interaction:
   - On Linux: direct kernel HIDRAW ioctl (`/dev/hidraw`) and `/sys/class/hwmon`.
   - On Windows: native Win32 HID subsystem via `HidD_SetFeature` and `HidD_SetOutputReport`.
3. **Embedded Execution Model**: The native engine will be bundled directly inside the Compose Desktop app JAR resources. At runtime, the JVM extracts it to the local cache directory and invokes it on demand via `ProcessBuilder` with JSON stdout/stdin. No daemons to babysit, zero IPC connection leaks.
4. **Hardware Autonomy**: All fan curves and settings are flashed directly into device EEPROM/firmware. The hardware executes curves autonomously—the software does not need to stay open for cooling to function.
5. **Modern Compose Desktop UI**: Built with JetBrains Compose Multiplatform Desktop, rendering at native refresh rates (60/144+ FPS) via Skiko (Vulkan/OpenGL on Wayland and Windows).

---

## 2. System Architecture & Engine Modules

```
┌────────────────────────────────────────────────────────────────────────┐
│                        app-compose (Kotlin/JVM)                        │
│                                                                        │
│   ┌──────────────────────────┐      ┌──────────────────────────────┐   │
│   │   Device & Channel Tabs  │      │     Interactive Curve Studio │   │
│   │ (Octo, Quadro, D5 Next)  │      │     (Compose Canvas Engine)  │   │
│   └────────────┬─────────────┘      └──────────────┬───────────────┘   │
│                │                                   │                   │
│                └─────────────┬─────────────────────┘                   │
│                              ▼      ▼                                  │
│                      ┌──────────────────────┐                          │
│                      │   Compose ViewModel  │                          │
│                      │   (StateFlow / MVI)  │                          │
│                      └──────────┬───────────┘                          │
│                                 │                                      │
│                      ┌──────────▼───────────┐                          │
│                      │     EngineBridge     │                          │
│                      │ (ProcessBuilder IPC) │                          │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ Invokes on-demand via JSON
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                 open-aqua-engine CLI (Kotlin/Native)                   │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │               :engine (Core Multiplatform Library)             │   │
│   │      Clikt Commands + HardwareController Contract + DI Module  │   │
│   └──────────────────────────┬─────────────────────────────┬───────┘   │
│                             │                             │            │
│                             ▼                             ▼            │
│   ┌──────────────────────────────┐ ┌─────────────────────────────────┐ │
│   │     :engine:engine-mock      │ │       :engine:engine-prod       │ │
│   │    MockHardwareController    │ │  LinuxHardwareController        │ │
│   │  (macosArm64, linuxX64 kexe) │ │    (linuxX64 ioctl + hwmon)     │ │
│   │                              │ │  WindowsHardwareController      │ │
│   │                              │ │    (mingwX64 winhid cinterop)   │ │
│   └──────────────────────────────┘ └─────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Module Breakdown
- **`:core-model`**: Domain data models, polymorphic protocol responses (`EngineResponse`, `DeviceConfig`, `OctoTelemetry`, `DescriptorsResponse`), device descriptors (`DeviceRegistry`), and curve math (`CurveMath`).
- **`:engine`**: Core multiplatform library containing Clikt commands, the `HardwareController` interface, protocol parsers/writers, CRC-16 computation, and `engineCoreModule` (Kodein DI).
- **`:engine:engine-mock`**: Standalone executable producing `open-aqua-engine-mock` with fake telemetry, devices, curve responses, and a simulation command for development on macOS/Linux without hardware.
- **`:engine:engine-prod`**: Production native executable producing `open-aqua-engine`:
  - **Linux (`linuxX64`)**: Uses POSIX `/dev/hidraw` ioctls and `/sys/class/hwmon` reads.
  - **Windows (`mingwX64`)**: Uses native Windows `hid.dll` APIs via cinterop (`WindowsHardwareController`).

---

## 3. Generic Device Abstraction (`core-model`)

All Aquacomputer USB devices share vendor ID `0x0C70` and communicate via USB HID Feature Report `0x03` (Control) and Report `0x01` (Status). The difference lies in channel counts, report offsets, and capabilities.

### Device Descriptor Schema
```kotlin
@Serializable
data class DeviceDescriptor(
    val type: DeviceType,
    val name: String,
    val productId: Int,
    val ctrlReportLength: Int,
    val statusReportLength: Int,
    val fanChannels: List<FanChannelDescriptor> = emptyList(),
    val tempSensors: List<SensorDescriptor> = emptyList(),
    val tempCtrlOffset: Int? = null,
    val firmwareVersionOffset: Int? = 0x0D,
    val hasFlowSensor: Boolean = false,
    val flowSensorOffset: Int? = null,
    val flowPulsesCtrlOffset: Int? = null,
    val hasRgbpx: Boolean = false,
    val rgbpxPortCount: Int = 0,
    val virtualSensorsCount: Int = 0,
    val virtualSensorsStartOffset: Int? = null,
)

@Serializable
data class FanChannelDescriptor(
    val channel: Int,
    val ctrlOffset: Int,
    val statusOffset: Int,
)

@Serializable
data class SensorDescriptor(
    val channel: Int,
    val hwmonIndex: Int,
    val isVirtual: Boolean = false,
)
```

### Supported Hardware Matrix & Testing Status

> **Disclaimer**: Only the **Aquacomputer OCTO** has been physically tested and verified against real hardware by the maintainer. **QUADRO** and **D5 NEXT** descriptors are implemented based on driver specifications and reverse engineering, but remain experimental until verified on hardware.

| Device | Product ID | Fan Channels | Temp Sensors | Flow Sensor | Control Report Size | Status Report Size | Hardware Testing Status |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **OCTO** | `0xF011` | 8 Channels | 4 Physical + 16 Virtual | Yes (Dedicated port, offset `0x7B`, dL/h) | `0x65F` (1631 bytes) | `0x147` (327 bytes) | 🟢 Physically Verified (Linux & Windows) |
| **QUADRO** | `0xF00D` | 4 Channels | 4 Physical + 16 Virtual | Yes (Dedicated port, offset `0x6E`, dL/h) | `0x3C1` (961 bytes) | `0xA5` (165 bytes) | 🟡 Experimental / Untested |
| **D5 NEXT** | `0xF00E` | 2 (Pump + Fan) | 1 Liquid + 8 Virtual | Calculated / Virtual | `0x329` (809 bytes) | `0x147` (327 bytes) | 🟡 Experimental / Untested |
| **FARBWERK 360**| `0xF010` | None | 4 Physical + 16 Virtual | None | `0xB6` | - | 📋 Planned |

---

## 4. Hardware Communication & Protocol Engine

### 1. The Curve Storage Format (Universal Across Devices)
Inside each fan channel's block in Control Report `0x03`, Aquacomputer devices store exactly **16 coordinate pairs**:
- **Temperatures (`+0x15` to `+0x34`)**: 16 $\times$ 16-bit Big-Endian integers in centidegrees Celsius ($T \times 100$).
- **Power / PWM (`+0x35` to `+0x54`)**: 16 $\times$ 16-bit Big-Endian integers in centipercent ($P \times 100$).

### 2. Checksum Algorithm (CRC-16-USB)
Before sending the report back to the device, a 16-bit CRC must be appended to the last two bytes of the report:
- **Polynomial**: `0x8005` (reversed: `0xA001`, `x^16 + x^15 + x^2 + 1`)
- **Initial value**: `0xFFFF`
- **XOR Out**: `0xFFFF`
- **Range**: Calculated over bytes `1` through `reportLength - 2`.

### 3. Linux POSIX Interface
Executed in Kotlin/Native via `platform.posix.*`:
```c
int fd = open("/dev/hidrawX", O_RDWR);
ioctl(fd, HIDIOCGFEATURE(reportLen), buffer); // Read Feature Report
ioctl(fd, HIDIOCSFEATURE(reportLen), buffer); // Write Feature Report
close(fd);
```

### 4. Windows Win32 HID Interface
Executed in Kotlin/Native via `winhid` cinterop against `hid.dll`:
```c
// Feature reports (Report 0x03 - Config and Control)
HidD_GetFeature(deviceHandle, buffer, reportLen);
HidD_SetFeature(deviceHandle, buffer, reportLen);

// Output reports (Report 0x02 - Secondary commit report)
// Note: On Windows, Report 0x02 is declared as an Output Report in the HID descriptor.
// Using HidD_SetFeature on Report 0x02 fails with ERROR_INVALID_PARAMETER (87);
// it must be written using HidD_SetOutputReport:
HidD_SetOutputReport(deviceHandle, buffer, reportLen);
```

---

## 5. The Automatic Curve Math (Aquasuite Algorithm Replication)

Rather than forcing users to manually enter 16 separate coordinate numbers, OpenAqua features Aquasuite's **Automatic Curve (Kurvenregler)** generator.

### Parameters
1. $T_{min}, T_{max}$: Temperature boundary in °C.
2. $P_{min}, P_{max}$: Power boundary in % (e.g. 15% to 100%).
3. $\text{shape}$ ($s \in \mathbb{R}$): Curvature slider:
   - **$s = 0.0$ (Linear)**: $f(t) = t$
   - **$s > 0.0$ (Progressive / Exponential)**: $f(t) = t^{\,1 + s}$  
     *(Keeps fans whisper-quiet at low-to-medium load, ramps up aggressively only near the temperature ceiling)*
   - **$s < 0.0$ (Digressive / Logarithmic)**: $f(t) = 1 - (1 - t)^{\,1 + |s|}$  
     *(Ramps up quickly at early temperatures to establish solid airflow baseline, then flattens out)*

### Formula for Point $i \in [0, 15]$:
$$t_i = \frac{i}{15}$$
$$T_i = T_{min} + t_i \cdot (T_{max} - T_{min})$$
$$P_i = P_{min} + f(t_i, s) \cdot (P_{max} - P_{min})$$

---

## 6. Stdin / Stdout JSON Command Protocol

All stdout responses from `open-aqua-engine` implement strongly typed data models with polymorphic `"type"` discriminators:

### 1. Discover Devices
```bash
$ open-aqua-engine devices
```
```json
{
  "devices": [
    {
      "id": "13235-54067",
      "alias": "octo-1",
      "type": "OCTO",
      "name": "Aquacomputer OCTO",
      "serial": "13235-54067",
      "firmwareVersion": 1020
    }
  ]
}
```

### 2. Print Device Descriptors
```bash
$ open-aqua-engine descriptors --type OCTO --pretty
```
```json
{
  "descriptors": [
    {
      "type": "OCTO",
      "name": "Aquacomputer OCTO",
      "productId": 61457,
      "ctrlReportLength": 1631,
      "statusReportLength": 327,
      "fanChannels": [
        {"channel": 0, "ctrlOffset": 90, "statusOffset": 125}
      ],
      "tempSensors": [
        {"channel": 0, "hwmonIndex": 1, "isVirtual": false}
      ],
      "hasFlowSensor": true,
      "flowSensorOffset": 123
    }
  ]
}
```

### 3. Read Telemetry
```bash
$ open-aqua-engine telemetry --device octo-1 --pretty
```
```json
{
  "type": "octo",
  "flowRateDlH": 182,
  "sensors": [
    {"channel": 0, "tempCelsius": 35.4, "isVirtual": false},
    {"channel": 1, "tempCelsius": null, "isVirtual": false}
  ],
  "fans": [
    {"channel": 0, "rpm": 2184, "powerWatts": 8.4, "voltageVolts": 12.0, "currentAmps": 0.7, "pwmPercent": 45.0},
    {"channel": 1, "rpm": 1367, "powerWatts": 1.13, "voltageVolts": 12.0, "currentAmps": 0.1, "pwmPercent": 30.0}
  ]
}
```

### 4. Stream Telemetry (NDJSON)
Continuously emits single-line JSON samples at the hardware report rate:
```bash
$ open-aqua-engine stream-telemetry --device octo-1
```
```ndjson
{"type":"octo","flowRateDlH":182,"fans":[...],"sensors":[...]}
{"type":"octo","flowRateDlH":182,"fans":[...],"sensors":[...]}
```

### 5. Read Hardware Configuration
Reads the complete persistent hardware configuration (fan channels, sensor calibration offsets, etc.) from EEPROM:
```bash
$ open-aqua-engine config read --device octo-1 --pretty
```
```json
{
  "type": "octo",
  "fans": [
    {
      "type": "curve",
      "channel": 0,
      "sensorIndex": 0,
      "points": [
        {"temp": 20.0, "pwm": 30.0},
        {"temp": 57.5, "pwm": 97.5}
      ]
    },
    {
      "type": "fixed",
      "channel": 2,
      "powerPercent": 50.0
    }
  ],
  "sensorOffsets": [
    {"channel": 0, "offsetCelsius": 0.0},
    {"channel": 1, "offsetCelsius": -0.2},
    {"channel": 2, "offsetCelsius": 0.0},
    {"channel": 3, "offsetCelsius": 0.5}
  ]
}
```

### 6. Flash Hardware Configuration
Flashes changes to hardware EEPROM in a single atomic read-modify-write cycle. Any omitted field or channel is left unchanged:
```bash
$ open-aqua-engine config flash --device octo-1 --request '{"fans":{"0":{"type":"fixed","channel":0,"powerPercent":60.0}}}'
```
Or from a JSON file:
```bash
$ open-aqua-engine config flash --device octo-1 --file profile.json
```

### 7. Feed Virtual Sensors
Streams host temperature readings over stdin to the hardware's 16 virtual temperature slots:
```bash
$ echo '{"sensors":{"0":45.2,"1":58.0}}' | open-aqua-engine feed-virtual-sensors --device octo-1
```

---

## 7. Compose Multiplatform UI Specification (Planned)

### 1. App Layout
- **Sidebar**: Device list (Octo, Quadro, etc.) + Global Settings + Profile Switcher.
- **Header**: Live coolant temperature badge, loop status pill (Optimal / Warm / Critical), connection health.
- **Main Area (Tabs)**:
  - **Dashboard Tab**: Circular live gauges (RPM, Voltage, Wattage, Flow Rate) with smooth animations.
  - **Curve Studio Tab**: Interactive Compose `Canvas`:
    - Renders the full 0%–100% PWM vs 20°C–60°C graph grid.
    - Draggable control points or interactive curvature slider with real-time Bézier/polynomial rendering.
    - **Live Operating Point Indicator**: A glowing, animated dot moving along the curve corresponding to live water temperature, showing current calculated PWM and actual RPM.
    - Dual Curve View: Dotted line for *Current Hardware Curve*, solid colored line for *Draft Curve*.
    - "Flash to Device" button with instant tactile feedback.
  - **Profiles Tab**: Save/Export/Import fan configurations in human-readable JSON files (*Silent*, *Gaming*, *Stress Test*).

---

## 8. Development & Build Roadmap

| Milestone | Status | Deliverables |
| :--- | :---: | :--- |
| **Phase 1: Project Scaffolding** | ✅ Complete | Multi-module Gradle project (`core-model`, `engine`, `app-compose`), Kotlin 2.x, Compose Multiplatform plugin. |
| **Phase 2: Generic Engine & Models** | ✅ Complete | Cross-platform engine for **Linux** (`linuxX64`) and **Windows** (`mingwX64`) + `engine-mock` on macOS. Full discovery, descriptors inspection, telemetry, flow sensing, EEPROM curve reading & flashing, virtual sensor feeds. |
| **Phase 3: Engine Bridge & Lifecycle** | ⏳ In Progress | `EngineBridge` in `app-compose` to extract embedded binary to cache and run CLI commands via coroutines with `StateFlow`. |
| **Phase 4: Curve Studio Canvas** | 📋 Planned | Interactive Compose Desktop Canvas curve editor with draggable handles, shape slider, and live coolant operating point. |
| **Phase 5: Dashboard & Polish** | 📋 Planned | Live hardware gauges, fan channel naming, theme styling (Dark Modern Glass), and desktop distribution packaging (`.deb`, `.rpm`, `.msi`, portable exe). |
