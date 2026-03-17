package info.isaksson.erland.architecturebrowser.indexer.extract;

final class JavaJaxRsMethodSemantics {
    private final JavaJaxRsSemanticsSupport jaxRsSemantics;

    JavaJaxRsMethodSemantics(JavaJaxRsSemanticsSupport jaxRsSemantics) {
        this.jaxRsSemantics = jaxRsSemantics;
    }

    void apply(ExtractionAccumulator accumulator, JavaMethodContext methodContext) {
        jaxRsSemantics.addJaxRsEndpointFacts(accumulator, methodContext);
    }
}
