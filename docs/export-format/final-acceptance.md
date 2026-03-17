# Export Format Documentation Package — Final Acceptance

## Scope of this acceptance pass

This acceptance pass closes the first export-format documentation wave. The goal of the wave was to make the current export format:

- understandable to human readers
- structurally documented for downstream consumers
- supported by curated examples
- protected against obvious documentation drift through automated checks

This is a documentation and contract-package acceptance pass. It is **not** a claim that the export format is frozen forever or that every enriched metadata family is exhaustively documented.

## Implemented package contents

The package now includes:

### Human-readable documentation
- `export-shape-baseline.md`
- `contract-boundaries.md`
- `export-format-spec.md`
- `versioning-and-compatibility.md`
- `README.md`

### Curated examples
- `examples/minimal-export.json`
- `examples/java-backend-export.json`
- `examples/frontend-export.json`
- `examples/mixed-full-export.json`

### Machine-readable schema
- `schema/architecture-index-document.schema.json`
- reusable sub-schemas for the main stable-core record families

### Automated contract checks
- curated examples copied under `src/test/resources/export-contract/`
- `ExportFormatSchemaExamplesContractTest`

## Acceptance conclusions

### 1. Human readability
Accepted.

A reader can now start from `docs/export-format/README.md`, move to `export-format-spec.md`, and understand:

- what the export is for
- what the top-level sections are
- how entities, relationships, scopes, diagnostics, and metadata fit together
- how dependency views and browser-view style metadata should be interpreted at a high level

### 2. Stable core vs enriched metadata boundaries
Accepted.

The package now explicitly distinguishes:

- **stable core contract**
- **enriched / derived metadata**

This reduces the risk that consumers couple to incidental metadata that was never intended to be a hard contract.

### 3. Compatibility guidance
Accepted.

The package now documents:

- how `schemaVersion` should be interpreted
- what typically counts as a breaking change
- what is generally additive and non-breaking
- maintainer responsibilities when the export evolves

### 4. Curated examples
Accepted.

The example set now covers:

- minimal export
- Java-backend-oriented export
- frontend-oriented export
- richer mixed export with dependency/browser-view style metadata

These examples are intended to improve reader understanding, not replace the real project outputs as the source of truth.

### 5. Machine-readable structural contract
Accepted.

The stable core export structure now has a top-level JSON Schema plus reusable sub-schemas for the main core object families.

The schema intentionally stays more permissive in enriched metadata areas so that extensibility is not accidentally broken by over-constraining the wrong parts of the export.

### 6. Drift protection
Accepted with limits.

The examples and schema are now tied to automated checks through:

- required-field checks derived from the schema
- deserialization into `ArchitectureIndexDocument`
- IR validation through the existing validator
- checks against checked-in real IR fixtures

This is a meaningful safeguard against obvious drift in the stable core contract, though it is not a full external JSON Schema engine validation pass.

## Current limits / known follow-up areas

The package is now useful and sustainable, but not exhaustive.

Areas still suitable for future refinement include:

- richer documentation of dependency-view metadata families
- richer documentation of browser-view descriptors and availability metadata
- more example outputs grounded in generated real project fixtures with denser metadata
- optional future full JSON Schema validation using a dedicated schema engine if the project wants stronger machine validation

## Practical maintenance expectations

When the export evolves, maintainers should update in the same change:

- the human-readable spec
- contract boundary guidance
- compatibility/versioning notes
- schema files
- curated examples
- contract tests

## Acceptance summary

This wave is accepted as complete for its intended scope.

The project now has an export-format documentation package that is:

- readable
- structured
- example-backed
- machine-checkable at the stable-core level
- maintainable enough to evolve with the export over time
