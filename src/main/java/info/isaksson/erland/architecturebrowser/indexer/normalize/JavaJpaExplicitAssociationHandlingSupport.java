package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.NormalizedAssociation;

import java.util.List;
import java.util.Map;

final class JavaJpaExplicitAssociationHandlingSupport {
    private JavaJpaExplicitAssociationHandlingSupport() {}

    static ArchitectureRelationship explicitlyHandledRelationship(
        ArchitectureRelationship relationship,
        JavaJpaInverseAssociationIndex index
    ) {
        if (relationship == null) {
            return null;
        }
        java.util.LinkedHashMap<String, Object> metadata = new java.util.LinkedHashMap<>(relationship.metadata());
        String handlingCategory = explicitHandlingCategory(relationship);
        if (handlingCategory != null) {
            metadata.put("jpaAssociationHandling", handlingCategory);
        }
        if (isExplicitNonPeerJpaValueLikeRelationship(relationship)) {
            metadata.put("jpaAssociationPeerEntity", Boolean.FALSE);
            metadata.put("jpaNonPeerAssociation", Boolean.TRUE);
            return new ArchitectureRelationship(
                relationship.id(),
                relationship.kind(),
                relationship.fromEntityId(),
                relationship.toEntityId(),
                relationship.label(),
                relationship.sourceRefs(),
                Map.copyOf(metadata),
                relationship.architecturalSemantics(),
                null
            );
        }
        if (!JavaJpaAssociationMetadataSupport.isJpaAssociation(relationship)) {
            return new ArchitectureRelationship(
                relationship.id(),
                relationship.kind(),
                relationship.fromEntityId(),
                relationship.toEntityId(),
                relationship.label(),
                relationship.sourceRefs(),
                Map.copyOf(metadata),
                relationship.architecturalSemantics(),
                relationship.normalizedAssociation()
            );
        }
        metadata.put("jpaAssociationPeerEntity", Boolean.TRUE);
        if (!JavaJpaAssociationMetadataSupport.hasMappedBy(relationship)) {
            metadata.put("jpaAssociationUnidirectional", Boolean.TRUE);
        }
        NormalizedAssociation normalizedAssociation = relationship.normalizedAssociation();
        if (normalizedAssociation == null) {
            metadata.put("jpaAssociationExplicitlyHandled", Boolean.TRUE);
            boolean explicitUnidirectional = !Boolean.TRUE.equals(metadata.get("inverseJpaAssociationMerged"))
                && !metadata.containsKey("mappedBy")
                && JavaJpaAssociationMetadataSupport.valueAtPath(metadata, "jpaAssociationEvidence", "mappedBy") == null
                && !JavaJpaInverseAssociationPairingSupport.hasAmbiguousSwappedJpaAssociation(relationship, index);
            if (explicitUnidirectional) {
                normalizedAssociation = new NormalizedAssociation(
                    existingAssociationKind(relationship, relationship),
                    JavaJpaAssociationMetadataSupport.associationCardinality(relationship),
                    JavaJpaAssociationMetadataSupport.stringValue(metadata.get("sourceLowerBound")),
                    JavaJpaAssociationMetadataSupport.stringValue(metadata.get("sourceUpperBound")),
                    JavaJpaAssociationMetadataSupport.stringValue(metadata.get("targetLowerBound")),
                    JavaJpaAssociationMetadataSupport.stringValue(metadata.get("targetUpperBound")),
                    Boolean.FALSE,
                    List.of(relationship.id()),
                    relationship.fromEntityId(),
                    JavaJpaAssociationMetadataSupport.propertyNameForRelationship(relationship),
                    null,
                    null
                );
            }
        }
        return new ArchitectureRelationship(
            relationship.id(),
            relationship.kind(),
            relationship.fromEntityId(),
            relationship.toEntityId(),
            relationship.label(),
            relationship.sourceRefs(),
            Map.copyOf(metadata),
            relationship.architecturalSemantics(),
            normalizedAssociation
        );
    }

    private static String explicitHandlingCategory(ArchitectureRelationship relationship) {
        String evidenceCategory = JavaJpaAssociationMetadataSupport.normalizedString(
            JavaJpaAssociationMetadataSupport.valueAtPath(relationship.metadata(), "jpaAssociationEvidence", "handlingCategory")
        );
        if (evidenceCategory != null && !"peer-entity-association".equals(evidenceCategory)) {
            return evidenceCategory;
        }
        if (isExplicitNonPeerJpaValueLikeRelationship(relationship)) {
            return evidenceCategory == null ? "value-like-non-peer" : evidenceCategory;
        }
        if (JavaJpaAssociationMetadataSupport.isJpaAssociation(relationship)) {
            return JavaJpaAssociationMetadataSupport.hasMappedBy(relationship)
                ? "inverse-peer-association"
                : "unidirectional-peer-association";
        }
        return evidenceCategory;
    }

    private static boolean isExplicitNonPeerJpaValueLikeRelationship(ArchitectureRelationship relationship) {
        return Boolean.TRUE.equals(JavaJpaAssociationMetadataSupport.booleanEvidenceOrNull(relationship, "valueLikeTarget"))
            || Boolean.TRUE.equals(JavaJpaAssociationMetadataSupport.booleanEvidenceOrNull(relationship, "elementCollection"))
            || Boolean.TRUE.equals(JavaJpaAssociationMetadataSupport.booleanEvidenceOrNull(relationship, "embedded"))
            || Boolean.TRUE.equals(JavaJpaAssociationMetadataSupport.booleanEvidenceOrNull(relationship, "embeddedId"));
    }

    private static String existingAssociationKind(ArchitectureRelationship left, ArchitectureRelationship right) {
        String value = JavaJpaAssociationMetadataSupport.stringValue(left.metadata().get("associationKind"));
        if (value != null) {
            return value;
        }
        return JavaJpaAssociationMetadataSupport.stringValue(right.metadata().get("associationKind"));
    }
}
