# Refactoring Wave 2 — Contract-First Test Guidance

This note documents the Step 10 test hardening approach.

## Goal

Prefer assertions on stable architect-facing behavior over assertions on incidental metadata shape, ordering, or derived flags.

## Introduced helpers

- `ArchitectureContractAssertions.assertContainsViews(...)`
- `ArchitectureContractAssertions.assertHasPackageDependency(...)`
- `ArchitectureContractAssertions.assertPublishesEvent(...)`
- `ArchitectureContractAssertions.assertObservesEvent(...)`
- `ArchitectureContractAssertions.assertHasRelationshipByLabel(...)`
- `ArchitectureContractAssertions.assertDependencyViewRelationship(...)`

## Intended use

Use these helpers in broad regressions and seam safety-net tests where the contract is:

- a dependency/view exists
- an endpoint exists
- an event is published or observed
- a package depends on another package
- a view family is available

Avoid exact list equality, exact stringified map matching, or assertions on incidental normalization flags unless that specific shape is the contract under test.
