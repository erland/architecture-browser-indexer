# Priority 1 Step 6 — Split dependency relationship enrichment out of IR assembly composition

Implemented changes:
- moved dependency relationship enrichment into `ArchitectureIrDependencyRelationshipEnricher`
- updated `ArchitectureIrAssemblyStateBuilder` to call the dedicated enricher directly
- removed the enrichment method from `ArchitectureIrAssemblyCompositionSupport` so that class stays focused on dependency-view assembly, synthetic rollups, and package/scope enrichment

Intent:
- keep the IR contract stable
- narrow the responsibilities of `ArchitectureIrAssemblyCompositionSupport`
- create a dedicated seam for future refinement of dependency metadata enrichment without touching the broader assembly-composition logic
