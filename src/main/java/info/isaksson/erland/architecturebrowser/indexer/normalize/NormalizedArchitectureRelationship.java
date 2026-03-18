package info.isaksson.erland.architecturebrowser.indexer.normalize;

import java.util.List;

/**
 * Internal normalization result for one relationship.
 */
public record NormalizedArchitectureRelationship(
    List<String> architecturalSemantics
) {
    public static final NormalizedArchitectureRelationship EMPTY = new NormalizedArchitectureRelationship(List.of());
}
