# Priority 1 — Step 7: Split synthetic package dependency rollups into a dedicated builder

## What changed

This step extracts synthetic package-to-package dependency rollup creation out of `ArchitectureIrAssemblyCompositionSupport` into:

- `ArchitectureIrSyntheticPackageDependencyRollupBuilder`

`ArchitectureIrAssemblyCompositionSupport.ensurePackageDependencyRelationships(...)` remains as a compatibility seam and now delegates to the dedicated builder.

## Why this helps

This removes one more distinct responsibility from the remaining IR internal god class:

- package rollup relationship synthesis
- package-entity lookup for rollup endpoints
- rollup metadata shaping

The result is a smaller `ArchitectureIrAssemblyCompositionSupport` with clearer boundaries before the next split steps.

## Intended behavior

No external behavior change is intended. The package rollup builder should preserve:

- generation of synthetic `USES` relationships between package entities
- `rollup = package-package`
- `dependencyView = package`
- propagation of dependency provenance such as `dependencySource` and `dependencyCategory`

## Next likely step

Proceed with:

- Step 8 — Split scope normalization and package entity enrichment into dedicated supports
