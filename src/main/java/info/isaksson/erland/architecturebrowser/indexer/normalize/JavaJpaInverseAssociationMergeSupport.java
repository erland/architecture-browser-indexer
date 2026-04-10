package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Merges high-confidence inverse JPA field-level associations into one canonical relationship.
 */
final class JavaJpaInverseAssociationMergeSupport {
    private JavaJpaInverseAssociationMergeSupport() {}

    static List<ArchitectureRelationship> mergeInverseJpaAssociations(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        if (relationships == null || relationships.isEmpty()) {
            return List.of();
        }
        List<ArchitectureRelationship> merged = new ArrayList<>();
        Set<String> consumed = new LinkedHashSet<>();
        JavaJpaInverseAssociationIndex index = JavaJpaInverseAssociationIndex.build(relationships);
        for (ArchitectureRelationship relationship : relationships) {
            if (relationship == null || consumed.contains(relationship.id())) {
                continue;
            }
            ArchitectureRelationship inverse = JavaJpaInverseAssociationPairingSupport.findInversePair(
                relationship,
                index,
                consumed
            );
            if (inverse == null) {
                merged.add(JavaJpaExplicitAssociationHandlingSupport.explicitlyHandledRelationship(relationship, index));
                consumed.add(relationship.id());
                continue;
            }
            ArchitectureRelationship canonical = JavaJpaNormalizedAssociationAssembler.canonicalRelationship(
                relationship,
                inverse,
                entitiesById
            );
            merged.add(canonical);
            consumed.add(relationship.id());
            consumed.add(inverse.id());
        }
        return List.copyOf(merged);
    }
}
