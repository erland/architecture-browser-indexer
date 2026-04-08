# JPA relationship normalization and merge rules

## Purpose

This note documents the current **JPA-specific normalization, merge, and evidence-retention rules** used when exporting canonical entity associations.

The goal is to make the current behavior explicit for:

- indexer maintainers evolving the extraction and normalization pipeline
- platform/browser consumers that prefer normalized entity associations over raw field-level JPA edges
- test authors adding regression coverage for new persistence scenarios

This document complements, rather than replaces:

- `normalized-association-metadata-contract.md`
- `relationship-semantics-contract.md`
- `jpa-relationship-emission-point.md`
- `normalized-association-current-limits.md`

## Scope

This note covers the behavior as of `indexer_step9` for:

- supported inverse-pair merge patterns
- non-merge patterns
- multiplicity derivation rules
- conservative `containment` promotion rules
- evidence retention strategy
- exported catalog/viewpoint expectations

It does **not** define a general ORM-agnostic association model. The normalized export is framework-agnostic at the contract layer, but the rules in this note are specifically about the current JPA-derived implementation.

## Output goals

When the indexer sees JPA association declarations, it aims to produce:

1. **one canonical normalized association** for high-confidence bidirectional inverse pairs
2. **explicit end multiplicities** on the canonical association
3. **conservative `containment`** when lifecycle/identity evidence is strong enough
4. **retained raw JPA evidence** so the canonical association remains traceable back to the source members and emitted raw relationships

## Core distinction: raw evidence vs canonical association

The export now distinguishes between two layers:

### Raw JPA evidence

Raw evidence is still extracted from the member declarations themselves, for example:

- `Task.project @ManyToOne(optional = false)`
- `Project.tasks @OneToMany(mappedBy = "project", orphanRemoval = true)`

This evidence may appear in:

- member/entity metadata
- raw relationship metadata
- normalized-association evidence references

### Canonical normalized association

When the evidence is strong enough, the indexer emits **one canonical relationship** that carries:

- normalized cardinality
- endpoint bounds
- bidirectionality
- evidence relationship ids
- ownership/inverse-side references
- optional normalized `associationKind = containment`

The canonical association is the preferred downstream relationship for entity/persistence-oriented views.

## Supported merge patterns

The current implementation merges only **high-confidence inverse pairs**.

### 1. `@ManyToOne` ↔ `@OneToMany(mappedBy = ...)`

This is the primary and most important merge pattern.

Typical example:

- child side: `@ManyToOne`
- parent side: `@OneToMany(mappedBy = "...")`

Expected normalized result:

- one canonical normalized association
- `bidirectional = true`
- cardinality family `one-to-many`
- both-end multiplicities derived conservatively
- evidence ids referencing both raw relationships

### 2. `@OneToOne` ↔ inverse `@OneToOne(mappedBy = ...)`

This pair is also merged when the inverse reference is clear.

Expected normalized result:

- one canonical normalized one-to-one association
- `bidirectional = true`
- both ends single-valued
- lower bounds derived conservatively from ownership/optionality evidence

### 3. `@ManyToMany` ↔ inverse `@ManyToMany(mappedBy = ...)`

This pair is merged into one canonical many-to-many association.

Expected normalized result:

- one canonical normalized association
- `bidirectional = true`
- many-to-many cardinality
- no containment promotion

## Matching requirements for inverse-pair merge

The current implementation does **not** merge associations merely because the same entity types appear on both sides.

A merge requires strong correspondence, including:

- compatible source and target entity types
- swapped entity direction across the candidate pair
- `mappedBy` alignment to the opposite member/property name when applicable
- member-level correspondence strong enough to conclude the declarations refer to the same conceptual association

This rule is important because the same two entity types may legitimately have multiple different relationships.

## Canonical direction rules

When a bidirectional inverse pair is merged, the implementation keeps one canonical relationship direction.

Current intent:

- prefer the `one-to-many` direction for `many-to-one`/`one-to-many` inverse pairs when possible
- keep a stable canonical direction for the merged edge so downstream consumers do not need to reason about both raw directions

The canonical direction is a representation choice. The semantic meaning is carried primarily by:

- endpoint multiplicities
- `associationCardinality`
- `bidirectional = true`
- evidence references

## Non-merge cases

The current implementation deliberately avoids over-merging.

### Separate associations between the same entity pair

If two entity types are connected by multiple different declared associations, they must remain distinct unless inverse correspondence is explicit.

Example:

- `Task.project`
- `Task.archivedFromProject`
- `Project.tasks`
- `Project.archivedTasks`

These should not collapse into one association just because they connect the same entity types.

### Unidirectional peer associations

These are **not** merged, but they are still handled explicitly as normalized single-sided peer associations.

Examples:

- unidirectional `@OneToMany`
- unidirectional `@OneToOne`
- unidirectional `@ManyToOne`

Expected normalized result:

- one explicit normalized association
- `bidirectional = false`
- single evidence relationship id

### Value-like and non-peer cases

The following are **not** treated as normal peer entity associations:

- `@ElementCollection`
- `@Embedded`
- `@Embeddable`
- `@EmbeddedId`

These are classified explicitly so they do not pollute entity-association diagrams as duplicate or misleading peer-entity edges.

## Handling categories

The current pipeline makes the following distinctions explicit in metadata/evidence handling:

- merged bidirectional peer association
- unidirectional peer association
- value collection
- embedded value
- embedded identifier
- non-peer/value-like relationship excluded from peer-association normalization

