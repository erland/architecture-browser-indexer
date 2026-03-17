package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class JavaJpaSemanticsSupport {
    private final JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter;

    JavaJpaSemanticsSupport(JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter) {
        this.relationshipEvidenceEmitter = relationshipEvidenceEmitter;
    }

    private JavaRelationshipEvidenceEmitter.ResolvedJavaType resolveJavaTypeReference(
        ExtractionAccumulator accumulator,
        String referencedType,
        EntityKind fallbackTargetKind,
        String relativePath,
        String packageName,
        int line,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        return relationshipEvidenceEmitter.resolveJavaTypeReference(
            accumulator, referencedType, fallbackTargetKind, relativePath, packageName, line, importsBySimpleName, declaredTypes
        );
    }
void addJpaTypeMetadata(
        ExtractionAccumulator accumulator,
        JavaTypeContext typeContext
    ) {
        String relativePath = typeContext.extractionContext().relativePath();
        String typeSnippet = JavaTypeSemanticsFlow.typeNodeSnippet(typeContext.typeNode(), typeContext.typeEntity());
        ExtractedEntityFact typeEntity = typeContext.typeEntity();
        if (typeEntity == null || typeEntity.kind() != EntityKind.CLASS || !JavaSyntaxTreeExtractionStage.isJpaPersistentType(typeSnippet, typeEntity)) {
            return;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(typeEntity.metadata());
        metadata.put("framework", "jpa");
        String jpaKind = JavaSyntaxTreeExtractionStage.detectJpaTypeKind(typeSnippet, typeEntity).orElse("entity");
        metadata.put("jpaKind", jpaKind);
        metadata.put("jpaEntity", "entity".equals(jpaKind));
        metadata.put("jpaEmbeddable", "embeddable".equals(jpaKind));
        metadata.put("jpaMappedSuperclass", "mapped-superclass".equals(jpaKind));
        JavaSyntaxTreeExtractionStage.extractJpaTableName(typeSnippet).ifPresent(table -> metadata.put("tableName", table));
        JavaSyntaxTreeExtractionStage.extractJpaInheritanceStrategy(typeSnippet).ifPresent(strategy -> metadata.put("inheritanceStrategy", strategy));
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

void addJpaFieldFacts(
        ExtractionAccumulator accumulator,
        JavaFieldContext fieldContext
    ) {
        JavaExtractionContext extractionContext = fieldContext.extractionContext();
        String relativePath = extractionContext.relativePath();
        String packageName = extractionContext.packageName();
        SyntaxNode fieldNode = fieldContext.fieldNode();
        ExtractedEntityFact fieldEntity = fieldContext.fieldEntity();
        String ownerTypeEntityId = fieldContext.ownerTypeEntityId();
        String ownerQualifiedName = fieldContext.ownerQualifiedName();
        String ownerTypeSnippet = fieldContext.ownerTypeSnippet();
        Map<String, String> importsBySimpleName = extractionContext.importsBySimpleName();
        Map<String, JavaDeclaredType> declaredTypes = extractionContext.declaredTypes();
        if (fieldEntity == null || ownerTypeEntityId == null || !JavaSyntaxTreeExtractionStage.isJpaPersistentType(ownerTypeSnippet, null)) {
            return;
        }
        String snippet = fieldEntity.sourceRefs().isEmpty() ? (fieldNode == null ? "" : fieldNode.textSnippet()) : fieldEntity.sourceRefs().getFirst().snippet();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(fieldEntity.metadata());
        metadata.put("framework", "jpa");
        boolean changed = false;
        if (JavaSyntaxTreeExtractionStage.hasAnnotation(JavaDeclaredTypeSupport.metadataStringList(fieldEntity.metadata().get("annotations")), "Id") || JavaSyntaxTreeExtractionStage.hasAnnotation(JavaDeclaredTypeSupport.metadataStringList(fieldEntity.metadata().get("annotations")), "EmbeddedId")) {
            metadata.put("jpaId", true);
            changed = true;
        }
        if (JavaSyntaxTreeExtractionStage.hasAnnotation(JavaDeclaredTypeSupport.metadataStringList(fieldEntity.metadata().get("annotations")), "Version")) {
            metadata.put("jpaVersion", true);
            changed = true;
        }
        if (JavaSyntaxTreeExtractionStage.hasAnnotation(JavaDeclaredTypeSupport.metadataStringList(fieldEntity.metadata().get("annotations")), "Embedded") || JavaSyntaxTreeExtractionStage.hasAnnotation(JavaDeclaredTypeSupport.metadataStringList(fieldEntity.metadata().get("annotations")), "EmbeddedId")) {
            metadata.put("jpaEmbedded", true);
            changed = true;
        }
        JavaSyntaxTreeExtractionStage.extractJpaColumnName(snippet).ifPresent(column -> {
            metadata.put("columnName", column);
        });
        if (snippet != null && snippet.contains("nullable = false")) {
            metadata.put("nullable", false);
            changed = true;
        }
        if (snippet != null && snippet.contains("nullable = true")) {
            metadata.put("nullable", true);
            changed = true;
        }
        if (snippet != null && snippet.contains("unique = true")) {
            metadata.put("unique", true);
            changed = true;
        }
        if (JavaSyntaxTreeExtractionStage.extractJpaColumnName(snippet).isPresent()) {
            changed = true;
        }
        Optional<String> association = JavaSyntaxTreeExtractionStage.detectJpaAssociation(snippet);
        if (association.isPresent()) {
            String associationKind = association.get();
            metadata.put("jpaAssociation", associationKind);
            JavaSyntaxTreeExtractionStage.extractJpaMappedBy(snippet).ifPresent(mappedBy -> metadata.put("mappedBy", mappedBy));
            JavaSyntaxTreeExtractionStage.extractJpaJoinColumn(snippet).ifPresent(joinColumn -> metadata.put("joinColumn", joinColumn));
            JavaSyntaxTreeExtractionStage.extractJpaJoinTable(snippet).ifPresent(joinTable -> metadata.put("joinTable", joinTable));
            changed = true;

            String declaredType = String.valueOf(fieldEntity.metadata().getOrDefault("declaredType", ""));
            List<String> referencedTypes = JavaRelationshipEvidenceEmitter.extractReferencedTypes(declaredType);
            if (!referencedTypes.isEmpty()) {
                JavaRelationshipEvidenceEmitter.ResolvedJavaType target = resolveJavaTypeReference(
                    accumulator,
                    referencedTypes.getLast(),
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    JavaSyntaxTreeExtractionStage.lineOf(fieldEntity.sourceRefs().isEmpty() ? null : fieldEntity.sourceRefs().getFirst(), fieldNode),
                    importsBySimpleName,
                    declaredTypes
                );
                if (target != null && !ownerTypeEntityId.equals(target.entityId())) {
                    SourceReference ref = fieldEntity.sourceRefs().isEmpty()
                        ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(fieldNode), snippet, Map.of("language", "java", "kind", "field_declaration"))
                        : fieldEntity.sourceRefs().getFirst();
                    LinkedHashMap<String, Object> relationshipMetadata = new LinkedHashMap<>();
                    relationshipMetadata.put("framework", "jpa");
                    relationshipMetadata.put("relationshipType", "hasAssociation");
                    relationshipMetadata.put("jpaAssociation", associationKind);
                    JavaSyntaxTreeExtractionStage.extractJpaMappedBy(snippet).ifPresent(mappedBy -> relationshipMetadata.put("mappedBy", mappedBy));
                    JavaSyntaxTreeExtractionStage.extractJpaJoinColumn(snippet).ifPresent(joinColumn -> relationshipMetadata.put("joinColumn", joinColumn));
                    JavaSyntaxTreeExtractionStage.extractJpaJoinTable(snippet).ifPresent(joinTable -> relationshipMetadata.put("joinTable", joinTable));
                    relationshipMetadata.put("ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName);
                    accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                        ownerTypeEntityId,
                        target.entityId(),
                        target.label(),
                        ref,
                        "java",
                        Map.copyOf(relationshipMetadata)
                    ));
                }
            }
        }
        if (Boolean.TRUE.equals(metadata.get("jpaEmbedded"))) {
            String declaredType = String.valueOf(fieldEntity.metadata().getOrDefault("declaredType", ""));
            List<String> referencedTypes = JavaRelationshipEvidenceEmitter.extractReferencedTypes(declaredType);
            if (!referencedTypes.isEmpty()) {
                JavaRelationshipEvidenceEmitter.ResolvedJavaType target = resolveJavaTypeReference(
                    accumulator,
                    referencedTypes.getLast(),
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    JavaSyntaxTreeExtractionStage.lineOf(fieldEntity.sourceRefs().isEmpty() ? null : fieldEntity.sourceRefs().getFirst(), fieldNode),
                    importsBySimpleName,
                    declaredTypes
                );
                if (target != null && !ownerTypeEntityId.equals(target.entityId())) {
                    SourceReference ref = fieldEntity.sourceRefs().isEmpty()
                        ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(fieldNode), snippet, Map.of("language", "java", "kind", "field_declaration"))
                        : fieldEntity.sourceRefs().getFirst();
                    accumulator.addRelationship(ExtractionSupport.typedRelationship(
                        info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.DEPENDS_ON,
                        "embeds-jpa-type",
                        ownerTypeEntityId,
                        target.entityId(),
                        target.label(),
                        ref,
                        "java",
                        Map.of("framework", "jpa", "relationshipType", "embeds", "ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName)
                    ));
                }
            }
        }
        if (changed) {
            SourceReference ref = fieldEntity.sourceRefs().isEmpty()
                ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(fieldNode), snippet, Map.of("language", "java", "kind", "field_declaration"))
                : fieldEntity.sourceRefs().getFirst();
            accumulator.addEntity(new ExtractedEntityFact(
                fieldEntity.id(),
                fieldEntity.kind(),
                fieldEntity.origin(),
                fieldEntity.name(),
                fieldEntity.displayName(),
                fieldEntity.scopeId(),
                List.of(ref),
                Map.copyOf(metadata)
            ));
        }
    }

