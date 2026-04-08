# JPA Relationship Normalization — Final Integration Checklist

## Purpose

This checklist is the final handoff verification guide for the JPA relationship normalization work delivered in indexer steps 1–10.

It is intended to verify that:

- canonical normalized associations are exported for mergeable JPA inverse pairs
- duplicate field-level relationships are preserved only as evidence
- multiplicities are conservative and endpoint-correct
- containment promotion remains conservative
- non-peer/value-like JPA patterns stay outside peer-association normalization
- entity/persistence-oriented downstream consumers can use the exported association catalog

## Verification Commands

Run the most focused regression suites first, then the full suite.

```bash
cd indexer
mvn test -Dtest=*Jpa*,*RelationshipNormalization*,*Viewpoint*,*Catalog*,*Regression*
```

Then run the full test suite:

```bash
cd indexer
mvn test
```

Optional deeper verification:

```bash
cd indexer
mvn verify
```

## Acceptance Criteria

### 1. Canonical normalized associations exist
Confirm that mergeable inverse JPA pairs produce exactly one canonical normalized association for downstream entity/persistence views.

Expected cases:
- `@ManyToOne` ↔ `@OneToMany(mappedBy = ...)`
- `@OneToOne` ↔ inverse `mappedBy`
- `@ManyToMany` ↔ inverse `mappedBy`

### 2. Inverse duplicate elimination works
Confirm that duplicate raw field-level edges are not exported as the primary canonical peer-entity association for mergeable cases.

Raw field-level relationships may still exist in the document as evidence.

### 3. Multiplicity derivation is conservative
Confirm that endpoint bounds are derived conservatively:
- scalar ends default to upper bound `1`
- collection ends default to upper bound `*`
- `optional = false` / `nullable = false` can raise lower bound to `1`
- ambiguity falls back to the weaker/safer lower bound and wider/safer upper bound

### 4. Containment promotion is conservative
Confirm that `containment` is promoted only when evidence is strong enough.

Typical strong-evidence cases:
- required ownership plus `orphanRemoval`
- required ownership plus cascade remove/all
- identity-bound one-to-one patterns such as `@MapsId` or `@PrimaryKeyJoinColumn`

Confirm that:
- weak/ambiguous cases remain plain association
- many-to-many is never promoted to containment

### 5. Non-peer/value-like cases are handled separately
Confirm that the following do not become misleading peer-entity normalized associations:
- `@ElementCollection`
- `@Embedded`
- `@Embeddable`
- `@EmbeddedId`

Also confirm that unidirectional peer associations are still exported explicitly as single-sided normalized associations where appropriate.

### 6. Exported association catalogs exist
Confirm the exported metadata includes canonical association catalogs for downstream consumers, including:

- `entityAssociationRelationships`
- `relationshipCatalogs.entityAssociations`

### 7. Evidence retention is intact
Confirm normalized associations still retain references to raw field-level evidence via relationship ids and/or member references.

### 8. Representative regression fixture coverage exists
Confirm representative JPA fixture coverage includes:
- bidirectional one-to-many
- bidirectional one-to-one
- bidirectional many-to-many
- same-entity-pair multiple associations that must remain distinct
- unidirectional peer association
- embedded/value-like patterns

## Platform Handoff Expectations

The platform should be able to:

- use normalized associations as the primary edge source for entity/persistence views
- show multiplicities on both ends
- style `containment` distinctly but conservatively
- expose raw evidence in details/facts panels

The platform should **not** need to re-derive JPA inverse-pair semantics from raw field annotations when normalized associations are present.

## Final Signoff

The JPA relationship normalization work can be considered ready for platform integration when all of the following are true:

- regression tests pass
- canonical association export is present
- evidence retention is preserved
- no duplicate mergeable inverse-pair edges remain as the primary peer-entity association
- representative sample export matches the documented normalized association shape
