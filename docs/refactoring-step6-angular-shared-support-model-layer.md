# Refactoring Step 6 — Angular shared support/model layer

## What changed

This step introduces a small Angular-focused shared support/model layer inside `extract` to reduce duplication across the Angular extraction subsystem.

Added shared classes:

- `AngularDecoratorModel`
- `AngularDecoratorModelExtractor`
- `AngularLiteralSupport`
- `AngularReferenceSupport`
- `AngularSourceSupport`

## Why this helps

Before this step, Angular extraction logic repeated low-level parsing and normalization logic in several places:

- decorator payload/object-literal parsing
- selector/reference normalization
- Angular-specific source reference creation
- inline snippet matching helpers

That made the Angular subsystem harder to extend safely.

After this step:

- decorator parsing has an explicit intermediate model
- low-level Angular literal parsing is centralized
- selector/reference normalization is centralized
- Angular source-reference helpers are centralized
- Angular extraction classes are more clearly focused on semantics rather than parsing utilities

## Main extractor updates

Updated extractors:

- `AngularDecoratorMetadataExtractor`
- `AngularDependencyInjectionExtractor`
- `AngularFrameworkRelationshipExtractor`
- `AngularTemplateCompositionExtractor`

## Intended next-step benefits

This creates better seams for future Angular phase-2 work such as:

- richer decorator payload modeling
- template-url file loading / cross-file composition
- module graph analysis
- standalone-component bootstrap analysis
- stronger provider token/value/factory modeling

## Verification target

Recommended verification command in a full dev environment:

```bash
mvn -Dtest='*Angular*Test,*RegressionTest' test
```

## Notes

I also added a focused helper test:

- `AngularSharedSupportTest`

This test is intended to protect the newly centralized Angular helper behavior during future refactors.
