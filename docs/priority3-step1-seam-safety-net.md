# Priority 3 — Step 1: seam-oriented safety-net inventory

This note freezes the current baseline before continuing the split of the two largest remaining core classes:

- `extract/JavaStructuralExtractor`
- `ir/ArchitectureIrFactory`

The goal is to make the current seams explicit so later refactors can move code without accidentally changing behavior.

## JavaStructuralExtractor seams protected in this step

### Declaration and containment seam
Protected by:
- `JavaStructuralExtractorSeamSafetyNetTest.preservesDeclarationContainmentAndMemberMetadataAcrossFutureSplits`
- existing supporting coverage in `StructuralExtractionServiceTest`

What it protects:
- class entity creation
- field entity creation
- method entity creation
- owner-qualified-name metadata
- declared field type metadata
- method return/parameter metadata
- `CONTAINS` relationships from owning type to field/method entities

### Hierarchy and declared-type dependency seam
Protected by:
- `JavaStructuralExtractorSeamSafetyNetTest.preservesHierarchyAndDeclaredTypeDependencyMetadataAcrossFutureSplits`
- existing supporting coverage in `StructuralExtractionServiceTest`

What it protects:
- `EXTENDS` relationship emission
- `IMPLEMENTS` relationship emission
- dependency-source metadata for hierarchy edges
- dependency-category metadata for hierarchy edges
- declared field-type dependency emission
- parameter/return-type dependency emission

### Framework-specific Java seams already protected
Protected by existing focused tests:
- `JavaJaxRsStructuralExtractionTest`
- `JavaJpaStructuralExtractionTest`
- `JavaCdiStructuralExtractionTest`
- `JavaWritePathStructuralExtractionTest`

These continue to protect:
- JAX-RS endpoint extraction
- JPA type/field/method metadata and relationships
- CDI publish/observe graph extraction
- write-path extraction

## ArchitectureIrFactory seams protected in this step

### Entity/relationship metadata mapping seam
Protected by:
- `ArchitectureIrFactorySeamSafetyNetTest.preservesNestedEntityAndRelationshipMetadataWhenMappingIntoFinalIr`

What it protects:
- entity metadata copied into the final IR document
- relationship metadata copied into the final IR document
- preservation of nested maps/lists during mapping
- stable handling of source references while IR assembly is further split

### Browser/dependency-view shaping seam
Protected by:
- `ArchitectureIrFactorySeamSafetyNetTest.buildsPackageMetricsBoundarySummaryAndBrowserViewCatalogForJavaBackendFixture`
- existing supporting coverage in `ArchitectureIrFactoryJavaBackendSafetyNetTest`

What it protects:
- package metrics presence in dependency views
- boundary summary presence and expected keys
- browser view catalog presence for Java backend fixture output
- continued availability of Java browser-view IDs while factory logic is split further

## Recommended targeted verification

Run these first during future split work:

```bash
mvn test -Dtest=JavaStructuralExtractorSeamSafetyNetTest
mvn test -Dtest=JavaJaxRsStructuralExtractionTest,JavaJpaStructuralExtractionTest,JavaCdiStructuralExtractionTest,JavaWritePathStructuralExtractionTest
mvn test -Dtest=ArchitectureIrFactorySeamSafetyNetTest,ArchitectureIrFactoryJavaBackendSafetyNetTest
```

Then run the broader regression suites:

```bash
mvn test -Dtest=*Java*RegressionTest
mvn test -Dtest=*ArchitectureIrFactory*
```
