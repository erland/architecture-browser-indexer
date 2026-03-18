# UI Navigation Baseline

This note captures the **Step 1 baseline decisions** for introducing a conservative,
architect-focused `ui-navigation` viewpoint.

The purpose of this step is to make the vocabulary, evidence boundaries, and likely code
seams explicit **before** adding new normalized ids or browser behavior.

## Purpose of the `ui-navigation` viewpoint

The `ui-navigation` viewpoint is intended to let downstream browser/platform consumers answer a
small, stable set of questions without understanding framework-specific frontend routing details.

For the MVP, the viewpoint should help answer:

- which user-facing pages/routes exist
- which pages can lead to which other pages when the evidence is grounded
- which routes are nested under other routes or layouts
- which routes redirect to other routes
- which routes are guarded when the guard relationship is explicit enough

The goal is **not** to expose every frontend implementation detail. The goal is to export a
canonical navigation slice that can remain stable across React Router, Angular Router, Next.js,
and similar frontend ecosystems over time.

## Conservative MVP scope

This baseline intentionally keeps the first scope narrow.

### In scope

- route/page-like entities
- route hierarchy
- static route-to-route navigation when the source and target are both grounded
- redirects
- obvious guard relationships
- confidence/evidence distinction between grounded and inferred navigation

### Out of scope

- arbitrary click-flow modeling inside generic components
- highly dynamic/computed URLs
- full state-dependent navigation logic
- transient overlays such as dialogs/drawers/tabs
- framework-specific browser logic

## Canonical vocabulary baseline

This step does **not** add the ids yet. It establishes the vocabulary that later steps will
introduce in code and contract docs.

### Candidate canonical entity roles

- `ui-page`
- `ui-layout`
- `ui-navigation-node`

Intended meaning:

- `ui-page` = a route/screen/page a user can navigate to
- `ui-layout` = a route shell/layout that contains child routes or page regions
- `ui-navigation-node` = an explicit navigation structure such as a menu/sidebar/nav group, but
  only when grounded strongly enough

### Candidate canonical entity traits

Only add these later if the evidence is strong enough and they are still needed:

- `user-facing`
- `route-declared`
- `guarded`

### Candidate canonical relationship semantics

- `navigates-to`
- `contains-route`
- `redirects-to`
- `guards-route`

### Candidate canonical viewpoint id

- `ui-navigation`

## Canonicalization approach decision

The current indexer already centralizes canonical entity-role and relationship-semantic ids in
string-backed enums such as:

- `ArchitecturalRole`
- `ArchitecturalRelationshipSemantic`

The current viewpoint catalog is likewise derived from canonical ids emitted by
`ArchitectureIrViewpointDerivationService`.

**Decision for the next steps:** use the existing enum-backed/string-id pattern rather than
introducing a second free-form canonicalization mechanism.

That means the expected future implementation path is:

- expand `ArchitecturalRole` for the new UI-navigation roles
- expand `ArchitecturalRelationshipSemantic` for the new UI-navigation semantics
- add the canonical viewpoint id in viewpoint derivation/docs/tests

This keeps vocabulary definition centralized and aligned with the current normalized export
architecture.

## Grounded evidence vs inferred navigation

The MVP should distinguish clearly between grounded evidence and inferred architectural meaning.
The browser should be able to visualize uncertainty instead of pretending full certainty.

### Grounded evidence examples

These are examples of evidence strong enough to support later normalization work:

- an Angular route declaration with explicit `path` and `component`
- an Angular child-route declaration under a parent route
- an Angular route declaration with `redirectTo`
- an Angular route declaration with `canActivate`/`canMatch` style guard references
- a React Router `<Route path="/orders" element={<OrdersPage />}>`
- a React Router route declaration nested under a layout route
- an explicit link component or navigation call whose target resolves to a concrete known route

### Inferred navigation examples

These may still be useful, but should be treated conservatively and never overclaimed:

- component composition that *suggests* page flow but does not declare it
- string literals that look like paths but are not clearly used for navigation
- dynamic URL construction where the final target route is not statically grounded
- shared layout/component nesting that does not actually imply user navigation reachability

## Current TypeScript/frontend extraction touchpoints

The repository already contains frontend extraction seams that are directly relevant to later
`ui-navigation` work.

### Route discovery and route-specific extraction

Primary touchpoints:

- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/extract/FrontendRouteDiscoverySupport.java`
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/extract/FrontendRoutingExtractor.java`
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/extract/FrontendRouteEmissionSupport.java`
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/extract/FrontendRoutePathNormalizationSupport.java`

Observed existing evidence already handled here includes:

- route path declarations
- Angular `component` route targets
- React route targets
- nested route structure
- lazy-load references
- guard references
- resolver references
- route-path normalization support

### Broader frontend evidence that may support later navigation interpretation

Relevant supporting touchpoints:

- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/extract/AngularDecoratorModelExtractor.java`
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/extract/AngularDependencyInjectionExtractor.java`
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/extract/ReactJsxCompositionExtractor.java`
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/extract/SyntaxTreeAnnotationSupport.java`
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/normalize/TypeScriptArchitectureEntityNormalizationRule.java`
- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/normalize/TypeScriptArchitectureRelationshipNormalizationRule.java`

These seams should remain supporting evidence only unless they can back a conservative,
canonical navigation meaning.

## Current browser/platform-facing registration touchpoints

This repository is the indexer rather than the browser app, so the browser-side registration
surface visible here is the **export contract and viewpoint handoff**, not the final UI code.

Current touchpoints that later `ui-navigation` work will need to align with are:

- `src/main/java/info/isaksson/erland/architecturebrowser/indexer/ir/ArchitectureIrViewpointDerivationService.java`
  - canonical viewpoint registration/derivation point
- `docs/export-format/viewpoint-catalog-contract.md`
  - canonical viewpoint catalog ids and descriptor shape
- `docs/export-format/viewpoint-availability-derivation.md`
  - current derivation strategy and conservative availability rules
- `docs/export-format/java-browser-view-bridge.md`
  - existing migration/bridge pattern from source-specific browser-view evidence into canonical
    viewpoints
- `docs/export-format/schema/viewpoint.schema.json`
  - stable viewpoint descriptor schema
- `docs/export-format/examples/frontend-export.json`
  - current frontend dependency-view/browser-view evidence, including route-oriented view families

This means the browser-facing integration path for `ui-navigation` is expected to be:

1. add normalized ids and viewpoint derivation in the indexer
2. emit `ui-navigation` through the existing canonical viewpoint catalog
3. let the browser consume the canonical viewpoint and associated normalized entities/
   relationships without needing router-specific logic

## Step 1 deliverable summary

After this step, maintainers should have a shared answer to these questions:

- what problem the `ui-navigation` viewpoint is trying to solve
- what is in scope for the MVP and what is intentionally excluded
- which canonical ids are expected later
- how grounded evidence differs from inferred navigation
- which existing frontend extraction seams are most relevant
- which existing export/viewpoint handoff seams the browser integration should follow

## What Step 1 intentionally did not do

Step 1 itself was documentation-only. After Step 2, the canonical ids are now reserved in code/docs, but the following still remain future work:

- no schema or exported DTO shape change is required yet
- runtime normalization does not emit the new UI-navigation roles or semantics yet
- runtime viewpoint derivation does not emit `ui-navigation` yet
- browser rendering is not implemented yet

That work starts in later steps once the baseline and vocabulary are explicit and easy to reference.
