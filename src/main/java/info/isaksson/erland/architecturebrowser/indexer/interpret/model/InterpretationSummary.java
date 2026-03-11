package info.isaksson.erland.architecturebrowser.indexer.interpret.model;

import java.util.Map;

public record InterpretationSummary(
    Map<String, Integer> inferredByKind,
    Map<String, Integer> relationshipsByKind,
    Map<String, Integer> matchesByRule
) {
    public InterpretationSummary {
        inferredByKind = inferredByKind == null ? Map.of() : Map.copyOf(inferredByKind);
        relationshipsByKind = relationshipsByKind == null ? Map.of() : Map.copyOf(relationshipsByKind);
        matchesByRule = matchesByRule == null ? Map.of() : Map.copyOf(matchesByRule);
    }
}
