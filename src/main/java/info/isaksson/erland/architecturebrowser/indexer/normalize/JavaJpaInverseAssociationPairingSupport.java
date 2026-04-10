package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.List;
import java.util.Objects;
import java.util.Set;

final class JavaJpaInverseAssociationPairingSupport {
    private JavaJpaInverseAssociationPairingSupport() {}

    static ArchitectureRelationship findInversePair(
        ArchitectureRelationship relationship,
        JavaJpaInverseAssociationIndex index,
        Set<String> consumed
    ) {
        InverseRelationshipMergeInput relationshipInput = index == null
            ? JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(relationship)
            : index.inputFor(relationship);
        if (relationshipInput == null) {
            return null;
        }
        List<InverseRelationshipMergeInput> candidates = index == null
            ? List.of()
            : index.inverseCandidatesFor(relationshipInput);
        for (InverseRelationshipMergeInput candidateInput : candidates) {
            if (candidateInput == null || Objects.equals(candidateInput.id(), relationshipInput.id()) || consumed.contains(candidateInput.id())) {
                continue;
            }
            if (isInversePair(relationshipInput, candidateInput)) {
                return candidateInput.relationship();
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
        JavaJpaInverseAssociationIndex index
    ) {
        if (relationship == null || index == null) {
            return false;
        }
        return index.hasSwappedJpaAssociation(relationship);
    }
}
