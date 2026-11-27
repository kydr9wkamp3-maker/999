# CHIMERA research track: a hybrid, testable direction

**CHIMERA is a provisional research-track codename, not a standard, product claim, or established novel invention.** A deliberately small CHM1 experiment is now implemented: tile-routed quantized base plus optional exact XOR residual. It contains no neural model. Search prior art, licenses, patents, and trademarks before public naming or novelty claims. ADAMANT ADM1 remains a separate codec documented in `FORMAT.md`; CHM1 is documented in `CHIMERA_FORMAT.md`.

## The central engineering insight

A container can carry different representations, but it cannot remove the information-theoretic cost of exact reconstruction. A visually convincing generative decoder does not recover details absent from the bitstream. If an exact residual is added, exactness is possible only when the residual plus the decoder's precisely specified base reproduces the source samples. If the base decoder is nondeterministic or differs across backends, a residual computed against one base can fail on another. A robust exact profile therefore needs either:

1. a deterministic, bit-exact base transform and fully encoded signed residual, with conformance-tested arithmetic; or
2. an independent lossless representation of the canonical source samples, with neural output retained only as an optional preview/side channel.

The second design uses more bits but avoids pretending that cross-device floating-point inference is exact. Always measure the complete file, including model identifiers, routing maps, headers, checksums, side information, and residuals.

## Research container model (future; not ADM1 v1)

A future versioned container could describe a DAG of streams. A decoder should be able to parse the directory without loading model weights and reject unsupported mandatory features.

- **Common header:** magic, format major/minor, profile, dimensions, sample type/bit depth, channel and alpha semantics, color profile/transfer, orientation policy, tile grid, directory length, integrity algorithm.
- **Directory:** explicit tile geometry, stream type/codec identifier, dependency IDs, byte ranges, decoded-byte budget, layer availability, integrity code. Never ask the decoder to independently classify a tile and hope it chooses the encoder's path.
- **Base stream:** deterministic transform/predictive, palette, or standard-codec stream selected by the encoder. The stream name and exact parameters are recorded.
- **Progressive refinement:** independently addressable refinements with an explicit dependency graph; every prefix has a defined output and quality status. The final exact refinement is required before calling the result sample-exact.
- **Exact residual:** signed residual or independent lossless sample stream. Define integer range, overflow behavior, alpha/color handling, transform inverses, and per-stream hashes. A checksum is not an authenticity signature.
- **Optional learned/INR/generative stream:** model artifact hash/version, architecture, quantization, input normalization, inference profile, deterministic contract, intended task, and license. It may improve a lossy preview but cannot silently override exact samples.
- **Optional machine/task side information:** explicit model/task/version and validation score domain. It does not replace a human-viewable sample stream unless the profile explicitly says it is task-only.

Total measured rate is the bytes of the entire serialized object, not only latent coefficients:

```text
R_total = R_header + R_directory + R_routing + R_base + R_refinement
        + R_exact_residual + R_model_reference + R_checksums + R_metadata
```

If a generative base requires a large model download, include model distribution/storage/availability in the product cost even when the model is not repeated per image. Offline operation means the selected model must be locally available before the experiment; no photo or metadata may be silently uploaded.

## Mode semantics

| Mode | Decoder promise | Must not be called |
|---|---|---|
| File-byte exact | Original encoded file bytes are identical, including metadata/container bytes | Pixel-exact only |
| Sample-exact | Named canonical sample array is byte-identical after decode | Lossless original-file archival |
| Lossy quality-target | Meets only named, measured metric/threshold/test protocol | Mathematically lossless / guaranteed invisible |
| Generative preview | Plausible synthesized preview; may invent texture, text, or identity details | Faithful evidence / archival copy |
| Task representation | Preserves named downstream task under a named test | General image reconstruction |
| Progressive | Each decoded prefix has documented incomplete-layer semantics | Complete/exact before required final layer |

A JND or perceptual threshold is not a proof that every viewer, use case, text glyph, scientific feature, or display will find an error invisible. State the metric, implementation, threshold, conditions, population, and uncertainty.

## What to combine—and what to isolate

1. **Reversible core first:** keep exact integer transforms separately testable. Add image-oriented transforms only with inverse/property tests.
2. **Region/tile routing:** classify conservatively at the encoder; serialize route decisions. Charge route-map overhead to the stream. Classifier errors should choose safe coding, not silently discard precision.
3. **Specialized classic streams:** investigate palette/run coding for screenshots, predictive transforms for smooth/medical data, and robust fallback for noise. Benchmark each as a separate codec; no codec substitution hidden in results.
4. **Residual coding:** compare direct residual, transformed residual, context-coded residual, and independent lossless canonical samples. Include decoder determinism in the experiment.
5. **INR:** per-image fitting may suit fixed images, scientific volumes, or ROI queries; measure encoder optimization time, transmitted parameters, decoder time, and task validity. Do not assume fast mobile capture.
6. **Diffusion/generative:** restrict to optional low-rate perceptual previews, with a visible generated/lossy label and exact-source preservation kept separately. A generator can invent medically or legally consequential details.
7. **Machine channel:** only add if a named task and dataset justify it. Store model version and report task metrics; human and machine quality are different axes.
8. **CPU/GPU/NPU:** hide execution backend behind a capability interface. Keep a tested CPU fallback. Do not promise bit-determinism across delegates; exactness must not depend on nondeterministic accelerator output.

