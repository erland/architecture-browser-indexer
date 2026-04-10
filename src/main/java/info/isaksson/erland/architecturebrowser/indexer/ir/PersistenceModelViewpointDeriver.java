package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRelationshipSemantic;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRole;

import java.util.List;

final class PersistenceModelViewpointDeriver implements ArchitectureViewpointDeriver {
    @Override
    public ArchitectureViewpoint derive(ViewpointEvidence evidence) {
        List<String> persistentEntities = evidence.entityIdsForRole(ArchitecturalRole.PERSISTENT_ENTITY.id());
        List<String> persistenceAccess = evidence.entityIdsForRole(ArchitecturalRole.PERSISTENCE_ACCESS.id());
        boolean hasCorePersistenceRoles = !persistentEntities.isEmpty() || !persistenceAccess.isEmpty();
        boolean hasPersistenceRelationshipCatalog = evidence.hasDependencyViewList("entityAssociationRelationships");
        boolean hasPersistenceSemantics = evidence.hasSemantic(ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id())
            || evidence.hasSemantic(ArchitecturalRelationshipSemantic.STORED_IN.id());
        String availability;
        double confidence;
        if (hasCorePersistenceRoles || hasPersistenceRelationshipCatalog) {
            availability = "available";
            confidence = ArchitectureViewpointDerivationSupport.clamp(0.76
                + (!persistentEntities.isEmpty() ? 0.08 : 0.0)
                + (!persistenceAccess.isEmpty() ? 0.08 : 0.0)
                + (hasPersistenceRelationshipCatalog ? 0.10 : 0.0)
                + (hasPersistenceSemantics ? 0.04 : 0.0));
        } else if (hasPersistenceSemantics || evidence.hasEntityKind(EntityKind.DATASTORE)) {
            availability = "partial";
            confidence = ArchitectureViewpointDerivationSupport.clamp(0.44
                + (hasPersistenceSemantics ? 0.16 : 0.0)
                + (evidence.hasEntityKind(EntityKind.DATASTORE) ? 0.08 : 0.0));
        } else {
            availability = "unavailable";
            confidence = 0.0;
        }
        List<String> seedEntityIds = ArchitectureViewpointDerivationSupport.mergeIds(persistentEntities, persistenceAccess);
        return new ArchitectureViewpoint(
            "persistence-model",
            "Persistence model",
            "Highlights persistent entities together with persistence access paths.",
            availability,
            confidence,
            seedEntityIds.isEmpty() ? null : seedEntityIds,
            ArchitectureViewpointDerivationSupport.roleIdsPresent(evidence,
                ArchitecturalRole.PERSISTENT_ENTITY.id(), ArchitecturalRole.PERSISTENCE_ACCESS.id()),
            ArchitectureViewpointDerivationSupport.presentSemantics(evidence,
                ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id(),
                ArchitecturalRelationshipSemantic.STORED_IN.id()),
            null,
            ArchitectureViewpointDerivationSupport.evidenceSources(
                evidence,
                hasCorePersistenceRoles,
                hasPersistenceSemantics || hasPersistenceRelationshipCatalog,
                true,
                false)
        );
    }
}
