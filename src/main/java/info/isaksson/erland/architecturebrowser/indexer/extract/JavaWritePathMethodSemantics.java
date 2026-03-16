package info.isaksson.erland.architecturebrowser.indexer.extract;

final class JavaWritePathMethodSemantics {
    private final JavaSyntaxTreeExtractionStage.JavaWritePathSemantics writePathSemantics;

    JavaWritePathMethodSemantics(JavaSyntaxTreeExtractionStage.JavaWritePathSemantics writePathSemantics) {
        this.writePathSemantics = writePathSemantics;
    }

    void apply(ExtractionAccumulator accumulator, JavaMethodContext methodContext) {
        writePathSemantics.addWritePathFacts(accumulator, methodContext);
    }
}
