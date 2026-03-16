# Priority 3 Step 8 — Centralize metadata-map conversion and evidence shaping for IR assembly

This step extracts repeated dependency/evidence metadata shaping logic from `ArchitectureIrFactory` into a focused internal helper:

- `ArchitectureIrDependencyMetadataSupport`

## What was centralized

- metadata map copying and immutable finalization
- `dependencySource` / `dependencyCategory` set population
- framework/framework-relationship / architecture-view-kind accumulation
- import-evidence relationship metadata shaping
- repeated summary-list conversion for normalized dependency metadata maps

## Main intent

Reduce repeated handwritten `Map<String, Object>` shaping inside `ArchitectureIrFactory` without changing the external IR contract.

## Important preserved contracts

- evidence dependencies still carry `dependencyTier = supporting-evidence`
- evidence dependencies still carry `evidenceKind = file-import`
- normalized dependency views still expose:
  - `dependencySources`
  - `dependencyCategories`
  - `frameworks`
  - `frameworkRelationships`
  - `architectureViewKinds`
  - `evidenceRelationshipIds`
  - `evidenceLabels`

## Follow-on work

The next step can continue reducing `ArchitectureIrFactory` by moving orchestration-only concerns out while keeping this helper as the stable metadata/evidence seam.
