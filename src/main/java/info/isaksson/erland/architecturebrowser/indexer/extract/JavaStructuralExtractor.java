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
                "java"
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
        } else if (isJavaMethodLikeDeclaration(node)) {
            ExtractedEntityFact methodEntity = toMethodEntity(parseResult, relativePath, extractionMode, fileScopeId, node, currentOwningQualifiedName);
            if (methodEntity != null) {
                accumulator.addEntity(methodEntity);
                SourceReference ref = methodEntity.sourceRefs().isEmpty() ? null : methodEntity.sourceRefs().getFirst();
                accumulator.addRelationship(ExtractionSupport.containsRelationship(
                    currentOwningTypeEntityId == null ? fileEntityId : currentOwningTypeEntityId,
                    methodEntity.id(),
                    ref
                ));
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
        for (String parentType : extractExtendedTypes(typeNode)) {
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
        for (String iface : extractImplementedTypes(typeNode)) {
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
        if (referencedType == null || referencedType.isBlank()) {
            return;
        }
        String normalized = normalizeTypeReference(referencedType);
        if (normalized.isBlank()) {
            return;
        }
        DeclaredJavaType declared = declaredTypes.get(normalized);
        String targetEntityId;
        String label;
        if (declared != null) {
            targetEntityId = declared.entityId();
            label = declared.qualifiedName();
        } else {
            String qualifiedName = resolveQualifiedTypeName(normalized, packageName, importsBySimpleName, declaredTypes);
            label = qualifiedName;
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
            targetEntityId = inferred.id();
        }
        accumulator.addRelationship(ExtractionSupport.typedRelationship(
            relationshipKind,
            relationshipPrefix,
            sourceType.id(),
            targetEntityId,
            label,
            ref,
            "java"
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

    private static String simpleName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return null;
        }
        int idx = qualifiedName.lastIndexOf('.');
        return idx >= 0 ? qualifiedName.substring(idx + 1) : qualifiedName;
    }

    private record DeclaredJavaType(String entityId, String qualifiedName, EntityKind kind) {
    }


    private static boolean isJavaTypeDeclaration(SyntaxNode node) {
        return node != null && Set.of(
            "class_declaration", "interface_declaration", "enum_declaration", "record_declaration"
        ).contains(node.type());
    }

    private static boolean isJavaMethodLikeDeclaration(SyntaxNode node) {
        return node != null && Set.of("method_declaration", "constructor_declaration").contains(node.type());
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
                "annotations", annotations,
                "parseStatus", parseResult.status().name(),
                "extractionMode", extractionMode.name()
            )
        );
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