void addJpaMethodFacts(
        ExtractionAccumulator accumulator,
        JavaMethodContext methodContext
    ) {
        JavaExtractionContext extractionContext = methodContext.extractionContext();
        String relativePath = extractionContext.relativePath();
        String packageName = extractionContext.packageName();
        SyntaxNode methodNode = methodContext.methodNode();
        ExtractedEntityFact methodEntity = methodContext.methodEntity();
        String ownerTypeEntityId = methodContext.ownerTypeEntityId();
        String ownerQualifiedName = methodContext.ownerQualifiedName();
        String ownerTypeSnippet = methodContext.ownerTypeSnippet();
        SourceReference ref = methodContext.sourceRef();
        String snippet = methodContext.snippet();
        Map<String, String> importsBySimpleName = extractionContext.importsBySimpleName();
        Map<String, JavaDeclaredType> declaredTypes = extractionContext.declaredTypes();
        if (methodEntity == null || ownerTypeEntityId == null || !JavaSyntaxTreeExtractionStage.isJpaPersistentType(ownerTypeSnippet, null) || JavaSyntaxTreeExtractionStage.isConstructor(methodEntity)) {
            return;
        }
        List<String> annotations = JavaDeclaredTypeSupport.metadataStringList(methodEntity.metadata().get("annotations"));
        if (methodNode != null) {
            java.util.LinkedHashSet<String> mergedAnnotations = new java.util.LinkedHashSet<>(annotations);
            SyntaxTreeExtractionSupport.descendantsByType(methodNode, Set.of("marker_annotation", "annotation")).stream()
                .flatMap(annotationNode -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(annotationNode.textSnippet()).stream())
                .forEach(mergedAnnotations::add);
            SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(methodNode.textSnippet()).forEach(mergedAnnotations::add);
            annotations = List.copyOf(mergedAnnotations);
        }
        String parameters = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
        String methodName = methodEntity.name();
        if (!annotations.stream().anyMatch(a -> a != null && !a.isBlank()) && !JavaSyntaxTreeExtractionStage.containsJpaPropertyAnnotation(snippet)) {
            return;
        }
        String propertyName = JavaSyntaxTreeExtractionStage.deriveJavaPropertyName(methodName, parameters);
        if (propertyName == null || propertyName.isBlank()) {
            return;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(methodEntity.metadata());
        metadata.put("framework", "jpa");
        metadata.put("jpaPropertyAccess", true);
        metadata.put("jpaPropertyName", propertyName);
        boolean changed = true;
        if (JavaSyntaxTreeExtractionStage.hasAnnotation(annotations, "Id") || JavaSyntaxTreeExtractionStage.hasAnnotation(annotations, "EmbeddedId")) {
            metadata.put("jpaId", true);
            changed = true;
        }
        if (JavaSyntaxTreeExtractionStage.hasAnnotation(annotations, "Version")) {
            metadata.put("jpaVersion", true);
            changed = true;
        }
        if (JavaSyntaxTreeExtractionStage.hasAnnotation(annotations, "Embedded") || JavaSyntaxTreeExtractionStage.hasAnnotation(annotations, "EmbeddedId")) {
            metadata.put("jpaEmbedded", true);
            changed = true;
        }
        JavaSyntaxTreeExtractionStage.extractJpaColumnName(snippet).ifPresent(column -> metadata.put("columnName", column));
        if (snippet != null && snippet.contains("nullable = false")) {
            metadata.put("nullable", false);
            changed = true;
        }
        if (snippet != null && snippet.contains("nullable = true")) {
            metadata.put("nullable", true);
            changed = true;
        }
        if (snippet != null && snippet.contains("unique = true")) {
            metadata.put("unique", true);
            changed = true;
        }
        if (JavaSyntaxTreeExtractionStage.extractJpaColumnName(snippet).isPresent()) {
            changed = true;
        }
        Optional<String> association = JavaSyntaxTreeExtractionStage.detectJpaAssociation(annotations, snippet);
        if (association.isEmpty()) {
            String loweredSnippet = snippet == null ? "" : snippet.toLowerCase(java.util.Locale.ROOT);
            if (loweredSnippet.contains("manytoone") || loweredSnippet.contains("many_to_one") || loweredSnippet.contains("many-to-one")) {
                association = Optional.of("many-to-one");
            } else if (loweredSnippet.contains("onetomany") || loweredSnippet.contains("one_to_many") || loweredSnippet.contains("one-to-many")) {
                association = Optional.of("one-to-many");
            } else if (loweredSnippet.contains("onetoone") || loweredSnippet.contains("one_to_one") || loweredSnippet.contains("one-to-one")) {
                association = Optional.of("one-to-one");
            } else if (loweredSnippet.contains("manytomany") || loweredSnippet.contains("many_to_many") || loweredSnippet.contains("many-to-many")) {
                association = Optional.of("many-to-many");
            }
        }
        String declaredType = String.valueOf(methodEntity.metadata().getOrDefault("returnType", ""));
        if (declaredType == null || declaredType.isBlank()) {
            declaredType = JavaSyntaxTreeExtractionStage.inferJavaMethodReturnTypeFromSnippet(snippet, methodName).orElse("");
        }
        if (association.isPresent()) {
            String associationKind = association.get();
            metadata.put("jpaAssociation", associationKind);
            JavaSyntaxTreeExtractionStage.extractJpaMappedBy(snippet).ifPresent(mappedBy -> metadata.put("mappedBy", mappedBy));
            JavaSyntaxTreeExtractionStage.extractJpaJoinColumn(snippet).ifPresent(joinColumn -> metadata.put("joinColumn", joinColumn));
            JavaSyntaxTreeExtractionStage.extractJpaJoinTable(snippet).ifPresent(joinTable -> metadata.put("joinTable", joinTable));
            changed = true;
            List<String> referencedTypes = JavaRelationshipEvidenceEmitter.extractReferencedTypes(declaredType);
            if (!referencedTypes.isEmpty()) {
                JavaRelationshipEvidenceEmitter.ResolvedJavaType target = resolveJavaTypeReference(
                    accumulator,
                    referencedTypes.getLast(),
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    JavaSyntaxTreeExtractionStage.lineOf(methodEntity.sourceRefs().isEmpty() ? null : methodEntity.sourceRefs().getFirst(), methodNode),
                    importsBySimpleName,
                    declaredTypes
                );
                if (target != null && !ownerTypeEntityId.equals(target.entityId())) {
                    LinkedHashMap<String, Object> relationshipMetadata = new LinkedHashMap<>();
                    relationshipMetadata.put("framework", "jpa");
                    relationshipMetadata.put("relationshipType", "hasAssociation");
                    relationshipMetadata.put("jpaAssociation", associationKind);
                    JavaSyntaxTreeExtractionStage.extractJpaMappedBy(snippet).ifPresent(mappedBy -> relationshipMetadata.put("mappedBy", mappedBy));
                    JavaSyntaxTreeExtractionStage.extractJpaJoinColumn(snippet).ifPresent(joinColumn -> relationshipMetadata.put("joinColumn", joinColumn));
                    JavaSyntaxTreeExtractionStage.extractJpaJoinTable(snippet).ifPresent(joinTable -> relationshipMetadata.put("joinTable", joinTable));
                    relationshipMetadata.put("ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName);
                    relationshipMetadata.put("ownerMemberKind", "method");
                    relationshipMetadata.put("ownerMemberName", methodName);
                    relationshipMetadata.put("ownerPropertyName", propertyName);
                    accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                        ownerTypeEntityId,
                        target.entityId(),
                        target.label(),
                        ref,
                        "java",
                        Map.copyOf(relationshipMetadata)
                    ));
                }
            }
        }
        if (Boolean.TRUE.equals(metadata.get("jpaEmbedded"))) {
            List<String> referencedTypes = JavaRelationshipEvidenceEmitter.extractReferencedTypes(declaredType);
            if (!referencedTypes.isEmpty()) {
                JavaRelationshipEvidenceEmitter.ResolvedJavaType target = resolveJavaTypeReference(
                    accumulator,
                    referencedTypes.getLast(),
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    JavaSyntaxTreeExtractionStage.lineOf(methodEntity.sourceRefs().isEmpty() ? null : methodEntity.sourceRefs().getFirst(), methodNode),
                    importsBySimpleName,
                    declaredTypes
                );
                if (target != null && !ownerTypeEntityId.equals(target.entityId())) {
                    accumulator.addRelationship(ExtractionSupport.typedRelationship(
                        info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.DEPENDS_ON,
                        "embeds-jpa-property-type",
                        ownerTypeEntityId,
                        target.entityId(),
                        target.label(),
                        ref,
                        "java",
                        Map.of(
                            "framework", "jpa",
                            "relationshipType", "embeds",
                            "ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName,
                            "ownerMemberKind", "method",
                            "ownerMemberName", methodName,
                            "ownerPropertyName", propertyName
                        )
                    ));
                }
            }
        }
        if (changed) {
            accumulator.addEntity(new ExtractedEntityFact(
                methodEntity.id(),
                methodEntity.kind(),
                methodEntity.origin(),
                methodEntity.name(),
                methodEntity.displayName(),
                methodEntity.scopeId(),
                List.of(ref),
                Map.copyOf(metadata)
            ));
        }
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
        String typeSnippet = JavaTypeSemanticsFlow.typeNodeSnippet(typeNode, typeEntity);
        if (typeEntity == null || !JavaSyntaxTreeExtractionStage.isJpaPersistentType(typeSnippet, typeEntity)) {
            return;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(typeNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, typeSnippet, Map.of("language", "java", "kind", typeNode.type()));
        for (String parentType : JavaSyntaxTreeExtractionStage.extractExtendedTypes(typeNode)) {
            JavaRelationshipEvidenceEmitter.ResolvedJavaType resolved = resolveJavaTypeReference(accumulator, parentType, EntityKind.CLASS, relativePath, packageName, line, importsBySimpleName, declaredTypes);
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
    
}
