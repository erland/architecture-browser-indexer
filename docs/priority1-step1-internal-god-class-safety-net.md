# Priority 1 — Step 1: freeze the current behavior of the two internal god classes

This step adds focused safety nets around the two remaining large internal implementation classes:

- `extract/JavaSyntaxTreeExtractionStage`
- `ir/ArchitectureIrAssemblyCompositionSupport`

The goal is not to change behavior. The goal is to lock down the current seams before the next split work starts.

## New safety-net coverage added in this step

### Java syntax-tree extraction stage
Protected by:
- `JavaSyntaxTreeExtractionStageSafetyNetTest.preservesStageLevelTypeMemberAndDependencyExtraction`
- `JavaSyntaxTreeExtractionStageSafetyNetTest.preservesStageLevelJaxRsAndJpaSemanticMetadata`

What these tests protect:
- file visit/extract counting at the stage entry
- declared type, field, and method extraction from syntax-tree input
- type containment relationships
- hierarchy and declared-type dependency metadata
- JAX-RS resource and endpoint metadata
- JPA association metadata and relationship emission

### IR assembly composition support
Protected by:
- `ArchitectureIrAssemblyCompositionSupportSafetyNetTest.preservesObservedTypeCanonicalizationAndDependencyMetadataEnrichment`
- `ArchitectureIrAssemblyCompositionSupportSafetyNetTest.preservesSyntheticPackageRollupsDependencyViewsAndScopeNormalization`

What these tests protect:
- observed-type canonicalization for dependency enrichment
- type-vs-evidence dependency metadata shaping
- synthetic package rollup creation
- dependency view assembly for package/module summaries
- package metric enrichment back into package entities
- scope-id normalization behavior used during final assembly

## Why these tests matter before further shrinking

The next Priority 1 steps will split traversal, member extraction, dependency emission, and IR assembly composition into narrower collaborators. These new tests keep the current internal contracts visible so that later refactors can move code without accidentally changing:

- the Java extraction output shape
- the dependency metadata contract in the final IR
- package/module rollup behavior
- stage-level framework semantics for Java backend analysis

## Recommended verification order

Run the new internal-class safety nets first:

```bash
mvn -Dtest=JavaSyntaxTreeExtractionStageSafetyNetTest,ArchitectureIrAssemblyCompositionSupportSafetyNetTest test
```

Then run the previously added seam-hardening tests:

```bash
mvn -Dtest=JavaExtractionSeamHardeningTest,ArchitectureIrSeamHardeningTest test
```

Then run the hotspot-adjacent suites before continuing with the actual shrink steps:

```bash
mvn -Dtest=JavaDeclarationDiscoveryTest,JavaEntityMapperTest,JavaRelationshipEvidenceEmitterTest,JavaJpaCdiWritePathDetailSupportTest test
mvn -Dtest=ArchitectureIrFactorySeamSafetyNetTest,ArchitectureIrDependencyMetadataSupportTest,ArchitectureIrPackageMetricsBoundaryBuilderTest test
```
