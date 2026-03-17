package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JavaTypeNodeSemanticsSupport {
    private final JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter;

    JavaTypeNodeSemanticsSupport(JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter) {
        this.relationshipEvidenceEmitter = relationshipEvidenceEmitter;
    }

    void applyTypeSemantics(ExtractionAccumulator accumulator, JavaTypeContext typeContext) {
        addJaxRsResourceMetadata(accumulator, typeContext);
        addJpaTypeMetadata(accumulator, typeContext);
    }

    void addJpaInheritanceFacts(
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        SyntaxNode typeNode,
        ExtractedEntityFact typeEntity,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        String typeSnippet = typeNodeSnippet(typeNode, typeEntity);
        if (typeEntity == null || !JavaExtractionSemanticsSupport.isJpaPersistentType(typeSnippet, typeEntity)) {
            return;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(typeNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, typeSnippet, Map.of("language", "java", "kind", typeNode.type()));
        for (String parentType : JavaRelationshipEvidenceEmitter.extractExtendedTypes(typeNode)) {
            JavaRelationshipEvidenceEmitter.ResolvedJavaType resolved = relationshipEvidenceEmitter.resolveJavaTypeReference(
                accumulator,
                parentType,
                EntityKind.CLASS,
                relativePath,
                packageName,
                line,
                importsBySimpleName,
                declaredTypes
            );
            if (resolved == null || typeEntity.id().equals(resolved.entityId())) {
                continue;
            }
            accumulator.addRelationship(ExtractionSupport.typedRelationship(
                info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.EXTENDS,
                "inherits-persistence-model",
                typeEntity.id(),
                resolved.entityId(),
                resolved.label(),
                ref,
                "java",
                Map.of("framework", "jpa", "relationshipType", "inheritsPersistenceModel")
            ));
        }
    }

    static String typeNodeSnippet(SyntaxNode typeNode, ExtractedEntityFact typeEntity) {
        if (typeEntity != null && !typeEntity.sourceRefs().isEmpty() && typeEntity.sourceRefs().getFirst().snippet() != null) {
            return typeEntity.sourceRefs().getFirst().snippet();
        }
        return typeNode == null ? "" : typeNode.textSnippet();
    }

    private void addJaxRsResourceMetadata(
        ExtractionAccumulator accumulator,
        JavaTypeContext typeContext
    ) {
        String relativePath = typeContext.extractionContext().relativePath();
        SyntaxNode typeNode = typeContext.typeNode();
        ExtractedEntityFact typeEntity = typeContext.typeEntity();
        if (typeEntity == null || typeEntity.kind() != EntityKind.CLASS || !JavaExtractionSemanticsSupport.isJaxRsResource(typeEntity)) {
            return;
        }
        String basePath = JavaExtractionSemanticsSupport.extractJaxRsPath(typeEntity.sourceRefs().isEmpty() ? (typeNode == null ? "" : typeNode.textSnippet()) : typeEntity.sourceRefs().getFirst().snippet())
            .orElse("/");
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(typeEntity.metadata());
        metadata.put("framework", "jax-rs");
        metadata.put("jaxRsResource", true);
        metadata.put("jaxRsBasePath", JavaExtractionSemanticsSupport.normalizeJaxRsPath(basePath));
        metadata.put("jaxRsResourceQualifiedName", String.valueOf(typeEntity.metadata().getOrDefault("qualifiedName", typeEntity.name())));
        SourceReference ref = typeEntity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(typeNode), typeNode.textSnippet(), Map.of("language", "java", "kind", "class_declaration"))
            : typeEntity.sourceRefs().getFirst();
        accumulator.addEntity(new ExtractedEntityFact(
            typeEntity.id(),
            typeEntity.kind(),
            typeEntity.origin(),
            typeEntity.name(),
            typeEntity.displayName(),
            typeEntity.scopeId(),
            List.of(ref),
            Map.copyOf(metadata)
        ));
    }

    private void addJpaTypeMetadata(
        ExtractionAccumulator accumulator,
        JavaTypeContext typeContext
    ) {
        String relativePath = typeContext.extractionContext().relativePath();
        String typeSnippet = typeNodeSnippet(typeContext.typeNode(), typeContext.typeEntity());
        ExtractedEntityFact typeEntity = typeContext.typeEntity();
        if (typeEntity == null || typeEntity.kind() != EntityKind.CLASS || !JavaExtractionSemanticsSupport.isJpaPersistentType(typeSnippet, typeEntity)) {
            return;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(typeEntity.metadata());
        metadata.put("framework", "jpa");
        String jpaKind = JavaExtractionSemanticsSupport.detectJpaTypeKind(typeSnippet, typeEntity).orElse("entity");
        metadata.put("jpaKind", jpaKind);
        metadata.put("jpaEntity", "entity".equals(jpaKind));
        metadata.put("jpaEmbeddable", "embeddable".equals(jpaKind));
        metadata.put("jpaMappedSuperclass", "mapped-superclass".equals(jpaKind));
        JavaExtractionSemanticsSupport.extractJpaTableName(typeSnippet).ifPresent(table -> metadata.put("tableName", table));
        JavaExtractionSemanticsSupport.extractJpaInheritanceStrategy(typeSnippet).ifPresent(strategy -> metadata.put("inheritanceStrategy", strategy));
        SourceReference ref = typeEntity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, 1, typeSnippet, Map.of("language", "java", "kind", "class_declaration"))
            : typeEntity.sourceRefs().getFirst();
        accumulator.addEntity(new ExtractedEntityFact(
            typeEntity.id(),
            typeEntity.kind(),
            typeEntity.origin(),
            typeEntity.name(),
            typeEntity.displayName(),
            typeEntity.scopeId(),
            List.of(ref),
            Map.copyOf(metadata)
        ));
    }
}
