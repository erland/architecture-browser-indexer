# Architecture Browser Indexer — Development Plan

## 1. Goal

Build a deterministic indexing pipeline that converts source repositories into a versioned architecture IR consumable by the platform. The indexer should prioritize correctness, traceability, extensibility, and regression-testability over breadth.

## 2. Technology Choices

### Proposed stack
- Primary implementation: Java
- Parsing layer: Tree-sitter bindings or integration
- Data model: versioned JSON IR
- Test strategy: fixture-heavy unit and integration tests
- Packaging: containerized worker and CLI entry point

### Brief justification
- Java is a strong fit for a long-lived deterministic pipeline and integrates well with the surrounding backend ecosystem.
- Tree-sitter provides efficient incremental parsing and broad language support for structural extraction.
- JSON IR makes platform integration and golden-fixture regression testing straightforward.
- A CLI plus worker form factor supports both local testing and service deployment.

## 3. Repository Responsibilities

This repository includes:
- source acquisition
- file scanning/classification
- language/framework extractors
- interpretation rules
- architecture IR generation
- diagnostics
- publication/export code
- CLI/worker entry points

This repository excludes:
- user-facing browsing UI
- workspace/repository management
- snapshot storage or presentation
- overlays and saved views

## 4. Project Structure

Proposed top-level structure:

- `/src/main/java/.../acquisition` — source retrieval and revision handling
- `/src/main/java/.../scan` — file inventory and classification
- `/src/main/java/.../extract` — language-specific structural extraction
- `/src/main/java/.../interpret` — framework and architecture interpretation
- `/src/main/java/.../ir` — normalized model and serializers
- `/src/main/java/.../publish` — output and publishing adapters
- `/src/main/java/.../cli` — CLI entry point
- `/src/test/...` — unit and integration tests
- `/src/test/resources/fixtures` — representative source fixtures and golden IR
- `/docs` — extractor and IR docs

## 5. Delivery Strategy

Start with a narrow set of supported languages and high-value architecture concepts. Build the pipeline in stages with strong golden-fixture verification before expanding language and framework coverage.

## 6. Assumptions

- Platform import contract can evolve in lockstep initially, then stabilize.
- MVP focus is on Java, TypeScript or JavaScript, SQL, and config files.
- Initial semantic richness comes from deterministic structure and framework rules rather than full compiler-grade symbol resolution.

## 7. Step-by-Step Plan

## Step 1. Create repository baseline and CLI shell

### Deliverables
- repository skeleton
- build/test setup
- CLI entry point that accepts source location and output target
- basic docs and README

### Verification
- project builds
- CLI runs and prints version and help
- test harness runs

## Step 2. Define versioned architecture IR and diagnostics model

### Deliverables
- IR schema and classes
- diagnostics model
- completeness metadata model
- serializer and deserializer
- golden fixture examples

### Verification
- IR round-trip serialization tests pass
- schema fixtures validate
- version metadata is present

## Step 3. Implement acquisition and file inventory

### Deliverables
- local path acquisition
- Git acquisition
- revision metadata capture
- file inventory scan
- ignore and filter behavior
- language and type classification baseline

### Verification
- local and Git sources can be scanned
- file inventory is deterministic
- unsupported files do not break scan

## Step 4. Integrate Tree-sitter parsing foundation

### Deliverables
- Tree-sitter integration layer
- parser registry
- common parse result abstraction
- graceful parse error handling

### Verification
- sample supported files parse successfully
- syntax errors produce diagnostics, not crashes
- parse tree access is available to extractors

## Step 5. Implement initial structural extractors for Java and TypeScript

### Deliverables
- Java extractor for packages, classes, interfaces, methods, imports, and annotations
- TypeScript or JavaScript extractor for modules, functions, classes, imports, and decorators where relevant
- source reference capture
- structural fact model

### Verification
- sample Java and TS fixtures produce expected structural facts
- results are deterministic across runs
- malformed files degrade gracefully

## Step 6. Implement extractors for SQL and config files

