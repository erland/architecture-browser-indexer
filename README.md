# architecture-browser-indexer

Deterministic architecture indexer for the Architecture Browser product.

## Current status

This repository currently contains:
- Step 1 baseline CLI shell
- Step 2 versioned architecture IR model
- Step 3 acquisition and file inventory
- Step 4 Tree-sitter parsing foundation
- Step 5 initial structural extraction for Java and TypeScript
- diagnostics and completeness metadata model
- JSON serializer/deserializer
- golden IR fixtures and regression tests

## Package root

`info.isaksson.erland.architecturebrowser.indexer`

## Build

This repository now targets Java 25 so it can use the official Java Tree-sitter runtime.

```bash
mvn test
```

## Tree-sitter setup

The Maven build now includes the official runtime dependency:

- `io.github.tree-sitter:jtreesitter:0.26.0`

The default registry attempts to create real Tree-sitter parsers for:
- Java
- TypeScript

That runtime still needs the corresponding Tree-sitter language shared libraries to be available.

The indexer now looks for bundled libraries automatically in this order:

1. explicit override via `ARCH_BROWSER_TREE_SITTER_LIB_DIR` or `-Darchbrowser.treesitter.lib.dir=...`
2. `./lib/<detected-os-arch>/`
3. `./lib/`
4. normal system library lookup

A recommended repository layout is:

```text
lib/
  macos-aarch64/
    libtree-sitter-java.dylib
    libtree-sitter-typescript.dylib
  linux-x86_64/
    libtree-sitter-java.so
    libtree-sitter-typescript.so
```

For your machine, the primary target is likely:

```text
lib/macos-aarch64/
```

If the libraries are not available, the indexer still degrades gracefully and emits
`BACKEND_UNAVAILABLE` diagnostics instead of crashing.

## CLI usage

Show help:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- --help
```

Show version:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- --version
```

Index a local repository path and write an IR payload:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- \
  --source /path/to/repository \
  --output /tmp/index-result.json
```

Acquire from Git and write an IR payload:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- \
  --git-url /path/to/local-or-remote-git-repo \
  --git-ref main \
  --output /tmp/index-result.json
```


## Tree-sitter cleanup notes

- The parser registry now takes `TreeSitterConfiguration` explicitly for deterministic tests and CLI wiring.
- Only Java and TypeScript are registered as active default Tree-sitter parsers right now.
- Other languages remain documented as future parser targets, but are not registered by default yet.


## Bundled Tree-sitter native libraries (macOS Apple Silicon)

For macOS arm64 development, build the pinned Tree-sitter runtime and grammar libraries with:

```bash
./scripts/setup-treesitter-macos-aarch64.sh
```

The script pins the grammar repos to `tree-sitter-java v0.23.5` and `tree-sitter-typescript v0.23.2` by default, and places the resulting libraries under `lib/macos-aarch64/`.


## Tree-sitter native library path during tests

The Maven Surefire configuration sets `java.library.path` to `lib/macos-aarch64` so `jtreesitter` can find `libtree-sitter.dylib` during test runs on Apple Silicon macOS. The official Java Tree-sitter docs state that the libraries can be installed in the OS-specific library search path or in `java.library.path`.


Step 7 adds a first-pass interpretation layer that infers higher-level architecture concepts such as endpoints, services, persistence adapters, UI modules, and startup points from extracted structural facts.


Step 8 adds logical scoping and relationship inference, including directory and source-root module scopes, internal Java/TypeScript dependency resolution, and rolled-up package/module relationships.


Step 9 adds explicit diagnostic summaries, degraded-path reporting, and more consistent completeness/partial-result assessment in the generated IR.


Step 10 adds a publication/export contract. CLI output now writes both the payload JSON and a sibling `.manifest.json` file with contract, checksum, and compatibility metadata.


Step 11 adds incremental reindex foundations: file fingerprints, snapshot JSON, changed-file detection, and a minimal reprocessing plan for added/changed files.


Step 12 adds worker mode and container deployment assets. The CLI can now run from a worker request JSON, and the repo includes Docker/GHCR packaging files for deployment.


Step 13 expands the regression suite and hardens extension seams so extractors, interpretation rules, topology resolution, and export targets can be extended with less core-code churn.




## Java backend semantics phase 1

The repository now includes a first architect-facing Java backend semantics layer for deterministic analysis of:

- JAX-RS resources and endpoints
- JPA entity-model relationships
- CDI event publication and observer flows
- service/repository write paths
- backend-specific browser/export dependency views

Key regression coverage added during this phase:

