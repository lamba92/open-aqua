<div align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset=".github/assets/banner-readme-dark.svg" />
    <source media="(prefers-color-scheme: light)" srcset=".github/assets/banner-readme-light.svg" />
    <img alt="open-aqua" src=".github/assets/banner-readme-dark.svg" width="600" />
  </picture>
</div>

# OpenAqua

> **An open-source, cross-platform thermal management and telemetry suite for Aquacomputer devices, designed for Linux and Windows.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0--Beta-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Linux%20%7C%20Windows-blue.svg)](https://github.com/lamba92/openaqua)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

---

## What is OpenAqua?

Aquacomputer hardware (**OCTO**, **QUADRO**, **D5 NEXT**, etc.) represents the gold standard in custom PC liquid cooling and thermal control. However, the official proprietary software (**Aquasuite**) is Windows-only (WPF/.NET) and tied to licensing models. On Linux, users have historically relied on Windows virtual machines or incomplete reverse-engineered scripts.

**OpenAqua** provides a modern, transparent, and completely free thermal management solution built with Kotlin Multiplatform:
- **Autonomous Hardware Operation**: Curves and static fan profiles are written directly to the device's onboard EEPROM/firmware. Once flashed, the device executes the curve in hardware—cooling remains fully active without needing any background daemon running.
- **Cross-Platform**: Designed from the ground up for both **Linux** (native HIDRAW ioctl + sysfs) and **Windows** (native Win32 HID subsystem).
- **Stateless Native Engine**: A lightweight, standalone CLI binary (`open-aqua-engine`) that handles hardware discovery, low-level USB HID reports, CRC-16 checksums, and telemetry streaming.

---

## ⚠️ Current Project Status

> **Important**: At this stage of development, **only the native hardware engine (`open-aqua-engine`) is implemented and fully operational**.

| Component | Status | Description |
| :--- | :---: | :--- |
| **Native Engine CLI (`:engine:engine-prod`)** | 🟢 **Working** | Fully functional on both **Linux** (`linuxX64`) and **Windows** (`mingwX64`). Supports discovery, live telemetry snapshot, continuous NDJSON telemetry streaming, EEPROM curve reading/flashing, virtual temperature sensor feeding, and device descriptor inspection. |
| **Mock Engine (`:engine:engine-mock`)** | 🟢 **Working** | Functional on **Linux**, **Windows**, and **macOS** (`macosArm64`) for local development without physical hardware attached. |
| **Desktop UI (`:app-compose`)** | 🟡 **In Development** | Compose Multiplatform Desktop frontend (Interactive Curve Studio, real-time gauges, local JSON profile management). |

---

## Supported Hardware & Testing Status

> [!WARNING]
> **Physical Hardware Disclaimer**: The maintainer only owns an **Aquacomputer OCTO** and has only physically tested on that hardware.
>
> While **Aquacomputer QUADRO** and **Aquacomputer D5 NEXT** have full device descriptors and protocol definitions implemented (based on reverse-engineering and driver specifications), **they have NOT been physically verified on hardware yet—expect bugs or untested edge cases on QUADRO and D5 NEXT**. Community testing, feedback, and issue reports for these devices are very welcome!

| Device | USB Product ID | Fan Channels | Temp Sensors | Flow Sensor | RGBpx | Physical Testing Status |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Aquacomputer OCTO** | `0xF011` | 8 Channels (PWM) | 4 Physical + 16 Virtual | Yes (Dedicated port, dL/h) | 2 Ports | 🟢 **Physically Verified & Tested** (Linux & Windows) |
| **Aquacomputer QUADRO** | `0xF00D` | 4 Channels (PWM) | 4 Physical + 16 Virtual | Yes (Dedicated port, dL/h) | 1 Port | 🟡 **Experimental / Untested on physical device** |
| **Aquacomputer D5 NEXT** | `0xF00E` | 2 (Pump + Fan) | 1 Liquid + 8 Virtual | Calculated / Virtual | 1 Port | 🟡 **Experimental / Untested on physical device** |

*(All devices share USB Vendor ID `0x0C70`)*

---

## Engine CLI (`open-aqua-engine`)

The engine binary provides a clean JSON / NDJSON command interface over stdin/stdout, making it ideal both for direct scripting and as the backend for the upcoming Compose Desktop UI.

```bash
$ open-aqua-engine --help
Usage: open-aqua-engine [<options>] <command> [<args>]...

  OpenAqua native hardware engine CLI. Provides low-level USB HID hardware
  control for Aquacomputer devices. Supported devices: OCTO (physically
  verified), QUADRO (experimental), D5 Next (experimental).
```

### 1. Discover Connected Devices
Enumerate all supported Aquacomputer devices connected via USB:
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

### 2. Inspect Supported Device Descriptors
Inspect hardware specifications, channel offsets, report sizes, and sensor capabilities. You can optionally filter by device type (`--type OCTO`):
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
        {"channel": 0, "ctrlOffset": 90, "statusOffset": 125},
        {"channel": 1, "ctrlOffset": 175, "statusOffset": 138}
      ],
      "tempSensors": [
        {"channel": 0, "hwmonIndex": 1, "isVirtual": false}
      ],
      "hasFlowSensor": true,
      "flowSensorOffset": 123,
      "virtualSensorsCount": 16
    }
  ]
}
```

### 3. Snapshot Telemetry
Read current real-time telemetry (temperatures, fan RPM, voltages, currents, power, flow rate):
```bash
$ open-aqua-engine telemetry --device octo-1 --pretty
```
```json
{
  "type": "octo",
  "flowRateDlH": 182,
  "fans": [
    {
      "channel": 0,
      "rpm": 2310,
      "powerWatts": 0.0,
      "voltageVolts": 12.03,
      "currentAmps": 0.0,
      "pwmPercent": 24.0
    }
  ],
  "sensors": [
    {
      "channel": 0,
      "tempCelsius": 36.2,
      "isVirtual": false
    }
  ]
}
```

### 4. Continuous Telemetry Streaming (NDJSON)
Continuously stream telemetry samples as line-delimited JSON (NDJSON):
```bash
$ open-aqua-engine stream-telemetry --device octo-1
```
*(Emits one self-contained JSON line per hardware report cycle)*

### 5. Read Hardware Configuration
Inspect the active curves, fixed PWM levels, and calibration offsets stored in the device's EEPROM:
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
        {"temp": 50.0, "pwm": 100.0}
      ]
    }
  ],
  "sensorOffsets": [
    {"channel": 0, "offsetCelsius": 0.0}
  ]
}
```

