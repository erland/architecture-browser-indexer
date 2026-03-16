# Priority 1 Java Stage Step 9 — Hardening the new seams

This step adds narrow unit tests around the new orchestration seams introduced while shrinking `JavaSyntaxTreeExtractionStage`, plus one broader regression-oriented check.

## Added narrow seam tests

- `JavaCompilationUnitExtractionFlowTest`
- `JavaTraversalNodeDispatchFlowTest`
- `JavaOwnerContextContractsTest`

These tests freeze:

- compilation-unit setup behavior
- import evidence emission from the compilation-unit flow
- traversal node dispatch and owner handoff behavior
- owner/result object round-tripping and handled/not-handled contracts

## Added broader regression check

- `JavaSyntaxTreeExtractionStageEndToEndRegressionTest`

This regression check ensures the Java stage still produces architect-facing semantics end-to-end for a compact backend-style fixture, including:

- JAX-RS endpoint metadata
- CDI published event metadata
- write-path metadata
- API dependency emission

## Intent

The goal of this step is not to change production behavior. It is to make the new internal seams safer to evolve in later cleanup steps while preserving the existing extraction contract.
