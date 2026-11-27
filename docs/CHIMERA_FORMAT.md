# CHM1 research container (experimental)

CHM1 is a **separate experimental container**, not an ADM1-compatible stream, published standard, or stable archival format. It combines a tile-routed quantized base with an optional exact XOR-residual layer. It does not contain diffusion, INR, learned semantics, JND, or a model. Host synthetic round-trip tests exist; the implementation must remain labelled `CHIMERA-1 research prototype` until Android/device validation and baseline benchmarking are complete.

## Header (46 bytes), little-endian

| Offset | Bytes | Field |
|---:|---:|---|
| 0 | 4 | Magic ASCII `CHM1` |
| 4 | 2 | Version (`1`) |
| 6 | 4 | Width |
| 10 | 4 | Height |
| 14 | 2 | Tile size (must be 64) |
| 16 | 1 | Maximum per-component absolute error tolerance (0..8) |
| 17 | 1 | Flags: bit 0 means an exact residual is present; all other bits must be zero |
| 18 | 4 | Tile count, must match dimensions |
| 22 | 8 | ADM1 base-stream length |
| 30 | 8 | ADM1 residual-stream length (nonzero for exact profile, zero for bounded-error profile) |
| 38 | 4 | Source RGBA CRC-32; zero for bounded-error profile |
| 42 | 4 | CRC-32 of the complete payload after the header |
| 46 | variable | Route map, base ADM1 stream, optional residual ADM1 stream |

Route map length is `ceil(tile_count/8)`, least-significant bit first. A set bit means that tile is copied exactly into the base because quantization would exceed the declared error bound. Unused high bits in the final map byte must be zero. Payload CRC covers route map + base stream + residual stream. Each inner ADM1 stream carries and verifies its own metadata/CRC.

## Base transform

Each 8-bit RGBA component is quantized to the nearest multiple of 16 using integer arithmetic, with the top level clamped to 255. The encoder inspects every component in each 64×64 tile. If any component would exceed the configured tolerance, the entire tile is routed exactly into the base; otherwise the quantized tile is retained.

- **Bounded-error profile:** residual stream is absent. Encoder and app compare source and decoded RGBA and require maximum absolute error across all four components to be no greater than the declared tolerance. This is a numeric sample bound, not a visual-quality or JND guarantee. A standalone decoder cannot certify the source-relative bound without the source.
- **Exact profile:** residual bytes are `source_rgba XOR base_rgba`, encoded by ADM1. Decoder XORs base and residual and verifies the source RGBA CRC. App additionally compares SHA-256 of normalized source and decoded RGBA.

## Limitations and threat model

- This design intentionally tests a hybrid of lossy base + exact residual. It may be larger and slower than ADM1 alone or standard codecs; no compression advantage is claimed.
- The exact residual is not omitted or selectively inferred in Exact mode. Do not remove it for a smaller file and keep the Exact label.
- Payload CRC detects accidental corruption, not malicious alteration/authenticity.
- The route map currently describes the encoder's threshold decision; it is included even though output pixels are sufficient to decode. This overhead is counted.
- No color profiles, bit depths above 8, metadata, chroma subsampling, alpha-specific treatment, signed residual API, random-access index, model info, signatures, or progressive decode are present.
- If the algorithm/header changes, increment the CHM version and add old-version fixtures/tests. Do not change the meaning of an existing version.
