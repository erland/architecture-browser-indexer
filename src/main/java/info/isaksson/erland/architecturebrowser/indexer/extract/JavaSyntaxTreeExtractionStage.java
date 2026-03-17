package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;

final class JavaSyntaxTreeExtractionStage {

    private final JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter;
    private final JavaTypeDeclarationFlow typeDeclarationFlow;
    private final JavaTraversalNodeDispatchFlow traversalNodeDispatchFlow;
    private final JavaCompilationUnitExtractionFlow compilationUnitExtractionFlow;

    JavaSyntaxTreeExtractionStage() {
        this(new JavaStageCompositionSupport().composeDefault());
    }

    JavaSyntaxTreeExtractionStage(JavaStageCompositionSupport.JavaStageComposition composition) {
        this.relationshipEvidenceEmitter = composition.relationshipEvidenceEmitter();
        this.typeDeclarationFlow = composition.typeDeclarationFlow();
        this.traversalNodeDispatchFlow = composition.traversalNodeDispatchFlow();
        this.compilationUnitExtractionFlow = composition.compilationUnitExtractionFlow();
    }

    ParseLanguage language() {
        return ParseLanguage.JAVA;
    }

    ExtractionAccumulator extract(SourceParseResult parseResult, ExtractionAccumulator accumulator) {
        accumulator.incrementFilesVisited();
        if (!parseResult.successful() || parseResult.syntaxTree() == null) {
            accumulator.addDiagnostic(ExtractionSupport.extractionWarning(
                parseResult,
                "extract.java.syntax-tree-required",
                "Java structural extraction requires a successful Tree-sitter syntax tree; no regex fallback is used."
            ));
            return accumulator;
        }
        return compilationUnitExtractionFlow.extractCompilationUnit(parseResult, accumulator, parseResult.request().relativePath(), parseResult.syntaxTree());
    }
}
