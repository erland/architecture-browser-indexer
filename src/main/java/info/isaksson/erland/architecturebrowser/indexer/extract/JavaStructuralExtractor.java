package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

final class JavaStructuralExtractor implements StructuralExtractor {

    private final JavaSyntaxTreeExtractionStage stage = new JavaSyntaxTreeExtractionStage();

    @Override
    public ParseLanguage language() {
        return stage.language();
    }

    @Override
    public ExtractionAccumulator extract(SourceParseResult parseResult, ExtractionAccumulator accumulator) {
        return stage.extract(parseResult, accumulator);
    }

    static boolean isJavaTypeDeclaration(SyntaxNode node) {
        return JavaSyntaxTreeExtractionStage.isJavaTypeDeclaration(node);
    }

    static String simpleName(String qualifiedName) {
        return JavaSyntaxTreeExtractionStage.simpleName(qualifiedName);
    }
}
