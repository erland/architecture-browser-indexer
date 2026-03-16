package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.naming.DisplayNamePolicy;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaStructuralExtractor implements StructuralExtractor {

    @Override
    public ParseLanguage language() {
        return ParseLanguage.JAVA;
    }

    @Override
    public ExtractionAccumulator extract(SourceParseResult parseResult, ExtractionAccumulator accumulator) {
        accumulator.incrementFilesVisited();
        if (!parseResult.successful() || parseResult.syntaxTree() == null) {
            accumulator.addDiagnostic(ExtractionSupport.extractionWarning(
                parseResult,
                "extract.java.syntax-tree-required",
                "Java structural extraction requires a successful Tree-sitter syntax tree; no regex fallback is used."
            ));
            return accumulator;
        }
        return extractFromSyntaxTree(parseResult, accumulator, parseResult.request().relativePath(), parseResult.syntaxTree());
    }

    private ExtractionAccumulator extractFromSyntaxTree(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        String relativePath,
        SyntaxTree syntaxTree
    ) {
        ExtractionMode extractionMode = ExtractionMode.SYNTAX_TREE;
        accumulator.incrementFilesExtracted("java", extractionMode);

        String repositoryScopeId = "scope:repo";
        var fileScope = ExtractionSupport.fileScope(repositoryScopeId, relativePath);
        accumulator.addScope(fileScope);

        SyntaxNode root = syntaxTree.root();
        String packageName = SyntaxTreeExtractionSupport.findAllByType(root, Set.of("package_declaration")).stream()
            .findFirst()
            .flatMap(node -> SyntaxTreeExtractionSupport.extractQualifiedName(node.textSnippet()))
            .orElse(derivePackageFromPath(relativePath));

        var packageScope = ExtractionSupport.packageScope(repositoryScopeId, packageName, relativePath, "java");
        accumulator.addScope(packageScope);

        var fileEntity = ExtractionSupport.fileModuleEntity(fileScope.id(), relativePath, "java");
        accumulator.addEntity(fileEntity);

        Map<String, String> importsBySimpleName = new LinkedHashMap<>();
        for (SyntaxNode importNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("import_declaration"))) {
            String imported = importQualifiedName(importNode.textSnippet()).orElse(null);
            if (imported == null || imported.isBlank()) {
                continue;
            }
            int line = SyntaxTreeExtractionSupport.oneBasedLine(importNode);
            String simpleName = simpleName(imported);
            if (simpleName != null && !simpleName.isBlank()) {
                importsBySimpleName.putIfAbsent(simpleName, imported);
            }
            var external = ExtractionSupport.externalDependencyEntity("java", imported, relativePath, line);
            accumulator.addEntity(external);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                fileEntity.id(), external.id(), imported,
                ExtractionSupport.sourceRef(relativePath, line, importNode.textSnippet(), Map.of("language", "java", "kind", "import")),
                "java",
                dependencyMetadata("import", "evidence")
            ));
        }

        Map<String, DeclaredJavaType> declaredTypes = new LinkedHashMap<>();
        collectDeclaredTypes(parseResult, relativePath, packageName, extractionMode, packageScope.id(), root, null, declaredTypes);

        extractTypeAndMethodFacts(
            parseResult,
            accumulator,
            relativePath,
            packageName,
            extractionMode,
            packageScope.id(),
            fileScope.id(),
            fileEntity.id(),
            root,
            null,
            null,
            null,
            importsBySimpleName,
            declaredTypes
        );
        return accumulator;
    }

    private void extractTypeAndMethodFacts(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        ExtractionMode extractionMode,
        String packageScopeId,
        String fileScopeId,
        String fileEntityId,
        SyntaxNode node,
        String owningTypeEntityId,
        String owningQualifiedName,
        String owningTypeSnippet,
        Map<String, String> importsBySimpleName,
        Map<String, DeclaredJavaType> declaredTypes
    ) {
        if (node == null) {
            return;
        }

        String currentOwningTypeEntityId = owningTypeEntityId;
        String currentOwningQualifiedName = owningQualifiedName;
        String currentOwningTypeSnippet = owningTypeSnippet;
        if (isJavaTypeDeclaration(node)) {
            ExtractedEntityFact typeEntity = toTypeEntity(parseResult, relativePath, packageName, extractionMode, packageScopeId, node, owningQualifiedName);
            if (typeEntity != null) {
                accumulator.addEntity(typeEntity);
                SourceReference ref = typeEntity.sourceRefs().isEmpty() ? null : typeEntity.sourceRefs().getFirst();
                accumulator.addRelationship(ExtractionSupport.containsRelationship(fileEntityId, typeEntity.id(), ref));
                addTypeRelationships(accumulator, relativePath, packageName, node, typeEntity, importsBySimpleName, declaredTypes);
                addJaxRsResourceMetadata(accumulator, relativePath, node, typeEntity);
                addJpaTypeMetadata(accumulator, relativePath, typeNodeSnippet(node, typeEntity), typeEntity);
                addJpaInheritanceFacts(
                    accumulator,
                    relativePath,
                    packageName,
                    node,
                    typeEntity,
                    importsBySimpleName,
                    declaredTypes
                );
                currentOwningTypeEntityId = typeEntity.id();
                currentOwningTypeSnippet = node.textSnippet();
                Object qualifiedName = typeEntity.metadata().get("qualifiedName");
                currentOwningQualifiedName = qualifiedName == null ? owningQualifiedName : String.valueOf(qualifiedName);
            }
        } else if (isJavaFieldDeclaration(node)) {
            for (ExtractedEntityFact fieldEntity : toFieldEntities(parseResult, relativePath, extractionMode, fileScopeId, node, currentOwningQualifiedName)) {
                accumulator.addEntity(fieldEntity);
                SourceReference ref = fieldEntity.sourceRefs().isEmpty() ? null : fieldEntity.sourceRefs().getFirst();
                String dependencySourceEntityId = currentOwningTypeEntityId == null ? fileEntityId : currentOwningTypeEntityId;
                accumulator.addRelationship(ExtractionSupport.containsRelationship(
                    dependencySourceEntityId,
                    fieldEntity.id(),
                    ref
                ));
                addDeclaredTypeDependencies(
                    accumulator,
                    dependencySourceEntityId,
                    List.of(String.valueOf(fieldEntity.metadata().getOrDefault("declaredType", ""))),
                    relativePath,
                    packageName,
                    lineOf(ref, node),
                    ref,
                    importsBySimpleName,
                    declaredTypes,
                    dependencyMetadata("field", "composition")
                );
                addJpaFieldFacts(
                    accumulator,
                    relativePath,
                    packageName,
                    node,
                    fieldEntity,
                    currentOwningTypeEntityId,
                    currentOwningQualifiedName,
                    currentOwningTypeSnippet,
                    importsBySimpleName,
                    declaredTypes
                );
            }
        } else if (isJavaMethodLikeDeclaration(node)) {
            ExtractedEntityFact methodEntity = toMethodEntity(parseResult, relativePath, extractionMode, fileScopeId, node, currentOwningQualifiedName);
            if (methodEntity != null) {
                accumulator.addEntity(methodEntity);
                SourceReference ref = methodEntity.sourceRefs().isEmpty() ? null : methodEntity.sourceRefs().getFirst();
                String dependencySourceEntityId = currentOwningTypeEntityId == null ? fileEntityId : currentOwningTypeEntityId;
                accumulator.addRelationship(ExtractionSupport.containsRelationship(
                    dependencySourceEntityId,
                    methodEntity.id(),
                    ref
                ));
                String returnType = String.valueOf(methodEntity.metadata().getOrDefault("returnType", ""));
                if (!returnType.isBlank()) {
                    addDeclaredTypeDependencies(
                        accumulator,
                        dependencySourceEntityId,
                        List.of(returnType),
                        relativePath,
                        packageName,
                        lineOf(ref, node),
                        ref,
                        importsBySimpleName,
                        declaredTypes,
                        dependencyMetadata("returnType", "api")
                    );
                }
                @SuppressWarnings("unchecked")
                List<String> parameterTypes = (List<String>) methodEntity.metadata().getOrDefault("parameterTypes", List.of());
                if (!parameterTypes.isEmpty()) {
                    addDeclaredTypeDependencies(
                        accumulator,
                        dependencySourceEntityId,
                        parameterTypes,
                        relativePath,
                        packageName,
                        lineOf(ref, node),
                        ref,
                        importsBySimpleName,
                        declaredTypes,
                        dependencyMetadata(isConstructor(methodEntity) ? "constructorParameter" : "parameterType", "api")
                    );
                }
                addJaxRsEndpointFacts(
                    accumulator,
                    relativePath,
                    node,
                    currentOwningTypeEntityId,
                    currentOwningQualifiedName,
                    currentOwningTypeSnippet,
                    methodEntity
                );
                addJpaMethodFacts(
                    accumulator,
                    relativePath,
                    packageName,
                    node,
                    methodEntity,
                    currentOwningTypeEntityId,
                    currentOwningQualifiedName,
                    currentOwningTypeSnippet,
                    importsBySimpleName,
                    declaredTypes
                );
                addCdiEventFacts(
                    accumulator,
                    relativePath,
                    packageName,
                    node,
                    methodEntity,
                    currentOwningTypeEntityId,
                    currentOwningQualifiedName,
                    currentOwningTypeSnippet,
                    parseResult.request() == null ? null : parseResult.request().sourceText(),
                    importsBySimpleName,
                    declaredTypes
                );
                addWritePathFacts(
                    accumulator,
                    relativePath,
                    packageName,
                    node,
                    methodEntity,
                    currentOwningTypeEntityId,
                    currentOwningQualifiedName,
                    currentOwningTypeSnippet,
                    importsBySimpleName,
                    declaredTypes
                );
            }
        }

        for (SyntaxNode child : node.children()) {
            extractTypeAndMethodFacts(
                parseResult,
                accumulator,
                relativePath,
                packageName,
                extractionMode,
                packageScopeId,
                fileScopeId,
                fileEntityId,
                child,
                currentOwningTypeEntityId,
                currentOwningQualifiedName,
                currentOwningTypeSnippet,
                importsBySimpleName,
                declaredTypes
            );
        }
    }



    private void addJaxRsResourceMetadata(
        ExtractionAccumulator accumulator,
        String relativePath,
        SyntaxNode typeNode,
        ExtractedEntityFact typeEntity
    ) {
        if (typeEntity == null || typeEntity.kind() != EntityKind.CLASS || !isJaxRsResource(typeEntity)) {
            return;
        }
        String basePath = extractJaxRsPath(typeEntity.sourceRefs().isEmpty() ? (typeNode == null ? "" : typeNode.textSnippet()) : typeEntity.sourceRefs().getFirst().snippet())
            .orElse("/");
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(typeEntity.metadata());
        metadata.put("framework", "jax-rs");
        metadata.put("jaxRsResource", true);
        metadata.put("jaxRsBasePath", normalizeJaxRsPath(basePath));
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

    private void addJaxRsEndpointFacts(
        ExtractionAccumulator accumulator,
        String relativePath,
        SyntaxNode methodNode,
        String ownerTypeEntityId,
        String ownerQualifiedName,
        String ownerTypeSnippet,
        ExtractedEntityFact methodEntity
    ) {
        if (methodEntity == null || ownerTypeEntityId == null || ownerQualifiedName == null || ownerQualifiedName.isBlank()) {
            return;
        }
        List<String> annotations = metadataStringList(methodEntity.metadata().get("annotations"));
        String httpMethod = jaxRsHttpMethod(annotations).orElse(null);
        if (httpMethod == null) {
            return;
        }
        SourceReference methodRef = methodEntity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(methodNode), methodNode.textSnippet(), Map.of("language", "java", "kind", methodNode.type()))
            : methodEntity.sourceRefs().getFirst();
        String classSnippet = ownerTypeSnippet == null ? "" : ownerTypeSnippet;
        String classPath = extractJaxRsPath(classSnippet).orElse("");
        String methodPath = extractJaxRsPath(methodRef.snippet()).orElse("");
        String resolvedPath = normalizeJaxRsEndpointPath(classPath, methodPath);
        String endpointName = httpMethod + " " + resolvedPath;
        int endpointLine = methodRef.startLine() == null ? SyntaxTreeExtractionSupport.oneBasedLine(methodNode) : methodRef.startLine();
        String endpointId = IdUtils.scopedEntityId("java-endpoint", relativePath, ownerQualifiedName + "#" + endpointName, endpointLine);
        List<Map<String, String>> parameterDetails = extractJaxRsParameterDetails(String.valueOf(methodEntity.metadata().getOrDefault("parameters", "()")));
        LinkedHashMap<String, Object> endpointMetadata = new LinkedHashMap<>();
        endpointMetadata.put("language", "java");
        endpointMetadata.put("framework", "jax-rs");
        endpointMetadata.put("httpMethod", httpMethod);
        endpointMetadata.put("path", resolvedPath);
        endpointMetadata.put("classLevelPath", normalizeJaxRsPath(classPath));
        endpointMetadata.put("methodLevelPath", normalizeJaxRsPath(methodPath));
        endpointMetadata.put("resourceQualifiedName", ownerQualifiedName);
        endpointMetadata.put("methodName", methodEntity.name());
        endpointMetadata.put("methodQualifiedName", ownerQualifiedName + "#" + methodEntity.name());
        endpointMetadata.put("parameterDetails", parameterDetails);
        endpointMetadata.put("annotations", annotations);
        accumulator.addEntity(new ExtractedEntityFact(
            endpointId,
            EntityKind.ENDPOINT,
            EntityOrigin.OBSERVED,
            endpointName,
            endpointName,
            methodEntity.scopeId(),
            List.of(methodRef),
            Map.copyOf(endpointMetadata)
        ));
        accumulator.addRelationship(ExtractionSupport.typedRelationship(
            info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.EXPOSES,
            "exposes-jaxrs-endpoint",
            ownerTypeEntityId,
            endpointId,
            endpointName,
            methodRef,
            "java",
            Map.of("framework", "jax-rs", "httpMethod", httpMethod, "path", resolvedPath)
        ));
        LinkedHashMap<String, Object> methodMetadata = new LinkedHashMap<>(methodEntity.metadata());
        methodMetadata.put("framework", "jax-rs");
        methodMetadata.put("jaxRsEndpoint", true);
        methodMetadata.put("httpMethod", httpMethod);
        methodMetadata.put("path", resolvedPath);
        methodMetadata.put("parameterDetails", parameterDetails);
        accumulator.addEntity(new ExtractedEntityFact(
            methodEntity.id(),
            methodEntity.kind(),
            methodEntity.origin(),
            methodEntity.name(),
            methodEntity.displayName(),
            methodEntity.scopeId(),
            List.of(methodRef),
            Map.copyOf(methodMetadata)
        ));
    }


    private static String typeNodeSnippet(SyntaxNode typeNode, ExtractedEntityFact typeEntity) {
        if (typeEntity != null && !typeEntity.sourceRefs().isEmpty() && typeEntity.sourceRefs().getFirst().snippet() != null) {
            return typeEntity.sourceRefs().getFirst().snippet();
        }
        return typeNode == null ? "" : typeNode.textSnippet();
    }

    private void addJpaTypeMetadata(
        ExtractionAccumulator accumulator,
        String relativePath,
        String typeSnippet,
        ExtractedEntityFact typeEntity
    ) {
        if (typeEntity == null || typeEntity.kind() != EntityKind.CLASS || !isJpaPersistentType(typeSnippet, typeEntity)) {
            return;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(typeEntity.metadata());
        metadata.put("framework", "jpa");
        String jpaKind = detectJpaTypeKind(typeSnippet, typeEntity).orElse("entity");
        metadata.put("jpaKind", jpaKind);
        metadata.put("jpaEntity", "entity".equals(jpaKind));
        metadata.put("jpaEmbeddable", "embeddable".equals(jpaKind));
        metadata.put("jpaMappedSuperclass", "mapped-superclass".equals(jpaKind));
        extractJpaTableName(typeSnippet).ifPresent(table -> metadata.put("tableName", table));
        extractJpaInheritanceStrategy(typeSnippet).ifPresent(strategy -> metadata.put("inheritanceStrategy", strategy));
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

    private void addJpaFieldFacts(
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        SyntaxNode fieldNode,
        ExtractedEntityFact fieldEntity,
        String ownerTypeEntityId,
        String ownerQualifiedName,
        String ownerTypeSnippet,
        Map<String, String> importsBySimpleName,
        Map<String, DeclaredJavaType> declaredTypes
    ) {
        if (fieldEntity == null || ownerTypeEntityId == null || !isJpaPersistentType(ownerTypeSnippet, null)) {
            return;
        }
        String snippet = fieldEntity.sourceRefs().isEmpty() ? (fieldNode == null ? "" : fieldNode.textSnippet()) : fieldEntity.sourceRefs().getFirst().snippet();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(fieldEntity.metadata());
        metadata.put("framework", "jpa");
        boolean changed = false;
        if (hasAnnotation(metadataStringList(fieldEntity.metadata().get("annotations")), "Id") || hasAnnotation(metadataStringList(fieldEntity.metadata().get("annotations")), "EmbeddedId")) {
            metadata.put("jpaId", true);
            changed = true;
        }
        if (hasAnnotation(metadataStringList(fieldEntity.metadata().get("annotations")), "Version")) {
            metadata.put("jpaVersion", true);
            changed = true;
        }
        if (hasAnnotation(metadataStringList(fieldEntity.metadata().get("annotations")), "Embedded") || hasAnnotation(metadataStringList(fieldEntity.metadata().get("annotations")), "EmbeddedId")) {
            metadata.put("jpaEmbedded", true);
            changed = true;
        }
        extractJpaColumnName(snippet).ifPresent(column -> {
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
        if (extractJpaColumnName(snippet).isPresent()) {
            changed = true;
        }
        Optional<String> association = detectJpaAssociation(snippet);
        if (association.isPresent()) {
            String associationKind = association.get();
            metadata.put("jpaAssociation", associationKind);
            extractJpaMappedBy(snippet).ifPresent(mappedBy -> metadata.put("mappedBy", mappedBy));
            extractJpaJoinColumn(snippet).ifPresent(joinColumn -> metadata.put("joinColumn", joinColumn));
            extractJpaJoinTable(snippet).ifPresent(joinTable -> metadata.put("joinTable", joinTable));
            changed = true;

            String declaredType = String.valueOf(fieldEntity.metadata().getOrDefault("declaredType", ""));
            List<String> referencedTypes = extractReferencedTypes(declaredType);
            if (!referencedTypes.isEmpty()) {
                ResolvedJavaType target = resolveJavaTypeReference(
                    accumulator,
                    referencedTypes.getLast(),
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    lineOf(fieldEntity.sourceRefs().isEmpty() ? null : fieldEntity.sourceRefs().getFirst(), fieldNode),
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
                    extractJpaMappedBy(snippet).ifPresent(mappedBy -> relationshipMetadata.put("mappedBy", mappedBy));
                    extractJpaJoinColumn(snippet).ifPresent(joinColumn -> relationshipMetadata.put("joinColumn", joinColumn));
                    extractJpaJoinTable(snippet).ifPresent(joinTable -> relationshipMetadata.put("joinTable", joinTable));
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
            List<String> referencedTypes = extractReferencedTypes(declaredType);
            if (!referencedTypes.isEmpty()) {
                ResolvedJavaType target = resolveJavaTypeReference(
                    accumulator,
                    referencedTypes.getLast(),
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    lineOf(fieldEntity.sourceRefs().isEmpty() ? null : fieldEntity.sourceRefs().getFirst(), fieldNode),
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

    private void addJpaMethodFacts(
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        SyntaxNode methodNode,
        ExtractedEntityFact methodEntity,
        String ownerTypeEntityId,
        String ownerQualifiedName,
        String ownerTypeSnippet,
        Map<String, String> importsBySimpleName,
        Map<String, DeclaredJavaType> declaredTypes
    ) {
        if (methodEntity == null || ownerTypeEntityId == null || !isJpaPersistentType(ownerTypeSnippet, null) || isConstructor(methodEntity)) {
            return;
        }
        List<String> annotations = metadataStringList(methodEntity.metadata().get("annotations"));
        if (methodNode != null) {
            java.util.LinkedHashSet<String> mergedAnnotations = new java.util.LinkedHashSet<>(annotations);
            SyntaxTreeExtractionSupport.descendantsByType(methodNode, Set.of("marker_annotation", "annotation")).stream()
                .flatMap(annotationNode -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(annotationNode.textSnippet()).stream())
                .forEach(mergedAnnotations::add);
            SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(methodNode.textSnippet()).forEach(mergedAnnotations::add);
            annotations = List.copyOf(mergedAnnotations);
        }
        String snippet = methodEntity.sourceRefs().isEmpty() ? (methodNode == null ? "" : methodNode.textSnippet()) : methodEntity.sourceRefs().getFirst().snippet();
        if ((snippet == null || snippet.isBlank()) && methodNode != null) {
            snippet = methodNode.textSnippet();
        }
        String parameters = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
        String methodName = methodEntity.name();
        if (!annotations.stream().anyMatch(a -> a != null && !a.isBlank()) && !containsJpaPropertyAnnotation(snippet)) {
            return;
        }
        String propertyName = deriveJavaPropertyName(methodName, parameters);
        if (propertyName == null || propertyName.isBlank()) {
            return;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(methodEntity.metadata());
        metadata.put("framework", "jpa");
        metadata.put("jpaPropertyAccess", true);
        metadata.put("jpaPropertyName", propertyName);
        boolean changed = true;
        if (hasAnnotation(annotations, "Id") || hasAnnotation(annotations, "EmbeddedId")) {
            metadata.put("jpaId", true);
            changed = true;
        }
        if (hasAnnotation(annotations, "Version")) {
            metadata.put("jpaVersion", true);
            changed = true;
        }
        if (hasAnnotation(annotations, "Embedded") || hasAnnotation(annotations, "EmbeddedId")) {
            metadata.put("jpaEmbedded", true);
            changed = true;
        }
        extractJpaColumnName(snippet).ifPresent(column -> metadata.put("columnName", column));
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
        if (extractJpaColumnName(snippet).isPresent()) {
            changed = true;
        }
        Optional<String> association = detectJpaAssociation(annotations, snippet);
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
            declaredType = inferJavaMethodReturnTypeFromSnippet(snippet, methodName).orElse("");
        }
        if (association.isPresent()) {
            String associationKind = association.get();
            metadata.put("jpaAssociation", associationKind);
            extractJpaMappedBy(snippet).ifPresent(mappedBy -> metadata.put("mappedBy", mappedBy));
            extractJpaJoinColumn(snippet).ifPresent(joinColumn -> metadata.put("joinColumn", joinColumn));
            extractJpaJoinTable(snippet).ifPresent(joinTable -> metadata.put("joinTable", joinTable));
            changed = true;
            List<String> referencedTypes = extractReferencedTypes(declaredType);
            if (!referencedTypes.isEmpty()) {
                ResolvedJavaType target = resolveJavaTypeReference(
                    accumulator,
                    referencedTypes.getLast(),
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    lineOf(methodEntity.sourceRefs().isEmpty() ? null : methodEntity.sourceRefs().getFirst(), methodNode),
                    importsBySimpleName,
                    declaredTypes
                );
                if (target != null && !ownerTypeEntityId.equals(target.entityId())) {
                    SourceReference ref = methodEntity.sourceRefs().isEmpty()
                        ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(methodNode), snippet, Map.of("language", "java", "kind", "method_declaration"))
                        : methodEntity.sourceRefs().getFirst();
                    LinkedHashMap<String, Object> relationshipMetadata = new LinkedHashMap<>();
                    relationshipMetadata.put("framework", "jpa");
                    relationshipMetadata.put("relationshipType", "hasAssociation");
                    relationshipMetadata.put("jpaAssociation", associationKind);
                    extractJpaMappedBy(snippet).ifPresent(mappedBy -> relationshipMetadata.put("mappedBy", mappedBy));
                    extractJpaJoinColumn(snippet).ifPresent(joinColumn -> relationshipMetadata.put("joinColumn", joinColumn));
                    extractJpaJoinTable(snippet).ifPresent(joinTable -> relationshipMetadata.put("joinTable", joinTable));
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
            List<String> referencedTypes = extractReferencedTypes(declaredType);
            if (!referencedTypes.isEmpty()) {
                ResolvedJavaType target = resolveJavaTypeReference(
                    accumulator,
                    referencedTypes.getLast(),
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    lineOf(methodEntity.sourceRefs().isEmpty() ? null : methodEntity.sourceRefs().getFirst(), methodNode),
                    importsBySimpleName,
                    declaredTypes
                );
                if (target != null && !ownerTypeEntityId.equals(target.entityId())) {
                    SourceReference ref = methodEntity.sourceRefs().isEmpty()
                        ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(methodNode), snippet, Map.of("language", "java", "kind", "method_declaration"))
                        : methodEntity.sourceRefs().getFirst();
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
            SourceReference ref = methodEntity.sourceRefs().isEmpty()
                ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(methodNode), snippet, Map.of("language", "java", "kind", "method_declaration"))
                : methodEntity.sourceRefs().getFirst();
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



    private void addCdiEventFacts(
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        SyntaxNode methodNode,
        ExtractedEntityFact methodEntity,
        String ownerTypeEntityId,
        String ownerQualifiedName,
        String ownerTypeSnippet,
        String sourceText,
        Map<String, String> importsBySimpleName,
        Map<String, DeclaredJavaType> declaredTypes
    ) {
        if (methodEntity == null || ownerTypeEntityId == null || ownerQualifiedName == null || ownerQualifiedName.isBlank()) {
            return;
        }
        String snippet = methodEntity.sourceRefs().isEmpty() ? (methodNode == null ? "" : methodNode.textSnippet()) : methodEntity.sourceRefs().getFirst().snippet();
        if ((snippet == null || snippet.isBlank()) && methodNode != null) {
            snippet = methodNode.textSnippet();
        }
        String exactMethodSnippet = exactNodeSnippet(sourceText, methodNode);
        if (exactMethodSnippet != null && !exactMethodSnippet.isBlank()) {
            snippet = exactMethodSnippet;
        }
        SourceReference ref = methodEntity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(methodNode), snippet, Map.of("language", "java", "kind", methodNode == null ? "method_declaration" : methodNode.type()))
            : methodEntity.sourceRefs().getFirst();

        LinkedHashMap<String, Object> methodMetadata = new LinkedHashMap<>(methodEntity.metadata());
        boolean methodChanged = false;

        for (PublishedCdiEvent publication : detectCdiPublishedEvents(snippet, ownerTypeSnippet)) {
            ResolvedJavaType target = resolveJavaTypeReference(
                accumulator,
                publication.eventType(),
                EntityKind.CLASS,
                relativePath,
                packageName,
                lineOf(ref, methodNode),
                importsBySimpleName,
                declaredTypes
            );
            if (target == null) {
                continue;
            }
            LinkedHashMap<String, Object> relationshipMetadata = new LinkedHashMap<>();
            relationshipMetadata.put("framework", "cdi");
            relationshipMetadata.put("relationshipType", "publishesEvent");
            relationshipMetadata.put("frameworkRelationship", "publishesEvent");
            relationshipMetadata.put("dependencySource", "eventPublish");
            relationshipMetadata.put("eventType", target.label());
            relationshipMetadata.put("publisherMethod", methodEntity.name());
            relationshipMetadata.put("publisherQualifiedName", ownerQualifiedName);
            relationshipMetadata.put("publisherAsync", publication.async());
            if (publication.publisherField() != null && !publication.publisherField().isBlank()) {
                relationshipMetadata.put("publisherField", publication.publisherField());
            }
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                ownerTypeEntityId,
                target.entityId(),
                target.label(),
                ref,
                "java",
                Map.copyOf(relationshipMetadata)
            ));
            LinkedHashMap<String, Object> methodRelationshipMetadata = new LinkedHashMap<>(relationshipMetadata);
            methodRelationshipMetadata.put("dependencySource", "eventPublishMethod");
            methodRelationshipMetadata.put("ownerMemberKind", "method");
            methodRelationshipMetadata.put("ownerMemberName", methodEntity.name());
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                methodEntity.id(),
                target.entityId(),
                target.label(),
                ref,
                "java",
                Map.copyOf(methodRelationshipMetadata)
            ));
            methodMetadata.put("framework", "cdi");
            methodMetadata.put("cdiEventPublisher", true);
            methodMetadata.put("cdiPublishedEventType", target.label());
            methodMetadata.put("cdiPublisherAsync", publication.async());
            methodChanged = true;
        }

        Optional<ObservedCdiEvent> observer = detectCdiObservedEvent(methodEntity, snippet);
        if (observer.isPresent()) {
            ObservedCdiEvent observed = observer.get();
            ResolvedJavaType target = resolveJavaTypeReference(
                accumulator,
                observed.eventType(),
                EntityKind.CLASS,
                relativePath,
                packageName,
                lineOf(ref, methodNode),
                importsBySimpleName,
                declaredTypes
            );
            if (target != null) {
                LinkedHashMap<String, Object> eventToObserverMetadata = new LinkedHashMap<>();
                eventToObserverMetadata.put("framework", "cdi");
                eventToObserverMetadata.put("relationshipType", "eventObservedBy");
                eventToObserverMetadata.put("frameworkRelationship", "observesEvent");
                eventToObserverMetadata.put("eventType", target.label());
                eventToObserverMetadata.put("observerQualifiedName", ownerQualifiedName);
                eventToObserverMetadata.put("observerMethod", methodEntity.name());
                eventToObserverMetadata.put("observerAsync", observed.async());
                if (!observed.qualifiers().isEmpty()) {
                    eventToObserverMetadata.put("observerQualifiers", observed.qualifiers());
                }
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    target.entityId(),
                    ownerTypeEntityId,
                    ownerQualifiedName,
                    ref,
                    "java",
                    Map.copyOf(eventToObserverMetadata)
                ));
                LinkedHashMap<String, Object> methodToEventMetadata = new LinkedHashMap<>();
                methodToEventMetadata.put("framework", "cdi");
                methodToEventMetadata.put("relationshipType", "observesEvent");
                methodToEventMetadata.put("frameworkRelationship", "observesEvent");
                methodToEventMetadata.put("eventType", target.label());
                methodToEventMetadata.put("observerAsync", observed.async());
                methodToEventMetadata.put("ownerMemberKind", "method");
                methodToEventMetadata.put("ownerMemberName", methodEntity.name());
                if (!observed.qualifiers().isEmpty()) {
                    methodToEventMetadata.put("observerQualifiers", observed.qualifiers());
                }
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    methodEntity.id(),
                    target.entityId(),
                    target.label(),
                    ref,
                    "java",
                    Map.copyOf(methodToEventMetadata)
                ));
                methodMetadata.put("framework", "cdi");
                methodMetadata.put("cdiObserver", true);
                methodMetadata.put("cdiObservedEventType", target.label());
                methodMetadata.put("observerAsync", observed.async());
                methodMetadata.put("observerQualifiers", observed.qualifiers());
                methodChanged = true;
            }
        }

        if (methodChanged) {
            accumulator.addEntity(new ExtractedEntityFact(
                methodEntity.id(),
                methodEntity.kind(),
                methodEntity.origin(),
                methodEntity.name(),
                methodEntity.displayName(),
                methodEntity.scopeId(),
                List.of(ref),
                Map.copyOf(methodMetadata)
            ));
        }
    }


    private void addWritePathFacts(
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        SyntaxNode methodNode,
        ExtractedEntityFact methodEntity,
        String ownerTypeEntityId,
        String ownerQualifiedName,
        String ownerTypeSnippet,
        Map<String, String> importsBySimpleName,
        Map<String, DeclaredJavaType> declaredTypes
    ) {
        if (methodEntity == null || ownerTypeEntityId == null) {
            return;
        }
        String snippet = methodEntity.sourceRefs().isEmpty() ? (methodNode == null ? "" : methodNode.textSnippet()) : methodEntity.sourceRefs().getFirst().snippet();
        if ((snippet == null || snippet.isBlank()) && methodNode != null) {
            snippet = methodNode.textSnippet();
        }
        SourceReference ref = methodEntity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(methodNode), snippet, Map.of("language", "java", "kind", methodNode == null ? "method_declaration" : methodNode.type()))
            : methodEntity.sourceRefs().getFirst();

        List<DetectedWritePath> detections = new ArrayList<>();
        detections.addAll(detectJpaWriteOperations(methodEntity, snippet));
        detections.addAll(detectRepositoryWriteOperations(methodEntity, snippet));
        if (detections.isEmpty()) {
            return;
        }

        Map<String, String> variableTypes = collectMethodVariableTypes(methodEntity, snippet);
        LinkedHashMap<String, Object> methodMetadata = new LinkedHashMap<>(methodEntity.metadata());
        java.util.LinkedHashSet<String> writeOperations = new java.util.LinkedHashSet<>(metadataStringList(methodMetadata.get("writeOperations")));
        java.util.LinkedHashSet<String> writeTargets = new java.util.LinkedHashSet<>(metadataStringList(methodMetadata.get("writeEntityTypes")));
        boolean changed = false;

        for (DetectedWritePath detection : detections) {
            String entityType = resolveWriteTargetEntityType(detection.argumentExpression(), variableTypes).orElse(null);
            if (entityType == null || entityType.isBlank()) {
                continue;
            }
            ResolvedJavaType target = resolveJavaTypeReference(
                accumulator,
                entityType,
                EntityKind.CLASS,
                relativePath,
                packageName,
                lineOf(ref, methodNode),
                importsBySimpleName,
                declaredTypes
            );
            if (target == null) {
                continue;
            }
            LinkedHashMap<String, Object> relationshipMetadata = new LinkedHashMap<>();
            relationshipMetadata.put("framework", "jpa");
            relationshipMetadata.put("relationshipType", "writePath");
            relationshipMetadata.put("writeOperation", detection.operation());
            relationshipMetadata.put("writeKind", detection.writeKind());
            relationshipMetadata.put("writerMethod", methodEntity.name());
            relationshipMetadata.put("writerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName);
            relationshipMetadata.put("entityType", target.label());
            if (detection.viaField() != null && !detection.viaField().isBlank()) {
                relationshipMetadata.put("writeViaField", detection.viaField());
            }
            if (detection.viaType() != null && !detection.viaType().isBlank()) {
                relationshipMetadata.put("writeViaType", detection.viaType());
            }
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                ownerTypeEntityId,
                target.entityId(),
                target.label(),
                ref,
                "java",
                Map.copyOf(relationshipMetadata)
            ));
            LinkedHashMap<String, Object> methodRelationshipMetadata = new LinkedHashMap<>(relationshipMetadata);
            methodRelationshipMetadata.put("ownerMemberKind", "method");
            methodRelationshipMetadata.put("ownerMemberName", methodEntity.name());
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                methodEntity.id(),
                target.entityId(),
                target.label(),
                ref,
                "java",
                Map.copyOf(methodRelationshipMetadata)
            ));
            writeOperations.add(detection.operation());
            writeTargets.add(target.label());
            changed = true;
        }

        if (changed) {
            methodMetadata.put("writePath", true);
            methodMetadata.put("writeOperations", List.copyOf(writeOperations));
            methodMetadata.put("writeEntityTypes", List.copyOf(writeTargets));
            accumulator.addEntity(new ExtractedEntityFact(
                methodEntity.id(),
                methodEntity.kind(),
                methodEntity.origin(),
                methodEntity.name(),
                methodEntity.displayName(),
                methodEntity.scopeId(),
                List.of(ref),
                Map.copyOf(methodMetadata)
            ));
        }
    }

    private static List<DetectedWritePath> detectJpaWriteOperations(ExtractedEntityFact methodEntity, String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return List.of();
        }
        List<DetectedWritePath> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*\\.\\s*(persist|merge|remove)\\s*\\(([^)]*)\\)", Pattern.DOTALL).matcher(snippet);
        while (matcher.find()) {
            result.add(new DetectedWritePath(matcher.group(2).toLowerCase(Locale.ROOT), "entity-manager", matcher.group(3).strip(), matcher.group(1), null));
        }
        return List.copyOf(result);
    }

    private static List<DetectedWritePath> detectRepositoryWriteOperations(ExtractedEntityFact methodEntity, String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return List.of();
        }
        List<DetectedWritePath> result = new ArrayList<>();
        Matcher callMatcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*\\.\\s*(saveAndFlush|save|update|delete|remove)\\s*\\(([^)]*)\\)", Pattern.DOTALL).matcher(snippet);
        while (callMatcher.find()) {
            String operation = normalizeWriteOperation(callMatcher.group(2));
            result.add(new DetectedWritePath(operation, "repository-call", callMatcher.group(3).strip(), callMatcher.group(1), null));
        }
        String ownerQualifiedName = String.valueOf(methodEntity.metadata().getOrDefault("ownerQualifiedName", ""));
        String loweredOwner = ownerQualifiedName.toLowerCase(Locale.ROOT);
        String methodName = methodEntity.name() == null ? "" : methodEntity.name();
        if (loweredOwner.contains("repository") || loweredOwner.contains("repo")) {
            String operation = normalizeWriteOperation(methodName);
            if (operation != null) {
                List<String> parameterTypes = metadataStringList(methodEntity.metadata().get("parameterTypes"));
                String params = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
                List<String> paramNames = extractParameterNames(params);
                for (int i = 0; i < Math.min(parameterTypes.size(), paramNames.size()); i++) {
                    String type = normalizeTypeReference(parameterTypes.get(i));
                    if (!type.isBlank()) {
                        result.add(new DetectedWritePath(operation, "repository-method", paramNames.get(i), null, type));
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static String normalizeWriteOperation(String rawOperation) {
        if (rawOperation == null || rawOperation.isBlank()) {
            return null;
        }
        String value = rawOperation.toLowerCase(Locale.ROOT);
        if (value.contains("save")) return "persist";
        if (value.contains("merge") || value.contains("update")) return "merge";
        if (value.contains("delete") || value.contains("remove")) return "remove";
        if (value.equals("persist")) return "persist";
        return null;
    }

    private static Map<String, String> collectMethodVariableTypes(ExtractedEntityFact methodEntity, String snippet) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        String params = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
        List<String> paramTypes = metadataStringList(methodEntity.metadata().get("parameterTypes"));
        List<String> paramNames = extractParameterNames(params);
        for (int i = 0; i < Math.min(paramTypes.size(), paramNames.size()); i++) {
            String type = normalizeTypeReference(paramTypes.get(i));
            if (!type.isBlank()) {
                result.putIfAbsent(paramNames.get(i), type);
            }
        }
        if (snippet != null && !snippet.isBlank()) {
            Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^>{}]+>)?)\\s+([A-Za-z_$][\\w$]*)\\s*=", Pattern.DOTALL).matcher(snippet);
            while (matcher.find()) {
                String type = normalizeTypeReference(matcher.group(1));
                String name = matcher.group(2);
                if (!type.isBlank() && !isJavaPrimitiveOrKeyword(type)) {
                    result.putIfAbsent(name, type);
                }
            }
        }
        return Map.copyOf(result);
    }

    private static Optional<String> resolveWriteTargetEntityType(String argumentExpression, Map<String, String> variableTypes) {
        if (argumentExpression == null || argumentExpression.isBlank()) {
            return Optional.empty();
        }
        String arg = argumentExpression.strip();
        Matcher newMatcher = Pattern.compile("new\\s+([A-Za-z_$][\\w.$]*)\\b").matcher(arg);
        if (newMatcher.find()) {
            return Optional.of(newMatcher.group(1));
        }
        Matcher identifierMatcher = Pattern.compile("([A-Za-z_$][\\w$]*)").matcher(arg);
        while (identifierMatcher.find()) {
            String candidate = identifierMatcher.group(1);
            if (variableTypes.containsKey(candidate)) {
                return Optional.of(variableTypes.get(candidate));
            }
        }
        return Optional.empty();
    }

    private static String exactNodeSnippet(String sourceText, SyntaxNode node) {
        if (sourceText == null || sourceText.isBlank() || node == null) {
            return null;
        }
        int start = Math.max(0, Math.min(node.startByte(), sourceText.length()));
        int end = Math.max(start, Math.min(node.endByte(), sourceText.length()));
        if (start >= end) {
            return null;
        }
        String snippet = sourceText.substring(start, end);
        return snippet.isBlank() ? null : snippet;
    }

    private static List<String> extractParameterNames(String parameterSnippet) {
        if (parameterSnippet == null || parameterSnippet.isBlank() || "()".equals(parameterSnippet.strip())) {
            return List.of();
        }
        String inner = parameterSnippet.strip();
        if (inner.startsWith("(")) inner = inner.substring(1);
        if (inner.endsWith(")")) inner = inner.substring(0, inner.length() - 1);
        if (inner.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String part : splitTopLevelCommaSeparated(inner)) {
            String name = extractParameterName(part.strip());
            if (!name.isBlank()) {
                result.add(name);
            }
        }
        return List.copyOf(result);
    }

    private record DetectedWritePath(String operation, String writeKind, String argumentExpression, String viaField, String viaType) {}
    private static List<PublishedCdiEvent> detectCdiPublishedEvents(String methodSnippet, String ownerTypeSnippet) {
        if (methodSnippet == null || methodSnippet.isBlank()) {
            return List.of();
        }
        java.util.LinkedHashMap<String, PublishedCdiEvent> events = new java.util.LinkedHashMap<>();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*\\.\\s*(fireAsync|fire)\\s*\\((.*?)\\)", Pattern.DOTALL).matcher(methodSnippet);
        while (matcher.find()) {
            String publisherField = matcher.group(1);
            boolean async = "fireAsync".equals(matcher.group(2));
            String args = matcher.group(3) == null ? "" : matcher.group(3);
            String eventType = extractCdiEventTypeFromField(ownerTypeSnippet, publisherField)
                .or(() -> extractCdiEventTypeFromArguments(args))
                .orElse(null);
            if (eventType == null || eventType.isBlank()) {
                continue;
            }
            events.putIfAbsent(publisherField + ":" + eventType + ":" + async, new PublishedCdiEvent(eventType, async, publisherField));
        }
        return List.copyOf(events.values());
    }

    private static Optional<String> extractCdiEventTypeFromField(String ownerTypeSnippet, String publisherField) {
        if (ownerTypeSnippet == null || ownerTypeSnippet.isBlank() || publisherField == null || publisherField.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("(?:^|[;{}\\s])(?:@[A-Za-z_][\\w.]*\\s*(?:\\([^)]*\\))?\\s*)*(?:public|protected|private)?\\s*(?:static\\s+|final\\s+|transient\\s+)*?(?:[A-Za-z_][\\w.]*\\.)?Event\\s*<\\s*([^>]+?)\\s*>\\s+" + Pattern.quote(publisherField) + "\\b", Pattern.DOTALL).matcher(ownerTypeSnippet);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1)).map(String::trim).filter(value -> !value.isBlank());
        }
        return Optional.empty();
    }

    private static Optional<String> extractCdiEventTypeFromArguments(String args) {
        if (args == null || args.isBlank()) {
            return Optional.empty();
        }
        Matcher constructorMatcher = Pattern.compile("new\\s+([A-Za-z_$][\\w.$]*)\\b").matcher(args);
        if (constructorMatcher.find()) {
            return Optional.of(constructorMatcher.group(1));
        }
        return Optional.empty();
    }

    private static Optional<ObservedCdiEvent> detectCdiObservedEvent(ExtractedEntityFact methodEntity, String snippet) {
        String parameters = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
        if (parameters == null || parameters.isBlank() || "()".equals(parameters.strip())) {
            parameters = snippet;
        }
        if (parameters == null || parameters.isBlank()) {
            return Optional.empty();
        }
        String normalized = parameters.replace('\n', ' ').replace('\r', ' ');
        Matcher matcher = Pattern.compile("((?:@[A-Za-z_][\\w.]*\\s*(?:\\([^)]*\\))?\\s*)*)@(?:[A-Za-z_][\\w.]*\\.)?(ObservesAsync|Observes)\\b(?:\\s*\\([^)]*\\))?\\s+([A-Za-z_$][\\w.$<>]*)").matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }
        boolean async = "ObservesAsync".equals(matcher.group(2));
        String eventType = matcher.group(3) == null ? "" : matcher.group(3).trim();
        String annotationPrefix = matcher.group(1) == null ? "" : matcher.group(1);
        java.util.LinkedHashSet<String> qualifiers = new java.util.LinkedHashSet<>(SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(annotationPrefix));
        qualifiers.removeIf(value -> value == null || value.endsWith("Observes") || value.endsWith("ObservesAsync"));
        return eventType.isBlank() ? Optional.empty() : Optional.of(new ObservedCdiEvent(eventType, async, List.copyOf(qualifiers)));
    }

    private void addJpaInheritanceFacts(
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        SyntaxNode typeNode,
        ExtractedEntityFact typeEntity,
        Map<String, String> importsBySimpleName,
        Map<String, DeclaredJavaType> declaredTypes
    ) {
        String typeSnippet = typeNodeSnippet(typeNode, typeEntity);
        if (typeEntity == null || !isJpaPersistentType(typeSnippet, typeEntity)) {
            return;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(typeNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, typeSnippet, Map.of("language", "java", "kind", typeNode.type()));
        for (String parentType : extractExtendedTypes(typeNode)) {
            ResolvedJavaType resolved = resolveJavaTypeReference(accumulator, parentType, EntityKind.CLASS, relativePath, packageName, line, importsBySimpleName, declaredTypes);
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

    private void addTypeRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        SyntaxNode typeNode,
        ExtractedEntityFact typeEntity,
        Map<String, String> importsBySimpleName,
        Map<String, DeclaredJavaType> declaredTypes
    ) {
        EntityKind sourceKind = typeEntity.kind();
        int line = SyntaxTreeExtractionSupport.oneBasedLine(typeNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, typeNode.textSnippet(), Map.of("language", "java", "kind", typeNode.type()));
        List<String> extendedTypes = extractExtendedTypes(typeNode);
        for (String parentType : extendedTypes) {
            addResolvedTypeRelationship(
                accumulator,
                typeEntity,
                parentType,
                sourceKind == EntityKind.INTERFACE ? EntityKind.INTERFACE : EntityKind.CLASS,
                info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.EXTENDS,
                "extends",
                relativePath,
                packageName,
                line,
                ref,
                importsBySimpleName,
                declaredTypes
            );
        }
        List<String> implementedTypes = extractImplementedTypes(typeNode);
        for (String iface : implementedTypes) {
            addResolvedTypeRelationship(
                accumulator,
                typeEntity,
                iface,
                EntityKind.INTERFACE,
                info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.IMPLEMENTS,
                "implements",
                relativePath,
                packageName,
                line,
                ref,
                importsBySimpleName,
                declaredTypes
            );
        }
        if (!extendedTypes.isEmpty()) {
            addDeclaredTypeDependencies(
                accumulator,
                typeEntity.id(),
                extendedTypes,
                relativePath,
                packageName,
                line,
                ref,
                importsBySimpleName,
                declaredTypes,
                dependencyMetadata("extends", "hierarchy")
            );
        }
        if (!implementedTypes.isEmpty()) {
            addDeclaredTypeDependencies(
                accumulator,
                typeEntity.id(),
                implementedTypes,
                relativePath,
                packageName,
                line,
                ref,
                importsBySimpleName,
                declaredTypes,
                dependencyMetadata(typeEntity.kind() == EntityKind.INTERFACE ? "extends" : "implements", "hierarchy")
            );
        }
    }

    private void addDeclaredTypeDependencies(
        ExtractionAccumulator accumulator,
        String sourceEntityId,
        List<String> declaredTypeTexts,
        String relativePath,
        String packageName,
        int line,
        SourceReference ref,
        Map<String, String> importsBySimpleName,
        Map<String, DeclaredJavaType> declaredTypes,
        Map<String, Object> metadata
    ) {
        if (sourceEntityId == null || declaredTypeTexts == null || declaredTypeTexts.isEmpty()) {
            return;
        }
        Set<String> seen = new java.util.LinkedHashSet<>();
        for (String declaredTypeText : declaredTypeTexts) {
            for (String referencedType : extractReferencedTypes(declaredTypeText)) {
                ResolvedJavaType resolved = resolveJavaTypeReference(
                    accumulator,
                    referencedType,
                    EntityKind.CLASS,
                    relativePath,
                    packageName,
                    line,
                    importsBySimpleName,
                    declaredTypes
                );
                if (resolved == null || sourceEntityId.equals(resolved.entityId()) || !seen.add(resolved.label())) {
                    continue;
                }
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    sourceEntityId,
                    resolved.entityId(),
                    resolved.label(),
                    ref,
                    "java",
                    metadata
                ));
            }
        }
    }

    private ResolvedJavaType resolveJavaTypeReference(
        ExtractionAccumulator accumulator,
        String referencedType,
        EntityKind fallbackTargetKind,
        String relativePath,
        String packageName,
        int line,
        Map<String, String> importsBySimpleName,
        Map<String, DeclaredJavaType> declaredTypes
    ) {
        if (referencedType == null || referencedType.isBlank()) {
            return null;
        }
        String normalized = normalizeTypeReference(referencedType);
        if (normalized.isBlank()) {
            return null;
        }
        DeclaredJavaType declared = declaredTypes.get(normalized);
        if (declared != null) {
            return new ResolvedJavaType(declared.entityId(), declared.qualifiedName(), declared.kind());
        }
        String qualifiedName = resolveQualifiedTypeName(normalized, packageName, importsBySimpleName, declaredTypes);
        var inferred = ExtractionSupport.inferredTypeEntity(
            "java",
            fallbackTargetKind,
            qualifiedName,
            relativePath,
            line,
            Map.of(
                "resolvedFrom", normalized,
                "resolution", qualifiedName.equals(normalized) ? "unresolved-or-external" : "import-or-package"
            )
        );
        accumulator.addEntity(inferred);
        return new ResolvedJavaType(inferred.id(), qualifiedName, inferred.kind());
    }

    private void addResolvedTypeRelationship(
        ExtractionAccumulator accumulator,
        ExtractedEntityFact sourceType,
        String referencedType,
        EntityKind fallbackTargetKind,
        info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind relationshipKind,
        String relationshipPrefix,
        String relativePath,
        String packageName,
        int line,
        SourceReference ref,
        Map<String, String> importsBySimpleName,
        Map<String, DeclaredJavaType> declaredTypes
    ) {
        ResolvedJavaType resolved = resolveJavaTypeReference(
            accumulator,
            referencedType,
            fallbackTargetKind,
            relativePath,
            packageName,
            line,
            importsBySimpleName,
            declaredTypes
        );
        if (resolved == null) {
            return;
        }
        String targetEntityId = resolved.entityId();
        String label = resolved.label();
        accumulator.addRelationship(ExtractionSupport.typedRelationship(
            relationshipKind,
            relationshipPrefix,
            sourceType.id(),
            targetEntityId,
            label,
            ref,
            "java",
            dependencyMetadata(relationshipPrefix, "hierarchy")
        ));
    }

    private static void collectDeclaredTypes(
        SourceParseResult parseResult,
        String relativePath,
        String packageName,
        ExtractionMode extractionMode,
        String packageScopeId,
        SyntaxNode node,
        String owningQualifiedName,
        Map<String, DeclaredJavaType> declaredTypes
    ) {
        if (node == null) {
            return;
        }
        String nextOwningQualifiedName = owningQualifiedName;
        if (isJavaTypeDeclaration(node)) {
            ExtractedEntityFact typeEntity = toTypeEntity(parseResult, relativePath, packageName, extractionMode, packageScopeId, node, owningQualifiedName);
            if (typeEntity != null) {
                String qualifiedName = String.valueOf(typeEntity.metadata().get("qualifiedName"));
                declaredTypes.putIfAbsent(qualifiedName, new DeclaredJavaType(typeEntity.id(), qualifiedName, typeEntity.kind()));
                String simpleName = simpleName(qualifiedName);
                if (simpleName != null && !simpleName.isBlank()) {
                    declaredTypes.putIfAbsent(simpleName, new DeclaredJavaType(typeEntity.id(), qualifiedName, typeEntity.kind()));
                }
                nextOwningQualifiedName = qualifiedName;
            }
        }
        for (SyntaxNode child : node.children()) {
            collectDeclaredTypes(parseResult, relativePath, packageName, extractionMode, packageScopeId, child, nextOwningQualifiedName, declaredTypes);
        }
    }

    private static List<String> extractExtendedTypes(SyntaxNode typeNode) {
        if (typeNode == null || typeNode.textSnippet() == null) {
            return List.of();
        }
        String header = declarationHeader(typeNode.textSnippet());
        if (typeNode.type().equals("interface_declaration")) {
            return extractTypeListAfterKeyword(header, "extends");
        }
        Optional<String> single = extractSingleTypeAfterKeyword(header, "extends");
        return single.map(List::of).orElseGet(List::of);
    }

    private static List<String> extractImplementedTypes(SyntaxNode typeNode) {
        if (typeNode == null || typeNode.textSnippet() == null) {
            return List.of();
        }
        return extractTypeListAfterKeyword(declarationHeader(typeNode.textSnippet()), "implements");
    }

    private static Optional<String> extractSingleTypeAfterKeyword(String header, String keyword) {
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\s+([A-Za-z_$][\\w.$]*(?:\\s*<[^>{}]+>)?)").matcher(header);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.ofNullable(normalizeTypeReference(matcher.group(1)));
    }

    private static List<String> extractTypeListAfterKeyword(String header, String keyword) {
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\s+([^\\{]+)$").matcher(header);
        if (!matcher.find()) {
            return List.of();
        }
        String raw = matcher.group(1);
        List<String> result = new ArrayList<>();
        for (String part : raw.split(",")) {
            String normalized = normalizeTypeReference(part);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return List.copyOf(result);
    }

    private static String declarationHeader(String snippet) {
        int brace = snippet.indexOf('{');
        String header = brace >= 0 ? snippet.substring(0, brace) : snippet;
        return header.replace('\n', ' ').replace('\r', ' ').strip();
    }

    private static String normalizeTypeReference(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("<[^>]+>", " ")
            .replaceAll("\bextends\b", " ")
            .replaceAll("\bsuper\b", " ")
            .replace("?", " ")
            .replace("[]", " ")
            .trim();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*)").matcher(normalized);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static List<String> extractReferencedTypes(String declaredTypeText) {
        if (declaredTypeText == null || declaredTypeText.isBlank()) {
            return List.of();
        }
        String normalized = declaredTypeText
            .replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ")
            .replaceAll("\bextends\b", " ")
            .replaceAll("\bsuper\b", " ")
            .replace("?", " ")
            .replace("[]", " ")
            .replace("...", " ");
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*)").matcher(normalized);
        Set<String> result = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (!isJavaPrimitiveOrKeyword(candidate)) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    private static boolean isJavaPrimitiveOrKeyword(String candidate) {
        return Set.of(
            "byte", "short", "int", "long", "float", "double", "boolean", "char", "void", "var", "this", "super"
        ).contains(candidate);
    }

    private static String resolveQualifiedTypeName(String reference, String packageName, Map<String, String> importsBySimpleName, Map<String, DeclaredJavaType> declaredTypes) {
        if (reference == null || reference.isBlank()) {
            return "";
        }
        if (reference.contains(".")) {
            return reference;
        }
        if (importsBySimpleName.containsKey(reference)) {
            return importsBySimpleName.get(reference);
        }
        DeclaredJavaType declared = declaredTypes.get(reference);
        if (declared != null) {
            return declared.qualifiedName();
        }
        if (packageName != null && !packageName.isBlank()) {
            return packageName + "." + reference;
        }
        return reference;
    }


    private static boolean isConstructor(ExtractedEntityFact methodEntity) {
        if (methodEntity == null) {
            return false;
        }
        Object ownerQualifiedName = methodEntity.metadata().get("ownerQualifiedName");
        if (ownerQualifiedName == null || String.valueOf(ownerQualifiedName).isBlank()) {
            return false;
        }
        String ownerSimpleName = simpleName(String.valueOf(ownerQualifiedName));
        return ownerSimpleName != null && ownerSimpleName.equals(methodEntity.name());
    }

    private static Map<String, Object> dependencyMetadata(String dependencySource, String dependencyCategory) {
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("dependencySource", dependencySource);
        metadata.put("dependencyCategory", dependencyCategory);
        return java.util.Map.copyOf(metadata);
    }

    private static String simpleName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return null;
        }
        int idx = qualifiedName.lastIndexOf('.');
        return idx >= 0 ? qualifiedName.substring(idx + 1) : qualifiedName;
    }

    private record DeclaredJavaType(String entityId, String qualifiedName, EntityKind kind) {
    }

    private record ResolvedJavaType(String entityId, String label, EntityKind kind) {
    }

    private record PublishedCdiEvent(String eventType, boolean async, String publisherField) {
    }

    private record ObservedCdiEvent(String eventType, boolean async, List<String> qualifiers) {
    }


    private static boolean containsJpaPropertyAnnotation(String snippet) {
        return hasAnyJpaAnnotation(snippet, "Id", "EmbeddedId", "Version", "Embedded", "Column", "OneToOne", "OneToMany", "ManyToOne", "ManyToMany", "JoinColumn", "JoinTable");
    }

    private static boolean hasAnyJpaAnnotation(String snippet, String... simpleNames) {
        if (snippet == null || snippet.isBlank()) {
            return false;
        }
        for (String simpleName : simpleNames) {
            if (containsAnnotationSnippet(snippet, simpleName)) {
                return true;
            }
        }
        return false;
    }

    private static String deriveJavaPropertyName(String methodName, String parameterSnippet) {
        if (methodName == null || methodName.isBlank()) {
            return null;
        }
        String params = parameterSnippet == null ? "" : parameterSnippet.strip();
        if (!(params.isBlank() || "()".equals(params))) {
            return null;
        }
        if (methodName.startsWith("get") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3))) {
            return decapitalizeJavaProperty(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2 && Character.isUpperCase(methodName.charAt(2))) {
            return decapitalizeJavaProperty(methodName.substring(2));
        }
        return null;
    }

    private static String decapitalizeJavaProperty(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() > 1 && Character.isUpperCase(value.charAt(0)) && Character.isUpperCase(value.charAt(1))) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }


    private static boolean isJpaPersistentType(String snippet, ExtractedEntityFact entity) {
        if (hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "Entity")
            || hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "Embeddable")
            || hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "MappedSuperclass")) {
            return true;
        }
        String lower = snippet == null ? "" : snippet.toLowerCase(Locale.ROOT);
        return lower.contains("@entity") || lower.contains("@embeddable") || lower.contains("@mappedsuperclass");
    }

    private static Optional<String> detectJpaTypeKind(String snippet, ExtractedEntityFact entity) {
        List<String> annotations = metadataStringList(entity == null ? null : entity.metadata().get("annotations"));
        if (hasAnnotation(annotations, "Embeddable") || containsAnnotationSnippet(snippet, "Embeddable")) {
            return Optional.of("embeddable");
        }
        if (hasAnnotation(annotations, "MappedSuperclass") || containsAnnotationSnippet(snippet, "MappedSuperclass")) {
            return Optional.of("mapped-superclass");
        }
        if (hasAnnotation(annotations, "Entity") || containsAnnotationSnippet(snippet, "Entity")) {
            return Optional.of("entity");
        }
        return Optional.empty();
    }

    private static boolean hasAnnotation(List<String> annotations, String simpleName) {
        if (annotations == null || simpleName == null || simpleName.isBlank()) {
            return false;
        }
        String expected = simpleName.toLowerCase(Locale.ROOT);
        return annotations.stream().map(value -> value == null ? "" : value.toLowerCase(Locale.ROOT)).anyMatch(value -> value.endsWith(expected));
    }

    private static boolean containsAnnotationSnippet(String snippet, String simpleName) {
        if (snippet == null || simpleName == null || simpleName.isBlank()) {
            return false;
        }
        return snippet.toLowerCase(Locale.ROOT).contains("@" + simpleName.toLowerCase(Locale.ROOT));
    }

    private static Optional<String> extractJpaTableName(String snippet) {
        return extractAnnotationStringAttribute(snippet, "Table", "name");
    }

    private static Optional<String> extractJpaColumnName(String snippet) {
        return extractAnnotationStringAttribute(snippet, "Column", "name");
    }

    private static Optional<String> extractJpaJoinColumn(String snippet) {
        return extractAnnotationStringAttribute(snippet, "JoinColumn", "name");
    }

    private static Optional<String> extractJpaJoinTable(String snippet) {
        return extractAnnotationStringAttribute(snippet, "JoinTable", "name");
    }

    private static Optional<String> extractJpaMappedBy(String snippet) {
        return extractAnnotationStringAttribute(snippet, "OneToMany", "mappedBy")
            .or(() -> extractAnnotationStringAttribute(snippet, "OneToOne", "mappedBy"))
            .or(() -> extractAnnotationStringAttribute(snippet, "ManyToMany", "mappedBy"));
    }

    private static Optional<String> extractJpaInheritanceStrategy(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return Optional.empty();
        }
        Matcher strategy = Pattern.compile("@(?:[A-Za-z_][\\w.]*\\.)?Inheritance\\s*\\([^)]*strategy\\s*=\\s*(?:[A-Za-z_][\\w.]*\\.)?([A-Z_]+)", Pattern.DOTALL).matcher(snippet);
        if (strategy.find()) {
            return Optional.of(strategy.group(1));
        }
        return Optional.empty();
    }

    private static Optional<String> detectJpaAssociation(String snippet) {
        return detectJpaAssociation(List.of(), snippet);
    }

    private static Optional<String> detectJpaAssociation(List<String> annotations, String snippet) {
        if (hasAnnotation(annotations, "OneToOne") || containsAnnotationSnippet(snippet, "OneToOne")) return Optional.of("one-to-one");
        if (hasAnnotation(annotations, "OneToMany") || containsAnnotationSnippet(snippet, "OneToMany")) return Optional.of("one-to-many");
        if (hasAnnotation(annotations, "ManyToOne") || containsAnnotationSnippet(snippet, "ManyToOne")) return Optional.of("many-to-one");
        if (hasAnnotation(annotations, "ManyToMany") || containsAnnotationSnippet(snippet, "ManyToMany")) return Optional.of("many-to-many");
        return Optional.empty();
    }

    private static Optional<String> inferJavaMethodReturnTypeFromSnippet(String snippet, String methodName) {
        if (snippet == null || snippet.isBlank() || methodName == null || methodName.isBlank()) {
            return Optional.empty();
        }
        Pattern pattern = Pattern.compile("(?:public|protected|private)?\\s*(?:static\\s+)?(?:final\\s+)?([A-Za-z_$][\\w$<>., ?]+?)\\s+" + Pattern.quote(methodName) + "\\s*\\(");
        Matcher matcher = pattern.matcher(snippet.replace("\n", " "));
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1)).map(String::trim).filter(value -> !value.isBlank());
        }
        return Optional.empty();
    }

    private static Optional<String> extractAnnotationStringAttribute(String snippet, String annotationSimpleName, String attributeName) {
        if (snippet == null || snippet.isBlank()) {
            return Optional.empty();
        }
        String annotationPattern = "@(?:[A-Za-z_][\\w.]*\\.)?" + Pattern.quote(annotationSimpleName) + "\\s*\\((.*?)\\)";
        Matcher matcher = Pattern.compile(annotationPattern, Pattern.DOTALL).matcher(snippet);
        while (matcher.find()) {
            String body = matcher.group(1);
            Matcher named = Pattern.compile(Pattern.quote(attributeName) + "\\s*=\\s*\"([^\"]*)\"").matcher(body);
            if (named.find()) {
                return Optional.of(named.group(1));
            }
            if ("name".equals(attributeName)) {
                Matcher positional = Pattern.compile("^\\s*\"([^\"]*)\"").matcher(body.strip());
                if (positional.find()) {
                    return Optional.of(positional.group(1));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isJavaTypeDeclaration(SyntaxNode node) {
        return node != null && Set.of(
            "class_declaration", "interface_declaration", "enum_declaration", "record_declaration"
        ).contains(node.type());
    }

    private static boolean isJavaFieldDeclaration(SyntaxNode node) {
        return node != null && Set.of("field_declaration", "constant_declaration").contains(node.type());
    }

    private static boolean isJavaMethodLikeDeclaration(SyntaxNode node) {
        return node != null && Set.of("method_declaration", "constructor_declaration").contains(node.type());
    }



    private static boolean isJaxRsResource(ExtractedEntityFact entity) {
        return metadataStringList(entity.metadata().get("annotations")).stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .anyMatch(value -> value.endsWith("path"));
    }

    private static Optional<String> jaxRsHttpMethod(List<String> annotations) {
        for (String annotation : annotations) {
            String value = annotation.toLowerCase(Locale.ROOT);
            if (value.endsWith("get")) return Optional.of("GET");
            if (value.endsWith("post")) return Optional.of("POST");
            if (value.endsWith("put")) return Optional.of("PUT");
            if (value.endsWith("delete")) return Optional.of("DELETE");
            if (value.endsWith("patch")) return Optional.of("PATCH");
            if (value.endsWith("head")) return Optional.of("HEAD");
            if (value.endsWith("options")) return Optional.of("OPTIONS");
        }
        return Optional.empty();
    }

    private static Optional<String> extractJaxRsPath(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return Optional.empty();
        }
        Matcher valueMatcher = Pattern.compile("@(?:[A-Za-z_][\\w.]*\\.)?Path\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"").matcher(snippet);
        if (valueMatcher.find()) {
            return Optional.ofNullable(valueMatcher.group(1));
        }
        Matcher bareMatcher = Pattern.compile("@(?:[A-Za-z_][\\w.]*\\.)?Path\\s*\\(\\s*\\)").matcher(snippet);
        if (bareMatcher.find()) {
            return Optional.of("/");
        }
        return Optional.empty();
    }

    private static String normalizeJaxRsPath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        String normalized = value.strip();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        normalized = normalized.replaceAll("//+", "/");
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeJaxRsEndpointPath(String classPath, String methodPath) {
        String base = normalizeJaxRsPath(classPath);
        String method = normalizeJaxRsPath(methodPath);
        if ("/".equals(base) && "/".equals(method)) {
            return "/";
        }
        if ("/".equals(base)) {
            return method;
        }
        if ("/".equals(method)) {
            return base;
        }
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base)
            + (method.startsWith("/") ? method : "/" + method);
    }

    private static List<Map<String, String>> extractJaxRsParameterDetails(String parameterSnippet) {
        if (parameterSnippet == null || parameterSnippet.isBlank() || "()".equals(parameterSnippet.strip())) {
            return List.of();
        }
        String inner = parameterSnippet.strip();
        if (inner.startsWith("(")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith(")")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        if (inner.isBlank()) {
            return List.of();
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (String part : splitTopLevelCommaSeparated(inner)) {
            String snippet = part.strip();
            if (snippet.isBlank()) {
                continue;
            }
            LinkedHashMap<String, String> detail = new LinkedHashMap<>();
            detail.put("name", extractParameterName(snippet));
            detail.put("declaredType", extractParameterDeclaredType(snippet));
            detail.put("parameterKind", classifyJaxRsParameter(snippet));
            result.add(Map.copyOf(detail));
        }
        return List.copyOf(result);
    }

    private static List<String> splitTopLevelCommaSeparated(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return List.of();
        }
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '<' || ch == '(' || ch == '[') {
                depth++;
            } else if (ch == '>' || ch == ')' || ch == ']') {
                depth = Math.max(0, depth - 1);
            } else if (ch == ',' && depth == 0) {
                String part = current.toString().trim();
                if (!part.isEmpty()) {
                    result.add(part);
                }
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            result.add(tail);
        }
        return List.copyOf(result);
    }

    private static String extractParameterName(String snippet) {
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*$").matcher(snippet);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String extractParameterDeclaredType(String snippet) {
        String value = snippet == null ? "" : snippet
            .replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ")
            .replaceAll("\\bfinal\\b", " ")
            .trim();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^>{}]+>)?(?:\\s*\\[\\])*)\\s+[A-Za-z_$][\\w$]*$").matcher(value);
        return matcher.find() ? matcher.group(1).replaceAll("\\s+", " ").trim() : "";
    }

    private static String classifyJaxRsParameter(String snippet) {
        String lower = snippet == null ? "" : snippet.toLowerCase(Locale.ROOT);
        if (lower.contains("@pathparam")) return "PATH";
        if (lower.contains("@queryparam")) return "QUERY";
        if (lower.contains("@headerparam")) return "HEADER";
        if (lower.contains("@cookieparam")) return "COOKIE";
        if (lower.contains("@matrixparam")) return "MATRIX";
        if (lower.contains("@formparam")) return "FORM";
        if (lower.contains("@beanparam")) return "BEAN";
        if (lower.contains("@context")) return "CONTEXT";
        return "BODY";
    }

    @SuppressWarnings("unchecked")
    private static List<String> metadataStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private static int lineOf(SourceReference ref, SyntaxNode fallbackNode) {
        return ref != null && ref.startLine() != null ? ref.startLine() : SyntaxTreeExtractionSupport.oneBasedLine(fallbackNode);
    }

    private static List<ExtractedEntityFact> toFieldEntities(
        SourceParseResult parseResult,
        String relativePath,
        ExtractionMode extractionMode,
        String fileScopeId,
        SyntaxNode fieldNode,
        String owningQualifiedName
    ) {
        List<String> fieldNames = SyntaxTreeExtractionSupport.javaFieldNames(fieldNode);
        if (fieldNames.isEmpty()) {
            return List.of();
        }
        String declaredType = SyntaxTreeExtractionSupport.javaFieldDeclaredType(fieldNode);
        List<String> annotations = SyntaxTreeExtractionSupport.descendantsByType(fieldNode, Set.of(
            "marker_annotation", "annotation"
        )).stream().flatMap(node -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(node.textSnippet()).stream()).distinct().toList();
        List<String> modifiers = SyntaxTreeExtractionSupport.javaModifiers(fieldNode);
        int line = SyntaxTreeExtractionSupport.oneBasedLine(fieldNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, fieldNode.textSnippet(), Map.of("language", "java", "kind", fieldNode.type()));
        List<ExtractedEntityFact> result = new ArrayList<>();
        for (String fieldName : fieldNames) {
            String canonicalName = owningQualifiedName == null || owningQualifiedName.isBlank()
                ? fieldName
                : owningQualifiedName + "#" + fieldName;
            result.add(new ExtractedEntityFact(
                IdUtils.scopedEntityId("java", relativePath, canonicalName, line),
                EntityKind.FIELD,
                EntityOrigin.OBSERVED,
                fieldName,
                DisplayNamePolicy.entityDisplayName(EntityKind.FIELD, canonicalName, "java"),
                fileScopeId,
                List.of(ref),
                Map.of(
                    "language", "java",
                    "declaredType", declaredType == null ? "" : declaredType,
                    "annotations", annotations,
                    "modifiers", modifiers,
                    "ownerQualifiedName", owningQualifiedName == null ? "" : owningQualifiedName,
                    "parseStatus", parseResult.status().name(),
                    "extractionMode", extractionMode.name()
                )
            ));
        }
        return List.copyOf(result);
    }

    private static ExtractedEntityFact toTypeEntity(
        SourceParseResult parseResult,
        String relativePath,
        String packageName,
        ExtractionMode extractionMode,
        String packageScopeId,
        SyntaxNode typeNode,
        String owningQualifiedName
    ) {
        String typeName = SyntaxTreeExtractionSupport.declarationName(typeNode);
        if (typeName == null || typeName.isBlank()) {
            return null;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(typeNode);
        EntityKind kind = "interface_declaration".equals(typeNode.type()) ? EntityKind.INTERFACE : EntityKind.CLASS;
        String declarationKind = javaDeclarationKind(typeNode.type());
        String qualifiedName = owningQualifiedName == null || owningQualifiedName.isBlank()
            ? (packageName == null || packageName.isBlank() ? typeName : packageName + "." + typeName)
            : owningQualifiedName + "." + typeName;
        List<String> annotations = SyntaxTreeExtractionSupport.descendantsByType(typeNode, Set.of(
            "marker_annotation", "annotation"
        )).stream().flatMap(node -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(node.textSnippet()).stream()).distinct().toList();
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, typeNode.textSnippet(), Map.of("language", "java", "kind", typeNode.type()));
        return new ExtractedEntityFact(
            IdUtils.scopedEntityId("java", relativePath, qualifiedName, line),
            kind,
            EntityOrigin.OBSERVED,
            typeName,
            DisplayNamePolicy.entityDisplayName(kind, qualifiedName, "java"),
            packageScopeId,
            List.of(ref),
            Map.of(
                "language", "java",
                "qualifiedName", qualifiedName,
                "packageName", packageName,
                "declarationKind", declarationKind,
                "annotations", annotations,
                "parseStatus", parseResult.status().name(),
                "extractionMode", extractionMode.name()
            )
        );
    }


    private static String javaDeclarationKind(String nodeType) {
        return switch (nodeType) {
            case "interface_declaration" -> "interface";
            case "enum_declaration" -> "enum";
            case "record_declaration" -> "record";
            case "class_declaration" -> "class";
            default -> "type";
        };
    }

    private static ExtractedEntityFact toMethodEntity(
        SourceParseResult parseResult,
        String relativePath,
        ExtractionMode extractionMode,
        String fileScopeId,
        SyntaxNode methodNode,
        String owningQualifiedName
    ) {
        String methodName = SyntaxTreeExtractionSupport.javaMethodLikeName(methodNode);
        if (methodName == null || methodName.isBlank()) {
            return null;
        }
        String parameterSnippet = SyntaxTreeExtractionSupport.parameterSnippet(methodNode);
        int line = SyntaxTreeExtractionSupport.oneBasedLine(methodNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, methodNode.textSnippet(), Map.of("language", "java", "kind", methodNode.type()));
        List<String> annotations = SyntaxTreeExtractionSupport.descendantsByType(methodNode, Set.of(
            "marker_annotation", "annotation"
        )).stream().flatMap(node -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(node.textSnippet()).stream()).distinct().toList();
        return new ExtractedEntityFact(
            IdUtils.scopedEntityId("java", relativePath, (owningQualifiedName == null || owningQualifiedName.isBlank() ? methodName : owningQualifiedName + "#" + methodName), line),
            EntityKind.FUNCTION,
            EntityOrigin.OBSERVED,
            methodName,
            SyntaxTreeExtractionSupport.javaMethodDisplayName(methodName, parameterSnippet),
            fileScopeId,
            List.of(ref),
            Map.of(
                "language", "java",
                "parameters", parameterSnippet,
                "returnType", SyntaxTreeExtractionSupport.javaMethodReturnType(methodNode),
                "parameterTypes", SyntaxTreeExtractionSupport.javaMethodParameterDeclaredTypes(methodNode),
                "annotations", annotations,
                "ownerQualifiedName", owningQualifiedName == null ? "" : owningQualifiedName,
                "parseStatus", parseResult.status().name(),
                "extractionMode", extractionMode.name()
            )
        );
    }


    private static java.util.Optional<String> importQualifiedName(String snippet) {
        return SyntaxTreeExtractionSupport.extractQualifiedName(
            snippet == null ? null : snippet.replaceFirst("^\\s*import\\s+", "").replaceFirst(";\\s*$", "")
        );
    }

    private static String derivePackageFromPath(String relativePath) {
        int marker = relativePath.indexOf("/java/");
        if (marker >= 0) {
            String candidate = relativePath.substring(marker + 6);
            int slash = candidate.lastIndexOf('/');
            if (slash > 0) {
                return candidate.substring(0, slash).replace('/', '.');
            }
        }
        int slash = relativePath.lastIndexOf('/');
        return slash > 0 ? relativePath.substring(0, slash).replace('/', '.') : "default";
    }
}