- `JavaBackendFrameworkBaselineRegressionTest`
- `JavaJaxRsEndpointRegressionTest`
- `JavaJpaEntityModelRegressionTest`
- `JavaCdiEventGraphRegressionTest`
- `JavaWritePathRegressionTest`
- `JavaFrameworkTopologyRegressionTest`
- `JavaFrameworkBrowserViewsRegressionTest`
- `JavaBackendArchitectureEndToEndFixtureRegressionTest`

A compact continuation summary is available in:

- `docs/java-backend-semantics-phase1-summary.md`

## HTTP worker service

Step 14 adds a thin HTTP worker wrapper around the existing worker-mode pipeline.

Start it from Maven:

```bash
mvn -q exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli -- \
  --serve-http \
  --http-host 0.0.0.0 \
  --http-port 8080 \
  --http-workspace-dir ./build/http-worker
```

Health check:

```bash
curl http://localhost:8080/health
```

Run an index job over HTTP:

```bash
curl -X POST http://localhost:8080/api/index-jobs/run \
  -H 'Content-Type: application/json' \
  -d '{
    "jobId": "demo-job-001",
    "repositoryId": "demo-repo",
    "sourcePath": "/workspace/demo"
  }'
```

When `outputPath` is omitted, the HTTP worker allocates a temporary output file under the configured worker workspace and returns the generated IR document inline together with the execution summary and manifest preview.


## Packaging

The Maven `package` build now produces a runnable shaded jar with `IndexerCli` as the main class so the Docker image can start the HTTP worker with `java -jar`.


## Worker diagnostics

- The HTTP worker now logs request start/success/failure details and full stack traces for both `Exception` and `Error` failures.
- For containerized runs on Apple Silicon, the default native library directory is `lib/linux-aarch64`, matching the Linux arm64 runtime used by Docker.
- If a worker request fails, inspect the `indexer` container logs to see the full stack trace and root-cause details.


## Refactoring status (steps 1–10)

The refactoring-analysis plan has now been implemented through Step 10.

Completed structural refactors:
- Step 1 — baseline safety net for current behavior
- Step 2 — split `JavaStructuralExtractor` into semantic collaborators
- Step 3 — introduce internal extraction result models for Java extraction
- Step 4 — split `ArchitectureIrFactory` into focused assemblers/builders
- Step 5 — modularize TypeScript/frontend extraction
- Step 6 — introduce Angular shared support/model layer
- Step 7 — split `TopologyService`
- Step 8 — introduce application orchestration below CLI
- Step 9 — clean up support/helper and interpretation classes
- Step 10 — final cleanup, docs, and continuation notes

The repository should now be read as a pipeline with thinner orchestration classes and more focused collaborators inside the heavy extraction / IR / topology / interpretation stages.

Key current seams:
- Java extraction orchestration: `extract/JavaStructuralExtractor`
- Java extraction collaborators: `JavaJaxRsSemantics`, `JavaJpaSemantics`, `JavaCdiSemantics`, `JavaWritePathSemantics`
- TypeScript extraction orchestration: `extract/TypeScriptStructuralExtractor`
- TypeScript/frontend collaborators: `TypeScriptImportExtractor`, `TypeScriptDeclarationExtractor`, `TypeScriptFrontendSemanticsExtractor`
- Angular shared helpers/models: `AngularDecoratorModel*`, `AngularLiteralSupport`, `AngularReferenceSupport`, `AngularSourceSupport`
- IR orchestration: `ir/ArchitectureIrFactory`
- IR helpers: `ArchitectureIrAssemblyStateBuilder`, `ArchitectureIrDiagnosticsBuilder`, `ArchitectureIrDocumentMetadataBuilder`, `ArchitectureIrRunMetadataBuilder`
- Topology orchestration: `topology/TopologyService`
- Topology helpers: `TopologyScopeInferenceService`, `TopologyRelationshipRollupService`
- Application orchestration: `application/IndexerApplicationService`
- Interpretation helpers: `JavaBackendRoleClassifier`, `JavaEndpointInterpreterSupport`, `TypeScriptFrontendClassifier`

## Recommended verification after refactoring

Run the full suite:

```bash
mvn test
```

Run the most load-bearing safety-net suites first when iterating locally:

```bash
mvn -Dtest=JavaJaxRsStructuralExtractionTest test
mvn -Dtest=ArchitectureIrFactoryJavaBackendSafetyNetTest test
mvn -Dtest=AngularSharedSupportTest test
mvn -Dtest=InterpretationHelperClassificationTest test
```

