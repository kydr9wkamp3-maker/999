package com.chromecore.camera.core

/**
 * Feature flags for controlling experimental/in-progress features.
 * All new features start with `false` and are enabled when ready.
 */
object FeatureFlags {
    // Camera features
    const val PRO_MODE = false                    // ISO, exposure, WB, focus controls
    const val RAW_CAPTURE = false                 // DNG/RAW capture
    const val HDR_MODE = false                    // Multi-frame HDR
    const val NIGHT_MODE = false                  // Night mode / LLB
    const val PORTRAIT_MODE = false               // Portrait with bokeh

    // Effects
    const val GPU_EFFECTS = false                 // Live GLSL filters
    const val LUT_SUPPORT = false                 // .cube LUT import
    const val CUSTOM_EFFECTS = false              // User-created effects

    // AR
    const val FACE_AR = false                     // Face masks/landmarks
    const val HAND_TRACKING = false               // Hand gestures
    const val SEGMENTATION = false                // Background segmentation

    // Video
    const val VIDEO_CAPTURE = false               // Video recording
    const val SLOW_MOTION = false                 // Slow-mo video
    const val TIME_LAPSE = false                  // Time-lapse

    // Editor
    const val PHOTO_EDITOR = false                // Non-destructive editor
    const val NODE_EDITOR = false                 // Node-based effect editor
    const val TIMELINE = false                    // After Effects-style timeline

    // UI
    const val ADVANCED_SETTINGS = false           // Pro settings panel
    const val GRID_OVERLAY = true                 // Rule of thirds grid (enabled)
    const val HISTOGRAM = false                   // Live histogram
    const val FOCUS_PEAKING = false               // Focus peaking highlight
    const val ZEBRA = false                       // Zebra stripes for exposure

    // Device
    const val DEVICE_LAB = true                   // Camera diagnostics (enabled)
    const val THERMAL_MONITORING = false          // Thermal status monitoring
    const val PERFORMANCE_STATS = false           // FPS/memory stats overlay
}
