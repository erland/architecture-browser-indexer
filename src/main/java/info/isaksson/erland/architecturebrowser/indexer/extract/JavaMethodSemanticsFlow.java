package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.Map;

final class JavaMethodSemanticsFlow {
    private final JavaSyntaxTreeExtractionStage.JavaJaxRsSemantics jaxRsSemantics;
    private final JavaSyntaxTreeExtractionStage.JavaJpaSemantics jpaSemantics;
    private final JavaSyntaxTreeExtractionStage.JavaCdiSemantics cdiSemantics;
    private final JavaSyntaxTreeExtractionStage.JavaWritePathSemantics writePathSemantics;

    JavaMethodSemanticsFlow(
        JavaSyntaxTreeExtractionStage.JavaJaxRsSemantics jaxRsSemantics,
        JavaSyntaxTreeExtractionStage.JavaJpaSemantics jpaSemantics,
        JavaSyntaxTreeExtractionStage.JavaCdiSemantics cdiSemantics,
        JavaSyntaxTreeExtractionStage.JavaWritePathSemantics writePathSemantics
    ) {
        this.jaxRsSemantics = jaxRsSemantics;
        this.jpaSemantics = jpaSemantics;
        this.cdiSemantics = cdiSemantics;
        this.writePathSemantics = writePathSemantics;
    }

    void applyMethodSemantics(
        ExtractionAccumulator accumulator,
        JavaExtractionContext extractionContext,
        SyntaxNode methodNode,
        ExtractedEntityFact methodEntity,
        String ownerTypeEntityId,
        String ownerQualifiedName,
        String ownerTypeSnippet
    ) {
        JavaMethodContext methodContext = javaMethodContext(
            extractionContext,
            methodNode,
            methodEntity,
            ownerTypeEntityId,
            ownerQualifiedName,
            ownerTypeSnippet
        );
        jaxRsSemantics.addJaxRsEndpointFacts(accumulator, methodContext);
        jpaSemantics.addJpaMethodFacts(accumulator, methodContext);
        cdiSemantics.addCdiEventFacts(accumulator, methodContext);
        writePathSemantics.addWritePathFacts(accumulator, methodContext);
    }

    private JavaMethodContext javaMethodContext(
        JavaExtractionContext extractionContext,
        SyntaxNode methodNode,
        ExtractedEntityFact methodEntity,
        String ownerTypeEntityId,
        String ownerQualifiedName,
        String ownerTypeSnippet
    ) {
        String snippet = methodEntity.sourceRefs().isEmpty() ? (methodNode == null ? "" : methodNode.textSnippet()) : methodEntity.sourceRefs().getFirst().snippet();
        if ((snippet == null || snippet.isBlank()) && methodNode != null) {
            snippet = methodNode.textSnippet();
        }
        SourceReference ref = methodEntity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(
                extractionContext.relativePath(),
                SyntaxTreeExtractionSupport.oneBasedLine(methodNode),
                snippet,
                Map.of("language", "java", "kind", methodNode == null ? "method_declaration" : methodNode.type())
            )
            : methodEntity.sourceRefs().getFirst();
        return new JavaMethodContext(
            extractionContext,
            methodNode,
            methodEntity,
            ownerTypeEntityId,
            ownerQualifiedName,
            ownerTypeSnippet,
            ref,
            snippet
        );
    }
}
