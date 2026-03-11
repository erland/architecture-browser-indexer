package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;

public final class InterpretationService {
    private final InterpretationRegistry registry;

    public InterpretationService(InterpretationRegistry registry) {
        this.registry = registry;
    }

    public InterpretationResult interpret(StructuralExtractionResult extractionResult) {
        InterpretationContext context = new InterpretationContext(extractionResult);
        InterpretationAccumulator accumulator = new InterpretationAccumulator();
        for (InterpretationRule rule : registry.rules()) {
            rule.apply(context, accumulator);
        }
        return accumulator.toResult();
    }
}
