package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

/**
 * Generic policy for merging multiplicity bounds from two inverse relationships into the canonical
 * orientation selected by a normalization pass.
 */
final class InverseRelationshipBoundMergePolicy {
    private InverseRelationshipBoundMergePolicy() {}

    static RelationshipMultiplicityBoundsSupport.RelationshipEndBounds mergeBounds(
        InverseRelationshipMergeInput canonicalBase,
        InverseRelationshipMergeInput paired
    ) {
        return mergeBounds(
            canonicalBase.relationship(),
            paired.relationship(),
            canonicalBase.associationCardinality(),
            paired.associationCardinality(),
            canonicalBase.bounds(),
            paired.bounds()
        );
    }

    static RelationshipMultiplicityBoundsSupport.RelationshipEndBounds mergeBounds(
        ArchitectureRelationship canonicalBase,
        ArchitectureRelationship paired,
        String canonicalCardinality,
        String pairedCardinality
    ) {
        return mergeBounds(
            canonicalBase,
            paired,
            canonicalCardinality,
            pairedCardinality,
            RelationshipMultiplicityBoundsSupport.boundsForRelationship(canonicalBase),
            RelationshipMultiplicityBoundsSupport.boundsForRelationship(paired)
        );
    }

    static RelationshipMultiplicityBoundsSupport.RelationshipEndBounds mergeBounds(
        ArchitectureRelationship canonicalBase,
        ArchitectureRelationship paired,
        String canonicalCardinality,
        String pairedCardinality,
        RelationshipMultiplicityBoundsSupport.RelationshipEndBounds canonicalBounds,
        RelationshipMultiplicityBoundsSupport.RelationshipEndBounds pairedBounds
    ) {
        if (canonicalBase == null || paired == null) {
            return new RelationshipMultiplicityBoundsSupport.RelationshipEndBounds(null, null, null, null);
        }
        if ("one-to-many".equals(canonicalCardinality) && "many-to-one".equals(pairedCardinality)) {
            return new RelationshipMultiplicityBoundsSupport.RelationshipEndBounds(
                pairedBounds.targetLowerBound(),
                pairedBounds.targetUpperBound(),
                canonicalBounds.targetLowerBound(),
                canonicalBounds.targetUpperBound()
            );
        }
        if ("many-to-one".equals(canonicalCardinality) && "one-to-many".equals(pairedCardinality)) {
            return new RelationshipMultiplicityBoundsSupport.RelationshipEndBounds(
                canonicalBounds.sourceLowerBound(),
                canonicalBounds.sourceUpperBound(),
                pairedBounds.sourceLowerBound(),
                pairedBounds.sourceUpperBound()
            );
        }
        return mergeConservativelyByCanonicalEntity(canonicalBase, paired, canonicalBounds, pairedBounds);
    }

    static RelationshipMultiplicityBoundsSupport.RelationshipEndBounds mergeConservativelyByCanonicalEntity(
        ArchitectureRelationship canonicalBase,
        ArchitectureRelationship paired
    ) {
        return mergeConservativelyByCanonicalEntity(
            canonicalBase,
            paired,
            RelationshipMultiplicityBoundsSupport.boundsForRelationship(canonicalBase),
            RelationshipMultiplicityBoundsSupport.boundsForRelationship(paired)
        );
    }

    static RelationshipMultiplicityBoundsSupport.RelationshipEndBounds mergeConservativelyByCanonicalEntity(
        ArchitectureRelationship canonicalBase,
        ArchitectureRelationship paired,
        RelationshipMultiplicityBoundsSupport.RelationshipEndBounds canonicalBounds,
        RelationshipMultiplicityBoundsSupport.RelationshipEndBounds pairedBounds
    ) {
        return new RelationshipMultiplicityBoundsSupport.RelationshipEndBounds(
            RelationshipMultiplicityBoundsSupport.combineLowerBound(
                RelationshipMultiplicityBoundsSupport.multiplicityLowerForEntity(canonicalBase, canonicalBase.fromEntityId(), canonicalBounds),
                RelationshipMultiplicityBoundsSupport.multiplicityLowerForEntity(paired, canonicalBase.fromEntityId(), pairedBounds)
            ),
            RelationshipMultiplicityBoundsSupport.combineUpperBound(
                RelationshipMultiplicityBoundsSupport.multiplicityUpperForEntity(canonicalBase, canonicalBase.fromEntityId(), canonicalBounds),
                RelationshipMultiplicityBoundsSupport.multiplicityUpperForEntity(paired, canonicalBase.fromEntityId(), pairedBounds)
            ),
            RelationshipMultiplicityBoundsSupport.combineLowerBound(
                RelationshipMultiplicityBoundsSupport.multiplicityLowerForEntity(canonicalBase, canonicalBase.toEntityId(), canonicalBounds),
                RelationshipMultiplicityBoundsSupport.multiplicityLowerForEntity(paired, canonicalBase.toEntityId(), pairedBounds)
            ),
            RelationshipMultiplicityBoundsSupport.combineUpperBound(
                RelationshipMultiplicityBoundsSupport.multiplicityUpperForEntity(canonicalBase, canonicalBase.toEntityId(), canonicalBounds),
                RelationshipMultiplicityBoundsSupport.multiplicityUpperForEntity(paired, canonicalBase.toEntityId(), pairedBounds)
            )
        );
    }
}
