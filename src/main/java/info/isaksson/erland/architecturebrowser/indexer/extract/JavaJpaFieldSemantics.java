package info.isaksson.erland.architecturebrowser.indexer.extract;

final class JavaJpaFieldSemantics {
    private final JavaJpaSemanticsSupport jpaSemantics;

    JavaJpaFieldSemantics(JavaJpaSemanticsSupport jpaSemantics) {
        this.jpaSemantics = jpaSemantics;
    }

    void apply(ExtractionAccumulator accumulator, JavaFieldContext fieldContext) {
        jpaSemantics.addJpaFieldFacts(accumulator, fieldContext);
    }
}
