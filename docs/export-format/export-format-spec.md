# Export Format Specification

## Purpose

The project exports an **architecture index document** as JSON. The export is intended to support:

- downstream architecture browsers
- offline analysis and diagnostics
- dependency and browser-view exploration
- integration with validation, regression, or transformation tooling

This specification is the **human-readable entry point** for understanding the format. It is intentionally descriptive and consumer-oriented. It should be read together with:

- `docs/export-format/export-shape-baseline.md`
- `docs/export-format/contract-boundaries.md`
- curated example files under `docs/export-format/examples/`

At this stage, the goal is to describe the current contract clearly rather than to redefine it.

---

## Document Overview

The export is a single JSON object representing one indexing run.

Current top-level shape:

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

Broadly:

- `runMetadata` describes the indexing run
- `source` describes the input repository/source acquisition
- `scopes` describe hierarchical structural containers
- `entities` describe architecture-relevant things discovered or inferred
- `relationships` connect entities into a graph
- `diagnostics` reports issues or limitations found during processing
- `completeness` summarizes whether the export is complete or partial
- `metadata` is an extensible area for derived views, summaries, and enrichment

---

## Stable Core vs Enriched Metadata

The export contains two layers:

### Stable core contract
These are the parts consumers should prefer to rely on first:

- top-level document structure
- `scopes`
- `entities`
- `relationships`
- `diagnostics`
- `completeness`
- broad meaning of core record fields such as ids, kinds, names, scope membership, and source traceability

### Enriched / derived metadata
These are useful but more extensible:

- detailed keys inside `metadata` maps
- dependency views
- browser views
- summaries and classifications
- framework-specific enrichments
- optional analysis-oriented derived fields

Consumers should use enriched metadata when needed, but should avoid assuming that every optional derived key is a long-term rigid contract.

---

## Core Concepts

## Entity

An **entity** is a discovered or inferred architecture-relevant node in the graph.

Examples include:

- modules
- classes
- interfaces
- functions
- endpoints
- routes
- components
- providers
- repositories
- external or inferred types

Entities are listed in the `entities` array.

## Relationship

A **relationship** connects two entities.

Examples include:

- depends on
- exposes
- contains
- extends
- implements
- references
- framework-specific relationships

Relationships are listed in the `relationships` array.

## Scope

A **scope** groups entities structurally.

Examples include:

- repository
- package
- file
- module-related scope families

Scopes are listed in the `scopes` array and may form a parent/child hierarchy.

## Origin

`origin` indicates whether a record was directly observed or inferred.

Typical meanings:

- `OBSERVED` — directly extracted from source/parse structures
- `INFERRED` — synthesized or resolved from evidence rather than directly declared as a first-class source node

## Source reference

A **source reference** links an exported record back to source text:

- file path
- line range
- snippet
- extensible metadata

These references are used inside scope, entity, and relationship records.

## Dependency view

A **dependency view** is a derived representation of relationships grouped or normalized for analysis, such as type, package, module, or evidence-oriented dependency structures.

Dependency views are not part of the simplest graph core, but are important enriched output for consumers that need summarized dependency analysis.

## Browser view

A **browser view** is a derived representation intended to support architecture browsing experiences, especially frontend or Java-oriented navigation and filtered architecture views.

---

## Top-Level Sections

## `schemaVersion`

Type: string

Purpose:
- identifies the export schema family/version expected by consumers

Consumer guidance:
- treat this as the first compatibility gate
- use it to decide whether a consumer/parser understands the payload

Example:
```json
"schemaVersion": "1.2.0"
```

## `indexerVersion`

Type: string

Purpose:
- identifies the producing tool version

Consumer guidance:
- useful for diagnostics and troubleshooting
- should not normally be the main compatibility boundary; `schemaVersion` matters more

## `runMetadata`

Type: object

Purpose:
- describes the indexing run itself

Observed fields:
- `startedAt`
- `completedAt`
- `outcome`
- `detectedTechnologies`
- `metadata`

Typical meanings:
- start/end timestamps for the run
- broad run outcome such as success or partial result
- technologies detected during analysis
- extensible run-level metadata

Example:
```json
"runMetadata": {
  "startedAt": "2026-03-10T12:00:00Z",
  "completedAt": "2026-03-10T12:00:01Z",
  "outcome": "SUCCESS",
  "detectedTechnologies": ["java"],
  "metadata": {}
}
```

## `source`

Type: object

Purpose:
- describes what repository or source input was analyzed

Observed fields:
- `repositoryId`
- `acquisitionType`
- `path`
- `remoteUrl`
- `branch`
- `revision`
- `acquiredAt`
- `metadata`

Consumer guidance:
- use this section for provenance and traceability, not as the main graph structure

## `scopes`

Type: array of scope objects

Purpose:
- describes structural containment/grouping

Stable scope fields:
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

Consumer guidance:
- use scopes for grouping and hierarchy, not as a substitute for graph relationships
- `parentScopeId` defines scope hierarchy

