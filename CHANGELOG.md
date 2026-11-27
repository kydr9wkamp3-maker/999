# Changelog

Changes are recorded only when implemented and tested. This is an experimental app; no stable public codec compatibility is promised.

## Unreleased

- Initial Android/Compose/CameraX/JNI prototype scaffold.
- ADM1 version 1 experimental RGBA predictor + Huffman container and standalone native tests.
- CHM1 experimental tile-routed quantized base, bounded-error profile, and optional XOR exact-residual profile with native checks.
- Local CSV experiment log, debug CI and guarded tag-release workflow.
- Added research, security, privacy, dataset, device-profile, benchmark, and agent-operating documentation.
- Phase 0 safety fix: save CameraX JPEG to `Pictures/ADAMANT` before decoding; if full-resolution analysis exceeds the current heap budget or fails with OOM, keep the original and offer an explicit `inSampleSize` analysis copy. The copy is marked lossy; Exact applies only to its normalized RGBA pixels, and the compression ratio is left blank for that path.
- Added startup/runtime memory diagnostics (`Runtime.maxMemory`, `memoryClass`, `largeMemoryClass`, allowed pixels and capture bounds) and a tested pure Kotlin helper for the dynamic pixel budget.
- Temporarily enabled `android:largeHeap="true"` as requested for phase 0 diagnostics. This is not a streaming/tiled-memory solution and does not establish support for 12.5 MP or 50 MP captures.
- Phase-0 validation: `./tests/native/run.sh` passed locally; CI run `45976790352` passed native checks, `testDebugUnitTest`, `lintDebug`, debug APK assembly and artifact upload. Local Gradle cannot run here because Java/JDK is absent. CameraX/MediaStore and reduced-copy behavior on a physical Moto G85 remain not verified.
