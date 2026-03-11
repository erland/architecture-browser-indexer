package info.isaksson.erland.architecturebrowser.indexer.topology.model;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;

import java.util.List;

public record TopologyResult(
    List<LogicalScope> scopes,
    List<ArchitectureEntity> entities,
    List<ArchitectureRelationship> relationships,
    List<Diagnostic> diagnostics,
    TopologySummary summary
) {
    public TopologyResult {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
        entities = entities == null ? List.of() : List.copyOf(entities);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
