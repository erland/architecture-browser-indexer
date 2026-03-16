package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.List;
import java.util.Map;

record ArchitectureIrDependencyViewAssemblyInputs(
    List<ArchitectureRelationship> relationships,
    Map<String, ArchitectureEntity> entitiesById,
    Map<String, ArchitectureEntity> observedTypesByQualifiedName
) {
}
