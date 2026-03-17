# Export Contract Boundaries: Stable Core vs Enriched Metadata

## Purpose

This note defines the intended coupling boundary for consumers of the export format.

The project’s export is intentionally rich: it contains a **core structural architecture contract** and a broader set of **derived, enriched, and analysis-oriented metadata**. Consumers should not treat all fields as equally stable.

This document divides the format into two layers:

1. **Stable core contract**
2. **Enriched / derived metadata**

This distinction should guide:
- downstream consumer implementations
- schema design
- documentation wording
- test assertions
- future evolution of the export format

---

## Layer 1 — Stable Core Contract

These fields and sections are the parts of the export that downstream consumers should be encouraged to rely on first.

### Top-level stable contract

Consumers may rely on the presence and basic meaning of these top-level sections when they are produced by the exporter:

- `metadata`
- `scopes`
- `entities`
- `relationships`
- `diagnostics`
- `completeness`

### Stable entity contract

For each entity object, the following are part of the stable core shape:

- `id`
- `kind`
- `origin`
- `name`
- `displayName`
- `scopeId`
- `sourceRefs`
- `metadata` (container exists, but not every metadata key is part of the stable core)

Consumers may generally rely on:
- entity identity through `id`
- broad entity classification through `kind`
- source of truth / inference status through `origin`
- display-oriented lookup through `name` and `displayName`
- structural grouping through `scopeId`
- source traceability through `sourceRefs`

### Stable relationship contract

For each relationship object, the following are part of the stable core shape:

- `id`
- `kind`
- `fromEntityId`
- `toEntityId`
- `sourceRefs`
- `metadata` (container exists, but not every metadata key is part of the stable core)

Consumers may generally rely on:
- relationship identity through `id`
- broad relationship classification through `kind`
- graph linkage through `fromEntityId` and `toEntityId`
- source traceability through `sourceRefs`

### Stable diagnostics/completeness contract

Consumers may rely on the broad meaning of:
- `diagnostics` as exporter-reported problems, warnings, or notices
- `completeness` as a summary of whether the output is partial, complete, or otherwise limited

### Stable source-reference contract

Within `sourceRefs`, consumers may generally rely on the meaning of:
- `path`
- `startLine`
- `endLine`
- `snippet`
- `metadata` as an extensible source-reference metadata map

---

## Layer 2 — Enriched / Derived Metadata

These fields are still valuable and often intentionally exposed, but consumers should treat them as more extensible and more likely to evolve additively.

This includes:
- analysis-oriented summaries
- browser-view descriptors
- dependency-view entries
- profile/classification metadata
- framework-specific enrichment
- optional diagnostic/detail metadata

### Current enriched areas explicitly identified in Step 1 baseline

The Step 1 baseline already identified these richer areas as present in fixtures, code, or known output paths:

- `metadata.dependencyViews`
- browser-view structures
- summary metadata

These areas are **important** but should currently be treated as enriched/derived contract surfaces unless explicitly promoted into the stable core in later documentation steps.

### Entity metadata

The `entity.metadata` object is intentionally useful, but many keys inside it are derived from extraction, interpretation, topology inference, or IR enrichment.

Examples of likely enriched metadata categories include:
- framework markers
- package/classification hints
- declaration details
- endpoint semantics
- JPA/CDI/JAX-RS derived flags
- browser-oriented descriptors

Rule of thumb:
- rely on the **existence of `metadata` as an extension container**
- do not assume every metadata key is a long-term strict contract unless explicitly documented as such

### Relationship metadata

The `relationship.metadata` object is also an extension-heavy area.

Examples of likely enriched relationship metadata categories include:
- dependency reasons
- framework relationship labels
- evidence labels and sources
- dependency categories
- view-specific hints
- internal/external/boundary classification

Rule of thumb:
- rely on `kind`, `fromEntityId`, `toEntityId` first
- treat metadata keys as enriched unless specifically documented as stable

### Dependency views and browser views

These are highly valuable and should absolutely be documented, but they should initially be described as:
- **stable in broad purpose and section identity**
- **extensible in detailed metadata shape**

That means consumers may rely on:
- the conceptual purpose of dependency views and browser views
- their broad identity and use cases

But consumers should be cautious about depending on:
- every optional detail field
- exact list composition/order
- every derived classification flag

---

## Consumer Guidance

### Safe coupling patterns

Prefer depending on:
- documented top-level sections
- entity and relationship identity and linkage
- stable enums and object families
- documented core fields
- explicitly documented view families

### Risky coupling patterns

Avoid depending too aggressively on:
- exact ordering of lists unless documented
- exact equality of metadata maps
- optional derived flags that are not explicitly documented as stable
- incidental formatting of display-oriented values
- exact browser/dependency-view detail fields unless the spec later promotes them to stable contract

---

## Documentation Rules Going Forward

When documenting the export format in later steps:

1. The markdown spec should explicitly label whether a section or field belongs to:
   - stable core contract
   - enriched / derived metadata
2. JSON Schema should strongly define the stable structural contract first.
3. Extensible metadata maps should not be over-constrained prematurely.
4. Examples should demonstrate both:
   - minimal/core usage
   - richer/enriched usage

---

## Change Management Guidance

### Usually additive, non-breaking changes

These should normally be treated as additive:
- adding new metadata keys in enriched areas
- adding new optional summary/detail fields
- adding new browser/dependency-view detail fields
- expanding diagnostic detail metadata

### Potentially breaking changes

These should be treated much more carefully:
- removing or renaming top-level sections
- removing or renaming stable entity/relationship identity fields
- changing the semantics of `kind`, `origin`, `scopeId`, `fromEntityId`, `toEntityId`
- changing the broad meaning of dependency-view/browser-view sections after they are documented

---

## Step 2 Output Summary

After Step 2, the intended coupling boundary is:

### Stable core
- top-level document structure
- entities
- relationships
- scopes
- diagnostics/completeness at broad meaning level
- source references

### Enriched / derived
- detailed metadata maps
- dependency views (especially detail fields)
- browser views (especially detail fields)
- summaries/classifications/framework-specific enrichments

This boundary can be refined in later steps, but it gives consumers and maintainers a clear starting point now.
