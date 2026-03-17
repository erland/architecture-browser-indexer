package info.isaksson.erland.architecturebrowser.indexer.extract;

final class JavaCdiMethodSemantics {
    private final JavaCdiSemanticsSupport cdiSemantics;

    JavaCdiMethodSemantics(JavaCdiSemanticsSupport cdiSemantics) {
        this.cdiSemantics = cdiSemantics;
    }

    void apply(ExtractionAccumulator accumulator, JavaMethodContext methodContext) {
        cdiSemantics.addCdiEventFacts(accumulator, methodContext);
    }
}
