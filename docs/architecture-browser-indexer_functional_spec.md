# Architecture Browser Indexer — Functional Specification

## 1. Purpose

The Architecture Browser Indexer is a deterministic code-intelligence pipeline that acquires source repositories, scans their contents, extracts structural and framework-aware facts, and emits a normalized architecture-oriented result for consumption by the platform.

Its purpose is not to present a UI directly to end users. Its purpose is to convert source trees into a stable, browseable architecture model.

## 2. Scope

### In scope
- repository acquisition
- file inventory and technology detection
- Tree-sitter-based structural extraction
- language/framework-specific interpretation rules
- normalized architecture IR generation
- diagnostics and partial-result reporting
- incremental reindex support
- publication/export of versioned normalized results

### Out of scope
- user-facing workspace management
- long-term snapshot browsing
- overlays/notes/saved views
- architecture review workflows
- LLM summarization
- source code modification

## 3. Actors

### Platform
Requests indexing and receives normalized results.

### Administrator/developer
Configures indexer behavior, monitors failures, and extends extractors.

### Extractor subsystem
Language- and framework-aware logic that inspects files and emits facts.

## 4. Core Concepts

### Source unit
A concrete repository revision or local source snapshot selected for analysis.

### File inventory
A catalog of files and metadata discovered during scanning.

### Structural fact
A low-level extracted observation such as symbol, import, annotation, route declaration, configuration key, or SQL artifact.

### Interpretation rule
A deterministic rule that maps structural facts into higher-level architecture meaning.

### Architecture IR
The normalized intermediate representation emitted by the indexer and consumed by the platform.

### Diagnostic
A warning, error, or informational item produced during scanning, extraction, interpretation, or publication.

### Partial result
A result where some subset of files or technologies failed while the remaining output remains usable.

## 5. Functional Goals

The indexer must support these goals:

1. Acquire a specific repository/revision for analysis.
2. Detect languages and relevant technology markers.
3. Extract deterministic structural facts from supported file types.
4. Interpret framework markers into architecture-relevant entities.
5. Normalize facts into a stable architecture IR.
6. Publish results with diagnostics and completeness metadata.
7. Support incremental re-runs where feasible.
8. Fail safely without corrupting previous platform state.

## 6. Functional Capabilities

## 6.1 Repository Acquisition

The indexer shall support acquisition of source input from:
- a local path
- a Git repository URL
- other source connectors added later

The indexer shall support selection of:
- repository identity
- branch/reference
- revision where provided

The indexer shall expose acquisition diagnostics when source retrieval fails.

## 6.2 File Inventory and Classification

The indexer shall scan acquired source trees and build a file inventory including:
- path
- size
- extension/type
- detected language
- ignore status where relevant
- candidate technology markers

The indexer shall allow unsupported files to be ignored without failing the full run.

## 6.3 Technology Detection

The indexer shall identify likely languages and technology families present in a source unit, such as:
- Java
- TypeScript/JavaScript
- SQL
- YAML
- JSON
- properties/config files

The indexer may also identify higher-level frameworks when markers are sufficient, such as:
- Spring
- Quarkus
- React
- Node service patterns

Technology detection shall be reported as metadata in the output.

## 6.4 Structural Extraction

For supported file types, the indexer shall extract structural facts such as:
- packages/modules/namespaces
- classes/interfaces/functions
- imports/dependencies
- annotations/decorators
- route declarations
- SQL statements or schema objects where supported
- configuration declarations
- startup/entry markers where supported

Extraction shall be deterministic for the same source input and extractor version.

## 6.5 Framework Interpretation

The indexer shall apply deterministic interpretation rules to structural facts in order to identify architecture-relevant concepts such as:
- API endpoints
- application services/use cases
- persistence adapters/repositories
- UI modules/components
- messaging producers/consumers
- external client integrations
- scheduled jobs
- startup/bootstrap components

Interpretation results shall retain traceability to supporting source facts where possible.

## 6.6 Architecture IR Generation

The indexer shall emit a normalized architecture IR containing at minimum:
- repositories/source metadata
- logical containers/scopes
- entities
- relationships
- source references
- diagnostics
- completeness metadata
- extractor/indexer version metadata

The IR shall be versioned.

## 6.7 Diagnostics

The indexer shall produce diagnostics for:
- acquisition failures
- parse failures
- unsupported constructs
- partially interpreted files
- conflicting or ambiguous interpretations where detectable
- publication or serialization failures

Diagnostics shall be attached to relevant scopes or files where possible.

## 6.8 Partial Results

If some files or extractors fail while others succeed, the indexer may emit a partial result if:
- the emitted result remains structurally valid
- completeness metadata is present
- diagnostics identify what was omitted or degraded

## 6.9 Incremental Reindex

The indexer should support incremental reindex behavior where feasible by reusing prior scan state or selectively reprocessing changed files.

Incremental support is an optimization and shall not change correctness expectations.

## 6.10 Publication

The indexer shall publish results in a stable, versioned format consumable by the platform.

Publication shall include:
- run outcome
- source revision metadata
- IR payload
- diagnostics
- completeness metadata

## 6.11 Extensibility

The indexer shall support extension with new:
- file classifiers
- language extractors
- interpretation rules
- entity/relationship kinds
- output fields via versioned evolution

## 7. Behavior Rules

### 7.1 Determinism
Given the same source input, extractor configuration, and indexer version, the produced IR shall be equivalent.

### 7.2 Safe degradation
Unsupported files or partially failing extractors shall not unnecessarily fail unrelated supported analysis.

### 7.3 Traceability
Higher-level architecture entities should retain references to supporting files and facts where available.

### 7.4 Versioning
IR format and extractor/indexer versions shall be emitted so the platform can reason about compatibility.

### 7.5 Isolation
Analysis of one repository or revision shall not contaminate another run.

## 8. Inputs and Outputs

### Inputs
- source location metadata
- revision/branch selection
- optional technology hints
- optional prior-state references for incremental runs
- extractor configuration

### Outputs
- run status/outcome
- versioned architecture IR
- source metadata
- diagnostics
- completeness metadata
- performance or profiling metadata where supported later

## 9. Validation and Error Handling

The indexer shall validate:
- source acquisition inputs
- IR structural validity before publication
- required metadata fields
- extractor contract assumptions internally

When errors occur, the indexer shall:
- produce diagnostics
- preserve successful extraction from unaffected scopes where possible
- avoid publishing malformed IR
- clearly distinguish fatal failure from partial success

## 10. Edge Cases

The indexer shall handle:
- empty repositories
- monorepos
- mixed-language repositories
- generated files
- vendored dependencies
- unsupported file types
- parse failures in isolated files
- duplicate symbol names across scopes
- repositories lacking build metadata
- partially cloned or inaccessible repositories

## 11. Non-Functional Considerations

The indexer should:
- be deterministic and testable
- support on-premises execution
- avoid dependence on external cloud services
- be efficient enough for small and medium repositories in MVP scope
- support fixture-based regression testing
- separate extraction from interpretation cleanly

## 12. Assumptions and Dependencies

- Tree-sitter is the primary structural parsing technology for the MVP.
- Initial extractor coverage targets Java, TypeScript/JavaScript, SQL, YAML, JSON, and properties-like configuration.
- The platform will store and present the resulting IR.
- Advanced semantic indexing beyond syntax/framework interpretation may be added later.
