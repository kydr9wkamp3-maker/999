# ADR 0002: Keep the MVP codec deterministic and CPU-only

- Status: Accepted for MVP
- Date: 2026-11-27

## Context

Diffusion/INR/learned codecs can require weights, training pipelines, external runtimes, accelerators, large memory, and device-specific kernels. None is integrated or verified for this project. Adding a model before the exact path, data policy, device tests, and reproducible decode contract are ready would make the prototype less auditable.

## Decision

Keep ADM1 v1 as a small deterministic C++ CPU path. Treat neural, semantic, NPU/GPU, and INR channels as research experiments behind feature flags and separate codecs/build variants, not as hidden changes to ADM1.

## Consequences

- The current codec is not claimed to beat standard codecs.
- A later learned codec requires model card, source/weight license, fixed preprocessing and inference ABI, supported Android ABI/backend matrix, memory/energy budget, reproducibility policy, adversarial tests, and clear perceptual-vs-exact semantics.
- The exact path remains available without weights or network access.
