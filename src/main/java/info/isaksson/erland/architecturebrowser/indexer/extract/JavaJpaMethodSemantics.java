package info.isaksson.erland.architecturebrowser.indexer.extract;

final class JavaJpaMethodSemantics {
    private final JavaJpaSemanticsSupport jpaSemantics;

    JavaJpaMethodSemantics(JavaJpaSemanticsSupport jpaSemantics) {
        this.jpaSemantics = jpaSemantics;
    }

    void apply(ExtractionAccumulator accumulator, JavaMethodContext methodContext) {
        jpaSemantics.addJpaMethodFacts(accumulator, methodContext);
    }
}
