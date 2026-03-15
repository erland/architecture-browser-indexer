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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TypeScriptStructuralExtractor implements StructuralExtractor {
    private static final Pattern IMPORT_FROM_SNIPPET = Pattern.compile("from\\s+['\\\"]([^'\\\"]+)['\\\"]");
    private static final Pattern IMPORT_SIDE_EFFECT_SNIPPET = Pattern.compile("^import\s+[\'\"]([^\'\"]+)[\'\"];?$");

    @Override
    public ParseLanguage language() {
        return ParseLanguage.TYPESCRIPT;
    }

    @Override
    public ExtractionAccumulator extract(SourceParseResult parseResult, ExtractionAccumulator accumulator) {
        accumulator.incrementFilesVisited();
        if (parseResult.syntaxTree() == null) {
            accumulator.addDiagnostic(ExtractionSupport.extractionWarning(
                parseResult,
                "extract.typescript.syntax-tree-required",
                "TypeScript structural extraction requires a Tree-sitter syntax tree; no regex fallback is used."
            ));
            return accumulator;
        }
        if (!parseResult.successful()) {
            accumulator.addDiagnostic(ExtractionSupport.extractionWarning(
                parseResult,
                "extract.typescript.degraded-syntax-tree",
                "TypeScript extraction is proceeding from a degraded Tree-sitter syntax tree despite parse errors."
            ));
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
        accumulator.incrementFilesExtracted("typescript", extractionMode);

        String repositoryScopeId = "scope:repo";
        var fileScope = ExtractionSupport.fileScope(repositoryScopeId, relativePath);
        accumulator.addScope(fileScope);
        var fileEntity = ExtractionSupport.fileModuleEntity(fileScope.id(), relativePath, "typescript");
        accumulator.addEntity(fileEntity);

        SyntaxNode root = syntaxTree.root();

        for (SyntaxNode importNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("import_statement"))) {
            String imported = importFromSnippet(importNode.textSnippet());
            if (imported == null || imported.isBlank()) {
                continue;
            }
            int line = SyntaxTreeExtractionSupport.oneBasedLine(importNode);
            ImportClassification classification = classifyImport(importNode.textSnippet(), imported);
            var target = classification.internalTarget()
                ? ExtractionSupport.internalDependencyEntity("typescript", imported, relativePath, line, classification.targetMetadata())
                : ExtractionSupport.externalDependencyEntity("typescript", imported, relativePath, line, classification.targetMetadata());
            accumulator.addEntity(target);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                fileEntity.id(), target.id(), imported,
                ExtractionSupport.sourceRef(relativePath, line, importNode.textSnippet(), Map.of("language", "typescript", "kind", "import")),
                "typescript",
                classification.relationshipMetadata()
            ));
        }

        Map<String, ExtractedEntityFact> declaredTypes = new LinkedHashMap<>();
        Map<String, ExtractedEntityFact> namedEntities = new LinkedHashMap<>();
        for (SyntaxNode typeAliasNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("type_alias_declaration"))) {
            ExtractedEntityFact typeEntity = addNamedEntityFromNode(parseResult, accumulator, fileEntity.id(), relativePath, typeAliasNode, EntityKind.INTERFACE, "type_alias_declaration", "typeAlias", extractionMode);
            if (typeEntity != null) {
                declaredTypes.putIfAbsent(typeEntity.name(), typeEntity);
                namedEntities.putIfAbsent(typeEntity.name(), typeEntity);
                Object qualifiedName = typeEntity.metadata().get("qualifiedName");
                if (qualifiedName instanceof String qualified && !qualified.isBlank()) {
                    declaredTypes.putIfAbsent(qualified, typeEntity);
                    namedEntities.putIfAbsent(qualified, typeEntity);
                }
            }
        }
        for (SyntaxNode enumNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("enum_declaration"))) {
            ExtractedEntityFact typeEntity = addNamedEntityFromNode(parseResult, accumulator, fileEntity.id(), relativePath, enumNode, EntityKind.CLASS, "enum_declaration", "enum", extractionMode);
            if (typeEntity != null) {
                declaredTypes.putIfAbsent(typeEntity.name(), typeEntity);
                namedEntities.putIfAbsent(typeEntity.name(), typeEntity);
                Object qualifiedName = typeEntity.metadata().get("qualifiedName");
                if (qualifiedName instanceof String qualified && !qualified.isBlank()) {
                    declaredTypes.putIfAbsent(qualified, typeEntity);
                    namedEntities.putIfAbsent(qualified, typeEntity);
                }
            }
        }
        for (SyntaxNode classNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("class_declaration"))) {
            ExtractedEntityFact typeEntity = addNamedEntityFromNode(parseResult, accumulator, fileEntity.id(), relativePath, classNode, EntityKind.CLASS, "class_declaration", "class", extractionMode);
            if (typeEntity != null) {
                declaredTypes.putIfAbsent(typeEntity.name(), typeEntity);
                namedEntities.putIfAbsent(typeEntity.name(), typeEntity);
                Object qualifiedName = typeEntity.metadata().get("qualifiedName");
                if (qualifiedName instanceof String qualified && !qualified.isBlank()) {
                    declaredTypes.putIfAbsent(qualified, typeEntity);
                    namedEntities.putIfAbsent(qualified, typeEntity);
                }
            }
        }
        for (SyntaxNode interfaceNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("interface_declaration"))) {
            ExtractedEntityFact typeEntity = addNamedEntityFromNode(parseResult, accumulator, fileEntity.id(), relativePath, interfaceNode, EntityKind.INTERFACE, "interface_declaration", "interface", extractionMode);
            if (typeEntity != null) {
                declaredTypes.putIfAbsent(typeEntity.name(), typeEntity);
                namedEntities.putIfAbsent(typeEntity.name(), typeEntity);
                Object qualifiedName = typeEntity.metadata().get("qualifiedName");
                if (qualifiedName instanceof String qualified && !qualified.isBlank()) {
                    declaredTypes.putIfAbsent(qualified, typeEntity);
                    namedEntities.putIfAbsent(qualified, typeEntity);
                }
            }
        }
        for (SyntaxNode classNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("class_declaration"))) {
            ExtractedEntityFact typeEntity = declaredTypes.get(SyntaxTreeExtractionSupport.declarationName(classNode));
            if (typeEntity != null) {
                addOwnedMembers(parseResult, accumulator, typeEntity, relativePath, classNode, extractionMode, "class", declaredTypes);
                addTypeRelationships(accumulator, relativePath, classNode, typeEntity, declaredTypes);
            }
        }
        for (SyntaxNode interfaceNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("interface_declaration"))) {
            ExtractedEntityFact typeEntity = declaredTypes.get(SyntaxTreeExtractionSupport.declarationName(interfaceNode));
            if (typeEntity != null) {
                addOwnedMembers(parseResult, accumulator, typeEntity, relativePath, interfaceNode, extractionMode, "interface", declaredTypes);
                addTypeRelationships(accumulator, relativePath, interfaceNode, typeEntity, declaredTypes);
            }
        }
        for (SyntaxNode functionNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("function_declaration"))) {
            ExtractedEntityFact functionEntity = addNamedEntityFromNode(parseResult, accumulator, fileEntity.id(), relativePath, functionNode, EntityKind.FUNCTION, "function_declaration", "function", extractionMode);
            if (functionEntity != null) {
                namedEntities.putIfAbsent(functionEntity.name(), functionEntity);
            }
        }
        for (SyntaxNode variableDeclarator : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("variable_declarator"))) {
            if (SyntaxTreeExtractionSupport.containsDescendantType(variableDeclarator, "arrow_function")) {
                ExtractedEntityFact functionEntity = addNamedEntityFromNode(parseResult, accumulator, fileEntity.id(), relativePath, variableDeclarator, EntityKind.FUNCTION, "arrow_function", "function", extractionMode);
                if (functionEntity != null) {
                    namedEntities.putIfAbsent(functionEntity.name(), functionEntity);
                }
            }
        }
        AngularFrameworkRelationshipExtractor.extract(accumulator, relativePath, namedEntities);
        ReactJsxCompositionExtractor.extract(accumulator, relativePath, namedEntities);
        ReactContextGraphExtractor.extract(accumulator, relativePath, parseResult.request().sourceText(), namedEntities);
        FrontendRoutingExtractor.extract(accumulator, relativePath, parseResult.request().sourceText(), namedEntities);
        return accumulator;
    }

    private static void addOwnedMembers(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        ExtractedEntityFact ownerEntity,
        String relativePath,
        SyntaxNode ownerNode,
        ExtractionMode extractionMode,
        String ownerDeclarationKind,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        String ownerQualifiedName = String.valueOf(ownerEntity.metadata().getOrDefault("qualifiedName", ownerEntity.name()));
        for (SyntaxNode memberNode : ownerNode.children()) {
            if (SyntaxTreeExtractionSupport.isTypeScriptMethodLikeDeclaration(memberNode)) {
                ExtractedEntityFact methodEntity = toTypeScriptMethodEntity(parseResult, relativePath, extractionMode, ownerEntity.scopeId(), memberNode, ownerQualifiedName, ownerDeclarationKind);
                if (methodEntity != null) {
                    accumulator.addEntity(methodEntity);
                    SourceReference ref = methodEntity.sourceRefs().isEmpty()
                        ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(memberNode), memberNode.textSnippet(), Map.of("language", "typescript", "kind", memberNode.type()))
                        : methodEntity.sourceRefs().getFirst();
                    accumulator.addRelationship(ExtractionSupport.containsRelationship(ownerEntity.id(), methodEntity.id(), ref));
                    addMethodTypeDependencies(accumulator, ownerEntity, methodEntity, relativePath, lineOf(ref, memberNode), ref, declaredTypes);
                }
            } else if (SyntaxTreeExtractionSupport.isTypeScriptPropertyLikeDeclaration(memberNode)) {
                ExtractedEntityFact propertyEntity = toTypeScriptPropertyEntity(parseResult, relativePath, extractionMode, ownerEntity.scopeId(), memberNode, ownerQualifiedName, ownerDeclarationKind);
                if (propertyEntity != null) {
                    accumulator.addEntity(propertyEntity);
                    SourceReference ref = propertyEntity.sourceRefs().isEmpty()
                        ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(memberNode), memberNode.textSnippet(), Map.of("language", "typescript", "kind", memberNode.type()))
                        : propertyEntity.sourceRefs().getFirst();
                    accumulator.addRelationship(ExtractionSupport.containsRelationship(ownerEntity.id(), propertyEntity.id(), ref));
                    addPropertyTypeDependencies(accumulator, ownerEntity, propertyEntity, relativePath, lineOf(ref, memberNode), ref, declaredTypes);
                }
            }
        }
    }


    private static void addTypeRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        SyntaxNode typeNode,
        ExtractedEntityFact typeEntity,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        if (typeNode == null || typeEntity == null) {
            return;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(typeNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, typeNode.textSnippet(), Map.of("language", "typescript", "kind", typeNode.type()));
        for (String parentType : extractExtendedTypes(typeNode)) {
            EntityKind targetKind = typeEntity.kind() == EntityKind.INTERFACE ? EntityKind.INTERFACE : EntityKind.CLASS;
            addResolvedTypeRelationship(accumulator, typeEntity, parentType, targetKind, info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.EXTENDS, "extends", relativePath, line, ref, declaredTypes);
        }
        for (String implementedType : extractImplementedTypes(typeNode)) {
            addResolvedTypeRelationship(accumulator, typeEntity, implementedType, EntityKind.INTERFACE, info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.IMPLEMENTS, "implements", relativePath, line, ref, declaredTypes);
        }
    }

    private static void addResolvedTypeRelationship(
        ExtractionAccumulator accumulator,
        ExtractedEntityFact sourceType,
        String referencedType,
        EntityKind fallbackTargetKind,
        info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind relationshipKind,
        String relationshipPrefix,
        String relativePath,
        int line,
        SourceReference ref,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        ResolvedTypeScriptType resolved = resolveTypeReference(accumulator, referencedType, fallbackTargetKind, relativePath, line, declaredTypes);
        if (resolved == null || sourceType.id().equals(resolved.entityId())) {
            return;
        }
        java.util.Map<String, Object> relationshipMetadata = enrichResolvedTargetMetadata(
            dependencyMetadata(relationshipPrefix, "hierarchy"),
            resolved
        );
        accumulator.addRelationship(ExtractionSupport.typedRelationship(
            relationshipKind,
            relationshipPrefix,
            sourceType.id(),
            resolved.entityId(),
            resolved.label(),
            ref,
            "typescript",
            relationshipMetadata
        ));
        accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
            sourceType.id(),
            resolved.entityId(),
            resolved.label(),
            ref,
            "typescript",
            relationshipMetadata
        ));
    }

    private static ResolvedTypeScriptType resolveTypeReference(
        ExtractionAccumulator accumulator,
        String referencedType,
        EntityKind fallbackTargetKind,
        String relativePath,
        int line,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        String normalized = normalizeTypeReference(referencedType);
        if (normalized.isBlank()) {
            return null;
        }
        ExtractedEntityFact declared = declaredTypes.get(normalized);
        if (declared != null) {
            String declaredQualifiedName = String.valueOf(declared.metadata().getOrDefault("qualifiedName", declared.name()));
            return new ResolvedTypeScriptType(declared.id(), declaredQualifiedName, declared.kind(), "observed-source-type", "internal");
        }
        boolean internalCandidate = isInternalTypeReference(normalized);
        var inferred = internalCandidate
            ? ExtractionSupport.inferredTypeEntity(
                "typescript",
                fallbackTargetKind,
                normalized,
                relativePath,
                line,
                Map.of(
                    "resolvedFrom", normalized,
                    "resolution", "inferred-internal",
                    "external", false,
                    "inferredInternal", true,
                    "targetClassification", "inferred-internal-type"
                )
            )
            : ExtractionSupport.inferredTypeEntity(
                "typescript",
                fallbackTargetKind,
                normalized,
                relativePath,
                line,
                Map.of(
                    "resolvedFrom", normalized,
                    "resolution", "unresolved-or-external",
                    "targetClassification", "external-package-target"
                )
            );
        accumulator.addEntity(inferred);
        return new ResolvedTypeScriptType(inferred.id(), normalized, inferred.kind(), internalCandidate ? "inferred-internal-type" : "external-package-target", internalCandidate ? "internal" : "external");
    }

    private static ExtractedEntityFact addNamedEntityFromNode(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        String parentEntityId,
        String relativePath,
        SyntaxNode node,
        EntityKind kind,
        String matchedKind,
        String declarationKind,
        ExtractionMode extractionMode
    ) {
        String name = SyntaxTreeExtractionSupport.declarationName(node);
        if (name == null || name.isBlank()) {
            return null;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(node);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, node.textSnippet(), Map.of("language", "typescript", "kind", matchedKind));
        List<String> decorators = SyntaxTreeExtractionSupport.descendantsByType(node, Set.of("decorator")).stream()
            .flatMap(candidate -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(candidate.textSnippet()).stream())
            .distinct()
            .toList();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("language", "typescript");
        metadata.put("declarationKind", declarationKind);
        metadata.put("decorators", decorators);
        metadata.putAll(AngularDecoratorMetadataExtractor.extract(node));
        metadata.put("parseStatus", parseResult.status().name());
        metadata.put("extractionMode", extractionMode.name());
        if (kind == EntityKind.CLASS || kind == EntityKind.INTERFACE) {
            metadata.put("qualifiedName", name);
        }
        ExtractedEntityFact entity = new ExtractedEntityFact(
            IdUtils.scopedEntityId("typescript", relativePath, name, line),
            kind,
            EntityOrigin.OBSERVED,
            name,
            DisplayNamePolicy.entityDisplayName(kind, name, "typescript"),
            IdUtils.scopeId("file", relativePath),
            List.of(ref),
            Map.copyOf(metadata)
        );
        accumulator.addEntity(entity);
        accumulator.addRelationship(ExtractionSupport.containsRelationship(parentEntityId, entity.id(), ref));
        return entity;
    }

    private static ExtractedEntityFact toTypeScriptMethodEntity(
        SourceParseResult parseResult,
        String relativePath,
        ExtractionMode extractionMode,
        String fileScopeId,
        SyntaxNode methodNode,
        String ownerQualifiedName,
        String ownerDeclarationKind
    ) {
        String methodName = SyntaxTreeExtractionSupport.declarationName(methodNode);
        if (methodName == null || methodName.isBlank()) {
            return null;
        }
        String parameterSnippet = SyntaxTreeExtractionSupport.parameterSnippet(methodNode);
        int line = SyntaxTreeExtractionSupport.oneBasedLine(methodNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, methodNode.textSnippet(), Map.of("language", "typescript", "kind", methodNode.type()));
        List<String> decorators = SyntaxTreeExtractionSupport.descendantsByType(methodNode, Set.of("decorator")).stream()
            .flatMap(node -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(node.textSnippet()).stream())
            .distinct()
            .toList();
        String canonicalName = ownerQualifiedName == null || ownerQualifiedName.isBlank() ? methodName : ownerQualifiedName + "#" + methodName;
        return new ExtractedEntityFact(
            IdUtils.scopedEntityId("typescript", relativePath, canonicalName, line),
            EntityKind.FUNCTION,
            EntityOrigin.OBSERVED,
            methodName,
            DisplayNamePolicy.entityDisplayName(EntityKind.FUNCTION, canonicalName, "typescript"),
            fileScopeId,
            List.of(ref),
            Map.of(
                "language", "typescript",
                "parameters", parameterSnippet,
                "returnType", SyntaxTreeExtractionSupport.typeScriptMethodReturnType(methodNode),
                "parameterTypes", SyntaxTreeExtractionSupport.typeScriptMethodParameterDeclaredTypes(methodNode),
                "decorators", decorators,
                "ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName,
                "ownerDeclarationKind", ownerDeclarationKind == null ? "" : ownerDeclarationKind,
                "parseStatus", parseResult.status().name(),
                "extractionMode", extractionMode.name()
            )
        );
    }

    private static ExtractedEntityFact toTypeScriptPropertyEntity(
        SourceParseResult parseResult,
        String relativePath,
        ExtractionMode extractionMode,
        String fileScopeId,
        SyntaxNode propertyNode,
        String ownerQualifiedName,
        String ownerDeclarationKind
    ) {
        String propertyName = SyntaxTreeExtractionSupport.declarationName(propertyNode);
        if (propertyName == null || propertyName.isBlank()) {
            return null;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(propertyNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, propertyNode.textSnippet(), Map.of("language", "typescript", "kind", propertyNode.type()));
        List<String> decorators = SyntaxTreeExtractionSupport.descendantsByType(propertyNode, Set.of("decorator")).stream()
            .flatMap(node -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(node.textSnippet()).stream())
            .distinct()
            .toList();
        String declaredType = SyntaxTreeExtractionSupport.typeScriptDeclaredType(propertyNode);
        List<String> modifiers = SyntaxTreeExtractionSupport.typeScriptModifiers(propertyNode);
        boolean optional = SyntaxTreeExtractionSupport.typeScriptOptional(propertyNode);
        boolean readonly = SyntaxTreeExtractionSupport.typeScriptReadonly(propertyNode);
        String accessibility = SyntaxTreeExtractionSupport.typeScriptAccessibility(propertyNode);
        String canonicalName = ownerQualifiedName == null || ownerQualifiedName.isBlank() ? propertyName : ownerQualifiedName + "#" + propertyName;
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("language", "typescript");
        metadata.put("declaredType", declaredType);
        metadata.put("optional", optional);
        metadata.put("readonly", readonly);
        metadata.put("accessibility", accessibility);
        metadata.put("modifiers", modifiers);
        metadata.put("decorators", decorators);
        metadata.put("ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName);
        metadata.put("ownerDeclarationKind", ownerDeclarationKind == null ? "" : ownerDeclarationKind);
        metadata.put("parseStatus", parseResult.status().name());
        metadata.put("extractionMode", extractionMode.name());
        return new ExtractedEntityFact(
            IdUtils.scopedEntityId("typescript", relativePath, canonicalName, line),
            EntityKind.FIELD,
            EntityOrigin.OBSERVED,
            propertyName,
            DisplayNamePolicy.entityDisplayName(EntityKind.FIELD, canonicalName, "typescript"),
            fileScopeId,
            List.of(ref),
            Map.copyOf(metadata)
        );
    }


    private static void addPropertyTypeDependencies(
        ExtractionAccumulator accumulator,
        ExtractedEntityFact ownerEntity,
        ExtractedEntityFact propertyEntity,
        String relativePath,
        int line,
        SourceReference ref,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        String declaredType = String.valueOf(propertyEntity.metadata().getOrDefault("declaredType", ""));
        if (declaredType.isBlank()) {
            return;
        }
        addDeclaredTypeDependencies(
            accumulator,
            ownerEntity.id(),
            List.of(declaredType),
            relativePath,
            line,
            ref,
            declaredTypes,
            dependencyMetadata("field", "composition")
        );
    }

    @SuppressWarnings("unchecked")
    private static void addMethodTypeDependencies(
        ExtractionAccumulator accumulator,
        ExtractedEntityFact ownerEntity,
        ExtractedEntityFact methodEntity,
        String relativePath,
        int line,
        SourceReference ref,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        String returnType = String.valueOf(methodEntity.metadata().getOrDefault("returnType", ""));
        if (!returnType.isBlank()) {
            addDeclaredTypeDependencies(
                accumulator,
                ownerEntity.id(),
                List.of(returnType),
                relativePath,
                line,
                ref,
                declaredTypes,
                dependencyMetadata("returnType", "api")
            );
        }
        List<String> parameterTypes = (List<String>) methodEntity.metadata().getOrDefault("parameterTypes", List.of());
        if (!parameterTypes.isEmpty()) {
            addDeclaredTypeDependencies(
                accumulator,
                ownerEntity.id(),
                parameterTypes,
                relativePath,
                line,
                ref,
                declaredTypes,
                dependencyMetadata("constructor".equals(methodEntity.name()) ? "constructorParameter" : "parameterType", "api")
            );
        }
    }

    private static void addDeclaredTypeDependencies(
        ExtractionAccumulator accumulator,
        String sourceEntityId,
        List<String> declaredTypeTexts,
        String relativePath,
        int line,
        SourceReference ref,
        Map<String, ExtractedEntityFact> declaredTypes,
        Map<String, Object> metadata
    ) {
        if (sourceEntityId == null || declaredTypeTexts == null || declaredTypeTexts.isEmpty()) {
            return;
        }
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (String declaredTypeText : declaredTypeTexts) {
            for (String referencedType : extractReferencedTypes(declaredTypeText)) {
                ResolvedTypeScriptType resolved = resolveTypeReference(accumulator, referencedType, EntityKind.CLASS, relativePath, line, declaredTypes);
                if (resolved == null || sourceEntityId.equals(resolved.entityId()) || !seen.add(resolved.label())) {
                    continue;
                }
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    sourceEntityId,
                    resolved.entityId(),
                    resolved.label(),
                    ref,
                    "typescript",
                    enrichResolvedTargetMetadata(metadata, resolved)
                ));
            }
        }
    }

    private static List<String> extractReferencedTypes(String declaredTypeText) {
        if (declaredTypeText == null || declaredTypeText.isBlank()) {
            return List.of();
        }
        String normalized = declaredTypeText
            .replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ")
            .replaceAll("\\bextends\\b", " ")
            .replaceAll("\\bkeyof\\b", " ")
            .replaceAll("\\breadonly\\b", " ")
            .replace("?", " ")
            .replace("[]", " ")
            .replace("...", " ")
            .replace("|", " ")
            .replace("&", " ");
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*)").matcher(normalized);
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (!isTypeScriptPrimitiveOrKeyword(candidate)) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    private static boolean isTypeScriptPrimitiveOrKeyword(String candidate) {
        return Set.of(
            "string", "number", "boolean", "void", "null", "undefined", "unknown", "never", "any",
            "object", "symbol", "bigint", "true", "false", "this", "super"
        ).contains(candidate);
    }

    private static List<String> extractExtendedTypes(SyntaxNode typeNode) {
        return extractClauseTypes(typeNode, "extends_clause");
    }

    private static List<String> extractImplementedTypes(SyntaxNode typeNode) {
        return extractClauseTypes(typeNode, "implements_clause");
    }

    private static List<String> extractClauseTypes(SyntaxNode typeNode, String clauseType) {
        if (typeNode == null) {
            return List.of();
        }
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        for (SyntaxNode clauseNode : typeNode.children()) {
            if (!clauseType.equals(clauseNode.type())) {
                continue;
            }
            for (SyntaxNode candidate : SyntaxTreeExtractionSupport.descendantsByType(clauseNode, Set.of("type_identifier", "nested_type_identifier", "predefined_type", "identifier"))) {
                String normalized = normalizeTypeReference(candidate.textSnippet());
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
        }
        return List.copyOf(result);
    }

    private static ImportClassification classifyImport(String importSnippet, String imported) {
        boolean sideEffect = isSideEffectImport(importSnippet);
        boolean typeOnly = isTypeOnlyImport(importSnippet);
        boolean relative = imported.startsWith("./") || imported.startsWith("../");
        String importKind = sideEffect ? "sideEffect" : (typeOnly ? "typeOnly" : (relative ? "relative" : "package"));
        String targetClassification = relative ? "inferred-internal-module" : "external-package-target";
        String targetBoundary = relative ? "internal" : "external";
        java.util.Map<String, Object> relationshipMetadata = new java.util.LinkedHashMap<>();
        relationshipMetadata.put("importKind", importKind);
        relationshipMetadata.put("importTargetBoundary", targetBoundary);
        relationshipMetadata.put("targetClassification", targetClassification);
        relationshipMetadata.put("dependencySource", "import");
        relationshipMetadata.put("dependencyCategory", relative ? "internal-module" : "external-package");
        java.util.Map<String, Object> targetMetadata = new java.util.LinkedHashMap<>();
        targetMetadata.put("importKind", importKind);
        targetMetadata.put("targetClassification", targetClassification);
        targetMetadata.put("resolution", relative ? "relative-import" : "package-import");
        targetMetadata.put("packageImport", !relative);
        return new ImportClassification(importKind, relative, java.util.Map.copyOf(relationshipMetadata), java.util.Map.copyOf(targetMetadata));
    }

    private static boolean isTypeOnlyImport(String snippet) {
        return snippet != null && snippet.strip().matches("^import\\s+type\\b.*");
    }

    private static boolean isSideEffectImport(String snippet) {
        return snippet != null && snippet.strip().matches("^import\\s+[\'\\\"].*[\'\\\"];?$");
    }

    private static boolean isInternalTypeReference(String normalized) {
        return normalized != null && (normalized.contains(".") || normalized.contains("/") || normalized.contains("#"));
    }

    private static String normalizeTypeReference(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value
            .replaceAll("<[^>]+>", " ")
            .replace("?", " ")
            .replace("[]", " ")
            .trim();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*)").matcher(normalized);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static int lineOf(SourceReference ref, SyntaxNode fallbackNode) {
        return ref == null ? SyntaxTreeExtractionSupport.oneBasedLine(fallbackNode) : java.util.Objects.requireNonNullElse(ref.startLine(), SyntaxTreeExtractionSupport.oneBasedLine(fallbackNode));
    }

    private static Map<String, Object> enrichResolvedTargetMetadata(Map<String, Object> metadata, ResolvedTypeScriptType resolved) {
        java.util.Map<String, Object> merged = new java.util.LinkedHashMap<>();
        if (metadata != null) {
            merged.putAll(metadata);
        }
        if (resolved != null) {
            merged.put("targetClassification", resolved.targetClassification());
            merged.put("dependencyTargetBoundary", resolved.targetBoundary());
        }
        return java.util.Map.copyOf(merged);
    }

    private static Map<String, Object> dependencyMetadata(String dependencySource, String dependencyCategory) {
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("dependencySource", dependencySource);
        metadata.put("dependencyCategory", dependencyCategory);
        return java.util.Map.copyOf(metadata);
    }

    private static String importFromSnippet(String snippet) {
        if (snippet == null) {
            return null;
        }
        Matcher fromMatcher = IMPORT_FROM_SNIPPET.matcher(snippet);
        if (fromMatcher.find()) {
            return fromMatcher.group(1);
        }
        Matcher sideEffectMatcher = IMPORT_SIDE_EFFECT_SNIPPET.matcher(snippet == null ? "" : snippet.strip());
        return sideEffectMatcher.find() ? sideEffectMatcher.group(1) : null;
    }

    private record ImportClassification(
        String importKind,
        boolean internalTarget,
        Map<String, Object> relationshipMetadata,
        Map<String, Object> targetMetadata
    ) {
    }

    private record ResolvedTypeScriptType(
        String entityId,
        String label,
        EntityKind kind,
        String targetClassification,
        String targetBoundary
    ) {
    }
}
