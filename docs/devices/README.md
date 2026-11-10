# OpenAqua Device Documentation

This directory contains technical specifications, reverse-engineering documentation, and protocol references for hardware supported by OpenAqua.

## Documents

* **[SPECIFICATIONS.md](SPECIFICATIONS.md)**: **The Comprehensive Technical Reference Manual**
  * Cross-platform OS driver architecture (Linux `/dev/hidraw` & `USBDEVFS` vs. Windows Win32 HID & `WinUSB`).
  * Telemetry Input Report (`0x01`) structure and offsets (temperatures, fan metrics, tachometers, flow rates).
  * Configuration Feature Report (`0x03`) and Secondary Commit Report (`0x02`) structure (16-point piecewise linear curve tables, fan setup guardrails, CRC-16/USB).
  * Virtual sensor bulk report (`0x04`) payload layouts (OCTO 51-byte vs. QUADRO 67-byte).
  * Real-world empirical findings, hardware quirks, and lessons learned (flow sensor indexing gap, firmware curve power rescaling, USB endpoint stability).
  * Device hardware matrix for OCTO, QUADRO, and D5 NEXT.

* **[LED_DESIGN.md](../LED_DESIGN.md)**: **LED & RGBpx Architecture and UI/UX Design Specification**
  * Subsystem audit: Core Model, Device Engine Mock vs. Production Engine.
  * Low-level hardware protocols: WS2812B/RGBpx virtual controllers and routing tables, fixed HSV zones, 13-bit PWM analog RGB.
  * Domain models and state management: `LightingZoneConfig`, `TempReactiveConfig`, `AppStoredData`.
  * Compose Multiplatform UI: Navigation integration, interactive WS2812B canvas simulator, autonomous temperature-reactive mode, and accessory library.
  * Step-by-step implementation roadmap across engine and app layers.
