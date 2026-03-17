package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.List;
import java.util.Map;

record ArchitectureIrAssemblyCompositionResult(
    List<ArchitectureRelationship> relationships,
    Map<String, Object> dependencyViews,
    Map<String, ArchitectureEntity> enrichedEntitiesById
) {
}
