# Test plan

Run automated tests on every change; perform device-only rows on a physical device. Until captured in logs, every device test is **not verified**.

## Automated checks

| Case | Command / assertion | Status in this checkout |
|---|---|---|
| Native round-trip including tile boundaries | `./tests/native/run.sh` | Executed successfully in this workspace |
| Native corruption detection | Standalone test flips ADM1 CRC and CHM1 payload; decoders reject | Executed successfully in this workspace |
| CHM1 Exact residual | Synthetic RGBA round-trip with residual and source CRC | Executed successfully in host standalone test |
| CHM1 Bounded-error | Synthetic RGBA maximum component error ≤ declared tolerance; routed tile preservation | Executed successfully in host standalone test |
| JNI wrapper C++ syntax | Host compile with temporary minimal JNI stub | Executed; not an Android NDK/JNI ABI validation |
| Kotlin metrics and CSV escaping | `./gradlew testDebugUnitTest` | Not run here: JDK/Android SDK unavailable |
| Android native JNI test | `./gradlew connectedDebugAndroidTest` on device/emulator | Not run here |
| Debug APK build | `./gradlew assembleDebug` | Not run here: JDK unavailable |
| Manifest / resource XML parse | XML parser | Executed successfully |
| No accidental whitespace errors | `git diff --check` | Executed successfully |

## Device matrix — Moto g85 5G from supplied screenshot

Record Android build/API, app version, image name, lens, dimensions, MIME, source bytes, output bytes, elapsed times, thermal state, and outcome. Do not record device serials or personal identifiers.

| Scenario | Procedure | Pass condition |
|---|---|---|
| Camera permission | Fresh install; deny then grant | Denial handled; grant opens preview |
| Rear/front lens | Capture with default and switched camera | Captured image opens; source labelled JPEG |
| Gallery PNG | Import PNG and run ADAMANT Exact | RGBA SHA matches after round-trip |
| CHIMERA Exact | Run CHM1 exact profile on PNG | RGBA SHA matches; residual is present; no size advantage assumed |
| CHIMERA Bounded-error | Run CHM1 bounded profile | Measured max per-channel RGBA error ≤ configured bound; mode is labelled lossy, never visual-lossless |
| Lossy baseline | Run JPEG/WebP quality 95 | Decode succeeds; metric/hash reflects actual decoded pixels |
| API-dependent WebP | Run WebP lossless | Works on API 30+; unavailable clearly reported below API 30 |
| EXIF rotations | Import images with orientation tags 1–8 | Display orientation correct; notes state normalization; pixels are internally consistent |
| Repeated capture | Capture, process, repeat 10 times | No crash, stale camera binding, or unbounded cache-file growth |
| Large image safety | Import/capture above computed limit | Clear rejection before full bitmap allocation; no OOM |
| Background/interruption | Background app during encode, return | No crash; result/status remains coherent or operation is safely cancelled |
| Offline behavior | Airplane mode; camera/gallery/compression/export | Core flow works; no network permission or required network request |
| Thermal repeatability | One warmup then five measured runs; note thermal status | Report raw observations and median; do not claim cross-device equivalence |
| Export | Export CSV through system document picker | UTF-8 CSV opens; quotes/newlines escaped; columns align |

## Manual device metadata

Use the commands in `DEVICE_PROFILE.md`; redact `adb devices` serial output before sharing. Confirm app heap with `dumpsys meminfo`; physical RAM in Settings is not the app heap budget.
