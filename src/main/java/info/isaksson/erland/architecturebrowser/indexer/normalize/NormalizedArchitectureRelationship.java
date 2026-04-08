package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.NormalizedAssociation;

import java.util.List;

/**
 * Internal normalization result for one relationship.
 */
public record NormalizedArchitectureRelationship(
    List<String> architecturalSemantics,
    NormalizedAssociation normalizedAssociation
) {
    public NormalizedArchitectureRelationship(List<String> architecturalSemantics) {
        this(architecturalSemantics, null);
    }

    public static final NormalizedArchitectureRelationship EMPTY = new NormalizedArchitectureRelationship(List.of(), null);
}
