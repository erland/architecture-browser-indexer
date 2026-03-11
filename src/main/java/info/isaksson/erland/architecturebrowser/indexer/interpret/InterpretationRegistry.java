package info.isaksson.erland.architecturebrowser.indexer.interpret;

import java.util.List;

public final class InterpretationRegistry {
    private final List<InterpretationRule> rules;

    public InterpretationRegistry(List<InterpretationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<InterpretationRule> rules() {
        return rules;
    }

    public static InterpretationRegistry defaultRegistry() {
        return new InterpretationRegistry(List.of(
            new JavaBackendInterpretationRule(),
            new TypeScriptFrontendInterpretationRule()
        ));
    }
}
