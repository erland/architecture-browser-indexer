package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.NormalizedAssociation;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class JavaJpaNormalizedAssociationAssembler {
    private JavaJpaNormalizedAssociationAssembler() {}

    static ArchitectureRelationship canonicalRelationship(
        ArchitectureRelationship left,
        ArchitectureRelationship right,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        InverseRelationshipMergeInput leftInput = JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(left);
        InverseRelationshipMergeInput rightInput = JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(right);
        InverseRelationshipMergeInput canonicalBase = chooseCanonicalBase(leftInput, rightInput);
        InverseRelationshipMergeInput paired = canonicalBase == leftInput ? rightInput : leftInput;
        String cardinality = canonicalBase.associationCardinality();
        String associationKind = JavaJpaContainmentPromotionPolicy.promotedAssociationKind(canonicalBase.relationship(), paired.relationship(), cardinality);
        RelationshipMultiplicityBoundsSupport.RelationshipEndBounds bounds = deriveMergedBounds(canonicalBase, paired);
        List<String> semantics = new ArrayList<>();
        if (canonicalBase.relationship().architecturalSemantics() != null) {
            semantics.addAll(canonicalBase.relationship().architecturalSemantics());
        }
        if (paired.relationship().architecturalSemantics() != null) {
            semantics.addAll(paired.relationship().architecturalSemantics());
        }
        List<String> evidenceIds = List.of(canonicalBase.relationship().id(), paired.relationship().id());
        NormalizedAssociation normalizedAssociation = new NormalizedAssociation(
            associationKind,
            cardinality,
            bounds.sourceLowerBound(),
            bounds.sourceUpperBound(),
            bounds.targetLowerBound(),
            bounds.targetUpperBound(),
            Boolean.TRUE,
            evidenceIds,
            owningSideEntityId(leftInput, rightInput),
            owningSideMemberId(leftInput, rightInput),
            inverseSideEntityId(leftInput, rightInput),
            inverseSideMemberId(leftInput, rightInput)
        );
        return new ArchitectureRelationship(
            canonicalBase.relationship().id(),
            canonicalBase.relationship().kind(),
            canonicalBase.relationship().fromEntityId(),
            canonicalBase.relationship().toEntityId(),
            canonicalBase.relationship().label(),
            canonicalizedSourceRefs(canonicalBase.relationship(), paired.relationship()),
            mergedMetadata(canonicalBase, paired, entitiesById),
            semantics.isEmpty() ? null : semantics,
            normalizedAssociation
        );
    }

    static ArchitectureRelationship chooseCanonicalBase(ArchitectureRelationship left, ArchitectureRelationship right) {
        return chooseCanonicalBase(
            JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(left),
            JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(right)
        ).relationship();
    }

    static InverseRelationshipMergeInput chooseCanonicalBase(InverseRelationshipMergeInput left, InverseRelationshipMergeInput right) {
        String leftCardinality = left.associationCardinality();
        String rightCardinality = right.associationCardinality();
        if ("one-to-many".equals(leftCardinality) && "many-to-one".equals(rightCardinality)) {
            return left;
        }
        if ("one-to-many".equals(rightCardinality) && "many-to-one".equals(leftCardinality)) {
            return right;
        }
        if (left.isInverseSide() && !right.isInverseSide()) {
            return right;
        }
        if (right.isInverseSide() && !left.isInverseSide()) {
            return left;
        }
        if (left.isOwningSide() && !right.isOwningSide()) {
            return left;
        }
        if (right.isOwningSide() && !left.isOwningSide()) {
            return right;
        }
        return left.id().compareTo(right.id()) <= 0 ? left : right;
    }

    static RelationshipMultiplicityBoundsSupport.RelationshipEndBounds deriveMergedBounds(ArchitectureRelationship canonicalBase, ArchitectureRelationship paired) {
        return deriveMergedBounds(
            JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(canonicalBase),
            JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(paired)
        );
    }

    static RelationshipMultiplicityBoundsSupport.RelationshipEndBounds deriveMergedBounds(
        InverseRelationshipMergeInput canonicalBase,
        InverseRelationshipMergeInput paired
    ) {
        return InverseRelationshipBoundMergePolicy.mergeBounds(canonicalBase, paired);
    }

    private static void putIfPresent(java.util.LinkedHashMap<String, Object> metadata, String key, String value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }

    private static Map<String, Object> mergedMetadata(
        InverseRelationshipMergeInput canonicalBase,
        InverseRelationshipMergeInput paired,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        java.util.LinkedHashMap<String, Object> metadata = new java.util.LinkedHashMap<>(canonicalBase.relationship().metadata());
        metadata.put("inverseJpaAssociationMerged", Boolean.TRUE);
        metadata.put("jpaAssociationPeerEntity", Boolean.TRUE);
        metadata.put("jpaAssociationHandling", "merged-bidirectional-peer-association");
        metadata.put("inverseJpaAssociationRelationshipIds", List.of(canonicalBase.relationship().id(), paired.relationship().id()).stream().sorted().toList());
        if (!metadata.containsKey("associationCardinality")) {
            metadata.put("associationCardinality", canonicalBase.associationCardinality());
        }
        String promotedAssociationKind = JavaJpaContainmentPromotionPolicy.promotedAssociationKind(
            canonicalBase.relationship(),
            paired.relationship(),
            canonicalBase.associationCardinality()
        );
        if (promotedAssociationKind != null) {
            metadata.put("associationKind", promotedAssociationKind);
            if ("containment".equalsIgnoreCase(promotedAssociationKind)) {
                metadata.put("containmentPromoted", Boolean.TRUE);
            }
        }
        RelationshipMultiplicityBoundsSupport.RelationshipEndBounds bounds = deriveMergedBounds(canonicalBase, paired);
        putIfPresent(metadata, "sourceLowerBound", bounds.sourceLowerBound());
        putIfPresent(metadata, "sourceUpperBound", bounds.sourceUpperBound());
        putIfPresent(metadata, "targetLowerBound", bounds.targetLowerBound());
        putIfPresent(metadata, "targetUpperBound", bounds.targetUpperBound());
        if (entitiesById != null && entitiesById.containsKey(canonicalBase.relationship().fromEntityId())) {
            metadata.putIfAbsent("sourceEntityId", canonicalBase.relationship().fromEntityId());
        }
        if (entitiesById != null && entitiesById.containsKey(canonicalBase.relationship().toEntityId())) {
            metadata.putIfAbsent("targetEntityId", canonicalBase.relationship().toEntityId());
        }
        return Map.copyOf(metadata);
    }

    private static List<SourceReference> canonicalizedSourceRefs(
        ArchitectureRelationship left,
        ArchitectureRelationship right
    ) {
        List<SourceReference> refs = new ArrayList<>();
        if (left.sourceRefs() != null) {
            refs.addAll(left.sourceRefs());
        }
        if (right.sourceRefs() != null) {
            refs.addAll(right.sourceRefs());
        }
        return refs.stream().distinct().toList();
    }


    private static String owningSideEntityId(InverseRelationshipMergeInput left, InverseRelationshipMergeInput right) {
        if (left.isInverseSide() && !right.isInverseSide()) {
            return right.fromEntityId();
        }
        if (right.isInverseSide() && !left.isInverseSide()) {
            return left.fromEntityId();
        }
        if (left.isOwningSide() && !right.isOwningSide()) {
            return left.fromEntityId();
        }
        if (right.isOwningSide() && !left.isOwningSide()) {
            return right.fromEntityId();
        }
        return left.fromEntityId();
    }

    private static String inverseSideEntityId(InverseRelationshipMergeInput left, InverseRelationshipMergeInput right) {
        if (left.isInverseSide() && !right.isInverseSide()) {
            return left.fromEntityId();
        }
        if (right.isInverseSide() && !left.isInverseSide()) {
            return right.fromEntityId();
        }
        if (left.isOwningSide() && !right.isOwningSide()) {
            return right.fromEntityId();
        }
        if (right.isOwningSide() && !left.isOwningSide()) {
            return left.fromEntityId();
        }
        return right.fromEntityId();
    }

    private static String owningSideMemberId(InverseRelationshipMergeInput left, InverseRelationshipMergeInput right) {
        if (left.isInverseSide() && !right.isInverseSide()) {
            return right.propertyName();
        }
        if (right.isInverseSide() && !left.isInverseSide()) {
            return left.propertyName();
        }
        if (left.isOwningSide() && !right.isOwningSide()) {
            return left.propertyName();
        }
        if (right.isOwningSide() && !left.isOwningSide()) {
            return right.propertyName();
        }
        return left.propertyName();
    }

    private static String inverseSideMemberId(InverseRelationshipMergeInput left, InverseRelationshipMergeInput right) {
        if (left.isInverseSide() && !right.isInverseSide()) {
            return left.propertyName();
        }
        if (right.isInverseSide() && !left.isInverseSide()) {
            return right.propertyName();
        }
        if (left.isOwningSide() && !right.isOwningSide()) {
            return right.propertyName();
        }
        if (right.isOwningSide() && !left.isOwningSide()) {
            return left.propertyName();
        }
        return right.propertyName();
    }
}
