package info.isaksson.erland.architecturebrowser.indexer.extract;

final class JavaJaxRsMethodSemantics {
    private final JavaSyntaxTreeExtractionStage.JavaJaxRsSemantics jaxRsSemantics;

    JavaJaxRsMethodSemantics(JavaSyntaxTreeExtractionStage.JavaJaxRsSemantics jaxRsSemantics) {
        this.jaxRsSemantics = jaxRsSemantics;
    }

    void apply(ExtractionAccumulator accumulator, JavaMethodContext methodContext) {
        jaxRsSemantics.addJaxRsEndpointFacts(accumulator, methodContext);
    }
}
