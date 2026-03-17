# Export Format Versioning and Compatibility

## Purpose

This document explains how consumers should reason about compatibility for the export format and how maintainers should evolve the format responsibly.

It complements:

- `docs/export-format/export-format-spec.md`
- `docs/export-format/contract-boundaries.md`
- `docs/export-format/export-shape-baseline.md`

The goal is to make format changes predictable for both producers and consumers.

---

## Primary Compatibility Boundary

The primary compatibility boundary is:

- `schemaVersion`

Consumers should treat `schemaVersion` as the first signal of whether they understand the payload contract.

`indexerVersion` is still useful, but it is primarily operational/diagnostic metadata:

- `schemaVersion` answers: **can I safely interpret this document shape and meaning?**
- `indexerVersion` answers: **which producer version generated this document?**

### Recommended consumer behavior

Consumers should:

1. read `schemaVersion` first
2. reject or downgrade processing when the schema version is unsupported
3. treat `indexerVersion` as informational unless a consumer has a specific producer-version workaround

---

## Compatibility Model

The export format is best understood as having two compatibility layers:

1. **stable core contract**
2. **enriched / derived metadata**

This follows the boundary defined in `contract-boundaries.md`.

### Stable core contract

Changes here have the highest compatibility impact.

This includes the broad meaning and structural role of:

- top-level document sections
- entity core fields
- relationship core fields
- scopes
- diagnostics/completeness at broad meaning level
- source-reference structure

### Enriched / derived metadata

This area is intentionally more extensible.

This includes:

- detailed `metadata` maps
- dependency views
- browser views
- summaries and classifications
- framework-specific enrichments
- optional detail flags and derived labels

Consumers may use these areas, but should assume they evolve additively more often than the stable core.

---

## What Counts as a Breaking Change

The following should be treated as breaking or potentially breaking changes unless there is an explicit migration plan.

### Top-level structure

Examples:

- removing a top-level section
- renaming a top-level section
- changing the meaning of a top-level section without versioning

### Stable entity contract

Examples:

- removing `id`, `kind`, `origin`, `name`, `displayName`, `scopeId`, or `sourceRefs`
- renaming those fields
- changing the broad meaning of those fields
- changing enum-like value semantics in a non-compatible way

### Stable relationship contract

Examples:

- removing `id`, `kind`, `fromEntityId`, `toEntityId`, or `sourceRefs`
- renaming those fields
- changing the broad meaning of relationship kinds
- changing graph linkage semantics

### Source-reference structure

Examples:

- removing `path`
- changing line-number semantics incompatibly
- changing the role of `snippet` or source-reference metadata in a way that breaks existing consumers

### Compatibility contract changes without versioning

Examples:

- making a previously required field optional without documenting it
- changing field type or shape incompatibly
- silently changing a stable contract section from one object shape to another

---

## What Usually Counts as an Additive, Non-Breaking Change

The following are usually safe, especially in enriched areas.

### Enriched metadata expansion

Examples:

- adding new keys inside metadata maps
- adding new optional summary metadata
- adding new browser-view detail fields
- adding new dependency-view detail fields
- adding new framework-specific annotations/labels/classifications

### New optional sections inside enriched areas

Examples:

- adding optional summary blocks
- adding optional derived view families
- adding optional diagnostics detail

### Additional enum-like values in extensible enriched areas

Examples:

- new dependency categories
- new browser-view families
- new framework markers

Consumers that use enriched metadata should be written to ignore unknown optional keys and tolerate additive extensions.

---

## Consumer Coupling Guidance

### Strong coupling is appropriate for

- `schemaVersion`
- top-level document identity
- core entity and relationship fields
- documented stable object families
- explicitly documented stable enums and meanings

### Cautious coupling is appropriate for

- detailed entity metadata
- detailed relationship metadata
- dependency-view entry details
- browser-view descriptors
- summaries and classifications

### Risky coupling patterns

Consumers should avoid depending on:

- exact ordering of extensible lists unless documented
- exact equality of metadata maps
- exact presence/absence of optional derived flags
- exact formatting of display-oriented strings unless documented
- undocumented browser/dependency-view detail keys as if they were rigid contracts

---

## Recommended Versioning Rules for Maintainers

### When to update `schemaVersion`

A new schema version should be considered when:

- the stable core contract changes incompatibly
- required fields are removed or renamed
- field types or major object shapes change incompatibly
- top-level section meaning changes materially

### When schema version may remain the same

The schema version can usually remain unchanged when:

- new optional enriched metadata is added
- new optional derived view fields are added
- examples/docs are clarified without structural change
- compatibility-neutral documentation is improved

### Documentation and test expectations for changes

Whenever the export format changes, maintainers should update:

1. the markdown spec
2. the compatibility note if the coupling boundary changes
3. curated examples if they no longer represent real output
4. schema files once introduced in later steps
5. contract tests that validate the format

---

## Compatibility Examples

### Example: additive enriched metadata change

A new browser-view descriptor field is added under document metadata.

Impact:

- usually non-breaking
- consumers should ignore unknown optional enriched fields
- schema/docs/examples should be updated

### Example: stable field rename

`fromEntityId` is renamed to `sourceId`.

Impact:

- breaking
- requires schema version change and migration handling

### Example: new dependency-view family

A new dependency-view family appears under `metadata.dependencyViews`.

Impact:

- usually additive if existing families remain stable
- should be documented as enriched/derived expansion

### Example: top-level section removed

`relationships` is removed or moved elsewhere.

Impact:

- breaking
- requires schema version change and coordinated downstream migration

---

## Guidance for Downstream Consumers

Consumers that want maximum resilience should:

- rely first on the stable core contract
- use enriched metadata opportunistically rather than rigidly
- ignore unknown optional metadata keys
- validate `schemaVersion`
- prefer tolerant parsing over exact-map matching in enriched areas

Consumers that need stricter guarantees for enriched areas should:

- explicitly pin supported schema versions
- document which enriched areas they rely on
- validate those areas with their own additional checks

---

## Current Position at Step 4

At this stage of the export documentation package:

- the compatibility boundary is defined conceptually
- the markdown spec exists
- curated examples are still minimal
- JSON Schema has not yet been introduced

That means compatibility is currently documented primarily through:

- prose
- contract boundaries
- real fixture examples

Later steps will strengthen this with schema files and automated validation.


## Export-format contributor checklist

When you change the export format:

- update the human-readable spec in `export-format-spec.md`
- update `contract-boundaries.md` if consumer coupling expectations changed
- update schema files if the stable structural contract changed
- update curated examples if representative output changed
- update contract tests in the same change
- change `schemaVersion` when the stable core contract changes in a way consumers must react to

Avoid silently promoting enriched metadata into stable contract. If you expect downstream consumers to rely on a formerly enriched field, document that change explicitly and treat it as a contract decision, not a quiet implementation detail.
