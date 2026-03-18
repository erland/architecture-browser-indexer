# UI navigation normalization seam

This note documents the internal normalization seam introduced after the raw frontend evidence inventory.

## Purpose

Normalization rules for `ui-navigation` should not need to know every raw metadata key emitted by the frontend extractor.
Instead, the indexer now exposes structured internal helpers:

- `FrontendRouteEvidence`
- `FrontendNavigationEvidence`

These are internal-only normalization helpers and are **not** part of the exported schema.

## Current seam shape

Entity normalization contexts can now expose:

- `frontendRouteEvidence()`

Relationship normalization contexts can now expose:

- `sourceRouteEvidence()`
- `targetRouteEvidence()`
- `relationshipRouteEvidence()`
- `frontendNavigationEvidence()`

## Intended use

Use these helpers from normalization rules when mapping:

- declared routes/pages to `ui-page`
- route containers to `ui-layout`
- static links / imperative navigation to `navigates-to`
- redirects to `redirects-to`
- grounded guards to `guards-route`

## Non-goals

- do not expose these helper types in the exported contract
- do not move framework-specific browser logic into the browser
- do not treat dynamic/computed navigation as grounded unless extraction evidence is explicit
