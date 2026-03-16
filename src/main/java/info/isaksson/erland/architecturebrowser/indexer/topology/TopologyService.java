package info.isaksson.erland.architecturebrowser.indexer.topology;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologySummary;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TopologyService {
    private final TopologyScopeInferenceService scopeInferenceService;
    private final TopologyRelationshipRollupService relationshipRollupService;

    public TopologyService() {
        this(new DefaultTopologyRelationshipResolver());
    }

    public TopologyService(TopologyRelationshipResolver relationshipResolver) {
        this(new TopologyScopeInferenceService(), new TopologyRelationshipRollupService(relationshipResolver));
    }

    TopologyService(
        TopologyScopeInferenceService scopeInferenceService,
        TopologyRelationshipRollupService relationshipRollupService
    ) {
        this.scopeInferenceService = scopeInferenceService;
        this.relationshipRollupService = relationshipRollupService;
    }

    public TopologyResult infer(FileInventory inventory, StructuralExtractionResult extractionResult, InterpretationResult interpretationResult) {
        TopologyInferenceState state = new TopologyInferenceState();
        TopologyScopeInferenceContext scopeContext = scopeInferenceService.infer(inventory, extractionResult, state);
        relationshipRollupService.inferRelationshipRollups(extractionResult, scopeContext, state);

        TopologySummary summary = new TopologySummary(
            state.inferredScopes().size(),
            state.inferredEntities().size(),
            state.inferredRelationships().size(),
            countsByKind(state.inferredScopes().values(), LogicalScope::kind),
            countsByKind(state.inferredEntities().values(), ArchitectureEntity::kind),
            countsByKind(state.inferredRelationships().values(), ArchitectureRelationship::kind)
        );

        return new TopologyResult(
            state.inferredScopes().values().stream().sorted(Comparator.comparing(LogicalScope::displayName)).toList(),
            java.util.List.copyOf(state.inferredEntities().values()),
            java.util.List.copyOf(state.inferredRelationships().values()),
            java.util.List.copyOf(state.diagnostics()),
            summary
        );
    }

    private static <T, K extends Enum<K>> Map<String, Integer> countsByKind(Collection<T> values, java.util.function.Function<T, K> classifier) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (T value : values) {
            String key = classifier.apply(value).name();
            counts.merge(key, 1, Integer::sum);
        }
        return Map.copyOf(counts);
    }
}
