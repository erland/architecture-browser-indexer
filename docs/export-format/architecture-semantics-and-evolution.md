# Canonical architecture semantics and evolution policy

This document explains the normalized architectural layer added on top of the export graph core.

It is intended for:

- contributors evolving the indexer
- downstream platform/browser work that wants stable architectural inputs
- future language/framework implementations that need to map their own evidence into the same architectural vocabulary

## Why this layer exists

The indexer can observe rich language- and framework-specific evidence. That evidence is useful, but it is not a good direct contract for the platform.

Examples:

- Java/JAX-RS resource annotations
- Spring or CDI service stereotypes
- JPA entity annotations
- repository/DAO conventions
- browser/dependency view families that exist today for specific stacks

Those details are often:

- framework-specific
- unevenly available across languages
- likely to evolve as the indexer gets better

The normalized layer exists so the platform can consume architectural meaning without needing to understand every underlying technology.

## Contract layers

Think about the export in four layers.

### 1. Graph core
The graph core is the basic document structure:

- document metadata
- scopes
- entities
- relationships
- diagnostics
- completeness

This is still the foundation of the export.

### 2. Normalized architectural semantics
This layer expresses stable architectural meaning in a framework-agnostic way.

Current normalized contract fields are:

- entity `architecturalRoles`
- entity `architecturalTraits`
- relationship `architecturalSemantics`
- document `viewpoints`

This is the preferred platform-facing architectural layer.

### 3. Framework evidence and enriched metadata
This includes technology-specific metadata and evidence that helped the indexer infer the normalized layer.

Examples:

- Java-specific metadata
- dependency view metadata
- browser view metadata
- qualified-name and framework annotation hints

This layer can be richer and more volatile than the normalized layer.

### 4. Derived viewpoint descriptors
Viewpoints summarize what architectural slices the snapshot can currently support.

They are derived from normalized roles, traits, relationship semantics, and bridge metadata where needed.

A viewpoint is not raw evidence. It is an indexer statement that a useful architectural slice is available, partially available, or unavailable.

## Current canonical vocabulary

The vocabulary should stay intentionally small and reusable.

### Role vocabulary
Roles describe what an entity is doing architecturally.

Current canonical role ids include:

- `api-entrypoint`
- `application-service`
- `persistent-entity`
- `persistence-access`
- `ui-page`
- `ui-layout`
- `ui-navigation-node`

Interpretation guidance:

- `api-entrypoint` means the entity is an externally reachable architectural entry into the system, not merely that it is called internally.
- `application-service` means the entity coordinates application behavior or use cases.
- `persistent-entity` means the entity represents persistent data modeled by the system.
- `persistence-access` means the entity mediates access to persistence technology or persistent storage.
- `ui-page` means the entity represents a user-navigable page, screen, or route target in the exported UI structure.
- `ui-layout` means the entity represents a route shell or layout that contains child routes or route regions.
- `ui-navigation-node` means the entity represents an explicit navigation structure such as a menu, sidebar, or nav group when grounded strongly enough.

### Trait vocabulary
Traits describe important cross-cutting characteristics of an entity.

Current canonical trait ids include:

- `externally-exposed`
- `persistent`

Interpretation guidance:

- `externally-exposed` means the entity is exposed beyond the internal implementation boundary.
- `persistent` means the entity participates directly in persisted data representation.

Traits should not duplicate roles mechanically unless that duplication helps downstream architectural work.

### Relationship semantic vocabulary
Relationship semantics describe the architectural meaning of a relationship independent of the low-level relationship kind.

Current canonical semantic ids include:

- `serves-request`
- `invokes-use-case`
- `accesses-persistence`
- `stored-in`
- `calls-external-system`
- `navigates-to`
- `contains-route`
- `redirects-to`
- `guards-route`

Interpretation guidance:

- relationship `kind` remains the structural graph edge type
- `architecturalSemantics` expresses the higher-level architectural meaning inferred from that edge

For example, a structural dependency or write edge may still carry normalized semantic meaning such as `accesses-persistence` or `stored-in`. Likewise, frontend route containment or redirect edges may carry canonical UI-navigation semantics such as `contains-route` or `redirects-to` without exposing framework-specific router details.

### Viewpoint catalog vocabulary
Viewpoints are canonical architectural slices that the platform can offer when evidence is strong enough.

Current viewpoint ids include:

- `api-surface`
- `request-handling`
- `persistence-model`
- `integration-map`
- `module-dependencies`
- `entry-points`
- `event-flow`
- `ui-navigation`

Not every snapshot will support every viewpoint.

The indexer should advertise viewpoint availability conservatively.

## Stability policy

### What should be treated as platform-facing stable contract
The following ids are platform-facing and should evolve carefully:

- normalized role ids
- normalized trait ids
- normalized relationship semantic ids
- canonical viewpoint ids

These ids are intentionally architectural, compact, and reusable. Downstream consumers are expected to rely on them more strongly than on raw framework evidence.

### What can evolve more freely
The following can evolve faster:

