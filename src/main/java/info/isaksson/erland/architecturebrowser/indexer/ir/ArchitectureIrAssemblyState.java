package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;

import java.util.List;
import java.util.Map;

record ArchitectureIrAssemblyState(
    LogicalScope repositoryScope,
    ArchitectureEntity inventoryEntity,
    List<LogicalScope> scopes,
    List<ArchitectureEntity> entities,
    List<ArchitectureRelationship> relationships,
    List<Diagnostic> diagnostics,
    Map<String, ArchitectureEntity> entitiesById,
    Map<String, ArchitectureEntity> observedTypesByQualifiedName,
    Map<String, Object> dependencyViews
) {
}
