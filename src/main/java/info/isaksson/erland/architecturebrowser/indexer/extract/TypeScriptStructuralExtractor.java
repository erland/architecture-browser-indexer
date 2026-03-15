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
            var external = ExtractionSupport.externalDependencyEntity("typescript", imported, relativePath, line);
            accumulator.addEntity(external);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                fileEntity.id(), external.id(), imported,
                ExtractionSupport.sourceRef(relativePath, line, importNode.textSnippet(), Map.of("language", "typescript", "kind", "import")),
                "typescript"
            ));
        }

        for (SyntaxNode classNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("class_declaration"))) {
            ExtractedEntityFact typeEntity = addNamedEntityFromNode(parseResult, accumulator, fileEntity.id(), relativePath, classNode, EntityKind.CLASS, "class_declaration", extractionMode);
            if (typeEntity != null) {
                addOwnedMembers(parseResult, accumulator, typeEntity, relativePath, classNode, extractionMode, "class");
            }
        }
        for (SyntaxNode interfaceNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("interface_declaration"))) {
            ExtractedEntityFact typeEntity = addNamedEntityFromNode(parseResult, accumulator, fileEntity.id(), relativePath, interfaceNode, EntityKind.INTERFACE, "interface_declaration", extractionMode);
            if (typeEntity != null) {
                addOwnedMembers(parseResult, accumulator, typeEntity, relativePath, interfaceNode, extractionMode, "interface");
            }
        }
        for (SyntaxNode functionNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("function_declaration"))) {
            addNamedEntityFromNode(parseResult, accumulator, fileEntity.id(), relativePath, functionNode, EntityKind.FUNCTION, "function_declaration", extractionMode);
        }
        for (SyntaxNode variableDeclarator : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("variable_declarator"))) {
            if (SyntaxTreeExtractionSupport.containsDescendantType(variableDeclarator, "arrow_function")) {
                addNamedEntityFromNode(parseResult, accumulator, fileEntity.id(), relativePath, variableDeclarator, EntityKind.FUNCTION, "arrow_function", extractionMode);
            }
        }
        return accumulator;
    }

    private static void addOwnedMembers(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        ExtractedEntityFact ownerEntity,
        String relativePath,
        SyntaxNode ownerNode,
        ExtractionMode extractionMode,
        String ownerDeclarationKind
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
                }
            } else if (SyntaxTreeExtractionSupport.isTypeScriptPropertyLikeDeclaration(memberNode)) {
                ExtractedEntityFact propertyEntity = toTypeScriptPropertyEntity(parseResult, relativePath, extractionMode, ownerEntity.scopeId(), memberNode, ownerQualifiedName, ownerDeclarationKind);
                if (propertyEntity != null) {
                    accumulator.addEntity(propertyEntity);
                    SourceReference ref = propertyEntity.sourceRefs().isEmpty()
                        ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(memberNode), memberNode.textSnippet(), Map.of("language", "typescript", "kind", memberNode.type()))
                        : propertyEntity.sourceRefs().getFirst();
                    accumulator.addRelationship(ExtractionSupport.containsRelationship(ownerEntity.id(), propertyEntity.id(), ref));
                }
            }
        }
    }

    private static ExtractedEntityFact addNamedEntityFromNode(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        String parentEntityId,
        String relativePath,
        SyntaxNode node,
        EntityKind kind,
        String matchedKind,
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
        metadata.put("decorators", decorators);
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
        String canonicalName = ownerQualifiedName == null || ownerQualifiedName.isBlank() ? propertyName : ownerQualifiedName + "#" + propertyName;
        return new ExtractedEntityFact(
            IdUtils.scopedEntityId("typescript", relativePath, canonicalName, line),
            EntityKind.FIELD,
            EntityOrigin.OBSERVED,
            propertyName,
            DisplayNamePolicy.entityDisplayName(EntityKind.FIELD, canonicalName, "typescript"),
            fileScopeId,
            List.of(ref),
            Map.of(
                "language", "typescript",
                "decorators", decorators,
                "ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName,
                "ownerDeclarationKind", ownerDeclarationKind == null ? "" : ownerDeclarationKind,
                "parseStatus", parseResult.status().name(),
                "extractionMode", extractionMode.name()
            )
        );
    }

    private static String importFromSnippet(String snippet) {
        if (snippet == null) {
            return null;
        }
        Matcher matcher = IMPORT_FROM_SNIPPET.matcher(snippet);
        return matcher.find() ? matcher.group(1) : null;
    }
}
