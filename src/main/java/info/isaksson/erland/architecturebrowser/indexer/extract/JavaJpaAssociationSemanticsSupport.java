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
