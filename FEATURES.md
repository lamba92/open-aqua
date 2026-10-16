# OpenAqua UI & Profile Specification: Supported Features

> **Design and functional specification for OpenAqua's Desktop UI (`app-compose`), local profile management, and hardware synchronization.**

---

## 1. Core Philosophy: Best of Aquasuite with Linux Native Performance

Aquasuite set the standard for thermal management ergonomics, but its WPF architecture on Windows left Linux users without a first-class experience. OpenAqua adopts the most beloved ergonomics of Aquasuite while making configuration transparent, portable (human-readable JSON profiles), and 100% hardware-autonomous.

---

## 2. Feature Matrix: Supported Capabilities

### 2.1. Fan & Sensor Custom Naming
In stock hardware telemetry, channels appear as raw identifiers (`fan1`, `fan2`, `temp1`, `temp2`). OpenAqua provides rich, persistent user naming:
- **Custom Nicknames**: Rename any channel (e.g. `fan1` → `"D5 Pump"`, `fan2` → `"Top 360 Radiator"`, `temp1` → `"Coolant Delta"`).
- **Role Tags / Icons**: Optional visual tags in the UI (e.g. 💧 *Pump*, 🌀 *Radiator*, 🌬️ *Exhaust*, 🌡️ *Water Temp*, ⛅ *Ambient*).
- **Persistent Local Mapping**: Stored in `~/.config/open-aqua/profiles/default.json` and automatically correlated by device serial number.

---

### 2.2. Flexible Channel Control Modes (Quick Selectors)
Each fan/pump channel can be switched between 4 intuitive modes with a single click:

| Mode | Description | Hardware Behavior |
| :--- | :--- | :--- |
| **1. Fixed / Manual** | Lock channel to a static speed (e.g. Pump at 50%, or 100% for bleeding air). | Flashes a flat 16-point curve (or fixed PWM mode) to hardware. |
| **2. Automatic (Kurvenregler)** | Specify $T_{min}, T_{max}, P_{min}, P_{max}$ and curvature slider $s$ (Progressive, Linear, Digressive). | Generates 16 optimal coordinates and flashes to hardware. |
| **3. Manual Points (Expert)** | Drag individual coordinate points freely on the canvas grid or edit coordinate tables. | Flashes custom 16 coordinate pairs directly to hardware. |
| **4. Follow / Mirror** | Link channel to mirror another channel (e.g. *"Fan 3 follows Fan 2 (Top Radiator)"*). | Flashes master channel's curve to this channel; UI locks curve editor and shows sync status. |

---

### 2.3. The Automatic Curve Studio (Aquasuite Ergonomics)
For Automatic Curve generation, users configure 5 primary tactile controls:
1. **Source Sensor Selector**: Dropdown to select the controlling temperature sensor (e.g. `Sensor 1 (Coolant Temp)`).
2. **Temperature Bounds ($T_{min} \dots T_{max}$)**:
   - Example: $25^\circ\text{C}$ (Idle/Quiet) to $45^\circ\text{C}$ (Ramp limit).
3. **Power Bounds ($P_{min} \dots P_{max}$)**:
   - Example: $20\%$ (Min spin threshold) to $100\%$ (Maximum thermal protection).
4. **Curvature Slider ($s \in [-1.0, 1.0]$)**:
   - **$s > 0$ (Progressive / Exponential)**: Whisper-quiet at low-to-medium loads; steep acceleration near $T_{max}$.
   - **$s = 0$ (Linear)**: Constant proportional slope.
   - **$s < 0$ (Digressive / Logarithmic)**: Immediate baseline airflow ramp, flattening out at high temperatures.
5. **Instant Flash Feedback**: A prominent `"Flash to Hardware"` button with status indication (Verified bytes written to EEPROM).

---

