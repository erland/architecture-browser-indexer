# Priority 1 Java stage Step 2 — Split type declaration handling from stage-level node coordination

This step extracts type declaration handling out of `JavaSyntaxTreeExtractionStage` into `JavaTypeDeclarationFlow`.

## What moved

- type-declaration detection handoff
- type entity mapping
- file containment relationship emission for type entities
- type dependency emission for extends/implements facts
- type semantic enrichment and JPA inheritance enrichment
- ownership handoff result calculation

## Result

`JavaSyntaxTreeExtractionStage` still coordinates traversal and node dispatch, but the concrete type-declaration behavior now lives behind a narrower seam.

## Next likely follow-up

- split field extraction from method extraction completely
- keep shrinking the stage into an orchestration-first coordinator
