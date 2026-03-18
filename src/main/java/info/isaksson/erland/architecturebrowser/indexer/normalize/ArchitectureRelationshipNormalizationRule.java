package info.isaksson.erland.architecturebrowser.indexer.normalize;

/**
 * Maps language/framework-specific evidence into canonical architectural relationship semantics.
 */
@FunctionalInterface
public interface ArchitectureRelationshipNormalizationRule {
    NormalizedArchitectureRelationship normalize(ArchitectureRelationshipNormalizationContext context);
}
