# IR notes for Step 2

This repository uses a deliberately small initial IR.

Principles applied:
- version the IR from day one
- keep IDs stable within a payload
- preserve traceability using `sourceRefs`
- keep entity and relationship kinds broad enough for both Java and TypeScript
- represent degraded or partial outcomes explicitly through diagnostics and completeness metadata

The IR is intentionally additive. Future steps can extend the schema while preserving backward compatibility where possible.
