package info.isaksson.erland.architecturebrowser.indexer.extract.model;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;

import java.util.List;

public record StructuralExtractionResult(
    List<LogicalScope> scopes,
    List<ExtractedEntityFact> entities,
    List<ExtractedRelationshipFact> relationships,
    List<Diagnostic> diagnostics,
    ExtractionSummary summary
) {
    public StructuralExtractionResult {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
        entities = entities == null ? List.of() : List.copyOf(entities);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
