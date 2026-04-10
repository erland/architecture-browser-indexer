package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

final class JavaJpaInverseRelationshipMergeInputFactory {
    private JavaJpaInverseRelationshipMergeInputFactory() {}

    static InverseRelationshipMergeInput fromRelationship(ArchitectureRelationship relationship) {
        if (relationship == null) {
            return null;
        }
        return new InverseRelationshipMergeInput(
            relationship,
            JavaJpaAssociationMetadataSupport.normalizedString(relationship.metadata().get("framework")),
            JavaJpaAssociationMetadataSupport.normalizedString(relationship.metadata().get("relationshipType")),
            JavaJpaAssociationMetadataSupport.associationCardinality(relationship),
            JavaJpaAssociationMetadataSupport.stringValue(relationship.metadata().get("associationKind")),
            sideRoleForRelationship(relationship),
            JavaJpaAssociationMetadataSupport.inverseSideReference(relationship),
            JavaJpaAssociationMetadataSupport.propertyNameForRelationship(relationship),
            RelationshipMultiplicityBoundsSupport.boundsForRelationship(relationship)
        );
    }

    private static InverseRelationshipSideRole sideRoleForRelationship(ArchitectureRelationship relationship) {
        if (!JavaJpaAssociationMetadataSupport.isJpaAssociation(relationship)) {
            return InverseRelationshipSideRole.UNSPECIFIED;
        }
        if (JavaJpaAssociationMetadataSupport.hasMappedBy(relationship)) {
            return InverseRelationshipSideRole.INVERSE;
        }
        return InverseRelationshipSideRole.OWNING;
    }
}
