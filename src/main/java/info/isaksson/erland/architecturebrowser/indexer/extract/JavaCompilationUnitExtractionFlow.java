package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class JavaCompilationUnitExtractionFlow {

    private final JavaSyntaxTreeTraversal syntaxTreeTraversal;
    private final JavaTraversalNodeDispatchFlow traversalNodeDispatchFlow;

    JavaCompilationUnitExtractionFlow(
        JavaSyntaxTreeTraversal syntaxTreeTraversal,
        JavaTraversalNodeDispatchFlow traversalNodeDispatchFlow
    ) {
        this.syntaxTreeTraversal = syntaxTreeTraversal;
        this.traversalNodeDispatchFlow = traversalNodeDispatchFlow;
    }

    ExtractionAccumulator extractCompilationUnit(
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
            String simpleName = JavaSyntaxTreeExtractionStage.simpleName(imported);
            if (simpleName != null && !simpleName.isBlank()) {
                importsBySimpleName.putIfAbsent(simpleName, imported);
            }
            var external = ExtractionSupport.externalDependencyEntity("java", imported, relativePath, line);
            accumulator.addEntity(external);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                fileEntity.id(), external.id(), imported,
                ExtractionSupport.sourceRef(relativePath, line, importNode.textSnippet(), Map.of("language", "java", "kind", "import")),
                "java",
                new JavaRelationshipEvidenceEmitter().dependencyMetadata("import", "evidence")
            ));
        }

        Map<String, JavaDeclaredType> declaredTypes = JavaDeclarationDiscovery.discoverDeclaredTypes(
            parseResult,
            relativePath,
            packageName,
            extractionMode,
            packageScope.id(),
            root
        );

        JavaExtractionContext extractionContext = new JavaExtractionContext(
            relativePath,
            packageName,
            parseResult.request() == null ? null : parseResult.request().sourceText(),
            Map.copyOf(importsBySimpleName),
            Map.copyOf(declaredTypes)
        );

        syntaxTreeTraversal.traverse(
            root,
            new JavaSyntaxTreeTraversal.JavaTraversalOwnership(null, null, null),
            (node, ownership) -> traversalNodeDispatchFlow.handleNode(
                parseResult,
                accumulator,
                relativePath,
                packageName,
                extractionMode,
                packageScope.id(),
                fileScope.id(),
                fileEntity.id(),
                node,
                ownership,
                importsBySimpleName,
                declaredTypes,
                extractionContext
            ).ownership()
        );
        return accumulator;
    }

    private static Optional<String> importQualifiedName(String snippet) {
        return SyntaxTreeExtractionSupport.extractQualifiedName(
            snippet == null ? null : snippet.replaceFirst("^\s*import\s+", "").replaceFirst(";\s*$", "")
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