### Deliverables
- SQL artifact extraction basics
- YAML, JSON, and properties extraction basics
- config-derived markers useful for architecture interpretation
- source references and diagnostics

### Verification
- supported config fixtures produce expected facts
- diagnostics identify unsupported constructs cleanly

## Step 7. Build interpretation framework and first high-value rules

### Deliverables
- interpretation pipeline abstraction
- Java framework rules for Spring or Quarkus-like patterns
- TypeScript rules for React or Node service patterns
- architecture-entity mapping rules for endpoints, services, persistence, startup points, and integrations

### Verification
- representative fixtures produce expected architecture entities
- rules remain deterministic
- traceability back to source facts is preserved

## Step 8. Build relationship inference and logical scoping

### Deliverables
- scope hierarchy generation for repo, module, package, and component
- relationship inference between entities
- ownership and container mapping
- dependency roll-up logic

### Verification
- entity hierarchy is stable
- inferred relationships are explainable and traceable
- duplicate names across scopes remain unambiguous

## Step 9. Add diagnostics, completeness, and partial-result handling

### Deliverables
- categorized diagnostics
- partial result metadata
- fatal vs non-fatal error rules
- run summary output

### Verification
- isolated parse failures do not destroy whole-run output unnecessarily
- malformed output is blocked from publication
- partial-result scenarios are represented correctly

## Step 10. Implement publication and export contract

### Deliverables
- final JSON export format
- publish adapter for platform handoff
- version compatibility checks
- sample published payloads

### Verification
- exported payload matches contract
- version metadata and diagnostics are included
- sample platform import fixtures succeed

## Step 11. Add incremental reindex foundations

### Deliverables
- changed-file detection model
- selective reprocessing flow where feasible
- cache or state abstraction
- equivalence verification against full reindex for supported scenarios

### Verification
- incremental result matches full reindex for fixture scenarios
- cache invalidation rules are safe

## Step 12. Package worker mode and container deployment

### Deliverables
- worker or service entry point
- container image
- environment-based configuration
- docs for local CLI and integrated worker operation

### Verification
- CLI works locally
- worker runs in container
- platform-facing publish path can be exercised in integration tests

## Step 13. Expand regression suite and harden extension seams

### Deliverables
- richer fixture matrix
- golden IR regression tests
- extension interfaces for new languages and rules
- contributor docs for adding extractors

### Verification
- regression suite catches IR changes
- adding a new extractor does not require cross-cutting rewrites

## 8. Testing Strategy

### Unit tests
- classifier logic
- extractor helpers
- interpretation rules
- IR builders
- diagnostics aggregation

### Integration tests
- source acquisition
- parse and extract pipelines
- end-to-end indexing of representative mini-repositories
- platform contract export tests

### Golden fixtures
- source fixture to expected IR JSON
- source fixture to expected diagnostics
- full vs incremental equivalence fixtures where supported

## 9. Minimal Safety Net

Before major expansion, ensure:
- parser smoke tests pass
- representative Java, TS, and config fixtures pass
- golden IR outputs remain stable unless intentionally changed
- publication contract tests pass

## 10. Verification Commands

The repository should expose clear commands for:
- build project
- run unit tests
- run integration tests
- run golden fixture tests
- execute CLI on sample repo
- build container image
- run worker locally or containerized

## 11. Risks and Mitigations

### Risk: Tree-sitter integration complexity
Mitigation: isolate parser adapter layer and keep extractors independent of binding details.

### Risk: framework interpretation becomes brittle
Mitigation: keep interpretation rules explicit, test heavily with fixtures, and preserve diagnostics when confidence is limited.

### Risk: IR churn destabilizes platform
Mitigation: version IR early and maintain contract fixtures across repos.

### Risk: trying to support too many languages too early
Mitigation: stay narrow in MVP and expand only after strong regression coverage.

## 12. Expected End State

At MVP completion, the repository should deliver:
- deterministic acquisition and scan pipeline
- Tree-sitter-backed extraction for priority languages and files
- framework-aware interpretation into a versioned architecture IR
- diagnostics and partial-result support
- publishable outputs consumable by the platform
- a strong fixture-based regression suite
