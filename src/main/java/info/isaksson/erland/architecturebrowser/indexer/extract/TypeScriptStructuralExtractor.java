package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;

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
        if (!parseResult.successful() || parseResult.syntaxTree() == null) {
            accumulator.addDiagnostic(ExtractionSupport.extractionWarning(
                parseResult,
                "extract.typescript.syntax-tree-required",
                "TypeScript structural extraction requires a successful Tree-sitter syntax tree; no regex fallback is used."
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
            addNamedEntityFromNode(parseResult, accumulator, fileEntity.id(), relativePath, classNode, EntityKind.CLASS, "class_declaration", extractionMode);
        }
        for (SyntaxNode interfaceNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("interface_declaration"))) {
            addNamedEntityFromNode(parseResult, accumulator, fileEntity.id(), relativePath, interfaceNode, EntityKind.INTERFACE, "interface_declaration", extractionMode);
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

    private static void addNamedEntityFromNode(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        String fileEntityId,
        String relativePath,
        SyntaxNode node,
        EntityKind kind,
        String matchedKind,
        ExtractionMode extractionMode
    ) {
        String name = SyntaxTreeExtractionSupport.declarationName(node);
        if (name == null || name.isBlank()) {
            return;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(node);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, node.textSnippet(), Map.of("language", "typescript", "kind", matchedKind));
        List<String> decorators = SyntaxTreeExtractionSupport.descendantsByType(node, Set.of("decorator")).stream()
            .flatMap(candidate -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(candidate.textSnippet()).stream())
            .distinct()
            .toList();
        ExtractedEntityFact entity = new ExtractedEntityFact(
            IdUtils.scopedEntityId("typescript", relativePath, name, line),
            kind,
            EntityOrigin.OBSERVED,
            name,
            relativePath + "#" + name,
            IdUtils.scopeId("file", relativePath),
            List.of(ref),
            Map.of(
                "language", "typescript",
                "decorators", decorators,
                "parseStatus", parseResult.status().name(),
                "extractionMode", extractionMode.name()
            )
        );
        accumulator.addEntity(entity);
        accumulator.addRelationship(ExtractionSupport.containsRelationship(fileEntityId, entity.id(), ref));
    }

    private static String importFromSnippet(String snippet) {
        if (snippet == null) {
            return null;
        }
        Matcher matcher = IMPORT_FROM_SNIPPET.matcher(snippet);
        return matcher.find() ? matcher.group(1) : null;
    }
}
