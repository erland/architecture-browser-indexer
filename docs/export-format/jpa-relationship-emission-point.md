# JPA relationship emission point

## Purpose

This note records the current emission seam for JPA relationship metadata so later steps can add normalized association metadata in one place.

The goal is to identify where relationship-level metadata such as `jpaAssociation`, `mappedBy`, `joinColumn`, and `joinTable` is attached today, and to recommend the cleanest place for adding the new normalized fields documented in `normalized-association-metadata-contract.md`.

## Current emission path

The current JPA relationship emission point is:

- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/extract/JavaJpaAssociationSemanticsSupport.java`
  - method: `emitAssociationRelationship(...)`

This method constructs the relationship metadata map and emits the relationship into the extraction accumulator.

Today it attaches relationship-level metadata including:

- `framework = jpa`
- `relationshipType = hasAssociation`
- `jpaAssociation`
- `mappedBy`
- `joinColumn`
- `joinTable`
- `ownerQualifiedName`
- `ownerMemberKind`
- `ownerMemberName`
- `ownerPropertyName`

This is the narrowest and most direct seam for JPA association relationship metadata.

## Upstream call sites

The main upstream callers that detect JPA association semantics and delegate into this seam are:

- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/extract/JavaJpaPropertySemanticsSupport.java`
  - `addJpaFieldFacts(...)`
  - `addJpaMethodFacts(...)`

These methods:

1. detect JPA association kind from field/method annotations or snippets,
2. attach property-level metadata such as `jpaAssociation` to the owning member entity, and then
3. call `JavaJpaAssociationSemanticsSupport.emitAssociationRelationship(...)` to emit the relationship between the owning type and the target type.

## Related but non-primary locations

The following classes also mention `jpaAssociation`, but they are not the primary relationship emission seam for the exported relationship object:

- `JavaJpaDetailSupport`
  - analyzes and enriches entity/member metadata
  - important for property/type metadata, but not the preferred place for new exported relationship-level normalized fields

- `JavaJpaPropertySemanticsSupport`
  - detects JPA association semantics and enriches property metadata
  - delegates actual relationship emission to `JavaJpaAssociationSemanticsSupport`

Because of that split, the recommended implementation point for new relationship-level normalized metadata is **not** `JavaJpaDetailSupport`; it is the dedicated relationship emitter in `JavaJpaAssociationSemanticsSupport`.

## Recommendation for next steps

For later steps in this plan, add normalized relationship metadata at:

- `JavaJpaAssociationSemanticsSupport.emitAssociationRelationship(...)`

Rationale:

- JPA association kind is already known at this seam.
- Relationship-level metadata is already assembled here.
- JPA-specific evidence and new normalized fields can be emitted together without duplication.
- This avoids scattering normalized relationship semantics across multiple property-analysis helpers.

## Recommended implementation rule

When adding normalized association metadata such as:

- `associationKind`
- `associationCardinality`
- `sourceLowerBound`
- `sourceUpperBound`
- `targetLowerBound`
- `targetUpperBound`

attach them in the same relationship metadata map currently used for `jpaAssociation` in:

- `JavaJpaAssociationSemanticsSupport.emitAssociationRelationship(...)`

Property-level metadata can continue to be enriched upstream where needed, but exported relationship-level normalized semantics should have one primary emission seam.

## Verification notes

Searches that support this conclusion:

- `jpaAssociation`
- `one-to-one`
- `one-to-many`
- `many-to-one`
- `many-to-many`
- `mappedBy`
- `joinColumn`
- `joinTable`

These point to `JavaJpaAssociationSemanticsSupport.emitAssociationRelationship(...)` as the place where JPA association relationships are actually assembled and emitted.


## Current source-side convention

As of `indexer_step5`, `JavaJpaAssociationSemanticsSupport.emitAssociationRelationship(...)` is also the canonical place where the **source/target association-end convention** is applied for normalized bounds.

For emitted `sourceEntityId -> targetEntityId` relationships:

- `sourceLowerBound` / `sourceUpperBound` describe the source entity end
- `targetLowerBound` / `targetUpperBound` describe the target entity end

This means the JPA emission seam is now responsible not only for attaching `jpaAssociation`, `associationKind`, and `associationCardinality`, but also for enforcing the stable interpretation of source-side bounds.


## Optionality evidence precedence

For single-valued JPA associations (`one-to-one`, `many-to-one`), normalized bounds now use this precedence:

1. explicit association-level `optional = ...` when it is the only signal
2. explicit `@JoinColumn(nullable = ...)` when it is the only signal
3. if both are present and agree, use that shared meaning
4. if both are present and conflict, choose the conservative optional interpretation (`0..1`) rather than asserting mandatory (`1..1`)

This keeps the export cautious in ambiguous cases while still using explicit JPA evidence when available.
