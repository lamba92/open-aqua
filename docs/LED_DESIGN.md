# OpenAqua LED & RGBpx Subsystem Architecture & Design Specification

> **Technical Architecture & UI/UX Design Manual for LED & RGBpx Lighting Management in OpenAqua**  
> Covers hardware communication protocols, memory block offsets, data models, state management, and Compose Multiplatform UI components.

---

## Table of Contents

1. [Executive Overview & Design Philosophy](#1-executive-overview--design-philosophy)
2. [Current Codebase & Device Engine Audit](#2-current-codebase--device-engine-audit)
   - [Core Model Status](#21-core-model-status)
   - [Device Engine Mock Status](#22-device-engine-mock-status)
   - [Production Device Engine Status](#23-production-device-engine-status)
   - [Summary of Deficiencies & Gaps](#24-summary-of-deficiencies--gaps)
3. [Low-Level Hardware & USB Protocol Specification](#3-low-level-hardware--usb-protocol-specification)
   - [Hardware Generation 1: Modern Digital RGBpx (WS2812B / SK6812)](#31-hardware-generation-1-modern-digital-rgbpx-ws2812b--sk6812)
   - [Hardware Generation 2: Fixed HSV Zone Models (D5 NEXT, High Flow Next)](#32-hardware-generation-2-fixed-hsv-zone-models-d5-next-high-flow-next)
   - [Hardware Generation 3: Classic Farbwerk 12V Analog RGB (13-Bit PWM)](#33-hardware-generation-3-classic-farbwerk-12v-analog-rgb-13-bit-pwm)
4. [Proposed Data Models & Domain Architecture](#4-proposed-data-models--domain-architecture)
   - [Extensions to `core-model-device`](#41-extensions-to-core-model-device)
   - [Persistence in `AppStoredData`](#42-persistence-in-appstoreddata)
5. [Application & UI/UX Design](#5-application--uiux-design)
   - [Navigation & Scaffold Integration](#51-navigation--scaffold-integration)
   - [Master Lighting Bar](#52-master-lighting-bar)
   - [Physical Topology & Strip Visualizer](#53-physical-topology--strip-visualizer)
   - [Interactive WS2812B Canvas Simulator](#54-interactive-ws2812b-canvas-simulator)
   - [Effect Studio & Control Modes](#55-effect-studio--control-modes)
   - [Hardware Autonomous Temperature-Reactive Mode](#56-hardware-autonomous-temperature-reactive-mode)
   - [Accessory Library & Hardware Presets](#57-accessory-library--hardware-presets)
6. [Implementation Roadmap](#6-implementation-roadmap)

---

## 1. Executive Overview & Design Philosophy

Aqua Computer devices (**OCTO**, **QUADRO**, **D5 NEXT**, **Farbwerk 360**, **High Flow Next**, **Leakshield**) provide high-precision digital addressable lighting controllers alongside their fan and pump capabilities. 

### Autonomous Hardware Operation
In strict alignment with OpenAqua's core architectural principle, **lighting must operate autonomously on the microcontroller**:
* The host computer compiles lighting effect modes, parameters, palettes, routing tables, and sensor bindings into the device's non-volatile configuration memory (Feature Report `0x03`).
* Once committed via Output Report `0x02`, the microcontroller executes all animation loops, color transitions, and temperature-reactive lookups directly in hardware.
* **Zero Host CPU Overhead**: The host does not stream RGB frames over USB during standard operation. If the operating system crashes, sleeps, or reboots, the hardware continues to illuminate and react to coolant temperatures without interruption.

---

## 2. Current Codebase & Device Engine Audit

An exhaustive audit of the OpenAqua codebase reveals the following status:

### 2.1 Core Model Status
* **`DeviceCapability.kt`**:
  Defines `DeviceCapability.Lighting.Rgbpx` and `DeviceCapability.Lighting.ClassicAnalogRgb`. All primary controllers (OCTO, QUADRO, D5 NEXT, FARBWERK 360, HIGH FLOW NEXT, LEAKSHIELD, AQUASTREAM ULTIMATE, FARBWERK Classic) correctly declare these capabilities in `DeviceRegistry.kt`.
* **`Protocol.kt`**:
  Contains initial stub definitions:
  * `RgbColor(r, g, b)`
  * `RgbEffectMode` (`OFF`, `STATIC`, `BREATHING`, `RAINBOW`, `COLOR_FLOW`, `TEMP_REACTIVE`)
  * `RgbpxPortConfig(port, enabled, ledCount, brightnessPercent, mode, primaryColor, secondaryColor, sensorIndex, speed)`
  * `DeviceConfig.rgbpx: List<RgbpxPortConfig>`
  * `FlashConfigRequest.rgbpx: Map<Int, RgbpxPortConfig>?`
* **`DeviceValidation.kt`**:
  Validates that requested RGBpx port indices fall within `0 until rgbpxCap.portCount`.

### 2.2 Device Engine Mock Status
* **`MockStorage.kt`**:
  Default mock device states for OCTO, QUADRO, D5 NEXT, and FARBWERK 360 are initialized with mock `RgbpxPortConfig` entries.
* **`MockHardwareController.kt`**:
  Applies incoming `FlashConfigRequest.rgbpx` updates to `MockStorage` via `applyRgbpxUpdates`.

### 2.3 Production Device Engine Status
* **`ConfigParser.kt` (Reading from Hardware)**:
  Does **not** parse RGBpx data from incoming Feature Report `0x03` buffers. Instead, line 313 invokes `defaultRgbpxPorts(...)` which returns hardcoded synthetic objects (30 LEDs, 100% brightness, static mode).
* **`ConfigWriter.kt` (Writing/Flashing to Hardware)**:
  Does **not** handle `request.rgbpx` at all in `applyFlashConfig(...)`. `request.rgbpx` is completely dropped before signing and transmission.
* **`OctoHardwareTest.kt`**:
  Line 35 explicitly notes:  
  > *"RGBpx: Flashing is not yet tested here as report byte offsets and encoding for RGBpx hardware controller memory are pending implementation in ConfigWriter."*

### 2.4 Summary of Deficiencies & Gaps
1. **Engine**: Report byte offsets for RGBpx blocks on OCTO (`0xF011`) and QUADRO (`0xF00D`) are not yet integrated into `ConfigParser` or `ConfigWriter`.
2. **Protocol**: The real hardware supports **up to 20 virtual controller slots** mapped via a **routing table** to physical ports, rather than a simplistic 1:1 port-to-mode mapping.
3. **UI**: No lighting tab exists in `CommonDeviceTab` or `DeviceScaffold`.

---

## 3. Low-Level Hardware & USB Protocol Specification

### 3.1 Hardware Generation 1: Modern Digital RGBpx (WS2812B / SK6812)
Found on **Farbwerk 360**, **OCTO**, and **QUADRO**.

* **Physical Header**: 4-pin Molex PicoBlade (1.25 mm pitch): Pin 1: `+5V DC`, Pin 2: `Data Out` (800 kHz NZR), Pin 3: `Data Return` (sensing), Pin 4: `GND`.
* **Capacity**: Max 90 individually addressable LEDs per physical port (OCTO: 2 ports = 180 LEDs; QUADRO: 1 port = 90 LEDs; Farbwerk 360: 4 ports = 360 LEDs).

#### Feature Report `0x03` Memory Layout (Farbwerk 360 Template: 1666 Bytes)
```
Byte Range        Size (Bytes)   Description
─────────────────────────────────────────────────────────────────────────────────
0                 1 byte         Report ID (0x03)
0..18             19 bytes       Header. Global Master Brightness stored at Byte 16 (0..100%).
19..1418          1400 bytes     20 Virtual Controller Slots × 70 bytes each.
                                 Slot k starts at: 19 + (70 × k)
1419..1478        60 bytes       Routing Table: Up to 20 records × 3 bytes each.
1479..1601        123 bytes      Reserved / Constant padding.
1602..1663        62 bytes       Footer / Constant padding.
1664..1665        2 bytes        CRC-16-USB over bytes [1..1663] (Stored Big-Endian).
─────────────────────────────────────────────────────────────────────────────────
Total: 1666 bytes (0x0682)
```

#### Virtual Controller Slot Anatomy (70 Bytes)
```
Offset in Slot    Length         Description
─────────────────────────────────────────────────────────────────────────────────
+0                1 byte         Slot Enable: 0x0F = Active, 0x00 = Disabled.
+1                1 byte         Effect Mode ID (0x01 = Static, 0x02 = Breathing, etc.).
+2..+4            3 bytes        Constant mode skeleton.
+5                1 byte         Mode Option Flags bitfield.
+6..+8            3 bytes        Constant mode skeleton.
+9                1 byte         Constant 0x0F (Virtual strip width = 15 LEDs).
+10..+22          13 bytes       Constant mode skeleton.
+23..+40          18 bytes       Up to 9 × 16-bit Little-Endian Effect Parameters (Words k0..k8).
+41..+45          5 bytes        Zero padding.
+46..+69          24 bytes       6-entry Palette (4 bytes each in [A, G, R, B] channel order):
                                 +0: A (Alpha / intensity, always 0xFF)
                                 +1: G (Green channel: 0..255)
                                 +2: R (Red channel: 0..255)
                                 +3: B (Blue channel: 0..255)
                                 Entry 0 serves as Background Color where applicable.
─────────────────────────────────────────────────────────────────────────────────
Total Slot Size: 70 bytes
```

#### Option Flags Bitfield (`+5`)
* `0x01` / `0x02`: Reverse Direction
* `0x04`: Fade / Smooth Transition
* `0x08`: Random Color Selection
* `0x10`: Slide Colors / Snowfall Mode
* `0x20`: Alternative Color Mode 2
* `0x40`: Color Change On Cycle
* `0x80`: Circular Loop

#### Effect Mode IDs & Parameters
| Mode ID | Effect Name | Parameters (Words at `+23+2k`) | Palette Entries |
| :---: | :--- | :--- | :--- |
| `0x01` | **Static** | None | Single color at `+47..+49` (`[G, R, B]`) |
| `0x02` | **Breathing** | $k_0$: Speed, $k_1$: Intensity, $k_2$: Delay Max, $k_3$: Delay Min | Entry 0: Target color |
| `0x03` | **Rotating Rainbow** | $k_0$: Speed, $k_1$: Color range | Internal rainbow |
| `0x04` | **Blinking** | $k_0$: Speed, $k_1$: Number of FG colors | Entry 0: Background, Entries 1..$N$: Foreground |
| `0x05` | **Color Change** | $k_0$: Speed, $k_1$: Number of colors | Entries 0..$N-1$: Color list |
| `0x07` | **Sequence** | $k_0$: Speed, $k_1$: Smoothness, $k_2$: Count, $k_3$: Delay After, $k_4$: Delay Before | Entry 0: Background, Entries 1..$N$: Colors |
| `0x08` | **Scanner** | $k_0$: Speed, $k_1$: Smoothness, $k_2$: Width, $k_3$: Start Delay, $k_4$: Min Interval, $k_5$: Max Interval | Entry 0: Background, Entry 1: Color 1, Entry 2: Color 2 |
| `0x09` | **Laser** | Identical layout to Scanner ($k_0..k_5$) | Entry 0: Background, Entry 1: Color 1, Entry 2: Color 2 |
| `0x0A` | **Wave** | $k_0$: Speed, $k_1$: Smoothness, $k_2$: Width, $k_5$: Start Delay, $k_7$: Max Interval | Entry 0: Background, Entries 1..$N$: Colors |
| `0x0B` | **Color Sequence** | $k_0$: Speed, $k_1$: Smoothness, $k_2$: 1 (constant), $k_3$: Count, $k_4$: Change Speed | Entries 0..$N-1$: Colors |
| `0x0C` | **Color Shift** | $k_0$: Speed, $k_1$: Color range, $k_2$: Total area | Entry 0: Base color |
| `0x0E` | **Flame** | $k_0$: Intensity | Entry 0: Background, Entry 1: Flame 1, Entry 2: Flame 2 |
| `0x0F` | **Rain** | $k_0$: Drop speed, $k_1$: Drop items, $k_2$: Drop size, $k_3$: Smoothness, $k_4$: Runtime, $k_5$: Min Interval, $k_6$: Max Interval | Entry 0: Background, Entry 1: Drop color |
| `0x10` | **Snowfall** | Identical layout to Rain ($k_0..k_6$) | Entry 0: Background, Entry 1: Flake color |
| `0x11` | **Stardust** | Identical layout to Rain ($k_0..k_6$) | Entry 0: Background, Entry 1: Star color |
| `0x13` | **Swiping Rainbow**| $k_0$: Point speed, $k_1$: Smoothness, $k_2$: Size, $k_3$: Change speed, $k_4$: Range | Entry 0: Point color, Entry 1: Strip color |

#### Routing Table Format (Bytes 1419..1478)
20 entries of 3 bytes each:
* **Byte 0**: `Controller ID` = Virtual slot index + 1 (1-based index).
* **Byte 1**: `Channel ID` = 0-based physical port index (`0` = RGBpx 1 .. `3` = RGBpx 4).
* **Byte 2**: `Start LED Index` = 0-based offset on the physical strip (`0` = LED 1).
* **Sorting Rule**: Entries must be sorted by **Channel ascending, Controller descending**.

### 3.2 Hardware Generation 2: Fixed HSV Zone Models (D5 NEXT, High Flow Next)
* Feature Report `0x03` contains 8 dedicated lighting zones (~70 bytes each).
* Color encoding uses **HSV** rather than RGB:
  * **Hue**: 16-bit Big-Endian integer ($0..1535$, scaled across $360^\circ$).
  * **Saturation**: 8-bit unsigned integer ($0..255$).
  * **Brightness**: 8-bit unsigned integer ($0..255$).

### 3.3 Hardware Generation 3: Classic Farbwerk 12V Analog RGB (13-Bit PWM)
* 4 analog 12V RGB channels (12 subchannels total: $4 \times [R, G, B]$).
* High-resolution **13-bit hardware PWM** (0 to 8,191 discrete duty steps) for flicker-free logarithmic dimming.

---

## 4. Proposed Data Models & Domain Architecture

### 4.1 Extensions to `core-model-device`

```kotlin
package io.github.lamba92.openaqua.model.device

import kotlinx.serialization.Serializable

/**
 * Represents a virtual lighting zone mapped to a slice of a physical RGBpx port.
 */
@Serializable
data class LightingZoneConfig(
    val slotIndex: Int,
    val name: String = "",
    val physicalPort: Int,
    val startLedIndex: Int,
    val ledCount: Int,
    val mode: RgbEffectMode,
    val speed: Int = 50,
    val brightnessPercent: Int = 100,
    val palette: List<RgbColor> = emptyList(),
    val flags: LightingFlags = LightingFlags(),
    val sensorIndex: Int? = null,
    val tempReactiveConfig: TempReactiveConfig? = null,
)

@Serializable
data class LightingFlags(
    val reverseDirection: Boolean = false,
    val smoothFade: Boolean = true,
    val randomColors: Boolean = false,
    val circularLoop: Boolean = true,
)

@Serializable
data class TempReactiveConfig(
    val sensorIndex: Int,
    val lowTempCelsius: Double = 30.0,
    val lowColor: RgbColor = RgbColor(0, 150, 255),
    val midTempCelsius: Double = 50.0,
    val midColor: RgbColor = RgbColor(0, 255, 120),
    val highTempCelsius: Double = 75.0,
    val highColor: RgbColor = RgbColor(255, 30, 0),
    val alertThresholdCelsius: Double? = 80.0,
    val alertStrobe: Boolean = true,
)

/**
 * Top-level lighting configuration for a device.
 */
@Serializable
data class DeviceLightingConfig(
    val masterEnabled: Boolean = true,
    val masterBrightnessPercent: Int = 100,
    val zones: List<LightingZoneConfig> = emptyList(),
)
```

### 4.2 Persistence in `AppStoredData`
Stored under `DeviceStoredData` in `AppStoredData.kt`:

```kotlin
@Serializable
data class DeviceStoredData(
    val name: ChannelName = ChannelName.Default,
    val fans: Map<Int, FanStoredData> = emptyMap(),
    val sensors: Map<Int, SensorStoredData> = emptyMap(),
    val lighting: LightingStoredData = LightingStoredData.DEFAULT,
)

@Serializable
data class LightingStoredData(
    val zoneNames: Map<Int, String> = emptyMap(),
    val savedProfiles: List<LightingProfilePreset> = emptyList(),
) {
    companion object {
        val DEFAULT = LightingStoredData()
    }
}
```

---

## 5. Application & UI/UX Design

### 5.1 Navigation & Scaffold Integration
* Add `CommonDeviceTab.LIGHTING` to `CommonDeviceTab`.
* In `DeviceScaffold.kt`, display the lighting tab whenever `descriptor.hasCapability<DeviceCapability.Lighting>()`:
  ```kotlin
  if (descriptor.hasCapability<DeviceCapability.Lighting>()) {
      DeviceDrawerTabItem(
          label = stringResource(Res.string.nav_lighting),
          icon = painterResource(Res.drawable.ic_lighting),
          selected = selectedTab == CommonDeviceTab.LIGHTING,
          onClick = { onSelectTab(CommonDeviceTab.LIGHTING) },
          colors = drawerColors,
      )
  }
  ```

### 5.2 Master Lighting Bar
Placed at the top of the lighting view:
* **Master Power Toggle**: Instant global ON / OFF toggle.
* **Global Brightness Slider**: Adjusts device-level brightness (0% to 100%) mapped to Byte 16 of Report `0x03`.
* **Quick Profiles Dropdown**: Pre-set hardware profiles:
  * *Stealth / Dark*: All zones disabled.
  * *Clean White*: 6500K neutral white illumination.
  * *Thermal Monitor*: All zones bound to Coolant / CPU temperatures.
  * *Cyberpunk Neon*: Dual-tone Cyan & Magenta wave.
* **Draft Banner & Flash Button**: Matches the Curve Studio paradigm (`"You have uncommitted lighting changes" -> [Flash to Hardware]`).

### 5.3 Physical Topology & Strip Visualizer
A vertical left pane displaying physical outputs:
* **Port Cards**:
  * Shows physical port capacity (e.g. `RGBpx Port 1: 57 / 90 LEDs used`).
  * Progress gauge showing remaining LED budget.
  * List of assigned virtual zones on that port.
* **Add Zone / Accessory Button**:
  * Opens the Accessory Library dialog to quickly attach known components.

### 5.4 Interactive WS2812B Canvas Simulator
An animated Compose `Canvas` providing instant visual feedback:
* Renders each physical/virtual LED as an emissive circular diode with radial gradient glow.
* Live animation shader reflects the selected effect (breathing rate, wave sweep, color shifts).
* Interactive selection: clicking an LED or range highlights the corresponding virtual zone.

### 5.5 Effect Studio & Control Modes
When a zone is selected, the right workspace presents the Effect Studio:
1. **Mode Picker Grid**:
   * *Static Color* (Solid tint, multi-color split)
   * *Breathing* (Adjustable rate, intensity, min/max wait)
   * *Rainbow Wave* (Direction, frequency, speed)
   * *Color Shift* (Smooth palette transitions)
   * *Scanner / Marquee* (Width, trail decay, bounce)
   * *Ambient* (Rain, Snowfall, Flame)
   * *Temperature Reactive* (Hardware autonomous thermal monitor)
2. **Palette Editor**:
   * Up to 6 color stops with hex input, RGB/HSV sliders, and quick swatches.
3. **Motion Modifiers**:
   * Direction toggle (`Reverse`), Transition smoothing (`Fade`), Loop style (`Circular`).

### 5.6 Hardware Autonomous Temperature-Reactive Mode
The signature thermal-computing feature:
* **Source Binding**: Dropdown selecting any available sensor (Integrated Coolant Temp, Physical NTC Probes 1..4, or Virtual Software Sensors).
* **Thermal Gradient Bar**:
  * Interactive horizontal gradient with 3 draggable temperature stops:
    * Cold threshold ($T_{\min}$, e.g. 30°C $\rightarrow$ Cyan)
    * Nominal threshold ($T_{\text{mid}}$, e.g. 45°C $\rightarrow$ Green)
    * Warm threshold ($T_{\text{max}}$, e.g. 65°C $\rightarrow$ Red)
* **Critical Alarm Strobe**:
  * Checkbox: Enable rapid red strobe if temperature exceeds safety limit ($T_{\text{alarm}}$, e.g. >75°C).

### 5.7 Accessory Library & Hardware Presets
A catalog modal allowing one-click setup without manual LED counting:
* **Official Aquacomputer Hardware**:
  * *RGBpx LED Strip 30cm* (15 LEDs)
  * *RGBpx LED Strip 50cm* (30 LEDs)
  * *RGBpx Lighting Ring for D5 NEXT / ULTITUBE* (12 or 16 LEDs)
  * *High Flow Next Integrated Ring* (10 LEDs)
  * *Leakshield Accent Ring* (8 LEDs)
* **Standard 3rd-Party Components (via Splitty or Adapter)**:
  * *120mm / 140mm ARGB Fans* (12, 16, or 32 LEDs)
  * *GPU Water Block* (15 to 30 LEDs)
  * *Distribution Plate / Reservoir* (18 to 40 LEDs)
  * *Custom LED Count* ($N$ LEDs)

---

## 6. Implementation Roadmap

### Milestone 1: Engine Protocol Implementation
- [ ] Determine exact control report offsets for OCTO (`0xF011`) and QUADRO (`0xF00D`) lighting blocks.
- [ ] Implement virtual controller decoding in `ConfigParser.kt`.
- [ ] Implement virtual controller and routing table serialization in `ConfigWriter.kt`.
- [ ] Update `ProtocolMappingTest` and `OctoHardwareTest` with real byte-level verification.

### Milestone 2: Domain & Persistence Integration
- [ ] Extend `Protocol.kt` with `LightingZoneConfig`, `LightingFlags`, and `TempReactiveConfig`.
- [ ] Update `DeviceConfig` and `FlashConfigRequest` to support full zone models.
- [ ] Add `LightingStoredData` to `AppStoredData.kt` for zone naming and presets.

### Milestone 3: ViewModel & UI Architecture
- [ ] Add `CommonDeviceTab.LIGHTING` and update `DeviceDrawerScaffold.kt`.
- [ ] Implement `LightingViewModel` using OpenAqua's MVI framework (`LightingUiState`, `LightingUiEvent`, `LightingUiEffect`).
- [ ] Build the physical port and zone list components.

### Milestone 4: Interactive Components & Canvas Preview
- [ ] Develop `LedStripCanvas.kt` for live animated WS2812B rendering.
- [ ] Develop `ThermalGradientBar.kt` for temperature-reactive visual threshold editing.
- [ ] Implement the Accessory Catalog dialog.
- [ ] Wire the flash confirmation dialog to `DeviceDataCacheService.flashConfig(...)`.
