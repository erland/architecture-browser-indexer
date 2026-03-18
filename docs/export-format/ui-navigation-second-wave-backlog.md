# UI navigation second-wave backlog

This note captures **future enhancements** for the `ui-navigation` viewpoint that are intentionally kept **out of the MVP**.

The purpose is to make follow-up work explicit without expanding the current contract or blocking the conservative first release.

## Status

Current MVP scope covers:

- `ui-page`
- `ui-layout`
- `ui-navigation-node`
- `contains-route`
- `navigates-to`
- `redirects-to`
- `guards-route`
- `ui-navigation` viewpoint derivation
- curated examples and contract coverage for the MVP vocabulary

This backlog is deliberately **non-binding**. None of the items below are part of the current required export contract.

## Design guardrails for future work

Any second-wave enhancement should preserve the same core principles as the MVP:

- normalize in the indexer, not in the browser
- keep the canonical vocabulary small unless there is strong downstream value
- distinguish grounded evidence from inferred behavior
- avoid pretending arbitrary UI behavior is fully knowable from static analysis
- prefer additive contract evolution over breaking renames or semantic drift

## Candidate follow-ups

### 1. Deep links

Potential future semantic:

- `deep-links-to`

Use when a route/page can be reached through explicit externally shareable URLs, route fragments, or framework-grounded deep-link declarations.

Why it is deferred:

- static evidence is often incomplete
- query parameters and fragment handling vary by framework
- easy to overclaim reachability semantics

Suggested acceptance bar:

- only when the route target is explicit and the deep-link form is grounded in code/config metadata

### 2. Dialog and modal navigation

Potential future semantic:

- `opens-dialog`

Use when a page or navigation node opens a modal/dialog surface that is important enough to model architecturally.

Why it is deferred:

- dialogs are often transient UI state, not route state
- many implementations are framework- or component-library-specific
- easy to mix view interaction semantics with page reachability semantics

Suggested acceptance bar:

- only for architecturally meaningful, clearly grounded dialog patterns
- keep distinct from route/page navigation

### 3. Tab switching and secondary navigation regions

Potential future semantic:

- `switches-tab`

Use when the application has grounded tabbed structures that matter for user navigation understanding.

Why it is deferred:

- tabs may be route-backed, state-backed, or component-local
- cross-framework normalization may become ambiguous quickly

Suggested acceptance bar:

- only when tabs are explicit, stable, and materially useful in architecture browsing

### 4. Route parameter modeling

Examples:

- `/orders/:id`
- `/cases/:caseId/events/:eventId`

Potential future additions:

- parameterized route metadata
- normalized route-parameter descriptors

Why it is deferred:

- route identity and route instance identity are different concerns
- parameter richness can explode contract complexity fast

Suggested acceptance bar:

- model route shape, not arbitrary runtime values
- keep parameters descriptive, not behavioral

### 5. Lazy-loaded module grouping

Potential future enhancement:

- group route/page subgraphs by lazy-loaded module or feature boundary

Why it is deferred:

- extraction support is framework-specific
- grouping semantics may belong more to decomposition/ownership views than navigation views

Suggested acceptance bar:

- only if grouping materially improves navigation comprehension without polluting the core vocabulary

### 6. Cross-app navigation and microfrontend boundaries

Potential future enhancement:

- explicit external navigation edges between applications or bounded browser surfaces

Why it is deferred:

- boundaries may be deployment-specific rather than code-local
- ownership, runtime shell, and navigation semantics can be conflated

Suggested acceptance bar:

- require explicit, grounded boundary evidence
- keep intra-app and cross-app semantics clearly separate

### 7. Stronger menu/sidebar/navigation structure modeling

Potential future enhancement:

- richer support for menu groups, section headers, nested sidebars, breadcrumbs, and contextual nav trees

Why it is deferred:

- navigation structures are often partially dynamic
- component-library abstractions vary a lot
- the MVP already supports conservative `ui-navigation-node`

Suggested acceptance bar:

- expand only when the additional structure materially helps architects reason about reachability or information scent

### 8. Confidence scoring improvements

Potential future enhancement:

- more explicit confidence levels for declared routes, inferred links, imperative navigation, redirects, and guards

Why it is deferred:

- confidence policy should be consistent across viewpoints
- premature scoring detail can create false precision

Suggested acceptance bar:

- confidence must remain explainable from evidence
- avoid opaque composite scores that downstream users cannot reason about

### 9. Domain workflow overlays on top of UI navigation

Potential future enhancement:

- overlay business workflows or user journeys on top of the normalized `ui-navigation` graph

Why it is deferred:

- workflow semantics are not the same as route reachability
- this likely belongs in an overlay or higher-order viewpoint, not the core MVP graph

Suggested acceptance bar:

- keep route topology and business workflow semantics distinct

## Suggested sequencing for future waves

If second-wave work is pursued later, a low-risk order is:

1. confidence scoring improvements
2. route parameter modeling
3. stronger menu/sidebar/navigation structure modeling
4. lazy-loaded module grouping
5. cross-app navigation / microfrontend boundaries
6. deep links
7. dialog/tab semantics
8. workflow overlays

This order favors changes that are most likely to stay additive and least likely to blur the MVP contract boundaries.

## Contract guidance for future contributors

Before adding any new UI-navigation semantic or role, verify all of the following:

- the concept cannot already be represented with existing canonical vocabulary
- the evidence is grounded enough to normalize conservatively
- the new concept is useful across frameworks, not just one extractor path
- the browser can consume the normalized shape without learning framework details
- the new concept can be documented with clear consumer expectations
- example fixtures and contract tests can protect the new behavior

When in doubt, prefer:

- enriched metadata over new canonical vocabulary
- backlog documentation over immediate contract expansion

## Expected outcome

Future work stays visible and prioritized, while the MVP remains small, conservative, and easier to trust.
