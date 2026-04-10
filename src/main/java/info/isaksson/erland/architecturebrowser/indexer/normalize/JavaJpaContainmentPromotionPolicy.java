package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

/**
 * JPA-specific containment promotion rules.
 * <p>
 * The surrounding merge pipeline is generic enough to be reused by other frameworks, but these
 * lifecycle and ownership hints are specifically derived from JPA annotations and evidence.
 */
final class JavaJpaContainmentPromotionPolicy {
    private JavaJpaContainmentPromotionPolicy() {}

    static String promotedAssociationKind(
        ArchitectureRelationship canonicalBase,
        ArchitectureRelationship paired,
        String associationCardinality
    ) {
        String existing = JavaJpaAssociationMetadataSupport.normalizedString(existingAssociationKind(canonicalBase, paired));
        if (existing != null && !"association".equals(existing)) {
            return existing;
        }
        if (shouldPromoteToContainment(canonicalBase, paired, associationCardinality)) {
            return "containment";
        }
        return existingAssociationKind(canonicalBase, paired);
    }

    private static String existingAssociationKind(ArchitectureRelationship left, ArchitectureRelationship right) {
        String value = JavaJpaAssociationMetadataSupport.stringValue(left.metadata().get("associationKind"));
        if (value != null) {
            return value;
        }
        return JavaJpaAssociationMetadataSupport.stringValue(right.metadata().get("associationKind"));
    }

    private static boolean shouldPromoteToContainment(
        ArchitectureRelationship canonicalBase,
        ArchitectureRelationship paired,
        String associationCardinality
    ) {
        if (associationCardinality == null || "many-to-many".equals(associationCardinality)) {
            return false;
        }
        ArchitectureRelationship owningSide = owningRelationship(canonicalBase, paired);
        ArchitectureRelationship inverseSide = inverseRelationship(canonicalBase, paired);
        if (owningSide == null || inverseSide == null) {
            return false;
        }
        boolean requiredOwnership = hasRequiredOwnership(owningSide);
        if (!requiredOwnership) {
            return false;
        }
        boolean inverseOrphanRemoval = JavaJpaAssociationMetadataSupport.booleanEvidence(inverseSide, "orphanRemoval");
        boolean inverseCascadeRemove = JavaJpaAssociationMetadataSupport.booleanEvidence(inverseSide, "cascadeRemove")
            || JavaJpaAssociationMetadataSupport.booleanEvidence(inverseSide, "cascadeAll");
        boolean ownerIdentityBound = JavaJpaAssociationMetadataSupport.booleanEvidence(owningSide, "mapsId")
            || JavaJpaAssociationMetadataSupport.booleanEvidence(owningSide, "primaryKeyJoinColumn");
        return switch (associationCardinality) {
            case "one-to-many" -> inverseOrphanRemoval && inverseCascadeRemove;
            case "one-to-one" -> ownerIdentityBound || (inverseOrphanRemoval && inverseCascadeRemove);
            default -> false;
        };
    }

    private static ArchitectureRelationship owningRelationship(ArchitectureRelationship first, ArchitectureRelationship second) {
        if (JavaJpaAssociationMetadataSupport.hasMappedBy(first) && !JavaJpaAssociationMetadataSupport.hasMappedBy(second)) {
            return second;
        }
        if (JavaJpaAssociationMetadataSupport.hasMappedBy(second) && !JavaJpaAssociationMetadataSupport.hasMappedBy(first)) {
            return first;
        }
        return null;
    }

    private static ArchitectureRelationship inverseRelationship(ArchitectureRelationship first, ArchitectureRelationship second) {
        if (JavaJpaAssociationMetadataSupport.hasMappedBy(first) && !JavaJpaAssociationMetadataSupport.hasMappedBy(second)) {
            return first;
        }
        if (JavaJpaAssociationMetadataSupport.hasMappedBy(second) && !JavaJpaAssociationMetadataSupport.hasMappedBy(first)) {
            return second;
        }
        return null;
    }

    private static boolean hasRequiredOwnership(ArchitectureRelationship relationship) {
        if (relationship == null) {
            return false;
        }
        Boolean associationOptional = JavaJpaAssociationMetadataSupport.booleanEvidenceOrNull(relationship, "associationOptional");
        Boolean joinColumnNullable = JavaJpaAssociationMetadataSupport.booleanEvidenceOrNull(relationship, "joinColumnNullable");
        if (Boolean.FALSE.equals(associationOptional) || Boolean.FALSE.equals(joinColumnNullable)) {
            return true;
        }
        return "1".equals(RelationshipMultiplicityBoundsSupport.normalizeBound(
            RelationshipMultiplicityBoundsSupport.multiplicityLowerForEntity(
                relationship,
                relationship.toEntityId(),
                RelationshipMultiplicityBoundsSupport.boundsForRelationship(relationship)
            )
        ));
    }
}
