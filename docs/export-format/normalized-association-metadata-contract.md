# Normalized association metadata contract

This note defines the **documented stable normalized metadata keys** that relationship producers may use to describe association-style relationships in a framework-agnostic way.

The goal is to let downstream consumers, especially the browser/platform layer, reason about entity associations without coupling directly to framework-specific metadata such as `jpaAssociation`.

## Scope

This document defines the exported normalized association contract in two layers:

- stable normalized metadata keys on `relationship.metadata`
- a first-class `relationship.normalizedAssociation` object for canonical association emission

As of the current implementation, the indexer emits `relationship.normalizedAssociation` for canonical JPA peer-entity associations and publishes browser-facing catalogs such as `dependencyViews.entityAssociationRelationships` and `dependencyViews.relationshipCatalogs.entityAssociations`.

## Stable normalized metadata keys

When present on `relationship.metadata`, the following keys are part of the documented normalized contract:

- `associationKind`
- `associationCardinality`
- `sourceLowerBound`
- `sourceUpperBound`
- `targetLowerBound`
- `targetUpperBound`

These keys are optional. Older exports may omit them entirely.

## Intended meaning

### `associationKind`

Canonical broad association family for the relationship.

Allowed first-step value:
- `association`

This field exists so future work can distinguish broad normalized relationship families without overloading cardinality.

### `associationCardinality`

Canonical normalized cardinality family.

Allowed first-step values:
- `one-to-one`
- `one-to-many`
- `many-to-one`
- `many-to-many`

This field is intended to be framework-agnostic. For example, a JPA relationship may still emit `jpaAssociation`, but downstream consumers should be able to rely on `associationCardinality` once it is emitted.

### Current implementation status

As of `indexer_step4`, JPA-derived exported association relationships now emit:

- `associationKind = association`
- `associationCardinality = <same value as jpaAssociation>`
- normalized endpoint bounds in `sourceLowerBound`, `sourceUpperBound`, `targetLowerBound`, and `targetUpperBound`

while retaining `jpaAssociation` and other JPA-specific evidence fields unchanged.

Current derivation is intentionally conservative:

- `@ManyToOne` and `@OneToOne` use `optional = false` or `@JoinColumn(nullable = false)` to derive mandatory single-valued target bounds
- `@OneToMany` and `@ManyToMany` currently default collection lower bounds to `0`

### `sourceLowerBound` and `targetLowerBound`

Lower multiplicity bound for the source or target association end.

Allowed first-step values:
- `0`
- `1`

These values are carried inside `relationship.metadata`, so they are encoded as metadata values rather than dedicated typed schema properties.

### `sourceUpperBound` and `targetUpperBound`

Upper multiplicity bound for the source or target association end.

Allowed first-step values:
- `1`
- `*`

As with lower bounds, these values live inside `relationship.metadata`.


## Source/target interpretation convention

As of `indexer_step5`, the normalized endpoint-bound contract uses the **full association-end convention**.

For a relationship emitted from `sourceEntityId -> targetEntityId`:

- `sourceLowerBound` / `sourceUpperBound` describe the multiplicity at the **source entity end**
- `targetLowerBound` / `targetUpperBound` describe the multiplicity at the **target entity end**

This is intentionally **not** a property-only convention. The goal is to let downstream diagramming/rendering code annotate both ends of the edge consistently.

### Current JPA source-side mapping convention

For JPA-derived associations, the current convention is:

- `many-to-one`
  - source end = `0..*`
  - target end = `0..1` or `1..1` depending on optionality/nullability evidence
- `one-to-many`
  - source end = `0..1`
  - target end = `0..*`
- `one-to-one`
  - both ends = `0..1` or `1..1` depending on optionality/nullability evidence
- `many-to-many`
  - both ends = `0..*`

This first version is intentionally conservative. In particular:

- collection lower bounds default to `0`
- `1..*` is not inferred without stronger evidence
- `one-to-many` source-side bounds stay at `0..1` in the first version rather than asserting stronger ownership semantics


## Current limits

The current normalized association metadata is intentionally conservative.

### Collection lower bounds are conservative

For the first version:

- `@OneToMany` collection targets default to `0..*`
- `@ManyToMany` ends default to `0..*`

The indexer does **not** currently attempt to infer `1..*` from JPA mappings alone, because ORM annotations usually do not provide reliable minimum-cardinality semantics for collections.

### `1..*` is not inferred without strong evidence

The current implementation does **not** upgrade collection-valued associations to `1..*` unless a future step introduces stronger, explicitly supported evidence for that claim.

This avoids exporting multiplicities that look precise but are only guessed.

### Some source-side bounds are intentionally conservative

Even though the contract uses a full association-end convention, some source-side bounds are still first-version approximations chosen for stability and readability rather than maximal semantic ambition.

In particular:

- `one-to-many` currently uses a conservative source end of `0..1`
- collection-oriented source-side ownership semantics are not over-interpreted

### Exact UML semantics depend on extracted framework evidence

