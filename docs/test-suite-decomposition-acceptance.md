# Test Suite Decomposition Acceptance

## Scope

This document closes the planned cleanup wave for the large and coupled test workstream.

Covered steps:

1. Baseline the large-test problem
2. Define taxonomy and naming/placement guidance
3. Extract shared contract assertion helpers
4. Decompose `TypeScriptStructuralExtractorSafetyNetTest`
5. Decompose `StructuralExtractionServiceTest`
6. Decompose `TypeScriptArchitectureFixtureRegressionTest`
7. Decompose `FrontendArchitectureEndToEndFixtureRegressionTest`
8. Remove representation-shaped assertions from remaining larger regressions
9. Introduce shared fixture builders
10. Add test-size and responsibility guardrails

## What changed across the wave

### Structural changes

The test suite now has clearer separation between:

- seam tests
- contract/regression tests
- broader end-to-end acceptance tests

The most important umbrella tests targeted in this wave were decomposed into smaller, concern-focused files:

- `TypeScriptStructuralExtractorSafetyNetTest`
- `StructuralExtractionServiceTest`
- `TypeScriptArchitectureFixtureRegressionTest`
- `FrontendArchitectureEndToEndFixtureRegressionTest`

### Helper changes

The suite now has stronger shared support for:

- contract-oriented architecture assertions via `ArchitectureContractAssertions`
- repeated synthetic parse/syntax-node setup via fixture builders
- shared TypeScript/frontend test support classes for decomposed regressions

### Maintenance guidance changes

The repository now documents:

- explicit test taxonomy and naming guidance
- file-size and responsibility guardrails
- decomposition watchlists and follow-up targets

## Acceptance conclusions

### 1. Large umbrella tests were materially reduced

This wave succeeded in reducing the main targeted umbrella tests by moving mixed concerns into focused regression classes.

### 2. Contract-oriented assertions improved

Broad regressions rely less on raw metadata-shape checks and more on contract helpers for stable architecture behavior.

### 3. Shared synthetic setup improved

Repeated synthetic parse-result and syntax-node setup is now more centralized, which should reduce boilerplate drift and inconsistent fixture construction.

### 4. The suite is healthier, but not “finished forever”

The suite is in a better state, but several sizable tests remain and should stay on the watchlist.

## Current watchlist after this wave

Largest current test files by line count at the time this acceptance doc was generated:

- `JavaStructuralExtractionContractRegressionTest (broad baseline only)` — about 991 LOC
- `FrontendFrameworkBaselineRegressionTest, FrontendRoleInterpretationRegressionTest, FrontendDependencyViewsRegressionTest` — about 548 LOC
- `ArchitectureDependencyFixtureRegressionTest, ArchitectureDependencyCycleRegressionTest, ArchitectureDependencyReasonRegressionTest` — about 524 LOC
- `AngularTypeScriptFrameworkSemanticsRegressionTest` — about 485 LOC
- `ArchitectureIrFactoryStructuralExtractionTest` — about 378 LOC
- `JavaJpaStructuralExtractionTest` — about 362 LOC
- `TypeScriptDeclarationExtractionSeamTest` — about 331 LOC

## What this means now

There is no longer one dominant frontend/TypeScript umbrella test acting as the main bottleneck for failure localization.

The remaining test-structure risk has shifted toward:

- large Java structural regression tests
- broad frontend framework baseline regressions
- broader architecture dependency fixture regressions
- some larger focused regressions that may need another split if they continue to grow

## Recommended verification

Run the full suite locally:

```bash
mvn test
```

Recommended spot checks for the decomposed areas:

```bash
mvn test -Dtest=TypeScript*Test
mvn test -Dtest=Frontend*Test
mvn test -Dtest=*StructuralExtraction*Test
```

## Acceptance result

This test cleanup wave should be considered **accepted** when the local Maven suite passes and the team agrees to keep using:

- the documented test taxonomy
- contract helpers instead of raw metadata scanning where practical
- shared fixture builders for synthetic setups
- decomposition guardrails for large tests


Java extraction contract regression concerns were decomposed into:
- JavaDeclarationOwnershipContractRegressionTest
- JavaHierarchyContractRegressionTest
- JavaFieldAndMethodDependencyContractRegressionTest


Angular TypeScript framework semantics concerns were decomposed into:
- AngularDecoratorPayloadExtractionRegressionTest
- AngularDependencyInjectionExtractionRegressionTest

The remaining AngularTypeScriptFrameworkSemanticsRegressionTest now acts as a smaller broad baseline for Angular framework relationships and template-composition behavior.
