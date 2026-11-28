# CHROMECORE X — Ultimate Mobile Photography Platform

<p align="center">
  <strong>Professional computational photography for Android.</strong><br>
  <em>Chromecore / Metalheart design identity.</em><br>
  Target: Motorola Moto G85 5G (Snapdragon 6s Gen 3, Sony LYTIA 600)
</p>

---

## What is CHROMECORE X?

CHROMECORE X is a full-featured Android camera application with:

- **Camera2 + CameraX** engine with full manual controls
- **Computational photography** — multi-frame HDR, noise reduction, detail recovery
- **30+ filter presets** — Liquid Chrome, Metalheart, Teal & Orange, Film emulation, VHS, Pixel Art, and more
- **Non-destructive editor** — brightness, contrast, saturation, temperature, sharpness, vignette, grain
- **Custom creative effects** — RGB Split, Scanlines, Pixel Sort, Dithering, Pixelation
- **Device Laboratory** — real hardware capability scanning and diagnostic export
- **Chromecore design system** — obsidian black, chrome silver, liquid metal UI

## Architecture

```
app/src/main/kotlin/com/chromecore/camera/
├── engine/           Camera2 API engine with full lifecycle management
├── capabilities/     Device hardware scanner (Camera2, codecs, GPU)
├── processing/       Image processing pipeline (non-destructive)
├── filters/          Filter engine with 30+ named presets
├── ui/
│   ├── theme/        Chromecore Material3 design system
│   ├── navigation/   Type-safe Compose Navigation
│   ├── screens/
│   │   ├── camera/   Full-bleed camera with mode selector & filter strip
│   │   ├── editor/   Non-destructive photo editor
│   │   ├── gallery/  Grid gallery with MediaStore integration
│   │   ├── settings/ Settings with progressive disclosure
│   │   └── devicelab/ Hardware capability viewer
│   └── components/   Shared UI components
├── di/               Hilt dependency injection
├── models/           Data models
└── utils/            MediaStore helpers, file utilities
```

## Tech Stack

| Layer          | Technology                                      |
| -------------- | ----------------------------------------------- |
| Language       | Kotlin 2.1                                      |
| UI             | Jetpack Compose + Material3                     |
| Camera         | CameraX 1.4 + Camera2 API                       |
| DI             | Hilt (Dagger)                                   |
| Async          | Kotlin Coroutines + Flow                        |
| Image Loading  | Coil                                            |
| Storage        | MediaStore (scoped storage)                     |
| AI (planned)   | ML Kit, LiteRT                                  |
| GPU (planned)  | OpenGL ES 3.2, GLSL shaders                     |
| Build          | Gradle 8.7, Version Catalog                     |

## Build

```bash
./gradlew assembleDebug
```

Requires: Android SDK 35, JDK 17, Gradle 8.7+.

## Design Identity

**Obsidian Chrome** — the visual language of CHROMECORE X:

- Obsidian Black `#0A0A0C` — backgrounds
- Chrome Silver `#C8CCD0` — text and icons
- Platinum `#D4D7DC` — highlights
- Titanium Grey `#4A4A52` — secondary surfaces
- Cyan Accent `#4DD0E1` — interactive elements
- Cold White `#F5F5F7` — primary text

No glassmorphism. No purple gradients. Liquid chrome, polished metal, reflective surfaces.

## Development Phases

1. **P0 — Core Camera** ✅ — Camera2 engine, preview, capture, switch, flash, zoom
2. **P0 — Device Scanner** ✅ — Hardware capability probe with JSON export
3. **P1 — Editor** ✅ — Non-destructive adjustments + 30+ filter presets
4. **P1 — Gallery** ✅ — MediaStore integration, grid view
5. **P2 — AI Engine** — LiteRT models for denoise, super-res, segmentation
6. **P2 — GPU Shaders** — OpenGL ES real-time filter pipeline
7. **P3 — Video** — MediaCodec recording, video effects
8. **P3 — Node Engine** — TouchDesigner-inspired visual programming

## Target Device

| Spec              | Motorola Moto G85 5G                          |
| ----------------- | --------------------------------------------- |
| SoC               | Snapdragon 6s Gen 3                           |
| GPU               | Adreno 619                                    |
| Main Camera       | 50 MP Sony LYTIA 600, OIS                     |
| Ultra-Wide        | 8 MP, 118°                                    |
| Front Camera      | 32 MP                                         |
| Display           | pOLED 120 Hz                                  |
| RAM               | 8 GB                                          |

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.
