package info.isaksson.erland.architecturebrowser.indexer.topology;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TopologyInferenceState {
    private final Map<String, LogicalScope> inferredScopes = new LinkedHashMap<>();
    private final Map<String, ArchitectureEntity> inferredEntities = new LinkedHashMap<>();
    private final Map<String, ArchitectureRelationship> inferredRelationships = new LinkedHashMap<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    Map<String, LogicalScope> inferredScopes() {
        return inferredScopes;
    }

    Map<String, ArchitectureEntity> inferredEntities() {
        return inferredEntities;
    }

    Map<String, ArchitectureRelationship> inferredRelationships() {
        return inferredRelationships;
    }

    List<Diagnostic> diagnostics() {
        return diagnostics;
    }
}
