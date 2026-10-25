# OpenAqua Brand & Design Guidelines

This document defines the official brand identity, color palette, typography, and icon asset specifications for **open-aqua**.

> **Reproducible Asset Generation**: All vector (`.svg`) and binary (`.png`, `.ico`, `.icns`) brand assets are generated deterministically by the `:assets` Kotlin/JVM module into [`assets/generated/`](generated/) (using shared color tokens from `:core-theme`) and committed to the repository. To regenerate and sync all assets after adjusting colors, geometry, or margins in [`BrandAssetGenerator.kt`](src/main/kotlin/io/github/lamba92/openaqua/brand/BrandAssetGenerator.kt) or [`OpenAquaPalette.kt`](../core-theme/src/main/kotlin/io/github/lamba92/openaqua/theme/OpenAquaPalette.kt), run:
> ```bash
> ./gradlew generateBrandAssets
> ```

---

## 1. Color Palette

| Role | Color Name | Hex | RGB | Usage |
| :--- | :--- | :--- | :--- | :--- |
| **Background / Canvas** | Deep Navy | `#0B192C` | `rgb(11, 25, 44)` | Squircle icon background, social banner background, `"open-"` logotype text on light backgrounds |
| **Primary Brand Mark** | Aqua Blue | `#32C3F2` | `rgb(50, 195, 242)` | Water droplet contour, `< >` code brackets, `"aqua"` logotype text |
| **Primary Contrast** | Pure White | `#FFFFFF` | `rgb(255, 255, 255)` | `"open-"` logotype text on dark backgrounds |
| **Accent** | Electric Cyan | `#5BE2FF` | `rgb(91, 226, 255)` | Interactive highlights, active telemetry accents |

---

## 2. Typography & Logotype

- **Font Family**: `Inter` or `Montserrat` (with system fallback: `system-ui, -apple-system, sans-serif`)
- **Font Weight**: Bold (`700`)
- **Letter Spacing**: `-2px` (at `112px` reference scale)
- **Case**: Strictly lowercase (`open-aqua`)
- **Color Split**:
  - Dark Theme / Deep Navy Canvas: `open-` in Pure White (`#FFFFFF`), `aqua` in Aqua Blue (`#32C3F2`)
  - Light Theme Canvas: `open-` in Deep Navy (`#0B192C`), `aqua` in Aqua Blue (`#32C3F2`)

---

## 3. Master Vector & Raster Assets

All master brand assets are stored in [`assets/generated/`](generated/) (with runtime UI icons synced to `app-compose/src/main/composeResources/drawable/`):

| File | Dimensions / ViewBox | Description |
| :--- | :--- | :--- |
| [`generated/icon-transparent.svg`](generated/icon-transparent.svg) | `512x512` | Standalone transparent water droplet + `< >` code brackets mark (`scale(0.88)`) |
| [`generated/app-icon-squircle.svg`](generated/app-icon-squircle.svg) | `512x512` (`rx="115"`) | Primary application icon with Deep Navy squircle background and optically centered margins (`scale(0.72)`) |
| [`generated/favicon-optimized.svg`](generated/favicon-optimized.svg) | `512x512` (`rx="96"`) | Small-scale optimized mark with heavier stroke weights (`40px` / `38px`, `scale(0.76)`) for `16x16` and `32x32` legibility |
| [`generated/banner-social-1200x630.svg`](generated/banner-social-1200x630.svg) | `1200x630` | OpenGraph / social preview vector master |
| [`generated/banner-social-1200x630.png`](generated/banner-social-1200x630.png) | `1200x630` | Rasterized OpenGraph / social preview banner |
| [`generated/banner-readme-dark.svg`](generated/banner-readme-dark.svg) | `1000x320` | Responsive README header banner for dark mode (`#FFFFFF` + `#32C3F2`) |
| [`generated/banner-readme-light.svg`](generated/banner-readme-light.svg) | `1000x320` | Responsive README header banner for light mode (`#0B192C` + `#32C3F2`) |
| [`generated/favicon.ico`](generated/favicon.ico) | `16x16`, `32x32`, `48x48`, `64x64`, `256x256` | Multi-resolution icon container for web and Windows packaging |
| [`generated/icon.icns`](generated/icon.icns) | `16x16`–`1024x1024` (`@1x`/`@2x`) | Multi-resolution Apple `.icns` bundle for macOS native packaging |
| [`generated/apple-touch-icon.png`](generated/apple-touch-icon.png) | `180x180` | Apple Touch icon rendered from `app-icon-squircle.svg` |
| [`generated/icon-192.png`](generated/icon-192.png) | `192x192` | Standard `192x192` app icon |
| [`generated/icon-512.png`](generated/icon-512.png) | `512x512` | High-resolution `512x512` app icon for desktop windowing and Linux packaging |
