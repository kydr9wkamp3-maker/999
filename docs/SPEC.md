# ADAMANT Lab MVP specification

## Scope

A local Android image-compression experiment app. Kotlin/Compose owns workflow and presentation; CameraX captures JPEG; system Photo Picker imports user-selected files; JNI calls a C++ reversible RGBA prototype. Nothing is uploaded. No claim of superiority or guaranteed compression ratio is made.

## Data flow and identity

- Keep original encoded bytes for byte count and `source_file_sha256`.
- CameraX JPEG is copied unchanged to MediaStore `Pictures/ADAMANT` before any bitmap decode or codec analysis. Bounds-check dimensions before full bitmap allocation; if full analysis is rejected or runs out of memory, keep the saved original and offer a user-triggered `inSampleSize` analysis copy. The copy is explicitly lossy and Exact applies only to its normalized RGBA pixels. Apply EXIF orientation for display/pixel processing when present.
- Convert the normalized bitmap to RGBA8888 row-by-row.
- ADAMANT Exact encodes those pixel bytes, decodes them, computes `source_rgba_sha256` and `decoded_rgba_sha256`, and compares exact bytes.
- CHIMERA Exact encodes a quantized/routed base plus an XOR residual and verifies recovered canonical RGBA. CHIMERA Bounded-error omits the residual and checks the decoded max absolute RGBA-component error against its declared threshold. Bounded-error is lossy; it is not a perceptual/JND guarantee.
- PNG/JPEG/WebP use Android `Bitmap.compress`; after decode, compare normalized pixel bytes to the same source RGBA.
- Do not describe a JPEG/WebP lossy mismatch as lossless. Camera output is already JPEG before app processing.

## Metrics status in MVP

- Source file bytes, encoded bytes, ratio, encode/decode elapsed time, file SHA, normalized RGBA SHA, bit-exact pixel status: collected for successful runs.
- PSNR: computed over RGBA bytes; exact equality is represented as not finite/blank in CSV and as infinity in UI.
- SSIM: explicitly a global luminance approximation; not standard windowed SSIM.
- Maximum absolute per-component RGBA sample error is measured and logged; CHIMERA Bounded-error refuses to report success if this exceeds its configured bound.
- Peak memory and SSIMULACRA2: not measured; exported fields remain blank with an explicit status.
- Unsupported codec/mode: report as unavailable/error; never substitute another codec silently.

## App privacy and permissions

- Camera permission is runtime-requested only when using the camera. `WRITE_EXTERNAL_STORAGE` is requested only on Android 9/API 28 and lower for MediaStore gallery saves.
- `READ_MEDIA_IMAGES` is declared but not requested by the current Photo Picker import flow.
- No `INTERNET` permission, upload client, ad SDK, or analytics SDK.
- Logs contain no IMEI, serial, MAC, IP, or SIM identifiers.

## Versioning and release policy

- App prototype version: `0.1-prototype`.
- ADM1 header version: 1, experimental only.
- Never publish the debug APK as a production release. Signing, release hardening, and format compatibility are future work.
