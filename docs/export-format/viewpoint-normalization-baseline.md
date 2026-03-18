# Viewpoint Normalization Baseline

This note captures the **Step 1 baseline contract-evolution decisions** for adding canonical
architecture semantics to the export format later without losing control of compatibility.

## Current export entry points

### Schema/version entry points
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/ir/ArchitectureIrVersions.java`
- `docs/export-format/schema/architecture-index-document.schema.json`
- `docs/export-format/schema/entity.schema.json`
- `docs/export-format/schema/relationship.schema.json`

### Export contract records
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/ir/model/ArchitectureIndexDocument.java`
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/ir/model/ArchitectureEntity.java`
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/ir/model/ArchitectureRelationship.java`

### Validation and assembly touchpoints
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/ir/ArchitectureIrValidator.java`
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/ir/ArchitectureIrFactory.java`

### Contract examples and tests
- `docs/export-format/examples/*.json`
- `src/test/resources/export-contract/*.json`
- `src/test/resources/fixtures/ir/*.json`
- `src/test/java/info/isaksson/erland/architecturebrowser/indexer/ir/ExportFormatSchemaExamplesContractTest.java`
- `src/test/java/info/isaksson/erland/architecturebrowser/indexer/ir/ArchitectureIrJsonTest.java`
- `src/test/java/info/isaksson/erland/architecturebrowser/indexer/ir/ArchitectureIrContractEvolutionBaselineTest.java`

## Baseline findings

1. The stable contract is currently versioned as `1.0.0`.
2. The top-level document schema is strict (`additionalProperties: false`).
3. The entity schema is strict (`additionalProperties: false`).
4. The relationship schema is strict (`additionalProperties: false`).
5. Document-level `metadata` remains the intentionally extensible area.

## Planned extension points

These are the intended future homes for the canonical normalization layer:

- `ArchitectureEntity`
  - future `architecturalRoles`
  - future `architecturalTraits`
  - possible future `architecturalEvidence`
- `ArchitectureRelationship`
  - future `architecturalSemantics`
  - possible future `architecturalEvidence`
- `ArchitectureIndexDocument`
  - future canonical viewpoint descriptors or availability catalog

The goal is to make these fields **first-class stable contract fields**, not hide them forever in
free-form metadata.

## Compatibility strategy for the normalization rollout

For the first normalization phase, maintainers should follow this strategy:

1. Keep the existing payloads valid and unchanged when the new fields are absent.
2. Introduce normalized fields as optional.
3. Update DTOs, JSON Schema, validator rules, curated examples, and checked-in fixtures together.
4. Perform an explicit `schemaVersion` review at the moment the new stable fields are introduced,
   because the current schema strictness means the change is not invisible to schema-aware
   consumers.
5. Preserve framework-specific metadata as supporting evidence; do not force consumers to derive
   common viewpoints from Java/TS-specific metadata alone.

## What Step 1 intentionally does not do

- does not add the normalization fields yet
- does not change example payloads yet
- does not bump `schemaVersion` yet
- does not change runtime assembly logic yet

That work starts in later steps once the baseline contract decisions are explicit and test-covered.
