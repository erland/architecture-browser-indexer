# Export format documentation

This directory documents the project export format in layers:

- `export-format-spec.md` — human-readable contract overview
- `contract-boundaries.md` — stable core vs enriched metadata guidance
- `versioning-and-compatibility.md` — compatibility expectations
- `examples/` — curated example exports
- `schema/` — JSON Schema files for the stable structural contract

## Schema layout

Step 7 splits the schema package into reusable sub-schemas so it is easier to read and maintain:

- `architecture-index-document.schema.json` — top-level document schema
- `common.schema.json` — shared definitions such as `stringMap` and `sourceReference`
- `runMetadata.schema.json`
- `source.schema.json`
- `scope.schema.json`
- `entity.schema.json`
- `relationship.schema.json`
- `diagnostic.schema.json`
- `completeness.schema.json`

The top-level schema references these files using relative `$ref` links.
