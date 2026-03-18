package info.isaksson.erland.architecturebrowser.indexer.normalize;

/**
 * Maps language/framework-specific evidence into canonical architectural roles and traits.
 */
@FunctionalInterface
public interface ArchitectureEntityNormalizationRule {
    NormalizedArchitectureEntity normalize(ArchitectureEntityNormalizationContext context);
}
