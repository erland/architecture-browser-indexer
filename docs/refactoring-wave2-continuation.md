# Refactoring Wave 2 Continuation Notes

## What Wave 2 accomplished

Wave 2 moved the project from hotspot reduction into a more modular steady state.

The main structural changes are now in place:

- TypeScript declaration handling has explicit discovery and metadata-shaping seams
- Angular and React enrichment paths are separated
- IR dependency-view assembly is phase-based
- dependency enrichment is split by family
- Java semantics are organized by domain
- frontend routing is phase-based
- typed intermediate metadata models now protect some of the highest-value metadata flows
- broad regressions are more contract-oriented

## What to avoid next

Avoid immediately collapsing the new collaborators back into larger helpers for convenience.

In particular:

- do not add new TypeScript declaration-family logic back into `TypeScriptDeclarationExtractor`
- do not reintroduce dependency-view shaping into `ArchitectureIrDependencyViewAssemblySupport`
- do not grow `JavaExtractionSemanticsSupport` back into the implementation center
- do not re-mix route discovery and route emission logic inside `FrontendRoutingExtractor`
- do not add new broad end-to-end assertions that depend on exact list order or incidental metadata-map shape

## Best next refactor directions

1. Target the densest remaining TypeScript helpers first.
2. Expand typed models to additional high-value metadata paths before introducing more new stringly fields.
3. Retire compatibility facades only when the replacement seams are already broadly adopted.
4. Keep adding contract helpers whenever a regression test starts depending on internal representation details.

## Good insertion points for future work

### TypeScript extraction

Start from:
- `TypeScriptDeclarationExtractor`
- `TypeScriptDeclarationDiscoverySupport`
- `TypeScriptNamedDeclarationSemanticsSupport`
- `TypeScriptMethodDeclarationSemanticsSupport`
- `TypeScriptPropertyDeclarationSemanticsSupport`
- `AngularTypeScriptFrameworkEnrichmentSupport`
- `ReactTypeScriptFrameworkEnrichmentSupport`

### IR assembly

Start from:
- `ArchitectureIrDependencyViewAssemblySupport`
- `ArchitectureIrDependencyNormalizationSupport`
- `ArchitectureIrTypeDependencyViewBuilder`
- `ArchitectureIrPackageDependencyViewBuilder`
- `ArchitectureIrModuleDependencyViewBuilder`
- `ArchitectureIrEvidenceDependencyViewBuilder`
- `ArchitectureIrDependencyRelationshipEnricher`
- typed metadata models in `ir`

### Java semantics

Start from:
- `JavaGenericSyntaxSupport`
- `JavaJpaDomainSemanticsSupport`
- `JavaJaxRsDomainSemanticsSupport`
- `JavaCdiDomainSemanticsSupport`
- `JavaWritePathDetectionSupport`

### Frontend routing

Start from:
- `FrontendRoutingExtractor`
- `FrontendRouteDiscoverySupport`
- `FrontendRoutePathNormalizationSupport`
- `FrontendRouteEmissionSupport`

## Suggested next-chat checklist

When continuing in a future chat:

1. name the targeted hotspot explicitly
2. identify the owning orchestrator and the phase/domain collaborator that should receive new logic
3. add or tighten one narrow seam test first
4. make the structural change
5. only then adjust broad regressions if the contract actually changed
