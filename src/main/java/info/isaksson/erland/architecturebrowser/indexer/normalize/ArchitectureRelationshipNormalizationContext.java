package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.Map;

/**
 * Context given to relationship normalization rules.
 */
public record ArchitectureRelationshipNormalizationContext(
    ArchitectureRelationship relationship,
    Map<String, ArchitectureEntity> entitiesById,
    Map<String, ArchitectureRelationship> relationshipsById
) {
    public ArchitectureRelationshipNormalizationContext {
        entitiesById = entitiesById == null ? Map.of() : Map.copyOf(entitiesById);
        relationshipsById = relationshipsById == null ? Map.of() : Map.copyOf(relationshipsById);
    }
}
