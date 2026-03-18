# Viewpoint Catalog Contract

Step 7 adds an optional document-level `viewpoints` collection to the stable export contract.

## Purpose

The `viewpoints` collection gives the browser/platform a framework-agnostic catalog of viewpoint descriptors without forcing the consumer to understand Java-specific dependency catalogs or metadata conventions.

## Initial descriptor shape

Required fields per viewpoint:

- `id`
- `title`
- `description`
- `availability`
- `confidence`

Optional fields per viewpoint:

- `seedEntityIds`
- `seedRoleIds`
- `expandViaSemantics`
- `preferredDependencyViews`
- `evidenceSources`

## Initial stable ids

The contract now allows these initial canonical ids to appear:

- `api-surface`
- `request-handling`
- `persistence-model`
- `integration-map`
- `module-dependencies`
- `event-flow`
- `entry-points`
- `ui-navigation`

## Compatibility note

Because the top-level document schema is strict, introducing `viewpoints` required a schema version advance from `1.2.0` to `1.3.0`.

The field remains optional in this step so older fixtures and partial exports can omit it safely.

## Vocabulary expansion note

Step 2 reserves the canonical viewpoint id `ui-navigation` in the documented catalog so later steps can derive and emit it without introducing a new platform-facing id at the same time as the first normalization rules.

At this stage, documenting the id does **not** require the derivation layer to emit it yet. Emission remains conservative and belongs to the later viewpoint-derivation step once canonical UI roles and semantics are actually produced by normalization.
