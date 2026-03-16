# Priority 3 — Step 6: Split browser/view shaping out of ArchitectureIrFactory

## What changed

This step moves browser-view descriptor and browser-view catalog shaping out of `ArchitectureIrFactory` into a focused helper:

- `ArchitectureIrBrowserViewMetadataBuilder`

`ArchitectureIrFactory` now delegates browser-view metadata construction instead of owning:

- frontend browser-view descriptors
- Java browser-view descriptors
- browser-view family catalog shaping
- frontend/browser view filtering helpers
- browser-view-specific metadata aggregation helpers

## Why this seam matters

`ArchitectureIrFactory` was still carrying a large amount of browser-facing metadata shaping logic that was conceptually separate from:

- IR document assembly
- relationship enrichment
- package metrics
- run/document metadata

By moving browser/view shaping into a dedicated builder, the next refactor steps can focus separately on:

1. package metrics / boundary summary shaping
2. metadata-map conversion helpers
3. final orchestration cleanup in `ArchitectureIrFactory`

## Intended behavior

This step is intended to preserve the existing contract for:

- `dependencyViews.frontendBrowserViews`
- `dependencyViews.javaBrowserViews`
- `dependencyViews.browserViewCatalog`

No schema changes are intended.

## Suggested verification

Run the targeted suites that exercise frontend and Java browser views:

```bash
mvn test -Dtest=ArchitectureIrFactorySeamSafetyNetTest
mvn test -Dtest=JavaFrameworkBrowserViewsRegressionTest
mvn test -Dtest=JavaBackendArchitectureEndToEndFixtureRegressionTest
mvn test -Dtest=TypeScriptArchitectureFixtureRegressionTest
mvn test -Dtest=FrontendArchitectureEndToEndFixtureRegressionTest
```
