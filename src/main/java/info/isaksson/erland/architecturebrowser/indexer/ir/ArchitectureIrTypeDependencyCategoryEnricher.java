package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.Map;

final class ArchitectureIrTypeDependencyCategoryEnricher {
    private ArchitectureIrTypeDependencyCategoryEnricher() {
    }

    static boolean shouldEnrich(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return ArchitectureIrGenericDependencyCategoryEnricher.isTypeDependencyRelationship(relationship, source, target);
    }

    static Map<String, Object> enrich(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        Map<String, Object> metadata,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        metadata.put("dependencyView", "type");
        metadata.put("dependencySourceTypeId", source == null ? relationship.fromEntityId() : source.id());
        metadata.put("dependencyTargetTypeId", target == null ? relationship.toEntityId() : target.id());
        metadata.put("dependencySourceBoundary", ArchitectureIrGenericDependencyCategoryEnricher.boundaryForEntity(source));
        metadata.put("dependencyTargetBoundary", ArchitectureIrGenericDependencyCategoryEnricher.boundaryForEntity(target));
        metadata.put("dependencyTargetInternal", ArchitectureIrGenericDependencyCategoryEnricher.isInternalEntity(target));
        metadata.put("dependencyTargetExternal", ArchitectureIrGenericDependencyCategoryEnricher.isExternalEntity(target));
        metadata.put("dependencyTargetClassification", ArchitectureIrGenericDependencyCategoryEnricher.typeClassificationForEntity(target));
        String sourcePackageName = ArchitectureIrGenericDependencyCategoryEnricher.packageNameForDependencyEntity(source);
        String targetPackageName = ArchitectureIrGenericDependencyCategoryEnricher.packageNameForDependencyEntity(target);
        if (sourcePackageName != null) {
            metadata.put("dependencySourcePackageName", sourcePackageName);
            metadata.put("dependencySourcePackageBoundary", ArchitectureIrGenericDependencyCategoryEnricher.packageBoundaryForName(sourcePackageName, entitiesById));
        }
        if (targetPackageName != null) {
            metadata.put("dependencyTargetPackageName", targetPackageName);
            metadata.put("dependencyTargetPackageBoundary", ArchitectureIrGenericDependencyCategoryEnricher.packageBoundaryForName(targetPackageName, entitiesById));
            metadata.put("dependencyTargetPackageClassification", ArchitectureIrGenericDependencyCategoryEnricher.packageClassificationForName(targetPackageName, entitiesById));
        }
        return metadata;
    }
}
