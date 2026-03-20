package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JavaJpaAssociationSemanticsSupport {
    private final JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter;

    JavaJpaAssociationSemanticsSupport(JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter) {
        this.relationshipEvidenceEmitter = relationshipEvidenceEmitter;
    }

    void emitAssociationRelationship(
        ExtractionAccumulator accumulator,
        String ownerTypeEntityId,
        String ownerQualifiedName,
        String ownerMemberKind,
        String ownerMemberName,
        String ownerPropertyName,
        String associationKind,
        String declaredType,
        String snippet,
        String relativePath,
        String packageName,
        int line,
        SourceReference ref,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        List<String> referencedTypes = JavaRelationshipEvidenceEmitter.extractReferencedTypes(declaredType);
        if (referencedTypes.isEmpty()) {
            return;
        }
        JavaRelationshipEvidenceEmitter.ResolvedJavaType target = relationshipEvidenceEmitter.resolveJavaTypeReference(
            accumulator,
            referencedTypes.getLast(),
            EntityKind.CLASS,
            relativePath,
            packageName,
            line,
            importsBySimpleName,
            declaredTypes
        );
        if (target == null || ownerTypeEntityId.equals(target.entityId())) {
            return;
        }
        LinkedHashMap<String, Object> relationshipMetadata = new LinkedHashMap<>();
        relationshipMetadata.put("framework", "jpa");
        relationshipMetadata.put("relationshipType", "hasAssociation");
        relationshipMetadata.put("associationKind", "association");
        relationshipMetadata.put("associationCardinality", associationKind);
        deriveAssociationBounds(associationKind, snippet).forEach(relationshipMetadata::put);
        relationshipMetadata.put("jpaAssociation", associationKind);
        JavaJpaDomainSemanticsSupport.extractJpaMappedBy(snippet).ifPresent(mappedBy -> relationshipMetadata.put("mappedBy", mappedBy));
        JavaJpaDomainSemanticsSupport.extractJpaJoinColumn(snippet).ifPresent(joinColumn -> relationshipMetadata.put("joinColumn", joinColumn));
        JavaJpaDomainSemanticsSupport.extractJpaJoinTable(snippet).ifPresent(joinTable -> relationshipMetadata.put("joinTable", joinTable));
        relationshipMetadata.put("ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName);
        if (ownerMemberKind != null) relationshipMetadata.put("ownerMemberKind", ownerMemberKind);
        if (ownerMemberName != null) relationshipMetadata.put("ownerMemberName", ownerMemberName);
        if (ownerPropertyName != null) relationshipMetadata.put("ownerPropertyName", ownerPropertyName);
        accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
            ownerTypeEntityId,
            target.entityId(),
            target.label(),
            ref,
            "java",
            Map.copyOf(relationshipMetadata)
        ));
    }

    /**
     * Derives normalized association-end bounds using the full association-end convention
     * documented in docs/export-format/normalized-association-metadata-contract.md.
     *
     * Convention:
     * - source* fields describe the multiplicity at the source entity end of the emitted
     *   source->target relationship
     * - target* fields describe the multiplicity at the target entity end
     *
     * This keeps the representation stable for diagramming and avoids a property-only
     * interpretation where only the referenced target multiplicity is exported.
     *
     * Optionality evidence precedence for single-valued associations:
     * - prefer explicit association optionality when it is the only signal
     * - otherwise use explicit join-column nullability when it is the only signal
     * - if both are present but conflict, choose the conservative optional interpretation
     *   and emit 0..1 rather than 1..1
     */
    static Map<String, Object> deriveAssociationBounds(String associationKind, String snippet) {
        AssociationBounds bounds = switch (associationKind == null ? "" : associationKind) {
            case "one-to-one" -> {
                String lower = isMandatorySingleValuedAssociation(snippet) ? "1" : "0";
                yield new AssociationBounds(lower, "1", lower, "1");
            }
            case "many-to-one" -> new AssociationBounds("0", "*", isMandatorySingleValuedAssociation(snippet) ? "1" : "0", "1");
            case "one-to-many" -> new AssociationBounds("0", "1", "0", "*");
            case "many-to-many" -> new AssociationBounds("0", "*", "0", "*");
            default -> null;
        };
        if (bounds == null) {
            return Map.of();
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceLowerBound", bounds.sourceLowerBound());
        metadata.put("sourceUpperBound", bounds.sourceUpperBound());
        metadata.put("targetLowerBound", bounds.targetLowerBound());
        metadata.put("targetUpperBound", bounds.targetUpperBound());
        return Map.copyOf(metadata);
    }

    private static boolean isMandatorySingleValuedAssociation(String snippet) {
        var associationOptional = JavaJpaDomainSemanticsSupport.extractJpaAssociationOptional(snippet);
        var joinColumnNullable = JavaJpaDomainSemanticsSupport.extractJpaJoinColumnNullable(snippet);

        if (associationOptional.isPresent() && joinColumnNullable.isPresent()) {
            boolean associationSaysMandatory = !associationOptional.orElse(true);
            boolean joinColumnSaysMandatory = !joinColumnNullable.orElse(true);
            if (associationSaysMandatory != joinColumnSaysMandatory) {
                return false;
            }
            return associationSaysMandatory;
        }
        if (associationOptional.isPresent()) {
            return !associationOptional.orElse(true);
        }
        if (joinColumnNullable.isPresent()) {
            return !joinColumnNullable.orElse(true);
        }
        return false;
    }

    private record AssociationBounds(String sourceLowerBound, String sourceUpperBound, String targetLowerBound, String targetUpperBound) {}

    void emitEmbeddedRelationship(
        ExtractionAccumulator accumulator,
        String ownerTypeEntityId,
        String ownerQualifiedName,
        String ownerMemberKind,
        String ownerMemberName,
        String ownerPropertyName,
        String declaredType,
        String relativePath,
        String packageName,
        int line,
        SourceReference ref,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes,
        String relationshipTypeName
    ) {
        List<String> referencedTypes = JavaRelationshipEvidenceEmitter.extractReferencedTypes(declaredType);
        if (referencedTypes.isEmpty()) {
            return;
        }
        JavaRelationshipEvidenceEmitter.ResolvedJavaType target = relationshipEvidenceEmitter.resolveJavaTypeReference(
            accumulator,
            referencedTypes.getLast(),
            EntityKind.CLASS,
            relativePath,
            packageName,
            line,
            importsBySimpleName,
            declaredTypes
        );
        if (target == null || ownerTypeEntityId.equals(target.entityId())) {
            return;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "jpa");
        metadata.put("relationshipType", "embeds");
        metadata.put("ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName);
        if (ownerMemberKind != null) metadata.put("ownerMemberKind", ownerMemberKind);
        if (ownerMemberName != null) metadata.put("ownerMemberName", ownerMemberName);
        if (ownerPropertyName != null) metadata.put("ownerPropertyName", ownerPropertyName);
        accumulator.addRelationship(ExtractionSupport.typedRelationship(
            info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.DEPENDS_ON,
            relationshipTypeName,
            ownerTypeEntityId,
            target.entityId(),
            target.label(),
            ref,
            "java",
            Map.copyOf(metadata)
        ));
    }
}
