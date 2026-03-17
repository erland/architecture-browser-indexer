# Test Suite Decomposition Baseline

## Purpose

This document defines the starting point for the test-suite cleanup work focused on large and coupled tests. The goal of this baseline step is to make the current problem explicit, classify the biggest tests by role, and identify the first decomposition targets before any structural test refactoring begins.

## Baseline snapshot

Largest test files by line count in the current baseline:

1. `src/test/java/info/isaksson/erland/architecturebrowser/indexer/extract/TypeScriptStructuralExtractorSafetyNetTest.java` — decomposed in Step 4; broad safety net reduced and concerns moved into focused test classes
2. `src/test/java/info/isaksson/erland/architecturebrowser/indexer/extract/StructuralExtractionServiceTest.java` — 1200 LOC
3. `src/test/java/info/isaksson/erland/architecturebrowser/indexer/regression/TypeScriptArchitectureFixtureRegressionTest.java` — 780 LOC
4. `src/test/java/info/isaksson/erland/architecturebrowser/indexer/regression/FrontendArchitectureEndToEndFixtureRegressionTest.java` — 712 LOC
5. `src/test/java/info/isaksson/erland/architecturebrowser/indexer/regression/FrontendFrameworkBaselineRegressionTest.java` — 548 LOC
6. `src/test/java/info/isaksson/erland/architecturebrowser/indexer/regression/ArchitectureDependencyFixtureRegressionTest.java` — 524 LOC
7. `src/test/java/info/isaksson/erland/architecturebrowser/indexer/ir/ArchitectureIrFactoryStructuralExtractionTest.java` — 378 LOC
8. `src/test/java/info/isaksson/erland/architecturebrowser/indexer/extract/JavaJpaStructuralExtractionTest.java` — 362 LOC

These are not all equally urgent, but they are the primary sources of test size, mixed concern coverage, and slower failure localization.

## Role classification of the main large tests

### 1. `TypeScriptStructuralExtractorSafetyNetTest` (decomposed in Step 4)
**Current role:** mixed seam test + mixed feature regression + broad extractor safety net

**Why it is a target:**
- very large for a seam/safety-net style class
- likely mixes declaration extraction, member extraction, relationship emission, and framework-facing expectations
- a single change in one TypeScript concern can force updates to a giant umbrella test

**Decomposition target:** highest priority

### 2. `StructuralExtractionServiceTest`
**Current role:** service orchestration test mixed with extractor behavior checks

**Why it is a target:**
- broad service-level tests often accumulate lower-level language/framework assertions
- failures may point at the service even when the actual problem lives in a specific extractor

**Decomposition target:** highest priority

### 3. `TypeScriptArchitectureFixtureRegressionTest`
**Current role:** architecture regression fixture with multiple concern groups in one class

**Why it is a target:**
- likely mixes framework semantics, dependency expectations, structure assertions, and navigation/routing semantics
- fixture regressions become brittle when several concern families evolve independently

**Decomposition target:** highest priority

### 4. `FrontendArchitectureEndToEndFixtureRegressionTest`
**Current role:** broad frontend acceptance regression

**Why it is a target:**
- likely mixes routing, composition, framework relationships, context/provider semantics, and dependency views
- one failure can represent many possible root causes

**Decomposition target:** highest priority

### 5. `FrontendFrameworkBaselineRegressionTest`
**Current role:** broad framework regression baseline

**Why it is a target:**
- may still be reasonably sized for current scope, but it should be watched for further growth
- can likely be split by framework concern family if it grows more

**Decomposition target:** watchlist / secondary priority

### 6. `ArchitectureDependencyFixtureRegressionTest`
**Current role:** dependency regression fixture

**Why it is a target:**
- medium-large file size
- may still mix multiple dependency categories and representation-shaped assertions

**Decomposition target:** secondary priority

### 7. `ArchitectureIrFactoryStructuralExtractionTest`
**Current role:** IR contract/regression test

**Why it is a target:**
- not one of the biggest files, but large enough to watch
- some assertions have already been hardened toward contract-based checks; the class may still need further narrowing later

**Decomposition target:** secondary priority

## Representation-shaped assertion watchlist

The main assertion patterns to reduce in later steps are:

- exact list ordering when order is not the contract
- exact metadata map equality when only a subset is behaviorally meaningful
- exact presence/absence of optional derived flags when the stable contract is the higher-level relationship or view
- exact internal IDs or internal naming forms unless those identifiers are themselves part of the contract
- broad stringified collection comparisons instead of focused helper assertions

