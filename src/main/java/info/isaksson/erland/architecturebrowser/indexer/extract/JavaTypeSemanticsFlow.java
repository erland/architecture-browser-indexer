package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.Map;

final class JavaTypeSemanticsFlow {
    private final JavaTypeNodeSemanticsSupport typeNodeSemanticsSupport;

    JavaTypeSemanticsFlow(JavaTypeNodeSemanticsSupport typeNodeSemanticsSupport) {
        this.typeNodeSemanticsSupport = typeNodeSemanticsSupport;
    }

    void applyTypeSemantics(ExtractionAccumulator accumulator, JavaTypeContext typeContext) {
        typeNodeSemanticsSupport.applyTypeSemantics(accumulator, typeContext);
    }

    void applyJpaInheritanceFacts(
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        SyntaxNode typeNode,
        ExtractedEntityFact typeEntity,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        typeNodeSemanticsSupport.addJpaInheritanceFacts(accumulator, relativePath, packageName, typeNode, typeEntity, importsBySimpleName, declaredTypes);
    }

    static String typeNodeSnippet(SyntaxNode typeNode, ExtractedEntityFact typeEntity) {
        return JavaTypeNodeSemanticsSupport.typeNodeSnippet(typeNode, typeEntity);
    }
}