The normalized bounds are only as strong as the evidence extracted from the source framework.

For JPA-derived associations, the current implementation relies primarily on:

- association type (`@OneToOne`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`)
- association `optional = ...`
- `@JoinColumn(nullable = ...)`

If that evidence is missing, incomplete, or conflicting, the current implementation falls back to a conservative interpretation.

### JPA-specific metadata remains part of the contract as provenance

The normalized fields do **not** replace JPA-specific evidence. JPA-origin details such as:

- `jpaAssociation`
- `mappedBy`
- `joinColumn`
- `joinTable`

remain valuable for debugging, inspector views, export verification, and future derivation improvements.

Consumers should treat the normalized fields as the primary cross-framework semantics and the JPA fields as provenance/evidence.

## Encoding note

Because `relationship.metadata` is an extensible metadata map, these normalized keys are currently documented as **stable metadata keys**, not as new first-class top-level relationship fields.

That means:

- no relationship object shape change is required in this step
- older consumers remain compatible
- older exports remain valid
- the contract is documented before emitters start populating the fields

## Relationship to framework-specific metadata

Framework-specific metadata is still allowed and remains useful as provenance/evidence.

Examples include:
- `jpaAssociation`
- `mappedBy`
- `joinColumn`
- `joinTable`

The intended layering is:

- **normalized metadata** for platform/viewpoint consumption
- **framework metadata** for provenance, debugging, and detailed inspector experiences

Example future relationship metadata shape:

```json
{
  "associationKind": "association",
  "associationCardinality": "many-to-one",
  "sourceLowerBound": "0",
  "sourceUpperBound": "*",
  "targetLowerBound": "1",
  "targetUpperBound": "1",
  "jpaAssociation": "many-to-one",
  "joinColumn": "customer_id"
}
```

## Compatibility note

This contract documentation does **not** require a `schemaVersion` change in this step because:

- relationship objects already allow extensible `metadata`
- the stable object shape is unchanged
- the change is documentation of optional metadata keys, not a new required structural field

A future change would need a schema version advance only if these semantics move out of metadata into new first-class schema-strict relationship fields, or if currently optional behavior becomes required.

## Consumer guidance

Consumers should follow this precedence when the emitter starts producing these fields:

1. Prefer normalized keys when present:
   - `associationKind`
   - `associationCardinality`
   - `sourceLowerBound`
   - `sourceUpperBound`
   - `targetLowerBound`
   - `targetUpperBound`
2. Fall back to framework-specific metadata such as `jpaAssociation` only when consuming older exports that do not yet emit the normalized keys.

## Current non-goals

This step does **not** yet decide:

- richer source-side inference beyond the currently documented full association-end convention
- how aggressively `1..*` is inferred
- how conflicting optionality/nullability evidence is resolved
- whether future frameworks besides JPA populate the same keys

Those decisions belong to later implementation steps.


## First-class normalized association object

As of schema version `1.4.0`, `relationship` may also carry an optional `normalizedAssociation` object.

This object is intended to become the canonical home for merged association semantics once later steps implement derivation. The object is optional so older exports remain readable and existing emitters do not need to populate it immediately.

Supported fields in this step:

- `associationKind`
- `associationCardinality`
- `sourceLowerBound`
- `sourceUpperBound`
- `targetLowerBound`
- `targetUpperBound`
- `bidirectional`
- `evidenceRelationshipIds`
- `owningSideEntityId`
- `owningSideMemberId`
- `inverseSideEntityId`
- `inverseSideMemberId`

Recommended interpretation:

- use the top-level `normalizedAssociation` object when present as the canonical merged association view
- fall back to normalized metadata keys inside `relationship.metadata` for older exports or during the transition period
- continue to treat framework-specific metadata as provenance/evidence

## Relationship to JPA normalization rules

This file defines the exported normalized association contract.

For the current JPA-specific derivation, merge, non-merge, multiplicity, containment, and evidence-retention rules, see [jpa-relationship-normalization-and-merge-rules](jpa-relationship-normalization-and-merge-rules.md).


## Current emitted browser-facing catalogs

When canonical normalized JPA peer associations are present, the export now also emits browser-facing catalog structures for downstream consumers:

- `dependencyViews.entityAssociationRelationships`
  - a list of canonical normalized peer-entity association entries
  - excludes topology rollups and non-peer/value-like JPA cases such as `value-collection`, `embedded-value`, and `embedded-identifier`
- `dependencyViews.relationshipCatalogs.entityAssociations`
  - catalog metadata describing the exported entity-association relationship set
- `dependencyViews.javaBrowserViews.views[*].relationshipCatalogView`
  - set to `entityAssociationRelationships` for the Java entity model graph when canonical association data is available
- `dependencyViews.javaBrowserViews.views[*].preferredDependencyView`
  - also set to `entityAssociationRelationships` for the Java entity model graph when canonical association data is available

These browser-facing catalogs are now part of the effective handoff contract for the platform layer.
