# Priority 3 Step 9 — Reduce ArchitectureIrFactory to orchestration and stable assembly composition

## What changed

This step moves the remaining low-level IR assembly composition work out of `ArchitectureIrFactory` and leaves the factory as the stable public orchestration entry point.

New helpers:

- `ArchitectureIrAssemblyCompositionSupport`
  - observed-type lookup
  - dependency relationship metadata enrichment
  - synthetic package dependency rollup creation
  - dependency-view shaping
  - package entity enrichment
  - scope-id normalization used during assembly
- `ArchitectureIrCompletenessNotesBuilder`
  - default completeness-note selection based on which pipeline stages were included

## Resulting responsibility split

`ArchitectureIrFactory` now mainly does:

1. accept the public assembly inputs
2. create `ArchitectureIrAssemblyInputs`
3. invoke `ArchitectureIrAssemblyStateBuilder`
4. derive completeness assessment and document metadata
5. assemble the final `ArchitectureIndexDocument`

The factory no longer contains the large block of dependency-view/package-enrichment helper logic.

## Why this matters

This makes `ArchitectureIrFactory` a stable composition root instead of a mixed orchestration-plus-helper monolith. Future maintenance can now target narrower helpers without increasing risk around the public document-creation API.

## Suggested verification

Run at least:

```bash
mvn test
```

And specifically verify:

```bash
mvn -Dtest=ArchitectureIrFactorySeamSafetyNetTest test
mvn -Dtest=FrontendArchitectureEndToEndFixtureRegressionTest test
mvn -Dtest=TypeScriptArchitectureFixtureRegressionTest test
```
