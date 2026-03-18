# Step 6 — Java-first relationship semantics

This note documents the first conservative emission of canonical `architecturalSemantics` values for Java snapshots.

## Goal

Expose a small, stable set of architecture-facing relationship semantics so platform/browser consumers can follow request and persistence flow without understanding JAX-RS, CDI, or JPA metadata directly.

## Implemented mappings

The current Java-first rule adds semantics only when there is clear evidence from existing Java interpretation and normalized entity roles.

### `serves-request`

Emitted for Java endpoint exposure relationships when the relationship kind is `EXPOSES` and the exposed side is a normalized `api-entrypoint`.

Typical current evidence:
- JAX-RS resource/controller exposing an interpreted endpoint entity

### `invokes-use-case`

Emitted for Java dependency/collaboration relationships when:
- relationship kind is `USES` or `DEPENDS_ON`
- source entity is a normalized `api-entrypoint`
- target entity is a normalized `application-service`

Typical current evidence:
- resource/controller field collaboration to a service

### `accesses-persistence`

Emitted for Java dependency/collaboration relationships when:
- relationship kind is `USES` or `DEPENDS_ON`
- source entity is a normalized `application-service`
- target entity is a normalized `persistence-access`

Typical current evidence:
- service field collaboration to a repository/DAO/mapper/persistence adapter

## Conservative optional mappings also supported when evidence exists

### `stored-in`

Emitted only when a normalized `persistent-entity` points to a `DATASTORE` entity through a dependency-style relationship.

### `calls-external-system`

Emitted only when a normalized entrypoint/service/persistence-access entity points to an `EXTERNAL_SYSTEM` entity through a dependency-style relationship.

## Important constraints

- the rule is additive only; it merges with any existing `architecturalSemantics`
- it intentionally prefers under-inference over overclaiming
- it relies on normalized entity roles plus existing Java evidence, rather than exposing framework-specific metadata as the platform contract
- it does not yet attempt to model full method-level request chains or event-flow semantics as canonical relationship semantics

## Expected downstream use

Consumers should prefer `architecturalSemantics` for high-level viewpoint seeding and graph expansion.
Raw Java metadata remains useful as evidence and debugging detail, but should not be required for the basic architectural flows covered here.
