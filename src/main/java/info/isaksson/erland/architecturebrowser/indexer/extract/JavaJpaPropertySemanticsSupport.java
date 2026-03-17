package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JavaJpaPropertySemanticsSupport {
    private final JavaJpaAssociationSemanticsSupport associationSemantics;

    JavaJpaPropertySemanticsSupport(JavaJpaAssociationSemanticsSupport associationSemantics) {
        this.associationSemantics = associationSemantics;
    }

    void addJpaFieldFacts(ExtractionAccumulator accumulator, JavaFieldContext fieldContext) {
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
        if (fieldEntity == null || ownerTypeEntityId == null || !JavaJpaDomainSemanticsSupport.isJpaPersistentType(ownerTypeSnippet, null)) {
            return;
        }
        String snippet = fieldEntity.sourceRefs().isEmpty() ? (fieldNode == null ? "" : fieldNode.textSnippet()) : fieldEntity.sourceRefs().getFirst().snippet();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(fieldEntity.metadata());
        metadata.put("framework", "jpa");
        boolean changed = enrichCommonJpaPropertyMetadata(metadata, JavaDeclaredTypeSupport.metadataStringList(fieldEntity.metadata().get("annotations")), snippet);
        Optional<String> association = JavaJpaDomainSemanticsSupport.detectJpaAssociation(snippet);
        if (association.isPresent()) {
            String associationKind = association.get();
            metadata.put("jpaAssociation", associationKind);
            JavaJpaDomainSemanticsSupport.extractJpaMappedBy(snippet).ifPresent(mappedBy -> metadata.put("mappedBy", mappedBy));
            JavaJpaDomainSemanticsSupport.extractJpaJoinColumn(snippet).ifPresent(joinColumn -> metadata.put("joinColumn", joinColumn));
            JavaJpaDomainSemanticsSupport.extractJpaJoinTable(snippet).ifPresent(joinTable -> metadata.put("joinTable", joinTable));
            changed = true;
            String declaredType = String.valueOf(fieldEntity.metadata().getOrDefault("declaredType", ""));
            associationSemantics.emitAssociationRelationship(
                accumulator, ownerTypeEntityId, ownerQualifiedName, null, null, null, associationKind, declaredType,
                snippet, relativePath, packageName,
                JavaGenericSyntaxSupport.lineOf(fieldEntity.sourceRefs().isEmpty() ? null : fieldEntity.sourceRefs().getFirst(), fieldNode),
                fieldEntity.sourceRefs().isEmpty() ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(fieldNode), snippet, Map.of("language", "java", "kind", "field_declaration")) : fieldEntity.sourceRefs().getFirst(),
                importsBySimpleName, declaredTypes
            );
        }
        if (Boolean.TRUE.equals(metadata.get("jpaEmbedded"))) {
            String declaredType = String.valueOf(fieldEntity.metadata().getOrDefault("declaredType", ""));
            associationSemantics.emitEmbeddedRelationship(
                accumulator, ownerTypeEntityId, ownerQualifiedName, null, null, null, declaredType,
                relativePath, packageName,
                JavaGenericSyntaxSupport.lineOf(fieldEntity.sourceRefs().isEmpty() ? null : fieldEntity.sourceRefs().getFirst(), fieldNode),
                fieldEntity.sourceRefs().isEmpty() ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(fieldNode), snippet, Map.of("language", "java", "kind", "field_declaration")) : fieldEntity.sourceRefs().getFirst(),
                importsBySimpleName, declaredTypes, "embeds-jpa-type"
            );
        }
        if (changed) {
            SourceReference ref = fieldEntity.sourceRefs().isEmpty()
                ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(fieldNode), snippet, Map.of("language", "java", "kind", "field_declaration"))
                : fieldEntity.sourceRefs().getFirst();
            accumulator.addEntity(new ExtractedEntityFact(
                fieldEntity.id(), fieldEntity.kind(), fieldEntity.origin(), fieldEntity.name(), fieldEntity.displayName(), fieldEntity.scopeId(), List.of(ref), Map.copyOf(metadata)
            ));
        }
    }

    void addJpaMethodFacts(ExtractionAccumulator accumulator, JavaMethodContext methodContext) {
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
        if (methodEntity == null || ownerTypeEntityId == null || !JavaJpaDomainSemanticsSupport.isJpaPersistentType(ownerTypeSnippet, null) || JavaGenericSyntaxSupport.isConstructor(methodEntity)) {
            return;
        }
        List<String> annotations = JavaDeclaredTypeSupport.metadataStringList(methodEntity.metadata().get("annotations"));
        if (methodNode != null) {
            java.util.LinkedHashSet<String> mergedAnnotations = new java.util.LinkedHashSet<>(annotations);
            SyntaxTreeExtractionSupport.descendantsByType(methodNode, java.util.Set.of("marker_annotation", "annotation")).stream()
                .flatMap(annotationNode -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(annotationNode.textSnippet()).stream())
                .forEach(mergedAnnotations::add);
            SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(methodNode.textSnippet()).forEach(mergedAnnotations::add);
            annotations = List.copyOf(mergedAnnotations);
        }
        String parameters = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
        String methodName = methodEntity.name();
        if (!annotations.stream().anyMatch(a -> a != null && !a.isBlank()) && !JavaJpaDomainSemanticsSupport.containsJpaPropertyAnnotation(snippet)) {
            return;
        }
        String propertyName = JavaJpaDomainSemanticsSupport.deriveJavaPropertyName(methodName, parameters);
        if (propertyName == null || propertyName.isBlank()) {
            return;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(methodEntity.metadata());
        metadata.put("framework", "jpa");
        metadata.put("jpaPropertyAccess", true);
        metadata.put("jpaPropertyName", propertyName);
        boolean changed = enrichCommonJpaPropertyMetadata(metadata, annotations, snippet);
        Optional<String> association = JavaJpaDomainSemanticsSupport.detectJpaAssociation(annotations, snippet);
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
            declaredType = JavaGenericSyntaxSupport.inferJavaMethodReturnTypeFromSnippet(snippet, methodName).orElse("");
        }
        if (association.isPresent()) {
            String associationKind = association.get();
            metadata.put("jpaAssociation", associationKind);
            JavaJpaDomainSemanticsSupport.extractJpaMappedBy(snippet).ifPresent(mappedBy -> metadata.put("mappedBy", mappedBy));
            JavaJpaDomainSemanticsSupport.extractJpaJoinColumn(snippet).ifPresent(joinColumn -> metadata.put("joinColumn", joinColumn));
            JavaJpaDomainSemanticsSupport.extractJpaJoinTable(snippet).ifPresent(joinTable -> metadata.put("joinTable", joinTable));
            changed = true;
            associationSemantics.emitAssociationRelationship(
                accumulator, ownerTypeEntityId, ownerQualifiedName, "method", methodName, propertyName, associationKind,
                declaredType, snippet, relativePath, packageName,
                JavaGenericSyntaxSupport.lineOf(methodEntity.sourceRefs().isEmpty() ? null : methodEntity.sourceRefs().getFirst(), methodNode),
                ref, importsBySimpleName, declaredTypes
            );
        }
        if (Boolean.TRUE.equals(metadata.get("jpaEmbedded"))) {
            associationSemantics.emitEmbeddedRelationship(
                accumulator, ownerTypeEntityId, ownerQualifiedName, "method", methodName, propertyName,
                declaredType, relativePath, packageName,
                JavaGenericSyntaxSupport.lineOf(methodEntity.sourceRefs().isEmpty() ? null : methodEntity.sourceRefs().getFirst(), methodNode),
                ref, importsBySimpleName, declaredTypes, "embeds-jpa-property-type"
            );
        }
        if (changed) {
            accumulator.addEntity(new ExtractedEntityFact(
                methodEntity.id(), methodEntity.kind(), methodEntity.origin(), methodEntity.name(), methodEntity.displayName(), methodEntity.scopeId(), List.of(ref), Map.copyOf(metadata)
            ));
        }
    }

    private boolean enrichCommonJpaPropertyMetadata(LinkedHashMap<String, Object> metadata, List<String> annotations, String snippet) {
        boolean changed = false;
        if (JavaJpaDomainSemanticsSupport.hasAnnotation(annotations, "Id") || JavaJpaDomainSemanticsSupport.hasAnnotation(annotations, "EmbeddedId")) {
            metadata.put("jpaId", true);
            changed = true;
        }
        if (JavaJpaDomainSemanticsSupport.hasAnnotation(annotations, "Version")) {
            metadata.put("jpaVersion", true);
            changed = true;
        }
        if (JavaJpaDomainSemanticsSupport.hasAnnotation(annotations, "Embedded") || JavaJpaDomainSemanticsSupport.hasAnnotation(annotations, "EmbeddedId")) {
            metadata.put("jpaEmbedded", true);
            changed = true;
        }
        if (snippet != null) {
            JavaJpaDomainSemanticsSupport.extractJpaColumnName(snippet).ifPresent(column -> metadata.put("columnName", column));
            if (snippet.contains("nullable = false")) {
                metadata.put("nullable", false);
                changed = true;
            }
            if (snippet.contains("nullable = true")) {
                metadata.put("nullable", true);
                changed = true;
            }
            if (snippet.contains("unique = true")) {
                metadata.put("unique", true);
                changed = true;
            }
            if (JavaJpaDomainSemanticsSupport.extractJpaColumnName(snippet).isPresent()) {
                changed = true;
            }
        }
        return changed;
    }
}
