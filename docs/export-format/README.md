# Export format documentation package

This package explains the JSON export produced by the indexer and is intended for both:

- people trying to understand the format
- tools or downstream consumers that want to validate or parse it

## Where to start

Read the files in this order:

1. `export-format-spec.md`
   - the main human-readable explanation of the format
   - start here if you want to understand what the document contains and how the main sections relate

2. `contract-boundaries.md`
   - explains which parts of the export are the stable core contract
   - explains which parts are enriched or derived metadata and should be treated more cautiously

3. `examples/`
   - small curated JSON examples showing common export shapes
   - the fastest way to get an intuition for what normal output looks like

4. `versioning-and-compatibility.md`
   - explains how compatibility should be understood
   - use this if you are building tooling that depends on the export format

5. `schema/`
   - machine-readable JSON Schema files for the stable structural contract
   - use these for validation, editor support, and downstream integration checks

## What each part is for

### Human-readable spec
- `export-format-spec.md`

Use this when you want to know:
- what the top-level sections mean
- how entities, relationships, scopes, diagnostics, and metadata fit together
- which concepts are core to the export model

### Contract boundary guidance
- `contract-boundaries.md`

Use this when you want to know:
- which fields are safe to rely on strongly
- which fields are enriched and may grow over time
- how to avoid coupling too tightly to incidental metadata

### Compatibility guidance
- `versioning-and-compatibility.md`

Use this when you want to know:
- what counts as a breaking change
- what kinds of changes are additive
- how `schemaVersion` should be interpreted

### Curated examples
- `examples/minimal-export.json`
- `examples/java-backend-export.json`
- `examples/frontend-export.json`
- `examples/mixed-full-export.json`

Use these when you want:
- a minimal valid example
- a Java-heavy example
- a frontend-heavy example
- a richer mixed example with dependency and browser-view style metadata

### JSON Schema
- `schema/architecture-index-document.schema.json`
- supporting sub-schemas under `schema/`

Use these when you want:
- structural validation
- a machine-readable contract for the stable core format
- better editor/autocomplete support in downstream tooling

## Schema layout

The schema package is split into reusable files so it is easier to read and maintain:

- `architecture-index-document.schema.json` — top-level document schema
- `common.schema.json` — shared reusable definitions
- `runMetadata.schema.json`
- `source.schema.json`
- `scope.schema.json`
- `entity.schema.json`
- `relationship.schema.json`
- `diagnostic.schema.json`
- `completeness.schema.json`

The top-level schema references these files using relative `$ref` links.

## How to use this package as a consumer

### If you are reading the export for the first time
Start with:
- `export-format-spec.md`
- then `examples/`

### If you are writing code against the format
Start with:
- `contract-boundaries.md`
- `versioning-and-compatibility.md`
- `schema/architecture-index-document.schema.json`

### If you are debugging an export sample
Compare:
- the real JSON output
- the closest file in `examples/`
- the relevant section in `export-format-spec.md`

## Maintenance expectations

When the export format changes, this package should be updated together:

- update the human-readable spec if the meaning or shape changes
- update contract-boundary guidance if coupling expectations change
- update curated examples if representative output changes
- update schema files if the stable structural contract changes
- keep tests aligned so examples and schema do not drift from real output


## Contributor guidance for evolving the export safely

When changing the export format, update the documentation package and contract checks in the same change.

### Maintainer checklist

- update `export-format-spec.md` when the meaning or layout of the export changes
- update `contract-boundaries.md` if a field moves between stable core and enriched metadata
- update `versioning-and-compatibility.md` when compatibility expectations change
- update the JSON Schema files when the stable structural contract changes
- update curated examples when representative output changes
- update contract tests in the same change so examples, schema, and real output stay aligned

### When to change `schemaVersion`

Change `schemaVersion` when consumers that rely on the stable core contract would need to change how they interpret the payload. Typical reasons include:

- removing or renaming a stable field
- changing the type or meaning of a stable field
- changing required top-level structure
- changing the meaning of a stable enum/value family in a breaking way

You usually do **not** need to change `schemaVersion` for:

- additive enriched metadata
- additive optional fields in explicitly extensible metadata maps
- new dependency/browser-view metadata that stays in the enriched layer

### Important caution

Do not silently promote enriched metadata into the stable contract. If a formerly optional/enriched field becomes something downstream consumers are expected to rely on, document that promotion explicitly in:

- `contract-boundaries.md`
- `export-format-spec.md`
- `versioning-and-compatibility.md`
- the schema files and contract tests, if the stable structure changed


## Name vs displayName note

The current code does **not** treat `name` as a universally fully qualified canonical name with `displayName` as a universally short name. In current emitters, many observed declarations use short names for both `name` and `displayName`, while canonical fully qualified values often live in metadata such as `metadata.qualifiedName`. Some interpreted entities, especially endpoints, use `name` for the stable canonical label (for example `GET /orders`) and `displayName` for a richer UI-oriented label (for example `OrderResource endpoint GET /orders`).


## Step 2 contract note

- `entity-roles-traits-contract.md`
- `relationship-semantics-contract.md`
- `java-first-role-mapping.md` describe the first normalized contract additions.
- `java-first-relationship-semantics.md` documents the first Java-first relationship semantic mappings.

- `internal-normalization-seam.md` — internal seam for mapping framework evidence into canonical architecture semantics
