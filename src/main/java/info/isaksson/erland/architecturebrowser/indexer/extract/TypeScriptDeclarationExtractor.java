package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.naming.DisplayNamePolicy;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TypeScriptDeclarationExtractor {
    private TypeScriptDeclarationExtractor() {
    }

    static TypeScriptDeclarationResult extract(TypeScriptExtractionContext context) {
        Map<String, ExtractedEntityFact> declaredTypes = new LinkedHashMap<>();
        Map<String, ExtractedEntityFact> namedEntities = new LinkedHashMap<>();
        TypeScriptDeclarationDiscoverySupport.TypeScriptDiscoveredDeclarations discoveredDeclarations =
            TypeScriptDeclarationDiscoverySupport.discover(context.root());

        extractNamedTopLevelDeclarations(context, discoveredDeclarations, declaredTypes, namedEntities);
        extractOwnedMembersAndTypeRelationships(context, discoveredDeclarations, declaredTypes);
        extractFunctions(context, discoveredDeclarations, namedEntities);

        return new TypeScriptDeclarationResult(Map.copyOf(declaredTypes), Map.copyOf(namedEntities));
    }

    private static void extractNamedTopLevelDeclarations(
        TypeScriptExtractionContext context,
        TypeScriptDeclarationDiscoverySupport.TypeScriptDiscoveredDeclarations discoveredDeclarations,
        Map<String, ExtractedEntityFact> declaredTypes,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        for (TypeScriptDeclarationDiscoverySupport.DiscoveredTypeDeclaration discoveredType : discoveredDeclarations.namedTypeDeclarations()) {
            ExtractedEntityFact typeEntity = TypeScriptNamedDeclarationSemanticsSupport.addNamedEntityFromNode(
                context.parseResult(),
                context.accumulator(),
                context.fileEntity().id(),
                context.relativePath(),
                discoveredType.node(),
                discoveredType.entityKind(),
                discoveredType.matchedKind(),
                discoveredType.declarationKind(),
                context.extractionMode()
            );
            indexNamedEntity(declaredTypes, namedEntities, typeEntity);
        }
    }

    private static void extractOwnedMembersAndTypeRelationships(
        TypeScriptExtractionContext context,
        TypeScriptDeclarationDiscoverySupport.TypeScriptDiscoveredDeclarations discoveredDeclarations,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        for (SyntaxNode classNode : discoveredDeclarations.classDeclarations()) {
            ExtractedEntityFact typeEntity = declaredTypes.get(SyntaxTreeExtractionSupport.declarationName(classNode));
            if (typeEntity != null) {
                addOwnedMembers(context.parseResult(), context.accumulator(), typeEntity, context.relativePath(), classNode, context.extractionMode(), "class", declaredTypes);
                addTypeRelationships(context.accumulator(), context.relativePath(), classNode, typeEntity, declaredTypes);
            }
        }
        for (SyntaxNode interfaceNode : discoveredDeclarations.interfaceDeclarations()) {
            ExtractedEntityFact typeEntity = declaredTypes.get(SyntaxTreeExtractionSupport.declarationName(interfaceNode));
            if (typeEntity != null) {
                addOwnedMembers(context.parseResult(), context.accumulator(), typeEntity, context.relativePath(), interfaceNode, context.extractionMode(), "interface", declaredTypes);
                addTypeRelationships(context.accumulator(), context.relativePath(), interfaceNode, typeEntity, declaredTypes);
            }
        }
    }

    private static void extractFunctions(
        TypeScriptExtractionContext context,
        TypeScriptDeclarationDiscoverySupport.TypeScriptDiscoveredDeclarations discoveredDeclarations,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        for (SyntaxNode functionNode : discoveredDeclarations.functionDeclarations()) {
            ExtractedEntityFact functionEntity = TypeScriptNamedDeclarationSemanticsSupport.addNamedEntityFromNode(
                context.parseResult(),
                context.accumulator(),
                context.fileEntity().id(),
                context.relativePath(),
                functionNode,
                EntityKind.FUNCTION,
                "function_declaration",
                "function",
                context.extractionMode()
            );
            if (functionEntity != null) {
                namedEntities.putIfAbsent(functionEntity.name(), functionEntity);
            }
        }
        for (SyntaxNode variableDeclarator : discoveredDeclarations.arrowFunctionDeclarators()) {
            ExtractedEntityFact functionEntity = TypeScriptNamedDeclarationSemanticsSupport.addNamedEntityFromNode(
                context.parseResult(),
                context.accumulator(),
                context.fileEntity().id(),
                context.relativePath(),
                variableDeclarator,
                EntityKind.FUNCTION,
                "arrow_function",
                "function",
                context.extractionMode()
            );
            if (functionEntity != null) {
                namedEntities.putIfAbsent(functionEntity.name(), functionEntity);
            }
        }
    }

    private static void indexNamedEntity(
        Map<String, ExtractedEntityFact> declaredTypes,
        Map<String, ExtractedEntityFact> namedEntities,
        ExtractedEntityFact typeEntity
    ) {
        if (typeEntity == null) {
            return;
        }
        declaredTypes.putIfAbsent(typeEntity.name(), typeEntity);
        namedEntities.putIfAbsent(typeEntity.name(), typeEntity);
        Object qualifiedName = typeEntity.metadata().get("qualifiedName");
        if (qualifiedName instanceof String qualified && !qualified.isBlank()) {
            declaredTypes.putIfAbsent(qualified, typeEntity);
            namedEntities.putIfAbsent(qualified, typeEntity);
        }
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
                ExtractedEntityFact methodEntity = TypeScriptMethodDeclarationSemanticsSupport.toTypeScriptMethodEntity(parseResult, relativePath, extractionMode, ownerEntity.scopeId(), memberNode, ownerQualifiedName, ownerDeclarationKind);
                if (methodEntity != null) {
                    accumulator.addEntity(methodEntity);
                    SourceReference ref = methodEntity.sourceRefs().isEmpty()
                        ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(memberNode), memberNode.textSnippet(), Map.of("language", "typescript", "kind", memberNode.type()))
                        : methodEntity.sourceRefs().getFirst();
                    accumulator.addRelationship(ExtractionSupport.containsRelationship(ownerEntity.id(), methodEntity.id(), ref));
                    addMethodTypeDependencies(accumulator, ownerEntity, methodEntity, relativePath, lineOf(ref, memberNode), ref, declaredTypes);
                }
            } else if (SyntaxTreeExtractionSupport.isTypeScriptPropertyLikeDeclaration(memberNode)) {
                ExtractedEntityFact propertyEntity = TypeScriptPropertyDeclarationSemanticsSupport.toTypeScriptPropertyEntity(parseResult, relativePath, extractionMode, ownerEntity.scopeId(), memberNode, ownerQualifiedName, ownerDeclarationKind);
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
            addResolvedTypeRelationship(accumulator, typeEntity, parentType, targetKind, RelationshipKind.EXTENDS, "extends", relativePath, line, ref, declaredTypes);
        }
        for (String implementedType : extractImplementedTypes(typeNode)) {
            addResolvedTypeRelationship(accumulator, typeEntity, implementedType, EntityKind.INTERFACE, RelationshipKind.IMPLEMENTS, "implements", relativePath, line, ref, declaredTypes);
        }
    }

    private static void addResolvedTypeRelationship(
        ExtractionAccumulator accumulator,
        ExtractedEntityFact sourceType,
        String referencedType,
        EntityKind fallbackTargetKind,
        RelationshipKind relationshipKind,
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

    record TypeScriptDeclarationResult(
        Map<String, ExtractedEntityFact> declaredTypes,
        Map<String, ExtractedEntityFact> namedEntities
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
