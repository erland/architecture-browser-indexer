# Test Style Guide

## Purpose

This guide defines the intended taxonomy, naming, and placement rules for tests in the indexer repository. The goal is to keep the suite understandable, keep failures easy to localize, and avoid the return of large umbrella tests that mix several concerns.

## Test taxonomy

### 1. Seam tests

**Purpose:** verify narrow collaborator behavior at a specific seam.

Use seam tests when validating:
- one extracted helper/support class
- one flow/coordinator with a tight scope
- one builder/enricher/normalizer
- one language/framework support helper

**Characteristics:**
- narrow fixture setup
- one primary behavior family
- one clear reason to fail
- minimal regression surface

**Naming:** `*SeamTest`

**Placement:**
- keep in the same package area as the production collaborator under test
- examples:
  - `src/test/java/.../extract/*SeamTest.java`
  - `src/test/java/.../ir/*SeamTest.java`

### 2. Contract/regression tests

**Purpose:** verify stable architect-facing behavior for a subsystem or feature family.

Use contract/regression tests when validating:
- dependency-view behavior
n- framework semantics
- extraction outputs for a feature area
- fixture-based subsystem behavior that should remain stable through refactors

**Characteristics:**
- focused on behavior, not incidental internal representation
- may use richer fixtures than seam tests
- should still stay within one main concern family

**Naming:** `*ContractRegressionTest` or `*RegressionTest`

**Placement:**
- place near the subsystem package if the scope is narrow enough
- place under `regression` if the test freezes a broader feature contract across multiple collaborators

### 3. End-to-end acceptance tests

**Purpose:** verify broader pipeline behavior across several layers.

Use end-to-end acceptance tests when validating:
- extraction -> interpretation -> topology -> IR composition flows
- broad fixture preservation
- whole-subsystem acceptance behavior

**Characteristics:**
- intentionally broader
- should be few in number
- should freeze the important baseline, not every internal detail

**Naming:** `*EndToEndRegressionTest` or `*EndToEndFixtureRegressionTest`

**Placement:**
- normally under `src/test/java/.../regression`

## Naming and placement rules

### Rule 1 — encode the test role in the class name

Every new or renamed test should make its role obvious from the class name.

Preferred forms:
- `TypeScriptDeclarationDiscoverySupportTest` for narrow helper tests already aligned with a seam role
- `TypeScriptDeclarationDiscoverySeamTest` for new tests where the seam role should be explicit
- `JavaFrameworkBrowserViewsRegressionTest`
- `ArchitectureIrCompositionEndToEndFixtureRegressionTest`

### Rule 2 — one main concern per test class

A test class should ideally have one main reason to fail.

Good:
- route discovery only
- package dependency contract only
- JAX-RS endpoint semantics only

Avoid:
- route discovery + framework composition + dependency views + browser metadata in one file

### Rule 3 — broad tests belong in `regression`

If a test freezes a broader feature baseline or fixture contract across multiple collaborators, it belongs in the `regression` package rather than mixed into a seam/unit-style package.

### Rule 4 — service-level tests should stay service-level

Tests for service/orchestration classes should verify orchestration behavior, not become a dumping ground for language/framework-specific semantics.

### Rule 5 — prefer helpers over raw representation scanning

Broad tests should prefer contract helpers such as:
- endpoint presence checks
- package dependency checks
- event publish/observe checks
- view presence checks

Avoid repeated inline scans over raw maps/lists unless the exact representation is itself the contract.

## Migration guidance for existing large tests

The main large tests identified in the baseline should be decomposed toward this taxonomy:

- `TypeScriptStructuralExtractorSafetyNetTest`
  - target: several seam/regression tests plus a smaller retained safety net
- `StructuralExtractionServiceTest`
  - target: orchestration-focused service tests + lower-level extractor tests
- `TypeScriptArchitectureFixtureRegressionTest`
  - target: focused regression files by concern family
- `FrontendArchitectureEndToEndFixtureRegressionTest`
  - target: narrower frontend acceptance slices + one small broad acceptance test if still needed

## Practical guardrails

When adding or refactoring tests:
- keep most new test classes narrowly scoped
- split tests once they start covering more than one architectural concern family
- avoid exact ordering assertions unless ordering is explicitly the contract
- avoid exact metadata map equality unless the map shape is explicitly the contract
- keep fixture builders/helper code small and readable

## Immediate naming/placement decisions for this workstream

During the decomposition work:
- new narrow collaborator tests should prefer the seam naming convention
- feature-family regressions should use regression-oriented names
- broad fixture acceptance tests should stay under `regression`
- existing large file names do not need to be mass-renamed in one pass, but each decomposition step should move toward this taxonomy


## Contract Assertion Helpers

Use shared helpers in `src/test/java/.../testing/ArchitectureContractAssertions` for architecture-level expectations instead of repeating raw list/map scans in large regression tests. Prefer helpers such as `assertHasEndpoint(...)`, `assertHasPackageDependency(...)`, `assertPublishesEvent(...)`, `assertObservesEvent(...)`, `assertHasFrameworkDependencyView(...)`, and `assertContainsViews(...)` when the goal is to assert stable behavior rather than internal representation shape.
