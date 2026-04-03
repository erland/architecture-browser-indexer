package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.Map;

final class JavaStructuralExtractor implements StructuralExtractor {

    private JavaSyntaxTreeExtractionStage stage = new JavaSyntaxTreeExtractionStage();

    void setWorkspaceDeclaredTypes(Map<String, JavaDeclaredType> workspaceDeclaredTypes) {
        this.stage = new JavaSyntaxTreeExtractionStage(workspaceDeclaredTypes);
    }

    @Override
    public ParseLanguage language() {
        return stage.language();
    }

    @Override
    public ExtractionAccumulator extract(SourceParseResult parseResult, ExtractionAccumulator accumulator) {
        return stage.extract(parseResult, accumulator);
    }

    static boolean isJavaTypeDeclaration(SyntaxNode node) {
        return JavaGenericSyntaxSupport.isJavaTypeDeclaration(node);
    }

    static String simpleName(String qualifiedName) {
        return JavaGenericSyntaxSupport.simpleName(qualifiedName);
    }
}
