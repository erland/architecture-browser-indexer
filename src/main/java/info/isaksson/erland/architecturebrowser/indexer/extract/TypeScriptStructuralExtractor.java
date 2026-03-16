package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;

final class TypeScriptStructuralExtractor implements StructuralExtractor {
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

        TypeScriptExtractionContext context = new TypeScriptExtractionContext(
            parseResult,
            accumulator,
            relativePath,
            extractionMode,
            syntaxTree.root(),
            fileEntity
        );

        TypeScriptImportExtractor.extract(context);
        var declarations = TypeScriptDeclarationExtractor.extract(context);
        TypeScriptFrontendSemanticsExtractor.extract(context, declarations.namedEntities());
        return accumulator;
    }
}
