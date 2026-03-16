# Priority 3 Step 10 — Final hardening pass for the new seams

## What this step adds

This step does not introduce new functionality. It hardens the collaborator seams introduced in Priority 3 and makes the intended extension points explicit before resuming feature work.

## Final collaborator map

### Java extraction

`JavaStructuralExtractor`
- public `StructuralExtractor` entry point only
- delegates extraction to `JavaSyntaxTreeExtractionStage`

`JavaSyntaxTreeExtractionStage`
- stage orchestration for Java tree traversal and extraction flow
- coordinates declaration discovery, entity mapping, relationship emission, and semantic detail helpers

`JavaDeclarationDiscovery`
- discovers declared Java types and keeps qualified/simple-name lookup stable

`JavaEntityMapper`
- maps discovered types, fields, and methods into extracted entities

`JavaRelationshipEvidenceEmitter`
- emits hierarchy and declared-type dependency relationships
- owns dependency metadata/evidence shaping for those relationships

`JavaJpaDetailSupport`
- JPA association and persistence detail analysis

`JavaCdiDetailSupport`
- CDI publish/observe detail analysis

`JavaWritePathDetailSupport`
- write-path detection from Java method bodies

### IR assembly

`ArchitectureIrFactory`
- stable public orchestration entry point for document creation
- delegates detailed assembly work to focused builders/support helpers

`ArchitectureIrAssemblyStateBuilder`
- creates the intermediate assembly state

`ArchitectureIrAssemblyCompositionSupport`
- low-level assembly composition, dependency enrichment, synthetic rollups, and package enrichment

`ArchitectureIrBrowserViewMetadataBuilder`
- browser/view family and descriptor shaping

`ArchitectureIrPackageMetricsBoundaryBuilder`
- package metrics and boundary summary generation

`ArchitectureIrDependencyMetadataSupport`
- metadata-map copying/finalization and dependency-evidence shaping helpers

`ArchitectureIrDocumentMetadataBuilder`
- final document metadata assembly

`ArchitectureIrRunMetadataBuilder`
- run metadata shaping

`ArchitectureIrCompletenessNotesBuilder`
- completeness note defaults

## Where future behavior should be added

### Add behavior here

- new Java declaration discovery rules: `JavaDeclarationDiscovery`
- new Java entity metadata or declaration-to-entity mapping: `JavaEntityMapper`
- new Java hierarchy/type relationship logic: `JavaRelationshipEvidenceEmitter`
- new JPA/CDI/write-path semantic detail heuristics: the corresponding `Java*DetailSupport` helper
- new browser-view families or view descriptor shaping: `ArchitectureIrBrowserViewMetadataBuilder`
- new package metrics or boundary-summary shaping: `ArchitectureIrPackageMetricsBoundaryBuilder`
- new dependency metadata/evidence conventions: `ArchitectureIrDependencyMetadataSupport`

### Avoid adding behavior here

- do not grow `JavaStructuralExtractor` back into a large mixed implementation class
- do not add low-level Java extraction logic directly to `JavaStructuralExtractor`
- do not grow `ArchitectureIrFactory` back into the main home for browser-view, metrics, or dependency-metadata shaping
- do not bypass the focused builders/helpers unless there is a strong public-API reason

## Hardening tests added in this step

### Java seam hardening
- `JavaExtractionSeamHardeningTest`
  - keeps declaration discovery, entity mapping, relationship emission, and semantic-helper contracts aligned on small focused fixtures

### IR seam hardening
- `ArchitectureIrSeamHardeningTest`
  - keeps browser-view ids/families stable
  - keeps package metrics and boundary-summary keys stable
  - keeps dependency metadata/evidence shaping stable

## Suggested verification order

Run focused tests first:

```bash
mvn -Dtest=JavaExtractionSeamHardeningTest,ArchitectureIrSeamHardeningTest test
```

Then run the seam and hotspot-oriented tests already added earlier:

```bash
mvn -Dtest=JavaDeclarationDiscoveryTest,JavaEntityMapperTest,JavaRelationshipEvidenceEmitterTest,JavaJpaCdiWritePathDetailSupportTest test
mvn -Dtest=ArchitectureIrFactorySeamSafetyNetTest,ArchitectureIrPackageMetricsBoundaryBuilderTest,ArchitectureIrDependencyMetadataSupportTest test
```

Then run the full suite before resuming feature work:

```bash
mvn test
```

## Exit assessment

Priority 3 can now be considered structurally complete once the full Maven suite is green in a normal development environment. At that point the remaining work should be maintenance-only cleanup or future functionality, not more hotspot-driven refactoring of these two classes.
