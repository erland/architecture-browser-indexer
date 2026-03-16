# Refactoring Step 7 — Split TopologyService

## Goal
Reduce `TopologyService` from a multi-responsibility class into smaller collaborators while preserving topology inference behavior.

## What changed

### New collaborators
- `TopologyInferenceState`
  - shared mutable state for inferred scopes, entities, relationships, and diagnostics during one inference run
- `TopologyPaths`
  - shared topology path/package/source-root helper logic
- `TopologyScopeInferenceService`
  - directory/module/package scope inference and containment relationship creation
- `TopologyScopeInferenceContext`
  - package/entity mapping context produced by scope inference and consumed by later rollup stages
- `TopologyRelationshipRollupService`
  - internal target resolution and file/entity/package/module dependency rollups

### TopologyService after the split
`TopologyService` is now primarily orchestration:
1. create inference state
2. infer scopes/entities/containment relationships
3. infer dependency rollups
4. build `TopologySummary`
5. return `TopologyResult`

## Behavioral intent
This step is intended to be a structural refactor only:
- keep the public `TopologyService.infer(...)` API stable
- preserve existing topology output and metadata semantics
- preserve TypeScript import-evidence handling
- preserve Java/TypeScript package and module rollups

## Follow-up opportunities
- extract package/module rollup deduplication keys into their own helper/model
- introduce focused unit tests directly for `TopologyScopeInferenceService` and `TopologyRelationshipRollupService`
- consider whether `InterpretationResult` should participate in future topology enrichment or be removed from the method signature if it remains unused