- language/framework-specific metadata
- inference details and evidence hints
- bridge metadata used during migration
- internal normalization rules and heuristics

Those parts may change as the indexer improves, as long as the canonical architectural meaning stays stable or changes in a clearly documented way.

### Conservative inference rule
Do not overclaim.

Prefer:

- fewer canonical roles than too many speculative ones
- `partial` viewpoint availability over `available` when evidence is incomplete
- explicit tests/examples for each new mapping family

A smaller trustworthy vocabulary is better than a broad but unstable one.

## Rules for adding new canonical vocabulary

A new role, trait, semantic, or viewpoint should satisfy all of the following.

### 1. It represents architectural meaning
The id should help a person or platform reason about the system architecture, not just restate a framework detail.

Good:

- `application-service`
- `calls-external-system`
- `navigates-to`
- `contains-route`
- `redirects-to`
- `guards-route`
- `persistence-model`

Weak unless clearly justified:

- framework brand names
- annotation names
- parser-specific concepts

### 2. It should be reusable across languages/frameworks where possible
Prefer vocabulary that TypeScript, Java, SQL, config, or future extractors can all target.

Good:

- `configuration-provider`
- `integration-adapter`
- `module-boundary`

Avoid introducing Java-only canonical ids when a broader architectural term would work.

### 3. It should not encode a single framework unless unavoidable
If the meaning is really specific to one stack, keep it in framework evidence first.

Promote it to canonical vocabulary only when:

- the meaning is still architectural
- it is expected to matter to downstream consumers
- no broader reusable term would be more appropriate

### 4. It needs grounding in tests and examples
Every new canonical vocabulary family should come with:

- focused unit/regression tests
- at least one curated example or scenario demonstrating intended output
- documentation update in this package

### 5. It should fit the existing vocabulary shape
Before adding a new id, check whether the meaning can be expressed by combining:

- existing role + trait
- existing relationship semantic
- existing viewpoint availability logic

Prefer extending derivation rules over multiplying ids when the existing vocabulary already covers the concept.

## How to map future language/framework support

Future language support should target the normalized layer instead of teaching the platform about raw parser output.

### General mapping approach
For each language/framework:

1. collect raw evidence in extraction/interpretation
2. map that evidence into normalized roles/traits/relationship semantics
3. let viewpoint derivation operate on those normalized outputs
4. keep framework-specific evidence available as supporting metadata where useful

### TypeScript guidance
Candidate mappings when evidence is strong enough:

- routes/controllers/HTTP handlers → `api-entrypoint`
- service-layer orchestrators/stateful application coordinators → `application-service`
- outbound HTTP client wrappers or message adapters → `integration-adapter`
- environment/config providers → `configuration-provider`

Examples of likely semantics:

- route/controller to service → `invokes-use-case`
- service/adapter to external API client → `calls-external-system`
- page/layout hierarchy → `contains-route`
- page-to-page navigation with a grounded static target → `navigates-to`
- explicit route redirects → `redirects-to`
- obvious route guard relationships → `guards-route`

### SQL guidance
Candidate mappings when evidence is strong enough:

- table/view/domain-model alignment → `persistent-entity`
- persistent store artifacts or schema targets → `stored-in`

SQL often provides strong persistence evidence but weaker application-flow evidence, so viewpoint availability should remain conservative.

### Config guidance
Candidate mappings when evidence is strong enough:

- configuration modules/providers → `configuration-provider`
- external service endpoints/brokers declared in config → `external-dependency`
- deployment/module partition hints → `module-boundary`

Config evidence is often indirect. Keep it as supporting evidence unless the architectural meaning is genuinely strong.

## Migration guidance for current Java-first delivery

The current implementation is intentionally Java-first.

That means:

- normalized architectural ids are the preferred stable layer
- existing Java browser/dependency metadata still exists during migration
- canonical viewpoints may be enriched from Java-specific bridge metadata

Consumers should increasingly prefer canonical viewpoints and normalized fields over Java-specific metadata.

## Recommended consumer behavior

### For platform/browser work
Prefer reading in this order:

1. `viewpoints`
2. `architecturalRoles`
3. `architecturalTraits`
4. `architecturalSemantics`
5. framework evidence only when the normalized layer is insufficient

### For future indexer contributors
When adding inference:

- first ask whether the meaning belongs in canonical vocabulary
- if yes, add it conservatively with tests/examples/docs
- if no, keep it in framework evidence or bridge metadata

## Change checklist for normalized semantics

When changing normalized semantics, update together:

- code and normalization rules
- validator/schema if the contract shape changes
- curated examples
- contract/regression tests
- this documentation package
- compatibility/versioning notes when platform-facing ids or meanings change

## Summary

The normalized architecture semantics layer exists to let the platform work with architectural meaning rather than raw framework detail.

The guiding rule is simple:

- keep the canonical layer small, stable, and architectural
- let framework evidence stay richer and more volatile underneath it
- derive viewpoints conservatively from normalized meaning
