# Benchmarks

**No Android-device benchmark results have been recorded yet.** Do not add inferred or illustrative measurements to this table. Add a row only when the CSV/log from an identified test run is available.

## Results

| Date UTC | Device / Android build | Image fixture + SHA-256 | Codec / mode | Original bytes | Encoded bytes | Encode ms | Decode ms | PSNR | Global SSIM | Max sample error | Pixel SHA match | Notes / CSV artifact |
|---|---|---|---|---:|---:|---:|---:|---:|---:|---|---|
| No measurements recorded | | | | | | | | | | | |

## Protocol required before comparison

- Same image bytes and dimensions for every codec row.
- Record whether image came from camera JPEG, PNG, screenshot, text, or synthetic noise.
- One warmup run followed by five timed runs; report all observations and median, not only a best case.
- Note Android version/build, device model, lens/source, temperature/thermal state, output format, app version, and whether background applications were present.
- Distinguish source-file hash from normalized RGBA pixel hashes.
- Exact means byte-identical normalized RGBA for the current prototype. A visual score does not establish bit-exactness.
- Do not compare debug APK timings with optimized release timings.
