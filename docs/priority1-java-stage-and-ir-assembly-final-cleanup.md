# Priority 1 Java stage and IR assembly final cleanup

## Scope

This cleanup closes the focused refactoring track around the two hotspots identified in the plan:

- `extract/JavaSyntaxTreeExtractionStage`
- `ir/ArchitectureIrAssemblyCompositionSupport`

The goal of this phase was not to redesign extraction or IR contracts. The goal was to make the existing behavior easier to understand, safer to extend, and easier to regression-test.

## Final state

### Java extraction

`JavaSyntaxTreeExtractionStage` now acts as a thin orchestration boundary.

The Java path is split into focused collaborators for:

- compilation-unit setup
- traversal dispatch
- type declaration handling
- field extraction
- method extraction
- semantics support for JAX-RS, JPA, CDI, and write-path detection
- explicit dispatch results for traversal handoff
- composition/wiring support for stage setup

### IR assembly

`ArchitectureIrAssemblyCompositionSupport` now acts as an orchestration-first composition seam.

The IR path is split into focused collaborators for:

- dependency metadata enrichment
- synthetic package rollup creation
- dependency-view assembly
- dependency-view catalog shaping
- browser/dependency handoff
- compatibility helpers isolated behind a clearly named compatibility layer
- package entity enrichment
- explicit composition input/result flow

## What was cleaned up in the final pass

- End-to-end hotspot regression tests were added for both the Java stage and IR composition pipeline.
- Broad regression assertions were adjusted to check stable architect-facing contracts rather than brittle internal naming details.
- Remaining documentation now reflects that the hotspot refactor is complete through final cleanup.
- Verification guidance now separates focused seam suites from broader end-to-end suites.

## Stable extension guidance

### Add new Java semantics here

Prefer adding new Java semantics in the owning collaborator instead of re-growing the stage:

- type-level semantics -> type declaration / type semantics support
- field-level semantics -> field extraction / field semantics support
- method-level semantics -> method extraction / method semantics support
- traversal coordination -> dispatch flow / dispatch result

### Add new IR dependency views here

Prefer extending the extracted IR collaborators instead of broadening the main composition seam:

- new dependency metadata rules -> dependency metadata enrichment support
- new filtered dependency view families -> dependency view catalog support
- browser-facing handoff rules -> browser dependency view handoff support
- package-level enrichment -> package entity enrichment support
- compatibility-only helpers -> compatibility support

## Verification

Run the narrow seam suites first during local iteration:

```bash
mvn -Dtest=JavaCompilationUnitExtractionFlowTest,JavaTraversalNodeDispatchFlowTest,JavaTypeDeclarationFlowTest,JavaFieldExtractionFlowTest,JavaMethodExtractionFlowTest test
mvn -Dtest=ArchitectureIrDependencyRelationshipEnricherTest,ArchitectureIrDependencyViewAssemblySupportTest,ArchitectureIrDependencyViewCatalogSupportTest,ArchitectureIrBrowserDependencyViewHandoffSupportTest,ArchitectureIrAssemblyCompositionOrchestrationTest test
```

Then run the broader hotspot regressions:

```bash
mvn -Dtest=JavaStageEndToEndFixtureRegressionTest,ArchitectureIrCompositionEndToEndFixtureRegressionTest test
```

Finally run the full suite:

```bash
mvn test
```
