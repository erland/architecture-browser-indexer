package info.isaksson.erland.architecturebrowser.indexer.extract;

final class JavaJpaFieldSemantics {
    private final JavaSyntaxTreeExtractionStage.JavaJpaSemantics jpaSemantics;

    JavaJpaFieldSemantics(JavaSyntaxTreeExtractionStage.JavaJpaSemantics jpaSemantics) {
        this.jpaSemantics = jpaSemantics;
    }

    void apply(ExtractionAccumulator accumulator, JavaFieldContext fieldContext) {
        jpaSemantics.addJpaFieldFacts(accumulator, fieldContext);
    }
}
