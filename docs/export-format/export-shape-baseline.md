# Export Shape Baseline

## Purpose

This document baselines the **current export shape actually produced and validated by the project** before schema and full prose documentation are added.

It is intentionally descriptive rather than normative. The source of truth remains the current code and the checked-in example payloads copied from test fixtures:

- `docs/export-format/examples/minimal-success.json`
- `docs/export-format/examples/partial-result.json`

These examples come from existing IR/export fixtures under `src/test/resources/fixtures/ir/` and are treated as the starting point for export-format documentation work.

## Representative real outputs collected in Step 1

### 1. Minimal successful payload
File:
- `docs/export-format/examples/minimal-success.json`

What it demonstrates:
- top-level document structure
- repository/package scopes
- basic entity and relationship records
- completeness metadata in a complete run
- empty document metadata

### 2. Partial-result payload
File:
- `docs/export-format/examples/partial-result.json`

What it demonstrates:
- the same top-level structure as the minimal payload
- diagnostics in a partial run
- completeness metadata for degraded output
- partial-result handling in `runMetadata.outcome` and `completeness.status`

## Current top-level document shape

The export payload is currently represented by `ArchitectureIndexDocument` and has these top-level fields:

1. `schemaVersion`
2. `indexerVersion`
3. `runMetadata`
4. `source`
5. `scopes`
6. `entities`
7. `relationships`
8. `diagnostics`
9. `completeness`
10. `metadata`

Source:
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/ir/model/ArchitectureIndexDocument.java`

## Top-level section inventory

### `schemaVersion`
String.

Purpose:
- identifies the export schema family/version expected by consumers

Observed in examples:
- `1.2.0`

### `indexerVersion`
String.

Purpose:
- identifies the producer version

Observed in examples:
- `0.1.0-SNAPSHOT`

### `runMetadata`
Object.

Current observed fields from examples:
- `startedAt`
- `completedAt`
- `outcome`
- `detectedTechnologies`
- `metadata`

Observed outcomes:
- `SUCCESS`
- `PARTIAL`

### `source`
Object describing the repository/input source.

Current observed fields from examples:
- `repositoryId`
- `acquisitionType`
- `path`
- `remoteUrl`
- `branch`
- `revision`
- `acquiredAt`
- `metadata`

### `scopes`
Array of scope objects.

Current observed scope shape:
- `id`
- `kind`
- `name`
- `displayName`
- `parentScopeId`
- `sourceRefs`
- `metadata`

Observed scope kinds in examples:
- `REPOSITORY`
- `PACKAGE`

### `entities`
Array of entity objects.

Current observed entity shape:
- `id`
- `kind`
- `origin`
- `name`
- `displayName`
- `scopeId`
- `sourceRefs`
- `metadata`

Observed entity kinds in examples:
- `CLASS`
- `ENDPOINT`

Observed entity origins in examples:
- `OBSERVED`
- `INFERRED`

### `relationships`
Array of relationship objects.

Current observed relationship shape:
- `id`
- `kind`
- `fromEntityId`
- `toEntityId`
- `label`
- `sourceRefs`
- `metadata`

Observed relationship kinds in examples:
- `EXPOSES`

### `diagnostics`
Array of diagnostic objects.

Current observed diagnostic shape:
- `id`
- `severity`
- `phase`
- `code`
- `message`
- `fatal`
- `filePath`
- `scopeId`
- `entityId`
- `sourceRefs`
- `metadata`

Observed in examples:
- empty array for successful runs
- populated array for partial runs

### `completeness`
Object describing how complete the export is.

Current observed fields:
- `status`
- `indexedFileCount`
- `totalFileCount`
- `degradedFileCount`
- `omittedPaths`
- `notes`

Observed statuses:
- `COMPLETE`
- `PARTIAL`

### `metadata`
Object for document-level metadata and derived summaries.

Observed in example fixtures:
- empty object

Current code indicates that richer real outputs may populate this field with:
- `inventoryEntries`
- `inventorySummary`
- `parseSummary`
- `extractionSummary`
- `interpretationSummary`
- `topologySummary`
- `dependencyViews`
- `diagnosticSummary`
- `partialResult`

Source:
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/ir/ArchitectureIrDocumentMetadataBuilder.java`

## Repeated object families observed in the export

### Source reference object
Appears inside scopes, entities, relationships, and diagnostics.

Observed shape:
- `path`
- `startLine`
- `endLine`
- `snippet`
- `metadata`

### Metadata map
Appears on most top-level and nested objects.

Observed shape:
- arbitrary object/map

Current understanding:
- metadata is a major extensibility mechanism and should be treated as an area requiring careful stable-vs-enriched classification in the next documentation step.

## Important object families implied by the current code but not yet represented in Step 1 examples

The current examples are intentionally small. The code indicates richer exports may also contain:

- dependency-view entries under `metadata.dependencyViews`
- browser-view availability and descriptors
- package metrics and boundary summaries
- extraction/interpretation/topology summaries
- richer entity/relationship kinds beyond the small fixture set

These richer areas should be captured in the next example-expansion/documentation steps.

## Initial documentation priorities derived from the baseline

Based on the current shape, the most important documentation targets after Step 1 are:

1. top-level document contract
2. entity/relationship/scopes object families
3. source reference object family
4. completeness and diagnostics semantics
5. document-level metadata, especially `dependencyViews`
6. stable core vs enriched metadata boundary

## Notes about current confidence

High confidence from real checked-in fixtures:
- top-level document structure
- scope/entity/relationship/diagnostic/completeness object families
- successful vs partial result shape

Moderate confidence from production code inspection but not yet covered by checked-in examples in this step:
- full `metadata.dependencyViews` structure
- browser-view structures
- richer summary payloads in document metadata

## Step 1 completion summary

This step now provides:
- a baseline note for the current export shape
- two real payload examples copied from existing fixtures
- an inventory of the most important object families and known richer areas to document next