The desired direction is:
- more contract-oriented assertion helpers
- more concern-specific checks
- fewer umbrella tests scanning raw maps/lists inline

## Priority decomposition order

Recommended decomposition order for the follow-up test cleanup work:

1. `TypeScriptStructuralExtractorSafetyNetTest`
2. `StructuralExtractionServiceTest`
3. `TypeScriptArchitectureFixtureRegressionTest`
4. `FrontendArchitectureEndToEndFixtureRegressionTest`
5. `ArchitectureDependencyFixtureRegressionTest`
6. `FrontendFrameworkBaselineRegressionTest`
7. `ArchitectureIrFactoryStructuralExtractionTest`


## Test taxonomy adopted for decomposition work

The cleanup work now uses this taxonomy:

- **Seam tests** — narrow collaborator behavior at one seam
- **Contract/regression tests** — stable architect-facing behavior for a subsystem or feature family
- **End-to-end acceptance tests** — broader fixture/pipeline coverage across several layers

Naming and placement guidance is documented in:

- `docs/test-style-guide.md`

Existing large tests do not need to be mass-renamed immediately, but each decomposition step should move their replacements toward this taxonomy and keep broad acceptance tests under `src/test/java/.../regression`.

## Acceptance criteria for this baseline step

This baseline step is complete when:

- the large-test inventory is written down
- the main large/coupled tests are classified by role
- the first decomposition targets are explicitly prioritized
- the main brittle assertion patterns are listed for later cleanup
- no production behavior changes are introduced
- no test behavior changes are intentionally introduced

## Immediate next step

Proceed to defining an explicit test taxonomy and naming/placement guidance so the later decomposition work has a consistent structure.


## Step 3 status

Shared contract assertion helpers are now the preferred mechanism for architecture-level assertions in broad regression tests. The next decomposition steps should migrate repeated raw scans toward these helpers before splitting the largest umbrella test classes.


## Step 4 outcome

`TypeScriptStructuralExtractorSafetyNetTest` has been reduced to a smaller cross-cutting safety net. Detailed coverage was moved into:

- `TypeScriptDeclarationExtractionSeamTest`
- `TypeScriptTypeRelationshipRegressionTest`
- `AngularTypeScriptFrameworkSemanticsRegressionTest`
- `ReactTypeScriptFrameworkSemanticsRegressionTest`
- `FrontendRoutingExtractionContractRegressionTest`


## Step 5 status — StructuralExtractionService test decomposition

Implemented decomposition split:

- `StructuralExtractionServiceTest` now focuses on orchestration/service behavior
- `JavaStructuralExtractionContractRegressionTest` now owns Java-specific extraction contracts
- `TypeScriptStructuralExtractionContractRegressionTest` now owns TypeScript-specific extraction contracts
- `AbstractStructuralExtractionServiceTestSupport` centralizes the shared `extract(ParseBatchResult)` helper

This step reduces service-level failure blast radius by keeping extractor-specific assertions out of the service orchestration test class.


Step 6 update: `TypeScriptArchitectureFixtureRegressionTest` has been decomposed into focused architecture concern tests including `TypeScriptArchitectureDependencyRegressionTest` and `TypeScriptArchitectureFrameworkRegressionTest`, while the original fixture regression now remains as a smaller broad-picture baseline.


Step 7 update: `FrontendArchitectureEndToEndFixtureRegressionTest` has been decomposed into focused acceptance slices including `FrontendRoutingAcceptanceRegressionTest`, `FrontendFrameworkCompositionAcceptanceRegressionTest`, and `FrontendStateProviderAcceptanceRegressionTest`.


## Step 8 status

Representation-shaped assertions in the remaining larger regression tests were reduced further by introducing contract helpers for:
- type dependency presence
- external dependency evidence presence
- browser view availability by id
- browser view descriptor contracts
- UI module profile contracts
- Java write-path relationship contracts

The goal is to keep broad regressions focused on durable architecture behavior rather than exact metadata-map shape or optional derived flags.


## Step 9 status

Shared fixture builders are now available for the most repeated synthetic setups:

- `ParseFixtureBuilder`
- `SyntaxNodeFixtureBuilder`
- `TypeScriptExtractionFixtureBuilder`
- `TypeScriptArchitectureDocumentFixtureBuilder`

These are intended to centralize synthetic parse-result, syntax-node, and TypeScript architecture document setup so future test decomposition work reuses consistent builders instead of hand-assembling the same structures repeatedly.
