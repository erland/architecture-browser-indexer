package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ArchitectureIrDependencyRelationshipEnricher {
    private ArchitectureIrDependencyRelationshipEnricher() {
    }

    static List<ArchitectureRelationship> enrichDependencyRelationshipMetadata(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        List<ArchitectureRelationship> enriched = new ArrayList<>(relationships.size());
        for (ArchitectureRelationship relationship : relationships) {
            ArchitectureEntity source = canonicalDependencyEntity(entitiesById.get(relationship.fromEntityId()), observedTypesByQualifiedName);
            ArchitectureEntity target = canonicalDependencyEntity(entitiesById.get(relationship.toEntityId()), observedTypesByQualifiedName);
            boolean genericDependency = ArchitectureIrGenericDependencyCategoryEnricher.shouldEnrich(relationship, source, target);
            boolean evidenceDependency = ArchitectureIrDependencyEvidenceEnricher.shouldEnrich(relationship, source, target);
            if (!genericDependency && !evidenceDependency) {
                enriched.add(relationship);
                continue;
            }
            Map<String, Object> metadata = ArchitectureIrDependencyMetadataSupport.mutableCopy(relationship.metadata());
            if (genericDependency) {
                metadata = ArchitectureIrGenericDependencyCategoryEnricher.enrich(
                    relationship,
                    source,
                    target,
                    metadata,
                    entitiesById
                );
            }
            if (evidenceDependency) {
                metadata = ArchitectureIrDependencyEvidenceEnricher.enrich(relationship, source, target, metadata);
            }
            enriched.add(new ArchitectureRelationship(
                relationship.id(),
                relationship.kind(),
                relationship.fromEntityId(),
                relationship.toEntityId(),
                relationship.label(),
                relationship.sourceRefs(),
                ArchitectureIrDependencyMetadataSupport.immutable(metadata)
            ));
        }
        return List.copyOf(enriched);
    }

    private static ArchitectureEntity canonicalDependencyEntity(
        ArchitectureEntity entity,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        if (entity == null || entity.origin() == EntityOrigin.OBSERVED) {
            return entity;
        }
        String qualifiedName = entity.metadata() == null ? null : (entity.metadata().get("qualifiedName") instanceof String q && !q.isBlank() ? q : null);
        if (qualifiedName == null || qualifiedName.isBlank()) {
            String name = entity.name();
            qualifiedName = (name == null || name.isBlank()) ? null : name;
        }
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return entity;
        }
        return observedTypesByQualifiedName.getOrDefault(qualifiedName, entity);
    }
}
