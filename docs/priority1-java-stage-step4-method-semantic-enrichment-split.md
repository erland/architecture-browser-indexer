# Priority 1 Java Stage Step 4 — Isolate method semantic enrichment from method structural extraction

## What changed

This step reduces coupling between method structural extraction and method semantic policy by introducing dedicated method-semantic collaborators:

- `JavaJaxRsMethodSemantics`
- `JavaJpaMethodSemantics`
- `JavaCdiMethodSemantics`
- `JavaWritePathMethodSemantics`

`JavaMethodSemanticsFlow` now acts as a narrow orchestration layer that prepares `JavaMethodContext` once and delegates semantic application to the dedicated helpers.

## Why this helps

Before this step, the method semantic flow still directly owned all framework-specific method semantics. After this step:

- structural method extraction remains in `JavaMethodExtractionFlow`
- semantic method policy is isolated behind dedicated helpers
- future changes to JAX-RS, JPA, CDI, or write-path method semantics can be made with less risk to structural extraction flow

## Intended behavior

No document/export contract changes are intended. The refactor preserves the existing Java method extraction behavior while narrowing the internal seam between structure and semantics.
