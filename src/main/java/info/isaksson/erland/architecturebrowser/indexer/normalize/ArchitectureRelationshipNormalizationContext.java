package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.Map;
import java.util.Optional;

/**
 * Context given to relationship normalization rules.
 */
public record ArchitectureRelationshipNormalizationContext(
    ArchitectureRelationship relationship,
    Map<String, ArchitectureEntity> entitiesById,
    Map<String, ArchitectureRelationship> relationshipsById,
    FrontendRouteEvidence sourceRouteEvidence,
    FrontendRouteEvidence targetRouteEvidence,
    FrontendRouteEvidence relationshipRouteEvidence,
    FrontendNavigationEvidence frontendNavigationEvidence
) {
    public ArchitectureRelationshipNormalizationContext(
        ArchitectureRelationship relationship,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureRelationship> relationshipsById
    ) {
        this(
            relationship,
            entitiesById,
            relationshipsById,
            FrontendRouteEvidence.fromEntity(entityFor(entitiesById, relationship == null ? null : relationship.fromEntityId())).orElse(null),
            FrontendRouteEvidence.fromEntity(entityFor(entitiesById, relationship == null ? null : relationship.toEntityId())).orElse(null),
            FrontendRouteEvidence.fromRelationship(relationship).orElse(null),
            FrontendNavigationEvidence.fromRelationship(relationship).orElse(null)
        );
    }

    public ArchitectureRelationshipNormalizationContext {
        entitiesById = entitiesById == null ? Map.of() : Map.copyOf(entitiesById);
        relationshipsById = relationshipsById == null ? Map.of() : Map.copyOf(relationshipsById);
    }

    public Optional<ArchitectureEntity> sourceEntity() {
        return Optional.ofNullable(entityFor(entitiesById, relationship == null ? null : relationship.fromEntityId()));
    }

    public Optional<ArchitectureEntity> targetEntity() {
        return Optional.ofNullable(entityFor(entitiesById, relationship == null ? null : relationship.toEntityId()));
    }

    public Optional<FrontendRouteEvidence> sourceRouteEvidenceOptional() {
        return Optional.ofNullable(sourceRouteEvidence);
    }

    public Optional<FrontendRouteEvidence> targetRouteEvidenceOptional() {
        return Optional.ofNullable(targetRouteEvidence);
    }

    public Optional<FrontendRouteEvidence> relationshipRouteEvidenceOptional() {
        return Optional.ofNullable(relationshipRouteEvidence);
    }

    public Optional<FrontendNavigationEvidence> frontendNavigationEvidenceOptional() {
        return Optional.ofNullable(frontendNavigationEvidence);
    }

    private static ArchitectureEntity entityFor(Map<String, ArchitectureEntity> entitiesById, String entityId) {
        if (entitiesById == null || entityId == null || entityId.isBlank()) {
            return null;
        }
        return entitiesById.get(entityId);
    }
}
