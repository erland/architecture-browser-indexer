package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.Map;

final class JavaTypeSemanticsFlow {
    private final JavaSyntaxTreeExtractionStage.JavaJaxRsSemantics jaxRsSemantics;
    private final JavaSyntaxTreeExtractionStage.JavaJpaSemantics jpaSemantics;

    JavaTypeSemanticsFlow(
        JavaSyntaxTreeExtractionStage.JavaJaxRsSemantics jaxRsSemantics,
        JavaSyntaxTreeExtractionStage.JavaJpaSemantics jpaSemantics
    ) {
        this.jaxRsSemantics = jaxRsSemantics;
        this.jpaSemantics = jpaSemantics;
    }

    void applyTypeSemantics(ExtractionAccumulator accumulator, JavaTypeContext typeContext) {
        jaxRsSemantics.addJaxRsResourceMetadata(accumulator, typeContext);
        jpaSemantics.addJpaTypeMetadata(accumulator, typeContext);
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
        jpaSemantics.addJpaInheritanceFacts(accumulator, relativePath, packageName, typeNode, typeEntity, importsBySimpleName, declaredTypes);
    }

    static String typeNodeSnippet(SyntaxNode typeNode, ExtractedEntityFact typeEntity) {
        if (typeEntity != null && !typeEntity.sourceRefs().isEmpty() && typeEntity.sourceRefs().getFirst().snippet() != null) {
            return typeEntity.sourceRefs().getFirst().snippet();
        }
        return typeNode == null ? "" : typeNode.textSnippet();
    }
}
