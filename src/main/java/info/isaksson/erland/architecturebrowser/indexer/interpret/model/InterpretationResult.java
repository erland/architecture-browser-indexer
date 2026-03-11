package info.isaksson.erland.architecturebrowser.indexer.interpret.model;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;

import java.util.List;

public record InterpretationResult(
    List<InterpretedEntityFact> entities,
    List<InterpretedRelationshipFact> relationships,
    List<Diagnostic> diagnostics,
    InterpretationSummary summary
) {
    public InterpretationResult {
        entities = entities == null ? List.of() : List.copyOf(entities);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
