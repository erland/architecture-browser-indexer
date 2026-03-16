package info.isaksson.erland.architecturebrowser.indexer.extract;

final class JavaJpaMethodSemantics {
    private final JavaSyntaxTreeExtractionStage.JavaJpaSemantics jpaSemantics;

    JavaJpaMethodSemantics(JavaSyntaxTreeExtractionStage.JavaJpaSemantics jpaSemantics) {
        this.jpaSemantics = jpaSemantics;
    }

    void apply(ExtractionAccumulator accumulator, JavaMethodContext methodContext) {
        jpaSemantics.addJpaMethodFacts(accumulator, methodContext);
    }
}
