# Test Suite Decomposition Continuation Notes

## Current status

The initial decomposition wave completed the planned work around the largest targeted TypeScript/frontend umbrella tests and established durable guidance for future test additions.

## Next likely candidates

If a future cleanup wave is needed, the most likely next candidates are:

1. `JavaStructuralExtractionContractRegressionTest (broad baseline only)`
2. `FrontendFrameworkBaselineRegressionTest, FrontendRoleInterpretationRegressionTest, FrontendDependencyViewsRegressionTest`
3. `ArchitectureDependencyFixtureRegressionTest`
4. `AngularTypeScriptFrameworkSemanticsRegressionTest`
5. `ArchitectureIrFactoryStructuralExtractionTest`
6. `JavaJpaStructuralExtractionTest`

## Suggested next decomposition order

### 1. Java structural extraction regressions

Focus on separating:

- entity/declaration extraction
- relationship/evidence emission
- framework semantics (JAX-RS / CDI / JPA)
- broad service-level extraction baselines

### 2. Frontend framework baseline regressions

Separate by concern family:

- React semantics
- Angular semantics
- composition/routing/state semantics
- framework-specific browser/dependency view behavior

### 3. Architecture dependency fixture regressions

Separate by dependency family:

- type/module/package dependency baselines
- evidence-tier behavior
- browser/dependency-view shaping
- framework-specific dependency categories

### 4. JPA-focused structural regressions

If JPA semantics continue to grow, split tests by:

- entity/type semantics
- field/property semantics
- relationship/join semantics
- inheritance/embedded/value-object behavior

## Ongoing maintenance rules

Keep following the rules established in `docs/test-style-guide.md`:

- keep most new tests under roughly 250 lines
- treat tests above roughly 400 lines as decomposition candidates
- keep one main concern family per test class
- keep service tests service-level
- prefer contract assertions over representation-shaped checks
- prefer shared fixture builders over repeated ad hoc setup

## Practical trigger for another cleanup pass

Start another dedicated decomposition pass when one of these becomes true:

- a single test class becomes the main source of localized refactor breakage
- a test file grows beyond about 400–500 lines and mixes multiple concern families
- failures repeatedly require editing several large tests for one production change
- new assertions start reintroducing raw metadata-map equality or ordering dependencies


Java extraction contract regression concerns were decomposed into:
- JavaDeclarationOwnershipContractRegressionTest
- JavaHierarchyContractRegressionTest
- JavaFieldAndMethodDependencyContractRegressionTest


Angular TypeScript framework semantics concerns were decomposed into:
- AngularDecoratorPayloadExtractionRegressionTest
- AngularDependencyInjectionExtractionRegressionTest

The remaining AngularTypeScriptFrameworkSemanticsRegressionTest now acts as a smaller broad baseline for Angular framework relationships and template-composition behavior.
