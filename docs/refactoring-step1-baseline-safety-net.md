# Step 1 — Baseline safety net for current behavior

This document is the implementation baseline for the first refactoring step. Its purpose is to make the protected behavior explicit **before** larger internal splits begin.

## Goal

Lock the current externally visible behavior around the highest-risk refactor areas so later work can safely reduce complexity without changing semantics by accident.

## Hotspot inventory and intended splits

### 1. `extract/JavaStructuralExtractor.java`
Current role:
- Java declaration extraction
- inheritance/interface relationships
- JAX-RS resource and endpoint semantics
- JPA entity / association / embedded-value semantics
- CDI publish / observe semantics
- write-path detection
- downstream metadata shaping

Intended split direction:
- `JavaTypeDeclarationExtractor`
- `JavaMemberExtractor`
- `JavaInheritanceRelationshipExtractor`
- `JavaJaxRsSemanticExtractor`
- `JavaJpaSemanticExtractor`
- `JavaCdiSemanticExtractor`
- `JavaWritePathExtractor`
- thin orchestration retained in `JavaStructuralExtractor`

### 2. `ir/ArchitectureIrFactory.java`
Current role:
- document assembly
- entity/relationship/scope mapping
- summaries and metadata
- browser/dependency view shaping
- completeness / diagnostics shaping

Intended split direction:
- `IrEntityAssembler`
- `IrRelationshipAssembler`
- `IrScopeAssembler`
- `IrDiagnosticsSummaryBuilder`
- `IrCompletenessMetadataBuilder`
- `IrBrowserMetadataBuilder`
- `IrExportMetadataBuilder`
- thin orchestration retained in `ArchitectureIrFactory`

### 3. `extract/TypeScriptStructuralExtractor.java`
Current role:
- generic TypeScript extraction
- React semantics
- Angular semantics
- frontend routing and composition wiring

Intended split direction:
- `TypeScriptDeclarationExtractor`
- `TypeScriptImportDependencyExtractor`
- `FrontendFrameworkExtractionCoordinator`
- framework-specific bundles under dedicated subpackages

### 4. `topology/TopologyService.java`
Current role:
- scope inference
- topology entity creation
- dependency rollups
- summary shaping

Intended split direction:
- `ScopeInferenceService`
- `TopologyEntityBuilder`
- `InternalDependencyResolver`
- `TopologyRollupService`
- `TopologySummaryBuilder`

### 5. `cli/IndexerCli.java`
Current role:
- command parsing
- pipeline orchestration
- worker/http dispatch
- output writing and summary rendering

Intended split direction:
- `IndexerApplicationService`
- `IndexRunRequest`
- `IndexRunResult`
- CLI reduced to parsing/delegation

## Behavior that must stay stable during the next refactor steps

### Java extraction behavior
Protect these semantic expectations:
- JAX-RS resources and endpoint entities are still extracted with HTTP method and resolved path metadata.
- JPA entities, embedded values, field/property access metadata, and association relationships remain intact.
- CDI event publisher and observer relationships remain intact, including async observer metadata.
- write-path detection continues to mark relevant methods and relationships for persist/update flows.

### IR output behavior
Protect these output expectations:
- IR document remains valid according to `ArchitectureIrValidator`.
- entities, relationships, scopes, and summaries continue to be emitted for the Java backend fixture.
- framework-specific dependency views and browser-view metadata remain present.
- no contract drift in dependency-view buckets, browser-view catalog metadata, or topology/scoping output.

## Golden / fixture suites to protect after every refactor step

### Highest-priority Java semantics / regression suites
- `JavaJaxRsStructuralExtractionTest`
- `JavaJpaStructuralExtractionTest`
- `JavaCdiStructuralExtractionTest`
- `JavaWritePathStructuralExtractionTest`
- `JavaCdiEventGraphRegressionTest`
- `JavaJpaEntityModelRegressionTest`
- `JavaWritePathRegressionTest`
- `JavaBackendFrameworkBaselineRegressionTest`
- `JavaBackendRoleInterpretationRegressionTest`
- `JavaFrameworkTopologyRegressionTest`
- `JavaFrameworkBrowserViewsRegressionTest`
- `JavaBackendArchitectureEndToEndFixtureRegressionTest`
- `ArchitectureDependencyFixtureRegressionTest`

### Highest-priority IR / contract suites
- `ArchitectureIrFactoryStructuralExtractionTest`
- `ArchitectureIrFactoryInterpretationTest`
- `ArchitectureIrFactoryTopologyTest`
- `ArchitectureIrFactoryPartialResultTest`
- `ArchitectureIrFactoryJavaBackendSafetyNetTest`
- `ArchitectureIrJsonTest`
- `ExportBundleWriterTest`
- `ExportContractRegressionTest`
- `PipelineRegressionSmokeTest`

## Exact suites to run after each refactor step

### Broad full safety net
```bash
mvn test
```

### Java-semantics focused pass
```bash
mvn -Dtest='*Java*Test,*Java*RegressionTest,*ArchitectureDependencyFixtureRegressionTest' test
```

### IR / export / contract focused pass
```bash
mvn -Dtest='*Ir*Test,*Export*Test,*PipelineRegressionSmokeTest,*JavaBackendArchitectureEndToEndFixtureRegressionTest' test
```

### Fastest targeted pass for early Java extractor splits
```bash
mvn -Dtest='JavaJaxRsStructuralExtractionTest,JavaJpaStructuralExtractionTest,JavaCdiStructuralExtractionTest,JavaWritePathStructuralExtractionTest,JavaCdiEventGraphRegressionTest,JavaJpaEntityModelRegressionTest,JavaWritePathRegressionTest,JavaBackendArchitectureEndToEndFixtureRegressionTest,ArchitectureIrFactoryJavaBackendSafetyNetTest' test
```

## Notes for the next implementation steps

- Start the refactor in `JavaStructuralExtractor`, but do **not** change public semantics intentionally in Step 2.
- When moving logic into collaborators, keep the regression-first style: move one concern at a time, rerun the targeted Java safety net, then continue.
- If any later step requires intentional output changes, update this document and the affected regression/golden assertions in the same commit so the drift is explicit.
