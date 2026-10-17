# OpenAqua: Architecture & Technical Specification

> **A modern, native Linux thermal management suite for Aquacomputer devices, written in Kotlin JVM with Compose Multiplatform Desktop and a lightweight Kotlin/Native hardware engine.**

---

## 1. Executive Summary & Philosophy

Aquacomputer hardware (OCTO, QUADRO, D5 NEXT, High Flow Next, Aquaero) is the gold standard of custom PC liquid cooling and thermal management. However, the official **Aquasuite** software is Windows-only (WPF/.NET). Linux users have historically had to dual-boot, pass USB through to a Windows VM, or rely on partial CLI tools.

**OpenAqua** aims to be a complete, elegant, and native Linux alternative.

### Core Principles
1. **Device-Agnostic Core**: While built initially for the OCTO, the architecture treats devices as data-driven descriptors (OCTO, QUADRO, D5 NEXT, etc.), allowing any supported Aquacomputer device to plug in effortlessly.
2. **Stateless Native Engine (`open-aqua-engine`)**: A tiny, ultra-fast Kotlin/Native binary handles low-level Linux hardware interaction (`/dev/hidraw` ioctl, `/sys/class/hwmon`, USB CRC-16).
3. **Embedded Execution Model**: The native engine is bundled directly inside the Compose Desktop app JAR resources. At runtime, the JVM extracts it to `~/.cache/open-aqua/bin/` and invokes it on demand via `ProcessBuilder` with JSON stdout/stdin. No daemons to babysit, no JNA/JNI crashes, zero IPC connection leaks.
4. **Hardware Autonomy**: All fan curves and settings are flashed directly into device EEPROM/firmware. The hardware executes curves autonomously—the software does not need to stay open for cooling to function.
5. **Modern Compose Desktop UI**: Built with JetBrains Compose Multiplatform Desktop, rendering at native refresh rates (60/144+ FPS) via Skiko (Vulkan/OpenGL on Wayland).

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
└─────────────────────────────────┬──────────────────────────────────────┘
                                   │ Invokes on-demand via JSON
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   engine CLI (Kotlin/Native + Kodein DI)               │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │               :engine (Core Multiplatform Library)             │   │
│   │      Clikt Commands + HardwareController Contract + DI Module  │   │
│   └─────────────────────────┬────────────────────────┬─────────────┘   │
│                             │                        │                 │
│                             ▼                        ▼                 │
│   ┌──────────────────────────────┐ ┌───────────────────────────────┐   │
│   │     :engine:engine-mock      │ │      :engine:engine-prod      │   │
│   │   MockHardwareController     │ │    LinuxHardwareController    │   │
│   │  (macosArm64, linuxX64 kexe) │ │    (linuxX64 ioctl + hwmon)   │   │
│   └──────────────────────────────┘ └───────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

### Module Breakdown
- **`:core-model`**: Domain data models, polymorphic protocol responses (`EngineResponse`), device descriptors (`DeviceRegistry`), and curve math (`CurveMath`).
- **`:engine`**: Core library containing Clikt commands, the `HardwareController` interface, and `engineCoreModule` (Kodein DI).
- **`:engine:engine-mock`**: Standalone executable producing `open-aqua-engine-mock` with fake telemetry, devices, curve responses, and a `fail` simulation command for development on macOS/Linux.
- **`:engine:engine-prod`**: Production Linux executable producing `open-aqua-engine` with actual POSIX `/dev/hidraw` ioctls and `/sys/class/hwmon` reads.

---

## 3. Generic Device Abstraction (`core-model`)

All Aquacomputer USB devices share vendor ID `0x0C70` and communicate via USB HID Feature Report `0x03` (Control) and `0x01`/`0x02` (Status). The difference lies entirely in channel counts and report offsets.

### Device Descriptor Schema
```kotlin
data class DeviceDescriptor(
    val type: DeviceType,
    val name: String,
    val vendorId: Int = 0x0C70,
    val productId: Int,
    val ctrlReportLength: Int,
    val statusReportLength: Int,
    val fanChannels: List<FanDescriptor>,
    val tempSensors: List<SensorDescriptor>,
    val tempCtrlOffset: Int? = null,
    val hasFlowSensor: Boolean = false,
    val hasRgbpx: Boolean = false,
)

data class FanDescriptor(
    val id: String,                // "fan1", "fan2", etc.
    val ctrlOffset: Int,           // Base byte offset in Report 0x03 (e.g. 0x5A, 0xAF...)
    val hwmonIndex: Int            // sysfs fanX / pwmX index
)

data class SensorDescriptor(
    val id: String,                // "temp1", "temp2"
    val hwmonIndex: Int,           // sysfs tempX index
    val isVirtual: Boolean = false
)
```

