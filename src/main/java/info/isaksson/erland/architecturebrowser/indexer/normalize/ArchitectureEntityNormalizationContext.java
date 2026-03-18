package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;

import java.util.Map;
import java.util.Optional;

/**
 * Context given to entity normalization rules.
 */
public record ArchitectureEntityNormalizationContext(
    ArchitectureEntity entity,
    Map<String, ArchitectureEntity> entitiesById,
    FrontendRouteEvidence frontendRouteEvidence
) {
    public ArchitectureEntityNormalizationContext(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        this(entity, entitiesById, FrontendRouteEvidence.fromEntity(entity).orElse(null));
    }

    public ArchitectureEntityNormalizationContext {
        entitiesById = entitiesById == null ? Map.of() : Map.copyOf(entitiesById);
    }

    public Optional<ArchitectureEntity> entityById(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(entitiesById.get(entityId));
    }

    public Optional<FrontendRouteEvidence> frontendRouteEvidenceOptional() {
        return Optional.ofNullable(frontendRouteEvidence);
    }
}
