# Current app and codec API boundary

## Kotlin public-to-domain surface

The current app has the following internal types in `org.adamant.lab`:

- `ImageInput`: decoded `Bitmap`, source encoded bytes, display name, MIME, and notes.
- `Codec`: ADAMANT prototype, CHIMERA-1 research prototype, PNG, JPEG 95, WebP lossless, WebP 95.
- `Mode`: Exact, Bounded-error, Visual-lossless, Progressive. ADAMANT currently implements Exact only. CHIMERA implements Exact with residual and bounded-error (lossy); Visual-lossless and Progressive fail explicitly. Baseline codecs accept only Exact mode selection in the app; their own JPEG/WebP format settings may still be lossy.
- `CompressionEngine.compress(input, codec, mode)`: encodes, decodes, times the codec path, computes normalized RGBA hashes/quality, returns `CompressionResult`.
- `ExperimentRecord.from(...)`: creates local CSV/log data. Null metrics remain blank, never fabricated.

Do not present these as a stable public SDK. Extract interfaces only when multiple implementations need a shared contract; avoid speculative abstractions that obscure what is actually implemented.

## JNI ABI (current prototype)

Kotlin object `AdamantNative` loads `libadamant.so` and declares:

```kotlin
nativeEncode(rgba: ByteArray, width: Int, height: Int): ByteArray
nativeDecode(container: ByteArray): ByteArray
nativeChimeraEncode(rgba: ByteArray, width: Int, height: Int, exact: Boolean, tolerance: Int): ByteArray
nativeChimeraDecode(container: ByteArray): ByteArray
```

The exported JNI symbols are named for `org.adamant.lab.AdamantNative`. R8 keep rules preserve the class and native method names. ADM1 uses the versioned prediction/Huffman container. CHM1 adds a routed quantized base and optional XOR residual; see `docs/CHIMERA_FORMAT.md`. Kotlin validates the returned byte count against input dimensions before materializing the bitmap, and its bounded-error path checks the observed max channel error against the declared bound.

## Contract expected before expanding the API

Future implementations should expose a small, explicit domain contract comparable to:

```text
CodecDescriptor: id, version, supported modes, capabilities
EncodeRequest: canonical sample format, dimensions, options, resource budget, cancellation
EncodeResult: bytes, dimensions/format, timings, notes, output hash
DecodeRequest: bytes, limits, requested layer/ROI
DecodeResult: samples, dimensions/format, integrity status, decoded layers
VerifyResult: identity domain, source/output hashes, exact boolean, integrity errors
```

Before adopting it, resolve ownership/lifetime, byte order, channel order, alpha semantics, color profile, stride, cancellation, failure types, and compatibility. JNI methods must reject invalid dimensions/lengths before allocating and translate native errors into typed Kotlin failures. Do not return a partially decoded image as successful after checksum failure.
