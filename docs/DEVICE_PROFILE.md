# Target device profile — user-provided Moto g85 5G

This profile records only the facts visible in the supplied “About device” screenshot. It is a test target description, **not a runtime probe or a benchmark result**.

| Property | Screenshot value | What still needs runtime confirmation |
|---|---|---|
| Device marketing name | moto g85 5G | Android `Build.MANUFACTURER`, `Build.MODEL`, build fingerprint; exact model code |
| Physical RAM | 12 GB | App Java heap limit, native heap behavior, free memory under load |
| Storage | 58 GB used / 256 GB total | Free space at test time; filesystem behavior |
| Rear camera modules | 50 MP + 8 MP | CameraX stream sizes, active lens IDs, RAW/DNG capability, JPEG output dimensions |
| Front camera | 32 MP | CameraX supported output sizes |
| Display | 2400 × 1080 | Logical dp size, refresh rate, density, orientation |
| Android version | Not visible | `adb shell getprop ro.build.version.release` and SDK level |
| SoC / GPU | Not visible | `adb shell getprop ro.soc.model` where available; never infer from marketing model |
| RAW support | Not visible | Query CameraX `ImageCaptureCapabilities` on the actual device |

## Test guidance for this device

- The screenshot's 12 GB figure is physical RAM; it does **not** state the app's heap limit. The app checks image dimensions against a conservative budget based on `Runtime.maxMemory()` and refuses oversized images rather than silently downscaling Exact input.
- Camera module megapixels do not establish the resolution selected by CameraX. Record actual `Bitmap` dimensions and JPEG file size for every capture.
- Test rear main, rear auxiliary/ultrawide, and front cameras separately. CameraX may not expose all manufacturer modules through the same selector; unsupported lenses must be documented, not assumed.
- Verify camera permission denial, permission grant, lens switch, rotation/orientation, repeated captures, memory pressure, and thermal behavior on the physical phone.
- Use PNG from the gallery for pixel-exact round-trip checks. A camera JPEG is already lossy before ADAMANT receives it.
- Do not store IMEI, serial number, MAC address, IP address, or SIM state in logs. The CSV records manufacturer/model and Android version only.

## Collect facts without sharing identifiers

Run locally and redact serial-like values before sharing output:

```sh
adb devices
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.soc.model
adb shell dumpsys thermalservice
adb shell dumpsys meminfo org.adamant.lab
adb shell df -h /data
```

`adb devices` can reveal a device serial. Do not paste it into issues or benchmark CSVs.
