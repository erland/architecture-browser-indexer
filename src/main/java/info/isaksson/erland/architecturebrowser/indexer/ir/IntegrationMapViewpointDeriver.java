package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRelationshipSemantic;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRole;

import java.util.List;

final class IntegrationMapViewpointDeriver implements ArchitectureViewpointDeriver {
    @Override
    public ArchitectureViewpoint derive(ViewpointEvidence evidence) {
        List<String> adapters = evidence.entityIdsForRole(ArchitecturalRole.INTEGRATION_ADAPTER.id());
        List<String> externals = evidence.entityIdsForRole(ArchitecturalRole.EXTERNAL_DEPENDENCY.id());
        boolean hasExternalCallSemantics = evidence.hasSemantic(ArchitecturalRelationshipSemantic.CALLS_EXTERNAL_SYSTEM.id());
        boolean hasExternalKinds = evidence.hasEntityKind(EntityKind.EXTERNAL_SYSTEM);
        String availability;
        double confidence;
        if ((hasExternalCallSemantics && (!adapters.isEmpty() || !externals.isEmpty())) || (!adapters.isEmpty() && !externals.isEmpty())) {
            availability = "available";
            confidence = ArchitectureViewpointDerivationSupport.clamp(0.72
                + (hasExternalCallSemantics ? 0.10 : 0.0)
                + (!adapters.isEmpty() ? 0.08 : 0.0)
                + (!externals.isEmpty() ? 0.08 : 0.0));
        } else if (hasExternalCallSemantics || !adapters.isEmpty() || !externals.isEmpty() || hasExternalKinds) {
            availability = "partial";
            confidence = ArchitectureViewpointDerivationSupport.clamp(0.40
                + (hasExternalCallSemantics ? 0.18 : 0.0)
                + ((!adapters.isEmpty() || !externals.isEmpty() || hasExternalKinds) ? 0.14 : 0.0));
        } else {
            availability = "unavailable";
            confidence = 0.0;
        }
        List<String> seedEntityIds = ArchitectureViewpointDerivationSupport.mergeIds(adapters, externals);
        return new ArchitectureViewpoint(
            "integration-map",
            "Integration map",
            "Highlights exported integration-facing dependencies when present in the graph.",
            availability,
            confidence,
            seedEntityIds.isEmpty() ? null : seedEntityIds,
            ArchitectureViewpointDerivationSupport.roleIdsPresent(evidence,
                ArchitecturalRole.INTEGRATION_ADAPTER.id(), ArchitecturalRole.EXTERNAL_DEPENDENCY.id()),
            ArchitectureViewpointDerivationSupport.presentSemantics(evidence,
                ArchitecturalRelationshipSemantic.CALLS_EXTERNAL_SYSTEM.id()),
            null,
            ArchitectureViewpointDerivationSupport.evidenceSources(
                evidence,
                !adapters.isEmpty() || !externals.isEmpty() || hasExternalKinds,
                hasExternalCallSemantics,
                false,
                true)
        );
    }
}
