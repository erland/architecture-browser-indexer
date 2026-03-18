package info.isaksson.erland.architecturebrowser.indexer.normalize;

import java.util.List;

/**
 * Internal normalization result for one entity.
 */
public record NormalizedArchitectureEntity(
    List<String> architecturalRoles,
    List<String> architecturalTraits
) {
    public static final NormalizedArchitectureEntity EMPTY = new NormalizedArchitectureEntity(List.of(), List.of());
}
