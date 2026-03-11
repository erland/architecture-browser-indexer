package info.isaksson.erland.architecturebrowser.indexer.topology.model;

import java.util.Map;

public record TopologySummary(
    int inferredScopeCount,
    int inferredEntityCount,
    int inferredRelationshipCount,
    Map<String, Integer> scopesByKind,
    Map<String, Integer> entitiesByKind,
    Map<String, Integer> relationshipsByKind
) {
    public TopologySummary {
        scopesByKind = scopesByKind == null ? Map.of() : Map.copyOf(scopesByKind);
        entitiesByKind = entitiesByKind == null ? Map.of() : Map.copyOf(entitiesByKind);
        relationshipsByKind = relationshipsByKind == null ? Map.of() : Map.copyOf(relationshipsByKind);
    }
}
