package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.List;
import java.util.Objects;
import java.util.Set;

final class JavaJpaInverseAssociationPairingSupport {
    private JavaJpaInverseAssociationPairingSupport() {}

    static ArchitectureRelationship findInversePair(
        ArchitectureRelationship relationship,
        List<ArchitectureRelationship> relationships,
        Set<String> consumed
    ) {
        InverseRelationshipMergeInput relationshipInput = JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(relationship);
        for (ArchitectureRelationship candidate : relationships) {
            if (candidate == null || Objects.equals(candidate.id(), relationship.id()) || consumed.contains(candidate.id())) {
                continue;
            }
            InverseRelationshipMergeInput candidateInput = JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(candidate);
            if (isInversePair(relationshipInput, candidateInput)) {
                return candidate;
            }
        }
        return null;
    }

    static boolean isInversePair(ArchitectureRelationship left, ArchitectureRelationship right) {
        return isInversePair(
            JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(left),
            JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(right)
        );
    }

    static boolean isInversePair(InverseRelationshipMergeInput left, InverseRelationshipMergeInput right) {
        if (!isJpaAssociation(left) || !isJpaAssociation(right)) {
            return false;
        }
        if (!left.isInverseOf(right)) {
            return false;
        }
        String leftCardinality = left.associationCardinality();
        String rightCardinality = right.associationCardinality();
        if (leftCardinality == null || rightCardinality == null) {
            return false;
        }
        return switch (leftCardinality) {
            case "many-to-one" -> "one-to-many".equals(rightCardinality)
                && inverseReferenceMatches(right, left);
            case "one-to-many" -> "many-to-one".equals(rightCardinality)
                && inverseReferenceMatches(left, right);
            case "one-to-one" -> "one-to-one".equals(rightCardinality)
                && (inverseReferenceMatches(left, right)
                || inverseReferenceMatches(right, left));
            case "many-to-many" -> "many-to-many".equals(rightCardinality)
                && (inverseReferenceMatches(left, right)
                || inverseReferenceMatches(right, left));
            default -> false;
        };
    }

    private static boolean isJpaAssociation(InverseRelationshipMergeInput input) {
        return input != null
            && "jpa".equals(input.framework())
            && "hasassociation".equals(input.relationshipType())
            && input.associationCardinality() != null;
    }

    private static boolean inverseReferenceMatches(InverseRelationshipMergeInput inverseSide, InverseRelationshipMergeInput owningSide) {
        if (inverseSide == null || owningSide == null || inverseSide.inverseSideReference() == null) {
            return false;
        }
        String propertyName = JavaJpaAssociationMetadataSupport.normalizedString(owningSide.propertyName());
        if (Objects.equals(inverseSide.inverseSideReference(), propertyName)) {
            return true;
        }
        String memberName = JavaJpaAssociationMetadataSupport.normalizedString(
            owningSide.relationship() == null ? null : owningSide.relationship().metadata().get("ownerMemberName")
        );
        return Objects.equals(inverseSide.inverseSideReference(), memberName);
    }

    static boolean hasAmbiguousSwappedJpaAssociation(
        ArchitectureRelationship relationship,
        List<ArchitectureRelationship> relationships
    ) {
        if (relationship == null || relationships == null) {
            return false;
        }
        for (ArchitectureRelationship candidate : relationships) {
            if (candidate == null || Objects.equals(candidate.id(), relationship.id())) {
                continue;
            }
            if (!isJpaAssociation(JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(candidate))) {
                continue;
            }
            if (Objects.equals(candidate.fromEntityId(), relationship.toEntityId())
                && Objects.equals(candidate.toEntityId(), relationship.fromEntityId())) {
                return true;
            }
        }
        return false;
    }
}
