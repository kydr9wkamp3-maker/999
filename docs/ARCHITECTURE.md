# CHROMECORE X — Architecture

## Overview

CHROMECORE X is a modular Android camera application built with Jetpack Compose, CameraX, and a custom image processing engine.

## Module Structure

```
app/
├── engine/              Camera2 API engine
│   └── CameraEngine     Full camera lifecycle, preview, capture, manual controls
│
├── capabilities/        Device hardware scanner
│   └── DeviceCapabilitiesScanner  Queries Camera2 for real hardware capabilities
│
├── processing/          Image processing pipeline
│   └── ImageProcessor   Non-destructive adjustments (exposure, contrast, etc.)
│
├── filters/             Filter engine
│   └── FilterEngine     Named presets + custom effect implementations
│
├── ui/
│   ├── theme/           Chromecore Material3 design system
│   ├── navigation/      Type-safe Compose Navigation
│   └── screens/
│       ├── camera/      Full-bleed camera with modes, filters, shutter
│       ├── editor/      Non-destructive adjustments + filter carousel
│       ├── gallery/     MediaStore-backed grid gallery
│       ├── settings/    Progressive disclosure settings
│       └── devicelab/   Hardware capability viewer + JSON export
│
├── di/                  Hilt dependency injection
├── models/              Data classes
└── utils/               MediaStore saver, file helpers
```

## Key Design Decisions

1. **Camera2 over CameraX for core engine** — Camera2 gives full manual control over ISO, shutter speed, focus distance, RAW capture, and multi-frame capture.

2. **Non-destructive editing** — All adjustments are parameterized; originals are never overwritten. The editor stores a list of parameters, not pixel mutations.

3. **FilterEngine as named presets** — Each filter is a data class with a name, category, and either parameter overrides or a custom applier function. This makes filters serializable, testable, and composable.

4. **Scoped storage** — All saves go through MediaStore. No legacy file system writes.

5. **Hilt DI** — Singletons for CameraEngine, ImageProcessor, FilterEngine, DeviceCapabilitiesScanner.

6. **Compose Navigation** — Type-safe routes, no string parsing.

## Data Flow

```
Camera Preview → Camera2 Frame → SurfaceTexture → PreviewView
                                                         ↓
Capture → ImageReader → JPEG bytes → Bitmap → ImageProcessor → FilterEngine → MediaStore
                                                         ↓
                                                    Editor (non-destructive params)
                                                         ↓
                                                    Export (apply pipeline → JPEG/HEIC)
```

## Performance Constraints

- Snapdragon 6s Gen 3 / Adreno 619
- 8 GB RAM
- All image processing on background dispatchers
- Bitmap pooling planned for Phase 2
- GPU shader pipeline planned for Phase 2