### 6. Flash Hardware Configuration
Atomically flash modified fan curves or fixed PWM settings into EEPROM. Unspecified channels remain untouched:
```bash
$ open-aqua-engine config flash --device octo-1 --request '{"fans":{"0":{"type":"fixed","channel":0,"powerPercent":50.0}}}'
```
Or flash from a JSON file:
```bash
$ open-aqua-engine config flash --device octo-1 --file custom-profile.json
```

### 7. Feed Software / Virtual Temperature Sensors
Stream external temperature readings (e.g., CPU package, GPU hotspot) from host monitoring tools directly to the hardware's virtual sensor slots:
```bash
$ echo '{"sensors":{"0":45.2,"1":58.0}}' | open-aqua-engine feed-virtual-sensors --device octo-1
```

---

## Platform Details & Permissions

### Linux
- **Driver**: Communicates with the kernel's `/dev/hidraw*` interface via `HIDIOCGFEATURE` and `HIDIOCSFEATURE` ioctls.
- **Rootless USB Access (udev)**: To run without `sudo`, install a udev rule (e.g. `/etc/udev/rules.d/99-aquacomputer.rules`):
  ```udev
  SUBSYSTEM=="hidraw", ATTRS{idVendor}=="0c70", MODE="0666", TAG+="uaccess"
  ```
  Then reload udev:
  ```bash
  sudo udevadm control --reload-rules && sudo udevadm trigger
  ```

### Windows
- **Driver**: Uses native Windows Win32 HID APIs (`hid.dll` via Kotlin/Native cinterop).
- **Report Routing**:
  - Control configuration (Report `0x03`): Transferred via `HidD_SetFeature`.
  - Secondary commit trigger (Report `0x02`): Written via `HidD_SetOutputReport` matching Windows HID report descriptors.
- No third-party drivers (like WinUSB or libusbK) required; works out-of-the-box with Windows default HID class driver.

---

## Building & Testing

### Prerequisites
- JDK 21+
- Kotlin 2.2+ (managed by Gradle wrapper)

### Build Executables
```bash
# Build Linux production native engine
./gradlew :engine:engine-prod:linuxX64Binaries

# Build Windows production native engine (cross-compiled or native)
./gradlew :engine:engine-prod:mingwX64Binaries

# Build mock engine (for UI development without physical hardware)
./gradlew :engine:engine-mock:assemble
```
Binaries will be placed in `engine/engine-prod/build/bin/<target>/releaseExecutable/`.

### Run Tests
```bash
# Run multiplatform unit tests & linter
./gradlew check

# Run live hardware tests on Linux (requires connected Aquacomputer OCTO)
./gradlew :engine:engine-prod:linuxX64Test -Ptest.hardware=true

# Run live hardware tests on Windows
# See docker/windows-test/README.md for instructions on using the automated QEMU/KVM Windows VM
```

---

## Further Documentation

- **[ARCHITECTURE.md](ARCHITECTURE.md)**: Detailed technical specification of the multi-module architecture, generic device abstraction, protocol byte offsets, CRC-16 implementation, and Aquasuite curve math replication.
- **[FEATURES.md](FEATURES.md)**: Functional and visual specification for the upcoming Compose Desktop UI and JSON profile management.
- **[.github/assets/BRAND_GUIDELINES.md](.github/assets/BRAND_GUIDELINES.md)**: Official brand color palette, typography, and icon asset specifications.
- **[docker/windows-test/README.md](docker/windows-test/README.md)**: Automated Docker + QEMU/KVM Windows testing environment for validating Windows builds against physical USB devices.

---

## License

OpenAqua is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