These categories exist to keep the diagram-oriented canonical association layer clean while preserving provenance.

## Conservative multiplicity derivation rules

Multiplicity derivation is intentionally conservative.

### Upper bounds

Current broad rules:

- scalar associations default to upper bound `1`
- collection associations default to upper bound `*`

### Lower bounds

Current broad rules:

- `optional = false` is evidence for lower bound `1` on the relevant single-valued end
- `@JoinColumn(nullable = false)` is also evidence for lower bound `1`
- when neither exists, the implementation defaults conservatively to lower bound `0`

### Conflicting evidence

When evidence conflicts, the current implementation prefers the weaker/safer interpretation.

Examples:

- conflicting mandatory vs optional signals fall back to optional (`0..1` on the affected single-valued end)
- conflicting merged endpoint bounds fall back to the weaker lower bound and wider upper bound

### Collection lower bounds

Collection lower bounds remain conservative in the first version.

Current behavior:

- collection-valued ends generally default to lower bound `0`
- `1..*` is **not** inferred without stronger supported evidence

## End-bound interpretation convention

For exported normalized associations:

- `sourceLowerBound` / `sourceUpperBound` describe the multiplicity at the **source entity end**
- `targetLowerBound` / `targetUpperBound` describe the multiplicity at the **target entity end**

This is a full association-end convention, not a property-only convention.

That means downstream diagram code can label both ends of the edge directly without needing to reinterpret the source member declaration.

## Conservative containment promotion rules

The normalized default is plain:

- `associationKind = association`

The implementation promotes a merged association to:

- `associationKind = containment`

only when the evidence is strong enough.

### One-to-many containment promotion

Current strong-evidence pattern:

- required ownership on the single-valued side, and
- inverse side has `orphanRemoval = true`, and
- lifecycle/cascade evidence includes remove/all semantics

### One-to-one containment promotion

Current strong-evidence patterns include:

- required ownership plus identity-bound hints such as `@MapsId` or `@PrimaryKeyJoinColumn`, or
- required ownership plus inverse orphan-removal/cascade-remove style lifecycle evidence

### Many-to-many exclusion

Many-to-many associations are not promoted to containment.

## Why `containment` instead of composition

The current export uses `containment` rather than asserting strict UML composition.

Rationale:

- JPA lifecycle/configuration evidence is strong, but not always enough to justify a universal composition claim
- `containment` communicates the stronger ownership/lifecycle hint without overclaiming exact conceptual semantics
- the platform can still render containment distinctly without forcing a strict UML symbol set

## Evidence retention strategy

Normalization does **not** discard raw evidence.

The canonical association keeps traceability through:

- evidence relationship ids for the raw emitted JPA relationships
- ownership/inverse-side references
- raw JPA metadata on the underlying source declarations and raw relationships

This allows downstream consumers to:

- render a clean single edge in the entity diagram
- inspect the field-level evidence in details/facts panels
- debug normalization decisions when needed

## Export and viewpoint expectations

The canonical normalized associations are now fed into exported relationship catalogs intended for downstream entity/persistence rendering.

Current expectation:

- entity/persistence-oriented consumers prefer the normalized association catalog
- raw field-level relationships remain available as evidence/provenance
- viewpoint and browser metadata can advertise the canonical entity-association relationship set without losing raw detail

## Current limitations

The current implementation remains intentionally cautious.

Important limits include:

- collection lower bounds are conservative
- `1..*` is not inferred without stronger evidence
- containment is promoted only for a narrow supported set of strong-evidence patterns
- merge behavior is only implemented for explicit/high-confidence inverse patterns
- value-like persistence constructs are handled separately and are not elevated into peer-entity canonical associations

See `normalized-association-current-limits.md` for the short limit summary.

## Recommended regression scenarios

When changing this behavior, protect these scenarios with tests/fixtures:

1. bidirectional one-to-many with required owner
2. bidirectional one-to-many with orphan removal and cascade remove/all leading to containment
3. bidirectional one-to-one with `@MapsId`
4. bidirectional many-to-many staying plain association
5. multiple distinct associations between the same entity pair that must stay separate
6. unidirectional peer association remaining explicit and non-bidirectional
7. `@Embedded` / `@ElementCollection` staying out of peer-association normalization
8. conflicting optionality/nullability evidence falling back conservatively

## Maintainer checklist

When changing the JPA normalization rules, update these together:

- code in the extraction/normalization pipeline
- regression fixtures and tests
- `normalized-association-metadata-contract.md` if the exported contract meaning changes
- `normalized-association-current-limits.md` if the stated limitations change
- this file so merge/non-merge behavior remains explicit



## Exported downstream catalogs

For platform/browser consumers, canonical JPA peer associations are currently surfaced through browser-facing dependency-view catalogs:

- `dependencyViews.entityAssociationRelationships` contains one entry per canonical normalized peer-entity association
- `dependencyViews.relationshipCatalogs.entityAssociations` describes that exported set
- `dependencyViews.javaBrowserViews.views[*].relationshipCatalogView` and `preferredDependencyView` point at `entityAssociationRelationships` for the Java entity model graph when present

The catalog builder deliberately excludes:

- topology rollup relationships such as ids starting with `rel:topology-`
- non-peer/value-like JPA handling categories such as `value-collection`, `embedded-value`, and `embedded-identifier`
- relationships marked with `jpaNonPeerAssociation = true`

This means the platform can treat `entityAssociationRelationships` as the preferred canonical browser-facing source for JPA peer-entity edges rather than re-deriving the semantics from raw field-level relationships.
