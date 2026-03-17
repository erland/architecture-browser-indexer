package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ArchitectureIrSyntheticPackageDependencyRollupBuilder {
    private ArchitectureIrSyntheticPackageDependencyRollupBuilder() {
    }

    static List<ArchitectureRelationship> ensurePackageDependencyRelationships(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        Map<String, ArchitectureRelationship> byId = new LinkedHashMap<>();
        for (ArchitectureRelationship relationship : relationships) {
            byId.put(relationship.id(), relationship);
        }

        Map<String, ArchitectureRelationship> synthetic = new LinkedHashMap<>();
        for (ArchitectureRelationship relationship : relationships) {
            ArchitectureEntity source = ArchitectureIrAssemblyCompatibilitySupport.canonicalDependencyEntity(
                entitiesById.get(relationship.fromEntityId()),
                observedTypesByQualifiedName
            );
            ArchitectureEntity target = ArchitectureIrAssemblyCompatibilitySupport.canonicalDependencyEntity(
                entitiesById.get(relationship.toEntityId()),
                observedTypesByQualifiedName
            );
            if (!ArchitectureIrAssemblyCompatibilitySupport.isTypeDependencyRelationship(relationship, source, target)) {
                continue;
            }
            String sourcePackageName = ArchitectureIrAssemblyCompatibilitySupport.packageNameForDependencyEntity(source);
            String targetPackageName = ArchitectureIrAssemblyCompatibilitySupport.packageNameForDependencyEntity(target);
            if (sourcePackageName == null || targetPackageName == null || sourcePackageName.equals(targetPackageName)) {
                continue;
            }
            String sourcePackageEntityId = ArchitectureIrAssemblyCompatibilitySupport.findPackageEntityIdByName(sourcePackageName, entitiesById);
            String targetPackageEntityId = ArchitectureIrAssemblyCompatibilitySupport.findPackageEntityIdByName(targetPackageName, entitiesById);
            if (sourcePackageEntityId == null || targetPackageEntityId == null || sourcePackageEntityId.equals(targetPackageEntityId)) {
                continue;
            }
            String syntheticId = IdUtils.relationshipId("ir-package-uses", sourcePackageEntityId, targetPackageEntityId, "");
            if (byId.containsKey(syntheticId) || synthetic.containsKey(syntheticId)) {
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("rollup", "package-package");
            metadata.put("dependencyView", "package");
            metadata.put("dependencySourcePackageId", sourcePackageEntityId);
            metadata.put("dependencyTargetPackageId", targetPackageEntityId);
            metadata.put("dependencySourcePackageName", sourcePackageName);
            metadata.put("dependencyTargetPackageName", targetPackageName);
            metadata.put("dependencySourcePackageBoundary", ArchitectureIrAssemblyCompatibilitySupport.packageBoundaryForName(sourcePackageName, entitiesById));
            metadata.put("dependencyTargetPackageBoundary", ArchitectureIrAssemblyCompatibilitySupport.packageBoundaryForName(targetPackageName, entitiesById));
            metadata.put("dependencyTargetBoundary", ArchitectureIrAssemblyCompatibilitySupport.packageBoundaryForName(targetPackageName, entitiesById));
            metadata.put("dependencyTargetPackageClassification", ArchitectureIrAssemblyCompatibilitySupport.packageClassificationForName(targetPackageName, entitiesById));
            if (relationship.metadata() != null) {
                Object dependencySource = relationship.metadata().get("dependencySource");
                Object dependencyCategory = relationship.metadata().get("dependencyCategory");
                if (dependencySource != null) {
                    metadata.put("dependencySource", dependencySource);
                }
                if (dependencyCategory != null) {
                    metadata.put("dependencyCategory", dependencyCategory);
                }
            }
            synthetic.put(syntheticId, new ArchitectureRelationship(
                syntheticId,
                RelationshipKind.USES,
                sourcePackageEntityId,
                targetPackageEntityId,
                relationship.label(),
                relationship.sourceRefs(),
                ArchitectureIrDependencyMetadataSupport.immutable(metadata)
            ));
        }

        if (synthetic.isEmpty()) {
            return relationships;
        }
        List<ArchitectureRelationship> merged = new ArrayList<>(relationships.size() + synthetic.size());
        merged.addAll(relationships);
        merged.addAll(synthetic.values());
        return List.copyOf(merged);
    }

}
