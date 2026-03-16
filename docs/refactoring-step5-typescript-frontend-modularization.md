# Step 5 — Modularize TypeScript/frontend extraction

This step keeps the external `StructuralExtractor` registration stable while splitting the growing `TypeScriptStructuralExtractor` into focused collaborators.

## What was introduced

### New TypeScript extraction collaborators
- `TypeScriptExtractionContext`
  - immutable package-local context passed between the TypeScript/frontend extraction stages
- `TypeScriptImportExtractor`
  - owns import parsing, import classification, and import dependency relationships
- `TypeScriptDeclarationExtractor`
  - owns TypeScript declaration extraction, member extraction, hierarchy relationships, and declared-type dependency inference
- `TypeScriptFrontendSemanticsExtractor`
  - owns orchestration of Angular/React/routing semantic enrichers

## Resulting responsibility split

### `TypeScriptStructuralExtractor`
Now mainly does orchestration:
1. validate syntax-tree availability
2. create file scope and file module entity
3. build `TypeScriptExtractionContext`
4. run import extraction
5. run TypeScript declaration/member extraction
6. run frontend framework semantic enrichment

### `TypeScriptImportExtractor`
Owns:
- `import_statement` traversal
- type-only / side-effect / relative / package import classification
- inferred internal vs external target creation
- import dependency relationship metadata

### `TypeScriptDeclarationExtractor`
Owns:
- top-level type/function discovery
- named-entity indexing
- class/interface member extraction
- property/method declared-type dependency inference
- `extends` / `implements` hierarchy relationships

### `TypeScriptFrontendSemanticsExtractor`
Owns:
- Angular framework relationship extraction
- Angular template composition extraction
- Angular dependency injection extraction
- React JSX/context/custom hook extraction
- frontend routing extraction

## Intentional non-goals in this step

This step does **not** yet split the Angular-specific helper classes into a dedicated shared support/model sub-layer. That remains the next safe seam.

## Safe follow-up seams

The next extractions are now easier because the orchestration and declaration logic are separated:
- move Angular-specific helpers into an `extract/angular` support/model cluster
- introduce frontend extraction result models if framework-level state starts to grow
- split generic TypeScript declarations from framework-aware enrichment even further if more frameworks are added

## Verification

Recommended local verification:

```bash
mvn test
```

Focused suites especially worth running:

```bash
mvn -Dtest=TypeScriptStructuralExtractorTest test
mvn -Dtest=TypeScriptArchitectureFixtureRegressionTest test
mvn -Dtest=FrontendArchitectureEndToEndFixtureRegressionTest test
mvn -Dtest=AngularDecoratorMetadataExtractorTest test
mvn -Dtest=ReactJsxCompositionExtractorTest test
```
