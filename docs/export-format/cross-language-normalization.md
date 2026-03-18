# Cross-language normalization (Step 12)

Step 12 broadens the normalization seam beyond the Java-first delivery while staying conservative.

## Goal

Reuse the same canonical architecture-facing vocabulary across additional evidence sources without making the export contract framework-specific.

## Implemented second-wave mappings

### TypeScript / React / Angular

When existing interpretation or extraction evidence is already strong enough, the normalization layer now emits:

- `api-entrypoint`
  - route/page-like UI modules
  - startup points
- `application-service`
  - state modules
  - Angular injectable/service-like application services
- `integration-adapter`
  - API client / gateway / HTTP-facing service modules
- `configuration-provider`
  - React context/provider style modules

Related traits emitted when grounded:

- `externally-exposed`
- `framework-managed`
- `configuration-driven`

Related relationship semantics emitted when grounded:

- `invokes-use-case`
- `calls-external-system`
- `accesses-persistence`

### SQL / config

When existing extraction evidence is already strong enough, the normalization layer now emits:

- `persistent-entity`
  - SQL table entities
- `external-dependency`
  - external systems/datastores inferred from config
- `configuration-provider`
  - config entry artifacts
- `module-boundary`
  - config/SQL module/file entities when the file boundary itself is architecturally relevant

Related traits emitted when grounded:

- `persistent`
- `configuration-driven`

Related relationship semantics emitted when grounded:

- `calls-external-system`
- `accesses-persistence`

## Deliberate constraints

This step is intentionally conservative.

It does **not** attempt to make TypeScript, SQL, or config reach Java-level semantic richness. It only reuses evidence that already exists in extraction/interpretation and maps that evidence into the same normalized vocabulary.

## Extension guidance

Future work can add broader mappings for:

- frontend route-to-route and route-to-layout flows
- TypeScript event/state propagation semantics
- SQL object types beyond tables
- config-to-runtime binding semantics
- module-boundary and external-dependency derivation from richer topology evidence
