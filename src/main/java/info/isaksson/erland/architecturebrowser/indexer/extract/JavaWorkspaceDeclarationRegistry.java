package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class JavaWorkspaceDeclarationRegistry {
    private JavaWorkspaceDeclarationRegistry() {}

    static Map<String, JavaDeclaredType> discover(ParseBatchResult parseBatchResult) {
        if (parseBatchResult == null || parseBatchResult.results().isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, JavaDeclaredType> discovered = new LinkedHashMap<>();
        for (SourceParseResult result : parseBatchResult.results()) {
            if (result == null
                || result.request() == null
                || result.request().language() != ParseLanguage.JAVA
                || !result.successful()
                || result.syntaxTree() == null) {
                continue;
            }
            SyntaxNode root = result.syntaxTree().root();
            if (root == null) {
                continue;
            }
            String relativePath = result.request().relativePath();
            String packageName = SyntaxTreeExtractionSupport.findAllByType(root, Set.of("package_declaration")).stream()
                .findFirst()
                .flatMap(node -> SyntaxTreeExtractionSupport.extractQualifiedName(node.textSnippet()))
                .orElse(JavaCompilationUnitExtractionFlow.derivePackageFromPath(relativePath));
            Map<String, JavaDeclaredType> perFile = JavaDeclarationDiscovery.discoverDeclaredTypes(
                result,
                relativePath,
                packageName,
                ExtractionMode.SYNTAX_TREE,
                ExtractionSupport.packageScope("scope:repo", packageName, relativePath, "java").id(),
                root
            );
            for (Map.Entry<String, JavaDeclaredType> entry : perFile.entrySet()) {
                discovered.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(discovered);
    }
}
