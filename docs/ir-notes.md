# IR notes for Step 2

This repository uses a deliberately small initial IR.

Principles applied:
- version the IR from day one
- keep IDs stable within a payload
- preserve traceability using `sourceRefs`
- keep entity and relationship kinds broad enough for both Java and TypeScript
- represent degraded or partial outcomes explicitly through diagnostics and completeness metadata

The IR is intentionally additive. Future steps can extend the schema while preserving backward compatibility where possible.


## Java backend browser-facing dependency views

The IR now also carries browser-facing dependency-view buckets for first-pass Java backend semantics. These are intended for architect exploration rather than low-level parser debugging.

Current backend-oriented dependency-view families include:

- `endpointTypeDependencies`
- `endpointModuleDependencies`
- `entityModelTypeDependencies`
- `entityModelModuleDependencies`
- `observerTypeDependencies`
- `observerModuleDependencies`
- `writePathTypeDependencies`
- `writePathModuleDependencies`

The export layer also exposes dedicated Java backend browser-view descriptors so the browser can present endpoint, entity-model, observer/event, and write-path perspectives directly.

These views remain additive to the existing generic package/type/module dependency views.