## Literature reading map (claims belong to the papers, not this app)

- [JPEG AI / ISO/IEC 6048 information](https://jpeg.org/jpegai/) — learning-based coding with human and machine-oriented goals. A standard's existence does not imply the current MVP supports it.
- [CoD: A Diffusion Foundation Model for Image Compression](https://arxiv.org/abs/2511.18706) — compression-oriented diffusion foundation model; arXiv lists CVPR 2026 acceptance. Study training/data/decoder costs before considering an Android deployment.
- [DiffO: Single-step Diffusion for Image Compression at Ultra-Low Bitrates](https://arxiv.org/abs/2506.16572) — one-step diffusion and VQ residual approach. Paper-reported comparisons are conditioned on its model, datasets, setup, and baselines; they are not mobile results for ADAMANT.
- [StableCodec: Taming One-Step Diffusion for Extreme Image Compression](https://arxiv.org/abs/2506.21977) — explores very low-rate generative reconstruction. The paper itself notes that diffusion reconstruction does not guarantee pixel consistency; low-bpp perceptual output is not lossless.
- [MoRIC, NeurIPS 2025](https://neurips.cc/virtual/2025/loc/san-diego/poster/118832) — modular region-based implicit neural representations and layered refinement; evaluate image-specific fitting and region-map costs.
- [INIF, Nature Computational Science (2025)](https://www.nature.com/articles/s43588-025-00889-4) — implicit neural image fields for biological microscopy and task-aware reconstruction. Scientific use requires validation of downstream measurements, not only visual quality.
- [Implicit Neural Representations for Image Compression, ECCV 2022](https://doi.org/10.1007/978-3-031-19809-0_5) — earlier INR source coding; useful baseline and context, not proof of universal superiority.
- [PICO project/paper](https://apple.github.io/ml-pico/) and [arXiv record](https://arxiv.org/abs/2605.05148) — practical learned codec work including performance-aware design. Numbers on those pages are author-reported and must not be copied into ADAMANT results.
- [SSIMULACRA2 source](https://github.com/cloudinary/ssimulacra2), [libjxl](https://github.com/libjxl/libjxl), [libavif](https://github.com/AOMediaCodec/libavif) — candidate evaluation tools/baselines. Review Android build support and licenses before integration.

No listed work establishes that these components can be combined with better end-to-end rate, fidelity, speed, energy, and exactness. That is the research question.

## Falsifiable hypotheses

Every hypothesis needs a preregistered fixture list, exact baseline versions, settings, primary metric, analysis code, and a decision threshold chosen before results are viewed.

- **H1: region routing improves total rate-distortion** after counting the route map and boundary overhead.
- **H2: transform/context coding reduces exact residual bytes** against the same independent lossless baseline on a fixed corpus.
- **H3: progressive refinements improve useful prefix quality per bit** without making final exact recovery ambiguous.
- **H4: a learned/INR preview improves named perceptual or scientific-task utility at a fixed complete-file rate** while staying within a measured device time/memory budget.
- **H5: backend fallback preserves decode compatibility** and does not affect exact integrity checks.

Null outcomes and regressions are valid findings. Do not choose a favorable test subset after seeing outputs.

## Experimental protocol

- Freeze image fixture IDs and source SHA-256; split by source/image identity before deriving crops. Record license and allowed redistribution in `DATASETS.md`.
- Compare at matched quality and matched rate; report full rate-distortion curves and an explicitly described BD-rate domain. Do not derive a headline from one quality point.
- Separate objective pixel metrics, perceptual metrics, human subjective ratings, and task metrics. FID/KID are distribution-level statistics, not per-image fidelity certificates; PSNR alone does not represent perception.
- For subjective work: randomize codec/order, blind labels, predefine viewing/display conditions, report sample/observer counts, uncertainty and statistical procedure. Do not show the source as a hidden reference unless the protocol specifies it.
- For speed: use identical device/build/input, warmup, repeated runs, median and tail quantiles, decode and encode separately, and thermal state. Never mix Debug and Release.
- For energy: instrument a reproducible power measurement (device power rails or external meter), document sampling/idle subtraction and battery/thermal state. Battery percentage delta alone is too coarse for per-image energy claims.
- For memory: report Java heap and native/RSS peaks separately where possible; a before/after heap delta is not peak RSS.
- Publish raw rows, scripts, configuration, codec/model hashes, failure counts, and environment. Document unavailable baselines rather than substituting another codec.

## Integration gates

1. Desktop/reference prototype: deterministic container parser, fixtures, property/fuzz tests, exact path.
2. Classical mobile extension: new profiles behind explicit codec IDs; full Android ABI tests and size/time/memory data.
3. Optional neural preview: only after model license/security review and reproducible host evaluation; run offline, make provenance visible, never replace exact source.
4. Device accelerator: only after backend support matrix, fallback, output consistency tests, thermal tests, and a demonstrated end-to-end advantage on target hardware.
5. Public release: decoder compatibility, format freeze/version policy, security review, signing, checksums, and independent reproduction.

Any gate without artifacts/logs remains **not verified**.
