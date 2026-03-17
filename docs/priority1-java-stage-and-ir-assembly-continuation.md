# Priority 1 continuation notes

## What should happen next

The urgent hotspot refactor is done. The next work in this area should be intentionally small.

Preferred order:

1. Keep seam tests aligned with stable contracts only.
2. Remove only obvious duplication inside extracted helpers.
3. Add new architect-facing semantics in the owning collaborator.
4. Expand fixtures when new semantics become user-visible.
5. Avoid broad pipeline or IR contract changes unless a separate plan is created first.

## What to avoid

Avoid these regressions:

- putting semantic helper logic back into `JavaSyntaxTreeExtractionStage`
- adding new browser/dependency shaping directly into `ArchitectureIrAssemblyCompositionSupport`
- making end-to-end tests depend on brittle qualified-name or internal boolean details when a relationship-level assertion is more stable
- mixing compatibility-only helpers back into the main composition seam

## Practical navigation guide

For Java-stage changes start with:

- `extract/JavaStageCompositionSupport`
- `extract/JavaCompilationUnitExtractionFlow`
- `extract/JavaTraversalNodeDispatchFlow`
- `extract/JavaTypeDeclarationFlow`
- `extract/JavaFieldExtractionFlow`
- `extract/JavaMethodExtractionFlow`
- `extract/JavaExtractionSemanticsSupport`

For IR composition changes start with:

- `ir/ArchitectureIrAssemblyCompositionSupport`
- `ir/ArchitectureIrAssemblyCompositionInputs`
- `ir/ArchitectureIrAssemblyCompositionResult`
- `ir/ArchitectureIrDependencyViewAssemblySupport`
- `ir/ArchitectureIrDependencyViewCatalogSupport`
- `ir/ArchitectureIrBrowserDependencyViewHandoffSupport`
- `ir/ArchitectureIrAssemblyCompatibilitySupport`

## Acceptance view

This refactoring track can now be considered complete when:

- narrow seam tests stay green
- hotspot end-to-end regressions stay green
- no new behavior is being routed through the old god-class seams
- new semantics are added in the owning extracted collaborator
