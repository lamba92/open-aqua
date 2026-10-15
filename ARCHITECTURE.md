# OpenAqua: Architecture & Technical Specification

> **A modern, native Linux thermal management suite for Aquacomputer devices, written in Kotlin JVM with Compose Multiplatform Desktop and a lightweight Kotlin/Native hardware engine.**

---

## 1. Executive Summary & Philosophy

Aquacomputer hardware (OCTO, QUADRO, D5 NEXT, High Flow Next, Aquaero) is the gold standard of custom PC liquid cooling and thermal management. However, the official **Aquasuite** software is Windows-only (WPF/.NET). Linux users have historically had to dual-boot, pass USB through to a Windows VM, or rely on partial CLI tools.

**OpenAqua** aims to be a complete, elegant, and native Linux alternative.

### Core Principles
1. **Device-Agnostic Core**: While built initially for the OCTO, the architecture treats devices as data-driven descriptors (OCTO, QUADRO, D5 NEXT, etc.), allowing any supported Aquacomputer device to plug in effortlessly.
2. **Stateless Native Engine (`open-aqua-engine`)**: A tiny (~600 KB), ultra-fast Kotlin/Native binary handles low-level Linux hardware interaction (`/dev/hidraw` ioctl, `/sys/class/hwmon`, USB CRC-16).
3. **Embedded Execution Model**: The native engine is bundled directly inside the Compose Desktop app JAR resources. At runtime, the JVM extracts it to `~/.cache/open-aqua/bin/` and invokes it on demand via `ProcessBuilder` with JSON stdout/stdin. No daemons to babysit, no JNA/JNI crashes, zero IPC connection leaks.
4. **Hardware Autonomy**: All fan curves and settings are flashed directly into device EEPROM/firmware. The hardware executes curves autonomously—the software does not need to stay open for cooling to function.
5. **Modern Compose Desktop UI**: Built with JetBrains Compose Multiplatform Desktop, rendering at native refresh rates (60/144+ FPS) via Skiko (Vulkan/OpenGL on Wayland).

---

## 2. System Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        app-compose (Kotlin/JVM)                        │
│                                                                        │
│   ┌──────────────────────────┐      ┌──────────────────────────────┐   │
│   │   Device & Channel Tabs  │      │     Interactive Curve Studio │   │
│   │ (Octo, Quadro, D5 Next)  │      │     (Compose Canvas Engine)  │   │
│   └─────────────┬────────────┘      └──────────────┬───────────────┘   │
│                 │                                  │                   │
│                 └─────────────┐      ┌─────────────┘                   │
│                               ▼      ▼                                 │
│                       ┌──────────────────────┐                         │
│                       │   Compose ViewModel  │                         │
│                       │   (StateFlow / MVI)  │                         │
│                       └──────────┬───────────┘                         │
│                                  │                                     │
│                       ┌──────────▼───────────┐                         │
│                       │     EngineBridge     │                         │
│                       │ (ProcessBuilder IPC) │                         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Invokes on-demand via JSON
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      engine-native (Kotlin/Native)                     │
│               [Embedded in JAR -> extracted to cache]                  │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                     Device Registry & CLI                      │   │
│   │     (telemetry | get-curves | flash-curve | list-devices)      │   │
│   └───────────────┬───────────────────────────────┬────────────────┘   │
│                   │                               │                    │
│   ┌───────────────▼──────────────┐ ┌──────────────▼────────────────┐   │
│   │    Linux Hwmon Telemetry     │ │    Generic HIDRaw Driver      │   │
│   │ (/sys/class/hwmon/hwmonX/..) │ │ (/dev/hidrawX ioctl + CRC16)  │   │
│   └──────────────────────────────┘ └───────────────────────────────┘   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ USB HID Feature Reports
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Physical Hardware Layer                         │
│    ┌──────────────┐     ┌──────────────┐     ┌──────────────────┐      │
│    │  Aqua OCTO   │     │ Aqua QUADRO  │     │   D5 NEXT Pump   │ ...  │
│    │ (8 Fans/PWM) │     │ (4 Fans/PWM) │     │ (Pump + Fan+OLED)│      │
│    └──────────────┘     └──────────────┘     └──────────────────┘      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Generic Device Abstraction (`core-model`)

All Aquacomputer USB devices share vendor ID `0x0C70` and communicate via USB HID Feature Report `0x03` (Control) and `0x01`/`0x02` (Status). The difference lies entirely in channel counts and report offsets.

### Device Descriptor Schema
```kotlin
data class DeviceDescriptor(
    val id: String,                // "octo", "quadro", "d5next"
    val name: String,              // "Aquacomputer OCTO"
    val usbProductId: Int,         // 0xF011 (OCTO), 0xF00D (QUADRO), 0xF00E (D5 NEXT)
    val ctrlReportLength: Int,     // 0x65F (OCTO), 0x3C1 (QUADRO), 0x329 (D5 NEXT)
    val fanChannels: List<FanChannelDescriptor>,
    val tempSensors: List<SensorDescriptor>,
    val hasFlowSensor: Boolean = false,
    val hasRgbpx: Boolean = false
)

data class FanChannelDescriptor(
    val id: String,                // "fan1", "fan2", etc.
    val defaultName: String,       // "Pump", "Upper Radiator", etc.
    val ctrlOffset: Int,           // Base byte offset in Report 0x03 (e.g. 0x5A, 0xAF...)
    val hwmonIndex: Int            // sysfs fanX / pwmX index
)

data class SensorDescriptor(
    val id: String,                // "temp1", "temp2"
    val defaultName: String,       // "Sensor 1", "Coolant Temp"
    val hwmonIndex: Int
)
```