### Supported Hardware Matrix

| Device | Product ID | Fan Channels | Temp Sensors | Flow Sensor | Control Report Size | Temp Ctrl Offset |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **OCTO** | `0xF011` | 8 Channels | 4 Physical + 16 Virtual | Optional (via channel) | `0x65F` (1631 bytes) | `0x0A` |
| **QUADRO** | `0xF00D` | 4 Channels | 4 Physical + 16 Virtual | 1 Dedicated | `0x3C1` (961 bytes) | `0x0A` |
| **D5 NEXT** | `0xF00E` | 2 (Pump + Fan) | 1 Liquid + 8 Virtual | Calculated / Virtual | `0x329` (809 bytes) | `0x2D` |
| **FARBWERK 360**| `0xF010` | None | 4 Physical + 16 Virtual | None | `0xB6` | `0x0A` |

---

## 4. Hardware Communication & Protocol Engine

### 1. The Curve Storage Format (Universal Across Devices)
Inside each fan channel's block in Control Report `0x03`, Aquacomputer devices store exactly **16 coordinate pairs**:
- **Temperatures (`+0x15` to `+0x34`)**: 16 $\times$ 16-bit Big-Endian integers in centidegrees Celsius ($T \times 100$).
- **Power / PWM (`+0x35` to `+0x54`)**: 16 $\times$ 16-bit Big-Endian integers in centipercent ($P \times 100$).

### 2. Checksum Algorithm (CRC-16-USB)
Before sending the report back to the device, a 16-bit CRC must be appended to the last two bytes of the report:
- **Polynomial**: `0x8005` (reversed: `0xA001`, `x^16 + x^15 + x^2 + 1`)
- - **Initial value**: `0xFFFF`
- **XOR Out**: `0xFFFF`
- **Range**: Calculated over bytes `1` through `reportLength - 2`.