For broader regression confidence, also run the architecture-facing fixture suites:

```bash
mvn -Dtest=JavaBackendArchitectureEndToEndFixtureRegressionTest test
mvn -Dtest=FrontendArchitectureEndToEndFixtureRegressionTest test
mvn -Dtest=TypeScriptArchitectureFixtureRegressionTest test
```

## Refactoring continuation guidance

If a future chat continues cleanup work, prefer this order:
1. small duplication-reduction passes inside new helper classes
2. package/subpackage moves only when they reduce cognitive load without changing public wiring
3. fixture expansion for any new architect-facing semantic
4. only then consider deeper IR-model or pipeline-contract changes

A compact summary of the completed refactoring phase is available in:
- `docs/refactoring-phase-summary.md`
- `docs/refactoring-step10-final-cleanup-and-continuation.md`


## Priority 3 status

Priority 3 hotspot reduction is now structurally complete through Step 10. The Java extraction and IR assembly seams now have focused hardening tests and a final continuation note in `docs/priority3-step10-final-hardening.md`.


## Priority 1 refactor status

The Java-stage and IR-assembly hotspot refactor is now complete through Step 13.

Current state:
- `JavaSyntaxTreeExtractionStage` is reduced to orchestration-first extraction setup and delegates detailed work to dedicated flows and semantics supports.
- Java traversal now uses explicit dispatch results instead of relying on implicit local coordination.
- `ArchitectureIrAssemblyCompositionSupport` is reduced to orchestration-first composition logic and the main IR assembly path now runs through explicit composition inputs/results.
- Dependency-view assembly, browser/dependency handoff, and compatibility wrappers are split into explicit collaborators instead of being mixed into the main composition seam.
- Broader end-to-end regression checks now cover both hotspot areas.

Primary follow-up should now be:
- keeping seam tests aligned with stable architect-facing contracts
- making only small duplication-reduction cleanups inside extracted helpers
- adding future Java or IR semantics in the dedicated owning collaborator instead of re-growing the orchestration seams

Recommended hotspot verification:

```bash
mvn -Dtest=JavaCompilationUnitExtractionFlowTest,JavaTraversalNodeDispatchFlowTest,JavaTypeDeclarationFlowTest,JavaFieldExtractionFlowTest,JavaMethodExtractionFlowTest test
mvn -Dtest=ArchitectureIrDependencyRelationshipEnricherTest,ArchitectureIrDependencyViewAssemblySupportTest,ArchitectureIrDependencyViewCatalogSupportTest,ArchitectureIrBrowserDependencyViewHandoffSupportTest,ArchitectureIrAssemblyCompositionOrchestrationTest test
mvn -Dtest=JavaStageEndToEndFixtureRegressionTest,ArchitectureIrCompositionEndToEndFixtureRegressionTest test
```

See also:
- `docs/priority1-java-stage-and-ir-assembly-final-cleanup.md`
- `docs/priority1-java-stage-and-ir-assembly-continuation.md`


## Wave 2 refactor status

Wave 2 is now structurally complete through Step 11.

Current state:
- TypeScript declaration handling is split into discovery, declaration-family metadata shaping, and framework enrichment seams.
- IR dependency-view assembly is reduced to phase orchestration over normalization and per-view builders.
- Dependency relationship enrichment is split into family-specific enrichers.
- Java semantics are organized by domain-specific supports behind a thin compatibility facade.
- Frontend routing is split into discovery, normalization, and emission phases.
- High-value metadata flows now use typed intermediate models in selected IR and Java semantics paths.
- Broad regressions are hardened around contract-style assertions rather than brittle representation checks.

Recommended Wave 2 verification:

```bash
mvn -Dtest=TypeScriptDeclarationDiscoverySupportTest,TypeScriptDeclarationMetadataShapingSupportTest,TypeScriptFrameworkEnrichmentSupportTest test
mvn -Dtest=ArchitectureIrDependencyAssemblyPhasesTest,ArchitectureIrDependencyEnrichmentFamiliesTest,TypedMetadataModelAdaptersTest test
mvn -Dtest=JavaDomainSemanticsSupportsSeamTest,JavaTypedSemanticsModelsTest test
mvn -Dtest=FrontendRoutingPhasesTest test
mvn -Dtest=JavaStageEndToEndFixtureRegressionTest,ArchitectureIrCompositionEndToEndFixtureRegressionTest test
mvn test
```

See also:
- `docs/refactoring-wave2-acceptance.md`
- `docs/refactoring-wave2-continuation.md`
- `docs/refactoring-wave2-test-contracts.md`
