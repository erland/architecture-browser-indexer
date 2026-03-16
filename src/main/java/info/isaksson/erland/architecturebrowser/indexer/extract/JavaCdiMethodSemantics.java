package info.isaksson.erland.architecturebrowser.indexer.extract;

final class JavaCdiMethodSemantics {
    private final JavaSyntaxTreeExtractionStage.JavaCdiSemantics cdiSemantics;

    JavaCdiMethodSemantics(JavaSyntaxTreeExtractionStage.JavaCdiSemantics cdiSemantics) {
        this.cdiSemantics = cdiSemantics;
    }

    void apply(ExtractionAccumulator accumulator, JavaMethodContext methodContext) {
        cdiSemantics.addCdiEventFacts(accumulator, methodContext);
    }
}