### 2.4. Clean, High-FPS Canvas Visualization
Using Skiko and Compose Desktop Canvas:
- **0%–100% PWM vs. 15°C–60°C Graph**: High-contrast, clean dark grid with readable tick marks.
- **Dual Curve Display**:
  - **Dashed Subdued Line**: Active hardware curve currently flashed in EEPROM.
  - **Solid Highlighted Line**: Pending draft curve being adjusted in the UI.
- **Live Operating Point (Glowing Pill)**:
  - An animated marker tracking real-time coolant temperature along the curve.
  - Live tooltip showing `T: 32.4°C → PWM: 38% (1,240 RPM)`.
- **Linked Channel Banner**: When a channel is set to **Follow**, the graph displays a lock banner:
  > 🔗 *Following Top Radiator (Fan 2) • Source: Coolant Temp (temp1)*  
  > `[Unlink to edit independently]`

---

### 2.5. Portable JSON Profile Management
Unlike proprietary XML/service databases, OpenAqua stores profiles in clean, user-editable JSON in `~/.config/open-aqua/profiles/`:
```json
{
  "profileName": "Daily Quiet",
  "devices": {
    "13235-54067": {
      "name": "Custom Loop OCTO",
      "channels": {
        "fan1": {
          "name": "D5 Next Pump",
          "mode": "FIXED",
          "fixedPwm": 50.0
        },
        "fan2": {
          "name": "Top 360 Rad",
          "mode": "AUTO_CURVE",
          "sensorId": "temp1",
          "autoCurve": {
            "tMin": 28.0,
            "tMax": 42.0,
            "pMin": 25.0,
            "pMax": 100.0,
            "shape": 0.4
          }
        },
        "fan3": {
          "name": "Front 360 Rad",
          "mode": "FOLLOW",
          "followChannel": "fan2"
        }
      }
    }
  }
}
```

---

## 3. UI Navigation & Layout Hierarchy

```
┌────────────────────────────────────────────────────────────────────────┐
│ OpenAqua 💧 Coolant: 32.4°C  | Loop: Optimal | OCTO Connected (USB)  │
├──────────────┬─────────────────────────────────────────────────────────┤
│ DEVICES      │ [Top 360 Rad (fan2)]  [Front 360]  [D5 Pump]  [+]       │
│ • OCTO 0     ├─────────────────────────────────────────────────────────┤
│              │ Mode: ( ) Fixed   (•) Auto Curve   ( ) Custom   ( ) Sync│
│ PROFILES     │ Sensor: [ temp1: Coolant Temp (32.4°C) ▼ ]              │
│ • Daily Quiet├─────────────────────────────────────────────────────────┤
│ • Gaming     │   PWM %                                                 │
│ • Stress Test│   100┌───────────────────────────────────────*          │
│              │      │                                    .-/           │
│ SETTINGS     │    50│                            .---*---/             │
│ • UI Theme   │      │                  *---.----/                      │
│ • About      │    20*---------*--------/      (● Live: 32.4°C, 38%)    │
│              │     0└─────────┴────────┴────────┴────────┴───────      │
│              │       15°C    25°C     35°C     45°C     55°C     60°C  │
│              ├─────────────────────────────────────────────────────────┤
│              │ T_min: [28.0°C]   T_max: [42.0°C]   Shape: [──●────] 0.4│
│              │ P_min: [ 25 % ]   P_max: [ 100 %]   [⚡ Flash to EEPROM]│
└──────────────┴─────────────────────────────────────────────────────────┘
```

---

## 4. Architectural Separation: App vs Native Engine

- **`open-aqua-engine` (Native CLI)**:
  - Strictly stateless hardware execution.
  - Commands: `devices`, `telemetry`, `curves`, `flash curve-params`, `flash points`.
  - Knows nothing about channel nicknames, follow links, or GUI state.
- **`app-compose` (JVM / Compose Desktop)**:
  - Owns profile loading/saving (`~/.config/open-aqua/profiles/`).
  - Resolves channel nicknames and follow relationships.
  - Renders live graphs and dispatches batch flash commands across linked fans via `EngineBridge`.
