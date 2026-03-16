# Priority 1 Java stage Step 8 — Orchestration-only stage cleanup

## What changed

This step reduced `JavaSyntaxTreeExtractionStage` to a more orchestration-first role by moving two remaining coordination responsibilities into dedicated collaborators:

- `JavaCompilationUnitExtractionFlow`
- `JavaTraversalNodeDispatchFlow`

## New seam layout

`JavaSyntaxTreeExtractionStage` now primarily:

- owns collaborator wiring
- validates that a syntax tree exists
- delegates compilation-unit extraction to `JavaCompilationUnitExtractionFlow`

`JavaCompilationUnitExtractionFlow` now owns:

- package/file scope setup
- import collection
- declared-type discovery
- extraction-context creation
- traversal startup

`JavaTraversalNodeDispatchFlow` now owns:

- per-node dispatch
- owner-context reconstruction
- type-node delegation to `JavaTypeDeclarationFlow`
- field-node delegation to `JavaFieldExtractionFlow`
- method-node delegation to `JavaMethodExtractionFlow`

## Intent

This keeps the stage boundary stable while making its methods read more like orchestration and less like concrete extraction logic.
