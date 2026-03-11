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

        for (SyntaxNode importNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of("import_declaration"))) {
            String imported = importQualifiedName(importNode.textSnippet()).orElse(null);
            if (imported == null || imported.isBlank()) {
                continue;
            }
            int line = SyntaxTreeExtractionSupport.oneBasedLine(importNode);
            var external = ExtractionSupport.externalDependencyEntity("java", imported, relativePath, line);
            accumulator.addEntity(external);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                fileEntity.id(), external.id(), imported,
                ExtractionSupport.sourceRef(relativePath, line, importNode.textSnippet(), Map.of("language", "java", "kind", "import")),
                "java"
            ));
        }

        for (SyntaxNode typeNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of(
            "class_declaration", "interface_declaration", "enum_declaration", "record_declaration"
        ))) {
            String typeName = SyntaxTreeExtractionSupport.declarationName(typeNode);
            if (typeName == null || typeName.isBlank()) {
                continue;
            }
            int line = SyntaxTreeExtractionSupport.oneBasedLine(typeNode);
            EntityKind kind = "interface_declaration".equals(typeNode.type()) ? EntityKind.INTERFACE : EntityKind.CLASS;
            String qualifiedName = packageName == null || packageName.isBlank() ? typeName : packageName + "." + typeName;
            List<String> annotations = SyntaxTreeExtractionSupport.descendantsByType(typeNode, Set.of(
                "marker_annotation", "annotation"
            )).stream().flatMap(node -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(node.textSnippet()).stream()).distinct().toList();
            SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, typeNode.textSnippet(), Map.of("language", "java", "kind", typeNode.type()));
            ExtractedEntityFact entity = new ExtractedEntityFact(
                IdUtils.scopedEntityId("java", relativePath, qualifiedName, line),
                kind,
                EntityOrigin.OBSERVED,
                typeName,
                qualifiedName,
                packageScope.id(),
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
            accumulator.addEntity(entity);
            accumulator.addRelationship(ExtractionSupport.containsRelationship(fileEntity.id(), entity.id(), ref));
        }

        for (SyntaxNode methodNode : SyntaxTreeExtractionSupport.findAllByType(root, Set.of(
            "method_declaration", "constructor_declaration"
        ))) {
            String methodName = SyntaxTreeExtractionSupport.declarationName(methodNode);
            if (methodName == null || methodName.isBlank()) {
                continue;
            }
            int line = SyntaxTreeExtractionSupport.oneBasedLine(methodNode);
            SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, methodNode.textSnippet(), Map.of("language", "java", "kind", methodNode.type()));
            List<String> annotations = SyntaxTreeExtractionSupport.descendantsByType(methodNode, Set.of(
                "marker_annotation", "annotation"
            )).stream().flatMap(node -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(node.textSnippet()).stream()).distinct().toList();
            ExtractedEntityFact entity = new ExtractedEntityFact(
                IdUtils.scopedEntityId("java", relativePath, methodName, line),
                EntityKind.FUNCTION,
                EntityOrigin.OBSERVED,
                methodName,
                methodName + "()",
                fileScope.id(),
                List.of(ref),
                Map.of(
                    "language", "java",
                    "parameters", SyntaxTreeExtractionSupport.parameterSnippet(methodNode),
                    "annotations", annotations,
                    "parseStatus", parseResult.status().name(),
                    "extractionMode", extractionMode.name()
                )
            );
            accumulator.addEntity(entity);
            accumulator.addRelationship(ExtractionSupport.containsRelationship(fileEntity.id(), entity.id(), ref));
        }
        return accumulator;
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
