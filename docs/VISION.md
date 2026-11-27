# Vision and scope

## North-star

Build a local, inspectable image-compression laboratory where a user can reproduce a codec experiment and distinguish exact recovery from perceptual similarity. Trustworthiness and reproducibility outrank a headline ratio.

## What this project is

- An Android experiment app and a versioned research prototype.
- A place to compare real output byte counts, measured time, and decoded pixels on a named device/build.
- A research platform for testing region routing, residual streams, progressive layers, neural representations, and optional machine-oriented side information.

## What it is not

- Not a claim to beat every established or learned codec.
- Not currently a general-purpose file format, archival standard, medical/scientific preservation format, or interoperable web format.
- Not evidence that diffusion, INR, semantic channels, or AI always improve rate-distortion.
- Not a replacement for a calibrated benchmark, controlled subjective study, or device test.

## Research objective

Choose a target domain before optimization: natural photos, screenshots/text, scientific imagery, or mobile capture. Report a Pareto frontier across rate, distortion/perception, encode/decode latency, memory, and energy. A single scalar score hides trade-offs; use it only when the product explicitly defines weights.

## Terminology

- **File-byte exact:** the decoded output byte sequence equals the original encoded input file.
- **Sample exact:** decoded canonical pixel/sample values equal the canonicalized input values.
- **Lossless:** only for a clearly specified input domain and a verified exact decoder path.
- **Lossy:** any reconstruction differs in that specified domain, including a generator that merely looks convincing.
- **Perceptual score:** a measured metric under named implementation/version/settings; it is not proof of losslessness or universal visual equivalence.
- **Machine-task preservation:** performance on a named model/task/dataset split; not equivalent to human fidelity.

## Definition of success

Success is a fully reproducible result for a selected scenario, with raw per-image rows, exact codec settings, artifact hashes, test data rights, device metadata, uncertainty/regressions, and a decoder that rejects malformed data safely. “Surpasses all” is not a project acceptance criterion.
