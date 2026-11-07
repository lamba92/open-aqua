# Aqua Computer Hardware & Protocol Specifications

> **Technical Reference Manual for OpenAqua**: Low-level communication protocols, OS driver interfaces, memory layouts, byte-level payloads, and empirical hardware findings for Aqua Computer USB devices.

---

## Table of Contents

1. [Executive Overview & Architectural Philosophy](#1-executive-overview--architectural-philosophy)
2. [Cross-Platform Hardware Access (Linux vs. Windows)](#2-cross-platform-hardware-access-linux-vs-windows)
   - [Universal USB Topology](#21-universal-usb-topology)
   - [Linux Implementation (`/dev/hidraw` & USBDEVFS)](#22-linux-implementation-devhidraw--usbdevfs)
   - [Windows Implementation (Win32 HID & WinUSB)](#23-windows-implementation-win32-hid--winusb)
   - [Why a Native Kotlin/Native Engine?](#24-why-a-native-kotlinnative-engine)
3. [Empirical Findings, Hardware Quirks & Lessons Learned](#3-empirical-findings-hardware-quirks--lessons-learned)
   - [Virtual Sensor Bulk Report Size (51 vs. 67 Bytes)](#31-virtual-sensor-bulk-report-size-51-vs-67-bytes)
   - [Hardware Sensor Index Mapping (The Flow Sensor Gap)](#32-hardware-sensor-index-mapping-the-flow-sensor-gap)
   - [Firmware Fan Power Rescaling (Hardware Min/Max Scaling)](#33-firmware-fan-power-rescaling-hardware-minmax-scaling)
   - [Curve Shape Discretization and Exponent Preservation](#34-curve-shape-discretization-and-exponent-preservation)
   - [USB Control Endpoint Wedging (`EPROTO 71`)](#35-usb-control-endpoint-wedging-eproto-71)
   - [False-Positive Telemetry Masking in UI](#36-false-positive-telemetry-masking-in-ui)
4. [Telemetry Specification (Input Report `0x01`)](#4-telemetry-specification-input-report-0x01)
   - [Report Characteristics & Transmission Rate](#41-report-characteristics--transmission-rate)
   - [Header & System Telemetry](#42-header--system-telemetry)
   - [Physical & Virtual Temperature Telemetry](#43-physical--virtual-temperature-telemetry)
   - [Cooling Channel (Fan / Pump) Telemetry](#44-cooling-channel-fan--pump-telemetry)
   - [Flow Sensor Telemetry](#45-flow-sensor-telemetry)
5. [Configuration Specification (Feature Report `0x03` & Commit `0x02`)](#5-configuration-specification-feature-report-0x03--commit-0x02)
   - [Checksum Algorithm (CRC-16-USB)](#51-checksum-algorithm-crc-16-usb)
   - [Secondary Commit Report (`0x02`)](#52-secondary-commit-report-0x02)
   - [Global Calibration Blocks (Flow Pulses & Sensor Offsets)](#53-global-calibration-blocks-flow-pulses--sensor-offsets)
   - [Fan Setup Structure (Hardware Limits & Guardrails)](#54-fan-setup-structure-hardware-limits--guardrails)
   - [Fan Control Structure (16-Point Curve Table & Modes)](#55-fan-control-structure-16-point-curve-table--modes)
6. [Virtual Sensors Feed Specification (USB Bulk Report `0x04`)](#6-virtual-sensors-feed-specification-usb-bulk-report-0x04)
   - [OCTO Bulk Report Structure (51 Bytes)](#61-octo-bulk-report-structure-51-bytes)
   - [QUADRO Bulk Report Structure (67 Bytes)](#62-quadro-bulk-report-structure-67-bytes)
   - [Feed Lifecycle, Expiration & Fallback Dynamics](#63-feed-lifecycle-expiration--fallback-dynamics)
7. [Device Hardware Matrix & Offset Reference](#7-device-hardware-matrix--offset-reference)

---

## 1. Executive Overview & Architectural Philosophy

Aqua Computer devices (**OCTO**, **QUADRO**, **D5 NEXT**, **High Flow Next**, **Farbwerk 360**, and **aquaero 5/6**) are embedded microcontrollers equipped with on-board non-volatile memory (EEPROM/Flash), analog-to-digital converters (ADCs), PWM motor drivers, tachometer frequency counters, and programmable RGBpx lighting controllers.

### Autonomous Hardware Autonomy
A fundamental tenet of Aqua Computer hardware design is **complete host autonomy**:
* The host computer is **not** responsible for calculating real-time fan curves or evaluating PID control loops.
* Instead, the host compiles fan curve lookup tables, sensor assignments, power limits, and calibration offsets, and flashes them directly into the device's control block (`Report 0x03`).
* Once written, the microcontroller executes cooling algorithms independently in hardware. If the operating system crashes, freezes, reboots, or sleeps, the device continues regulating thermal dissipation uninterrupted.

OpenAqua adheres strictly to this model. Rather than running a heavyweight daemon that continuously evaluates fan speeds in user-space, OpenAqua serves as a high-precision configurator and telemetry stream monitor, only actively transmitting data when feeding host software telemetry (e.g. CPU/GPU package temperatures) into the device's virtual sensor registers.

---

## 2. Cross-Platform Hardware Access (Linux vs. Windows)

### 2.1 Universal USB Topology

All Aqua Computer modern USB controllers share a common USB vendor and endpoint architecture:

* **USB Vendor ID (VID)**: `0x0C70` (`AQUACOMPUTER_VENDOR_ID`)
* **Product IDs (PID)**:
  * **OCTO**: `0xF011`
  * **QUADRO**: `0xF00D`
  * **D5 NEXT**: `0xF00E`
  * **FARBWERK 360**: `0xF010`
  * **HIGH FLOW NEXT**: `0xF012`
  * **LEAKSHIELD**: `0xF013`

#### USB Interfaces & Endpoints
Every physical device enumerates as a USB Composite Device with at least two interfaces:
1. **Interface 0 — Vendor-Specific Class (`0xFF`)**:
   * **Endpoint `0x02` OUT (Bulk)**: Used exclusively for streaming high-frequency software telemetry (Virtual Sensors Report `0x04`).
2. **Interface 1 — Human Interface Device (`HID`) Class (`0x03`)**:
   * **Endpoint `0x81` IN (Interrupt)**: Used for asynchronous Input Reports (Live Telemetry Report `0x01`).
   * **Endpoint `0x00` (Control)**: Standard USB control pipe used for reading/writing HID Feature Reports (Configuration Report `0x03`) and HID Output Reports (Secondary Commit Report `0x02`).

```
                              ┌──────────────────────────────────────────────┐
                              │    Aqua Computer USB Controller (0x0C70)     │
                              └──────┬────────────────────────────────┬──────┘
                                     │                                │
                     Interface 0 (Vendor / Bulk)          Interface 1 (HID Class)
                                     │                                │
                             Endpoint 0x02 OUT                Endpoint 0x81 IN / EP0
                                     │                                │
                         ┌───────────▼───────────┐        ┌───────────▼───────────┐
                         │ Virtual Sensors Feed  │        │ Telemetry: Report 0x01│
                         │     (Report 0x04)     │        │ Config:    Report 0x03│
                         │   51 / 67-Byte Bulk   │        │ Commit:    Report 0x02│
                         └───────────────────────┘        └───────────────────────┘
```

---

### 2.2 Linux Implementation (`/dev/hidraw` & USBDEVFS)

On Linux, OpenAqua interacts directly with the kernel using standard POSIX subsystem calls through Kotlin/Native's POSIX bindings, avoiding third-party dynamic libraries.

#### 1. Device Discovery & Identification
OpenAqua parses sysfs entries under `/sys/class/hidraw/`:
1. Iterates over `/sys/class/hidraw/hidrawN/device/uevent`.
2. Inspects `HID_ID` (format `BUS:VENDOR:PRODUCT`, matching `0003:00000C70:0000F011`).
3. Extracts device serial number from `HID_UNIQ`.
4. Matches the Product ID against `DeviceRegistry`.

#### 2. Telemetry Streaming (Input Report `0x01`)
* Opens `/dev/hidrawN` with `O_RDWR | O_NONBLOCK`.
* Uses POSIX `poll()` on the file descriptor with `POLLIN`.
* When ready, calls `read(fd, buf, 512)`.
* Verifies `buf[0] == 0x01` and length matches `descriptor.statusReportLength`.
* Parses packet into strongly typed Kotlin data structures (`DeviceTelemetry`).

#### 3. Configuration Read/Write (Feature Report `0x03`)
HID Feature reports cannot be read or written using raw `read()`/`write()`. Instead, Linux kernel `hidraw` provides specific ioctls:

```c
// Macro definitions for HID feature transfer
#define HIDIOCGFEATURE(len) _IOC(_IOC_READ|_IOC_WRITE, 'H', 0x07, len)
#define HIDIOCSFEATURE(len) _IOC(_IOC_WRITE|_IOC_READ, 'H', 0x06, len)

// Reading Configuration:
uint8_t buffer[reportLength + 1];
buffer[0] = 0x03; // Report ID
ioctl(fd, HIDIOCGFEATURE(sizeof(buffer)), buffer);

// Writing Configuration:
// buffer[0] = 0x03; buffer[1..len-3] = payload; buffer[len-2..len-1] = CRC16
ioctl(fd, HIDIOCSFEATURE(sizeof(buffer)), buffer);
```

#### 4. Virtual Sensors Feeding via USBDEVFS
The Linux HID subsystem does not expose vendor-specific bulk endpoints on Interface 0. OpenAqua accesses the underlying USB device node via USBDEVFS:
1. Resolves the sysfs path `/sys/class/hidraw/hidrawN/device` via POSIX `realpath()`.
2. Climbs directory levels until finding `busnum` and `devnum`.
3. Opens `/dev/bus/usb/{busnum:03d}/{devnum:03d}` with `O_RDWR`.
4. Claims Interface 0 using `ioctl(fd, USBDEVFS_CLAIMINTERFACE, &interface_0)`.
5. Transmits the raw bulk report via `ioctl(fd, USBDEVFS_BULK, &bulk_transfer)` targeting Endpoint `0x02`.

#### 5. Required Linux Udev Permissions
By default, USB device nodes and hidraw nodes are restricted to `root:root`. For non-root operation, OpenAqua provides a udev rule (`/etc/udev/rules.d/70-open-aqua.rules`):
```udev
SUBSYSTEM=="hidraw", ATTRS{idVendor}=="0c70", TAG+="uaccess"
SUBSYSTEM=="usb", ATTRS{idVendor}=="0c70", TAG+="uaccess"
```

---

### 2.3 Windows Implementation (Win32 HID & WinUSB)

On Windows, OpenAqua communicates using native Win32 APIs via Kotlin/Native's `cinterop` against `hid.dll`, `setupapi.dll`, and `winusb.dll`.

#### 1. Device Discovery & Identification
* Calls `HidD_GetHidGuid(&hidGuid)` and queries the system device tree using `SetupDiGetClassDevsW` with `DIGCF_PRESENT | DIGCF_DEVICEINTERFACE`.
* Enumerates interfaces with `SetupDiEnumDeviceInterfaces` and retrieves device symbolic paths using `SetupDiGetDeviceInterfaceDetailW`.
* Opens each candidate device path via `CreateFileW` with `GENERIC_READ | GENERIC_WRITE`.
* Calls `HidD_GetAttributes` to validate `VendorID == 0x0C70`.
* Calls `HidD_GetSerialNumberString` to extract device unique identity.

#### 2. Telemetry Streaming (Input Report `0x01`)
* Uses Win32 `ReadFile` over the device handle opened with `FILE_FLAG_OVERLAPPED`.
* Reads incoming 512-byte frames and validates `buf[0] == 0x01`.

#### 3. Configuration Read/Write (Feature Report `0x03`)
* **Reading**: Calls `HidD_GetFeature(handle, buffer, length)`.
* **Writing**: Calls `HidD_SetFeature(handle, buffer, length)`.

#### 4. The Critical Windows Report `0x02` Output Report Quirk
On Windows, attempting to write Secondary Commit Report `0x02` via `HidD_SetFeature` returns **`FALSE`** with `GetLastError() == ERROR_INVALID_PARAMETER (87)`.
* **Cause**: In the Aqua Computer HID Report Descriptor, Report ID `0x02` is explicitly defined as an **Output Report**, whereas Report ID `0x03` is defined as a **Feature Report**.
* While the Linux kernel's `HIDIOCSFEATURE` ioctl is permissive and routes Report `0x02` regardless of descriptor classification, Windows enforces strict report type parity.
* **Solution**: On Windows, Report `0x02` must be transmitted using `HidD_SetOutputReport(handle, buffer, length)` or standard `WriteFile()`.

#### 5. Virtual Sensors Feeding via WinUSB
To access Bulk Endpoint `0x02` on Interface 0 on Windows:
* Enumerates device interfaces registered under the standard WinUSB Interface Class GUID (`{A5DCBF10-6530-11D2-901F-00C04FB951ED}`).
* Initializes a WinUSB handle using `WinUsb_Initialize(fileHandle, &winUsbHandle)`.
* Configures pipe timeout via `WinUsb_SetPipePolicy(winUsbHandle, 0x02, PIPE_TRANSFER_TIMEOUT, sizeof(timeout), &timeout)`.
* Writes the 51/67-byte payload via `WinUsb_WritePipe(winUsbHandle, 0x02, buffer, length, &transferred, NULL)`.

---

### 2.4 Why a Native Kotlin/Native Engine?

OpenAqua avoids running native hardware code inside the JVM (e.g. via `libusb`, `usb4java`, or `hidapi` wrappers):
1. **JVM Crash Immunity**: Native driver crashes or segfaults (e.g., pulling a USB cable mid-bulk transfer) terminate only the isolated helper process, never taking down the Compose Desktop UI.
2. **Zero Native Dependency Hell**: No external dynamic libraries (`.so` or `.dll`) need to be installed in system directories. The compiled binary (`open-aqua-engine`) is statically linked, bundled in JAR application resources, extracted to user cache on first run, and executed via `ProcessBuilder`.
3. **Strict Separation of Privilege**: The engine can be cleanly invoked with elevated privileges or via polkit/sudo if udev permissions are missing, without elevating the graphical user interface.

---

## 3. Empirical Findings, Hardware Quirks & Lessons Learned

During the reverse-engineering and empirical hardware validation of OpenAqua, several critical hardware and firmware discrepancies were uncovered.

---

### 3.1 Virtual Sensor Bulk Report Size (51 vs. 67 Bytes)

* **Initial Assumption**: Early open-source reverse-engineering projects (derived from QUADRO captures) documented a 67-byte bulk report payload on Endpoint `0x02` OUT:
  ```
  Byte 0:      Report ID (0x04)
  Bytes 1..32: 16 x u16 BE centi-°C temperature values
  Bytes 33..48: 16 x u8 sensor type flags (0x03 = temperature)
  Bytes 49..64: 16 x 0x64 (padding block)
  Bytes 65..66: CRC16-USB over bytes [1..64]
  Total:       67 bytes
  ```
* **Hardware Failure on OCTO**:
  When sending this 67-byte report to an Aqua Computer OCTO:
  * The Linux `ioctl(USBDEVFS_BULK)` returned `67` (reporting OS success).
  * However, Status Report `0x01` slots `0x45..0x64` remained stuck at `0x7FFF` (`Disconnected`), and sensor types remained `0x00`.
  * **The OCTO microcontroller firmware silently rejected the 67-byte payload.**
* **Empirical Resolution**:
  Hardware probing proved that OCTO firmware expects strictly **51 bytes** (matching the Leakshield template in the Linux kernel driver):
  ```
  Byte 0:      Report ID (0x04)
  Bytes 1..32: 16 x u16 BE centi-°C temperature values
  Bytes 33..48: 16 x u8 sensor type flags (0x03 = temperature)
  Bytes 49..50: CRC16-USB over bytes [1..48] (Stored BE)
  Total:       51 bytes
  ```
  * Note: The CRC calculation must run strictly over bytes `[1..48]`, **excluding** Report ID byte `0`.
* **Architecture Fix**: `DeviceDescriptor` includes `virtualSensorsReportSize` (configured as `0x33` / 51 bytes for OCTO, and `0x43` / 67 bytes for QUADRO).

---

### 3.2 Hardware Sensor Index Mapping (The Flow Sensor Gap)

In OpenAqua's software domain, temperature sensors are indexed sequentially:
* `Channel 0..3`: Physical NTC Sensors 1..4
* `Channel 4..19`: Virtual Software Sensors 1..16

However, inside the microcontroller's hardware fan configuration register (`Report 0x03`, bytes `+0x03..+0x04` `temp_sensor`), the register allocation is:

| Hardware Register Index | Device Hardware Binding                   | OpenAqua Software Domain |
|-------------------------|-------------------------------------------|--------------------------|
| `0`                     | Physical Sensor 1                         | `Channel 0`              |
| `1`                     | Physical Sensor 2                         | `Channel 1`              |
| `2`                     | Physical Sensor 3                         | `Channel 2`              |
| `3`                     | Physical Sensor 4                         | `Channel 3`              |
| **`4`**                 | **Flow Sensor (Pulses/Flow Calculation)** | *N/A (Flow Metric)*      |
| **`5`**                 | **Virtual Software Sensor 1 (Slot 0)**    | **`Channel 4`**          |
| `6`                     | Virtual Software Sensor 2 (Slot 1)        | `Channel 5`              |
| `...`                   | `...`                                     | `...`                    |
| `20`                    | Virtual Software Sensor 16 (Slot 15)      | `Channel 19`             |

* **Consequence of Unmapped Indices**:
  Writing OpenAqua's `Channel 4` directly to hardware caused the fan controller to bind to hardware index `4` (the Flow Sensor). Because no flow meter was attached, the sensor evaluated to disconnected / 0 °C, and the fan ran at fallback speed (~24%–29% PWM) while the CPU was thermal throttling.
* **Resolution**:
  `ConfigWriter` and `ConfigParser` apply dynamic translation for devices with flow sensors (`OCTO` and `QUADRO`):
  $$\text{HwSensorIndex} = \begin{cases} \text{Channel}, & \text{if } \text{Channel} < 4 \\ \text{Channel} + 1, & \text{if } \text{Channel} \ge 4 \end{cases}$$

---

### 3.3 Firmware Fan Power Rescaling (Hardware Min/Max Scaling)

A subtle and critical feature of Aqua Computer firmware across **OCTO, QUADRO, D5 NEXT, and aquaero** is the automatic rescaling of curve controller outputs.

#### Verbatim Specification (OCTO Manual Section 10.3 / QUADRO Section 10.3 / D5 NEXT Section 11.3):
> *"The output value in percent corresponding to the current temperature is scaled to the range between minimum and maximum fan power settings before being applied to the fan output."*

#### Mathematical Firmware Formula:
$$\text{PWM}_{\text{actual}} = \text{MinPower} + \left(\frac{\text{CurvePWM}}{100.0}\right) \times (\text{MaxPower} - \text{MinPower})$$

Every fan header has independent electrical guardrail registers in the `Fan setup` block:
* `min_percent`: Lowest electrical duty cycle allowed (e.g. `5.0%` factory default on OCTO Fan 4).
* `max_percent`: Highest electrical duty cycle allowed (typically `100.0%`).

#### Observed Telemetry Discrepancies Explained:
1. **Idle Curve Discrepancy (25% Curve $\rightarrow$ 28.8% Telemetry)**:
   $$\text{PWM} = 5.0\% + (25.0\% \times 0.95) = 5.0\% + 23.75\% = 28.75\% \approx 28.8\%$$
2. **Operating Point Discrepancy (42.7% Curve $\rightarrow$ 45.6% Telemetry)**:
   $$\text{PWM} = 5.0\% + (42.7\% \times 0.95) = 5.0\% + 40.565\% = 45.565\% \approx 45.6\%$$

#### Behavior Across Fan Modes:
* **Curve Controller Mode**: Proportional linear rescaling across `[MinPower .. MaxPower]`.
* **Manual / Preset Power Mode**: Absolute value (1:1), clamped to `[MinPower .. MaxPower]`. If `hold_min_power` flag is `0`, setting `0%` shuts off the fan.
* **Target Temperature (PID) Mode**: Clamped saturation limits (anti-windup bounds).

---

### 3.4 Curve Shape Discretization and Exponent Preservation

* The hardware microcontroller stores exactly **16 discrete `(temp, pwm)` coordinates**. It does not evaluate continuous mathematical functions; it executes piecewise linear interpolation between adjacent points.
* In OpenAqua, curves are generated using Aquasuite's exponential curve model ($T_{min}, T_{max}, P_{min}, P_{max}, \text{shape}$).
* **The Bug**: Converting raw 16-point tables back into drafts hardcoded `shape = 0.0`. Opening an existing progressive curve (`shape = 5.0`) silently flattened it to a linear curve.
* **The Solution**: `CurveMath.inferShape` performs non-linear least-squares fitting of the 16 normalized coordinate offsets against $t^{1+s}$ (or $1 - (1-t)^{1+|s|}$ for digressive shapes) to preserve progressive profiles when modifying configurations.

---

### 3.5 USB Control Endpoint Wedging (`EPROTO 71`)

On Linux, opening `/dev/bus/usb/BBB/DDD`, claiming Interface 0 (`USBDEVFS_CLAIMINTERFACE`), sending a bulk packet, and immediately releasing the interface (`USBDEVFS_RELEASEINTERFACE` or closing the file descriptor) causes the device's USB control endpoint (EP0) to crash. Subsequent `HIDIOCGFEATURE` and `HIDIOCSFEATURE` calls on `/dev/hidrawN` fail with POSIX errno `71` (`EPROTO`).

* **Mitigation**: OpenAqua maintains a persistent `UsbBulkSession` across virtual sensor feed cycles. The interface remains claimed throughout the application lifecycle, avoiding interface detach thrashing.

---

### 3.6 False-Positive Telemetry Masking in UI

In earlier revisions, the UI models fell back to displaying the host-side feed temperature whenever device telemetry reported `Disconnected`. Consequently, when the OCTO silently rejected 67-byte bulk reports, the user interface displayed live CPU temperatures and marked the sensor as "Operational," masking the hardware failure.

* **Resolution**: UI status indicators strictly evaluate `telemetry is SensorTelemetry.Connected`, requiring confirmation from device Status Report `0x01` before displaying operational status.

---

## 4. Telemetry Specification (Input Report `0x01`)

### 4.1 Report Characteristics & Transmission Rate

* **Report ID**: `0x01`
* **Transport**: HID Interrupt In (Endpoint `0x81`) or polled read on `/dev/hidraw`
* **Update Interval**: Autonomous stream transmitted approximately every **1000 ms** (~1 Hz)
* **Report Lengths**:
  * **OCTO**: 327 bytes (`0x147`)
  * **QUADRO**: 165 bytes (`0xA5`)
  * **D5 NEXT**: 118 bytes (`0x76`)

---

### 4.2 Header & System Telemetry

| Byte Offset | Type / Width | Description       | Units / Format                     |
|-------------|--------------|-------------------|------------------------------------|
| `0x00`      | `u8`         | Report ID         | Always `0x01`                      |
| `0x0D`      | `u16 BE`     | Firmware Revision | Raw integer (e.g. `1020` = v10.20) |

---

### 4.3 Physical & Virtual Temperature Telemetry

All temperature values are encoded as **16-bit Signed Big-Endian integers in centidegrees Celsius** ($T \times 100$).
* Disconnected / Invalid Probe Value: `0x7FFF` (`32767`) or `-32768`.

#### Temperature Offsets by Device:
* **OCTO**:
  * Physical Sensors 1..4: `0x3D`, `0x3F`, `0x41`, `0x43` (2 bytes each)
  * Virtual Sensors 1..16: `0x45 + (slot * 2)` (Slots `0..15`, spanning `0x45..0x64`)
  * Virtual Sensor Types: `0x65 + slot` (1 byte each, `3` = temperature)
* **QUADRO**:
  * Physical Sensors 1..4: `0x34`, `0x36`, `0x38`, `0x3A`
  * Virtual Sensors 1..16: `0x3C + (slot * 2)`
  * Virtual Sensor Types: `0x5C + slot`
* **D5 NEXT**:
  * Coolant Temperature: `0x57`
  * Virtual Sensors 1..8: `0x3F + (slot * 2)`

---

### 4.4 Cooling Channel (Fan / Pump) Telemetry

Each cooling channel reports a **13-byte telemetry block**:

| Channel Offset Relative to Base | Type / Width | Parameter                 | Unit / Conversion                                |
|---------------------------------|--------------|---------------------------|--------------------------------------------------|
| `+0x00`                         | `u16 BE`     | PWM Duty Cycle            | Centipercent ($P \times 100$, divide by 100.0)   |
| `+0x02`                         | `u16 BE`     | Voltage                   | Centivolts ($V \times 100$, divide by 100.0)     |
| `+0x04`                         | `u16 BE`     | Current                   | Milliamperes (mA, divide by 1000.0)              |
| `+0x06`                         | `u16 BE`     | Power Dissipation         | Centiwatts ($W \times 100$, divide by 100.0)     |
| `+0x08`                         | `u16 BE`     | Fan Tachometer            | Revolutions Per Minute (RPM)                     |
| `+0x0A`                         | `u16 BE`     | Reserved / Internal Flags | Raw                                              |
| `+0x0C`                         | `u8`         | Channel Status Flags      | Bits `0x04` / `0x08` indicate stall / disconnect |

#### Fan Telemetry Base Offsets:
* **OCTO**: Channels 0..7 start at `0x7D + (ch * 0x0D)`
  * Fan 1: `0x7D`
  * Fan 2: `0x8A`
  * Fan 3: `0x97`
  * Fan 4: `0xA4`
  * Fan 5: `0xB1`
  * Fan 6: `0xBE`
  * Fan 7: `0xCB`
  * Fan 8: `0xD8`
* **QUADRO**: Channels 0..3 start at `0x70 + (ch * 0x0D)`
* **D5 NEXT**:
  * Pump (Channel 0): `0x6C`
  * Fan Header (Channel 1): `0x5F`

---

### 4.5 Flow Sensor Telemetry

* **OCTO**: Offset `0x7B` (16-bit unsigned BE integer).
* **QUADRO**: Offset `0x6E` (16-bit unsigned BE integer).
* **Unit**: Deciliters per hour ($\text{dL/h}$). Divide by $10.0$ to obtain Liters per hour ($\text{L/h}$).

---

## 5. Configuration Specification (Feature Report `0x03` & Commit `0x02`)

### 5.1 Checksum Algorithm (CRC-16-USB)

Every Control Report `0x03` must terminate with a 16-bit CRC calculated over the entire payload:
* **Report ID**: Excluded from CRC calculation (`[1 .. length - 2]`).
* **Polynomial**: `0x8005` (bit-reversed representation: `0xA001`, $x^{16} + x^{15} + x^2 + 1$).
* **Initial Value**: `0xFFFF`
* **Final XOR**: `0xFFFF`
* **Storage Location**: Last two bytes of Report `0x03` in **Big-Endian** format.

```kotlin
fun crc16Usb(data: UByteArray, startIndex: Int, length: Int): UShort {
    var crc = 0xFFFF
    for (i in startIndex until (startIndex + length)) {
        crc = crc xor (data[i].toInt() and 0xFF)
        for (b in 0 until 8) {
            crc = if ((crc and 0x0001) != 0) (crc ushr 1) xor 0xA001 else crc ushr 1
        }
    }
    return (crc xor 0xFFFF).toUShort()
}
```

---

### 5.2 Secondary Commit Report (`0x02`)

Writing Report `0x03` updates the microcontroller's RAM registers. To commit these changes permanently into EEPROM, a secondary 11-byte sequence must be written immediately after:

```hex
02 00 00 00 02 00 00 00 00 34 C6
```
*(Bytes 9..10 contain the precomputed CRC-16 checksum `0x34C6`).*

---

### 5.3 Global Calibration Blocks (Flow Pulses & Sensor Offsets)

#### 1. Flow Calibration
* **OCTO & QUADRO Offset**: `0x06` (16-bit unsigned Big-Endian integer).
* Represents the calibration constant in **Pulses per Liter** (imp/L).

#### 2. Physical Sensor Temperature Offsets
* **OCTO & QUADRO Offset**: `0x0A` (spans 8 bytes for 4 physical sensors).
* **D5 NEXT Offset**: `0x2D`.
* Formatted as signed 16-bit Big-Endian centidegrees Celsius ($\Delta T \times 100$). Added algebraically to ADC readings.

---

### 5.4 Fan Setup Structure (Hardware Limits & Guardrails)

Preceding the fan control blocks in Report `0x03`, each physical fan channel has a 9-byte electrical guardrail block:
* **OCTO Setup Offsets**: `0x12 + (ch * 0x09)` (Channels 0..7 span `0x12..0x59`)
* **QUADRO Setup Offsets**: `0x12 + (ch * 0x09)` (Channels 0..3 span `0x12..0x35`)
* **D5 NEXT Setup Offsets**: Fan (Channel 1) at `0x2F`, Pump (Channel 0) at `0x38`

| Relative Offset | Width    | Field Name    | Description                                          |
|-----------------|----------|---------------|------------------------------------------------------|
| `+0x00`         | `u8`     | `flags`       | Bit 0: `hold_min_power`, Bit 1: `start_boost`        |
| `+0x01`         | `u16 BE` | `min_percent` | Hardware minimum power floor (centi-%, $0..10000$)   |
| `+0x03`         | `u16 BE` | `max_percent` | Hardware maximum power ceiling (centi-%, $0..10000$) |
| `+0x05`         | `u16 BE` | `fallback`    | Fallback power if sensor disconnects (centi-%)       |
| `+0x07`         | `u16 BE` | `graph_rpm`   | Upper limit for display scaling                      |

In OpenAqua, these registers map to the `FanLimits` data class (`minPowerPercent`, `maxPowerPercent`, `startBoost`, `holdMinPower`, `fallbackPowerPercent`) embedded directly within `FanChannelConfig.limits`. When flashing via `FlashConfigRequest(fans = mapOf(ch to config))`, `ConfigWriter.encodeFanLimits` updates bytes `+0x00..+0x06` whenever `config.limits != null`, while preserving the existing display scaling (`graph_rpm`). Dedicated limits updates can also be flashed using `LimitsOnlyFanConfig(channel, limits)`.

---

### 5.5 Fan Control Structure (16-Point Curve Table & Modes)

Each fan channel occupies an **85-byte (`0x55`)** control block:
* **OCTO Base Offset**: `0x5A + (ch * 0x55)`
* **QUADRO Base Offset**: `0x36 + (ch * 0x55)`

#### Fan Channel Control Block Layout:

```
Offset          Length      Field
─────────────────────────────────────────────────────────────────────────────
+0x00           1 byte      Fan Mode:
                              0x00 = Fixed / Manual Power Preset
                              0x01 = Target Temperature (PID)
                              0x02 = Curve Controller (16-Point Table)
                              0x05 = Two-Point Controller
+0x01..0x02     2 bytes     Fixed Power Setting (u16 BE, centi-%)
+0x03..0x04     2 bytes     Sensor Hardware Index (u16 BE):
                              0..3  = Physical Sensors 1..4
                              4     = Flow Sensor
                              5..20 = Virtual Software Sensors 1..16
+0x05..0x06     2 bytes     Target Temperature Setting (i16 BE, centi-°C)
+0x07..0x12     12 bytes    PID Controller Coefficients & Timing Parameters
+0x13..0x14     2 bytes     Start Temperature Threshold (i16 BE, centi-°C)
+0x15..0x34     32 bytes    16 x Temperature Coordinates (16 x i16 BE centi-°C)
+0x35..0x54     32 bytes    16 x Power / PWM Coordinates (16 x u16 BE centi-%)
─────────────────────────────────────────────────────────────────────────────
Total: 85 bytes (0x55)
```

#### Detailed Look at the 16 Curve Coordinate Points:
* **Temperatures (`+0x15` to `+0x34`)**:
  * Point 0: `+0x15..+0x16`
  * Point 1: `+0x17..+0x18`
  * `...`
  * Point 15: `+0x33..+0x34`
* **Duty Cycles (`+0x35` to `+0x54`)**:
  * Point 0: `+0x35..+0x36`
  * Point 1: `+0x37..+0x38`
  * `...`
  * Point 15: `+0x53..+0x54`

---

## 6. Virtual Sensors Feed Specification (USB Bulk Report `0x04`)

Virtual sensors allow host software (OpenAqua) to stream host metrics (CPU package, GPU core, motherboard VRM, ambient delta) into the controller over USB.

* **Interface**: Interface 0 (Vendor Specific)
* **Endpoint**: Endpoint `0x02` OUT (Bulk)
* **Report ID**: `0x04`
* **Transport**: USBDEVFS bulk transfer (Linux) / WinUSB WritePipe (Windows)

---

### 6.1 OCTO Bulk Report Structure (51 Bytes)

```
Byte Offset     Length      Description
─────────────────────────────────────────────────────────────────────────────
0x00            1 byte      Report ID (0x04)
0x01..0x20      32 bytes    16 x Temperature Slots (u16 BE, centi-°C)
                              Unused/Inactive slot: 0x7FFF
0x21..0x30      16 bytes    16 x Sensor Type Flags (u8):
                              0x03 = Temperature Source
                              0x00 = Inactive / Unused Slot
0x31..0x32      2 bytes     CRC-16-USB over bytes [1..48] (Stored Big-Endian)
─────────────────────────────────────────────────────────────────────────────
Total: 51 bytes (0x33)
```

---

### 6.2 QUADRO Bulk Report Structure (67 Bytes)

```
Byte Offset     Length      Description
─────────────────────────────────────────────────────────────────────────────
0x00            1 byte      Report ID (0x04)
0x01..0x20      32 bytes    16 x Temperature Slots (u16 BE, centi-°C)
0x21..0x30      16 bytes    16 x Sensor Type Flags (u8, 0x03 = Temp)
0x31..0x40      16 bytes    Padding Block: 16 x 0x64
0x41..0x42      2 bytes     CRC-16-USB over bytes [1..64] (Stored Big-Endian)
─────────────────────────────────────────────────────────────────────────────
Total: 67 bytes (0x43)
```

---

### 6.3 Feed Lifecycle, Expiration & Fallback Dynamics

1. **Hardware Watchdog**:
   * When the microcontroller receives a valid bulk report, it updates its internal virtual sensor table and resets an on-board hardware watchdog timer.
   * If no valid bulk packet arrives for **10 to 15 seconds**, the microcontroller invalidates all virtual sensor slots, transitioning them to `0x7FFF` (`Disconnected`).
2. **Fallback Fan Speed Activation**:
   * Any fan channel bound to an expired virtual sensor triggers its safety fallback protocol:
   * The fan accelerates to its programmed `fallback` duty cycle (typically `100%` or `10000` centi-%).
3. **Recommended Host Transmission Frequency**:
   * OpenAqua transmits virtual sensor bulk reports at **1.0 Hz** (every 1000 ms), providing robust headroom against watchdog timeouts while minimizing USB bus utilization.

---

## 7. Device Hardware Matrix & Offset Reference

| Metric / Parameter                 |   Aquacomputer OCTO   |  Aquacomputer QUADRO  |   Aquacomputer D5 NEXT    |
|------------------------------------|:---------------------:|:---------------------:|:-------------------------:|
| **USB Product ID (PID)**           |       `0xF011`        |       `0xF00D`        |         `0xF00E`          |
| **Cooling Channels**               |     8 (PWM Fans)      |     4 (PWM Fans)      |    2 (1 Pump + 1 Fan)     |
| **Physical Temp Sensors**          |      4 (NTC 10k)      |      4 (NTC 10k)      |     1 (Coolant Probe)     |
| **Virtual Sensors**                |          16           |          16           |             8             |
| **Flow Sensor Input**              | Dedicated Port (dL/h) | Dedicated Port (dL/h) |   Virtual / Calculated    |
| **RGBpx Ports**                    |        2 Ports        |        1 Port         |          1 Port           |
| **Integrated OLED Display**        |         None          |         None          | 128x64 Graphical Display  |
| **Status Report Length (`0x01`)**  |  `0x147` (327 bytes)  |  `0xA5` (165 bytes)   |    `0x76` (118 bytes)     |
| **Control Report Length (`0x03`)** | `0x65F` (1631 bytes)  |  `0x3C1` (961 bytes)  |    `0x329` (809 bytes)    |
| **Virtual Sensors Report Size**    | **51 Bytes (`0x33`)** | **67 Bytes (`0x43`)** |   **51 Bytes (`0x33`)**   |
| **Fan Telemetry Stride**           |   `0x0D` (13 bytes)   |   `0x0D` (13 bytes)   |       Fixed offsets       |
| **Fan Telemetry Base**             |        `0x7D`         |        `0x70`         | Pump: `0x6C`, Fan: `0x5F` |
| **Fan Control Stride**             |   `0x55` (85 bytes)   |   `0x55` (85 bytes)   |       Fixed offsets       |
| **Fan Control Base**               |        `0x5A`         |        `0x36`         | Pump: `0x96`, Fan: `0x41` |
| **Physical Temp Telemetry Base**   |        `0x3D`         |        `0x34`         |          `0x57`           |
| **Virtual Temp Telemetry Base**    |        `0x45`         |        `0x3C`         |          `0x3F`           |
| **Flow Telemetry Offset**          |        `0x7B`         |        `0x6E`         |            N/A            |
| **Flow Pulses Ctrl Offset**        |        `0x06`         |        `0x06`         |            N/A            |
| **Temp Sensor Ctrl Offset**        |        `0x0A`         |        `0x0A`         |          `0x2D`           |
| **Firmware Version Offset**        |        `0x0D`         |        `0x0D`         |          `0x0D`           |