### 3. Linux POSIX Interface
Executed in Kotlin/Native via `platform.posix.*`:
```c
int fd = open("/dev/hidrawX", O_RDWR);
ioctl(fd, HIDIOCGFEATURE(reportLen), buffer); // Read
ioctl(fd, HIDIOCSFEATURE(reportLen), buffer); // Write
close(fd);
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
   - **$s < 0.0$ (Digressive / Logarithmic)**: $f(t) = 1 - (1 - t)^{\\,1 + |s|}$  
     *(Ramps up quickly at early temperatures to establish solid airflow baseline, then flattens out)*

### Formula for Point $i \in [0, 15]$:
$$t_i = \frac{i}{15}$$
$$T_i = T_{min} + t_i \cdot (T_{max} - T_{min})$$
$$P_i = P_{min} + f(t_i, s) \cdot (P_{max} - P_{min})$$

---

## 6. Stdin / Stdout JSON Command Protocol

All stdout responses from `open-aqua-engine` implement the polymorphic `@Serializable sealed interface EngineResponse` with a `"type"` discriminator:

### 1. Discover Devices
```bash
$ open-aqua-engine devices
```
```json
{
  "type": "devices",
  "devices": [
    {
      "id": "octo_0",
      "type": "OCTO",
      "name": "Aquacomputer OCTO",
      "serial": "13235-54067",
      "firmwareVersion": 1020,
      "hidrawPath": "/dev/hidraw9",
      "hwmonPath": "/sys/class/hwmon/hwmon4"
    }
  ]
}
```

### 2. Read Telemetry
```bash
$ open-aqua-engine telemetry --device octo_0
```
```json
{
  "type": "telemetry",
  "deviceId": "octo_0",
  "deviceType": "OCTO",
  "name": "Aquacomputer OCTO",
  "sensors": [
    {"id": "temp1", "tempCelsius": 35.4, "offsetCelsius": 0.0, "isVirtual": false},
    {"id": "temp2", "tempCelsius": null, "offsetCelsius": null, "isVirtual": false}
  ],
  "fans": [
    {"id": "fan1", "rpm": 2184, "powerWatts": 8.4, "voltageVolts": 12.0, "currentAmps": 0.7, "pwmPercent": 45.0},
    {"id": "fan2", "rpm": 1367, "powerWatts": 1.13, "voltageVolts": 12.0, "currentAmps": 0.1, "pwmPercent": 30.0}
  ],
  "flowRateDlH": null
}
```

### 3. Read Hardware Curves
```bash
$ open-aqua-engine curves --device octo_0
```
```json
{
  "type": "curves",
  "deviceId": "octo_0",
  "curves": {
    "fan4": [
      {"temp": 25.0, "pwm": 15.0},
      {"temp": 26.7, "pwm": 15.0},
      {"temp": 35.2, "pwm": 26.0},
      {"temp": 50.5, "pwm": 100.0}
    ]
  }
}
```

### 4. Flash Curve to Hardware EEPROM
Via generated curve parameters:
```bash
$ open-aqua-engine flash curve-params --device octo_0 --fan fan4 --curve-params '{"tMin":25.0,"tMax":75.0,"pMin":15.0,"pMax":100.0}'
```

Via explicit 16-point curve coordinates:
```bash
$ open-aqua-engine flash points --device octo_0 --fan fan4 --points '[{"temp":25.0,"pwm":15.0}, ...]'
```
```json
{
  "type": "flash",
  "status": "ok",
  "deviceId": "octo_0",
  "fanId": "fan4",
  "bytesWritten": 1631,
  "verified": true,
  "error": null
}
```

### 5. Set Sensor Calibration Offset
```bash
$ open-aqua-engine sensor-offset --device octo_0 --sensor temp1 --offset 0.5
```
```json
{
  "type": "set-sensor-offset",
  "status": "ok",
  "deviceId": "octo_0",
  "sensorId": "temp1",
  "offsetCelsius": 0.5,
  "verified": true,
  "error": null
}
```

### 6. Error Response
```json
{
  "type": "error",
  "error": "Device 'octo_0' not found",
  "exceptionType": "IllegalStateException"
}
```

---

## 7. Compose Multiplatform UI Specification

### 1. App Layout
- **Sidebar**: Device list (Octo, Quadro, etc.) + Global Settings + Profile Switcher.
- **Header**: Live coolant temperature badge, loop status pill (Optimal / Warm / Critical), connection health.
- **Main Area (Tabs)**:
  - **Dashboard Tab**: Circular live gauges (RPM, Voltage, Wattage, Flow Rate) with smooth animations.
  - **Curve Studio Tab**: Interactive Compose `Canvas`:
    - Renders the full 0%–100% PWM vs 20°C–60°C graph grid.
    - Draggable control points or interactive curvature slider with real-time Bézier/polynomial rendering.
    - **Live Operating Point Indicator**: A glowing, animated dot moving along the curve corresponding to live water temperature, showing current calculated PWM and actual RPM.
    - Dual Curve View: Dotted line for *Current Hardware Curve*, solid colored line for *Draft Curve*..
    - "Flash to Device" button with instant tactile feedback.
  - **Profiles Tab**: Save/Export/Import fan configurations in human-readable JSON files (*Silent*, *Gaming*, *Stress Test*).

---

## 8. Development & Build Roadmap

| Milestone | Deliverables |
| :--- | :--- |
| **Phase 1: Project Scaffolding** | Setup multi-module Gradle project (`core-model`, `engine`, `app-compose`), configure Kotlin 2.x and Compose Desktop plugin. |
| **Phase 2: Generic Engine & Models** | Implement `core-model` (models & curve math) + `engine` library with `engine-mock` and `engine-prod` using Kodein DI. *(Note: `optimizeEngineExecutable` strip + UPX compression task is temporarily dropped and will be re-added when finalizing release packaging).* |
| **Phase 3: Engine Bridge & Lifecycle** | Create `EngineBridge` in `app-compose` to extract embedded binary to cache and run CLI commands via coroutines with `StateFlow`. |
| **Phase 4: Curve Studio Canvas** | Build the interactive Compose Desktop Canvas curve editor with draggable handles, shape slider, and live coolant operating point. |
| **Phase 5: Dashboard & Polish** | Add live hardware gauges, fan channel naming, theme styling (Dark Modern Glass), and desktop distribution packaging (`.deb`, `.rpm`, runnable bundle). |
