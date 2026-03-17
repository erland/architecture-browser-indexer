package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.List;
import java.util.Map;

final class ArchitectureIrAssemblyCompositionSupport {
    private ArchitectureIrAssemblyCompositionSupport() {
    }

    static List<ArchitectureRelationship> ensurePackageDependencyRelationships(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        return ArchitectureIrSyntheticPackageDependencyRollupBuilder.ensurePackageDependencyRelationships(
            relationships,
            entitiesById,
            observedTypesByQualifiedName
        );
    }

    static Map<String, ArchitectureEntity> enrichPackageEntities(
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, Object> dependencyViews
    ) {
        return ArchitectureIrPackageEntityEnrichmentSupport.enrichPackageEntities(entitiesById, dependencyViews);
    }

    static List<ArchitectureRelationship> enrichDependencyRelationshipMetadata(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        return ArchitectureIrDependencyRelationshipEnricher.enrichDependencyRelationshipMetadata(
            relationships,
            entitiesById,
            observedTypesByQualifiedName
        );
    }

    static Map<String, Object> buildDependencyViews(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        return buildDependencyViews(new ArchitectureIrDependencyViewAssemblyInputs(
            relationships,
            entitiesById,
            observedTypesByQualifiedName
        ));
    }

    static Map<String, Object> buildDependencyViews(ArchitectureIrDependencyViewAssemblyInputs inputs) {
        return ArchitectureIrDependencyViewAssemblySupport.buildDependencyViews(inputs);
    }
}
