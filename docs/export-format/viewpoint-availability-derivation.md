# Step 8 — Viewpoint availability derivation

This step teaches the indexer to populate the document-level `viewpoints` catalog from already exported normalized roles, traits, relationship semantics, and assembled dependency-view evidence.

## Intent

The platform should be able to ask a snapshot what it is good at showing before it tries to build a browser viewpoint. Step 8 keeps that logic inside the indexer so the platform does not need to interpret Java/JPA specific metadata directly.

## Conservative first rules

The first derivation pass computes:

- `availability`
- `confidence`
- `seedRoleIds`
- `evidenceSources`
- `seedEntityIds` when they are easy to identify

Implemented viewpoints:

- `api-surface`
- `request-handling`
- `persistence-model`
- `integration-map`
- `module-dependencies`

## Current evidence strategy

The derivation layer looks at:

- normalized entity roles such as `api-entrypoint`, `application-service`, `persistent-entity`, `persistence-access`
- normalized relationship semantics such as `serves-request`, `invokes-use-case`, `accesses-persistence`, `calls-external-system`
- assembled dependency-view metadata for module dependency availability
- lightweight metadata hints only for evidence-source labelling, for example Java/JPA signals

## Compatibility note

This step does not change the exported schema shape introduced in Step 7. It changes how the indexer populates the already-added `viewpoints` contract fields.