## `entities`

Type: array of entity objects

Purpose:
- the main node set of the architecture graph

Stable entity fields:
- `id`
- `kind`
- `origin`
- `name`
- `displayName`
- `scopeId`
- `sourceRefs`
- `metadata`

Examples of meanings:
- `id` — stable identifier within the document
- `kind` — broad entity class such as CLASS, ENDPOINT, ROUTE, etc.
- `origin` — observed vs inferred
- `name` — logical name
- `displayName` — consumer-friendly display label
- `scopeId` — structural container
- `sourceRefs` — traceability back to source
- `metadata` — extensible entity enrichment

## `relationships`

Type: array of relationship objects

Purpose:
- the main edge set of the architecture graph

Stable relationship fields:
- `id`
- `kind`
- `fromEntityId`
- `toEntityId`
- `label`
- `sourceRefs`
- `metadata`

Examples of meanings:
- `kind` — broad relationship family
- `fromEntityId` / `toEntityId` — graph linkage
- `label` — readable edge description when available
- `metadata` — extensible relationship enrichment

## `diagnostics`

Type: array

Purpose:
- exporter/indexer-reported problems, warnings, or degradation details

Consumer guidance:
- presence of diagnostics does not necessarily mean the document is unusable
- diagnostics should be interpreted together with `completeness`

## `completeness`

Type: object

Purpose:
- summarizes whether the export is complete or partial

Consumer guidance:
- use this to decide whether downstream processing should trust the result fully, partially, or treat it as degraded

## `metadata`

Type: object

Purpose:
- document-level enriched metadata and derived views

Important note:
- the metadata container is part of the stable top-level shape
- the exact keys within it are more extensible

Known current enriched areas include:
- dependency views
- browser-view structures
- summaries/classifications

---

## Scope Model

A scope object currently has this broad shape:

- `id`
- `kind`
- `name`
- `displayName`
- `parentScopeId`
- `sourceRefs`
- `metadata`

### Semantic meaning

- `id` uniquely identifies the scope in the document
- `kind` identifies the scope family
- `parentScopeId` allows hierarchical nesting
- `sourceRefs` ties the scope back to relevant source text

### Example

```json
{
  "id": "scope:package:com.example",
  "kind": "PACKAGE",
  "name": "com.example",
  "displayName": "example",
  "parentScopeId": "scope:repo",
  "sourceRefs": [
    {
      "path": "src/main/java/com/example/DemoController.java",
      "startLine": 1,
      "endLine": 20,
      "snippet": "package com.example;",
      "metadata": {}
    }
  ],
  "metadata": {
    "language": "java"
  }
}
```

---

## Entity Model

An entity object currently has this broad shape:

- `id`
- `kind`
- `origin`
- `name`
- `displayName`
- `scopeId`
- `sourceRefs`
- `metadata`

### Required semantic meaning

Consumers should treat these as the primary architecture-node contract:

- `id` — document-unique node identity
- `kind` — broad entity category
- `origin` — observed vs inferred
- `name` / `displayName` — logical and display-oriented naming. For many observed declarations, both are short names; fully qualified canonical names commonly live in `metadata.qualifiedName`. Some interpreted entities such as endpoints use `name` for the stable canonical label and `displayName` for a richer UI-oriented label.
- `scopeId` — where the entity belongs structurally

### Entity metadata

`metadata` often includes useful information such as:

- language
- framework markers
- HTTP method/path details
- declaration kind details
- inferred classification hints

These fields are valuable, but many are enriched/derived rather than the narrowest stable core.

### Example: class entity

```json
{
  "id": "entity:class:demo-controller",
  "kind": "CLASS",
  "origin": "OBSERVED",
  "name": "DemoController",
  "displayName": "DemoController",
  "scopeId": "scope:package:com.example",
  "sourceRefs": [
    {
      "path": "src/main/java/com/example/DemoController.java",
      "startLine": 3,
      "endLine": 18,
      "snippet": "public class DemoController",
      "metadata": {}
    }
  ],
  "metadata": {
    "language": "java"
  }
}
```

### Example: inferred endpoint entity

```json
{
  "id": "entity:endpoint:demo",
  "kind": "ENDPOINT",
  "origin": "INFERRED",
  "name": "GET /demo",
  "displayName": "DemoController endpoint GET /demo",
  "scopeId": "scope:package:com.example",
  "sourceRefs": [
    {
      "path": "src/main/java/com/example/DemoController.java",
      "startLine": 8,
      "endLine": 12,
      "snippet": "@GetMapping(\"/demo\")",
      "metadata": {}
    }
  ],
  "metadata": {
    "httpMethod": "GET",
    "path": "/demo"
  }
}
```

---

## Relationship Model

A relationship object currently has this broad shape:

- `id`
- `kind`
- `fromEntityId`
- `toEntityId`
- `label`
- `sourceRefs`
- `metadata`

