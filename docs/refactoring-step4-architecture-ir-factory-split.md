# Step 4 — Split `ArchitectureIrFactory` into focused assemblers/builders

This step keeps the external `ArchitectureIrFactory.createInventoryDocument(...)` API stable while moving major internal responsibilities into smaller package-local collaborators.

## What was introduced

### New assembly/building types
- `ArchitectureIrAssemblyInputs`
  - immutable input bundle for IR assembly
- `ArchitectureIrAssemblyState`
  - immutable assembled state containing repository scope, inventory entity, scopes, entities, relationships, diagnostics, observed type index, and dependency views
- `ArchitectureIrDiagnosticsBuilder`
  - aggregates acquisition, parse, extraction, interpretation, and topology diagnostics
- `ArchitectureIrAssemblyStateBuilder`
  - assembles repository scope, inventory entity, scopes, entities, relationships, and dependency views
- `ArchitectureIrDocumentMetadataBuilder`
  - builds document-level metadata payloads such as inventory summary, parse summary, extraction summary, interpretation summary, topology summary, dependency views, and diagnostic summary
- `ArchitectureIrRunMetadataBuilder`
  - builds `RunMetadata` from assessment + pipeline stage presence

## Resulting responsibility split

### `ArchitectureIrFactory`
Now mainly does orchestration:
1. create assembly inputs
2. build assembled state
3. compute run assessment/completeness
4. build document metadata
5. build run metadata
6. return `ArchitectureIndexDocument`

### `ArchitectureIrAssemblyStateBuilder`
Owns the lower-level structural assembly:
- repository scope
- inventory entity
- scopes
- entity map
- relationship map
- dependency enrichment/rollups
- dependency views
- package-entity enrichment

## Intentional non-goals in this step

This step does **not** yet split the large dependency-view helper logic out of `ArchitectureIrFactory`. That logic is still centralized there to avoid a broad behavioral refactor in the same step.

## Safe follow-up seams

The next extractions are now easier because state and inputs are explicit:
- dependency-view normalization/builders
- package/module/type dependency rollup helpers
- browser-view catalog builders
- package metrics/entity enrichment helpers

## Verification

Recommended local verification:

```bash
mvn test
```

Focused suites especially worth running:

```bash
mvn -Dtest=ArchitectureIrFactoryStructuralExtractionTest test
mvn -Dtest=ArchitectureIrFactoryTopologyTest test
mvn -Dtest=ArchitectureIrFactoryJavaBackendSafetyNetTest test
mvn -Dtest=JavaFrameworkBrowserViewsRegressionTest test
mvn -Dtest=ArchitectureDependencyFixtureRegressionTest test
```
