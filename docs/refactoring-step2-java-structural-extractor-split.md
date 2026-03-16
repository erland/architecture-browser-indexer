# Refactoring Step 2 — Split `JavaStructuralExtractor` into semantic collaborators

This step keeps the Java extraction behavior intact while reducing the amount of semantic logic directly hosted in the traversal/orchestration part of `JavaStructuralExtractor`.

## What changed

`JavaStructuralExtractor` now delegates semantic enrichment to focused collaborators:

- `JavaJaxRsSemantics`
- `JavaJpaSemantics`
- `JavaCdiSemantics`
- `JavaWritePathSemantics`

## Intent

The traversal/orchestration flow in `JavaStructuralExtractor` still owns:

- syntax-tree entry handling
- file/package/import setup
- declared-type collection
- recursive traversal over types, fields, and methods
- base entity creation and containment relationships

The semantic collaborators now own framework/domain enrichment for:

- JAX-RS resources and endpoints
- JPA type/field/method/inheritance semantics
- CDI event publication/observation semantics
- write-path detection and emitted write relationships

## Why this is useful

This reduces the risk of future changes by separating:

- traversal/orchestration concerns
- JAX-RS concerns
- JPA concerns
- CDI concerns
- write-path concerns

That makes the next refactoring steps safer, especially any later extraction-result model cleanup or further Java extractor decomposition.

## Notes

This step is intentionally behavior-preserving. The semantic helper classes are still colocated inside `JavaStructuralExtractor` so they can reuse the current private helper methods and resolution logic without forcing a larger support-model refactor yet.