### Required semantic meaning

Consumers should treat these as the primary graph-edge contract:

- `id` — edge identity
- `kind` — broad relationship type
- `fromEntityId` / `toEntityId` — the directed connection
- `sourceRefs` — traceability evidence

### Relationship metadata

Relationship metadata may contain:

- dependency-source details
- evidence labels
- framework relationship hints
- category or view-related enrichments

These are useful but more extensible than the core edge shape.

### Example

```json
{
  "id": "rel:controller:exposes:endpoint",
  "kind": "EXPOSES",
  "fromEntityId": "entity:class:demo-controller",
  "toEntityId": "entity:endpoint:demo",
  "label": "controller exposes endpoint",
  "sourceRefs": [
    {
      "path": "src/main/java/com/example/DemoController.java",
      "startLine": 8,
      "endLine": 12,
      "snippet": "@GetMapping(\"/demo\")",
      "metadata": {}
    }
  ],
  "metadata": {}
}
```

---

## Source Reference Model

A source reference object currently has this broad shape:

- `path`
- `startLine`
- `endLine`
- `snippet`
- `metadata`

### Purpose

Source references provide traceability from exported scopes/entities/relationships back to source text.

### Consumer guidance

- use `path` + line range for navigation
- treat `snippet` as helpful display/debug context
- treat `metadata` as extensible

---

## Diagnostics and Completeness

## Diagnostics

Diagnostics communicate exporter/indexer issues, warnings, or degraded conditions.

Consumers should interpret diagnostics as:
- additional context about the quality or completeness of the run
- not necessarily fatal corruption of the payload

## Completeness

Completeness summarizes whether the document is complete or partial.

Typical consumer use:
- show a warning banner in UIs
- reduce trust in derived views if the run is partial
- keep the document usable while making the degradation explicit

Example broad interpretation:
- `SUCCESS` + complete status → normal trust level
- `PARTIAL` + degraded completeness → usable but incomplete document

---

## Dependency Views

Dependency views are enriched derived structures that summarize or group relationships for analysis.

Known current families include concepts such as:
- type dependencies
- package dependencies
- module dependencies
- evidence-oriented dependency views

### Purpose

They allow consumers to work with a more analysis-friendly representation than the raw edge list alone.

### Consumer guidance

Consumers should:
- use dependency views when they need summarized architectural dependency analysis
- not assume that every derived metadata key inside a dependency view entry is part of the narrowest stable core unless documented as such

### Current documentation status

Step 1 established that dependency views are present as an important enriched area. Later steps will document their exact structural shape more exhaustively and pair them with schema/examples.

---

## Browser Views

Browser views are enriched structures intended to support architecture browsing experiences.

Examples of broad uses:
- frontend-oriented architecture browsing
- Java-oriented browser views
- filtered architecture perspectives

### Purpose

They provide curated or categorized navigation surfaces beyond raw graph traversal.

### Consumer guidance

Consumers should:
- rely on the existence and broad meaning of browser-view structures only where documented
- avoid coupling too tightly to every optional descriptor key until schema/examples are added

---

## Summary and Classification Metadata

The export may contain summary-oriented metadata and classification outputs at document, entity, relationship, or view level.

Examples include broad categories such as:
- framework classification
- profile classification
- architecture view labels
- browser availability hints

These are valuable for UX and analysis, but they belong primarily to the enriched layer unless explicitly promoted into the stable core.

---

## Known Extension Points

The format intentionally allows extensibility in:

- `runMetadata.metadata`
- `source.metadata`
- `scope.metadata`
- `entity.metadata`
- `relationship.metadata`
- document-level `metadata`
- diagnostics details

Consumers should therefore:
- tolerate additive metadata keys
- avoid rejecting unknown metadata fields
- prefer stable documented fields for strict integrations

---

## Consumer Guidance

### Safer coupling patterns

Prefer:
- top-level section presence checks
- entity/relationship graph traversal using ids and kinds
- scope hierarchy through `parentScopeId`
- broad completeness/diagnostic interpretation
- documented dependency/browser-view families once examples/schema are in place

### Riskier coupling patterns

Avoid over-relying on:
- exact shape of optional metadata maps
- every individual derived classification key
- incidental ordering unless explicitly documented
- enriched browser/dependency metadata keys that are not yet promoted into stable contract language

---

## Example Map

Current example files:

- `docs/export-format/examples/minimal-success.json`
  - smallest representative successful payload
- `docs/export-format/examples/partial-result.json`
  - representative partial/degraded payload

Planned later examples:
- Java-backend richer export
- frontend richer export
- mixed full export with dependency/browser-view enrichment

---

## Current Status of the Specification

This specification documents the current export in human-readable form and establishes the main concepts and boundaries.

Follow-on steps will add:
- versioning/compatibility guidance
- machine-readable JSON Schema
- more curated examples for dependency and browser-view-rich documents
- automated validation to keep examples/docs aligned with real output