### Supported Hardware Matrix

| Device | Product ID | Fan Channels | Temp Sensors | Flow Sensor | Control Report Size |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **OCTO** | `0xF011` | 8 Channels | 4 Physical + 16 Virtual | Optional (via channel) | `0x65F` (1631 bytes) |
| **QUADRO** | `0xF00D` | 4 Channels | 4 Physical + 16 Virtual | 1 Dedicated | `0x3C1` (961 bytes) |
| **D5 NEXT** | `0xF00E` | 2 (Pump + Fan) | 1 Liquid + 8 Virtual | Calculated / Virtual | `0x329` (809 bytes) |
| **FARBWERK 360**| `0xF010` | None | 4 Physical + 16 Virtual | None | `0xB6` |

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
   - **$s < 0.0$ (Digressive / Logarithmic)**: $f(t) = 1 - (1 - t)^{\,1 + |s|}$  
     *(Ramps up quickly at early temperatures to establish solid airflow baseline, then flattens out)*

### Formula for Point $i \in [0, 15]$:
$$t_i = \frac{i}{15}$$
$$T_i = T_{min} + t_i \cdot (T_{max} - T_{min})$$
$$P_i = P_{min} + f(t_i, s) \cdot (P_{max} - P_{min})$$

---

## 6. Stdin / Stdout JSON Command Protocol

Communication between `app-compose` and `open-aqua-engine` is 100% JSON over process invocation:

### 1. Discover Devices
```bash
$ open-aqua-engine devices
```
```json
[
  {
    "id": "octo_0",
    "device_type": "octo",
    "name": "Aquacomputer OCTO",
    "serial": "13235-54067",
    "firmware": 1020,
    "hidraw_path": "/dev/hidraw9",
    "hwmon_path": "/sys/class/hwmon/hwmon4",
    "fan_count": 8,
    "temp_sensor_count": 4
  }
]
```

### 2. Read Telemetry
```bash
$ open-aqua-engine telemetry --device octo_0
```
```json
{
  "device_id": "octo_0",
  "sensors": [
    {"id": "temp1", "name": "Sensor 1", "temp_c": 35.4},
    {"id": "temp2", "name": "Sensor 2", "temp_c": null}
  ],
  "fans": [
    {"id": "fan1", "name": "Pump", "rpm": 2184, "power_w": 0.0, "pwm_pct": 19.0},
    {"id": "fan2", "name": "Upper Rad", "rpm": 1367, "power_w": 1.13, "pwm_pct": 53.0},
    {"id": "fan3", "name": "Front Rad", "rpm": 805, "power_w": 0.49, "pwm_pct": 20.0},
    {"id": "fan4", "name": "Rear Exhaust", "rpm": 1023, "power_w": 0.21, "pwm_pct": 26.0}
  ]
}
```

### 3. Read Hardware Curves
```bash
$ open-aqua-engine curves --device octo_0
```
```json
{
  "device_id": "octo_0",
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
```bash
$ open-aqua-engine flash --device octo_0 --fan fan4 --json '[{"temp":25.0,"pwm":15.0}, ...]'
```
```json
{
  "status": "success",
  "device_id": "octo_0",
  "fan": "fan4",
  "bytes_written": 1631,
  "verified": true
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
    - Dual Curve View: Dotted line for *Current Hardware Curve*, solid colored line for *Draft Curve*.
    - "Flash to Device" button with instant tactile feedback.
  - **Profiles Tab**: Save/Export/Import fan configurations in human-readable JSON files (*Silent*, *Gaming*, *Stress Test*).

---

## 8. Development & Build Roadmap

| Milestone | Deliverables |
| :--- | :--- |
| **Phase 1: Project Scaffolding** | Setup multi-module Gradle project (`core-model`, `engine-native`, `app-compose`), configure Kotlin 2.x and Compose Desktop plugin. |
| **Phase 2: Generic Engine & Models** | Implement `core-model` (models & curve math) + `engine-native` with generic device discovery, telemetry, curves, and flashing. Wire UPX packaging into Gradle. |
| **Phase 3: Engine Bridge & Lifecycle** | Create `EngineBridge` in `app-compose` to extract embedded binary to cache and run CLI commands via coroutines with `StateFlow`. |
| **Phase 4: Curve Studio Canvas** | Build the interactive Compose Desktop Canvas curve editor with draggable handles, shape slider, and live coolant operating point. |
| **Phase 5: Dashboard & Polish** | Add live hardware gauges, fan channel naming, theme styling (Dark Modern Glass), and desktop distribution packaging (`.deb`, `.rpm`, runnable bundle). |
