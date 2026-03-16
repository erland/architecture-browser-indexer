package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaRelationshipEvidenceEmitter {

    void addTypeRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        SyntaxNode typeNode,
        ExtractedEntityFact typeEntity,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
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

    void addDeclaredTypeDependencies(
        ExtractionAccumulator accumulator,
        String sourceEntityId,
        List<String> declaredTypeTexts,
        String relativePath,
        String packageName,
        int line,
        SourceReference ref,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes,
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

    Map<String, Object> dependencyMetadata(String dependencySource, String dependencyCategory) {
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("dependencySource", dependencySource);
        metadata.put("dependencyCategory", dependencyCategory);
        return java.util.Map.copyOf(metadata);
    }

    ResolvedJavaType resolveJavaTypeReference(
        ExtractionAccumulator accumulator,
        String referencedType,
        EntityKind fallbackTargetKind,
        String relativePath,
        String packageName,
        int line,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        if (referencedType == null || referencedType.isBlank()) {
            return null;
        }
        String normalized = normalizeTypeReference(referencedType);
        if (normalized.isBlank()) {
            return null;
        }
        JavaDeclaredType declared = declaredTypes.get(normalized);
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
        Map<String, JavaDeclaredType> declaredTypes
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

    static List<String> extractExtendedTypes(SyntaxNode typeNode) {
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

    static List<String> extractImplementedTypes(SyntaxNode typeNode) {
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

    static String normalizeTypeReference(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("<[^>]+>", " ")
            .replaceAll("\\bextends\\b", " ")
            .replaceAll("\\bsuper\\b", " ")
            .replace("?", " ")
            .replace("[]", " ")
            .trim();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*)").matcher(normalized);
        return matcher.find() ? matcher.group(1) : "";
    }

    static List<String> extractReferencedTypes(String declaredTypeText) {
        if (declaredTypeText == null || declaredTypeText.isBlank()) {
            return List.of();
        }
        String normalized = declaredTypeText
            .replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ")
            .replaceAll("\\bextends\\b", " ")
            .replaceAll("\\bsuper\\b", " ")
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

    static String resolveQualifiedTypeName(String reference, String packageName, Map<String, String> importsBySimpleName, Map<String, JavaDeclaredType> declaredTypes) {
        if (reference == null || reference.isBlank()) {
            return "";
        }
        if (reference.contains(".")) {
            return reference;
        }
        if (importsBySimpleName.containsKey(reference)) {
            return importsBySimpleName.get(reference);
        }
        JavaDeclaredType declared = declaredTypes.get(reference);
        if (declared != null) {
            return declared.qualifiedName();
        }
        if (packageName != null && !packageName.isBlank()) {
            return packageName + "." + reference;
        }
        return reference;
    }

    record ResolvedJavaType(String entityId, String label, EntityKind kind) {
    }
}
