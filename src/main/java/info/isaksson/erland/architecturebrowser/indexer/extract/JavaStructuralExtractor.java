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
        Map<String, String> importsBySimpleName,
        Map<String, DeclaredJavaType> declaredTypes
    ) {
        if (node == null) {
            return;
        }

        String currentOwningTypeEntityId = owningTypeEntityId;
        String currentOwningQualifiedName = owningQualifiedName;
        if (isJavaTypeDeclaration(node)) {
            ExtractedEntityFact typeEntity = toTypeEntity(parseResult, relativePath, packageName, extractionMode, packageScopeId, node, owningQualifiedName);
            if (typeEntity != null) {
                accumulator.addEntity(typeEntity);
                SourceReference ref = typeEntity.sourceRefs().isEmpty() ? null : typeEntity.sourceRefs().getFirst();
                accumulator.addRelationship(ExtractionSupport.containsRelationship(fileEntityId, typeEntity.id(), ref));
                addTypeRelationships(accumulator, relativePath, packageName, node, typeEntity, importsBySimpleName, declaredTypes);
                currentOwningTypeEntityId = typeEntity.id();
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
                importsBySimpleName,
                declaredTypes
            );
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
