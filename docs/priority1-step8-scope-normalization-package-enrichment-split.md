# Priority 1 — Step 8: Split scope normalization and package entity enrichment into dedicated supports

## What changed

This step moves two remaining responsibilities out of `ArchitectureIrAssemblyCompositionSupport`:

- scope-id normalization -> `ArchitectureIrScopeNormalizationSupport`
- package entity metric enrichment -> `ArchitectureIrPackageEntityEnrichmentSupport`

`ArchitectureIrAssemblyStateBuilder` now calls those dedicated supports directly during entity assembly.

## Why this matters

This reduces the remaining surface area of `ArchitectureIrAssemblyCompositionSupport` and makes the assembly flow easier to reason about:

1. entity assembly and scope normalization are now an explicit seam
2. package metric enrichment is no longer hidden inside the broader dependency-view composition class
3. later steps can continue shrinking IR assembly composition without changing the public assembly entrypoints

## Compatibility

For low-risk continuity, `ArchitectureIrAssemblyCompositionSupport` still keeps thin delegating wrappers for:

- `normalizeScopeId(...)`
- `enrichPackageEntities(...)`

so any internal callers or safety-net tests using the old shape keep working while the primary flow moves to the new dedicated supports.
