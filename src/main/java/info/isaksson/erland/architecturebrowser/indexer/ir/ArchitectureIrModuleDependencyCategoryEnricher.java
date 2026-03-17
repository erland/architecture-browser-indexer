package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.Map;
import java.util.Objects;

final class ArchitectureIrModuleDependencyCategoryEnricher {
    private ArchitectureIrModuleDependencyCategoryEnricher() {
    }

    static boolean shouldEnrich(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return ArchitectureIrGenericDependencyCategoryEnricher.hasRollup(relationship, "module-module")
            || ArchitectureIrGenericDependencyCategoryEnricher.isModuleDependencyRelationship(relationship, source, target);
    }

    static Map<String, Object> enrich(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        Map<String, Object> metadata,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        metadata.put("dependencyView", "module");
        metadata.put("dependencySourceModuleId", relationship.fromEntityId());
        metadata.put("dependencyTargetModuleId", relationship.toEntityId());
        String sourceModuleName = ArchitectureIrGenericDependencyCategoryEnricher.moduleNameForDependencyEntity(source);
        String targetModuleName = ArchitectureIrGenericDependencyCategoryEnricher.moduleNameForDependencyEntity(target);
        if (sourceModuleName != null) {
            metadata.put("dependencySourceModuleName", sourceModuleName);
            metadata.put("dependencySourceModuleBoundary", ArchitectureIrGenericDependencyCategoryEnricher.moduleBoundaryForName(sourceModuleName, entitiesById));
        }
        if (targetModuleName != null) {
            metadata.put("dependencyTargetModuleName", targetModuleName);
            metadata.put("dependencyTargetModuleBoundary", ArchitectureIrGenericDependencyCategoryEnricher.moduleBoundaryForName(targetModuleName, entitiesById));
            metadata.put("dependencyTargetModuleClassification", ArchitectureIrGenericDependencyCategoryEnricher.moduleClassificationForName(targetModuleName, entitiesById));
        }
        metadata.put("dependencyTargetBoundary", targetModuleName == null ? "unknown" : ArchitectureIrGenericDependencyCategoryEnricher.moduleBoundaryForName(targetModuleName, entitiesById));
        metadata.put("sameModule", Objects.equals(sourceModuleName, targetModuleName));
        return metadata;
    }
}
