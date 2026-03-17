package info.isaksson.erland.architecturebrowser.indexer.extract;

final class JavaWritePathMethodSemantics {
    private final JavaWritePathSemanticsSupport writePathSemantics;

    JavaWritePathMethodSemantics(JavaWritePathSemanticsSupport writePathSemantics) {
        this.writePathSemantics = writePathSemantics;
    }

    void apply(ExtractionAccumulator accumulator, JavaMethodContext methodContext) {
        writePathSemantics.addWritePathFacts(accumulator, methodContext);
    }
}
