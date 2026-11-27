# Contributing

## Development prerequisites

- JDK 17.
- Android SDK Platform 35, Build Tools 35.0.0, NDK 27.2.12479018, CMake 3.22.1.
- Android device/emulator for instrumented tests; real camera claims require physical hardware.

## Local verification

```sh
./tests/native/run.sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

The standalone native test does not validate Android JNI packaging. Report unavailable tools and tests as not run.

## Pull requests

Use `.github/pull_request_template.md`. Each code change should include relevant automated tests and explain behavior changes, performance trade-offs, privacy impact, format compatibility, and device-only follow-up. Do not submit benchmark claims without raw CSV/logs and exact input fixture hashes.

## Images and datasets

Do not upload personal photos, raw camera data, generated artifacts, or datasets to issues/PRs. For public fixtures, record the source, exact license, permitted use, redistribution terms, version, SHA-256, and fixed split in `docs/DATASETS.md`; get review before adding the fixture. Use external storage/Git LFS only after licensing and repository cost are understood.

## Binary format changes

Update the written spec and versioning decision first. Add deterministic fixtures, successful round-trip tests, truncation/corruption/property/fuzz tests, old-version compatibility checks, and an explicit migration policy. Never silently change the meaning of an existing version or mode.

## Releases

Only reviewed `vMAJOR.MINOR.PATCH` tags trigger the signed release workflow. Follow `docs/RELEASE.md`; do not commit signing keys or publish a debug APK as a release.
