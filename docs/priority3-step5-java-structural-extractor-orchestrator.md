# Priority 3 — Step 5: Reduce `JavaStructuralExtractor` to a true stage orchestrator

## What changed

This step reduces `JavaStructuralExtractor` to a thin orchestration wrapper.

A new package-private collaborator, `JavaSyntaxTreeExtractionStage`, now owns the existing Java syntax-tree extraction implementation, including:

- syntax-tree preconditions and file/package setup
- import extraction
- declared-type discovery handoff
- recursive type/field/method traversal
- Java JAX-RS/JPA/CDI/write-path semantic enrichment
- existing internal Java extraction helper logic

`JavaStructuralExtractor` now only:

- exposes the `StructuralExtractor` interface
- delegates `language()`
- delegates `extract(...)`
- preserves the small static compatibility helpers still used by existing collaborators/tests:
  - `isJavaTypeDeclaration(...)`
  - `simpleName(...)`

## Why this step matters

This is an intentionally low-risk seam step.

It does **not** try to finish the deeper Java extraction breakup in one move. Instead it:

1. makes the public extractor entrypoint small and stable,
2. isolates the still-large implementation behind a narrower internal stage boundary,
3. makes the next Java extraction refactors easier to perform inside `JavaSyntaxTreeExtractionStage` without constantly changing the registered extractor class.

## Intended next follow-up

After this step, the remaining Java extraction work can continue inside `JavaSyntaxTreeExtractionStage`, for example by splitting out:

- method dependency emission
- member traversal / visitation
- JAX-RS semantic enrichment
- JPA semantic enrichment
- CDI semantic enrichment
- write-path semantic enrichment
- shared source/metadata helper utilities

## Verification guidance

Run the focused Java extraction and IR safety nets, then the full Maven test suite:

```bash
mvn test
```

If you want a faster targeted pass first:

```bash
mvn -Dtest=JavaDeclarationDiscoveryTest,JavaEntityMapperTest,JavaRelationshipEvidenceEmitterTest,JavaStructuralExtractorSeamSafetyNetTest,ArchitectureIrFactorySeamSafetyNetTest test
```
