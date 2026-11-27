# Privacy statement for the prototype

- Images are selected through the Android system picker or captured by CameraX, then processed on-device.
- The app does not declare `INTERNET`, and no cloud upload, analytics, advertising, or account subsystem is intended.
- The local experiment log stores image display name, dimensions, codec/mode, byte sizes, timings, quality results, and hashes. A filename can itself contain personal information; users should review the CSV before sharing it.
- Device metadata should be limited to manufacturer/model and Android version. Never collect IMEI, serial, MAC/IP, SIM identifiers, contacts, location, or unrelated EXIF location.
- Source bytes are kept in app memory during an experiment. Camera temporary files are placed in app cache and deleted after capture processing; cache cleanup on exceptional process death remains a test item.
- CSV is written only to a destination chosen by the user via the system document picker. “Share report” sends text to the Android share sheet; it does not upload automatically, but the receiving app may transmit it.
- The `READ_MEDIA_IMAGES` permission is declared for compatibility but the current import path uses Photo Picker and does not request broad gallery access. Remove the declaration if no planned supported flow needs it.

This document describes intended behavior, not a completed privacy audit. Verify merged manifests/dependencies and inspect device network behavior before claiming privacy certification.
