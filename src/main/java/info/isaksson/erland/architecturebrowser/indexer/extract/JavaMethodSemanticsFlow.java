package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.Map;

final class JavaMethodSemanticsFlow {
    private final JavaJaxRsMethodSemantics jaxRsMethodSemantics;
    private final JavaJpaMethodSemantics jpaMethodSemantics;
    private final JavaCdiMethodSemantics cdiMethodSemantics;
    private final JavaWritePathMethodSemantics writePathMethodSemantics;

    JavaMethodSemanticsFlow(
        JavaJaxRsMethodSemantics jaxRsMethodSemantics,
        JavaJpaMethodSemantics jpaMethodSemantics,
        JavaCdiMethodSemantics cdiMethodSemantics,
        JavaWritePathMethodSemantics writePathMethodSemantics
    ) {
        this.jaxRsMethodSemantics = jaxRsMethodSemantics;
        this.jpaMethodSemantics = jpaMethodSemantics;
        this.cdiMethodSemantics = cdiMethodSemantics;
        this.writePathMethodSemantics = writePathMethodSemantics;
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
        jaxRsMethodSemantics.apply(accumulator, methodContext);
        jpaMethodSemantics.apply(accumulator, methodContext);
        cdiMethodSemantics.apply(accumulator, methodContext);
        writePathMethodSemantics.apply(accumulator, methodContext);
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
