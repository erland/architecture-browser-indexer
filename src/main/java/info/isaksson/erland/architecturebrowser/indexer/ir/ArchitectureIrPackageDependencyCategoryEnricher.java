package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.Map;

final class ArchitectureIrPackageDependencyCategoryEnricher {
    private ArchitectureIrPackageDependencyCategoryEnricher() {
    }

    static boolean shouldEnrich(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return ArchitectureIrGenericDependencyCategoryEnricher.hasRollup(relationship, "package-package")
            || ArchitectureIrGenericDependencyCategoryEnricher.isPackageDependencyRelationship(relationship, source, target);
    }

    static Map<String, Object> enrich(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        Map<String, Object> metadata,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        metadata.put("dependencyView", "package");
        metadata.put("dependencySourcePackageId", relationship.fromEntityId());
        metadata.put("dependencyTargetPackageId", relationship.toEntityId());
        String sourcePackageName = source == null ? null : source.name();
        String targetPackageName = target == null ? null : target.name();
        if (sourcePackageName != null) {
            metadata.put("dependencySourcePackageName", sourcePackageName);
            metadata.put("dependencySourcePackageBoundary", ArchitectureIrGenericDependencyCategoryEnricher.packageBoundaryForName(sourcePackageName, entitiesById));
        }
        if (targetPackageName != null) {
            metadata.put("dependencyTargetPackageName", targetPackageName);
            metadata.put("dependencyTargetPackageBoundary", ArchitectureIrGenericDependencyCategoryEnricher.packageBoundaryForName(targetPackageName, entitiesById));
            metadata.put("dependencyTargetPackageClassification", ArchitectureIrGenericDependencyCategoryEnricher.packageClassificationForName(targetPackageName, entitiesById));
        }
        metadata.put("dependencyTargetBoundary", targetPackageName == null ? "unknown" : ArchitectureIrGenericDependencyCategoryEnricher.packageBoundaryForName(targetPackageName, entitiesById));
        return metadata;
    }
}
