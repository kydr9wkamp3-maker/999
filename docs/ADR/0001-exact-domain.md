# ADR 0001: Define exactness over canonical RGBA samples

- Status: Accepted for MVP, not a file-format guarantee
- Date: 2026-11-27

## Context

Camera/gallery sources arrive in different encoded formats with metadata, EXIF orientation, color profiles, alpha modes, and potentially different encoders. Current `Bitmap` decode/encode does not preserve original file bytes or all metadata. Comparing the original PNG/JPEG file hash to an ADM1 pixel container would be an invalid assertion.

## Decision

MVP Exact means byte-identical round-trip of the explicitly normalized RGBA8888 pixel array. Keep and record source-file SHA-256 separately. UI and CSV disclose the hash basis. The app does not claim encoded-file-byte exactness or archival preservation.

## Consequences

- This is useful for validating the reversible pixel core, but cannot establish preservation of EXIF, ICC, original compression, metadata, or source file bytes.
- If file-byte exact archive mode is introduced, it needs a distinct input domain and a codec that round-trips the original byte sequence; it must not reuse the current `bit_exact` label without an explicit schema change.
- Any change to orientation, color conversion, bit depth, channel order, or alpha canonicalization requires a new explicit policy and fixtures.
