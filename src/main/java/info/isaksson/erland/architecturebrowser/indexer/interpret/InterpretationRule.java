package info.isaksson.erland.architecturebrowser.indexer.interpret;

public interface InterpretationRule {
    String ruleId();

    void apply(InterpretationContext context, InterpretationAccumulator accumulator);
}
