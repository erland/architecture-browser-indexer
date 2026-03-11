package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationSummary;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class InterpretationAccumulator {
    private final Map<String, InterpretedEntityFact> entities = new LinkedHashMap<>();
    private final Map<String, InterpretedRelationshipFact> relationships = new LinkedHashMap<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final Map<String, Integer> inferredByKind = new LinkedHashMap<>();
    private final Map<String, Integer> relationshipsByKind = new LinkedHashMap<>();
    private final Map<String, Integer> matchesByRule = new LinkedHashMap<>();

    void addEntity(InterpretedEntityFact entity, String ruleId) {
        if (entities.putIfAbsent(entity.id(), entity) == null) {
            increment(inferredByKind, entity.kind().name());
            increment(matchesByRule, ruleId);
        }
    }

    void addRelationship(InterpretedRelationshipFact relationship, String ruleId) {
        if (relationships.putIfAbsent(relationship.id(), relationship) == null) {
            increment(relationshipsByKind, relationship.kind().name());
            increment(matchesByRule, ruleId);
        }
    }

    void addDiagnostic(Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    InterpretationResult toResult() {
        return new InterpretationResult(
            List.copyOf(entities.values()),
            List.copyOf(relationships.values()),
            List.copyOf(diagnostics),
            new InterpretationSummary(
                Map.copyOf(inferredByKind),
                Map.copyOf(relationshipsByKind),
                Map.copyOf(matchesByRule)
            )
        );
    }

    private static void increment(Map<String, Integer> map, String key) {
        map.merge(key, 1, Integer::sum);
    }
}
