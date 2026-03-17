package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.List;
import java.util.Map;

final class ArchitectureIrAssemblyCompositionSupport {
    private ArchitectureIrAssemblyCompositionSupport() {
    }

    static ArchitectureIrAssemblyCompositionResult compose(ArchitectureIrAssemblyCompositionInputs inputs) {
        List<ArchitectureRelationship> relationships = ArchitectureIrDependencyRelationshipEnricher.enrichDependencyRelationshipMetadata(
            inputs.relationships(),
            inputs.entitiesById(),
            inputs.observedTypesByQualifiedName()
        );
        relationships = ArchitectureIrSyntheticPackageDependencyRollupBuilder.ensurePackageDependencyRelationships(
            relationships,
            inputs.entitiesById(),
            inputs.observedTypesByQualifiedName()
        );
        Map<String, Object> dependencyViews = ArchitectureIrDependencyViewAssemblySupport.buildDependencyViews(
            new ArchitectureIrDependencyViewAssemblyInputs(
                relationships,
                inputs.entitiesById(),
                inputs.observedTypesByQualifiedName()
            )
        );
        Map<String, ArchitectureEntity> enrichedEntitiesById = ArchitectureIrPackageEntityEnrichmentSupport.enrichPackageEntities(
            inputs.entitiesById(),
            dependencyViews
        );
        return new ArchitectureIrAssemblyCompositionResult(
            relationships,
            dependencyViews,
            enrichedEntitiesById
        );
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
