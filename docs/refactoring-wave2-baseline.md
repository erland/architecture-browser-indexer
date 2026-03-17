# Refactoring Wave 2 Baseline and Safety Net

This note freezes the intended starting point for the second refactoring wave after `indexer_step13`.

## Target hotspots

1. `extract/TypeScriptDeclarationExtractor`
2. `ir/ArchitectureIrDependencyViewAssemblySupport`
3. `extract/JavaExtractionSemanticsSupport`
4. `extract/FrontendRoutingExtractor`
5. stringly-typed metadata flows across extraction and IR assembly

## Scope of this baseline step

This step does **not** change production behavior. It tightens the safety net around the next refactoring wave by:

- adding narrow seam tests where coverage was still indirect or missing
- softening a few brittle IR list-shape assertions into contract-oriented contains checks
- documenting the intended seams and non-goals before structural changes begin

## What should stay stable during wave 2

### TypeScript declaration extraction
- top-level declarations remain discoverable
- member extraction still emits `CONTAINS` links and declaration/type metadata
- inheritance / implementation dependencies still resolve against named local types

### IR dependency-view assembly
- core dependency entry points stay present
- package/type/module views remain available even if additional framework-aware views are added later

### Java semantics
- JAX-RS, CDI, JPA, and write-path helper semantics remain stable
- constructor/property/event/write-path detection remains available through dedicated helper seams

### Frontend routing
- Angular and React routes still emit route entities and route-target / guard / resolver relationships
- route full-path normalization stays stable for nested routes

### Metadata contracts
- broad regressions should assert architect-facing behavior, not exact incidental list shape or optional convenience flags unless those are the contract under test

## Non-goals

- no large production refactor yet
- no JSON/export contract changes
- no typed metadata migration yet

## Recommended verification

Run the full suite:

```bash
mvn test
```

Useful focused commands during the next wave:

```bash
mvn test -Dtest=TypeScript*Test
mvn test -Dtest=ArchitectureIr*Test
mvn test -Dtest=Java*Test
mvn test -Dtest=*Routing*Test
```
