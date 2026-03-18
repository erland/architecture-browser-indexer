package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;

import java.util.Map;

/**
 * Context given to entity normalization rules.
 */
public record ArchitectureEntityNormalizationContext(
    ArchitectureEntity entity,
    Map<String, ArchitectureEntity> entitiesById
) {
    public ArchitectureEntityNormalizationContext {
        entitiesById = entitiesById == null ? Map.of() : Map.copyOf(entitiesById);
    }
}
