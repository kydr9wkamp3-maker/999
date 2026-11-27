# Instructions for coding agents

This file applies to all repository work. If instructions conflict, follow: platform/system instructions → latest user request → this file → other repository docs.

## Project reality and evidence

- ADAMANT-1 is an experimental C++ RGBA predictor + Huffman prototype. CHIMERA-1/CHM1 is a separate research prototype with a quantized/routed base and optional exact XOR residual. Neither is a standards-compliant general-purpose format, and neither has a demonstrated advantage over established codecs.
- The current Android app's Exact check is equality of normalized RGBA pixel bytes. It is not byte-for-byte equality of the original encoded PNG/JPEG file. Keep `source_file_sha256` separate from pre/post RGBA hashes.
- CameraX captures JPEG in the MVP. It is already camera-processed/lossy, not sensor RAW. Do not say RAW/DNG is supported until verified through runtime capability checks on real hardware.
- The minimal CHM1 tile-router + quantized-base + optional residual experiment is implemented; diffusion, INR, semantic/machine channels, JND model, NPU/GPU backends, and progressive layers remain proposals. Never label a paper's claimed result as an ADAMANT/CHIMERA result.
- Never invent test outcomes, benchmark numbers, scientific citations, licenses, device features, or compression percentages. Say **не проверено** for anything not supported by a test log, CSV, artifact, source code, or authoritative source.
- A perceptual/semantic/generative reconstruction is lossy unless a formally verified lossless path reconstructs every specified input byte/sample.

## Privacy and security invariants

- Never add a photo upload, network transport, telemetry, advertising, or analytics dependency. Keep `INTERNET` absent unless the user explicitly revises the offline requirement.
- Do not log IMEI, device serial, MAC/IP address, SIM identifiers, private URI paths, or image contents. Device model and Android version are sufficient metadata for the experiment schema.
- Treat imported files and ADM1 containers as hostile. Check lengths/dimensions before allocation, use checked arithmetic, cap work/memory, validate CRC/hash, and fail closed on malformed input.
- CRC detects accidental corruption; it is not authentication. Do not describe CRC or an unkeyed SHA-256 as tamper-proof authenticity.
- Never commit keystores, secrets, APK/AAB artifacts, user images, private captures, or datasets without documented redistribution rights.

## Engineering rules

- Preserve the current Arena branch; do not switch, rename, or create branches.
- Kotlin UI must not contain codec internals. Keep stable interfaces between app, domain, and JNI/native codec.
- Do not downsample, rotate, color-convert, strip metadata, or change bit depth silently in Exact mode. Record canonicalization explicitly. Separate source-file identity from canonical pixel identity.
- Keep the on-device deterministic CPU path available. Optional GPU/NPU/model backends must be capability-tested and have a disclosed fallback; never assume a particular phone supports them.
- For format changes, update `docs/FORMAT.md`, `docs/SPEC.md`, compatibility fixtures, corruption tests, and versioning decision before claiming compatibility.
- For lossy changes, include rate-distortion results against fixed inputs, parameter settings, Android/device/build details, repeated timing runs, and quality measures. Include failures/regressions, not just wins.
- Add tests to each PR. Add parser fuzz/property tests for new binary features. Never compare Debug timings against Release timings.

## Required pre-finish checks

Run what the environment supports and report each command separately:

```sh
./tests/native/run.sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Physical-device camera, thermal, app-heap, and repeated-capture claims need device logs. CI success is not physical-device validation. If a required tool is absent, report the blocker rather than silently skipping or claiming success.

## Required final report

1. What changed.
2. Files changed/added.
3. Exact build/test commands run and their outcomes.
4. What remains unrun or failed and why.
5. User/privacy/security implications.
6. Limitations and next measurable step.
