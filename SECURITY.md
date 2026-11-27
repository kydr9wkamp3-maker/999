# Security policy

ADAMANT Lab processes user-selected images locally. The current manifest has no `INTERNET` permission and the app does not intend to transmit image data.

## Threat model

Input bytes can be malformed, truncated, extremely large, deliberately crafted to overflow dimensions, or contain unusual image metadata. A native parser failure can terminate the process or expose memory-safety bugs. A file chosen through a content URI may change or become unavailable while read.

## Required controls

- Enforce encoded-file, width, height, pixel-count, output, and decode-work budgets before allocation; use checked arithmetic in Kotlin and C++.
- Validate ADM1/CHM1 magic, versions, dimensions, channel count, tile size/count, exact byte lengths, frequency sums, bitstream bounds, route-map padding bits, payload end, and CRC before exposing output.
- Reject unknown mandatory flags/versions. Do not silently continue after malformed data or checksum failure.
- Native C++ must not keep pointers into JNI arrays after release, write out of bounds, rely on signed overflow, or allow exceptions to cross JNI. Fuzz parser entry points and run ASan/UBSan on host builds where available.
- Keep the UI responsive; constrain CPU work and allow cancellation/timeouts for untrusted workloads.
- Treat CRC as accidental-corruption detection only. Use a signature or MAC only if authenticity becomes a requirement and key management is designed.
- Do not export secrets, local URI paths, device identifiers, or image bytes in logs/crash reports.

## Reporting

Report reproducible security issues privately to the repository maintainer. Include app/codec versions, Android/API, input hash (not the sensitive image itself), exact reproduction steps, and crash stack. Do not include personal photos in public issues.

## Current gaps

The prototype has basic bounds and CRC checks but has not received professional security review, coverage-guided fuzzing, sanitizer testing across ABIs, or malicious image corpus testing. Do not use it for sensitive archival or medical data until those gaps are closed.
