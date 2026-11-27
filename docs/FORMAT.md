# ADM1 container format, version 1

All integer fields are unsigned little-endian unless stated otherwise. The current codec stores an 8-bit-per-channel RGBA image. The format is experimental and is not yet a stable public interchange format.

## Fixed header (2,085 bytes)

| Byte offset | Length | Field | Meaning |
|---:|---:|---|---|
| 0 | 4 | magic | ASCII `ADM1` |
| 4 | 2 | version | `1` |
| 6 | 4 | width | Pixel width |
| 10 | 4 | height | Pixel height |
| 14 | 1 | channels | Must be `4` (RGBA) |
| 15 | 2 | tile size | Must be `64` |
| 17 | 8 | raw length | Must equal `width × height × 4`; MVP rejects above 1 GiB |
| 25 | 4 | CRC-32 | CRC-32/IEEE of reconstructed RGBA bytes |
| 29 | 8 | bit count | Number of valid high-to-low bits in payload |
| 37 | 2,048 | frequencies | 256 little-endian `uint64` symbol frequencies |
| 2,085 | variable | payload | Huffman-coded residual bytes, MSB-first within each byte |

The file length must equal header length plus `ceil(bit_count / 8)`. Frequencies must sum to `raw_length`. Decoder validates metadata and CRC before returning pixels.

## Transform and coding order

1. Traverse tiles left-to-right, top-to-bottom; within each tile traverse rows then columns.
2. For each RGBA component, predict from the left pixel within the current tile; at a tile's left edge predict from the above pixel within the same tile; at the top-left of a tile predict zero.
3. Store the unsigned modulo-256 difference `current - predictor` as one residual byte.
4. Build one deterministic Huffman tree over the whole residual stream. Priority ties use frequency, minimum symbol, then node creation serial. The frequency table allows the decoder to rebuild the same tree.
5. Decode residuals in the same traversal order and add the same predictor modulo 256.
6. Verify CRC-32 over reconstructed RGBA bytes.

## Explicit limitations

- No file metadata, EXIF, ICC profile, original file bytes, arbitrary bit depth, alpha mode, flags, or per-tile lengths are stored. Dimensions and orientation are normalized by the app before pixel encoding; the original source file is retained only for logging its SHA-256 and byte size.
- Exact means round-trip identity of the normalized RGBA pixel byte array, not identity of the original PNG/JPEG file bytes.
- The full 2 KiB frequency table is stored even for tiny images; this is a research prototype and can expand input substantially.
- Maximum raw buffer is a native safety ceiling, not a promise the Android app can process that much. App heap limits impose a lower dynamic image-size limit.
- Version 1 has no compatibility guarantee until the format is frozen and fixture files are checked into tests.
