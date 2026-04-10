package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

/**
 * Framework-neutral input contract for inverse relationship normalization.
 * <p>
 * Framework-specific adapters can project their relationship evidence into this shape so the
 * pairing and merge pipeline can stay reusable across frameworks.
 */
record InverseRelationshipMergeInput(
    ArchitectureRelationship relationship,
    String framework,
    String relationshipType,
    String associationCardinality,
    String associationKind,
    InverseRelationshipSideRole sideRole,
    String inverseSideReference,
    String propertyName,
    RelationshipMultiplicityBoundsSupport.RelationshipEndBounds bounds
) {
    String id() {
        return relationship == null ? null : relationship.id();
    }

    String fromEntityId() {
        return relationship == null ? null : relationship.fromEntityId();
    }

    String toEntityId() {
        return relationship == null ? null : relationship.toEntityId();
    }

    boolean isInverseOf(InverseRelationshipMergeInput other) {
        return other != null
            && java.util.Objects.equals(fromEntityId(), other.toEntityId())
            && java.util.Objects.equals(toEntityId(), other.fromEntityId());
    }

    boolean isInverseSide() {
        return sideRole == InverseRelationshipSideRole.INVERSE;
    }

    boolean isOwningSide() {
        return sideRole == InverseRelationshipSideRole.OWNING;
    }
}
